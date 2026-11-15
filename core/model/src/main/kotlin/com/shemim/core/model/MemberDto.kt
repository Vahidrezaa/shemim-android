package com.shemim.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * عضو — از حلقهٔ اعضای loadData().
 *
 * فیلدهای مشروط (فقط برای بینندهٔ دارای مجوز ارسال می‌شوند و برای بقیه اصلاً
 * در JSON نیستند): `fatherName`/`nationalCode` (مدیر، ناظر‌نوشتار، سرگروه، خودِ
 * عضو) و `isSupervisor`/`supervisorPerms` (مدیر؛ خودِ عضو فقط برای خودش).
 * `supervisorPerms` ساختار authz_normalizePerms دارد و در WP-2.3 مدل می‌شود.
 */
@Serializable
data class MemberDto(
    val id: String,
    val membershipNumber: Int = 0,
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    /** admin | groupLeader | member | … (نقش‌های سرور — استرینگ برای سازگاری رو به جلو) */
    val role: String = "",
    val isOwner: Boolean = false,
    val joinDate: String? = null,
    val birthDate: String? = null,
    val birthDateLocked: Boolean = false,
    val phone: String? = null,
    /** برای بینندهٔ بدون مجوز رشتهٔ خالی ارسال می‌شود (نه null) */
    val email: String = "",
    /** کلید تم سرور (هم‌ارز updateMyTheme) */
    val colorTheme: String? = null,
    val transactions: List<TransactionDto> = emptyList(),
    val loans: List<LoanDto> = emptyList(),
    val fatherName: String? = null,
    val nationalCode: String? = null,
    val isSupervisor: Boolean? = null,
    val supervisorPerms: JsonObject? = null,
)
