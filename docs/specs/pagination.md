# 分页规划器规格（core/pagination）

关联决策：D-07、D-08
关联裁决：C-013、C-015、C-016、C-025、C-027、C-047、C-048

## 冻结决策（D-07、D-08）

- **D-07**：只打印 printable 知识点；左侧名称限一行、8–10 字截断；右上全称以「 / 」拼接、允许换行；图例仅首页一次。
- **D-08**：按日分页、条目不得跨页；整条移至下页、不拆分；每移一条一个「续下页」标记，按题目顺序堆叠。

## 接口

```kotlin
interface PagePlanner {
    fun plan(
        planId: String,
        dailyGroups: Map<LocalDate, List<RenderableQuestion>>,
        template: TemplateGeometry
    ): PaginationResult
}

data class RenderableQuestion(
    val questionId: String,
    val questionNumber: String,
    val knowledgeCount: Int,                  // 1..4，只计 printable 知识点（!dormant && activeQuestionCount > 0）
    val knowledgeNames: List<String>,         // 全称，按 KnowledgePoint.number 升序（C-013/C-015）
    val knowledgeLineCount: Int,              // 右上全称换行后的视觉行数，由 feature/print 计算
    val attachmentCount: Int,                 // 0..3
    val hasNoteArea: Boolean = true
)

data class TemplateGeometry(
    val pageWidthMm: Double = 210.0,
    val pageHeightMm: Double = 297.0,
    val horizontalMarginMm: Double = 12.0,    // 左右页边距
    val topMarginMm: Double = 12.0,
    val bottomMarginMm: Double = 12.0,
    val leftRatio: Double = 0.40,
    val textPaddingMm: Double = 3.0,          // 右上文本区内边距
    val legendHeightMm: Double = 8.0,         // 首页图例；仅文档首页扣除
    val headerRowHeightMm: Double = 7.0,      // 每页左侧表头，每页扣除一次
    val rowHeightMm: Double = 7.5,            // 每知识点行高
    val questionFixedInfoHeightMm: Double = 7.0, // 右上第一行
    val noteAreaBaseHeightMm: Double = 30.0,
    val attachmentHeightPerItemMm: Double = 10.0,
    val itemSpacingMm: Double = 3.0,
    val qrSizeMm: Double = 18.0,
    val qrPaddingMm: Double = 3.0,
    val maxAttachmentHeightRatio: Double = 0.30   // C-027
) {
    val qrFooterHeightMm: Double
        get() = qrSizeMm + qrPaddingMm
    val usableHeightMm: Double
        get() = pageHeightMm - topMarginMm - bottomMarginMm
    val firstPageUsableHeightMm: Double
        get() = usableHeightMm - legendHeightMm
    val contentWidthMm: Double
        get() = pageWidthMm - 2 * horizontalMarginMm
    val leftAreaWidthMm: Double
        get() = contentWidthMm * leftRatio
    val rightAreaWidthMm: Double
        get() = contentWidthMm * (1 - leftRatio)
    val rightTextWidthMm: Double
        get() = rightAreaWidthMm - 2 * textPaddingMm
    val maxAttachmentHeightMm: Double
        get() = usableHeightMm * maxAttachmentHeightRatio
}

sealed interface PaginationResult {
    data class Success(
        val pages: List<PlannedPage>,
        val overflowItems: List<OverflowItem>,
        val warnings: List<PaginationWarning>
    ) : PaginationResult

    data class Failure(
        val code: PaginationFailureCode,
        val message: String
    ) : PaginationResult
}

enum class PaginationFailureCode { INVALID_TEMPLATE_GEOMETRY, PLAN_TOO_LONG }

data class PlannedPage(
    val pageId: String,                       // "{planId}-{pageNumber:02d}"
    val pageNumber: Int,                      // 1..99，全文档连续
    val date: LocalDate,
    val items: List<PlacedItem>,
    val continuationMarkers: List<ContinuationMarker>   // 按 order 升序
)

data class PlacedItem(
    val questionId: String,
    val yOffsetMm: Double,                    // 从页面顶边起算（含 topMarginMm）
    val heightMm: Double
)

data class ContinuationMarker(
    val fromPageId: String,
    val toPageId: String,
    val questionId: String,
    val order: Int                            // 当页内从 0 递增，按题目顺序
)

data class OverflowItem(
    val questionId: String,
    val date: LocalDate,
    val heightMm: Double,
    val reason: OverflowReason
)

enum class OverflowReason { ITEM_TOO_TALL, ATTACHMENT_OVER_LIMIT }

data class PaginationWarning(
    val questionId: String,
    val code: String,                         // 例如 KNOWLEDGE_TEXT_OVERFLOW
    val message: String
)
```

## 可用高度（C-025 方案一）

- `usableHeightMm = pageHeightMm - topMarginMm - bottomMarginMm`，所有容量判断的基准，不再使用 `pageHeightMm * 0.70`。
- `firstPageUsableHeightMm = usableHeightMm - legendHeightMm`，仅文档首页使用；后续页不扣图例。
- **每页实际内容区高度**（用于放置条目与 `ITEM_TOO_TALL` 判定）：
  - `contentHeightMm = usableHeightMm - headerRowHeightMm`
  - `firstPageContentHeightMm = firstPageUsableHeightMm - headerRowHeightMm`
  `headerRowHeightMm` 是页级表头，每页只扣一次，不随条目重复计入。
- `maxAttachmentHeightMm = usableHeightMm * maxAttachmentHeightRatio`（默认 30%）；该上限与首页/后续页无关，首页空间不足由条目高度判定处理。
- 模板参数必须满足 `usableHeightMm > 0` 且 `firstPageContentHeightMm > 0`；否则返回 `PaginationResult.Failure(PaginationFailureCode.INVALID_TEMPLATE_GEOMETRY, ...)`，不得静默产生负高度页。

## 单题高度公式（必须原样实现并测试）

```text
leftHeight    = knowledgeCount * rowHeightMm
rightTop      = questionFixedInfoHeightMm + knowledgeLineCount * rowHeightMm
attachmentArea = attachmentCount * attachmentHeightPerItemMm
rightBottom   = if (hasNoteArea) noteAreaBaseHeightMm + attachmentArea else 0.0
itemHeight    = max(leftHeight, rightTop + rightBottom) + qrFooterHeightMm + itemSpacingMm
```

- `knowledgeLineCount` 由 `feature/print` 按 `print-template.md` 的右上换行规则计算；不在 `core/pagination` 里猜测文本换行。
- `attachmentArea > maxAttachmentHeightMm` → `OverflowItem(reason = ATTACHMENT_OVER_LIMIT)`，整条不打印，提示用户减少附加笔记或拆分题目（C-027）。
- `itemHeight > contentHeightMm` → `OverflowItem(reason = ITEM_TOO_TALL)`，整条不打印，绝不在分页器内拆分或压缩（C-016/C-025）。
- 单题高度上限由模板参数保证：`noteAreaBaseHeightMm` 不得随条目增长；若配置导致常规条目普遍超过 `contentHeightMm`，属于模板参数错误，应在测试中失败。

## 分页规则

- 输入 `Map<LocalDate, List<RenderableQuestion>>` 必须先按 `LocalDate` 升序排序再分页；输出与输入 Map 的迭代顺序无关（C-025）。
- 按日期分组，每个日期从新页开始（D-08）；同一天产生多页时，页序连续，`PlannedPage.date` 相同。
- 条目整条放置：当前页剩余高度不足以放下整条时，整条移到下一页，**不得拆分**（C-016）。
- 被移走的条目在当页留白区底部生成**一个** `ContinuationMarker`（"续下页"），不画填涂框；多个被移条目按题目顺序生成多个 marker，`order` 从 0 递增，渲染时从上到下堆叠（C-016）。
- 如果条目连一个空白的后续页都放不下（`itemHeight > contentHeightMm`），进入 `overflowItems`，不进入 `pages`。
- 如果条目能放进后续页但放不进首页（首页扣了图例），首页生成 marker，条目放到下一页；首页即使没有条目也要存在，以承载图例。
- 留白区域不放置任何方框，不参与 OMR 采样。
- `pageId = "{planId}-{pageNumber:02d}"`；`pageNumber` 从 1 连续递增，超过 99 时返回 `PaginationResult.Failure(PaginationFailureCode.PLAN_TOO_LONG, ...)`（计划过长，需拆分计划）。
- `Success.overflowItems` 不进入 `pages`，不打印；`Success.warnings` 同时给出用户可读提示（含 `questionId` 与原因）。
- 图例仅文档首页出现一次（D-07、C-001）：仅首页可用高度扣除 `legendHeightMm`，后续页不扣除。
- `PagePlanner` 只返回映射，不写数据库；`feature/planning` 负责持久化 `PlanPage`（pageId/pageNumber/date/questionIds/knowledgeRows/markers）并把 `Question.lastScheduledDate` 写为目标日（C-033 方案一）。

## 放置语义（C-048，必须原样实现）

- `PlacedItem.yOffsetMm = topMarginMm + (pageNumber == 1 ? legendHeightMm : 0) + headerRowHeightMm + Σ(当页已放置条目的 itemHeight)`；从页面顶边起算。
- `PlacedItem.heightMm = itemHeight`（含 `itemSpacingMm`）。
- 当前页按输入顺序尝试放置；**遇到第一条放不下的条目就结束当前页**，该条及后续条目按原顺序进入下一页；不得跳过该条去填更小的后续条目。
- 每条被移走的条目在来源页生成一个 `ContinuationMarker`：`fromPageId=来源页`、`toPageId=实际放置页`、`order` 按该页被移条目的题目顺序从 0 递增；不画填涂框。
- 条目高于 `contentHeightMm` 时进入 `overflowItems(ITEM_TOO_TALL)`，不生成 marker、不打印、不拆分/压缩。
- 空日期（`List` 为空）不建页；`dailyGroups` 全空时返回 `Success(emptyList(), emptyList(), emptyList())`。有任意条目时，首页即使没有条目也保留以承载图例。
- `overflowItems` 按 `date` 升序、同日按输入题目顺序；`warnings` 按 `questionId` 字典序升序。
- 非法输入抛 `IllegalArgumentException`：`knowledgeCount !in 1..4`、`attachmentCount !in 0..3`、`knowledgeLineCount !in 1..4`；不静默截断或补 0。

## 用例表

| ID | 场景 | 断言 |
|---|---|---|
| P-01 | 一日一条，高度正常 | 一页一条 |
| P-02 | 一日多条，恰好填满 | 无 overflow，无跨页 |
| P-03 | 一日多条，最后一条放不下 | 该条整条移下一页；当页恰有一个 `ContinuationMarker`，`order=0` |
| P-04 | 单题知识点 1/2/3/4 | `leftHeight` 按公式递增；页级表头只扣一次 |
| P-05 | 开启附加 1/2/3 条 | `rightBottom` 递增；附加区超过 `usableHeightMm * 0.30` 时整条进入 `overflowItems(ATTACHMENT_OVER_LIMIT)` |
| P-06 | 左侧比右侧高 | `itemHeight` 取左侧 |
| P-07 | 右侧比左侧高 | `itemHeight` 取右侧和 |
| P-08 | 跨多日 | 每日从新页开始；日期升序；同日多页连续 |
| P-09 | 属性测试：随机 100 组 | 无条目跨页；`continuationMarkers` 与被移条目一一对应；marker 数量、`order` 与题目顺序一致；`overflowItems` 不出现在 `pages` |
| P-10 | 首页与后续页 | 首页内容区扣 `legendHeightMm` 与 `headerRowHeightMm`；后续页只扣 `headerRowHeightMm`（C-001） |
| P-11 | 附加区 30% 上限 | 恰好 30% 通过；超过 1mm 进入 `ATTACHMENT_OVER_LIMIT`；不截断附加区 |
| P-12 | 输入 Map 乱序 | 输出按日期升序，与输入顺序无关 |
| P-13 | 条目能进后续页、进不了首页 | 首页生成 marker（即使首页没有其它条目）；条目在下一页完整出现；判定用 `firstPageContentHeightMm` 与 `contentHeightMm` |
| P-14 | 条目高于 `contentHeightMm` | 进入 `ITEM_TOO_TALL`，不拆分、不压缩 |
| P-15 | `hasNoteArea=false` | `rightBottom = 0`，附加区不计入 |
| P-16 | `knowledgeLineCount` 1..4 | `rightTop` 按行数递增，不按 `knowledgeCount` 递增 |
| P-17 | 同一题重复分页 / 重启后重规划 | `knowledgeNames` 按 `number` 升序输入，行序与名称顺序稳定不变（C-015） |
| P-18 | `itemHeight` 落在 `(contentHeightMm, usableHeightMm]` | 必须进入 `ITEM_TOO_TALL`；不得出现「既放不下又不在 overflow」的区间 |
| P-19 | QR 预留区 | `itemHeight` 包含 `qrFooterHeightMm`；QR 不挤占笔记书写区；`template.qrFooterHeightMm = qrSizeMm + qrPaddingMm` |
| P-20 | 非法计数字段 | `knowledgeCount`/`attachmentCount`/`knowledgeLineCount` 越界时抛 `IllegalArgumentException`，不静默截断或补 0 |

## 验收门禁

- `./gradlew :core:pagination:test` 全绿
- 属性测试通过（JUnit `@RepeatedTest(100)` + `kotlin.random.Random` 手写实现，种子固定以保证可复现；不引入 jqwik/kotest 等属性测试库，C-002）
- 测试中不出现任何 `android.*` import
- 输入乱序、首页/后续页、30% 附加区、单题超高四个边界必须有独立用例
- `PaginationResult.Failure` 覆盖 `INVALID_TEMPLATE_GEOMETRY` 与 `PLAN_TOO_LONG`；非法计数字段抛 `IllegalArgumentException`

## 明确不做的事

- 不在本模块绘制 Canvas；渲染在 `feature/print`
- 不做纸张规格自适应；仅 A4 纵向
- 不处理二维码生成
- 不拆分条目、不压缩行高、不截断附加区来「塞进」页面
