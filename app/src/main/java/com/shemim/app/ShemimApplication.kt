package com.shemim.app

import android.app.Application
import android.content.Context
import com.shemim.core.datastore.SessionRepository
import com.shemim.core.datastore.ThemePrefsRepository
import com.shemim.core.network.KeystoreCookieStore
import com.shemim.core.network.PersistentCookieJar
import com.shemim.core.network.ShemimApiClient

/**
 * کلاس Application — WP-0.3.
 *
 * تا معرفی DI در WP-0.5، وابستگی‌های سطح پردازه اینجا ساخته می‌شوند:
 * «یک نمونه به‌ازای کل عمر پردازه»، نه به‌ازای هر Composition اکتیویتی
 * (ساخت در Composition با هر چرخش، OkHttpClient و CookieJar را از نو
 * می‌ساخت و بازیابی کوکی (AndroidKeyStore) را روی ترد اصلی می‌آورد).
 */
class ShemimApplication : Application() {

    val graph: AppGraph by lazy { AppGraph(this) }

    override fun onCreate() {
        super.onCreate()
        FreezeDiagnostics.install(this)
        FreezeDiagnostics.milestone("app-onCreate")
    }
}

/** ظرف وابستگی‌های دستی — جایگزین موقت DI (WP-0.5). */
class AppGraph(context: Context) {
    val cookieJar: PersistentCookieJar = PersistentCookieJar(KeystoreCookieStore(context))
    val api: ShemimApiClient = ShemimApiClient(cookieJar = cookieJar)
    val sessionRepo: SessionRepository = SessionRepository(context)
    val themeRepo: ThemePrefsRepository = ThemePrefsRepository(context)
}
