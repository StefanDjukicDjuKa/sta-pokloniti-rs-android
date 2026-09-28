ŠTA POKLONITI RS — Android WebView aplikacija
=============================================

ŠTA JE URAĐENO
--------------
• Prava Android APK aplikacija koja prikazuje https://stapoklonitirs.com u WebView-u.
• Nema Chrome/adresne trake, tabova niti browser interfejsa.
• Naziv aplikacije: ŠTA POKLONITI RS
• Tvoj dostavljeni logo je ikonica aplikacije, splash/loading logo i offline logo.
• Full-screen loading ekran sa kružnim stvarnim procentom učitavanja.
• Shopify kolačići, JavaScript, localStorage i korpa ostaju aktivni.
• Upload slika/fajlova radi preko Android file pickera; na Android 10+ dodat je i izbor kamere.
• Android BACK prvo vraća prethodnu WebView stranicu.
• tel:, mailto:, intent: i slični linkovi mogu da otvore odgovarajuću Android aplikaciju.
• Download fajlova je podržan preko Android DownloadManager-a.

KADA NEMA INTERNETA
-------------------
Prikazuje se poseban native ekran:

"Na pauzi smo dok ne popraviš vezu sa internetom, kad to rešiš bićemo tu pre tebe."

Ispod je animirani refresh kružić i dugme OSVEŽI.

Dok nema interneta svira blag lokalni ambijentalni zvuk (žubor + diskretni cvrkut). Zvuk je u samoj APK aplikaciji i NE zahteva internet. Kada internet dođe, zvuk automatski prestaje, aplikacija prikaže loading procenat i ponovo učita stranicu.

KAKO NAJLAKŠE NAPRAVITI APK
---------------------------
OPCIJA 1 — Android Studio (preporučeno)
1. Instaliraj Android Studio.
2. Open -> izaberi folder StaPoklonitiRS_AndroidApp.
3. Sačekaj da Android Studio završi Gradle Sync i po potrebi instalira Android SDK 35.
4. Build -> Build App Bundle(s) / APK(s) -> Build APK(s).
5. Debug APK će biti u:
   app/build/outputs/apk/debug/app-debug.apk

OPCIJA 2 — Windows jednim fajlom
Ako Android Studio i Android SDK već postoje na računaru:
1. Dvoklik na BUILD_APK_WINDOWS.bat
2. Prvi put će preuzeti Gradle i potrebne build komponente.
3. Na kraju dobijaš:
   OUTPUT/STA_POKLONITI_RS.apk

ZA GOOGLE PLAY
--------------
Debug APK možeš odmah instalirati ručno na Android telefon.
Za Google Play potrebno je napraviti signed release AAB/APK i sačuvati signing key. Android Studio to radi kroz:
Build -> Generate Signed Bundle / APK.

TEHNIČKI PODACI
---------------
Package: rs.stapokloniti.app
Min Android: Android 8.0 (API 26)
Target/Compile SDK: 35
Početni URL: https://stapoklonitirs.com
Verzija: 1.0.0

VAŽNO
-----
WebView prikazuje isti Shopify sajt, pa promene proizvoda, cena, Custom Liquid-a i dizajna na Shopify-ju ne zahtevaju novu verziju aplikacije.
