# 数据模型规格（data/room）

关联决策：D-01、D-03、D-06、D-11、D-12、D-13
关联裁决：C-006、C-009、C-010、C-013、C-015、C-017、C-019、C-021、C-022、C-023、C-026、C-028、C-030、C-031、C-046、C-050、C-070、C-071、C-072、C-077、C-078、C-080

## 冻结决策（D-06、D-11、D-12、D-13）

- **D-06**：手写笔记 OCR 只存储和展示，不参与调度；原始 OCR 只读、修正文本可编辑；导出两者都含。
- **D-11**：归档条目退出调度，历史反馈保留为知识点背景数据；活跃题目计数为 0 时知识点休眠；取消归档重新激活，`continuousScore` 保留。
- **D-12**：单题关联知识点 ≤ 4；知识点编号全局唯一不复用；`number` 唯一索引、墓碑实体、独立编号序列；录入层与 Repository 双重校验。
- **D-13**：确认页逐项标记，不得一键全部接受；`NOT_REVIEWED` 不写 `ReviewFeedback`，只写 `ConfirmationDecisionLog`；`ConfirmationDecisionLog` 纳入同版本备份。

## 实体

### KnowledgePoint

```kotlin
@Entity(
    tableName = "knowledge_point",
    indices = [Index(value = ["number"], unique = true)]
)
data class KnowledgePoint(
    @PrimaryKey val id: String,
    val number: Int,                 // 全局唯一、创建后不可变、不复用（D-12 / C-028）
    val name: String,                // 全称；左侧 8–10 字截断只是渲染规则，不改数据（C-013）
    val currentTier: Tier,           // UNKNOWN / FAMILIAR / MASTERED
    val continuousScore: Float,      // [0,1]；持久化 Float，算术在 UseCase 内部用 Double（C-050）
    val effectiveFeedbackCount: Float,  // 0.5 的倍数；C-006
    val nextDueDate: LocalDate,
    val activeQuestionCount: Int,
    val dormant: Boolean,
    val difficulty: Int?,               // 墨墨 DHP 难度；冷启动阶段 null（C-018 已确认）
    val halfLifeDays: Float?,           // 墨墨 DHP 半衰期；冷启动阶段 null（C-018 已确认）
    val createdAt: Instant              // 创建时间；DHP 第一段间隔的起点（C-037）
)
```

- `number` 由 `KnowledgeNumberSequence` 在事务内分配；删除知识点时写 `KnowledgePointTombstone`，编号不回收。
- `effectiveFeedbackCount` 为 `Float`，取值始终是 0.5 的倍数；切换阈值为 `>= 3.0f`，不做 epsilon 容差（C-006/C-050）。
- `continuousScore` 落库为 `Float`；`ApplyFeedbackUseCase` 的加减与夹取在 `Double` 中完成，写入 Room 或构造 `KnowledgeState` 时转 `Float`（C-050）。
- 新建知识点的初始值：`currentTier = UNKNOWN`、`continuousScore = 0.5`、`effectiveFeedbackCount = 0.0`、`nextDueDate = 创建日 + 1 天`、`activeQuestionCount = 0`、`dormant = false`、`difficulty = null`、`halfLifeDays = null`、`createdAt = now`；首次关联题目后由 Repository 重算 `activeQuestionCount`。
- 新建题目的 `lastScheduledDate = null`、`lastReviewedDate = null`；首次打印/确认后再写。
- `difficulty`/`halfLifeDays` 在第 3 条 triggered 反馈后由 `ApplyFeedbackUseCase` 初始化，之后每次 triggered 反馈更新；有效反馈未达 3.0 时 `nextDueDate` 仍用冷启动 1/3/7，达到后从下一次反馈起用 DHP（C-018）。
- **可打印知识点（printable）**：`!dormant && activeQuestionCount > 0`。左侧行、右上全称、OMR 行组、调度候选统一使用该谓词；`dormant=false` 但 `activeQuestionCount=0` 的初始态不打印。

### Question

```kotlin
@Entity(tableName = "question")
data class Question(
    @PrimaryKey val id: String,
    val pageId: String,                        // 最近一次打印所在页面的 pageId；由 feature/print 打印成功后写（C-033 方案一）
    val questionNumber: String,
    val qrPayload: String,                     // 最近一次 QR；由 feature/print 打印成功后写（C-033 方案一）
    val archived: Boolean,
    val lastScheduledDate: LocalDate?,         // 计划日；由 feature/planning 写（C-033）
    val lastReviewedDate: LocalDate?,           // 实际复习日；由 ApplyFeedbackUseCase 写（C-039）
    val estimatedReviewMinutes: Int = 15,      // 单位：分钟；缺失按 15（C-017）
    val createdAt: Instant,
    val archivedAt: Instant?
)
```

- `Question` 实体没有 `linkedKnowledgeIds` 列；调度 DTO `SchedulerQuestion.linkedKnowledgeIds` 由 `data/room` 从 `QuestionKnowledgeCrossRef` 按 `KnowledgePoint.number` 升序构造（C-046/C-049）。

### CoolingOverrideEvent（C-008 方案二）

```kotlin
@Entity(tableName = "cooling_override_event")
data class CoolingOverrideEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,                  // 由确认页创建；记录来源会话
    val questionId: String,
    val createdAt: Instant,
    val consumedAt: Instant?                // 计划实际排入该题后置为消费时间；未消费可继续用于下一份计划
)
```

- 一次性事件：确认页的「本次提前复习」创建；`plan()` 通过 `coolingOverrides: Set<String>` 接收未消费事件的 questionId。
- 同一题至多保留一个未消费事件；重复创建时复用或去重。
- 事件是临时操作态，不参与调度状态；不导出到备份（与 `PendingFeedback` 同策略，C-031）。

### QuestionKnowledgeCrossRef

```kotlin
@Entity(
    tableName = "question_knowledge_cross_ref",
    primaryKeys = ["questionId", "knowledgePointId"]
)
data class QuestionKnowledgeCrossRef(
    val questionId: String,
    val knowledgePointId: String,
    val weight: Float = 1.0f
)
```

- 行序不存于此表。打印时按 `linkedKnowledgeIds` 中未休眠、`activeQuestionCount > 0` 的知识点 `sortedBy { KnowledgePoint.number }` 生成，并冻结到 `PlanPage.knowledgeRows`；回读按快照重建（C-015）。
- `weight` 目前恒为 `1.0`，不参与 D-01 的 0.3 传播；0.3 由 `ApplyFeedbackUseCase` 按非触发规则施加，避免两个权重来源混淆。

### ReviewFeedback

```kotlin
@Entity(tableName = "review_feedback")
data class ReviewFeedback(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val knowledgePointId: String,
    val questionId: String,
    val planId: String,                // 来源计划（C-038/C-040）
    val pageId: String,                // 来源页（C-038/C-040）
    val plannedDate: LocalDate,        // 计划日（C-038）
    val reviewedOn: LocalDate,         // 实际复习日（C-038）
    val tier: Tier,                    // 本次填涂/确认档位
    val triggered: Boolean,            // 本行是否触发该知识点（C-009）；冷启动事件计数只数 true
    val rawOcrText: String?,           // 只读，D-06
    val correctedText: String?,        // 可编辑，D-06
    val isSupplemental: Boolean,       // "补填"标记，D-13
    val effectiveWeight: Float,        // 1.0，或 FAMILIAR 的 0.5；NOT_REVIEWED 不写行（C-006/C-010）
    val weightApplied: Float,          // 审计字段：触发=1.0，非触发=0.3；不反向驱动其它字段
    val timestamp: Instant
)
```

- `effectiveWeight` 替代原 `countedAsEffective: Boolean`；值域 `{0.5, 1.0}`。它记录该行反馈自身的权重；只有 `triggered=true` 的行才把它累加进 `effectiveFeedbackCount`，非触发行不累加（C-009）。
- `NOT_REVIEWED` 不写本表；其审计痕迹在 `ConfirmationDecisionLog`（C-010）。

### PendingFeedback（临时状态，确认前）

```kotlin
@Entity(
    tableName = "pending_feedback",
    primaryKeys = ["sessionId", "questionId", "knowledgePointId"]
)
data class PendingFeedback(
    val sessionId: String,
    val planId: String,                    // 所属计划（C-040）
    val pageId: String,                    // 所属页（C-040）
    val plannedDate: LocalDate,             // 该页的计划日（C-038）
    val actualReviewDate: LocalDate? = plannedDate, // 确认页可覆盖；NOT_REVIEWED 可为 null（C-038）
    val questionId: String,
    val knowledgePointId: String,
    val triggered: Boolean,                 // 触发行/非触发行（C-009）
    val detectedTier: Tier?,                // OK 时非空
    val detectionStatus: DetectionStatus,   // OK / NO_MARK / CONFLICT / UNREADABLE
    val conflict: Boolean = false,          // 多组填涂冲突标记
    val detectedRawOcr: String?,            // 只读
    val userDecision: UserDecision = UserDecision.UNMARKED,
    val selectedTier: Tier?,                // CORRECTED/SUPPLEMENTED 必填；ACCEPT 在 detectedTier 为空时必填
    val correctedText: String?,             // 可编辑修正文本
    val isSupplemental: Boolean = false
)

enum class UserDecision { UNMARKED, ACCEPT, CORRECTED, NOT_REVIEWED, SUPPLEMENTED }
enum class DetectionStatus { OK, NO_MARK, CONFLICT, UNREADABLE }
```

- 复合主键承载「一次回读一页多题、每题最多 4 个知识点」的逐行决策（C-022）。
- `UNMARKED` 是初始态；提交前每行必须离开 `UNMARKED`（C-010）。
- **生命周期**：scan 完成归属校验后创建 → confirm 逐行决策 → `ApplyFeedbackUseCase` 成功后按 `sessionId` 删除。它是临时状态，不是历史数据；备份不导出（C-031）。

### ScanSession（计划级扫描会话，C-040）

```kotlin
@Entity(tableName = "scan_session")
data class ScanSession(
    @PrimaryKey val sessionId: String,
    val planId: String,
    val startedAt: Instant,
    val status: ScanSessionStatus,      // SCANNING / READY / SUBMITTED / ABANDONED
    val submittedAt: Instant?
)

enum class ScanSessionStatus { SCANNING, READY, SUBMITTED, ABANDONED }
```

- 一次 plan 扫描共用一个 `sessionId`；多页扫描向同一 session 追加 `PendingFeedback` 行。
- 状态流转：`startSession → SCANNING`；打开确认页或追加页 → `READY`；提交成功 → `SUBMITTED`；用户取消/超过 24h → `ABANDONED`。
- App 启动时把 `startedAt < now-24h` 且非 `SUBMITTED` 的 session 标为 `ABANDONED`；不引入 WorkManager。
- 整批无跳过时提交成功：`status=SUBMITTED` 并删除该 session 的 `PendingFeedback`；失败保持 `READY`。
- 存在整题跳过时（C-078/C-080 方案 A）：已写入题目的待确认行删除，跳过行保留，会话保持 `READY` 供数据修复后重试；只有全部题目成功才置 `SUBMITTED`。`SUBMITTED` 会话的重复提交在 UseCase 前置校验被拒绝。
- 临时操作态，不导出备份；导入时清空。

### ConfirmationDecisionLog（确认决定日志）

```kotlin
@Entity(tableName = "confirmation_decision_log")
data class ConfirmationDecisionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val planId: String,
    val pageId: String,
    val questionId: String,
    val knowledgePointId: String,
    val plannedDate: LocalDate,        // 计划日（C-038）
    val reviewedOn: LocalDate?,        // 实际复习日；NOT_REVIEWED 为 null（C-038/C-039）
    val triggered: Boolean,
    val decision: UserDecision,        // 含 NOT_REVIEWED
    val selectedTier: Tier?,
    val correctedText: String?,
    val isSupplemental: Boolean,
    val decidedAt: Instant
)
```

- 每次提交对每一行写一条，包括 `NOT_REVIEWED` 与整题被跳过的行（C-078 方案 A）；这是 R-04 的审计落点，不等于写 `ReviewFeedback`。
- 整题跳过时该题所有行仍写本表，但不写 `ReviewFeedback`、不写知识点状态与题目复习日（C-077/C-078 方案 A）。
- `MISSED` 不落库：按 `(planId,pageId,questionId)` 在该表无记录，或所有决策均为 `NOT_REVIEWED`，且 `plannedDate <= 今天` 派生。
- 纳入备份导出（历史记录，不是未确认状态）。

### PlanPage

```kotlin
@Entity(
    tableName = "plan_page",
    primaryKeys = ["planId", "pageId"],
    indices = [Index(value = ["pageId"], unique = true)]
)
data class PlanPage(
    val planId: String,
    val pageId: String,                 // = "{planId}-{pageNumber:02d}"，全局唯一（C-014）
    val pageNumber: Int,                // 计划内页码，01..99
    val date: LocalDate,
    val itemCount: Int,
    val questionIds: String,            // 序列化 JSON 数组，本页题目的打印顺序
    val knowledgeRows: String,          // 序列化 JSON 数组：每题一条行快照与 trigger 标记（C-009/C-015/C-070）；映射时只解码，不重排、不重算（C-071 方案 A）
    val continuationMarkers: String     // 序列化 JSON，按题目顺序排列
)
```

- `pageId` 全局唯一索引使手动输入「页面 ID + 题号」可以只凭 pageId 解析出 planId（C-011/C-014）。
- `questionIds` 用于手动回退与归属校验；不依赖 `Question.pageId` 的「最近一次打印」值。
- `knowledgeRows` 的 JSON 编解码由 `core:model` 的 `KnowledgeRowsCodec` 负责（纯 Kotlin，不依赖 Room/Android）；`data/room` 只负责把 `PlanPage.knowledgeRows` 这段冻结快照解码为 `List<PlanPageRows>`（每题一条，不按 `number` 重排、不从当前关联重算、不合并或丢弃记录），不自行拼 JSON（C-046、C-071 方案 A）。
- `knowledgeRows` 的结构：
  ```json
  [{"questionId":"q1","rows":[{"knowledgePointId":"k1","number":3},{"knowledgePointId":"k2","number":5}],"triggeredKnowledgePointIds":["k1"]}]
  ```
  它冻结打印时的行集合、行序（生成时由 `number` 升序产生，回读不得重排）与 trigger 标记，并按 `questionId` 每题一条保存，一页可有多条（C-070/C-071 方案 A）；scan 按它重建行组，确认页按它校验归属。打印后修改题目关联知识点不会改变旧稿的行映射；若用户要求新关联生效，必须重新生成计划并打印（C-015）。
- `continuationMarkers` 的 JSON 结构见 `pagination.md`；数组顺序即绘制顺序（C-016）。

### NoteAttachment

```kotlin
@Entity(tableName = "note_attachment")
data class NoteAttachment(
    @PrimaryKey val questionId: String,
    val attachToPrint: Boolean = false,
    val attachmentCount: Int = 0        // 0..3；为选择打印的条数 = min(3, 实际条数)
)
```

### NoteAttachmentItem（C-026 方案二）

```kotlin
@Entity(tableName = "note_attachment_item")
data class NoteAttachmentItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: String,
    val sourceFeedbackId: Long?,        // 来源 ReviewFeedback；可为空
    val text: String,                   // 可编辑修正文本 correctedText 的快照
    val imageUri: String?,              // 原始笔记图片/文件 URI；可为空
    val order: Int,                     // 同一题内的展示顺序
    val createdAt: Instant
)
```

- 打印时按 `questionId` 取最近 3 条（`createdAt DESC`，同时间按 `order`），每条摘要最多 2 行。
- 字段映射使用 `ReviewFeedback.correctedText`，不用 `rawOcrText`（D-06）。
- 备份导出/恢复本实体；`NoteAttachment.attachmentCount` 按恢复后的实际条数校正。

### KnowledgePointTombstone（C-028）

```kotlin
@Entity(tableName = "knowledge_point_tombstone")
data class KnowledgePointTombstone(
    @PrimaryKey val number: Int,
    val knowledgePointId: String,
    val voidedAt: Instant,
    val reason: String?                 // 例如 "deleted" / "import-conflict"
)
```

- 墓碑记录「该编号曾经存在且已作废」，不是物理删除编号；用途是防止编号被再次分配，并保留审计。

### KnowledgeNumberSequence（C-028）

```kotlin
@Entity(tableName = "knowledge_number_sequence")
data class KnowledgeNumberSequence(
    @PrimaryKey val id: Int = 1,        // 单行表
    val lastIssued: Int
)
```

- 分配编号与插入 `KnowledgePoint` 必须在同一事务内：读取 `lastIssued` → +1 → 检查未命中墓碑 → 写回并插入。
- 建库/迁移时必须种入单行 `KnowledgeNumberSequence(id = 1, lastIssued = 0)`；若表为空（旧库/异常导入），Repository 用 `getOrCreate` 初始化为 `lastIssued = max(KnowledgePoint.number, KnowledgePointTombstone.number, 0)`。首个知识点不允许因空表而无法编号。

## 迁移与归档

- Room schema 版本递增必须提供 `Migration`，不允许 destructive migration。
- 归档不改表结构，只置 `archived=true`；查询时 `where archived=false`。题目只归档、不硬删除；归档/取消归档时同步维护关联知识点的 `activeQuestionCount` 与 `dormant`。
- 知识点删除前必须无未归档题目引用；否则拒绝。删除时先写 `KnowledgePointTombstone`，编号不回收。
- `effectiveFeedbackCount` 由 `Int` 改 `Float`：迁移时逐行转 `toFloat()`。
- `countedAsEffective` 删除、`effectiveWeight` 新增：旧行 `countedAsEffective=true` 回填 `1.0f`；`false` 行先写入 `ConfirmationDecisionLog`（若无法还原 session/plan 信息则记录迁移警告），再从 `ReviewFeedback` 删除，避免 `0.0` 落在新值域 `{0.5,1.0}` 之外。
- `review_feedback` 新增 `triggered` 列：迁移时旧行默认 `true`（历史实现只记录触发行）；若旧行实际来自非触发更新则无法区分，迁移报告需提示。新写入按实际触发行/非触发行赋值。
- `PendingFeedback` 主键由单列改复合：建新表 → 迁移 → 删旧表；新增 `planId/pageId/plannedDate/actualReviewDate` 列，旧临时数据可直接清空。
- `question` 删除 `coolingPeriodOverrideDays` 列（C-008 方案二）；新增 `lastReviewedDate: LocalDate?`（C-039）与 `cooling_override_event` 表。
- `review_feedback` 新增 `planId`/`pageId`/`plannedDate`/`reviewedOn`；旧行 `planId/pageId` 回填空字符串，`plannedDate/reviewedOn` 用 `timestamp` 的本地日期回填，并记录迁移警告。
- `confirmation_decision_log` 新增 `plannedDate`/`reviewedOn`；旧行用 `decidedAt` 的本地日期回填两者。
- `knowledge_point` 新增可空列 `difficulty: Int?`、`halfLifeDays: Float?`、`createdAt: Instant`（C-018/C-037）；旧数据 `difficulty/halfLifeDays` 为 `null`，`createdAt` 用迁移时间回填。
- 新增表：`confirmation_decision_log`、`note_attachment_item`、`knowledge_point_tombstone`、`knowledge_number_sequence`、`cooling_override_event`、`scan_session`；`knowledge_number_sequence` 迁移时种入 `(1, max(number, 0))`。
- 新增唯一索引 `knowledge_point.number` 与 `plan_page.pageId` 前必须先清理重复值；迁移脚本遇到重复应失败并提示，不得静默重编号。
- `plan_page` 增加 `knowledgeRows` 列：迁移时默认空数组 `[]`；旧稿没有快照时不得按当前关联 + `number` 升序重算，应视为「无快照」并在回读/确认时阻断，提示重新生成计划并按新稿打印，同时记录警告（C-072 方案 A）。
- 导出 JSON 的范围以 `backup.md` 的白名单为准，不再表述为「全部实体」；`PendingFeedback`、`ScanSession` 与 `CoolingOverrideEvent` 不导出（C-031/C-040/C-008）。

## 禁止

- 不得为方便而合并 `Question` 和 `KnowledgePoint`。
- 不得在 `ReviewFeedback` 中新增未经规格确认的字段。
- 不得使用 `rawOcrText` 参与任何调度计算（D-06）。
- 不得让 `weightApplied` 反向驱动 `continuousScore`、`tier` 或 `nextDueDate`。
- 不得复用 `KnowledgePoint.number`，也不得在打印后修改它（C-015/C-028）。
