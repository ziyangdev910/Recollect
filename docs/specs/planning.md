# 计划编排规格（feature/planning）

关联决策：D-01、D-02、D-04、D-08、D-11、D-12
关联裁决：C-047、C-074、C-076、C-090、C-091
关联规格：`scheduler.md`、`pagination.md`、`print-template.md`、`feedback-usecase.md`、`repository.md`

## 职责

`feature/planning` 负责把「知识状态 + 题目 + 日期范围 + 设置」变成一份可打印计划，并把计划快照、`Question.lastScheduledDate`、冷却覆盖事件消费、冷却顺延放进 `PlanDraft` 交给 `PlanRepository.savePlan` 的单事务落库（C-076 方案 A）。它不实现调度/分页算法，只做编排与事务。

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
    // C-090 方案 A：返回结果包装，成功含打印输入，空计划含空态与提示。
    suspend fun execute(settings: PlanningSettings): PlanGenerationOutcome
}

// C-090 方案 A：唯一的结果出口；PaginationResult.Failure 不在此出现，按 C-047 抛 PlanGenerationException。
sealed interface PlanGenerationOutcome {
    data class Planned(
        val printJob: PrintJob,          // 打印层唯一输入；字段不含任何界面状态
        val preview: PlanPreviewContent  // 预览页唯一输入
    ) : PlanGenerationOutcome

    // C-060：整份计划没有可打印题目；仍需把 overflow 原因、建议与被推迟题目交给预览页。
    data class Empty(
        val message: String,
        val warnings: List<PlanPreviewWarning>,
        val deferred: List<PlanPreviewDeferred> = emptyList()
    ) : PlanGenerationOutcome
}

// 预览投影：只给 PlanPreview 使用；打印层不读取。
data class PlanPreviewContent(
    val planId: String,
    val pages: List<PlanPreviewPage>,
    val warnings: List<PlanPreviewWarning>,
    val deferred: List<PlanPreviewDeferred> = emptyList()
)

// 计划预览的题目标识：`页码-第X题`（X 为题号）；没有来源页码时只显示 `第X题`。
// 同一题号可能出现在不同来源页上，单看题号无法区分。
data class PlanPreviewPage(
    val pageNumber: Int,
    val date: LocalDate,
    val questionLabels: List<String>
)

// C-065 方案 B：界面只展示中文 label 与 message，不展示分页内部代码。
data class PlanPreviewWarning(
    val questionId: String,      // 仅供定位/日志，不直接展示
    val questionNumber: String,  // 界面展示的题号
    val label: String,
    val message: String
)

// 被推迟题目的预览投影（冷却期 / 当天时间不足 / 关联知识点失效）；不会出现在打印稿里。
data class PlanPreviewDeferred(
    val questionId: String,      // 仅供定位/日志，不直接展示
    val questionLabel: String,   // 与 PlanPreviewPage 相同：`页码-第X题`（无来源时 `第X题`）
    val label: String,           // 中文原因标签，不展示调度器枚举名
    val message: String
)

data class PlanningSettings(
    val planStartDate: LocalDate,          // 默认：今天所在自然周的周一（currentWeekSettings）
    val planEndDate: LocalDate,            // 默认：planStartDate + 6
    val dailyAvailableMinutes: Map<LocalDate, Int>, // 缺失默认 60
    val coolingPeriodDays: Int = 7,      // >= 0；0 = 关闭冷却（内部/测试），负数非法（C-057 方案 D）
    // C-091 方案 A：生产模板为四角标记预留 14mm 角区，内容区从 26mm 开始。
    val template: TemplateGeometry = TemplateGeometry.a4Print()
)

// feature/planning 从 AppSettings 映射；today 由 Clock 提供
fun planningSettingsFrom(settings: AppSettings, today: LocalDate): PlanningSettings

// C-074 方案 A：PlanDraft 定义在 data:room（见 repository.md），编排层只构造入参。
data class PlanDraft(
    val planId: String,
    val pages: List<PlannedPage>,
    val knowledgeRowsByQuestionId: Map<String, PlanPageRows>, // 打印时冻结的每题行快照（C-071/C-072）
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

// C-074 方案 A：定义在 data:room，由数据层在进入事务前抛出；编排层与 App 只捕获/映射。
class PlanGenerationException(
    val code: PaginationFailureCode,
    message: String
) : Exception(message)

interface KnowledgeTextMeasurer {   // 定义在 core/common；feature/print 实现
    fun lineCount(knowledgeNames: List<String>, widthMm: Double, fontMm: Double): Int
}
```

### 固定流程

1. 读取 `PlanningSettings`；`planStartDate` 默认为今天所在自然周的周一（`today.with(previousOrSame(MONDAY))`，C-098），`planEndDate = planStartDate + 6`；`dailyAvailableMinutes` 缺失按 60 分钟/天。首页另有紧随其后的下一自然周窗口（`nextWeekSettings`），只提供本周与下周两个入口，不允许生成更远的周。
2. 读取 `KnowledgePointRepository.states()`、`QuestionRepository.schedulerQuestions()/questionNumbers()/sourcePages()`、`PlanRepository.pendingCoolingOverrides()`（题号与来源页码用于预览标识与打印元数据）。
3. 调用 `Scheduler.plan(..., dateRange = planStartDate..planEndDate, coolingOverrides = 未消费事件 questionId 集合)`。
4. 对每个 `PlannedQuestion`：
   - 按 `KnowledgePoint.number` 升序生成 `knowledgeRows` 行快照（含 `triggeredKnowledgePointIds`）；
   - 用 `KnowledgeTextMeasurer`（`feature/print` 实现，Paint 测量）计算 `knowledgeLineCount`，构建 `RenderableQuestion`；`knowledgeNames` 必须与左侧同一 printable 集合、按 `number` 升序，且数量等于 `knowledgeCount`（C-067，分页器不校验、不补齐）；
   - 调用 `PagePlanner.plan(planId, dateGroups, template)` 得到 `PaginationResult`：
     - `Success`：取 `pages`、`overflowItems`、`warnings` 继续；计划层把文本测量产生的 `KNOWLEDGE_TEXT_OVERFLOW` 警告合并进该 `warnings`（C-064 方案 A）：按 `questionId + code` 去重（同一题同一 code 至多一条；不同 code 可并存，例如整题过高与文字溢出），按 `questionId` 字典序、同 `questionId` 按 `code` 枚举序排序；分页器不产出该警告，也不猜文本换行；
     - `Failure(code, message)`：抛 `PlanGenerationException(code, message)`，**不进入事务**，由 App 映射为可重试 UI 错误；
     - `Success` 且 `pages` 为空：返回 `PlanGenerationOutcome.Empty(message, warnings 的预览投影)`，**不建页、不进入事务**（C-060）；`message` 必须区分“没有到期题目”与“有题目但全部无法打印”，且 overflow 的原因与题目不得丢弃（C-061/C-062）。
   - 为每道已排入题目生成行快照 `PlanPageRows`，与页一起放入 `PlanDraft.knowledgeRowsByQuestionId`；行组按打印时的 `KnowledgePoint.number` 升序生成后冻结，后续不接受重算（C-071/C-072）。`PlanPage` 的 `pageId = "{planId}-{pageNumber:02d}"`、`questionIds`、`knowledgeRows`、`continuationMarkers` 由 `PlanRepository.savePlan` 按草稿落库；`continuationMarkers` 仅作内部移页元数据，不在纸上绘制“续下页”（C-063）。
   - 有可打印页时组装预览投影：`PlanPreviewContent(planId, pages, warnings, deferred)`；警告已按 C-064 合并去重排序，并按 C-065 方案 B 映射为中文标签与 `message`，不把分页内部代码交给界面。
   - 预览页的页面卡片与推迟卡片统一用题目标识 `页码-第X题`（来源页码取 `QuestionRepository.sourcePages()`，无来源时回落 `第X题`）；`deferred` 取 `PlanResult.deferred`，按中文原因标签与说明投影，不展示调度器枚举名。
5. 仅当存在可打印页时，在**一个 Room 事务**内调用 `PlanRepository.savePlan(draft)`（`PlanDraft` 含上述行快照与 `deferredRecords`，C-074 方案 A）：
   - 写 `plan` 头；`deferred` 保存 `questionId/题号/来源页码/原因` 的冻结快照，重开计划时不再依赖当前题库（旧快照缺来源页码时，仅展示层可按当前题库兜底补齐）；
   - 写全部 `PlanPage`；
   - 对每个已排入题目写 `Question.lastScheduledDate = targetDate`；
   - 对本次命中的 `CoolingOverrideEvent` 写 `consumedAt = now`；
   - 应用 `DeferredItem(COOLING)` 的 `targetNextDueDates`（同一事务，C-076 方案 A）。
6. 事务成功后返回 `PlanGenerationOutcome.Planned(printJob, preview)`；`PrintJob` 仍是打印层唯一输入，`preview` 仍是预览页唯一输入。事务失败则整体回滚，不返回 `Planned`。空计划返回 `PlanGenerationOutcome.Empty`，不写库。

> 打印不重新分页：`PrintDocumentAdapter` 只消费 `PrintJob.pages`。`PlanPage` 在规划阶段已持久化。

## PlanSession 与本周计划复用（C-097）

- `PlanSession.open(settings)`：先查 `PlanRepository.activePlan(planStartDate, planEndDate)`；存在 GENERATED/PRINTED/COMPLETED 计划时用 `LoadPlanUseCase` 重建 `PrintJob` 与预览，**不生成新计划号**；不存在时才调用 `GeneratePlanUseCase.execute`。计划窗口由首页入口（本周/下周）决定，预览页不自行顺延到更远的周（C-098）。
- `PlanSession.regenerate(settings)`：仅由用户在预览页显式确认后调用；把当前计划号传给 `PlanGenerator.regenerate(settings, previousPlanId)`，新计划保存事务内把旧计划标记为 SUPERSEDED。
- `LoadPlanUseCase` 只读取 `plan` 头、`plan_page` 与 `plan.renderMeta`/题块几何重建 `PlannedPage`、`RenderableQuestion` 与附件文本；不重新调度、不重新分页。缺元数据/几何时直接失败，不按当前题库猜测。预览的题目标识从 `renderMeta.sourcePage + questionNumber` 重建；推迟卡片从 `plan.deferred` 重建，旧快照缺 `sourcePage` 时仅用 `QuestionRepository.sourcePages()` 做展示兜底（不改写快照、不影响打印）。
- `GeneratePlanUseCase` 在保存前冻结 `PlanRenderMeta`（题号、来源页、知识点名称、行数、附件数与附件文本）并传入 `PlanDraft`；`weekStartDate/weekEndDate` 取 `PlanningSettings` 的计划窗口。
- `PlanPreviewViewModel` 首次进入调用 `refreshIfNeeded`（open），从打印页返回复用当前状态；`regenerate()` 才生成新计划号并清空旧打印任务。

## 计划完成判定（C-099）

- `PlanStatus.COMPLETED` 由确认提交驱动，不按日期自动判定：当一份计划的所有 `plan_page` 页都已在 `confirmation_decision_log` 中出现，且本次提交没有整题跳过时，`RoomFeedbackRepository.saveDecisions` 在**同一事务**内把计划状态从 GENERATED/PRINTED 迁移为 COMPLETED。
- 判定比较的是页集合（`plan_page.pageId` ⊆ `confirmation_decision_log.pageId`），不是行数；其他计划或残留日志不会让计划提前完成。
- 已被显式替代（SUPERSEDED）或已经 COMPLETED 的计划不会被改写；有整题跳过时本次不判定，留待修复后的重试。
- `PlanRepository.activePlan` / `PlanDao.activeForWeek` 把 COMPLETED 视为可用计划：首页仍能查看/重印已完成计划；本周完成时首页把下周计划升为卡片主区块、本周降为次要行（C-100），下周未生成时主按钮为“生成下周计划”。
- 已完成计划允许重印：`savePrintSnapshots` 可为 COMPLETED 写题目快照，但 `markPrinted` 只把 GENERATED/PRINTED 置为 PRINTED，不得把 COMPLETED 降级。

## ID、时区与默认设置

- `planId`：6 位 Crockford Base32（去掉 I/L/O/U），由 `PlanIdGenerator` 生成；与 `PlanPage.planId` 冲突时重试 3 次，仍冲突则失败。该格式与全局唯一性是 `PagePlanner` 的前置条件，分页器只拼接、不校验（C-069 方案 A）。
- `pageId`：`"{planId}-{pageNumber:02d}"`，`pageNumber` 由 `PagePlanner` 从 1 连续分配；超过 99 时 `PagePlanner` 返回 `PaginationResult.Failure(PaginationFailureCode.PLAN_TOO_LONG, ...)`，`GeneratePlanUseCase` 转 `PlanGenerationException`，不写库。
- 业务日期用 `LocalDate`（设备本地时区）；审计/事件时间用 `Instant`（UTC）。生成计划时记录当时的本地日期；跨时区不自动改写已生成计划。
- 默认每日可用 60 分钟；用户可按星期调整。`planStartDate` 默认为今天所在自然周的周一，`planEndDate = planStartDate + 6`（周一到周日），允许用户改选任意连续 1～7 天。
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

- C-090 方案 A：`GeneratePlanUseCase.execute()` 返回 `PlanGenerationOutcome`；`Planned` 同时携带 `PrintJob`（打印层唯一输入）与 `PlanPreviewContent`（预览页唯一输入）；`Empty` 携带可读文案、`PlanPreviewWarning` 与 `PlanPreviewDeferred`，不得只返回页列表或丢弃提示。`PaginationResult.Failure` 不进入结果包装，仍按 C-047 抛 `PlanGenerationException`。
- 计划预览的页面卡片与“未排入计划”卡片使用同一题目标识 `页码-第X题`（无来源页码时 `第X题`）；重开计划时从冻结的 `renderMeta`/`plan.deferred` 重建，旧推迟快照缺来源页码可按当前题库兜底展示，但不得改写快照或依赖题库重算分页。
- 计划生成后：`PlanPage`、`Question.lastScheduledDate`、冷却事件消费、顺延在同一事务内；任一步失败全部回滚。
- C-099：所有页确认提交后计划迁移为 COMPLETED；只确认部分页、或有整题跳过时保持原状态；SUPERSEDED 计划不得改回 COMPLETED。
- `PagePlanner` 返回 `Failure` 时抛 `PlanGenerationException`，不进入事务、不写任何 `PlanPage`。
- C-074 方案 A：`PlanDraft`/`PlanGenerationException` 位于 `data:room`；草稿携带冻结行快照，编排层不重算行组。
- 同一 `planId` 重试生成不会产生重复 `PlanPage`；`planId` 冲突按规则重试/失败。
- 打印层只消费 `PrintJob`，不在 `onLayout` 查询数据库或重新分页。
