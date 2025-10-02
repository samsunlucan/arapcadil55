import numpy as np
import pandas as pd
from typing import Dict, List, Optional, Literal
from datetime import datetime
from collections import deque
from loguru import logger
import json
from pathlib import Path

from config import settings


class MetricsTracker:
    """Tahmin performansını izle ve hata analizi yap"""
    
    def __init__(self, symbol: str, horizon: str):
        self.symbol = symbol
        self.horizon = horizon
        
        # Tahmin geçmişi (FIFO queue)
        self.predictions: deque = deque(maxlen=settings.METRIC_WINDOW_SIZE)
        
        # Metrik dosyası
        self.metrics_dir = Path(f"metrics/{symbol}")
        self.metrics_dir.mkdir(parents=True, exist_ok=True)
        self.metrics_file = self.metrics_dir / f"{horizon}_predictions.jsonl"
    
    def record_prediction(
        self,
        timestamp: datetime,
        point_forecast: float,
        interval_80: List[float],
        interval_95: List[float],
        direction_prob_up: float,
        confidence: float,
        current_price: float,
        features_used: List[str]
    ):
        """Tahmini kaydet"""
        
        record = {
            "timestamp": timestamp.isoformat(),
            "point_forecast": point_forecast,
            "interval_80": interval_80,
            "interval_95": interval_95,
            "direction_prob_up": direction_prob_up,
            "confidence": confidence,
            "current_price": current_price,
            "features_used": features_used,
            "actual_price": None,  # Sonradan doldurulacak
            "actual_direction": None
        }
        
        self.predictions.append(record)
        
        # Diske kaydet (append mode)
        if settings.STORE_PREDICTIONS:
            with open(self.metrics_file, "a") as f:
                f.write(json.dumps(record) + "\n")
    
    def update_actual(self, timestamp: datetime, actual_price: float):
        """Gerçekleşen fiyatı güncelle"""
        
        for record in self.predictions:
            if record["timestamp"] == timestamp.isoformat() and record["actual_price"] is None:
                record["actual_price"] = actual_price
                
                # Yön hesapla
                direction_up = actual_price > record["current_price"]
                record["actual_direction"] = 1 if direction_up else 0
                
                logger.debug(f"Updated actual for {timestamp}: {actual_price}")
                break
    
    def compute_metrics(self) -> Dict:
        """Performans metriklerini hesapla"""
        
        # Sadece gerçekleşmiş tahminler
        completed = [p for p in self.predictions if p["actual_price"] is not None]
        
        if len(completed) < 5:
            return {
                "n_predictions": len(completed),
                "warning": "Insufficient completed predictions for metrics"
            }
        
        # Arrays
        y_true = np.array([p["actual_price"] for p in completed])
        y_pred = np.array([p["point_forecast"] for p in completed])
        
        # 1. Point forecast errors
        errors = y_true - y_pred
        abs_errors = np.abs(errors)
        pct_errors = abs_errors / y_true * 100
        
        mae = float(np.mean(abs_errors))
        rmse = float(np.sqrt(np.mean(errors ** 2)))
        mape = float(np.mean(pct_errors))
        
        # Symmetric MAPE
        smape = float(np.mean(2 * abs_errors / (np.abs(y_true) + np.abs(y_pred)) * 100))
        
        # 2. Direction accuracy
        direction_true = np.array([p["actual_direction"] for p in completed])
        direction_pred = np.array([1 if p["direction_prob_up"] > 0.5 else 0 for p in completed])
        
        direction_accuracy = float(np.mean(direction_true == direction_pred))
        
        # 3. Calibration (Brier score for probabilistic predictions)
        direction_probs = np.array([p["direction_prob_up"] for p in completed])
        brier_score = float(np.mean((direction_probs - direction_true) ** 2))
        
        # 4. Interval coverage (80% ve 95% aralıklarının kapsama oranı)
        in_80 = sum(
            p["interval_80"][0] <= p["actual_price"] <= p["interval_80"][1]
            for p in completed
        )
        coverage_80 = in_80 / len(completed)
        
        in_95 = sum(
            p["interval_95"][0] <= p["actual_price"] <= p["interval_95"][1]
            for p in completed
        )
        coverage_95 = in_95 / len(completed)
        
        # 5. Confidence vs actual accuracy correlation
        confidences = np.array([p["confidence"] for p in completed])
        accuracies = 1 - pct_errors / 100  # 0-1 scale
        
        if len(confidences) > 10:
            confidence_corr = float(np.corrcoef(confidences, accuracies)[0, 1])
        else:
            confidence_corr = 0.0
        
        # 6. Bias (sistematik hata)
        bias = float(np.mean(errors))
        bias_pct = float(bias / np.mean(y_true) * 100)
        
        return {
            "n_predictions": len(completed),
            "mae": mae,
            "rmse": rmse,
            "mape": mape,
            "smape": smape,
            "bias": bias,
            "bias_pct": bias_pct,
            "direction_accuracy": direction_accuracy,
            "brier_score": brier_score,
            "coverage_80": coverage_80,
            "coverage_95": coverage_95,
            "confidence_correlation": confidence_corr,
            "avg_confidence": float(np.mean(confidences))
        }
    
    def analyze_errors(self) -> Dict:
        """Hata analizi ve öğrenme önerileri"""
        
        completed = [p for p in self.predictions if p["actual_price"] is not None]
        
        if len(completed) < 10:
            return {"learning_action": "no_update", "reason": "Insufficient data"}
        
        # Recent vs older performance (concept drift detection)
        recent_n = min(20, len(completed) // 2)
        recent = completed[-recent_n:]
        older = completed[:-recent_n]
        
        recent_mape = np.mean([
            abs(p["actual_price"] - p["point_forecast"]) / p["actual_price"] * 100
            for p in recent
        ])
        
        older_mape = np.mean([
            abs(p["actual_price"] - p["point_forecast"]) / p["actual_price"] * 100
            for p in older
        ]) if older else recent_mape
        
        performance_degradation = (recent_mape - older_mape) / older_mape if older_mape > 0 else 0
        
        # Error by model component (eğer kayıtlıysa)
        # Burada basitleştirilmiş heuristik
        
        learning_action = "no_update"
        reason = ""
        
        if performance_degradation > 0.3:  # %30+ degradation
            learning_action = "retrain_needed"
            reason = f"Performance degraded by {performance_degradation*100:.1f}% - possible concept drift"
        
        elif recent_mape > 5.0:  # %5+ MAPE
            learning_action = "update_weights"
            reason = f"High recent error (MAPE={recent_mape:.2f}%) - update ensemble weights"
        
        elif len(completed) % 50 == 0:  # Periodic update
            learning_action = "update_weights"
            reason = "Periodic weight update"
        
        # Yön tahminlerinin kalibrasyonu
        direction_errors = [
            abs(p["direction_prob_up"] - p["actual_direction"])
            for p in recent
        ]
        avg_direction_error = np.mean(direction_errors)
        
        if avg_direction_error > 0.3:
            learning_action = "retrain_needed"
            reason += " | Direction predictions poorly calibrated"
        
        return {
            "learning_action": learning_action,
            "reason": reason,
            "recent_mape": float(recent_mape),
            "performance_degradation_pct": float(performance_degradation * 100),
            "avg_direction_error": float(avg_direction_error)
        }
    
    def get_feature_importance_analysis(self) -> List[str]:
        """En sık kullanılan ve en başarılı özellikleri analiz et"""
        
        completed = [p for p in self.predictions if p["actual_price"] is not None]
        
        if len(completed) < 10:
            return []
        
        # Feature kullanım sıklığı
        feature_counts = {}
        for p in completed:
            for feat in p.get("features_used", []):
                feature_counts[feat] = feature_counts.get(feat, 0) + 1
        
        # Sıklığa göre sırala
        top_features = sorted(feature_counts.items(), key=lambda x: x[1], reverse=True)
        
        return [feat for feat, _ in top_features[:5]]
    
    def load_history(self) -> bool:
        """Geçmiş tahminleri yükle"""
        
        if not self.metrics_file.exists():
            return False
        
        try:
            with open(self.metrics_file, "r") as f:
                for line in f:
                    record = json.loads(line.strip())
                    self.predictions.append(record)
            
            logger.info(f"Loaded {len(self.predictions)} prediction records")
            return True
            
        except Exception as e:
            logger.error(f"Error loading prediction history: {e}")
            return False


class PerformanceReporter:
    """Çoklu varlık ve horizon için performans raporu"""
    
    @staticmethod
    def generate_report(symbols: List[str], horizons: List[str]) -> Dict:
        """Tüm varlıklar için özet rapor"""
        
        report = {
            "timestamp": datetime.now().isoformat(),
            "assets": []
        }
        
        for symbol in symbols:
            asset_metrics = {"symbol": symbol, "horizons": {}}
            
            for horizon in horizons:
                tracker = MetricsTracker(symbol, horizon)
                tracker.load_history()
                
                metrics = tracker.compute_metrics()
                analysis = tracker.analyze_errors()
                
                asset_metrics["horizons"][horizon] = {
                    "metrics": metrics,
                    "analysis": analysis
                }
            
            report["assets"].append(asset_metrics)
        
        return report


if __name__ == "__main__":
    # Test
    tracker = MetricsTracker("BTC", "1h")
    
    # Dummy predictions
    for i in range(20):
        tracker.record_prediction(
            timestamp=datetime.now(),
            point_forecast=100 + i * 0.5,
            interval_80=[99, 101],
            interval_95=[98, 102],
            direction_prob_up=0.6,
            confidence=0.7,
            current_price=100,
            features_used=["rsi", "macd", "volume"]
        )
        
        # Simulate actual
        tracker.update_actual(datetime.now(), 100 + i * 0.4 + np.random.randn() * 0.2)
    
    metrics = tracker.compute_metrics()
    print(f"\nMetrics: {json.dumps(metrics, indent=2)}")
    
    analysis = tracker.analyze_errors()
    print(f"\nAnalysis: {json.dumps(analysis, indent=2)}")
