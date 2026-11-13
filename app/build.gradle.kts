plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.shemim.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.shemim.app"
        minSdk = 24
        targetSdk = 36

        // versionName از gradle.properties خوانده می‌شود (سند ۰۶ §۴)
        // versionCode در CI با github.run_number بازنویسی می‌شود: -PversionCode=N
        versionCode = providers.gradleProperty("versionCode").orElse("1").get().toInt()
        versionName = providers.gradleProperty("VERSION_NAME").get()
    }

    buildTypes {
        release {
            // امضای release فقط وقتی متغیرهای محیطی (از GitHub Secrets) موجود باشد؛
            // در غیر این صورت با کلید debug امضا می‌شود تا بیلد خصوصی نشکند (سند ۰۶ §۳).
            val ksFile = System.getenv("KEYSTORE_FILE")
            val ksPassword = System.getenv("KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("KEY_ALIAS")
            val keyPassword = System.getenv("KEY_PASSWORD")
            val hasReleaseSigning =
                !ksFile.isNullOrBlank() && file(ksFile).exists() &&
                    !ksPassword.isNullOrBlank() && !keyAlias.isNullOrBlank() && !keyPassword.isNullOrBlank()

            if (hasReleaseSigning) {
                signingConfigs.create("release") {
                    storeFile = file(ksFile!!)
                    storePassword = ksPassword
                    this.keyAlias = keyAlias
                    this.keyPassword = keyPassword
                }
                signingConfig = signingConfigs.getByName("release")
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }

            // R8/minify در فاز ۶ فعال می‌شود (WP-6.4)
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
