# Arxitektura va qarorlar

## MVC chegaralari

Flutter View faqat ekran chizadi va controller amallarini chaqiradi. Controller
MethodChannel orqali Android adapteriga so‘rov yuboradi, EventChannel orqali holatni
oladi. Kotlin Model ichidagi DrowsinessEngine Android API’lariga bog‘liq emas.
CameraSession model uchun Observation yaratadi. MonitorService resurslar va fon
hayot siklini boshqaradi. AlarmController faqat model qaroriga javob beradi.

```mermaid
flowchart LR
  V[Flutter View] --> C[Flutter Controller]
  C --> B[Platform channels]
  B --> S[Kotlin MonitorService]
  S --> K[CameraX / bundled ML Kit]
  K --> M[Observation / DrowsinessEngine]
  M --> A[Alarm / vibratsiya / TTS]
  M --> B
  A -. ixtiyoriy USB .-> P[Python local API / OpenAI]
```

Kadrlar Kotlin ichida qoladi; Flutter stream faqat raqamlar va holat matnini oladi.
Inference backpressure eng oxirgi kadrni saqlaydi, tahlil taxminan 8 FPS bilan
chegaralanadi. Bu Galaxy A51’da o‘lchangan FPS emas; qurilmada profil qilish kerak.

## Aniqlash

- Bitta kuzatilayotgan yuz kerak. Frontal rejimda yaw ±18°, pitch ±30° va
  roll ±25° ichida ML Kit’ning ikkala ko‘z klassifikatsiyasi ishlatiladi.
  Ikkala qiymat <0.35 bo‘lsa yopiq deb olinadi.
- |Yaw| 18° dan katta va 60° gacha, pitch va roll ±25° ichida bo‘lsa,
  MediaPipe Face Landmarker CPU modeli ko‘rinadigan ko‘zni tekshiradi.
  Bitta kuzatilayotgan yuz, yuz ichidagi burun va ko‘z nuqtalari talab qilinadi.
- Yaqin ko‘z qovog‘i geometriyasi va eyeBlink klassifikatori kelishganda
  ochiq/yopiq holati beriladi. Ko‘z kamida 10 piksel kenglikda bo‘lishi kerak.
  Blink ≥0.45 va qovoq nisbati ≤0.11 bo‘lsa kuchli yopilish dalili olinadi.
  Blink ≥0.65 va nisbat ≤0.16 ham yopiq hisoblanadi. Blink ≤0.25 va
  nisbat 0.12–0.65 ochiq hisoblanadi. Bu chegaralar tajriba uchun, kamera validatsiyasi zarur.
- 700 ms da UI ogohlantiradi, 1500 ms da signal beradi.
- PERCLOSga o‘xshash yopiq ko‘z ulushi 30 s oynada, kuzatilgan vaqt bilan hisoblanadi.
  Ko‘zlar 3 s davomida baholanmasa, oldingi ulush tarixi tozalanadi.
  Bu klinik PERCLOS validatsiyasi emas. Frontal klassifikatsiya va yon kontur holati ishlatiladi.
- 700 ms dan katta kadr uzilishlari tarixni tozalaydi; kuzatilmagan vaqt ko‘z yumilishi
  sifatida sanalmaydi. Alohida watchdog 3 s kadr kelmasa cheklov signalini beradi.
- Yuz identity’si o‘zgarsa vaqt tarixi yangilanadi. Bir nechta yuz ko‘rinsa haydovchi
  taxminan tanlanmaydi; kamerani faqat haydovchiga qaratish talab qilinadi.
- Boshlang‘ich chegaralar tadqiqot taxminlari; haqiqiy dataset va telefonda sozlanishi kerak.

## Fon rejimi

Camera foreground service foydalanuvchi ilova ichida Start bosganda kamera ruxsatidan
keyin boshlanadi. Bildirishnomada Stop bor. Activity backgroundga o‘tganda preview
ajratiladi, ImageAnalysis service LifecycleOwner’da qoladi. START_NOT_STICKY ishlatiladi:
process o‘lgandan keyin kamera foydalanuvchisiz qayta ochilmaydi. Boot receiver yo‘q.

Android yoki Samsung processni o‘ldirsa, telefon qizib ketsa, kamera boshqa ilovaga
o‘tsa yoki ruxsat bekor qilinsa, davomiylik kafolatlanmaydi. Ekran o‘chgan holat,
batareya optimizatsiyasi, signal/DND va telefon qo‘ng‘irog‘i jismoniy sinov talab qiladi.

## Ma’lumot va LLM

Flutter View sozlamalar va ulanish holatini ko‘rsatadi. Controller ulanishni
tekshiradi va sozlamani saqlaydi. Model qatlamida Kotlin RecoveryTrigger
uyquchanlikdan keyin bir soniyalik ochiq ko‘z dalilini kutadi, Python
DriverConversation esa OpenAI suhbat ID’si va ovozga ruxsat etilgan javoblarni boshqaradi.
Android alarm, TTS va nutqni tanish, HTTP va Flutter platform channel tashqi
xizmatlarga adapter sifatida ishlaydi.

Android mikrofonni faqat yordamchi yoqilgan va ruxsat berilgan kuzatuv sessiyasida,
savol tugaganidan keyin qisqa muddat ochadi. Alarm qaytsa yoki kuzatuv to‘xtasa,
ovozli almashinuv bekor qilinadi. Android nutqni tanish xizmati ishlatiladi va
offline tanish so‘raladi. Xizmatning o‘zi ishlash usulini tanlaydi.

Python server MacBook loopback manzilida ishlaydi. ADB reverse orqali telefon
USB yoki Wi-Fi’dan ulanadi. API kaliti MacBook muhitida qoladi.
OpenAI suhbatiga faqat trigger matni va tanilgan javob matni yuboriladi.
Qayta ulanish, keyingi triggerlar va server qayta ishga tushganda saqlangan
suhbat ID’si ishlatiladi.

## Rasmiy manbalar

- [Flutter platform channels](https://docs.flutter.dev/platform-integration/platform-channels)
- [Flutter SDK archive](https://docs.flutter.dev/install/archive)
- [Android camera foreground service](https://developer.android.com/develop/background-work/services/fgs/service-types#camera)
- [Foreground service background restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- [ML Kit Android face detection](https://developers.google.com/ml-kit/vision/face-detection/android)
- [ML Kit face angles and frontal eye classification](https://developers.google.com/ml-kit/vision/face-detection/face-detection-concepts)
- [Ultralytics pose keypoints](https://docs.ultralytics.com/tasks/pose/)
- [OpenAI Responses API](https://developers.openai.com/api/docs/guides/text)

## Release optimizatsiyasi

`app/proguard-rules.pro` ML Kit reflection orqali yaratadigan Firebase
ComponentRegistrar implementatsiyalarining public no-arg konstruktorlarini saqlaydi.
Emulyatordagi release sinovida bu qoidasiz FaceDetection.getClient ishlamasligi
aniqlandi. [R8 / ML Kit issue](https://issuetracker.google.com/issues/574078150).
