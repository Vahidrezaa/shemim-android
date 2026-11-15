package com.shemim.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shemim.core.designsystem.theme.DarkModePreference
import com.shemim.core.designsystem.theme.ShemimTheme
import com.shemim.core.designsystem.theme.ShemimThemeKey
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * عیب‌یابی پیکسلی صفحهٔ ورود — ۱۴۰۵/۰۸/۲۵.
 *
 * زمینه: شواهد گوشی و امولاتور نشان داد OutlinedTextField/Button آمادهٔ
 * M3 در حالت عادی اصلاً رسم نمی‌شوند (چیدمان سالم — لمس کار می‌کند) و
 * فقط با تغییر وضعیت (لودینگ) لحظه‌ای پیدا می‌شوند. تست‌های قبلی فقط
 * semantics را می‌دیدند؛ این کلاس خودِ پیکسل‌های رسم‌شده را می‌شمارد:
 *
 *  ۱) realApp_* — مسیر کامل اپ (اسپلش + پوشش MainActivity + تم):
 *     صفحهٔ بازسازی‌شده باید «رنگ primary دکمه» و «حاشیهٔ فیلدها» را
 *     واقعاً روی پیکسل‌ها بکشد — گارد رگرسیون فیکس.
 *  ۲) matrix_* — ماتریس ایزوله‌سازی کامپوننت‌های M3 (بدون assert؛
 *     فقط گزارش عددی در خروجی CI برای ریشه‌یابی بعدی).
 *
 * تشخیص رنگ‌ها مستقل از فونت است (شکل/حاشیه می‌شمریم، نه متن) تا
 * خطای احتمالی رندر فونت در Robolectric نتیجه را خراب نکند.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-420dpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LoginRenderDiagTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    // پالت زمرد روشن (پیش‌فرض اپ در Robolectric: سیستم روشن، بدون نشست)
    private val bgArgb = 0xFFF5FBF6.toInt()
    private val primaryArgb = 0xFF1A6B52.toInt()
    private val outlineArgb = 0xFF707974.toInt()

    private fun awaitLogin() {
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithText("ورود به حساب").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun dist(a: Int, b: Int): Int {
        val dr = ((a ushr 16) and 0xFF) - ((b ushr 16) and 0xFF)
        val dg = ((a ushr 8) and 0xFF) - ((b ushr 8) and 0xFF)
        val db = (a and 0xFF) - (b and 0xFF)
        return abs(dr) + abs(dg) + abs(db)
    }

    /** (پیکسلِ primary، پیکسلِ outline، پیکسلِ غیرپس‌زمینه) روی کل تصویر. */
    private fun countPixels(bmp: ImageBitmap): Triple<Int, Int, Int> {
        var primary = 0
        var outline = 0
        var nonBg = 0
        for (p in bmp.toPixelMap().pixels) {
            if (dist(p, bgArgb) > 40) nonBg++
            if (dist(p, primaryArgb) < 60) primary++
            if (dist(p, outlineArgb) < 60) outline++
        }
        return Triple(primary, outline, nonBg)
    }

    private fun report(tag: String): String {
        val bmp = composeRule.onRoot().captureToImage()
        val (primary, outline, nonBg) = countPixels(bmp)
        val total = bmp.width * bmp.height
        return "DIAG $tag: primary=$primary outline=$outline nonBg=$nonBg " +
            "total=$total size=${bmp.width}x${bmp.height}"
    }

    // ---------- ۱) مسیر واقعی اپ (assert) ----------

    /** صفحهٔ بازسازی‌شده روی مسیر کامل اپ: دکمه و حاشیهٔ فیلدها واقعاً رنگ می‌شوند. */
    @Test
    fun realApp_rebuiltLogin_paintsButtonAndFieldBorders() {
        awaitLogin()
        composeRule.waitForIdle()
        val line = report("realApp_idle")
        println(line)
        val bmp = composeRule.onRoot().captureToImage()
        val total = bmp.width * bmp.height
        val (primary, outline, _) = countPixels(bmp)
        assertTrue(
            "رنگ primary دکمهٔ ورود روی پیکسل‌ها دیده نشد — $line",
            primary > total / 2000,
        )
        assertTrue(
            "حاشیهٔ (outline) فیلدها روی پیکسل‌ها دیده نشد — $line",
            outline > total / 5000,
        )
    }

    // ---------- ۲) ماتریس ایزوله‌سازی M3 (فقط گزارش؛ هرگز سبز/قرمز را عوض نمی‌کند) ----------

    private fun setMatrixContent(content: @Composable () -> Unit) {
        composeRule.setContent {
            ShemimTheme(
                themeKey = ShemimThemeKey.EMERALD,
                darkMode = DarkModePreference.LIGHT,
            ) { content() }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun matrix_m3_plain_enabled() {
        setMatrixContent { M3Form(enabled = true) }
        println(report("matrix_m3_plain_enabled"))
    }

    @Test
    fun matrix_m3_plain_disabled() {
        setMatrixContent { M3Form(enabled = false) }
        println(report("matrix_m3_plain_disabled"))
    }

    @Test
    fun matrix_m3_scroll_insets_enabled() {
        setMatrixContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp),
            ) {
                M3FieldsAndButton(enabled = true)
            }
        }
        println(report("matrix_m3_scroll_insets_enabled"))
    }

    @Test
    fun matrix_tap_clickable_vs_pointerInput() {
        setMatrixContent {
            Column(Modifier.fillMaxSize().padding(24.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ShemimGreen)
                        .clickable { },
                )
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ShemimGreen)
                        .pointerInput(Unit) { detectTapGestures { } },
                )
            }
        }
        println(report("matrix_tap_clickable_vs_pointerInput"))
    }

    @Composable
    private fun M3Form(enabled: Boolean) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            M3FieldsAndButton(enabled = enabled)
        }
    }

    @Composable
    private fun M3FieldsAndButton(enabled: Boolean) {
        OutlinedTextField(
            value = "",
            onValueChange = { },
            label = { Text("نام کاربری") },
            enabled = enabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = "",
            onValueChange = { },
            label = { Text("رمز عبور") },
            enabled = enabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { },
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
        ) {
            Text("ورود")
        }
    }
}

/** همان سبز primary پالت زمرد برای جعبه‌های تشخیصی لمس. */
private val ShemimGreen = androidx.compose.ui.graphics.Color(0xFF1A6B52)
