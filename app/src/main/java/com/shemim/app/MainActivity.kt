package com.shemim.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shemim.app.ui.HomePlaceholderScreen
import com.shemim.app.ui.ThemeShowcaseScreen
import com.shemim.core.datastore.SessionState
import com.shemim.core.datastore.ThemePrefs
import com.shemim.core.designsystem.theme.ShemimTheme
import com.shemim.feature.auth.LoginScreen
import com.shemim.feature.auth.LoginViewModel
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // اسپلشِ مدیریت‌شدهٔ رسمی — الزاماً قبل از super.onCreate.
        // هنداف اسپلش را از starting-window سیستمی (که لاگ MIUI نشان داد
        // کانال ورودی‌اش «کثیف» رها می‌شود) به مسیر کتابخانهٔ رسمی می‌برد.
        val splash = installSplashScreen()
        // خروج بی‌انیمیشن اسپلش — رفع «پردهٔ سفید گیرکرده» روی MIUI:
        // شواهد لاگ همین گوشی (پنجرهٔ رهاشده با آلفای ~۰.۱) نشان داد
        // محوشدن سیستمیِ نمای اسپلش روی این دستگاه نیمه‌کاره می‌ماند و
        // لایه‌ای تقریباً مات روی اپ می‌ماند: تیترِ درشت از زیرش سرِستی
        // می‌شود ولی خطوط نازک فیلدها و دکمه محو می‌شوند و فقط موقع
        // recomposition سنگین (لودینگِ «در حال بررسی…») لحظه‌ای پیدا
        // می‌شوند. با remove() مستقیم، هیچ انیمیشن خروجی برای گیرکردن
        // باقی نمی‌ماند. نقطهٔ عطف تشخیصی هم ثبت می‌شود تا در گزارش بعدی
        // معلوم شود این مسیر واقعاً اجرا شده است.
        splash.setOnExitAnimationListener { provider ->
            FreezeDiagnostics.milestone("splash-view-removed")
            provider.remove()
        }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FreezeDiagnostics.milestone("activity-onCreate")
        setContent {
            FreezeDiagnostics.milestone("composed")

            // ظرف وابستگی‌های سطح پردازه — یک نمونه برای کل عمر اپ.
            // api/cookieJar عمداً اینجا گرفته نمی‌شوند: زنجیرهٔ شبکه در
            // ShemimApplication روی ترد IO گرم شده و فقط در لحظهٔ نیاز
            // (فرم ورود/خروج) لمس می‌شود — composition هیچ چیزی نمی‌سازد.
            val graph = (application as ShemimApplication).graph
            val sessionRepo = graph.sessionRepo
            val themeRepo = graph.themeRepo
            val scope = rememberCoroutineScope()

            val prefs by themeRepo.prefs.collectAsState(initial = ThemePrefs.Default)
            val session by sessionRepo.session.collectAsState(initial = SessionState.Loading)

            // «auth_required» از هر فراخوانی → پاک‌شدن نشست محلی و برگشت به ورود
            // (همان forceLogoutToLoginPage در auth-flow.js — سند ۰۲ §۳).
            // کل جمع‌آوری روی ترد IO — دست نزدن به ترد اصلی.
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    graph.api.authEvents.collect { sessionRepo.clear() }
                }
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
                        // ردیابی لمس در سطح ریشه: اگر در فریزِ بعدی «pointer-Press»
                        // در لاگ ثبت شد یعنی لمس به Compose رسیده (مشکل داخل اپ)؛
                        // ثبت نشد یعنی مرگ ورودی سیستم‌ساید است.
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == PointerEventType.Press ||
                                        event.type == PointerEventType.Release
                                    ) {
                                        FreezeDiagnostics.milestone("pointer-${event.type}")
                                    }
                                }
                            }
                        }
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
                                viewModel { LoginViewModel(graph.api, sessionRepo) }
                            val loginState by loginViewModel.state.collectAsState()
                            // نشانگر نسخه زیر صفحهٔ ورود — ردگیری اینکه کدام بیلد روی گوشی است
                            Box(modifier = Modifier.fillMaxSize()) {
                                LoginScreen(
                                    state = loginState,
                                    onLogin = loginViewModel::login,
                                    onConsumeError = loginViewModel::consumeError,
                                )
                                Text(
                                    text = getString(R.string.skeleton_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toString()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = 6.dp),
                                )
                            }
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
                                        // (کل کار شبکه/فایل روی ترد IO)
                                        scope.launch(Dispatchers.IO) {
                                            graph.api.logout()
                                            graph.cookieJar.clear()
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
        // صادرات گزارش تازهٔ فریز/کرش به Downloads (در پیش‌زمینه مجاز است)
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
