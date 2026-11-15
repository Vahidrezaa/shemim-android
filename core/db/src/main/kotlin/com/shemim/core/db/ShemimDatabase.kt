package com.shemim.core.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shemim.core.db.dao.FundDao
import com.shemim.core.db.dao.MemberDao
import com.shemim.core.db.dao.SyncMetaDao
import com.shemim.core.db.dao.WorkflowDao
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

/**
 * کش سینک شمیم — WP-0.4.
 *
 * - این پایگاه‌داده فقط کش است: منبع حقیقت سرور است و هر loadData کامل همهٔ
 *   جداول سینک‌شونده را در یک تراکنش جایگزین می‌کند؛ بنابراین تا پیش از فاز ۶
 *   که مهاجرت‌های واقعی لازم می‌شود `exportSchema=false` و افت مخرب نسخه‌ها
 *   به‌صورت آگاهانه پذیرفته شده (بازسازی = یک sync).
 */
@Database(
    entities = [
        MemberEntity::class,
        TransactionEntity::class,
        LoanEntity::class,
        InstallmentPaymentEntity::class,
        ResignedMemberEntity::class,
        IncomeExpenseEntity::class,
        GroupEntity::class,
        GroupMemberCrossRef::class,
        FundSettingsEntity::class,
        ProfitReportEntity::class,
        RequestEntity::class,
        RequestV2Entity::class,
        LoanQueueEntity::class,
        NotificationEntity::class,
        SyncMetaEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ShemimDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun fundDao(): FundDao
    abstract fun workflowDao(): WorkflowDao
    abstract fun syncMetaDao(): SyncMetaDao
}
