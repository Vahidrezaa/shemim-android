package com.shemim.core.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** موجودیت‌های حوزهٔ مالی/تنظیمات صندوق. */

@Entity(tableName = "income_expenses", indices = [Index("date")])
data class IncomeExpenseEntity(
    @PrimaryKey val id: String,
    /** income | expense */
    val type: String,
    val amount: Long,
    val reason: String?,
    val description: String?,
    val date: String,
    val createdAt: String?,
    val createdBy: String?,
)

@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey val id: String,
    val leaderId: String,
)

@Entity(tableName = "group_members", primaryKeys = ["groupId", "memberId"])
data class GroupMemberCrossRef(
    val groupId: String,
    val memberId: String,
)

/**
 * ردیف یگانهٔ تنظیمات صندوق (id=1) + شمارنده‌ها و موجودی‌های محاسبه‌شدهٔ
 * سرور که با هر sync تازه می‌شوند (WP-0.5 می‌نویسد؛ پیشخوان‌ها می‌خوانند).
 */
@Entity(tableName = "fund_settings")
data class FundSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val fundName: String,
    val mainAccount: Long?,
    val incomeExpenseAccount: Long?,
    val nextMemberNumber: Int?,
    val nextLoanNumber: Int?,
    val loanMultiplier: Double?,
    val loanMultiplierMode: String?,
    val loanReservePercent: Int?,
    val loanMonthlyDivisor: Int?,
    val loanCooldownMonths: Int?,
    val loanDelayCooldownFactor: Double?,
    val loanPersonCapFactor: Double?,
    val latePenaltyPerDayPerMillion: Double?,
    val birthdayGiftAmount: Long?,
    val currency: String?,
    val displayMode: String?,
    val fundLogo: String?,
    val theme: String?,
)

@Entity(tableName = "profit_reports", indices = [Index("date")])
data class ProfitReportEntity(
    @PrimaryKey val id: String,
    val version: Int?,
    val date: String?,
    val startDate: String?,
    val endDate: String?,
    val description: String?,
    val totalProfit: Long,
    /** JSON خام آرایهٔ details (ساختارش در فاز ۴ مدل می‌شود) */
    val detailsJson: String,
)
