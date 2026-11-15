package com.shemim.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * صفحهٔ ورود — WP-0.3.
 *
 * همان فرم وب (نام کاربری + رمز عبور) با زبان طراحی اپ؛
 * پیام‌های خطا همان متن‌های سرور/وب‌اند.
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
                .verticalScroll(rememberScrollState())
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
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    if (state.error != null) onConsumeError()
                },
                label = { Text(stringResource(R.string.auth_username_label)) },
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                singleLine = true,
                enabled = state.canSubmit,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (state.error != null) onConsumeError()
                },
                label = { Text(stringResource(R.string.auth_password_label)) },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(
                                if (showPassword) R.string.auth_hide_password else R.string.auth_show_password,
                            ),
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                enabled = state.canSubmit,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth(),
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
            // برخلاف گذشته، دکمه همیشه (تا پیش از ارسال در جریان) فعال است —
            // همان رفتار وب: لمس با فرم خالی، خطای «الزامی» را نشان می‌دهد تا
            // صفحه همیشه به لمس واکنش دیداری بدهد (گزارش دستگاهی ۱۴۰۵/۰۸/۲۳:
            // دکمهٔ غیرفعال هیچ بازخوردی نمی‌داد و «فریز» حس می‌شد).
            Button(
                onClick = { submit() },
                enabled = state.canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(stringResource(R.string.auth_submitting))
                } else {
                    Text(
                        text = stringResource(R.string.auth_submit),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
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
