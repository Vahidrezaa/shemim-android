// SyncEngine — WP-0.5 (Outbox و WorkManager دوره‌ای در WP خودشان در فاز ۶ افزوده می‌شوند)
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.shemim.core.sync"
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
    // بخش عمومی API موتور سینک: خود دیتابیس و کلاینت شبکه
    api(project(":core:db"))
    api(project(":core:network"))

    implementation(project(":core:model"))
    implementation(libs.androidx.room.runtime) // withTransaction — از ۲.۷.۰ داخل room-runtime
    implementation(libs.kotlinx.coroutines.core)

    // تست موتور سینک با سرور جعلی واقعی HTTP + Room درون‌حافظه‌ای (الگوی core:network و core:db)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.okhttp.mockwebserver)
}
