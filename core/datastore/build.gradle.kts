// تنظیمات کاربر (DataStore Preferences) — WP-0.2: انتخاب تم و حالت تیره
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.shemim.core.datastore"
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
    api(project(":core:common"))
    api(project(":core:designsystem"))

    api(libs.androidx.datastore.preferences)
}
