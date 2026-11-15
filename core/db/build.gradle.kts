// core:db — Room entities/DAOs و نگاشت appData→Room — WP-0.4
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.shemim.core.db"
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

    testOptions {
        unitTests {
            // تست دود Room با Robolectric (بازکردن دیتابیس درون‌حافظه‌ای بدون گوشی)
            isIncludeAndroidResources = true
            all { test -> test.maxHeapSize = "2048m" }
        }
    }
}

dependencies {
    // مدل‌های appData — ورودی مپرها
    api(project(":core:model"))

    implementation(libs.androidx.room.runtime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    ksp(libs.androidx.room.compiler)

    // مپرها DTO→Entity کاملاً JVM خالص‌اند — بدون نیاز به اندروید
    testImplementation(libs.junit)

    // تست دود دیتابیس درون‌حافظه‌ای
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.room.testing)
}
