package com.shemim.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * درخواست V1 (جدول requests) — **نام‌گذاری مخلوط سرور** را عیناً حفظ می‌کند:
 * برخی کلیدها camelCase (memberId/memberName/loanId) و برخی snake_case
 * (request_date/requested_by/from_month و …). این عمدی است و با خروجی
 * data-helpers.php تطبیق داده شده — تغییرش parsing را می‌شکند.
 */
@Serializable
data class RequestDto(
    val id: String,
    val type: String? = null,
    val memberId: String? = null,
    val memberName: String? = null,
    val amount: Long = 0,
    val description: String? = null,
    @SerialName("request_date") val requestDate: String? = null,
    /** pending | approved | rejected | … */
    val status: String? = null,
    @SerialName("requested_by") val requestedBy: String? = null,
    val category: String? = null,
    @SerialName("from_month") val fromMonth: Int? = null,
    @SerialName("from_year") val fromYear: Int? = null,
    @SerialName("to_month") val toMonth: Int? = null,
    @SerialName("to_year") val toYear: Int? = null,
    val installments: Int? = null,
    @SerialName("preferred_date") val preferredDate: String? = null,
    @SerialName("loan_id") val loanId: String? = null,
    @SerialName("installment_number") val installmentNumber: Int? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("installment_date") val installmentDate: String? = null,
    @SerialName("approved_by") val approvedBy: String = "",
    @SerialName("approved_at") val approvedAt: String = "",
    @SerialName("reject_reason") val rejectReason: String = "",
)

/**
 * درخواست V2 (جدول requests_v2) — برخلاف V1 ردیف **خام** است و همهٔ کلیدها
 * snake_case‌اند (SELECT * بدون بازنویسی).
 */
@Serializable
data class RequestV2Dto(
    val id: String,
    val type: String? = null,
    @SerialName("member_id") val memberId: String? = null,
    @SerialName("member_name") val memberName: String? = null,
    val amount: Long = 0,
    val description: String? = null,
    @SerialName("request_date") val requestDate: String? = null,
    val status: String? = null,
    @SerialName("requested_by") val requestedBy: String? = null,
    val category: String? = null,
    @SerialName("from_month") val fromMonth: Int? = null,
    @SerialName("from_year") val fromYear: Int? = null,
    @SerialName("to_month") val toMonth: Int? = null,
    @SerialName("to_year") val toYear: Int? = null,
    val installments: Int? = null,
    @SerialName("preferred_date") val preferredDate: String? = null,
    @SerialName("loan_id") val loanId: String? = null,
    @SerialName("installment_number") val installmentNumber: Int? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("installment_date") val installmentDate: String? = null,
    @SerialName("approved_by") val approvedBy: String = "",
    @SerialName("approved_at") val approvedAt: String = "",
    @SerialName("reject_reason") val rejectReason: String = "",
)
