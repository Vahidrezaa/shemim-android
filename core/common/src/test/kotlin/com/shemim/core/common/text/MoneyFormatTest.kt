package com.shemim.core.common.text

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * تست فرمت مبالغ — هم‌ارز formatMoney / addThousandSep / parseInputAmount سایت.
 * (نام متدها به دلیل محدودیت شناسهٔ JVM روی نویسه‌های فارسی ترکیبی، ASCII است.)
 */
class MoneyFormatTest {

    @Test
    fun rial_noChange_latinThousandSep_likeSite() {
        assertEquals("1,234,567", MoneyFormat.format(1_234_567, CurrencyUnit.RIAL))
        assertEquals("0", MoneyFormat.format(0, CurrencyUnit.RIAL))
        assertEquals("500", MoneyFormat.format(500, CurrencyUnit.RIAL))
    }

    @Test
    fun toman_floorDivBy10_likeJsFloor() {
        assertEquals("123,456", MoneyFormat.format(1_234_567, CurrencyUnit.TOMAN))
        // ۱۵ ریال → floor(1.5) = ۱ تومان (رفتار JS)
        assertEquals("1", MoneyFormat.format(15, CurrencyUnit.TOMAN))
        assertEquals("1", MoneyFormat.format(19, CurrencyUnit.TOMAN))
        assertEquals("2", MoneyFormat.format(20, CurrencyUnit.TOMAN))
    }

    @Test
    fun showUnit_persianTitle() {
        assertEquals("1,234,567 ریال", MoneyFormat.format(1_234_567, CurrencyUnit.RIAL, showUnit = true))
        assertEquals("123,456 تومان", MoneyFormat.format(1_234_567, CurrencyUnit.TOMAN, showUnit = true))
    }

    @Test
    fun persianFormat_digitsAndSeparator() {
        assertEquals("۱٬۲۳۴٬۵۶۷ ریال", MoneyFormat.formatFa(1_234_567, CurrencyUnit.RIAL, showUnit = true))
        assertEquals("۱۲۳٬۴۵۶", MoneyFormat.formatFa(1_234_567, CurrencyUnit.TOMAN))
    }

    @Test
    fun thousandSep_digitsOnlyLikeSite() {
        assertEquals("1,234,567", MoneyFormat.addThousandSep("1234567"))
        assertEquals("1,234", MoneyFormat.addThousandSep("1,234")) // کامای قبلی حذف و بازچینی
        assertEquals("", MoneyFormat.addThousandSep(""))
        assertEquals("", MoneyFormat.addThousandSep("abc"))
        assertEquals("12,345", MoneyFormat.addThousandSep("12,345 ریال"))
    }

    @Test
    fun parseInput_stripsComma_tomanX10() {
        assertEquals(1_234L, MoneyFormat.parseInputAmount("1,234", CurrencyUnit.RIAL))
        assertEquals(12_340L, MoneyFormat.parseInputAmount("1,234", CurrencyUnit.TOMAN))
        assertEquals(1_000L, MoneyFormat.parseInputAmount("1000", CurrencyUnit.RIAL))
        assertEquals(10_000L, MoneyFormat.parseInputAmount("1000", CurrencyUnit.TOMAN))
        assertEquals(0L, MoneyFormat.parseInputAmount("", CurrencyUnit.RIAL))
        assertEquals(0L, MoneyFormat.parseInputAmount("abc", CurrencyUnit.RIAL))
        // ارقام فارسی و جداساز «٬» هم پذیرفته می‌شود (ورودی اپ)
        assertEquals(12_340L, MoneyFormat.parseInputAmount("۱٬۲۳۴", CurrencyUnit.TOMAN))
    }

    @Test
    fun displayAndRialConversions() {
        assertEquals(1_000L, MoneyFormat.toRial(100, CurrencyUnit.TOMAN))
        assertEquals(100L, MoneyFormat.toRial(100, CurrencyUnit.RIAL))
        assertEquals(123_456L, MoneyFormat.displayAmount(1_234_567, CurrencyUnit.TOMAN))
        assertEquals(1_234_567L, MoneyFormat.displayAmount(1_234_567, CurrencyUnit.RIAL))
    }

    @Test
    fun fromKey_defaultRial_likeSite() {
        assertEquals(CurrencyUnit.TOMAN, CurrencyUnit.fromKey("toman"))
        assertEquals(CurrencyUnit.RIAL, CurrencyUnit.fromKey("rial"))
        assertEquals(CurrencyUnit.RIAL, CurrencyUnit.fromKey(null))
        assertEquals(CurrencyUnit.RIAL, CurrencyUnit.fromKey(""))
    }
}

/**
 * تست عدد به حروف — خروجی «دقیقاً» مثل numberToWords سایت (بدون حرف اتصال).
 */
class NumberToWordsFaTest {

    @Test
    fun baseNumbers() {
        assertEquals("صفر", NumberToWordsFa.toWords(0))
        assertEquals("هفت", NumberToWordsFa.toWords(7))
        assertEquals("ده", NumberToWordsFa.toWords(10))
        assertEquals("پانزده", NumberToWordsFa.toWords(15))
        assertEquals("بیست", NumberToWordsFa.toWords(20))
    }

    @Test
    fun noConjunction_betweenParts_likeSite() {
        assertEquals("بیست یک", NumberToWordsFa.toWords(21))
        assertEquals("سی چهار", NumberToWordsFa.toWords(34))
        assertEquals("یکصد هجده", NumberToWordsFa.toWords(118))
        assertEquals("نهصد نود نه", NumberToWordsFa.toWords(999))
    }

    @Test
    fun hundredsAndThousands() {
        assertEquals("یکصد", NumberToWordsFa.toWords(100))
        assertEquals("یکصد پنج", NumberToWordsFa.toWords(105))
        assertEquals("دویست", NumberToWordsFa.toWords(200))
        assertEquals("دویست سی چهار", NumberToWordsFa.toWords(234))
        assertEquals("یک هزار", NumberToWordsFa.toWords(1_000))
        assertEquals("یک هزار یک", NumberToWordsFa.toWords(1_001))
        assertEquals("سه هزار چهارصد", NumberToWordsFa.toWords(3_400))
    }

    @Test
    fun millionBillionTrillion() {
        assertEquals("یک میلیون", NumberToWordsFa.toWords(1_000_000))
        assertEquals("یک میلیون دویست سی چهار هزار پانصد شصت هفت", NumberToWordsFa.toWords(1_234_567))
        assertEquals("یک میلیارد", NumberToWordsFa.toWords(1_000_000_000))
        assertEquals("دو بیلیون", NumberToWordsFa.toWords(2_000_000_000_000))
        // تومانِ نمایشیِ ۱۲٬۳۴۵٬۶۷۸ ریال = ۱٬۲۳۴٬۵۶۷ تومان
        assertEquals(
            "یک میلیون دویست سی چهار هزار پانصد شصت هفت",
            NumberToWordsFa.toWords(MoneyFormat.displayAmount(12_345_678, CurrencyUnit.TOMAN)),
        )
    }
}
