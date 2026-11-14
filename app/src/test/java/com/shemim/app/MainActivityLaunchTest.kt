package com.shemim.app

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shemim.core.datastore.SessionRepository
import com.shemim.core.datastore.StoredUser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * تست ران‌تایم کامل MainActivity روی JVM (Robolectric) — همان چرخهٔ
 * lifecycle/رندر/UI واقعی بدون گوشی. هر گیرِ composition، حلقهٔ بی‌نهایت
 * یا بلاکِ تردهای اصلی باید اینجا به‌صورت تایم‌اوت/کرش ظاهر شود.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MainActivityLaunchTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private fun awaitLoginScreen() {
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithText("ورود به حساب").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** باز شدن اپ → صفحهٔ ورود، بدون گیر کردن. */
    @Test
    fun launch_reachesLoginScreen_withoutFreezing() {
        awaitLoginScreen()
        composeRule.onNodeWithText("نام کاربری").assertExists()
        composeRule.onNodeWithText("رمز عبور").assertExists()
    }

    /** فیلدها ورودی را می‌پذیرند و دکمهٔ ورود فعال می‌شود — یعنی UI زنده است. */
    @Test
    fun loginFields_acceptInput_andButtonEnables() {
        awaitLoginScreen()
        val fields = composeRule.onAllNodes(hasSetTextAction())
        assertTrue("دو فیلد ورود پیدا نشد", fields.fetchSemanticsNodes().size >= 2)
        fields[0].performTextInput("testuser")
        fields[1].performTextInput("testpass")
        composeRule.onNodeWithText("ورود").assertIsEnabled()
    }

    /** بازسازی اکتیویتی (مثل چرخش) → دوباره صفحهٔ ورود، بدون هنگ. */
    @Test
    fun activityRecreate_showsLoginAgain() {
        awaitLoginScreen()
        composeRule.activityRule.scenario.recreate()
        awaitLoginScreen()
    }

    /** نشست ذخیره‌شده → خانهٔ موقت (مسیر Active) بدون کرش/فریز. */
    @Test
    fun storedSession_showsHomeScreen() {
        awaitLoginScreen()
        composeRule.activityRule.scenario.onActivity { activity ->
            runBlocking {
                SessionRepository(activity.applicationContext)
                    .save(StoredUser(id = "u1", username = "testuser"))
            }
        }
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithText("خروج از حساب").fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** عیب‌یابی بدون گزارش فریز/کرش: باید بی‌صدا و بی‌کرش no-op باشد. */
    @Test
    fun onAppForeground_isSafe_withoutReport() {
        val ctx = androidx.test.core.app.ApplicationProvider
            .getApplicationContext<android.content.Context>()
        FreezeDiagnostics.onAppForeground(ctx)
    }
}
