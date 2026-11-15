package com.shemim.core.model

import kotlinx.serialization.Serializable

/**
 * گروه کاری صندوق (جداول groups + group_members) — سرگروهِ هر گروه به عنوان
 * «اعضای من» فیلتر می‌شود.
 */
@Serializable
data class GroupDto(
    val id: String,
    val leaderId: String = "",
    val memberIds: List<String> = emptyList(),
)
