# Chapdan o‘ngga ko‘z holati protokoli

SD-05 va SD-06, 2026-11-11. Yangi tajriba front, chap va o‘ng guruhlarda
alohida hisobot beradi. Oldingi test natijasi yangi modelni tanlashga ishlatilmaydi.

- Train sessiyalari 007, 009, 012. Yangi test sessiyalari 001 va 008.
- RT-GENE har sessiyadagi birinchi 48 MiB yuz arxivi va RT-BENE ko‘z
  yorliqlari ishlatiladi. Yon guruh YuNet burun/ko‘z geometriyasidan olinadi.
  Yo‘nalish tasvirga nisbatan beriladi, o‘lchangan yaw gradusi emas.
- Har yo‘nalishda seed 42 va SHA-256 bo‘yicha yorliqqa qaramasdan 80/20
  namuna olinadi. Bir subyekt ikkala splitga tushmaydi. Oldingi barcha
  mahalliy manifestlar auditga beriladi.
- Har guruhda ochiq va yopiq ko‘z bo‘lishi treningdan oldin tekshiriladi.
- PyTorch EyeClassifier 4×4 pooling, AdamW, batch 64, 15 epoch, seed 42.
  Closed vazni faqat train ichidagi sinflardan hisoblanadi.
- Oxirgi checkpoint 0.50 chegarada bir marta test qilinadi.
- Bu ko‘z kesmasining holatini baholaydi. Android Face Landmarker tayyor
  Google modeli bo‘lib, yangi PyTorch checkpointidan alohida tekshiriladi.

## Yakuniy natija

1 704 train tasviri uchta subyektdan va 426 yangi test tasviri ikkita
subyektdan ajratildi. 15-epoch checkpoint bir marta 0.50 chegarada baholandi.

| Ko‘rinish | Test tasviri | Yopiq ko‘zni topdi | Ochiq ko‘zni xato yopiq dedi |
|---|---:|---:|---:|
| Chap | 89 | 0/1 | 50/88 |
| Old | 279 | 1/15 | 145/264 |
| O‘ng | 58 | 0/8 | 3/50 |

Umumiy accuracy 48,12%, yopiq ko‘z recall 4,17%. Bu checkpoint ko‘z
yumilishini ishonchli topmadi. U mobil inferensga ulanmaydi.
Test natijasiga qarab model, epoch yoki threshold qayta tanlanmadi.
Keyingi modelga yangi mustaqil test subyektlari kerak.
[Agregat hisobot](experiments/bilateral-eye-2026-11-11.json).
