# 计划编排规格（feature/planning）

关联决策：D-01、D-02、D-04、D-08、D-11、D-12
关联裁决：C-047
关联规格：`scheduler.md`、`pagination.md`、`print-template.md`、`feedback-usecase.md`、`repository.md`

## 职责

`feature/planning` 负责把「知识状态 + 题目 + 日期范围 + 设置」变成一份可打印计划，并把计划快照、`Question.lastScheduledDate`、冷却覆盖事件消费、冷却顺延落库。它不实现调度/分页算法，只做编排与事务。

## GeneratePlanUseCase

```kotlin
class GeneratePlanUseCase(
    private val knowledgeRepository: KnowledgePointRepository,
    private val questionRepository: QuestionRepository,
    private val planRepository: PlanRepository,
    private val noteAttachmentRepository: NoteAttachmentRepository,
    private val scheduler: Scheduler,
    private val pagePlanner: PagePlanner,
    private val textMeasurer: KnowledgeTextMeasurer,
    private val planIdGenerator: PlanIdGenerator,
    private val clock: Clock
) {
    suspend fun execute(settings: PlanningSettings): PrintJob
}

data class PlanningSettings(
    val planStartDate: LocalDate,          // 默认：生成日所在周末后的下一个周一
    val planEndDate: LocalDate,            // 默认：planStartDate + 6
    val dailyAvailableMinutes: Map<LocalDate, Int>, // 缺失默认 60
    val coolingPeriodDays: Int = 7,      // >= 0；0 = 关闭冷却（内部/测试），负数非法（C-057 方案 D）
    val template: TemplateGeometry = TemplateGeometry()
)

// feature/planning 从 AppSettings 映射；today 由 Clock 提供
fun planningSettingsFrom(settings: AppSettings, today: LocalDate): PlanningSettings

data class PlanDraft(
    val planId: String,
    val pages: List<PlannedPage>,
    val scheduledDates: Map<String, LocalDate>, // questionId -> targetDate
    val coolingEventIdsToConsume: List<Long>,
    val deferred: List<DeferredItem>
)

data class PrintJob(
    val planId: String,
    val pages: List<PlannedPage>,
    val questions: Map<String, RenderableQuestion>,
    val notes: Map<String, List<NoteAttachmentItem>>,
    val template: TemplateGeometry
)

class PlanGenerationException(
    val code: PaginationFailureCode,
    message: String
) : Exception(message)

interface KnowledgeTextMeasurer {   // 定义在 core/common；feature/print 实现
    fun lineCount(knowledgeNames: List<String>, widthMm: Double, fontMm: Double): Int
}
```

### 固定流程

1. 读取 `PlanningSettings`；`planStartDate` 默认为“本周末之后的下一个周一”，`planEndDate = planStartDate + 6`；`dailyAvailableMinutes` 缺失按 60 分钟/天。
2. 读取 `KnowledgePointRepository.states()`、`QuestionRepository.schedulerQuestions()`、`PlanRepository.pendingCoolingOverrides()`。
3. 调用 `Scheduler.plan(..., dateRange = planStartDate..planEndDate, coolingOverrides = 未消费事件 questionId 集合)`。
4. 对每个 `PlannedQuestion`：
   - 按 `KnowledgePoint.number` 升序生成 `knowledgeRows` 行快照（含 `triggeredKnowledgePointIds`）；
   - 用 `KnowledgeTextMeasurer`（`feature/print` 实现，Paint 测量）计算 `knowledgeLineCount`，构建 `RenderableQuestion`；`knowledgeNames` 必须与左侧同一 printable 集合、按 `number` 升序，且数量等于 `knowledgeCount`（C-067，分页器不校验、不补齐）；
   - 调用 `PagePlanner.plan(planId, dateGroups, template)` 得到 `PaginationResult`：
     - `Success`：取 `pages`、`overflowItems`、`warnings` 继续；计划层把文本测量产生的 `KNOWLEDGE_TEXT_OVERFLOW` 警告合并进该 `warnings`（C-064 方案 A）：按 `questionId + code` 去重（同一题同一 code 至多一条；不同 code 可并存，例如整题过高与文字溢出），按 `questionId` 字典序、同 `questionId` 按 `code` 枚举序排序；分页器不产出该警告，也不猜文本换行；
     - `Failure(code, message)`：抛 `PlanGenerationException(code, message)`，**不进入事务**，由 App 映射为可重试 UI 错误；
   - 生成 `PlanPage` 行（`pageId = "{planId}-{pageNumber:02d}"`、`questionIds`、`knowledgeRows`、`continuationMarkers`）；`continuationMarkers` 仅作内部移页元数据，不在纸上绘制“续下页”（C-063）。
5. 在**一个 Room 事务**内调用 `PlanRepository.savePlan(draft)`：
   - 写全部 `PlanPage`；
   - 对每个已排入题目写 `Question.lastScheduledDate = targetDate`；
   - 对本次命中的 `CoolingOverrideEvent` 写 `consumedAt = now`；
   - 应用 `DeferredItem(COOLING)` 的 `targetNextDueDates`。
6. 事务成功后组装 `PrintJob`；失败则整体回滚，不返回 `PrintJob`。

> 打印不重新分页：`PrintDocumentAdapter` 只消费 `PrintJob.pages`。`PlanPage` 在规划阶段已持久化。

## ID、时区与默认设置

- `planId`：6 位 Crockford Base32（去掉 I/L/O/U），由 `PlanIdGenerator` 生成；与 `PlanPage.planId` 冲突时重试 3 次，仍冲突则失败。该格式与全局唯一性是 `PagePlanner` 的前置条件，分页器只拼接、不校验（C-069 方案 A）。
- `pageId`：`"{planId}-{pageNumber:02d}"`，`pageNumber` 由 `PagePlanner` 从 1 连续分配；超过 99 时 `PagePlanner` 返回 `PaginationResult.Failure(PaginationFailureCode.PLAN_TOO_LONG, ...)`，`GeneratePlanUseCase` 转 `PlanGenerationException`，不写库。
- 业务日期用 `LocalDate`（设备本地时区）；审计/事件时间用 `Instant`（UTC）。生成计划时记录当时的本地日期；跨时区不自动改写已生成计划。
- 默认每日可用 60 分钟；用户可按星期调整。`planStartDate` 默认下一个周一，`planEndDate = planStartDate + 6`（周一到周日），允许用户改选任意连续 1～7 天。
- 扫描窗口：`planEndDate + 1` 到 `planEndDate + 7`；窗口内为正常批量；超过 `planEndDate + 7` 标 `LATE_BATCH`。扫描早于 `planEndDate` 时，未来日期行只能标 `NOT_REVIEWED`（C-041）。
- `Question.lastReviewedDate` 由 `ApplyFeedbackUseCase` 在确认后写，规划层不写；规划层只写 `lastScheduledDate`（计划日）。

## 归档、取消归档、删除、计数

- 题目只提供归档/取消归档，不提供硬删除；`ReviewFeedback` 永远保留。
- 归档事务：`archived=true`、`archivedAt=now`；对每个关联知识点 `activeQuestionCount--`；若为 0 则 `dormant=true`。
- 取消归档：反向操作；`dormant=false`，`continuousScore` 保留。
- 知识点删除：仍被任何未归档题目引用时拒绝；否则写 `KnowledgePointTombstone` 后删除实体。编号不回收。
- 题目修改关联知识点：只允许在未打印/未排入当前计划时改；修改后重算受影响知识点的 `activeQuestionCount`。

## 笔记附件生命周期

- 确认页修正文本可「保存为笔记附件」：`NoteAttachmentItem(text=correctedText, imageUri=null, order=当前最大+1)`。
- 拍照/选图：文件复制到 app 私有目录，`imageUri` 存相对路径；备份时随 JSON 打包或置空并警告。
- `NoteAttachment.attachToPrint` 控制是否上纸；`attachmentCount = min(3, 实际条数)`。
- 打印取最近 3 条（`createdAt DESC`，同时间按 `order`），每条摘要最多 2 行。

## 验收

- 计划生成后：`PlanPage`、`Question.lastScheduledDate`、冷却事件消费、顺延在同一事务内；任一步失败全部回滚。
- `PagePlanner` 返回 `Failure` 时抛 `PlanGenerationException`，不进入事务、不写任何 `PlanPage`。
- 同一 `planId` 重试生成不会产生重复 `PlanPage`；`planId` 冲突按规则重试/失败。
- 打印层只消费 `PrintJob`，不在 `onLayout` 查询数据库或重新分页。
