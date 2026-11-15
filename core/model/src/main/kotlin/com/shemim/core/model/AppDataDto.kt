package com.shemim.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * ریشهٔ appData — خروجی `loadData()` در api/data-helpers.php.
 *
 * همهٔ فیلدها مقدار پیش‌فرض دارند تا پاسخ‌های ناقص/آینده‌خواه (کلید جدید سرور)
 * بدون کرش پارس شوند (این لایه فقط کش است و همیشه از سرور بازسازی می‌شود).
 *
 * `ownerPower` و `quizPoints` خلاصه‌های محاسبه‌شدهٔ مخصوص بیننده‌اند (نه جدول
 * سینک‌شونده) و در فازهای ۴ مدل دقیق خود را می‌گیرند — فعلاً JSON شفاف.
 */
@Serializable
data class AppDataDto(
    val fundName: String = "",
    val ownerPower: JsonObject? = null,
    val quizPoints: JsonObject? = null,
    /** موجودی حساب اصلی = جمع واریزهای غیرقسطی منهای برداشت‌ها (محاسبهٔ سرور) */
    val mainAccount: Long = 0,
    /** موجودی حساب درآمد/هزینه (محاسبهٔ سرور) */
    val incomeExpenseAccount: Long = 0,
    val members: List<MemberDto> = emptyList(),
    val incomeExpenseTransactions: List<IncomeExpenseDto> = emptyList(),
    val groups: List<GroupDto> = emptyList(),
    val resignedMembers: List<ResignedMemberDto> = emptyList(),
    val settings: SettingsDto = SettingsDto(),
    val profitReports: List<ProfitReportDto> = emptyList(),
    val requests: List<RequestDto> = emptyList(),
    val requestsV2: List<RequestV2Dto> = emptyList(),
    val loanQueue: List<LoanQueueDto> = emptyList(),
    val notifications: List<NotificationDto> = emptyList(),
    val nextMemberNumber: Int = 1,
    val nextLoanNumber: Int = 1,
)
