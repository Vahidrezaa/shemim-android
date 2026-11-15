package com.shemim.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * پاکت پاسخ `action=loadData` — api/core.php.
 *
 * دو حالت دارد (سند ۰۲ §۴):
 *  - بدون تغییر: `{success, unchanged:true, dataVersion, rfWatch}` — هیچ نگاشتی لازم نیست
 *  - دادهٔ کامل:  `{success, appData, dataVersion, rfWatch}`
 *
 * `rfWatch` (پیکربندی تماشای زندهٔ صندوق گردشی) فعلاً شفاف عبور داده می‌شود؛
 * مدل‌سازی دقیق آن در WP-3.4 انجام می‌شود.
 */
@Serializable
data class LoadDataResponse(
    val success: Boolean = false,
    val unchanged: Boolean = false,
    val dataVersion: String = "",
    val appData: AppDataDto? = null,
    val rfWatch: JsonObject? = null,
) {
    /** کلاینت باید کل داده را جایگزین کند؟ (نه در حالت unchanged) */
    val hasFreshData: Boolean get() = success && !unchanged && appData != null
}
