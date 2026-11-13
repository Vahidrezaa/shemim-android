package com.shemim.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * تم موقتِ فاز صفر — فقط پالت «زمرد» (پیش‌فرض ADR-010) به‌صورت دستی.
 *
 * در WP-0.2 ده تم کامل سایت (روشن/تیره + انتخاب کاربر + sync با updateMyTheme)
 * جایگزین این نسخهٔ ساده می‌شود (سند ۰۳ §۲).
 */

private val LightColors = lightColorScheme(
    primary = Color(0xFF07654F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9CF2D0),
    onPrimaryContainer = Color(0xFF002017),
    secondary = Color(0xFF4B635B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE8DD),
    onSecondaryContainer = Color(0xFF072019),
    tertiary = Color(0xFF8A5300), // طلایی شامپاین — هم‌خانوادهٔ تم زمرد
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCBD),
    onTertiaryContainer = Color(0xFF2C1700),
    background = Color(0xFFF5FBF6),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF5FBF6),
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDBE5DE),
    onSurfaceVariant = Color(0xFF3F4944),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D5B4),
    onPrimary = Color(0xFF003827),
    primaryContainer = Color(0xFF00513B),
    onPrimaryContainer = Color(0xFF9CF2D0),
    secondary = Color(0xFFB1CCC1),
    onSecondary = Color(0xFF1D352D),
    secondaryContainer = Color(0xFF334B43),
    onSecondaryContainer = Color(0xFFCDE8DD),
    tertiary = Color(0xFFFFB868),
    onTertiary = Color(0xFF492900),
    tertiaryContainer = Color(0xFF673D00),
    onTertiaryContainer = Color(0xFFFFDCBD),
    background = Color(0xFF0F1512),
    onBackground = Color(0xFFDEE4DF),
    surface = Color(0xFF0F1512),
    onSurface = Color(0xFFDEE4DF),
    surfaceVariant = Color(0xFF3F4944),
    onSurfaceVariant = Color(0xFFBEC9C2),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun ShemimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
