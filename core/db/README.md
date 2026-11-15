# core:db — پایگاه‌دادهٔ محلی (WP-0.4)

کش سینک Room — ۱۵ موجودیت روی ۱۵ جدول هم‌نام جداول سرور (سند ۰۲ §۴-۶):

- الگوی sync: `replaceAll*` در یک `@Transaction` — چون loadData همیشه appData
  کامل برمی‌گرداند، جایگزینی کامل درست‌تر و ساده‌تر از diff است.
- `fallbackToDestructiveMigration` عمدی نیست — نسخه ۱ بدون exportSchema؛ تا
  فاز ۶ (WP-6.2) که مهاجرت لازم شود، بازسازی = یک sync.
- `AppDataMappers` خالص JVM است (بدون API اندروید): `AppDataDto.toSnapshot()`
  خروجی یکسان با ورودی WP-0.5 (موتور سینک) می‌دهد.
- بدون ForeignKey عمدی: دادهٔ کثیف سرور نباید insert بشکند؛ نظم با تراکنش
  جایگزینی تضمین می‌شود.

تست‌ها: `AppDataMapperTest` (۶ تست نگاشت JSON→Room — با kotlinc محلی سبز) +
`ShemimDatabaseSmokeTest` (دود Robolectric در CI: بازکردن DB، snapshot کامل،
جایگزینی دوم بدون دوبل‌شدن).
