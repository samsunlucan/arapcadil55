from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from typing import List, Dict, Optional, Literal
from datetime import datetime
import asyncio
from loguru import logger
import sys

from config import settings
from data_collector import CryptoDataCollector
from feature_engineering import FeatureEngineer
from forecaster import EnsembleForecaster
from metrics_tracker import MetricsTracker, PerformanceReporter

# Logging setup
logger.remove()
logger.add(sys.stderr, level="INFO")
logger.add("logs/app_{time}.log", rotation="1 day", retention="7 days")

# FastAPI app
app = FastAPI(
    title="Kripto Para Olasılıksal Tahmin API",
    description="Top 100 kripto para için çoklu zaman dilimi tahminleri. ⚠️ Finansal tavsiye DEĞİLDİR.",
    version="1.0.0"
)

# CORS (React Native için)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Production'da kısıtlanmalı
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Global instances
data_collector = CryptoDataCollector()
feature_engineer = FeatureEngineer()

# Cache (basit in-memory, production'da Redis)
forecast_cache: Dict = {}
top100_cache: Optional[List[Dict]] = None
cache_timestamp: Dict = {}


# Response Models
class ForecastResponse(BaseModel):
    symbol: str
    horizon: Literal["15m", "1h", "4h", "1d"]
    timestamp: str
    point_forecast: float
    interval_80: List[float]
    interval_95: List[float]
    direction_prob_up: float
    confidence: float
    rationale: str
    features_used: List[str]
    learning_action: Literal["no_update", "update_weights", "retrain_needed"]


class AssetInfo(BaseModel):
    symbol: str
    name: str
    market_cap_rank: int
    current_price: float
    market_cap: float
    total_volume: float
    price_change_24h: float


class MetricsResponse(BaseModel):
    symbol: str
    horizon: str
    n_predictions: int
    mae: Optional[float] = None
    rmse: Optional[float] = None
    mape: Optional[float] = None
    direction_accuracy: Optional[float] = None
    brier_score: Optional[float] = None
    coverage_80: Optional[float] = None
    coverage_95: Optional[float] = None


# Endpoints

@app.get("/")
async def root():
    return {
        "message": "Kripto Para Tahmin API",
        "warning": "⚠️ Bu sistem yalnızca eğitim amaçlıdır. Finansal tavsiye DEĞİLDİR.",
        "endpoints": {
            "assets": "/assets/top100",
            "forecast": "/forecast?symbol=BTC&horizon=1h",
            "metrics": "/metrics?symbol=BTC&horizon=1h",
            "health": "/health"
        }
    }


@app.get("/health")
async def health_check():
    return {
        "status": "healthy",
        "timestamp": datetime.now().isoformat(),
        "version": "1.0.0"
    }


@app.get("/assets/top100", response_model=List[AssetInfo])
async def get_top_100_assets():
    """Top 100 kripto para listesini döndür"""
    
    global top100_cache, cache_timestamp
    
    # Cache kontrolü (1 saat)
    cache_key = "top100"
    if cache_key in cache_timestamp:
        age = (datetime.now() - cache_timestamp[cache_key]).seconds
        if age < 3600 and top100_cache:
            logger.info("Returning cached top100 list")
            return top100_cache
    
    try:
        assets = await data_collector.get_top_100_assets()
        
        if not assets:
            raise HTTPException(status_code=503, detail="Failed to fetch asset list")
        
        top100_cache = assets
        cache_timestamp[cache_key] = datetime.now()
        
        return assets
    
    except Exception as e:
        logger.error(f"Error in get_top_100_assets: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/forecast", response_model=ForecastResponse)
async def get_forecast(
    symbol: str = Query(..., description="Kripto sembolü (örn: BTC, ETH)"),
    horizon: Literal["15m", "1h", "4h", "1d"] = Query("1h", description="Tahmin ufku")
):
    """Belirli bir kripto için tahmin üret"""
    
    symbol = symbol.upper()
    cache_key = f"{symbol}_{horizon}"
    
    # Cache kontrolü (5 dakika)
    if cache_key in cache_timestamp:
        age = (datetime.now() - cache_timestamp[cache_key]).seconds
        if age < settings.FORECAST_CACHE_TTL and cache_key in forecast_cache:
            logger.info(f"Returning cached forecast for {cache_key}")
            return forecast_cache[cache_key]
    
    try:
        # 1. Veri toplama
        horizon_to_interval = {
            "15m": "15m",
            "1h": "1h",
            "4h": "4h",
            "1d": "1d"
        }
        interval = horizon_to_interval[horizon]
        
        logger.info(f"Fetching data for {symbol} - {interval}")
        df = await data_collector.get_ohlcv_binance(symbol, interval=interval, limit=500)
        
        if df is None or len(df) < settings.MIN_DATA_POINTS:
            raise HTTPException(
                status_code=404,
                detail=f"Insufficient data for {symbol}. Coin may not be available on Binance."
            )
        
        # 2. Feature engineering
        logger.info(f"Engineering features for {symbol}")
        df_features = feature_engineer.engineer_all_features(df)
        
        if df_features is None or len(df_features) < 50:
            raise HTTPException(
                status_code=422,
                detail="Feature engineering failed - data quality issues"
            )
        
        # 3. Tahmin modeli
        forecaster = EnsembleForecaster(symbol=symbol, horizon=horizon)
        
        # Model varsa yükle, yoksa eğit
        model_exists = forecaster.load_models()
        if not model_exists:
            logger.info(f"Training new model for {symbol} - {horizon}")
            success = forecaster.train(df_features)
            if not success:
                logger.warning("Training failed, using fallback prediction")
        
        # 4. Tahmin
        prediction = forecaster.predict(df_features, return_components=False)
        
        # 5. Metrics ve learning action
        tracker = MetricsTracker(symbol, horizon)
        tracker.load_history()
        analysis = tracker.analyze_errors()
        
        # 6. Rationale oluştur
        current_price = prediction["current_price"]
        forecast_price = prediction["point_forecast"]
        change_pct = (forecast_price - current_price) / current_price * 100
        
        direction = "Yükseliş" if change_pct > 0 else "Düşüş"
        volatility_desc = "Yüksek volatilite" if df_features["atr_pct"].iloc[-1] > 0.02 else "Normal volatilite"
        
        rationale = (
            f"{direction} beklentisi (%{abs(change_pct):.2f}). {volatility_desc}. "
            f"⚠️ Risk: Kripto piyasalar son derece volatildir. Yatırım tavsiyesi değildir."
        )
        
        # 7. Response
        response = ForecastResponse(
            symbol=symbol,
            horizon=horizon,
            timestamp=datetime.now().isoformat(),
            point_forecast=prediction["point_forecast"],
            interval_80=prediction["interval_80"],
            interval_95=prediction["interval_95"],
            direction_prob_up=prediction["direction_prob_up"],
            confidence=prediction["confidence"],
            rationale=rationale,
            features_used=feature_engineer.get_feature_importance_names(top_n=5),
            learning_action=analysis.get("learning_action", "no_update")
        )
        
        # 8. Cache
        forecast_cache[cache_key] = response
        cache_timestamp[cache_key] = datetime.now()
        
        # 9. Tahmini kaydet
        if settings.STORE_PREDICTIONS:
            tracker.record_prediction(
                timestamp=datetime.now(),
                point_forecast=response.point_forecast,
                interval_80=response.interval_80,
                interval_95=response.interval_95,
                direction_prob_up=response.direction_prob_up,
                confidence=response.confidence,
                current_price=current_price,
                features_used=response.features_used
            )
        
        return response
    
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error generating forecast for {symbol}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/metrics", response_model=MetricsResponse)
async def get_metrics(
    symbol: str = Query(..., description="Kripto sembolü"),
    horizon: Literal["15m", "1h", "4h", "1d"] = Query("1h", description="Tahmin ufku")
):
    """Model performans metriklerini döndür"""
    
    symbol = symbol.upper()
    
    try:
        tracker = MetricsTracker(symbol, horizon)
        tracker.load_history()
        
        metrics = tracker.compute_metrics()
        
        return MetricsResponse(
            symbol=symbol,
            horizon=horizon,
            n_predictions=metrics.get("n_predictions", 0),
            mae=metrics.get("mae"),
            rmse=metrics.get("rmse"),
            mape=metrics.get("mape"),
            direction_accuracy=metrics.get("direction_accuracy"),
            brier_score=metrics.get("brier_score"),
            coverage_80=metrics.get("coverage_80"),
            coverage_95=metrics.get("coverage_95")
        )
    
    except Exception as e:
        logger.error(f"Error fetching metrics for {symbol}: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/report")
async def get_performance_report(
    symbols: str = Query("BTC,ETH,SOL", description="Virgülle ayrılmış semboller")
):
    """Çoklu varlık için performans raporu"""
    
    symbol_list = [s.strip().upper() for s in symbols.split(",")]
    horizons = settings.HORIZONS
    
    try:
        report = PerformanceReporter.generate_report(symbol_list, horizons)
        return report
    
    except Exception as e:
        logger.error(f"Error generating report: {e}")
        raise HTTPException(status_code=500, detail=str(e))


# Startup & Shutdown

@app.on_event("startup")
async def startup_event():
    logger.info("🚀 Kripto Para Tahmin API başlatılıyor...")
    logger.info(f"Sunucu: {settings.API_HOST}:{settings.API_PORT}")
    logger.info(f"Top N varlık: {settings.TOP_N_ASSETS}")
    logger.info(f"Tahmin ufukları: {settings.HORIZONS}")
    
    # İlk top100 listesini önbelleğe al
    try:
        global top100_cache
        top100_cache = await data_collector.get_top_100_assets()
        logger.info(f"✅ Top 100 listesi yüklendi: {len(top100_cache)} varlık")
    except Exception as e:
        logger.error(f"❌ Top 100 listesi yüklenemedi: {e}")


@app.on_event("shutdown")
async def shutdown_event():
    logger.info("🛑 API kapatılıyor...")


# Run server
if __name__ == "__main__":
    import uvicorn
    
    uvicorn.run(
        "main:app",
        host=settings.API_HOST,
        port=settings.API_PORT,
        reload=True,
        log_level="info"
    )
