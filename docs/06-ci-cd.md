# ۰۶ — CI/CD: بیلد با GitHub Actions در ریپوی خصوصی

## ۱. گردش کار بیلد

دو workflow:

### الف) `android-debug.yml` — روی هر push به هر شاخه + pull_request (فاز ۰ تا ۵)
- Setup JDK 17 (Temurin) + Gradle cache
- ساخت `google-services.json` از Secret (اگر فایل نبود، از نسخهٔ placeholder با debug config استفاده می‌شود تا فازهای ۰-۴ بدون Firebase هم بیلد شوند)
- `./gradlew assembleDebug` + اجرای تست‌های واحد (`testDebugUnitTest`)
- آپلود artifact: `Shemim-debug-<run_number>.apk`

### ب) `android-release.yml` — روی تگ `v*` (فاز ۶ به بعد + مایلستون‌ها)
- همان مراحل + `assembleRelease` + امضا با keystore از Secrets
- ساخت GitHub Release با APK پیوست

## ۲. ساخت keystore (یک‌بار؛ دستورات دقیق)

روی کامپیوتر خودتان (JDK نصب باشد):

```bash
keytool -genkeypair -v \
  -keystore shemim-release.keystore \
  -alias shemim \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storetype PKCS12
```
رمز دلخواه بگذارید و فایل را **جایی غیر از ریپو** نگه دارید (رمز و فایل را گم نکنید؛ آپدیت‌های بعدی فقط با همین امضا نصب می‌شوند).

## ۳. Secrets ریپو

| نام Secret | محتوا |
|---|---|
| `KEYSTORE_BASE64` | خروجی `base64 -w0 shemim-release.keystore` |
| `KEYSTORE_PASSWORD` | رمز keystore |
| `KEY_ALIAS` | `shemim` |
| `KEY_PASSWORD` | همان رمز (PKCS12 یکی است) |
| `GOOGLE_SERVICES_JSON_BASE64` | `base64 -w0 google-services.json` (فاز ۵ به بعد) |

خواندن در workflow:

```yaml
- name: Decode keystore (optional)
  run: printf '%s' "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > "$RUNNER_TEMP/shemim-release.keystore"
- name: Decode google-services
  run: mkdir -p app/src/debug && echo "${{ secrets.GOOGLE_SERVICES_JSON_BASE64 }}" | base64 -d > app/google-services.json
```

متغیرهای محیطی که Gradle می‌خواند: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (ورک‌فلو Secretها را decode کرده و به این متغیرها صادر می‌کند).

امضا با `signingConfigs` که اگر متغیرهای محیطی نبودند به debug keystore برگردد (بیلد خصوصی بدون ست‌کردن Secretها نشکند).

## ۴. نسخه‌گذاری

- `versionName`: از `gradle.properties` (مثل `1.0.0`)، فقط با تگ تغییر می‌کند.
- `versionCode`: در CI با شمارهٔ `github.run_number` override می‌شود (تکافزایی و مونوتون تضمین‌شده):
  `-PversionCode=${{ github.run_number }}`
- قاعدهٔ SemVer: `MAJOR.MINOR.PATCH` — فازهای ۰-۵: `0.x.y`، انتشار عمومی: `1.0.0`.
- **checkUpdateConfig:** اپ هنگام شروع `appConfig` را می‌خواند؛ `versionCode < minAppVersion` → صفحهٔ به‌روزرسانی اجباری؛ کمتر از `latest` → بنر اختیاری.

## ۵. کیفیت بیلد (همه در CI)

- `testDebugUnitTest` (تقویم/پول/مپر) — اگر رد شود بیلد release نمی‌گیرد
- `lintDebug` با `abortOnError` فقط برای خطاهای بحرانی (ترجمهٔ ناقص RTL و…)
- R8/Minify برای release (حجم کمتر + محافظت؛ قوانین نگهداشت برای serialization/Room/Compose)

## ۶. توزیع

- GitHub Release (PRIVAT repo) → لینک APK را با اعضای صندوق به‌اشتراک بگذارید (لینک فقط با دسترسی ریپو کار می‌کند؛ برای توزیع عمومی: صفحهٔ `apkUrl` را روی یک سرویس فایل میزبانی کنید یا Release عمومی در ریپوی عمومی دوم — تصمیم شما، سند ۰۷ Q4).
- نصب اولیه: «نصب از منابع ناشناس» روی گوشی کاربران (راهنمای تصویری در فاز ۶ داخل خود اپ/راهنما).

## ۷. ساختار فایل‌های ورودی CI (از فاز ۰)

```
.github/workflows/
├── android-debug.yml
└── android-release.yml
gradle/wrapper/… + gradlew (اجرایی)
gradle.properties          ← versionName
```

> Android SDK از آبان ۱۴۰۵ در سندباکس من نصب است و بیلد محلیِ هر WP پیش از تحویل اجرا می‌شود (WP-0.1 محلی سبز شد)؛ با این حال «تست‌شده» یعنی همین چرخهٔ Actions + دستگاه شما. Workflowها را طوری می‌نویسم که خطای رایج (کش Gradle، تغییر distributionUrl، API level توصیه‌شدهٔ AGP) از قبل حذف شده باشد و لاگ‌های خوانا بدهند.
