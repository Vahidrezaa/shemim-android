# ۰۲ — معماری فنی اپلیکیشن اندروید

## ۱. پشتهٔ فناوری

| لایه | انتخاب | دلیل |
|---|---|---|
| زبان | Kotlin 2.x | رسمی گوگل؛ coroutines برای همگام‌سازی |
| UI | Jetpack Compose + Material 3 | UI اعلانی؛ RTL و فارسی درست؛ انیمیشن‌های سبک |
| تزریق وابستگی | Hilt | استاندارد، پشتیبانی Compose/WorkManager |
| شبکه | Retrofit 2 + OkHttp 4 + kotlinx.serialization | کنترل کامل کوکی/هدر؛ JSON با کدجن |
| کش | Room 2.7+ | لیست‌های قابل‌کوئری؛ Flow برای UI واکنشی |
| تنظیمات | DataStore (Preferences + Proto) | سبک؛ ذخیرهٔ رمزنگاری‌شدهٔ session با EncryptedFile/Keystore |
| پس‌زمینه | WorkManager | sync دوره‌ای، صف ارسال، retry با backoff |
| پوش | Firebase Messaging (FCM) | سند ۰۵ |
| نمودار | Vico 2 (Compose) | نمودار خطی/میله‌ای/دایره‌ای کامپوز-محور |
| ناوبری | Compose Navigation + typed routes | deep link پوش → صفحهٔ هدف |
| تصویر | Coil 3 | لوگو/پیوست‌ها |
| تست | JUnit + Turbine + Compose UI Test | تست‌های واحد تقویم/پول/مپر حیاتی‌اند |

**SDK:** `minSdk 24` ، `targetSdk 36` ، `compileSdk 36`. جاوا 17 (CI: Temurin 17؛ همهٔ ماژول‌ها با target 17 کامپایل می‌شوند).

**نسخه‌های قفل‌شدهٔ فاز ۰:** AGP 8.10.1 · Gradle 8.13 · Kotlin 2.1.21 · Compose BOM 2025.06.01 · compileSdk 36 — منبع حقیقت: `gradle/libs.versions.toml`

## ۲. لایه‌بندی و ماژول‌ها (Gradle multi-module)

```
:app                     ← MainActivity، NavHost، DI graph، BottomBar
:core:designsystem       ← تم‌ها (۱۰ تم × روشن/تیره)، تایپ، کامپوننت‌های پایه
:core:network            ← Retrofit/OkHttp/CookieJar، ApiClient، error mapping
:core:db                 ← Room entities/DAOs، migration
:core:sync               ← SyncEngine، Outbox، WorkManager workers
:core:common             ← تقویم شمسی، Money/Number فارسی، Result types، Dispatchers — **ماژول Kotlin/JVM (ADR-012)**
:core:model              ← مدل‌های appData و DTOها (kotlinx.serialization)
:core:datastore          ← session/تنظیمات کاربر
:feature:auth            ← login/register/reset/otp
:feature:dashboard       ← admin + my
:feature:members         ← لیست/پروفایل/CRUD/انصرافی‌ها/ناظر
:feature:transactions
:feature:loans           ← + اقساط + ماشین‌حساب
:feature:queue           ← نوبت وام
:feature:money           ← درآمد/هزینه/انتقال
:feature:requests        ← V1+V2
:feature:notifications   ← + پیوست‌ها
:feature:rotatingfund    ← + قرعه‌کشی زنده + قراردادها
:feature:investfunds     ← ivf + legacy
:feature:polls           ← نظرسنجی/انتخابات/مسابقه
:feature:points          ← امتیازات + quiz points
:feature:calendar
:feature:analytics       ← + خروجی‌ها
:feature:roadmap
:feature:dues
:feature:groups
:feature:settings        ← صندوق + شخصی + تم
:feature:backup
:feature:messenger       ← بله + SMS/ایمیل تست
:feature:assistant
:feature:guide
:feature:ownerpower
:feature:more            ← «همهٔ بخش‌ها» + درباره/به‌روزرسانی
```

قاعدهٔ وابستگی: `feature → core:*` (هرگز feature→feature). `:app` همه‌چیز را مونتاژ می‌کند. این ساختار از فاز ۰ کامل ساخته می‌شود ولی ماژول‌های خالی با placeholder تا اپ سبک بماند (با `android.dynamicFeatures` کاری نداریم؛ سادگی > بازی‌سازی).

## ۳. ارتباط با API (قرارداد ۱:۱ با وب)

- **یک `ApiService` واحد** با امضای `suspend fun call(action: String, body: JsonObject): ApiResponse` + توابع نوع‌دار برای اکشن‌های پرتکرار. چون سرور الگوی یکنواخت `?action=` دارد، wrapper عمومی + enum of actions از تولید ۱۷۰ تابع دستی مطمئن‌تر است؛ برای اکشن‌های خاص (login/loadData/…) مدل ورودی/خروجی صریح تعریف می‌شود.
- **Session:** `PersistentCookieJar` (ذخیرهٔ کوکی‌ها در Encrypted DataStore؛ چرخش کوکی لاگین به‌صورت خودکار توسط OkHttp انجام می‌شود).
- **خطای `auth_required`:** به‌صورت سراسری intercept می‌شود → پاک کردن session محلی → هدایت به لاگین (همان رفتار `auth-core.js`).
- **خطای شبکه/timeout:** تفکیک «آفلاین» از «خطای سرور»؛ آفلاین → دادهٔ Room + نوار وضعیت آفلاین.
- **gzip** توسط OkHttp خودکار (Accept-Encoding). زمان پاسخ‌های بزرگ در لاگ دیباگ.

## ۴. موتور همگام‌سازی (SyncEngine)

```
fun refresh(reason): Flow<SyncState>
  1) POST loadData {sinceVersion: localVersion}
  2) unchanged=true  → فقط rfWatch به‌روز می‌شود
  3) appData کامل    → trx { map به Room (upsert کامل جداول سینک‌شونده) }
  4) dataVersion ذخیره → State.Synced(at)
```

- **محرک‌ها:** شروع اپ، بازگشت به اپ (ON_RESUME با نرخ‌حد)، pull-to-refresh، اتمام موفق هر عملیات نوشتنی، دریافت پوش FCM نوع `data_changed`، WorkManager دوره‌ای (هر ~۳۰ دقیقه در صورت باز بودن اپ + battery-not-low).
- **محرک سروری:** سرور بعد از هر عملیات نوشتنی `dataVersion` را عوض می‌کند؛ اپ بعد از هر write خودش sync می‌گیرد — بدون نیاز به polling ریز.
- **معیار تازگی:** اگر `dataVersion` محلی == سروری، هیچ نگاشتی انجام نمی‌شود (همان بهینه‌سازی وب).
- UI هر صفحه از **Room Flow** می‌خواند؛ بنابراین همیشه دادهٔ محلی فوراً هست و sync در پس‌زمینه صادقانه به‌روزش می‌کند (Single Source of Truth = Room، منبع Room = سرور).

## ۵. عملیات نوشتنی + آفلاین (Outbox)

- هر عملیات نوشتنی → رکورد در جدول Room `outbox(id, action, payload, createdAt, attempts, state)` → ارسال فوری؛ در شکست شبکه → `state=QUEUED` و WorkManager با backoff نمایی (تا ۲۴ ساعت) ادامه می‌دهد.
- **دامنهٔ آفلاین دقیقاً همان دامنهٔ لایهٔ وب** (سند ۰۱ §۵.۴): `addTransaction, editTransaction, deleteTransaction, payInstallment, addLoan, addIncomeExpense, editIncomeExpense, deleteIncomeExpense`. بقیهٔ عملیات فقط آنلاین (همان محدودیت وب — تصمیم ADR-005).
- **idempotency:** شناسه‌ها client-generated (UUID) — سرور فعلاً تکراری را نادیده می‌گیرد (رفتار اثبات‌شدهٔ وب). قبل از فعال‌سازی آفلاینِ هر اکشن، رفتار سرور در برابر ID تکراری روی **عضو تست** بررسی و مستند می‌شود؛ اگر اکشنی idempotent نبود، از صف آفلاین حذف می‌شود (صداقت کامل در سند ۰۴ ثبت می‌شود).
- نوار «تغییرات آفلاین در صف: N» در داشبورد؛ امکان مرور/لغو صف قبل از ارسال.

## ۶. کارایی با دادهٔ بزرگ

- نگاشت appData→Room در Dispatchers.IO با `@Transaction` دسته‌ای؛ UI هرگز JSON خام نمی‌بیند.
- لیست‌ها: `LazyColumn` + `Room Paging` برای تراکنش‌ها (پرس‌وجوی چند-هزارتایی).
- اولین رندر از کش قبلی (stale-while-revalidate)؛ اسپلش فقط تا دادهٔ محلی.
- سنجهٔ هدف (به‌روزرسانی می‌شود بعد از تست واقعی): کمتر از ۲ ثانیه تا پیشخوان روی دادهٔ کش؛ کمتر از ۵ ثانیه sync کامل روی 4G برای صندوق ۵۰ عضوی.

## ۷. تقویم شمسی و فرمت‌ها

- **پورت دقیق** توابع `miladiToJalali/jalaliToMiladi` از `js/config.js` (بدون کتابخانهٔ خارجی) + واحد تست روی تاریخ‌های مرزی (کبیسه ۱۴۰۳/۱۴۰۸، عبور اسفند، ۲۹/۳۰ شش‌ماهه‌ها) با خروجی‌های مرجع تولیدشده از خود الگوریتم سایت.
- فرمت مبلغ: جداکنندهٔ هزارگان + حذف صفرهای غیرضروری + پسوند تومان/ریال طبق `settings`؛ ارقام فارسی به‌صورت پیش‌فرض با گزینهٔ لاتین (مثل وب).
- تاریخ‌ها در همهٔ فرم‌ها با **Date Picker شمسی** اختصاصی (کامپوننت فاز ۰).

## ۸. پوش نوتیفیکیشن (FCM) — خلاصه (شرح سرور: سند ۰۵)

- کانال‌های اعلان: `financial` (رویدادهای مالی)، `notifications` (اطلاعیه‌ها)، `draws` (قرعه‌کشی)، `reminders` (سررسید/تولد) — کاربر از تنظیمات اپ روشن/خاموش و اولویت می‌دهد.
- هر پیام FCM data-payload حامل `{type, target}` است → **Deep Link** به صفحهٔ مربوط (مثلاً `shemim://transaction/<id>` یا `shemim://rotatingfund/<fundId>/draw`).
- ثبت توکن: بعد از لاگین `fcmRegister`؛ در خروج/لاگین کاربر دیگر `fcmUnregister` (تا پیام نفر قبلی روی گوشی او نیاید).

## ۹. امنیت اپ

- کوکی و توکن FCM در Encrypted DataStore (Keystore اندروید).
- `android:allowCleartextTraffic=false`؛ فقط HTTPS به دامنهٔ صندوق (گواهی فعلی Let's Encrypt موجود).
- Certificate Pinning: **فعلاً خیر** (ریسک قطع‌شدگی هنگام تمدید/تغییر CA) — ADR-006؛ اگر خواستید در فاز ۶ با پینِ عمومی (SPKI دو CA) اضافه می‌شود.
- رمز/اطلاعات حساس هرگز در لاگ (Release با `Log.strip`؛ proguard-rule مخصوص).
- Backup rules: `allowBackup=false` برای دادهٔ مالی.
- Play Integrity / SafetyNet: لازم نیست (توزیع خارج از مارکت) — ذکر برای شفافیت.

## ۱۰. تست

| سطح | دامنه |
|---|---|
| Unit | تقویم شمسی (طلا)، Money format، نگاشت appData→Room، Outbox state machine، نقش‌ها/دسترسی ناظر |
| Integration (Robolectric) | CookieJar + چرخش کوکی لاگین، SyncEngine با سرور جعلی (MockWebServer با پاسخ‌های ضبط‌شدهٔ واقعی) |
| UI (Compose) | لاگین، پیشخوان، فرم واریز، پرداخت قسط |
| روی دستگاه (شما) | چک‌لیست هر فاز (سند ۰۴) — لاگ Actions + نصب APK |

## ۱۱. ساختار پوشهٔ ریپو

```
shemim-android/
├── app/ … (ماژول‌های Gradle بالا)
├── server/                ← فایل‌های PHP جدید/تغییری برای هاست (سند ۰۵)
├── assets/brand/          ← آیکون و لوگوی صندوق (منبع: سایت)
├── docs/                  ← همین مجموعه + ADRها
├── .github/workflows/android-debug.yml + android-release.yml
├── gradle/libs.versions.toml
└── README.md
```
