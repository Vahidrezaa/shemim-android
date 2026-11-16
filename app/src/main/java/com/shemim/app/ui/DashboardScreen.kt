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
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shemim.app.R
import com.shemim.core.common.date.JalaliCalendar
import com.shemim.core.common.date.PersianDateText
import com.shemim.core.common.text.MoneyFormat
import com.shemim.core.common.text.PersianDigits
import com.shemim.core.db.entity.FundSettingsEntity
import com.shemim.core.designsystem.component.AmountText
import com.shemim.core.designsystem.component.EmptyState
import com.shemim.core.designsystem.component.OfflineBanner
import com.shemim.core.designsystem.component.ShemimCard
import com.shemim.core.designsystem.component.StatusChip
import androidx.compose.ui.res.stringResource
import com.shemim.core.sync.SyncUiState
import java.util.Calendar

/**
 * پیشخوان فاز ۰ — WP-0.5 («پیشخوان خالی با آخرین به‌روزرسانی»).
 *
 * هدف این صفحه اثبات کل زنجیرهٔ داده است، نه پیاده‌سازی پیشخوان واقعی
 * (آن کار فاز ۱/۲ است): sync دستی/خودکار → Room → نمایش. بنابراین فقط
 * داده‌های سطح-صفر را نشان می‌دهد: نام صندوق، موجودی حساب‌ها (محاسبهٔ
 * سرور)، آمار کش و «آخرین به‌روزرسانی» به تاریخ شمسی و ارقام فارسی.
 *
 * UI از هیچ نتیجهٔ refresh مستقیم نمی‌خواند؛ همه‌چیز از Flowهای Room و
 * SyncUiState می‌آید (سند ۰۲ §۴: Single Source of Truth = Room).
 */
@Composable
fun DashboardScreen(
    userName: String,
    fundName: String,
    syncState: SyncUiState,
    settings: FundSettingsEntity?,
    memberCount: Int,
    transactionCount: Int,
    loanCount: Int,
    onSyncNow: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ---------- هدر ----------
        Text(
            text = stringResource(R.string.dash_welcome, userName.ifBlank { "کاربر" }),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = fundName.ifBlank { stringResource(R.string.fund_full_name) },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // ---------- بنر آفلاین (کش قدیمی + شبکه قطع) ----------
        OfflineBanner(visible = syncState.offline)

        // ---------- کارت همگام‌سازی ----------
        ShemimCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(R.string.dash_sync_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = syncState.lastSyncAtMillis?.let {
                        stringResource(R.string.dash_sync_last, formatSyncTimeFa(it))
                    } ?: stringResource(R.string.dash_sync_never),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = onSyncNow, enabled = !syncState.running) {
                        if (syncState.running) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.dash_sync_running))
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.dash_sync_now))
                        }
                    }
                    syncState.dataVersion?.let {
                        StatusChip(
                            text = stringResource(
                                R.string.dash_version,
                                PersianDigits.toPersian(it),
                            ),
                        )
                    }
                }
                syncState.error?.let { detail ->
                    Text(
                        text = stringResource(R.string.dash_sync_failed, detail),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        if (syncState.lastSyncAtMillis == null) {
            // هنوز هیچ syncای موفق نبوده — راهنمای صادقانه به‌جای دادهٔ ساختگی
            EmptyState(
                title = stringResource(R.string.dash_empty_title),
                message = stringResource(R.string.dash_empty_message),
                icon = Icons.Filled.Sync,
            )
        } else {
            // ---------- موجودی حساب‌ها (محاسبهٔ سرور — همان dashboard وب) ----------
            val unit = MoneyFormat.CurrencyUnit.fromKey(settings?.currency)
            ShemimCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.dash_balances_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    BalanceRow(
                        label = stringResource(R.string.dash_main_account),
                        amountRial = settings?.mainAccount,
                        unit = unit,
                    )
                    BalanceRow(
                        label = stringResource(R.string.dash_incexp_account),
                        amountRial = settings?.incomeExpenseAccount,
                        unit = unit,
                    )
                }
            }

            // ---------- آمار کش محلی (اثبات زنجیرهٔ Room) ----------
            ShemimCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.dash_cache_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatCell(
                            label = stringResource(R.string.dash_cache_members),
                            value = memberCount,
                            modifier = Modifier.weight(1f),
                        )
                        StatCell(
                            label = stringResource(R.string.dash_cache_transactions),
                            value = transactionCount,
                            modifier = Modifier.weight(1f),
                        )
                        StatCell(
                            label = stringResource(R.string.dash_cache_loans),
                            value = loanCount,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

/** یک ردیف «برچسب ← مبلغ» — مبالغ با ارقام فارسی و واحد نمایشی صندوق. */
@Composable
private fun BalanceRow(
    label: String,
    amountRial: Long?,
    unit: MoneyFormat.CurrencyUnit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (amountRial != null) {
            AmountText(amountRial = amountRial, unit = unit)
        } else {
            Text(
                text = "—",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** یک سلول آمار: عدد درشت فارسی + برچسب. */
@Composable
private fun StatCell(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = PersianDigits.toPersian(value.toString()),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** «شنبه ۲۵ آبان ۱۴۰۵، ساعت ۱۷:۰۸» — تاریخ شمسی + ساعت، همه با ارقام فارسی. */
private fun formatSyncTimeFa(atMillis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = atMillis }
    val jalali = JalaliCalendar.gregorianToJalali(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH),
    )
    val time = "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    return PersianDateText.formatJalaliFullFa(jalali) + "، " +
        "ساعت " + PersianDigits.toPersian(time)
}
