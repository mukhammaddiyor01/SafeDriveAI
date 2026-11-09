# SafeDriveAI

Flutter + Kotlin + Python asosidagi Android haydovchi kuzatuvi prototipi.
Interfeys inglizcha, kod MVC asosida sodda qatlamlarga ajratilgan.

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
dam olish kerak. Ovozli yordamchi yoqilganida telefon savoldan keyin qisqa javobni tinglaydi.

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

## ChatGPT ovozli yordamchisi

Python serverini kompyuterda ishga tushiring. `OPENAI_API_KEY` kalitini
kompyuter muhitiga xavfsiz kiriting va `OPENAI_MODEL` uchun API loyihangizda
mavjud model ID’sini belgilang. Kalit kompyuterda qoladi.

```bash
source scripts/env.sh
source .venv/bin/activate
uvicorn safedrive.controllers.assistant:app --host 127.0.0.1 --port 8765
```

Boshqa terminalda ADB tunnelini yoqing. USB qurilmada:

```bash
source scripts/env.sh
adb -d reverse tcp:8765 tcp:8765
```

Wireless debugging ishlatilsa, `adb devices -l` ro‘yxatidan qurilma ID’sini olib,
`adb -s 'QURILMA_ID' reverse tcp:8765 tcp:8765` ni bajaring.

Telefonda **Settings → Connect ChatGPT** ni bosing. Kuzatuvni boshlaganda
mikrofonga ruxsat bering. Uyquchanlikda alarm va inglizcha to‘xtash
ogohlantirishi ishlaydi. Alarm tugagach, ko‘zlar bir soniya davomida ochiq
ko‘rinsa, ChatGPT xavfsiz to‘xtash haqida savol beradi. Telefon bitta inglizcha
javobni tinglaydi va qisqa yakuniy eslatma aytiladi.

Mac serveri alohida OpenAI API suhbatining ID’sini
`~/.safedriveai/conversation-id` faylida saqlaydi. Ulanish va keyingi
ogohlantirishlar shu ID’dan foydalanadi. Suhbat OpenAI API orqali yuritiladi
va ChatGPT ilovasidagi shaxsiy chatlar ro‘yxatidan alohida.
OpenAI’ga kamera yoki audio fayli yuborilmaydi. Tanilgan javob matni suhbatda
saqlanadi. Nutqni tanishda telefonning Android xizmati ishlatiladi.

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

## Telefonda ChatGPT hisobiga kirish

SafeDriveAI’da **Settings → Continue with ChatGPT** ni bosing. Telefon
brauzerida hisob va tarifdan foydalanishga ruxsat bering, keyin
**Return to SafeDriveAI** orqali qayting. Mos hisob bilan ovozli yordamchi
telefon interneti orqali ishlaydi. So‘nggi 12 ta xabar telefonda shifrlangan
holda saqlanadi va navbatdagi savolga kontekst bo‘ladi.
**Sign out of ChatGPT** ushbu telefondagi sessiyani yopadi.

[OpenAI kirish hujjati](https://developers.openai.com/siwc/token-sharing-open-source/sign-in)

## Gemini Live ovozli suhbat

MacBook yordamchi serverida `.env` faylidagi `GEMINI_API` dan foydalaniladi.
Telefonni USB orqali ulang va `adb -d reverse tcp:8765 tcp:8765` ni bajaring.
SafeDriveAI’da **Settings → Gemini Live** va **Voice reminders** ni yoqing.
**Start monitoring** bosilganda mikrofon ruxsatini bering.

Uyquchanlik alarmi tugab, ko‘zlar bir soniya ochiq ko‘ringach, ilova Gemini Live
sessiyasini ochadi. Yumaloq indikator ulanish va suhbat holatini ko‘rsatadi.
Gemini qisqa xavfsizlik eslatmasidan keyin inglizcha erkin suhbat boshlaydi.
Savollaringizga javob beradi, siz tanlagan mavzuga o‘tadi va turli savollar beradi. Suhbatni tugatish uchun alohida javob sifatida **I feel okay**
deng. Alarm qaytsa, kuzatuv to‘xtatilsa yoki ulanish uzilsa ham sessiya yopiladi.

### Gemini Live’ni USB kabelsiz ishlatish

Telefondan ochiladigan HTTPS manzilida alohida token serverini ishga tushiring.
Server muhitiga `GEMINI_API` va `SAFEDRIVE_LIVE_BROKER_TOKEN` ni kiriting.
Serverga kirish tokenini `openssl rand -hex 32` bilan yaratish mumkin.
HTTPS reverse proxy serverga quyidagi jarayonni yo‘naltiradi.

```bash
.venv/bin/python -m uvicorn safedrive.controllers.live_broker:app --host 127.0.0.1 --port 8766
```

**Settings → Gemini Live connection** ichida HTTPS serverning asosiy
manzilini va `SAFEDRIVE_LIVE_BROKER_TOKEN` qiymatini kiriting.
**Test and save server** tugmasi token olinadigan manzilni tekshiradi. Telefon
bir martalik Gemini tokenini shu serverdan oladi va audio uchun bevosita
Gemini’ga ulanadi. **Use USB** bosilsa, ilova mahalliy USB ulanishiga qaytadi.
