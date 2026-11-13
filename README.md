# شمیم — اپلیکیشن اندروید صندوق قرض‌الحسنه امیرالمؤمنین(ع)

اپلیکیشن نیتیو اندروید (Kotlin + Jetpack Compose + Material 3) برای صندوق قرض‌الحسنه.
این اپ **جایگزین وب‌سایت نیست** — روی همان دیتابیس و همان API وب (`api.php?action=…`) کار می‌کند
و تجربه‌ای روان و نیتیو می‌سازد. شرح کامل معماری در [`docs/`](docs/) است.

> ریپو باید **خصوصی** بماند (اسناد داخلی صندوق + جزئیات سیستم).

## وضعیت فعلی

| فاز | وضعیت |
|---|---|
| فاز ۰ — زیرساخت و اسکلت | 🟡 در حال اجرا (WP-0.1 و 0.8 ✅ سبز در Actions؛ WP-0.2 و 0.6 تحویل شد، منتظر Actions) |
| فاز ۱ تا ۶ | ⚪ شروع نشده |

وضعیت لحظه‌ای همهٔ بسته‌های کاری: [`docs/04-feature-map.md`](docs/04-feature-map.md) (سند زنده).

## ساختار

```
app/                  ← MainActivity، مونتاژ نهایی
core/
  common/             ← ابزارهای خالص Kotlin (ارقام فارسی، …) — JVM
  designsystem/       ← ۱۰ تم سایت + Vazirmatn FD + کامپوننت‌های پایه (WP-0.2 ✅ تحویل)
  common/             ← تقویم شمسی، مبالغ، ارقام فارسی (WP-0.6 ✅ تحویل — ۳۸ تست واحد)
  model/              ← DTOهای appData (WP-0.4)
  network/            ← Retrofit/OkHttp/CookieJar (WP-0.3)
  db/                 ← Room (WP-0.4)
  sync/               ← SyncEngine + Outbox (WP-0.5)
  datastore/          ← session/تنظیمات (WP-0.3)
feature/              ← ۲۵ ماژول ویژگی (اسکلت خالی — هر کدام در فاز خودش پر می‌شود)
server/               ← فایل‌های PHP سمت هاست (فاز ۵ — سند ۰۵)
docs/                 ← اسناد پروژه (۰۰ تا ۰۷)
.github/workflows/    ← CI بیلد دیباگ + ریلیز امضاشده
```

نسخه‌های قفل‌شدهٔ ابزارها: `gradle/libs.versions.toml`
(AGP 8.10.1 · Gradle 8.13 · Kotlin 2.1.21 · compileSdk 36 · minSdk 24 · Java 17)

## بیلد

```bash
./gradlew :app:assembleDebug          # APK دیباگ
./gradlew test                        # تست‌های واحد
./gradlew :app:lintDebug              # lint
```

پیش‌نیاز محلی: JDK 17 + Android SDK (platform 36). یا فقط Android Studio بروز باز کنید.

## CI/CD (GitHub Actions)

- **هر push** → `Android Debug Build`: ساخت APK دیباگ + تست + lint → artifact قابل دانلود
- **تگ `v*`** → `Android Release Build`: APK امضاشده + GitHub Release
  (اگر Secretهای امضا ست نشده باشند، با کلید debug امضا می‌شود تا بیلد نشکند — سند ۰۶)

Secretهای لازم (سند ۰۶ §۳): `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
و از فاز ۵: `GOOGLE_SERVICES_JSON_BASE64`.

## مستندات

| سند | محتوا |
|---|---|
| [00-PLAN](docs/00-PLAN.md) | نقشهٔ کلی، فازها، معیارهای پذیرش |
| [01-system-analysis](docs/01-system-analysis.md) | تحلیل سیستم وب (API، دیتابیس، نقش‌ها) |
| [02-architecture](docs/02-architecture.md) | معماری اپ: ماژول‌ها، همگام‌سازی، آفلاین، امنیت |
| [03-design-system](docs/03-design-system.md) | زبان طراحی: ۱۰ تم، تایپوگرافی، کامپوننت‌ها |
| [04-feature-map](docs/04-feature-map.md) | نقشهٔ پوشش قابلیت‌ها + وضعیت اجرا (سند زنده) |
| [05-backend-additions](docs/05-backend-additions.md) | تغییرات حداقلی سرور |
| [06-ci-cd](docs/06-ci-cd.md) | بیلد، امضا، Secrets، انتشار |
| [07-risks-and-decisions](docs/07-risks-and-decisions.md) | ریسک‌ها، سؤالات، ADRها |
