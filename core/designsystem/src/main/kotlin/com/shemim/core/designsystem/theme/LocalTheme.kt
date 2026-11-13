package com.shemim.core.designsystem.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * کلید تم فعال — توسط ShemimTheme ارائه می‌شود تا کامپوننت‌هایی که به
 * رنگ‌های اختصاصیِ تم سایت نیاز دارند (رنگ کاشی‌ها، گرادیان) بدون پارامتر
 * اضافه به آن دسترسی داشته باشند.
 */
val LocalShemimThemeKey = staticCompositionLocalOf { ShemimThemeKey.DEFAULT }

/** در دسترس بودن هم‌زمان رنگ‌های کاشیِ تم فعال (tiles سایت). */
val LocalShemimTiles = compositionLocalOf { ShemimThemeKey.DEFAULT.tileColors }
