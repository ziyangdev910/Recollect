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
