package com.shemim.app

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shemim.feature.auth.R as AuthR
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * تست صفحهٔ ورود بازنویسی‌شده — چون صفحه View-محور است، این تست هم
 * بدون Compose و فقط با ActivityScenario + findViewById کار می‌کند:
 * کم‌وابسته‌ترین و مطمئن‌ترین شکل تست روی Robolectric.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class LoginActivityTest {

    /** فرم کامل باز می‌شود؛ فیلدها ورودی می‌پذیرند و دکمه فعال است. */
    @Test
    fun form_shows_withEditableFields_andEnabledButton() {
        ActivityScenario.launch(LoginActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val user = activity.findViewById<EditText>(R.id.edit_username)
                val pass = activity.findViewById<EditText>(R.id.edit_password)
                val button = activity.findViewById<Button>(R.id.button_login)
                assertNotNull("فیلد نام کاربری نیست", user)
                assertNotNull("فیلد رمز عبور نیست", pass)
                assertNotNull("دکمهٔ ورود نیست", button)
                assertEquals(View.VISIBLE, user.visibility)
                assertEquals(View.VISIBLE, pass.visibility)
                assertEquals(View.VISIBLE, button.visibility)
                assertTrue(button.isEnabled)

                user.setText("testuser")
                pass.setText("testpass")
                assertEquals("testuser", user.text.toString())
                assertEquals("testpass", pass.text.toString())
                assertTrue(button.isEnabled)
            }
        }
    }

    /** ارسال فرم خالی → خطای «الزامی» بدون هیچ تماس شبکه. */
    @Test
    fun emptySubmit_showsRequiredError() {
        ActivityScenario.launch(LoginActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<Button>(R.id.button_login).performClick()
            }
            // صف ترد اصلی را خالی کن تا state.collect و رندر خطا اجرا شود
            shadowOf(Looper.getMainLooper()).idle()
            scenario.onActivity { activity: Activity ->
                val error = activity.findViewById<TextView>(R.id.text_error)
                assertEquals(View.VISIBLE, error.visibility)
                assertEquals(
                    activity.getString(AuthR.string.auth_error_required),
                    error.text.toString(),
                )
            }
        }
    }

    /** تیک نمایش رمز — inputType بین مخفی/آشکار جابه‌جا می‌شود. */
    @Test
    fun passwordToggle_changesInputType() {
        ActivityScenario.launch(LoginActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val pass = activity.findViewById<EditText>(R.id.edit_password)
                val toggle = activity.findViewById<TextView>(R.id.toggle_password)
                val hidden = pass.inputType
                toggle.performClick()
                assertFalse("نمایش رمز تغییر نکرد", pass.inputType == hidden)
                assertEquals(
                    activity.getString(AuthR.string.auth_hide_password),
                    toggle.text.toString(),
                )
                toggle.performClick()
                assertEquals("بازگشت به مخفی نشد", hidden, pass.inputType)
            }
        }
    }

    /** بازسازی اکتیویتی (چرخش) → دوباره فرم، بدون کرش. */
    @Test
    fun recreate_showsFormAgain() {
        val scenario = ActivityScenario.launch(LoginActivity::class.java)
        scenario.recreate()
        scenario.onActivity { activity ->
            assertNotNull(activity.findViewById<EditText>(R.id.edit_username))
            assertNotNull(activity.findViewById<Button>(R.id.button_login))
        }
        scenario.close()
    }
}
