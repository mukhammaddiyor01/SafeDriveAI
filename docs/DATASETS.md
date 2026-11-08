# Dataset va ko‘p burchakli model rejasi

Bu repository shaxsiy video yoki datasetni tarqatmaydi. Hozirgi APK tayyor ML Kit
modelidan foydalanadi. Pipeline belgili eye crop datasetini qabul qiladi;
haqiqiy MRL eye-crop tajribasi [MRL_BASELINE.md](MRL_BASELINE.md)da qayd etilgan.
Bu boshlang‘ich model RGB/ko‘p burchakli yo‘l sinovidan o‘tmagan va APK’ga ulanmagan.

## Manbalar

- [MRL Eye](https://mrl.cs.vsb.cz/eyedataset.html): ochiq/yopiq ko‘z uchun IR tasvirlar.
  [Import va dublikatlar](MRL_EYE.md), [oldindan qayd etilgan protokol](MRL_PROTOCOL.md),
  [haqiqiy held-out natija](MRL_BASELINE.md). Bosh/kamera burchagi annotatsiyasi yo‘q.

- [DROZY — University of Liège](https://www.drozy.ulg.ac.be/): uyquchanlik bilan bog‘liq
  video, vaqt va boshqa signallar. Oddiy RGB kamera bilan infraqizil yozuvlar orasida
  domain farqi bor. Datasetdagi uyquchanlik bahosi har bir kadrdagi ko‘z label’i emas.
- [DMD — Vicomtech](https://dmd.vicomtech.org/): turli sensor/burchaklar bilan haydovchi
  diqqat va holat tahlili. [Muallif repositorysi](https://github.com/Vicomtech/DMD-Driver-Monitoring-Dataset).
- [DAD — TUM](https://www.ce.cit.tum.de/mmk/dad/): old va tepa ko‘rinishlar, abnormal
  harakatlar tadqiqoti; ko‘z yumilishi uchun tayyor frame-level label deb qabul qilmang.

Yuklashdan oldin tegishli muallifning foydalanish va qayta tarqatish shartlarini o‘qing.
Ultralytics’ning ham AGPL/enterprise litsenziyasini tijoriy foydalanishdan oldin tekshiring.
DROZY/DMD/DAD hali yuklanmagan yoki baholanmagan; MRL uchun lokal tadqiqot bajarilgan.
Datasetlar va weights repositoryga qayta tarqatilmaydi.

## CSV formati

`image_path` — CSV fayliga nisbatan yo‘l. Kirish tasviri **ko‘z crop’i**, to‘liq haydovchi
tasviri emas. Ko‘rinmaydigan ko‘zni taxmin bilan `closed` belgilamang; o‘qitishdan chiqarib,
alohida coverage hisobotiga oling.

```csv
image_path,subject_id,label,view
eyes/driver01_001.png,driver01,open,front
eyes/driver01_002.png,driver01,closed,front
eyes/driver02_001.png,driver02,open,left
```

`label`: open/closed; `view`: front/left/right/up/down/unknown. Manbada burchak bo‘lmasa unknown ishlatiladi. Bu misol haqiqiy dataset emas.
Bir odamning barcha kun/video/kadrlariga bitta subject_id bering.
Amaldagi split: **80% train / 20% test**, rasm soni butun songa yaxlitlanadi.
Bir haydovchi ikkala splitga tushmaydi; butun subjectlar bilan nisbat chiqmasa xato
qaytariladi. Har bir splitda ikkala label bo‘lishi shart. Eski 70/15/15 hash tartibi
faqat tarixiy tajribaga tegishli. [80/20 va foydalanish auditi](DATA_SPLIT_POLICY.md). Ko‘p subyekt kerak; aniq sonlar hash
taqsimotiga bog‘liq. Trening test ma’lumotiga tegmaydi. Baholash kamera burchagi bo‘yicha
closed recall va false positive rate chiqaradi.

## Markaziy telefon tutqichi va navigator

To‘xtab turgan avtomobilda har bir haydovchini markazdagi past va baland
tutqichdan yozing. Telefon tomonga qaramasdan yo‘l tomonga qarash, bosh tik
holda navigatorga qisqa qarash, boshni egish va ko‘zni yumish holatlarini
alohida belgilang. Kamera chap yoki o‘ng tomondan qarashini `view` da saqlang.
Ko‘z crop’idan tashqari, bosh holatini o‘qitish uchun mos to‘liq yuz kadrini
va yozuv vaqtini ham manba annotatsiyasida saqlang.

Ko‘z CSV’iga uchta ustun qo‘shiladi.

- `camera_mount` qiymatlari `center_low`, `center_high` yoki `front_reference`.
- `head_posture` qiymatlari `upright` va `down`. Boshni yo‘lga nisbatan belgilang.
- `gaze_target` qiymatlari `road`, `navigation` va `unknown`.
  Yopiq ko‘z uchun `unknown` qo‘llanadi.

Har ikkala split har ikki markaziy balandlikda to‘rtta holatni qamraydi.
Bosh tik holda yo‘lga qarash, bosh tik holda ko‘z yumilishi, bosh tik holda
navigatorga qarash va ochiq ko‘z bilan bosh egilishi kerak. Shu shartlar
bajarilmasa manifest rad etiladi. Avval ishlatilgan manifestlarni auditga kiriting.

Baholashdagi `metrics_by_mount.csv` fayli ko‘z ochiq yoki yopiqligini
tutqich, kamera burchagi, bosh holati va nigoh kesimida ko‘rsatadi. Har bir qatorda
namunalar va haydovchilar soni beriladi. `unknown` alohida ko‘rsatiladi. Bosh va nigoh bu hisobotda
annotatsiya sifatida ishlatiladi. Ularni topadigan model alohida o‘qitiladi va baholanadi.

## Keyingi modelni qabul qilish mezonlari

1. Telefon tutqichi burchaklari, teri rangi, yoritish, ko‘zoynak va tun holatlarini qamrash.
2. Video voqealari darajasida alarm kechikishi, o‘tkazib yuborilgan hodisalar,
   soatiga yolg‘on alarm va kuzatuv yo‘q vaqt ulushini o‘lchash.
3. Ko‘z modeli ko‘rinmagan ko‘zdan xulosa chiqarmasligi uchun sifat modeli/landmark
   tekshiruvini qo‘shish. Yuqori ishonchsizlikda limited holati saqlanadi.
4. YOLO bosh, yuz va yelka crop’larini tajribada topadi; COCO ko‘z nuqtalari ko‘z
   ochiqligini aniqlamaydi, alohida ko‘z modeli kerak. Bo‘yin nuqtasi COCO’da yo‘q.
5. Tasdiqlangan modelni LiteRT yoki ONNX runtime formatiga export qilib, aynan bir xil
   preprocessing va natija pariteti bilan Android’ga ulash. Hozirgi APK’da bu adapter yo‘q.
6. Galaxy A51’da FPS, quvvat, temperatura, signal kechikishi va ekran o‘chgan holatni sinash.

Unit testlarda yoki pipeline smoke sinovida ishlatilgan sun’iy tasvirlar aniqlik dalili emas.
