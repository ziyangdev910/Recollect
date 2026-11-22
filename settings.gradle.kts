import org.gradle.api.initialization.resolve.RepositoriesMode

pluginManagement {
    repositories {
        // Google Maven 直连可用时优先；阿里云只作回退，避免镜像 DNS 抖动拖慢整个构建。
        google()
        maven("https://maven.aliyun.com/repository/google")
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://maven.aliyun.com/repository/google")
        mavenCentral()
        maven("https://jitpack.io") {
            content {
                includeGroupByRegex("com\\.github\\..*")
            }
        }
    }
}

rootProject.name = "Recollect"

include(
    ":app",
    ":core:common",
    ":core:model",
    ":core:scheduler",
    ":core:pagination",
    ":data:room",
    ":feature:entry",
    ":feature:planning",
    ":feature:progress",
    ":feature:print",
    ":feature:scan",
    ":feature:confirm",
)
