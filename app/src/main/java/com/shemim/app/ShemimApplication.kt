package com.shemim.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.shemim.core.datastore.SessionRepository
import com.shemim.core.datastore.ThemePrefsRepository
import com.shemim.core.db.ShemimDatabase
import com.shemim.core.network.KeystoreCookieStore
import com.shemim.core.network.PersistentCookieJar
import com.shemim.core.network.ShemimApiClient
import com.shemim.core.sync.SyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * کلاس Application — WP-0.3 + WP-0.5.
 *
 * وابستگی‌های سطح پردازه اینجا ساخته می‌شوند: «یک نمونه به‌ازای کل عمر
 * پردازه»، نه به‌ازای هر Composition اکتیویتی.
 *
 * زنجیرهٔ شبکه (api/cookieJar) و پایگاه‌دادهٔ کش Room روی ترد IO ساخته
 * می‌شوند؛ مسیر composition هرگز چیزی نمی‌سازد و ترد اصلی همیشه آزاد
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

        // پیش‌گرم‌کردن فریم‌ورک انیمیشن MIUI روی ترد پس‌زمینه — ADR-020.
        // گزارش فریز ۸ ثانیه‌ای گوشی کارفرما (۱۴۰۵/۰۸/۲۵) نشان داد اولین
        // مقداردهی کلاس miuix.animation.internal.AnimRunner می‌تواند روی
        // «ترد اصلی» و زیر بار سیستم (دقایقی بعد از نصب/به‌روزرسانی) طول
        // بکشد: <clinit> آن یک HandlerThread می‌سازد و با Object.wait تا
        // بالا آمدنش می‌ایستد. با مقداردهی زودهنگام روی ترد IO، هر تاخیری
        // به همین ترد منتقل می‌شود، نه ترد اصلی. روی غیر MIUI کلاس وجود
        // ندارد و این کار بی‌اثر است.
        appScope.launch {
            val prewarmed = runCatching {
                Class.forName("miuix.animation.internal.AnimRunner")
            }.isSuccess
            if (prewarmed) FreezeDiagnostics.milestone("miu-anim-prewarmed")
        }
    }
}

/**
 * ظرف وابستگی‌های دستی — جایگزین موقت DI.
 * هر وابستگی `by lazy` ساخته می‌شود: هیچ‌چیز زودتر از موعدِ نیاز واقعی
 * (اولین فراخوانی API یا اولین خواندن کش) ساخته نمی‌شود.
 *
 * `db`/`sync` از WP-0.5: کش Room همیشه در دسترس است (حتی آفلاین) و
 * SyncEngine تنها نویسندهٔ آن از طریق loadData است (سند ۰۲ §۴).
 */
class AppGraph(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** اجرای یک کار پس‌زمینهٔ سطح اپ (مثل sync در onResume) خارج از composition. */
    fun launch(block: suspend () -> Unit) {
        scope.launch { block() }
    }

    val cookieJar: PersistentCookieJar by lazy { PersistentCookieJar(KeystoreCookieStore(context)) }
    val api: ShemimApiClient by lazy { ShemimApiClient(cookieJar = cookieJar) }
    val sessionRepo: SessionRepository by lazy { SessionRepository(context) }
    val themeRepo: ThemePrefsRepository by lazy { ThemePrefsRepository(context) }

    /**
     * کش Room — فقط بازتابی از سرور است؛ افت مخرب نسخه‌ها پذیرفته شده
     * (مستند روی خود ShemimDatabase): بازسازی = یک sync.
     */
    val db: ShemimDatabase by lazy {
        Room.databaseBuilder(context, ShemimDatabase::class.java, "shemim.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    val sync: SyncEngine by lazy { SyncEngine(api = api, db = db, scope = scope) }
}
