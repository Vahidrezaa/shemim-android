pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "shemim"

// ماژول اپلیکیشن
include(":app")

// ماژول‌های هسته (سند ۰۲)
include(":core:common")
include(":core:designsystem")
include(":core:model")
include(":core:network")
include(":core:db")
include(":core:sync")
include(":core:datastore")

// ماژول‌های ویژگی — اسکلت از فاز ۰ کامل ساخته شده؛ هر یک در فاز مربوط خودش (سند ۰۰) پر می‌شود
include(":feature:auth")
include(":feature:dashboard")
include(":feature:members")
include(":feature:transactions")
include(":feature:loans")
include(":feature:queue")
include(":feature:money")
include(":feature:requests")
include(":feature:notifications")
include(":feature:rotatingfund")
include(":feature:investfunds")
include(":feature:polls")
include(":feature:points")
include(":feature:calendar")
include(":feature:analytics")
include(":feature:roadmap")
include(":feature:dues")
include(":feature:groups")
include(":feature:settings")
include(":feature:backup")
include(":feature:messenger")
include(":feature:assistant")
include(":feature:guide")
include(":feature:ownerpower")
include(":feature:more")
