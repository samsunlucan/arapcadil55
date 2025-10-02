# Kurulum ve Kullanım Kılavuzu

## 📋 Gereksinimler

### Backend (Sunucu)
- Python 3.10 veya üzeri
- pip (Python paket yöneticisi)
- Redis (opsiyonel, önbellekleme için)

### Mobile (Android Uygulama)
- Node.js 18+ 
- npm veya yarn
- Expo CLI
- Android Studio (Android emulator için) VEYA fiziksel Android telefon

---

## 🚀 Backend Kurulumu

### 1. Bağımlılıkları Yükle

```bash
cd /workspace

# Virtual environment oluştur (önerilir)
python -m venv venv
source venv/bin/activate  # Linux/Mac
# veya
venv\Scripts\activate  # Windows

# Paketleri yükle
pip install -r requirements.txt
```

### 2. Gerekli Dizinleri Oluştur

```bash
mkdir -p models metrics logs
```

### 3. Sunucuyu Başlat

```bash
python main.py
```

Sunucu `http://localhost:8000` adresinde çalışacak.

### 4. API Test Et

Tarayıcıda veya Postman ile:
- `http://localhost:8000` - API dökümantasyonu
- `http://localhost:8000/docs` - Swagger UI
- `http://localhost:8000/health` - Health check

---

## 📱 Mobile Kurulumu

### 1. Node Bağımlılıklarını Yükle

```bash
cd mobile
npm install
```

### 2. API Endpoint'i Ayarla

`mobile/src/config.js` dosyasını düzenle:

```javascript
// Bilgisayarınızın yerel IP'sini kullanın
const API_BASE_URL = 'http://192.168.1.100:8000';  // Örnek IP
```

**IP Adresinizi Öğrenme:**
- Windows: `ipconfig`
- Linux/Mac: `ifconfig` veya `ip addr`

### 3. Expo Başlat

```bash
npx expo start
```

### 4. Android'de Çalıştır

**Seçenek A: Fiziksel Telefon (Önerilir)**
1. Play Store'dan "Expo Go" uygulamasını indir
2. Telefon ve bilgisayar aynı WiFi ağında olmalı
3. Expo QR kodunu Expo Go ile tara

**Seçenek B: Android Emulator**
1. Android Studio'yu aç ve emulator başlat
2. Terminalde `a` tuşuna bas (Android emulator'da aç)

---

## 🔧 Konfigürasyon

### Backend (`config.py`)

Özelleştirmek için `config.py` dosyasını düzenleyin:

```python
# API ayarları
API_HOST = "0.0.0.0"
API_PORT = 8000

# Varlık sayısı
TOP_N_ASSETS = 100

# Tahmin ufukları
HORIZONS = ["15m", "1h", "4h", "1d"]

# Model parametreleri
LEARNING_RATE = 0.01
FORGETTING_FACTOR = 0.95
```

### Rate Limiting

Ücretsiz API limitleri:
- CoinGecko: 10-50 istek/dakika
- Binance: 1200 istek/dakika

Ayarları `config.py`'de değiştirebilirsiniz.

---

## 📊 Kullanım Örnekleri

### API Kullanımı (cURL)

**Top 100 listesi:**
```bash
curl http://localhost:8000/assets/top100
```

**BTC için 1 saatlik tahmin:**
```bash
curl "http://localhost:8000/forecast?symbol=BTC&horizon=1h"
```

**Performans metrikleri:**
```bash
curl "http://localhost:8000/metrics?symbol=ETH&horizon=4h"
```

### Python ile Kullanım

```python
import requests

# Tahmin al
response = requests.get(
    "http://localhost:8000/forecast",
    params={"symbol": "BTC", "horizon": "1h"}
)
forecast = response.json()

print(f"Tahmin: ${forecast['point_forecast']:.2f}")
print(f"Güven: {forecast['confidence']*100:.1f}%")
print(f"Yükseliş olasılığı: {forecast['direction_prob_up']*100:.1f}%")
```

---

## 🎯 Mobil Uygulama Kullanımı

### Varlıklar Sekmesi
- Top 100 kripto para listesini görüntüle
- Arama yaparak istediğin coini bul
- Bir varlığa tıklayarak tahmin ekranına git

### Tahmin Sekmesi
1. Kripto sembolü gir (BTC, ETH, vb.)
2. Zaman dilimi seç (15dk, 1s, 4s, 1g)
3. "Tahmin Al" butonuna bas
4. Sonuçları incele:
   - Nokta tahmini
   - Güven aralıkları (%80 ve %95)
   - Yükseliş olasılığı
   - Model güveni
   - Kullanılan göstergeler

### Performans Sekmesi
1. Sembol ve zaman dilimi seç
2. "Performans Metrikleri" butonuna bas
3. Model doğruluğunu incele:
   - MAPE (hata yüzdesi)
   - Yön doğruluğu
   - Güven aralığı kapsama oranı

---

## 🐛 Sorun Giderme

### Backend Sorunları

**"Module not found" hatası:**
```bash
pip install -r requirements.txt
```

**TensorFlow yüklenemiyor:**
```bash
# CPU versiyonu
pip install tensorflow-cpu==2.15.0
```

**Redis bağlantı hatası:**
Redis gerekli değil, kod fallback kullanacak. Görmezden gelebilirsiniz.

### Mobile Sorunları

**"Unable to connect to server":**
1. Backend sunucusunun çalıştığından emin ol
2. `mobile/src/config.js`'deki IP adresini kontrol et
3. Telefon ve bilgisayar aynı ağda mı?

**Metro bundler hatası:**
```bash
cd mobile
rm -rf node_modules
npm install
npx expo start --clear
```

**Android emulator yavaş:**
Fiziksel telefon kullanın, çok daha hızlı olacak.

---

## 📈 İlk Model Eğitimi

İlk tahmin isteğinde model otomatik olarak eğitilecek. Bu ~1-2 dakika sürebilir.

Önceden eğitmek için:

```python
python -c "
from data_collector import CryptoDataCollector
from feature_engineering import FeatureEngineer
from forecaster import EnsembleForecaster
import asyncio

async def train_btc():
    collector = CryptoDataCollector()
    engineer = FeatureEngineer()
    
    df = await collector.get_ohlcv_binance('BTC', '1h', 500)
    df_features = engineer.engineer_all_features(df)
    
    forecaster = EnsembleForecaster('BTC', '1h')
    forecaster.train(df_features)
    print('✅ BTC model eğitildi!')

asyncio.run(train_btc())
"
```

---

## ⚠️ Önemli Notlar

1. **Finansal Tavsiye Değildir:** Bu sistem sadece eğitim amaçlıdır.

2. **API Limitleri:** Ücretsiz API'leri aşırı kullanmayın. Rate limiting aktif.

3. **Model Performansı:** İlk tahminler düşük güvenilirlikte olabilir. Zaman içinde iyileşir.

4. **Veri Gecikmesi:** Gerçek zamanlı değil, 1-5 dakika gecikme olabilir.

5. **Telif:** Tüm kullanılan kütüphaneler açık kaynak ve ücretsizdir.

---

## 📚 Ek Kaynaklar

- FastAPI Dökümantasyon: https://fastapi.tiangolo.com
- Expo Dökümantasyon: https://docs.expo.dev
- LightGBM: https://lightgbm.readthedocs.io
- TensorFlow: https://www.tensorflow.org

---

## 🤝 Destek

Sorun yaşarsanız:
1. Hata mesajını ve logları kontrol edin
2. `logs/` klasöründeki log dosyalarını inceleyin
3. API'nin `/health` endpoint'ine istek atarak sunucu durumunu kontrol edin

**Başarılar! 🚀**
