// feature:loans — وام‌ها + اقساط + ماشین‌حساب — فاز ۱-۲
// اسکلت خالی؛ در WP مربوطه (سند ۰۰) پر می‌شود.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.shemim.feature.loans"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
