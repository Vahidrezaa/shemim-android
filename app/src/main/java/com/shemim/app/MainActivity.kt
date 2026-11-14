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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shemim.app.ui.HomePlaceholderScreen
import com.shemim.app.ui.ThemeShowcaseScreen
import com.shemim.core.datastore.SessionRepository
import com.shemim.core.datastore.SessionState
import com.shemim.core.datastore.ThemePrefs
import com.shemim.core.datastore.ThemePrefsRepository
import com.shemim.core.designsystem.theme.ShemimTheme
import com.shemim.core.network.KeystoreCookieStore
import com.shemim.core.network.PersistentCookieJar
import com.shemim.core.network.ShemimApiClient
import com.shemim.feature.auth.LoginScreen
import com.shemim.feature.auth.LoginViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // تا معرفی DI در WP-0.5 — ساخت دستی وابستگی‌ها (یک نمونه به‌ازای هر اجرا)
            val appContext = applicationContext
            val cookieJar = remember { PersistentCookieJar(KeystoreCookieStore(appContext)) }
            val api = remember { ShemimApiClient(cookieJar = cookieJar) }
            val sessionRepo = remember { SessionRepository(appContext) }
            val themeRepo = remember { ThemePrefsRepository(appContext) }
            val scope = rememberCoroutineScope()

            val prefs by themeRepo.prefs.collectAsState(initial = ThemePrefs.Default)
            val session by sessionRepo.session.collectAsState(initial = SessionState.Loading)

            // «auth_required» از هر فراخوانی → پاک‌شدن نشست محلی و برگشت به ورود
            // (همان forceLogoutToLoginPage در auth-flow.js — سند ۰۲ §۳)
            LaunchedEffect(api) {
                api.authEvents.collect { sessionRepo.clear() }
            }

            var showThemes by remember { mutableStateOf(false) }

            // خروج/تبدیل نشست → بازگشت به حالت خانه (نه صفحهٔ تم‌ها)
            LaunchedEffect(session) {
                if (session is SessionState.LoggedOut) showThemes = false
            }

            ShemimTheme(
                themeKey = prefs.themeKey,
                darkMode = prefs.darkMode,
                dynamicColor = prefs.dynamicColor,
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
                                    scope.launch {
                                        // مثل وب: logout سمت سرور (نتیجه‌اش مسیر را عوض نمی‌کند)
                                        // سپس پاک‌کردن کامل محلی — حتی اگر آفلاین بودیم
                                        api.logout()
                                        cookieJar.clear()
                                        sessionRepo.clear()
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
