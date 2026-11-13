package com.shemim.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shemim.core.designsystem.theme.LocalShemimTiles

/**
 * آواتار عضو — دایرهٔ حرف اول نام + رنگ پایدار از شناسهٔ عضو
 * (رنگ از فیلد tiles تم فعال سایت انتخاب می‌شود — سند ۰۳ §۱ «گرمای خانوادگی»).
 */
@Composable
fun MemberAvatar(
    displayName: String,
    memberId: Long,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    val tiles = LocalShemimTiles.current
    val color = if (tiles.isEmpty()) {
        MaterialTheme.colorScheme.primary
    } else {
        tiles[(memberId % tiles.size).toInt().let { if (it < 0) it + tiles.size else it }]
    }
    Box(
        modifier = modifier
            .size(size)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = displayName.trim().firstOrNull()?.toString() ?: "؟",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}
