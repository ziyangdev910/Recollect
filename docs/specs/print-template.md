# 打印模板规格（feature/print）

关联决策：D-07、D-08、D-09
关联裁决：C-013、C-014、C-015、C-016、C-020、C-026、C-027、C-029、C-051、C-063、C-064、C-066、C-067、C-091、C-092

## 冻结决策（D-07～D-09）

- **D-07**（2026-11-24 用户要求调整）：每题上方左侧为知识点填涂区（约 42% 宽）、右侧为题号/来源/知识点信息区（约 58%）；两个模块下方改为通栏笔记区——上方是历史笔记显示区，下方是「复习笔记」书写区，QR 与 pageId 标签嵌在书写区右侧并排（书写区不再占满整幅宽度），题块不再有独立的 QR 页脚带。填涂框上方每页直接标注「掌握 / 较熟悉 / 不会」，不再绘制 A/B/C 图例；只打印 printable 知识点；左侧名称限一行、超 8–10 字截断；右侧保留全称，按编号以「 / 」拼接并允许换行。
- **D-08**：按日强制分页，条目不得跨页；整条移至下页，绝不拆分。不再绘制「续下页」标签与「本日共需复习 X 题」，只保留日期大标题。
- **D-09**：回读顺序 ArUco → 透视 → QR → OMR → OCR → 确认；QR 含版本/planId/pageId/questionId/校验码，不含知识点 ID；页码即 pageId；QR 失败走手动输入 pageId + 题号（纸面自 2026-11-18 起不显示完整 pageId，自 2026-11-24 起不再显示页序，只保留题号与来源页，手动回退影响见 `docs/conflicts.md` C-095）。

## 页面几何（A4 纵向 210×297mm）

- `TemplateGeometry` 的具体默认值见 `pagination.md`。
- **四角标记预留（C-091 方案 A）**：生产模板 `TemplateGeometry.a4Print()` 在 12mm 页边距基础上增加 14mm `cornerReserveMm`，内容区从 26mm 开始，避开 ArUco 内码 10mm + 白边 4mm；`contentLeftMm`/`contentTopMm` 是内容区边界，左上填涂区、右上信息、下方通栏笔记、QR 与 OMR 均以此为准。`a4Print()` 下内容区宽 158mm、可用高 245mm，左区 66.36mm、右区 91.64mm（`leftRatio = 0.42`）。唯一例外是 2026-11-25 的每日题序徽标：绘制在左侧 12–26mm 空白带内，且纵向低于左上 ArUco 白边（见下条）。
- 日期大标题（C-063）：每个日期的第一页顶部绘制日期大标题（如「2026-11-16 周一」）；同日后续页重复日期小标题。2026-11-22 用户要求删除「本日共需复习 X 题」，标题块高度只剩 `dateHeadingHeightMm`（默认 7mm），绘制在 `contentTopMm` 之前的预留带内（`a4Print()` 下为 19–26mm），**不占用内容区高度**。
- 页首：**不再绘制图例**。每页表头直接显示「序号 | 知识点 | 掌握 | 较熟悉 | 不会」，三个档位文字分别以对应填涂格中心居中（D-07；2026-11-22 用户要求）。表头文字底边锚定到本页第一条题目的实际起点（`PlacedItem.yOffsetMm - 3.5mm`），紧贴第一道题，不再在表头带内垂直居中；旧计划即使保留旧图例偏移，表头也会贴近该页首题。
- 左侧填涂区：约 40% 宽（`leftRatio = 0.42`，为加宽档位文字间距留出空间）；表头"序号 | 知识点 | 掌握 | 较熟悉 | 不会"；**每知识点一行，按 `KnowledgePoint.number` 升序稳定排列**（C-015）；仅打印 printable 知识点（`!dormant && activeQuestionCount > 0`）；名称限一行，**仅在左侧**超 10 个 Unicode 码点截断为前 9 字 + "…"（C-013）；三框为答题卡式 16:9 宽格（`bubbleWidthMm × bubbleHeightMm` = 5.7 × 3.2mm），水平等距、框间距 **3.1mm**，便于横向填涂并避免「掌握 / 较熟悉 / 不会」挤在一起（2026-11-22 用户要求）；序号列 6mm、知识点列 34mm、列间距 1.5mm；行高 7.5mm。三个填涂格中心 x 为 71.85 / 80.65 / 89.45mm（`a4Print()`）；OMR 仍只采样每个框中心 2.5mm×2.5mm 窗口（见 `scan-pipeline.md`）。
- 每日题序徽标（2026-11-25 用户要求）：以每日复习计划为编组，为每道题绘制「【n】」题序（1 起）；同一天跨页连续编号，换日从 1 重新开始。徽标 3.8mm 加粗，绘制在左侧「序号」列左边 1mm（题块左上角，位于 12–26mm 页边空白带），纵向与题块首行对齐；首题起点为 33mm，低于左上 ArUco 白边的 8–26mm，不会侵入定位标记，也不改变填涂区列几何、OMR 采样中心与 QR。
- 右上题目信息区：约 58% 宽；与左上填涂区并排，顶部信息段高度取两者较高者（`TemplateGeometry.topSectionHeightMm`）：
  - 第一行：`来源 第X页 第Y题` 加粗，与计划预览共用 `core:common` 的 `formatQuestionLabel`（2026-11-25 用户要求；此前为「题号 {questionNumber}　来源页 {sourcePage}」）：X 为页码编号、Y 为题号；`sourcePage` 为来源页码的展示串（用户可调编号，或 `编号-说明`，如 `2-同步练习`；说明为空时只有编号。2026-11-20 用户要求），没有来源说明时页码显示为 `编号X`（如 `编号2 第T-1题`），无来源页码时只显示 `第Y题`。QR 载荷仍为完整 `pageId`（C-014），但纸面不再显示完整 pageId，扫码失败后的手动回退见 `docs/conflicts.md` C-095；
  - 第二行起：**知识点全称**，与左侧同一 printable 集合、按 `number` 升序以 `" / "` 拼接成一个长文本，允许自动换行；不套用左侧的 8–10 字截断规则（C-013）；名称数量等于知识点数量，由计划/数据层保证（C-067）；
  - 换行与高度：行高 = `rowHeightMm`；最多 4 个视觉行；超过 4 行时字号从 3.2mm 逐级降到下限 2.8mm；仍超出则末行以省略号截断，并在计划生成结果中给出 `KNOWLEDGE_TEXT_OVERFLOW` 警告（由计划层产出并与分页警告合并，C-064；正常 1–4 个知识点不应触发）；
  - 换行后的视觉行数 `knowledgeLineCount` 传给 `PagePlanner`，与 `knowledgeCount` 一起决定顶部信息段高度（取左填涂行与右信息行的较高者）；
  - 不预留作答空白。
- 下方通栏笔记区（2026-11-24 用户要求）：位于顶部信息段之下，占满内容宽度：
  - 上方为历史笔记显示区：最多 3 条 × 2 行摘要（来源 `NoteAttachmentItem` 最近 3 条，文本取 `correctedText`，C-026），通栏显示；附加区总高 ≤ 单页可用高度 30%（C-027）。
  - 下方为「复习笔记」书写区：标题 + 横线书写区（默认 3 条横线，间距 `noteWritingLineStepMm = (noteAreaBaseHeightMm − noteTitleHeightMm − noteWritingBottomPaddingMm) / noteWritingLineCount = (30 − 5 − 1) / 3 = 8mm`），宽度 = 内容宽度 − QR 列（`qrSizeMm`）− 列间距（`noteQrColumnGapMm`，默认 3mm）；右侧与 QR 并排，不再占满整幅宽度。最后一条横线距书写行底部 1mm；书写区高度与 OCR 区域不变。
  - 题间距（2026-11-25 用户要求）：两道题之间的可见空白取「复习笔记两条横线之间的距离」。`itemSpacingMm` 取 5mm，加上最后一条横线到书写行底的 1mm、下一题首行文字的行内垂直居中（约 2mm），实测可见空白约 7.8mm ≈ 横线间距 8mm。
- 每题 QR（2026-11-24 用户要求，2026-11-25 调整对齐）：载荷为 `版本|planId|pageId|questionId|校验码`，不含知识点 ID（D-09、C-030），与 pageId 标签一起嵌在复习笔记书写区右侧；QR + 标签组合的**底边与最后一条复习笔记横线水平对齐**（`noteQrTopOffsetMm = lastNoteWritingLineOffsetMm − qrStackHeightMm`），组合必须完整落在题块内。完整 `pageId`（如 `K7M2Q9-01`）以 2.2mm 小字号绘制在 QR 下方，与 QR 的间距为标签高度的一半（2mm），并在 QR 宽度内右对齐（右缘与 QR 右缘一致），供扫码失败时手动输入（C-095 方案 B；2026-11-21 用户要求）；`hasNoteArea=false` 时无书写区，QR 仍落在题块底部的 `qrFooterHeightMm` 预留带内。右上只显示题号与来源页（「页码 XX」已于 2026-11-24 按用户要求删除）。
- 页面四角：ArUco 定位标记（四角各一个；页脚只放 QR）。标记按 `DICT_4X4_50` 的真实 6×6 模块图案绘制（`ArucoPattern`，纯 Kotlin 常量，不引入 OpenCV）。
- 留白区域：不放置任何方框，不参与 OMR。

## 文本测量与换行

- 统一按 300 DPI：`mm→px = 11.811`。
- `feature/print` 实现 `KnowledgeTextMeasurer`，供 `feature/planning` 在分页前计算 `knowledgeLineCount`。
- 右上全称使用 `Paint` + `Typeface.DEFAULT`（中文回退系统 Noto Sans CJK），字号 3.2mm ≈ 37.8px，行高 = `rowHeightMm`。
- 用 `Paint.breakText` 按 `rightTextWidthMm` 逐行折行；`knowledgeLineCount` 为实际视觉行数，传给 `PagePlanner`。
- 超过 4 行：字号从 3.2mm 逐级降到 2.8mm（3.2→3.0→2.8）；仍超过 4 行则末行 `…` 截断并产生 `KNOWLEDGE_TEXT_OVERFLOW` 警告。
- 左侧名称只在渲染层截断为 10 个 Unicode 码点（前 9 字 + `…`），不改 `KnowledgePoint.name`。

## ArUco 与 QR 几何

- ArUco：`DICT_4X4_50`，ID 0/1/2/3 = 左上/右上/右下/左下；内码 10mm，外加 4mm 白边，距页边 12mm。标记白边外沿到 8mm、内沿到 26mm；内容区必须按 `contentLeftMm`/`contentTopMm` 从 26mm 开始，**禁止标记/白边与日期标题、表头、条目不重叠**（C-091 方案 A）。
- QR：每题一个，版本 5、纠错 M、边长 18mm；2026-11-24 起在复习笔记书写区右侧并排（水平间距 `noteQrColumnGapMm` 默认 3mm），模块直接铺满 18mm 框，quiet zone 4 modules 由框外空白页边距提供。QR 与 pageId 标签的纵向堆高 `qrStackHeightMm = qrSizeMm + pageIdLabelGapMm + pageIdLabelHeightMm = 24mm`，2026-11-25 起组合底边与最后一条复习笔记横线对齐（`noteQrTopOffsetMm`），书写行高度仍为 `noteRowHeightMm = max(noteAreaBaseHeightMm, qrStackHeightMm)`；无笔记区时仍用题块底部预留带 `qrFooterHeightMm = qrSizeMm + qrPaddingMm + pageIdLabelGapMm + pageIdLabelHeightMm = 27mm`。载荷由打印层按 C-030 生成并直接编码进页面（ZXing core 3.5.3），不依赖扫描阶段补写。
- 载荷：`version|planId|pageId|questionId|CRC32`；当前约 64 字节，版本 5/M 的 84 字节容量足够。
- 一页多题时每题块各自带 QR；QR 中心 y 坐标用于归属题块。

## PrintJob

打印层只消费 `PrintJob`，不在 `onLayout` 查库或分页：

```kotlin
data class PrintJob(
    val planId: String,
    val pages: List<PlannedPage>,
    val questions: Map<String, RenderableQuestion>,
    val notes: Map<String, List<NoteAttachmentItem>>,
    val template: TemplateGeometry
)
```

## PrintDocumentAdapter 职责边界

- `onLayout`：`pageCount = PrintJob.pages.size`；用 `PrintAttributes` 固定 A4、300 DPI、页边距 0（边距由 Canvas 内部处理）；不重新分页（R-02）。
- `onWrite`：按 `PrintJob.pages` 逐页 Canvas 绘制；左上填涂区一行一组、组内三框，右上题目信息，下方通栏历史笔记与书写区，书写区右侧并排 QR，四角 ArUco。
- **条目整条移至下页时**，不绘制“续下页”文本，也不画框；分页结果中的 `continuationMarkers` 仅作内部元数据（C-063）。
- 渲染器不得在 `onLayout`/`onWrite` 中临时压缩行高来"塞下"条目；放不下就是放不下，由 `PagePlanner` 决定移页或溢出。
- 打印前由计划层把本页的 `PlanPage.questionIds` 与 `PlanPage.knowledgeRows`（行快照 + trigger 标记）写入数据库，并把每道题的 `Question.lastScheduledDate` 写为该题的目标日；没有快照的旧稿不得按当前关联重算，回读/确认时阻断并提示重新生成计划、按新稿打印（C-009/C-015/C-033 方案一、C-072 方案 A）。
- 打印文档写出成功后，App 内必须显示「已打印 / 未打印」确认（C-094 方案 B）；只有用户点「已打印」才写 `Question.pageId` 与 `Question.qrPayload`（最近一次打印快照），点「未打印」或直接离开则不写。快照写入还必须校验计划仍为当前计划（GENERATED/PRINTED）：已被“重新生成本周计划”替代的 SUPERSEDED 计划不得覆盖最新快照，并提示未更新（C-097）。系统 `onWrite` 只代表文档生成，不代表纸张打印完成；打印失败不写（C-033 方案一）。「分享 PDF」使用同一份 `PrintLayout` 与 `PrintPdfWriter` 生成缓存文件并经 `FileProvider` 打开系统分享面板；分享成功后同样写入 `Question.pageId/qrPayload` 并标记计划 PRINTED（与「已打印」同一回写入口），计划已被替代时不更新并提示（2026-11-24 用户要求）。
- `PlanPage` 已在 `feature/planning` 持久化；打印层不重复写。

## 验收（C-029 方案一：真机/模拟器上的引擎回读）

1. 用 `TemplateGeometry.a4Print()` 与 `PrintedPdfDocument` 生成 A4 PDF；保存到 `evidence/print/`。
2. 在**模拟器或真机**的 instrumented test（`androidTest`）中用 Android `PdfRenderer` 按 300 DPI 渲染每页为 Bitmap（A4 约 2480×3508 px，`mm → px = 300 / 25.4 ≈ 11.811`）。
3. 把渲染图喂给 **自研回读引擎**（`feature:scan` 的 `ScanEngine`：OpenCV 定位/透视 + ML Kit 逐题 QR + 自采样 OMR）执行实际图像回读；这是硬门禁（C-003、R-01）。
   - C-092 方案 C：不再使用 OpenScanVision 的 850×540 适配层。四角标记按 `DICT_4X4_50` 真实图案绘制，扫描端以相同字典检测并矫正到 2480×3508；PDF 光栅位图只作功能验证，R-01 仍以真实打印稿照片为准。
4. 断言：
   - 左侧按行分组正确，掌握/较熟悉/不会三框采样不被跨行合并；
   - 每题左上角显示「【n】」每日题序，同一天跨页连续、换日从 1 重置，且不侵入四角 ArUco 白边；
   - 左侧名称截断不影响框位；右上全称换行不产生气泡组；
   - 右上题目信息区文字、下方历史笔记与书写区横线、右侧 QR 不被误采样为气泡组；
   - 留白区不产生气泡判定；
   - 四角 ArUco 的标记/白边不与日期标题、表头、首行知识点重叠（C-091 方案 A，内容区从 26mm 开始）；
   - QR 解码成功，payload 含正确 `questionId`，CRC 通过；
   - 每页表头都出现「掌握 / 较熟悉 / 不会」，且三列中心与填涂格中心一致；页面中不再出现 A/B/C 图例；
   - 附加笔记区高度**超过**可用高度 30% 的条目不被打印（进入 overflow）。
5. 证据存 `evidence/print/`：PDF、每页 PNG、引擎原始输出、测试报告。
6. 人工比对几何仅作辅助记录，**不能单独作为验收依据**（C-003）。
7. 不合格时优先调整几何参数（比例、行高、框间距、名称截断、字号），不回退 D-07 整体布局。
8. 不允许只在桌面 JVM 上用 `pdftoppm` 回读就宣称通过 R-01；`pdftoppm` 仅可用于人工目视辅助，不作为门禁。

## 明确不做的事

- 不引入 iText/PDFBox；使用 `PrintedPdfDocument` + Canvas。
- 不做 Letter/其他纸张规格。
- 不在本模块做二维码生成逻辑之外的内容排版决策。
- 不在渲染层做「压缩到放得下」的兜底；溢出由 `core/pagination` 判定。
