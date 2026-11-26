import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // C-052 方案 C：Android 单元测试使用 JUnit 5；androidTest 保持 JUnit 4。
    alias(libs.plugins.android.junit5)
}

// 正式签名凭据放在仓库外的 keystore.properties（已在 .gitignore 中忽略）。
// 文件缺失时 release 回退 debug 签名，保证未配置密钥的机器仍可本地安装构建。
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseKeystore = keystoreProperties.getProperty("storeFile") != null

android {
    namespace = "org.ziyangdev910.recollect"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.recollect"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "0.3.0"
        // 设备证据准备用例沿用平台 runner（同 :feature:print/:feature:scan），不引入 androidx.test。
        testInstrumentationRunner = "android.test.InstrumentationTestRunner"
        ndk {
            // 仅打包真机 ABI，去掉 x86/x86_64 模拟器原生库，显著减小 APK。
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    buildFeatures {
        compose = true
    }

    signingConfigs {
        // 仅在配置了正式 keystore 时创建；无条件创建会因空凭据导致 assembleRelease 校验失败。
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // 开发构建保持不压缩：不做 R8 混淆/代码压缩与资源裁剪，便于调试与堆栈定位。
            isMinifyEnabled = false
            isShrinkResources = false
        }
        release {
            // 代码压缩与资源裁剪：减小 DEX/资源体积；规则见 proguard-rules.pro。
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // 正式发布使用 keystore.properties 指向的正式 keystore；未配置时回退 debug 签名，
            // 仅用于本地 adb install，不可用于分发。
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
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
    // DemoBankAssetTest 直接读取首次演示的示例题库；登记为输入，改资产后测试不会命中旧缓存。
    inputs.file(layout.projectDirectory.file("src/main/assets/demo/seed.json"))
        .withPropertyName("firstRunDemoBank")
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
    implementation(project(":feature:backup"))
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
