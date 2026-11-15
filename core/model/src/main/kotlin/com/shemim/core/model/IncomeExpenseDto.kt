package com.shemim.core.model

import kotlinx.serialization.Serializable

/**
 * تراکنش درآمد/هزینهٔ صندوق (جدول income_expenses) — خروجی خام SELECT به‌علاوهٔ
 * سه کلید اضافه‌شده در سرور: date/createdBy/createdAt.
 *
 * نوع: `income` | `expense`.
 */
@Serializable
data class IncomeExpenseDto(
    val id: String,
    val type: String = "",
    val amount: Long = 0,
    val reason: String? = null,
    val description: String? = null,
    /** = transaction_date (کلید اضافه‌شدهٔ سرور؛ فرمت شمسی) */
    val date: String = "",
    val createdAt: String? = null,
    val createdBy: String? = null,
)
