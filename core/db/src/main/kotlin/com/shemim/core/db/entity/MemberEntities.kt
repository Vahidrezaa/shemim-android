package com.shemim.core.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * موجودیت‌های حوزهٔ اعضا — نام جداول عمداً همان جداول سرور است تا نگاشت ذهنی
 * ساده بماند (این پایگاه‌داده فقط «کشِ سینک» است؛ منبع حقیقت سرور است).
 *
 * بدون ForeignKey عمدی: جایگزینی کامل جداول در یک @Transaction انجام می‌شود
 * و دادهٔ کثیف سرور نباید insert را بشکند.
 */

@Entity(tableName = "members", indices = [Index("membershipNumber"), Index("role")])
data class MemberEntity(
    @PrimaryKey val id: String,
    val membershipNumber: Int,
    val firstName: String,
    val lastName: String,
    val username: String,
    val role: String,
    val isOwner: Boolean,
    val joinDate: String?,
    val birthDate: String?,
    val birthDateLocked: Boolean,
    val phone: String?,
    val email: String,
    val colorTheme: String?,
    /** فقط برای بینندهٔ دارای مجوز؛ JSON خام supervisorPerms */
    val fatherName: String?,
    val nationalCode: String?,
    val isSupervisor: Boolean?,
    val supervisorPermsJson: String?,
)

@Entity(
    tableName = "member_transactions",
    indices = [Index("memberId"), Index("loanId"), Index("date")],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val memberId: String,
    /** deposit | withdraw */
    val type: String,
    val amount: Long,
    val description: String?,
    val date: String,
    val createdAt: String?,
    val createdBy: String?,
    val category: String?,
    val isInstallment: Boolean,
    val loanId: String?,
    val fromMonth: Int?,
    val fromYear: Int?,
    val toMonth: Int?,
    val toYear: Int?,
)

@Entity(tableName = "loans", indices = [Index("memberId"), Index("loanNumber")])
data class LoanEntity(
    @PrimaryKey val id: String,
    val memberId: String,
    val loanNumber: Int,
    val amount: Long,
    val totalInstallments: Int,
    val paidInstallments: Int,
    val fee: Long,
    val startDate: String,
    val description: String?,
    val isEmergency: Boolean,
    val splitMethod: Int,
)

@Entity(tableName = "installment_payments", primaryKeys = ["loanId", "installmentNumber"])
data class InstallmentPaymentEntity(
    val loanId: String,
    val installmentNumber: Int,
    val amount: Long,
    val paymentDate: String,
)

@Entity(tableName = "resigned_members", indices = [Index("membershipNumber")])
data class ResignedMemberEntity(
    @PrimaryKey val id: String,
    val membershipNumber: Int,
    val firstName: String,
    val lastName: String,
    val username: String,
    val role: String,
    val joinDate: String?,
    val birthDate: String?,
    val email: String,
    val phone: String?,
    val resignedDate: String,
)
