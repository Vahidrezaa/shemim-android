package com.shemim.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * تم شمیم — WP-0.2 (جایگزین نسخهٔ موقتِ فاز صفر).
 *
 * - ۱۰ تم سایت با همان کلیدها؛ پالت روشن/تیرهٔ هر تم از الگوریتم رسمی متریال
 *   تولید و به‌صورت کد صریح در ShemimColorSchemes آمده (سند ۰۳ §۲).
 * - حالت تیره: دنبال سیستم + انتخاب دستی (سیستم/روشن/تیره).
 * - «مشکی و طلایی» طبق وب همیشه تیره می‌ماند.
 * - Dynamic Color (کاغذدیواری اندروید ۱۲+): پیش‌فرض خاموش (سند ۰۳)، انتخابی.
 */
@Composable
fun ShemimTheme(
    themeKey: ShemimThemeKey = ShemimThemeKey.DEFAULT,
    darkMode: DarkModePreference = DarkModePreference.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (darkMode) {
        DarkModePreference.SYSTEM -> systemDark
        DarkModePreference.LIGHT -> false
        DarkModePreference.DARK -> true
    } || themeKey.alwaysDark

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> themeKey.darkScheme()
        else -> themeKey.lightScheme()
    }

    ProvideShemimLocals(themeKey) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ShemimTypography,
            content = content,
        )
    }
}

/** ارائهٔ کلید تم و رنگ کاشی‌ها به کامپوننت‌ها (MemberAvatar و …). */
@Composable
private fun ProvideShemimLocals(themeKey: ShemimThemeKey, content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalShemimThemeKey provides themeKey,
        LocalShemimTiles provides themeKey.tileColors,
    ) {
        content()
    }
}

@Composable
private fun ShemimThemeKey.lightScheme() = remember(this) { lightSchemeOf(this) }

@Composable
private fun ShemimThemeKey.darkScheme() = remember(this) { darkSchemeOf(this) }

internal fun lightSchemeOf(key: ShemimThemeKey) = when (key) {
    ShemimThemeKey.EMERALD -> EmeraldLightColors
    ShemimThemeKey.ROYAL -> RoyalLightColors
    ShemimThemeKey.BURGUNDY -> BurgundyLightColors
    ShemimThemeKey.TURQUOISE -> TurquoiseLightColors
    ShemimThemeKey.POMEGRANATE -> PomegranateLightColors
    ShemimThemeKey.BLACKGOLD -> BlackgoldLightColors
    ShemimThemeKey.LAPIS -> LapisLightColors
    ShemimThemeKey.SAFFRON -> SaffronLightColors
    ShemimThemeKey.FOREST -> ForestLightColors
    ShemimThemeKey.COSMIC -> CosmicLightColors
}

internal fun darkSchemeOf(key: ShemimThemeKey) = when (key) {
    ShemimThemeKey.EMERALD -> EmeraldDarkColors
    ShemimThemeKey.ROYAL -> RoyalDarkColors
    ShemimThemeKey.BURGUNDY -> BurgundyDarkColors
    ShemimThemeKey.TURQUOISE -> TurquoiseDarkColors
    ShemimThemeKey.POMEGRANATE -> PomegranateDarkColors
    ShemimThemeKey.BLACKGOLD -> BlackgoldDarkColors
    ShemimThemeKey.LAPIS -> LapisDarkColors
    ShemimThemeKey.SAFFRON -> SaffronDarkColors
    ShemimThemeKey.FOREST -> ForestDarkColors
    ShemimThemeKey.COSMIC -> CosmicDarkColors
}
