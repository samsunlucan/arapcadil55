import pandas as pd
import numpy as np
from typing import Dict, List, Optional
import ta
from ta.trend import MACD, EMAIndicator, SMAIndicator
from ta.momentum import RSIIndicator, StochasticOscillator
from ta.volatility import BollingerBands, AverageTrueRange
from ta.volume import OnBalanceVolumeIndicator, VolumeWeightedAveragePrice
from loguru import logger
from config import settings


class FeatureEngineer:
    """Teknik gösterge ve özellik mühendisliği"""
    
    def __init__(self):
        self.rsi_period = settings.RSI_PERIOD
        self.macd_fast = settings.MACD_FAST
        self.macd_slow = settings.MACD_SLOW
        self.macd_signal = settings.MACD_SIGNAL
        self.ma_periods = settings.MA_PERIODS
    
    def add_price_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Temel fiyat özellikleri"""
        if df is None or len(df) < 2:
            return df
        
        df = df.copy()
        
        # Fiyat değişimleri
        df["price_change"] = df["close"].pct_change()
        df["price_change_abs"] = df["close"].diff()
        
        # Log returns
        df["log_return"] = np.log(df["close"] / df["close"].shift(1))
        
        # High-Low range
        df["hl_range"] = (df["high"] - df["low"]) / df["close"]
        
        # Open-Close momentum
        df["oc_momentum"] = (df["close"] - df["open"]) / df["open"]
        
        return df
    
    def add_momentum_indicators(self, df: pd.DataFrame) -> pd.DataFrame:
        """Momentum göstergeleri: RSI, MACD, Stochastic"""
        if df is None or len(df) < max(self.rsi_period, self.macd_slow) + 10:
            logger.warning("Insufficient data for momentum indicators")
            return df
        
        df = df.copy()
        
        try:
            # RSI
            rsi = RSIIndicator(close=df["close"], window=self.rsi_period)
            df["rsi"] = rsi.rsi()
            df["rsi_oversold"] = (df["rsi"] < 30).astype(int)
            df["rsi_overbought"] = (df["rsi"] > 70).astype(int)
            
            # MACD
            macd = MACD(
                close=df["close"],
                window_fast=self.macd_fast,
                window_slow=self.macd_slow,
                window_sign=self.macd_signal
            )
            df["macd"] = macd.macd()
            df["macd_signal"] = macd.macd_signal()
            df["macd_diff"] = macd.macd_diff()
            df["macd_cross"] = (df["macd"] > df["macd_signal"]).astype(int)
            
            # Stochastic %K
            stoch = StochasticOscillator(
                high=df["high"],
                low=df["low"],
                close=df["close"],
                window=14,
                smooth_window=3
            )
            df["stoch_k"] = stoch.stoch()
            df["stoch_d"] = stoch.stoch_signal()
            
        except Exception as e:
            logger.error(f"Error computing momentum indicators: {e}")
        
        return df
    
    def add_trend_indicators(self, df: pd.DataFrame) -> pd.DataFrame:
        """Trend göstergeleri: MA'lar, Bollinger Bands"""
        if df is None or len(df) < max(self.ma_periods) + 10:
            logger.warning("Insufficient data for trend indicators")
            return df
        
        df = df.copy()
        
        try:
            # Hareketli ortalamalar
            for period in self.ma_periods:
                if len(df) >= period:
                    # Simple Moving Average
                    sma = SMAIndicator(close=df["close"], window=period)
                    df[f"sma_{period}"] = sma.sma_indicator()
                    
                    # Exponential Moving Average
                    ema = EMAIndicator(close=df["close"], window=period)
                    df[f"ema_{period}"] = ema.ema_indicator()
                    
                    # Price vs MA
                    df[f"price_vs_sma_{period}"] = (df["close"] - df[f"sma_{period}"]) / df[f"sma_{period}"]
            
            # Bollinger Bands
            if len(df) >= 20:
                bb = BollingerBands(close=df["close"], window=20, window_dev=2)
                df["bb_high"] = bb.bollinger_hband()
                df["bb_mid"] = bb.bollinger_mavg()
                df["bb_low"] = bb.bollinger_lband()
                df["bb_width"] = bb.bollinger_wband()
                df["bb_pct"] = bb.bollinger_pband()
                
        except Exception as e:
            logger.error(f"Error computing trend indicators: {e}")
        
        return df
    
    def add_volatility_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Volatilite özellikleri: ATR, realized volatility"""
        if df is None or len(df) < 20:
            return df
        
        df = df.copy()
        
        try:
            # Average True Range
            atr = AverageTrueRange(
                high=df["high"],
                low=df["low"],
                close=df["close"],
                window=14
            )
            df["atr"] = atr.average_true_range()
            df["atr_pct"] = df["atr"] / df["close"]
            
            # Realized volatility (rolling std of returns)
            for window in [7, 14, 30]:
                if len(df) >= window:
                    df[f"volatility_{window}d"] = df["log_return"].rolling(window).std() * np.sqrt(window)
            
            # Parkinson volatility (uses high-low range)
            df["parkinson_vol"] = np.sqrt(
                (np.log(df["high"] / df["low"]) ** 2) / (4 * np.log(2))
            )
            
        except Exception as e:
            logger.error(f"Error computing volatility features: {e}")
        
        return df
    
    def add_volume_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Hacim özellikleri"""
        if df is None or len(df) < 20:
            return df
        
        df = df.copy()
        
        try:
            # Volume change
            df["volume_change"] = df["volume"].pct_change()
            
            # Volume moving averages
            df["volume_ma_7"] = df["volume"].rolling(7).mean()
            df["volume_ma_20"] = df["volume"].rolling(20).mean()
            df["volume_ratio"] = df["volume"] / df["volume_ma_20"]
            
            # On-Balance Volume
            obv = OnBalanceVolumeIndicator(close=df["close"], volume=df["volume"])
            df["obv"] = obv.on_balance_volume()
            
            # VWAP (Volume Weighted Average Price)
            if len(df) >= 14:
                vwap = VolumeWeightedAveragePrice(
                    high=df["high"],
                    low=df["low"],
                    close=df["close"],
                    volume=df["volume"],
                    window=14
                )
                df["vwap"] = vwap.volume_weighted_average_price()
                df["price_vs_vwap"] = (df["close"] - df["vwap"]) / df["vwap"]
            
        except Exception as e:
            logger.error(f"Error computing volume features: {e}")
        
        return df
    
    def add_time_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Zaman özellikleri (döngüsellik)"""
        if df is None or len(df) == 0:
            return df
        
        df = df.copy()
        
        try:
            # Saat, gün, hafta bilgileri
            df["hour"] = df.index.hour
            df["day_of_week"] = df.index.dayofweek
            df["day_of_month"] = df.index.day
            df["month"] = df.index.month
            
            # Döngüsel encoding (sin/cos)
            df["hour_sin"] = np.sin(2 * np.pi * df["hour"] / 24)
            df["hour_cos"] = np.cos(2 * np.pi * df["hour"] / 24)
            df["day_sin"] = np.sin(2 * np.pi * df["day_of_week"] / 7)
            df["day_cos"] = np.cos(2 * np.pi * df["day_of_week"] / 7)
            
            # İşlem saatleri (UTC bazlı)
            df["is_us_hours"] = ((df["hour"] >= 13) & (df["hour"] <= 21)).astype(int)
            df["is_asia_hours"] = ((df["hour"] >= 0) & (df["hour"] <= 8)).astype(int)
            
        except Exception as e:
            logger.error(f"Error computing time features: {e}")
        
        return df
    
    def add_orderbook_features(self, df: pd.DataFrame, orderbook_data: List[Dict]) -> pd.DataFrame:
        """Emir defteri özelliklerini ekle"""
        if df is None or not orderbook_data:
            return df
        
        df = df.copy()
        
        try:
            # Orderbook verilerini DataFrame'e dönüştür
            ob_df = pd.DataFrame(orderbook_data)
            if "timestamp" in ob_df.columns:
                ob_df["timestamp"] = pd.to_datetime(ob_df["timestamp"])
                ob_df = ob_df.set_index("timestamp")
                
                # df ile merge
                df = df.merge(
                    ob_df[["spread_pct", "order_imbalance"]], 
                    left_index=True, 
                    right_index=True, 
                    how="left"
                )
        
        except Exception as e:
            logger.error(f"Error adding orderbook features: {e}")
        
        return df
    
    def engineer_all_features(
        self, 
        df: pd.DataFrame,
        orderbook_data: Optional[List[Dict]] = None
    ) -> pd.DataFrame:
        """Tüm özellikleri hesapla"""
        
        if df is None or len(df) < settings.MIN_DATA_POINTS:
            logger.warning(f"Insufficient data points: {len(df) if df is not None else 0}")
            return None
        
        logger.info(f"Engineering features for {len(df)} data points")
        
        # Özellikleri sırayla ekle
        df = self.add_price_features(df)
        df = self.add_momentum_indicators(df)
        df = self.add_trend_indicators(df)
        df = self.add_volatility_features(df)
        df = self.add_volume_features(df)
        df = self.add_time_features(df)
        
        if orderbook_data:
            df = self.add_orderbook_features(df, orderbook_data)
        
        # NaN değerleri temizle (ilk satırlar gösterge hesabı için eksik olabilir)
        initial_len = len(df)
        df = df.dropna()
        
        if len(df) < initial_len * 0.7:
            logger.warning(f"Lost {initial_len - len(df)} rows due to NaN values")
        
        logger.info(f"Feature engineering complete: {len(df)} rows, {len(df.columns)} features")
        
        return df
    
    def get_feature_importance_names(self, top_n: int = 5) -> List[str]:
        """Model için en önemli özelliklerin isimlerini döndür"""
        # Bu liste model eğitimi sonrası güncellenebilir
        important_features = [
            "rsi", "macd_diff", "volume_ratio", "bb_pct", 
            "price_vs_sma_20", "atr_pct", "stoch_k",
            "volatility_14d", "obv", "order_imbalance"
        ]
        return important_features[:top_n]


# Test
if __name__ == "__main__":
    # Dummy data oluştur
    dates = pd.date_range(start="2025-01-01", end="2025-03-01", freq="1h")
    np.random.seed(42)
    
    df = pd.DataFrame({
        "open": np.random.randn(len(dates)).cumsum() + 100,
        "high": np.random.randn(len(dates)).cumsum() + 102,
        "low": np.random.randn(len(dates)).cumsum() + 98,
        "close": np.random.randn(len(dates)).cumsum() + 100,
        "volume": np.random.randint(1000, 10000, len(dates))
    }, index=dates)
    
    df["high"] = df[["open", "close"]].max(axis=1) + abs(np.random.randn(len(dates)))
    df["low"] = df[["open", "close"]].min(axis=1) - abs(np.random.randn(len(dates)))
    
    engineer = FeatureEngineer()
    df_features = engineer.engineer_all_features(df)
    
    print(f"\nFeatures created: {len(df_features.columns)}")
    print(f"Sample features:\n{df_features.columns.tolist()[:20]}")
    print(f"\nLast 3 rows:")
    print(df_features.tail(3))
