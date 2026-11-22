# Repository 与模型映射规格（data/repository + data/room + core/model）

关联决策：D-01、D-04、D-11、D-12、D-13
关联裁决：C-015、C-046、C-070、C-071、C-072、C-073、C-074、C-076、C-077、C-078、C-079、C-080、C-089
关联规格：`planning.md`、`scheduler.md`、`pagination.md`、`feedback-usecase.md`

## 边界

- ViewModel 不直接访问 Room、CameraX、PrintManager、ML Kit（R-03）。
- Repository 返回领域模型/Scheduler/Pagination DTO，不向上暴露 Room `@Entity`。
- 所有跨表写操作使用一个事务；UseCase 只调用 Repository 的事务方法，不逐条拼事务。
- `core/model` 只放纯 DTO/枚举与 `KnowledgeRowsCodec`，不依赖 Android；Room Entity ↔ DTO 的映射放在 `data/room`（C-046 方案 A）。

## Repository 接口

```kotlin
interface KnowledgePointRepository {
    suspend fun states(): List<KnowledgeState>
    suspend fun allocateNumber(): Int
    suspend fun updateState(state: KnowledgeState)
    suspend fun onQuestionArchived(questionId: String)    // activeQuestionCount--
    suspend fun onQuestionUnarchived(questionId: String)  // activeQuestionCount++
    suspend fun deleteWithTombstone(knowledgePointId: String, reason: String)
}

interface QuestionRepository {
    suspend fun schedulerQuestions(): List<SchedulerQuestion>
    suspend fun markScheduled(questionId: String, targetDate: LocalDate)
    suspend fun setPrintSnapshot(questionId: String, pageId: String, qrPayload: String)
    suspend fun archive(questionId: String)
    suspend fun unarchive(questionId: String)
}

interface PlanRepository {
    suspend fun pendingCoolingOverrides(): List<CoolingOverrideEvent>
    // Plan 头 + PlanPage + lastScheduledDate + consume + deferrals，同一事务（C-097）
    suspend fun savePlan(draft: PlanDraft)
    suspend fun activePlan(weekStart: LocalDate, weekEnd: LocalDate): StoredPlan?
    suspend fun findPlan(planId: String): StoredPlan?
    suspend fun storedPages(planId: String): List<StoredPlanPage>
    // 只允许 GENERATED/PRINTED 计划写入；同一事务内写题目快照并标记 PRINTED
    suspend fun savePrintSnapshots(
        planId: String,
        snapshots: List<PrintSnapshotRecord>,
        printedAt: Instant
    ): Boolean
    suspend fun getPlanPage(planId: String, pageId: String): PlanPage?
    suspend fun cleanupPlanPages(keepWeeks: Int)
}

interface BackupRepository {
    suspend fun export(): BackupEnvelope
    suspend fun import(envelope: BackupEnvelope): ImportReport
}

interface FeedbackRepository {
    suspend fun pending(sessionId: String): List<PendingFeedback>
    suspend fun session(sessionId: String): ConfirmationSession?
    suspend fun saveDecisions(
        sessionId: String,
        decisions: List<ConfirmedDecision>,
        updatedKnowledgeStates: List<KnowledgeState>,
        questionLastReviewedDates: Map<String, LocalDate>,
        noteAttachments: List<NoteAttachmentDraft> = emptyList()
    ): SaveDecisionsResult
    suspend fun deletePending(sessionId: String)
    suspend fun planCompletion(planId: String): PlanCompletion
}

// 扫描会话的只读投影；Repository 不向上暴露 Room @Entity（C-079/C-080 方案 A）。
data class ConfirmationSession(
    val sessionId: String,
    val planId: String,
    val status: ScanSessionStatus
)

data class PlanCompletion(
    val planId: String,
    val plannedPages: Int,
    val scannedPages: Int,
    val reviewedItems: Int,
    val missedItems: Int,
    val lateItems: Int
)

// C-074 方案 A：计划草稿与确认提交的入参/结果放在 data:room；编排层只构造入参与展示。
data class PlanDraft(
    val planId: String,
    val pages: List<PlannedPage>,
    val knowledgeRowsByQuestionId: Map<String, PlanPageRows>, // 打印时冻结的每题行快照（C-071/C-072）
    val scheduledDates: Map<String, LocalDate>,                // questionId -> targetDate
    val coolingEventIdsToConsume: List<Long>,
    val deferred: List<DeferredItem>
)

class PlanGenerationException(
    val code: PaginationFailureCode,
    message: String
) : Exception(message)

data class NoteAttachmentDraft(
    val questionId: String,
    val knowledgePointId: String, // 用于关联本事务写入的 ReviewFeedback
    val text: String,
    val imageUri: String? = null
)

data class SaveDecisionsResult(
    val writtenFeedbackCount: Int,
    val writtenDecisionLogCount: Int,            // 含整题跳过的行（C-078 方案 A）
    val skippedQuestionIds: List<String>,        // C-055：关联失效被整题跳过的题目
    val skippedKnowledgePointIds: List<String>   // 对应解析不到的关联 id，不补默认知识点
)

interface NoteAttachmentRepository {
    suspend fun add(questionId: String, text: String, imageUri: String?)
    suspend fun setAttachToPrint(questionId: String, attach: Boolean)
    suspend fun items(questionId: String, limit: Int = 3): List<NoteAttachmentItem>
}

interface ScanRepository {
    suspend fun startSession(planId: String): ScanSession   // 已有 SCANNING/READY 则复用
    suspend fun appendPending(sessionId: String, rows: List<PendingFeedback>)
    suspend fun updateActualReviewDate(sessionId: String, questionId: String, knowledgePointId: String, date: LocalDate?)
    suspend fun markReady(sessionId: String)
    suspend fun markSubmitted(sessionId: String)
    suspend fun abandon(sessionId: String)
}

interface SettingsRepository {
    fun settings(): Flow<AppSettings>
    suspend fun update(settings: AppSettings)
}
```

### 事务规则

- `PlanRepository.savePlan(draft)` 必须在一个 Room 事务内完成：写 `PlanPage`（`knowledgeRows` 只编码草稿中的冻结行快照，不从当前关联重算，C-071/C-072）、更新 `Question.lastScheduledDate`、消费 `CoolingOverrideEvent`、应用冷却顺延。分页 `Failure` 在进入事务前转 `PlanGenerationException`（C-047）。冷却顺延不另设独立的 UseCase，统一由本事务写入（C-076 方案 A）。
- `FeedbackRepository.saveDecisions` 必须在一个事务内完成：写 `ReviewFeedback`、`ConfirmationDecisionLog`、按 `updatedKnowledgeStates` 更新 `KnowledgePoint`（`continuousScore` 已是 `Float`，C-050）、按 `questionLastReviewedDates` 更新 `Question.lastReviewedDate`、写 `NoteAttachmentItem`、删除 `PendingFeedback`。知识点状态由 UseCase 计算，Repository 不重算。
- 任何一步失败 → 整体回滚；`PendingFeedback` 保留供重试。
- 关联 id 解析不到的题目整题跳过、不补默认知识点，其待确认行保留供数据修复后重试；跳过清单通过 `SaveDecisionsResult` 返回（C-055 方案 C）。
- **整题跳过的审计与状态（C-077/C-078 方案 A）**：跳过题目仍写该题所有行的 `ConfirmationDecisionLog`，但不写 `ReviewFeedback`、不写知识点状态、不写 `Question.lastReviewedDate`、不写 `NoteAttachmentItem`；`updatedKnowledgeStates` 只落库“仍被未跳过题目引用”的知识点。
- **会话状态（C-080 方案 A）**：整批无跳过时在同一事务把 `ScanSession.status` 置为 `SUBMITTED` 并写 `submittedAt`；存在跳过时保持 `READY`，只删除已写入题目的待确认行，跳过行保留供修复后重试。`SUBMITTED` 会话的重复提交由 `ApplyFeedbackUseCase` 在进入事务前拒绝。
- **前置校验归属（C-079 方案 A）**：空决策、未标记、重复行、会话状态、待确认行集合、计划日/触发标记、知识点归属、复习日上下界、档位可用由 `ApplyFeedbackUseCase` 校验并抛 `InvalidConfirmationException`；Repository 保留非空/未标记/重复键等防御性校验，作为直接调用者的最后防线。
- `PlanDraft`、`PlanGenerationException`、`SaveDecisionsResult` 与 `NoteAttachmentDraft` 定义在 `data:room`；`feature/planning` 与确认页只负责构造入参、展示跳过清单（C-074 方案 A）。
- `ScanRepository.startSession(planId)`：同一 plan 已有 `SCANNING`/`READY` 会话则复用；`SUBMITTED`/`ABANDONED` 才新建。
- `appendPending` 按 `(planId,pageId,questionId,knowledgePointId)` 去重，保留最新识别结果。

### PlanCompletion 派生

- `plannedPages`：该 `planId` 的 `PlanPage` 数量。
- `scannedPages`：该 plan 的 `ScanSession`/`ConfirmationDecisionLog` 中出现过的 `pageId` 去重数。
- `reviewedItems`：`ConfirmationDecisionLog` 中该 plan 的非 `NOT_REVIEWED` 行数。
- `missedItems`：`plannedDate <= 今天` 且该 `(planId,pageId,questionId)` 无日志或全部 `NOT_REVIEWED` 的行数。
- `lateItems`：`reviewedOn > plannedDate` 的行数。
- 不落库；查询结果用于 Home/Confirm 的完成度展示。

## core/model 与 data/room 映射（C-046、C-071、C-073 方案 A）

`core:model` 只放纯 DTO/枚举与 Codec，不出现 `android.*` / `androidx.*`；打印题目投影 `RenderableQuestion` 属于 `core:pagination`，不在 `core:model` 重复定义（C-073 方案 A）：

```kotlin
// core:model
enum class Tier { UNKNOWN, FAMILIAR, MASTERED }
data class KnowledgeState(/* 见 scheduler.md */)
data class SchedulerQuestion(/* 见 scheduler.md */)
data class KnowledgeRow(val knowledgePointId: String, val number: Int)
data class PlanPageRows(
    val questionId: String, // 该记录对应的题目（C-070）
    val rows: List<KnowledgeRow>,
    val triggeredKnowledgePointIds: List<String>
)
object KnowledgeRowsCodec {
    fun decode(json: String): List<PlanPageRows>
    fun encode(rows: List<PlanPageRows>): String
}

// core:pagination（C-073 方案 A）
data class RenderableQuestion(/* 见 pagination.md */)
```

Room Entity ↔ DTO 的映射放在 `data/room`：

```kotlin
// data:room
fun KnowledgePoint.toKnowledgeState(): KnowledgeState
fun Question.toSchedulerQuestion(linkedKnowledgeIds: List<String>): SchedulerQuestion
fun PlannedQuestion.toRenderableQuestion(
    questionNumber: String,
    knowledgeNames: List<String>,
    knowledgeLineCount: Int,
    attachmentCount: Int
): RenderableQuestion
// 只解码 PlanPage.knowledgeRows 的冻结 JSON；一页可有多条记录（C-071）
fun PlanPage.toPlanPageRows(): List<PlanPageRows>
```

- `RenderableQuestion` 由 `core:pagination` 提供（C-073 方案 A）；`data:room` 通过依赖 `core:pagination` 构造打印投影，`core:model` 不重复定义同名类型。
- `KnowledgePoint.difficulty/halfLifeDays` 直接映射到 `KnowledgeState`。
- `Question` 实体没有 `linkedKnowledgeIds` 列；`data/room` 从 `QuestionKnowledgeCrossRef` 读取并按 `KnowledgePoint.number` 升序构造该列表。scheduler 不依赖输入列表顺序（C-049）。
- `data/room` 构造调度 DTO 时保证 `knowledgeStates` 与 `questions` 的 id 唯一、每条 `linkedKnowledgeIds` 内无重复，并在正常情况下保证每个关联 id 都能解析到知识点（C-056 方案 A）；备份导入/数据损坏导致的悬空关联由 scheduler 按 C-055 方案 C 跳过并留下 `UNRESOLVED_KNOWLEDGE` 记录。
- `PlannedQuestion` 没有 `questionNumber`；映射时由 `data/room` 从 `Question.questionNumber` 补齐。
- `PlannedQuestion.toRenderableQuestion` 的 `knowledgeNames` 必须与左侧填涂行属于同一 printable 集合（`!dormant && activeQuestionCount > 0`）、按 `KnowledgePoint.number` 升序，且数量等于 `knowledgeCount`（C-067 方案 B）；分页器只消费、不补齐/截断/重算。
- `Question.coolingPeriodOverrideDays` 已删除；调度只通过 `coolingOverrides` 接收一次性事件。
- `PlanPage.knowledgeRows` 的 JSON 编解码集中在 `core:model` 的 `KnowledgeRowsCodec`，Print/Scan 共用；`core:model` 不依赖 Room 或 Android。
- `PlanPageRows` 按题目记录行快照：`questionId` 对应本页题目，`rows` 顺序保持打印时的行序，不得按 `number` 重排（C-015、C-070）；`triggeredKnowledgePointIds` 原样保留。`data:room` 只做 `PlanPage` 行与 `List<PlanPageRows>` 的映射，不自行拼 JSON；映射来源是 `PlanPage.knowledgeRows` 的冻结 JSON，只解码为 `List<PlanPageRows>`（每题一条，一页可有多条），不得从当前题目-知识点关联重新生成行，不得合并或丢弃一页内多条记录（C-071 方案 A）。
- `Room Entity ↔ DTO` 的转换不改变字段语义；缺失/非法值按对应规格的默认值处理（如 `estimatedReviewMinutes=15`）；`knowledgeRows` 为空或不可解析时不得按当前关联重算，应视为无快照并提示重新生成计划、按新稿打印（C-072 方案 A）。

## 归档/删除的原子操作

- `QuestionRepository.archive`：`archived=true`、`archivedAt=now`，并对每个关联知识点调用 `onQuestionArchived`；若计数为 0 则 `dormant=true`。
- `unarchive` 反向操作；`continuousScore` 不变。
- `KnowledgePointRepository.deleteWithTombstone`：先检查是否仍有未归档题目引用；有则拒绝；否则写墓碑并删除。
- 题目不提供硬删除；历史反馈永远保留。

## 验收

- Repository 单元测试覆盖：事务回滚、并发编号分配、归档计数、`savePlan` 部分失败不落库。
- C-077～C-080、C-089 方案 A：跳过题目不写反馈/状态/复习日但仍写决定日志；整批成功标记会话 `SUBMITTED`；已提交会话重复提交被拒绝；反馈/日志的计划号、页号与计划日有断言。
- 前置校验（C-079 方案 A）：空决策、重复行、会话未就绪/已提交、待确认行集合不完整、计划日/触发标记不一致、知识点不属于题目、复习日上下界、档位缺失均有负例测试。
- C-074 方案 A：`savePlan`/`saveDecisions` 的成功路径与「任一步失败整体回滚」均有自动化用例；`SaveDecisionsResult` 返回关联失效的整题跳过清单，且不补默认知识点。
- C-097：`savePlan` 同一事务内写 `plan` 计划头；若 `supersedesPlanId` 非空，把旧计划标记为 SUPERSEDED。同一周已有非替代计划时拒绝保存，防止隐式替换。`savePrintSnapshots` 只对 GENERATED/PRINTED 计划写入 `Question.pageId/qrPayload` 并标记 PRINTED，SUPERSEDED 计划返回 `false`，调用方不得提示“已记录”。`renderMeta` 由 `planning` 冻结、`data:room` 只编码/解码，不在回读时按当前题库重算。
- `data/room` 映射测试覆盖：Entity ↔ Scheduler/Pagination DTO 字段一一对应，无丢失/重命名漂移；`PlanPage.knowledgeRows` 解码后每题一条、条数与顺序不变，且不按 `number` 重排（C-071）。
- `core:model` 纯度测试：`src/main` 与 `src/test` 均不得出现 `android.*` / `androidx.*` import；`KnowledgeRowsCodec` 可独立测试。
