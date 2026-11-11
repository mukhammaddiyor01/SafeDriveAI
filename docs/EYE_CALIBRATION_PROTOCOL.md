# Yon ko‘z kalibrlash protokoli

SD-03, SD-05, SD-06. 2026-11-11, inferens natijasini ko‘rishdan oldingi reja.

- Train RT-GENE/RT-BENE sessiyalari 007, 009, 010, 012. Test sessiyalari
  011 va 013. Oldingi barcha manifestlar bilan subject ID va SHA-256 auditi.
- Train 007/009/012 yuz arxivining avval saqlangan 48 MiB qismi ishlatiladi.
  010/011/013 uchun 128 MiB qism. Faqat to‘liq PNG yozuvlari olinadi.
- Ikkala ko‘z uchun annotatorlar bir xil 0.0 yoki 1.0 yorliq bergan kadrlar.
  Noaniq 0.5 va ikki ko‘z yorlig‘i mos kelmagan kadrlar chiqariladi.
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
