# GÖK — Akıllı Hava ve Gün Planlama

KAMP+ 2026 Android proje teslimi.

## Uygulama
Şehir seçimine göre hava tahmini, saatlik/7 günlük veriler, etkinlik saatlerinin karşılaştırılması, planlama, Akıllı Çanta ve kartpostal deneyimi.

## Mimari
Bu teslimde Android ana ekranı **Kotlin + Jetpack Compose AndroidView üzerinde WebView** ile çalışır. Arayüzün kaynak dosyası `app/src/main/assets/gok.html` ve web önizlemesi `preview/index.html` altındadır. Tamamen native Jetpack Compose uygulaması değildir.

## Çalıştırma
Android Studio'da bu depoyu açın (Java 17, Android SDK 36). Gradle senkronize olduktan sonra `assembleDebug` çalıştırın.

**Doğrulama notu:** Buradaki ortamda Android SDK/Gradle indirmeleri olmadığı için APK derlemesi doğrulanamadı. Web arayüzünün Android içindeki bazı tarayıcı köprüleri (ör. indirme/paylaşım) ayrıca cihazda doğrulanmalıdır. Bu sürüm, önceki proje ZIP'indeki tüm native kaynak/test modüllerini içermez.

Önizleme: [preview/index.html](preview/index.html). Hava verisi için ağ bağlantısı gerekir.
