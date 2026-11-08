# SafeDriveAI — loyiha maqsadi va kelishilgan talablar

Bu hujjat foydalanuvchining loyiha bo‘yicha asosiy talablarini saqlaydi. Joriy
prototipning cheklovlari yakuniy talablarni bekor qilmaydi. Talablarni foydalanuvchi
ko‘rsatmasisiz qisqartirmang; amalga oshmaganini tayyor deb yozmang.

## Maqsad

SafeDriveAI — kamera orqali haydovchining uyquchanlik alomatlarini kuzatadigan,
vaqtida alarm beradigan va ovozli yordamchi orqali murojaat qiladigan Android ilova.
Ilova haydovchiga xavfsiz joyda to‘xtab, dam olish zarurligini bildiradi. Hushyor
qolishni kafolatlash yoki suhbatni dam olish o‘rniga qo‘yish mahsulot va’dasi emas.

## Kelishilgan asos

- Nomi: **SafeDriveAI**.
- Platforma: Android; USB kabel orqali telefonga o‘rnatiladigan APK.
- Birinchi jismoniy sinov qurilmasi: **Samsung Galaxy A51**. Uning Android versiyasi
  hali foydalanuvchi tomonidan bildirilmagan.
- Interfeys: **Flutter / Dart**; kamera va Android fon xizmati: **Kotlin**.
- AI tayyorlash va tajribalar: **Python, OpenCV, Pandas, YOLO, PyTorch**.
- Interfeys va ovozli yordamchi tili: **inglizcha**.
- Ovozli AI yordamchi: **ChatGPT / OpenAI Responses API**; aniqlash va alarm mustaqil ishlaydi.
- Dataset: **80% train / 20% avval ishlatilmagan test**; haydovchilar va tasvir nusxalari aralashmasin.
- Arxitektura: **MVC**. Kod sodda, clean va odam o‘qib tushunadigan bo‘lsin.
- Repository: https://github.com/mukhammaddiyor01/SafeDriveAI
- Lokal loyiha: `/Users/mukhammaddiyor/Desktop/Projects/SafeDriveAI`.

## Funksional talablar

| ID | Yakuniy talab | Qabul qilish uchun dalil |
|---|---|---|
| SD-01 | Kamera bilan haydovchini kuzatish | Telefonda kamera, preview va ketma-ket kadr tahlili ishlaydi |
| SD-02 | Yuz, ko‘z, bosh va bo‘yin atrofidagi holatni baholash | Har bir kuzatiladigan belgi va uning ishonchliligi ko‘rsatiladi; taxminiy bo‘yin nuqtasi bevosita o‘lchov deb taqdim etilmaydi |
| SD-03 | Uyquchanlik alomatlarida alarm berish | Uzoq ko‘z yumilishi/bosh holati voqealarida alarm kechikishi va yolg‘on signallar o‘lchanadi |
| SD-04 | Fon rejimida ham ishlash | Ilova fon va ekran o‘chgan holatda jismoniy telefonda tekshiriladi; doimiy bildirishnoma va Stop mavjud |
| SD-05 | Old, yonbosh va turli burchaklardan aniqlash | Burchaklar bo‘yicha alohida dataset baholashi va telefon sinovi mavjud |
| SD-06 | AI datasetlardan foydalanish | Dataset manbasi, 80/20 split, testning ishlatilmaganligi va yakuniy test natijalari qayd etiladi |
| SD-07 | Haydovchiga ovozli murojaat va savollar | Mahalliy alarmdan keyin tiklanish dalili bo‘lsa inglizcha suhbat boshlanadi. Telefon javobni tinglaydi va SafeDriveAI uchun saqlangan OpenAI suhbatidan foydalanadi. |
| SD-08 | USB orqali o‘rnatish | APK yig‘iladi, imzosi tekshiriladi va Galaxy A51’ga adb orqali o‘rnatiladi |
| SD-09 | MVC va tushunarli kod | View, controller va domain model vazifalari ajratilgan; biznes mantig‘i alohida sinovdan o‘tadi |
| SD-10 | Kichik mantiqiy commitlar | Har bir commit bitta vazifani bajaradi; faqat `feat:` va `fix:` prefikslari ishlatiladi |

## Texnologiyalarning vazifasi

| Texnologiya | Loyihadagi vazifa |
|---|---|
| Flutter / Dart | Ekranlar, holat ko‘rinishi va foydalanuvchi amallari |
| Kotlin / CameraX | Kamera kadrlarini olish, foreground service, signal, vibratsiya va Android bilan aloqa |
| Python | Dataset, o‘qitish, baholash va mahalliy yordamchi server |
| OpenCV | Tasvir/video o‘qish, crop, preprocessing va vizual tekshirish |
| Pandas | Dataset manifestlari, tahlil va baholash hisobotlari |
| YOLO | Bosh/yuz/yelka bilan bog‘liq detection yoki pose tajribalari; ko‘z ochiqligi uchun alohida dalil kerak |
| PyTorch | Maxsus modelni o‘qitish, tekshirish va keyingi mobil integratsiyaga tayyorlash |
| LLM | Qisqa ovozli murojaat va suhbat; alarm qarorini almashtirmaydi |
| ML Kit | Joriy APK’dagi boshlang‘ich yuz/ko‘z tahlili; maxsus ko‘p burchakli modelning tayyorligini anglatmaydi |

## Yo‘nalishni saqlash

- Har bir yangi ish yuqoridagi talablarning biriga xizmat qilsin. Aloqasiz yangi
  mahsulot funksiyalarini ushbu vazifaga qo‘shmang.
- Kuzatilmagan ko‘z yoki yo‘qolgan kadrni hushyorlik dalili deb hisoblamang.
- Ko‘p burchakli aniqlash maqsadini faqat frontal yuz bilan almashtirib qo‘ymang;
  ko‘rinmaydigan belgilar uchun noaniqlikni ko‘rsating va qolgan ishni qayd eting.
- Dataset tayyorlash kodini haqiqiy model treningi deb, emulyatorni esa Galaxy A51
  sinovi deb taqdim etmang.
- Joriy tayyorlik: [PROJECT_STATUS.md](PROJECT_STATUS.md). Amaliy qoidalar:
  [DEVELOPMENT_RULES.md](DEVELOPMENT_RULES.md).
