package com.shemim.core.common.text

/**
 * واحد پول — همان getCurrencyUnit سایت: تنظیمات سرور ('rial' یا 'toman')،
 * پیش‌فرض «ریال». سرور همهٔ مبالغ را به «ریال» نگه می‌دارد؛ «تومان» فقط
 * حالت نمایش است (تقسیم بر ۱۰).
 */
enum class CurrencyUnit(val key: String, val faTitle: String) {
    RIAL("rial", "ریال"),
    TOMAN("toman", "تومان");

    companion object {
        /** هم‌ارز `(appData.settings.currency) || 'rial'` سایت. */
        fun fromKey(value: String?): CurrencyUnit =
            if (value == TOMAN.key) TOMAN else RIAL
    }
}

/**
 * فرمت مبالغ — پورتِ وفادارِ formatMoney / addThousandSep / parseInputAmount /
 * readAmountAmount از js/config.js سایت.
 *
 * قرارداد مبالغ (همان وب):
 *  - همهٔ مبالغِ ذخیره‌شده و ردوبدل‌شده با سرور «ریال» هستند (Long).
 *  - در حالت تومان فقط «نمایش» با floor(ریال ÷ ۱۰) و «ورودی» با ×۱۰ تبدیل می‌شود
 *    — دقیقاً مثل formatMoney/parseInputAmount سایت (Math.floor برای تقسیم).
 *
 * جداسازی ارقام: این لایه همان خروجی لاتینِ سایت را می‌دهد («1,234,567»)؛
 * تبدیل به ارقام فارسی و جداساز «٬» فقط در لایهٔ نمایش
 * (MoneyDisplay در core:designsystem) انجام می‌شود.
 */
object MoneyFormat {

    /** همان formatMoney سایت: تبدیل به واحد نمایشی + جداکنندهٔ هزارگان؛ اختیاراً با واحد. */
    fun format(amountRial: Long, unit: CurrencyUnit, showUnit: Boolean = false): String {
        val display = displayAmount(amountRial, unit)
        val grouped = addThousandSep(display.toString())
        return if (showUnit) "$grouped ${unit.faTitle}" else grouped
    }

    /** مقدار نمایشی در واحد انتخابی — Math.floor(amount/10) سایت برای تومان. */
    fun displayAmount(amountRial: Long, unit: CurrencyUnit): Long =
        if (unit == CurrencyUnit.TOMAN) amountRial.floorDiv(10) else amountRial

    /** مقدار ریالیِ ذخیره‌شده از ورودی کاربر در واحد نمایشی — ×۱۰ سایت برای تومان. */
    fun toRial(displayValue: Long, unit: CurrencyUnit): Long =
        if (unit == CurrencyUnit.TOMAN) displayValue * 10 else displayValue

    /**
     * همان addThousandSep سایت: فقط ارقام باقی می‌مانند و هر ۳ رقم از راست
     * کاما می‌گیرند. رشتهٔ بدون رقم → رشتهٔ خالی.
     */
    fun addThousandSep(value: String): String {
        val n = value.filter { it in '0'..'9' }
        if (n.isEmpty()) return ""
        return buildString(n.length + n.length / 3) {
            val firstLen = n.length % 3
            var i = 0
            if (firstLen != 0) {
                append(n, 0, firstLen)
                i = firstLen
                if (n.length > firstLen) append(',')
            }
            while (i < n.length) {
                append(n, i, i + 3)
                i += 3
                if (i < n.length) append(',')
            }
        }
    }

    /**
     * همان parseInputAmount سایت: کاماها حذف، عدد، و اگر واحد تومان بود ×۱۰ به ریال.
     * ارقام فارسی/عربی و جداساز هزارگان فارسی («٬») هم پذیرفته می‌شوند (ورودی اپ).
     */
    fun parseInputAmount(input: String, unit: CurrencyUnit): Long {
        val latin = PersianDigits.toLatin(input)
        val cleaned = latin.replace(",", "").replace("٬", "")
        val amount = cleaned.toLongOrNull() ?: 0L
        return toRial(amount, unit)
    }

    /** فرمت نمایشی فارسی: ارقام فارسی + جداساز «٬» + واحد — برای کامپوننت AmountText. */
    fun formatFa(amountRial: Long, unit: CurrencyUnit, showUnit: Boolean = false): String {
        val latin = format(amountRial, unit, showUnit = false)
        val fa = PersianDigits.toPersian(latin).replace(",", "٬")
        return if (showUnit) "$fa ${unit.faTitle}" else fa
    }
}
