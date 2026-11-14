// core:network — کلاینت API: OkHttp + CookieJar پایدار + مدیریت auth_required — WP-0.3
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.shemim.core.network"
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

dependencies {
    // OkHttp خالص به‌جای Retrofit — ADR-017 (قرارداد تک‌endpoint اکشن‌محور سایت)
    api(libs.okhttp.core)
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
}
