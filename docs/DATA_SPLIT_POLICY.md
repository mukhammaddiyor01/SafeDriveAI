# 80% train / 20% test — amaldagi qoida

Foydalanuvchi talabi, 2026-11-06. SD-06 uchun yangi treninglarda ushbu hujjat
oldingi train/validation/test tajriba tartibidan ustun turadi.

## Ajratish va foydalanish

- Rasmlarning 80% qismi train, 20% qismi test bo‘ladi. Test soni butun rasmga
  yaxlitlanadi: `round(total * 0.20)`; qolgan rasmlar train’ga kiradi.
- Bir haydovchi ikkala splitga tushmaydi. Butun haydovchilar bilan shu nisbatni
  hosil qilib bo‘lmasa, noto‘g‘ri nisbatga o‘tish o‘rniga xato qaytariladi.
- Bir xil fayl/hash nusxalari splitlar orasida bo‘lmasin. MRL importida takroriy
  guruhlarning barcha nusxalari chiqariladi.
- Model faqat train’ning 80% tasvirlari bilan o‘qitiladi. Test model vaznlarini,
  epoch, arxitektura yoki thresholdni tanlashda ishlatilmaydi.
- Hozirgi yangi trening alohida validation ajratmaydi: oldindan belgilangan epoch
  soni yakunida oxirgi checkpoint saqlanadi. Test inferensi treningdan keyin bajariladi.
- Seed va rezervlangan manifest saqlanadi. Mavjud manifest ustidan yozish yoki
  testni qayta aralashtirish rad etiladi.

## Haqiqatan ishlatilmagan test

Avvalgi train, validation va test uchun ishlatilgan barcha mavjud manifestlarni
`--prior-manifest` bilan ko‘rsating. Flag bir necha marta berilishi mumkin.
Fayl yo‘li, SHA-256 yoki subject ID oldingi foydalanishga mos kelsa `used` belgilanadi.
Eski tasvirlar o‘chirilgan bo‘lsa ham ularning CSV/hash tarixi orqali tekshiriladi.

```bash
python -m safedrive.views.cli data/new/source.csv \
  --output data/new/prepared/manifest.csv --seed 42 \
  --prior-manifest data/previous/manifest.csv
```

`prior_usage=used` bo‘lgan test namunasi yangi trening va baholashni to‘xtatadi.
Oldingi tarix berilmasa, audit global tarixiy yangilikni tasdiqlamaydi; bu holat
`.split.json`da yoziladi. Uni "butunlay yangi test" deb taqdim etmang.
`unseen_in_supplied_history` faqat ko‘rsatilgan tarixda topilmaganini anglatadi.

## MRL auditining joriy holati

84,822 rasmdan train=67,858 (30 haydovchi), test=16,964 (7 haydovchi) ajratildi.
Bu butun rasmga yaxlitlangan 80/20: test ulushi 19.9995%.

Ammo 16,964 test rasmining barchasi oldingi MRL tajribasida ishlatilgan.
Shuning uchun yangi haqiqiy trening yoki test inferensi bajarilmadi. Ishlatilmagan
rasmlar/haydovchilar bilan yangi ma’lumot kerak. [Audit dalili](experiments/mrl-80-20-audit-2026-11-06.json).
Lokal manifest: `data/mrl/prepared-80-20-final/manifest.csv`.

Oldingi [MRL natijalari](MRL_BASELINE.md) o‘z tarixiy train/val/test protokoliga
tegishli. Ular 80/20 natijasi deb qayta nomlanmaydi. Eski uch splitli manifestlar
faqat tarixiy baholash uchun o‘qilishi mumkin; yangi train buyrug‘i ularni rad etadi.

## Tekshiruv

33 ta Python testi o‘tdi. Integratsion test model faqat train tasvirlarini yuklashi,
ikki epochda ham testni ko‘rmasligi, oxirgi epoch saqlanishi, oldingi foydalanish
aniqlanishi va 70/30 qilib o‘zgartirilgan manifest rad etilishini tekshiradi.
Haqiqiy MRL manifestida ham oldingi foydalanish himoyasi ishga tushgani tekshirildi.
APK va oldingi model natijalari o‘zgartirilmadi.
