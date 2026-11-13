package com.shemim.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * سبک‌های مُهر وضعیت — به نقش‌های تم مپ می‌شوند تا با هر ۱۰ تم سازگار باشند.
 */
enum class StatusChipStyle {
    /** تأییدشده / فعال / پرداخت‌شده */
    SUCCESS,

    /** ردشده / معوق / خطا */
    ERROR,

    /** در انتظار / نزدیک سررسید */
    WARNING,

    /** اطلاع / اعلان */
    INFO,

    /** بدون وضعیت خاص */
    NEUTRAL,
}

/**
 * مُهر وضعیت — چیپ کوچک به سبک «مُهر» برای وضعیت‌های مالی
 * (وام فعال، معوق، تأییدشده، …) — سند ۰۳ §۱.
 */
@Composable
fun StatusChip(
    text: String,
    modifier: Modifier = Modifier,
    style: StatusChipStyle = StatusChipStyle.NEUTRAL,
) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (style) {
        StatusChipStyle.SUCCESS -> scheme.primaryContainer to scheme.onPrimaryContainer
        StatusChipStyle.ERROR -> scheme.errorContainer to scheme.onErrorContainer
        StatusChipStyle.WARNING -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        StatusChipStyle.INFO -> scheme.secondaryContainer to scheme.onSecondaryContainer
        StatusChipStyle.NEUTRAL -> scheme.surfaceVariant to scheme.onSurfaceVariant
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content,
        border = BorderStroke(1.dp, content.copy(alpha = 0.25f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
