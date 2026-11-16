package com.shemim.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shemim.app.R
import com.shemim.core.common.text.PersianDigits
import com.shemim.core.datastore.StoredUser
import com.shemim.core.datastore.ThemePrefs
import com.shemim.core.db.entity.FundSettingsEntity
import com.shemim.core.designsystem.component.SectionHeader
import com.shemim.core.designsystem.component.ShemimCard
import com.shemim.core.designsystem.component.StatusChip
import com.shemim.core.designsystem.theme.DarkModePreference
import com.shemim.core.designsystem.theme.ShemimThemeKey
import com.shemim.core.sync.SyncUiState

/**
 * اسکلت ناوبری نقش‌محور — WP-0.7.
 *
 * نوار پایین چهار بخش پرکاربردِ نقش کاربر + «بیشتر» را نشان می‌دهد
 * (گزینش از منوی خودِ نقش در `AppNav` — پورت menusByRole وب). بقیهٔ
 * بخش‌ها از صفحهٔ «بیشتر» به‌صورت placeholder قابل پیمایش‌اند و هر یک
 * می‌گوید در کدام فاز فعال می‌شود — «پیمایش بین بخش‌ها با placeholder»
 * (خروجی قابل‌تست WP-0.7).
 *
 * بازگشت (Back): از overlay تم به «بیشتر»، از placeholder به پیشخوان؛
 * روی خود پیشخوان رفتار پیش‌فرض سیستم (خروج از اپ).
 */
@Composable
fun AppShellScreen(
    prefs: ThemePrefs,
    onThemeChange: (ShemimThemeKey) -> Unit,
    onDarkModeChange: (DarkModePreference) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    user: StoredUser,
    syncState: SyncUiState,
    settings: FundSettingsEntity?,
    memberCount: Int,
    transactionCount: Int,
    loanCount: Int,
    onSyncNow: () -> Unit,
    onLogout: () -> Unit,
    versionLabel: String,
) {
    val tabs = remember(user.role) { AppNav.bottomTabs(user.role) }
    val homePage = tabs.first().page
    var current by rememberSaveable { mutableStateOf(homePage) }
    var showThemes by rememberSaveable { mutableStateOf(false) }

    if (showThemes) {
        BackHandler { showThemes = false }
    } else {
        BackHandler(enabled = current != homePage) { current = homePage }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = !showThemes && current == tab.page,
                        onClick = {
                            showThemes = false
                            current = tab.page
                        },
                        icon = { Text(text = tab.emoji, fontSize = 20.sp) },
                        label = { Text(text = tab.title, maxLines = 1) },
                    )
                }
                NavigationBarItem(
                    selected = !showThemes && current == "more",
                    onClick = {
                        showThemes = false
                        current = "more"
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.MoreHoriz,
                            contentDescription = null,
                        )
                    },
                    label = { Text(text = stringResource(R.string.nav_more), maxLines = 1) },
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                showThemes -> ThemeShowcaseScreen(
                    prefs = prefs,
                    onThemeChange = onThemeChange,
                    onDarkModeChange = onDarkModeChange,
                    onDynamicColorChange = onDynamicColorChange,
                )

                current == "more" -> MoreScreen(
                    role = user.role,
                    onOpenSection = { page ->
                        showThemes = false
                        current = page
                    },
                    onOpenThemes = { showThemes = true },
                    onLogout = onLogout,
                    versionLabel = versionLabel,
                )

                AppNav.isDashboardPage(current) -> DashboardScreen(
                    userName = user.firstName ?: user.username,
                    fundName = settings?.fundName.orEmpty(),
                    syncState = syncState,
                    settings = settings,
                    memberCount = memberCount,
                    transactionCount = transactionCount,
                    loanCount = loanCount,
                    onSyncNow = onSyncNow,
                )

                else -> SectionPlaceholderScreen(
                    section = remember(user.role, current) {
                        findSection(user.role, current) ?: AppSection(
                            page = current,
                            title = current,
                            desc = "",
                            emoji = "📄",
                            phase = 1,
                        )
                    },
                )
            }
        }
    }
}

/** پیدا کردن آیتم منوی معادل از منوی همان نقش (اصلی + کناری). */
private fun findSection(role: String, page: String): AppSection? =
    AppNav.mainSections(role).firstOrNull { it.page == page }
        ?: AppNav.sideSections(role).firstOrNull { it.page == page }

/**
 * صفحهٔ «بیشتر» — تمام کاشی‌های منوی نقش (اصلی و کناری، همان وب) به‌صورت
 * placeholder + دسترسی به تم‌ها + خروج از حساب + برچسب نسخه.
 */
@Composable
private fun MoreScreen(
    role: String,
    onOpenSection: (String) -> Unit,
    onOpenThemes: () -> Unit,
    onLogout: () -> Unit,
    versionLabel: String,
) {
    var confirmLogout by remember { mutableStateOf(false) }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text(stringResource(R.string.home_logout)) },
            text = { Text(stringResource(R.string.home_logout_confirm)) },
            confirmButton = {
                TextButton(onClick = onLogout) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SectionHeader(title = stringResource(R.string.nav_more_sections))
        AppNav.mainSections(role).forEach { section ->
            SectionRow(section = section, onClick = { onOpenSection(section.page) })
        }

        SectionHeader(title = stringResource(R.string.nav_more_side))
        AppNav.sideSections(role).forEach { section ->
            SectionRow(section = section, onClick = { onOpenSection(section.page) })
        }

        SectionHeader(title = stringResource(R.string.nav_more_display))
        ShemimCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenThemes),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = "🎨", fontSize = 22.sp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.nav_more_themes),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = stringResource(R.string.showcase_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.size(8.dp))
        Button(
            onClick = { confirmLogout = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            ),
        ) {
            Text(stringResource(R.string.nav_logout))
        }
        Text(
            text = versionLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 8.dp),
            textAlign = TextAlign.Center,
        )
    }
}

/** یک ردیف منو: ایموجی وب + عنوان + توضیح + برچسب فاز. */
@Composable
private fun SectionRow(section: AppSection, onClick: () -> Unit) {
    ShemimCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = section.emoji, fontSize = 22.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                if (section.desc.isNotBlank()) {
                    Text(
                        text = section.desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            StatusChip(
                text = stringResource(
                    R.string.nav_section_phase,
                    PersianDigits.toPersian(section.phase.toString()),
                ),
            )
        }
    }
}

/** placeholder یک بخش — تا فاز خودش ساخته شود. */
@Composable
private fun SectionPlaceholderScreen(section: AppSection) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = section.emoji, fontSize = 56.sp)
        Spacer(Modifier.size(16.dp))
        Text(
            text = section.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        if (section.desc.isNotBlank()) {
            Spacer(Modifier.size(4.dp))
            Text(
                text = section.desc,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.size(16.dp))
        StatusChip(
            text = stringResource(
                R.string.nav_section_phase,
                PersianDigits.toPersian(section.phase.toString()),
            ),
        )
    }
}
