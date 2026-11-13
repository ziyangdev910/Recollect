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

## 待确认

- 是否引入 ktlint / spotless 作为 hook 门禁（C-053）。
- OpenScanVision 方案 A spike 的 R-01 结果；不达标时是否转方案 B。
- 16KB 页面设备上的 OpenCV/ML Kit `.so` 对齐是否需要升级依赖。
