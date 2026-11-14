package com.shemim.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shemim.app.ui.HomePlaceholderScreen
import com.shemim.app.ui.ThemeShowcaseScreen
import com.shemim.core.datastore.SessionState
import com.shemim.core.datastore.ThemePrefs
import com.shemim.core.designsystem.theme.ShemimTheme
import com.shemim.feature.auth.LoginScreen
import com.shemim.feature.auth.LoginViewModel
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FreezeDiagnostics.milestone("activity-onCreate")
        setContent {
            FreezeDiagnostics.milestone("composed")

            // ظرف وابستگی‌های سطح پردازه — یک نمونه برای کل عمر اپ
            val graph = (application as ShemimApplication).graph
            val api = graph.api
            val cookieJar = graph.cookieJar
            val sessionRepo = graph.sessionRepo
            val themeRepo = graph.themeRepo
            val scope = rememberCoroutineScope()

            val prefs by themeRepo.prefs.collectAsState(initial = ThemePrefs.Default)
            val session by sessionRepo.session.collectAsState(initial = SessionState.Loading)

            // «auth_required» از هر فراخوانی → پاک‌شدن نشست محلی و برگشت به ورود
            // (همان forceLogoutToLoginPage در auth-flow.js — سند ۰۲ §۳)
            LaunchedEffect(api) {
                api.authEvents.collect { sessionRepo.clear() }
            }

            var showThemes by remember { mutableStateOf(false) }

            // نقاط عطف تشخیصی: هر تغییر وضعیت نشست + خروج به خانه (نه صفحهٔ تم‌ها)
            LaunchedEffect(session) {
                when (session) {
                    is SessionState.Loading -> FreezeDiagnostics.milestone("ui-loading")
                    is SessionState.LoggedOut -> FreezeDiagnostics.milestone("ui-login")
                    is SessionState.Active -> FreezeDiagnostics.milestone("ui-home")
                }
                if (session is SessionState.LoggedOut) showThemes = false
            }

            ShemimTheme(
                themeKey = prefs.themeKey,
                darkMode = prefs.darkMode,
                dynamicColor = prefs.dynamicColor,
            ) {
                // لحظهٔ رسم اولین فریم — اگر فریز بعد از این نقطه باشد،
                // مسئله «خروج اسپلش/رندر» است؛ اگر قبل از آن، مسئله composition.
                val drewFirstFrame = remember { AtomicBoolean(false) }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            if (drewFirstFrame.compareAndSet(false, true)) {
                                FreezeDiagnostics.milestone("first-frame-drawn")
                            }
                        },
                ) {
                    when (val s = session) {
                        is SessionState.Loading -> LoadingSplash()

                        is SessionState.LoggedOut -> {
                            val loginViewModel: LoginViewModel =
                                viewModel { LoginViewModel(api, sessionRepo) }
                            val loginState by loginViewModel.state.collectAsState()
                            LoginScreen(
                                state = loginState,
                                onLogin = loginViewModel::login,
                                onConsumeError = loginViewModel::consumeError,
                            )
                        }

                        is SessionState.Active -> {
                            if (showThemes) {
                                BackHandler { showThemes = false }
                                ThemeShowcaseScreen(
                                    prefs = prefs,
                                    onThemeChange = { key -> scope.launch { themeRepo.setTheme(key) } },
                                    onDarkModeChange = { mode -> scope.launch { themeRepo.setDarkMode(mode) } },
                                    onDynamicColorChange = { enabled ->
                                        scope.launch { themeRepo.setDynamicColor(enabled) }
                                    },
                                )
                            } else {
                                HomePlaceholderScreen(
                                    user = s.user,
                                    onOpenThemes = { showThemes = true },
                                    onLogout = {
                                        // فوری: نشست محلی پاک می‌شود تا UI همان لحظه به ورود
                                        // برگردد (وگرنه در آفلاین تا ۱۵ ثانیه منتظر تایم‌اوت
                                        // خروج سرور می‌ماند و «فریز» به نظر می‌رسید)
                                        scope.launch { sessionRepo.clear() }
                                        // بعد: خروج سمت سرور با کوکی فعلی، سپس پاک‌کردن کوکی
                                        scope.launch {
                                            api.logout()
                                            cookieJar.clear()
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // اینجا اپ در پیش‌زمینه است: صادرات گزارشِ تازه به Downloads + اعلان
        // (در Application.onCreate اندروید ۱۰+ اجازهٔ بازکردن UI نمی‌دهد)
        FreezeDiagnostics.onAppForeground(this)
    }
}

/** چند فریم اول تا رسیدن دادهٔ نشست از DataStore. */
@Composable
private fun LoadingSplash() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}
