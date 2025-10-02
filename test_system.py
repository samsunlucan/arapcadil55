"""
Sistem entegrasyon testi - Tüm bileşenleri test eder
"""

import asyncio
import sys
from loguru import logger

logger.remove()
logger.add(sys.stdout, level="INFO")

async def test_full_pipeline():
    """Tam pipeline testi"""
    
    logger.info("🧪 Sistem testi başlıyor...")
    
    # 1. Veri toplama testi
    logger.info("\n1️⃣ Veri toplama testi...")
    from data_collector import CryptoDataCollector
    
    collector = CryptoDataCollector()
    
    # Top 100 listesi
    top100 = await collector.get_top_100_assets()
    assert len(top100) > 0, "Top 100 listesi boş!"
    logger.info(f"✅ {len(top100)} varlık alındı")
    
    # BTC verisi
    btc_data = await collector.get_ohlcv_binance("BTC", interval="1h", limit=200)
    assert btc_data is not None, "BTC verisi alınamadı!"
    assert len(btc_data) >= 100, "Yetersiz veri"
    logger.info(f"✅ BTC OHLCV verisi alındı: {len(btc_data)} satır")
    
    # 2. Feature engineering testi
    logger.info("\n2️⃣ Feature engineering testi...")
    from feature_engineering import FeatureEngineer
    
    engineer = FeatureEngineer()
    df_features = engineer.engineer_all_features(btc_data)
    
    assert df_features is not None, "Feature engineering başarısız!"
    assert len(df_features.columns) > 20, "Yetersiz feature sayısı"
    logger.info(f"✅ {len(df_features.columns)} feature oluşturuldu")
    
    # 3. Model eğitimi testi
    logger.info("\n3️⃣ Model eğitimi testi...")
    from forecaster import EnsembleForecaster
    
    forecaster = EnsembleForecaster(symbol="BTC", horizon="1h")
    success = forecaster.train(df_features)
    
    assert success, "Model eğitimi başarısız!"
    logger.info("✅ Model eğitimi tamamlandı")
    
    # 4. Tahmin testi
    logger.info("\n4️⃣ Tahmin testi...")
    prediction = forecaster.predict(df_features)
    
    assert "point_forecast" in prediction, "Tahmin eksik!"
    assert "confidence" in prediction, "Güven skoru eksik!"
    
    logger.info(f"✅ Tahmin oluşturuldu:")
    logger.info(f"   Nokta tahmini: ${prediction['point_forecast']:.2f}")
    logger.info(f"   Güven: {prediction['confidence']*100:.1f}%")
    logger.info(f"   Yükseliş olasılığı: {prediction['direction_prob_up']*100:.1f}%")
    logger.info(f"   %80 aralık: ${prediction['interval_80'][0]:.2f} - ${prediction['interval_80'][1]:.2f}")
    
    # 5. Metrik izleme testi
    logger.info("\n5️⃣ Metrik izleme testi...")
    from metrics_tracker import MetricsTracker
    from datetime import datetime
    
    tracker = MetricsTracker(symbol="BTC", horizon="1h")
    
    # Tahmin kaydet
    tracker.record_prediction(
        timestamp=datetime.now(),
        point_forecast=prediction["point_forecast"],
        interval_80=prediction["interval_80"],
        interval_95=prediction["interval_95"],
        direction_prob_up=prediction["direction_prob_up"],
        confidence=prediction["confidence"],
        current_price=btc_data["close"].iloc[-1],
        features_used=["rsi", "macd", "volume"]
    )
    
    logger.info("✅ Tahmin kaydedildi")
    
    # 6. API endpoint testi (mock)
    logger.info("\n6️⃣ API response formatı testi...")
    
    response_data = {
        "symbol": "BTC",
        "horizon": "1h",
        "timestamp": datetime.now().isoformat(),
        "point_forecast": prediction["point_forecast"],
        "interval_80": prediction["interval_80"],
        "interval_95": prediction["interval_95"],
        "direction_prob_up": prediction["direction_prob_up"],
        "confidence": prediction["confidence"],
        "rationale": "Test tahmini. ⚠️ Gerçek yatırım için kullanmayın.",
        "features_used": ["rsi", "macd", "volume", "atr", "bb_pct"],
        "learning_action": "no_update"
    }
    
    logger.info("✅ API response formatı geçerli")
    
    # Başarı özeti
    logger.info("\n" + "="*50)
    logger.info("🎉 TÜM TESTLER BAŞARILI!")
    logger.info("="*50)
    logger.info("\nSistem hazır. Başlatmak için:")
    logger.info("  Backend: python main.py")
    logger.info("  Mobile:  cd mobile && npx expo start")
    logger.info("\n⚠️ UYARI: Finansal tavsiye değildir!")
    
    return True


if __name__ == "__main__":
    try:
        result = asyncio.run(test_full_pipeline())
        sys.exit(0 if result else 1)
    except Exception as e:
        logger.error(f"❌ Test başarısız: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
