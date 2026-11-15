package com.shemim.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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
import com.shemim.core.db.entity.SyncMetaEntity
import com.shemim.core.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAOهای کش سینک — الگوی یکسان برای همه (سند ۰۲ §۴):
 * `replaceAll*` کل جدول را در یک تراکنش جایگزین می‌کند چون loadData همیشه
 * appData کامل برمی‌گرداند (نسخه‌بندی سرور فقط «بدون تغییر» را کوتاه می‌کند).
 * پرس‌وجوهای ویژگی‌ها (جستجو/فیلتر/پیجینگ) در WP خودشان به این DAOها افزوده
 * می‌شوند؛ اینجا فقط مشاهده‌گرهای پایه هست.
 */

@Dao
interface MemberDao {
    @Transaction
    suspend fun replaceAll(
        members: List<MemberEntity>,
        transactions: List<TransactionEntity>,
        loans: List<LoanEntity>,
        installments: List<InstallmentPaymentEntity>,
        resigned: List<ResignedMemberEntity>,
    ) {
        clearMembers()
        clearTransactions()
        clearLoans()
        clearInstallments()
        clearResigned()
        insertMembers(members)
        insertTransactions(transactions)
        insertLoans(loans)
        insertInstallments(installments)
        insertResigned(resigned)
    }

    @Query("DELETE FROM members")
    suspend fun clearMembers()

    @Query("DELETE FROM member_transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM loans")
    suspend fun clearLoans()

    @Query("DELETE FROM installment_payments")
    suspend fun clearInstallments()

    @Query("DELETE FROM resigned_members")
    suspend fun clearResigned()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(rows: List<MemberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(rows: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoans(rows: List<LoanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallments(rows: List<InstallmentPaymentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResigned(rows: List<ResignedMemberEntity>)

    @Query("SELECT * FROM members ORDER BY membershipNumber ASC")
    fun observeMembers(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM member_transactions WHERE memberId = :memberId ORDER BY date DESC")
    fun observeTransactionsOf(memberId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM loans WHERE memberId = :memberId ORDER BY loanNumber DESC")
    fun observeLoansOf(memberId: String): Flow<List<LoanEntity>>

    @Query("SELECT * FROM installment_payments WHERE loanId = :loanId ORDER BY installmentNumber ASC")
    fun observeInstallmentsOf(loanId: String): Flow<List<InstallmentPaymentEntity>>

    @Query("SELECT COUNT(*) FROM members")
    suspend fun countMembers(): Int

    @Query("SELECT COUNT(*) FROM member_transactions")
    suspend fun countTransactions(): Int
}

@Dao
interface FundDao {
    @Transaction
    suspend fun replaceAll(
        incomeExpenses: List<IncomeExpenseEntity>,
        groups: List<GroupEntity>,
        groupMembers: List<GroupMemberCrossRef>,
        settings: FundSettingsEntity?,
        profitReports: List<ProfitReportEntity>,
    ) {
        clearIncomeExpenses()
        clearGroups()
        clearGroupMembers()
        clearProfitReports()
        insertIncomeExpenses(incomeExpenses)
        insertGroups(groups)
        insertGroupMembers(groupMembers)
        settings?.let { upsertSettings(it) }
        insertProfitReports(profitReports)
    }

    @Query("DELETE FROM income_expenses")
    suspend fun clearIncomeExpenses()

    @Query("DELETE FROM groups")
    suspend fun clearGroups()

    @Query("DELETE FROM group_members")
    suspend fun clearGroupMembers()

    @Query("DELETE FROM profit_reports")
    suspend fun clearProfitReports()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeExpenses(rows: List<IncomeExpenseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(rows: List<GroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMembers(rows: List<GroupMemberCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfitReports(rows: List<ProfitReportEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(row: FundSettingsEntity)

    @Query("SELECT * FROM fund_settings WHERE id = 1")
    fun observeSettings(): Flow<FundSettingsEntity?>

    @Query("SELECT * FROM income_expenses ORDER BY date DESC")
    fun observeIncomeExpenses(): Flow<List<IncomeExpenseEntity>>

    @Query("SELECT COUNT(*) FROM income_expenses")
    suspend fun countIncomeExpenses(): Int

    @Query("SELECT COUNT(*) FROM groups")
    suspend fun countGroups(): Int
}

@Dao
interface WorkflowDao {
    @Transaction
    suspend fun replaceAll(
        requests: List<RequestEntity>,
        requestsV2: List<RequestV2Entity>,
        loanQueue: List<LoanQueueEntity>,
        notifications: List<NotificationEntity>,
    ) {
        clearRequests()
        clearRequestsV2()
        clearLoanQueue()
        clearNotifications()
        insertRequests(requests)
        insertRequestsV2(requestsV2)
        insertLoanQueue(loanQueue)
        insertNotifications(notifications)
    }

    @Query("DELETE FROM requests")
    suspend fun clearRequests()

    @Query("DELETE FROM requests_v2")
    suspend fun clearRequestsV2()

    @Query("DELETE FROM loan_queue")
    suspend fun clearLoanQueue()

    @Query("DELETE FROM notifications")
    suspend fun clearNotifications()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequests(rows: List<RequestEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequestsV2(rows: List<RequestV2Entity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanQueue(rows: List<LoanQueueEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(rows: List<NotificationEntity>)

    @Query("SELECT * FROM notifications ORDER BY date DESC")
    fun observeNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM loan_queue ORDER BY requestDate ASC")
    fun observeLoanQueue(): Flow<List<LoanQueueEntity>>

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun countNotifications(): Int

    @Query("SELECT COUNT(*) FROM requests")
    suspend fun countRequests(): Int

    @Query("SELECT COUNT(*) FROM requests_v2")
    suspend fun countRequestsV2(): Int

    @Query("SELECT COUNT(*) FROM loan_queue")
    suspend fun countLoanQueue(): Int
}

@Dao
interface SyncMetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(meta: SyncMetaEntity)

    @Query("SELECT * FROM sync_meta WHERE id = 1")
    suspend fun get(): SyncMetaEntity?

    @Query("SELECT dataVersion FROM sync_meta WHERE id = 1")
    suspend fun currentVersion(): String?
}
