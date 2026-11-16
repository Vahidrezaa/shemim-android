package com.shemim.app

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shemim.core.datastore.SessionRepository
import com.shemim.core.datastore.StoredUser
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * تست چرخهٔ MainActivity پس از بازنویسی اساسی — صفحهٔ ورود دیگر کامپوز
 * نیست (LoginActivity با View)؛ این تست‌ها فقط دو مسیر باقی‌مانده را
 * پوشش می‌دهند: نشست ذخیره‌شده → خانه (Active) و خروج → دست‌به‌دست‌ شدن
 * به LoginActivity. همه بر پایهٔ ActivityScenario + نقاط عطف
 * FreezeDiagnostics — بدون Compose rule.
 *
 * نکتهٔ حیاتی دربارهٔ ترتیب اجرا: DataStore با delegate
 * `preferencesDataStore` یک singleton در طول عمر JVM تست است و بین
 * همهٔ تست‌ها مشترک می‌ماند (حتی با app تازه در هر تست). اگر تستِ
 * «نشست ذخیره‌شده» قبل از تستِ خروج اجرا شود، کاربر seed‌شده در همان
 * نمونه باقی می‌ماند و تست خروج به‌اشتباه Active می‌بیند. به همین دلیل
 * تست خروج، نشست را پیش از launch صریحاً clear می‌کند تا فارغ از ترتیبِ
 * اجرای تست‌ها همیشه LoggedOut ببیند.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MainActivityLaunchTest {

    /** نشست ذخیره‌شده → مستقیم خانه (مسیر Active) بدون رفتن به ورود و بدون فریز. */
    @Test
    fun storedSession_showsHomeScreen_withoutFreezing() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        runBlocking {
            SessionRepository(app).save(StoredUser(id = "u1", username = "testuser"))
        }
        ActivityScenario.launch(MainActivity::class.java).use { _ ->
            val milestones = File(app.filesDir, "milestones.log")
            var found = false
            var attempts = 0
            while (!found && attempts < 100) {
                shadowOf(Looper.getMainLooper()).idle()
                found = runCatching { milestones.readText() }
                    .getOrDefault("")
                    .contains("MILESTONE ui-home")
                if (!found) Thread.sleep(20)
                attempts++
            }
            assertTrue("خانه پس از نشست ذخیره‌شده رندر نشد (ui-home ثبت نشد)", found)
        }
    }

    /**
     * نشست پاک‌شده → دست‌به‌دست‌ شدن به LoginActivity (صفحهٔ ورود
     * View-محور). صریحاً clear می‌کنیم چون DataStore بین تست‌های یک
     * JVM مشترک است (توضیح بالای کلاس)؛ milestone در فایل همین تست
     * ثبت می‌شود و مسیر واقعی handoff را اثبات می‌کند.
     */
    @Test
    fun loggedOut_handsOffToLoginActivity() {
        val app = ApplicationProvider.getApplicationContext<Context>()
        runBlocking { SessionRepository(app).clear() }
        ActivityScenario.launch(MainActivity::class.java).use { _ ->
            val milestones = File(app.filesDir, "milestones.log")
            var found = false
            var attempts = 0
            while (!found && attempts < 150) {
                shadowOf(Looper.getMainLooper()).idle()
                found = runCatching { milestones.readText() }
                    .getOrDefault("")
                    .contains("MILESTONE handoff-to-login-activity")
                if (!found) Thread.sleep(20)
                attempts++
            }
            assertTrue(
                "مسیر دست‌به‌دست شدن به صفحهٔ ورود اجرا نشد (milestone ثبت نشد)",
                found,
            )
        }
    }

    /** عیب‌یابی بدون گزارش فریز/کرش: باید بی‌صدا و بی‌کرش no-op باشد. */
    @Test
    fun onAppForeground_isSafe_withoutReport() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        FreezeDiagnostics.onAppForeground(ctx)
    }
}
