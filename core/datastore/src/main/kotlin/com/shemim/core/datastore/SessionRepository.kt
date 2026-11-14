package com.shemim.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * کاربر ذخیره‌شدهٔ نشست — همان چیزی که وب با کلید `sandogh_session`
 * در localStorage نگه می‌داشت (currentUser).
 */
data class StoredUser(
    val id: String,
    val username: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val role: String = "user",
)

/**
 * وضعیت نشست اپ — منبع حقیقت UI برای «وارد شده‌ایم یا نه» (WP-0.3).
 */
sealed interface SessionState {
    /** هنوز از DataStore خوانده نشده — چند فریم اول اپ. */
    data object Loading : SessionState

    /** کاربر وارد شده (بازسازی بهینهٔ نشست — ADR-019). */
    data class Active(val user: StoredUser) : SessionState

    data object LoggedOut : SessionState
}

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "shemim_session",
)

/**
 * نشست کاربر — WP-0.3.
 *
 * مثل وب: بعد از لاگین، اطلاعات کاربر محلی ذخیره می‌شود و باز شدن بعدیِ اپ
 * بلافاصله وارد می‌شود (`restoreSession` وب)؛ اعتبار واقعی نشست با اولین
 * فراخوانی API بعدی سنجیده می‌شود (`auth_required` → خروج خودکار).
 *
 * خودِ کوکیِ نشست (مقدار حساس) رمزنگاری‌شده در `core:network`
 * (`KeystoreCookieStore`) نگه داشته می‌شود — اینجا فقط مشخصات کاربر است.
 */
class SessionRepository(private val context: Context) {

    private object Keys {
        val ID = stringPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("user_username")
        val FIRST_NAME = stringPreferencesKey("user_first_name")
        val LAST_NAME = stringPreferencesKey("user_last_name")
        val ROLE = stringPreferencesKey("user_role")
    }

    val session: Flow<SessionState> = context.sessionDataStore.data.map { p ->
        val id = p[Keys.ID]
        if (id.isNullOrBlank()) {
            SessionState.LoggedOut
        } else {
            SessionState.Active(
                StoredUser(
                    id = id,
                    username = p[Keys.USERNAME] ?: "",
                    firstName = p[Keys.FIRST_NAME]?.takeIf { it.isNotBlank() },
                    lastName = p[Keys.LAST_NAME]?.takeIf { it.isNotBlank() },
                    role = p[Keys.ROLE] ?: "user",
                ),
            )
        }
    }

    suspend fun save(user: StoredUser) {
        context.sessionDataStore.edit {
            it[Keys.ID] = user.id
            it[Keys.USERNAME] = user.username
            it[Keys.FIRST_NAME] = user.firstName.orEmpty()
            it[Keys.LAST_NAME] = user.lastName.orEmpty()
            it[Keys.ROLE] = user.role
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }
}
