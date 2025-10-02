import asyncio
import httpx
import pandas as pd
import numpy as np
from typing import List, Dict, Optional
from datetime import datetime, timedelta
from loguru import logger
from config import settings
import time

class CryptoDataCollector:
    """Ücretsiz API'lerden kripto para verisi toplama"""
    
    def __init__(self):
        self.coingecko_base = settings.COINGECKO_API_URL
        self.binance_base = settings.BINANCE_API_URL
        self.last_coingecko_call = 0
        self.last_binance_call = 0
        
    async def _rate_limit_coingecko(self):
        """CoinGecko free tier: ~10-50 req/min"""
        elapsed = time.time() - self.last_coingecko_call
        min_interval = 60.0 / settings.COINGECKO_RATE_LIMIT
        if elapsed < min_interval:
            await asyncio.sleep(min_interval - elapsed)
        self.last_coingecko_call = time.time()
    
    async def _rate_limit_binance(self):
        """Binance public API: 1200 req/min"""
        elapsed = time.time() - self.last_binance_call
        min_interval = 60.0 / settings.BINANCE_RATE_LIMIT
        if elapsed < min_interval:
            await asyncio.sleep(min_interval - elapsed)
        self.last_binance_call = time.time()
    
    async def get_top_100_assets(self) -> List[Dict]:
        """Top 100 kripto para listesini CoinGecko'dan al"""
        await self._rate_limit_coingecko()
        
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.get(
                    f"{self.coingecko_base}/coins/markets",
                    params={
                        "vs_currency": "usd",
                        "order": "market_cap_desc",
                        "per_page": settings.TOP_N_ASSETS,
                        "page": 1,
                        "sparkline": False
                    }
                )
                response.raise_for_status()
                data = response.json()
                
                assets = []
                for coin in data:
                    assets.append({
                        "symbol": coin["symbol"].upper(),
                        "name": coin["name"],
                        "market_cap_rank": coin["market_cap_rank"],
                        "current_price": coin["current_price"],
                        "market_cap": coin["market_cap"],
                        "total_volume": coin["total_volume"],
                        "price_change_24h": coin.get("price_change_percentage_24h", 0)
                    })
                
                logger.info(f"Fetched {len(assets)} top assets from CoinGecko")
                return assets
                
        except Exception as e:
            logger.error(f"Error fetching top 100 assets: {e}")
            return []
    
    async def get_ohlcv_binance(
        self, 
        symbol: str, 
        interval: str = "1m", 
        limit: int = 500
    ) -> Optional[pd.DataFrame]:
        """Binance'den OHLCV verisi al (ücretsiz)
        
        Args:
            symbol: Trading pair (e.g., "BTCUSDT")
            interval: Kline interval (1m, 5m, 15m, 1h, 4h, 1d)
            limit: Number of candles (max 1000)
        """
        await self._rate_limit_binance()
        
        # Symbol formatını Binance'e uyarla
        binance_symbol = f"{symbol}USDT" if not symbol.endswith("USDT") else symbol
        
        try:
            async with httpx.AsyncClient(timeout=30.0) as client:
                response = await client.get(
                    f"{self.binance_base}/klines",
                    params={
                        "symbol": binance_symbol,
                        "interval": interval,
                        "limit": limit
                    }
                )
                
                if response.status_code == 404:
                    logger.warning(f"Symbol {binance_symbol} not found on Binance")
                    return None
                    
                response.raise_for_status()
                klines = response.json()
                
                if not klines:
                    return None
                
                # OHLCV DataFrame oluştur
                df = pd.DataFrame(klines, columns=[
                    "timestamp", "open", "high", "low", "close", "volume",
                    "close_time", "quote_volume", "trades", 
                    "taker_buy_base", "taker_buy_quote", "ignore"
                ])
                
                # Veri tiplerini düzenle
                df["timestamp"] = pd.to_datetime(df["timestamp"], unit="ms")
                for col in ["open", "high", "low", "close", "volume"]:
                    df[col] = pd.to_numeric(df[col], errors="coerce")
                
                # Sadece gerekli kolonları tut
                df = df[["timestamp", "open", "high", "low", "close", "volume"]]
                df = df.set_index("timestamp")
                
                logger.debug(f"Fetched {len(df)} candles for {binance_symbol} ({interval})")
                return df
                
        except Exception as e:
            logger.error(f"Error fetching OHLCV for {symbol}: {e}")
            return None
    
    async def get_orderbook_snapshot(self, symbol: str, depth: int = 20) -> Optional[Dict]:
        """Binance'den emir defteri özeti al"""
        await self._rate_limit_binance()
        
        binance_symbol = f"{symbol}USDT" if not symbol.endswith("USDT") else symbol
        
        try:
            async with httpx.AsyncClient(timeout=15.0) as client:
                response = await client.get(
                    f"{self.binance_base}/depth",
                    params={"symbol": binance_symbol, "limit": depth}
                )
                
                if response.status_code == 404:
                    return None
                    
                response.raise_for_status()
                data = response.json()
                
                # Bid/Ask spread ve imbalance hesapla
                bids = np.array([[float(p), float(q)] for p, q in data["bids"][:depth]])
                asks = np.array([[float(p), float(q)] for p, q in data["asks"][:depth]])
                
                if len(bids) == 0 or len(asks) == 0:
                    return None
                
                bid_volume = np.sum(bids[:, 1])
                ask_volume = np.sum(asks[:, 1])
                
                return {
                    "best_bid": bids[0, 0],
                    "best_ask": asks[0, 0],
                    "spread": asks[0, 0] - bids[0, 0],
                    "spread_pct": (asks[0, 0] - bids[0, 0]) / bids[0, 0] * 100,
                    "bid_volume": bid_volume,
                    "ask_volume": ask_volume,
                    "order_imbalance": (bid_volume - ask_volume) / (bid_volume + ask_volume)
                }
                
        except Exception as e:
            logger.error(f"Error fetching orderbook for {symbol}: {e}")
            return None
    
    async def get_market_data_batch(
        self, 
        symbols: List[str], 
        interval: str = "1h",
        limit: int = 200
    ) -> Dict[str, pd.DataFrame]:
        """Birden fazla coin için paralel veri toplama"""
        
        tasks = []
        for symbol in symbols:
            task = self.get_ohlcv_binance(symbol, interval, limit)
            tasks.append(task)
        
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        data_dict = {}
        for symbol, result in zip(symbols, results):
            if isinstance(result, pd.DataFrame):
                data_dict[symbol] = result
            else:
                logger.warning(f"Failed to fetch data for {symbol}")
        
        return data_dict


# Test fonksiyonu
async def test_collector():
    collector = CryptoDataCollector()
    
    # Top 100 listesi
    top100 = await collector.get_top_100_assets()
    print(f"\nTop 5 Assets:")
    for asset in top100[:5]:
        print(f"  {asset['symbol']}: ${asset['current_price']:,.2f}")
    
    # BTC OHLCV
    btc_data = await collector.get_ohlcv_binance("BTC", interval="1h", limit=100)
    if btc_data is not None:
        print(f"\nBTC OHLCV (last 5 hours):")
        print(btc_data.tail())
    
    # BTC orderbook
    orderbook = await collector.get_orderbook_snapshot("BTC")
    if orderbook:
        print(f"\nBTC Orderbook:")
        print(f"  Spread: {orderbook['spread_pct']:.4f}%")
        print(f"  Order Imbalance: {orderbook['order_imbalance']:.4f}")


if __name__ == "__main__":
    asyncio.run(test_collector())
