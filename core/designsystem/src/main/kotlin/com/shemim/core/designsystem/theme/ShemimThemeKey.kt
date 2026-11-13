package com.shemim.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * تم‌های اپ — همان ۱۰ تم سایت (js/themes.js)، با همان کلیدها (برای sync با
 * `updateMyTheme` در WP-1.7)، همان نام‌های فارسی، رنگ هویتی، رنگ کاشی‌ها
 * (tiles) و گرادیان نمایش (swatch) دقیقاً از سایت.
 *
 * «کلید» همان رشته‌ای است که در سرور ذخیره می‌شود؛ عوض‌کردنش با وب ناسازگار است.
 *
 * «مشکی و طلایی» طبق رفتار وب در حالت روشن هم تیره می‌ماند.
 */
enum class ShemimThemeKey(
    val key: String,
    val faTitle: String,
    /** رنگ هویتی تم — همان themeColor سایت. */
    val themeColorHex: String,
    /** رنگ کاشی ماژول‌ها — همان فیلد tiles سایت (۶ رنگ). */
    val tilesHex: List<String>,
    /** سه ایستگاه گرادیان انتخابگر تم — همان CSS swatch سایت. */
    val swatchHex: List<String>,
    /** در هر دو حالت روشن/تیره، تیره بماند (رفتار وب). */
    val alwaysDark: Boolean = false,
) {
    EMERALD(
        "emerald", "زمرد و شامپاین", "#0a4f3b",
        listOf("#14966f", "#b8892b", "#0e7490", "#6b8e23", "#a16207", "#0f766e"),
        listOf("#0a4f3b", "#14966f", "#c9a24b"),
    ),
    ROYAL(
        "royal", "یاقوت سلطنتی", "#0b2a5b",
        listOf("#1f56ad", "#c98a0a", "#0f766e", "#5b5fc7", "#b45309", "#2563eb"),
        listOf("#0b2a5b", "#1f56ad", "#d4a017"),
    ),
    BURGUNDY(
        "burgundy", "ارغوان و رزگلد", "#611230",
        listOf("#a3234f", "#c2703d", "#7c2d6b", "#b4443c", "#8a5a2b", "#d1568a"),
        listOf("#611230", "#a3234f", "#d9976a"),
    ),
    TURQUOISE(
        "turquoise", "فیروزهٔ نیشابور", "#0a6870",
        listOf("#0e8a90", "#ff7a59", "#2563a8", "#e0a100", "#12a37f", "#d6558f"),
        listOf("#0a6870", "#17a8aa", "#ff7a59"),
    ),
    POMEGRANATE(
        "pomegranate", "انار و زعفران", "#7d1228",
        listOf("#c8283f", "#e08a00", "#2f6f4f", "#8f3a6b", "#b5541c", "#d4a017"),
        listOf("#7d1228", "#c8283f", "#f2a900"),
    ),
    BLACKGOLD(
        "blackgold", "مشکی و طلایی", "#0d0e13",
        listOf("#e0b04a", "#4fd1c5", "#f2994a", "#a78bfa", "#68d391", "#f87171"),
        listOf("#10121b", "#2a2f45", "#e0b04a"),
        alwaysDark = true,
    ),
    LAPIS(
        "lapis", "کاشی لاجوردی", "#17307a",
        listOf("#2f5fd0", "#14b8a6", "#e0a100", "#c2410c", "#7c3aed", "#0e7490"),
        listOf("#17307a", "#2f5fd0", "#14b8a6"),
    ),
    SAFFRON(
        "saffron", "زعفران و گردو", "#6e3a05",
        listOf("#c77a12", "#2f7d6b", "#b23a48", "#6b4f9e", "#3e6fb0", "#8a6d1d"),
        listOf("#6e3a05", "#c77a12", "#f5b301"),
    ),
    FOREST(
        "forest", "جنگلی", "#14532d",
        listOf("#16a34a", "#0d9488", "#65a30d", "#ca8a04", "#15803d", "#0891b2"),
        listOf("#14532d", "#16a34a", "#eab308"),
    ),
    COSMIC(
        "cosmic", "کهکشانی", "#0d0a24",
        listOf("#c084fc", "#67e8f9", "#f0abfc", "#a5b4fc", "#fbbf24", "#86efac"),
        listOf("#4c1d95", "#8b5cf6", "#c084fc"),
    );

    /** رنگ هویتی به‌صورت Compose Color. */
    val themeColor: Color get() = hexToColor(themeColorHex)

    /** رنگ کاشی ماژول‌ها (همان tiles سایت). */
    val tileColors: List<Color> get() = tilesHex.map(::hexToColor)

    /** رنگ‌های گرادیان انتخابگر تم. */
    val swatchColors: List<Color> get() = swatchHex.map(::hexToColor)

    companion object {
        /** پیش‌فرض اپ — نزدیک‌ترین به برند سبز صندوق (ADR-010). */
        val DEFAULT = EMERALD

        /** کلید سرور (updateMyTheme) → تم؛ ناشناخته → پیش‌فرض. */
        fun fromKey(key: String?): ShemimThemeKey =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

/** ترجیح حالت تیره — سند ۰۳ §۲: دنبال‌کردن سیستم + انتخاب دستی. */
enum class DarkModePreference { SYSTEM, LIGHT, DARK }

/** «#0a4f3b» → Color — فقط شکل ۶ رقمی RRGGBB سایت پشتیبانی می‌شود. */
internal fun hexToColor(hex: String): Color {
    val h = hex.removePrefix("#")
    require(h.length == 6) { "hex نامعتبر: $hex" }
    return Color((0xFFL shl 24) or h.toLong(16))
}
