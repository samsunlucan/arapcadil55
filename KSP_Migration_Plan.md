KSP Migration Plan

Amaç
- Projede kapt kullanan Room (ve diğer annotation processor) yapılarını KSP'ye geçirmek. Amaç: derleme performansını artırmak ve gelecekteki Kotlin sürümleriyle daha iyi uyum.

Ön şartlar
- Projeyi çalışır ve testlerin geçtiği bir durumda bırakın (şu an assembleDebug ve unit test adımları çalıştırıldı).
- Tüm değişikliklerin bir Git dalında (ör. feature/ksp-migration) olmasını sağlayın.

Adımlar
1) Versiyon eşleştirmesi
   - Kotlin sürümünü kontrol et (gradle/libs.versions.toml -> kotlin = "2.2.10").
   - KSP plugin sürümünü Kotlin sürümü ile uyumlu seç (ör: KSP 1.0.16 veya KSP sürümlerinin Kotlin eşleri). KSP dağıtım notlarına bakın.

2) Plugin yönetimi
   - settings.gradle.kts içinde pluginManagement.repositories { gradlePluginPortal(); mavenCentral(); google() } olduğundan emin olun.
   - Modul build.gradle.kts içinde kapt plugin yerine id("com.google.devtools.ksp") version "<uyumlu-sürüm>" ekle (veya catalog eklentisi kullan).

3) Bağımlılık değişimi
   - Kapt ile eklenen annotation-processor bağımlılıklarını (ör. kapt("androidx.room:room-compiler:VERSION")) ksp("androidx.room:room-compiler:VERSION") olarak değiştir.
   - Eğer başka kapt kullanan kütüphaneler varsa (Moshi, AutoService, vb.), KSP sürümlerini kontrol et veya alternatif KSP-compat paketlerini kullan.

4) Derleme ve test
   - ./gradlew clean assembleDebug test
   - Eğer derleme hatası verirse: logları incele, eksik plugin/artifact hatasıysa plugin sürümünü değiştir ve tekrar deneriz.

5) Geri dönüş planı
   - Eğer KSP ile uyumsuzluk veya kritik hata çıkarsa: geri al (git checkout -b rollback-ksp veya revert commit). Kapt konfigürasyonumuz mevcut, geri döndüğünde kapt çalışır durumda olacak.

6) Son adımlar
   - Başarılıysa CI workflow içinde kapt kullanımını kaldırıp KSP ile güncelle. Gradle cache/CI ayarlarını güncelle.
   - Gözlem: ilk KSP geçişi sonrası incremental derleme süresinde ölçüm yapın.

Örnek komutlar
- Gradle temiz + derleme: ./gradlew clean assembleDebug --no-daemon
- Test: ./gradlew test

Notlar
- Room zaten kapt ile çalışıyor; KSP'ye geçiş dikkat ve sürüm uyumu ister. Ben uyumlu plugin sürümünü seçip deneme yapabilirim; ama ilk denemede plugin çözümlemesi/versiyon hatası çıkarsa hızla geri alırım.

Sonraki öneri
- İzin verirseniz ben bu dalda (feature/ksp-migration) otomatik olarak uyumlu KSP plugin sürümünü seçip küçük bir deneme (app module) uygulayayım. Başarısız olursa otomatik revert yaparım.

---

Gerçekleşenler (Tarih: 2025-09-06)
- KSP geçişi tamamlandı: app modülünde kapt kaldırıldı, KSP eklentisi eklendi ve Room compiler ksp yapılandırmasına alındı.
- Sürüm hizalaması: Kotlin 2.0.21, KSP 2.0.21-1.0.25 (2.1.x/2.2.x için KSP plugin artefact’ları repo’larda bulunamadığından en yakın kararlı ikili seçildi).
- Bağımlılıklar: Room, Navigation, Lifecycle-VM-Compose, Paging, WorkManager sürüm kataloğu alias’larıyla kullanıldı.
- Manifest uyarısı giderildi: AndroidManifest.xml içindeki package özniteliği kaldırıldı (namespace Gradle tarafında).
- Derleme ve test: clean + assembleDebug + test başarılı.

Uyarılar (derleme zamanı)
- RoomDatabase.fallbackToDestructiveMigration() API’si deprecated; uygun overload’a geçilebilir.
- SrsDao.kt içinde sorgu-kolon eşleşmesi için Room’un uyarıları mevcut; @RewriteQueriesToDropUnusedColumns veya @ColumnInfo ile değerlendirilebilir.

İzlenecekler / Güncelleme Planı
- Kotlin’i 2.2.x’e yükseltme: KSP’nin ilgili plugin sürümü yayınlandığında (plugin repos’da mevcut olduğunda) Kotlin’i tekrar yükseltip KSP sürümünü eşitleyerek denenecek.
- CI: Kapt kaldırıldığı için ek bir ayara gerek yok; standart Gradle cache ayarları yeterli. Yine de ilk birkaç build’te süre ölçümü yapılacak.
