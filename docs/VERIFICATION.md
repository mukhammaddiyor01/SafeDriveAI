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
