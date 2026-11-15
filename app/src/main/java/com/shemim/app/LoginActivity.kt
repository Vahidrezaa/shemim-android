package com.shemim.app

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.shemim.core.datastore.SessionRepository
import com.shemim.core.datastore.SessionState
import com.shemim.feature.auth.LoginError
import com.shemim.feature.auth.LoginUiState
import com.shemim.feature.auth.LoginViewModel
// با nonTransitiveRClass=true، رشته‌های auth_* در R ماژول feature:auth هستند
import com.shemim.feature.auth.R as AuthR
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * صفحهٔ ورود — بازنویسی اساسی (WP-0.3) با ویجت‌های کلاسیک فریم‌ورک، نه Compose.
 *
 * تصمیم معماری: دو روز عیب‌یابی روی گوشی واقعی نشان داد در این دستگاه محتوای
 * Compose پس از اولین فریم پایدار رنگ نمی‌گیرد (چیدمان و لمس سالم است ولی
 * فیلدها/دکمه نامرئی‌اند و فقط هنگام recomposition سنگین لحظه‌ای پیدا می‌شوند)،
 * در حالی که اسپلش، کیبورد و رابط سیستمی همان گوشی با پایپ‌لاین View بی‌نقص
 * رندر می‌شوند. صفحهٔ ورود — اولین چیزی که کاربر می‌بیند — به View محور
 * منتقل شد (activity_login.xml + EditText/Button/TextView) با رنگ‌های
 * literal و بدون هیچ وابستگی به تم/ColorScheme؛ منطق ورود (LoginViewModel)
 * و جریان نشست (SessionRepository/DataStore) دست‌نخورده ماند.
 *
 * جریان: اپ با این اکتیویتی باز می‌شود (LAUNCHER). اسپلش تا وقتی وضعیت نشست
 * از DataStore مشخص نشده روی صفحه می‌ماند؛ اگر نشست ذخیره‌شده بود، مستقیم
 * MainActivity (خانه) می‌رود؛ وگرنه فرم ورود نمایش داده می‌شود. بعد از ورود
 * موفق، SessionRepository «Active» می‌شود و همینجا به خانه دست‌به‌دست می‌شود.
 */
class LoginActivity : ComponentActivity() {

    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var sessions: SessionRepository
    private lateinit var viewModel: LoginViewModel

    private lateinit var usernameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var togglePassword: TextView
    private lateinit var errorText: TextView
    private lateinit var loginButton: Button
    private lateinit var progress: ProgressBar

    /** آخرین وضعیت نشست — برای شرط نگه‌داشتن اسپلش (session یک Flow است نه StateFlow). */
    private var latestSession: SessionState = SessionState.Loading

    private var showPassword = false

    override fun onCreate(savedInstanceState: Bundle?) {
        // وابستگی‌ها قبل از اسپلش آماده می‌شوند (application قبل از اکتیویتی ساخته می‌شود)
        val graph = (application as ShemimApplication).graph
        sessions = graph.sessionRepo
        viewModel = LoginViewModel(graph.api, sessions)

        // اسپلشِ مدیریت‌شدهٔ رسمی — الزاماً قبل از super.onCreate (قرارداد کتابخانه).
        val splash = installSplashScreen()
        // تا وقتی وضعیت نشست از DataStore مشخص نشده، اسپلش بماند
        // (باز شدن با نشست ذخیره‌شده → بدون فلش صفحهٔ ورود، مستقیم خانه).
        splash.setKeepOnScreenCondition { latestSession is SessionState.Loading }
        // خروج بی‌انیمیشن اسپلش — همان گارد اثبات‌شدهٔ MIUI (پنجرهٔ رهاشدهٔ
        // با آلفای ~۰.۱): هیچ انیمیشن خروجی برای گیرکردن باقی نمی‌ماند.
        splash.setOnExitAnimationListener { provider ->
            FreezeDiagnostics.milestone("splash-view-removed")
            provider.remove()
        }

        super.onCreate(savedInstanceState)
        FreezeDiagnostics.milestone("login-onCreate")
        setContentView(R.layout.activity_login)

        usernameInput = findViewById(R.id.edit_username)
        passwordInput = findViewById(R.id.edit_password)
        togglePassword = findViewById(R.id.toggle_password)
        errorText = findViewById(R.id.text_error)
        loginButton = findViewById(R.id.button_login)
        progress = findViewById(R.id.progress_login)

        findViewById<TextView>(R.id.text_version).text = getString(
            R.string.skeleton_version,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE.toString(),
        )

        togglePassword.setOnClickListener { setShowPassword(!showPassword) }
        loginButton.setOnClickListener {
            viewModel.login(
                usernameInput.text.toString(),
                passwordInput.text.toString(),
            )
        }
        // «Done» کیبورد روی فیلد رمز = ارسال فرم
        passwordInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                loginButton.performClick()
                true
            } else {
                false
            }
        }

        // نشست → خانه. منبع حقیقت SessionRepository است؛ بعد از ورود موفق
        // (save در LoginViewModel) یا نشست ذخیره‌شده، همینجا دست‌به‌دست می‌شود.
        uiScope.launch {
            sessions.session.collect { state ->
                latestSession = state
                if (state is SessionState.Active) goHome()
            }
        }
        // وضعیت فرم → UI (متن دکمه، پروگرس، خطا، فعال/غیرفعال فیلدها)
        uiScope.launch {
            viewModel.state.collect { render(it) }
        }
    }

    private fun goHome() {
        FreezeDiagnostics.milestone("login-goes-home")
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun render(state: LoginUiState) {
        progress.visibility = if (state.loading) View.VISIBLE else View.GONE
        loginButton.isEnabled = state.canSubmit
        loginButton.text = getString(
            if (state.loading) AuthR.string.auth_submitting else AuthR.string.auth_submit,
        )
        usernameInput.isEnabled = state.canSubmit
        passwordInput.isEnabled = state.canSubmit
        val message = when (val error = state.error) {
            null -> null
            is LoginError.Server -> error.message.ifBlank { getString(AuthR.string.auth_error_generic) }
            LoginError.Required -> getString(AuthR.string.auth_error_required)
            LoginError.Offline -> getString(AuthR.string.auth_error_offline)
            LoginError.Network -> getString(AuthR.string.auth_error_network)
            LoginError.Malformed -> getString(AuthR.string.auth_error_malformed)
            LoginError.AuthRequired -> getString(AuthR.string.auth_error_auth_required)
        }
        if (message == null) {
            errorText.visibility = View.GONE
        } else {
            errorText.text = message
            errorText.visibility = View.VISIBLE
        }
    }

    private fun setShowPassword(show: Boolean) {
        showPassword = show
        if (show) {
            passwordInput.inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            togglePassword.setText(AuthR.string.auth_hide_password)
        } else {
            passwordInput.inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_VARIATION_PASSWORD
            togglePassword.setText(AuthR.string.auth_show_password)
        }
        passwordInput.setSelection(passwordInput.text.length)
    }

    /** edge-to-edge فقط برای اکتیویتی کامپوز (خانه)؛ صفحهٔ ورود View-محور
     *  زیر نوارهای سیستم رسم می‌شود و insets سیستمی را فریم‌ورک مدیریت می‌کند. */
    override fun onDestroy() {
        uiScope.cancel()
        super.onDestroy()
    }
}
