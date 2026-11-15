# Repository 与模型映射规格（data/repository + data/room + core/model）

关联决策：D-01、D-04、D-11、D-12、D-13
关联裁决：C-046
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
    suspend fun savePlan(draft: PlanDraft)   // PlanPage + lastScheduledDate + consume + deferrals
    suspend fun getPlanPage(planId: String, pageId: String): PlanPage?
    suspend fun cleanupPlanPages(keepWeeks: Int)
}

interface BackupRepository {
    suspend fun export(): BackupEnvelope
    suspend fun import(envelope: BackupEnvelope): ImportReport
}

interface FeedbackRepository {
    suspend fun pending(sessionId: String): List<PendingFeedback>
    suspend fun saveDecisions(sessionId: String, decisions: List<ConfirmedDecision>)
    suspend fun deletePending(sessionId: String)
    suspend fun planCompletion(planId: String): PlanCompletion
}

data class PlanCompletion(
    val planId: String,
    val plannedPages: Int,
    val scannedPages: Int,
    val reviewedItems: Int,
    val missedItems: Int,
    val lateItems: Int
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

- `PlanRepository.savePlan(draft)` 必须在一个 Room 事务内完成：写 `PlanPage`、更新 `Question.lastScheduledDate`、消费 `CoolingOverrideEvent`、应用冷却顺延。
- `FeedbackRepository.saveDecisions` 必须在一个事务内完成：写 `ReviewFeedback`、`ConfirmationDecisionLog`、更新 `KnowledgePoint`、写/更新 `NoteAttachmentItem`、删除 `PendingFeedback`。
- 任何一步失败 → 整体回滚；`PendingFeedback` 保留供重试。
- `ScanRepository.startSession(planId)`：同一 plan 已有 `SCANNING`/`READY` 会话则复用；`SUBMITTED`/`ABANDONED` 才新建。
- `appendPending` 按 `(planId,pageId,questionId,knowledgePointId)` 去重，保留最新识别结果。

### PlanCompletion 派生

- `plannedPages`：该 `planId` 的 `PlanPage` 数量。
- `scannedPages`：该 plan 的 `ScanSession`/`ConfirmationDecisionLog` 中出现过的 `pageId` 去重数。
- `reviewedItems`：`ConfirmationDecisionLog` 中该 plan 的非 `NOT_REVIEWED` 行数。
- `missedItems`：`plannedDate <= 今天` 且该 `(planId,pageId,questionId)` 无日志或全部 `NOT_REVIEWED` 的行数。
- `lateItems`：`reviewedOn > plannedDate` 的行数。
- 不落库；查询结果用于 Home/Confirm 的完成度展示。

## core/model 与 data/room 映射（C-046 方案 A）

`core:model` 只放纯 DTO/枚举与 Codec，不出现 `android.*` / `androidx.*`：

```kotlin
// core:model
enum class Tier { UNKNOWN, FAMILIAR, MASTERED }
data class KnowledgeState(/* 见 scheduler.md */)
data class SchedulerQuestion(/* 见 scheduler.md */)
data class RenderableQuestion(/* 见 pagination.md */)
data class KnowledgeRow(val knowledgePointId: String, val number: Int)
data class PlanPageRows(
    val rows: List<KnowledgeRow>,
    val triggeredKnowledgePointIds: List<String>
)
object KnowledgeRowsCodec {
    fun decode(json: String): List<PlanPageRows>
    fun encode(rows: List<PlanPageRows>): String
}
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
fun PlannedPage.toPlanPageRows(): PlanPageRows
```

- `KnowledgePoint.difficulty/halfLifeDays` 直接映射到 `KnowledgeState`。
- `Question` 实体没有 `linkedKnowledgeIds` 列；`data/room` 从 `QuestionKnowledgeCrossRef` 读取并按 `KnowledgePoint.number` 升序构造该列表。scheduler 不依赖输入列表顺序（C-049）。
- `data/room` 构造调度 DTO 时保证 `knowledgeStates` 与 `questions` 的 id 唯一、每条 `linkedKnowledgeIds` 内无重复，并在正常情况下保证每个关联 id 都能解析到知识点（C-056 方案 A）；备份导入/数据损坏导致的悬空关联由 scheduler 按 C-055 方案 C 跳过并留下 `UNRESOLVED_KNOWLEDGE` 记录。
- `PlannedQuestion` 没有 `questionNumber`；映射时由 `data/room` 从 `Question.questionNumber` 补齐。
- `Question.coolingPeriodOverrideDays` 已删除；调度只通过 `coolingOverrides` 接收一次性事件。
- `PlanPage.knowledgeRows` 的 JSON 编解码集中在 `core:model` 的 `KnowledgeRowsCodec`，Print/Scan 共用；`core:model` 不依赖 Room 或 Android。
- `Room Entity ↔ DTO` 的转换不改变字段语义；缺失/非法值按对应规格的默认值处理（如 `estimatedReviewMinutes=15`）。

## 归档/删除的原子操作

- `QuestionRepository.archive`：`archived=true`、`archivedAt=now`，并对每个关联知识点调用 `onQuestionArchived`；若计数为 0 则 `dormant=true`。
- `unarchive` 反向操作；`continuousScore` 不变。
- `KnowledgePointRepository.deleteWithTombstone`：先检查是否仍有未归档题目引用；有则拒绝；否则写墓碑并删除。
- 题目不提供硬删除；历史反馈永远保留。

## 验收

- Repository 单元测试覆盖：事务回滚、并发编号分配、归档计数、`savePlan` 部分失败不落库。
- `data/room` 映射测试覆盖：Entity ↔ Scheduler/Pagination DTO 字段一一对应，无丢失/重命名漂移。
- `core:model` 纯度测试：`src/main` 与 `src/test` 均不得出现 `android.*` / `androidx.*` import；`KnowledgeRowsCodec` 可独立测试。
