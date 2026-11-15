package com.shemim.core.model

import kotlinx.serialization.Serializable

/**
 * تراکنش مالی عضو — عضوِ داخل MemberDto.transactions (جدول member_transactions).
 *
 * `fromMonth/fromYear/toYear/toMonth` فقط برای تراکنش‌های مقرری ماهانه چندماهه
 * (واریز دسته‌ای جایزه/مقرری) مقدار دارند.
 */
@Serializable
data class TransactionDto(
    val id: String,
    /** deposit | withdraw */
    val type: String = "",
    val amount: Long = 0,
    val description: String? = null,
    /** تاریخ شمسی رشته‌ای مثل 1403/05/12 (فرمت سایت) */
    val date: String = "",
    val createdAt: String? = null,
    val createdBy: String? = null,
    val category: String? = null,
    /** قسط وام است؟ (اتصال به loanId) */
    val isInstallment: Boolean = false,
    val loanId: String? = null,
    val fromMonth: Int? = null,
    val fromYear: Int? = null,
    val toMonth: Int? = null,
    val toYear: Int? = null,
)
