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
| 2026-11-13 | `core/scheduler/src/main/kotlin/com/recollect/scheduler/Scheduler.kt`（新增） | S-01：落地 scheduler.md 接口段的公开类型与接口（`Tier`、`KnowledgeState`、`SchedulerQuestion`、`PlanResult`、`PlannedQuestion`、`DeferredItem`、`DeferReason`、`Scheduler`）；DTO 用 C-046 的新名 `SchedulerQuestion` | D-01、D-11、C-046、C-049 |
| 2026-11-13 | `core/scheduler/src/main/kotlin/com/recollect/scheduler/DefaultScheduler.kt`（新增） | S-01 最小实现：归档过滤、到期候选过滤、`targetDate`、直接放入目标日；不含冷却、排序、时间装箱 | D-01、D-11、C-020、C-049 |
| 2026-11-13 | `core/scheduler/src/test/kotlin/com/recollect/scheduler/SchedulerPlanTest.kt`（新增） | S-01 红绿测试：k1 到期 + q1 关联 → 排入 2026-11-16，`triggeredKnowledgeIds=[k1]`，15 分钟，无 deferred | S-01、D-01 |
| 2026-11-13 | `docs/conflicts.md` | 新增 C-053：scheduler DTO 归属（`core:model` 规格 vs 本轮 `core:scheduler` 落点）待裁决 | C-046、R-06 |
| 2026-11-13 | `docs/agent-log/2026-11-13.md` | 记录 S-01 的目标、红/绿输出、验证结果与下轮建议 | 工作循环 |
| 2026-11-13 | `core/model/src/main/kotlin/com/recollect/model/SchedulerModels.kt`（新增） | C-053 方案 A：`Tier`、`KnowledgeState`、`SchedulerQuestion` 迁入 `core:model`（包 `com.recollect.model`） | C-046、C-053 |
| 2026-11-13 | `core/scheduler/build.gradle.kts` | 增加 `implementation(project(":core:model"))` | C-053 |
| 2026-11-13 | `core/scheduler/src/main/kotlin/com/recollect/scheduler/Scheduler.kt` | 移除已迁出的 DTO，只保留 `Scheduler`/`PlanResult`/`PlannedQuestion`/`DeferredItem`/`DeferReason`，并 import `com.recollect.model.*` | C-053 |
| 2026-11-13 | `core/scheduler/src/main/kotlin/com/recollect/scheduler/DefaultScheduler.kt`、`core/scheduler/src/test/kotlin/com/recollect/scheduler/SchedulerPlanTest.kt` | DTO import 改为 `com.recollect.model.*`，行为与断言不变 | C-053 |
| 2026-11-13 | `docs/prompts.md` | 1.1 的调度 DTO 旧称 `Question` 改为 `SchedulerQuestion`（Room 实体名的 `Question` 引用不变） | C-046、C-053 |
| 2026-11-13 | `docs/conflicts.md` | 移除已裁决的 C-053；已裁决/已落地条目改为指向归档文件 | C-053 |
| 2026-11-13 | `docs/conflicts-archive.md`（新增） | 归档 C-034～C-053 的裁决与落地位置，以及 C-050/C-051 待验证门禁 | 工作循环、C-053 |
| 2026-11-13 | `core/scheduler/src/test/kotlin/com/recollect/scheduler/SchedulerPlanTest.kt` | 新增 S-08（归档题不出现在任何 `dailyAssignments`）与 S-09（`dormant` 或 `activeQuestionCount=0` 的知识点不调度、不进入 `nonTriggeredKnowledgeIds`）；首跑即绿，用临时变异证明两条测试可捕获过滤缺失 | S-08、S-09、D-01、D-11 |
| 2026-11-13 | `docs/agent-log/2026-11-13.md` | 追加 S-08/S-09 的目标、Observe、红/绿证据（含变异验证）、验证结果与下轮建议 | 工作循环、D-11 |
| 2026-11-13 | `core/scheduler/src/test/kotlin/com/recollect/scheduler/SchedulerPlanTest.kt` | 新增 S-02（冷却期内不排入；`DeferredItem(COOLING)` 的 `knowledgePointIds` 含全部触发知识点、`targetNextDueDates` 每个 +1 天；`lastReviewedDate + 7 == targetDate` 边界仍排入）与 S-19（命中未消费覆盖 id 时跳过冷却；未命中/已消费不生效；下一份计划恢复冷却）；两条均先红后绿 | S-02、S-19、D-04、C-049 |
| 2026-11-13 | `core/scheduler/src/main/kotlin/com/recollect/scheduler/DefaultScheduler.kt` | 实现冷却判定（严格大于）与 `coolingOverrides` 一次性排除；生成并按 `reason → questionId` 排序 `DeferredItem(COOLING)`；提取 `SchedulerQuestion.isInCoolingPeriod` | D-04、C-049 |
| 2026-11-13 | `core/scheduler/src/main/kotlin/com/recollect/scheduler/DefaultScheduler.kt` | 候选排序层：`Candidate` 封装 `hasTierGap → groupTier（MASTERED > FAMILIAR > UNKNOWN）→ 同档位组内 min continuousScore → targetDate → questionId`；不跨 groupTier 比较分数；冷却/归档/休眠与 deferred 逻辑不变 | D-02、D-03、C-049、C-054 |
| 2026-11-13 | `core/scheduler/src/test/kotlin/com/recollect/scheduler/SchedulerPlanTest.kt` | 新增 S-04（有档位差在前、无档位差不淘汰）与 S-05/S-18（Q1 触发 MASTERED 0.9 + UNKNOWN 0.1、Q2 触发 MASTERED 0.5 时 Q2 在前；UNKNOWN 0.99 不越过 MASTERED 组）；均先红后绿，容量给足 | S-04、S-05、S-18、D-02、D-03 |
| 2026-11-13 | `docs/conflicts.md` | 新增 C-054：`hasTierGap` 规格文字（含其它 triggered）与 S-18「Q2 在前」冲突；记录本轮临时方案与待裁决事项 | D-02、R-06、S-18 |
| 2026-11-13 | `docs/agent-log/2026-11-13.md` | 追加候选排序层目标、Observe、S-04/S-05/S-18 红绿证据、验证命令与结果、未决问题与下轮建议 | 工作循环 |
| 2026-11-15 | `CLAUDE.md` | 「冲突处理」新增规则：冲突条目必须写明对用户的实际影响，并给出以该影响为依据的简短选型参考；描述尽量少用内部变量名/类名 | 工作循环、R-06 |
| 2026-11-15 | `docs/conflicts.md` | C-054 裁决为方案 A 并移出未决；C-032 补“对用户的实际影响”与“选型参考” | C-054、C-032、D-02 |
| 2026-11-15 | `docs/conflicts-archive.md` | 归档 C-054（方案 A）：用户语言描述现象、用户影响、可选方案与选型依据 | C-054、D-02 |
| 2026-11-15 | `core/scheduler/src/main/kotlin/com/recollect/scheduler/DefaultScheduler.kt` | 注释更新：C-054 已裁决方案 A，档位差只比较本期到期点与本期未到期、仍会打印的关联点；无行为变更 | C-054、D-02 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md`（新增） | 记录 C-054 裁决落地、CLAUDE.md 冲突规则与验证结果 | 工作循环 |
| 2026-11-15 | `docs/specs/scheduler.md` | C-054 方案 A 回写：`hasTierGap` 只比较 triggered 与**非 triggered 的 printable 关联点**；同题内 triggered 之间的档位差不计入；同步算法分层第 1 键 | C-054、D-02 |
| 2026-11-15 | `docs/prompts.md` | 34 处冲突模板同步：加入“对用户的实际影响”与“以用户影响为依据的简短选型参考”，要求少用内部变量名/类名 | 工作循环、R-06 |
| 2026-11-15 | `docs/conflicts-archive.md` | C-054 落地补充：规格回写与 prompts 模板同步 | C-054 |
| 2026-11-15 | `core/scheduler/src/main/kotlin/com/recollect/scheduler/DefaultScheduler.kt` | 时间装箱层：候选按第 2 层排序后按目标日分组，逐日 first-fit（剩余容量 ≥ 时长则排入，否则记 `TIME_OVERFLOW` 并继续尝试当日后续候选）；容量缺失按 0；`dailyAssignments` 只含非空日期；新增 `estimatedMinutes()` 把缺失/≤0 的时长归一为 15；装箱阶段不改候选顺序 | C-017、C-020、C-049、D-03 |
| 2026-11-15 | `core/scheduler/src/test/kotlin/com/recollect/scheduler/SchedulerPlanTest.kt` | 新增 S-05b（容量 30，20/20/10：第 2 条溢出后第 3 条仍排入）、S-15（缺失/0/-5 按 15，自定义 25 参与累加）、S-17（第 1 天溢出不占第 2 天容量，第 2 天候选不回填第 1 天余量）；S-05b、S-15 先红后绿，S-17 首跑即绿（断言未放宽） | S-05b、S-15、S-17、C-017、C-020 |
| 2026-11-15 | `docs/conflicts.md` | C-032 状态行更新：scheduler 现有 10 个测试方法，时间装箱层已落地（仅进度事实，不改裁决） | C-032、C-017、C-020 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加时间装箱轮次记录：目标、Observe、红绿证据、验证命令与结果、未决问题、下轮建议 | 工作循环 |
| 2026-11-15 | `core/scheduler/src/test/kotlin/com/recollect/scheduler/SchedulerPlanTest.kt` | 新增 S-13（逾期落到计划首日、只排一次、日期键升序、无条目日期不出现）、S-14（一题多知识点在范围内到期只排最早目标日一次、全部计入 triggered、范围外知识点作非触发）、S-16（List/Map 迭代顺序打乱后 `PlanResult` 完全相同，含冷却与溢出的 deferred 顺序）；三条均首跑即绿，用临时变异证明可捕获回归，断言未放宽 | S-13、S-14、S-16、D-01、C-020、C-049 |
| 2026-11-15 | `core/scheduler/src/main/kotlin/com/recollect/scheduler/DefaultScheduler.kt` | 无净改动：为红测证据临时变异三次（去掉逾期落首日夹取 / 目标日改取最晚到期日 / 去掉第 2 层排序），每次都还原；最终与轮初逐字一致 | S-13、S-14、S-16 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加日期范围与稳定输出轮次记录：目标、Observe、红绿证据（含临时变异）、验证命令与结果、未决问题、下轮建议 | 工作循环 |
| 2026-11-15 | `docs/conflicts.md` | C-032 状态行更新：scheduler 现有 13 个测试方法，日期范围与稳定输出已落地（仅进度事实，不改裁决） | C-032、S-13、S-14、S-16 |
| 2026-11-15 | `core/scheduler/src/main/kotlin/com/recollect/scheduler/DefaultScheduler.kt` | S-10：收到单题关联 > 4 个知识点时抛 `IllegalArgumentException`（`require` + 常量 4）；0 个关联仍因无到期知识点自然跳过、不进入候选；其余行为不变 | D-12、C-049、S-10 |
| 2026-11-15 | `core/scheduler/src/test/kotlin/com/recollect/scheduler/SchedulerPlanTest.kt` | 新增 S-10（5 个关联抛异常；0 个关联不进 `dailyAssignments`、不产生 deferred）与属性测试 `@RepeatedTest(100)` + `Random(20261113L)`（归档/休眠不出现、同日不重复、计划内至多一次、日期键严格升序）；属性测试首跑即绿，四条断言均用临时变异取得红证据后还原，断言未放宽 | S-10、D-11、D-12、C-049 |
| 2026-11-15 | `docs/conflicts.md` | C-032 状态行更新：scheduler 现有 14 个测试方法（含 1 个 100 次重复的属性测试），S-10 与属性测试已落地（仅进度事实，不改裁决） | C-032、S-10、D-12 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加 S-10 与属性测试轮次记录：目标、Observe、红绿证据（含四次临时变异）、验证命令与结果、未决问题、下轮建议 | 工作循环、S-10、D-12 |
| 2026-11-15 | `core/scheduler/src/main/kotlin/com/recollect/scheduler/DefaultScheduler.kt` | 收尾：`dateRange.start > endInclusive` 时短路返回全空 `PlanResult`，不抛异常、不生成冷却顺延（规格边界输入） | C-049、S-13 |
| 2026-11-15 | `core/scheduler/src/test/kotlin/com/recollect/scheduler/SchedulerPlanTest.kt` | 收尾补测：S-03（一题关联到期/未到期知识点只排一次、触发/非触发 id 正确）、S-11（非触发行打印、调度器不改写输入状态）、空日期范围、负容量、缺失容量、id 字典序、同因 deferred 排序、最高档位/组内最小分；属性测试改为每次 repetition 独立种子并新增守恒断言 | S-03、S-11、C-049、D-01、D-11、C-019 |
| 2026-11-15 | `docs/conflicts.md` | 新增 scheduler 收尾审计待裁决 C-055～C-059（悬空关联、重复 id、冷却非正数、校验优先级、档位差与 S-05 冲突）；C-032 更新为 22 个测试方法 | C-055～C-059、C-032、D-02、D-04、D-11、D-12 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加 scheduler 收尾轮：S-01～S-19 映射、红绿证据、验证结果、未决问题与下一轮建议（P-01） | 工作循环 |
| 2026-11-15 | `docs/specs/scheduler.md` | C-055～C-059 回写：新增 `DeferReason.UNRESOLVED_KNOWLEDGE`、输入唯一性前置条件、关联完整性、校验优先级、冷却 0/负数语义、排序键改为 `groupTier → hasTierGap`、S-04 措辞与 S-20～S-22 | C-055～C-059、D-02、D-04、D-11、D-12 |
| 2026-11-15 | `docs/specs/planning.md`、`docs/specs/settings.md`、`docs/specs/repository.md` | 同步冷却天数 0/负数语义与 data/room 的 id 唯一/关联完整前置条件 | C-056、C-057 |
| 2026-11-15 | `core/scheduler/src/main/kotlin/com/recollect/scheduler/Scheduler.kt` | `DeferReason` 新增 `UNRESOLVED_KNOWLEDGE` | C-055 |
| 2026-11-15 | `core/scheduler/src/main/kotlin/com/recollect/scheduler/DefaultScheduler.kt` | 裁决落地：冷却天数非负校验；归档题先跳过；关联 id 解析失败整题跳过并记 `UNRESOLVED_KNOWLEDGE`；排序键改为 `groupTier → hasTierGap` | C-055～C-059、D-02、D-04、D-11、D-12 |
| 2026-11-15 | `core/scheduler/src/test/kotlin/com/recollect/scheduler/SchedulerPlanTest.kt` | S-04 改为同档位组内区分用例；新增 C-059 高档位优先、S-20 关联失效、S-21 归档/空范围超限、S-22 冷却 0/负数 | C-055～C-059、S-04、S-20～S-22 |
| 2026-11-15 | `docs/conflicts.md`、`docs/conflicts-archive.md` | C-055～C-059 移出待裁决并归档裁决、落地与规格同步；C-032 更新为 27 个 `@Test` + 属性测试 | C-055～C-059、C-032 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加 C-055～C-059 裁决落地记录：红绿证据、验证结果、未决问题与下轮建议 | 工作循环 |
| 2026-11-15 | `core/scheduler/src/main/kotlin/com/recollect/scheduler/DefaultScheduler.kt` | 复核修正：冷却判定以 `coolingPeriodDays > 0` 短路，0 无条件关闭；移除 `linkedKnowledgeIds.distinct()`，与 C-056 A“不去重/合并”一致 | C-056、C-057 |
| 2026-11-15 | `core/scheduler/src/test/kotlin/com/recollect/scheduler/SchedulerPlanTest.kt` | 复核加强：S-22 用“实际复习日晚于目标日”锁定 0 无条件关闭并断言 1 天仍冷却；S-20 增加两个未解析 id 的字典序与容量充足对照；S-04 增加同档非触发点反例；新增 C-058 校验优先级用例 | C-055～C-059、S-04、S-20～S-22 |
| 2026-11-15 | `docs/specs/scheduler.md`、`docs/conflicts-archive.md` | 冷却公式补充 `coolingPeriodDays > 0` 短路；头部关联裁决补 C-054～C-059；注释同步 `UNRESOLVED_KNOWLEDGE`；C-056/C-057 落地措辞与代码一致 | C-056、C-057、C-059 |
| 2026-11-15 | `core/pagination/src/main/kotlin/com/recollect/pagination/TemplateGeometry.kt`（新增） | 落地模板几何默认参数与 6 个派生量：`usableHeightMm`=273、`firstPageUsableHeightMm`=265、`contentHeightMm`=266、`firstPageContentHeightMm`=258、`maxAttachmentHeightMm`=81.9、`qrFooterHeightMm`=21；宽度派生量留待后续用例 | D-07、D-08、C-025、C-027、P-19 |
| 2026-11-15 | `core/pagination/src/test/kotlin/com/recollect/pagination/TemplateGeometryTest.kt`（新增） | 锁定默认模板 6 个派生量（先红后绿）；默认几何下 P-05/P-11/P-13/P-14/P-18 不可达，后续必须显式传自定义 template | P-04、P-10、P-19、C-025、C-027 |
| 2026-11-15 | `docs/conflicts.md` | C-032 状态与影响行更新：core:pagination 已有 `TemplateGeometry` 与 1 个测试，PagePlanner 未实现（仅进度事实，不改裁决） | C-032、C-025、C-027 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加阶段 2 首轮（模板几何）目标、Observe、红绿证据、验证结果、未决问题与下轮建议 | 工作循环 |
| 2026-11-15 | `core/pagination/src/main/kotlin/com/recollect/pagination/RenderableQuestion.kt`（新增） | 落地打印投影 DTO：题号、知识点数、全称列表、右上视觉行数、附加数、笔记区开关 | D-07、D-08、C-013、C-015、C-027 |
| 2026-11-15 | `core/pagination/src/main/kotlin/com/recollect/pagination/PaginationResult.kt`（新增） | 落地 sealed `PaginationResult`（`Success`/`Failure`）、`PlannedPage`、`PlacedItem`、`ContinuationMarker`、`OverflowItem`、`PaginationWarning`、两个枚举；失败不混入 pages/warnings | C-047、C-048、C-016、C-025 |
| 2026-11-15 | `core/pagination/src/main/kotlin/com/recollect/pagination/PagePlanner.kt`、`DefaultPagePlanner.kt`（新增） | `PagePlanner` 接口与最小实现：每天一页、空日期不建页、按单题高度公式算高、按输入顺序放置（`yOffsetMm` 含首页图例与页级表头）；未实现移页/marker/overflow/附件上限/日期排序 | P-01、D-08、C-025、C-048 |
| 2026-11-15 | `core/pagination/src/test/kotlin/com/recollect/pagination/PagePlannerTest.kt`（新增） | 锁定 P-01：一日一条、高度 83.5mm、`pageId=plan-p01-01`、`pageNumber=1`、首条 `yOffsetMm=27`、无 marker/overflow/warnings（先红后绿） | P-01、D-08、C-047、C-048 |
| 2026-11-15 | `docs/conflicts.md` | C-032 状态与影响行更新：core:pagination 已有 `PagePlanner`、规格类型与 2 个测试（仅进度事实，不改裁决） | C-032、P-01 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加阶段 2 P-01 轮次记录：目标、Observe、红绿证据、验证结果、未决问题与下轮建议 | 工作循环、P-01 |
| 2026-11-15 | `core/pagination/src/main/kotlin/com/recollect/pagination/DefaultPagePlanner.kt` | P-03：按 C-048 逐页按容量放置；遇到第一条放不下的条目就结束当前页，该条及后续条目按原顺序进入下一页；被移条目在来源页各生成一个 `ContinuationMarker`（`toPageId` 取实际放置页）；首页用 `firstPageContentHeightMm`、后续页用 `contentHeightMm`；空白页也放不下的分支留待 P-14 | P-03、D-07、D-08、C-016、C-025、C-048 |
| 2026-11-15 | `core/pagination/src/test/kotlin/com/recollect/pagination/PagePlannerTest.kt` | 新增 P-03：四条 68.5mm 条目，第 4 条整条移第 2 页，第 1 页恰一个 `order=0` marker 且 from/to pageId 正确；不拆分、不压缩高度（先红后绿） | P-03、C-048 |
| 2026-11-15 | `docs/conflicts.md` | C-032 状态/影响行更新为 pagination 3 个测试、P-03 已落地（仅进度事实，不改裁决） | C-032、P-03 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加阶段 2 P-03 轮次记录：目标、Observe、红绿证据、验证结果、未决问题与下轮建议 | 工作循环、P-03 |
| 2026-11-15 | `core/pagination/src/main/kotlin/com/recollect/pagination/DefaultPagePlanner.kt` | P-08：按日期升序迭代 `dailyGroups`，输出与输入 Map 顺序无关；P-13：首页扣图例后放不下、但空白后续页放得下时，保留空首页并在下一页放置；类注释同步；单题高度公式未改 | P-08、P-13、D-08、C-025、C-048 |
| 2026-11-15 | `core/pagination/src/test/kotlin/com/recollect/pagination/PagePlannerTest.kt` | 新增 P-08（日期升序、每日本页起、页号全文档连续、同日多页 date 相同）、P-10（自定义模板 65/75mm、12 条 11mm：首页 5 条、后续页 6 条、第 3 页 1 条，同时锁定首页图例+表头与后续页表头扣除）、P-13（70mm 条目进不了首页 65mm、进得了后续页 75mm，首页保留为空且恰一个 marker）；P-10 首跑即绿，用三种临时变异取得红证据并全部还原 | P-08、P-10、P-13、D-07、D-08、C-001、C-025、C-048 |
| 2026-11-15 | `docs/conflicts.md` | C-032 状态/影响行更新：pagination 现有 6 个测试方法（P-01/P-03/P-08/P-10/P-13 + 模板几何），仅进度事实 | C-032、P-08、P-10、P-13 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加阶段 2 P-08/P-10/P-13 轮次记录：目标、Observe、红绿证据、独立复核、验证结果、未决问题与下轮建议 | 工作循环 |
| 2026-11-15 | `core/pagination/src/main/kotlin/com/recollect/pagination/DefaultPagePlanner.kt` | P-14/P-18：单题高于 `contentHeightMm` 时整条进入 `overflowItems(ITEM_TOO_TALL)`，不打印、不拆分/压缩、不生成 `ContinuationMarker`；剩余条目继续按 C-048 分页；`warnings` 由 overflow 按 `questionId` 字典序派生（`code` 取原因名）；判定改用 `partition` 一次求值 | P-14、P-18、D-07、D-08、C-016、C-025、C-027、C-048 |
| 2026-11-15 | `core/pagination/src/test/kotlin/com/recollect/pagination/PagePlannerTest.kt` | 新增 P-14（105mm 过高条目进 overflow、正常条目照常放置、无 marker）与 P-18（95mm 恰等于 `contentHeightMm` 可放后续页、100mm 恰等于 `usableHeightMm` 必须进 overflow）；抽出 `smallPageTemplate` 共用模板并修正注释中的 QR/间距算式 | P-14、P-18、C-016、C-025、C-048 |
| 2026-11-15 | `docs/conflicts.md` | C-032 进度更新为 pagination 8 个测试方法、P-14/P-18 已落地；新增 C-060（整日/整份计划条目全部过高时是否保留空白首页）待裁决 | C-032、C-060、D-07、D-08 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加阶段 2 P-14/P-18 轮次记录：目标、Observe、红绿证据（含临时变异）、独立复核、验证结果、未决问题与下轮建议 | 工作循环 |
| 2026-11-15 | `core/pagination/src/test/kotlin/com/recollect/pagination/PagePlannerTest.kt` | C-060 方案 B：新增锁定测试——全部条目过高的日期不建页、不占页号，也不输出仅含图例的空白页，只返回 overflow；正常日期仍是文档第 1 页并扣图例 | C-060、D-07、D-08、C-048 |
| 2026-11-15 | `docs/conflicts.md`、`docs/conflicts-archive.md` | C-060 移出未决并按方案 B 归档裁决、落地、测试与规格回写待授权事项；未决仅剩 C-032，pagination 进度更新为 9 个测试方法 | C-060、C-032 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加 C-060 裁决落地记录：行为核对、测试、验证结果与规格回写待授权事项 | 工作循环、C-060 |
| 2026-11-15 | `docs/specs/pagination.md` | C-060 方案 B 回写（获授权）：关联裁决加入 C-060；分页规则补充「全溢出日期不建页、整份计划无可打印条目时由计划层提示」；放置语义把“有任意条目”改为“有可打印条目”并补充全溢出规则 | C-060、D-07、D-08、C-048 |
| 2026-11-15 | `docs/conflicts-archive.md` | C-060 的“规格同步”由待授权更新为已回写，并注明改动位置 | C-060 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加 C-060 规格回写记录：改动位置、与锁定测试的一致性、验证结果 | 工作循环、C-060 |
| 2026-11-15 | `core/pagination/src/main/kotlin/com/recollect/pagination/DefaultPagePlanner.kt` | P-05/P-11：overflow 判定拆为「附件面积 > `usableHeightMm × 30%` → `ATTACHMENT_OVER_LIMIT`」与「单题高度 > `contentHeightMm` → `ITEM_TOO_TALL`」；溢出条目保持公式原值（不截断附件区），不打印、无 marker；抽出 `attachmentAreaMm` 供高度公式与上限判定共用；两条同时成立时的原因顺序未裁决（C-061），代码注释标明不得视为定案 | P-05、P-11、D-07、D-08、C-016、C-025、C-027、C-048、C-061 |
| 2026-11-15 | `core/pagination/src/test/kotlin/com/recollect/pagination/PagePlannerTest.kt` | 新增 P-05（自定义模板 usable=230、上限 69、每条附件 24：1/2 条打印且高度 84→108 递增，3 条 72 超限整条进入 `ATTACHMENT_OVER_LIMIT`，`heightMm=132` 不截断）与 P-11（自定义模板 usable=100、上限 30：2 条×15=30 恰好通过并完整打印 80mm；2 条×15.5=31 超 1mm 进入 overflow，`heightMm=81` 保留全额附加区，全溢出日期不建页）；P-11 首跑即绿，用两次临时变异取得红证据后还原 | P-05、P-11、C-027、C-060、C-048 |
| 2026-11-15 | `docs/conflicts.md` | 新增 C-061：附加笔记超限与整题过高同时成立时提示归属未定义，记录用户影响、三种方案、选型参考与裁决前禁止事项；C-032 进度更新为 pagination 11 个测试方法、附件上限已落地（仅进度事实，不改裁决） | C-061、C-032、C-027 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加阶段 2 P-05/P-11 轮次记录：目标、Observe、红绿证据（含临时变异）、验证结果、未决问题与下轮建议 | 工作循环、P-05、P-11 |
| 2026-11-15 | `core/pagination/src/main/kotlin/com/recollect/pagination/DefaultPagePlanner.kt` | C-061 方案 B 落地：`overflowReasonOf` 改为先判整题高度、再判附件面积；两条同时成立时归为 `ITEM_TOO_TALL`；类注释与判定注释同步裁决 | C-061、C-027、P-05、P-11、P-14、P-18 |
| 2026-11-15 | `core/pagination/src/test/kotlin/com/recollect/pagination/PagePlannerTest.kt` | 新增 `C-061 item both too tall and over attachment limit is reported as ITEM_TOO_TALL`：3 条附件 45mm 超限且整题 105mm 过高，断言归为 `ITEM_TOO_TALL`、`heightMm=105`、该日不建页；先红后绿 | C-061、P-14、P-18、C-060 |
| 2026-11-15 | `docs/conflicts.md`、`docs/conflicts-archive.md` | C-061 移出未决，按方案 B 归档裁决、落地、测试与规格回写待授权事项；未决仅剩 C-032，pagination 进度更新为 12 个测试方法 | C-061、C-032 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加 C-061 裁决落地记录：红绿证据、验证结果、未决问题与下轮建议 | 工作循环、C-061 |
| 2026-11-15 | `docs/specs/pagination.md` | C-061 方案 B 回写（获授权）：头部关联裁决加入 C-061；「单题高度公式」段新增「两条同时成立时按 `ITEM_TOO_TALL` 处理、只输出一条 overflow 记录、附加上限只在整题高度未超时判定」 | C-061、D-07、D-08、C-027 |
| 2026-11-15 | `docs/conflicts-archive.md` | C-061 的「规格同步」由待授权更新为已回写，注明改动位置 | C-061 |
| 2026-11-15 | `docs/conflicts.md` | 新增 C-062：不打印笔记书写区的题目是否仍受附加笔记上限约束；记录用户影响、三种方案、选型参考与裁决前禁止事项 | C-062、C-027、P-11、P-15 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加 C-061 规格回写与 `hasNoteArea=false` 现状分析记录 | 工作循环、C-061、C-062 |
| 2026-11-15 | `core/pagination/src/main/kotlin/com/recollect/pagination/DefaultPagePlanner.kt` | C-062 方案 B 落地：附加上限判定增加 `hasNoteArea` 前置；不打印笔记区时附加数量既不参与高度也不参与上限判定，且不被静默清零；类注释与判定注释同步 | C-062、C-027、P-05、P-11、P-15 |
| 2026-11-15 | `core/pagination/src/test/kotlin/com/recollect/pagination/PagePlannerTest.kt` | 新增 `C-062 attachment limit applies only when the note area is printed`：同一模板下打印笔记区的 81mm 题目因附件超限移出，不打印笔记区的 40mm 题目正常建页；先红后绿 | C-062、C-060、P-15 |
| 2026-11-15 | `docs/conflicts.md`、`docs/conflicts-archive.md` | C-062 移出未决，按方案 B 归档裁决、落地、测试与规格回写待授权事项；未决仅剩 C-032，pagination 进度更新为 13 个测试方法 | C-062、C-032 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加 C-062 裁决落地记录：红绿证据、验证结果、未决问题与下轮建议 | 工作循环、C-062 |
| 2026-11-15 | `docs/specs/pagination.md` | C-062 方案 B 回写（获授权）：头部关联裁决加入 C-062；「单题高度公式」段把附加上限改为仅在 `hasNoteArea=true` 时判定，`hasNoteArea=false` 时附加数量不参与上限判定且不得被静默清零 | C-062、C-027、D-07、D-08 |
| 2026-11-15 | `docs/conflicts-archive.md` | C-062 的「规格同步」由待授权更新为已回写，注明改动位置 | C-062 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加 C-062 规格回写记录 | 工作循环、C-062 |
| 2026-11-15 | `docs/prompts.md` | 修正 2.7「其余边界 + 属性测试」描述：背景补 C-060/C-061/C-062 已裁决；P-12/P-15/P-16/P-19 断言写清；P-15 明确 `hasNoteArea=false` 时附加上限不判定且附加数量不得清零；P-09 生成器覆盖 C-060/C-061/C-062；注明模板校验、`PLAN_TOO_LONG`、P-20 留到 2.8 | C-060、C-061、C-062、P-02、P-12、P-15、P-16、P-19、P-09 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加 prompts.md 2.7 条目修正记录 | 工作循环 |
| 2026-11-15 | `docs/prompts.md` | 依据 `conflicts-archive.md` 修正 2.8 及后续阶段的任务描述：2.8 覆盖 C-060/C-061/C-062、P-20、模板校验与 `PLAN_TOO_LONG`；3.x 补 C-050/C-053/C-055/C-056 与 `Failure` 处理；4.x 补 C-047/C-050/C-055/C-056/C-057 与唯一性/Float 验收；5.x 补 C-047/C-053/C-060/C-061/C-062 的映射与提示；6.x 补 C-038～C-041、C-050、C-052、C-060～C-062 与 C-051 回退门禁 | C-038～C-041、C-047、C-050、C-052、C-053、C-055～C-057、C-060～C-062 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加 prompts.md 2.8 及后续阶段修正记录 | 工作循环 |
| 2026-11-15 | `CLAUDE.md` | 按已裁决修正不符表述：D-02 补 C-054/C-059 的同档位组内排序与高低档顺序；D-04 由“默认 7 天”改为“固定 7 天（0 关闭、负数非法，C-057）”；D-12 补 C-056 的 id 唯一性归属；模块结构补 C-053 的 DTO 位置；文档索引标注 `preflight-report.md` 尚未创建；新增「决策落地后的文档同步」规则 | D-02、D-04、D-12、C-046、C-053、C-054、C-056、C-057、C-059 |
| 2026-11-15 | `docs/conflicts.md` | 未决文档缺口：`preflight-report.md` 条目改为“尚未创建，索引已标注” | 工作循环 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加 CLAUDE.md 一致性检查与同步规则记录 | 工作循环 |
| 2026-11-15 | `CLAUDE.md` | 决策同步规则补充 `docs/architecture/overview.md`：裁决后同时修订字段产权/多形态表，并在 changelog/agent-log 记录 | 工作循环 |
| 2026-11-15 | `docs/architecture/overview.md` | 修正过期状态与裁决口径：头部实现进度更新为 scheduler/pagination 已落地、data/feature/app 未落地；业务代码进度指向 C-032；`KnowledgeState`/`SchedulerQuestion` 标注 core:model（C-053）；`linkedKnowledgeIds` 与 id 唯一性标注 C-046/C-056 | C-032、C-046、C-053、C-056 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加 overview.md 一致性修正与规则补充记录 | 工作循环 |
| 2026-11-15 | `core/pagination/src/test/kotlin/com/recollect/pagination/PagePlannerTest.kt` | 新增 P-02（三条 76/83.5/98.5mm 恰好填满首页 258mm，最后一条底边落在下页边距 285mm）、P-12（逆序插入的三日输入与升序输入规划结果整体相等）、P-15（无笔记区 88.5→38.5mm、附件 31mm 超限不判定且条目照常打印）、P-16（行数 1..4 高度每行 +7.5mm、知识点数 2..4 高度不变）、P-19（QR 底栏 21→28mm 时高度精确 +7mm、笔记区仍为 30mm）；五个用例首跑即绿，均以临时变异取得红证据后全部还原 | P-02、P-12、P-15、P-16、P-19、D-07、D-08、C-016、C-025、C-027、C-048、C-062 |
| 2026-11-15 | `core/pagination/src/test/kotlin/com/recollect/pagination/PagePlannerPropertyTest.kt`（新增） | P-09 属性测试：`@RepeatedTest(100)` + 固定种子 `20261113 + 序号`，断言条目不跨页/不重叠/不越界、marker 与被移条目一一对应、overflow 不在 pages、每题恰好处理一次、`pageNumber` 连续、`pageId` 唯一、日期升序、warnings 一一对应；生成器按 repetition 轮换模板并显式构造 C-060/C-061/C-062 场景，`@AfterAll` 断言三类分支均被覆盖；以两个临时变异取得红证据（78/100、34/100 次失败）后还原 | P-09、D-08、C-048、C-060、C-061、C-062 |
| 2026-11-15 | `docs/conflicts.md` | C-032 状态/影响行更新：pagination 已有 17 个 `PagePlannerTest` 方法与 1 个属性测试（100 次重复），P-02/P-09/P-12/P-15/P-16/P-19 已落地；剩余模板校验、`PLAN_TOO_LONG` 与 P-20（仅进度事实，不改裁决） | C-032、P-02、P-09、P-12、P-15、P-16、P-19 |
| 2026-11-15 | `docs/prompts.md` | 2.7「其余边界 + 属性测试」标记 【done】 | 工作循环 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加阶段 2 P-02/P-12/P-15/P-16/P-19/P-09 轮次记录：目标、Observe、红绿证据（含临时变异）、验证结果、未决问题与下轮建议 | 工作循环 |
| 2026-11-15 | `core/pagination/src/main/kotlin/com/recollect/pagination/DefaultPagePlanner.kt` | 收尾验收门禁：模板几何非法返回 `Failure(INVALID_TEMPLATE_GEOMETRY)`（`usableHeightMm` 或 `firstPageContentHeightMm` 非正）；`P-20` 三类计数越界抛 `IllegalArgumentException`；页数超过 99 返回 `Failure(PLAN_TOO_LONG)`；KDoc 同步 | C-047、C-048、P-20、P-14、P-18 |
| 2026-11-15 | `core/pagination/src/test/kotlin/com/recollect/pagination/PagePlannerTest.kt` | 收尾补测：P-04（左侧行高随知识点数递增、表头只扣一次）、P-06（左侧更高取左侧）、P-07（右侧更高取右侧和）、P-17（重复规划稳定、分页器不读取/重排名称）、P-20（六种越界计数）、模板非法（零/负/NaN）与 >99 页两条验收门禁、默认模板守卫、C-048（空输入/空日期不建页）；P-04/P-06/P-07/P-17/C-048/默认模板守卫首跑即绿并用临时变异取得红证据，均还原；P-20/模板/`PLAN_TOO_LONG` 先红后绿 | P-04、P-06、P-07、P-17、P-20、C-047、C-048 |
| 2026-11-15 | `docs/conflicts.md` | C-032 更新为 pagination 收尾完成（26 个 `PagePlannerTest` + P-09 属性测试 + 模板几何，P-01～P-20 与验收门禁全覆盖）；新增收尾审计待裁决 C-063～C-069 与低影响观察 | C-032、C-047、C-048、C-060、C-061、C-062 |
| 2026-11-15 | `docs/prompts.md` | 2.8「模块收尾 + 规格审计」标记 【done】 | 工作循环 |
| 2026-11-15 | `docs/architecture/overview.md` | 头部实现进度由“pagination 已落地部分 P-01～P-20”改为“已覆盖 P-01～P-20 与验收门禁” | C-032、C-047、C-048 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加阶段 2 收尾轮次记录：P-01～P-20 映射、红绿证据（含临时变异）、审计与代码审查结论、验证命令与结果、未决问题、下轮建议 | 工作循环 |
| 2026-11-15 | `core/pagination/src/main/kotlin/com/recollect/pagination/PaginationResult.kt` | C-065 方案 B：新增稳定枚举 `PaginationWarningCode`（分页器两类 + 计划层预留 `KNOWLEDGE_TEXT_OVERFLOW`），`PaginationWarning.code` 改为枚举；C-063：`ContinuationMarker`/`continuationMarkers` 注明仅内部元数据、不再渲染“续下页” | C-063、C-064、C-065 |
| 2026-11-15 | `core/pagination/src/main/kotlin/com/recollect/pagination/DefaultPagePlanner.kt` | C-065：warning 代码由内部原因名改为稳定枚举映射；warnings 排序补 `code` 枚举序次键 | C-064、C-065 |
| 2026-11-15 | `core/pagination/src/test/kotlin/com/recollect/pagination/PagePlannerTest.kt` | 警告代码断言改为枚举；C-068 方案 A：新增“模板+计数同时非法”与“模板非法+空输入”断言，锁定模板校验优先 | C-065、C-068 |
| 2026-11-15 | `core/pagination/src/test/kotlin/com/recollect/pagination/PagePlannerPropertyTest.kt` | P-09 的 warning code 断言改为 `code.name`，与枚举映射一致 | C-065 |
| 2026-11-15 | `docs/specs/pagination.md` | 回写 C-063（marker 仅内部元数据）、C-064（`KNOWLEDGE_TEXT_OVERFLOW` 由计划层产出）、C-065（警告代码枚举与角色）、C-067（名称集合/数量由计划与数据层保证）、C-068（校验顺序）、C-069（`planId` 前置条件） | C-063～C-065、C-067～C-069 |
| 2026-11-15 | `docs/specs/print-template.md` | 回写 C-063：移除“续下页”，新增日期大标题与“本日共需复习 X 题”；C-064/C-067：文字溢出警告来源与名称数量一致性 | C-063、C-064、C-067 |
| 2026-11-15 | `docs/specs/planning.md` | 回写 C-064（合并文字溢出警告、去重与排序）、C-067（名称集合/数量）、C-069（`planId` 前置条件）、C-063（marker 仅内部元数据） | C-063、C-064、C-067、C-069 |
| 2026-11-15 | `docs/specs/repository.md`、`docs/specs/app.md` | C-067：映射保证 `knowledgeNames` 数量 = `knowledgeCount`；C-065：PlanPreview 只展示中文标签与文案，不展示内部代码 | C-065、C-067 |
| 2026-11-15 | `CLAUDE.md` | D-07 补名称集合一致性；D-08 改为“不再绘制续下页，每日第一页标注本日总题数”；D-09 补 `planId` 前置条件 | C-063、C-067、C-069 |
| 2026-11-15 | `docs/architecture/overview.md` | 多形态表与生命周期表同步：marker 仅内部元数据、每日总题数由打印层统计 | C-063 |
| 2026-11-15 | `docs/prompts.md` | 3.2/5.2/6.1 同步 C-063～C-067 的映射、警告合并与打印规则 | C-063～C-067 |
| 2026-11-15 | `docs/conflicts.md`、`docs/conflicts-archive.md` | C-063～C-065、C-067～C-069 移出未决并归档裁决、落地、测试与规格同步；C-066 保持未决（等待“拆分题目”解释后的裁决） | C-063～C-069 |
| 2026-11-15 | `docs/agent-log/2026-11-15.md` | 追加审计裁决落地记录：C-063～C-069 的改动、C-066 解释、验证结果与未决问题 | 工作循环 |
| 2026-11-17 | `core/pagination/src/main/kotlin/com/recollect/pagination/DefaultPagePlanner.kt` | C-066：整题过高提示改为“减少本题关联的知识点/缩短知识点描述”，附加超限提示改为“减少附加笔记”；删除“拆分题目”，原因归类不变 | C-061、C-066 |
| 2026-11-17 | `core/pagination/src/main/kotlin/com/recollect/pagination/TemplateGeometry.kt` | C-063：新增 `dateHeadingFontMm=5.0`、`dateHeadingHeightMm=7.0` 与派生 `dailyTotalFontMm=2.5`、`dailyTotalHeightMm=3.5`、`dateHeadingBlockHeightMm=10.5`；默认绘制在上边距内，不占内容区 | C-063 |
| 2026-11-17 | `core/pagination/src/test/kotlin/com/recollect/pagination/PagePlannerTest.kt` | C-066：P-14/C-061/P-05 增加文案断言（含减少知识点/缩短描述、不含“拆分题目”）；旧文案临时变异时 3/3 失败后还原 | C-066 |
| 2026-11-17 | `core/pagination/src/test/kotlin/com/recollect/pagination/TemplateGeometryTest.kt` | C-063：断言日期标题 5/7mm、总题数 2.5/3.5mm、合计 10.5mm ≤ 12mm 上边距 | C-063 |
| 2026-11-17 | `docs/specs/pagination.md` | C-066：两处提示文案去掉“拆分题目”；C-063：模板新增日期标题参数/派生量与上边距放置说明 | C-063、C-066 |
| 2026-11-17 | `docs/specs/print-template.md` | C-063：日期大标题 5/7mm、总题数 2.5/3.5mm、合计 10.5mm 绘制在上边距内；C-066 关联裁决 | C-063、C-066 |
| 2026-11-17 | `docs/prompts.md` | 5.2/6.1 同步 C-066 文案与 C-063 日期标题几何 | C-063、C-066 |
| 2026-11-17 | `docs/conflicts.md`、`docs/conflicts-archive.md` | C-066 移出未决并归档；C-063 补 X 确认与几何裁决；C-061 文案更新注记 | C-061、C-063、C-066 |
| 2026-11-17 | `docs/agent-log/2026-11-17.md`（新增） | 记录 C-066 与 C-063 几何裁决的改动、红绿证据、验证结果与未决问题 | 工作循环 |
| 2026-11-17 | `core/model/src/main/kotlin/com/recollect/model/KnowledgeRowsCodec.kt`（新增） | 阶段 3.1：新增 `KnowledgeRow`/`PlanPageRows` 与 `KnowledgeRowsCodec`；Kotlin 标准库手写紧凑 JSON 编解码，行序与触发列表原样保留；未新增依赖 | C-046 方案 A、C-015、C-053、C-070 |
| 2026-11-17 | `core/model/src/test/kotlin/com/recollect/model/KnowledgeRowsCodecTest.kt`（新增） | 阶段 3.1：用 `data-model.md` 的示例做 decode→encode 往返；断言 rows 数量/顺序、`triggeredKnowledgePointIds` 原样保留；`[5,3]` 解码后不按 number 重排 | C-015、C-046 |
| 2026-11-17 | `docs/conflicts.md` | 新增 C-070：`repository.md` 的记录字段未列题目标识、`data-model.md` 的示例含题目标识；记录用户影响、方案与建议，不代替裁决 | C-015、C-046 |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加阶段 3.1 轮次记录（目标、改动、红绿证据、验证、未决、下轮建议） | 工作循环 |
| 2026-11-17 | `docs/specs/repository.md` | C-070 方案 A（人类授权）：`PlanPageRows` 增加 `questionId`；补充行快照按题目记录、顺序冻结、不按 number 重排的说明 | C-070、C-015、C-046 |
| 2026-11-17 | `core/model/src/main/kotlin/com/recollect/model/KnowledgeRowsCodec.kt` | C-070 裁决后更新文件头注解，移除“repository.md 未列出该字段”的待裁决表述；代码行为不变 | C-070 |
| 2026-11-17 | `docs/conflicts.md`、`docs/conflicts-archive.md` | C-070 移出未决并归档：裁决、落地位置、测试与规格同步状态；未决恢复为仅 C-032 | C-070 |
| 2026-11-17 | `docs/prompts.md` | 3.1 标记【done】并补 C-070；3.2 的映射任务与 ModelMappingTest 要求补 `questionId`、rows 顺序保真 | C-070、C-015 |
| 2026-11-17 | `docs/architecture/overview.md` | 头部实现进度补 `core:model` 的 KnowledgeRowsCodec；多形态表增加 `PlanPage.knowledgeRows` ↔ `PlanPageRows` 行 | C-070、C-046 |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加 C-070 裁决落地记录 | C-070 |
| 2026-11-17 | `settings.gradle.kts` | 注册 `:data:room` 模块 | C-046 方案 A |
| 2026-11-17 | `data/room/build.gradle.kts`（新增） | Kotlin/JVM 17 模块，依赖 `core:model`/`core:scheduler`/`core:pagination`；本轮不接 Room 注解/KSP，留待阶段 4.1 | C-046 方案 A |
| 2026-11-17 | `data/room/.../entity/KnowledgePoint.kt`、`Question.kt`、`QuestionKnowledgeCrossRef.kt`、`PlanPage.kt`（新增） | 按 `data-model.md` 建立四个映射所需实体；`continuousScore` 为 `Float`（C-050） | C-046、C-050、C-015 |
| 2026-11-17 | `data/room/.../mapping/ModelMapping.kt`（新增） | Entity ↔ 纯 DTO：`toKnowledgeState`、`toSchedulerQuestion`（含 cross-ref 按 number 升序、唯一性校验、悬空 id 保留）、`toRenderableQuestion`（名称数量 = knowledgeCount）、`toPlanPageRows`（快照只解码、不重排） | C-046、C-050、C-055、C-056、C-067、C-070、C-015 |
| 2026-11-17 | `data/room/src/test/.../ModelMappingTest.kt`（新增） | 5 个用例：字段一一对应、difficulty/halfLifeDays=null、estimatedReviewMinutes<=0→15、continuousScore 为 Float、linkedKnowledgeIds 唯一且按 number 升序、名称数量 = knowledgeCount、questionId/rows 顺序保真 | C-046、C-050、C-056、C-067、C-070、C-015 |
| 2026-11-17 | `docs/conflicts.md` | 新增 C-071：`repository.md` 的行快照映射签名与数据来源不一致；记录用户影响、方案、建议与裁决前禁止事项；C-032 状态补 `data:room` 映射落地 | C-046、C-070、C-015 |
| 2026-11-17 | `docs/architecture/overview.md` | 头部实现进度补 `data:room` 映射落地，Room 注解/schema/DAO 待 4.1 | C-046、C-070 |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加阶段 3.2 记录：目标、改动、红绿证据、验证结果、未决问题、下轮建议 | 工作循环 |
| 2026-11-17 | `docs/conflicts.md`、`docs/conflicts-archive.md` | C-071 按方案 A 裁决并归档：未决恢复为仅 C-032；归档裁决、落地、测试与“规格同步待授权” | C-071、C-046、C-015、C-070 |
| 2026-11-17 | `docs/prompts.md` | 3.2 标记【done】；背景与任务改为 Room `PlanPage.knowledgeRows` → `List<PlanPageRows>`，只解码、不重排、不重生成 | C-071、C-015、C-070 |
| 2026-11-17 | `docs/architecture/overview.md` | 多形态表改为 `List<PlanPageRows>` 并标注 C-071 | C-071、C-046 |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加 C-071 裁决落地记录与待授权规格措辞清单 | C-071 |
| 2026-11-17 | `docs/conflicts.md` | 只读审计发现旧稿无快照时的重算条款与 C-071 方案 A 冲突，新增 C-072（旧稿是否回退重算），不代替裁决 | C-071、C-015 |
| 2026-11-17 | `docs/conflicts-archive.md` | C-071 归档的「规格同步」段改为自包含清单，并指向 C-072；修正 `overview.md` 已同步的过时说明 | C-071、C-072 |
| 2026-11-17 | `docs/prompts.md` | 3.2 的 `ModelMappingTest` 清单补「只解码 `PlanPage.knowledgeRows`、不从当前关联重新生成」 | C-071 |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加只读审计结果：A 类 10 处措辞、B 类 4 处行为条款、C-072 与验证命令 | C-071、C-072 |
| 2026-11-17 | `docs/specs/repository.md` | A 类授权回写：关联裁决补 C-015/C-070/C-071/C-072；小节标题补 C-071；映射签名改为 `PlanPage.toPlanPageRows(): List<PlanPageRows>`；行快照补只解码/不重生成/不重排/不合并；`knowledgeRows` 空或不可解析按 C-072 阻断；验收补题目标识与顺序保真 | C-071、C-072、C-015、C-070 |
| 2026-11-17 | `docs/specs/data-model.md` | A 类授权回写：关联裁决、`knowledgeRows` 字段注释、映射说明、快照说明；C-072：迁移条目禁止按当前关联重算，无快照旧稿阻断并提示重印 | C-071、C-072、C-070 |
| 2026-11-17 | `docs/specs/print-template.md` | C-072：没有快照的旧稿不得重算，回读/确认阻断并提示重新生成计划、重印 | C-072、C-009、C-015 |
| 2026-11-17 | `docs/specs/scan-pipeline.md` | C-072：回读只读 `PlanPage.knowledgeRows`；无快照不创建 `PendingFeedback`、不进入确认页；验收改「只使用快照、不回退重算」 | C-072、C-071、C-015 |
| 2026-11-17 | `docs/specs/confirm-flow.md` | C-072：`knowledgeRows` 为空或不可解析视为无快照，阻断本页与提交 | C-072 |
| 2026-11-17 | `docs/conflicts.md`、`docs/conflicts-archive.md` | C-072 按方案 A 裁决并归档；未决恢复为仅 C-032；C-071 的规格同步状态改为已授权回写 | C-071、C-072 |
| 2026-11-17 | `docs/prompts.md` | 3.2 背景补 C-072 方案 A：无快照旧稿不可回读、禁止按当前关联重算 | C-072 |
| 2026-11-17 | `docs/architecture/overview.md` | 生命周期表补无快照旧稿不可回读；多形态表标注 C-072 | C-072、C-071 |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加 C-072 裁决落地记录：规格回写位置、关键口径、验证与未决 | C-072 |
| 2026-11-17 | `data/room/src/test/kotlin/com/recollect/data/room/FirstVerticalSliceTest.kt`（新增） | 阶段 3.3：首个跨模块纵切（core:model DTO → data:room 映射 → scheduler → pagination）；2 个知识点、2 道题（q1 关联 k1/k2，q2 关联 k2）→ 1 页 2 条、`pageId={planId}-01`、D-01 同一题只出现一次、D-08 按日分页且无题日期不建页；`Failure` 时按 C-047 记录 code/message 并让测试失败；测试首跑即绿，以两处临时变异取得红证据后原样还原 | D-01、D-08、C-046、C-047、C-053、C-067 |
| 2026-11-17 | `docs/conflicts.md` | 新增 C-073：打印题目投影类型的模块归属在 `repository.md` 与 `pagination.md`/实现间不一致；无用户可见影响，记录方案与建议；C-032 状态补纵切测试 | C-046、C-053 |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加阶段 3.3 轮次记录：目标、Observe、红绿证据、验证、未决与下轮建议 | 工作循环 |
| 2026-11-17 | `docs/specs/repository.md` | C-073 方案 A：关联裁决与小节标题补 C-073；`RenderableQuestion` 移出 `// core:model` 代码块并标注 `// core:pagination`；映射段说明由 `core:pagination` 提供、`core:model` 不重复定义同名类型 | C-073、C-046、C-053 |
| 2026-11-17 | `CLAUDE.md` | 模块约束补：打印题目投影 `RenderableQuestion` 属于 `core:pagination`，不在 `core:model` 重复定义 | C-073 |
| 2026-11-17 | `docs/architecture/overview.md` | 多形态表标注 `RenderableQuestion` 在 `core:pagination`（C-073 方案 A），核心模型模块不重复定义 | C-073 |
| 2026-11-17 | `docs/prompts.md` | 3.3 标记【done】；背景补 C-073 方案 A；任务写明从 `core:pagination` 引用；目标测试命令改为 `:data:room:test` | C-073 |
| 2026-11-17 | `docs/conflicts.md`、`docs/conflicts-archive.md` | C-073 按方案 A 裁决并归档：未决恢复为仅 C-032；归档裁决、落地、测试与规格同步 | C-073 |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加 C-073 裁决落地记录 | C-073 |
| 2026-11-17 | `data/room/build.gradle.kts` | 阶段 4.1：转为 Android 库（AGP + Kotlin Android + KSP + `de.mannodermaus.android-junit5`），接入 Room 2.7.2 与 schema 导出；继续依赖三个 core 模块；未新增版本目录外依赖 | C-046 方案 A、C-052 方案 C |
| 2026-11-17 | `data/room/src/main/AndroidManifest.xml`（新增） | Android 库最小清单（namespace 在构建脚本） | 阶段 4.1 |
| 2026-11-17 | `data/room/.../entity/`（4 个既有实体加注解，新增 `ReviewFeedback`、`ConfirmationDecisionLog`、`PendingFeedback`、`ScanSession`、`CoolingOverrideEvent`） | 按 data-model.md 加 `@Entity`/`@PrimaryKey`/`@Index`；`knowledge_point.number` 与 `plan_page.pageId` 唯一索引；交叉表/待确认/页表复合主键；`continuousScore`/`effectiveFeedbackCount` 为 `Float` | D-11、D-12、D-13、C-050、C-056 |
| 2026-11-17 | `data/room/.../RoomTypeConverters.kt`（新增） | Room 2.7.2 未内置 `java.time` 转换器：`LocalDate` ↔ ISO-8601 TEXT、`Instant` ↔ epoch 毫秒 INTEGER | 阶段 4.1 |
| 2026-11-17 | `data/room/.../RecollectDatabase.kt`（新增） | `@Database` v1、9 个实体、`exportSchema = true`；禁止 destructive migration | D-11、C-056 |
| 2026-11-17 | `data/room/.../dao/*.kt`（9 个新增） | 9 个实体的最小 DAO：可打印知识点、打印快照、归档、关联去重、待确认替换、会话复用/过期、覆盖事件消费、页快照查询 | C-056、C-022、C-040、C-008、C-014 |
| 2026-11-17 | `data/room/.../migration/SchemaMigrationRules.kt`、`RecollectMigrations.kt`（新增） | 唯一索引迁移守卫：重复 `number`/`pageId` 直接失败并提示，禁止静默重编号；迁移清单为空（v1 首建） | D-12、C-056 |
| 2026-11-17 | `data/room/src/test/.../schema/RoomSchemaTest.kt`、`MiniJson.kt`（新增） | schema 测试：9 张表的字段名/类型/可空性/主键/索引与 data-model.md 一一对应；锁定两个唯一索引、交叉表复合主键、`continuousScore`/`effectiveFeedbackCount` 为 REAL | C-050、C-056、D-12 |
| 2026-11-17 | `data/room/src/test/.../migration/SchemaMigrationRulesTest.kt`（新增） | 重复 `number`/`pageId` 抛错且消息含“重复/不得静默重编号”；唯一值不抛错 | D-12、C-056 |
| 2026-11-17 | `data/room/schemas/com.recollect.data.room.RecollectDatabase/1.json`（生成） | Room KSP 导出的 v1 schema，作为 schema 测试与后续迁移的事实来源 | 阶段 4.1 |
| 2026-11-17 | `docs/architecture/overview.md` | 头部实现进度：`data:room` 已落地 Room 实体/schema/DAO，余 4 个实体随对应 UseCase 补齐 | 阶段 4.1 |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加阶段 4.1 轮次记录：目标、Observe、红绿证据、验证、未决与下轮建议 | 工作循环 |
| 2026-11-17 | `data/room/.../repository/TransactionRunner.kt`（新增） | 事务执行器接口与 Room `withTransaction` 实现；Repository 的跨表写操作统一从一个入口进入事务 | C-052 方案 C |
| 2026-11-17 | `data/room/.../repository/PlanDraft.kt`（新增） | 计划持久化草稿（含冻结行快照）与 `PlanGenerationException`；`fromPagination` 在进入事务前把 `PaginationResult.Failure` 转异常（C-047）；签名缺项见 C-074 | C-047、C-071、C-072 |
| 2026-11-17 | `data/room/.../repository/PlanRepository.kt`（新增） | `savePlan` 单事务：写 `PlanPage`、`Question.lastScheduledDate`、消费 `CoolingOverrideEvent`、按调度结果原样应用冷却顺延；写库前校验页 id 唯一、题目只出现一次、计划日与行快照齐全（C-056）；不改写冷却天数/不夹取（C-057） | D-01、D-04、C-047、C-056、C-057 |
| 2026-11-17 | `data/room/.../repository/PlanPageJson.kt`（新增） | `plan_page.questionIds` 与 `continuationMarkers` 的 JSON 序列化；`knowledgeRows` 仍由 `KnowledgeRowsCodec` 编码 | C-015、C-016、C-046 |
| 2026-11-17 | `data/room/.../repository/ConfirmedDecision.kt`、`FeedbackRepository.kt`（新增） | `saveDecisions` 单事务：写 `ReviewFeedback`、`ConfirmationDecisionLog`、更新 `KnowledgePoint`（`continuousScore` 以 `Float` 落库）、写 `NoteAttachmentItem`、更新 `Question.lastReviewedDate`、按结果删除 `PendingFeedback`；关联失效整题跳过并返回可见清单、不补默认知识点（C-055）；失败整体回滚、待确认行保留 | D-01、D-13、C-050、C-055、C-056 |
| 2026-11-17 | `data/room/.../entity/NoteAttachmentItem.kt`、`dao/NoteAttachmentItemDao.kt`（新增） | 补 `note_attachment_item` 实体与 DAO（下一展示顺序、最近 3 条）；`saveDecisions` 事务需要 | C-026 方案二 |
| 2026-11-17 | `data/room/.../RecollectDatabase.kt`、`migration/RecollectMigrations.kt` | schema 升到 v2：登记第 10 个实体并新增 v1→v2 迁移（建 `note_attachment_item`），仍不允许 destructive migration | D-12、C-056 |
| 2026-11-17 | `data/room/src/test/.../repository/*`（新增） | fake DAO + 可回滚事务执行器；`PlanRepositoryTransactionTest` 2 例（成功全写 + 任一步失败全回滚、C-047 事务前失败）、`FeedbackRepositoryTransactionTest` 2 例（成功全写 + 失败回滚且 `PendingFeedback` 保留、C-055 整题跳过可见记录）；先红后绿 | C-047、C-050、C-055、C-056、C-057 |
| 2026-11-17 | `data/room/src/test/.../schema/RoomSchemaTest.kt`、`schemas/.../2.json`（生成） | schema 测试锁定 v2 与 `note_attachment_item` 字段/主键；Room 导出 v2 schema | 阶段 4.2 |
| 2026-11-17 | `docs/conflicts.md` | 新增 C-074：Repository 接口签名缺少行快照/状态与复习日输入、计划异常归属、跳过记录出口；无当前用户可见影响，待裁决类型归属 | C-047、C-055、C-071、C-072 |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加阶段 4.2 轮次记录：目标、Observe、红绿证据、验证、未决与下轮建议 | 工作循环 |
| 2026-11-17 | `CLAUDE.md` | 按「无需决策的一轮完成后的文档同步」执行收尾（该小节由用户定义，本轮未改其内容）：同步 prompts/overview/changelog/agent-log；未改 specs 与 D-01～D-13 | 工作循环 |
| 2026-11-17 | `docs/prompts.md` | 4.2 标记【done】并补充落地状态与 C-074；4.3a/4.3b/4.3c 背景同步 `saveDecisions` 实际签名（UseCase 计算状态、Repository 只落库、返回跳过清单） | C-050、C-055、C-074 |
| 2026-11-17 | `docs/architecture/overview.md` | 头部实现进度更新为 10 个实体、schema v2（含 v1→v2 迁移）与 `savePlan`/`saveDecisions` 单事务 Repository | 阶段 4.2 |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加阶段 4.2 收尾同步记录：按 CLAUDE.md「无需决策的一轮完成后的文档同步」回写 prompts/overview，未改代码与测试 | 工作循环 |
| 2026-11-17 | `docs/conflicts.md`、`docs/conflicts-archive.md` | C-074 按方案 A 裁决并归档：数据层草稿/提交输入显式补齐行快照、状态、复习日与笔记附件，提交结果返回跳过清单；未决恢复为仅 C-032 | C-047、C-055、C-071、C-072 |
| 2026-11-17 | `docs/prompts.md` | 4.2 落地说明与 4.3a/4.3b/4.3c 背景由「待 C-074 裁决」改为「C-074 方案 A」 | C-074 |
| 2026-11-17 | `CLAUDE.md` | 模块约束补：`PlanDraft` 与 `PlanGenerationException` 放在 `data:room`，编排层只构造入参与展示 | C-074 方案 A |
| 2026-11-17 | `docs/architecture/overview.md` | 多形态表标注 `PlanDraft` 归属 `data:room`（C-074 方案 A） | C-074 方案 A |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加 C-074 裁决落地记录：归档、代码落地位置、测试与规格同步待授权 | C-074 |
| 2026-11-17 | `docs/specs/repository.md` | 授权回写 C-074 方案 A：关联裁决补 C-074；`saveDecisions` 签名补状态/复习日/笔记附件与 `SaveDecisionsResult`；新增 `PlanDraft`/`PlanGenerationException`/`NoteAttachmentDraft`/`SaveDecisionsResult` 与归属说明；事务规则与验收同步 | C-074 方案 A、C-047、C-055、C-050 |
| 2026-11-17 | `docs/specs/planning.md` | 授权回写 C-074 方案 A：关联裁决补 C-074；`PlanDraft` 增加 `knowledgeRowsByQuestionId` 并标注定义在 data:room；`PlanGenerationException` 归属；固定流程与验收同步 | C-074 方案 A、C-071、C-072 |
| 2026-11-17 | `docs/conflicts-archive.md` | C-074 规格同步状态由「待授权」改为「已授权回写」，记录两份规格的回写内容 | C-074 方案 A |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加 C-074 规格回写记录 | C-074 方案 A |
| 2026-11-17 | `data/room/.../usecase/ApplyFeedbackUseCase.kt`（新增） | 阶段 4.3a：U-01～U-05。触发行 ACCEPT/CORRECTED 全量更新档位/连续掌握度/有效反馈计数/下次到期日；非触发行按该行档位增量 ×0.3；`NOT_REVIEWED` 只写决定日志、不改状态；`continuousScore` 内部 `Double`、边界转 `Float`（C-050）；写入统一走 `saveDecisions`（C-074 方案 A） | D-01、D-03、D-05、D-13、C-050、C-074 |
| 2026-11-17 | `data/room/.../usecase/NextDueCalculator.kt`（新增） | `MemoryState`/`NextDueResult`/`NextDueCalculator` 接口与冷启动实现：按反馈事件序号 1/3/7 天推进；DHP、3.0 切换与记忆状态初始化留给 4.3b（U-06～U-12） | D-05、C-050 |
| 2026-11-17 | `data/room/.../repository/KnowledgePointRepository.kt`（新增） | 状态更新所需的只读端口：`KnowledgePointRepository.states()` 与 `ReviewFeedbackHistoryRepository.triggeredCount()`（非触发行不推进冷启动）；Repository 仍只落库、不重算状态 | C-074 方案 A、D-05 |
| 2026-11-17 | `data/room/src/test/.../usecase/ApplyFeedbackUseCaseTest.kt`（新增） | U-01～U-05 共 5 个 JUnit 5 用例；U-01/U-02/U-03/U-05 先红后绿，U-04 由 U-03 的非触发路径覆盖、单测首跑即绿；断言未放宽，`Float` 期望值按 `Double` 同序计算 | D-01、D-03、D-05、D-13、C-050 |
| 2026-11-17 | `docs/conflicts.md` | 新增 C-075：反馈规格的状态更新用例接口（三仓储注入、返回反馈行号）与 C-074 的单一事务提交入口不一致；无用户可见影响，待裁决后回写规格 | C-074 方案 A、R-04、D-13 |
| 2026-11-17 | `docs/prompts.md` | 4.3a 标记【done】，补落地说明、冷启动/ DHP 边界与 C-075 待裁决 | 阶段 4.3a、C-075 |
| 2026-11-17 | `docs/architecture/overview.md` | 头部实现进度补 `ApplyFeedbackUseCase` U-01～U-05（阶段 4.3a）与 C-075 | 阶段 4.3a |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加阶段 4.3a 轮次记录：目标、Observe、红绿证据、验证命令与结果、未决与下轮建议 | 工作循环 |
| 2026-11-17 | `docs/specs/feedback-usecase.md` | C-075 方案 A 授权回写：关联裁决补 C-075；接口段改为读端口（`states()`/`triggeredCount()`）+ 单事务 `saveDecisions`，去掉独立时钟；`ApplyResult` 改为携带 `SaveDecisionsResult` 与更新后状态；处理顺序注明步骤 1/4 与状态落库、删除待确认由数据层事务执行 | C-075 方案 A、C-074 方案 A、D-13 |
| 2026-11-17 | `docs/conflicts.md`、`docs/conflicts-archive.md` | C-075 按方案 A 裁决并归档；未决恢复为仅 C-032；归档记录落地、测试与规格同步状态，并记录 `:data:test` 命令修正项（同日授权后已修正） | C-075 方案 A |
| 2026-11-17 | `docs/prompts.md` | 4.3a 落地说明改为已按 C-075 方案 A 回写；4.3b/4.3c 背景补 `ApplyResult` 口径；4.3a/4.3b/4.3c/4.4 的验证命令由不存在的 `:data:test` 修正为 `:data:room:test` | C-075 方案 A |
| 2026-11-17 | `CLAUDE.md` | 模块约束补：确认提交状态在 `data:room` 的 `ApplyFeedbackUseCase` 计算并转 `Float`，写入/决定日志/删除待确认由 `saveDecisions` 单事务负责，`ApplyResult` 携带 `SaveDecisionsResult` | C-075 方案 A |
| 2026-11-17 | `docs/architecture/overview.md` | 头部实现进度补 C-075 方案 A 的接口口径 | C-075 方案 A |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加 C-075 裁决落地记录：归档、规格回写、验证与 `:data:test` 命令说明 | C-075 方案 A |
| 2026-11-17 | `docs/specs/feedback-usecase.md`、`docs/specs/entry.md` | 授权修正验收命令：`./gradlew :data:test` 改为 `./gradlew :data:room:test`（仓库只有 `:data:room` 项目，无 `:data`）；`docs/conflicts-archive.md` 与 `docs/agent-log/2026-11-17.md` 的「待授权」状态同步为已完成 | C-075 方案 A |
| 2026-11-17 | `data/room/.../usecase/NextDueCalculator.kt` | 新增 `TriggeredFeedback`、`MemoryStateInitializer`/`DefaultMemoryStateInitializer` 与 `MaimemoNextDueCalculator`：d_init/h_init、MASTERED/FAMILIAR/UNKNOWN 的 h/d 更新、90% 保持率 + 30 天上限；`NextDueCalculator` 以带默认值的 `actualIntervalDays` 表达 C-037 的实际间隔（规格接口段未列该入参，待授权同步） | D-05、C-018、C-037、C-050 |
| 2026-11-17 | `data/room/.../repository/KnowledgePointRepository.kt` | `ReviewFeedbackHistoryRepository` 新增 `recentTriggered(limit)` 与 `TriggeredFeedbackRecord`；按复习日升序返回 triggered 历史，供 DHP 初始化与实际间隔计算；不向上暴露 Room 实体 | C-037、C-074 方案 A |
| 2026-11-17 | `data/room/.../usecase/ApplyFeedbackUseCase.kt` | 4.3b U-06～U-12：第 3 条 triggered 反馈后初始化 `difficulty/halfLifeDays`；事件序号 >3 时按实际间隔更新 DHP；切换阈值精确 `effectiveFeedbackCountBefore >= 3.0f`，达到 3.0 的本次仍按冷启动 7 天；历史不足时按当前档位重复 + 1/3/7 兜底 | D-01、D-03、D-05、C-006、C-018、C-037、C-050 |
| 2026-11-17 | `data/room/src/test/.../usecase/ApplyFeedbackUseCaseTest.kt` | 新增 U-06/U-07/U-08/U-09/U-10/U-11/U-12 共 7 个 JUnit 5 用例；U-06 编译红、U-07 断言红、U-12 断言红后转绿；U-08/U-09/U-10/U-11 由既有实现或 U-12 修复覆盖、首跑即绿；断言未放宽，DHP 到期日按存储的 `halfLifeDays` 同序计算 | D-01、D-03、D-05、D-13、C-050 |
| 2026-11-17 | `docs/prompts.md` | 4.3b 标记【done】，补落地说明与规格接口待同步项 | 阶段 4.3b |
| 2026-11-17 | `docs/architecture/overview.md` | 头部实现进度补 U-06～U-12（冷启动 + DHP 切换） | 阶段 4.3b |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加阶段 4.3b 轮次记录：目标、Observe、红绿证据、验证、未决与下轮建议 | 工作循环 |
| 2026-11-17 | `docs/conflicts.md` | 「未决文档缺口（非冲突）」补 `NextDueCalculator` 实际间隔入参待规格同步；不新增待裁决条目 | C-037 |
| 2026-11-17 | `docs/specs/feedback-usecase.md` | 授权回写接口段：`NextDueCalculator` 增加 `actualIntervalDays`；`ReviewFeedbackHistoryRepository` 增加 `recentTriggered()` 与 `TriggeredFeedbackRecord`；处理顺序与 C-037 间隔来源明确为 `reviewedOn`，并注明间隔入参 | C-037、C-018、D-05 |
| 2026-11-17 | `docs/conflicts.md` | 移除「未决文档缺口」中的实际间隔入参待同步条目（已授权回写）；仍不新增待裁决条目 | C-037 |
| 2026-11-17 | `docs/prompts.md` | 4.3b 落地说明改为「规格接口段已授权回写」并补充回写内容 | 阶段 4.3b、C-037 |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加规格授权回写记录：改动范围、未改代码、验证沿用 4.3b 结果 | C-037 |
| 2026-11-17 | `data/room/.../usecase/ApplyFeedbackUseCase.kt` | 4.3c U-13：同一提交内同一知识点被多道题触发时，事件序号与 DHP 历史累计本批已处理的触发行，不再两行共用「库内历史 + 1」 | D-05、C-018、C-037 |
| 2026-11-17 | `data/room/src/test/.../usecase/ApplyFeedbackUseCaseTest.kt` | 新增 U-13/U-15/U-16/U-17/U-18/U-19 与「多行提交中途失败整体回滚、待确认行保留」共 7 个 JUnit 5 用例；U-13 断言红后转绿，其余首跑即绿并用断言锁定既有实现 | D-01、D-03、D-05、D-13、C-050、C-057、C-074 |
| 2026-11-17 | `data/room/src/test/.../repository/PlanRepositoryTransactionTest.kt` | 新增 U-14：用真实 `DefaultScheduler` 驱动连续两份计划各顺延 1 天，并覆盖 C-057 的 0=关闭冷却、负数非法；顺延按 `repository.md` 由 `savePlan` 事务承载 | D-04、C-021、C-057 |
| 2026-11-17 | `docs/conflicts.md` | 新增 C-076：冷却顺延写入位置在 `feedback-usecase.md`（独立用例）与 `repository.md`（计划保存事务）之间不一致；无用户可见差异，待裁决 | C-021、C-057、D-04 |
| 2026-11-17 | `docs/prompts.md` | 4.3c 标记【done】，补落地说明、U-13 事件序号修复、U-14 落点与 C-076 | 阶段 4.3c、C-076 |
| 2026-11-17 | `docs/architecture/overview.md` | 头部实现进度补 U-13～U-19（阶段 4.3c）与 U-14 落点/C-076 | 阶段 4.3c、C-076 |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加阶段 4.3c 轮次记录：目标、Observe、红绿证据、验证命令与结果、未决问题、下轮建议 | 工作循环 |
| 2026-11-17 | `docs/conflicts.md`、`docs/conflicts-archive.md` | C-076 按方案 A 裁决并归档：冷却顺延由 `savePlan` 计划保存事务写入，不另设独立顺延用例；未决恢复为仅 C-032 | C-076 方案 A、C-021、C-057 |
| 2026-11-17 | `docs/specs/feedback-usecase.md` | 授权回写 C-076 方案 A：关联裁决补 C-076；不另设顺延用例；「冷却顺延应用」改为 `savePlan` 单事务并补失败回滚；U-14 断言补事务口径 | C-076 方案 A、C-021 |
| 2026-11-17 | `docs/specs/scheduler.md` | 授权回写：关联裁决补 C-076；状态更新规则改为规划层放入 `PlanDraft`、由 `savePlan` 事务应用顺延 | C-076 方案 A |
| 2026-11-17 | `docs/specs/planning.md` | 授权回写：关联裁决补 C-076；职责与固定流程第 5 步标注冷却顺延在同一事务内落库 | C-076 方案 A |
| 2026-11-17 | `docs/specs/repository.md` | 授权回写：关联裁决补 C-076；事务规则注明冷却顺延不另设独立 UseCase | C-076 方案 A |
| 2026-11-17 | `CLAUDE.md` | D-04 摘要与模块约束补：冷却顺延由计划保存事务原样写入，不另设独立顺延用例 | C-076 方案 A |
| 2026-11-17 | `docs/architecture/overview.md` | 字段产权 `nextDueDate` 写入者改为 `ApplyFeedbackUseCase / PlanRepository.savePlan`；生命周期与实现进度同步 C-076 方案 A | C-076 方案 A |
| 2026-11-17 | `docs/prompts.md` | 4.3c 落地说明由「C-076 待裁决」改为「C-076 方案 A 已裁决并回写规格」 | C-076 方案 A |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加 C-076 裁决落地记录：归档、规格回写范围与验证沿用 | C-076 方案 A |
| 2026-11-17 | `data/room/src/test/.../usecase/ApplyFeedbackUseCaseTest.kt` | 阶段 4.4 收尾：U-05 由“触发行 NOT_REVIEWED”改为用例表要求的“触发行 A 已填涂 + 非触发行 B 本次未复习”，补 B 状态完全不变、两行都写决定日志、A 正常推进与题目复习日断言；临时变异（取消 NOT_REVIEWED 跳过）使 U-05/U-10/U-13/U-19 共 4 个用例变红，还原后全绿 | D-13、C-010、C-050 |
| 2026-11-17 | `docs/conflicts.md` | 阶段 4.4 只读审计：新增 C-077～C-089（②代码做了但规格未定义、③测试断言与规格不一致、④需人类裁决），均含用户影响、方案、选型参考、建议与裁决前禁止事项；未改 specs | C-055、C-018、C-037、C-050、D-05、D-13、R-04 |
| 2026-11-17 | `docs/architecture/overview.md` | 头部实现进度补 4.4 收尾：U-01～U-19/C-050 复验、U-05 场景补齐、审计新增 C-077～C-089 | 阶段 4.4 |
| 2026-11-17 | `docs/prompts.md` | 4.4 标记【done】，补落地说明、验证结论与审计去向 | 阶段 4.4 |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加阶段 4.4 记录：目标、U-01～U-19 核对表、C-050 复验、红绿证据、验证结果、审计 ① 清单、代码审查三档、未决与下轮建议 | 工作循环 |
| 2026-11-17 | `data/room/.../repository/ConfirmedDecision.kt` | C-083 方案 C：新增共享的 `effectiveTier()`，UseCase 与写入入口共用同一档位解析 | C-083 方案 C、C-022 |
| 2026-11-17 | `data/room/.../repository/QuestionLookupRepository.kt`（新增） | C-079 方案 A：提交前校验“知识点属于该题”的只读端口；题目不存在返回 null，交由整题跳过 | C-079 方案 A、C-055 |
| 2026-11-17 | `data/room/.../repository/PlanWeekStartProvider.kt`（新增） | C-081 方案 A：计划周起始日端口，默认周一；确认提交按“计划日所在周 + 设置”计算复习日下界 | C-081 方案 A |
| 2026-11-17 | `data/room/.../repository/FeedbackRepository.kt` | C-077～C-080 方案 A：新增 `pending()`/`session()` 只读入口；跳过行仍写确认日志；只落库未跳过题目引用的状态；整批成功标记会话 SUBMITTED，有跳过则保留 READY；前置校验抛 `InvalidConfirmationException` | C-077～C-080 方案 A、C-055、C-089 方案 A |
| 2026-11-17 | `data/room/.../usecase/ApplyFeedbackUseCase.kt` | C-079/C-080/C-081/C-082 方案 A：注入时钟、待确认/题目关联/周起始端口；完成空/重复/会话/行集合/计划日/触发标记/知识点归属/复习日上下界/档位校验；跳过题目不贡献状态；迁移窗口首条间隔取前一条复习日 | C-077～C-082 方案 A、C-084 方案 A |
| 2026-11-17 | `data/room/.../usecase/NextDueCalculator.kt` | C-084 方案 A：`actualIntervalDays` 去掉默认值成为必填；C-085 方案 B：移除生产源集的 `ColdStartNextDueCalculator` | C-084 方案 A、C-085 方案 B |
| 2026-11-17 | `data/room/src/test/.../usecase/ColdStartNextDueCalculator.kt`（新增） | C-085 方案 B：冷启动对照实现移到测试源集，不进入生产装配 | C-085 方案 B |
| 2026-11-17 | `data/room/src/test/.../repository/FakeStores.kt` | 新增 fake 扫描会话 DAO、题目关联查询与内存映射，支撑 C-079/C-080 的会话与归属校验 | C-079、C-080 方案 A |
| 2026-11-17 | `data/room/src/test/.../usecase/ApplyFeedbackUseCaseTest.kt` | 新增 C-077/C-078/C-079/C-080/C-081/C-082/C-088 用例；U-06～U-17 改为经 `submit()` 种入会话/待确认行；C-086/C-087 改为独立硬编码 DHP 期望值；全部断言未放宽 | C-077～C-089 方案 A/C |
| 2026-11-17 | `data/room/src/test/.../repository/FeedbackRepositoryTransactionTest.kt` | C-078 方案 A：跳过行断言改为“写日志、不写反馈”；C-089 方案 A：补计划号/页号/计划日断言；新增“仅被跳过题目贡献的状态不落库”用例（C-077） | C-077～C-089 方案 A |
| 2026-11-17 | `docs/conflicts.md`、`docs/conflicts-archive.md` | C-077～C-089 移出未决并按人类选择归档（裁决、落地、测试、规格同步待授权、裁决前禁止事项） | C-077～C-089 |
| 2026-11-17 | `docs/prompts.md` | 4.4 落地说明补裁决口径；6.4 确认页背景同步 C-077～C-089 | C-077～C-089 |
| 2026-11-17 | `CLAUDE.md` | 模块约束补确认前校验、跳过行日志、已提交会话拒绝重复提交 | C-077～C-080 方案 A |
| 2026-11-17 | `docs/architecture/overview.md` | 头部进度与确认生命周期同步 C-077～C-080 方案 A；测试数更新为每配置 48 个 | C-077～C-089 |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加裁决落地记录：目标、改动、红绿证据、验证结果、未决与下轮建议 | 工作循环 |
| 2026-11-17 | `docs/specs/feedback-usecase.md` | 授权回写 C-077～C-089：UseCase 前置校验与时钟/待确认/题目关联/周起始依赖；会话 READY/未提交与重复提交；跳过题目不写状态、跳过行写日志；部分成功保持 READY；迁移窗口锚点；`effectiveTier()` 与 `actualIntervalDays` 必填 | C-077～C-084、C-086、C-087 |
| 2026-11-17 | `docs/specs/repository.md` | 授权回写：`FeedbackRepository` 增加 `pending()`/`session()` 与 `ConfirmationSession`；跳过行写日志、状态按未跳过题目过滤、整批成功标记 SUBMITTED；前置校验归属 UseCase；验收补 C-077～C-089 | C-077～C-080、C-089 |
| 2026-11-17 | `docs/specs/confirm-flow.md` | 授权回写：重复提交拒绝、计划周起始下界、数据层整题跳过与部分成功展示、跳过行审计、补填验收 | C-077、C-078、C-080、C-081、C-088 |
| 2026-11-17 | `docs/specs/data-model.md` | 授权回写：`ConfirmationDecisionLog` 含跳过行；`ScanSession` 部分成功保持 READY、整批成功才 SUBMITTED | C-077、C-078、C-080 |
| 2026-11-17 | `docs/conflicts-archive.md` | C-077～C-089 的“规格同步”由待授权更新为已回写，注明文件与段落 | C-077～C-089 |
| 2026-11-17 | `docs/agent-log/2026-11-17.md` | 追加规格授权回写记录：改动范围、与代码一致性、验证沿用 | 工作循环 |
| 2026-11-18 | `settings.gradle.kts`、`gradle/libs.versions.toml`、`app/build.gradle.kts`（新增） | 新增 `:app` 模块（Compose + android-junit5）；版本目录补 `compose-runtime`/`compose-ui`/`compose-material3`（BOM 管理）与 `activity-compose` 1.11.0；app 依赖 core 四模块、`data:room`、`room-runtime` | 阶段 5.1、app.md、C-052、R-05 |
| 2026-11-18 | `app/src/main/AndroidManifest.xml`（新增） | 注册 `MainActivity` 为启动页；使用平台 `Theme.Material.Light.NoActionBar`，不引入 appcompat/material XML 依赖 | 阶段 5.1、app.md |
| 2026-11-18 | `app/src/main/.../navigation/AppNavigator.kt`（新增） | `AppRoute` sealed 路由；`AppNavigator` 用 `mutableStateOf` + `ArrayDeque` 实现 push/pop/replace 与 `canPop`；不引入 navigation-compose | app.md「DI 与导航」 |
| 2026-11-18 | `app/src/main/.../AppContainer.kt`（新增） | 手写进程内单例容器：现有 `data:room` Repository、`ApplyFeedbackUseCase`、`DefaultScheduler`、`DefaultPagePlanner`；不引入 Hilt/Koin、不复制 `core:model` DTO；不捕获分页失败（C-047 出口仍在 `PlanDraft.fromPagination`） | app.md、C-047、C-053、R-03 |
| 2026-11-18 | `app/src/main/.../AppRoot.kt`（新增） | Compose 壳：`BackHandler` 仅在可返回时拦截并 pop；根路由交系统结束 Activity；路由占位文案为后续功能页留位 | app.md、R-03 |
| 2026-11-18 | `app/src/main/.../MainActivity.kt`（新增） | 只创建 `AppContainer`/`AppNavigator`/`AppRoot` 并 `setContent`；不直接访问 Room/CameraX/PrintManager/ML Kit | app.md、R-03 |
| 2026-11-18 | `app/src/test/.../navigation/AppNavigatorTest.kt`（新增） | 1 个 JUnit 5 用例：push 两层后逐次 pop 回到根，根上 pop 返回 false 且当前路由不变 | app.md、C-052 |
| 2026-11-18 | `docs/dependency-proposals.md` | 新增 2026-11-18 条目：Compose 壳所需 runtime/ui/material3（BOM）与 `activity-compose` 1.11.0；补 `libandroidx.graphics.path.so` 的 ABI 与 `zipalign -P 16` 实测 | 阶段 5.1、R-05 |
| 2026-11-18 | `docs/prompts.md` | 5.1 标记【done】，补落地说明、C-047 出口、5.2 待接入项与无设备状态 | 阶段 5.1 |
| 2026-11-18 | `docs/architecture/overview.md` | 头部实现进度补阶段 5.1 的 `:app` 壳、装配范围与导航用例 | 阶段 5.1 |
| 2026-11-18 | `docs/agent-log/2026-11-18.md`（新增） | 本轮目标、Observe、红绿证据、验证结果、16KB/设备检查、未决与下轮建议 | 工作循环 |
| 2026-11-18 | `docs/conflicts.md` | 只更新 C-032 的「部分落地」状态：记录 `:app` 壳已落地，`feature`/`GeneratePlanUseCase`/`core:common` 仍未完成；不新增待裁决条目 | C-032、阶段 5.1 |
| 2026-11-18 | `settings.gradle.kts` | 注册 `:feature:entry`、`:feature:planning` 两个功能模块 | 阶段 5.2、app.md 模块图 |
| 2026-11-18 | `feature/entry/build.gradle.kts`、`feature/planning/build.gradle.kts`（新增） | Android library + Compose + android-junit5；依赖 core 模块与 `data:room`；显式声明已批准的 `activity-compose` 1.11.0，避免 material3 传递解析到未缓存的 1.8.2 | 阶段 5.2、C-052、R-05 |
| 2026-11-18 | `core/common/src/main/.../UiState.kt`（新增） | `UiState`（Loading/Empty/Content/Error）与 `UiError(code, message, retryable)`；纯 Kotlin | app.md「DI 与导航」「全局错误与恢复」 |
| 2026-11-18 | `core/common/src/main/.../PlanIdGenerator.kt`（新增） | 6 位 Crockford Base32 计划号生成器（去掉 I/L/O/U）；冲突重试留给计划持久化边界 | planning.md、C-069 |
| 2026-11-18 | `core/common/src/main/.../KnowledgeTextMeasurer.kt`（新增） | 右上知识点全称的视觉行数测量接口；实现留给打印层 | print-template.md、R-02 |
| 2026-11-18 | `core/pagination/.../TemplateGeometry.kt` | 补 `pagination.md` 已定义的宽度派生量 `contentWidthMm`/`leftAreaWidthMm`/`rightAreaWidthMm`/`rightTextWidthMm`，供计划层测量右上文名 | D-07、C-067 |
| 2026-11-18 | `data/room/.../repository/KnowledgePointRepository.kt` | 增加 `KnowledgePointRef` 与 `catalog()`（编号 + 名称，按编号升序）；不向上暴露 Room 实体 | C-015、C-067、C-046 方案 A |
| 2026-11-18 | `data/room/.../repository/QuestionRepository.kt`（新增） | `schedulerQuestions()`（未归档题目 + cross-ref 映射）与 `questionNumbers()`；计划层用它补齐调度 DTO 缺的题号 | repository.md、C-046、C-056 |
| 2026-11-18 | `data/room/.../repository/NoteAttachmentRepository.kt`（新增） | 最近 N 条笔记附件只读端口，供计划层计算打印附件数 | repository.md、C-026 |
| 2026-11-18 | `data/room/.../repository/EntryRepository.kt`、`RoomEntryRepository.kt`（新增） | 首个纵切录入端口：一个事务内写知识点、题目与关联；编号取现有最大编号 + 1（序列表/墓碑随 E-04/E-05/E-10 补齐） | entry.md、D-12、C-028 |
| 2026-11-18 | `data/room/.../dao/KnowledgePointDao.kt`、`data/room/src/test/.../repository/FakeStores.kt` | 增加 `maxNumber()` 供录入事务分配编号；同步 fake DAO | D-12、C-028 |
| 2026-11-18 | `data/room/.../repository/PlanRepository.kt` | 增加 `pendingCoolingOverrides()` 与只读投影 `PendingCoolingOverride`；`savePlan` 行为不变 | planning.md、C-008 方案二、C-076 方案 A |
| 2026-11-18 | `feature/planning/.../PlanningModels.kt`、`GeneratePlanUseCase.kt`（新增） | 计划编排：调度 → 行快照/名称（同集合按编号升序，C-067）→ 分页 → `PlanDraft.fromPagination` 单事务；`Failure` 转 `PlanGenerationException` 不写库（C-047）；空计划/全 overflow 返回空态并保留提示（C-060/C-061）；文字警告与分页警告按 `questionId + code` 去重排序合并（C-064）；预览只产生中文标签（C-065） | C-047、C-060～C-067、D-08、D-12 |
| 2026-11-18 | `feature/planning/.../PlanPreviewViewModel.kt`、`PlanPreviewScreen.kt`（新增） | 不持有 Context 的状态持有者：计划失败映射为可重试 `UiError`；空态携带 warnings；Compose 页面展示页/题号/中文警告 | app.md、C-047、C-065、R-03 |
| 2026-11-18 | `feature/entry/.../EntryViewModel.kt`、`EntryScreen.kt`（新增） | Entry 空态与新建 1 题 1 知识点；空题号/空名称在状态层拒绝，编号由数据层分配；Compose 页面不直接访问 Room/Context | entry.md、D-12、R-03 |
| 2026-11-18 | `feature/planning/src/test/.../GeneratePlanUseCaseTest.kt`、`PlanPreviewViewModelTest.kt`（新增） | 7 个 JUnit 5 用例：1 页 1 条、C-047 失败不写库、空计划空态、全 overflow 保留题目与原因、文字溢出标签、C-064 去重排序、C-067 名称数量/顺序；状态层成功/空态/可重试错误 | C-047、C-060～C-067 |
| 2026-11-18 | `feature/entry/src/test/.../EntryViewModelTest.kt`（新增） | 2 个 JUnit 5 用例：空题库空态；新建 1 题 1 知识点后列表 1 条 | entry.md、阶段 5.2 |
| 2026-11-18 | `app/src/main/.../AppContainer.kt` | 装配 `EntryRepository`/`QuestionRepository`/`NoteAttachmentRepository`/`GeneratePlanUseCase`/`PlanIdGenerator`；对外暴露业务 `clock`，不暴露 Context | app.md、R-03 |
| 2026-11-18 | `app/src/main/.../AndroidKnowledgeTextMeasurer.kt`（新增） | 用 `Paint.breakText` 按毫米宽度折行；3.2→3.0→2.8mm 降字号，仍超 4 行返回实际行数供计划层发警告；暂放在 app（`feature:print` 未建） | print-template.md、C-064、R-02 |
| 2026-11-18 | `app/src/main/.../AppRoot.kt`、`app/build.gradle.kts` | Home → Entry → PlanPreview 的首个可跑纵切；接入两个 feature 模块 | 阶段 5.2、app.md |
| 2026-11-18 | `docs/conflicts.md` | 新增 C-090：计划生成规格返回“打印输入”，但 C-060/C-061 要求把空态与提示送到预览页，两者缺少承载位置；记录现状、用户影响、A/B/C 方案与裁决前禁止事项；同日按方案 A 裁决并归档（见下） | C-060、C-061、C-062、planning.md |
| 2026-11-18 | `docs/prompts.md` | 5.2 补落地说明：已落地范围、验证结果、C-090 状态与无设备状态 | 阶段 5.2 |
| 2026-11-18 | `docs/architecture/overview.md` | 头部进度补阶段 5.2：`feature` 模块、录入事务、计划编排与预览状态 | 阶段 5.2 |
| 2026-11-18 | `CLAUDE.md` | 模块结构补现有 `:feature:entry`/`:feature:planning` 与首个纵切边界 | 阶段 5.2 |
| 2026-11-18 | `docs/agent-log/2026-11-18.md` | 追加阶段 5.2 记录：目标、Observe、红绿证据、验证命令与结果、无设备、未决与下轮建议 | 工作循环 |
| 2026-11-18 | `docs/conflicts.md`、`docs/conflicts-archive.md` | C-090 按方案 A 裁决并归档：计划生成返回结果包装（成功含打印输入，空计划含空态与提示）；未决恢复为仅 C-032 | C-090 方案 A、C-060、C-061 |
| 2026-11-18 | `docs/specs/planning.md` | 授权回写 C-090 方案 A：`execute()` 返回 `PlanGenerationOutcome`；补 `Planned`/`Empty`、预览投影类型、固定流程第 4～6 步与验收 | C-090 方案 A、C-047、C-060、C-061、C-062、C-065 |
| 2026-11-18 | `CLAUDE.md` | 模块约束补 `PlanGenerationOutcome` 返回形态；分页失败仍抛 `PlanGenerationException` 且不写库 | C-090 方案 A、C-047、C-060 |
| 2026-11-18 | `docs/architecture/overview.md` | 头部进度与多形态表补 C-090 方案 A 的结果出口与消费方 | C-090 方案 A |
| 2026-11-18 | `docs/prompts.md` | 5.2 落地说明由 C-090 待裁决改为已裁决并回写 | C-090 方案 A |
| 2026-11-18 | `docs/agent-log/2026-11-18.md` | 追加 C-090 裁决落地记录：归档、规格回写、无需改代码、测试沿用 | C-090 方案 A |
| 2026-11-18 | `evidence/screens/`、`evidence/ui/`（新增） | 实体机 `AXRBUT6518000086`（VKJ-AN00）验收证据：Home、Entry 空态、输入、保存后 1 条、PlanPreview 1 页 1 条、清空数据后的 C-060 空计划；含一次返回键误退出与重启的如实截图/UI 树 | 阶段 5.3、app.md「验收」 |
| 2026-11-18 | `docs/prompts.md` | 5.3 补落地说明：设备序列号、操作路径、截图清单、IME/返回键问题与未覆盖路径；阶段 5 标记 5.2/5.3【done】并补完成状态，5.1/5.2 的设备表述同步为 5.3 已补做 | 阶段 5.2、5.3 |
| 2026-11-18 | `docs/architecture/overview.md` | 头部进度补实体机 5.3 验收通过与其证据位置 | 阶段 5.3 |
| 2026-11-18 | `docs/agent-log/2026-11-18.md` | 追加阶段 5.3 记录：设备、操作步骤、预期/实际、问题、验证命令与未决 | 工作循环 |
| 2026-11-18 | `settings.gradle.kts`、`feature/print/build.gradle.kts`（新增） | 注册 `:feature:print`（Android library，JUnit 5 单测）；证据用例使用平台 `android.test.InstrumentationTestRunner`，仅以 SDK optional jar 做编译期引用，不引入 androidx.test/JUnit4 新依赖 | 阶段 6.1、print-template.md、R-05 |
| 2026-11-18 | `feature/print/.../PrintLayout.kt`（新增） | 只消费 `PlannedPage`/`PlacedItem` 生成毫米布局：A4 内容区、左侧行/三框、右上页码与全称、右下笔记/附件、QR 预留、四角 ArUco；日期首页绘制标题 +「本日共需复习 X 题」并置于上边距内，同日后续页只重复标题；`hasNoteArea=false` 不建笔记区；不生成续下页与 overflow 占位 | D-07、D-08、R-02、C-013、C-060～C-063 |
| 2026-11-18 | `feature/print/.../PrintRenderer.kt`（新增） | Canvas 渲染布局；按 3.2→3.0→2.8mm 降字号、末行省略；四角 ArUco 预留白边与标记位；QR 预留框；不压缩行高 | print-template.md、C-013、C-063 |
| 2026-11-18 | `feature/print/.../PrintPdfWriter.kt`、`RecollectPrintDocumentAdapter.kt`（新增） | `PrintedPdfDocument` A4/300DPI/0 页边距输出 PDF；`onLayout` 只报告页数，无页时失败不补空白页；`PrintDocumentAdapter` 接收计划层 `PrintJob` 的字段（页/题目/笔记文本/模板），生产代码不依赖兄弟 feature | D-08、R-02、C-060 |
| 2026-11-18 | `feature/print/.../AndroidKnowledgeTextMeasurer.kt`（迁移）、`app/.../AppContainer.kt`、`app/build.gradle.kts` | 文本测量实现从 `:app` 迁到 `feature/print`（app.md 归属）；app 增加 `:feature:print` 依赖，行为不变 | print-template.md、app.md、C-064 |
| 2026-11-18 | `feature/print/src/test/.../PrintLayoutTest.kt`（新增） | 1 个 JUnit 5 用例 PR-01：断言 A4 几何与 `TemplateGeometry` 一致、日期标题/每日题数位置与字号、图例仅首页、无“续下页”、条目 yOffset/height 原样取自分页、`hasNoteArea=false` 无笔记区、左侧 9 码点截断、四角 ArUco 与 QR 预留 | D-07、D-08、R-02、C-062、C-063 |
| 2026-11-18 | `feature/print/src/androidTest/.../PrintPdfGenerationTest.kt`、`src/androidTest/AndroidManifest.xml`（新增） | 设备上用 `PrintedPdfDocument` 生成 PDF，并用 `PdfRenderer` 断言页数；渲染 300 DPI PNG 供人工目视辅助（不作 R-01 验收），原件存 `evidence/print/` | print-template.md「验收」、C-003、C-051 |
| 2026-11-18 | `evidence/print/recollect-print-K7M2Q9.pdf`、`...-page-1.png`、`...-page-2.png`（新增） | 布局证据：2 页；PDF 132165 字节；渲染图可见左上 ArUco 覆盖图例/每日题数/表头/首行的问题（见 C-091） | 阶段 6.1、C-091 |
| 2026-11-18 | `docs/conflicts.md` | 新增 C-091：四角定位标记按“距页边 12mm”绘制，与 12mm 内容区起点重叠；记录用户影响、A/B/C 方案、选型参考、建议与裁决前禁止事项；未修改 specs/分页几何 | print-template.md、pagination.md、D-07～D-09 |
| 2026-11-18 | `docs/prompts.md` | 6.1 补落地进度：布局/PDF/证据已落地，C-091 待裁决，ArUco 实际编码与 R-01 回读未完成 | 阶段 6.1、C-091 |
| 2026-11-18 | `CLAUDE.md`、`docs/architecture/overview.md` | 模块结构补 `:feature:print`；实现进度补阶段 6.1 与 C-091 | 阶段 6.1 |
| 2026-11-18 | `docs/dependency-proposals.md` | 记录 androidTest 证据用例使用平台 `android.test.InstrumentationTestRunner` 与 SDK optional jar（compileOnly）；未新增 Maven 依赖，后续换 JUnit4/androidx.test 需先提案 | 阶段 6.1、R-05、C-052 |
| 2026-11-18 | `core/pagination/.../TemplateGeometry.kt` | C-091 方案 A：新增 `cornerReserveMm`、`contentLeftMm`/`contentTopMm` 派生量与生产模板 `a4Print()`（14mm 角区预留）；`usableHeightMm`/`contentWidthMm` 扣除角区；默认 `cornerReserveMm=0` 保持既有纯几何语义 | C-091 方案 A、D-07、pagination.md |
| 2026-11-18 | `core/pagination/.../DefaultPagePlanner.kt` | C-091 方案 A：条目 `yOffset` 起点由 `topMarginMm` 改为 `contentTopMm`，分页按预留后的内容区重算 | C-091 方案 A、C-048 |
| 2026-11-18 | `core/pagination/src/test/.../TemplateGeometryTest.kt` | 新增 `print template reserves corner area for aruco markers`：锁定内容区 26mm、宽 158mm、可用高 245mm、首页内容高 230mm、附件上限 73.5mm，并断言内容区越过 ArUco 白边外沿 26mm | C-091 方案 A |
| 2026-11-18 | `feature/planning/.../PlanningModels.kt` | `PlanningSettings.template` 默认值改为 `TemplateGeometry.a4Print()`，计划/分页/打印统一使用预留角区的生产几何 | C-091 方案 A、planning.md |
| 2026-11-18 | `feature/print/.../PrintLayout.kt`、`src/test/.../PrintLayoutTest.kt`、`src/androidTest/.../PrintPdfGenerationTest.kt` | 打印布局改用 `contentLeftMm`/`contentTopMm`，日期标题块绘制在预留带内（15.5–26mm）；单测与证据改用 `a4Print()`，断言内容区 26mm、条目起点 41/33mm、同日后续页只重复标题 | C-091 方案 A、C-063、C-091 |
| 2026-11-18 | `evidence/print/`（重新生成） | 预留角区后的 PDF 2 页、146878 字节；PNG 2480×3508（214939/102857 字节）；左上标记不再覆盖图例、每日题数、表头与首行 | C-091 方案 A、C-003 |
| 2026-11-18 | `docs/conflicts.md`、`docs/conflicts-archive.md` | C-091 按方案 A 裁决并归档：记录落地、测试、规格同步待授权与裁决前禁止事项（已解除）；未决恢复为仅 C-032 | C-091 方案 A |
| 2026-11-18 | `docs/prompts.md`、`CLAUDE.md`、`docs/architecture/overview.md` | 6.1 背景与进度同步 C-091 方案 A（`a4Print()`、内容区 26mm）；模块结构与进度同步 | C-091 方案 A、阶段 6.1 |
| 2026-11-18 | `docs/specs/print-template.md` | 授权回写 C-091 方案 A：页面几何新增四角标记预留（`a4Print()`、内容区 26mm、宽 158mm）；日期标题块位置改为 `contentTopMm` 前 15.5–26mm；图例从 `contentTopMm` 开始；ArUco 白边不得与正文重叠；验收补不重叠断言，PDF 用 `a4Print()` | C-091 方案 A、D-07、C-063 |
| 2026-11-18 | `docs/specs/pagination.md` | 授权回写 C-091 方案 A：`TemplateGeometry` 增加 `cornerReserveMm`、`contentLeftMm`/`contentTopMm`、`a4Print()`；`usableHeightMm`/`contentWidthMm` 扣除角区；`yOffset` 起点改 `contentTopMm`；日期标题预留带；新增 P-21 与验收门禁 | C-091 方案 A、C-048、C-063 |
| 2026-11-18 | `docs/specs/scan-pipeline.md` | 授权回写 C-091 方案 A：OMR 左区起点改为 `contentLeftMm`（26mm）、宽 `leftAreaWidthMm`（63.2mm）；A/B/C 框 3.5mm、中心 73.75/79.25/84.75mm；与打印共用 `a4Print()`/`OmrConfig` | C-091 方案 A、C-051 |
| 2026-11-18 | `docs/specs/planning.md` | 授权回写 C-091 方案 A：`PlanningSettings.template` 默认改为 `TemplateGeometry.a4Print()`；关联裁决补 C-091 | C-091 方案 A、C-090 |
| 2026-11-18 | `docs/conflicts-archive.md` | C-091 规格同步状态由「待授权」改为「已授权回写」，注明四份规格的回写内容；未决仍仅 C-032 | C-091 方案 A |
| 2026-11-18 | `feature/print/build.gradle.kts` | 增加 `androidTestImplementation(libs.openscanvision)`（坐标与版本已在版本目录登记，未新增依赖）；仅 spike 用例使用，生产代码与 release 产物不引入 OpenCV | C-051 方案 A、R-05 |
| 2026-11-18 | `feature/print/src/androidTest/.../OpenScanVisionReadbackSpikeTest.kt`（新增） | 阶段 6.2 spike：`PrintedPdfDocument` 生成真实 A4 PDF → 设备 `PdfRenderer` 渲染 2480×3508 → `OpenScanVision.initialize/scan`，逐字段记录库原始返回并写出矫正图；不是通过性断言用例，不改断言迁就实现 | C-051 方案 A、R-01、D-09 |
| 2026-11-18 | `evidence/print/osv-spike/`（新增） | 原始证据：spike PDF、2480×3508 PNG、库返回报告、`am instrument` 原始输出、依赖/API/16KB `p_align` 记录与 README；真机结果为 ArUco `none`、`LowConfidence(confidence=0.0)` | R-01、C-051、C-003 |
| 2026-11-18 | `docs/conflicts.md` | 新增 C-092：方案 A spike 结论（库模板写死、单 QR、参考画面 850×540 横向；打印层尚无真实标记/QR；16KB 原生库 4KB 对齐），记录用户影响、A/B/C 选项、选型参考、建议与裁决前禁止事项；未改 specs | C-051、D-09、R-06 |
| 2026-11-18 | `docs/dependency-proposals.md` | 记录 spike 结论：坐标可解析、公开 API 无法自定义模板、体积与 16KB 风险实测；未新增依赖 | C-051、R-05 |
| 2026-11-18 | `docs/conflicts-archive.md`、`docs/conflicts.md` | C-092 按方案 C（自研引擎）裁决并归档：记录现象、用户影响、落地位置、测试、规格同步待授权与遗留；未决恢复为仅 C-032 | C-092、C-051、D-09 |
| 2026-11-18 | `docs/prompts.md`、`CLAUDE.md`、`docs/architecture/overview.md` | 6.2 标【done】；6.3 背景改为 C-092 方案 C；技术栈/模块结构与 D-09 摘要同步自研引擎（OpenCV + ML Kit + 自采样 OMR） | C-092 方案 C、阶段 6.2/6.3 |
| 2026-11-18 | `gradle/libs.versions.toml` | 新增 `opencvContrib = 4.5.3.0` 与 `opencv-contrib`（原 `openscanvision` 传递依赖，版本不变）；`openscanvision` 注明仅供 spike 复现 | C-092 方案 C、R-05 |
| 2026-11-18 | `settings.gradle.kts`、`feature/scan/build.gradle.kts`（新增） | 注册 `:feature:scan`；依赖 core:common/model/pagination、opencv-contrib、ML Kit barcode；单测 JUnit 5，androidTest 用平台 runner | C-092 方案 C、C-052 |
| 2026-11-18 | `feature/scan/.../qr/QrPayloadCodec.kt`（新增） | `version|planId|pageId|questionId|CRC-32` 编解码；Crockford Base32、pageId 归属、UTF-8 CRC-32（8 位大写）；解码失败分字段数/版本/字符集/校验码 | C-030、scan-pipeline.md |
| 2026-11-18 | `feature/scan/.../omr/OmrClassifier.kt`（新增） | 暗像素比 `>30%` 命中、`<10%` 未命中、`10%–30%` 不可判定；行状态 OK/NO_MARK/CONFLICT/UNREADABLE，A/B/C → 掌握/较熟悉/不会 | scan-pipeline.md、D-07 |
| 2026-11-18 | `feature/scan/.../ScanModels.kt`、`ScanEngine.kt`、`OpenCvScanEngine.kt`（新增） | 引擎接口与数据同规格；OpenCV `DICT_4X4_50` 定位 → 按标记中心与打印几何做单应性变换 → 2480×3508；ML Kit 逐题 QR；2.5mm 窗口按 8/34/3.5/2mm 列宽采样 | C-092 方案 C、D-09、R-03 |
| 2026-11-18 | `feature/print/.../ArucoPattern.kt`（新增）、`PrintRenderer.kt` | 四角改为真实 `DICT_4X4_50` 图案（6×6 模块，纯 Kotlin 常量），不再画空框与文字标签；图案由设备 OpenCV 导出后固化，release 不引入 OpenCV | C-092 方案 C、print-template.md |
| 2026-11-18 | `feature/scan/src/test/...`、`feature/print/src/test/.../ArucoPatternTest.kt`（新增） | QR 载荷 7 个用例（往返、CRC、版本、字段数、字符集、pageId 归属、分隔符）、OMR 2 个用例、ArUco 图案 2 个用例 | C-030、scan-pipeline.md、print-template.md |
| 2026-11-18 | `feature/scan/src/androidTest/.../OpenCvScanEngineDeviceTest.kt`、`feature/print/src/androidTest/.../ArucoDetectionDiagnosticTest.kt`（新增） | 设备上以真实打印页驱动引擎：四角坐标、rotation=0、矫正 2480×3508、未涂页全 NO_MARK、缺 QR 抛 QR_NOT_FOUND；诊断用例对照 `drawMarker` 与本仓库图案 | R-01、C-092、D-09 |
| 2026-11-18 | `evidence/scan/`（新增） | 设备原始输出：`raw-instrumentation-6.3.txt`、`scan-SCAN01-report.txt`、spike PDF、诊断对照与角点裁剪图 | R-01、C-003 |
| 2026-11-18 | `evidence/print/recollect-print-K7M2Q9*.png`（重新生成） | 四角空框替换为真实 ArUco 图案后的 300 DPI 渲染图 | C-092 方案 C |
| 2026-11-18 | `docs/dependency-proposals.md` | 记录直连 `opencv-contrib:4.5.3.0` 的用途、体积/ABI、16KB 风险；二维码生成暂无可用库 | C-092 方案 C、R-05 |
| 2026-11-18 | `gradle/libs.versions.toml`、`feature/print/build.gradle.kts` | 新增 `zxing = 3.5.3` 与 `zxing-core`，`:feature:print` 增加 `implementation(libs.zxing.core)`；仅用于二维码编码（无原生库） | 二维码生成方案 A、R-05 |
| 2026-11-18 | `core/common/.../qr/QrPayloadCodec.kt`（迁入） | QR 载荷编解码从 `:feature:scan` 迁到纯 Kotlin 的 `core:common`，供打印与扫描共用；校验逻辑不变 | C-030、C-046 |
| 2026-11-18 | `feature/print/.../QrCodeRenderer.kt`（新增）、`PrintLayout.kt`、`PrintRenderer.kt` | 每题载荷 `qrPayload` 由布局层生成；渲染层用 ZXing 生成版本 5/纠错 M 位图并绘制到 18mm 预留框（不滤波保证锐利），替换空白预留框 | D-09、C-030、print-template.md |
| 2026-11-18 | `feature/scan/.../ownership/ScanOwnershipValidator.kt`（新增） | 归属校验：页面存在、planId/pageId 一致、题号在页、knowledgeRows 快照非空；失败不进入确认页 | C-011 方案二、C-071/C-072 |
| 2026-11-18 | `feature/scan/.../session/ScanSessionAccumulator.kt`（新增） | 扫描会话：同键去重保留最新、未来日期行单列、`planEnd+7` 后 `LATE_BATCH` | C-038、C-040、C-041 |
| 2026-11-18 | `core/common/src/test/...`、`feature/scan/src/test/...`、`feature/print/src/test/.../PrintQrPayloadTest.kt`（新增/迁移） | QR 7、OMR 2、归属 6、会话 4、打印载荷 1、ArUco 图案 2，全部通过 | C-030、scan-pipeline.md |
| 2026-11-18 | `feature/scan/src/androidTest/.../OpenCvScanEngineDeviceTest.kt` | QR 断言从「缺 QR 抛错误」改为「每题一个 QR、题号一致、CRC 通过、pageId 一致」 | D-09、R-01 |
| 2026-11-18 | `evidence/print/`、`evidence/scan/`（重新生成） | 打印页含每题真实二维码；扫描报告 `QR_RESULTS=q7@(2065.5,1152.0) checksum=true, q8@(2065.5,1695.5) checksum=true` | C-092、R-01 |
| 2026-11-18 | `data/room/.../repository/ScanSessionRepository.kt`（新增） | 回读会话端口与 Room 实现：`pageFacts(pageId)` 解码冻结快照；`startOrResumeSession` 复用 SCANNING/READY；`appendPage` 单事务写 `PendingFeedback`（默认 `UNMARKED`、`plannedDate=实际复习日=PlanPage.date`）并校验题号/知识点在快照内；已结束会话拒绝追加 | C-011、C-038、C-040、R-04、C-072 |
| 2026-11-18 | `data/room/.../repository/PlanPageJson.kt` | 新增 `decodeQuestionIds`：严格解析 `plan_page.questionIds`，结构非法直接抛错 | C-011、C-071 |
| 2026-11-18 | `feature/scan/.../ownership/ScanOwnershipValidator.kt`、`feature/scan/build.gradle.kts` | `PlanPageFacts` 改由 `data:room` 提供；`:feature:scan` 增加 `:data:room` 依赖，归属校验的读端口改为仓库投影 | C-011、C-092 |
| 2026-11-18 | `app/.../AppContainer.kt` | 装配 `scanSessionRepository`（`RoomScanSessionRepository`，事务运行器与三张表的 DAO） | C-092、阶段 6.3 |
| 2026-11-18 | `data/room/src/test/.../FakeStores.kt`、`ScanSessionRepositoryTest.kt`（新增） | 假存储补 `scanSessions` 快照与插入失败注入；6 个用例覆盖会话复用、快照解码、默认未决策写入、重复页去重、越界行拒绝、整页回滚 | C-011、C-038、R-04 |
| 2026-11-18 | `feature/scan/.../usecase/ScanPageUseCase.kt`（新增） | 回读编排：`scan`（引擎 → OCR → 校验 → 写库）与可 JVM 测试的 `store`；校验顺序 CRC → 页面 → 题号 → 行快照，失败零写入；成功写库后把会话标 READY，并返回 `lateBatch`/`pageIsFuture` | D-09、C-011、C-038～C-041、R-04 |
| 2026-11-18 | `feature/scan/.../ocr/NoteOcr.kt`、`MlKitNoteOcr.kt`（新增） | OCR 端口与 ML Kit 中文实现：只裁剪手写书写区（不含标题/印刷摘要），结果只作 `detectedRawOcr`；未接入时用 `NoNoteOcr` | D-06、C-012、scan-pipeline.md |
| 2026-11-18 | `feature/scan/build.gradle.kts` | 增加 `mlkit-text-recognition-chinese`（版本已在目录固定） | D-10、R-05 |
| 2026-11-18 | `data/room/.../repository/ScanSessionRepository.kt` | `PlanPageFacts` 增加页面日期；新增 `planEndDate`、`markReady`、`abandonStaleSessions`（24h，非 SUBMITTED） | C-038～C-041、data-model.md |
| 2026-11-18 | `feature/scan/src/test/.../ScanPageUseCaseTest.kt`（新增） | 9 个用例：正常写入（含触发标记/档位/OCR）、CRC 无效、题号越界、行数不符、重复 QR、缺 QR、跨页 QR 均不写库；`lateBatch`、`pageIsFuture`、写后 READY | C-011、C-038～C-041、R-04 |
| 2026-11-18 | `data/room/src/test/.../ScanSessionRepositoryTest.kt` | 新增 3 个用例：计划末日、markReady、24h 过期会话 ABANDONED；合计 57 个用例 | C-038～C-041 |
| 2026-11-18 | `feature/scan/src/androidTest/.../OpenCvScanEngineDeviceTest.kt` | 设备上用真实打印页跑「引擎 → 归属 → 写库」整链；空白笔记区 OCR 为空；证据更新 | R-01、C-092 |
| 2026-11-18 | `docs/conflicts.md` | 新增 C-093：题块/笔记区几何未持久化，导致重启或隔天回读无法定位；记录用户影响、A/B/C 方案、建议 A 与裁决前禁止事项 | D-09、scan-pipeline.md |
| 2026-11-18 | `docs/conflicts-archive.md`、`docs/conflicts.md` | C-093 按方案 A 裁决并归档：几何随打印快照持久化；记录落地、测试、规格同步待授权与遗留（旧稿不可回读、迁移未在设备实跑） | C-093 方案 A、D-09 |
| 2026-11-18 | `core/pagination/.../TemplateGeometry.kt`、`ScanGeometryProjection.kt`（新增） | 增加 `noteTitleHeightMm`；新增题块/笔记书写区投影（打印与计划共用一份公式） | C-093 方案 A、D-09 |
| 2026-11-18 | `data/room/.../entity/PlanPage.kt`、`migration/RecollectMigrations.kt`、`RecollectDatabase.kt`、`schemas/3.json` | `plan_page.itemGeometry`（JSON，默认 `[]`）；schema v3；`MIGRATION_2_3` 用 `ALTER TABLE`；导出 v3 schema | C-093 方案 A |
| 2026-11-18 | `data/room/.../repository/PlanDraft.kt`、`PlanRepository.kt`、`ItemGeometryJson.kt`（新增） | 计划草稿携带 `geometryByPage`；`savePlan` 写入几何快照；严格 JSON 编解码 | C-093 方案 A、C-071 |
| 2026-11-18 | `data/room/.../repository/ScanSessionRepository.kt` | `PlanPageFacts` 增加 `date` 与 `items`，几何从快照解码；新增 `planEndDate`/`markReady`/`abandonStaleSessions` | C-093 方案 A、C-038～C-041 |
| 2026-11-18 | `feature/planning/.../GeneratePlanUseCase.kt` | 生成计划时按 `ScanGeometryProjection.of(...)` 冻结每页几何并写入 `PlanDraft` | C-093 方案 A、D-09 |
| 2026-11-18 | `feature/scan/.../usecase/ScanPageUseCase.kt`、`.../ocr/*` | 编排改为读取持久化几何；新增 ML Kit 中文 OCR（只裁书写区）；失败零写入；`lateBatch`/`pageIsFuture`/READY | D-06、C-093 方案 A、R-04 |
| 2026-11-18 | `core/pagination/src/test`、`data/room/src/test`、`feature/scan/src/test`、`feature/scan/src/androidTest` | 新增几何投影 3、几何落库/解码 3、编排 9（含失败零写入）、会话状态 3；schema 断言升到 v3；真机整链通过 | C-093 方案 A |
| 2026-11-18 | `evidence/scan/`（更新） | 真机报告：`OCR_RESULTS={q7=}`、`STORE_OUTCOME=Stored(... rowCount=3 ...)` | R-01、C-093 |
| 2026-11-18 | `settings.gradle.kts`、`feature/confirm/build.gradle.kts`（新增） | 注册 `:feature:confirm`（Android library，Compose + JUnit 5 单测）；依赖 core:common/model 与 data:room，不新增外部依赖 | 阶段 6.4、C-052、R-05 |
| 2026-11-18 | `feature/confirm/.../ConfirmStateMachine.kt`（新增） | 纯 Kotlin 逐行状态机：接受（OMR 优先，OMR 为空需选档）、修正/补填（必须选档）、本次未复习；未来日期行只允许本次未复习；没有一键全部接受；只有全部行有效标记才可提交；NOT_REVIEWED 生成空 `actualReviewDate` | D-13、R-04、C-022、C-088 |
| 2026-11-18 | `feature/confirm/src/test/.../ConfirmStateMachineTest.kt`（新增） | 1 个 JUnit 5 用例覆盖四种决策、逐行独立、未标记阻止提交、OMR 为空需选档、修正文本保留、补填标记、未来日期限制与 NOT_REVIEWED 不携带复习日 | D-13、R-04、C-022、C-088 |
| 2026-11-18 | `feature/confirm/.../ConfirmViewModel.kt`（新增） | 加载会话/待确认行/知识点名称/题号；会话已提交/未就绪/过期分别提示；提交走 `ConfirmSubmitter` → `ApplyFeedbackUseCase`；整题跳过时展示清单并重载剩余行；未标记或 UseCase 拒绝时保留页面并提示 | C-077/C-078、C-079、C-080、R-03 |
| 2026-11-18 | `feature/confirm/.../ConfirmScreen.kt`（新增） | Compose 确认页：按页分组、触发/关联标记、OMR 状态、原始 OCR 只读、修正文本可编辑、四类逐行按钮、档位对话框、底部剩余行数/提交按钮与提交结果卡片 | D-06、D-13、C-077/C-078、R-04 |
| 2026-11-18 | `app/build.gradle.kts`、`app/.../AppRoot.kt`、`app/.../MainActivity.kt` | app 接入 `:feature:confirm`；`AppRoute.Confirm` 装配 ViewModel 与 `ApplyFeedbackUseCase`；`MainActivity` 支持 `--es confirmSessionId` 恢复确认页（扫描页落地前的入口）；androidTest 使用平台 runner，仅 compileOnly 引用 SDK optional jar | 阶段 6.4、C-052、R-05 |
| 2026-11-18 | `app/src/androidTest/.../ConfirmSeedTest.kt`（新增） | 设备证据准备：从真实计划快照生成待确认行（人工指定识别档位），仅驱动确认页 UI，不作为识别验收；含从应用外部目录恢复数据库的测试准备步骤 | 阶段 6.4、R-01 边界 |
| 2026-11-18 | `evidence/screens/confirm-01-unmarked.png`、`confirm-02-bottom-marked.png`、`confirm-03-top-marked.png`、`confirm-04-submitted.png`、`confirm-05-repeat-blocked.png`、`confirm-device-verify.txt`（新增） | 实体机 `AXRBUT6518000086` 证据：未标记禁用提交、四类决策、提交 3 条反馈/4 条决定日志、重复提交拒绝；数据库核对与证据边界 | 阶段 6.4、C-080、C-088、C-050 |
| 2026-11-18 | `evidence/ui/confirm-*.xml`（新增） | 上述各步骤的 UI 树；用于核对提交按钮 `enabled` 与逐行标记文本 | 阶段 6.4 |
| 2026-11-18 | `docs/prompts.md`、`CLAUDE.md`、`docs/architecture/overview.md` | 6.4 标【done】并补落地说明；模块结构补 `:feature:confirm`；架构总览补确认页进度与未接入项 | 阶段 6.4、D-13 |
| 2026-11-18 | `docs/changelog.md`、`docs/agent-log/2026-11-18.md` | 记录本轮目标、改动文件、验证命令与结果、未决问题与下轮建议 | 工作循环 |
| 2026-11-18 | `feature/print/build.gradle.kts` | 打印模块增加 Compose 插件与 BOM/runtime/ui/material3（均为已批准 artifact，无新外部依赖） | 阶段 6.4 后续、app.md、R-05 |
| 2026-11-18 | `feature/print/.../PrintPlanModels.kt`（新增） | `PrintPlanInput`（与 planning 的 `PrintJob` 解耦）、`PrintedQuestionSnapshot`、`PrintLauncher`/`PrintLaunchResult`/`PrintSnapshotWriter` 端口；`PrintLayout.printSnapshots()` 纯函数 | app.md「Print」、print-template.md、R-02/R-03 |
| 2026-11-18 | `feature/print/.../PrintPlanViewModel.kt`（新增） | 打印页状态：无计划空态、按日汇总页/题数、调起系统打印、成功/失败提示；不持有 Context/PrintManager | app.md、R-03 |
| 2026-11-18 | `feature/print/.../PrintPlanScreen.kt`（新增） | Compose 打印页：计划号、总页/题数、每日安排、A4 提示、“打开系统打印”与结果提示 | app.md「Print」 |
| 2026-11-18 | `feature/print/.../RecollectPrintDocumentAdapter.kt` | 新增 `onWritten` 回调：文档写出成功后把每题的 `pageId/qrPayload` 交给上层；回调异常只记录日志，不影响打印结果 | print-template.md「打印成功后写 `Question.pageId/qrPayload`」、C-033 |
| 2026-11-18 | `feature/print/.../AndroidPrintLauncher.kt`（新增） | 封装 `PrintManager`/`Context`，把 `RecollectPrintDocumentAdapter` 交给系统打印；写出成功后通过 `PrintSnapshotWriter` 异步回写快照 | R-03、print-template.md |
| 2026-11-18 | `data/room/.../repository/QuestionRepository.kt` | 新增 `savePrintSnapshot(questionId, pageId, qrPayload)`；Room 实现调用既有 `QuestionDao.setPrintSnapshot` | C-033 方案一、print-template.md |
| 2026-11-18 | `feature/planning/.../PlanPreviewViewModel.kt`、`PlanPreviewScreen.kt` | 计划成功后把同一份 `PrintJob` 交给打印页，失败/空态清空；预览页新增“打印计划”按钮（单行布局） | app.md、C-090、阶段 6.4 后续 |
| 2026-11-18 | `app/.../PrintPlanSession.kt`（新增）、`AppContainer.kt`、`AppRoot.kt` | 计划→打印的内存交接；装配 `PrintSnapshotWriter`（回写 `Question.pageId/qrPayload`）与 `AndroidPrintLauncher`；Home/PlanPreview 新增打印入口，`AppRoute.Print` 渲染打印页 | app.md「DI 与导航」、R-03 |
| 2026-11-18 | `feature/print/src/test/.../PrintPlanViewModelTest.kt`（新增） | 3 个 JUnit 5 用例：无计划空态、按日汇总与端口转发、打印失败提示 | app.md「Print」 |
| 2026-11-18 | `feature/print/src/test/.../PrintLayoutTest.kt`、`feature/planning/src/test/.../PlanPreviewViewModelTest.kt`、`feature/planning/src/test/.../FakePlanningPorts.kt` | 新增 PR-02 打印快照映射用例；新增“计划成功发布 PrintJob、失败清空”用例；fake 补 `savePrintSnapshot` | print-template.md、app.md |
| 2026-11-18 | `evidence/screens/print-01-plan-preview.png` … `print-07-after-save.png`、`evidence/ui/print-*.xml`、`evidence/screens/print-device-verify.txt`、`evidence/print/recollect-6CX9R1.pdf`（新增） | 实体机 `AXRBUT6518000086` 验收：PlanPreview → 打印页 → 系统打印对话框（A4 2 页）→ 保存为 PDF；保存后 4 题 `pageId/qrPayload` 写回计划 `6CX9R1` | app.md「Print」、print-template.md、C-033 |
| 2026-11-18 | `docs/prompts.md`、`CLAUDE.md`、`docs/architecture/overview.md`、`docs/changelog.md`、`docs/agent-log/2026-11-18.md` | 同步打印计划页落地、系统打印入口、打印快照回写与未决项 | 阶段 6.1 后续、app.md |
| 2026-11-18 | `docs/conflicts.md` | 新增 C-094：打印“成功”以文档生成（`onWrite`）为准还是以实体打印完成为准；记录用户影响、A/B/C 方案、选型参考、建议与裁决前禁止事项；未修改 specs | print-template.md、app.md、C-033 |
| 2026-11-18 | `app/.../navigation/AppNavigator.kt` | 返回栈由普通 `ArrayDeque` 改为 `mutableStateListOf` 快照状态；栈顶即 `current`，`canPop` 随栈变化；修复二级页面按返回直接结束 Activity | app.md「DI 与导航」、阶段 5.1 |
| 2026-11-18 | `app/.../AppRoot.kt` | 顶层显式读取 `current`/`canPop`，并用于 `BackHandler`、标题、返回图标与路由分发；确保 `BackHandler(enabled = ...)` 随入栈/出栈更新 | app.md「DI 与导航」、阶段 5.1 |
| 2026-11-18 | `app/src/test/.../AppNavigatorTest.kt` | 补充 `canPop` 与 `replace` 断言，导航用例增至 2 个 | app.md「DI 与导航」、阶段 5.1 |
| 2026-11-18 | `evidence/screens/nav-back-entry.png`、`nav-back-home.png`、`evidence/ui/nav-back-entry.xml`、`nav-back-home.xml`（新增） | 实体机 `AXRBUT6518000086` 验证 Home→Entry 返回一级、Home→PlanPreview→Print 逐级返回与根路由退出应用 | 阶段 5.1、app.md |
| 2026-11-18 | `docs/prompts.md`、`docs/architecture/overview.md`、`docs/agent-log/2026-11-18.md` | 同步返回栈实现口径、修复说明与验证记录 | 工作循环 |
| 2026-11-18 | `feature/print/.../PrintLayout.kt` | 表头由整行字符串改为按列定位的 `headerCells`（序号/知识点/A/B/C）；`knowledgeNameLeftMm` 与 `bubbleLeftMm` 同时供表头与填涂行使用；新增 `PrintTextAlign`（A/B/C 与序号在列内居中） | print-template.md「左侧填涂区」、D-07、R-02 |
| 2026-11-18 | `feature/print/.../PrintRenderer.kt` | 支持 `PrintTextAlign.Center`；逐格绘制表头，不再绘制整行字符串 | print-template.md |
| 2026-11-18 | `feature/print/src/test/.../PrintLayoutTest.kt` | 断言表头五格的文本、列 x、宽度与对齐方式，并锁定表头 `leftMm` 与真实气泡/名称文本坐标相等 | print-template.md、D-07 |
| 2026-11-18 | `evidence/print/recollect-print-K7M2Q9.pdf`、`recollect-print-K7M2Q9-page-1.png`、`page-2.png`（更新） | 设备 `PrintPdfGenerationTest` 重新生成：两页表头的 A/B/C 与填涂格同列、知识点与名称文本左对齐 | print-template.md、R-02 |
| 2026-11-18 | `evidence/print/recollect-5Z3FEK.pdf`、`evidence/screens/print-header-preview-01.png`、`print-header-save-dialog.png`、`print-header-after-save.png`、`evidence/ui/print-header-print-page.xml`（新增） | App 实机全链路（Home → 计划预览 → 打印页 → 系统打印预览 → 保存为 PDF）证据，确认两页表头对齐 | print-template.md、R-02 |
| 2026-11-18 | `core/pagination/.../TemplateGeometry.kt` | `dailyTotalFontMm` 改为与图例一致的 3.2mm（`ANNOTATION_FONT_MM`）；`dailyTotalHeightMm` 与绘制 top 不变 | print-template.md、pagination.md、C-063 补充 |
| 2026-11-18 | `feature/print/.../PrintLayout.kt` | 图例字号改用 `TemplateGeometry.ANNOTATION_FONT_MM`，与「本日共需复习 X 题」共用同一字号来源 | print-template.md |
| 2026-11-18 | `core/pagination/src/test/.../TemplateGeometryTest.kt`、`feature/print/src/test/.../PrintLayoutTest.kt` | 断言每日总题数字号 3.2mm、高度 3.5mm、top 22.5mm 不变，且字号等于图例 | print-template.md、C-063 |
| 2026-11-18 | `docs/specs/print-template.md`、`docs/specs/pagination.md`、`docs/conflicts-archive.md`、`docs/prompts.md`、`docs/architecture/overview.md` | 按用户指令回写字号口径与 C-063 补充；高度/top 保持不变 | C-063、print-template.md |
| 2026-11-18 | `evidence/screens/print-annotation-font-preview.png`、`print-annotation-font-after-save.png`、`evidence/print/recollect-57D6P6.pdf`、`evidence/ui/print-annotation-font-print-page.xml`、`evidence/print/recollect-print-K7M2Q9-page-1.png`（更新） | 实机打印预览与保存 PDF 证据：标签字号与图例一致、与日期标题间距不变；300 DPI 渲染同步更新 | print-template.md、C-063 |
| 2026-11-18 | `feature/print/.../PrintLayout.kt` | 表头「序号」由列内居中改为左对齐 `contentLeft`，与行序号文本同左边界 | print-template.md、D-07 |
| 2026-11-18 | `feature/print/src/test/.../PrintLayoutTest.kt` | 断言「序号」左对齐且 `leftMm` 等于 `indexText.leftMm` | print-template.md、D-07 |
| 2026-11-18 | `evidence/screens/print-index-header-preview.png`、`print-index-header-after-save.png`、`evidence/print/recollect-FZ7N1T.pdf`、`evidence/ui/print-index-header-print-page.xml`、`evidence/print/recollect-print-K7M2Q9-page-1.png`（更新） | 实机打印预览与保存 PDF：表头「序号」与行序号左对齐 | print-template.md、D-07 |
| 2026-11-18 | `data/room/.../entity/Question.kt` | 新增 `sourcePage`（题目来源页码，非打印稿 pageId），`@ColumnInfo(defaultValue = "''")` | data-model.md、entry.md |
| 2026-11-18 | `data/room/.../repository/EntryRepository.kt`、`RoomEntryRepository.kt` | `EntryDraft`/`EntrySummary` 增加 `sourcePage`；Repository 事务内校验非空并写入/读取 | entry.md、D-12 双重校验 |
| 2026-11-18 | `feature/entry/.../EntryViewModel.kt`、`EntryScreen.kt` | 表单新增页码输入（必填），状态层校验；列表显示「页码」，空白页码拒绝保存 | entry.md |
| 2026-11-18 | `feature/entry/src/test/.../EntryViewModelTest.kt` | 断言页码保存与展示；新增空白页码拒绝用例（3 个用例） | entry.md |
| 2026-11-18 | `data/room/.../RecollectDatabase.kt`、`migration/RecollectMigrations.kt`、`schemas/4.json`（新增）、`schema/RoomSchemaTest.kt` | schema v4；`MIGRATION_3_4` 添加 `sourcePage TEXT NOT NULL DEFAULT ''`；schema 断言更新 | data-model.md |
| 2026-11-18 | `docs/specs/data-model.md`、`docs/specs/entry.md`、`docs/architecture/overview.md`、`docs/prompts.md`、`CLAUDE.md` | 同步来源页码字段、双重校验与 schema v4 | entry.md、data-model.md |
| 2026-11-18 | `evidence/screens/entry-source-page-saved.png`、`entry-source-page-restart.png`、`evidence/ui/entry-source-page-restart.xml`（新增） | 实体机验证 v3→v4 迁移、页码录入、列表展示与重启持久化 | entry.md |
| 2026-11-18 | `core/pagination/.../RenderableQuestion.kt` | 打印投影增加 `sourcePage`（默认空，不参与分页） | pagination.md |
| 2026-11-18 | `data/room/.../repository/QuestionRepository.kt`、`mapping/ModelMapping.kt` | 新增 `sourcePages()` 投影；`toRenderableQuestion` 透传来源页 | repository.md、C-067 |
| 2026-11-18 | `feature/planning/.../GeneratePlanUseCase.kt`、`FakePlanningPorts.kt` | 计划生成读取 `sourcePages()` 并填入打印投影 | planning.md |
| 2026-11-18 | `feature/print/.../PrintLayout.kt` | 页码只显示两位页序，不再带计划号；右上第一行按需追加「来源页」 | print-template.md、D-09 |
| 2026-11-18 | `feature/confirm/.../ConfirmStateMachine.kt`、`ConfirmViewModel.kt`、`ConfirmScreen.kt` | 确认行增加来源页码并逐行展示（空值「未填写」） | confirm-flow.md |
| 2026-11-18 | `feature/print/src/test/.../PrintLayoutTest.kt`、`feature/planning/src/test/.../GeneratePlanUseCaseTest.kt` | 锁定页码格式与来源页透传 | print-template.md |
| 2026-11-18 | `docs/specs/print-template.md`、`pagination.md`、`confirm-flow.md`、`conflicts.md`、`overview.md`、`prompts.md` | 回写页码/来源页展示；新增 C-095（纸面不显示完整 pageId 后的手动回退） | D-09、print-template.md |
| 2026-11-18 | `evidence/screens/print-source-page-preview.png`、`confirm-source-page.png`、`evidence/print/recollect-CCJQJ3.pdf`、`evidence/ui/print-source-page-print-page.xml`、`evidence/print/recollect-print-K7M2Q9-page-1.png`（更新） | 实机验证：打印稿页码不含计划号、T-2 显示来源页 P12；确认页显示来源页 P12 | print-template.md、confirm-flow.md |
| 2026-11-18 | `feature/print/.../PrintPlanModels.kt`、`AndroidPrintLauncher.kt` | `PrintLauncher` 增加文档生成快照回调；适配器不再直接写库 | C-094 方案 B |
| 2026-11-18 | `feature/print/.../PrintPlanViewModel.kt`、`PrintPlanScreen.kt` | 文档生成后等待「已打印/未打印」确认；确认后写快照，拒绝则丢弃 | C-094 方案 B、app.md |
| 2026-11-18 | `app/.../AppRoot.kt` | 打印页 ViewModel 注入 `PrintSnapshotWriter`；`AndroidPrintLauncher` 去掉 writer 依赖 | C-094 方案 B、R-03 |
| 2026-11-18 | `feature/print/src/test/.../PrintPlanViewModelTest.kt` | 新增等待确认、确认写快照、拒绝不写 2 个用例；launcher fake 更新 | C-094 方案 B |
| 2026-11-18 | `feature/print/.../PrintLayout.kt`、`PrintRenderer.kt` | 页脚 QR 左侧增加 2.2mm 小字完整 pageId | C-095 方案 B、print-template.md |
| 2026-11-18 | `feature/print/src/test/.../PrintLayoutTest.kt` | 断言 `pageIdLabel` 文本/位置/字号且不压 QR | C-095 方案 B |
| 2026-11-18 | `docs/specs/print-template.md`、`app.md`、`scan-pipeline.md`、`conflicts.md`、`conflicts-archive.md`、`CLAUDE.md`、`overview.md`、`prompts.md` | 裁决 C-094/C-095 方案 B 并归档；同步打印确认与页脚小字 | C-094、C-095 |
| 2026-11-18 | `evidence/screens/print-pageid-footer-preview.png`、`print-confirm-after-doc.png`、`print-confirm-recorded.png`、`evidence/print/recollect-XW21BB.pdf`、`evidence/ui/print-confirm-recorded.xml`（新增） | 实机验证：文档生成后等待确认、确认后写快照；页脚小字 pageId 可见 | C-094、C-095 |
| 2026-11-20 | `data/room/.../entity/KnowledgePointTombstone.kt`、`KnowledgeNumberSequence.kt`、`dao/KnowledgePointTombstoneDao.kt`、`KnowledgeNumberSequenceDao.kt`（新增） | 标签库编号序列与墓碑实体/DAO；编号只增不减，删除知识点写墓碑且不复用（D-12 / C-028） | D-12；C-028；entry.md E-04/E-05/E-10 |
| 2026-11-20 | `data/room/.../repository/KnowledgePointLibraryRepository.kt`（新增） | 标签库端口与 Room 实现：列表/新增/重命名/删除；事务内分配编号、跳过墓碑；删除前校验未被未归档题目引用；文案面向用户 | D-12；app.md「KnowledgePoints」；entry.md |
| 2026-11-20 | `data/room/.../dao/KnowledgePointDao.kt` | 新增 `delete(id)`，只删除实体；墓碑由 Repository 在同一事务先写 | D-12 |
| 2026-11-20 | `data/room/.../repository/EntryRepository.kt`、`RoomEntryRepository.kt` | `EntryDraft` 改为 1～4 个 `knowledgePointIds`；事务内校验存在性/上限、建立关联并重算 `activeQuestionCount/dormant`；新增 `InvalidKnowledgePointLinkException` | D-12；entry.md E-01/E-03 |
| 2026-11-20 | `data/room/.../RecollectDatabase.kt`、`migration/RecollectMigrations.kt`、`schemas/5.json`（新增）、`schema/RoomSchemaTest.kt` | schema v5；新增两张表与 v4→v5 迁移（序列按现有最大编号种子）；schema 断言更新为 12 个实体 | D-12；data-model.md |
| 2026-11-20 | `data/room/src/test/.../KnowledgePointLibraryRepositoryTest.kt`、`EntryRepositoryTest.kt`（新增）、`FakeStores.kt` | 锁定编号分配/空序列种子/墓碑跳过/引用不可删/归档不阻止删除/标签排序与计数；锁定录入越界、悬空、空选择拒绝与活跃计数重算 | D-12；entry.md E-01～E-05/E-10 |
| 2026-11-20 | `feature/entry/.../EntryViewModel.kt`、`EntryScreen.kt` | 录入改为从标签库选择 1～4 个标签；空选择/超限在状态层拦截；题目列表占满页面，表单收进可拖动底部卡片，由悬浮按钮打开 | entry.md；app.md「Entry」 |
| 2026-11-20 | `feature/entry/.../KnowledgePointLibraryViewModel.kt`、`KnowledgePointLibraryScreen.kt`（新增） | 标签库页面：编号/名称列表、搜索、新增、重命名、删除确认；引用计数与禁用删除；`actionError` 保留列表并提示失败原因 | D-12；app.md「KnowledgePoints」 |
| 2026-11-20 | `app/.../AppContainer.kt`、`AppRoot.kt` | 装配 `KnowledgePointLibraryRepository`；Entry 传入 `onManageTags`，`AppRoute.KnowledgePoints` 渲染标签库页面 | app.md「DI 与导航」；R-03 |
| 2026-11-20 | `feature/entry/src/test/.../EntryViewModelTest.kt`、`KnowledgePointLibraryViewModelTest.kt`（新增） | 标签选择、空/超限拒绝、标签库增改删与引用不可删提示；共 9 个 JUnit 5 用例 | entry.md；app.md |
| 2026-11-20 | `evidence/screens/tag-01-home.png` … `tag-30-saved-sheet-closed.png`、`evidence/ui/tag-*.xml`（新增） | 实体机 `AXRBUT6518000086` 验证：标签库新增/重命名/删除、删除后编号跳到 #7 不复用、录入 T-6 关联 A/Temp、重启持久化、列表占满页面、底部卡片拖动收起与保存 | D-12；app.md；entry.md |
| 2026-11-20 | `CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md`、`docs/changelog.md`、`docs/agent-log/2026-11-20.md` | 同步标签库/标签式录入/底部卡片与 schema v5；标记 5.4 完成；规格未修改（entry.md/app.md 已支持），无新增冲突 | D-12；工作循环 |
| 2026-11-20 | `data/room/.../entity/Question.kt`、`dao/QuestionDao.kt`、`repository/SourcePageFormat.kt`（新增） | 来源页码拆为系统编号 `sourcePageCode`（唯一索引、顺序、只读）与可选说明 `sourcePage`；新增 `formatSourcePage`（空说明不带「-」）与 `maxSourcePageCode()` | 2026-11-20 用户要求；data-model.md |
| 2026-11-20 | `data/room/.../repository/EntryRepository.kt`、`RoomEntryRepository.kt`、`QuestionRepository.kt` | `EntryDraft` 改为可选 `sourcePageLabel`，事务内分配 `sourcePageCode`；`EntrySummary` 携带编号+说明并派生展示串；`sourcePages()` 输出 `编号-说明` | entry.md；print-template.md；confirm-flow.md |
| 2026-11-20 | `data/room/.../RecollectDatabase.kt`、`migration/RecollectMigrations.kt`、`schemas/6.json`（新增）、`schema/RoomSchemaTest.kt` | schema v6；v5→v6 迁移按 `createdAt` 补 1..N 编号并建唯一索引；schema 断言更新 | data-model.md |
| 2026-11-20 | `data/room/src/test/.../EntryRepositoryTest.kt`、`FakeStores.kt` | 锁定编号顺序分配、空说明只显示编号、`编号-说明` 展示与 `nextSourcePageCode` | entry.md；2026-11-20 用户要求 |
| 2026-11-20 | `feature/entry/.../EntryViewModel.kt`、`EntryScreen.kt`、`EntryViewModelTest.kt` | 表单改为只读「页码编号」+ 简短「说明（可空）」；说明不再必填；保存后刷新下一编号；用例更新为编号/空说明断言 | entry.md；2026-11-20 用户要求 |
| 2026-11-20 | `docs/specs/entry.md`、`data-model.md`、`print-template.md`、`confirm-flow.md` | 按用户指令同步页码语义：系统编号 + 可选说明，展示格式 `编号-说明`，迁移补编号 | 2026-11-20 用户要求 |
| 2026-11-20 | `evidence/screens/page-01-list-migrated.png` … `page-09-home-final.png`、`evidence/ui/page-*.xml`（新增） | 实体机 `AXRBUT6518000086` 验证 v5→v6 迁移补编号、只读下一编号、`8-Workbook`、空说明显示 `9`、重启后下一编号为 10 | 2026-11-20 用户要求 |
| 2026-11-20 | `CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md`、`docs/agent-log/2026-11-20.md` | 同步来源页码的编号/说明语义与 schema v6 | 工作循环 |
| 2026-11-20 | `data/room/.../entity/Question.kt`、`dao/QuestionDao.kt`、`repository/EntryRepository.kt`、`RoomEntryRepository.kt` | 页码编号改为用户通过 +/− 调整（最小 1、同页可复用）；默认取最近一次录入的编号且保存后不自增；移除唯一索引；`EntryDraft` 携带 `sourcePageCode` | 2026-11-20 用户要求；entry.md |
| 2026-11-20 | `data/room/.../RecollectDatabase.kt`、`migration/RecollectMigrations.kt`、`schemas/7.json`（新增）、`schema/RoomSchemaTest.kt` | schema v7；v6→v7 删除 `index_question_sourcePageCode`；schema 断言更新 | data-model.md |
| 2026-11-20 | `data/room/src/test/.../EntryRepositoryTest.kt`、`FakeStores.kt` | 锁定同页复用编号、默认取最近一次编号、编号小于 1 拒绝 | entry.md；2026-11-20 用户要求 |
| 2026-11-20 | `feature/entry/.../EntryScreen.kt`、`EntryViewModel.kt`、`EntryViewModelTest.kt` | 「页码编号」文本框内置右侧上下堆叠的 +/− 按钮（无外部按钮）；默认沿用上次编号、保存后不自增；状态层拦截编号小于 1 | 2026-11-20 用户要求 |
| 2026-11-20 | `evidence/screens/pageadj-01-default.png` … `pageadj-13-list-final.png`、`evidence/ui/pageadj-*.xml`（新增） | 实体机验证 +/−（9→10→9）、同页两道题均为 `9-Same`、重开卡片仍为 9；v6→v7 迁移正常 | 2026-11-20 用户要求 |
| 2026-11-20 | `docs/specs/entry.md`、`data-model.md`、`print-template.md`、`confirm-flow.md`、`CLAUDE.md`、`overview.md`、`prompts.md` | 同步页码编号的用户可调语义与 schema v7（取消唯一索引） | 2026-11-20 用户要求 |
| 2026-11-20 | `feature/entry/.../EntryScreen.kt` | 「页码编号」与「说明」宽度改为 1:2（约 1/3 与 2/3），+/- 仍内置在编号框右侧 | 2026-11-20 用户要求 |
| 2026-11-20 | `evidence/screens/pageadj-14-ratio.png`、`evidence/ui/pageadj-14-ratio.xml`（新增） | 实体机验证：编号框约占总宽度 1/3（x 56–431），说明框约占 2/3 | 2026-11-20 用户要求 |
| 2026-11-20 | `data/room/.../repository/EntryRepository.kt`、`RoomEntryRepository.kt` | 新增 `update(questionId, draft)` 与 `archive(questionId)`；更新在事务内覆盖字段、替换标签关联并重算新旧标签活跃计数；删除为归档（D-11）；`EntrySummary` 补标签 id、预估分钟与是否已打印 | entry.md；D-11 |
| 2026-11-20 | `feature/entry/.../EntryScreen.kt`、`EntryViewModel.kt` | 卡片右侧增加编辑（铅笔）与删除（垃圾桶）图标；铅笔打开同一录入卡片并回填题号/页码/标签/分钟，保存覆盖；删除二次确认；编辑失败在表单内提示；已打印题目显示旧稿失效提示 | entry.md E-08/E-11/E-12；D-11 |
| 2026-11-20 | `feature/entry/src/main/res/drawable/ic_edit.xml`、`ic_delete.xml`（新增） | 铅笔与垃圾桶矢量图标（无新增依赖） | 2026-11-20 用户要求 |
| 2026-11-20 | `data/room/src/test/.../EntryRepositoryTest.kt`、`feature/entry/src/test/.../EntryViewModelTest.kt` | 新增更新覆盖、旧/新标签计数、缺失题目拒绝、归档隐藏与持久化、编辑校验与删除用例 | entry.md E-11/E-12 |
| 2026-11-20 | `evidence/screens/crud-01-list-icons.png` … `crud-07-restart.png`、`evidence/ui/crud-*.xml`（新增） | 实体机验证：回填编辑 T－1→T－1E、页码 1→3、关联 A→A/D；删除 T－4 后 12→11；重启后保持 | 2026-11-20 用户要求 |
| 2026-11-20 | `docs/specs/entry.md`、`CLAUDE.md`、`overview.md`、`prompts.md` | 同步题库增删改查、编辑回填与归档删除语义 | entry.md；D-11 |
| 2026-11-20 | `app/.../AppRoot.kt`、`app/src/main/res/drawable/ic_arrow_back.xml`（新增） | 顶栏「返回」文本改为返回箭头图标（contentDescription「返回」） | 2026-11-20 用户要求 |
| 2026-11-20 | `feature/entry/.../EntryScreen.kt`、`src/main/res/drawable/ic_expand_more.xml`（新增） | 录入卡片「收起」文本改为向下箭头图标（contentDescription「收起」） | 2026-11-20 用户要求 |
| 2026-11-20 | `feature/entry/.../KnowledgePointLibraryScreen.kt` | 标签行「重命名」「删除」文本改为铅笔/垃圾桶图标（contentDescription 保留原语义） | 2026-11-20 用户要求 |
| 2026-11-20 | `evidence/screens/icons-01-entry-back.png` … `icons-03-library-icons.png`、`evidence/ui/icons-*.xml`（新增） | 实体机验证返回箭头、收起箭头与标签库铅笔/垃圾桶图标；确认对应文本不再出现 | 2026-11-20 用户要求 |
| 2026-11-20 | `feature/entry/.../EntryScreen.kt` | 「预估复习时长（分钟）」与「保存」按钮改为同一行（输入框 `weight(1f)`，按钮右侧） | 2026-11-20 用户要求 |
| 2026-11-20 | `evidence/screens/row-01-minutes-save.png`、`evidence/ui/row-01-minutes-save.xml`（新增） | 实体机验证分钟输入框与保存按钮同排、垂直居中对齐 | 2026-11-20 用户要求 |
| 2026-11-20 | `feature/entry/.../QuestionEditViewModel.kt`、`QuestionEditScreen.kt`（新增） | 录入/编辑题目改为独立页面：加载标签与待编辑题目、回填、新建/更新保存、表单校验与错误提示 | entry.md「题库增删改查」 |
| 2026-11-20 | `feature/entry/.../EntryScreen.kt`、`EntryViewModel.kt` | 移除底部卡片与表单；列表页只保留列表、编辑/删除入口与删除确认；悬浮按钮与铅笔改为路由跳转 | 2026-11-20 用户要求 |
| 2026-11-20 | `app/.../AppRoot.kt` | 接入 `AppRoute.QuestionEdit`（null 新建 / 非空编辑），顶栏标题为「录入题目」或「编辑题目」 | app.md「DI 与导航」 |
| 2026-11-20 | `feature/entry/src/test/.../QuestionEditViewModelTest.kt`（新增）、`EntryViewModelTest.kt`、`FakeEntryPorts.kt`（新增） | 覆盖加载回填、新建/更新保存、表单校验与缺失题目；列表页覆盖删除与错误提示 | entry.md E-11/E-12 |
| 2026-11-20 | `evidence/screens/page-01-new-question-page.png` … `page-06-back-unsaved.png`、`evidence/ui/page-*.xml`（新增） | 实体机验证独立新建页、编辑回填 T-7→8-P21、保存返回、返回箭头不保存 | 2026-11-20 用户要求 |
| 2026-11-20 | `docs/specs/entry.md`、`CLAUDE.md`、`overview.md`、`prompts.md` | 同步独立录入/编辑页面与路由语义 | entry.md；app.md |
| 2026-11-20 | `data/room/.../dao/ScanSessionDao.kt`、`repository/ScanSessionRepository.kt` | 新增 `latestActive()`/`activeSession()`，扫描页进入或进程重建后恢复最近一次未提交会话 | app.md「全局错误与恢复」；R-03 |
| 2026-11-20 | `data/room/src/test/.../FakeStores.kt` | `FakeScanSessionDao` 实现 `latestActive()` | 扫描会话恢复 |
| 2026-11-20 | `feature/scan/.../usecase/ScanPageUseCase.kt` | 新增由 `pageId` 推导 planId 的 `scan(bitmap, engine, noteOcr)`；`Stored` 增加 `replacedPage`，用于重复页提示 | scan-pipeline.md；R-04 |
| 2026-11-20 | `feature/scan/.../manual/ScanManualCodec.kt`（新增） | 手动回退页面 ID 规范化：大小写不敏感、`I/L→1`、`O→0`、ASCII 字符集、`6 位计划号-2 位页码` | scan-pipeline.md「手动回退」 |
| 2026-11-20 | `feature/scan/.../usecase/ScanManualEntryUseCase.kt`（新增） | `pageId + 题号` 解析唯一题目并写 `UNREADABLE` 行；不校验 QR 的版本/CRC；阻断路径零写入 | scan-pipeline.md；C-011 方案二；R-04 |
| 2026-11-20 | `feature/scan/.../io/ScanImageLoader.kt`（新增） | 相机/相册图片解码端口；长边 ≥2480 的 2 的幂采样，IO 线程解码 | app.md「权限与文件」；R-03 |
| 2026-11-20 | `feature/scan/.../ui/ScanViewModel.kt`、`ScanScreen.kt`（新增） | 扫描回读页：拍照/相册、繁忙态、已扫页列表、进入确认、错误卡片、手动输入表单、重复页/晚扫描/未来日期提示 | app.md「Scan」；scan-pipeline.md |
| 2026-11-20 | `feature/scan/src/main/AndroidManifest.xml`、`res/xml/scan_file_paths.xml`（新增） | `FileProvider` 缓存目录与 Android 11+ `IMAGE_CAPTURE` 可见性；不申请存储权限 | app.md「权限与文件」；C-096 |
| 2026-11-20 | `feature/scan/build.gradle.kts` | 开启 Compose 并加入 BOM/runtime/ui/material3/activity-compose（均属既有批准依赖） | app.md「DI 与导航」；R-05 |
| 2026-11-20 | `app/.../AppContainer.kt`、`AppRoot.kt`、`app/build.gradle.kts` | 装配扫描引擎与用例，只向页面暴露 `ScanRunner`/`ScanManualEntry`；Home 增加「扫描回读」，成功后进入确认页 | app.md；R-03 |
| 2026-11-20 | `feature/scan/src/test/.../ScanManualCodecTest.kt`、`ScanManualEntryUseCaseTest.kt`、`ScanViewModelTest.kt`（新增） | 覆盖规范化、手动回退阻断/写入、会话恢复与 QR 错误开放手动输入等 20+ 用例 | scan-pipeline.md；app.md |
| 2026-11-20 | `docs/conflicts.md` | 新增 C-096：取图用系统相机 + FileProvider，未使用 CameraX，待裁决 | scan-pipeline.md；app.md |
| 2026-11-20 | `CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md`、`docs/changelog.md`、`docs/agent-log/2026-11-20.md` | 同步扫描回读页、手动回退与会话恢复；按要求未做实机验证，未改 `docs/specs/` | 工作循环 |
| 2026-11-20 | `gradle/libs.versions.toml`、`feature/scan/build.gradle.kts` | 新增 CameraX 1.7.0（core/camera2/lifecycle/view）并作为 `:feature:scan` 依赖；C-096 方案 B | C-096 方案 B；docs/dependency-proposals.md |
| 2026-11-20 | `feature/scan/.../ui/ScanCameraPane.kt`（新增） | 页内 CameraX 预览与拍摄：A4 长宽比、`COMPATIBLE`/TextureView、`ImageCapture` 写缓存 | C-096 方案 B；app.md「Scan」 |
| 2026-11-20 | `feature/scan/.../ui/ScanScreen.kt`、`AndroidManifest.xml`、`AndroidScanImageLoader.kt` | 进入扫描页请求 `CAMERA`（拒绝回退相册）；移除系统相机 Intent/`FileProvider` 与 `queries`；相机临时文件读取后删除 | C-096 方案 B；app.md「权限与文件」；R-03 |
| 2026-11-20 | `evidence/screens/scanA4-03-camera-preview.png`、`scanA4-04-capture-result.png`、`scanA4-28-marker-result.png`、`scanA4-55-manual-stored.png`、`scanA4-58-submitted.png`，`evidence/ui/scanA4-*.xml`（新增） | 实体机验证：A4 取景、CameraX 拍摄、`PAGE_NOT_FOUND`、`QR_NOT_FOUND`、手动补录 `ZQ5FN8-01 + T-2`、确认页提交（0 反馈 / 1 决定日志） | C-096 方案 B；scan-pipeline.md；confirm-flow.md |
| 2026-11-20 | `docs/conflicts.md`、`docs/conflicts-archive.md`、`docs/dependency-proposals.md`、`docs/prompts.md`、`CLAUDE.md`、`docs/architecture/overview.md` | C-096 裁决为方案 B 并归档；登记 CameraX 版本、体积/16KB 风险与设备验证结论 | C-096；工作循环 |
| 2026-11-20 | `feature/scan/.../ui/ScanCameraPane.kt`、`ScanScreen.kt` | 「拍摄」与「从相册选择」横排同一行（各占一半宽度）；相机权限被拒绝时相册按钮仍单独保留 | C-096 方案 B；2026-11-20 用户要求 |
| 2026-11-20 | `evidence/screens/scanB-06-buttons-row.png`、`evidence/ui/scanB-06-buttons-row.xml`（新增） | 实体机验证两个按钮同一 y 范围（2317–2387），左右各占半行 | 2026-11-20 用户要求 |
| 2026-11-21 | `app/.../AppRoot.kt`、`feature/planning/.../PlanPreviewScreen.kt`、`PlanPreviewViewModel.kt` | Home 删除“打印计划”按钮（打印入口只保留在计划预览页）；预览页 ViewModel 提升到 `AppRoot` 顶层并新增 `refreshIfNeeded()`，从打印页返回预览页不再重新生成计划、计划号不变；仅再次点击“生成本周计划”时递增 generation 重新生成 | 2026-11-21 用户要求；app.md「屏幕清单与状态」（规格同步待授权） |
| 2026-11-21 | `feature/planning/src/test/.../PlanPreviewViewModelTest.kt` | 新增用例锁定“返回预览页复用原计划、主动生成才换计划号” | 2026-11-21 用户要求 |
| 2026-11-21 | `docs/prompts.md`、`docs/architecture/overview.md`、`docs/changelog.md`、`docs/agent-log/2026-11-21.md`、`docs/specs/app.md` | 同步打印入口与计划号行为；经人类授权回写 `app.md` 的 Home/PlanPreview/Print 入口描述与返回预览不重新生成计划 | 工作循环；2026-11-21 用户授权 |
| 2026-11-21 | `evidence/screens/printret-01-home.png`～`printret-06-preview-regenerated.png`、`evidence/ui/printret-0*.xml`（新增） | 实体机 VKJ-AN00 验证：Home 无“打印计划”按钮；预览 `HMKGQN` → 打印页同一计划号 → 返回预览仍为 `HMKGQN`；回 Home 重新生成得到 `JTYYT5` | 2026-11-21 用户要求；app.md |
| 2026-11-21 | `docs/specs/app.md` | 经授权回写：Home 去掉打印入口；PlanPreview 为唯一打印入口；Print 由 PlanPreview 进入；补充“返回预览不重新生成、计划号不变” | 2026-11-21 用户授权；app.md |
| 2026-11-21 | `core/pagination/.../TemplateGeometry.kt`、`feature/print/.../PrintLayout.kt`、`PrintRenderer.kt`、`QrCodeRenderer.kt` | pageId 标签从 QR 左侧移到 QR 下方：间距 = 原标签高度（4mm），在 QR 宽度内右对齐；页脚增加 `pageIdLabelGapMm/pageIdLabelHeightMm`（`qrFooterHeightMm` 21→29mm）；新增 `PrintTextAlign.Right`；QR 模块改为铺满 18mm 预留框，使可见二维码与标签右缘真正对齐 | 2026-11-21 用户要求；print-template.md、pagination.md |
| 2026-11-21 | `core/pagination/src/test/...`、`feature/print/src/test/.../PrintLayoutTest.kt` | 按新页脚高度更新默认模板用例；自定义小模板显式关闭标签预留以保持原边界构造；新增标签下方/右对齐断言 | 2026-11-21 用户要求 |
| 2026-11-21 | `evidence/print/label-2026-11-21-recollect-print-K7M2Q9-{page-1,page-2,page-3}.png`、`.pdf`、`label-2026-11-21-crop-q1.png`、`crop-q3.png`（新增） | 实体机生成并核对：`K7M2Q9-01`/`-02` 位于 QR 下方、右缘与 QR 右缘对齐，间距 4mm | 2026-11-21 用户要求 |
| 2026-11-21 | `docs/specs/print-template.md`、`docs/specs/pagination.md`、`CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md` | 同步页脚标签位置、29mm 页脚高度与 QR 铺满 18mm 框 | 2026-11-21 用户授权 |
| 2026-11-21 | `evidence/screens/printlabel-live-01-home.png`～`11-qr-zoom.png`、`evidence/ui/printlabel-live-0*.xml`、`evidence/print/live-2026-11-21-recollect-ZXWZGB.pdf`（新增） | 实体机 App 内走查：Home（无打印按钮）→ 预览 `ZXWZGB` → 打印页 → 系统打印「保存为 PDF」→ App 显示「已打印/未打印」→ 返回预览仍为 `ZXWZGB`；系统打印预览/放大图确认 pageId 位于 QR 下方并右对齐；保存 PDF 为 A4 2 页（595×841pt） | 2026-11-21 用户要求；print-template.md |
| 2026-11-21 | `feature/scan/src/androidTest/.../OpenCvScanEngineDeviceTest.kt`（运行，未改代码）、`evidence/scan/scan-SCAN01-report-2026-11-21.txt`、`evidence/scan/scan-SCAN01-2026-11-21.pdf`（新增） | 实机回读渲染页：ArUco 四角/rotation=0/2480×3508、OMR 全 NO_MARK、q7/q8 QR 校验通过、pageId=SCAN01-01、会话写入成功；验证 QR 铺满 18mm 后仍可回读 | R-01 辅助证据（非纸张照片）；2026-11-21 用户要求 |
| 2026-11-21 | `core/pagination/.../TemplateGeometry.kt`、分页与打印测试 | `pageIdLabelGapMm` 4→2mm（标签高度的一半），`qrFooterHeightMm` 29→27mm；默认模板用例高度按 -2mm 更新，P-02 改用 pageHeight=293 保持“恰好填满”构造 | 2026-11-21 用户要求 |
| 2026-11-21 | `evidence/print/gap2-2026-11-21-*.png/.pdf`、`evidence/screens/gap2-live-0*.png`、`evidence/ui/gap2-live-0*.xml`、`evidence/scan/scan-SCAN01-report-gap2-2026-11-21.txt`（新增） | 实体机验证：PDF/PNG 测得标签块间距 2mm、QR 铺满 18mm、右缘对齐；App 内系统打印预览放大图确认间距缩短；回读引擎测试通过（ArUco/OMR/QR CRC） | 2026-11-21 用户要求 |
| 2026-11-21 | `app/.../AppRoot.kt`、`feature/entry/.../EntryViewModel.kt`、`EntryScreen.kt`、`EntryViewModelTest.kt`、`FakeEntryPorts.kt` | 题库页 ViewModel 与 `LazyListState` 提升到 `AppRoot` 顶层；新增 `refreshIfNeeded()`/`markNeedsRefresh()`：从编辑/新建页返回不重新加载、保留滚动位置，保存后仅静默刷新；新增单测锁定调用次数与内容 | 2026-11-21 用户要求；entry.md、app.md |
| 2026-11-21 | `evidence/screens/entrykeep-01…15`、`evidence/ui/entrykeep-*.xml`（新增） | 实体机走查：列表滚动到 T-7 后进入编辑/新建页再返回，`题号：T-7` 的 bounds 完全不变（y=583）；保存 T-7→T-7X 后列表原地更新且位置不变；已恢复为 T-7 | 2026-11-21 用户要求 |
| 2026-11-22 | `data/room/.../entity/Plan.kt`、`dao/PlanDao.kt`、`RecollectDatabase.kt`、`migration/RecollectMigrations.kt`、`schemas/.../8.json`（新增） | schema v8：新增计划头 `plan`（周窗口、状态、printedAt、supersededBy、renderMeta）；v7→v8 只建表，旧 `plan_page` 不补计划头 | C-097；data-model.md、repository.md |
| 2026-11-22 | `data/room/.../repository/PlanRenderMeta.kt`、`PlanDraft.kt`、`PlanRepository.kt` | `PlanRenderMetaJson` 冻结/解码题号、来源页、知识点名称与行数、附件数与文本；`savePlan` 同事务写计划头并在 `supersedesPlanId` 时替代旧计划；新增 `activePlan/findPlan/storedPages/savePrintSnapshots`，后者只允许当前计划写快照并标记 PRINTED | C-097；repository.md |
| 2026-11-22 | `feature/planning/.../GeneratePlanUseCase.kt`、`PlanSession.kt`（新增）、`LoadPlanUseCase.kt`（新增）、`PlanPreviewViewModel.kt`、`PlanPreviewScreen.kt` | 生成时冻结 `renderMeta` 与计划窗口；`PlanSession.open` 复用当前计划、`regenerate` 显式替代；`LoadPlanUseCase` 从持久化计划重建 `PrintJob`；预览页新增二次确认“重新生成本周计划” | C-097；planning.md、app.md |
| 2026-11-22 | `feature/print/.../PrintPlanModels.kt`、`PrintPlanViewModel.kt`、`app/.../AppContainer.kt`、`AppRoot.kt` | `PrintSnapshotWriter` 带 `planId` 并返回 SAVED/IGNORED_NOT_ACTIVE；确认打印只为当前计划写快照；首页按钮按计划状态显示“生成本周计划/查看并打印本周计划”；AppContainer 装配 `PlanSession`/`LoadPlanUseCase` | C-097；app.md、print-template.md |
| 2026-11-22 | `data/room/src/test/...`、`feature/planning/src/test/...`、`feature/print/src/test/...` | 新增计划头/替代/快照守卫、`PlanSession` 复用与显式重新生成、`LoadPlanUseCase` 重建、SUPERSEDED 打印确认负例；schema 测试升至 v8 | C-097 |
| 2026-11-22 | `evidence/screens/planreuse-*`、`evidence/ui/planreuse-*`、`evidence/db/planreuse-recollect.db*`（新增） | 实体机验证：`192512` 复用、显式重新生成 `NBPCB6`、DB 中 `192512=SUPERSEDED`、`NBPCB6=PRINTED` 且 4 题快照前缀为 `NBPCB6`；强停重开后仍复用 `NBPCB6` | C-097；2026-11-22 用户要求 |
| 2026-11-22 | `docs/conflicts-archive.md`、`data-model.md`、`repository.md`、`planning.md`、`app.md`、`print-template.md`、`CLAUDE.md`、`overview.md`、`prompts.md` | 归档 C-097 并同步计划单例、复用/重新生成、快照守卫与首页按钮行为 | C-097 |
| 2026-11-22 | `app/.../AppRoot.kt` | 首页“录入题目”按钮下新增“管理知识点”，跳转 `AppRoute.KnowledgePoints`；返回栈可正常退回首页 | 2026-11-22 用户要求；app.md |
| 2026-11-22 | `evidence/screens/home-kp-03-home.png`、`home-kp-04-knowledge.png`、`evidence/ui/home-kp-0*.xml`（新增） | 实体机验证：首页四按钮顺序正确；点击“管理知识点”进入知识点页（共 6 个标签），返回首页正常 | 2026-11-22 用户要求 |
| 2026-11-22 | `data/room/.../repository/ReviewProgressRepository.kt`（新增） | 复习进度只读端口：未归档题目 + 关联知识点状态投影（档位、连续掌握度、有效反馈、半衰期、下次到期日） | 2026-11-22 用户要求；D-01、D-11 |
| 2026-11-22 | `feature/progress/`（新增模块：`ReviewProgressModels.kt`、`ReviewProgressAggregator.kt`、`ReviewProgressViewModel.kt`、`ReviewProgressScreen.kt`） | 复习进度页：题目总数/逾期/今天/7 天内统计、未来 14 天到期柱状分布、逐题卡片（题号、来源页、上次复习、最早到期、每个知识点的掌握度进度条与到期日）；聚合逻辑为纯 Kotlin | 2026-11-22 用户要求 |
| 2026-11-22 | `settings.gradle.kts`、`app/build.gradle.kts`、`app/.../AppContainer.kt`、`AppNavigator.kt`、`AppRoot.kt` | 注册 `:feature:progress`；新增 `AppRoute.Progress` 与「复习进度」标题；首页在“管理知识点”与计划按钮之间增加“查看复习进度” | 2026-11-22 用户要求；app.md |
| 2026-11-22 | `data/room/src/test/.../ReviewProgressRepositoryTest.kt`、`feature/progress/src/test/.../ReviewProgressAggregatorTest.kt`、`ReviewProgressViewModelTest.kt`（新增） | 覆盖未归档过滤、知识点编号排序、来源页格式、到期口径（取最早未休眠知识点）、统计与 14 天分布、排序、掌握度/反馈标签、空态与可重试错误 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/screens/progress-01-home.png`～`progress-05-back-home.png`、`evidence/ui/progress-0*.xml`（新增） | 实体机 VKJ-AN00 验证：首页四按钮顺序（录入题目/管理知识点/查看复习进度/查看并打印本周计划/扫描回读）、进度页统计与题卡、列表滚动、返回首页 | 2026-11-22 用户要求 |
| 2026-11-22 | `CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md`、`docs/agent-log/2026-11-22.md` | 同步新模块、路由与页面说明；`docs/specs/app.md` 屏幕清单未获授权，待人类确认后回写 | 工作循环 |
| 2026-11-22 | `core/pagination/.../TemplateGeometry.kt` | 新增左侧列几何与填涂格参数：序号列 6mm、知识点列 34mm、列间距 1.5mm、填涂格 5.7×3.2mm（16:9）、框间距 1.55mm、采样窗 2.5mm；新增 `bubbleLeftMm`/`bubbleCenterXMm`/`bubbleCentersXMm` 供打印与回读共用 | 2026-11-22 用户要求；D-07 |
| 2026-11-22 | `feature/print/.../PrintLayout.kt`、`PrintRenderer.kt` | 填涂格由 3.5mm 方框改为答题卡式 16:9 宽格；`BubbleLayout` 拆为 `widthMm/heightMm`；表头 A/B/C 与行内三格同列同宽 | 2026-11-22 用户要求；print-template.md |
| 2026-11-22 | `feature/scan/.../OpenCvScanEngine.kt` | OMR 采样中心改为直接用 `TemplateGeometry.bubbleCenterXMm` 计算，移除引擎内重复的 8/34/3.5/2mm 常量；项目未发布，不做旧模板/旧稿适配 | 2026-11-22 用户要求；scan-pipeline.md |
| 2026-11-22 | `feature/print/src/test/.../PrintLayoutTest.kt`、`core/pagination/src/test/.../TemplateGeometryTest.kt`、`feature/scan/src/androidTest/.../OpenCvScanEngineDeviceTest.kt` | 锁定新列宽、16:9 宽高比、三格右缘对齐、采样窗小于格高；设备用例新增“A 格横向笔迹命中” | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/print/widebubble-2026-11-22-recollect-print-K7M2Q9.{pdf,png}`、`evidence/scan/scan-SCAN01-widebubble-2026-11-22.pdf`、`evidence/scan/scan-SCAN01-report-widebubble-2026-11-22.txt`（新增） | 实体机生成新布局 PDF/PNG；扫描设备用例报告 `OMR_MARKED_ROWS=q7#0=OK/MASTERED`，其余行 `NO_MARK`，ArUco/QR 均通过 | 2026-11-22 用户要求；R-01 辅助证据（非真实纸张） |
| 2026-11-22 | `docs/specs/print-template.md`、`scan-pipeline.md`、`pagination.md`、`app.md`、`repository.md` | 经用户授权回写：16:9 填涂格与列几何、OMR 中心公式、`TemplateGeometry` 新字段、复习进度页/路由/模块图、`ReviewProgressRepository` 只读端口 | 2026-11-22 用户授权；D-07、C-093 |
| 2026-11-22 | `docs/agent-log/2026-11-22.md`、`docs/architecture/overview.md`、`docs/prompts.md`、`docs/changelog.md` | 将“规格同步待授权”更新为已回写，并记录授权与回写范围 | 2026-11-22 用户授权 |
| 2026-11-22 | `feature/entry/.../QuestionSearch.kt`（新增） | 题库搜索纯函数：题号、来源页码（`编号-说明`）或任一关联知识点名称，大小写不敏感、trim 后空查询返回全部 | 2026-11-22 用户要求 |
| 2026-11-22 | `feature/entry/.../EntryViewModel.kt`、`EntryScreen.kt` | ViewModel 保存搜索关键词（编辑页返回不丢）；列表顶部新增搜索框，标题显示“匹配 M / N 道题”，无匹配时提示“没有匹配的题目。” | 2026-11-22 用户要求；app.md（Entry 搜索） |
| 2026-11-22 | `feature/entry/src/test/.../QuestionSearchTest.kt`（新增） | 覆盖空查询、题号大小写、页码编号/说明、知识点、trim 与无匹配 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/screens/entrysearch-0*.png`、`evidence/ui/entrysearch-0*.xml`（新增） | 实体机验证：搜索框、`Temp` 命中 5 题、`T-6` 命中 1 题、`12` 命中 2 题、编辑页返回保留条件、清空恢复 10 题 | 2026-11-22 用户要求 |
| 2026-11-22 | `CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md`、`docs/agent-log/2026-11-22.md` | 同步题库搜索能力与验证结果；`docs/specs/entry.md`/`app.md` 已按 2026-11-22 用户授权回写 | 工作循环 |
| 2026-11-22 | `docs/specs/entry.md`、`app.md` | 经用户授权回写题库搜索：关键词匹配题号/来源页码/知识点、大小写不敏感、空查询全部、无匹配提示、编辑返回保留关键词；新增用例 E-13；Entry 屏幕行同步 | 2026-11-22 用户授权；E-13 |
| 2026-11-22 | `app/src/main/res/drawable/ic_home_{entry,knowledge,progress,plan,scan}.xml`（新增） | 首页语义化图标：文档+号（录入）、标签（知识点）、柱状图（进度）、日历（本周计划）、扫码框（扫描）；Material Icons 矢量路径，无新增依赖 | 2026-11-22 用户要求 |
| 2026-11-22 | `app/.../AppRoot.kt` | 首页五个入口按钮改为 `HomeActionButton`：图标置于文字开头（18dp，间距 8dp），图标 `contentDescription=null` 避免读屏重复；按钮文案、顺序与跳转不变 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/screens/homeicons-01-home.png`、`evidence/ui/homeicons-01-home.xml`（新增） | 实体机验证五个按钮图标与文字对齐，文字起点右移、按钮文案不变 | 2026-11-22 用户要求 |
| 2026-11-22 | `docs/architecture/overview.md`、`docs/prompts.md`、`docs/agent-log/2026-11-22.md` | 同步首页图标改动与验证结果；纯视觉改动，无规格语义变更，无需改 `docs/specs/` 与 `CLAUDE.md` 决策摘要 | 工作循环 |
| 2026-11-22 | `app/.../AppRoot.kt` | 首页改为仪表盘式布局：本周计划主卡（周窗口、状态、生成/打印主操作）+ 题目/知识点/已逾期/7 天内概览 + 2×2 快捷操作卡片（录入题目 / 查看复习进度 / 扫描回读 / 管理知识点）；进入 Home 时静默读取统计，失败显示“—”，不阻塞入口 | 2026-11-22 用户要求；app.md |
| 2026-11-22 | `app/.../AppRoot.kt` | 新增 `quietly` 静默读取（不吞协程取消）；周窗口与统计由 `LaunchedEffect(current)` 在回到 Home 时刷新 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/screens/homelayout-01-dashboard.png`～`homelayout-04-knowledge.png`、`evidence/ui/homelayout-0*.xml`（新增） | 实体机验证仪表盘布局、统计（10 题 / 6 知识点 / 10 逾期 / 0 七天内）、主计划按钮跳转预览、快捷卡进入进度与知识点页 | 2026-11-22 用户要求 |
| 2026-11-22 | `docs/specs/app.md`、`CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md`、`docs/agent-log/2026-11-22.md` | 同步 Home 仪表盘布局、入口顺序与统计口径；更新复习进度入口从“按钮之间”到“快捷卡片”的表述 | 2026-11-22 用户要求 |
| 2026-11-22 | `app/.../AppRoot.kt` | 首页计划主卡状态细化为“未生成 / 待打印 / 已打印 / 已完成 / 已替代”，状态说明与按钮文案随 `PlanStatus` 变化 | 2026-11-22 用户要求；app.md |
| 2026-11-22 | `evidence/screens/homelayout-05-status.png`、`evidence/ui/homelayout-05-status.xml`（新增） | 实体机验证：已打印计划显示“已打印”与对应说明，主按钮为“查看并打印本周计划” | 2026-11-22 用户要求 |
| 2026-11-22 | `app/src/main/res/values/strings.xml`（新增）、`app/src/main/AndroidManifest.xml` | 应用名从 Manifest 字面量提取为 `@string/app_name`（值“拾知”），Application 与 Launcher Activity 均显式绑定，桌面标签与应用信息统一 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/screens/appname-01-appinfo.png`、`evidence/ui/appname-01-appinfo.xml`（新增） | 实体机验证应用信息页显示“拾知”；APK 的 `application-label` 与 `launchable-activity label` 均为“拾知” | 2026-11-22 用户要求 |
| 2026-11-22 | `docs/prompts.md`、`docs/agent-log/2026-11-22.md` | 记录应用名资源化与自定义启动图标的标准做法；无规格变更 | 工作循环 |
| 2026-11-22 | `app/src/main/res/drawable/ic_launcher_{background,foreground,monochrome}.xml`（新增） | 无文字自适应图标：紫色渐变背景 + 白色复习单（纸面、页眉、A/B/C 填涂格、遗忘曲线）；前景核心图形位于 66×66 安全区；提供 Android 13 单色主题图标层 | 2026-11-22 用户要求 |
| 2026-11-22 | `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`、`ic_launcher_round.xml`（新增）、`AndroidManifest.xml` | 注册自适应图标（含圆形图标），Manifest 增加 `android:icon` / `android:roundIcon`；minSdk 26 下无需额外密度 PNG | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/screens/appicon-01-home.png`、`evidence/ui/appicon-01-home.xml`（新增） | 实体机验证：桌面显示新图标（纸面 + 填涂格 + 遗忘曲线）且名称“拾知”正常显示；`aapt` 确认 480/640 密度均解析到自适应图标 | 2026-11-22 用户要求 |
| 2026-11-22 | `app/src/main/res/drawable/ic_launcher_foreground.xml`、`ic_launcher_monochrome.xml` | 按用户要求保留“纸面 + 页眉 + A/B/C 填涂格”构图，仅把对勾替换为遗忘曲线；无文字 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/screens/appicon-03-curve.png`、`evidence/ui/appicon-03-curve.xml`（新增） | 实体机验证最终图标：纸面、页眉、A/B/C 填涂格与遗忘曲线，名称“拾知”正常 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/icon-concepts/icon-concepts-sheet.png`、`icon-{a..e}-*.png`（新增） | 5 个带对勾的图标候选预览（卡片/徽章/循环/曲线/进度环），供用户决策；当前应用图标未变更 | 2026-11-22 用户要求 |
| 2026-11-22 | `app/src/main/res/drawable/ic_launcher_foreground.xml`、`ic_launcher_monochrome.xml` | 按用户选择的方案 E 落地：270° 白色进度环（浅色完整环示意剩余进度）+ 三个紫色复习节点（顶/右/底）+ 中心对勾；单色主题层同步 | 2026-11-22 用户选择 E |
| 2026-11-22 | `evidence/screens/appicon-04-progress-check.png`、`evidence/ui/appicon-04-progress-check.xml`（新增） | 实体机验证最终图标：进度环、复习节点、对勾与桌面名称“拾知”均正常 | 2026-11-22 用户选择 E |
| 2026-11-22 | `app/src/main/res/drawable/ic_launcher_foreground.xml`、`ic_launcher_monochrome.xml` | 按用户改选方案 A：白色复习卡（边长 72/108 ≈ 2/3，居中，四周约 1/6 紫色边距）+ 页眉 + A/B/C 填涂格 + 紫色对勾；单色主题层同步 | 2026-11-22 用户选择 A |
| 2026-11-22 | `evidence/screens/appicon-05-card-a.png`、`evidence/ui/appicon-05-card-a.xml`（新增） | 实体机验证：卡片居中、边长约 2/3，A/B/C 格与对勾清晰，桌面名称“拾知”正常 | 2026-11-22 用户选择 A |
| 2026-11-22 | `app/src/main/res/drawable/ic_launcher_foreground.xml`、`ic_launcher_monochrome.xml` | 对勾略微缩小并下移（端点 35,66.2 → 44.8,75.8 → 70,52.2，线宽 6.75→6.3），与 A/B/C 填涂格留出明显间距 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/screens/appicon-06-card-a-check-gap.png`、`evidence/ui/appicon-06-card-a-check-gap.xml`（新增） | 实体机验证：对勾与第三格不再相碰，卡片仍居中且名称“拾知”正常 | 2026-11-22 用户要求 |
| 2026-11-22 | `app/src/main/res/values/colors.xml`、`values/themes.xml`、`values-v31/themes.xml`（新增）、`AndroidManifest.xml` | 去除 Android 12+ 启动画面的应用图标：新增 `Theme.Recollect`（窗口背景 `#FFFBFE`），v31 将 `windowSplashScreenAnimatedIcon` 设为透明、背景与图标背景同色、动画时长 0；Manifest 切换到该主题 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/screens/splash-01-no-icon.png`（新增） | 冷启动连拍验证：启动画面仅显示纯色背景，不再出现应用图标 | 2026-11-22 用户要求 |
| 2026-11-22 | `data/room/.../entity/SourceTag.kt`、`dao/SourceTagDao.kt`、`repository/SourceTagLibraryRepository.kt`（新增）、`RecollectDatabase.kt`、`migration/RecollectMigrations.kt`、`dao/QuestionDao.kt` | schema v9 新增来源标签库 `source_tag`（名称唯一）；增删改查在事务内完成，重命名同步 `question.sourcePage`，删除前校验未被未归档题目引用；v8→v9 把旧自由文本来源去重种入 | 2026-11-22 用户要求；entry.md、data-model.md |
| 2026-11-22 | `feature/entry/.../SourceTagLibraryViewModel.kt`、`SourceTagLibraryScreen.kt`（新增）、`QuestionEditViewModel.kt`、`QuestionEditScreen.kt` | 录入页「说明（可空）」改为「来源（可空）」单选标签，可取消；「管理来源」在录入页内打开全屏窗口增删改查，关闭后保留未保存表单并刷新候选；旧题自由文本来源作为临时标签 | 2026-11-22 用户要求；entry.md、app.md |
| 2026-11-22 | `app/.../AppContainer.kt`、`AppRoot.kt` | 装配 `SourceTagLibraryRepository` 与来源标签 ViewModel，向录入页传入来源窗口 | 2026-11-22 用户要求 |
| 2026-11-22 | `data/room/src/test/.../SourceTagLibraryRepositoryTest.kt`、`FakeStores.kt`、`schema/RoomSchemaTest.kt`、`feature/entry/src/test/.../SourceTagLibraryViewModelTest.kt`、`QuestionEditViewModelTest.kt`、`FakeEntryPorts.kt` | 覆盖来源标签增删改查、重命名同步、引用不可删、名称重复/空白、加载候选；schema 测试升至 v9 并锁定 `source_tag` | 2026-11-22 用户要求；E-14/E-15 |
| 2026-11-22 | `docs/specs/entry.md`、`app.md`、`data-model.md`、`CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md`、`docs/agent-log/2026-11-22.md` | 同步来源标签库、录入选来源、全屏窗口与 schema v9 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/screens/source-01-edit.png`、`source-02-manager.png`、`source-03-list.png`、`evidence/ui/source-02-manager.xml`（新增） | 实体机验证：来源窗口新增/重命名/删除、单选与取消、已选来源跟随重命名、删除后清空、录入表单关闭窗口后保留、保存显示 `页码：1-WB`、被引用来源不可删 | 2026-11-22 用户要求 |
| 2026-11-22 | `data/room/.../dao/QuestionDao.kt`、`repository/EntryRepository.kt`、`RoomEntryRepository.kt` | `defaultSourcePageCode(hasSource)` 按有无来源分别查询最近一条题目的编号；DAO 拆为 `latestSourcePageCodeWithoutSource/WithSource`（`trim(sourcePage)` 判空） | 2026-11-22 用户确认；entry.md |
| 2026-11-22 | `feature/entry/.../QuestionEditViewModel.kt`、`QuestionEditScreen.kt` | 页码改为文本状态：带来源可数字键盘输入（过滤非数字、最多 5 位），无来源只读 + `+/−`；空/0 提示并禁用保存；新建页切换来源且在未手改时套用对应默认值，编辑既有题不重置 | 2026-11-22 用户确认；entry.md、E-16/E-17 |
| 2026-11-22 | `data/room/src/test/.../EntryRepositoryTest.kt`、`FakeStores.kt`、`feature/entry/src/test/.../QuestionEditViewModelTest.kt`、`FakeEntryPorts.kt` | 覆盖无来源/有来源默认值分离、ViewModel 两个默认值、保存用最终文本编号 | 2026-11-22 用户确认 |
| 2026-11-22 | `evidence/screens/pagecode-01-list.png`、`pagecode-02-invalid.png`、`evidence/ui/pagecode-01-list.xml`（新增） | 实体机验证：无来源键盘输入不生效、`+/−` 到 5 保存；选中来源自动切到有来源默认 1、键盘改为 8 保存 `8-WB`；下一题默认回到无来源记忆 5；清空页码提示并禁用保存 | 2026-11-22 用户确认 |
| 2026-11-22 | `docs/specs/entry.md`、`data-model.md`、`CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md`、`docs/agent-log/2026-11-22.md` | 同步页码编号的两种编辑方式、分别记忆默认值与用例 E-16/E-17 | 2026-11-22 用户确认 |
| 2026-11-22 | `feature/entry/.../SourceTagLibraryScreen.kt`、`res/drawable/ic_close.xml`（新增） | 来源标签窗口的导航栏改为与「知识点」页一致的 TopAppBar：左侧语义关闭图标（无文字）+ 标题 | 2026-11-22 用户要求 |
| 2026-11-22 | `feature/entry/.../QuestionEditScreen.kt` | 题号与页码编号改为同一行并排，各占一半宽度（间距 8dp） | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/screens/uialign-01-row.png`、`uialign-02-sourcebar.png`、`evidence/ui/uialign-01-row.xml`、`uialign-02-sourcebar.xml`（新增） | 实体机验证：题号 `[56,415][618,639]` 与页码编号 `[646,415][1208,639]` 同行各半；来源窗口关闭图标位于左侧 `[56,205][140,289]`，标题与知识点页同位置 | 2026-11-22 用户要求 |
| 2026-11-22 | `docs/specs/app.md`、`docs/prompts.md`、`docs/agent-log/2026-11-22.md` | 同步录入页并排字段与来源窗口导航栏；纯视觉调整，无调度/数据语义变化 | 2026-11-22 用户要求 |
| 2026-11-22 | `core/pagination/.../TemplateGeometry.kt`、`DefaultPagePlanner.kt` | 删除首页图例与每日总题数：移除 `legendHeightMm`、`firstPageUsableHeightMm`、`firstPageContentHeightMm`、`dailyTotalFontMm`、`dailyTotalHeightMm`；`dateHeadingBlockHeightMm = dateHeadingHeightMm`；每页容量统一 `contentHeightMm`，删除空首页分支；`leftRatio = 0.42`、`bubbleGapMm = 3.1` 以容纳「掌握 / 较熟悉 / 不会」文字间距 | 2026-11-22 用户要求；D-07、D-08、C-063 |
| 2026-11-22 | `feature/print/.../PrintLayout.kt`、`PrintRenderer.kt` | 删除图例与「本日共需复习 X 题」绘制；表头改为「序号 / 知识点 / 掌握 / 较熟悉 / 不会」，三个档位文字以对应填涂格中心居中（单元宽度 10mm，避免被截断） | 2026-11-22 用户要求；print-template.md |
| 2026-11-22 | `feature/scan/.../OpenCvScanEngine.kt` | 识别中心继续直接取 `TemplateGeometry.bubbleCenterXMm`；框间距扩大后采样中心自动平移为 71.85 / 80.65 / 89.45mm，无需另写常量或改阈值 | 2026-11-22 用户要求；scan-pipeline.md |
| 2026-11-22 | `feature/confirm/.../ConfirmStateMachine.kt`、`ConfirmScreen.kt` | 修正文本默认填入 OCR 原文，编辑只改 `correctedText`、原始 `detectedRawOcr` 保持只读；卡片删除「OMR：…」与「识别原文（只读）」，改为直接显示「复习效果识别：掌握/较熟悉/不会/未填涂/填涂冲突/无法识别」，只保留一个预填文本框（提示「请核对笔记识别结果」） | 2026-11-22 用户要求；D-06、confirm-flow.md |
| 2026-11-22 | `core/pagination`、`feature/print`、`feature/confirm`、`feature/planning` 测试 | 更新 P-01/P-02/P-04…P-21 的 yOffset、容量与表头断言；P-10 改为“每页只扣表头”，删除已不可达的 P-13；新增确认文本预填与原文保持不变用例 | 2026-11-22 用户要求 |
| 2026-11-22 | `docs/specs/print-template.md`、`pagination.md`、`scan-pipeline.md`、`confirm-flow.md`、`CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md`、`docs/agent-log/2026-11-22.md` | 同步删除图例/每日总题数、档位表头与框间距 3.1mm、确认页单文本框预填与识别结果展示；规格已按 2026-11-22 用户要求回写 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/print/tierlabels-2026-11-22-recollect-print-K7M2Q9.pdf`、`-page-1/2/3.png`（新增） | 真机 VKJ-AN00 生成并拉取：无图例与每日总题数，每页表头「掌握 / 较熟悉 / 不会」与三框中心对齐，文字间距正常；仅打印布局证据，不等于 R-01 识别验收 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/print/recollect-G63XPC-20261122-tierlabels.pdf`、`evidence/screens/tierlabels-2026-11-22-printplan.png`（新增） | 实机 App 全流程（Home → 本周计划 G63XPC → 系统打印 → 保存为 PDF）：1 页 3 题，文本抽取确认表头「掌握/较熟悉/不会」、3 题均有「复习笔记」、无图例与「本日共需复习」；确认「未打印」不写快照 | 2026-11-22 用户要求做实机测试 |
| 2026-11-22 | `evidence/screens/tierlabels-2026-11-22-app-pdf-page1.png`（新增） | 在设备 PDF 查看器中打开实机生成稿并截图：3 道题均有「复习笔记」栏，表头「掌握 / 较熟悉 / 不会」正常显示 | 2026-11-22 用户要求做实机测试 |
| 2026-11-22 | `feature/print/.../PrintLayout.kt`、`PrintLayoutTest.kt` | 表头不再在 7mm 表头带内垂直居中，改为以本页第一条题目的实际 `PlacedItem.yOffsetMm` 为锚、在首题正上方 3.5mm 处绘制；新计划表头基线 32.28mm、首行 37.85mm（间距 5.57mm），旧计划也能贴近首题 | 2026-11-22 用户要求“将图例靠近题目”；print-template.md |
| 2026-11-22 | `evidence/print/recollect-GHB2EB-20261122-headerclose.pdf`、`evidence/screens/tierlabels-2026-11-22-headerclose-regenerated.png`（新增） | 真机重新生成本周计划 `GHB2EB` 后保存 PDF：表头紧贴第一道题；截图来自设备 PDF 查看器 | 2026-11-22 用户要求 |
| 2026-11-22 | `docs/specs/print-template.md`、`CLAUDE.md`、`docs/agent-log/2026-11-22.md` | 回写表头锚定首题的规则与实机验证结论 | 2026-11-22 用户要求 |
| 2026-11-22 | `feature/scan/.../omr/OmrClassifier.kt`、`OmrClassifierTest.kt` | 行判定改为“唯一明确命中优先”：恰好 1 个 >30% HIT 即判该档，其余框的 10%–30% 模糊读数不再把整行拖成 `UNREADABLE`；0 HIT 且有模糊框仍为 `UNREADABLE`，>1 HIT 为 `CONFLICT` | 2026-11-22 用户反馈 `fail_1.png` 三行误判“无法识别” |
| 2026-11-22 | `feature/scan/src/androidTest/.../OmrFailImageRegressionTest.kt`、`assets/fail_1.png`（新增） | 真实失败样张回归：603×862 整页图经 ArUco 矫正后读出 `MASTERED/FAMILIAR/UNKNOWN`；报告 `evidence/scan/fail_1-report.txt`、矫正图 `evidence/scan/fail_1-corrected.png` | 2026-11-22 用户反馈；R-01 |
| 2026-11-22 | `docs/specs/scan-pipeline.md`、`CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md`、`docs/agent-log/2026-11-22.md` | 同步 OMR 行判定优先级与唯一明确命中优先的修复口径 | 2026-11-22 用户反馈 |
| 2026-11-22 | `feature/entry/.../QuestionEditScreen.kt` | 「预估复习时长（分钟）」增加合法性校验：必须为 `>= 1` 的整数；为空、0 或超出整数范围时输入框显示错误态、下方提示「预估复习时长必须输入不小于 1 的整数。」并禁用保存按钮 | 2026-11-22 用户要求；entry.md |
| 2026-11-22 | `evidence/screens/minutes-2026-11-22-zero.png`、`minutes-2026-11-22-one.png`、`evidence/ui/minutes-2026-11-22-*.xml`（新增） | 实体机验证：输入 0 时保存禁用并显示错误提示；改为 1 后保存恢复可用 | 2026-11-22 用户要求 |
| 2026-11-22 | `docs/specs/entry.md`、`docs/agent-log/2026-11-22.md` | 同步预估复习时长的 `>= 1` 整数校验与保存禁用规则 | 2026-11-22 用户要求 |
| 2026-11-22 | `app/.../navigation/AppNavigator.kt`、`AppRoot.kt`、`res/drawable/ic_home_source.xml`、`ic_chevron_right.xml`（新增） | 新增 `AppRoute.SourceTags` 与「来源标签」标题；首页「快捷操作」顶部改为通栏「录入题目」，下方 2×2 调整为左上「扫描回读」、右上「查看复习进度」、左下「管理知识点」、右下「管理题目来源」；收紧纵向留白使五张卡片首次进入即完整显示 | 2026-11-22 用户要求 |
| 2026-11-22 | `docs/specs/app.md`、`docs/architecture/overview.md` | 回写 Home 的「快捷操作」通栏录入 + 2×2 布局、`AppRoute.SourceTags` 独立页面与 Home → SourceTags 返回/刷新语义 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/screens/sourcehome-2026-11-22-home.png`、`sourcehome-2026-11-22-sourcetags.png`（新增） | 实体机验证：首页通栏录入题目 + 2×2 顺序正确且首次进入即完整显示；点击右下「管理题目来源」进入「来源标签」页（3 个来源），返回首页正常 | 2026-11-22 用户要求 |
| 2026-11-22 | `feature/entry/.../KnowledgePointLibraryScreen.kt`、`QuestionEditScreen.kt`、`app/.../AppRoot.kt` | 录入页「管理知识点」改为页内全屏窗口（与「来源标签」一致：左上角关闭叉 + 标题），关闭后只刷新知识点候选并保留未保存表单；已选知识点若在窗口内被删除则从选择中移除；`AppRoute.KnowledgePoints` 保留给首页入口 | 2026-11-22 用户反馈：返回录入页整体刷新导致已填内容丢失 |
| 2026-11-22 | `evidence/ui/kpfix-*.xml`（新增） | 实体机验证：录入页填写题号 T99、选中知识点后打开知识点窗口，新增标签并关闭；返回后题号、页码、已选知识点与表单状态全部保留，新标签出现在候选；系统返回键只关闭窗口不退出录入页 | 2026-11-22 用户反馈 |
| 2026-11-22 | `docs/prompts.md`、`CLAUDE.md`、`docs/architecture/overview.md`、`docs/agent-log/2026-11-22.md` | 同步「管理知识点」页内窗口交互与实机验证结论；无调度/数据语义变化 | 2026-11-22 用户反馈 |
| 2026-11-22 | `feature/entry/.../EntryScreen.kt` | 题库卡片在「知识点：…」下方增加「预估复习时长：X分钟」，取自 `EntrySummary.estimatedReviewMinutes` | 2026-11-22 用户要求；entry.md |
| 2026-11-22 | `evidence/ui/minuteslist-01-entry.xml`（新增） | 实体机验证：三张题目卡片均按「题号 / 页码 / 知识点 / 预估复习时长」顺序显示，时长分别为 15、30、12 分钟 | 2026-11-22 用户要求 |
| 2026-11-22 | `feature/confirm/.../ConfirmScreen.kt`、`ConfirmStateMachine.kt`、`ConfirmViewModel.kt` | 确认页取消「修正」与「补填」的区分：每行只保留「接受 / 修改复习效果 / 本次未复习」；删除补填按钮与 `SUPPLEMENTED` 动作，档位弹窗标题改为「修改为哪一档」，当前状态显示「已修改复习效果」 | 2026-11-22 用户要求；D-13 |
| 2026-11-22 | `data/room/.../ConfirmedDecision.kt`、`entity/ReviewFeedback.kt`、`entity/ConfirmationDecisionLog.kt`、`entity/PendingFeedback.kt`、`repository/FeedbackRepository.kt`、`repository/ScanSessionRepository.kt` | 删除 `ConfirmedDecision.isSupplemental` 与三张表的 `isSupplemental` 列；`UserDecision` 删除 `SUPPLEMENTED`；`effectiveTier()` 只保留 `CORRECTED` | 2026-11-22 用户要求；D-13 |
| 2026-11-22 | `data/room/.../migration/RecollectMigrations.kt`、`RecollectDatabase.kt`、`schemas/.../10.json` | schema v10：重建 `review_feedback`、`confirmation_decision_log`、`pending_feedback` 删除 `isSupplemental`，历史 `SUPPLEMENTED` 归并为 `CORRECTED`；导出 v10 schema | 2026-11-22 用户要求 |
| 2026-11-22 | `data/room/src/test/.../ApplyFeedbackUseCaseTest.kt`、`FeedbackRepositoryTransactionTest.kt`、`schema/RoomSchemaTest.kt`、`feature/confirm/src/test/.../ConfirmStateMachineTest.kt`、`app/src/androidTest/.../ConfirmSeedTest.kt` | 测试改为三种决策；删除 C-088 补填用例；schema 断言升至 v10 并移除 `isSupplemental`；确认页状态机测试改用「修改复习效果」 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/ui/confirmfix-*.xml`、`evidence/db/recollect-after-v10.db`（新增） | 实体机验证：v9 数据升级到 v10 正常启动；确认页显示三个按钮（接受 / 修改复习效果 / 本次未复习），无补填；数据库 `user_version=10` 且 `review_feedback` 无 `isSupplemental` 列 | 2026-11-22 用户要求 |
| 2026-11-22 | `docs/specs/confirm-flow.md`、`data-model.md`、`feedback-usecase.md`、`CLAUDE.md`（D-13、R-04）、`docs/architecture/overview.md`、`docs/prompts.md`、`docs/agent-log/2026-11-22.md` | 同步取消修正/补填区分与 schema v10；确认页操作统一为「接受 / 修改复习效果 / 本次未复习」 | 2026-11-22 用户要求 |
| 2026-11-22 | `feature/confirm/.../ConfirmScreen.kt`、`res/drawable/ic_row_confirmed.xml`（新增） | 确认页单行完成决策后在按钮行右侧（与校对文本框右缘对齐、垂直居中）显示对勾；该行校对文本框与「确认 / 修改复习效果 / 本次未复习」全部禁用；「确认」按钮文案与当前状态同步为「已确认」 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/ui/confirmdone-01-before.xml`、`confirmdone-02-decided.xml`、`confirmdone-03-aligned.xml`（新增） | 实体机验证：点击「本次未复习」后对勾出现在按钮行最右侧（`[1082,1479][1166,1563]`，右缘与文本框 1166 对齐、与按钮文本中心同高），该行文本框与三个按钮均为禁用；底部未标记计数减少 | 2026-11-22 用户要求 |
| 2026-11-22 | `docs/specs/confirm-flow.md`、`CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md`、`docs/agent-log/2026-11-22.md` | 同步确认页单行完成反馈与锁定规则 | 2026-11-22 用户要求 |
| 2026-11-22 | `feature/confirm/.../ConfirmScreen.kt` | 「本次未复习」后该行复习效果标签改为「复习效果：本次未复习」；其余行的识别结果文案不变 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/ui/confirmdone-04-notreviewed-label.xml`（新增） | 实体机验证：标记本次未复习的行显示「复习效果：本次未复习」，对勾与禁用状态保持；未操作行仍显示「复习效果：较熟悉」 | 2026-11-22 用户要求 |
| 2026-11-22 | `docs/specs/confirm-flow.md`、`CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md`、`docs/agent-log/2026-11-22.md` | 同步本次未复习后的复习效果文案 | 2026-11-22 用户要求 |
| 2026-11-22 | `data/room/.../dao/BackupDao.kt`、`export/BackupJson.kt`、`export/DatabaseExporter.kt`、`RecollectDatabase.kt`（新增/修改） | 导出白名单只读查询（含 v8 `plan` 与 v9 `source_tag`）；纯 Kotlin JSON writer 与 `RoomDatabaseExporter`；`SCHEMA_VERSION = 10` 作为 `roomSchemaVersion` 唯一来源；临时态不导出 | 2026-11-22 用户要求；`backup.md`、C-031、C-043 |
| 2026-11-22 | `feature/backup/`（新增模块：`BackupViewModel.kt`、`BackupScreen.kt`、`BackupSharer.kt`、`build.gradle.kts`） | 「导出并分享数据库」页：`IDLE → EXPORTING → EXPORTED/ERROR`，展示文件名/大小/格式版本/各实体行数/分享结果；分享失败可只重试分享；ViewModel 不持 Context（R-03） | 2026-11-22 用户要求；`backup.md` |
| 2026-11-22 | `settings.gradle.kts`、`app/build.gradle.kts` | 登记并依赖 `:feature:backup` | 2026-11-22 用户要求 |
| 2026-11-22 | `app/.../navigation/AppNavigator.kt`（无改动，路由已存在）、`AppRoot.kt`、`AppContainer.kt`、`AndroidBackupSharer.kt`（新增） | Home 顶部导航栏右侧新增「导出数据库」+ 导出语义图标入口，进入 `AppRoute.Backup`；`FileProvider` + `ACTION_SEND` 分享实现留在 app 层；缓存保留最近 5 份导出 | 2026-11-22 用户要求；R-03 |
| 2026-11-22 | `app/src/main/res/drawable/ic_export.xml`、`res/xml/file_paths.xml`、`AndroidManifest.xml`（新增/修改） | 导出图标（Material file_download）；只暴露 `cache/backup-exports/` 的 FileProvider 路径，不申请存储权限 | 2026-11-22 用户要求 |
| 2026-11-22 | `data/room/src/test/.../export/RoomDatabaseExporterTest.kt`、`feature/backup/src/test/.../BackupViewModelTest.kt`（新增） | 顶层字段/白名单/日期格式/转义/空库与状态机用例；JUnit 5 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/2026-11-22-export/01-home.png`、`02-backup-page.png`、`03-share-sheet.png`、`04-exported-card.png`（新增） | 真机 AXRBUT6518000086：首页右上角入口、导出页、系统分享面板（`recollect-20261122-1703.json`，11.52 kB，可选 QQ/微信/钉钉）、导出完成明细（知识点 3/题目 4/关联 4）；`run-as` 读取 JSON 确认顶层字段与 `roomSchemaVersion=10` | 2026-11-22 用户要求 |
| 2026-11-22 | `docs/specs/backup.md`、`app.md`、`CLAUDE.md`、`docs/architecture/overview.md`、`docs/agent-log/2026-11-22.md` | 同步「导出并分享」页面入口/状态机、JSON 白名单新增 `plans`/`sourceTags` 与逐字段矩阵；`noteAttachments` 键名以实现为准 | 2026-11-22 用户要求 |
| 2026-11-22 | `feature/backup/.../BackupScreen.kt` | 删除「导出内容」卡片与导出完成卡片的「数据行数」列表；页面保留导出说明、文件名/大小/格式版本与分享结果 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/2026-11-22-export/05-backup-page-no-scope.png`、`06-exported-card-no-counts.png`（新增） | 真机复验：导出页无「导出内容」卡片；导出完成卡只显示文件名、11.3 KB、格式版本 1 与分享状态 | 2026-11-22 用户要求 |
| 2026-11-22 | `docs/specs/backup.md`、`app.md`、`CLAUDE.md`、`docs/architecture/overview.md`、`docs/agent-log/2026-11-22.md` | 同步删除导出范围与实体行数展示 | 2026-11-22 用户要求 |
| 2026-11-22 | `data/room/.../export/BackupJsonParser.kt`、`export/DatabaseImporter.kt`、`dao/BackupRestoreDao.kt`、`RecollectDatabase.kt`（新增/修改） | 导入链路：纯 Kotlin JSON 解析、字段/版本/编号校验、B-01 跨 schema 降级、单事务清空白名单表与临时表并批量写入；`backupRestoreDao()` 注册 | 2026-11-22 用户要求；B-01、R-04 |
| 2026-11-22 | `feature/backup/.../BackupFileReader.kt`、`ImportViewModel.kt`、`ImportDialogs.kt`（新增） | 导入端口与状态机 `IDLE → READING → CONFIRMING → IMPORTING → SUCCESS/ERROR`；失败/确认/进度对话框；成功不弹窗，由调用方 Toast | 2026-11-22 用户要求 |
| 2026-11-22 | `app/.../AndroidBackupFileReader.kt`、`ImportToast.kt`（新增）、`MainActivity.kt`、`AppRoot.kt`、`AppContainer.kt` | 导航栏「导入数据库」（导出左侧、上传图标）；SAF 选择与系统 `ACTION_VIEW` 两个入口共用状态机；带对勾 Toast「导入成功」；导入成功后刷新首页统计 | 2026-11-22 用户要求；R-03 |
| 2026-11-22 | `app/src/main/AndroidManifest.xml`、`res/values/strings.xml`、`res/drawable/ic_import.xml`、`ic_import_success_check.xml`（新增/修改） | json 文件「用其他方式打开」注册 `ACTION_VIEW + application/json`，标签「导入到拾知数据库」；上传与成功对勾图标 | 2026-11-22 用户要求 |
| 2026-11-22 | `data/room/src/test/.../export/RoomDatabaseImporterTest.kt`、`feature/backup/src/test/.../ImportViewModelTest.kt`（新增） | 合法/非法/版本过高/编号冲突/悬空关联/降级恢复/覆盖写入与状态机用例；JUnit 5 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/2026-11-22-export/07-home-import-entry.png`～`31-restored-home.png`（新增） | 真机 AXRBUT6518000086：导航栏双入口、SAF 选择、覆盖确认、带对勾 Toast、失败提示、`ACTION_VIEW` content URI 直接进入确认、导入后首页统计由 3/3 变为 3/1 再恢复；`pm disable-user` 期间验证 recollect 是 json 处理者，`cmd package query-activities` 输出确认注册 | 2026-11-22 用户要求 |
| 2026-11-22 | `docs/specs/backup.md`、`app.md`、`CLAUDE.md`、`docs/architecture/overview.md`、`docs/agent-log/2026-11-22.md` | 回写导入入口、状态机、系统打开方式、20MB 上限与成功 Toast 口径 | 2026-11-22 用户要求 |
| 2026-11-22 | `app/src/main/AndroidManifest.xml` | `MainActivity` 增加 `android:screenOrientation="portrait"`，应用固定竖屏，系统自动旋转时不切横屏 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/2026-11-22-export/32-portrait-locked.png`、`33-portrait-forced-rotation.png`（新增） | 真机复验：应用回到竖屏；把系统 `user_rotation` 强制为 1 后应用仍保持竖屏 | 2026-11-22 用户要求 |
| 2026-11-22 | `docs/specs/app.md`、`docs/agent-log/2026-11-22.md` | 回写固定竖屏约束 | 2026-11-22 用户要求 |
| 2026-11-22 | `app/.../AppRoot.kt` | 导航栏「导入数据库」「导出数据库」图标互换：导入使用原导出的下载图形，导出使用原导入的上传图形 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/2026-11-22-export/34-icons-swapped.png`（新增） | 真机复验：导入标签右侧为向下箭头、导出标签右侧为向上箭头 | 2026-11-22 用户要求 |
| 2026-11-22 | `docs/specs/backup.md`、`app.md`、`CLAUDE.md`、`docs/agent-log/2026-11-22.md` | 同步图标互换后的入口描述 | 2026-11-22 用户要求 |
| 2026-11-22 | `app/.../AppRoot.kt` | 导入成功后的全量失效：`dataRevision` 触发知识点/来源/进度页 ViewModel 重建，题库 `markNeedsRefresh` 并在进入/停留时重读，计划预览递增 generation 且清空 `PrintPlanSession`；首页统计沿用 `homeRefreshKey` | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/2026-11-22-export/36-refresh-home.png`～`41-restored-after-refresh-test.png`（新增） | 真机验证：导入后首页变为「未生成 / 题目 2」；题库 2 道题、知识点名称与来源标签为新值；计划预览按新数据生成 C7VNBY（2 条）；随后导入完整备份恢复原始状态 | 2026-11-22 用户要求 |
| 2026-11-22 | `docs/specs/backup.md`、`app.md`、`CLAUDE.md`、`docs/agent-log/2026-11-22.md` | 回写导入成功后的刷新范围 | 2026-11-22 用户要求 |
| 2026-11-22 | `app/.../AppRoot.kt`、`MainActivity.kt` | 记录导入来源：系统「用其他方式打开」(`ACTION_VIEW`) 导入成功后先显示 Toast，延时 800ms 再回调 `finishAndRemoveTask()` 退出应用；应用内 SAF 导入不退出 | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/2026-11-22-export/42-external-import-toast-then-exit.png`、`43-external-import-exited.png`、`45-saf-confirm.png`、`46-saf-import-stays.png`（新增） | 真机验证：外部导入后 Toast 显示并回到文件管理器（前台不再是拾知）；SAF 导入后仍停留在首页 | 2026-11-22 用户要求 |
| 2026-11-22 | `docs/specs/backup.md`、`app.md`、`docs/agent-log/2026-11-22.md` | 回写外部打开导入成功后自动退出的规则 | 2026-11-22 用户要求 |
| 2026-11-22 | `app/build.gradle.kts`、`app/src/main/AndroidManifest.xml`、`app/.../AppRoot.kt`、`ImportToast.kt` | `namespace` 改为 `org.ziyangdev910.recollect`、`applicationId` 改为 `org.recollect`；Manifest 的 Activity 改用全限定名 `com.recollect.app.MainActivity`（源码包保持不变），app 源码显式导入新命名空间的 `R` | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/2026-11-22-export/47-new-package.png`、`48-new-package-export.png`、`49-new-package-share.png`（新增） | 真机验证：安装为 `org.recollect` 并正常启动；`cmd package query-activities` 显示新包注册 json 打开入口；导出并分享在 `org.recollect.fileprovider` 下正常打开系统分享面板 | 2026-11-22 用户要求 |
| 2026-11-22 | `compile_and_install.bat`、`app/build.gradle.kts` | 安装脚本改为构建并安装 release APK（`:app:assembleRelease` → `app-release.apk`）；为便于本地 adb 安装，release 构建类型沿用 debug 签名（`signingConfigs.getByName("debug")`） | 2026-11-22 用户要求 |
| 2026-11-22 | `evidence/2026-11-22-export/50-release-build.png`（新增） | 运行脚本：`BUILD SUCCESSFUL`、安装成功；`dumpsys package org.recollect` 的 flags 不含 `DEBUGGABLE`，应用正常启动 | 2026-11-22 用户要求 |
| 2026-11-22 | `app/build.gradle.kts`、`app/proguard-rules.pro`（新增） | release 开启 R8（`isMinifyEnabled`）与资源裁剪（`isShrinkResources`）；新增 OpenCV JNI 保留规则与 ML Kit 缺失类忽略；`ndk.abiFilters` 只保留 `arm64-v8a`/`armeabi-v7a` | 2026-11-22 用户要求「应用压缩」 |
| 2026-11-22 | `evidence/2026-11-22-export/51-minified-release.png`～`61-minified-scan-result.png`（新增） | 真机验证压缩包：APK 246MB → 82.8MB；启动、导入（Room）、计划预览、导出分享、扫描识别（OpenCV ArUco + ML Kit QR/OCR，样张读出计划 769N80 3 行）均正常 | 2026-11-22 用户要求 |
| 2026-11-22 | `CLAUDE.md`、`docs/specs/app.md`、`docs/agent-log/2026-11-22.md` | 回写 release 压缩、ABI 过滤与本地签名说明 | 2026-11-22 用户要求 |
| 2026-11-23 | `app/build.gradle.kts` | 显式声明 `debug` 构建类型 `isMinifyEnabled = false`、`isShrinkResources = false`；release 保持 R8 压缩与资源裁剪 | 2026-11-23 用户要求 |
| 2026-11-23 | `debug_compile_and_install.bat`、`release_compile.bat`（新增） | debug 脚本改为 `:app:assembleDebug` → `app-debug.apk` 并 `adb install -r`；新增 release 脚本执行 `:app:assembleRelease` 并输出 APK 路径与大小 | 2026-11-23 用户要求 |
| 2026-11-23 | `release_compile.bat` → `release_compile_and_install.bat`（重命名） | release 脚本增加设备检测与 `adb install -r`，改为构建 minify 后的 release 包并安装到实机；真机 `AXRBUT6518000086` 安装 `Success` | 2026-11-23 用户要求 |
| 2026-11-23 | `app/build.gradle.kts` | 新增 `signingConfigs.release`：从仓库根目录 `keystore.properties`（gitignore）读取正式 keystore，release 构建使用该签名；文件缺失时回退 debug 签名 | 2026-11-23 用户要求 |
| 2026-11-23 | `release_compile_and_install.bat` | 安装失败时追加提示：`INSTALL_FAILED_UPDATE_INCOMPATIBLE` 为签名不一致，需先在应用内导出数据再 `adb uninstall org.recollect` | 2026-11-23 用户要求 |
| 2026-11-23 | `CLAUDE.md`、`docs/specs/app.md`、`docs/agent-log/2026-11-23.md` | 回写正式签名来源、缺失回退与换签名迁移规则 | 2026-11-23 用户要求 |
| 2026-11-23 | `CLAUDE.md`、`docs/specs/app.md`、`docs/agent-log/2026-11-23.md` | 同步 debug 不压缩、debug/release 两个脚本的职责划分 | 2026-11-23 用户要求 |
| 2026-11-23 | `docs/ppt-outline.md`（新增） | 新增 20 页项目方案 PPT 大纲：创作理念、问题与需求、技术路径、设计过程、成果与团队协作；含统一视觉规范、逐页文案、素材清单与一键生成提示词 | 2026-11-23 用户要求 |
| 2026-11-23 | `ppt/`（新增） | 将 `docs/ppt-outline.md` 与素材清单中的 13 个文件复制到项目根目录 `ppt/`，保留 `evidence/...` 相对路径；数据库文件仅作参考，PPT 中仍建议用截图或结构图 | 2026-11-23 用户要求 |
| 2026-11-23 | `feature/planning/.../PlanPreviewViewModel.kt`、`PlanPreviewScreen.kt`、`app/.../AppRoot.kt`、`feature/planning/src/test/.../PlanWeekSettingsTest.kt`（新增） | 首页计划主卡改为“本周 + 下周”双区块：`currentWeekSettings`（今天所在自然周）/`nextWeekSettings` 替换“下一个周一”默认窗口；`PlanWeek` 仅 CURRENT/NEXT，首页只有这两个入口；预览页“重新生成…”文案随窗口变化；首页同时查询两个窗口的 `activePlan` | C-098；2026-11-23 用户要求 |
| 2026-11-23 | `evidence/screens/plancurrent-01-home.png`～`plancurrent-07-current-preview.png`（新增） | 实体机 VKJ-AN00（release 包）验证：首页显示本周 2026-11-23 — 2026-11-29 与下周 2026-11-30 — 2026-12-06；本周已打印计划可见；生成下周计划 `06233A`（第 1 页 · 2026-11-30）后返回首页显示“待打印”，强停重开仍为“待打印”，再次查看仍为 `06233A`（未顺延到更远的周）；本周计划 `769N80`（第 1 页 · 2026-11-23）正常复用 | 2026-11-23 用户要求 |
| 2026-11-23 | `docs/conflicts-archive.md`（C-098）、`docs/conflicts.md`、`docs/prompts.md`、`CLAUDE.md`、`docs/architecture/overview.md`、`docs/agent-log/2026-11-23.md` | 归档 C-098 并同步文档；按用户授权回写 `docs/specs/planning.md`、`app.md` 的窗口描述 | C-098 |
| 2026-11-23 | `data/room/.../PlanDao.kt`、`ConfirmationDecisionLogDao.kt`、`FeedbackRepository.kt`、`PlanRepository.kt`、`app/.../AppRoot.kt`、`data/room/src/test/.../FakeStores.kt`、`ApplyFeedbackUseCaseTest.kt` | 补齐 `COMPLETED`：计划全部页确认提交后在同一事务内标记完成；`activeForWeek` 与 `savePrintSnapshots` 纳入 COMPLETED，`markPrinted` 不把已完成降级；首页已完成时下周按钮提升为主按钮 | C-099；2026-11-23 用户要求 |
| 2026-11-23 | `app/src/androidTest/.../PlanCompletionAndroidTest.kt`（新增）、`evidence/screens/plancomplete-03-home-completed.png`（新增） | 真实 Room 设备测试 2 个用例通过（全部页确认→COMPLETED；SUPERSEDED 不改写）；实体机首页显示“已完成”与引导文案，下周按钮为填充主按钮 | 2026-11-23 用户要求 |
| 2026-11-23 | `docs/specs/planning.md`、`repository.md`、`app.md`、`conflicts-archive.md`（C-098/C-099）、`conflicts.md`、`prompts.md`、`CLAUDE.md`、`docs/architecture/overview.md`、`docs/agent-log/2026-11-23.md` | 按用户授权回写规格并归档 C-099 | C-098/C-099 |
| 2026-11-23 | `app/.../AppRoot.kt` | 快捷操作四个大卡片改为与“录入题目”等高的横向小标签（固定 56dp）：`QuickActionCard` 改为图标 + 单行标题，去掉副标题与箭头；`WideActionCard` 同步固定 56dp；2×2 排列不变 | 2026-11-23 用户要求 |
| 2026-11-23 | `evidence/screens/home-compact-01.png`（新增） | 实体机 VKJ-AN00 验证：五个入口高度均为 196px（56dp），页面最后一排卡片底部距屏幕底部约 247px，整个首页一屏完整显示、无滚动 | 2026-11-23 用户要求 |
| 2026-11-23 | `docs/specs/app.md`、`docs/agent-log/2026-11-23.md` | 按用户授权回写 Home 行：录入题目含一句说明，2×2 小标签只显示图标 + 单行标题，五个入口统一 56dp、首页一屏显示 | 2026-11-23 用户授权 |
| 2026-11-23 | `app/.../AppRoot.kt` | 下周计划的“未生成”从日期行移出，改为与本周同款胶囊，放在“生成/查看下周计划”按钮之上；`StatusPill` 增加 compact 模式 | 2026-11-23 用户要求 |
| 2026-11-23 | `app/.../AppRoot.kt` | 下周区块纵向收紧：胶囊内边距 4→2dp、间距 6→3dp；新增 `CompactPlanAction`（30dp 胶囊点击容器，避开 Material Button 的 48dp 最小高度），主卡明显变矮 | 2026-11-23 用户要求 |
| 2026-11-23 | `evidence/screens/home-compact-02-next-status.png`、`home-compact-03-next-compact.png`（新增） | 实体机 VKJ-AN00 验证：状态胶囊位于下周按钮上方、日期行不再带状态；收紧后主卡与快捷操作整体上移，首页仍一屏完整显示 | 2026-11-23 用户要求 |
| 2026-11-23 | `app/.../AppRoot.kt` | `CompactPlanAction` 去掉右侧内边距：保持透明背景、不加纯色底，让“生成/查看下周计划”文字右缘与状态胶囊右缘对齐 | 2026-11-23 用户要求 |
| 2026-11-23 | `evidence/screens/home-compact-04-next-aligned.png`（新增） | 实体机 VKJ-AN00 验证：按钮文字右缘 1131px 与胶囊背景右缘 1131px 对齐 | 2026-11-23 用户要求 |
| 2026-11-23 | `app/.../AppRoot.kt` | 删除「录入题目」的说明副标题，标题在图标与箭头之间水平居中；`WideActionCard` 移除 `subtitle` 参数 | 2026-11-23 用户要求 |
| 2026-11-23 | `evidence/screens/home-compact-05-entry-centered.png`（新增） | 实体机 VKJ-AN00 验证：「录入题目」只剩图标 + 居中标题 + 箭头，与 2×2 小标签等高，首页一屏显示；重新构建并安装 release 包 | 2026-11-23 用户要求 |
| 2026-11-23 | `app/.../AppRoot.kt`、`app/src/main/res/drawable/ic_home_library.xml`（新增） | 首页快捷操作顶部改为一行两个主入口：「录入题目」直达新建题目页（`QuestionEdit(null)`），右侧新增「管理题库」进入题库列表页；两者文本左对齐、56dp 等高；移除不再使用的 `WideActionCard` | 2026-11-23 用户要求 |
| 2026-11-23 | `evidence/screens/home-compact-06-entry-library.png`、`home-compact-07-library-page.png`（新增） | 实体机 VKJ-AN00 验证：首页两个入口左对齐并排；「录入题目」进入新建题目表单、「管理题库」进入题库页；首页一屏显示；重新构建并安装 release 包 | 2026-11-23 用户要求 |
| 2026-11-23 | `docs/specs/app.md`、`docs/agent-log/2026-11-23.md` | 按用户授权同步 Home 行：顶部两个并排入口（录入题目直达新建题目页 / 管理题库进入题库页）、六个入口图标 + 左对齐标题且统一 56dp、下周状态胶囊位于按钮上方且与按钮文字右对齐；清理日志中过时的待授权项 | 2026-11-23 用户授权 |
| 2026-11-23 | `feature/planning/.../{PlanningModels,PlanDeferredPreview,GeneratePlanUseCase,LoadPlanUseCase,PlanPreviewViewModel,PlanPreviewScreen}.kt`、`data/room/.../{entity/Plan,repository/PlanDeferred,PlanDraft,PlanRepository,migration/RecollectMigrations,RecollectDatabase,export/DatabaseExporter,export/DatabaseImporter}.kt`、`data/room/schemas/.../11.json` | 计划预览页显示被推迟的题目：生成时把调度器的 `deferred` 投影为“题号 + 中文原因 + 说明”，并把 `questionId/题号/原因` 冻结到新增的 `plan.deferred`（schema v11，v10→v11 迁移给旧计划补 `[]`）；`LoadPlanUseCase` 重开计划时回读快照，强停重开后仍显示；导出/导入同步该字段。容量不足提示“复习时间不足”，冷却与知识点缺失也各有中文说明 | 2026-11-23 用户要求；`app.md`/`planning.md` 规格同步待授权 |
| 2026-11-23 | `data/room/.../schema/RoomSchemaTest.kt`、`export/RoomDatabaseExporterTest.kt`、`export/RoomDatabaseImporterTest.kt`、`repository/PlanDeferredJsonTest.kt`、`repository/PlanRepositoryTransactionTest.kt`、`feature/planning/.../{GeneratePlanUseCaseTest,LoadPlanUseCaseTest,PlanPreviewViewModelTest}.kt`（新增/修改） | 锁定推迟快照编解码、随计划落库与回读、生成时 `TIME_OVERFLOW` 投影、预览页 Content/Empty 透传；schema 测试升到 v11，导出/导入测试的 `roomSchemaVersion` 同步为 11 | 2026-11-23 用户要求 |
| 2026-11-23 | `evidence/screens/plandeferred-01-generated.png`、`plandeferred-02-reload.png`、`evidence/ui/plandeferred-02-reload.xml`（新增） | 实体机 VKJ-AN00（release，保留数据由 v10 迁移到 v11）验证：重新生成本周计划 `XV105D` 后预览页显示“有 1 道题未排入本次计划 / 题号 13 · 复习时间不足”；强停重开再次进入仍复用 `XV105D` 并保留提示 | 2026-11-23 用户要求 |
| 2026-11-23 | `feature/planning/.../{PlanningModels,PlanDeferredPreview,GeneratePlanUseCase,LoadPlanUseCase,PlanPreviewScreen}.kt`、`data/room/.../repository/PlanDeferred.kt` | 计划页面卡片与“未排入计划”卡片统一改用「页码-第X题」标识（无来源页码时回落「第X题」）：`PlanPreviewPage.questionNumbers` 改为 `questionLabels`、`PlanPreviewDeferred.questionNumber` 改为 `questionLabel`；推迟快照 `PlanDeferredRecord` 增加 `sourcePage` 并随 `plan.deferred` 冻结，旧快照缺该字段按空串兼容 | 2026-11-23 用户要求；`app.md`/`planning.md` 规格同步待授权 |
| 2026-11-23 | `feature/planning/.../{GeneratePlanUseCaseTest,LoadPlanUseCaseTest,PlanPreviewViewModelTest}.kt`、`data/room/.../{PlanDeferredJsonTest,PlanRepositoryTransactionTest}.kt` | 断言页面/推迟项使用「页码-第X题」标识、推迟快照冻结来源页码；新增旧 `plan.deferred` JSON（无 `sourcePage`）兼容用例与旧快照从题库兜底展示的用例（含“快照已有来源页码时不查题库”）；修正容量不足用例对现行建议文案的断言 | 2026-11-23 用户要求 |
| 2026-11-23 | `feature/planning/.../LoadPlanUseCase.kt`、`PlanDeferredPreview.kt`、`app/.../AppContainer.kt` | 兼容旧推迟快照：`LoadPlanUseCase` 增加 `sourcePages` 读端口，仅当 `plan.deferred` 缺 `sourcePage` 时从题库补齐展示，使旧计划的“未排入计划”卡片同样显示「页码-第X题」；快照已有时不额外查询、不改写落库数据 | 2026-11-23 用户要求 |
| 2026-11-23 | `evidence/screens/label-format-01-preview.png`、`label-format-02-next-preview.png`、`label-format-03-deferred-fallback.png`、`evidence/ui/label-format-preview.xml`、`label-format-deferred-fallback.xml`（新增） | 实体机 VKJ-AN00（release，覆盖安装保留数据）验证：本周计划 `DCFEKV` 页面卡片显示「50-通用作业本-第4题」等标识；改动前生成的推迟卡片由题库兜底显示「2-信息作业本-第1题」「56-物理练透-第13题」；新生成的下周计划 `BHPZZC` 同样使用「页码-第X题」 | 2026-11-23 用户要求 |
| 2026-11-23 | `CLAUDE.md`、`docs/prompts.md`、`docs/architecture/overview.md`、`docs/agent-log/2026-11-23.md` | 同步计划预览题目标识改为「页码-第X题」的实现与未授权规格待办 | 2026-11-23 用户要求 |
| 2026-11-23 | `docs/specs/planning.md`、`app.md`、`repository.md`、`data-model.md` | 按用户授权回写：PlanPreview 页面卡片与“未排入计划”卡片统一「页码-第X题」、新增 `PlanPreviewDeferred` 与 `questionLabels`、`PlanDraft.deferredRecords`、`PlanDeferredRecord.sourcePage`（旧 JSON 缺字段按空串兼容）、`plan.deferred`（schema v11）与 `LoadPlanUseCase` 的展示兜底；同步清理 prompts/agent-log 中过时的“规格同步待授权”标记 | 2026-11-23 用户授权 |
| 2026-11-23 | `data/room/.../entity/AppSettings.kt`、`dao/AppSettingsDao.kt`、`repository/SettingsRepository.kt`（新增）、`dao/BackupDao.kt`、`dao/BackupRestoreDao.kt`、`export/DatabaseExporter.kt`、`export/DatabaseImporter.kt`、`migration/RecollectMigrations.kt`、`RecollectDatabase.kt`、`schemas/.../12.json`（新增） | 新增 Room 单行表 `app_settings`（schema v12）持久化「每日复习时长」（默认 60，1～240）；v11→v12 迁移建表并写入默认行；导出新增 `appSettings` 键（库中无行时导出默认行），导入无该键的旧备份时写回默认行，保证数据库始终含该字段 | 2026-11-23 用户要求「保证导入、导出的数据库中存有每日复习时长字段」 |
| 2026-11-23 | `app/.../AppRoot.kt`、`app/.../AboutScreen.kt`（新增）、`navigation/AppNavigator.kt`、`AppContainer.kt`、`app/src/main/res/drawable/ic_clock.xml`、`ic_info.xml`（新增）、`feature/planning/.../PlanningModels.kt` | 首页删除「快捷操作」标签文本；底部新增卡片栏：左「每日复习时长」（时钟图标 + 左对齐文字 + 当前分钟数副标题，点击弹悬浮窗，输入框下附说明），右「关于」（信息图标）进入新页面展示应用图标/名称/版本/作者/开源仓库/功能说明；新增 `AppRoute.About`；设置值经 `SettingsRepository` 落库，`PlanningSettings.withDailyMinutes` 铺满下一份计划每天 | 2026-11-23 用户要求 |
| 2026-11-23 | `data/room/src/test/.../{schema/RoomSchemaTest,export/RoomDatabaseExporterTest,export/RoomDatabaseImporterTest,repository/SettingsRepositoryTest}.kt` | schema 测试升到 v12 并锁定 `app_settings` 字段；导出测试断言 `appSettings` 与默认 60 分钟；导入测试断言每日时长随备份恢复；新增设置仓库测试覆盖默认值、保存与 1～240 收敛 | 2026-11-23 用户要求 |
| 2026-11-23 | `docs/specs/{data-model,backup,settings,app}.md`、`CLAUDE.md`、`docs/agent-log/2026-11-23.md` | 按用户授权回写：`app_settings`（v12）、备份 `appSettings` 白名单、首页底部设置入口与关于页、每日时长单值实现说明 | 2026-11-23 用户授权 |
| 2026-11-23 | `app/.../AboutScreen.kt` | 「在浏览器中打开」由 Material `TextButton`（40dp 最小高度）改为约 28dp 的紧凑可点击文本；同步收紧页面外边距 24→20dp、区块间距 16→12dp、卡片内边距 16→14dp、应用图标 88→80dp，使关于页在常见手机上无需滚动即可完整显示 | 2026-11-23 用户要求 |
| 2026-11-23 | `app/.../AppRoot.kt` | 首页「关于」卡片增加副标题「版本 X」，版本号取自 `AppContainer.appVersionName`（PackageManager 的 versionName） | 2026-11-23 用户要求 |
| 2026-11-23 | `app/.../HomePlanPresentation.kt`（新增）、`app/.../AppRoot.kt` | 首页计划卡改为“本周完成后交换主次”：`homePlanFocus` 在本周 `COMPLETED` 时把下周升为主区块、本周降为次要行；`PlanHeroCard` 拆为 `PlanPrimaryBlock`/`PlanSecondaryRow`，`CompactPlanAction` 去掉主按钮分支；按周区分标题、按钮文案与状态说明 | C-100；2026-11-23 用户选定 |
| 2026-11-23 | `app/src/test/.../HomePlanPresentationTest.kt`（新增） | 锁定 focus（仅本周 COMPLETED 切到下周）、两个窗口的标题/主次按钮文案与状态说明 | C-100 |
| 2026-11-23 | `docs/specs/app.md`、`docs/specs/planning.md`、`docs/conflicts-archive.md`（C-100）、`docs/conflicts.md`、`docs/prompts.md`、`CLAUDE.md`、`docs/architecture/overview.md`、`docs/agent-log/2026-11-23.md` | 按用户授权回写首页主次交换规则、C-100 归档与各文档表述 | C-100；2026-11-23 用户授权 |
| 2026-11-23 | `evidence/screens/homefocus-01-current-primary.png`、`homefocus-03-next-primary.png`、`homefocus-04-next-empty.png`、`homefocus-05-restored.png`、`evidence/ui/homefocus-03-next-primary.xml`、`homefocus-04-next-empty.xml`（新增） | 实体机 VKJ-AN00 验证三态：本周为主、本周 COMPLETED 后下周为主、下周未生成时通栏“生成下周计划”；测试后恢复原数据并装回 release 包 | C-100 |
| 2026-11-23 | `data/room/.../entity/{KnowledgeCategory,KnowledgePoint}.kt`、`dao/{KnowledgeCategoryDao,BackupDao,BackupRestoreDao}.kt`、`repository/{KnowledgeCategoryRepository,KnowledgePointLibraryRepository}.kt`、`migration/RecollectMigrations.kt`、`RecollectDatabase.kt`、`schemas/.../13.json`（新增） | 新增知识点分类（schema v13 `knowledge_category`，名称唯一）；`KnowledgePoint.categoryId` 默认「语文」；`MIGRATION_12_13` 建表、按用户给定顺序种入 11 个默认分类并把旧知识点归入「语文」，空库由 `onCreate` 回调种入；新增 `KnowledgeCategoryRepository`（名称唯一、有知识点的分类不可删）；知识点新增/编辑必须指定存在的分类 | 2026-11-23 用户要求 |
| 2026-11-23 | `data/room/.../export/{DatabaseExporter,DatabaseImporter}.kt`、`dao/{BackupDao,BackupRestoreDao}.kt` | 备份新增 `knowledgeCategories` 键与知识点 `categoryId`；旧备份缺分类时补默认 11 个分类，并把缺失/失效分类的知识点归入「语文」 | 2026-11-23 用户要求 |
| 2026-11-23 | `feature/entry/.../{KnowledgePointLibraryViewModel,KnowledgePointLibraryScreen,KnowledgeCategoryFilter,QuestionEditViewModel,QuestionEditScreen}.kt`、`app/.../{AppContainer,AppRoot}.kt` | 「知识点」页搜索框下新增横向分类筛选卡（「全部」+ 各分类）与「管理分类」（增删改、有知识点不可删）；新增/编辑标签对话框要求必选分类；缩小搜索框与导航栏间距；「录入题目」知识点标签区加入同样的筛选卡并让标签列表服从筛选；分类候选由 ViewModel 透传 | 2026-11-23 用户要求 |
| 2026-11-23 | `data/room/src/test/.../{schema/RoomSchemaTest,export/RoomDatabaseExporterTest,export/RoomDatabaseImporterTest,repository/{KnowledgePointLibraryRepositoryTest,KnowledgeCategoryRepositoryTest},repository/FakeStores}.kt`、`feature/entry/src/test/.../{KnowledgePointLibraryViewModelTest,QuestionEditViewModelTest,FakeEntryPorts}.kt` | schema 测试升到 v13 并锁定 `knowledge_category` 与 `categoryId`；新增分类仓库测试；更新标签库/编辑页/导出导入测试；新增旧备份无分类时补默认分类用例 | 2026-11-23 用户要求 |
| 2026-11-23 | `CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md`、`docs/agent-log/2026-11-23.md` | 同步知识点分类的实现与文档；规格回写见下一行 | 2026-11-23 用户要求 |
| 2026-11-23 | `evidence/screens/knowledge-category-01-home.png`、`02-page.png`、`04-add-dialog.png`、`05-filter-math.png`、`07-entry-filter-math.png`、`08-entry-filter-chinese.png`、`evidence/ui/knowledge-category-03-page.xml`、`05-filter-math.xml`、`07-entry-filter-math.xml`、`08-entry-filter-chinese.xml`（新增） | 实体机 VKJ-AN00（release 正式签名覆盖安装、保留数据）验证：v12→v13 迁移后 10 个知识点全部归入「语文」；知识点页与录入页的分类筛选生效；新增标签对话框要求必选分类 | 2026-11-23 用户要求 |
| 2026-11-23 | `docs/specs/{data-model,entry,app,backup,repository}.md` | 按用户授权回写知识点分类规格：schema v13 `knowledge_category` 与 `KnowledgePoint.categoryId`、默认 11 个分类、分类增删改与删除保护、两页筛选卡的位置与行为、备份 `knowledgeCategories`/旧备份兜底、Repository 接口与校验 | 2026-11-23 用户授权 |
| 2026-11-23 | `feature/entry/.../QuestionEditScreen.kt` | 在「知识点标签」的分类筛选选项卡与知识点条目之间增加横向分割线（`HorizontalDivider`，outlineVariant），引导区分「上方筛选项 / 下方条目」 | 2026-11-23 用户要求 |
| 2026-11-23 | `evidence/screens/knowledge-category-09-entry-divider.png`、`evidence/ui/knowledge-category-09-entry-divider.xml`（新增） | 实体机 VKJ-AN00（release 覆盖安装）验证：录入页筛选卡下方出现通栏细分割线，标签条目位于线下方 | 2026-11-23 用户要求 |
| 2026-11-23 | `docs/specs/{app,entry}.md`、`docs/agent-log/2026-11-23.md` | 回写筛选卡与标签条目之间的分割线 | 2026-11-23 用户授权 |
| 2026-11-24 | `feature/print/.../{PrintPlanModels,PrintPlanViewModel,PrintPlanScreen}.kt`、`app/.../{AndroidPlanPdfSharer(AppContainer,AppRoot)}.kt`、`app/src/main/res/xml/file_paths.xml` | 打印页新增「分享 PDF」：`:feature:print` 定义 `PlanPdfSharer` 端口与 `PdfShareOutcome`，`PrintPlanViewModel.sharePdf()` 驱动 `pdfSharing` 状态与结果提示；app 层 `AndroidPlanPdfSharer` 用 `PrintLayoutBuilder`+`PrintPdfWriter` 生成缓存 PDF，经 `FileProvider` 以 `application/pdf` 打开系统分享面板（可发微信、QQ）；`file_paths.xml` 增加 `plan-pdf-exports/`；分享成功后调用同一 `PrintSnapshotWriter` 写入 `Question.pageId/qrPayload` 并把 GENERATED 计划标记为 PRINTED，首页状态标签随之更新，SUPERSEDED 计划不写 | 2026-11-24 用户要求 |
| 2026-11-24 | `feature/print/src/test/.../PrintPlanViewModelTest.kt` | 新增分享成功（传递同一份计划输入、写回每题 pageId/qrPayload、提示文件名）、分享失败（错误提示、零写入）、已被替代计划（提示未更新）与空计划不分享的用例；所有既有构造补 `pdfSharer` 假实现 | 2026-11-24 用户要求 |
| 2026-11-24 | `evidence/screens/print-share-01-chooser.png`、`print-share-02-message.png`、`evidence/ui/print-share-chooser.xml`、`print-share-after.xml`（新增） | 实体机 VKJ-AN00（release 覆盖安装）验证：打印页「分享 PDF」打开系统分享面板，展示 `拾知周计划-F0K3HE.pdf`（130 kB）与 QQ、微信、钉钉、PDF 查看入口；分享下周计划 `P12251` 后返回打印页显示“本次打印已记录”，返回首页下周计划胶囊由「待打印」变为「已打印」 | 2026-11-24 用户要求 |
| 2026-11-24 | `CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md`、`docs/agent-log/2026-11-24.md` | 同步打印页分享 PDF 的实现与“分享即记录 PRINTED”规则；`docs/specs/app.md` 的 Print 行/流程与 `print-template.md` 已按用户授权回写 | 2026-11-24 用户授权 |
| 2026-11-24 | `feature/entry/.../QuestionEditScreen.kt` | 修复「录入/编辑题目」页键盘弹出后光标手柄（小水滴）停留在旧位置：`imePadding` 让表单内部滚动、把输入框顶到键盘上方后，Compose 的手柄 Popup 只在创建时锚定一次、不会随窗口内部滚动重新定位；键盘可见期间把 `LocalTextSelectionColors.handleColor` 置为透明（光标、选区高亮、输入与手柄拖动仍正常），键盘收起后恢复显示并落在光标下方（同日提升为应用级，见下方 AppRoot 行） | 2026-11-24 用户要求 |
| 2026-11-24 | `evidence/screens/ime-check-04-edit-focused.png`、`ime-fix-01-focused.png`、`ime-fix-02-kb-hidden.png`、`ime-fix-09-manager-edit-focused.png`（新增） | 实体机 VKJ-AN00（release 覆盖安装、保留数据）验证：修复前键盘弹出后手柄停在键盘上方、与输入框脱节；修复后键盘展开期间不显示手柄，收起键盘后手柄位于光标下方；从录入页打开的编辑标签对话框手柄照常显示 | 2026-11-24 用户要求 |
| 2026-11-24 | `CLAUDE.md`、`docs/prompts.md`、`docs/agent-log/2026-11-24.md` | 同步录入页光标手柄修复与「Compose 手柄 Popup 不随窗口内部滚动重新锚定」的实现约束；本修复不涉及调度/数据/规格，`docs/specs/` 无需变更 | 2026-11-24 用户要求 |
| 2026-11-24 | `app/.../AppRoot.kt` | 修复「录入/编辑题目」页键盘弹出后输入框下方的大块白色占位：系统把未显式声明的软输入模式解析为 `adjustPan`（整窗上移），与表单 `imePadding` 叠加造成双重避让；进入录入/编辑页时把窗口 `adjust` 位设为 `adjustResize`，离开时恢复 `adjustPan`，只改 adjust 位并保留其他状态位 | 2026-11-24 用户要求 |
| 2026-11-24 | `feature/planning/src/test/.../GeneratePlanUseCaseTest.kt` | 同步「容量不足」建议文案断言：改为校验「缩短本题预估复习时长」「延长每日复习时长」「重新生成计划」三段可执行建议，与 `PlanDeferredPreview.kt` 现行文案一致 | 2026-11-24 用户要求 |
| 2026-11-24 | `evidence/screens/ime-blank-01.png`、`ime-blank-07-final.png`、`ime-blank-08-dialog.png`（新增） | 实体机 VKJ-AN00（release 覆盖安装、保留数据）验证：修复前输入框下方有大块空白；修复后顶栏保留、输入框紧贴键盘且无空白；首页「每日复习时长」对话框仍正常显示手柄 | 2026-11-24 用户要求 |
| 2026-11-24 | `CLAUDE.md`、`docs/prompts.md`、`docs/agent-log/2026-11-24.md` | 同步录入页按路由切换软输入模式的实现约束与计划文案断言 | 2026-11-24 用户要求 |
| 2026-11-24 | `app/.../AppRoot.kt` | 键盘可见期间隐藏手柄提升为应用级：在 `MaterialTheme` 内提供透明的 `LocalTextSelectionColors.handleColor`，覆盖录入页表单、各页搜索框与所有弹窗输入框；`QuestionEditScreen.kt` 回退局部实现（保留原有 `imePadding`） | 2026-11-24 用户要求 |
| 2026-11-24 | `evidence/screens/ime-all-01-dialog-ime.png`、`ime-all-02-dialog-noime.png`、`ime-all-03-form-ime.png`、`ime-all-04-form-noime.png`、`ime-all-05-knowledge-ime.png`、`ime-all-06-knowledge-noime.png`（新增） | 实体机 VKJ-AN00（release 覆盖安装、保留数据）验证：录入页、首页「每日复习时长」弹窗、知识点编辑弹窗在键盘展开期间均不显示手柄，收起键盘后手柄回到光标下方 | 2026-11-24 用户要求 |
| 2026-11-24 | `CLAUDE.md`、`docs/prompts.md`、`docs/agent-log/2026-11-24.md` | 同步「所有手动输入文本框统一在键盘可见期间隐藏手柄」 | 2026-11-24 用户要求 |
| 2026-11-24 | `app/.../AboutScreen.kt` | 「关于」页新增「致谢」卡片：记忆调度算法受 SSP-MMC-Plus（Jingyong Su 等）启发，附 IEEE TKDE 论文完整引用与「基于论文独立编写、未复制或改编原项目源代码」声明；卡片样式与「关于拾知」保持一致 | 2026-11-24 用户要求 |
| 2026-11-24 | `evidence/screens/about-01-thanks.png`、`about-02-thanks-scrolled.png`（新增） | 实体机 VKJ-AN00（release 覆盖安装、保留数据）验证：关于页底部出现「致谢」卡片，论文引用完整显示 | 2026-11-24 用户要求 |
| 2026-11-24 | `CLAUDE.md`、`docs/prompts.md`、`docs/agent-log/2026-11-24.md` | 同步关于页致谢栏目；`docs/specs/app.md` 的 About 行规格同步待授权 | 2026-11-24 用户要求 |
| 2026-11-24 | `docs/specs/app.md`、`docs/architecture/overview.md` | 按用户授权回写关于页「致谢」栏目：About 行补充致谢内容（SSP-MMC-Plus 启发、IEEE TKDE 论文引用、独立实现声明）；overview 的关于页描述同步 | 2026-11-24 用户授权 |
| 2026-11-24 | `app/.../AboutScreen.kt` | 致谢引用中的 DOI 改为可点击链接：`buildAnnotatedString` + `LinkAnnotation.Url`（主题色 + 下划线），点按经 `LocalUriHandler` 打开 `https://doi.org/10.1109/TKDE.2023.3251721` | 2026-11-24 用户要求 |
| 2026-11-24 | `evidence/screens/about-03-doi-link.png`、`about-04-doi-opened.png`、`about-05-returned.png`（新增） | 实体机 VKJ-AN00（release 覆盖安装）验证：DOI 显示为下划线主题色链接；点按后系统浏览器打开 IEEE Xplore 页面，返回后关于页保持原状 | 2026-11-24 用户要求 |
| 2026-11-24 | `docs/specs/app.md`、`CLAUDE.md`、`docs/prompts.md`、`docs/agent-log/2026-11-24.md` | 同步 DOI 可点击链接的实现与规格 | 2026-11-24 用户授权回写 |
| 2026-11-24 | `LICENSE`（新增） | 仓库采用 GNU General Public License v3.0；以 gnu.org 标准全文落地，任何派生分发必须同样开源 | 2026-11-24 用户要求 |
| 2026-11-24 | `NOTICE`（新增） | 记录项目版权（2026 ziyangdev910）与 GPL-3.0 声明，并列出第三方组件许可（AndroidX/Kotlin/OpenCV/ZXing 为 Apache-2.0，ML Kit 为 Google ML Kit Terms，JUnit 5 为 EPL-2.0，OpenScanVision 为 MIT 且仅测试使用） | 2026-11-24 用户要求 |
| 2026-11-24 | `README.md`（新增） | 新增项目说明、功能、技术栈、构建方式与许可证段落；明确 GPL-3.0 下分发 APK 需提供对应源代码、禁止闭源上架 | 2026-11-24 用户要求 |
| 2026-11-24 | `app/.../AboutScreen.kt` | 「关于」页作者/仓库卡片新增「开源许可证：GNU GPL v3.0」一行（`InfoRow` + 分割线），并更新页面注释 | 2026-11-24 用户要求 |
| 2026-11-24 | `docs/agent-log/2026-11-24.md` | 记录开源许可证落地与编译验证 | — |
| 2026-11-24 | `LICENSE-EXCEPTION`（新增）、`NOTICE`、`README.md` | 许可证细化为 `GPL-3.0-only WITH Classpath-exception-2.0`：新增 Classpath Exception 2.0 全文；NOTICE/README 更新 SPDX 标识并说明该例外允许链接 ML Kit 等闭源独立模块，不改变自身与衍生 App 的 GPL 开源义务 | 2026-11-24 用户要求 |
| 2026-11-24 | `app/.../AboutScreen.kt` | 关于页许可证显示改为「GPL-3.0 with Classpath Exception」 | 2026-11-24 用户要求 |
| 2026-11-24 | `docs/specs/app.md`、`CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md` | 按用户授权回写关于页开源许可证与项目许可证策略 | 2026-11-24 用户授权 |
| 2026-11-24 | `NOTICE`、`README.md`、`app/.../AboutScreen.kt` | 补充算法与公式归属：DHP 公式来源为 MIT 许可的 `maimemo/SSP-MMC`，`SSP-MMC-Plus` 为论文与设计参考；NOTICE 增加版权、用途与 MIT 全文，README 增加「算法来源」，关于页声明改为“公式参考、实现独立编写” | 2026-11-24 用户要求 |
| 2026-11-24 | `docs/research/maimemo-model.md`、`docs/specs/app.md`、`CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md` | 同步公式来源与关于页致谢声明 | 2026-11-24 用户授权回写 |
| 2026-11-24 | `app/src/main/assets/licenses/{LICENSE,LICENSE-EXCEPTION,NOTICE}.txt`（新增）、`app/.../LicenseScreen.kt`（新增）、`navigation/AppNavigator.kt`、`AppRoot.kt`、`AboutScreen.kt` | 许可证全文随 APK 打包：关于页「查看许可证全文」进入 `AppRoute.License`，三个选项卡展示 GPL-3.0、Classpath 例外与 NOTICE；assets 副本与根目录文件保持同步 | 2026-11-24 用户要求 |
| 2026-11-24 | `docs/specs/app.md`、`CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md` | 同步 License 路由、屏幕清单、关于页入口与 assets 副本同步约束 | 2026-11-24 用户授权回写 |
| 2026-11-24 | `evidence/screens/license-01-gpl.png`、`license-02-exception.png`、`license-03-notice.png`、`license-04-back-about.png`（新增） | 实体机 VKJ-AN00（release 覆盖安装）验证：关于页「查看许可证全文」进入开源许可页，GPL-3.0 / 例外条款 / NOTICE 三个选项卡正文完整显示，返回关于页正常 | 2026-11-24 用户要求 |
| 2026-11-24 | `app/.../AppRoot.kt` | 首页删除「管理知识点」「管理题目来源」两个快捷入口及其回调参数（`AppRoute.KnowledgePoints`/`AppRoute.SourceTags` 路由与页面保留）；快捷卡片高度由 56dp 增至 72dp，其余入口更易点按 | 2026-11-24 用户要求 |
| 2026-11-24 | `CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md`、`docs/specs/app.md` | 同步首页两行 2×2 快捷操作（录入题目 / 管理题库、扫描回读 / 查看复习进度）与 72dp 卡片高度；app.md 的 Home 行、SourceTags 行与 Home → SourceTags 流程按用户授权回写为「首页不再提供管理类入口，路由与页面保留」 | 2026-11-24 用户要求；用户授权回写 |
| 2026-11-24 | `evidence/screens/home-actions-2026-11-24.png`、`evidence/ui/home-actions-2026-11-24.xml`（新增） | 实体机 VKJ-AN00（release 覆盖安装、保留数据）验证：首页仅剩四个快捷卡片，两行 2×2 顺序正确、卡片高度 72dp，首屏完整显示 | 2026-11-24 用户要求 |
| 2026-11-24 | `feature/entry/.../QuestionEditScreen.kt` | 编号字段标题随来源切换：未选来源时显示「试卷编号」，选中来源后显示「页码编号」；存储、默认值与 +/− 行为不变 | 2026-11-24 用户要求 |
| 2026-11-24 | `evidence/screens/entry-label-01-no-source.png`、`entry-label-02-with-source.png`、`evidence/ui/entry-label-01-no-source.xml`、`entry-label-02-with-source.xml`（新增） | 实体机 VKJ-AN00（release 覆盖安装）验证：新建页未选来源时标题为「试卷编号」（编号 1，+/- 只读）；选中「化学练透」后标题变为「页码编号」并套用带来源默认值 50；取消选择后标题回到「试卷编号」、编号回到 1 | 2026-11-24 用户要求 |
| 2026-11-24 | `CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md`、`docs/agent-log/2026-11-24.md` | 同步编号字段标题随来源切换的表述、本轮任务记录与实体机验证结果 | 2026-11-24 用户要求 |
| 2026-11-24 | `docs/specs/entry.md`、`docs/specs/app.md` | 按 2026-11-24 用户授权回写：新增「编号字段标题随来源切换」条款（未选来源「试卷编号」/已选来源「页码编号」，字段语义与编辑方式不变）与用例 E-20，app.md 的 QuestionEdit 行同步标题切换；changelog、agent-log、prompts 同步记为已授权 | 2026-11-24 用户授权回写 |
| 2026-11-24 | `app/.../navigation/AppNavigator.kt`、`app/.../AppRoot.kt`、`app/.../SuccessToast.kt`（由 `ImportToast.kt` 重命名） | 录入/编辑题目保存成功后先弹带绿色对勾的「保存成功」Toast；`QuestionEdit` 路由增加 `fromHome`，首页「录入题目」以 `fromHome = true` 入栈，保存后 `replace(AppRoute.Entry)` 强制跳到题库页（返回键回首页）；题库页入口保持原逻辑 `markNeedsRefresh()` + `pop()`；Toast 工具抽出通用 `showSuccessToast`，导入成功与保存成功共用样式 | 2026-11-24 用户要求 |
| 2026-11-24 | `evidence/screens/entry-save-01-toast.png`、`entry-save-02-toast-from-entry.png`、`evidence/ui/entry-save-02-from-home.xml`、`entry-save-03-from-entry.xml`（新增） | 实体机 VKJ-AN00（release 覆盖安装、保留数据）验证：首页进入保存后出现「✓ 保存成功」并落到题库（6→7 题），返回键回首页；题库页进入保存后同样提示并返回题库（7→8 题） | 2026-11-24 用户要求 |
| 2026-11-24 | `CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md`、`docs/agent-log/2026-11-24.md` | 同步录入页保存成功提示与跳转分流 | 2026-11-24 用户要求 |
| 2026-11-24 | `docs/specs/app.md`、`docs/specs/entry.md` | 按用户授权回写：`QuestionEdit` 路由签名增加 `fromHome`；QuestionEdit 屏幕行补充保存成功 Toast 与入口分流；Entry → QuestionEdit 与 Home → QuestionEdit 的保存后行为分别写明；entry.md 列表状态/新增条款同步，新增用例 E-21（保存提示与跳转） |
| 2026-11-24 | `data/room/.../entity/{Question,KnowledgePoint}.kt`、`dao/{QuestionDao,KnowledgePointDao}.kt`、`repository/{RoomEntryRepository,KnowledgePointLibraryRepository}.kt`、`migration/RecollectMigrations.kt`、`RecollectDatabase.kt`、`schemas/.../14.json`（新增） | 题库与知识点列表改为按用户修改时间倒序（schema v14）：`Question`/`KnowledgePoint` 增加 `updatedAt`（默认 `createdAt`，列默认值 0），新建/编辑题目或知识点时写入当前时间；新增 `activeOrderedByUpdatedAt()` 与 `allOrderedByUpdatedAt()` 查询（题库时间相同按 rowid、知识点按 number 兜底）；`MIGRATION_13_14` 补列并把旧数据回填为 `createdAt`；调度、打印与归档的系统写回不改变 `updatedAt` | 2026-11-24 用户要求 |
| 2026-11-24 | `data/room/.../export/{DatabaseExporter,DatabaseImporter}.kt` | 备份导出 `Question.updatedAt` 与 `KnowledgePoint.updatedAt`；导入时缺失该字段（v13 及更早备份）回退为 `createdAt` | 2026-11-24 用户要求 |
| 2026-11-24 | `data/room/src/test/.../{schema/RoomSchemaTest,export/RoomDatabaseExporterTest,export/RoomDatabaseImporterTest,repository/{EntryRepositoryTest,KnowledgePointLibraryRepositoryTest},repository/FakeStores}.kt` | schema 测试升到 v14 并锁定两个 `updatedAt` 列；导出/导入测试同步版本与字段（含旧备份回退 `createdAt`）；新增「编辑排最前」的 Repository 测试，覆盖题库与知识点；Fake DAO 实现新查询 | 2026-11-24 用户要求 |
| 2026-11-24 | `evidence/screens/order-verify-*`、`evidence/ui/order-verify-*`（新增） | 实体机 VKJ-AN00（release 正式签名覆盖安装、保留数据，v13→v14 迁移）：题库编辑靠后题目后保存，该题立即出现在列表最前；知识点编辑「虚拟语气」与新增测试标签「zzz」后均立即置顶，验证后已恢复名称并删除测试标签 | 2026-11-24 用户要求 |
| 2026-11-24 | `CLAUDE.md`、`docs/architecture/overview.md`、`docs/specs/{entry,data-model,backup,app}.md` | 按用户需求同步：题库/知识点列表按 `updatedAt` 倒序、schema v14 字段与迁移回填、备份字段矩阵与旧备份兜底、用例 E-22 | 2026-11-24 用户授权回写 | 2026-11-24 用户授权回写 |
| 2026-11-24 | `feature/entry/.../KnowledgePointLibraryScreen.kt` | 删去「知识点」页搜索框与关键词过滤（含 `query` 状态）；分类筛选、空态与「共 N 个标签」计数保留，列表直接按所选分类展示 | 2026-11-24 用户要求 |
| 2026-11-24 | `feature/entry/.../SourceTagLibraryScreen.kt` | 删去「来源标签」页搜索框与关键词过滤（含 `query` 状态）；名称列表与新增/重命名/删除不变 | 2026-11-24 用户要求 |
| 2026-11-24 | `docs/specs/app.md`、`docs/specs/entry.md` | 按用户要求删去 KnowledgePoints / SourceTags / 来源标签窗口的搜索描述；entry.md 来源标签库与分类筛选位置同步为页面顶部（筛选卡位于「共 N 个标签」与「新增标签」之上） | 2026-11-24 用户要求 |
| 2026-11-24 | `CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md`、`docs/agent-log/2026-11-24.md` | 同步删去「知识点」页与「来源标签」页搜索框的产品与任务描述 | 2026-11-24 用户要求 |
| 2026-11-24 | `evidence/screens/search-removal-*.png`、`evidence/ui/search-removal-*.xml`（新增） | 实体机 VKJ-AN00（release 正式签名覆盖安装、保留数据）验证：「知识点」页无搜索框、分类筛选卡位于顶部且「英语」筛选显示空态、切回「全部」恢复正常；「来源标签」页无搜索框、列表与增删改入口正常；题库页搜索框保持不变。验证未改动数据 | 2026-11-24 用户要求 |
| 2026-11-24 | `feature/print/.../PrintLayout.kt`、`feature/print/src/test/.../PrintLayoutTest.kt` | 打印稿题目右上第一行删除「页码 XX」，只保留「题号 X　来源页 Y」；删除 `pageLineText` 与 `itemLayout` 中不再使用的 `pageNumber` 参数；QR 载荷与页脚小字 pageId 不变，测试断言同步；`./gradlew :feature:print:test` 通过 | 2026-11-24 用户要求 |
| 2026-11-24 | `docs/specs/print-template.md`、`CLAUDE.md`、`docs/architecture/overview.md`、`docs/prompts.md` | 按用户要求回写打印稿右上第一行格式（删除「页码 XX」，保留题号与来源页），同步 D-07/D-09 摘要、Print 字段产权行与落地说明 | 2026-11-24 用户授权回写 |
| 2026-11-24 | `core/pagination/.../TemplateGeometry.kt`、`DefaultPagePlanner.kt`、`ScanGeometryProjection.kt` | 新排版几何：新增 `topSectionHeightMm`（左上填涂行与右上信息行取高者）、`noteRowHeightMm`（有笔记区时 `max(noteAreaBaseHeightMm, qrStackHeightMm)`，无笔记区时 `qrFooterHeightMm`）、`itemHeightMm`、`bubbleRowTopMm`、`noteWritingWidthMm`、`qrColumnLeftMm`、`qrStackHeightMm`、`noteQrColumnGapMm`（默认 3mm）；单题高度不再叠加 QR 页脚带；`ScanGeometryProjection` 的书写区改为通栏左侧（右侧扣除 QR 列） | 2026-11-24 用户要求 |
| 2026-11-24 | `feature/print/.../PrintLayout.kt`、`PrintRenderer.kt` | 每题排版改为左上填涂区、右上题号/来源/知识点；下方通栏历史笔记显示区（`NotesDisplayLayout`）+ 复习笔记书写区（`NoteAreaLayout`，宽度扣除 QR 列）；QR 与 pageId 标签在书写行内垂直并排；`hasNoteArea=false` 仍用题块底部 QR 预留带；渲染器按新结构绘制 | 2026-11-24 用户要求 |
| 2026-11-24 | `feature/scan/.../OpenCvScanEngine.kt` | OMR 行起点改用 `TemplateGeometry.bubbleRowTopMm`（与打印共用）；填涂区仍在题块左上角，列中心、2.5mm 采样窗、30%/10% 阈值与唯一命中优先规则不变；QR 归属容差沿用 `qrFooterHeightMm/2` | 2026-11-24 用户要求 |
| 2026-11-24 | `core/pagination/src/test/.../{PagePlannerTest,PagePlannerPropertyTest,ScanGeometryProjectionTest,TemplateGeometryTest}.kt`、`feature/print/src/test/.../PrintLayoutTest.kt` | 按新高度公式与新排版重算用例：条目高度、yOffset、笔记显示区/书写区/QR 坐标；新增 `TemplateGeometry` 派生量用例；P-16 改为「顶部取较高者」语义、P-19 改为「QR 与书写区并排」语义 | 2026-11-24 用户要求 |
| 2026-11-24 | `docs/specs/{print-template,pagination,scan-pipeline}.md`、`CLAUDE.md`、`docs/architecture/overview.md` | 按用户授权回写新打印排版、单题高度公式、OMR/QR 绑定与笔记 OCR 区域；同步 D-07 摘要与 Print 字段产权 | 2026-11-24 用户授权回写 |
| 2026-11-24 | `docs/prompts.md`、`docs/agent-log/2026-11-24.md` | 记录本轮排版调整、验证命令与结果（单测/构建通过；`:feature:print:connectedDebugAndroidTest` 3 项通过；`:feature:scan` 设备用例被打断未完成，已清理残留进程） | 工作循环 |
| 2026-11-25 | `core/pagination/.../TemplateGeometry.kt` | 题间距 `itemSpacingMm` 由 3.0mm 减半为 1.5mm；打印布局、分页高度与扫描几何共用该值，排版代码无第二处硬编码 | 2026-11-25 用户要求 |
| 2026-11-25 | `core/pagination/src/test/.../{PagePlannerTest,TemplateGeometryTest}.kt`、`feature/print/src/test/.../PrintLayoutTest.kt` | 按 1.5mm 重算条目高度、yOffset、blockTop 与笔记/QR 坐标；P-02 恰好填满用例改为 pageHeight 228 / 内容区 197 | 2026-11-25 用户要求 |
| 2026-11-25 | `docs/specs/pagination.md`、`docs/agent-log/2026-11-25.md`（新增） | 同步题间距默认值与本轮记录 | 2026-11-25 用户授权回写 |
| 2026-11-25 | `feature/print/.../PrintLayout.kt` | 实测 PDF 空白带后二次修正：书写横线改为铺到书写行底部、最后一条距底 1mm（`WRITING_BOTTOM_PADDING_MM`），两道题之间的可见空白由约 7.5mm 降到约 4.2mm；书写区高度与 OCR 区域不变 | 2026-11-25 用户要求 |
| 2026-11-25 | `feature/print/src/test/.../PrintLayoutTest.kt`、`docs/specs/print-template.md` | 新增横线位置断言并回写书写区横线分布说明 | 2026-11-25 用户授权回写 |
| 2026-11-25 | `evidence/print/spacing-2026-11-25-{before,after}/`（新增） | 真机生成的 PDF 与 300 DPI PNG，用于对比题间空白（before 7.54mm → after 4.23mm） | 2026-11-25 用户要求 |
| 2026-11-25 | `core/pagination/.../TemplateGeometry.kt`、`feature/print/.../PrintLayout.kt` | 题间距改为「复习笔记横线间距」的视觉节奏：新增横线条数/底部内边距与横线偏移、`noteQrTopOffsetMm`；`itemSpacingMm` = 5mm；QR + pageId 标签组合底边与最后一条横线对齐 | 2026-11-25 用户要求 |
| 2026-11-25 | `core/pagination/src/test/.../{PagePlannerTest,TemplateGeometryTest}.kt`、`feature/print/src/test/.../PrintLayoutTest.kt` | 按 5mm 题间距重算高度/坐标；新增横线偏移与 QR 对齐断言 | 2026-11-25 用户要求 |
| 2026-11-25 | `docs/specs/{print-template,pagination}.md`、`evidence/print/spacing-2026-11-25-final/`（新增） | 回写题间距=横线间距、QR 底边对齐最后一条横线的规则；真机 PDF/PNG 实测横线间距 8.0mm、题间可见空白 7.8mm | 2026-11-25 用户授权回写 |
