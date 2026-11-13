package com.shemim.core.common.date

import com.shemim.core.common.text.PersianDigits

/**
 * قالب‌بندی نمایشیِ تاریخ شمسی با ارقام فارسی.
 *
 * قرارداد: همهٔ منطق تاریخ با ارقام لاتین کار می‌کند (همان سرور)؛ این آبجکت
 * فقط «لایهٔ نمایش» است و رشتهٔ نهایی فارسی می‌سازد.
 */
object PersianDateText {

    /** «۱۴۰۵/۰۸/۲۳» — معادل فارسیِ formatJalali سایت. */
    fun formatJalaliFa(jy: Int, jm: Int, jd: Int): String =
        PersianDigits.toPersian(JalaliCalendar.formatJalali(jy, jm, jd))

    /** «۲۳ آبان ۱۴۰۵». */
    fun formatJalaliLongFa(jy: Int, jm: Int, jd: Int): String =
        "${PersianDigits.toPersian(jd.toString())} ${JalaliCalendar.monthNames[jm - 1]} ${PersianDigits.toPersian(jy.toString())}"

    /** «شنبه، ۲۳ آبان ۱۴۰۵». */
    fun formatJalaliFullFa(date: JalaliDate): String =
        "${JalaliCalendar.weekDayName(date)}، ${formatJalaliLongFa(date.year, date.month, date.day)}"

    /** فاصلهٔ نسبی انسانی: «امروز»، «فردا»، «۳ روز پیش»، «۵ روز مانده»، وگرنه تاریخ کامل. */
    fun relativeFa(date: JalaliDate, today: JalaliDate = JalaliCalendar.todayJalali()): String {
        val diff = JalaliCalendar.daysBetweenJalali(today, date)
        return when {
            diff == 0 -> "امروز"
            diff == 1 -> "فردا"
            diff == -1 -> "دیروز"
            diff > 1 && diff <= 30 -> "${PersianDigits.toPersian(diff.toString())} روز مانده"
            diff < -1 && diff >= -30 -> "${PersianDigits.toPersian((-diff).toString())} روز پیش"
            else -> date.formatLongFa()
        }
    }
}
