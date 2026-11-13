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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shemim.app.BuildConfig
import com.shemim.app.R
import com.shemim.core.common.date.JalaliCalendar
import com.shemim.core.common.date.PersianDateText
import com.shemim.core.common.text.CurrencyUnit
import com.shemim.core.common.text.MoneyFormat
import com.shemim.core.common.text.NumberToWordsFa
import com.shemim.core.common.text.PersianDigits
import com.shemim.core.datastore.ThemePrefs
import com.shemim.core.designsystem.component.AmountText
import com.shemim.core.designsystem.component.AmountWithWordsText
import com.shemim.core.designsystem.component.EmptyState
import com.shemim.core.designsystem.component.MemberAvatar
import com.shemim.core.designsystem.component.OfflineBanner
import com.shemim.core.designsystem.component.SectionHeader
import com.shemim.core.designsystem.component.ShemimCard
import com.shemim.core.designsystem.component.StatusChip
import com.shemim.core.designsystem.component.StatusChipStyle
import com.shemim.core.designsystem.component.ThemeSwatch
import com.shemim.core.designsystem.theme.DarkModePreference
import com.shemim.core.designsystem.theme.ShemimThemeKey

/**
 * صفحهٔ نمایش WP-0.2 + WP-0.6 — دموی زندهٔ سیستم طراحی (۱۰ تم، دارک‌مود،
 * کامپوننت‌های پایه) و ابزارهای فارسی (تقویم شمسی، مبالغ، عدد به حروف).
 * در WP-0.7 با اسکلت ناوبری واقعی جایگزین می‌شود.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeShowcaseScreen(
    prefs: ThemePrefs,
    onThemeChange: (ShemimThemeKey) -> Unit,
    onDarkModeChange: (DarkModePreference) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tomanUnit by remember { mutableStateOf(true) }
    val unit = if (tomanUnit) CurrencyUnit.TOMAN else CurrencyUnit.RIAL
    val today = remember { JalaliCalendar.todayJalali() }
    val sampleAmount = 12_345_678L // ریال

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = {
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Text(
                        text = stringResource(R.string.showcase_subtitle),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            })
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { Spacer(Modifier.height(padding.calculateTopPadding())) }
            item { OfflineBanner(visible = true) }

            // ---------- ۱) تم‌ها ----------
            item {
                SectionHeader(
                    title = stringResource(R.string.showcase_themes_title),
                    index = 1,
                )
                Text(
                    text = stringResource(R.string.showcase_themes_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val rows = ShemimThemeKey.entries.chunked(2)
            items(rows.size) { rowIndex ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows[rowIndex].forEach { key ->
                        ThemeSwatch(
                            themeKey = key,
                            selected = prefs.themeKey == key,
                            onClick = { onThemeChange(key) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rows[rowIndex].size == 1) Spacer(Modifier.weight(1f))
                }
            }

            // ---------- ۲) حالت نمایش ----------
            item {
                SectionHeader(
                    title = stringResource(R.string.showcase_display_title),
                    index = 2,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    DarkModePreference.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = prefs.darkMode == mode,
                            onClick = { onDarkModeChange(mode) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = DarkModePreference.entries.size,
                            ),
                        ) {
                            Text(
                                text = when (mode) {
                                    DarkModePreference.SYSTEM -> stringResource(R.string.dark_mode_system)
                                    DarkModePreference.LIGHT -> stringResource(R.string.dark_mode_light)
                                    DarkModePreference.DARK -> stringResource(R.string.dark_mode_dark)
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.dynamic_color_title),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(R.string.dynamic_color_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = prefs.dynamicColor, onCheckedChange = onDynamicColorChange)
                }
            }

            // ---------- ۳) کامپوننت‌های پایه ----------
            item {
                SectionHeader(
                    title = stringResource(R.string.showcase_components_title),
                    index = 3,
                )
                ShemimCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.sample_card_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(R.string.sample_card_balance),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        AmountText(
                            amountRial = sampleAmount,
                            unit = unit,
                            style = MaterialTheme.typography.displaySmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusChip(stringResource(R.string.status_active), style = StatusChipStyle.SUCCESS)
                            StatusChip(stringResource(R.string.status_overdue), style = StatusChipStyle.ERROR)
                            StatusChip(stringResource(R.string.status_pending), style = StatusChipStyle.WARNING)
                            StatusChip(stringResource(R.string.status_rejected), style = StatusChipStyle.NEUTRAL)
                        }
                    }
                }
            }
            item {
                // مبالغ جهت‌دار + اعداد ستونی (tnum)
                ShemimCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.sample_amounts_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                stringResource(R.string.amount_deposit),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            AmountText(
                                amountRial = 1_850_000,
                                unit = unit,
                                showSign = true,
                                direction = com.shemim.core.designsystem.component.AmountDirection.POSITIVE,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                stringResource(R.string.amount_withdraw),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            AmountText(
                                amountRial = 750_000,
                                unit = unit,
                                showSign = true,
                                direction = com.shemim.core.designsystem.component.AmountDirection.NEGATIVE,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        AmountWithWordsText(amountRial = sampleAmount, unit = unit)
                    }
                }
            }
            item {
                ShemimCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.sample_members_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MemberAvatar(displayName = "زهرا", memberId = 7)
                            MemberAvatar(displayName = "محمد", memberId = 2)
                            MemberAvatar(displayName = "فاطمه", memberId = 11)
                            MemberAvatar(displayName = "علی", memberId = 4)
                        }
                        Text(
                            text = stringResource(R.string.sample_members_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                EmptyState(
                    title = stringResource(R.string.sample_empty_title),
                    message = stringResource(R.string.sample_empty_message),
                )
            }

            // ---------- ۴) تقویم شمسی ----------
            item {
                SectionHeader(
                    title = stringResource(R.string.showcase_calendar_title),
                    index = 4,
                )
                ShemimCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoRow(
                            label = stringResource(R.string.calendar_today_full),
                            value = PersianDateText.formatJalaliFullFa(today),
                        )
                        InfoRow(
                            label = stringResource(R.string.calendar_today_short),
                            value = today.formatFa(),
                        )
                        InfoRow(
                            label = stringResource(R.string.calendar_month_days),
                            value = stringResource(
                                R.string.calendar_month_days_value,
                                today.monthName,
                                PersianDigits.toPersian(
                                    JalaliCalendar.monthDaysJalali(today.year, today.month).toString(),
                                ),
                            ),
                        )
                        InfoRow(
                            label = stringResource(R.string.calendar_year_type),
                            value = if (JalaliCalendar.isLeapJalali(today.year)) {
                                stringResource(R.string.calendar_year_leap)
                            } else {
                                stringResource(R.string.calendar_year_normal)
                            },
                        )
                    }
                }
            }

            // ---------- ۵) مبالغ و واحد ----------
            item {
                SectionHeader(
                    title = stringResource(R.string.showcase_money_title),
                    index = 5,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.money_unit_toggle),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = tomanUnit, onCheckedChange = { tomanUnit = it })
                }
                ShemimCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoRow(
                            label = stringResource(R.string.money_amount_rial),
                            value = MoneyFormat.formatFa(sampleAmount, CurrencyUnit.RIAL, showUnit = true),
                        )
                        InfoRow(
                            label = stringResource(R.string.money_amount_display),
                            value = MoneyFormat.formatFa(sampleAmount, unit, showUnit = true),
                        )
                        InfoRow(
                            label = stringResource(R.string.money_input_parse),
                            value = stringResource(
                                R.string.money_input_parse_value,
                                PersianDigits.toPersian("1,234"),
                                MoneyFormat.formatFa(
                                    MoneyFormat.parseInputAmount("1,234", unit),
                                    CurrencyUnit.RIAL,
                                    showUnit = true,
                                ),
                            ),
                        )
                        Text(
                            text = stringResource(
                                R.string.money_words,
                                NumberToWordsFa.toWords(
                                    MoneyFormat.displayAmount(sampleAmount, unit),
                                ),
                                unit.faTitle,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.fund_full_name),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.showcase_footer),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.skeleton_version,
                                PersianDigits.toPersian(BuildConfig.VERSION_NAME),
                                PersianDigits.toPersian(BuildConfig.VERSION_CODE.toString()),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
