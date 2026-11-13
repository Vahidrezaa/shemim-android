package com.shemim.core.designsystem.component

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.shemim.core.common.text.CurrencyUnit
import com.shemim.core.common.text.MoneyFormat

/**
 * رنگِ جهتِ مبلغ — همان قرارداد رنگی ثابتِ سایت (getTransactionTypeTheme):
 * واریز سبز، برداشت سرخ؛ مستقل از تم (سند ۰۳). نسخهٔ تیره برای دارک‌مود.
 */
@Immutable
enum class AmountDirection {
    POSITIVE, // واریز / درآمد
    NEGATIVE, // برداشت / هزینه
    NEUTRAL; // موجودی / بدون جهت
}

private val positiveLight = Color(0xFF15803D) // text رنگ واریز در سایت
private val negativeLight = Color(0xFFB91C1C) // text رنگ برداشت در سایت
private val positiveDark = Color(0xFF4ADE80) // border واریز در سایت
private val negativeDark = Color(0xFFF87171) // border برداشت در سایت

@Composable
private fun AmountDirection.color(): Color = when (this) {
    AmountDirection.POSITIVE -> if (isSystemInDarkTheme()) positiveDark else positiveLight
    AmountDirection.NEGATIVE -> if (isSystemInDarkTheme()) negativeDark else negativeLight
    AmountDirection.NEUTRAL -> MaterialTheme.colorScheme.onSurface
}

/**
 * متن مبلغ مالی — رقم‌های فارسی، جداساز «٬»، واحد، اعداد ستونی (tnum) و
 * رنگ جهت (واریز/برداشت). خروجی عددی همان MoneyFormat سایت است.
 *
 * @param amountRial مبلغ به «ریال» (قرارداد ذخیره‌سازی سرور).
 * @param unit واحد نمایش (تنظیم صندوق).
 */
@Composable
fun AmountText(
    amountRial: Long,
    unit: CurrencyUnit,
    modifier: Modifier = Modifier,
    direction: AmountDirection = AmountDirection.NEUTRAL,
    showUnit: Boolean = true,
    showSign: Boolean = false,
    style: TextStyle = MaterialTheme.typography.titleLarge,
) {
    val text = MoneyFormat.formatFa(amountRial, unit, showUnit = showUnit)
    val sign = when {
        !showSign -> ""
        direction == AmountDirection.POSITIVE -> "+"
        direction == AmountDirection.NEGATIVE -> "−"
        else -> ""
    }
    Text(
        text = "$sign$text",
        modifier = modifier,
        color = direction.color(),
        style = style.copy(
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = "tnum", // ارقام ستونی برای تراز مبالغ (سند ۰۳ §۳)
            fontSize = style.fontSize * 1.05f, // خوانایی مالی: کمی درشت‌تر از متن هم‌اندازه
        ),
    )
}

/** پیش‌نمایش مبلغ با «عدد به حروف» زیر آن — مثل فرم‌های مبلغ سایت. */
@Composable
fun AmountWithWordsText(
    amountRial: Long,
    unit: CurrencyUnit,
    modifier: Modifier = Modifier,
    direction: AmountDirection = AmountDirection.NEUTRAL,
) {
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        AmountText(amountRial = amountRial, unit = unit, direction = direction)
        val words = com.shemim.core.common.text.NumberToWordsFa.toWords(
            MoneyFormat.displayAmount(amountRial, unit),
        )
        Text(
            text = "$words ${unit.faTitle}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
