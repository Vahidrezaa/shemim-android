package com.shemim.core.network

import kotlinx.serialization.json.JsonObject

/**
 * نتیجهٔ فراخوانی API — WP-0.3.
 *
 * قرارداد پاسخ سرور (`api.php`): همیشه HTTP 200 با JSON «{success: bool, …}»؛
 * خطای منطقی در `error` (متن فارسیِ قابل نمایش به کاربر) و گاهی `code`
 * (مثل `too_many_attempts`) می‌آید. این sealed type همان قرارداد را مدل می‌کند
 * و «خطای شبکه/آفلاین» را از «خطای منطقی سرور» جدا می‌کند (سند ۰۲ §۳).
 */
sealed interface ApiResult {

    /** success == true — بدنهٔ کامل پاسخ به‌صورت خام، برای پارس اکشن‌محور (WP-0.4+). */
    data class Ok(val body: JsonObject) : ApiResult

    /** success == false — خطای منطقی از خود سرور؛ متن `error` فارسی و قابل نمایش است. */
    data class ServerError(
        val error: String? = null,
        val code: String? = null,
        val message: String? = null,
    ) : ApiResult

    /**
     * نشست از سمت سرور معتبر نیست (`auth_required`).
     * کلاینت کوکی را پاک کرده و رویداد سراسری صادر کرده است؛ اپ باید به ورود برگردد
     * (همان `forceLogoutToLoginPage` در auth-flow.js).
     */
    data object AuthRequired : ApiResult

    /** قطع شبکه/تایم‌اوت/عدم دسترسی به سرور — تفکیک «آفلاین» از خطای دیگر. */
    data class NetworkError(
        val detail: String,
        val likelyOffline: Boolean,
    ) : ApiResult

    /** پاسخ غیر-JSON (مثلاً HTML خطای ۵۰۰ هاست). */
    data class MalformedResponse(val detail: String) : ApiResult
}
