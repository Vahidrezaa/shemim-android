# ۰۴ — نقشهٔ کامل پوشش قابلیت‌ها (سند زنده)

> این سند **عقدِ پوشش کامل** پروژه است: هر ماژول وب، اکشن‌های API آن، صفحه/صفحات معادل وب، صفحهٔ(های) اپ، و فاز اجرا. ستون «وضعیت» بعد از هر WP به‌روز می‌شود.
> وضعیت‌ها: 🔲 برنامه‌ریزی‌شده | 🟡 در حال توسعه | 🟢 پیاده‌سازی‌شده (کد) | ✅ تست‌شده روی بیلد Actions | ⛔ بلوکه (با دلیل در سند ۰۷)

## خلاصهٔ پوشش

- ۳۳ صفحهٔ وب / ۱۷۶ اکشن API / endpointهای مستقل (آپلود پیوست، وب‌هوک بله، آپلود ویدیو، کرون‌ها) / ۶۰ جدول → **۳۱ ماژول اپ** در ۷ فاز.
- هیچ ماژولی «حذف‌شده» نیست. موارد دارای محدودیت یا جایگزینی، صریحاً در ردیف خودشان با علامت ⚠️ آمده‌اند.

---

## M01 — احراز هویت و حساب
**فاز ۰-۱ | نقش‌ها: همه**

| قابلیت | اکشن API | صفحهٔ وب | صفحهٔ اپ |
|---|---|---|---|
| ورود | `login` | index (login) | Login |
| ثبت‌نام عمومی (اگر فعال) | `register`, `getRegistrationStatus` | فرم ثبت‌نام | Register |
| خروج | `logout` | — | Profile ▸ خروج |
| بازیابی رمز (ایمیل) | `requestPasswordReset`, `validateResetToken`, `resetPassword` | فرم فراموشی | ResetFlow |
| ورود با OTP بله | `baleOtpRequest`, `baleOtpVerify` | لاگین | Login ▸ ورود با بله (باز کردن دیپ‌لینک بله) |
| وضعیت دورهٔ آزمایشی | `getTrialStatus`, `ping` | شمارش معکوس/بلوک | Splash/Block |

نکات: محدودیت تلاش (`too_many_attempts`) به‌صورت شمارشگر باقی‌مانده نمایش داده می‌شود؛ چرخش کوکی بعد از لاگین توسط CookieJar خودکار.

## M02 — پیشخوان‌ها
**فاز ۱ (من) + ۲ (مدیر)**

| قابلیت | اکشن | وب | اپ |
|---|---|---|---|
| پیشخوان من (موجودی، وام، قسط بعدی، معوق) | `loadData` (محلی) | my-dashboard | HomeScreen(user) |
| پیشخوان مدیر (آمار کل، هشدارها، حساب‌ها) | `loadData` | dashboard | HomeScreen(admin) |
| پیام خوش‌آمد + سلام زمانی + وضعیت اقساط | محلی (منطق auth-flow) | مودال خوش‌آمد | HomeBanner |
| پاداش مدیران (قدرت وام) | `getOwnerPower`, `setOwnerPowerPercent` | owner-power | More ▸ OwnerPower (فاز ۴) |

## M03 — اعضا
**فاز ۲ | admin + سرگروه (زیرمجموعه) + ناظر (مطابق perms)**

| قابلیت | اکشن | اپ |
|---|---|---|
| لیست/جستجو/فیلتر همهٔ اعضا | `loadData.members` | MembersScreen |
| پروفایل کامل عضو (کارت مالی: موجودی، تراکنش‌ها، وام‌ها، اقساط) | محلی + `loadData` | MemberProfile |
| افزودن/ویرایش عضو | `addMember`, `updateMember` | MemberEditor |
| انصراف / حذف / حذف دائم | `deleteMember`, `permanentDeleteMember` | MemberProfile ▸ اقدامات |
| اعضای انصرافی | `loadData.resignedMembers` | ResignedScreen |
| تعیین ناظر + سطح دسترسی | `setMemberSupervisor` | MemberProfile ▸ نقش ناظر |
| حضور/آنلاین | `getMembersPresence` | More ▸ Presence (فاز ۲) |

## M04 — گروه‌ها
**فاز ۲**

| قابلیت | اکشن | اپ |
|---|---|---|
| لیست گروه‌ها + اعضا + سرگروه | `loadData.groups` | GroupsScreen |
| افزودن/ویرایش/حذف | `addGroup`, `updateGroup`, `deleteGroup` | GroupEditor |
| «اعضای من» سرگروه | محلی از groups | MyMembersScreen |

## M05 — تراکنش‌ها (سپرده)
**فاز ۱ (من) + ۲ (مدیریت)** | جداول: `member_transactions`

| قابلیت | اکشن | آفلاین؟ |
|---|---|---|
| لیست/جستجو/فیلتر (بازهٔ شمسی، نوع، دسته) | محلی | خواندن آفلاین |
| ثبت واریز/برداشت | `addTransaction` | ✅ آفلاین (admin) |
| ویرایش/حذف | `editTransaction`, `deleteTransaction` | ✅ آفلاین (admin) |
| دسته‌بندی و اتصال به وام (`is_installment`,`loan_id`) | داخل payload | — |

## M06 — وام‌ها و اقساط
**فاز ۱ (من) + ۲ (مدیریت)** | جداول: `loans`, `installment_payments`

| قابلیت | اکشن | آفلاین؟ |
|---|---|---|
| لیست وام‌ها + جزئیات + جدول اقساط | محلی | خواندن |
| افزودن/ویرایش/حذف وام | `addLoan`, `editLoan`, `deleteLoan` | addLoan ✅ |
| پرداخت قسط | `payInstallment` | ✅ |
| لغو پرداخت قسط | `unpayInstallment` | ❌ (مثل وب) |
| ویرایش پرداخت قسط | `editInstallmentPayment` | ❌ |
| ویرایشگر تک‌تک اقساط (installments-edit) | محلی + `editInstallmentPayment` | — |
| شمارهٔ وام خودکار | `loadData.nextLoanNumber` | — |
| ماشین‌حساب وام | محلی (port از loan-queue-profit.js) | کامل آفلاین |
| قواعد وام + ضریب زنده | `saveSettings` (loan settings) | محاسبهٔ زندهٔ محلی |

## M07 — نوبت وام (صف)
**فاز ۲**

| قابلیت | اکشن |
|---|---|
| لیست صف | `loadData.loanQueue` |
| افزودن به صف / حذف | `addToQueue`, `removeFromQueue` |
| ویرایش آیتم / همگام‌سازی | `updateQueueItem`, `syncLoanQueue` |

## M08 — درآمد/هزینه و انتقال
**فاز ۲**

| قابلیت | اکشن | آفلاین؟ |
|---|---|---|
| لیست درآمد/هزینه + فیلتر | محلی (`incomeExpenseTransactions`) | خواندن |
| ثبت/ویرایش/حذف | `addIncomeExpense`, `editIncomeExpense`, `deleteIncomeExpense` | هر سه ✅ |
| انتقال بین حساب‌ها | `transfer` | ❌ |
| حساب‌ها | `loadData.mainAccount`, `incomeExpenseAccount` | — |

## M09 — سود
**فاز ۲**

| قابلیت | اکشن |
|---|---|
| توزیع سود نسخه ۲ (و کلاسیک) | `distributeProfit` (محاسبات پیش‌نمایش محلی) |
| گزارش‌های سود | `loadData.profitReports`, `addProfitReport`, `deleteProfitReportById` |

## M10 — درخواست‌ها
**فاز ۱ (من) + ۲ (مدیریت)**

| قابلیت | اکشن |
|---|---|
| ثبت درخواست V2 | `addRequestV2` |
| مدیریت/تأیید/رد V1+V2 | `updateRequest`, `deleteRequest`, `updateRequestV2`, `deleteRequestV2` |
| ثبت درخواست قدیمی (V1) | `addRequest` |

## M11 — اطلاعیه‌ها و پیوست‌ها
**فاز ۱ (خواندن) + ۵ (مدیریت کامل)**

| قابلیت | اکشن/endpoint | اپ |
|---|---|---|
| لیست هدفمند + خوانده‌شده | `loadData.notifications`, `markNotificationRead` | NotificationsScreen |
| افزودن اطلاعیه (admin) | `addNotification` | NotificationEditor |
| آپلود پیوست (تا ۸×۳۰MB) | `POST /api/notification-upload.php` (multipart) | NotificationEditor |
| دانلود/پخش پیوست (Range) | `GET /api/notification-upload.php?id=…` | Viewer + Coil/ExoPlayer |
| ورود گروهی از CSV | محلی (فرمت CSV همان notifications-csv.js) | CSVImport (فاز ۵) |

## M12 — صندوق وام گردشی ⚙️ پیچیده‌ترین ماژول
**فاز ۳** | جداول: `rotating_funds` + ۴ جدول فرعی + `rf_*` (۶ جدول قرعه‌کشی زنده/قرارداد/لاگ)

| قابلیت | اکشن |
|---|---|
| لیست صندوق‌ها | `getRotatingFunds` |
| جزئیات صندوق (اعضا/دوره‌ها/پرداخت‌ها/تحویل‌ها) | `getRotatingFundDetail` |
| ایجاد/ویرایش/حذف + تأیید/رد | `createRotatingFund`, `editRotatingFund`, `deleteRotatingFund`, `approveRotatingFund`, `rejectRotatingFund` |
| افزودن/حذف عضو، خروج، اجازهٔ عضوگیری | `addRotatingFundMembers`, `removeRotatingFundMember`, `leaveRotatingFund`, `setRotatingFundMemberCreate` |
| واریز مقرری تکی/گروهی/چندماهه | `payRotatingFundInstallment`, `payRotatingFundMultiMonth` |
| ویرایش/حذف واریز | `editRotatingFundPayment`, `deleteRotatingFundPayment` |
| تعیین تاریخ قرعه | `setRotatingFundDrawDate` |
| قرعه‌کشی آفلاین + لغو | `drawRotatingFundLotteryOffline`, `undoRotatingFundLottery` |
| قرعه‌کشی زنده (گردونه + صدا + تماشا) | `startRfLiveSpin`, `getRfLiveDraw`, `cancelRfLiveDraw`, `postRfLiveAudio`, `setRotatingFundLiveWatch` |
| تحویل پول / لغو تحویل | `registerRotatingFundDelivery`, `cancelRotatingFundDelivery` |
| دفتر کل / گروهی | `getRotatingFundLedger`, `getRotatingFundLedgerBulk` |
| مقرری من | `getMyRotatingFundDues` |
| تاریخچهٔ قرعه‌ها | `getRfDrawLog` |
| تعهدنامه‌ها (فردی + گروهی) | `rfContractPreview`, `rfContractStart`, `rfContractVerify`, `rfContractStartGroup`, `rfContractVerifyGroup`, `getRfContracts` |
| آپلود ویدیو (وب) | `rotating-fund-video-upload.php` | ⚠️ در اپ: فقط در صورت نیاز فاز ۳ (اولویت پایین — اعلام شد) |

رفتارهای حیاتی که عیناً بازتولید می‌شوند: قواعد شانس (زودهنگام ۲×)، ظرفیت=تعداد دوره‌ها، توقف گردونه روی برندهٔ سروری، ارسال پیام بله بعد از ایست گردونه.

## M13 — صندوق‌های سرمایه‌گذاری (نسخهٔ جدید ivf)
**فاز ۴** | جداول: `ivf_*` (۱۰ جدول)

| قابلیت | اکشن |
|---|---|
| لیست صندوق‌ها | `getInvestFunds` |
| درخواست خرید سهام (عضو) | `ivfRequestShares` |
| تصمیم درخواست‌ها | `ivfDecide` |
| ثبت/ویرایش/وضعیت/حذف صندوق | `ivfSave`, `ivfSetStatus`, `ivfDelete` |
| دارایی‌ها (holdings) | `ivfAddHolding`, `ivfRemoveHolding` |
| پرداخت‌ها | `ivfAddPayment`, `ivfDeletePayment`, `ivfConfirmPayment`, `ivfMarkPaid` |
| دوره‌ها و سود | `ivfConfirmPeriod`, `ivfSetPeriodPps`, `ivfDistribute`, `ivfDeleteDistribution` |
| تسویه | `settleInvestmentFund` |
| قراردادها | `ivfContractPreview`, `ivfContractStart`, `ivfContractVerify`, `getInvestContracts` |
| تنظیمات عمومی | `ivfSaveSettings` |

## M14 — سرمایه‌گذاری قدیمی (legacy)
**فاز ۴ (پایین‌تر از ivf ولی کامل)**

`getInvestmentFunds`, `getInvestmentFundDetail`, `createInvestmentFund`, `updateInvestmentFund`, `deleteInvestmentFund`, `adminAddInvestmentFundMember`, `buyInvestmentFundShares`, `payInvestmentFundProfit`

> اگر در دادهٔ واقعی شما این ماژول بلااستفاده است، اعلام کنید تا ردیفش به «به‌درخواست» تغییر کند (کاهش حجم بی‌دلیل ممنوع).

## M15 — نظرسنجی / انتخابات / مسابقه
**فاز ۴** | جداول: `polls` + ۶ جدول

| قابلیت | اکشن |
|---|---|
| لیست/جزئیات | `getPolls`, `getPoll`, `getPollsOverview` |
| رأی دادن | `pollVote` |
| مدیریت (ایجاد/انتشار/بستن/تمدید/حذف) | `pollSave`, `pollPublish`, `pollClose`, `pollExtend`, `pollDelete` |
| زنده کردن / لغو نتیجه | `pollSetLive`, `pollRevoke` |
| انتصاب (در انتخابات) | `pollAppoint`, `getPollCandidates` |
| پرداخت‌های معلق | `pollPayPending` |
| تنظیمات | `pollSaveSettings` |
| ساخت سؤال با AI (کلیپ‌بورد) | محلی (همان قالب‌های polls.js) |

## M16 — امتیازات (points + quiz)
**فاز ۴**

| قابلیت | اکشن |
|---|---|
| نمای کلی / موجودی‌ها / دفتر | `getPointsOverview`, `getPointsBalances`, `getPointsLedger` |
| تنظیم دستی / انتقال | `pointsAdjust`, `pointsTransfer` |
| بازمحاسبه / بازسازی (پیش‌نمایش+اعمال) | `pointsRecalc`, `pointsRebuildPreview`, `pointsRebuildApply` |
| تنظیمات | `pointsSaveSettings` |
| تشکر دیرهنگام | `getMyThanks` (+ جدول late_thanks) |
| امتیاز مسابقات | `getMyQuizPoints`, `getQuizPointsAdmin`, `qpConvert`, `qpLookup`, `qpTransfer` |

## M17 — تقویم شمسی صندوق
**فاز ۱ (شخصی) + ۲ (مدیریت)** | `getCalendarEvents` + کامپوننت تقویم اختصاصی؛ رویدادها: سررسید اقساط، تولدها، قرعه‌کشی‌ها، پرداخت‌های گردشی.

## M18 — گزارشات تحلیلی
**فاز ۵ (نمودارها) + ۵ (خروجی)**
نمودارهای analytics.js (روند سپرده، وام‌های فعال، درآمد/هزینه، توزیع سنین/نقش و…) با Vico + خروجی CSV/XLSX/PDF اندروید + Share Sheet. محاسبات دقیقاً port از analytics.js.

## M19 — گزارش مطالبات و نقشهٔ راه
**فاز ۴** | dues-report.js و roadmap.js → معوقات + پیش‌بینی ماه بعد + نقشهٔ ۱۲ ماهه مدیر.

## M20 — تنظیمات
**فاز ۱ (شخصی) + ۲ (صندوق)**

| قابلیت | اکشن |
|---|---|
| تنظیمات صندوق (نام، لوگو، قواعد وام، ثبت‌نام عمومی) | `saveSettings`, `setRegistrationEnabled` |
| تم شخصی (sync وب↔اپ) | `updateMyTheme` |
| تغییر مشخصات/رمز خودم | `updateMember` (self) |

## M21 — پشتیبان‌گیری
**فاز ۵ (admin)**: `createBackupDownloadLink`, `downloadBackup`, `downloadBackupFile`, `restoreBackup`, `restoreSqlBackup`, `getAutoBackupStatus`, `runAutoBackupNow`. دانلود به پوشهٔ Downloads + share؛ بازیابی با تأیید دوباره‌مرحله‌ای (اقدام حساس).

## M22 — پیام‌رسان بله
**فاز ۵**

| قابلیت | اکشن |
|---|---|
| وضعیت اتصال من / اتصال با کد / قطع | `msStatus`, `msCreateCode`, `msUnlink` |
| شمارهٔ اشتراکی | `msSetSharedPhone` |
| تنظیمات مدیر + تست + فهرست متصل‌ها | `msSaveSettings`, `msTest` |

## M23 — پیامک و ایمیل (تست مدیر)
**فاز ۵**: `sendTestSms`, `sendTestEmail` (آسانک + mailer موجود).

## M24 — دستیار هوشمند
**فاز ۵**: `assistantAsk` (چت) + `getAssistantAI`, `saveAssistantAI`, `testAssistantAI` (تنظیم مدیر؛ کلید Gemini فقط سمت سرور می‌ماند). سهمیهٔ روزانه از پاسخ سرور.

## M25 — فعالیت کاربران
**فاز ۲**: `getUserActivity` (آخرین ورودها) — صفحهٔ گزارش ورود.

## M26 — راهنما
**فاز ۵**: محتوای guide-data*.js به‌صورت صفحات راهنمای داخل اپ (بروزرسانی با نسخه).

## M27 — پوش نوتیفیکیشن
**فاز ۵**: FCM + endpoint جدید (سند ۰۵) + کانال‌ها + Deep Link + تنظیمات کاربر. رفتار تحویل پیام‌های مالی/اطلاعیه همان نقاط ارسال Web Push فعلی.

## M28 — آفلاین و صف ارسال
**فاز ۶** (هستهٔ خواندن از فاز ۰): دامنهٔ دقیقاً برابر لایهٔ وب (سند ۰۲ §۵) + خروجی/ورودی فایل دادهٔ رمزگذاری‌شده (قابلیت موجود وب) اگر خواستید.

## M29 — به‌روزرسانی و نسخه
**فاز ۶**: `appConfig` (endpoint جدید) → بنر «نسخهٔ جدید» + دانلود APK از لینک شما.

## M30 — داده‌های پایه (appData)
**فاز ۰**: همهٔ کلیدهای loadData (سند ۰۱ §۴) در مدل‌ها + نگاشت Room + تست‌های تطبیقی.

## M31 — صفحهٔ «بیشتر» و پوستهٔ نقش‌محور
**فاز ۰**: نقشهٔ کامل منو مطابق `menusByRole` (admin/leader/user + overlay ناظر) — منبع حقیقت دسترسی UI.

---

## جدول وضعیت اجرا (به‌روز می‌شود)

| فاز | WP | عنوان | وضعیت | بیلد تست‌شده | یادداشت |
|---|---|---|---|---|---|
| ۰ | 0.1 | ساختار ریپو، Gradle، نسخه‌ها، lint | ✅ | Actions ✅ (گزارش کارفرما ۱۴۰۵/۰۹/۲۴) | ۳۳ ماژول + wrapper + کاتالوگ نسخه |
| ۰ | 0.8 | CI دیباگ + artifact | ✅ | Actions ✅ | ورک‌فلوی debug (بیلد+تست+lint+artifact) و release (تگ v*) سبز شد |
| ۰ | 0.2 | سیستم طراحی: ۱۰ تم، تایپ، کامپوننت‌های پایه | ✅ | Actions ✅ (گزارش کارفرما ۱۴۰۵/۰۸/۲۳؛ رفع رشته‌های جاافتادهٔ SkeletonScreen) | ده تم سایت (رنگ‌ها و گرادیان از themes.js) + Vazirmatn FD + کامپوننت‌های پایه + صفحهٔ نمایش دمو |
| ۰ | 0.6 | تقویم شمسی + فرمت مبالغ/ارقام فارسی | ✅ | Actions ✅ (همان بیلد) | پورت ۱:۱ از config.js (JalaliCalendar/MoneyFormat/NumberToWordsFa) + ۳۲ تست واحد جدید (مجموع ۳۸) با لنگرهای تقویم رسمی |
| ۰ | 0.3 | لایهٔ شبکه + CookieJar پایدار + مدیریت auth_required | 🟢 | — | core:network کامل (OkHttp خالص — ADR-017؛ کوکی رمزنگاری‌شده با Keystore — ADR-018؛ بازسازی بهینهٔ نشست — ADR-019) + SessionRepository در core:datastore + صفحهٔ ورود feature:auth + دروازهٔ نشست و خانهٔ موقت در app + مجوز INTERNET؛ ۱۳ تست واحد جدید (CookieJar ۶ + کلاینت با MockWebServer ۷؛ مجموع ۵۱) — کامپایل و اجرای محلی با Kotlin 2.1.21 و همان نسخه‌های وابستگی انجام و سبز شد؛ در انتظار Actions و تست گوشی |
| … | … | (WPهای بعدی با شروع هر فاز اضافه می‌شوند) | | | |

> یادداشت صداقت: «بیلد محلی موفق» یعنی در سندباکس من (Gradle 8.13 + JDK 17 + SDK 36) همان دستور CI بدون خطا اجرا شد و ۶ تست واحد سبز بود. تا وقتی Actions سبز نشود و APK روی گوشی نصب و باز نشود، وضعیت به ✅ تغییر نمی‌کند.

## چک‌لیست تست دستگاه هر فاز (الگو)

- نصب تمیز + ورود + خروج + ورود مجدد (بقای session)
- چرخش صفحه / حالت تاریک / فونت بزرگ
- آفلاین: پرواز → باز شدن صفحات کش‌شده → بازگشت آنلاین → تخلیهٔ صف
- عملیات مالی نمونه روی **عضو تست** + تطبیق عددی با وب (همان لحظه)
- اعلان FCM در حالت بسته‌بودن اپ
