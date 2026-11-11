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
