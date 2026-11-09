# Yonboshdagi ko‘z holati tajribasi

SD-05 va SD-06. RT-BENE ochiq/yopiq ko‘z yorliqlari va ko‘z kesmalari
RT-GENE yuz kadrlari bilan sessiya va kadr raqami orqali bog‘lanadi.
RT-GENE ko‘zoynaksiz qismida qo‘lda belgilangan bosh burchagi yo‘q.
Chap/o‘ng guruhini YuNet topgan ikki ko‘z va burun belgilaydi. Burunning
ko‘zlar orasidagi o‘q bo‘yicha markazdan siljishi ko‘zlar orasidagi masofaning
kamida 18% bo‘lsa, kadr yonbosh guruhiga kiradi. Chap va o‘ng tasvirga
nisbatan beriladi. Bu o‘lchov gradus yoki telefon tutqichi joyi emas.

## Oldindan belgilangan protokol

- Train sessiyalari 007, 012, 015, 016. Test sessiyalari 003 va 004.
  RT-GENE hujjatidagi 014/002 va 015/005 takroriy qatnashchilar splitni kesib o‘tmaydi.
- Har sessiya uchun rasmiy RT-GENE yuz arxivining birinchi 48 MiB qismidagi
  to‘liq PNG yozuvlari ishlatiladi. RT-BENE noaniq 0.5 yorliqlari chiqariladi.
- Avvalgi MRL va RT-BENE manifestlari bilan test SHA-256 va subyekt ID bo‘yicha
  solishtiriladi. Bir xil mazmundagi ko‘z tasvirlari ikkala splitdan chiqariladi.
- Har yo‘nalishda ko‘z yorlig‘iga qaramasdan seed 42 va SHA-256 tartibida
  namuna olinadi. Har bitta test tasviriga to‘rtta train tasviri ajratiladi.
- PyTorch EyeClassifier 4×4 spatial pooling bilan, 10 epoch, batch 64, seed 42,
  AdamW va faqat train sinflari sonidan hisoblangan closed vazni bilan ishlaydi.
  Oxirgi checkpoint 0.50 chegarada bir marta baholanadi.
- Testda har tomon uchun yopiq ko‘zni topish ulushi va ochiq ko‘zni xato
  yopiq deyish ulushi alohida beriladi. Bu tayyor ko‘z kesmasining holatini
  tekshiradi. To‘liq kamera kadridan ko‘z joyini topish va alarm alohida sinovdir.

Manbalar: [RT-BENE ko‘z yorliqlari](https://zenodo.org/records/3685316),
[RT-GENE yuz tasvirlari](https://zenodo.org/records/2529036),
[OpenCV YuNet](https://github.com/opencv/opencv_zoo/tree/main/models/face_detection_yunet).
