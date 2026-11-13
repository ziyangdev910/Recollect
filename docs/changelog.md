# 变更记录

| 日期 | 文件 | 意图 | 关联决策 |
|---|---|---|---|
| 2026-11-11 | `CLAUDE.md` | 同步 D-01/D-05 注记/D-07/D-08/D-09、R-04 与文档索引；不改变未裁决语义 | D-01、D-05、D-07～D-09、R-04；C-009、C-013、C-016、C-018、C-010 |
| 2026-11-11 | `docs/decisions/0001-architecture-decisions.md` | 补充 D-01/D-05 注记/D-07/D-08/D-09/D-12/D-13 可执行定义；B-01 全文迁入；更新变更记录 | D-01、D-05、D-07～D-09、D-12、D-13、B-01；C-009、C-010、C-013、C-014、C-016、C-018、C-024、C-028 |
| 2026-11-11 | `docs/conflicts.md` | 写入 26 条冲突的人类裁决、落地位置与 C-008/C-018/C-031 说明；更新缺口清单 | C-006～C-031 |
| 2026-11-11 | `docs/specs/scheduler.md` | 重写接口与三层算法：Float 有效反馈、同档位排序、目标日/日期范围、estimatedReviewMinutes、DeferredItem 归属、移除状态更新 | D-01～D-05、D-11、D-12；C-006、C-007、C-017、C-019、C-020、C-021、C-018 |
| 2026-11-11 | `docs/specs/feedback-usecase.md`（新增） | 状态更新 UseCase、冷启动/切换边界、冷却顺延应用、C-019 三方案对比 | D-01、D-03、D-04、D-05、D-13；C-006、C-009、C-010、C-018、C-019、C-021、C-022 |
| 2026-11-11 | `docs/specs/data-model.md` | Float 计数、estimatedReviewMinutes、复合主键 PendingFeedback、确认决定日志、NoteAttachmentItem、墓碑/编号序列、PlanPage pageId 唯一索引与 knowledgeRows 行快照 | D-01、D-06、D-11、D-12、D-13；C-006、C-009、C-013、C-015、C-017、C-019、C-022、C-023、C-026、C-028、C-030、C-031 |
| 2026-11-11 | `docs/specs/confirm-flow.md` | UNMARKED 状态机、归属校验、NOT_REVIEWED 审计、触发行/非触发行、OCR 只读/修正分离 | D-06、D-09、D-13；C-009、C-010、C-011、C-012、C-022、C-030 |
| 2026-11-11 | `docs/specs/scan-pipeline.md` | QR CRC-32/错误分流、归属校验、OMR 行组按行快照重建、手动回退、OCR 只读验收 | D-06、D-09、D-10；C-010、C-011、C-012、C-015、C-021、C-030 |
| 2026-11-11 | `docs/specs/print-template.md` | 左截断/右全称、pageId 上纸、续下页措辞、NoteAttachmentItem 摘要、真机/模拟器回读验收 | D-07、D-08、D-09；C-013、C-014、C-015、C-016、C-026、C-027、C-029 |
| 2026-11-11 | `docs/specs/pagination.md` | usableHeightMm、OverflowItem、日期升序、knowledgeLineCount、30% 附加区、marker 顺序 | D-07、D-08；C-013、C-015、C-016、C-025、C-027 |
| 2026-11-11 | `docs/specs/backup.md` | backupFormatVersion、逐字段矩阵、白名单、跨版本旧稿失效 | B-01、D-11；C-023、C-024、C-026、C-031 |
| 2026-11-11 | `docs/specs/entry.md`（新增） | 录入层/Repository 双重校验、编号序列与墓碑 | D-06、D-12；C-026、C-028 |
| 2026-11-11 | `docs/research/maimemo-model.md`（新增） | 墨墨 DHP 模型公式、前三次反馈→初始半衰期方案、待确认问题 | D-05；C-018 |
| 2026-11-11 | `docs/changelog.md`（新增） | 建立本轮文件改动记录 | 工作循环 |
| 2026-11-13 | `docs/architecture/overview.md`（新增） | 架构总览：字段产权、实体关系、生命周期、多形态、最小状态集、自测三问 | C-032、C-033；D-01～D-13 |
| 2026-11-13 | `docs/agent-log/2026-11-13.md`（新增） | 记录本地架构盘点清单、无 Kotlin 实现的发现、overview 章节检查 | C-032、C-033 |
| 2026-11-13 | `docs/conflicts.md` | 新增 C-032（本地实现缺失）与 C-033（字段写入者未定义） | C-032、C-033 |
| 2026-11-13 | `CLAUDE.md` | 文档索引新增架构总览行 | C-032、C-033 |
| 2026-11-13 | `docs/conflicts.md` | 删除已裁决条目；最终仅保留未决 C-032（C-008/C-018/C-031/C-033 已裁决） | 用户要求 |
| 2026-11-13 | `docs/specs/scheduler.md` | C-008 方案二：删除 `coolingPeriodOverrideDays`，`plan()` 增加 `coolingOverrides`；C-018：`KnowledgeState` 增加 `difficulty/halfLifeDays` | C-008、C-018、C-033 |
| 2026-11-13 | `docs/specs/data-model.md` | 新增 `CoolingOverrideEvent`、`difficulty/halfLifeDays`；删除 `Question.coolingPeriodOverrideDays`；迁移备注 | C-008、C-018、C-031 |
| 2026-11-13 | `docs/specs/feedback-usecase.md` | C-018 契约落地：`MemoryState`/`NextDueResult`/`MemoryStateInitializer`；DHP 初始化与切换边界 | C-018 |
| 2026-11-13 | `docs/specs/confirm-flow.md` | 增加「本次提前复习」一次性 `CoolingOverrideEvent` | C-008 |
| 2026-11-13 | `docs/specs/print-template.md` | C-033 方案一：规划层写 `lastScheduledDate/PlanPage`，打印层写 `pageId/qrPayload` | C-033 |
| 2026-11-13 | `docs/specs/pagination.md` | 说明 `PlanPage`/`lastScheduledDate` 由 `feature/planning` 持久化 | C-033 |
| 2026-11-13 | `docs/specs/backup.md` | `difficulty/halfLifeDays` 恢复矩阵；明确排除 `PendingFeedback` 与 `CoolingOverrideEvent` | C-018、C-031、C-008 |
| 2026-11-13 | `docs/decisions/0001-architecture-decisions.md` | D-04 一次性冷却覆盖；D-05 墨墨 DHP 模型确认 | C-008、C-018、C-033 |
| 2026-11-13 | `CLAUDE.md` | D-04/D-05 同步确认后的规则 | C-008、C-018 |
| 2026-11-13 | `docs/research/maimemo-model.md` | 状态改为已确认，记录裁决与实现落点 | C-018 |
| 2026-11-13 | `docs/architecture/overview.md` | 同步新增字段、`CoolingOverrideEvent`、写入者与最小状态集 | C-008、C-018、C-033 |
| 2026-11-13 | `docs/agent-log/2026-11-13.md` | 记录裁决落地与清单更新 | C-008、C-018、C-031、C-032、C-033 |
| 2026-11-13 | `docs/decisions/0001-architecture-decisions.md`（删除） | 按当前架构修正 D-01～D-13/B-01，随后合并到 specs/CLAUDE 并删除文件与 decisions 文件夹 | D-01～D-13、B-01 |
| 2026-11-13 | `docs/conflicts.md` | 替换为实现层缺口 2～5 的决策提案（编排/打印/扫描/DHP），待审核；未改 specs | C-034～C-037、C-032 |
| 2026-11-13 | `docs/specs/planning.md`（新增） | 计划编排 `GeneratePlanUseCase`、事务、ID/时区/默认设置、归档删除、笔记附件 | C-034 |
| 2026-11-13 | `docs/specs/repository.md`（新增） | Repository 接口、事务规则、core/model 映射 | C-034 |
| 2026-11-13 | `docs/specs/pagination.md` | `TemplateGeometry` 默认值/新增字段、`qrFooterHeightMm`、内容宽度、P-19 | C-035 |
| 2026-11-13 | `docs/specs/print-template.md` | 文本测量、ArUco/QR 几何、`PrintJob`、PrintDocumentAdapter 接线 | C-035 |
| 2026-11-13 | `docs/specs/scan-pipeline.md` | `ScanEngine`、页面检测/旋转、OMR 几何/阈值、QR-题块绑定、PAGE_* 错误 | C-036 |
| 2026-11-13 | `docs/specs/feedback-usecase.md` | DHP 边界：间隔来源、延迟复习、d_init/h_init、夹取、迁移兜底、事务 | C-037 |
| 2026-11-13 | `docs/specs/data-model.md` / `confirm-flow.md` | 归档/删除原子规则、笔记附件保存入口 | C-034 |
| 2026-11-13 | `CLAUDE.md` / `docs/architecture/overview.md` | 文档索引加入 planning/repository；多形态表加入 PlanDraft/PrintJob | C-034、C-035 |
| 2026-11-13 | `docs/conflicts.md` | 替换为“周末打印/下周扫描”场景审查，提出 C-038～C-041 | C-038～C-041 |
| 2026-11-13 | `docs/specs/data-model.md` | C-037 增加 `KnowledgePoint.createdAt`（DHP 首段间隔起点） | C-037 |
| 2026-11-13 | `docs/specs/data-model.md` | C-038～C-041：`Question.lastReviewedDate`、`PendingFeedback planId/pageId/plannedDate/actualReviewDate`、`ReviewFeedback`/`ConfirmationDecisionLog` 计划/实际日期、`ScanSession` | C-038～C-041 |
| 2026-11-13 | `docs/specs/scheduler.md` | 冷却改用 `lastReviewedDate`；`KnowledgeState` 增加 `createdAt` | C-039、C-037 |
| 2026-11-13 | `docs/specs/feedback-usecase.md` | 批量实际复习日期、`lastReviewedDate`、LATE/MISSED 语义、U-18/U-19 | C-038、C-039 |
| 2026-11-13 | `docs/specs/confirm-flow.md` | 批次实际复习日期、`ScanSession` 多页确认、提前/晚扫描校验 | C-038、C-040、C-041 |
| 2026-11-13 | `docs/specs/scan-pipeline.md` | `ScanSession`、多页追加/去重、提前/晚扫描 | C-038、C-040、C-041 |
| 2026-11-13 | `docs/specs/planning.md` / `repository.md` | 计划周/扫描窗口、`ScanRepository`、笔记附件接口 | C-038～C-041 |
| 2026-11-13 | `docs/specs/backup.md` | `lastReviewedDate`、`ScanSession` 排除、新字段矩阵 | C-038～C-041 |
| 2026-11-13 | `docs/conflicts.md` | C-038～C-041 已落地；提出剩余缺口 C-042～C-045 | C-042～C-045 |
| 2026-11-13 | `docs/specs/app.md`（新增） | 模块图、手写 DI、状态式导航、屏幕清单/状态、权限、计划完成度 UI | C-042、C-044 |
| 2026-11-13 | `docs/specs/settings.md`（新增） | 每日分钟、计划周、扫描窗口、PlanPage 保留/清理设置 | C-042、C-045 |
| 2026-11-13 | `docs/specs/backup.md` | JSON Schema、逐版本迁移、SAF 状态机、保留/清理 | C-043、C-045 |
| 2026-11-13 | `docs/specs/repository.md` | `PlanCompletion` 查询与派生规则 | C-044 |
| 2026-11-13 | `CLAUDE.md` | 文档索引加入 app/settings | C-042 |
| 2026-11-13 | `docs/conflicts.md` | C-042～C-045 已落地；仅保留 C-032 暂缓 | C-032 |
| 2026-11-13 | `cmdline-tools` 与 `platforms/android-36`（SDK，非仓库文件） | P0 环境补全：腾讯镜像安装 cmdline-tools 16111833，安装 Android 16（API 36）平台；实体机方案，不装模拟器 | 工作循环 |
| 2026-11-13 | `settings.gradle.kts`（新增） | Gradle 8.13 Wrapper、四个 core 模块、Google 优先/阿里云回退仓库、JitPack 仅限 com.github.* | C-032、C-046～C-052 |
| 2026-11-13 | `build.gradle.kts`（新增） | 根插件声明（apply false）与 `verifyCorePurity` 任务 | R-02、C-032、C-046 |
| 2026-11-13 | `gradle/libs.versions.toml`（新增） | 锁定 AGP 8.13.2、Kotlin 2.2.21、KSP、Compose BOM、Room、JUnit 5、ML Kit、OpenScanVision 版本 | C-051、C-052、R-05 |
| 2026-11-13 | `gradle.properties`、`gradle/wrapper/*`、`gradlew`、`gradlew.bat`（新增） | JVM 参数/并行/缓存；Wrapper 指向腾讯 Gradle 8.13 分发包 | C-032 |
| 2026-11-13 | `core/{common,model,scheduler,pagination}/build.gradle.kts`（新增） | Kotlin/JVM 17、JUnit 5 测试运行时、关闭并行执行保证确定性 | C-052 |
| 2026-11-13 | `.gitignore` | 忽略 Gradle/IDE/SDK 本地文件；把 `agent-log/` 等模式锚定到仓库根，使 `docs/` 审计记录可入库 | 工作循环 |
| 2026-11-13 | `.claude/settings.json`（新增，按当前 `.gitignore` 不入库） | Bash 超时 300s、Android/JDK 环境变量、Gradle/adb/git/curl 权限 | 工作循环 |
| 2026-11-13 | `docs/dependency-proposals.md`（新增） | 记录 bootstrap 版本矩阵、镜像选择理由、AGP 9.x 不采用原因 | R-05、C-051、C-052 |
| 2026-11-13 | `docs/conflicts.md` | C-032 改为“部分落地”；新增 C-046～C-052（model 映射、分页失败通道/放置语义、scheduler 确定性、Float、OpenScanVision、Android JUnit 5） | C-046～C-052 |
| 2026-11-13 | `docs/prompts.md`（新增） | 按阶段与固定循环整理可直接使用的 TDD Prompts、审计/审查/设备验证/收尾 Prompt | 工作循环、C-046～C-052 |
| 2026-11-13 | `docs/specs/scheduler.md` | C-046：DTO 改名 `SchedulerQuestion`；C-049：id 列表/deferred 顺序、空关联/负容量/空日期范围边界；C-050：`continuousScore` 内部 Double/对外 Float | C-046、C-049、C-050 |
| 2026-11-13 | `docs/specs/pagination.md` | C-047：`PaginationResult` 改为 sealed `Success`/`Failure`；C-048：yOffset/height、首页/续下页、空日、overflow 顺序、非法输入 | C-047、C-048 |
| 2026-11-13 | `docs/specs/repository.md` | C-046 方案 A：core:model 只放纯 DTO/枚举 + `KnowledgeRowsCodec`；Room↔DTO 映射移到 data/room | C-046 |
| 2026-11-13 | `docs/specs/data-model.md` | C-046：`linkedKnowledgeIds` 来自 cross-ref、Codec 归 core:model；C-050：`continuousScore` 类型与算术边界 | C-046、C-050 |
| 2026-11-13 | `docs/specs/feedback-usecase.md` | C-050：`continuousScore` 内部 Double、落库 Float；`effectiveFeedbackCount` 精确阈值；测试断言规则 | C-050 |
| 2026-11-13 | `docs/specs/confirm-flow.md` | C-050：确认页状态更新的 Double/Float 边界 | C-050 |
| 2026-11-13 | `docs/specs/scan-pipeline.md` | C-051 方案 A：OpenScanVision 根坐标、850×540 适配、16KB 对齐风险、R-01 spike 门禁 | C-051 |
| 2026-11-13 | `docs/specs/print-template.md` | C-051 方案 A：打印验收使用适配层，精度以真实打印稿为准 | C-051 |
| 2026-11-13 | `docs/specs/planning.md` | C-047：`PaginationResult.Failure` 转 `PlanGenerationException`，不进入事务；`PLAN_TOO_LONG` 不写库 | C-047 |
| 2026-11-13 | `docs/specs/app.md` | C-046：core:model 纯 DTO/Codec；C-052：单元测试 JUnit 5、androidTest JUnit 4 | C-046、C-052 |
| 2026-11-13 | `docs/architecture/overview.md` | C-046：映射归属与 `SchedulerQuestion`；C-050：`continuousScore` 类型边界；骨架状态更新 | C-046、C-050 |
| 2026-11-13 | `CLAUDE.md` | OpenScanVision 根坐标/方案 A、JUnit 5/JUnit 4 测试栈、D-03/D-09、模块纯度、实体机验证命令、文档索引 | C-046、C-050、C-051、C-052 |
| 2026-11-13 | `docs/dependency-proposals.md` | 记录 C-046～C-052 裁决；新增 `de.mannodermaus.android-junit5` 1.13.4.0；OpenScanVision 根坐标与 16KB 风险 | C-051、C-052 |
| 2026-11-13 | `docs/prompts.md` | 把 C-046～C-052 裁决写进各阶段 Prompt，移除“若选方案”分支 | C-046～C-052 |
| 2026-11-13 | `gradle/libs.versions.toml`、`build.gradle.kts` | 加入 `de.mannodermaus.android-junit5` 1.13.4.0 插件别名（apply false）与 `mlkit-barcode-scanning` 17.2.0 版本目录条目 | C-051、C-052 |
| 2026-11-13 | `docs/conflicts.md` | 删除已裁决的 C-046～C-052 详情；仅保留 C-032 与“已裁决但待验证”门禁 | C-032、C-046～C-052 |
