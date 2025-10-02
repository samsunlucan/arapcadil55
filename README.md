# 🚀 Kripto Para Olasılıksal Tahmin Sistemi

**⚠️ DİKKAT: Bu yazılım yalnızca eğitim amaçlıdır. Finansal tavsiye DEĞİLDİR. Yatırım kararlarınızı kendi sorumluluğunuzda alın.**

---

## ✨ Özellikler

### 📊 Tahmin Yetenekleri
- ✅ Top 100 kripto para için tahmin
- ✅ 4 farklı zaman dilimi: 15dk, 1s, 4s, 1g
- ✅ Olasılıksal tahmin: nokta + %80/%95 güven aralıkları
- ✅ Yön tahmini (yukarı/aşağı) + olasılık
- ✅ Dinamik güven skoru

### 🤖 Makine Öğrenmesi
- ✅ **Ensemble model:** LightGBM + LSTM + kalibrasyon
- ✅ **50+ teknik gösterge:** RSI, MACD, Bollinger Bands, ATR, OBV, VWAP, vb.
- ✅ **Walk-forward backtesting:** Veri sızıntısı yok
- ✅ **Online öğrenme:** Hatalardan kendini günceller
- ✅ **Concept drift tespiti:** Piyasa değişikliklerini algılar

### 🌐 Teknoloji Stack
- ✅ **Backend:** FastAPI + Python (hızlı, modern API)
- ✅ **Mobil:** React Native + Expo (iOS/Android uyumlu)
- ✅ **ML:** scikit-learn + LightGBM + TensorFlow
- ✅ **Veri:** CoinGecko + Binance (ücretsiz, telif yok)

### 📱 Android Mobil Uygulama
- ✅ Modern Material Design UI
- ✅ Gerçek zamanlıya yakın tahminler
- ✅ Top 100 kripto listesi ve arama
- ✅ Performans metrikleri ve analiz
- ✅ Tamamen ücretsiz (Expo)

---

## 🏗️ Mimari

```
┌─────────────────┐
│  Android App    │  (React Native + Expo)
│  (Expo Go)      │
└────────┬────────┘
         │ REST API
         ▼
┌─────────────────┐
│  FastAPI        │  Endpoints: /forecast, /assets, /metrics
│  Backend        │
└────────┬────────┘
         │
    ┌────┴─────┬──────────┬────────────┐
    ▼          ▼          ▼            ▼
┌────────┐ ┌────────┐ ┌──────────┐ ┌──────────┐
│CoinGecko│ │Binance│ │LightGBM  │ │ LSTM     │
│  API   │ │  API  │ │ Model    │ │ Model    │
└────────┘ └────────┘ └──────────┘ └──────────┘
```

---

## 🚀 Hızlı Başlangıç (10 dakika)

### 1️⃣ Backend (5 dakika)

```bash
# Bağımlılıkları yükle
pip install -r requirements.txt

# Sunucuyu başlat
python main.py
```

✅ Sunucu: `http://localhost:8000`

### 2️⃣ Mobile (5 dakika)

```bash
cd mobile
npm install

# IP adresini ayarla (mobile/src/config.js)
# API_BASE_URL: 'http://YOUR_COMPUTER_IP:8000'

npx expo start
```

**Android telefonunda:**
- Play Store → **Expo Go** indir
- QR kodu tara
- Uygulama hazır! 🎉

📖 **Detaylı kurulum:** [`HIZLI_BASLANGIC.md`](HIZLI_BASLANGIC.md)

---

## 📡 API Endpoints

| Endpoint | Açıklama | Örnek |
|----------|----------|-------|
| `GET /assets/top100` | Top 100 kripto listesi | - |
| `GET /forecast` | Tahmin al | `?symbol=BTC&horizon=1h` |
| `GET /metrics` | Model performansı | `?symbol=ETH&horizon=4h` |
| `GET /health` | Sistem durumu | - |

**API Dökümantasyon:** `http://localhost:8000/docs` (Swagger UI)

---

## 📊 Tahmin Örneği

**Request:**
```bash
curl "http://localhost:8000/forecast?symbol=BTC&horizon=1h"
```

**Response:**
```json
{
  "symbol": "BTC",
  "horizon": "1h",
  "timestamp": "2025-10-02T14:30:00Z",
  "point_forecast": 67450.23,
  "interval_80": [66800, 68100],
  "interval_95": [66200, 68700],
  "direction_prob_up": 0.625,
  "confidence": 0.713,
  "rationale": "Yükseliş beklentisi (%2.3). Normal volatilite. ⚠️ Risk: Kripto piyasalar son derece volatildir.",
  "features_used": ["RSI", "MACD", "volume_24h", "ATR", "BB_pct"],
  "learning_action": "update_weights"
}
```

---

## 🧪 Sistem Testi

```bash
python test_system.py
```

Test eder:
- ✅ Veri toplama (CoinGecko + Binance)
- ✅ Feature engineering (50+ gösterge)
- ✅ Model eğitimi (LightGBM + LSTM)
- ✅ Tahmin üretimi
- ✅ Metrik izleme

---

## 📈 Özellikler (Features)

### Momentum Göstergeleri
- RSI (Relative Strength Index)
- MACD (Moving Average Convergence Divergence)
- Stochastic Oscillator (%K, %D)

### Trend Göstergeleri
- SMA/EMA (7, 20, 50, 200 periods)
- Bollinger Bands
- Price vs MA cross

### Volatilite
- ATR (Average True Range)
- Parkinson Volatility
- Realized Volatility (7d, 14d, 30d)

### Hacim
- OBV (On-Balance Volume)
- VWAP (Volume Weighted Average Price)
- Volume ratios

### Emir Defteri
- Bid/Ask spread
- Order imbalance

### Zaman
- Saat, gün, ay (cyclic encoding)
- Trading sessions (US, Asia)

---

## 🎯 Performans Metrikleri

Model performansı şunlarla izlenir:

- **MAPE** (Mean Absolute Percentage Error)
- **RMSE** (Root Mean Square Error)
- **Direction Accuracy** (Yön tahmin doğruluğu)
- **Brier Score** (Olasılık kalibrasyonu)
- **Coverage** (Güven aralığı kapsama oranı)
- **Confidence Correlation** (Güven vs gerçek doğruluk)

---

## 📱 Mobil Uygulama Ekran Görüntüleri

### 1. Varlıklar (Top 100)
- Market cap sıralaması
- 24h fiyat değişimi
- Arama özelliği
- Anlık fiyatlar

### 2. Tahmin
- Sembol seçimi
- Zaman dilimi (15m, 1h, 4h, 1d)
- Nokta tahmini
- Yükseliş olasılığı (progress bar)
- Güven skoru
- %80 ve %95 güven aralıkları
- Kullanılan göstergeler

### 3. Performans
- Model doğruluk metrikleri
- Tarihsel performans
- Metrik açıklamaları

---

## 🔒 Güvenlik ve Gizlilik

- ✅ Kullanıcı verisi toplanmaz
- ✅ Kişisel bilgi saklanmaz
- ✅ Sadece genel piyasa verisi kullanılır
- ✅ API key gerekmez (ücretsiz kaynaklar)

---

## 📜 Lisans

**MIT License** - Ticari kullanım dahil tamamen ücretsiz!

Tüm kullanılan kütüphaneler açık kaynak ve telif sorunu yok.

📄 Detaylar: [`LISANS.md`](LISANS.md)

---

## 📚 Dokümantasyon

- 📖 **Hızlı Başlangıç:** [`HIZLI_BASLANGIC.md`](HIZLI_BASLANGIC.md)
- 🛠️ **Kurulum Kılavuzu:** [`KURULUM.md`](KURULUM.md)
- ⚖️ **Lisans Bilgisi:** [`LISANS.md`](LISANS.md)

---

## ⚠️ Önemli Uyarılar

### Finansal Sorumluluk Reddi

Bu yazılım:
- ❌ Finansal tavsiye SAĞLAMAZ
- ❌ Yatırım önerisi DEĞİLDİR
- ❌ Gelecekteki performans garantisi VERMEZ
- ✅ Yalnızca eğitim amaçlıdır

### Riskler

Kripto para piyasaları:
- Aşırı volatil
- 7/24 işlem görür
- Düzenlenmemiş olabilir
- Tüm sermayenizi kaybedebilirsiniz

**Sadece kaybetmeyi göze alabileceğiniz parayla yatırım yapın!**

---

## 🤝 Katkıda Bulunma

Bu açık kaynak bir projedir. Katkılarınızı bekliyoruz!

1. Fork yapın
2. Feature branch oluşturun (`git checkout -b feature/amazing`)
3. Commit yapın (`git commit -m 'Add amazing feature'`)
4. Push edin (`git push origin feature/amazing`)
5. Pull Request açın

---

## 📞 Destek

Sorun mu yaşıyorsunuz?

1. [`KURULUM.md`](KURULUM.md) → Sorun giderme bölümü
2. `logs/` klasöründeki log dosyaları
3. `python test_system.py` → Sistem testi

---

## 🌟 Özellikler Yol Haritası

- [ ] WebSocket ile gerçek zamanlı tahminler
- [ ] Daha fazla teknik gösterge (ichimoku, fibonacci)
- [ ] Sentiment analysis (Twitter, Reddit)
- [ ] Portfolio optimization
- [ ] Backtesting UI
- [ ] iOS uygulama (Expo sayesinde zaten hazır!)

---

## 🎓 Öğrenme Kaynakları

- **Machine Learning:** [scikit-learn docs](https://scikit-learn.org)
- **Deep Learning:** [TensorFlow tutorials](https://www.tensorflow.org/tutorials)
- **Technical Analysis:** [TA-Lib](https://ta-lib.org)
- **FastAPI:** [FastAPI docs](https://fastapi.tiangolo.com)
- **React Native:** [Expo docs](https://docs.expo.dev)

---

**Başarılar! 🚀 Ama unutma: Tahminler olasılıksaldır, garantili değil!** 📈📉
