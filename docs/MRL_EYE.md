# MRL Eye datasetini tayyorlash

Talab: **SD-06**. Manba: [MRL mualliflarining sahifasi](https://mrl.cs.vsb.cz/eyedataset.html).
Bu infraqizil ko‘z crop’lari ustida boshlang‘ich klassifikator tajribasi. Telefonda
RGB kamera, bosh/bo‘yin yoki ko‘p burchakli natijani o‘zi tasdiqlamaydi.

## Manba va foydalanish

Mualliflar sahifasi datasetni klassifikatorlarni o‘qitish va tekshirish uchun ochiq
yuklashga taqdim etadi. Sahifada alohida qayta tarqatish litsenziyasi ko‘rsatilmagan.
Dataset va undan o‘qitilgan weights faqat lokal tadqiqotda saqlanadi; ularga tijoriy
foydalanish yoki qayta tarqatish huquqi bor deb xulosa qilinmaydi.

- Rasmiy arxiv: https://mrl.cs.vsb.cz/data/eyedataset/mrlEyes_2018_01.zip
- 2026-11-06 yuklangan arxiv SHA-256:
  `17ff89928005cb6e63a53fe020e419060279ff59e57555cfa49693fc1865fdfc`.
- Manba tasvirlari, manifestlar, dublikatlar ro‘yxati va provenance: lokal `data/mrl/`.
  Bu papka Git’dan chiqarilgan; tasvirlar va weights repositoryga push qilinmaydi.

## Belgilarni o‘qish

Muallif fayl nomi tartibi: subject, image ID, gender, glasses, eye state,
reflections, lighting, sensor. `eye state`: `0=closed`, `1=open`.
Parser noto‘g‘ri format yoki noma’lum label’da xato beradi.

Kamera/bosh burilish burchagi bu belgilarda yo‘q. Shuning uchun `view=unknown`
saqlanadi; tasvirlarga o‘zboshimchalik bilan frontal/yonbosh belgisi berilmaydi.

## Import

Yangi importer 80/20 ajratadi; mavjud tajriba manifestlarini `--prior-manifest`
(repeatable) orqali auditga bering. [Amaldagi qoida](DATA_SPLIT_POLICY.md).
Quyidagi eski `prepared-clean` natijasi tarixiy holat; output ustidan yozilmaydi.

Arxivni lokal `data/mrl/raw/` ichiga ochgandan keyin loyiha ildizidan:

```bash
source .venv/bin/activate
python -m safedrive.controllers.import_mrl data/mrl/raw/mrlEyes_2018_01 \
  --archive data/mrl/mrlEyes_2018_01.zip --output data/mrl/prepared-clean --seed 42
```

Oldindan mavjud output papkasi ustidan yozilmaydi. Yangi import uchun yangi output
yo‘lini tanlang. Boshlang‘ich manba fayllarini o‘zgartirmang.

Importer har bir tasvir fayli uchun SHA-256 hisoblaydi. Bir xil fayl baytlari
uchragan guruhning **barcha** nusxalari splitdan oldin chiqariladi. Bu qoida
label qarama-qarshiligida yoki turli haydovchilarda takrorlangan fayllarni ham
bir splitda qoldirib yubormaydi. Chiqarilgan qatorlar `excluded_duplicates.csv`ga
yoziladi. Bu tekshiruv qayta kodlangan yoki o‘xshash kadrlarni aniqlash usuli emas.

`source.csv`, `manifest.csv`, `split_summary.csv` va `provenance.json` yaratiladi.
Split subject ID bo‘yicha deterministik; seed `42`. Tasvirlar splitlar orasida
tasodifiy kadrlar bo‘yicha aralashtirilmaydi.

## 2026-11-06 dastlabki import dalili — tarixiy train/val/test

84,898 manba tasviridan 76 ta takroriy-hash guruhiga tegishli tasvir chiqarildi.
Natija: **84,822 tasvir, 37 haydovchi**.

| Split | Haydovchi | Closed | Open | Jami |
|---|---:|---:|---:|---:|
| Train | 25 | 31,341 | 35,150 | 66,491 |
| Validation | 6 | 5,842 | 2,262 | 8,104 |
| Test | 6 | 4,726 | 5,501 | 10,227 |

Splitlar hajmi hash taqsimotiga bog‘liq. Bu natija label va manba tarkibini
tasvirlaydi; model aniqligi emas. MRL tasvirlaridagi yaqin kadrlar, infraqizil/RGB
farqi va kamera burchagi belgilarining yo‘qligi model hisobotida saqlanishi kerak.
