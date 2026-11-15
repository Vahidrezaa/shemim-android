package com.shemim.core.db.mapper

import com.shemim.core.db.entity.FundSettingsEntity
import com.shemim.core.db.entity.GroupEntity
import com.shemim.core.db.entity.GroupMemberCrossRef
import com.shemim.core.db.entity.IncomeExpenseEntity
import com.shemim.core.db.entity.InstallmentPaymentEntity
import com.shemim.core.db.entity.LoanEntity
import com.shemim.core.db.entity.LoanQueueEntity
import com.shemim.core.db.entity.MemberEntity
import com.shemim.core.db.entity.NotificationEntity
import com.shemim.core.db.entity.ProfitReportEntity
import com.shemim.core.db.entity.RequestEntity
import com.shemim.core.db.entity.RequestV2Entity
import com.shemim.core.db.entity.ResignedMemberEntity
import com.shemim.core.db.entity.TransactionEntity
import com.shemim.core.model.AppDataDto
import com.shemim.core.model.AppDataJson
import com.shemim.core.model.GroupDto
import com.shemim.core.model.IncomeExpenseDto
import com.shemim.core.model.InstallmentPaymentDto
import com.shemim.core.model.LoanDto
import com.shemim.core.model.MemberDto
import com.shemim.core.model.NotificationDto
import com.shemim.core.model.ProfitReportDto
import com.shemim.core.model.RequestDto
import com.shemim.core.model.RequestV2Dto
import com.shemim.core.model.ResignedMemberDto
import com.shemim.core.model.SettingsDto
import com.shemim.core.model.TransactionDto
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonObject

/**
 * نگاشت appData→Room — WP-0.4 (سند ۰۲ §۶).
 *
 * همهٔ توابع خالص JVMاند (بدون API اندروید) و در تست واحد با همان فیکسچر
 * JSON لایهٔ مدل راستی‌آزمایی می‌شوند. موتور سینک (WP-0.5) فقط
 * `AppDataDto.toSnapshot()` را صدا می‌زند و نتیجه را به DAOها می‌دهد.
 */

/** همهٔ ردیف‌های ناشی از یک loadData کامل + نسخهٔ داده. */
data class AppDataSnapshot(
    val dataVersion: String,
    val members: List<MemberEntity>,
    val transactions: List<TransactionEntity>,
    val loans: List<LoanEntity>,
    val installments: List<InstallmentPaymentEntity>,
    val resignedMembers: List<ResignedMemberEntity>,
    val incomeExpenses: List<IncomeExpenseEntity>,
    val groups: List<GroupEntity>,
    val groupMembers: List<GroupMemberCrossRef>,
    val settings: FundSettingsEntity?,
    val profitReports: List<ProfitReportEntity>,
    val requests: List<RequestEntity>,
    val requestsV2: List<RequestV2Entity>,
    val loanQueue: List<LoanQueueEntity>,
    val notifications: List<NotificationEntity>,
)

fun AppDataDto.toSnapshot(dataVersion: String): AppDataSnapshot = AppDataSnapshot(
    dataVersion = dataVersion,
    members = members.map { it.toEntity() },
    transactions = members.flatMap { m -> m.transactions.map { it.toEntity(m.id) } },
    loans = members.flatMap { m -> m.loans.map { it.toEntity(m.id) } },
    installments = members.flatMap { m ->
        m.loans.flatMap { l -> l.installmentsHistory.map { it.toEntity(l.id) } }
    },
    resignedMembers = resignedMembers.map { it.toEntity() },
    incomeExpenses = incomeExpenseTransactions.map { it.toEntity() },
    groups = groups.map { it.toEntity() },
    groupMembers = groups.flatMap { g -> g.memberIds.map { GroupMemberCrossRef(g.id, it) } },
    settings = toSettingsEntity(),
    profitReports = profitReports.map { it.toEntity() },
    requests = requests.map { it.toEntity() },
    requestsV2 = requestsV2.map { it.toEntity() },
    loanQueue = loanQueue.map { it.toEntity() },
    notifications = notifications.map { it.toEntity() },
)

// ---------- حوزهٔ اعضا ----------

fun MemberDto.toEntity() = MemberEntity(
    id = id,
    membershipNumber = membershipNumber,
    firstName = firstName,
    lastName = lastName,
    username = username,
    role = role,
    isOwner = isOwner,
    joinDate = joinDate,
    birthDate = birthDate,
    birthDateLocked = birthDateLocked,
    phone = phone,
    email = email,
    colorTheme = colorTheme,
    fatherName = fatherName,
    nationalCode = nationalCode,
    isSupervisor = isSupervisor,
    supervisorPermsJson = supervisorPerms?.toString(),
)

fun TransactionDto.toEntity(memberId: String) = TransactionEntity(
    id = id,
    memberId = memberId,
    type = type,
    amount = amount,
    description = description,
    date = date,
    createdAt = createdAt,
    createdBy = createdBy,
    category = category,
    isInstallment = isInstallment,
    loanId = loanId,
    fromMonth = fromMonth,
    fromYear = fromYear,
    toMonth = toMonth,
    toYear = toYear,
)

fun LoanDto.toEntity(memberId: String) = LoanEntity(
    id = id,
    memberId = memberId,
    loanNumber = loanNumber,
    amount = amount,
    totalInstallments = totalInstallments,
    paidInstallments = paidInstallments,
    fee = fee,
    startDate = startDate,
    description = description,
    isEmergency = isEmergency,
    splitMethod = splitMethod,
)

fun InstallmentPaymentDto.toEntity(loanId: String) = InstallmentPaymentEntity(
    loanId = loanId,
    installmentNumber = installmentNumber,
    amount = amount,
    paymentDate = date,
)

fun ResignedMemberDto.toEntity() = ResignedMemberEntity(
    id = id,
    membershipNumber = membershipNumber,
    firstName = firstName,
    lastName = lastName,
    username = username,
    role = role,
    joinDate = joinDate,
    birthDate = birthDate,
    email = email,
    phone = phone,
    resignedDate = resignedDate,
)

// ---------- حوزهٔ مالی/تنظیمات ----------

fun IncomeExpenseDto.toEntity() = IncomeExpenseEntity(
    id = id,
    type = type,
    amount = amount,
    reason = reason,
    description = description,
    date = date,
    createdAt = createdAt,
    createdBy = createdBy,
)

fun GroupDto.toEntity() = GroupEntity(id = id, leaderId = leaderId)

private fun AppDataDto.toSettingsEntity(): FundSettingsEntity {
    val s: SettingsDto = settings
    return FundSettingsEntity(
        fundName = fundName,
        mainAccount = mainAccount,
        incomeExpenseAccount = incomeExpenseAccount,
        nextMemberNumber = nextMemberNumber,
        nextLoanNumber = nextLoanNumber,
        loanMultiplier = s.loanMultiplier,
        loanMultiplierMode = s.loanMultiplierMode,
        loanReservePercent = s.loanReservePercent,
        loanMonthlyDivisor = s.loanMonthlyDivisor,
        loanCooldownMonths = s.loanCooldownMonths,
        loanDelayCooldownFactor = s.loanDelayCooldownFactor,
        loanPersonCapFactor = s.loanPersonCapFactor,
        latePenaltyPerDayPerMillion = s.latePenaltyPerDayPerMillion,
        birthdayGiftAmount = s.birthdayGiftAmount,
        currency = s.currency,
        displayMode = s.displayMode,
        fundLogo = s.fundLogo,
        theme = s.theme ?: s.colorTheme,
    )
}

fun ProfitReportDto.toEntity() = ProfitReportEntity(
    id = id,
    version = version,
    date = date,
    startDate = startDate,
    endDate = endDate,
    description = description,
    totalProfit = totalProfit,
    detailsJson = AppDataJson.encodeToString(
        ListSerializer(JsonObject.serializer()), details,
    ),
)

// ---------- گردش‌کار ----------

fun RequestDto.toEntity() = RequestEntity(
    id = id,
    type = type,
    memberId = memberId,
    memberName = memberName,
    amount = amount,
    description = description,
    requestDate = requestDate,
    status = status,
    requestedBy = requestedBy,
    category = category,
    fromMonth = fromMonth,
    fromYear = fromYear,
    toMonth = toMonth,
    toYear = toYear,
    installments = installments,
    preferredDate = preferredDate,
    loanId = loanId,
    installmentNumber = installmentNumber,
    dueDate = dueDate,
    installmentDate = installmentDate,
    approvedBy = approvedBy,
    approvedAt = approvedAt,
    rejectReason = rejectReason,
)

fun RequestV2Dto.toEntity() = RequestV2Entity(
    id = id,
    type = type,
    memberId = memberId,
    memberName = memberName,
    amount = amount,
    description = description,
    requestDate = requestDate,
    status = status,
    requestedBy = requestedBy,
    category = category,
    fromMonth = fromMonth,
    fromYear = fromYear,
    toMonth = toMonth,
    toYear = toYear,
    installments = installments,
    preferredDate = preferredDate,
    loanId = loanId,
    installmentNumber = installmentNumber,
    dueDate = dueDate,
    installmentDate = installmentDate,
    approvedBy = approvedBy,
    approvedAt = approvedAt,
    rejectReason = rejectReason,
)

fun com.shemim.core.model.LoanQueueDto.toEntity() = LoanQueueEntity(
    id = id,
    memberId = memberId,
    memberName = memberName,
    requestDate = requestDate,
    status = status,
    priority = priority,
    requestedAmount = requestedAmount,
    requestedInstallments = requestedInstallments,
    adminEstimatedDate = adminEstimatedDate,
    loanId = loanId,
    loanNumber = loanNumber,
    grantedAt = grantedAt,
)

fun NotificationDto.toEntity() = NotificationEntity(
    id = id,
    title = title,
    message = message,
    priority = priority,
    date = date,
    target = target,
    targetMemberId = targetMemberId,
    repliesJson = AppDataJson.encodeToString(
        ListSerializer(JsonObject.serializer()), replies,
    ),
    readByJson = AppDataJson.encodeToString(
        ListSerializer(String.serializer()), readBy,
    ),
    filesJson = AppDataJson.encodeToString(
        ListSerializer(com.shemim.core.model.NotificationFileDto.serializer()), files,
    ),
)
