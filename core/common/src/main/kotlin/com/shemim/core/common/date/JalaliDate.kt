package com.shemim.core.common.date

/**
 * تاریخ شمسی — نوع دادهٔ پایهٔ تقویم اپ.
 *
 * سال/ماه/روز همیشه با ارقام لاتین در حافظه نگه داشته می‌شوند (همان قرارداد
 * سرور و js/config.js سایت)؛ تبدیل به ارقام فارسی فقط در لایهٔ نمایش انجام می‌شود.
 */
data class JalaliDate(
    val year: Int,
    val month: Int,
    val day: Int,
) {
    init {
        require(month in 1..12) { "ماه نامعتبر: $month" }
        require(day in 1..31) { "روز نامعتبر: $day" }
    }

    /** نام ماه — هم‌ارز getMonthName سایت. */
    val monthName: String get() = JalaliCalendar.monthNames[month - 1]

    /** «۱۴۰۵/۰۸/۲۳» با ارقام فارسی (نمایشی). */
    fun formatFa(): String = PersianDateText.formatJalaliFa(year, month, day)

    /** «۲۳ آبان ۱۴۰۵» با ارقام فارسی (نمایشی). */
    fun formatLongFa(): String = PersianDateText.formatJalaliLongFa(year, month, day)

    companion object {
        /** همان parseJalali سایت + پذیرش ارقام فارسی/عربی. */
        fun parse(text: String): JalaliDate? = JalaliCalendar.parseJalali(text)
    }
}
