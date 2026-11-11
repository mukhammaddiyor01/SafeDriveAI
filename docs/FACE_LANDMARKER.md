# Mobil yon ko‘z modeli

SD-05, 2026-11-11. Yonbosh tasvirlarda ML Kit frontal ko‘z klassifikatsiyasi
o‘rniga Google MediaPipe Face Landmarker ishlatiladi. ML Kit barcha yuzlarni
sanash va tracking ID uchun qoladi. MediaPipe faqat bitta yuz va mos yon
burchak bo‘lganda kamera worker’ida ishlaydi. Har kadr oxirida vaqtinchalik
bitmaplar va MPImage yopiladi. Kamera tasvirlari tashqi serverga yuborilmaydi.

Model tayyor Google Face Landmarker float16/1 bundle, Tasks Vision 1.2.2.
SHA-256: `64184e229b263107bc2b804c6625db1341ff2bb731874b0bcc2fe6544e0bc9ff`.
`scripts/prepare-face-model.sh` modelni rasmiy manbadan yuklaydi va xeshini
tekshiradi. `scripts/build-android.sh` buni yig‘ishdan oldin chaqiradi.
Bevosita Flutter orqali ishga tushirishdan oldin ham tayyorlash skriptini bajaring.
Model APK assets ichiga qo‘shiladi va Git’da saqlanmaydi.

[Model manbasi](https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task).
[Google model hujjati](https://ai.google.dev/edge/mediapipe/solutions/vision/face_landmarker).

Yuborilgan ikkala skrinshotning kamera sohasida Python MediaPipe modeli
bitta yuz topdi va ko‘rinadigan ko‘zni ochiq deb baholadi. Bu o‘lchov
Galaxy A51 kamerasi yoki ko‘z yumilishi uchun to‘liq validatsiya emas.
±60° oralig‘i qo‘llab-quvvatlanadigan tekshiruv doirasi. Ko‘rinmaydigan yoki
noaniq ko‘z `limited` bo‘lib qoladi.

## Release tekshiruvi

Debug builddagi rasm sinovi release APK uchun yetarli dalil bo‘lmagan.
R8 protobuf maydonlari, Flogger chaqiruv izlari va native Graph callbacklarini
buzgan. `proguard-rules.pro` shu runtime qismlarini saqlaydi.
Kamera modeli xatolari endi 10 soniyada ko‘pi bilan bir marta logga yoziladi.
Logda kamera tasviri saqlanmaydi.

Release APK yig‘ilib va USB orqali o‘rnatilgach, quyidagi buyruq aynan
o‘rnatilgan APK xeshini tekshirib, uning ichidagi modelga lokal rasm beradi.
Bu tester faqat o‘z developer kaliti bilan imzolangan APK va test harnessiga
ega Android USB qurilmasi uchun. Beriladigan rasm ochiq ko‘zli yuz kesmasi
bo‘lishi kerak. Qo‘shimcha argumentlar yaw va kutilayotgan ochiq holat
qiymatidir. Default holat yaw -43° va ochiq ko‘z.

```bash
./scripts/check-release-eye.sh /absolute/path/to/open-eye-face-crop.jpg
./scripts/check-release-eye.sh /absolute/path/to/closed-eye-face-crop.jpg 43 false
```

[MediaPipe release muammosi](https://github.com/google-ai-edge/mediapipe/issues/5141),
[Flogger release muammosi](https://github.com/google-ai-edge/mediapipe/issues/6138).

## Yopiq ko‘zdagi o‘rta blink bahosi

2026-11-11 yuborilgan ikki yopiq-ko‘z rasmida Python MediaPipe o‘lchovi
yaqin ko‘z uchun blink taxminan 0.50–0.64 bo‘lganini ko‘rsatdi.
Oldingi 0.65 minimal qoida kuchli tor qovoq dalilini ham rad etgan.
Tor qovoq va o‘rta blink birga kuzatilganda yopiq holatga yo‘l ochildi.
Past blink, juda kichik ko‘z yoki zid geometriya noma’lum holatda qoladi.

USB service dump endi yuz/bosh holati, blink, qovoq nisbati, ko‘z kengligi
va qaysi bosqichda dalil yo‘qolganini ko‘rsatadi. Kamera tasviri saqlanmaydi,
bu raqamlar tarmoqqa uzatilmaydi.
