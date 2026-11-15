package com.shemim.core.model

import kotlinx.serialization.Serializable

/**
 * تنظیمات صندوق (ردیف `settings` در app_settings) — همان شیءای که صفحهٔ
 * تنظیمات وب می‌سازد. کلیدها با مقدار nullable مدل شده‌اند چون صندوق‌ها
 * نسخه‌های متفاوتی از این شیء دارند و مقدار غایب یعنی «پیش‌فرض سایت».
 *
 * منبع کلیدها: defaults در data-helpers.php + مصرف‌کنندگان js
 * (income-requests.js، members.js، loan-queue-profit.js، rotating-fund.js).
 */
@Serializable
data class SettingsDto(
    /** ضریب وام — می‌تواند کسری باشد (ضریب زندهٔ WP-2.5) */
    val loanMultiplier: Double? = null,
    /** auto | manual */
    val loanMultiplierMode: String? = null,
    /** درصد ذخیرهٔ وام 0..100 */
    val loanReservePercent: Int? = null,
    /** مقسوم ماهانهٔ وام */
    val loanMonthlyDivisor: Int? = null,
    /** ماه‌های دورهٔ انتظار بین دو وام */
    val loanCooldownMonths: Int? = null,
    /** ضریب دورهٔ انتظار در دیرکرد اقساط */
    val loanDelayCooldownFactor: Double? = null,
    /** ضریب سقف نفرات هم‌زمان وام */
    val loanPersonCapFactor: Double? = null,
    /** جریمهٔ دیرکرد در روز به ازای هر میلیون */
    val latePenaltyPerDayPerMillion: Double? = null,
    /** مبلغ جایزهٔ تولد */
    val birthdayGiftAmount: Long? = null,
    /** rial | toman */
    val currency: String? = null,
    val displayMode: String? = null,
    /** داده‌ی data-url لوگو (اختیاری) */
    val fundLogo: String? = null,
    /** کلید تم — کلید جاری سایت */
    val theme: String? = null,
    /** کلید قدیمی تم در defaults سرور (سازگاری) */
    val colorTheme: String? = null,
)
