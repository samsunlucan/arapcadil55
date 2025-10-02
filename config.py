from pydantic_settings import BaseSettings
from typing import Literal

class Settings(BaseSettings):
    # API Configuration
    API_HOST: str = "0.0.0.0"
    API_PORT: int = 8000
    
    # Data Sources (Free Tier)
    COINGECKO_API_URL: str = "https://api.coingecko.com/api/v3"
    BINANCE_API_URL: str = "https://api.binance.com/api/v3"
    
    # Rate Limiting (respecting free tier limits)
    COINGECKO_RATE_LIMIT: int = 10  # requests per minute
    BINANCE_RATE_LIMIT: int = 1200  # requests per minute
    
    # Cache Configuration
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    CACHE_TTL_SECONDS: int = 60  # 1 minute for price data
    FORECAST_CACHE_TTL: int = 300  # 5 minutes for forecasts
    
    # Model Configuration
    TOP_N_ASSETS: int = 100
    HORIZONS: list[str] = ["15m", "1h", "4h", "1d"]
    
    # Feature Engineering
    RSI_PERIOD: int = 14
    MACD_FAST: int = 12
    MACD_SLOW: int = 26
    MACD_SIGNAL: int = 9
    MA_PERIODS: list[int] = [7, 20, 50, 200]
    
    # Model Training
    TRAIN_LOOKBACK_DAYS: int = 90
    MIN_DATA_POINTS: int = 100
    LEARNING_RATE: float = 0.01
    FORGETTING_FACTOR: float = 0.95
    
    # Ensemble Weights (initial)
    LGBM_WEIGHT: float = 0.5
    LSTM_WEIGHT: float = 0.3
    NAIVE_WEIGHT: float = 0.2
    
    # Confidence & Intervals
    CONFIDENCE_LEVELS: list[float] = [0.80, 0.95]
    MIN_CONFIDENCE_THRESHOLD: float = 0.3
    
    # Performance Tracking
    STORE_PREDICTIONS: bool = True
    METRIC_WINDOW_SIZE: int = 100  # last N predictions for metrics
    
    class Config:
        env_file = ".env"
        case_sensitive = True

settings = Settings()
