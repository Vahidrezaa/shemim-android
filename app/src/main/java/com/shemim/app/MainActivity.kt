package com.shemim.app

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.shemim.app.ui.AppShellScreen
import com.shemim.core.datastore.SessionState
import com.shemim.core.datastore.ThemePrefs
import com.shemim.core.designsystem.theme.ShemimTheme
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * میزبان بخش «وارد‌شده» — پیشخوان و اسکلت ناوبری (WP-0.5 + WP-0.7).
 *
 * این اکتیویتی فقط سه حالت دارد: Loading (چند فریم اول تا خواندن
 * DataStore)، LoggedOut (دست‌به‌دست‌ شدن به LoginActivity با رندر View —
 * بلافاصله finish تا انباشته نشود) و Active (پوستهٔ اپ: نوار پایین
 * نقش‌محور + پیشخوان فاز ۰). اسپلشِ مدیریت‌شده به LoginActivity منتقل
 * شده است (LAUNCHER جدید).
 *
 * محرک‌های sync (سند ۰۲ §۴): شروع نشست در composition + بازگشت به اپ
 * (onResume با نرخ‌حد ۳۰ ثانیه) + دکمهٔ دستی روی پیشخوان.
 */
class MainActivity : ComponentActivity() {

    /** آخرین زمان sync خودکارِ onResume — نرخ‌حد محرک بازگشت به اپ. */
    private var lastResumeSyncAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FreezeDiagnostics.milestone("activity-onCreate")
        setContent {
            FreezeDiagnostics.milestone("composed")

            // ظرف وابستگی‌های سطح پردازه — یک نمونه برای کل عمر اپ.
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

            // نقاط عطف تشخیصی: هر تغییر وضعیت نشست
            LaunchedEffect(session) {
                when (session) {
                    is SessionState.Loading -> FreezeDiagnostics.milestone("ui-loading")
                    is SessionState.LoggedOut -> FreezeDiagnostics.milestone("ui-login")
                    is SessionState.Active -> FreezeDiagnostics.milestone("ui-home")
                }
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
                        // ردیابی لمس در سطح ریشه
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
                            // ورود دیگر کامپوز نیست: اکتیویتی مستقل View-محور
                            // باز می‌شود و این اکتیویتی بسته می‌شود (بدون انباشت
                            // نمونه‌ها). پاک‌شدن نشست (خروج یا auth_required) هم
                            // از همین مسیر به ورود برمی‌گردد.
                            LaunchedEffect(s) {
                                FreezeDiagnostics.milestone("handoff-to-login-activity")
                                startActivity(
                                    Intent(this@MainActivity, LoginActivity::class.java),
                                )
                                finish()
                            }
                        }

                        is SessionState.Active -> {
                            // UI همیشه از Room می‌خواند (سند ۰۲ §۴) — sync فقط
                            // پس‌زمینه صادقانه این Flowها را به‌روز می‌کند.
                            val syncState by graph.sync.state.collectAsState()
                            val settings by graph.db.fundDao()
                                .observeSettings()
                                .collectAsState(initial = null)
                            val memberCount by graph.db.memberDao()
                                .observeMemberCount()
                                .collectAsState(initial = 0)
                            val transactionCount by graph.db.memberDao()
                                .observeTransactionCount()
                                .collectAsState(initial = 0)
                            val loanCount by graph.db.memberDao()
                                .observeLoanCount()
                                .collectAsState(initial = 0)

                            // محرک شروع: اولین compositionِ حالت Active یک sync
                            // خودکار می‌گیرد (با نرخ‌حد، تا ورود/خروج سریع
                            // پشت‌سرهم درخواست اضافه نسازد).
                            LaunchedEffect(Unit) {
                                graph.sync.maybeRefresh("app-start", minIntervalMs = 15_000)
                            }

                            AppShellScreen(
                                prefs = prefs,
                                onThemeChange = { key ->
                                    scope.launch { themeRepo.setTheme(key) }
                                },
                                onDarkModeChange = { mode ->
                                    scope.launch { themeRepo.setDarkMode(mode) }
                                },
                                onDynamicColorChange = { enabled ->
                                    scope.launch { themeRepo.setDynamicColor(enabled) }
                                },
                                user = s.user,
                                syncState = syncState,
                                settings = settings,
                                memberCount = memberCount,
                                transactionCount = transactionCount,
                                loanCount = loanCount,
                                onSyncNow = {
                                    scope.launch {
                                        // مثل دکمهٔ دستی وب: بدون sinceVersion، همیشه کامل
                                        graph.sync.refresh("manual", force = true)
                                    }
                                },
                                onLogout = {
                                    // فوری: نشست محلی پاک می‌شود تا UI همان لحظه به ورود
                                    // برگردد (وگرنه در آفلاین تا ۱۵ ثانیه منتظر تایم‌اوت
                                    // خروج سرور می‌ماند و «فریز» به نظر می‌رسید)
                                    scope.launch { sessionRepo.clear() }
                                    // بعد: خروج سمت سرور با کوکی فعلی، سپس پاک‌کردن کوکی
                                    scope.launch(Dispatchers.IO) {
                                        graph.api.logout()
                                        graph.cookieJar.clear()
                                    }
                                },
                                versionLabel = getString(
                                    R.string.skeleton_version,
                                    BuildConfig.VERSION_NAME,
                                    BuildConfig.VERSION_CODE.toString(),
                                ),
                            )
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

        // محرک بازگشت به اپ با نرخ‌حد (سند ۰۲ §۴) — خارج از composition،
        // روی اسکوپ پس‌زمینهٔ گراف.
        val now = SystemClock.elapsedRealtime()
        if (now - lastResumeSyncAt >= RESUME_SYNC_MIN_INTERVAL_MS) {
            lastResumeSyncAt = now
            val appGraph = (application as ShemimApplication).graph
            appGraph.launch {
                appGraph.sync.maybeRefresh("on-resume", minIntervalMs = RESUME_SYNC_MIN_INTERVAL_MS)
            }
        }
    }

    private companion object {
        /** حداقل فاصلهٔ sync خودکارِ onResume — ۳۰ ثانیه. */
        const val RESUME_SYNC_MIN_INTERVAL_MS = 30_000L
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
