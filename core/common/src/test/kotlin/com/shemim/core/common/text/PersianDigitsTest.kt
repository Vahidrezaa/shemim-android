package com.shemim.core.common.text

import org.junit.Assert.assertEquals
import org.junit.Test

class PersianDigitsTest {

    @Test
    fun `all latin digits convert to persian`() {
        assertEquals("۰۱۲۳۴۵۶۷۸۹", PersianDigits.toPersian("0123456789"))
    }

    @Test
    fun `persian digits convert to latin`() {
        assertEquals("0123456789", PersianDigits.toLatin("۰۱۲۳۴۵۶۷۸۹"))
    }

    @Test
    fun `arabic digits convert to latin`() {
        assertEquals("079", PersianDigits.toLatin("٠٧٩"))
    }

    @Test
    fun `non digits are untouched`() {
        assertEquals("سلام ۱۲ نفر", PersianDigits.toPersian("سلام 12 نفر"))
        assertEquals("12.5-تومان", PersianDigits.toLatin("۱۲.۵-تومان"))
    }

    @Test
    fun `already persian string is idempotent`() {
        assertEquals("۱۴۰۵/۰۸/۲۳", PersianDigits.toPersian(PersianDigits.toPersian("1405/08/23")))
    }

    @Test
    fun `empty string stays empty`() {
        assertEquals("", PersianDigits.toPersian(""))
        assertEquals("", PersianDigits.toLatin(""))
    }
}
