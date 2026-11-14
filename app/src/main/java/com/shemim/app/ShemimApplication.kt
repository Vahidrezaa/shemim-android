package com.shemim.app

import android.app.Application
import android.content.Context
import com.shemim.core.datastore.SessionRepository
import com.shemim.core.datastore.ThemePrefsRepository
import com.shemim.core.network.KeystoreCookieStore
import com.shemim.core.network.PersistentCookieJar
import com.shemim.core.network.ShemimApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * کلاس Application — WP-0.3.
 *
 * تا معرفی DI در WP-0.5، وابستگی‌های سطح پردازه اینجا ساخته می‌شوند:
 * «یک نمونه به‌ازای کل عمر پردازه»، نه به‌ازای هر Composition اکتیویتی.
 *
 * زنجیرهٔ شبکه (api/cookieJar) بلافاصله بعد از بالا آمدن پردازه روی ترد IO
 * «گرم» می‌شود؛ مسیر composition هرگز چیزی نمی‌سازد و ترد اصلی همیشه آزاد
 * می‌ماند. توجه: سازندهٔ KeystoreCookieStore فقط یک شیء File می‌سازد؛
 * خود AndroidKeyStore فقط در اولین فراخوانی شبکه و روی ترد IO دست می‌خورد.
 */
class ShemimApplication : Application() {

    val graph: AppGraph by lazy { AppGraph(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        FreezeDiagnostics.install(this)
        FreezeDiagnostics.milestone("app-onCreate")

        // گرم‌کردن زنجیرهٔ وابستگی‌های شبکه روی ترد پس‌زمینه (نه ترد اصلی):
        // تا وقتی صفحهٔ ورود به آن نیاز پیدا کند، آماده است.
        appScope.launch {
            graph.api
            FreezeDiagnostics.milestone("api-warmed")
        }
    }
}

/**
 * ظرف وابستگی‌های دستی — جایگزین موقت DI (WP-0.5).
 * هر وابستگی `by lazy` ساخته می‌شود: هیچ‌چیز در Application.onCreate
 * زودتر از موعدش ساخته نمی‌شود؛ تا لحظهٔ نیاز واقعی (اولین فراخوانی API)
 * هیچ کار سنگینی انجام نمی‌گیرد.
 */
class AppGraph(private val context: Context) {
    val cookieJar: PersistentCookieJar by lazy { PersistentCookieJar(KeystoreCookieStore(context)) }
    val api: ShemimApiClient by lazy { ShemimApiClient(cookieJar = cookieJar) }
    val sessionRepo: SessionRepository by lazy { SessionRepository(context) }
    val themeRepo: ThemePrefsRepository by lazy { ThemePrefsRepository(context) }
}
