// اسکلت ریشه — نسخه‌های واقعی در gradle/libs.versions.toml قفل شده‌اند (سند ۰۶)
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    // WP-0.4 — پردازشگر حاشیه‌نویسی Room
    alias(libs.plugins.ksp) apply false
}
