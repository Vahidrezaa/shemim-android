package com.shemim.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shemim.app.BuildConfig
import com.shemim.app.R
import com.shemim.core.datastore.StoredUser
import com.shemim.core.designsystem.component.MemberAvatar
import com.shemim.core.designsystem.component.SectionHeader
import com.shemim.core.designsystem.component.ShemimCard
import com.shemim.core.designsystem.component.StatusChip
import com.shemim.core.designsystem.component.StatusChipStyle
import com.shemim.core.network.UserRoles

/**
 * خانهٔ موقت فاز ۰ — WP-0.3.
 *
 * بعد از ورود موفق نشان داده می‌شود تا «لاگین/خروج واقعی» روی بیلد قابل
 * تست باشد؛ در WP-0.7 با اسکلت ناوبری و BottomBar نقش‌محور جایگزین می‌شود.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePlaceholderScreen(
    user: StoredUser,
    onOpenThemes: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmLogout by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = stringResource(R.string.home_phase0_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ---------- کاربر واردشده ----------
            ShemimCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MemberAvatar(
                        displayName = user.displayName(),
                        memberId = user.id.hashCode().toLong(),
                        size = 52.dp,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user.displayName(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            text = "@${user.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(6.dp))
                        StatusChip(
                            text = roleLabel(user.role),
                            style = if (user.role == UserRoles.ADMIN) StatusChipStyle.INFO else StatusChipStyle.NEUTRAL,
                        )
                    }
                }
            }

            // ---------- وضعیت نشست ----------
            SectionHeader(title = stringResource(R.string.home_session_title), index = 1)
            ShemimCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_session_ok),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.home_server),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(
                            R.string.skeleton_version,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE.toString(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ---------- اقدامات فاز ۰ ----------
            SectionHeader(title = stringResource(R.string.home_actions_title), index = 2)
            Button(
                onClick = onOpenThemes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Icon(Icons.Filled.Palette, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.home_open_showcase))
            }
            OutlinedButton(
                onClick = { confirmLogout = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.home_logout))
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // تأیید خروج — همان confirm وب («خارج می‌شوید؟») با الگوی نیتیو
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.home_logout)) },
            text = { Text(stringResource(R.string.home_logout_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onLogout()
                }) {
                    Text(stringResource(R.string.home_logout_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) {
                    Text(stringResource(R.string.home_logout_no))
                }
            },
        )
    }
}

private fun StoredUser.displayName(): String {
    val name = listOfNotNull(firstName?.takeIf { it.isNotBlank() }, lastName?.takeIf { it.isNotBlank() })
        .joinToString(" ")
    return name.ifBlank { username }
}

/** برچسب فارسی نقش — همان اصطلاح‌های وب. */
private fun roleLabel(role: String): String = when (role) {
    UserRoles.ADMIN -> "مدیر کل"
    UserRoles.GROUP_LEADER -> "سرگروه"
    UserRoles.USER -> "عضو"
    UserRoles.RESIGNED -> "انصرافی"
    else -> role
}
