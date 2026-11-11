# Tekshirish natijalari — 2026-11-05

Versiya: 0.1.0+1. Flutter 3.47.9, Dart 3.13.7, JDK 17, Android SDK 36.
APK: arm64, Android API 26+, target SDK 36, release rejimi, developer imzosi.

| Tekshiruv | Natija |
|---|---|
| flutter analyze | Xato va lint muammosi yo‘q |
| Flutter controller/UI | 3 test o‘tdi; 360×800 ekran va sozlamalar tekshirildi |
| Kotlin vaqt mantig‘i | 8 test o‘tdi: blink, uzoq yumilish, yonbosh, uzilish, yuz yo‘qolishi, identity, PERCLOS, ko‘p yuz |
| Python | 10 test o‘tdi: dataset split/validatsiya, trening/baholash, assistant API va fallback |
| Build skripti | Release APK yaratdi va releases/SafeDriveAI.apk ga saqladi |
| APK imzosi | apksigner tekshiruvi o‘tdi, v2 signature |
| YOLO/PyTorch | Rasmiy yolo11n-pose.pt bilan sun’iy bo‘sh kadrda inferens bajarildi |
| Python HTTP server | /health va /question: HTTP 200, model belgilanmaganda source=offline |

## Android 13 / API 33 emulyatorida

- Release APK o‘rnatildi va ochildi.
- Kamera ruxsati rad etilganda tushunarli xabar chiqdi; qayta ruxsat so‘rash ishladi.
- ML Kit uchun R8 reflection konstruktorlari saqlanib, release startup xatosi tuzatildi.
- Kamera preview ochildi; sun’iy kamera kadrida yuz yo‘qligi limited sifatida ko‘rsatildi.
- Foreground service va doimiy bildirishnoma ishladi.
- Home bosilgandan keyin haqiqiy ML Kit kuzatuv hisoblagichi 260 ga yetdi, oxirgi
  qayta ishlangan kadr yoshi 70 ms edi.
- Ekran Asleep holatida kuzatuvlar 260 → 567 ga ko‘paydi; oxirgi kadr yoshi 32 ms.
  Bu qisqa smoke sinovi; uzoq fon/batareya kafolati emas.
- Bildirishnoma matni joriy holatga yangilandi.
- Bildirishnomadagi To‘xtatish service va kamerani yopdi; faol camera client qolmadi.
- Yakuniy sinovda AndroidRuntime crash yoki SafeDriveAI error log qayd etilmadi.

[Emulyatordagi asosiy ekran](images/dashboard.png).

## 2026-11-05 holatida hali tasdiqlanmagan

Samsung Galaxy A51 ulanmagan: haqiqiy kamera, ko‘z/bosh holati aniqligi, 1.5 s
signalning real kechikishi, ovoz/vibratsiya eshitilishi, tun/ko‘zoynak/yonbosh
holatlari, TTS tili, 30–60 daqiqalik fon ishlashi va quvvat sarfi tekshirilmagan.

Trening pipeline sun’iy test tasvirlarida tekshirildi. Bu haqiqiy haydovchilar uchun
model aniqligi dalili emas. Maxsus datasetda model o‘qitilmadi va PyTorch/YOLO
modeli Android alarmiga ulanmagan. Haqiqiy Ollama modeli o‘rnatib ishga tushirilmadi:
LLM adapteri HTTP mock bilan, mahalliy server esa offline fallback bilan tekshirildi.
Ovozli javobni tinglash va to‘liq bo‘yin/ko‘p burchakli maxsus model keyingi ishlar.


# Qo‘shimcha tekshiruv — 2026-11-06

Talab SD-06 davom ettirildi. Android ilovasi va APK o‘zgarmadi; yangi Flutter/Kotlin
qurilma sinovi o‘tkazilgani da’vo qilinmaydi. Galaxy A51 USB ro‘yxatida topilmadi.

- Rasmiy MRL arxivi lokal yuklandi; SHA-256 va tasvir manbasi qayd etildi.
- 84,898 tasvirdan 76 ta dublikat guruhiga tegishli tasvir chiqarildi; 84,822 qoldi.
- 37 haydovchi train/val/test uchun 25/6/6 qilib ajratildi; filename, split,
  takroriy/almashgan tasvir, checkpoint va dataset identifikatori tekshiriladi.
- 24 ta Python unit/integratsion test o‘tdi. Synthetic sinov haqiqiy model natijasidan ajratilgan.
- Ikkita 5-epoch MPS run faqat validation loss bilan solishtirildi; 4×4 pooling,
  4-epoch checkpoint tanlandi. Protokol haqiqiy test inferensidan oldin commit qilindi.
- 10,227 held-out IR eye-crop: accuracy 86.06%, closed recall 96.51%, FPR 22.92%.
  Ko‘zoynakli guruh FPR 77.98%. Bu alarm hodisasi yoki telefon aniqligi emas.
- [To‘liq natija va cheklovlar](MRL_BASELINE.md), [agregat JSON](experiments/mrl-eye-2026-11-06.json).
- Tasvirlar, shaxsiy qatorlar va model weights Git’ga qo‘shilmadi. Model APK’ga ulanmagan.


## 80/20 talabi kiritilgandan keyingi tekshiruv — 2026-11-06

- 33 ta Python testi o‘tdi; 80/20 nisbati, haydovchilar ajratilishi, train-only
  yuklash, oxirgi epoch, ishlatilgan testni bloklash va manifestni muzlatish tekshirildi.
- MRL uchun 67,858 train / 16,964 test ajratildi; 30/7 haydovchi.
- Testning 16,964 tasviri ham oldingi foydalanishda topildi. Yangi haqiqiy trening
  yoki test inferensi bajarilmadi; oldingi natijalar o‘zgartirilmadi.
- [Audit va protokol](DATA_SPLIT_POLICY.md). Android/APK o‘zgarmadi.

## Kamera ko‘rinishi tiklangandagi alarm — 2026-11-08

- Kotlin’da 9 ta test o‘tdi. Regressiya testi 3 s davomida yuz
  ko‘rinmagach, ochiq ko‘zlar qayta ko‘ringanda eski PERCLOS alarmi
  ishga tushmasligini tekshiradi.
- Flutter analizida muammo topilmadi, 3 ta test o‘tdi.
- ARM64 release APK qayta yig‘ildi. APK v2 imzosi va SHA-256 mosligi tekshirildi.
- ADB qurilma ko‘rsatmadi. Yangilangan APK Galaxy A51 qurilmasida sinovdan o‘tmadi.

## USB o‘rnatish skripti (SD-08) — 2026-11-08

- Bash sintaksisi va beshta ADB simulyatsiyasi tekshirildi: standart USB tanlovi,
  aniq seriya raqami, qurilma yo‘qligi, offline holat va o‘rnatish xatosi.
  O‘rnatish xato bilan tugasa, ilovani ochish buyrug‘i yuborilmaydi.
- Haqiqiy ADB qurilma ko‘rsatmadi. Skript to‘xtadi va USB debugging hamda
  telefondagi ruxsatni tekshirish bo‘yicha ko‘rsatma chiqardi.

## Yangi dataset manifestida tasvir mazmuni (SD-06) — 2026-11-08

- 34 ta Python testi va 4 ta subtest o‘tdi. Xesh ustunisiz kirishda
  boshqa nomdagi nusxa rad etilishi va arxivdagi xesh orqali eski tasvir
  aniqlanishi tekshirildi.
- Pipeline sinovida 80/20 ajratish va butun train bilan o‘qitish sun’iy
  tasvirlarda bajarildi. Yangi haqiqiy dataset yoki model aniqligi baholanmadi.

## Inglizcha ekran va ChatGPT ovozli yordamchisi — 2026-11-08

- 360×800 Flutter widget sinovidan olingan bosh ekran rasmi ko‘zdan kechirildi.
  Flutter static analysis toza, 3 ta Flutter testi va 9 ta Kotlin testi o‘tdi.
- 34 ta Python testi va 6 ta subtest o‘tdi. Mock OpenAI transporti tasdiqlangan
  savolni qaytarish, HTTP xatosi, yaroqsiz javob va mahalliy fallbackni tekshirdi.
- ARM64 release APK yig‘ildi, imzo va SHA-256 mosligi tasdiqlandi.
- Jonli OpenAI kaliti sozlanmagan va ADB qurilma ko‘rsatmadi. Jonli API
  javobi va Galaxy A51’da inglizcha ovozning eshitilishi tekshirilmagan.

## Telefon sinovi va Test alarm tugmasi — 2026-11-08

- Wireless debugging yo‘riqnomasidan keyin foydalanuvchi telefon sinovi yaxshi
  o‘tganini bildirdi. Qaysi holatlar sinovdan o‘tgani alohida qayd etilmagan.
- Test alarm faqat ovoz va vibratsiyani sinaydi. Sinov tugaganda, ilova fon
  rejimiga o‘tganda yoki kuzatuv boshlanganda audio resurslari yopiladi.
- 9 Kotlin va 3 Flutter testi o‘tdi. ARM64 APK qayta yig‘ildi, imzosi va SHA-256
  qiymati tekshirildi. Shu yangi versiyada ovoz va vibratsiya jismoniy qurilmada
  hali tinglanmagan.

## Tiklanishdan keyin ovozli suhbat — 2026-11-08

- 35 ta Python testi o‘tdi. OpenAI transport simulyatsiyasida bitta suhbat
  yaratildi va server qayta ishga tushishi, ovozli javob hamda keyingi trigger
  shu suhbat ID’sidan foydalandi.
- 10 ta Kotlin va 3 ta Flutter testi o‘tdi. Kotlin testi tiklanish triggeri
  alarm va uzilgan kuzatuvda ishga tushmasligini tekshirdi.
- ARM64 release APK yig‘ildi, imzo va SHA-256 qiymati tekshirildi.
- ADB qurilma ko‘rsatmadi. Jonli OpenAI va Galaxy A51 mikrofon oqimi sinovdan
  o‘tishi kerak.

## Markaziy tutqichni kamera burchagi bo‘yicha baholash — 2026-11-08

- 37 ta Python testi o‘tdi. Sun’iy 80/20 manifestda past markaziy tutqich
  va navigator holatidagi chap va o‘ng kamera natijalari alohida chiqdi.
- Test hisobotidagi barcha 16 tasvir hisobga olindi. Bu yangi haqiqiy
  yozuvlardagi model aniqligini baholamaydi.

## Ovozli ogohlantirishni tuzatish va USB o‘rnatish — 2026-11-08

- Kotlin va Flutter testlari o‘tdi. Release APK imzosi tekshirildi.
- ADB orqali Galaxy A51’ga APK o‘rnatildi va ilova jarayoni ishga tushdi.
  USB orqali 8765 port yo‘naltirildi. Jonli OpenAI suhbat va ovoz tezligi
  telefonda alohida sinov talab qiladi.

## Telefonda ChatGPT hisobiga kirish — 2026-11-08

- Kotlin testlari, Flutter tahlili va to‘rtta Flutter testi o‘tdi.
- ARM64 release APK yig‘ildi va imzosi tekshirildi.
- ADB qurilma ko‘rsatmadi. Brauzer orqali haqiqiy hisobga kirish va
  telefondan OpenAI’ga yuborilgan jonli suhbat hali tekshirilmagan.

## Chrome orqali ChatGPT qaytish manzili — 2026-11-08

- Galaxy A51’da OpenAI tasdiqlashdan keyin OAuth kodi va holatini loopback
  manziliga yubordi. Chrome DevTools `ERR_CONNECTION_TIMED_OUT` ko‘rsatdi.
- Release APK yig‘ildi, Flutter tahlili va to‘rtta testi o‘tdi. Chrome
  qaytish manzilini ilovada qo‘lda topshirish yo‘li qo‘shildi. Haqiqiy hisob
  orqali shu yo‘lni yakunlash telefon egasining tasdig‘ini talab qiladi.

## Gemini Live — 2026-11-08

- 37 ta Python testi, Kotlin unit testlari va to‘rtta Flutter testi o‘tdi.
- SafeDriveAI token endpointi haqiqiy Gemini so‘rovida HTTP 200 qaytardi.
  Alohida Live sinovida bir martalik token bilan 24 kHz audio olindi.
- Release APK yig‘ildi va imzosi tekshirildi. Telefonda mikrofon, karnay
  va alarmdan keyingi avtomatik ishga tushish alohida tekshirilishi kerak.

## Gemini Live’ning HTTPS ulanishi — 2026-11-09

- 37 ta Python testi, Kotlin unit testlari va to‘rtta Flutter testi o‘tdi.
- Alohida token serveri ruxsatsiz so‘rovlarga HTTP 401 qaytardi. To‘g‘ri
  ruxsat bilan haqiqiy Gemini tokeni olindi va javobga no-store qo‘yildi.
- Release APK yig‘ildi va imzosi tekshirildi. ADB qurilma ko‘rsatmadi.
  Telefonda USB kabelsiz jonli audio sinovi hali bajarilishi kerak.

## Gemini Live navbatma-navbat suhbat (SD-07) — 2026-11-09

- Gemini Live WebSocket API sinovida sun’iy ovozdagi birinchi javob transkripsiya
  qilindi va keyingi savol audiosi qaytdi. Mikrofon oqimining pauzasi
  `audioStreamEnd` bilan bildirilganda ikkinchi javob `I feel okay.` ham
  transkripsiya qilindi.
- 37 ta Python testi va 6 ta subtest o‘tdi. Kotlin unit testlari, Flutter
  analizi va 4 ta Flutter testi o‘tdi. Release APK yig‘ildi.
- Galaxy A51’da jonli mikrofon va karnay bilan suhbatni to‘liq tasdiqlash
  uchun to‘xtab turgan mashinada foydalanuvchi sinovi kerak.

## Gemini Live erkin suhbat (SD-07) — 2026-11-09

- Yangi ko‘rsatma bilan jonli Gemini API’ga sun’iy ovoz yuborildi. Haydovchi
  suhbat mavzusini kosmosga o‘zgartirib, eng katta sayyorani so‘raganda
  Gemini ovozda Jupiter deb javob berdi. Keyingi `I feel okay.` javobi tanildi.
- Flutter analizi va 4 ta Flutter testi o‘tdi. Bu sinov Galaxy A51 mikrofonida
  jonli suhbatni tasdiqlamaydi.

## Yonbosh ko‘z holati tajribasi (SD-05, SD-06) — 2026-11-09

- 37 Python testi va 6 subtest o‘tdi.
- 80/20 hold-out: 724 train va 181 test kesmasi, 4/2 boshqa-boshqa subyekt.
- 10 epoch PyTorch MPS trening va 0.50 chegarada bir martalik CPU test.
  Chap tomonda 12/20 yopiq ko‘z aniqlandi. O‘ng testda yopiq ko‘z namunasi yo‘q.
- [Protokol va natijalar](SIDE_EYE_PILOT.md). Galaxy A51 inferensi va kamera
  kadridan ko‘zni topish sinovi bu tajribaga kirmagan.

## Gemini Live HTTPS joylashtiruvi (SD-07) — 2026-11-09

- Python token serveri alohida Docker konteynerida ishga tushdi. Lokal
  endpoint tokensiz HTTP 401, token bilan HTTP 200 qaytardi.
- Alohida Nginx virtual serveri orqali HTTPS ulanishi tekshirildi. Token
  berilmagan so‘rov HTTP 401 qaytardi. Mavjud sayt HTTP 200 qaytardi.
- Let's Encrypt sertifikatini yangilash sinovi muvaffaqiyatli o‘tdi.
- Galaxy A51’da HTTPS serveri muvaffaqiyatli saqlandi. Foydalanuvchi USB
  kabelsiz sinov muvaffaqiyatli o‘tganini bildirdi.

## Yonboshda ko‘rinadigan ko‘z kuzatuvi (SD-05) — 2026-11-10

- 14 Kotlin testi o‘tdi. Yangi sinovlar ±38° da ochiq ko‘z, davomli
  yumilish, noaniq kadrdan keyin timer va frontal bir ko‘z yumilishini qamradi.
- Flutter analizi toza, 4 Flutter testi o‘tdi.
- Sinovlar kamera kadridagi kontur sifati yoki Galaxy A51 aniqligini o‘lchamaydi.

## Ikki yon burchak va alarm (SD-03, SD-05, SD-06) — 2026-11-11

- 37 Python testi va 6 subtest, 15 Kotlin testi va 4 Flutter testi o‘tdi.
- ARM64 release APK yig‘ildi va USB orqali Galaxy A51’ga o‘rnatildi.
- Vaqtinchalik instrumental sinov Galaxy A51’da foydalanuvchi yuborgan
  ikkita kamera sohasini lokal qayta ishladi. ML Kit yaw 34,14° va -32,02°
  o‘lchadi. MediaPipe ko‘rinadigan ko‘zni ikkala rasmda ham ochiq deb topdi.
  Screenshot preview’idan olingan burchak haqiqiy kadr telemetry’siga teng emas.
- Sinov rasmlari va vaqtinchalik test harness Git’ga qo‘shilmadi.
- Bu statik rasm sinovi. Jonli kamera yopiq ko‘zni topishi, barcha burchaklar,
  alarmning real balandligi va mashina Bluetooth’i bilan eshitilishi hali
  foydalanuvchi bilan tekshirilishi kerak.
- [Audio oqimi](ALARM_AUDIO.md), [mobil model](FACE_LANDMARKER.md),
  [yangi 80/20 trening va test](BILATERAL_EYE_PROTOCOL.md).

## Tasdiqlangan mobil dizayn (SD-01, SD-09) — 2026-11-11

- Flutter analizi toza, 5 test o‘tdi. 320×640 ekranda 1.3 matn masshtabi
  bilan faol ekran, Stop tugmasi, burchak, noaniq ko‘z holati va to‘xtatish tekshirildi.
- 360×800 ekranda sozlamalar va boshlash amali tekshirildi. Kamera-off
  holatining PNG ko‘rinishi render qilinib ko‘rib chiqildi.
- Burchak shkalasi kamera yaw ma’lumotini ko‘rsatadi. Ma’lumot yo‘q bo‘lsa
  marker ko‘rsatilmaydi, diapazondan tashqarida son asl qiymatda qoladi.
- Bluetooth qatori ulanishni tasdiqlovchi real telemetry emas. Matn
  avtomobil audiosi ulanganida ishlashini bildiradi.

## Release yon ko‘z modeli regressiyasi (SD-05) — 2026-11-11

- O‘rnatilgan optimallashtirilgan APK ichidagi model refleksiya orqali
  chaqirilib, xato qayta olindi. MediaPipe protobuf-lite `platform_`
  maydoni R8 tomonidan chiqarib tashlangan edi.
- GeneratedMessageLite saqlangach, Flogger’da `no caller found on the stack`
  xatosi qayta olindi. Logger chaqiruv izlari, JNI framework callbacklari
  va face-result konvertorlari uchun scope qilingan keep qoidalari qo‘shildi.
- Keyingi release APK USB orqali Galaxy A51’ga o‘rnatildi. Telefonda uning
  SHA-256 qiymati lokal release APK bilan mosligi tekshirildi.
- Aynan shu release ichidagi model foydalanuvchining yangi rasmidagi
  ko‘zni ochiq deb qaytardi: `eye=true`, instrumentatsiya kodi `-1`.
- `scripts/check-release-eye.sh` mahalliy ochiq-ko‘zli yuz kesmasi bilan
  shu regressiyani qayta ishlatadi. AndroidTest instrumenti rasmlarni
  vaqtinchalik asset sifatida oladi va skript yakunida olib tashlaydi.
- Flutter analizida muammo yo‘q, 5 test o‘tdi. Jonli kamera va turli
  burchaklarda ko‘z yumilishi haqiqiy video bilan yana tekshirilishi kerak.

## Yonboshdagi yopiq ko‘z regressiyasi (SD-03, SD-05) — 2026-11-11

- Foydalanuvchi ko‘z ochiqligi topilganini, yumilganda esa limited qolganini
  bildirdi va chap/o‘ng yopiq-ko‘z rasmlarini berdi.
- Python o‘lchovida yaqin ko‘z blink qiymati 0.50–0.64 oralig‘ida edi.
  Oldingi 0.65 sharti tor qovoq dalili bilan birga ham yopiq holatni rad etgan.
- Endi blink ≥0.45 va qovoq nisbati ≤0.11 bo‘lsa yopiq deb olinadi.
  Oldingi blink ≥0.65 va nisbat ≤0.16 sharti ham saqlanadi.
- 16 Kotlin testi va 5 Flutter testi o‘tdi. Regressiya testi ±42/43°
  holatda o‘rta blink/tor qovoqdan 1 500 ms davomli yopilish alarmi chiqishini tekshiradi.
- Yangilangan ARM64 release APK Galaxy A51’ga o‘rnatildi. APK xeshi
  mosligi tekshirilib, aynan release modelida ikki yopiq rasm `eye=false`,
  oldingi ochiq rasm `eye=true` natijasini berdi. Uchala instrumentatsiya
  natijasi `-1` bo‘ldi. Kamera preview rasmlari tahlil koordinatalariga
  moslash uchun lokal gorizontal akslantirildi.
- Rasmlar va vaqtinchalik fixture Git’ga qo‘shilmadi. Bu kadr sinovi va
  domain timer sinovi. Jonli kameradagi alarm natijasi foydalanuvchidan kutilmoqda.
