package com.shemim.core.common.date

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * تست‌های تقویم شمسی — WP-0.6.
 *
 * ۱) لنگرهای تقویم رسمی ایران (نوروزها و روز کبیسهٔ ۱۴۰۳).
 * ۲) رفت‌وبرگشت کامل هر ۱۹ روز در بازهٔ ۱۹۹۰ تا ۲۰۴۰ میلادی.
 * ۳) قواعد ماه/کبیسه/جمع‌ماه/اختلاف روز — هم‌ارز توابع js/config.js.
 * (نام متدها به دلیل محدودیت شناسهٔ JVM روی نویسه‌های فارسی ترکیبی، ASCII است.)
 */
class JalaliCalendarTest {

    // ---------- ۱) لنگرهای رسمی ----------

    @Test
    fun nowruz1403_isMarch20_2024() {
        assertEquals(JalaliDate(1403, 1, 1), JalaliCalendar.gregorianToJalali(2024, 3, 20))
    }

    @Test
    fun year1403_isLeap_esfand30_isMarch20_2025() {
        assertEquals(JalaliDate(1403, 12, 30), JalaliCalendar.gregorianToJalali(2025, 3, 20))
        assertTrue(JalaliCalendar.isLeapJalali(1403))
        assertEquals(30, JalaliCalendar.monthDaysJalali(1403, 12))
    }

    @Test
    fun nowruz1404_isMarch21_2025() {
        assertEquals(JalaliDate(1404, 1, 1), JalaliCalendar.gregorianToJalali(2025, 3, 21))
    }

    @Test
    fun nowruz1405_isMarch21_2026() {
        assertEquals(JalaliDate(1405, 1, 1), JalaliCalendar.gregorianToJalali(2026, 3, 21))
    }

    @Test
    fun docDate_23Aban1405_isNov14_2026() {
        assertEquals(JalaliDate(1405, 8, 23), JalaliCalendar.gregorianToJalali(2026, 11, 14))
        val back = JalaliCalendar.jalaliToGregorian(1405, 8, 23)
        assertEquals(Triple(2026, 11, 14), back)
    }

    // ---------- ۲) رفت‌وبرگشت کامل ----------

    @Test
    fun roundtrip_every19days_1990_to_2040() {
        val jd = Calendar.getInstance().apply { set(1990, Calendar.JANUARY, 1) }
        val end = Calendar.getInstance().apply { set(2040, Calendar.DECEMBER, 31) }
        var checked = 0
        while (!jd.after(end)) {
            val gy = jd.get(Calendar.YEAR)
            val gm = jd.get(Calendar.MONTH) + 1
            val gd = jd.get(Calendar.DAY_OF_MONTH)
            val j = JalaliCalendar.gregorianToJalali(gy, gm, gd)
            val back = JalaliCalendar.jalaliToGregorian(j.year, j.month, j.day)
            assertEquals("برگشت $gy/$gm/$gd", Triple(gy, gm, gd), back)
            checked++
            jd.add(Calendar.DAY_OF_MONTH, 19)
        }
        assertTrue("حداقل ۵۰۰ تاریخ بررسی شود؛ شد: $checked", checked > 500)
    }

    @Test
    fun monthNames_sameArrayAsSite() {
        assertEquals("فروردین", JalaliCalendar.monthNames[0])
        assertEquals("اسفند", JalaliCalendar.monthNames[11])
        assertEquals(12, JalaliCalendar.monthNames.size)
    }

    // ---------- ۳) قواعد ماه و کبیسه ----------

    @Test
    fun monthLengths_firstSix31_rest30() {
        for (m in 1..6) assertEquals(31, JalaliCalendar.monthDaysJalali(1405, m))
        for (m in 7..11) assertEquals(30, JalaliCalendar.monthDaysJalali(1405, m))
    }

    @Test
    fun knownLeapYears_of33YearCycle() {
        // چرخهٔ ۳۳ سالهٔ الگوریتم سایت: ۱۳۹۹، ۱۴۰۳، ۱۴۰۸ کبیسه‌اند؛ ۱۴۰۰ و ۱۴۰۴ نیستند.
        assertTrue(JalaliCalendar.isLeapJalali(1399))
        assertTrue(JalaliCalendar.isLeapJalali(1403))
        assertTrue(JalaliCalendar.isLeapJalali(1408))
        assertTrue(!JalaliCalendar.isLeapJalali(1400))
        assertTrue(!JalaliCalendar.isLeapJalali(1404))
        assertEquals(29, JalaliCalendar.monthDaysJalali(1404, 12))
    }

    @Test
    fun monthLength_matchesDistanceOfMonthStarts() {
        for (y in 1403..1408) {
            for (m in 1..12) {
                val start = JalaliDate(y, m, 1)
                val next = if (m == 12) JalaliDate(y + 1, 1, 1) else JalaliDate(y, m + 1, 1)
                val expected = JalaliCalendar.daysBetweenJalali(start, next)
                assertEquals("طول $y/$m", expected, JalaliCalendar.monthDaysJalali(y, m))
            }
        }
    }

    // ---------- ۴) اختلاف روز و جمع ماه ----------

    @Test
    fun year1403_has366_days_1404_has365() {
        assertEquals(366, JalaliCalendar.daysBetweenJalali(JalaliDate(1403, 1, 1), JalaliDate(1404, 1, 1)))
        assertEquals(365, JalaliCalendar.daysBetweenJalali(JalaliDate(1404, 1, 1), JalaliDate(1405, 1, 1)))
    }

    @Test
    fun dayDiff_negativeForReverseDirection() {
        val d1 = JalaliDate(1405, 8, 23)
        val d2 = JalaliDate(1405, 9, 1)
        // آبان ۳۰ روزه است: ۲۳ آبان → ۱ آذر = ۸ روز
        assertEquals(8, JalaliCalendar.daysBetweenJalali(d1, d2))
        assertEquals(-8, JalaliCalendar.daysBetweenJalali(d2, d1))
    }

    @Test
    fun addMonths_clampsDayToTargetMonthLength() {
        // ۳۰ اسفند ۱۴۰۳ + ۲ ماه → اردیبهشت ۱۴۰۴ (۳۱ روزه) → روز ۳۰ می‌ماند
        assertEquals(JalaliDate(1404, 2, 30), JalaliCalendar.addMonthsJalali(JalaliDate(1403, 12, 30), 2))
        // ۳۰ اسفند ۱۴۰۳ + ۱۴ ماه → ۱۴۰۳/۱۲ + ۱۴ = ۱۴۰۵/۰۲ (۳۱ روزه) → روز ۳۰
        assertEquals(JalaliDate(1405, 2, 30), JalaliCalendar.addMonthsJalali(JalaliDate(1403, 12, 30), 14))
        // ۳۰ اسفند ۱۴۰۳ + ۱۲ ماه → اسفند ۱۴۰۴ (۲۹ روزه) → روز ۲۹ (گیراندنِ روز)
        assertEquals(JalaliDate(1404, 12, 29), JalaliCalendar.addMonthsJalali(JalaliDate(1403, 12, 30), 12))
        // ۳۱ خرداد + ۴ ماه → مهر (۳۰ روزه) → روز ۳۰
        assertEquals(JalaliDate(1405, 7, 30), JalaliCalendar.addMonthsJalali(JalaliDate(1405, 3, 31), 4))
        // ۳۱ خرداد + ۱ ماه → تیر (۳۱ روزه) → روز ۳۱ می‌ماند
        assertEquals(JalaliDate(1405, 4, 31), JalaliCalendar.addMonthsJalali(JalaliDate(1405, 3, 31), 1))
    }

    // ---------- ۵) قالب و تجزیه ----------

    @Test
    fun formatJalali_paddedLatinDigits_likeSite() {
        assertEquals("1405/08/23", JalaliCalendar.formatJalali(1405, 8, 23))
        assertEquals("1403/01/09", JalaliCalendar.formatJalali(1403, 1, 9))
    }

    @Test
    fun parseJalali_latinPersianAndNoPadding() {
        assertEquals(JalaliDate(1405, 8, 23), JalaliCalendar.parseJalali("1405/08/23"))
        assertEquals(JalaliDate(1405, 8, 23), JalaliCalendar.parseJalali("1405/8/23"))
        assertEquals(JalaliDate(1405, 8, 23), JalaliCalendar.parseJalali("۱۴۰۵/۰۸/۲۳"))
    }

    @Test
    fun parseJalali_invalidInputs_null() {
        assertNull(JalaliCalendar.parseJalali(""))
        assertNull(JalaliCalendar.parseJalali("1405"))
        assertNull(JalaliCalendar.parseJalali("1405/13/01"))
        assertNull(JalaliCalendar.parseJalali("1404/12/30")) // ۱۴۰۴ کبیسه نیست
        assertNull(JalaliCalendar.parseJalali("1405/00/10"))
        assertNull(JalaliCalendar.parseJalali("1405/01/32"))
        assertNull(JalaliCalendar.parseJalali("abc/def/ghi"))
    }

    // ---------- ۶) امروز و روز هفته ----------

    @Test
    fun todayJalali_withInjectedCalendar() {
        val cal = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 14, 12, 0) }
        assertEquals(JalaliDate(1405, 8, 23), JalaliCalendar.todayJalali(cal))
    }

    @Test
    fun weekDayNames_saturdayStartsWeek() {
        assertEquals("شنبه", JalaliCalendar.weekDayName(JalaliDate(1405, 8, 23)))
        assertEquals("جمعه", JalaliCalendar.weekDayName(JalaliDate(1405, 8, 22)))
    }
}

class PersianDateTextTest {

    @Test
    fun persianFormats() {
        val d = JalaliDate(1405, 8, 23)
        assertEquals("۱۴۰۵/۰۸/۲۳", d.formatFa())
        assertEquals("۲۳ آبان ۱۴۰۵", d.formatLongFa())
        assertEquals("شنبه، ۲۳ آبان ۱۴۰۵", PersianDateText.formatJalaliFullFa(d))
    }

    @Test
    fun relativePhrases() {
        val today = JalaliDate(1405, 8, 23)
        assertEquals("امروز", PersianDateText.relativeFa(JalaliDate(1405, 8, 23), today))
        assertEquals("فردا", PersianDateText.relativeFa(JalaliDate(1405, 8, 24), today))
        assertEquals("دیروز", PersianDateText.relativeFa(JalaliDate(1405, 8, 22), today))
        assertEquals("۵ روز مانده", PersianDateText.relativeFa(JalaliDate(1405, 8, 28), today))
        assertEquals("۱۰ روز پیش", PersianDateText.relativeFa(JalaliDate(1405, 8, 13), today))
        // خارج از بازهٔ ۳۰ روز → تاریخ کامل
        assertEquals("۱۵ مهر ۱۴۰۵", PersianDateText.relativeFa(JalaliDate(1405, 7, 15), today))
    }
}
