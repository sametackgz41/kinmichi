# Tsuzuku Firebase kurulumu

Uygulamadaki Google girişi ve bulut eşitleme kodu hazırdır. Gerçek bağlantı için uygulama sahibinin Firebase projesini bir kez yapılandırması gerekir.

1. [Firebase Console](https://console.firebase.google.com/) içinde bir proje oluştur.
2. Android uygulaması ekle ve paket adı olarak `app.tsuzuku` yaz.
3. Bu çalışma alanındaki debug imzasını Firebase uygulamasına ekle:
   - SHA-1: `5D:C1:4A:C1:F9:4B:65:F7:49:7F:0E:39:DC:F8:B9:D2:86:22:58:DC`
   - SHA-256: `B5:30:10:7B:17:43:F3:46:F4:5D:13:11:8D:90:6F:20:24:A7:EC:DA:9B:6B:42:FB:F0:5C:1D:9E:A4:B8:54:BF`
   Yayın öncesinde release imzasının SHA-1 ve SHA-256 değerlerini de ayrıca ekle.
4. Authentication > Sign-in method bölümünde Google sağlayıcısını etkinleştir.
5. Firestore Database oluştur. Üretim modu seçilebilir; erişim kuralları bu projedeki `firestore.rules` dosyasındadır.
6. Firebase’den güncel `google-services.json` dosyasını indir ve tam olarak `app/google-services.json` konumuna koy.
7. Firebase CLI kullanıyorsan güvenlik kurallarını `firebase deploy --only firestore:rules` ile yayınla.
8. Uygulamayı yeniden derle. Profil > Google hesabı ekranında “Google ile devam et” düğmesi açılır.

## Veri modeli ve güvenlik

- Her kullanıcının verileri yalnızca `users/{firebaseUid}` yolunun altında tutulur.
- Firestore kuralları, oturumdaki UID ile yol UID’si eşleşmedikçe bütün okuma ve yazmaları reddeder.
- Yerel Room veritabanı ana çalışma alanıdır; çevrimdışı kullanım devam eder.
- Bulut kopyaları SHA-256 bütünlük kontrolüyle parçalara ayrılır. İki cihazda aynı anda değişiklik varsa uygulama sessizce üzerine yazmaz ve kullanıcıdan korunacak kopyayı seçmesini ister.
- PIN, PIN özeti, biyometri ayarı ve cihaz güvenlik tercihleri buluta gönderilmez.
