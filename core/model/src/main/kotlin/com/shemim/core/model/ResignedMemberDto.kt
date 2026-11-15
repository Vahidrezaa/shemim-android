package com.shemim.core.model

import kotlinx.serialization.Serializable

/**
 * عضو انصرافی (role=resigned) — ردیف خام به‌علاوهٔ کلید‌های camelCase اضافه‌شده.
 * کلیدهای خام snake_case موازی (first_name و …) با ignoreUnknownKeys نادیده
 * گرفته می‌شوند؛ اینجا فقط فیلدهای پایدار مدل شده‌اند.
 */
@Serializable
data class ResignedMemberDto(
    val id: String,
    val membershipNumber: Int = 0,
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val role: String = "",
    val joinDate: String? = null,
    val birthDate: String? = null,
    /** فقط مدیر می‌بیند؛ برای بقیه خالی */
    val email: String = "",
    val phone: String? = null,
    val resignedDate: String = "",
)
