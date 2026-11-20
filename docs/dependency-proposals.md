# 依赖提案（2026-11-13 bootstrap）

> 依据 CLAUDE.md R-05：不引入设计决策外的新依赖；需要时写入本文件等待人类确认。
> 状态：2026-11-13 用户指示“生成构建骨架”，视为对本表 bootstrap 依赖与版本的确认。
> 之后新增依赖或升级版本仍需人类确认。

## 版本矩阵

| 组件 | 版本 | 用途 | 证据 |
|---|---|---|---|
| Gradle | 8.13 | Wrapper | AGP 8.13.2 的 `GRADLE_MIN_VERSION=8.13`（javap 读取） |
| AGP | 8.13.2 | Android 构建插件 | Aliyun Google 镜像元数据 200 |
| Kotlin | 2.2.21 | Kotlin/JVM 与 Android | Maven Central 稳定版 |
| KSP | 2.2.21-2.0.5 | Room 注解处理 | 与 Kotlin 2.2.21 严格匹配 |
| Compose BOM | 2025.12.01 | Compose 版本对齐 | Aliyun Google 镜像 POM 200 |
| Room | 2.7.2 | 持久化 | Aliyun Google 镜像 POM 200 |
| JUnit Jupiter | 5.13.4 | 单元测试 | Maven Central 200 |
| JUnit Platform Launcher | 1.13.4 | Gradle 8.13 的 JUnit 5 运行时 | Maven Central 200 |
| ML Kit text-recognition-chinese | 16.0.0 | 中文离线 OCR（CLAUDE.md 冻结） | Aliyun Google 镜像 200 |
| ML Kit vision-common | 17.3.0 | ML Kit 公共依赖（CLAUDE.md 冻结） | Aliyun Google 镜像 200 |
| OpenScanVision | 1.0.0（C-051 方案 A，spike） | ArUco/透视/QR/OMR | JitPack `com.github.MatiwosKebede:openscanvision:1.0.0`（不存在 `openscanvision-core` 坐标） |
| android-junit5 插件 | 1.13.4.0（C-052 方案 C） | Android 单元测试 JUnit 5 | Maven Central 插件 marker 200；与 JUnit Jupiter 5.13.4 对齐 |
| ML Kit barcode-scanning | 17.2.0（C-051 方案 A，已加入版本目录） | 逐题 QR 解码 | OpenScanVision 1.0.0 POM 的传递依赖；直接调用 API 时在模块 build 文件显式声明并复核 GMS 依赖 |

## 选择理由

- **不使用 AGP 9.x**：AGP 9.1.1 最低要求 Gradle 9.3.1，AGP 9.5.1 最低要求 Gradle 9.6.0；本机缓存是 Gradle 9.1.0，且 AGP 9.x 引入 built-in Kotlin，首次 bootstrap 不值得承担迁移风险。
- **不用 Gradle 官方分发包**：`services.gradle.org` 会跳转到 GitHub release assets，当前网络下 Wrapper URL 校验失败；改用腾讯镜像 `https://mirrors.cloud.tencent.com/gradle/gradle-8.13-bin.zip`。
- **Compose BOM 2025.12.01**：与 Kotlin 2.2.21 的元数据兼容性更稳；2026 年的 BOM 可能由更新 Kotlin 编译，升级时需同步提升 Kotlin/KSP 并做首次构建验证。
- **JUnit 5 覆盖纯 Kotlin 与 Android 单元测试**：`core:common/model/scheduler/pagination` 直接使用 JUnit 5；Android 模块按 C-052 方案 C 引入 `de.mannodermaus.android-junit5` 1.13.4.0，`androidTest` 保持 JUnit 4。

## 仓库与镜像

- Gradle 依赖：`maven.aliyun.com/repository/google` → `google()` → `mavenCentral()`。
- JitPack 仅允许 `com.github.*`，避免被无关请求拖慢。
- Android SDK：`cmdline-tools` 与 `platforms/android-36` 已从腾讯镜像/Google 仓库安装；`build-tools/35.0.0` 由 AGP 8.13 默认使用，无需额外安装。
- 设备验证：实体机 + adb；不安装 emulator/system image。

## 2026-11-13 裁决落地

- **C-046 方案 A**：`core:model` 只放纯 DTO/枚举与 `KnowledgeRowsCodec`；Room Entity ↔ DTO 映射在 `data:room`。
- **C-047 方案 A**：`PaginationResult` 改为 sealed `Success` / `Failure(PaginationFailureCode)`；失败码 `INVALID_TEMPLATE_GEOMETRY`、`PLAN_TOO_LONG`。
- **C-048**：分页放置语义按 `pagination.md` 的“放置语义（C-048）”执行。
- **C-049**：scheduler 的 id 列表、deferred 顺序、空关联/负容量等边界按 `scheduler.md` 的“确定性与边界输入（C-049）”执行。
- **C-050**：`continuousScore` 内部 `Double`、对外/落库 `Float`；`effectiveFeedbackCount` 精确 `>= 3.0f`。
- **C-051 方案 A**：先接 JitPack 根坐标并做 `ScanEngine` 适配层；R-01 以真实打印稿验证，不达标转方案 B（fork）。
- **C-052 方案 C**：Android 单元测试用 JUnit 5（`de.mannodermaus.android-junit5` 1.13.4.0）；`androidTest` 用 JUnit 4。

## 2026-11-18 阶段 5.1：app 壳的 Compose Activity 接入

本轮任务明确要求 `MainActivity` + Compose 壳，因此需要以下 artifact。除 `activity-compose` 外均由已批准的 Compose BOM 2025.12.01 管理版本。

| 名称 | 版本 | 用途 | 替代方案 | 体积/ABI/16KB 风险 |
|---|---|---|---|---|
| `androidx.compose.runtime:runtime` | BOM 管理（1.10.0） | `AppNavigator` 的 `mutableStateOf` | 无（Compose 状态基础） | 纯 JVM/Android 代码，无 `.so`；无 16KB 页对齐风险 |
| `androidx.compose.ui:ui` | BOM 管理（1.10.0） | Compose 壳的 `setContent`/布局 | 传统 View + XML | AAR 体积中等；传递依赖带入 `libandroidx.graphics.path.so`（arm64-v8a/armeabi-v7a/x86/x86_64）；`zipalign -c -P 16` 校验通过，见 agent-log 2026-11-18 |
| `androidx.compose.material3:material3` | BOM 管理（1.4.0） | `Scaffold`/`TopAppBar` 壳 | 只用 `compose.ui` 手写布局 | AAR 体积中等；无 `.so`；不涉及 16KB 对齐 |
| `androidx.activity:activity-compose` | 1.11.0 | `ComponentActivity.setContent` 与 `BackHandler` | 手写 `ComposeView` + 自建 `LifecycleOwner`（易错，不推荐） | 小型 AAR；无 `.so`；不涉及 16KB 对齐 |

选择理由：`activity-compose` 1.11.0 与本仓库 Kotlin 2.2.21 / AGP 8.13.2 / compileSdk 36 兼容，属于 Compose 应用的最小标准接入；`BackHandler` 直接来自该 artifact，符合 app.md“用 `BackHandler` 处理返回”。不引入 navigation-compose、Hilt/Koin。

> 状态：本轮任务已明确要求 Compose 壳，视为对接入 `activity-compose` 的确认；如人类不认可，可回退本轮 `app` 模块与 `libs.versions.toml` 对应条目。
>
> 16KB 实测（2026-11-18）：`app-debug.apk` 中 4 个 ABI 的 `libandroidx.graphics.path.so` 均通过 `zipalign -c -P 16 -v 4`；仅验证打包偏移，未做 16KB 设备实机启动验证。

## 2026-11-18 阶段 6.1：androidTest 证据用例不引入新 Maven 依赖

- `:feature:print` 的 PDF 证据用例使用设备自带 `android.test.InstrumentationTestRunner`（JUnit3 风格 `AndroidTestCase`）。
- 编译期仅通过 `androidTestCompileOnly` 引用 SDK 的 `platforms/android-36/optional/android.test.runner.jar` 与 `android.test.base.jar`；运行期由设备 optional library 提供，不写入打包依赖，也不进入 release 产物。
- **未新增** Maven/Libs 条目。若后续需要标准 JUnit4 或 `androidx.test`（例如 `connectedAndroidTest` 报告、`InstrumentationRegistry`），需先在本文件登记版本、用途、替代方案与体积/ABI/16KB 风险，等人类确认后再改构建文件。

## 2026-11-18 阶段 6.2：OpenScanVision 1.0.0 spike 结论与 16KB 风险（C-051）

未新增 Maven/Libs 条目：`openscanvision` 与 `mlkit-barcode-scanning` 已在版本目录中登记；本轮只在 `:feature:print` 增加 `androidTestImplementation(libs.openscanvision)`（spike 用例专用，生产代码与 release 产物不引入 OpenCV）。证据见 `evidence/print/osv-spike/`，冲突记录见 `docs/conflicts.md` C-092。

| 项目 | 实测/记录 | 结论 |
|---|---|---|
| 坐标解析 | JitPack `com.github.MatiwosKebede:openscanvision:1.0.0` 的 POM 返回 200；传递依赖 `com.quickbirdstudios:opencv-contrib:4.5.3.0`、`mlkit barcode-scanning:17.2.0`、`play-services-tasks:18.1.0`、`gson:2.11.0`、`kotlinx-coroutines-android:1.8.1` 均可解析 | 坐标有效，无「不存在」问题 |
| 公开 API | `initialize(Context)`、`scan(Bitmap, ScanOptions, token)`、`scanFromFrame(Bitmap, ScanOptions, corners)`；结果只有一次填涂索引列表、置信度、JSON、矫正图与**单个**二维码载荷 | 无法返回多题二维码与按行走势 |
| 模板可配置性 | 参考画面固定 850×540（横向）；内置两套版式的标记位置/二维码区域/气泡分组为库内部常量；`ScanOptions` 的自定义版式参数类型位于库内部包，模块外不可构造，公开可达的只有默认版式 | **方案 A 无法表达本项目 A4 纵向 + 每题 QR 的模板** |
| 体积 | `opencv-contrib` AAR 72.2MB、`opencv` AAR 53.9MB（arm64 `libopencv_java4.so` 25.9MB、`libc++_shared.so` 6.2MB）；ML Kit 条码 `libbarhopper_v3.so` 4.4MB；本次 androidTest APK 含 4 个 ABI 共 **198MB** | 若进入生产，需限制 ABI 并评估体积；本 spike 仅在 test APK 中 |
| 16KB 页对齐 | arm64 `libopencv_java4.so`、`libc++_shared.so`、`libbarhopper_v3.so` 的 ELF `PT_LOAD.p_align=0x1000`（4KB）；`libandroidx.graphics.path.so` 为 `0x4000` | **确认风险**：Android 15+ 16KB 页设备无法加载这三个库；需升级到 16KB 对齐版本或改用其他实现 |
| 真机运行 | `OpenScanVision.initialize` → `OPENCV_INITIALIZED=true`；`scan` 返回 `LowConfidence(confidence=0.0)`，ArUco 检测为 `none` | 库本身可加载运行；输入页没有真实定位图形与 QR，故 OMR 精度**未被测量** |

**结论**：方案 A 在公开 API 层面无法承接本项目模板（不是精度好坏的问题），建议按 C-051 转方案 B（fork，补齐 A4 纵向参考帧、旋转、多 QR、自定义模板入口），并把自研采样（C）作为轻量变体一并评估；待人类裁决（C-092）。裁决前不修改 `docs/specs/`、不更换依赖。

## 2026-11-18 阶段 6.3：C-092 方案 C 直连 OpenCV（已获确认）

C-092 裁决为方案 C（自研回读引擎）后，`:feature:scan` 需要直接调用 OpenCV 做 ArUco 定位与透视。`opencv-contrib` 原本只是 `openscanvision` 的传递依赖，本轮把它登记为版本目录条目并显式声明；版本保持不变。

| 名称 | 版本 | 用途 | 替代方案 | 体积/ABI/16KB 风险 |
|---|---|---|---|---|
| `com.quickbirdstudios:opencv-contrib` | 4.5.3.0 | `DICT_4X4_50` 定位、单应性变换与透视矫正（`org.opencv.aruco` / `imgproc`） | fork OpenScanVision（C-092 已否决）；自写 ArUco 检测器（工作量大、风险高） | AAR 72.2MB；arm64 `libopencv_java4.so` 25.9MB + `libc++_shared.so` 6.2MB；四个 ABI 全打时 test APK 约 196MB。**`PT_LOAD.p_align=0x1000`（4KB），Android 15+ 16KB 页设备无法加载**，需升级到 16KB 对齐版本或替换实现 |

- 打印层**不**依赖 OpenCV：`ArucoPattern` 把 0～3 号图案固化为纯 Kotlin 常量，四角标记由 Canvas 绘制，避免把 72MB 原生库带进 release 产物。
- `mlkit-barcode-scanning:17.2.0` 已在版本目录登记（原 `openscanvision` 的传递依赖），本轮显式声明用于逐题 QR 解码；`libbarhopper_v3.so` 同为 4KB 对齐。
- 体积/ABI 建议：正式接入时按设备 ABI 打包或改用动态特性；16KB 设备风险未解除，需在 R-01 前给出方案。
- `:feature:scan` 的 androidTest 沿用 `:feature:print` 的做法：平台 `android.test.InstrumentationTestRunner` + SDK optional jar 的 `compileOnly` 引用，**未新增** androidx.test/JUnit4 Maven 条目；C-052 的「androidTest 用 JUnit 4」在此按既有先例执行，如需改为标准 JUnit4/androidx.test 需另行提案。

## 2026-11-18 阶段 6.3：二维码生成方案（待确认）

打印层需要为每题生成 `version|planId|pageId|questionId|CRC32` 的二维码（print-template.md：版本 5、纠错 M、边长 18mm、quiet zone 4 modules）。当前依赖里没有编码器：OpenCV 4.5.3.0 的 Java 绑定只有 `QRCodeDetector`、没有 `QRCodeEncoder`；ML Kit 条码只解不编。

| 方案 | 名称/版本 | 用途 | 替代方案 | 体积/ABI/16KB 风险 |
|---|---|---|---|---|
| A（推荐） | `com.google.zxing:core:3.5.3` | 纯 Java 生成 QR（byte mode、版本 5、纠错 M），输出位图交给打印 Canvas | 自写编码器（方案 B） | AAR/JAR 约 0.5MB，无 `.so`、无 ABI 限制、不涉及 16KB；仅打印层使用 |
| B | 自写编码器（无新依赖） | 固定版本 5/M，GF(256) 与 Reed–Solomon 自行实现 | ZXing（方案 A） | 无体积/ABI/16KB 风险；实现与验证成本高，需用设备 ML Kit 解码回验 |
| C | 升级 OpenCV 到含 `QRCodeEncoder` 的版本 | 编码与解码共用 OpenCV | A/B | AAR 体积不变但需整体升级检测库，且 16KB 4KB 对齐问题不会因此解决；改动面最大 |

- 选择理由（方案 A）：ZXing core 是纯 JVM 实现，最符合“端侧、无原生库、低风险”的约束；只用它的编码 API，打印层不依赖 Android 组件。
- 若选方案 A，改动范围：`libs.versions.toml` 增加 `zxing-core = "3.5.3"`；`:feature:print` 增加 `implementation(libs.zxing.core)`；`PrintLayout`/`PrintRenderer` 把空白 QR 预留框换成真实二维码位图。
- 若选方案 B，改动范围：在 `:core:common` 或 `:feature:print` 新增编码器与单测，不新增依赖；需要用设备 ML Kit 对生成的码做解码回验。
- 方案 C 不推荐：升级 OpenCV 会牵动已跑通的 ArUco 检测与 R-01，且不解决 16KB 对齐。

**状态：2026-11-18 人类确认方案 A。** 已新增 `zxing = "3.5.3"` 与 `zxing-core` 条目，`:feature:print` 增加 `implementation(libs.zxing.core)`；打印层只使用其编码 API，不引入 Android 组件。

## 待确认

- 是否引入 ktlint / spotless 作为 hook 门禁（C-053）。
- OpenScanVision 方案 A spike 已按 C-092 结论转方案 C（自研引擎）；`openscanvision` 坐标保留仅为复现 spike，生产不引入。
- 16KB 页面设备上的 OpenCV/ML Kit `.so` 对齐：已实测为 4KB 对齐，需要升级依赖或替换实现；具体版本待选型与提案。
- 二维码**生成**已按方案 A 落地（ZXing core 3.5.3，print 模块专用）；二维码成功解码路径与 CRC 已在设备上验证。

## 2026-11-20 C-096 方案 B：扫描页内 CameraX 预览与拍摄（已获确认）

C-096 裁决为方案 B（页内取景）后，`:feature:scan` 需要 CameraX 提供预览与拍摄。版本取 Google Maven 上当前稳定版 `1.7.0`；四个 artifact 同版本对齐。

| 名称 | 版本 | 用途 | 替代方案 | 体积/ABI/16KB 风险 |
|---|---|---|---|---|
| `androidx.camera:camera-core` | 1.7.0 | 相机用例、`ImageCapture` | 系统相机 Intent（C-096 方案 A，已否决） | AAR 无原生库；纯 Java/Kotlin |
| `androidx.camera:camera-camera2` | 1.7.0 | Camera2 后端 | 自写 Camera2 预览/拍照（工作量大、易错） | 无独立 `.so` |
| `androidx.camera:camera-lifecycle` | 1.7.0 | `ProcessCameraProvider` 生命周期绑定 | 手动管理 Camera2 session | 无独立 `.so` |
| `androidx.camera:camera-view` | 1.7.0 | `PreviewView`（TextureView/SurfaceView） | 自建 SurfaceView 预览 | 传入 `libsurface_util_jni.so`，`PT_LOAD.p_align=0x4000`（16KB 对齐） |

- 实测：`app-debug.apk` 中 `libimage_processing_util_jni.so` 与 `libsurface_util_jni.so` 的 arm64 `PT_LOAD.p_align=0x4000`（16KB 对齐），不新增 16KB 障碍；既有 OpenCV/ML Kit 的 4KB 风险不变。
- 权限：只在 `:feature:scan` 清单声明 `CAMERA` 与 `uses-feature android.hardware.camera.any (required=false)`；不申请存储权限，照片只写 app 缓存并在读取后删除。
- 版本升级或替换相机方案时，必须重跑扫描页的设备路径（预览、拍摄、拒绝权限回退相册）；真实打印稿 R-01 仍需单独的纸张照片证据。
