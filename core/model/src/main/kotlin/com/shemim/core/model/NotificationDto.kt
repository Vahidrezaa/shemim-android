package com.shemim.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * اطلاعیه (جدول notifications) — ردیف خام به‌علاوهٔ readBy (وضعیت خوانده‌شدهٔ
 * همهٔ اعضا) و replies (پاسخ‌های حاضرین، JSON دیکدشده) و files (پیوست‌ها از
 * notification_files توسط nf_attach).
 *
 * حریم خصوصی: اطلاعیه‌های دارای targetMemberId قبل از ارسال برای اعضای دیگر
 * در سرور فیلتر می‌شوند (data-helpers.php §notifications).
 */
@Serializable
data class NotificationDto(
    val id: String,
    val title: String = "",
    val message: String = "",
    /** normal | high | … */
    val priority: String = "normal",
    /** تاریخ شمسی */
    val date: String = "",
    /** all | member | … */
    val target: String = "all",
    /** فقط برای اطلاعیهٔ اختصاصی (تشکر/هشدار معوقه) */
    @SerialName("target_member_id") val targetMemberId: String? = null,
    /** پاسخ‌ها — ساختار UI وابسته؛ فعلاً JSON شفاف (WP-1.5 مدلش می‌کند) */
    val replies: List<JsonObject> = emptyList(),
    /** شناسهٔ اعضایی که خوانده‌اند */
    val readBy: List<String> = emptyList(),
    val files: List<NotificationFileDto> = emptyList(),
)

/** پیوست اطلاعیه (nf_attach) — دانلود در WP-1.5 با اکشن notif-files. */
@Serializable
data class NotificationFileDto(
    val id: String,
    val name: String = "",
    val mime: String? = null,
    val size: Long = 0,
)
