package com.shemim.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** نقش‌های صندوق — همان رشته‌های ستون `members.role` در سرور. */
object UserRoles {
    const val ADMIN = "admin"
    const val GROUP_LEADER = "groupLeader"
    const val USER = "user"
    const val RESIGNED = "resigned"
}

/**
 * کاربر واردشده — همان آبجکت `user` در پاسخ اکشن `login` (api/members.php):
 * id رشته‌ای است (مثل اعضای ثبت‌نامی `reg_…`)، نه عدد.
 */
@Serializable
data class ShemimUser(
    val id: String,
    val username: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val role: String = UserRoles.USER,
)

private val lenientJson = Json { ignoreUnknownKeys = true }

/**
 * پارس آبجکت `user` از بدنهٔ پاسخ login.
 * null یعنی پاسخ شکل موردانتظار را نداشت (خطای سرور/تغییر قرارداد).
 */
fun parseLoginUser(body: JsonObject): ShemimUser? = try {
    body["user"]?.let { element ->
        lenientJson.decodeFromJsonElement(ShemimUser.serializer(), element)
    }
} catch (e: SerializationException) {
    null
} catch (e: IllegalArgumentException) {
    null
}
