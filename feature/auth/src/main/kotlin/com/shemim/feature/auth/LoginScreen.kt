package com.shemim.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * صفحهٔ ورود — WP-0.3 (بازسازی ۱۴۰۵/۰۸/۲۵).
 *
 * همان فرم وب (نام کاربری + رمز عبور) با همان رفتار و متن‌های قبلی؛
 * اما کنترل‌ها این‌بار از پریمیتیوهای ساده ساخته شده‌اند:
 *  - فیلد = «متنِ برچسب + ظرفِ background/border + BasicTextField»
 *  - دکمه = «ظرفِ background + pointerInput + Text»
 *
 * دلیل: گزارش‌های دستگاهی و امولاتور (۱۴۰۵/۰۸/۲۴–۲۵) نشان داد
 * کامپوننت‌های آمادهٔ M3 (OutlinedTextField/Button) روی این پروژه در
 * حالت عادی اصلاً رسم نمی‌شوند — چیدمان درست است (لمسِ دکمه کار
 * می‌کند و موقع لودینگ همه‌چیز لحظه‌ای پیدا می‌شود) ولی رنگ‌شان
 * هرگز به صفحه نمی‌رسد. متن ساده و Surface ساده همیشه رسم شده‌اند؛
 * بنابراین فرم با همین پریمیتیوهای اثباتاً سالم بازسازی شد — بدون
 * clickable/ripple و بدون verticalScroll که در زنجیرهٔ مشکوک بودند.
 * رفتار (خطاها، آیکون‌ها، نمایش رمز، لودینگ) دقیقاً همان قبلی است.
 */
@Composable
fun LoginScreen(
    state: LoginUiState,
    onLogin: (username: String, password: String) -> Unit,
    onConsumeError: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }

    fun submit() {
        if (state.canSubmit) onLogin(username, password)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            // ---------- برندینگ ----------
            Text(
                text = stringResource(R.string.auth_brand_fund),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.auth_title),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Spacer(Modifier.height(32.dp))

            // ---------- فرم ----------
            ShemimField(
                value = username,
                onValueChange = {
                    username = it
                    if (state.error != null) onConsumeError()
                },
                label = stringResource(R.string.auth_username_label),
                leadingIcon = Icons.Filled.Person,
                enabled = state.canSubmit,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
            )

            Spacer(Modifier.height(12.dp))

            ShemimField(
                value = password,
                onValueChange = {
                    password = it
                    if (state.error != null) onConsumeError()
                },
                label = stringResource(R.string.auth_password_label),
                leadingIcon = Icons.Filled.Lock,
                enabled = state.canSubmit,
                password = true,
                showPassword = showPassword,
                onTogglePassword = { showPassword = !showPassword },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )

            // ---------- خطا (متن سرور یا خطای محلی) ----------
            val errorText = loginErrorText(state.error)
            if (errorText != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(24.dp))

            // ---------- دکمهٔ ورود ----------
            // همیشه فعال (تا پیش از ارسال در جریان) — لمس با فرم خالی خطای
            // «الزامی» نشان می‌دهد تا صفحه همیشه به لمس واکنش دیداری بدهد.
            // لمس با pointerInput/detectTapGestures گرفته می‌شود (نه clickable)
            // و ظرف با background ساده — پریمیتیوهایی که مطمئن رسم می‌شوند.
            val buttonShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(buttonShape)
                    .background(
                        if (state.loading) {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    .pointerInput(state.canSubmit) {
                        if (state.canSubmit) {
                            detectTapGestures { submit() }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (state.loading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.size(12.dp))
                        Text(
                            text = stringResource(R.string.auth_submitting),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.auth_submit),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ---------- راهنمای آفلاین ----------
            Row(
                modifier = Modifier.widthIn(max = 340.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.auth_offline_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * فیلد ورود ساخته‌شده از پریمیتیوهای ساده — ظاهر هم‌خانوادهٔ فیلد M3:
 * برچسب بالای ظرف، ظرف با پس‌زمینهٔ ملایم و حاشیهٔ گرد (متمایز هنگام
 * فوکوس)، آیکون پیشرو، متن ورودی با BasicTextField و آیکون دنباله
 * (تغییر نمایش رمز) با لمس pointerInput.
 */
@Composable
private fun ShemimField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    enabled: Boolean,
    password: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val borderColor =
        if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .border(1.dp, borderColor, shape)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(12.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                    fontWeight = MaterialTheme.typography.bodyLarge.fontWeight,
                    fontFamily = MaterialTheme.typography.bodyLarge.fontFamily,
                ),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = if (password && !showPassword) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                interactionSource = interaction,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 14.dp),
            )
            if (onTogglePassword != null) {
                Spacer(Modifier.size(8.dp))
                Icon(
                    imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = stringResource(
                        if (showPassword) R.string.auth_hide_password else R.string.auth_show_password,
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(shape)
                        .pointerInput(Unit) { detectTapGestures { onTogglePassword() } }
                        .padding(8.dp),
                )
            }
        }
    }
}

/** متن نمایشی خطا: پیام سرور مستقیم؛ خطاهای محلی از منابع. */
@Composable
private fun loginErrorText(error: LoginError?): String? = when (error) {
    null -> null
    is LoginError.Server -> error.message.ifBlank { stringResource(R.string.auth_error_generic) }
    LoginError.Required -> stringResource(R.string.auth_error_required)
    LoginError.Offline -> stringResource(R.string.auth_error_offline)
    LoginError.Network -> stringResource(R.string.auth_error_network)
    LoginError.Malformed -> stringResource(R.string.auth_error_malformed)
    LoginError.AuthRequired -> stringResource(R.string.auth_error_auth_required)
}
