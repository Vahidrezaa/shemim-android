package com.shemim.core.common.date

import com.shemim.core.common.text.PersianDigits

/**
 * تقویم شمسی — پورتِ وفادارِ یک‌به‌یک الگوریتم‌های `js/config.js` سایت
 * (توابع miladiToJalali / jalaliToMiladiOnlyForCalculation / monthDaysJalali /
 * addMonthsToJalali / daysBetweenJalali / formatJalali / parseJalali).
 *
 * تفاوت‌های عمداً حداقلی با JS:
 *  - به‌جای شیء Date جاوااسکریپت برای اختلاف روز، از عدد روز جولیَن (JDN) با
 *    ریاضیات صحیح استفاده شده — هم‌ارزِ دقیقِ Math.round(diff/86400000) و بدون
 *    نیاز به java.time (که روی minSdk 24 بدون desugaring در دسترس نیست).
 *  - parseJalali علاوه بر ارقام لاتین، ارقام فارسی/عربی را هم می‌پذیرد (افزودهٔ
 *    اپ؛ سایت فقط لاتین می‌پذیرد — پذیرشِ بیشتر است، نه کمتر).
 *
 * تست‌های JalaliCalendarTest این هم‌ارزی را با تاریخ‌های مرجع رسمی قفل می‌کنند.
 */
object JalaliCalendar {

    /** نام ماه‌ها — همان آرایهٔ monthNames سایت. */
    val monthNames = arrayOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    /** نام روزهای هفته (شنبه‌محور) — افزودهٔ اپ؛ سایت نام روز ندارد ولی تقویم رسمی ایران همین است. */
    val weekDayNames = arrayOf(
        "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه",
    )

    /** همان miladiToJalali سایت — تبدیل میلادی به شمسی. */
    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gDm = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666L + (365L * gy) +
            (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 +
            gd + gDm[gm - 1]
        var jy = -1595 + (33 * (days / 12053)).toInt()
        days %= 12053
        jy += (4 * (days / 1461)).toInt()
        days %= 1461
        if (days > 365) {
            jy += ((days - 1) / 365).toInt()
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + (days / 31).toInt()
            jd = 1 + (days % 31).toInt()
        } else {
            jm = 7 + ((days - 186) / 30).toInt()
            jd = 1 + ((days - 186) % 30).toInt()
        }
        return JalaliDate(jy, jm, jd)
    }

    /** همان jalaliToMiladiOnlyForCalculation سایت — تبدیل شمسی به میلادی. */
    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        var jyL = jy + 1595
        var days = -355668L + (365L * jyL) +
            ((jyL / 33) * 8) + (((jyL % 33) + 3) / 4) + jd +
            (if (jm < 7) (jm - 1) * 31 else (jm - 7) * 30 + 186)
        var gy = (400 * (days / 146097)).toInt()
        days %= 146097
        if (days > 36524) {
            --days // همان --days جاوااسکریپت
            gy += (100 * (days / 36524)).toInt()
            days %= 36524
            if (days >= 365) days++
        }
        gy += (4 * (days / 1461)).toInt()
        days %= 1461
        if (days > 365) {
            gy += ((days - 1) / 365).toInt()
            days = (days - 1) % 365
        }
        var gd = (days + 1).toInt()
        val leapG = (gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)
        val salA = intArrayOf(0, 31, if (leapG) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0
        while (gm < 13 && gd > salA[gm]) {
            gd -= salA[gm]
            gm++
        }
        return Triple(gy, gm, gd)
    }

    /**
     * همان monthDaysJalali سایت: ۶ ماه اول ۳۱، ماه ۷ تا ۱۱ سی روز، اسفند با
     * اختلافِ روزِ ابتدای ماه بعد و همین ماه (دقیقاً همان روش سایت).
     */
    fun monthDaysJalali(year: Int, month: Int): Int {
        if (month <= 6) return 31
        if (month <= 11) return 30
        val (gy1, gm1, gd1) = jalaliToGregorian(year, 12, 1)
        val nextYear = if (month == 12) year + 1 else year
        val nextMonth = if (month == 12) 1 else month + 1
        val (gy2, gm2, gd2) = jalaliToGregorian(nextYear, nextMonth, 1)
        return (jdn(gy2, gm2, gd2) - jdn(gy1, gm1, gd1)).toInt()
    }

    /** سال کبیسهٔ شمسی — هم‌ارز monthDaysJalali(y, 12) == 30 (همان روش مؤثر سایت). */
    fun isLeapJalali(year: Int): Boolean = monthDaysJalali(year, 12) == 30

    /** همان daysBetweenJalali سایت — اختلاف روز (d2 - d1) با ریاضیات صحیح. */
    fun daysBetweenJalali(d1: JalaliDate, d2: JalaliDate): Int {
        val (gy1, gm1, gd1) = jalaliToGregorian(d1.year, d1.month, d1.day)
        val (gy2, gm2, gd2) = jalaliToGregorian(d2.year, d2.month, d2.day)
        return (jdn(gy2, gm2, gd2) - jdn(gy1, gm1, gd1)).toInt()
    }

    /** همان addMonthsToJalali سایت — روزِ خروجی به طول ماه مقصد «گیران» (clamp) می‌شود. */
    fun addMonthsJalali(date: JalaliDate, months: Int): JalaliDate {
        var newYear = date.year
        var newMonth = date.month + months
        while (newMonth > 12) {
            newMonth -= 12
            newYear++
        }
        val maxDay = monthDaysJalali(newYear, newMonth)
        val newDay = minOf(date.day, maxDay)
        return JalaliDate(newYear, newMonth, newDay)
    }

    /** همان formatJalali سایت: «۱۴۰۵/۰۸/۲۳» با ارقام لاتین و صفرِ پیشوند. */
    fun formatJalali(jy: Int, jm: Int, jd: Int): String =
        "$jy/${jm.padded()}/${jd.padded()}"

    /**
     * همان parseJalali سایت؛ به‌علاوه ارقام فارسی/عربی هم پذیرفته می‌شود.
     * خروجی null یعنی رشته تاریخ معتبر نبود — دقیقاً مثل سایت.
     */
    fun parseJalali(str: String): JalaliDate? {
        if (str.isEmpty()) return null
        val latin = PersianDigits.toLatin(str)
        val parts = latin.split("/")
        if (parts.size != 3) return null
        val y = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        val d = parts[2].toIntOrNull() ?: return null
        if (m !in 1..12) return null
        val maxDay = monthDaysJalali(y, m)
        if (d < 1 || d > maxDay) return null
        return JalaliDate(y, m, d)
    }

    /** امروزِ تقویم دستگاه — همان todayJalali سایت (تاریخ محلی، نه UTC). */
    fun todayJalali(calendar: java.util.Calendar = java.util.Calendar.getInstance()): JalaliDate =
        gregorianToJalali(
            calendar.get(java.util.Calendar.YEAR),
            calendar.get(java.util.Calendar.MONTH) + 1,
            calendar.get(java.util.Calendar.DAY_OF_MONTH),
        )

    /** نام روز هفتهٔ یک تاریخ شمسی — افزودهٔ اپ (تقویم رسمی: شنبه شروع هفته). */
    fun weekDayName(date: JalaliDate): String {
        val (gy, gm, gd) = jalaliToGregorian(date.year, date.month, date.day)
        val cal = java.util.Calendar.getInstance()
        cal.clear()
        cal.set(gy, gm - 1, gd)
        val dow = cal.get(java.util.Calendar.DAY_OF_WEEK) // یکشنبه=1 … شنبه=7
        return weekDayNames[dow % 7] // شنبه(7)→۰، یکشنبه(1)→۱، … جمعه(6)→۶
    }

    // ------------------------------------------------------------------
    // عدد روز جولیَن (JDN) — ریاضیات صحیح Fliegel–Van Flandern؛ جایگزینِ
    // دقیقِ شیء Date جاوااسکریپت برای اختلاف روز (بدون DST، بدون desugaring).
    // ------------------------------------------------------------------
    private fun jdn(y: Int, m: Int, d: Int): Long {
        val a = (14 - m) / 12
        val y2 = y + 4800 - a
        val m2 = m + 12 * a - 3
        return d.toLong() + (153L * m2 + 2) / 5 + 365L * y2 + y2 / 4 - y2 / 100 + y2 / 400 - 32045
    }

    private fun Int.padded(): String = if (this < 10) "0$this" else toString()
}
