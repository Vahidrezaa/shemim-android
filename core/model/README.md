# core:model — مدل‌های داده (WP-0.4)

DTOهای kotlinx.serialization برای قرارداد `loadData` — تطبیق ۱:۱ با
`api/data-helpers.php` (سند ۰۲ §۳-۴):

- `AppDataDto` ریشهٔ appData؛ همهٔ فیلدها default دارند تا کلید جدید سرور
  پارس را نشکند (این لایه فقط کش است و از سرور بازسازی می‌شود).
- نام‌گذاری عمدی: requests (V1) نام‌گذاری **مخلوط** سرور را حفظ می‌کند و
  requestsV2 ردیف **خام** snake_case است — با `@SerialName` صریح.
- فیلدهای مشروط (fatherName/nationalCode/isSupervisor) nullable با default؛
  غیبتشان برای بینندهٔ بدون مجوز طبیعی است.
- خلاصه‌های بیننده‌محور (`ownerPower`/`quizPoints`/`rfWatch`) فعلاً `JsonObject`
  شفاف‌اند و در فازهای مربوط (۳.۴/۴.۷) مدل دقیق می‌گیرند.
- `AppDataJson` پیکربندی مشترک (ignoreUnknownKeys + coerceInputValues).

تست: `AppDataDtoParseTest` — ۹ تست پارس با فیکسچر «کثیف» (کلید ناشناخته،
null صریح، فیلد غایب).
