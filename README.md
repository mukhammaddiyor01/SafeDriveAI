# SafeDriveAI

Flutter + Kotlin + Python asosidagi Android haydovchi kuzatuvi prototipi.
Interfeys o‘zbekcha, kod MVC asosida sodda qatlamlarga ajratilgan.

## Loyiha maqsadi va ish qoidalari

- [PROJECT_BRIEF](docs/PROJECT_BRIEF.md) — asl talablar, texnologiyalar va qabul qilish mezonlari.
- [DEVELOPMENT_RULES](docs/DEVELOPMENT_RULES.md) — MVC, clean code, AI va kichik feat/fix commit qoidalari.
- [PROJECT_STATUS](docs/PROJECT_STATUS.md) — tayyor/qolgan ishlar va keyingi sessiyada davom ettirish.
- [AGENTS.md](AGENTS.md) — ushbu hujjatlarni har bir ish boshida o‘qish bo‘yicha agent ko‘rsatmasi.

## Hozirgi imkoniyatlar

- Old kamera orqali yuz, ko‘z ochiqligi va boshning burilish/egilish burchaklari.
- Ko‘zlar ketma-ket 1.5 soniya yumilganda ovozli signal va vibratsiya.
- 30 soniyalik oynada vaqtga asoslangan ko‘z yumilish ulushi; kamida 10 soniya
  ishonchli ko‘z kuzatuvidan so‘ng 35% chegarada ogohlantirish.
- Bosh uzoq burilgan/egilganida va yuz yoki ko‘zlar ko‘rinmay qolganda alohida ogohlantirish.
- Ilova ochiq paytda yoqiladigan, doimiy bildirishnomali camera foreground service.
- Qurilmaning Text-to-Speech ovozi bilan dam olish haqida eslatma va qisqa savol.
- Ixtiyoriy: USB orqali kompyuterdagi Ollama LLM bilan savol generatsiyasi.
- Python’da OpenCV/Pandas/PyTorch bilan ko‘z modeli o‘qitish va tekshirish,
  YOLO bilan bosh/yelka nuqtalarini ko‘rish uchun alohida tajriba vositasi.

## Aniq chegaralar

Bu yo‘l xavfsizligi uchun sertifikatlangan mahsulot emas. Hozirgi APK ko‘z holatini
**bundled ML Kit** modeli bilan baholaydi. Maxsus PyTorch/YOLO modeli hali Android
alarmiga ulanmagan. MRL infraqizil ko‘z tasvirlarida PyTorch klassifikatori o‘qitildi:
held-out accuracy 86.06%, ochiq ko‘zni xato closed deyish ulushi 22.92%.
Bu tadqiqot modeli telefon/ko‘p burchakli foydalanish uchun tasdiqlanmagan.
Batafsil: [MRL boshlang‘ich natijasi](docs/MRL_BASELINE.md).
YOLO COCO nuqtalari ko‘zning ochiq-yopiqligini yoki bo‘yinni bevosita o‘lchamaydi.

Yonboshda yuz/bosh aniqlanishi mumkin, ammo ML Kit ko‘z klassifikatsiyasi asosan
frontal yuzlar uchun ishlaydi. Ilova ±18° dan katta burilishda ko‘z holatini noma’lum
deb ko‘rsatadi; bunday kadrni "haydovchi hushyor" deb hisoblamaydi. Ko‘p burchakli,
tungi, ko‘zoynakli holatlar uchun dataset, model va qurilmada tekshirish kerak.

Ovozli savol haydovchini davom etishga undamaydi. Uyquchanlikda xavfsiz joyda to‘xtab,
dam olish kerak. Bu versiya mikrofonni ishlatmaydi va ovozli javobni tinglamaydi.

## Papkalar

```text
mobile/lib/models/           Flutter holat modeli
mobile/lib/views/            Flutter ekranlari
mobile/lib/controllers/      UI amallari va holat yangilanishi
mobile/lib/services/         Android platform channel adapteri
mobile/android/.../model/    Kotlin vaqtga asoslangan aniqlash mantig‘i
mobile/android/.../view/     CameraX preview
mobile/android/.../controller/ Kamera, foreground service, alarm, ovoz
ai/src/safedrive/models/     Dataset, PyTorch modeli, LLM adapteri
ai/src/safedrive/controllers/ O‘qitish, baholash, YOLO tajribasi, local API
ai/src/safedrive/views/      CLI
docs/                       Arxitektura, dataset va qurilma sinovi
scripts/                    APK build/install buyruqlari
```

## Android APK yaratish

Kerak: Flutter **3.47.9** (Dart 3.13.7), JDK 17, Android SDK 36, build tools va
platform-tools. Android Studio orqali SDK’ni o‘rnatish mumkin.
Ilova Android **8.0 / API 26+** uchun; Galaxy A51’da jismoniy sinov talab qilinadi.

Ushbu kompyuterda vositalar `.tools/` ichida joylashtirilgan bo‘lsa, loyiha ildizida
`source scripts/env.sh` orqali yo‘llarni terminal sessiyasiga ulang. Build/install
skriptlari buni avtomatik bajaradi. `.tools/` Git’ga yuborilmaydi.

```bash
cd mobile
flutter pub get
flutter analyze
flutter test
flutter build apk --release --target-platform android-arm64
```

Natija: `mobile/build/app/outputs/flutter-apk/app-release.apk`.
`./scripts/build-android.sh` bu APK’ni `releases/SafeDriveAI.apk` nomi bilan ham saqlaydi.
Bu release rejimida yig‘ilgan, developer imzoli sinov APK; Play Store uchun alohida signing kerak.

## Samsung Galaxy A51’ga USB orqali o‘rnatish

1. Telefon: Settings → About phone → Software information → Build number’ni 7 marta bosing.
2. Developer options → USB debugging’ni yoqing.
3. USB kabelni ulang, telefondagi kompyuter ruxsatini tasdiqlang.
4. Mac terminalida loyiha ichidan:

```bash
source scripts/env.sh
adb devices -l
./scripts/install-android.sh
```

Skript odatda bitta USB telefonni tanlaydi. Bir nechta telefon ulanganida
`adb devices -l` chiqishidan Galaxy A51 seriya raqamini olib,
`ANDROID_SERIAL=TELEFON_SERIYA_RAQAMI ./scripts/install-android.sh` buyrug‘ini bajaring.
Ilovada kamera va bildirishnoma ruxsatlarini bering. Alarm ovozini tekshiring.
Samsung batareya sozlamalarida ilovani sleeping/deep sleeping ro‘yxatidan chiqaring;
bu fon ishlashini kafolatlamaydi. Android’dagi Stop/Force stop ilovani to‘xtatadi.
Qurilma yoki process qayta ishga tushsa, kuzatuvni ilova ichidan qayta yoqing.

## Python AI

Yangi trening: **80% train / 20% test**. Test o‘qitish yoki model tanlashda
ishlatilmaydi. [Amaldagi dataset qoidasi](docs/DATA_SPLIT_POLICY.md).
Oldingi MRL natijalarini yangi 80/20 natijasi deb qabul qilmang.

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -e './ai[vision,assistant,dev]'
python -m unittest discover -s ai/tests -v
```

Haqiqiy datasetni tayyorlash/o‘qitish: [docs/DATASETS.md](docs/DATASETS.md).

```bash
python -m safedrive.views.cli data/eyes.csv --output artifacts/manifest.csv
python -m safedrive.controllers.train artifacts/manifest.csv --epochs 15 --pool-size 4
python -m safedrive.controllers.evaluate artifacts/manifest.csv artifacts/eye/eye_state.pt
python -m safedrive.controllers.pose --source 0
```

YOLO weights birinchi ishga tushishda yuklanadi. UI yoki alarm uchun Python server
talab qilinmaydi. PyTorch treningi va YOLO tajribasi telefon emas, kompyuterda ishlaydi.

## Ixtiyoriy mahalliy LLM

Kompyuterda Ollama va mos model mavjud bo‘lsa:

```bash
export OLLAMA_MODEL='siz-ornatgan-model-nomi'
uvicorn safedrive.controllers.assistant:app --host 127.0.0.1 --port 8765
```

Boshqa terminalda:

```bash
adb reverse tcp:8765 tcp:8765
curl -X POST http://127.0.0.1:8765/question
```

Ilova sozlamalarida "Kompyuterdagi AI yordamchi"ni yoqing. Telefonning kamera
tasviri kompyuterga yuborilmaydi. Model yo‘q yoki server ishlamasa, `source: offline`
qaytadi va oddiy savol ishlaydi. O‘zbekcha TTS ovozi bo‘lmasa, inglizcha eslatma
ishlaydi; inglizcha fallback paytida LLM so‘ralmaydi. LLM alarm qarorini chiqarmaydi.

## Tekshirish

```bash
cd mobile
flutter analyze
flutter test
cd android
./gradlew testDebugUnitTest
```

Qurilmada bajariladigan tekshiruvlar: [docs/DEVICE_TESTING.md](docs/DEVICE_TESTING.md).
Texnik qarorlar va rasmiy manbalar: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

Tekshirilgan natijalar va chegaralar: [docs/VERIFICATION.md](docs/VERIFICATION.md).
