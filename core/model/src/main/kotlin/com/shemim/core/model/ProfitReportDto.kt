package com.shemim.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * گزارش سود توزیع‌شده (جدول profit_reports).
 *
 * `details` ساختار وابسته به نسخه دارد (v1: سهم نقدی؛ v2: امتیاز موزون
 * balanceScore/loanInRangeScore) و در فاز ۴ (WP-4.2) مدل دقیق می‌شود؛
 * فعلاً به‌صورت JSON شفاف ذخیره می‌شود.
 */
@Serializable
data class ProfitReportDto(
    val id: String,
    /** 1 | 2 | null (null = گزارش قدیمی — سرور از روی details حدس می‌زند) */
    val version: Int? = null,
    val date: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val description: String? = null,
    val totalProfit: Long = 0,
    val details: List<JsonObject> = emptyList(),
)
