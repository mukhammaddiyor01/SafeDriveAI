# RT-BENE 80/20 tajribasi

2026-11-07 · SD-06

Mahalliy tadqiqot uchun RT-BENE ko‘z kesmalaridan besh subyekt tanlandi. Mualliflarning training guruhidan 007, 009 va 012 treningga, validation guruhidan 015 va 016 loyiha testiga ajratildi. Datasetning foydalanish shartlari va yorliq tavsifi manbada berilgan. [RT-BENE, Cortacero, Fischer va Demiris](https://zenodo.org/records/3685316)

Yuklangan 11 fayl muallif e’lon qilgan MD5 qiymatlariga mos keldi. Import noaniq 0.5 yorliqlarni chiqaradi va fayl xeshlari hamda subyekt IDlarini avvalgi MRL manifesti bilan solishtiradi. Moslik topilmadi. Tanlangan besh subyektdagi mavjud yozuvlardan seed 42 bilan fayl xeshiga asoslangan namuna olindi. Testda manbadagi ko‘z ochiqligi yorliqlariga qarab namuna tanlanmadi.

| Guruh | Tasvir | Subyekt | Yopiq ko‘z |
|---|---:|---:|---:|
| Train | 7 000 | 3 | 217 |
| Test | 1 750 | 2 | 18 |

PyTorch modeli MPS’da 10 epoch o‘qitildi. Yopiq sinf vazni train to‘plamidagi 6 783 ochiq va 217 yopiq tasvirdan hisoblandi. 10-epoch checkpointi oldindan belgilangan 0.5 chegarada bir marta baholandi.

| Test natijasi | Qiymat |
|---|---:|
| Accuracy | 64.40% |
| Balanced accuracy | 73.77% |
| Yopiq ko‘z recall | 83.33% (15/18) |
| Ochiq ko‘zni xato yopiq deyish | 35.80% (620/1 732) |
| Yopiq ko‘z precision | 2.36% (15/635) |

Natija ko‘zoynaksiz ikki test subyektining ko‘z kesmalariga tegishli. Kamera burchagi annotatsiyasi mavjud emas. Yuqori xato ulushi va kam sonli yopiq ko‘z namunasi sabab model tadqiqot bosqichida qoladi. Galaxy A51’dagi dalil uchun telefon yozuvlari kerak.

Manifest `data/rt-bene/pilot/manifest.csv`da, model va mahalliy hisobot `artifacts/rt-bene-pilot-2026-11-07/`da. Keyingi tajribalarda RT-BENE pilot manifestini MRL manifesti bilan birga foydalanish tarixiga kiriting. [Agregat hisobot](experiments/rt-bene-pilot-2026-11-07.json)

Import yangi papkada qayta ishga tushirilganda manifest baytma-bayt mos keldi. Python suite’dagi 33 ta test o‘tdi.
