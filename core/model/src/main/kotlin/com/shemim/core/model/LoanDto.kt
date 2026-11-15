package com.shemim.core.model

import kotlinx.serialization.Serializable

/**
 * وام عضو — MemberDto.loans (جدول loans) به‌همراه تاریخچهٔ اقساط پرداخت‌شده.
 */
@Serializable
data class LoanDto(
    val id: String,
    val loanNumber: Int = 0,
    val amount: Long = 0,
    val totalInstallments: Int = 0,
    val paidInstallments: Int = 0,
    /** کارمزد وام (حذف می‌شود یا به صورت اقساط افزوده می‌شود — splitMethod) */
    val fee: Long = 0,
    val startDate: String = "",
    val description: String? = null,
    /** وام فوری/اضطراری */
    val isEmergency: Boolean = false,
    /** نحوهٔ تقسیم کارمزد: 0..3 (حذف | افزودن به مبلغ | توزیع در اقساط | …) */
    val splitMethod: Int = 0,
    val installmentsHistory: List<InstallmentPaymentDto> = emptyList(),
)

/**
 * یک قسط پرداخت‌شدهٔ وام (جدول installment_payments) — کلید ترکیبی
 * (loanId, installmentNumber).
 */
@Serializable
data class InstallmentPaymentDto(
    val installmentNumber: Int = 0,
    val amount: Long = 0,
    val date: String = "",
)
