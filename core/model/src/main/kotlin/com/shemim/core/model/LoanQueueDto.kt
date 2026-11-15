package com.shemim.core.model

import kotlinx.serialization.Serializable

/**
 * ردیف نوبت وام (جدول loan_queue) — نام‌گذاری camelCase مطابق data-helpers.php.
 */
@Serializable
data class LoanQueueDto(
    val id: String,
    val memberId: String = "",
    val memberName: String = "",
    val requestDate: String? = null,
    /** in_queue | granted | done | … */
    val status: String? = null,
    /** normal | high | … */
    val priority: String? = null,
    val requestedAmount: Long = 0,
    val requestedInstallments: Int = 0,
    val adminEstimatedDate: String = "",
    val loanId: String? = null,
    val loanNumber: Int? = null,
    val grantedAt: String? = null,
)
