package com.shemim.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shemim.core.datastore.SessionRepository
import com.shemim.core.datastore.StoredUser
import com.shemim.core.network.ApiResult
import com.shemim.core.network.ShemimApiClient
import com.shemim.core.network.parseLoginUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * خطای فرم ورود — متنِ سرور مستقیم نمایش داده می‌شود؛
 * خطاهای محلی به رشتهٔ منبع مپ می‌شوند (در LoginScreen).
 */
sealed interface LoginError {
    /** پیام فارسی از خود سرور (مثل «نام کاربری یا رمز عبور اشتباه است»). */
    data class Server(val message: String) : LoginError

    data object Required : LoginError
    data object Offline : LoginError
    data object Network : LoginError
    data object Malformed : LoginError
    data object AuthRequired : LoginError
}

/** وضعیت فرم ورود — WP-0.3. */
data class LoginUiState(
    val loading: Boolean = false,
    val error: LoginError? = null,
    /** true یعنی ورود موفق بوده؛ UI از روی SessionState عوض می‌شود. */
    val success: Boolean = false,
) {
    val canSubmit: Boolean get() = !loading && !success
}

/**
 * ViewModel ورود — WP-0.3.
 *
 * جریان همان وب است (auth-flow.js):
 * چک اعتبار با اکشن `login` → ذخیرهٔ کاربر در نشست محلی →
 * UI از روی `SessionRepository.session` به خانه می‌رود؛
 * `loadData` جداگانه در پس‌زمینه انجام می‌شود (WP-0.5).
 */
class LoginViewModel(
    private val api: ShemimApiClient,
    private val sessions: SessionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun login(username: String, password: String) {
        val user = username.trim()
        if (!_state.value.canSubmit) return

        // همان چک اولیهٔ وب (و همان متن سرور)
        if (user.isEmpty() || password.isEmpty()) {
            _state.update { it.copy(error = LoginError.Required) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = api.login(user, password)) {
                is ApiResult.Ok -> {
                    val parsed = parseLoginUser(result.body)
                    if (parsed == null) {
                        _state.update { it.copy(loading = false, error = LoginError.Malformed) }
                    } else {
                        sessions.save(
                            StoredUser(
                                id = parsed.id,
                                username = parsed.username,
                                firstName = parsed.firstName,
                                lastName = parsed.lastName,
                                role = parsed.role,
                            ),
                        )
                        _state.update { it.copy(loading = false, success = true) }
                    }
                }

                is ApiResult.ServerError -> _state.update {
                    // متن error خود سرور فارسی و قابل نمایش است (مثل too_many_attempts)
                    it.copy(
                        loading = false,
                        error = LoginError.Server(
                            result.error ?: result.message ?: "",
                        ),
                    )
                }

                is ApiResult.AuthRequired -> _state.update {
                    it.copy(loading = false, error = LoginError.AuthRequired)
                }

                is ApiResult.NetworkError -> _state.update {
                    it.copy(
                        loading = false,
                        error = if (result.likelyOffline) LoginError.Offline else LoginError.Network,
                    )
                }

                is ApiResult.MalformedResponse -> _state.update {
                    it.copy(loading = false, error = LoginError.Malformed)
                }
            }
        }
    }

    /** پاک‌کردن پیام خطا (وقتی کاربر دوباره تایپ می‌کند). */
    fun consumeError() {
        _state.update { it.copy(error = null) }
    }
}
