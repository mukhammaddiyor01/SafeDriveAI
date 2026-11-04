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

## Hali tasdiqlanmagan

Samsung Galaxy A51 ulanmagan: haqiqiy kamera, ko‘z/bosh holati aniqligi, 1.5 s
signalning real kechikishi, ovoz/vibratsiya eshitilishi, tun/ko‘zoynak/yonbosh
holatlari, TTS tili, 30–60 daqiqalik fon ishlashi va quvvat sarfi tekshirilmagan.

Trening pipeline sun’iy test tasvirlarida tekshirildi. Bu haqiqiy haydovchilar uchun
model aniqligi dalili emas. Maxsus datasetda model o‘qitilmadi va PyTorch/YOLO
modeli Android alarmiga ulanmagan. Haqiqiy Ollama modeli o‘rnatib ishga tushirilmadi:
LLM adapteri HTTP mock bilan, mahalliy server esa offline fallback bilan tekshirildi.
Ovozli javobni tinglash va to‘liq bo‘yin/ko‘p burchakli maxsus model keyingi ishlar.
