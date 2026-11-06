# MRL boshlang‘ich tajriba protokoli — 2026-11-06

**Tarixiy tajriba:** ushbu natija/buyruqlar 80/20 talabi kiritilishidan oldingi
`2cabcca` va undan avvalgi train/val/test kodiga tegishli. Yangi ishlar uchun
[80/20 qoidasi](DATA_SPLIT_POLICY.md) amal qiladi. Ushbu natija qayta nomlanmaydi.

Talab: SD-06. Manba/import: [MRL_EYE.md](MRL_EYE.md).
Ushbu protokol haqiqiy test splitida inferensdan oldin qayd etilgan.

## O‘zgarmas ma’lumot

- Import: 84,822 tasvir / 37 haydovchi; dublikat guruhlaridagi 76 tasvir chiqarilgan.
- Seed 42; train/val/test haydovchilari mos ravishda 25/6/6, kesishmaydi.
- Dataset fingerprint: `4dc986bc73cce9488df02201980a9c26107ad2dfa62329f8507e0c08d7fdfb29`.
- Input: grayscale 64×64, OpenCV INTER_AREA, qiymatlar /255, NCHW.
- MRL `0=closed` → trening y=1; `1=open` → trening y=0. Chiqish closed-eye logit.
- Kamera burchagi annotatsiyasi yo‘q: view=unknown.

## Trening va tanlash

- Bir xil kichik uch convolution blokli EyeClassifier.
- A varianti: 1×1 adaptive pooling; B varianti: 4×4 adaptive pooling.
- Har ikkisi: 5 epoch, seed 42, batch 256, MPS, CPU threads 4.
- AdamW, learning rate 0.001, BCEWithLogitsLoss, augmentation yo‘q.
- Har bir run checkpointi faqat eng kichik validation loss bo‘yicha tanlanadi.
- A/B orasida ham eng kichik validation loss tanlanadi. Test natijasiga qarab
  arxitektura, epoch yoki threshold tanlanmaydi.
- Model card’da konfiguratsiya, source/manifest/checkpoint hash va vaqtlar saqlanadi.

## Yakuniy test

- Faqat tanlangan checkpointda: 10,227 test tasviri, 6 boshqa haydovchi.
- Sigmoid(closed logit) >= 0.5 → closed. Threshold oldindan 0.5 ga belgilangan.
- CPU batch 128 bilan baholash; inferensga trening yoki validation qatorlari kirmaydi.
- Umumiy confusion counts, accuracy, balanced accuracy, closed recall, precision va FPR.
- Sharoitlar kesimida: ko‘zoynak, yorug‘lik, refleksiya, sensor, subject va mavjud view.
- Ushbu protokolda ishlab chiqarishga chiqarish uchun sonli mezon belgilanmagan;
  natija tadqiqot uchun boshlang‘ich dalil sifatida qayd etiladi.

## Talqin chegarasi

Tasvir darajasidagi IR ko‘z klassifikatsiyasi uyquchanlik voqeasi, bosh/bo‘yin,
ko‘p burchak yoki RGB telefon kamerasi aniqligi emas. Test kadrlarida vaqt bo‘yicha
bog‘liqlik bo‘lishi mumkin; ularni mustaqil yo‘l hodisalari deb hisoblamang.
APK hozircha ML Kit’dan foydalanadi; ushbu tajriba avtomatik mobil almashtirish emas.
