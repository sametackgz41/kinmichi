# Değişiklik günlüğü

## 2.3.2 — Akıcılık

- İstatistik, seri, rekor ve bugün listesi hesapları artık arka planda yapılıyor; dokunma ve sekme geçişlerinde ekran donmuyor
- İlerleme, takvim ve planlama ekranları istatistikleri her çizimde baştan hesaplamıyor

## 2.3.1 — Hız

- APK artık optimize edilmiş sürüm derlemesi (hata ayıklama modu kapalı, R8 ve hazır derleme profilleri): geçişler ve dokunmalar belirgin biçimde daha akıcı, dosya 30 MB'tan 5 MB'a indi
- Sekme ve ekran geçişleri kısaltıldı, günlük ilerleme halkası daha çabuk oturuyor

## 2.3.0 — Expressive, paylaşım ve saat

- Material 3 Expressive görünüm: yaylanan geçişler, daha yuvarlak şekiller, kalın başlıklar, dalgalı ilerleme halkaları ve yeni alt gezinme çubuğu
- Tamamlanınca dairesinden tırtıklı bir şekle dönüşen işaretleme düğmesi
- Seri kartı paylaşma: alışkanlık detayındaki paylaş düğmesi, alışkanlığın renginde seri, son 20 hafta ve son 30 günün oranını içeren bir görsel oluşturur
- Wear OS: "Bugünkü alışkanlıklar" kutucuğu ve küçük bir saat uygulaması; dokunulan alışkanlık telefonda işaretlenir (saat APK'sı ayrı dosya olarak yayında)

## 2.2.0 — Hedef koçu ve ruh hali

- Akıllı hedef önerisi: sayılı/süreli bir alışkanlıkta son 14 günün %40'ından azında hedefe ulaşıldıysa, gerçekte ulaştığın miktardan başlamayı öneren kart; yeni hedef tutunca kademeli olarak geri büyütme önerisi (kabul ya da redden sonra 14 gün sessiz)
- Bugün ekranında tek dokunuşla ruh hali (5 yüz); not ve ayrıntılı değerlendirme "Not ekle" ile
- Akşam 21:00'de "Bugün nasıl geçti?" bildirimi, üç hızlı yanıtla; Ayarlar'dan kapatılabilir
- İlerleme sekmesinde ruh hali içgörüleri: 10 gün kayıttan sonra hangi alışkanlıkların yapıldığı günlerin daha iyi ya da daha zor geçtiğini gösterir

## 2.1.0 — Yeni widget'lar

- Bugün widget'ı: günün ilerleme halkası ve kalan alışkanlık sayısı, + ile yeni alışkanlık, her satırda alışkanlığın rengi ve seri bilgisi
- Sayılı alışkanlıklarda +1 çevresinde ilerleme halkası, süreli alışkanlıklarda başlat düğmesi; liste kaydırılabiliyor, küçültülünce yalnızca özet
- Tek alışkanlık widget'ı: alışkanlığın renginde, boyuta göre büyüyen halka, ilerleme ve seri yazısı, genişken yatay düzen
- Bu hafta widget'ı: takvim haftası (hafta başı ayardan), her gün için halka, haftanın yüzdesi
- Widget seçicide gerçek önizlemeler (Türkçe ve İngilizce)

## 2.0.0 — Sade, odaklı alışkanlık takibi

- Japon temalı editoryal görünüm yerine Material You (duvar kâğıdından renk), standart tipografi ve şekiller
- Üç sekme: Bugün · İlerleme · Ayarlar; tek ekranlı alışkanlık düzenleyici, ilk açılışta hazır şablonlar
- Kaldırıldı: kütüphane, projeler, gelen kutusu, Gemini/AI asistanı ve planlayıcı, ustalık rozetleri, Health Connect (veritabanı tabloları yedek uyumluluğu için korundu)
- Yalnızca Türkçe ve İngilizce; veri katmanındaki Türkçe metinler (atlama nedenleri, rozetler) yerelleştirildi
- Üç animasyonlu widget: Bugün listesi, tek alışkanlık halkası, haftalık ızgara
- Haftada bir unutulan gün seriyi bozmuyor (dinlenme hakkı); Bugün satırlarında seri sayıları artık gerçekten hesaplanıyor
- Alışkanlık detayında seri sayıları ve son 12 ayın ısı haritası
- Pazar akşamı haftalık özet bildirimi; bildirimdeki "Tamamla" artık geri almıyor, tamamlanmış alışkanlık için hatırlatma gelmiyor
- Uygulama içinden hesap ve veri silme
- Hızlı ayarlar kutucuğu, uygulama kısayolları, tamamlama titreşimleri
- Düzenleyici ekran döndürmede bilgileri koruyor; bildirim izni kapalıysa uyarı gösteriliyor

## 1.4.1 — Son kalite düzeltmeleri

- Google girişinde uygun hesap bulunamaması ve yarım kalan ilk kurulum güvenli biçimde ele alındı
- Google hesabından çıkarken yerel veriyi tutma veya cihazdan kaldırma seçimi eklendi
- Plan görevlerine tam düzenleme ve serbest tarih seçici eklendi
- Görev çoğaltmada kontrol listeleri, şablonlarda etiketler korunuyor
- Kategori silmede bağlantılı kayıtlar silinmeden kategori etiketleri temizleniyor
- Kişisel kategoriler kütüphane kategori seçim adımına bağlandı
- CSV ve JSON yedek kapsamı ile Türkçe karakter testleri genişletildi

## 1.4.0 — Düzenleme ve keşif

- Alışkanlık, plan görevi ve kütüphane kayıtları için ortak etiketler
- Türkçe karakter uyumlu birleşik arama, kapsam filtreleri ve adlandırılabilir kayıtlı aramalar
- Emoji ve hazır renk paleti kullanan, yedeklenen kişisel kategori yönetimi
- Formlarda kişisel kategori önerileri ve plan görevlerinde toplu etiketleme
- Gelişmiş alışkanlık hedefleri, etkin tarih aralığı, zorluk, bağlam, niyet ve duraklatma
- Plan görevlerinde zaman blokları, tekrarlar, şablonlar, kontrol listeleri ve gerçekleşen süre
- Galeriden doğrudan özel kütüphane kapağı seçimi
- Etiket/kategori filtreli istatistikler ve alışkanlık zaman çizelgesi
- Uygulama kilidi için hemen, 1, 5 veya 15 dakika gecikme seçimi

## 1.3.0 — Tümden arayüz yenilemesi

- Alt sekmeye göre değişen tek bağlamsal ekleme düğmesi ve geçişli yeni uygulama kabuğu
- Bugün ekranında zaman çizgisi biçimli alışkanlıklar ve editoryal günlük odak alanı
- Kütüphanede gerçek raf ızgarası ile ayrıntılı liste görünümü arasında seçim
- Takvim günleri için düzenlenebilir alt panel ve istatistiklerde beş haftalık ısı haritası
- Alışkanlık, içerik ve hedef oluştururken anlık canlı önizleme; hedeflerde yerel tarih seçici
- Profil ve tüm ayarların kart yığını yerine bölümlü, taranabilir ayar mimarisi
- Planlama, güvenlik, bulut, yedekleme, tanılama ve yönetim ekranlarında ortak açık sayfa düzeni
- Örnek veri içermeyen temiz ilk kullanım ve Google hesabı adını kullanan kişisel karşılama

## 1.2.0 — Tsuzuku Editorial

- Tüm ekranlarda sıcak kâğıt ve kömür tonlarıyla yeni editoryal renk sistemi
- Ölçülü Japon kırmızısı vurgular ve Japonca bölüm imleri
- Daha sıkı tipografi, daha küçük köşeler, ince sınırlar ve azaltılmış kart görünümü
- Kompakt, çizgi göstergeli yeni ana navigasyon
- Bugün ekranında yeniden kurulan başlık, ilerleme özeti, rutinler ve alışkanlık satırları
- Kütüphane, takvim, istatistik, hedef, planlama, profil ve yardımcı ekranlarda ortak görsel hiyerarşi
- Tatil modu için elle tarih yazmak yerine yerel tarih aralığı seçici

## 1.1.1 — Giriş akışı düzeltmesi

- Google girişi tamamlanınca kaynak seçimi bekletmeden doğrudan Bugün ekranını açma
- İlk kurulumdaki bulut geri yükleme veya ilk yedeklemeyi arka planda tamamlama
- Google görünen adının ilk bölümünü kişisel selamlamada kullanma
- Onboarding profil adı alanını kaldırma

## 1.1.0 — Esnek planlama ve değerlendirme

- Günlük, haftalık veya aylık hedef dönemi ve kullanıcı tarafından belirlenen dönem hedefi
- Alışkanlıkları rutin adları altında gruplama
- Bir alışkanlığı başka bir alışkanlığın tamamlanmasına bağlama
- Tatil/mola tarih aralığı; bu günleri seri ve başarı oranı hesabının dışında tutma
- Haftalık ilerleme özeti ve kullanıcı notu
- İsteğe bağlı Health Connect adım eşitleme
- Ayrıntılı CSV ve okunabilir PDF raporu dışa aktarma

## 1.0.0 — Google hesabı ve bulut eşitleme

- Credential Manager üzerinden Google ile giriş
- Firebase Authentication ile kullanıcı kimliği
- Room yerel veritabanını koruyan Firestore bulut kopyaları
- İlk girişte cihaz veya bulut verisini seçme
- İki tarafta değişiklik olduğunda veri kaybını önleyen çakışma seçimi
- SHA-256 doğrulamalı, parçalara ayrılmış bulut yedekleri
- UID tabanlı Firestore güvenlik kuralları

Gerçek Firebase projesini bağlamak için [FIREBASE_SETUP.md](FIREBASE_SETUP.md) dosyasını izle. `google-services.json` bulunmadığında uygulama çevrimdışı çalışmaya ve kurulum ekranını göstermeye devam eder.

## 0.9.0

- Seçilebilir alışkanlıklar, hazır paletler ve boyuta uyarlanan satırlarla ana ekran widget'ı
- İzin gerektirmeyen JSON yedekleme ve doğrulamalı geri yükleme
- Yerel, dışa aktarılabilir hata raporları
- Yeni kurulumlar için dört adımlı ilk kullanım deneyimi

## 0.8.0

- Bağımsız hedefler: mevcut/son değer, birim, açıklama ve isteğe bağlı bitiş tarihi
- Hedef ilerletme, otomatik tamamlama, yeniden aktifleştirme, düzenleme, arşivleme ve silme
- Tuzlanmış SHA-256 özetiyle saklanan 4 haneli uygulama PIN'i
- Uyumlu cihazlarda isteğe bağlı biyometrik kilit açma

Tsuzuku, kişisel ve sade bir Android alışkanlık takip uygulamasıdır. Proje Kotlin ve Jetpack Compose ile geliştirilir.

## Şu anda çalışanlar

- Türkçe ve İngilizce kaynaklar
- Sistem, açık ve koyu tema arasında kalıcı kullanıcı seçimi
- Tsuzuku indigo renk sistemi
- Bugün ekranı ve günlük ilerleme özeti
- Tamamlandı, adet ve süre alışkanlıkları
- Room ile cihazda kalıcı alışkanlık ve günlük ilerleme kayıtları
- Etkileşimli ilerleme kontrolleri ve tamamlananlar ayrımı
- Beş adımlı yeni alışkanlık oluşturma akışı
- Bugün, Kütüphane, Takvim, İstatistik ve Profil ana sekmeleri
- Pazartesi başlayan aylık takvim ve geçmiş gün düzenleme
- 7/30/90 günlük başarı oranları, aktif günler ve alışkanlık serileri
- Kütüphane kategori/durum istatistikleri
- Kişisel profil adı; Türkçe/İngilizce dil ve Pazartesi/Pazar hafta başlangıcı seçimi
- Açılıp kapatılabilen günlük özet ve ekran görüntüsü/son uygulamalar koruması
- Seçili günlerde çalışan, cihaz yeniden başladığında kurulan gerçek alışkanlık bildirimleri
- Bildirimden doğrudan alışkanlık tamamlama
- Süre alışkanlıkları için kalıcı ve hedefte otomatik sonlanan kronometre
- Alışkanlık düzenleme, genel not, arşivleme ve onaylı silme
- Alışkanlıklar ve kütüphane için kullanıcı tarafından belirlenen elle sıralama
- Kütüphane içeriklerini sezon ve notlarıyla birlikte onaylı silme
- Bildirimde kullanıcının belirlediği dakika sonrası için tekrar hatırlatma
- Kitap, novel, manga, film, dizi, anime ve özel içerik kütüphanesi
- Kategori/durum filtreleri, arama ve uzaktan/yerel kapak görselleri
- Sayfa, dakika ve bölüm ilerlemesi; diziler ile animeler için ayrı sezonlar
- İçerik durumları, genel not ve zaman damgalı kısa notlar
- Kare Android adaptive icon

## Yerel geliştirme

1. Klasörü Android Studio ile açın.
2. Android SDK 36 ve JDK 17 seçin.
3. `app` yapılandırmasını çalıştırın.

Komut satırından doğrulama:

```powershell
./gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Google ile giriş ve bulut eşitleme, Firebase projesine ait `google-services.json` sağlandığında bağlanacaktır.
