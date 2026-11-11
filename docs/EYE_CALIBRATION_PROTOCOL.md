# Yon ko‘z kalibrlash protokoli

SD-03, SD-05, SD-06. 2026-11-11, inferens natijasini ko‘rishdan oldingi reja.

- Train RT-GENE/RT-BENE sessiyalari 007, 009, 010, 012. Test sessiyalari
  011 va 013. Oldingi barcha manifestlar bilan subject ID va SHA-256 auditi.
- Train 007/009/012 yuz arxivining avval saqlangan 48 MiB qismi ishlatiladi.
  010/011/013 uchun 128 MiB qism. Faqat to‘liq PNG yozuvlari olinadi.
- Rasmiy CSV’dagi left/right yorlig‘i aynan o‘sha ko‘zning xususiyatiga
  bog‘lanadi. Faqat 0.0/1.0 ishlatiladi; noaniq 0.5 chiqariladi. Bitta ko‘z
  yorlig‘ini ikkinchisiga nusxalash mumkin emas. Import xatosi inferensdan
  oldin tuzatildi; qolgan protokol o‘zgarmadi.
- Muzlatilgan Google Face Landmarker float16/1 dan blink va qovoq oralig‘i
  olinadi. Bu modelning vaznlari train yoki test asosida o‘zgartirilmaydi.
- Avtomatik bosh transformidan |yaw| ≤45° kadrlar olinadi. Front ≤18°,
  qolganlari tasvirdagi chap/o‘ng ko‘rinishga ajratiladi. Bu burchaklar
  qo‘lda o‘lchangan ground truth emas, avtomatik model bahosi.
- Har yo‘nalishda SHA-256 va seed 42 tartibida 80/20 tanlanadi. Subyektlar
  aralashmaydi. Yangi 20% test vazn, epoch yoki threshold tanlamaydi.
- Ikki xususiyatli PyTorch logistik model. Faqat train mean/std, train’dan
  hisoblangan positive weight, Adam 0.025, seed 42, to‘liq batch 400 epoch.
- Oxirgi checkpoint 0.50 chegarada bir marta test qilinadi.
- Mobil integratsiya uchun har ikki yon guruhda kamida 5 closed va 20 open,
  closed recall ≥80%, open false-positive rate ≤15% talab qilinadi.
  Blink vazni musbat va gap vazni manfiy bo‘lishi kerak.
- Faqat qabul qilingan model foizlik eye-open score va qaror uchun ulanadi.
  Bu anatomik foiz yoki klinik xavfsizlik kafolati emas.
- Kamera-relative ±45° talabini telefonda jonli o‘lchash alohida qoladi.

## Yakuniy natija — 2026-11-11

3 552 train va 888 test (aniq 80/20). 400 epoch yakunida yopiq-ko‘z recall
89.74%, false-positive rate 9.78%, precision 29.66%. Chap yon: 3/4 yopiq
aniqlandi (75%), o‘ng yon: yopiq test namunasi yo‘q. **Integratsiya rad etildi**:
ikki yon uchun oldindan belgilangan minimum va recall bajarilmadi. Bu testga
qarab threshold yoki epoch o‘zgartirilmadi. Keyingi tajribaga yangi test kerak.

[Hisobot va hashlar](experiments/eye-calibration-2026-11-11.json). Rasm/model
fayllari repositoryga qo‘shilmaydi. Mobil ilovada avvalgi ehtiyotkor blink/gap
qarori qoladi; ushbu yangi koeffitsiyentlar ishlatilmaydi. Importda bitta left
ko‘z yorlig‘i butun yuz yoki boshqa ko‘z uchun ko‘chirilmaydi.
