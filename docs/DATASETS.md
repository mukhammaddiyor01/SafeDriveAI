# Dataset va ko‘p burchakli model rejasi

Bu repository shaxsiy video yoki datasetni tarqatmaydi. Hozirgi APK tayyor ML Kit
modelidan foydalanadi. Quyidagi pipeline haqiqiy belgili eye crop datasetini kutadi;
hali uni o‘qitilgan yoki yo‘lda tekshirilgan model deb hisoblash mumkin emas.

## Manbalar

- [DROZY — University of Liège](https://www.drozy.ulg.ac.be/): uyquchanlik bilan bog‘liq
  video, vaqt va boshqa signallar. Oddiy RGB kamera bilan infraqizil yozuvlar orasida
  domain farqi bor. Datasetdagi uyquchanlik bahosi har bir kadrdagi ko‘z label’i emas.
- [DMD — Vicomtech](https://dmd.vicomtech.org/): turli sensor/burchaklar bilan haydovchi
  diqqat va holat tahlili. [Muallif repositorysi](https://github.com/Vicomtech/DMD-Driver-Monitoring-Dataset).
- [DAD — TUM](https://www.ce.cit.tum.de/mmk/dad/): old va tepa ko‘rinishlar, abnormal
  harakatlar tadqiqoti; ko‘z yumilishi uchun tayyor frame-level label deb qabul qilmang.

Yuklashdan oldin tegishli muallifning foydalanish va qayta tarqatish shartlarini o‘qing.
Ultralytics’ning ham AGPL/enterprise litsenziyasini tijoriy foydalanishdan oldin tekshiring.
Datasetlar avtomatik yuklanmaydi, ulardan o‘lchangan aniqlik natijalari bu loyihada yo‘q.

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

`label`: open/closed; `view`: front/left/right/up/down. Bu misol haqiqiy dataset emas.
Bir odamning barcha kun/video/kadrlariga bitta subject_id bering.
Deterministik subject split: train 70%, val 15%, test 15% taxminiy taqsimot.
Har bir splitda ikkala label bo‘lishi shart. Ko‘p subyekt kerak; aniq sonlar hash
taqsimotiga bog‘liq. Trening test ma’lumotiga tegmaydi. Baholash kamera burchagi bo‘yicha
closed recall va false positive rate chiqaradi.

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
