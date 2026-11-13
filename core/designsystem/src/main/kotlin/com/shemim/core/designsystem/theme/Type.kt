package com.shemim.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.shemim.core.designsystem.R

/**
 * تایپوگرافی — فونت Vazirmatn نسخهٔ FD (ارقام فارسی، همان وب) در ۴ وزن
 * (سند ۰۳ §۳). مقیاس M3 با کمی درشتی برای خوانایی مالی.
 *
 * نکته: ارقامِ ستونیِ مبالغ با ویژگی tnum در AmountText اعمال می‌شود،
 * نه در کل تایپوگرافی.
 */
val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn_fd_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_fd_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_fd_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_fd_extrabold, FontWeight.ExtraBold),
)

val ShemimTypography = Typography(
    // نمایش — عدد موجودی پیشخوان
    displayLarge = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp),
    displayMedium = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp),
    displaySmall = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp),
    // تیتر صفحه
    headlineLarge = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp),
    headlineMedium = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 27.sp),
    headlineSmall = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp),
    // عنوان کارت / بخش
    titleLarge = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleSmall = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    // متن
    bodyLarge = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    // برچسب / متادیتا
    labelLarge = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = VazirmatnFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)
