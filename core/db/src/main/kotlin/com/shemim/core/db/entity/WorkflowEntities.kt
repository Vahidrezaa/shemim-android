package com.shemim.core.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** موجودیت‌های گردش‌کار: درخواست‌ها، نوبت وام، اطلاعیه‌ها و فرادادهٔ sync. */

@Entity(tableName = "requests", indices = [Index("memberId"), Index("status")])
data class RequestEntity(
    @PrimaryKey val id: String,
    val type: String?,
    val memberId: String?,
    val memberName: String?,
    val amount: Long,
    val description: String?,
    val requestDate: String?,
    val status: String?,
    val requestedBy: String?,
    val category: String?,
    val fromMonth: Int?,
    val fromYear: Int?,
    val toMonth: Int?,
    val toYear: Int?,
    val installments: Int?,
    val preferredDate: String?,
    val loanId: String?,
    val installmentNumber: Int?,
    val dueDate: String?,
    val installmentDate: String?,
    val approvedBy: String,
    val approvedAt: String,
    val rejectReason: String,
)

/** همان ساختار — جدول V2 (ردیف‌های خام سرور). */
@Entity(tableName = "requests_v2", indices = [Index("memberId"), Index("status")])
data class RequestV2Entity(
    @PrimaryKey val id: String,
    val type: String?,
    val memberId: String?,
    val memberName: String?,
    val amount: Long,
    val description: String?,
    val requestDate: String?,
    val status: String?,
    val requestedBy: String?,
    val category: String?,
    val fromMonth: Int?,
    val fromYear: Int?,
    val toMonth: Int?,
    val toYear: Int?,
    val installments: Int?,
    val preferredDate: String?,
    val loanId: String?,
    val installmentNumber: Int?,
    val dueDate: String?,
    val installmentDate: String?,
    val approvedBy: String,
    val approvedAt: String,
    val rejectReason: String,
)

@Entity(tableName = "loan_queue", indices = [Index("memberId"), Index("status")])
data class LoanQueueEntity(
    @PrimaryKey val id: String,
    val memberId: String,
    val memberName: String,
    val requestDate: String?,
    val status: String?,
    val priority: String?,
    val requestedAmount: Long,
    val requestedInstallments: Int,
    val adminEstimatedDate: String,
    val loanId: String?,
    val loanNumber: Int?,
    val grantedAt: String?,
)

@Entity(tableName = "notifications", indices = [Index("date"), Index("targetMemberId")])
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val priority: String,
    val date: String,
    val target: String,
    val targetMemberId: String?,
    /** JSON خام: آرایهٔ پاسخ‌ها / شناسهٔ خوانده‌ندگان / پیوست‌ها */
    val repliesJson: String,
    val readByJson: String,
    val filesJson: String,
)

/** فرادادهٔ سینک — ردیف یگانه (id=1). */
@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey val id: Int = 1,
    val dataVersion: String,
    val syncedAtMillis: Long,
)
