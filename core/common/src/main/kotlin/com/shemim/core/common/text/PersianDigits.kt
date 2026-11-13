package com.shemim.core.common.text

/**
 * تبدیل ارقام لاتین/فارسی/عربی — هم‌ارز رفتار سایت.
 *
 * این ابزار در WP-0.1 فقط برای نمایش نسخهٔ فارسی استفاده می‌شود؛
 * فرمت کامل مبالغ (جداکنندهٔ هزارگان + واحد) در WP-0.6 اضافه می‌شود.
 */
object PersianDigits {

    private val persian = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    private val arabic = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

    /** «123» → «۱۲۳» — هر رقم لاتین به فارسی؛ بقیهٔ نویسه‌ها دست‌نخورده. */
    fun toPersian(input: String): String = buildString(input.length) {
        for (c in input) {
            val d = c - '0'
            if (d in 0..9) append(persian[d]) else append(c)
        }
    }

    /** «۱۲۳» و «١٢٣» → «123» — رقم فارسی و عربی هر دو به لاتین. */
    fun toLatin(input: String): String = buildString(input.length) {
        for (c in input) {
            val p = persian.indexOf(c)
            if (p >= 0) {
                append('0' + p)
                continue
            }
            val a = arabic.indexOf(c)
            if (a >= 0) {
                append('0' + a)
                continue
            }
            append(c)
        }
    }
}
