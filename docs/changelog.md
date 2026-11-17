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
