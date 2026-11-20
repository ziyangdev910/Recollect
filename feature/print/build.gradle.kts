plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // C-052 方案 C：Android 单元测试使用 JUnit 5；本模块 androidTest 证据用例用平台
    // InstrumentationTestRunner（JUnit3 风格），理由与后续替换条件见 docs/dependency-proposals.md。
    alias(libs.plugins.android.junit5)
}

android {
    namespace = "com.recollect.feature.print"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        // 证据生成用平台自带 runner，避免为一张排版 PDF 引入 androidx.test 依赖（R-05）。
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
    implementation(project(":core:pagination"))

    // 每题二维码生成（C-092 方案 C 的打印侧；纯 Java，无原生库）。
    implementation(libs.zxing.core)

    // 打印页 Compose 界面；artifact 均在已批准的 Compose BOM 内，未新增外部依赖。
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    // 证据用例构造真实 PrintJob 样本；仅测试期引用，打印模块生产代码不依赖兄弟 feature。
    androidTestImplementation(project(":feature:planning"))
    androidTestImplementation(project(":data:room"))

    // 阶段 6.2（C-051 方案 A）spike：把渲染后的 A4 位图喂给 OpenScanVision 记录原始输出。
    // 坐标与版本已在 libs.versions.toml 固定（R-05：未新增版本目录之外的依赖）；仅 androidTest 使用，
    // 生产代码与 release 产物不引入 OpenCV。结论与 16KB 对齐风险见 docs/dependency-proposals.md。
    androidTestImplementation(libs.openscanvision)

    // 证据用例使用设备上的平台 android.test.runner（androidTest 清单已声明 uses-library），
    // 仅编译期引用 SDK 的 optional jar；不引入 androidx.test 或 JUnit4 新依赖（R-05）。
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
