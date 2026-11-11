# SafeDriveAI — joriy holat va davom ettirish

Oxirgi mazmuniy tekshiruv: **2026-11-11**. Bu hujjat tayyorlik holatini saqlaydi;
mahsulotning yakuniy talablari [PROJECT_BRIEF.md](PROJECT_BRIEF.md)da.
Dalillar: [VERIFICATION.md](VERIFICATION.md). O‘zgargan qismni yangi dalil bilan yangilang.

## Amaldagi dataset qoidasi — foydalanuvchi o‘zgartirishi

**80% train / 20% ishlatilmagan test**. Yangi trening faqat train bilan ishlaydi;
oldindan belgilangan oxirgi epoch saqlanadi. [To‘liq qoida](DATA_SPLIT_POLICY.md).
Yangi umumiy manifestlar uchun SHA-256 hisoblanadi yoki tekshiriladi; takroriy
mazmundagi tasvirlar split ajratilishidan oldin rad etiladi.
MRL taqsimoti: 67,858 train / 16,964 test, haydovchilar 30/7.
Auditda yangi test rasmlarining barchasi avval ishlatilgani aniqlandi; qayta
trening yoki yangi test natijasi chiqarilmadi. Yangi mustaqil test ma’lumoti kerak.
Oldingi 86.06% natija eski train/val/test tajribasiga tegishli, 80/20 natijasi emas.

## RT-BENE tadqiqot tajribasi — 2026-11-07

Yangi RT-BENE manbasida 7 000 train / 1 750 test tasvir bilan 80/20 tajriba bajarildi.
Ikki test subyekti treningdan ajratildi. MRL tarixida mos fayl xeshlari topilmadi.
Model 18 yopiq ko‘z tasviridan 15 tasini topdi va 1 732 ochiq tasvirdan 620 tasini xato
yopiq dedi. Model tadqiqot bosqichida qoladi. Keyingi testlarda ushbu manifestni
foydalanish tarixiga qo‘shing. [Natija va cheklovlar](RT_BENE_PILOT.md)

## Yonbosh ko‘z holati tajribasi — 2026-11-09

RT-BENE/RT-GENE asosida 724 train va 181 avval ishlatilmagan test kesmasi
ajratildi. Chap tomonda 12/20 yopiq ko‘z aniqlandi. O‘ng testda faqat ochiq
ko‘zlar bor. Model tadqiqot bosqichida. [Tajriba va natija](SIDE_EYE_PILOT.md)

## Joriy bosqich

**Birinchi ishlaydigan prototip. To‘liq ko‘p burchakli yakuniy mahsulot hali tayyor emas.**
Flutter + Kotlin + Python MVC tuzilmasi mavjud. APK 0.1.0+1, arm64, Android API 26+;
lokal fayl `releases/SafeDriveAI.apk`. Repositoryga kod va hujjatlar yuborilgan.
Oldingi bitta katta commit 29 ta kichik `feat:`/`fix:` commitga ajratilgan; keyin
ish tartibi alohida commitda saqlangan. Keyingi o‘zgarishlar ham kichik commitlarda bajarilsin.

## Talablar bo‘yicha holat

| Talab | Hozir bor | Hali bajarilishi yoki tasdiqlanishi kerak |
|---|---|---|
| SD-01 Kamera | CameraX, preview va ML Kit tahlili emulyatorda ishladi | Galaxy A51 haqiqiy kamera sinovi |
| SD-02 Yuz/ko‘z/bosh/bo‘yin | Yuz, ko‘z ehtimollari va bosh burchaklari; Python YOLO bosh/yelka tajribasi | To‘liq bo‘yin holati, ishonchlilik va maxsus modelni telefonga ulash |
| SD-03 Alarm | Vaqtga asoslangan domain mantiq, ovoz/vibratsiya kodi va 3 s uzilishdan keyin PERCLOS tarixini tozalash. Test alarm ovoz va vibratsiyani alohida sinaydi | Haqiqiy ko‘z yumilishida kechikish, eshitilish va yolg‘on alarm sinovi |
| SD-04 Fon | Android 13 emulyatorida Home va ekran o‘chganida tahlil davom etdi; notification Stop ishladi | A51’da uzoq ishlash, batareya, qizish, qo‘ng‘iroq va boshqa kamera bilan sinash |
| SD-05 Ko‘p burchak | MediaPipe ko‘rinadigan ko‘z, ±60° tekshiruv doirasi, Galaxy A51’da ikki statik rasm sinovi | Jonli ko‘z yumilishi, barcha burchaklar va markaziy tutqich validatsiyasi |
| SD-06 Dataset/model | 80/20 kodi va foydalanish auditi mavjud; MRL: 84,822 tasvir, 37 haydovchi; 5 epoch PyTorch MPS trening va 6 boshqa haydovchida held-out hisobot; YOLO vositasi bor | Ishlatilmagan 20% test ma’lumoti, yuqori FPR’ni train doirasida yaxshilash, RGB/ko‘p burchakli dalil, mobil model adapteri |
| SD-07 Ovoz/LLM | Ilovada ulanish, tiklanish triggeri, inglizcha savol va bitta ovozli javob, qayta ishlatiladigan OpenAI suhbat ID’si | Jonli API hamda Galaxy A51’da mikrofon va fon rejimi |
| SD-08 USB APK | Release APK imzosi tekshirildi. Wireless debugging yo‘riqnomasidan keyin foydalanuvchi telefon sinovi yaxshi o‘tganini bildirdi | USB orqali o‘rnatish va telefon sinovining batafsil natijalarini qayd etish |
| SD-09 MVC | Flutter, Kotlin va Python qatlamlari ajratilgan | Keyingi funksiyalarda chegaralarni saqlash |
| SD-10 Commit | Kichik feat/fix tarix va loyiha qoidalari mavjud | Har bir keyingi qadamda shu tartibga amal qilish |

## Tekshirilgan boshlang‘ich dalillar

- Flutter: static analysis toza, 3 ta controller/UI test o‘tgan.
- Kotlin: vaqtga asoslangan mantiq uchun 9 ta unit test o‘tgan.
- Python: 2026-11-06 dataset, provenance, trening/baholash va assistant va 80/20 test izolyatsiyasi uchun 33 ta test o‘tdi.
  Flutter/Kotlin/emulyator dalillari 2026-11-05 dagi sinovga tegishli; bu sessiyada Android kodi o‘zgarmadi.
- Release APK emulyatorda ochildi; R8 ML Kit registrar tuzatishi tekshirildi.
- Home’dan keyin inferens hisoblagichi oshdi. Ekran Asleep holatida kuzatuvlar
  260 → 567 bo‘ldi; oxirgi qayta ishlangan kadr yoshi 32 ms edi.
- Python server /health va /question HTTP 200 qaytardi; haqiqiy LLM modeli
  o‘rnatilmaganda offline fallback sinovdan o‘tdi.
- YOLO/PyTorch sun’iy bo‘sh kadrda ishga tushdi. Trening pipeline ham sun’iy
  tasvirlarda tekshirilgan edi; o‘sha smoke sinovlari haqiqiy aniqlik dalili emas.
- 2026-11-06 MRL IR tasvirlarida haqiqiy trening va held-out test bajarildi:
  accuracy 86.06%, closed recall 96.51%, ochiq ko‘zni xato closed deyish 22.92%.
  Ko‘zoynakli guruhda FPR 77.98%; natija mobil alarmga tayyor deb belgilanmagan.
- [MRL protokoli](MRL_PROTOCOL.md), [natijalar](MRL_BASELINE.md),
  [manba/import](MRL_EYE.md). Tanlangan weights lokal
  `artifacts/mrl-spatial-2026-11-06/eye_state.pt`; APK hamon ML Kit bilan ishlaydi.

## Keyingi ishlarning yo‘nalishi

1. **Qurilma dalili:** Galaxy A51 Android versiyasi va USB ulanishi mavjud bo‘lganda
   [DEVICE_TESTING.md](DEVICE_TESTING.md) bo‘yicha sinash; muammolarni alohida kichik fixlarda tuzatish.
2. **Dataset:** MRL import/trening yakunlandi. Endi yangi mustaqil RGB yozuvlar,
   ko‘zoynak/refleksiya va turli burchaklar qamrovini tayyorlash. Tartib: [DATASETS.md](DATASETS.md).
3. **Maxsus model:** 80% train doirasida MRL boshlang‘ich modelining yuqori FPR va
   sifat muammolarini yaxshilash; 20% test uchun yangi, ishlatilmagan dalil tayyorlash.
   2026-11-06 test natijasiga moslab sozlab, uni yana yangi mustaqil natija deb yozmaslik.
   Ko‘rinmas ko‘z, bosh/bo‘yin va haqiqiy burchaklar alohida qoladi.
4. **Mobil integratsiya:** tasdiqlangan modelni telefonga mos formatga export qilish,
   preprocessing/natija tengligini va A51 tezligi/quvvatini tekshirish.
5. **Ovozli yordamchi:** jonli OpenAI ulanishi, tiklanish triggeri va mikrofonni
   Galaxy A51’da ekran yoniq va fon rejimida sinash.

Bu tartib yangi funksiyalarni hozir avtomatik boshlash topshirig‘i emas. Foydalanuvchining
joriy vazifasini bajaring; undagi ishni mos talab va keyingi qadam bilan bog‘lang.

## Keyingi sessiyada davom ettirish

1. `AGENTS.md`, PROJECT_BRIEF, DEVELOPMENT_RULES va ushbu faylni o‘qing.
2. `git status`, oxirgi commitlar va tegishli kodni tekshiring; foydalanuvchi
   o‘zgartirishlarini ustidan yozmang. Texnik detallar uchun ARCHITECTURE’ni oching.
3. Bitta kichik natijani tanlang, tegishli tekshiruvni bajaring va feat/fix commit qiling.
4. Yangi dalil yoki holat bo‘lsa shu faylni yangilang. Batafsil sinov dalilini
   VERIFICATION’da sanasi bilan saqlang; eski sinovni bugungi sinov deb ko‘rsatmang.

Markaziy tutqich uchun dataset protokoli tayyor. Haqiqiy trening uchun bosh holati va navigatorga nigoh belgilangan RGB yozuvlar kerak. [Dataset tartibi](DATASETS.md)

SafeDriveAI sozlamalarida **Continue with ChatGPT** telefon brauzeri orqali
hisobga kirishni boshlaydi. Mos hisobdan foydalanilganda, ovozli yordamchi
telefondan OpenAI’ga ulanadi va so‘nggi 12 ta xabarni shifrlangan holda
saqlaydi. Jonli hisob bilan Android sinovi hali talab qilinadi.

**Gemini Live (SD-07)** alarmdan keyin inglizcha erkin ovozli suhbatni boshlaydi.
Gemini savollarga javob beradi va haydovchi tanlagan mavzuga o‘tadi. Haydovchi **I feel okay** desa, suhbat
tugaydi va kamera kuzatuvi davom etadi. MacBook serveri bir martalik token
beradi. Galaxy A51’da mikrofon va karnay bilan to‘liq sinov hali talab qilinadi.

Gemini Live uchun HTTPS token serveri alohida Docker konteynerida ishlaydi.
Nginx HTTPS so‘rovlarini konteynerning lokal portiga uzatadi. Galaxy A51’da
kabelsiz ulanish sozlamasi muvaffaqiyatli saqlandi. Foydalanuvchi sinov
muvaffaqiyatli o‘tganini bildirdi. [Tekshiruv natijalari](VERIFICATION.md)

## 2026-11-11 qo‘shimchalari

- Google Face Landmarker modeli yonboshda ko‘rinadigan ko‘zni lokal tahlil qiladi.
  Galaxy A51’da ikkita yuborilgan rasm ochiq ko‘z deb topildi.
- Alarm va Bluetooth media oqimi maksimumga ko‘tariladi va audio fokus
  so‘raladi. Mashina Bluetooth’i bilan real eshitilish sinovi hali kerak.
- Yangi 1 704 train / 426 test PyTorch tajribasi yakunlandi. Yopiq ko‘z
  recall 4,17% bo‘ldi. Bu checkpoint mobil inferensga ulanmagan.
- Dizayn taklifi rasm sifatida ko‘rsatildi. Interfeysni qayta dizayn qilish
  foydalanuvchi ko‘rib chiqishidan keyingi alohida qadam.
