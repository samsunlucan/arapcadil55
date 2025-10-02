import numpy as np
import pandas as pd
from typing import Dict, List, Tuple, Optional, Literal
from datetime import datetime, timedelta
import pickle
import json
from pathlib import Path
from loguru import logger

# ML imports
from sklearn.preprocessing import StandardScaler
from sklearn.isotonic import IsotonicRegression
from sklearn.calibration import CalibratedClassifierCV
import lightgbm as lgb

# Deep learning
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers

from config import settings


HorizonType = Literal["15m", "1h", "4h", "1d"]


class EnsembleForecaster:
    """Ensemble tahmin motoru: LightGBM + LSTM + Kalibrasyon"""
    
    def __init__(self, symbol: str, horizon: HorizonType):
        self.symbol = symbol
        self.horizon = horizon
        self.scaler = StandardScaler()
        
        # Model ağırlıkları (dinamik olarak güncellenecek)
        self.weights = {
            "lgbm": settings.LGBM_WEIGHT,
            "lstm": settings.LSTM_WEIGHT,
            "naive": settings.NAIVE_WEIGHT
        }
        
        # Modeller
        self.lgbm_model: Optional[lgb.Booster] = None
        self.lstm_model: Optional[keras.Model] = None
        self.calibrator_up: Optional[IsotonicRegression] = None
        self.calibrator_down: Optional[IsotonicRegression] = None
        
        # Tahmin geçmişi (öğrenme için)
        self.prediction_history: List[Dict] = []
        self.max_history = settings.METRIC_WINDOW_SIZE
        
        # Model dosya yolları
        self.model_dir = Path(f"models/{symbol}/{horizon}")
        self.model_dir.mkdir(parents=True, exist_ok=True)
        
    def prepare_sequences(
        self, 
        df: pd.DataFrame, 
        lookback: int = 24,
        forecast_steps: int = 1
    ) -> Tuple[np.ndarray, np.ndarray, List[str]]:
        """LSTM için sequence data hazırlama"""
        
        # Feature seçimi (numerik kolonlar)
        feature_cols = [
            col for col in df.columns 
            if col not in ["target", "target_direction"] and df[col].dtype in [np.float64, np.int64]
        ]
        
        X_data = df[feature_cols].values
        y_data = df["target"].values if "target" in df.columns else None
        
        # Normalizasyon
        X_scaled = self.scaler.fit_transform(X_data)
        
        # Sequence oluştur
        X_sequences = []
        y_values = []
        
        for i in range(lookback, len(X_scaled)):
            X_sequences.append(X_scaled[i-lookback:i])
            if y_data is not None:
                y_values.append(y_data[i])
        
        X_sequences = np.array(X_sequences)
        y_values = np.array(y_values) if y_values else None
        
        return X_sequences, y_values, feature_cols
    
    def build_lstm_model(self, input_shape: Tuple[int, int]) -> keras.Model:
        """LSTM model mimarisi"""
        
        model = keras.Sequential([
            layers.LSTM(64, return_sequences=True, input_shape=input_shape),
            layers.Dropout(0.2),
            layers.LSTM(32, return_sequences=False),
            layers.Dropout(0.2),
            layers.Dense(16, activation="relu"),
            layers.Dense(1)  # Regresyon: fiyat tahmini
        ])
        
        model.compile(
            optimizer=keras.optimizers.Adam(learning_rate=settings.LEARNING_RATE),
            loss="mse",
            metrics=["mae"]
        )
        
        return model
    
    def train(self, df: pd.DataFrame, target_col: str = "target"):
        """Ensemble modeli eğit"""
        
        if len(df) < settings.MIN_DATA_POINTS:
            logger.warning(f"Insufficient data for training: {len(df)} < {settings.MIN_DATA_POINTS}")
            return False
        
        logger.info(f"Training ensemble for {self.symbol} - {self.horizon}")
        
        # Target oluştur (horizon kadar ileri fiyat)
        horizon_map = {"15m": 1, "1h": 1, "4h": 1, "1d": 1}
        shift_periods = horizon_map.get(self.horizon, 1)
        
        df = df.copy()
        df["target"] = df["close"].shift(-shift_periods)
        df["target_direction"] = (df["target"] > df["close"]).astype(int)
        df = df.dropna()
        
        if len(df) < 50:
            logger.warning("Insufficient data after target creation")
            return False
        
        # Train/validation split (80/20)
        split_idx = int(len(df) * 0.8)
        train_df = df.iloc[:split_idx]
        val_df = df.iloc[split_idx:]
        
        # Feature selection
        feature_cols = [
            col for col in df.columns 
            if col not in ["target", "target_direction"] 
            and df[col].dtype in [np.float64, np.int64, np.float32, np.int32]
        ]
        
        X_train = train_df[feature_cols].values
        y_train = train_df["target"].values
        y_train_dir = train_df["target_direction"].values
        
        X_val = val_df[feature_cols].values
        y_val = val_df["target"].values
        
        # 1. LightGBM eğitimi
        try:
            lgb_train = lgb.Dataset(X_train, y_train)
            lgb_val = lgb.Dataset(X_val, y_val, reference=lgb_train)
            
            params = {
                "objective": "regression",
                "metric": "rmse",
                "boosting_type": "gbdt",
                "num_leaves": 31,
                "learning_rate": 0.05,
                "feature_fraction": 0.8,
                "verbose": -1
            }
            
            self.lgbm_model = lgb.train(
                params,
                lgb_train,
                num_boost_round=100,
                valid_sets=[lgb_val],
                callbacks=[lgb.early_stopping(stopping_rounds=10, verbose=False)]
            )
            
            logger.info("LightGBM training complete")
            
        except Exception as e:
            logger.error(f"LightGBM training failed: {e}")
        
        # 2. LSTM eğitimi
        try:
            X_seq_train, y_seq_train, _ = self.prepare_sequences(train_df, lookback=24)
            X_seq_val, y_seq_val, _ = self.prepare_sequences(val_df, lookback=24)
            
            if len(X_seq_train) > 0:
                self.lstm_model = self.build_lstm_model(
                    input_shape=(X_seq_train.shape[1], X_seq_train.shape[2])
                )
                
                self.lstm_model.fit(
                    X_seq_train, y_seq_train,
                    validation_data=(X_seq_val, y_seq_val),
                    epochs=30,
                    batch_size=32,
                    verbose=0,
                    callbacks=[
                        keras.callbacks.EarlyStopping(patience=5, restore_best_weights=True)
                    ]
                )
                
                logger.info("LSTM training complete")
        
        except Exception as e:
            logger.error(f"LSTM training failed: {e}")
        
        # 3. Kalibrasyon (yön tahminleri için)
        try:
            # LightGBM ile yön tahminleri
            lgb_pred = self.lgbm_model.predict(X_train) if self.lgbm_model else np.zeros(len(X_train))
            direction_probs = 1 / (1 + np.exp(-(lgb_pred - y_train)))  # Sigmoid
            
            # Isotonic regression ile kalibrasyon
            self.calibrator_up = IsotonicRegression(out_of_bounds="clip")
            self.calibrator_up.fit(direction_probs, y_train_dir)
            
            logger.info("Calibration complete")
            
        except Exception as e:
            logger.error(f"Calibration failed: {e}")
        
        # Model kaydet
        self.save_models()
        
        return True
    
    def predict(
        self, 
        df: pd.DataFrame,
        return_components: bool = False
    ) -> Dict:
        """Tahmin üret (ensemble)"""
        
        if df is None or len(df) < 10:
            return self._fallback_prediction(df)
        
        # Feature preparation
        feature_cols = [
            col for col in df.columns 
            if df[col].dtype in [np.float64, np.int64, np.float32, np.int32]
        ]
        
        X = df[feature_cols].iloc[-1:].values
        current_price = df["close"].iloc[-1]
        
        predictions = {}
        
        # 1. Naive baseline (persistence)
        naive_pred = current_price
        predictions["naive"] = naive_pred
        
        # 2. LightGBM
        if self.lgbm_model:
            try:
                lgbm_pred = self.lgbm_model.predict(X)[0]
                predictions["lgbm"] = lgbm_pred
            except Exception as e:
                logger.warning(f"LightGBM prediction failed: {e}")
                predictions["lgbm"] = naive_pred
        else:
            predictions["lgbm"] = naive_pred
        
        # 3. LSTM
        if self.lstm_model and len(df) >= 24:
            try:
                X_seq, _, _ = self.prepare_sequences(df.iloc[-25:], lookback=24)
                if len(X_seq) > 0:
                    lstm_pred = self.lstm_model.predict(X_seq[-1:], verbose=0)[0][0]
                    # Denormalize
                    predictions["lstm"] = float(lstm_pred)
                else:
                    predictions["lstm"] = naive_pred
            except Exception as e:
                logger.warning(f"LSTM prediction failed: {e}")
                predictions["lstm"] = naive_pred
        else:
            predictions["lstm"] = naive_pred
        
        # 4. Ensemble (weighted average)
        point_forecast = (
            self.weights["naive"] * predictions["naive"] +
            self.weights["lgbm"] * predictions["lgbm"] +
            self.weights["lstm"] * predictions["lstm"]
        )
        
        # 5. Yön olasılığı (calibrated)
        direction_prob_up = 0.5  # default
        if self.calibrator_up and self.lgbm_model:
            try:
                raw_prob = 1 / (1 + np.exp(-(predictions["lgbm"] - current_price) / current_price))
                direction_prob_up = float(self.calibrator_up.predict([raw_prob])[0])
            except:
                direction_prob_up = 0.5 if point_forecast >= current_price else 0.3
        else:
            direction_prob_up = 0.6 if point_forecast > current_price else 0.4
        
        # 6. Güven aralıkları (historical volatility-based)
        volatility = df["log_return"].std() if "log_return" in df.columns else 0.02
        
        # Monte Carlo benzeri interval (simplified)
        z_80 = 1.28  # 80% confidence
        z_95 = 1.96  # 95% confidence
        
        interval_80_low = point_forecast * (1 - z_80 * volatility)
        interval_80_high = point_forecast * (1 + z_80 * volatility)
        interval_95_low = point_forecast * (1 - z_95 * volatility)
        interval_95_high = point_forecast * (1 + z_95 * volatility)
        
        # 7. Güven skoru (model agreement + data quality)
        model_agreement = 1 - np.std(list(predictions.values())) / np.mean(list(predictions.values()))
        data_quality = min(len(df) / settings.MIN_DATA_POINTS, 1.0)
        confidence = float(np.clip(0.3 + 0.4 * model_agreement + 0.3 * data_quality, 0, 1))
        
        result = {
            "point_forecast": float(point_forecast),
            "interval_80": [float(interval_80_low), float(interval_80_high)],
            "interval_95": [float(interval_95_low), float(interval_95_high)],
            "direction_prob_up": float(direction_prob_up),
            "confidence": float(confidence),
            "current_price": float(current_price)
        }
        
        if return_components:
            result["components"] = predictions
        
        return result
    
    def _fallback_prediction(self, df: Optional[pd.DataFrame]) -> Dict:
        """Veri yetersizliğinde fallback tahmini"""
        
        current_price = df["close"].iloc[-1] if df is not None and len(df) > 0 else 0
        
        return {
            "point_forecast": float(current_price),
            "interval_80": [float(current_price * 0.98), float(current_price * 1.02)],
            "interval_95": [float(current_price * 0.96), float(current_price * 1.04)],
            "direction_prob_up": 0.5,
            "confidence": 0.1,  # Çok düşük güven
            "current_price": float(current_price),
            "warning": "Insufficient data - using fallback"
        }
    
    def update_weights(self, error_metrics: Dict[str, float]):
        """Model ağırlıklarını hata metriklerine göre güncelle"""
        
        # Inverse error weighting
        total_inv_error = sum(1 / (e + 1e-6) for e in error_metrics.values())
        
        for model_name in self.weights.keys():
            if model_name in error_metrics:
                inv_error = 1 / (error_metrics[model_name] + 1e-6)
                new_weight = inv_error / total_inv_error
                
                # Exponential moving average ile güncelle
                alpha = 1 - settings.FORGETTING_FACTOR
                self.weights[model_name] = (
                    settings.FORGETTING_FACTOR * self.weights[model_name] + 
                    alpha * new_weight
                )
        
        # Normalize
        total = sum(self.weights.values())
        self.weights = {k: v/total for k, v in self.weights.items()}
        
        logger.info(f"Updated weights for {self.symbol}-{self.horizon}: {self.weights}")
    
    def save_models(self):
        """Modelleri diske kaydet"""
        try:
            if self.lgbm_model:
                self.lgbm_model.save_model(str(self.model_dir / "lgbm.txt"))
            
            if self.lstm_model:
                self.lstm_model.save(str(self.model_dir / "lstm.keras"))
            
            if self.calibrator_up:
                with open(self.model_dir / "calibrator.pkl", "wb") as f:
                    pickle.dump(self.calibrator_up, f)
            
            # Ağırlıkları kaydet
            with open(self.model_dir / "weights.json", "w") as f:
                json.dump(self.weights, f)
            
            logger.info(f"Models saved to {self.model_dir}")
            
        except Exception as e:
            logger.error(f"Error saving models: {e}")
    
    def load_models(self) -> bool:
        """Modelleri diskten yükle"""
        try:
            lgbm_path = self.model_dir / "lgbm.txt"
            if lgbm_path.exists():
                self.lgbm_model = lgb.Booster(model_file=str(lgbm_path))
            
            lstm_path = self.model_dir / "lstm.keras"
            if lstm_path.exists():
                self.lstm_model = keras.models.load_model(str(lstm_path))
            
            calibrator_path = self.model_dir / "calibrator.pkl"
            if calibrator_path.exists():
                with open(calibrator_path, "rb") as f:
                    self.calibrator_up = pickle.load(f)
            
            weights_path = self.model_dir / "weights.json"
            if weights_path.exists():
                with open(weights_path, "r") as f:
                    self.weights = json.load(f)
            
            logger.info(f"Models loaded from {self.model_dir}")
            return True
            
        except Exception as e:
            logger.error(f"Error loading models: {e}")
            return False


if __name__ == "__main__":
    # Test
    dates = pd.date_range(start="2025-01-01", periods=500, freq="1h")
    df = pd.DataFrame({
        "close": np.random.randn(len(dates)).cumsum() + 100,
        "open": np.random.randn(len(dates)).cumsum() + 100,
        "high": np.random.randn(len(dates)).cumsum() + 102,
        "low": np.random.randn(len(dates)).cumsum() + 98,
        "volume": np.random.randint(1000, 10000, len(dates)),
        "log_return": np.random.randn(len(dates)) * 0.01
    }, index=dates)
    
    forecaster = EnsembleForecaster(symbol="BTC", horizon="1h")
    forecaster.train(df)
    
    prediction = forecaster.predict(df)
    print(f"\nPrediction: {prediction}")
