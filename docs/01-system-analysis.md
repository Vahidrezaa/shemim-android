# ۰۱ — تحلیل سیستم فعلی (وب‌سایت شمیم)

> این سند نتیجهٔ بازبینی مستقیم سورس‌کد (نسخهٔ پیوست‌شده در `abcd (3).zip`) است، نه حدس. هر ادعایی که اینجا نیامده، در سند ۰۷ به‌عنوان «ناشناخته» ثبت شده است.

## ۱. مشخصات کلی

| مورد | مقدار |
|---|---|
| نام سامانه | صندوق قرض‌الحسنه امیرالمؤمنین(ع) — «شمیم» |
| دامنه | `https://abcd.khaneh-soorati.ir` (ساب‌دامین مستقل، ریشهٔ وب) |
| بک‌اند | PHP + MySQL (`khanehso_vam2`)، ورودی واحد `api.php?action=…` با بدنهٔ JSON |
| فرانت‌اند | SPA وانیلا JS (بدون فریمورک) + PWA (Service Worker, Web Push/VAPID) |
| فونت | Vazirmatn (متغیر) |
| زبان/جهت | فارسی، RTL کامل، تقویم شمسی (الگوریتم تبدیل داخل `js/config.js`) |
| هاست | cPanel/LiteSpeed؛ اسرار در `api/config.secrets.php` (یا بیرون از public_html) |

## ۲. نقش‌ها و دسترسی‌ها

- **مدیر کل (`admin`):** همه‌چیز.
- **سرگروه (`groupLeader`):** رابط اعضای گروه(های) خودش (اعضای من، تراکنش/وام/درخواست زیرمجموعه).
- **کاربر عادی (`user`):** فقط داده‌های شخصی.
- **ناظر (attribute روی عضو، جدا از role):** ستون‌های `members.is_supervisor` و `supervisor_perms`؛ دسترسی ریزدانهٔ خواندن/نوشتن به ۱۲+ حوزه (`authz_supervisorCatalog()`)؛ فقط مدیر تعیین می‌کند؛ سرگروه می‌تواند همزمان ناظر باشد.
- **`resigned`:** نقش اعضای انصرافی؛ لاگینشان رد می‌شود.
- کنترل دسترسی سمت سرور: `api/authz.php` — نقش همیشه از دیتابیس خوانده می‌شود (اعتماد به کلاینت ممنوع). اکشن‌های مدیریتی در `authz_adminOnlyActions()`، اکشن‌های واگذارشده به «سازندهٔ صندوق گردشی» در `authz_rotatingFundDelegatedActions()`.

## ۳. API — قرارداد ارتباطی

- **آدرس:** `POST /api.php?action=<name>` با بدنهٔ JSON (برخی اکشن‌ها GET-پذیرند ولی الگوی ثابت POST است).
- **احراز هویت:** Session PHP با کوکی `PHPSESSID`، عمر ۶۰ روز، `secure` روی HTTPS، `HttpOnly`. در لاگین موفق `session_regenerate_id` (چرخش کوکی — کلاینت باید CookieJar واقعی داشته باشد).
- **گارد:** به‌جز `login`, `register`, `getTrialStatus`, `ping`, `getRegistrationStatus`, `pushLatest`, `savePushSubscription`, `removePushSubscription`, `getUserActivity`, `getMembersPresence`, `requestPasswordReset`, `validateResetToken`, `resetPassword`, `baleOtpRequest`, `baleOtpVerify` — بقیه بدون نشست معتبر خطای `auth_required` می‌گیرند (HTTP 200 با `success:false`).
- **قالب پاسخ:** همیشه `{success: bool, …}`؛ خطاها: `error` (متن فارسی) و گاهی `code` (مثل `too_many_attempts`).
- **نسخه‌بندی داده:** هر اکشن نوشتنی `dataVersion` را دوبار bump می‌کند؛ `loadData` با `sinceVersion` اگر تغییری نبوده `unchanged:true` برمی‌گرداند.
- **فشرده‌سازی:** gzip سمت سرور روی پاسخ‌های بزرگ (loadData می‌تواند چند مگابایت باشد → روی شبکه ~۱۰-۲۰٪).
- **محدودیت لاگین:** حداکثر ۱۵ تلاش/IP و ۳۰ تلاش/نام‌کاربری در ۱۰ دقیقه (`too_many_attempts`).
- **دورهٔ آزمایشی:** ثابت `TRIAL_EXPIRY_DATE`؛ بعد از انقضا فقط اکشن‌های لیست سفید (`$trialAllowedActionsAfterExpiry`) مجازند.
- **حجم:** ۴۳ فایل PHP در `api/`، **۱۷۶ اکشن** + endpointهای مستقل (آپلود پیوست اطلاعیه، وب‌هوک بله، آپلود ویدیو گردشی، دو کرون پشتیبان/تولد، و ابزارهای وب: version.php، bundle.php). فهرست کامل به تفکیک ماژول: سند ۰۴.

## ۴. مدل داده — `appData` (خروجی loadData)

پاسخ `loadData` (بعد از لاگین) این کلیدها را دارد:

```
fundName, ownerPower, quizPoints, mainAccount, incomeExpenseAccount,
members[],            ← هر عضو: مشخصات + transactions[] + loans[] (هر وام: installments[])
incomeExpenseTransactions[], groups[], resignedMembers[], settings{},
profitReports[], requests[], requestsV2[], loanQueue[], notifications[],
nextMemberNumber, nextLoanNumber, dataVersion, rfWatch
```

نکات مهم برای اپ:
- `mainAccount` / `incomeExpenseAccount`: موجودی حساب‌های صندوق (محاسبه‌شده سمت سرور).
- `settings`: آبجکت کلی صندوق (نام، لوگو، رنگ/تم پیش‌فرض، قواعد وام: `loanMultiplier`, `loanReservePercent`, `loanMonthlyDivisor`, `loanMultiplierMode` و…).
- ایمیل/داده‌های حساس اعضا در `loadData` فقط برای مجازان (خود عضو، مدیر، سرگروهش، ناظرِ دارای دسترسی) برمی‌گردد — پاسخ API بین نقش‌ها فرق دارد.
- `rfWatch`: تنظیم قرعه‌کشی زندهٔ در حال تماشا (polling صفحهٔ زنده).
- جداول دیتابیس: **۶۰ جدول** (+۱ جدول جدید `fcm_tokens` طبق سند ۰۵). گروه‌بندی کامل در سند ۰۴.

## ۵. زیرسیستم‌های خاص (رفتارهایی که اپ باید عیناً بازتولید کند)

1. **قرعه‌کشی صندوق گردشی زنده:** سرور برنده را از قبل تعیین می‌کند (`startRfLiveSpin`)؛ کلاینت گردونهٔ Canvas را چند دور می‌چرخاند و روی برندهٔ واقعی می‌ایستد؛ صدا با `postRfLiveAudio` قابل ارسال است؛ تماشاگران با polling وضعیت (`getRfLiveDraw`، جدول `rf_live_viewers`)؛ پیام بله بعد از ایستادن گردونه از `messenger_pending` ارسال می‌شود.
2. **قواعد قرعه‌کشی:** فقط واریزکنندگان همان دوره که قبلاً برنده نشده‌اند؛ واریز زودهنگام (`is_early=1`) شانس را ۲× می‌کند؛ اگر همه برنده شده باشند محدودیت برداشته می‌شود.
3. **قراردادها (تعهدنامه):** صندوق گردشی (`rfContractPreview/Start/Verify` + گروهی) و سرمایه‌گذاری (`ivfContract…`) — پیش‌نمایش HTML امضاشده با کد تأیید؛ اپ این پیش‌نمایش را با WebView کوچکِ هم‌کوکی نمایش می‌دهد (تصمیم ADR-004).
4. **لایهٔ آفلاین وب (`offline.js`):** دامنهٔ مجاز آفلاین فقط برای مدیر: `addTransaction, payInstallment, addLoan, addIncomeExpense(+edit/delete), editTransaction, deleteTransaction`؛ صف به‌ترتیب؛ سرور شناسهٔ تکراری را نادیده می‌گیرد → اپ همین دامنه را پیاده می‌کند (نه بیشتر؛ چون منطق سرور هم فقط همین‌ها را idempotent تحمل می‌کند).
5. **پیام‌رسان بله:** اتصال با کد جفت‌سازی (`msCreateCode`) در ربات؛ OTP ورود (`baleOtpRequest/Verify`)؛ ارسال رویدادهای مالی به عضو + سرگروه(ها)؛ تنظیمات مدیر + فهرست متصل‌ها؛ وب‌هوک: `messenger-webhook.php`.
6. **پیامک:** وب‌سرویس آسانک (`sms.asanak.ir`)، اعتبارنامه در فایل اسرار؛ نرمال‌سازی شمارهٔ ایرانی (ارقام فارسی/عربی، +98، 0098).
7. **دستیار هوشمند:** Google Gemini از سمت سرور (کلید و سهمیهٔ روزانه در `app_settings`)؛ اکشن `assistantAsk`؛ فقط متن سؤال می‌رود، هیچ دادهٔ مالی نمی‌رود.
8. **اعلان‌ها:** اطلاعیهٔ هدفمند (همه/عضو/گروه)، اولویت‌ها (عادی/تولد/هشدار معوقه)، پیوست فایل (تا ۸ فایل × ۳۰MB، سرو از طریق PHP با Range)، وضعیت خوانده‌شدن، ورود CSV، پوش وب (VAPID) + مکانیزم تحویل pull برای PWA (`pushLatest`).
9. **کارهای روزانهٔ سرور** (روی `loadData` اجرا می‌شوند، بدون کرون هم): تولدها، هشدار معوقه، یادآوری گردشی، پشتیبان خودکار روزانهٔ gz (۱۴ روز + ۶ ماه اول ماه) — یعنی **اپ نیازی به تکرار این منطق‌ها ندارد؛ سرور خودش موقع loadData انجام می‌دهد.**
10. **پشتیبان‌گیری:** لینک دانلود امن، دانلود، بازیابی از بک‌آپ پنل و از SQL خام (`restoreSqlBackup`) — فقط مدیر.
11. **نقشهٔ راه / پیش‌بینی:** محاسبات ۱۲ ماه آیندهٔ صندوق (سمت کلاینت از روی appData).
12. **مقیاس‌پذیری سرور:** `loadData` با ۳ کوئری دسته‌ای (بدون N+1) — ولی برای صندوق‌های خیلی بزرگ همچنان پاسخ کامل است؛ اپ باید با پاسخ چند-مگابایتی خوب رفتار کند (سند ۰۲ §۶).

## ۶. فهرست نقاط اتصال خارجی

| سرویس | کاربرد | تنظیم |
|---|---|---|
| بله (messengers) | ربات رویدادهای مالی + OTP ورود | فایل اسرار + app_settings |
| آسانک SMS | پیامک | فایل اسرار |
| Gemini (Google AI) | دستیار عمومی | `app_settings.assistantAI` (کلید فقط سمت سرور) |
| Web Push (VAPID) | پوش مرورگر | کلید عمومی در `js/config.js` |
| ایمیل (mailer.php) | بازیابی رمز و تست | فایل اسرار |

## ۷. مواردی که در اپ «معادل نیتیو» می‌گیرند (نه کپی وب)

| در وب | در اپ |
|---|---|
| PWA + Service Worker + پیام نصب | APK نیتیو؛ کش Room |
| Web Push (VAPID) | FCM (با endpoint جدید — سند ۰۵) |
| دکمهٔ Back مرورگر + تأیید خروج | OnBackPressed + خروج با تأیید (الگوی نیتیو) |
| خروجی Excel/PDF سمت مرورگر | خروجی CSV/XLSX + PDF اندروید + Share Sheet |
| گردونهٔ Canvas قرعه‌کشی | انیمیشن Canvas/Compose با همان منطق ایستادن روی برندهٔ سرور |
| `pushLatest` (polling پوش PWA) | با FCM جایگزین می‌شود؛ polling فقط fallback |

## ۸. نکات امنیتی موجود که اپ باید رعایت کند

- هرگز متن خطای SQL به کاربر نمی‌رسد (اپ هم پیام فارسی عمومی نشان می‌دهد و جزئیات را فقط لاگ محلی می‌کند).
- نقش/دسترسی همیشه از پاسخ سرور گرفته می‌شود؛ دکمه‌های مخفی‌شده در کلاینت صرفاً UX است (همان اصل وب).
- `saveData` کلان در اپ استفاده نمی‌شود (ADR-002).
- کوکی session باید در ذخیره‌گاه رمزنگاری‌شده (Encrypted DataStore) نگهداری شود.
