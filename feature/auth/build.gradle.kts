// feature:auth — ورود/خروج — WP-0.3 (ثبت‌نام/بازیابی/OTP بله در فازهای بعدی طبق سند ۰۰)
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.shemim.feature.auth"
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

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:datastore"))
    implementation(project(":core:network"))

    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // آیکون‌های Visibility/Person/Lock (مجموعهٔ گسترده) — مانند designsystem
    implementation(libs.androidx.compose.material.icons)
}
