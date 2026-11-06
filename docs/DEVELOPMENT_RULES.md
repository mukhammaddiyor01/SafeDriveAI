# SafeDriveAI — ishlab chiqish qoidalari

Asosiy talablar: [PROJECT_BRIEF.md](PROJECT_BRIEF.md). Joriy texnik qarorlar:
[ARCHITECTURE.md](ARCHITECTURE.md). Ushbu qoidalar keyingi ishlarda ham saqlansin.

## MVC va kod tuzilishi

- **Model:** kuzatuv ma’lumoti, vaqt oynalari, uyquchanlik qarori va domain qoidalari.
  Kotlin aniqlash mantig‘ini kamera/UI API’lariga bog‘lamang.
- **View:** ekran chizish va foydalanuvchi amalini controllerga uzatish. Widget ichida
  ko‘z chegarasi, PERCLOS, model treningi yoki alarm qarorini hisoblamang.
- **Controller:** foydalanuvchi amallari, model natijalari va resurslar hayot siklini
  muvofiqlashtirish. Takrorlangan domain qoidalarini controllerlarga ko‘chirmang.
- Kamera, platform channel, model runtime va LLM integratsiyasi mos adapterlarda
  qolsin. Har bir kamera kadrini Flutter channel orqali yubormang.
- Python qismida dataset/model, o‘qitish/baholash va CLI/API vazifalarini ajrating.

## Sodda va clean kod

- Funksiya va klass bitta aniq vazifaga ega bo‘lsin. Nomlar vazifani ochiq aytsin.
- Odam kuzatib bora oladigan oddiy control flow ishlating; ortiqcha abstraction,
  yashirin yon ta’sir va asossiz framework qatlamlarini qo‘shmang.
- Bir xil mantiqni turli qatlamlarda ko‘paytirmang. Chegaralar va vaqt birliklari
  tushunarli nomlansin; o‘zgarsa tegishli test va hujjat ham yangilansin.
- Izohlar qarorning sababini yoki muhim cheklovni tushuntirsin. Vaqtinchalik
  cheklovni yashirmang, muvaffaqiyatsiz amalni muvaffaqiyatli deb qaytarmang.
- Xato xabari foydalanuvchiga nima qilish mumkinligini aytsin. Diagnostikada kamera
  tasviri, maxfiy ma’lumot yoki kalitlarni logga yozmang.
- Kamera, image proxy, executor, audio, TTS va service resurslarini to‘g‘ri yoping.

## Kamera, AI va alarm

- Asosiy kuzatuv va alarm internet yoki LLM javobini kutmasin.
- Yuz/ko‘z ko‘rinmasa, tahlil sekinlashsa yoki kadr uzilsa noaniqlikni alohida holat
  sifatida bildiring. Bunday vaqtni kuzatilgan ochiq/yopiq ko‘z deb hisoblamang.
- Oddiy blink va davomli ko‘z yumilishini vaqtga asoslangan mantiq bilan ajrating.
  Amaldagi raqamlar [ARCHITECTURE.md](ARCHITECTURE.md)da; ularni tasdiqlangan
  universal tibbiy chegara deb taqdim etmang.
- Android ruxsatlari, doimiy bildirishnoma va foydalanuvchi Stop amali saqlansin.
  Android cheklovlarini yashirin kamera yoki majburiy qayta ishga tushish bilan
  chetlab o‘tishni loyihaning odatiy yechimiga aylantirmang.
- Hozirgi oqimda tasvirlar saqlanmaydi yoki serverga yuborilmaydi. Buni o‘zgartirish
  zarur bo‘lsa, yangi ma’lumot oqimini aniq hujjatlashtiring va vazifa doirasini aniqlang.
- LLM uzilganda mahalliy eslatma ishlasin. Ovozli savollar to‘xtab dam olishga
  yo‘naltirsin; hushyorlik yoki xavfsizlik kafolati bermasin.

## Dataset va tekshirish

- Amaldagi qoida: 80% train / 20% test; butun rasmga yaxlitlash va oldingi
  foydalanish auditi [DATA_SPLIT_POLICY.md](DATA_SPLIT_POLICY.md)da.
- Yangi trening to‘liq 80% train bilan ishlaydi va oldindan belgilangan oxirgi
  epochni saqlaydi. Testni model/epoch/threshold tanlashga ishlatmang.

- Manba, label, preprocessing, subject split va model versiyasini qayd eting.
- Bir haydovchining kadrlarini train va test orasida aralashtirmang. Test split
  natijasidan trening yoki threshold tanlash uchun foydalanmang.
- Frontal, chap/o‘ng, yorug‘lik va ko‘rinish yo‘q holatlarni alohida baholang.
  Aniqlik, coverage, yolg‘on alarm va kechikish dalilsiz va’da qilinmasin.
- Sun’iy tasvir smoke sinovi faqat pipeline ishlashini tasdiqlaydi. Haqiqiy dataset
  aniqligi, haqiqiy LLM va jismoniy telefon sinovini alohida qayd eting.
- O‘zgarishga mos tekshiruvni bajaring. Domain o‘zgarsa unit test, Android hayot sikli
  o‘zgarsa runtime sinovi, model o‘zgarsa held-out baholash kerak.
- Faqat Markdown matni o‘zgarsa havolalar va mazmun mosligini tekshirish yetarli;
  o‘zgarmagan APK’ni qayta yig‘ish shart emas.

## Git va vazifani yakunlash

- Faqat **`feat:`** va **`fix:`** ishlating. Nom qisqa, aniq va inglizcha bo‘lsin.
- Har bir commit bitta kichik, yaxlit mantiqiy o‘zgarishni saqlasin. Turli funksiyalarni
  yig‘ib, bitta katta commit qilmang; bitta tuzatishni ham mazmunsiz bo‘lib tashlamang.
- Mantiqning bevosita testi va tegishli izohi shu o‘zgarish bilan birga kelsin.
- Tugallangan kichik qadamlarni ish davomida commit qiling; vazifa oxirida
  tekshirilgan commitlarni push qiling. Yangi ishda mavjud tarixni odatiy ravishda qayta yozmang.
- Tarixni qayta yozish foydalanuvchi aniq so‘ragan istisno: zaxira, mazmun tengligi
  tekshiruvi va aniq remote SHA bilan `--force-with-lease` ishlating.
- `.tools`, `.venv`, build natijalari, datasetlar, weights, APK va kalitlar Git’ga
  tushmasin. USB uchun APK lokal `releases/` papkasida saqlansin.
- Yakunda nima o‘zgargani, nima tekshirilgani va qolgan cheklovni aniq ayting.
  Joriy holat o‘zgarganda [PROJECT_STATUS.md](PROJECT_STATUS.md)ni yangilang.
