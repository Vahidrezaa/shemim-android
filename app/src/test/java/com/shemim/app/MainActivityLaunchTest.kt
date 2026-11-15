package com.shemim.app

import android.app.Application
import android.content.Context
import android.content.Intent
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

    /** بدون نشست → دست‌به‌دست‌ شدن به LoginActivity (صفحهٔ ورود View-محور). */
    @Test
    fun loggedOut_handsOffToLoginActivity() {
        // تایپ استاتیک باید Application باشد تا overload درستِ shadowOf رزولو شود
        // (با Context، ShadowApplication و nextStartedActivity پیدا نمی‌شود).
        val app = ApplicationProvider.getApplicationContext<Application>()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var started: Intent? = null
            var attempts = 0
            // intent را هم از سایهٔ خود اکتیویتی می‌پرسیم (مسیر رسمی
            // activity.startActivity) و هم از سایهٔ اپلیکیشن (مسیر
            // context-level) — هر کدام زودتر ثبت شده باشد.
            while (started == null && attempts < 100) {
                shadowOf(Looper.getMainLooper()).idle()
                scenario.onActivity { activity ->
                    if (started == null) {
                        started = shadowOf(activity).nextStartedActivity
                            ?: shadowOf(app).nextStartedActivity
                    }
                }
                if (started == null) Thread.sleep(20)
                attempts++
            }
            assertTrue("MainActivity در حالت خروج، LoginActivity را باز نکرد", started != null)
            assertTrue(
                "اکتیویتی باز‌شده LoginActivity نبود: ${started?.component}",
                started?.component?.className == LoginActivity::class.java.name,
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
