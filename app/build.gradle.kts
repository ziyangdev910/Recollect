plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // C-052 方案 C：Android 单元测试使用 JUnit 5；androidTest 保持 JUnit 4。
    alias(libs.plugins.android.junit5)
}

android {
    namespace = "com.recollect.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.recollect"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        // 设备证据准备用例沿用平台 runner（同 :feature:print/:feature:scan），不引入 androidx.test。
        testInstrumentationRunner = "android.test.InstrumentationTestRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:scheduler"))
    implementation(project(":core:pagination"))
    implementation(project(":data:room"))
    implementation(project(":feature:entry"))
    implementation(project(":feature:planning"))
    implementation(project(":feature:progress"))
    implementation(project(":feature:print"))
    implementation(project(":feature:scan"))
    implementation(project(":feature:confirm"))
    // `RecollectDatabase` 的父类 `RoomDatabase` 出现在公共构造签名中，app 需要该运行时类型。
    implementation(libs.room.runtime)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    // 设备证据准备用例只编译期引用 SDK optional jar；运行期由设备提供（R-05，无新 Maven 依赖）。
    androidTestCompileOnly(
        files(
            File(
                android.sdkDirectory,
                "platforms/android-${android.compileSdk}/optional/android.test.runner.jar",
            ),
            File(
                android.sdkDirectory,
                "platforms/android-${android.compileSdk}/optional/android.test.base.jar",
            ),
        ),
    )
}
