# MRL ko‘z klassifikatori — haqiqiy boshlang‘ich natija

**Tarixiy tajriba:** ushbu natija/buyruqlar 80/20 talabi kiritilishidan oldingi
`2cabcca` va undan avvalgi train/val/test kodiga tegishli. Yangi ishlar uchun
[80/20 qoidasi](DATA_SPLIT_POLICY.md) amal qiladi. Ushbu natija qayta nomlanmaydi.

Sana: **2026-11-06**. Talab: **SD-06**. [Oldindan qayd etilgan protokol](MRL_PROTOCOL.md).
[Import/manba](MRL_EYE.md). [Aniq agregat JSON hisobot](experiments/mrl-eye-2026-11-06.json).

## Trening va model tanlash

Ikkala variant ham 66,491 train tasvirida 5 epoch o‘qitildi: seed 42, batch 256,
PyTorch 2.15.0 / MPS, AdamW 0.001. 8,104 validation tasviri 6 boshqa haydovchidan.

| Pooling | Eng yaxshi epoch | Validation loss | Tanlov |
|---|---:|---:|---|
| 1×1 | 4 | 0.565237 | Tanlanmadi; test inferensi bajarilmadi |
| 4×4 | 4 | 0.169402 | Validation bo‘yicha tanlandi |

Test natijasi arxitektura, epoch yoki threshold tanlashga ishlatilmadi.

## Held-out test: 6 haydovchi, 10,227 tasvir

Threshold oldindan 0.5 ga belgilangan. CPU batch 128 bilan faqat tanlangan
checkpoint baholandi. Ijobiy sinf — **closed**.

| Ko‘rsatkich | Natija |
|---|---:|
| Accuracy | 86.06% |
| Balanced accuracy | 86.79% |
| Closed recall | 96.51% |
| Closed precision | 78.34% |
| Ochiq ko‘zni xato closed deyish (FPR) | 22.92% |

TP=4561, FN=165, FP=1261, TN=4240.

## Sharoitlar kesimidagi muammo

| Sharoit | Tasvir | Accuracy | Closed recall | FPR |
|---|---:|---:|---:|---:|
| Ko‘zoynaksiz | 7541 | 90.23% | 94.87% | 12.68% |
| Ko‘zoynak bilan | 2686 | 74.35% | 99.12% | 77.98% |
| Yomon yoritish | 5744 | 82.05% | 96.74% | 28.67% |
| Yaxshi yoritish | 4483 | 91.19% | 96.27% | 14.17% |
| Katta refleksiya | 864 | 40.97% | 99.04% | 77.56% |

Bu guruhlar o‘zaro kesishadi; sababiy xulosa chiqarilmaydi. Testda faqat
sensor=01 kuzatildi, boshqa sensorlar bo‘yicha test dalili yo‘q.

## Qaror va keyingi ish

Ochiq ko‘zlardagi xato ulushi, ayniqsa ko‘zoynak/refleksiyada, yuqori.
Model **Android alarmiga ulanmagan** va tayyor haydovchi monitoring modeli deb belgilanmaydi.
Bu raqamlar IR ko‘z crop klassifikatsiyasi; uyquchanlik hodisasi, bo‘yin/bosh,
ko‘p burchak yoki RGB telefon kamerasi aniqligi emas. FPS va alarm kechikishi bu tajribada o‘lchanmagan.

Keyingi tajriba validation bilan yaxshilanishi, sifat/ko‘rinmas ko‘z holatini ajratishi
va yangi, reprezentativ RGB/ko‘p burchakli ma’lumot bilan baholanishi kerak.
Bu test natijasiga moslab threshold yoki trening tanlab, uni yana yangi held-out
natija deb taqdim etmang. Yakuniy telefon dalili uchun yangi mustaqil yozuvlar kerak.

## Lokal natijalar va qayta bajarish

- Tanlangan weights: `artifacts/mrl-spatial-2026-11-06/eye_state.pt`.
- Checkpoint SHA-256: `8c7e7616568618cb49b3d6dedb8bbcc868382aa9ab158459c8fb7c0d42800aac`.
- Model card va epoch tarixi shu run papkasida. To‘liq prediction/sharoit/subject
  hisobotlari uning `evaluation/` papkasida. Dataset va weights Git’ga yuborilmagan.
- Yangi run uchun bo‘sh output yo‘lini ishlating; mavjud tajriba ustidan yozilmaydi.

```bash
source .venv/bin/activate
python -m safedrive.controllers.train data/mrl/prepared-clean/manifest.csv \
  --output artifacts/mrl-reproduce --epochs 5 --seed 42 --device mps \
  --batch-size 256 --threads 4 --pool-size 4
python -m safedrive.controllers.evaluate data/mrl/prepared-clean/manifest.csv \
  artifacts/mrl-reproduce/eye_state.pt --output artifacts/mrl-reproduce/evaluation \
  --threshold 0.5 --batch-size 128 --threads 4
```

Boshqa qurilmada `--device cpu` yoki mos backendni tanlang. Seed va konfiguratsiya
qayd etiladi; turli backend/versiyalarda bit darajasida bir xil natija va’da qilinmaydi.
