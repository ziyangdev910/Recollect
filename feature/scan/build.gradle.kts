plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // C-052 方案 C：Android 单元测试使用 JUnit 5；androidTest 保持 JUnit 4。
    alias(libs.plugins.android.junit5)
}

android {
    namespace = "com.recollect.feature.scan"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        // 证据用例使用平台自带 runner（同 :feature:print），不引入 androidx.test（R-05）。
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
    implementation(project(":core:pagination"))
    // 归属校验的页面快照与回读会话持久化由 data:room 提供。
    implementation(project(":data:room"))

    // C-092 方案 C：自研引擎直接用 OpenCV 做 ArUco/透视；QR 用 ML Kit 条码。
    // 两者版本均已在 libs.versions.toml 固定，登记与 16KB 风险见 docs/dependency-proposals.md。
    implementation(libs.opencv.contrib)
    implementation(libs.mlkit.barcode.scanning)
    // 手写笔记 OCR（CLAUDE.md 冻结的中文离线模型）。
    implementation(libs.mlkit.text.recognition.chinese)

    // 扫描回读页 Compose 界面；artifact 均在已批准的 Compose BOM 内，未新增外部依赖。
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    // 相机/相册 ActivityResult 契约；activity-compose 版本已在 app 批准（1.11.0）。
    implementation(libs.activity.compose)
    // C-096 方案 B：扫描页内 CameraX 预览与拍摄（见 docs/dependency-proposals.md 2026-11-20）。
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // C-052 方案 C：单元测试 JUnit 5。
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    // 设备证据用例用真实打印页（PDF → 2480×3508）驱动引擎；仅测试期引用兄弟 feature。
    androidTestImplementation(project(":feature:print"))
    androidTestImplementation(project(":feature:planning"))
    androidTestImplementation(project(":data:room"))
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
