package com.shemim.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * کارت «سند» — کارت پایهٔ اپ با خط مُهر (سند ۰۳ §۱ و §۴):
 * گوشهٔ ۲۰dp، سطح ۱ ملایم، و خط ۳dp رنگ تأیید در بالای کارت.
 */
@Composable
fun ShemimCard(
    modifier: Modifier = Modifier,
    /** رنگ خط مُهر — پیش‌فرض رنگ اصلی تم. */
    stampColor: Color = MaterialTheme.colorScheme.primary,
    showStamp: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column {
            if (showStamp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(stampColor),
                )
            }
            Column(content = content)
        }
    }
}
