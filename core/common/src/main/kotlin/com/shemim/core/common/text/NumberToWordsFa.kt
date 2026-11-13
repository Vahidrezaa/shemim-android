package com.shemim.core.common.text

/**
 * عدد به حروف فارسی — پورتِ وفادارِ numberToWords در js/config.js سایت.
 *
 * نکتهٔ وفاداری: نسخهٔ سایت بین اجزا هیچ حرف اتصالی نمی‌گذارد؛
 * مثلاً ۱۲۳ → «یکصد بیست سه» (نه «یکصد و بیست و سه»). این رفتار عمداً
 * حفظ شده تا خروجی اپ با وب یکی باشد؛ اگر روزی در وب اصلاح شود، همین‌جا
 * فقط joinSeparator باید عوض شود. (یافتهٔ تحلیل — سند ۰۷.)
 */
object NumberToWordsFa {

    private val units = arrayOf("", "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه")
    private val teens = arrayOf("ده", "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده")
    private val tens = arrayOf("", "", "بیست", "سی", "چهل", "پنجاه", "شصت", "هفتاد", "هشتاد", "نود")
    private val hundreds = arrayOf("", "یکصد", "دویست", "سیصد", "چهارصد", "پانصد", "ششصد", "هفتصد", "هشتصد", "نهصد")
    private val thousands = arrayOf("", "هزار", "میلیون", "میلیارد", "بیلیون")

    /** حرف اتصال بین اجزا — «» یعنی فقط فاصله (رفتار فعلی سایت). */
    private const val joinSeparator = ""

    fun toWords(num: Long): String {
        if (num == 0L) return "صفر"
        require(num > 0) { "عدد منفی پشتیبانی نمی‌شود (مثل سایت): $num" }

        var result = ""
        var chunkIndex = 0
        var remaining = num
        while (remaining > 0) {
            val chunk = (remaining % 1000).toInt()
            if (chunk > 0) {
                val chunkText = convertChunk(chunk)
                result = if (chunkIndex > 0) {
                    "$chunkText ${joinWithSep(thousands[chunkIndex])} $result"
                } else {
                    "$chunkText $result"
                }
            }
            remaining /= 1000
            chunkIndex++
        }
        return result.trim()
    }

    private fun joinWithSep(word: String): String =
        if (joinSeparator.isEmpty()) word else "$joinSeparator $word"

    /** همان convertChunk سایت: صدگان [جداساز] دهگان [جداساز] یکگان — با فاصله. */
    private fun convertChunk(n: Int): String {
        if (n == 0) return ""
        val sb = StringBuilder()
        val h = n / 100
        val rest = n % 100
        if (h > 0) sb.append(joinWithSep(hundreds[h])).append(' ')
        if (rest in 10..19) {
            sb.append(joinWithSep(teens[rest - 10])).append(' ')
        } else {
            val t = rest / 10
            val u = rest % 10
            if (t > 0) sb.append(joinWithSep(tens[t])).append(' ')
            if (u > 0) sb.append(joinWithSep(units[u])).append(' ')
        }
        return sb.toString().trim()
    }
}
