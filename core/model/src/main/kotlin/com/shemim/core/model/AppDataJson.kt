package com.shemim.core.model

import kotlinx.serialization.json.Json

/**
 * پیکربندی مشترک Json برای کل لایهٔ داده.
 *
 * - `ignoreUnknownKeys`: سرور کلیدهای جدید اضافه می‌کند (appData عزیق تطبیقی)؛
 *   کلاینت نباید روی کلید ناشناخته بشکند.
 * - `coerceInputValues`: مقدار null برای فیلد غیر-null دارای default به همان
 *   default برگردد (پاسخ‌های قدیمی/ناقص سرور).
 * - `encodeDefaults`: هنگام باز انکود (مثلاً payloadهای نوشتنی) مقادیر
 *   پیش‌فرض هم صریح بروند تا رفتار با وب یکی بماند.
 */
val AppDataJson: Json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
}
