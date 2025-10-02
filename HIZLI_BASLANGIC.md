# ⚡ Hızlı Başlangıç

Android telefonunuzda 10 dakikada çalıştırın!

---

## 🚀 Backend (Sunucu) - 5 dakika

```bash
# 1. Bağımlılıkları yükle
pip install -r requirements.txt

# 2. Sunucuyu başlat
python main.py
```

✅ Sunucu `http://localhost:8000` adresinde hazır!

**Test et:**
```bash
curl http://localhost:8000/health
```

---

## 📱 Mobile (Android) - 5 dakika

```bash
# 1. Mobile klasörüne git
cd mobile

# 2. Paketleri yükle
npm install

# 3. IP adresini ayarla
# mobile/src/config.js dosyasını düzenle:
# API_BASE_URL: 'http://BILGISAYAR_IP:8000'

# 4. Expo başlat
npx expo start
```

**Android telefonunuzda:**
1. Play Store'dan **Expo Go** indir
2. QR kodu tara
3. Uygulama açılacak!

---

## 📖 İlk Kullanım

### 1. Varlıklar Sekmesi
- Top 100 kripto listesi görünecek
- Herhangi birine tıkla (örn: BTC)

### 2. Tahmin Sekmesi
- Sembol: **BTC** (otomatik geldi)
- Zaman: **1h** seç
- **"Tahmin Al"** butonuna bas
- ⏳ İlk seferinde 1-2 dakika sürebilir (model eğitiliyor)

### 3. Sonuçlar
```
Tahmin Edilen Fiyat: $67,450.23
Yükseliş Olasılığı: %62.5
Güven Skoru: %71.3
```

---

## 🎯 Örnek Kullanım Senaryoları

### BTC 1 saatlik tahmin
```
Sembol: BTC
Zaman: 1h
→ "Yükseliş beklentisi (%2.3). Normal volatilite."
```

### ETH 4 saatlik tahmin
```
Sembol: ETH
Zaman: 4h
→ "Düşüş beklentisi (%1.8). Yüksek volatilite riski."
```

### SOL günlük tahmin
```
Sembol: SOL
Zaman: 1d
→ "Yükseliş beklentisi (%5.1). Artan hacim."
```

---

## 🔧 Sorun Giderme

### "Unable to connect to server"
```bash
# 1. Backend çalışıyor mu kontrol et
curl http://localhost:8000/health

# 2. Firewall izni ver (Windows)
# Windows Defender → İzin Ver

# 3. IP adresini doğrula
# mobile/src/config.js → API_BASE_URL
```

### "Insufficient data" hatası
- Bu coin Binance'de olmayabilir
- Başka bir coin dene (BTC, ETH, BNB hep çalışır)

### İlk tahmin çok uzun sürüyor
- Normal! Model ilk seferinde eğitiliyor
- Sonraki tahminler çok hızlı olacak (1-2 saniye)

---

## 📊 Tavsiye Edilen Coinler

✅ **Güvenilir (çok veri var):**
- BTC, ETH, BNB, SOL, ADA, XRP, DOT, MATIC

⚠️ **Riskli (az veri olabilir):**
- Yeni listelenmiş coinler
- Düşük hacimli altcoinler

---

## ⚡ Performans İpuçları

1. **İlk 5-10 tahminden sonra** model iyileşir
2. **1h ve 4h tahminleri** en güvenilir
3. **Yüksek volatilitede** güven skoru düşer (normal)
4. **Güven < %50** ise → tahmini ciddiye alma

---

## 🎨 Mobil Arayüz

```
┌─────────────────────────┐
│  🪙 Varlıklar           │ ← Top 100 liste
│  📈 Tahmin              │ ← Tahmin al
│  📊 Performans          │ ← Model metrikleri
└─────────────────────────┘
```

---

## ⚠️ ÖNEMLİ HATIRLATMA

```
⚠️ FİNANSAL TAVSİYE DEĞİLDİR!

Bu uygulama:
✅ Eğitim amaçlıdır
✅ Olasılıksal tahmin yapar
✅ Hata payı vardır

❌ Kesin sonuç vermez
❌ Yatırım tavsiyesi değildir
❌ Kar garantisi yoktur

Yatırım kararlarınızı kendi araştırmanıza 
ve profesyonel danışmanlığa dayandırın!
```

---

## 🆘 Hızlı Yardım

**Backend loglara bak:**
```bash
tail -f logs/app_*.log
```

**Mobil loglara bak:**
- Expo terminalinde canlı görürsün

**Sistemi test et:**
```bash
python test_system.py
```

---

## 📚 Detaylı Dokümantasyon

- **Kurulum:** `KURULUM.md`
- **Lisans:** `LISANS.md`
- **Genel:** `README.md`

---

## 🎉 Hadi Başla!

```bash
# Terminal 1: Backend
python main.py

# Terminal 2: Mobile
cd mobile && npx expo start

# Telefon: Expo Go ile QR tara
# 🚀 Hazırsın!
```

**İyi tahminler! 📈**

*(ama yine de gerçek parayla dikkatli ol 😉)*
