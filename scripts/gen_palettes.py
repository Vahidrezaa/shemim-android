#!/usr/bin/env python3
"""
تولید پالت‌های Material 3 برای ۱۰ تم سایت شمیم — WP-0.2

منبع رنگ‌ها: js/themes.js سایت (در تاریخ ۱۴۰۵/۰۸/۲۴ واکشی شد).
الگوریتم: material-color-utilities (پورت رسمی C++ متریال) — Variant.TONALSPOT،
contrast_level=0.0 — دقیقاً همان پیش‌فرض Material Theme Builder.

قاعدهٔ بذر رنگ:
- ۸ تم با themeColor خودِ سایت (رنگ هویتی پرچمدار).
- blackgold و cosmic: themeColor سایت تقریباً سیاه است (رنگ پس‌زمینه)؛ بذر پالت
  = رنگ میانی swatch سایت (#e0b04a طلایی و #8b5cf6 بنفش) تا هویت تم حفظ شود.
- blackgold طبق رفتار وب همیشه تیره می‌ماند (نسخهٔ روشن = همان تیره).

خروجی: فایل Kotlin با ۲۰ ColorScheme صریح + گزارش کنتراست WCAG.
"""
import json
import sys

import material_color_utilities as mcu

# (key وب, نام فارسی, بذر پالت, همیشه‌تیره)
THEMES = [
    ("emerald",     "زمرد و شامپاین",   "#0a4f3b", False),
    ("royal",       "یاقوت سلطنتی",     "#0b2a5b", False),
    ("burgundy",    "ارغوان و رزگلد",   "#611230", False),
    ("turquoise",   "فیروزهٔ نیشابور",  "#0a6870", False),
    ("pomegranate", "انار و زعفران",    "#7d1228", False),
    ("blackgold",   "مشکی و طلایی",     "#e0b04a", True),
    ("lapis",       "کاشی لاجوردی",     "#17307a", False),
    ("saffron",     "زعفران و گردو",    "#6e3a05", False),
    ("forest",      "جنگلی",            "#14532d", False),
    ("cosmic",      "کهکشانی",          "#8b5cf6", False),
]

# نقش‌های ColorScheme کامپوز که از دیکِت طرح می‌خوانیم (به همان ترتیب lightColorScheme)
ROLES = [
    "primary", "on_primary", "primary_container", "on_primary_container",
    "secondary", "on_secondary", "secondary_container", "on_secondary_container",
    "tertiary", "on_tertiary", "tertiary_container", "on_tertiary_container",
    "background",
    "surface", "on_surface",
    "surface_variant", "on_surface_variant",
    "surface_dim", "surface_bright",
    "surface_container_lowest", "surface_container_low",
    "surface_container", "surface_container_high",
    "surface_container_highest",
    "outline", "outline_variant",
    "error", "on_error", "error_container", "on_error_container",
    "inverse_primary", "inverse_surface", "inverse_on_surface",
    "scrim",
]

CONTRAST_PAIRS = [
    ("primary", "on_primary"), ("primary_container", "on_primary_container"),
    ("secondary", "on_secondary"), ("secondary_container", "on_secondary_container"),
    ("tertiary", "on_tertiary"), ("tertiary_container", "on_tertiary_container"),
    ("surface", "on_surface"), ("background", "on_surface"),
    ("surface_variant", "on_surface_variant"),
    ("error", "on_error"), ("error_container", "on_error_container"),
]


def scheme_dict(seed_hex: str, dark: bool) -> dict:
    theme = mcu.theme_from_color(
        seed_hex, contrast_level=0.0, variant=mcu.Variant.TONALSPOT
    )
    schemes = theme.dict()["schemes"]
    return schemes["dark" if dark else "light"]


def roles_kt(sd: dict) -> str:
    lines = []
    for role in ROLES:
        hexv = sd[role].upper()
        parts = role.split("_")
        camel = parts[0] + "".join(p.capitalize() for p in parts[1:])
        lines.append(f"    {camel} = Color(0x{hexv.lstrip('#')}),")
    return "\n".join(lines)


def audit(sd: dict, label: str) -> list:
    problems = []
    for a, b in CONTRAST_PAIRS:
        ratio = mcu.get_contrast_ratio(sd[a], sd[b])
        if ratio < 4.5:
            problems.append(f"  ⚠ {label} {a}/{b}: {ratio:.2f}")
    return problems


def main():
    out = []
    warnings = []
    for key, fa, seed, always_dark in THEMES:
        light = scheme_dict(seed, dark=False)
        dark = scheme_dict(seed, dark=True)
        warnings += audit(light, f"{key}/light")
        warnings += audit(dark, f"{key}/dark")

        camel = "".join(p.capitalize() for p in key.split("_"))
        out.append(f"// ══ {key} — {fa} (بذر: {seed}) ══")
        if always_dark:
            out.append("// طبق رفتار وب، این تم در هر دو حالتِ روشن/تیره، تیره می‌ماند.")
            out.append(f"internal val {camel}DarkColors = darkColorScheme(\n{roles_kt(dark)}\n)")
            out.append(f"@Suppress(\"unused\") internal val {camel}LightColors = {camel}DarkColors")
        else:
            out.append(f"internal val {camel}LightColors = lightColorScheme(\n{roles_kt(light)}\n)")
            out.append(f"internal val {camel}DarkColors = darkColorScheme(\n{roles_kt(dark)}\n)")
        out.append("")

    body = "\n".join(out)
    header = '''package com.shemim.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * پالت‌های Material 3 برای ده تم سایت — تولیدشده با الگوریتم رسمی
 * Material Color Utilities (Variant.TONALSPOT, contrast 0) از رنگ هویتی هر تم در
 * js/themes.js؛ اسکریپت مولد: scripts/gen_palettes.py در مخزن تحویل.
 *
 * نسخهٔ روشن/تیرهٔ هر تم به‌صورت کد صریح (نه محاسبهٔ زمان اجرا) تا:
 *  - پیش‌نمایش Android Studio درست باشد؛
 *  - هیچ وابستگی زمان اجرا به کتابخانهٔ رنگ اضافه نشود.
 */
'''
    sys.stdout.write(header + body)
    if warnings:
        sys.stderr.write("\n".join(warnings) + "\n")
        sys.stderr.write(f"TOTAL WARNINGS: {len(warnings)}\n")


if __name__ == "__main__":
    main()
