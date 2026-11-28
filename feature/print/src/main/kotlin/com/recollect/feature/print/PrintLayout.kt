package com.recollect.feature.print

import com.recollect.common.formatQuestionLabel
import com.recollect.common.qr.QrPayloadCodec
import com.recollect.pagination.PlannedPage
import com.recollect.pagination.RenderableQuestion
import com.recollect.pagination.TemplateGeometry
import java.time.LocalDate

/**
 * 打印页面布局（print-template.md）。
 *
 * 本层只把 `PagePlanner` 的 `PlannedPage`/`PlacedItem` 映射成可绘制的毫米坐标：
 * - 条目位置与高度原样取自 `PlacedItem`，不重新分页、不压缩行高（R-02）；
 * - 日期大标题绘制在上边距内，不占内容区（C-063；2026-11-22 删除「本日共需复习 X 题」）；
 * - 填涂框上方的表头直接标注「掌握 / 较熟悉 / 不会」，每页重复，不再绘制 A/B/C 图例；
 * - `hasNoteArea=false` 时不生成笔记区与附件（C-062）；
 * - overflow 条目不在 `pages` 中，因此不会产生占位行或空白页（C-060/C-061）。
 */
data class PrintLayout(
    val planId: String,
    val pages: List<PrintPageLayout>,
)

data class PrintPageLayout(
    val pageId: String,
    val pageNumber: Int,
    val date: LocalDate,
    val pageWidthMm: Double,
    val pageHeightMm: Double,
    val contentLeftMm: Double,
    val contentTopMm: Double,
    val contentWidthMm: Double,
    val leftAreaWidthMm: Double,
    val rightAreaLeftMm: Double,
    val rightAreaWidthMm: Double,
    val rightTextWidthMm: Double,
    val dateHeading: PrintText?,
    /**
     * 表头「序号 / 知识点 / 掌握 / 较熟悉 / 不会」按列拆开：知识点列与左侧名称文本左对齐，
     * 三个档位文字分别以对应填涂格中心居中，避免整行字符串的字体宽度造成错位。
     */
    val headerCells: List<PrintText>,
    val items: List<PrintItemLayout>,
    val arucoMarkers: List<ArucoMarkerLayout>,
)

/**
 * 单题排版（2026-11-24 用户要求）：
 * 上方左侧为知识点填涂区、右侧为题号/来源/知识点；
 * 下方先通栏显示「之前的复习笔记」，再是复习笔记书写区，
 * QR 与 pageId 标签嵌在书写区右侧（书写区不占满整幅宽度）。
 *
 * 两道题之间的垂直空白来自 `PlacedItem.heightMm` 中包含的
 * `TemplateGeometry.itemSpacingMm`（2026-11-25 起默认 1.5mm），本层不另设间距常量。
 */
data class PrintItemLayout(
    val questionId: String,
    /**
     * 当日题序（1 起）：以每日复习计划为编组，同一天跨页连续编号，换日从 1 重新开始。
     * 题序徽标绘制在左侧「序号」列的左边，即整个题块的左上角（2026-11-25 用户要求）。
     */
    val dailyIndex: Int,
    /** 题序徽标「【n】」；位于题块左上角（左侧「序号」列左侧的页边空白带），加粗。 */
    val questionIndexText: PrintText,
    val blockTopMm: Double,
    val blockHeightMm: Double,
    val left: LeftAreaLayout,
    val rightTop: RightTopLayout,
    /** 附加历史笔记显示区（通栏）；无历史笔记或 `hasNoteArea=false` 时为 null。 */
    val notesDisplay: NotesDisplayLayout?,
    /** 复习笔记书写区（左侧）；`hasNoteArea=false` 时为 null。 */
    val note: NoteAreaLayout?,
    val qr: QrReservationLayout,
    val qrPayload: String, // 每题 QR 载荷（D-09、C-030）；渲染层据此生成位图
)

data class LeftAreaLayout(
    val xMm: Double,
    val widthMm: Double,
    val rows: List<LeftRowLayout>,
)

data class LeftRowLayout(
    val index: Int, // 左侧「序号」列，按题块内行序从 1 递增
    val topMm: Double,
    val heightMm: Double,
    val knowledgeName: String, // 已按左侧规则截断；不改知识点原值
    val indexText: PrintText,
    val nameText: PrintText,
    val bubbles: List<BubbleLayout>,
)

data class BubbleLayout(
    val label: String, // A / B / C
    val leftMm: Double,
    val topMm: Double,
    /** 答题卡式 16:9 宽格（2026-11-22 用户要求）。 */
    val widthMm: Double,
    val heightMm: Double,
)

data class RightTopLayout(
    val xMm: Double,
    val topMm: Double,
    val widthMm: Double,
    val heightMm: Double,
    val pageLine: PrintText, // 题目标识「来源 第X页 第Y题」，加粗
    val knowledgeText: PrintText, // 知识点全称，允许换行
)

/** 附加历史笔记显示区（位于书写区上方，通栏显示）。 */
data class NotesDisplayLayout(
    val xMm: Double,
    val topMm: Double,
    val widthMm: Double,
    val heightMm: Double,
    val texts: List<PrintText>,
)

/** 复习笔记书写区；右侧与 QR 列并排，`widthMm` 不含 QR 列。 */
data class NoteAreaLayout(
    val xMm: Double,
    val topMm: Double,
    val widthMm: Double,
    val heightMm: Double,
    val title: PrintText,
    val writingLines: List<PrintLine>,
)

data class QrReservationLayout(
    val leftMm: Double,
    val topMm: Double,
    val sizeMm: Double,
    val quietZoneModules: Int,
    /** C-095 方案 B：QR 下方右对齐的小字号完整 pageId，供扫码失败时手动输入。 */
    val pageIdLabel: PrintText,
)

data class ArucoMarkerLayout(
    val id: Int, // 0/1/2/3 = 左上/右上/右下/左下
    val code: PrintRect,
    val quietZoneMm: Double,
)

data class PrintText(
    val text: String,
    val leftMm: Double,
    val topMm: Double,
    val widthMm: Double,
    val heightMm: Double,
    val fontMm: Double,
    val bold: Boolean = false,
    val maxLines: Int = 1,
    /** 逐级降字号候选（print-template.md：3.2 → 3.0 → 2.8mm）；空表示不降字号。 */
    val fontFallbacksMm: List<Double> = emptyList(),
    /** 水平对齐；默认左对齐，表头的三个档位文字在各自列宽内居中。 */
    val align: PrintTextAlign = PrintTextAlign.Left,
)

enum class PrintTextAlign {
    Left,
    Center,
    /** 右对齐：行尾对齐到 `leftMm + widthMm`（pageId 标签与 QR 右缘对齐）。 */
    Right,
}

data class PrintLine(
    val leftMm: Double,
    val topMm: Double,
    val rightMm: Double,
)

data class PrintRect(
    val leftMm: Double,
    val topMm: Double,
    val rightMm: Double,
    val bottomMm: Double,
) {
    val widthMm: Double get() = rightMm - leftMm
    val heightMm: Double get() = bottomMm - topMm
}

object PrintLayoutBuilder {

    fun build(
        planId: String,
        pages: List<PlannedPage>,
        questions: Map<String, RenderableQuestion>,
        noteTexts: Map<String, List<String>>,
        template: TemplateGeometry,
    ): PrintLayout {
        // 以日期为编组跨页累计题序：同一天的第 2 页接着第 1 页继续编号，换日归零。
        val dailyCounters = mutableMapOf<LocalDate, Int>()
        return PrintLayout(
            planId = planId,
            pages = pages.map { page ->
                pageLayout(
                    planId = planId,
                    page = page,
                    questions = questions,
                    noteTexts = noteTexts,
                    template = template,
                    dailyCounters = dailyCounters,
                )
            },
        )
    }

    private fun pageLayout(
        planId: String,
        page: PlannedPage,
        questions: Map<String, RenderableQuestion>,
        noteTexts: Map<String, List<String>>,
        template: TemplateGeometry,
        dailyCounters: MutableMap<LocalDate, Int>,
    ): PrintPageLayout {
        val contentLeft = template.contentLeftMm
        val contentTop = template.contentTopMm
        val contentWidth = template.contentWidthMm
        val leftAreaWidth = template.leftAreaWidthMm
        val rightAreaLeft = contentLeft + leftAreaWidth
        val rightAreaWidth = template.rightAreaWidthMm

        // 日期标题块绘制在内容区上边界之前的预留带内，不占用内容区高度（C-063/C-091）。
        val headingBlockTop = template.contentTopMm - template.dateHeadingBlockHeightMm
        // 每个日期第一页绘制日期大标题；同日后续页重复日期小标题（print-template.md）。
        // 2026-11-22：删除「本日共需复习 X 题」，标题块只剩日期本身。
        val dateHeading = PrintText(
            text = formatDate(page.date),
            leftMm = contentLeft,
            topMm = headingBlockTop,
            widthMm = contentWidth,
            heightMm = template.dateHeadingHeightMm,
            fontMm = template.dateHeadingFontMm,
            bold = true,
        )

        // 表头与左侧填涂行共用列几何：知识点左对齐名称列，三个档位文字对齐三格中心。
        // 以本页第一条题目的实际起点为锚：新计划为 contentTop + headerRowHeight，
        // 旧计划即使保留了旧图例偏移，表头也会贴着该页第一条题目。
        val firstItemTopMm = page.items.firstOrNull()?.yOffsetMm
            ?: (contentTop + template.headerRowHeightMm)
        val headerCells = buildHeaderCells(
            contentLeft = contentLeft,
            firstItemTopMm = firstItemTopMm,
            template = template,
        )

        return PrintPageLayout(
            pageId = page.pageId,
            pageNumber = page.pageNumber,
            date = page.date,
            pageWidthMm = template.pageWidthMm,
            pageHeightMm = template.pageHeightMm,
            contentLeftMm = contentLeft,
            contentTopMm = contentTop,
            contentWidthMm = contentWidth,
            leftAreaWidthMm = leftAreaWidth,
            rightAreaLeftMm = rightAreaLeft,
            rightAreaWidthMm = rightAreaWidth,
            rightTextWidthMm = template.rightTextWidthMm,
            dateHeading = dateHeading,
            headerCells = headerCells,
            items = page.items.map { placed ->
                val question = requireNotNull(questions[placed.questionId]) {
                    "打印输入缺少题目 ${placed.questionId}；计划层必须在分页前补齐投影（C-067）"
                }
                val dailyIndex = dailyCounters.merge(page.date, 1, Int::plus)!!
                itemLayout(
                    planId = planId,
                    pageId = page.pageId,
                    placedQuestionId = placed.questionId,
                    placedTop = placed.yOffsetMm,
                    placedHeight = placed.heightMm,
                    dailyIndex = dailyIndex,
                    question = question,
                    noteTexts = noteTexts[placed.questionId].orEmpty(),
                    template = template,
                    contentLeft = contentLeft,
                    contentWidth = contentWidth,
                    leftAreaWidth = leftAreaWidth,
                    rightAreaLeft = rightAreaLeft,
                    rightAreaWidth = rightAreaWidth,
                )
            },
            arucoMarkers = arucoMarkers(template),
        )
    }

    private fun itemLayout(
        planId: String,
        pageId: String,
        placedQuestionId: String,
        placedTop: Double,
        placedHeight: Double,
        dailyIndex: Int,
        question: RenderableQuestion,
        noteTexts: List<String>,
        template: TemplateGeometry,
        contentLeft: Double,
        contentWidth: Double,
        leftAreaWidth: Double,
        rightAreaLeft: Double,
        rightAreaWidth: Double,
    ): PrintItemLayout {
        val rowCount = question.knowledgeCount
        val knowledgeLineCount = question.knowledgeLineCount.coerceAtLeast(1)
        val topSectionHeight = template.topSectionHeightMm(rowCount, knowledgeLineCount)

        val leftRows = (0 until rowCount).map { index ->
            val rowTop = template.bubbleRowTopMm(placedTop, index)
            val displayName = truncateForLeftArea(question.knowledgeNames.getOrElse(index) { "" })
            LeftRowLayout(
                index = index + 1,
                topMm = rowTop,
                heightMm = template.rowHeightMm,
                knowledgeName = displayName,
                indexText = PrintText(
                    text = (index + 1).toString(),
                    leftMm = contentLeft,
                    topMm = rowTop,
                    widthMm = template.indexColumnWidthMm,
                    heightMm = template.rowHeightMm,
                    fontMm = 3.2,
                ),
                nameText = PrintText(
                    text = displayName,
                    leftMm = knowledgeNameLeftMm(contentLeft, template),
                    topMm = rowTop,
                    widthMm = template.knowledgeColumnWidthMm,
                    heightMm = template.rowHeightMm,
                    fontMm = 3.2,
                ),
                bubbles = bubbleLayouts(rowTop, template),
            )
        }

        val textLeft = rightAreaLeft + template.textPaddingMm
        // 2026-11-25 用户要求：每日题序徽标置于左侧「序号」列的左边，即整个题块的左上角。
        // 徽标落在页边距与内容区之间的空白带（12–26mm），不改变填涂区列几何与 OMR 采样中心。
        val questionIndexText = PrintText(
            text = "【$dailyIndex】",
            leftMm = contentLeft - QUESTION_INDEX_WIDTH_MM - QUESTION_INDEX_GAP_MM,
            topMm = placedTop,
            widthMm = QUESTION_INDEX_WIDTH_MM,
            heightMm = template.rowHeightMm,
            fontMm = QUESTION_INDEX_FONT_MM,
            bold = true,
        )
        val rightTop = RightTopLayout(
            xMm = rightAreaLeft,
            topMm = placedTop,
            widthMm = rightAreaWidth,
            heightMm = topSectionHeight,
            pageLine = PrintText(
                text = pageLineText(question),
                leftMm = textLeft,
                topMm = placedTop,
                widthMm = template.rightTextWidthMm,
                heightMm = template.questionFixedInfoHeightMm,
                fontMm = 3.2,
                bold = true,
            ),
            knowledgeText = PrintText(
                text = question.knowledgeNames.joinToString(NAME_SEPARATOR),
                leftMm = textLeft,
                topMm = placedTop + template.questionFixedInfoHeightMm,
                widthMm = template.rightTextWidthMm,
                heightMm = knowledgeLineCount * template.rowHeightMm,
                fontMm = 3.2,
                maxLines = knowledgeLineCount,
                fontFallbacksMm = listOf(3.2, 3.0, 2.8),
            ),
        )

        // 下方通栏：先显示历史笔记（可选），再是书写区；QR 与书写区并排。
        val bottomTop = placedTop + topSectionHeight
        val notesDisplayHeight = if (question.hasNoteArea) {
            template.notesDisplayHeightMm(question.attachmentCount)
        } else {
            // C-062：纸上没有笔记区时不绘制历史笔记。
            0.0
        }
        val notesDisplay = if (notesDisplayHeight > 0.0) {
            notesDisplayLayout(
                attachmentCount = question.attachmentCount,
                noteTexts = noteTexts,
                topMm = bottomTop,
                contentLeft = contentLeft,
                contentWidth = contentWidth,
                template = template,
            )
        } else {
            null
        }

        val note = if (question.hasNoteArea) {
            noteAreaLayout(
                topMm = bottomTop + notesDisplayHeight,
                template = template,
                contentLeft = contentLeft,
            )
        } else {
            null
        }

        val qrTop = if (question.hasNoteArea) {
            // 2026-11-25 用户要求：QR + pageId 标签组合的底边与最后一条复习笔记横线对齐。
            bottomTop + notesDisplayHeight + template.noteQrTopOffsetMm
        } else {
            // 无笔记区时退化为题块底部 QR 预留带（QR 仍须落在题块内供归属绑定）。
            bottomTop + template.qrPaddingMm
        }
        val qrLeft = template.qrColumnLeftMm

        return PrintItemLayout(
            questionId = placedQuestionId,
            dailyIndex = dailyIndex,
            questionIndexText = questionIndexText,
            blockTopMm = placedTop,
            blockHeightMm = placedHeight,
            left = LeftAreaLayout(
                xMm = contentLeft,
                widthMm = leftAreaWidth,
                rows = leftRows,
            ),
            rightTop = rightTop,
            notesDisplay = notesDisplay,
            note = note,
            qr = QrReservationLayout(
                leftMm = qrLeft,
                topMm = qrTop,
                sizeMm = template.qrSizeMm,
                quietZoneModules = QR_QUIET_ZONE_MODULES,
                // 2026-11-21 用户要求：完整 pageId 移到 QR 下方，间距缩短为标签高度的一半（2mm），
                // 并在 QR 宽度内右对齐（右缘与 QR 右缘一致）。
                pageIdLabel = PrintText(
                    text = pageId,
                    leftMm = qrLeft,
                    topMm = qrTop + template.qrSizeMm + template.pageIdLabelGapMm,
                    widthMm = template.qrSizeMm,
                    heightMm = template.pageIdLabelHeightMm,
                    fontMm = PAGE_ID_LABEL_FONT_MM,
                    align = PrintTextAlign.Right,
                ),
            ),
            qrPayload = QrPayloadCodec.encode(
                planId = planId,
                pageId = pageId,
                questionId = placedQuestionId,
            ),
        )
    }

    /** 附加历史笔记显示区：通栏，最多 3 条 × 2 行摘要（C-026）。 */
    private fun notesDisplayLayout(
        attachmentCount: Int,
        noteTexts: List<String>,
        topMm: Double,
        contentLeft: Double,
        contentWidth: Double,
        template: TemplateGeometry,
    ): NotesDisplayLayout {
        val textLeft = contentLeft + template.textPaddingMm
        val textWidth = contentWidth - 2 * template.textPaddingMm
        val texts = noteTexts.mapIndexed { index, text ->
            PrintText(
                text = text,
                leftMm = textLeft,
                topMm = topMm + index * template.attachmentHeightPerItemMm,
                widthMm = textWidth,
                heightMm = template.attachmentHeightPerItemMm,
                fontMm = 2.8,
                maxLines = ATTACHMENT_LINES,
            )
        }
        return NotesDisplayLayout(
            xMm = contentLeft,
            topMm = topMm,
            widthMm = contentWidth,
            heightMm = template.notesDisplayHeightMm(attachmentCount),
            texts = texts,
        )
    }

    private fun noteAreaLayout(
        topMm: Double,
        template: TemplateGeometry,
        contentLeft: Double,
    ): NoteAreaLayout {
        val writingWidth = template.noteWritingWidthMm
        val textLeft = contentLeft + template.textPaddingMm
        val title = PrintText(
            text = "复习笔记",
            leftMm = textLeft,
            topMm = topMm,
            widthMm = writingWidth - 2 * template.textPaddingMm,
            heightMm = template.noteTitleHeightMm,
            fontMm = 3.2,
            bold = true,
        )
        return NoteAreaLayout(
            xMm = contentLeft,
            topMm = topMm,
            widthMm = writingWidth,
            heightMm = template.noteAreaBaseHeightMm,
            title = title,
            writingLines = writingLines(topMm, template, contentLeft, writingWidth),
        )
    }

    /**
     * 书写横线：位置与题间距共用 [TemplateGeometry] 的横线几何。
     *
     * 2026-11-25 用户要求题间距等于两条复习笔记横线之间的距离，并让 QR 组合
     * 与最后一条横线水平对齐；横线偏移与 QR 偏移都从模板派生，避免两处各算一套。
     */
    private fun writingLines(
        writingTop: Double,
        template: TemplateGeometry,
        writingLeft: Double,
        writingWidth: Double,
    ): List<PrintLine> = template.noteWritingLineOffsetsMm.map { offset ->
        PrintLine(
            leftMm = writingLeft + template.textPaddingMm,
            topMm = writingTop + offset,
            rightMm = writingLeft + writingWidth - template.textPaddingMm,
        )
    }

    /**
     * 表头单元格：列 x 与左侧填涂行完全共用 `TemplateGeometry.bubbleLeftMm` 与名称列偏移，
     * 因此文字始终落在实际列上，不依赖整行字符串的字体度量。
     */
    private fun buildHeaderCells(
        contentLeft: Double,
        firstItemTopMm: Double,
        template: TemplateGeometry,
    ): List<PrintText> {
        // 2026-11-22 用户要求：表头贴近第一道题，不再在表头带内垂直居中。
        // 表头文字底边贴住本页第一条知识点行的上沿，行内留白只存在于文字上方。
        val headerTextTop = firstItemTopMm - HEADER_TEXT_HEIGHT_MM
        val cells = mutableListOf(
            PrintText(
                text = "序号",
                leftMm = contentLeft,
                topMm = headerTextTop,
                // 表头字号 3.0mm 的「序号」约 6mm 宽；序号列收窄到 6mm 后，
                // 表头单独给 7mm 余量，避免 breakText 因浮点舍入截断成「序…」。
                widthMm = HEADER_INDEX_WIDTH_MM,
                heightMm = HEADER_TEXT_HEIGHT_MM,
                fontMm = 3.0,
                align = PrintTextAlign.Left,
            ),
            PrintText(
                text = "知识点",
                leftMm = knowledgeNameLeftMm(contentLeft, template),
                topMm = headerTextTop,
                widthMm = template.knowledgeColumnWidthMm,
                heightMm = HEADER_TEXT_HEIGHT_MM,
                fontMm = 3.0,
            ),
        )
        HEADER_TIER_LABELS.forEachIndexed { index, label ->
            // 档位文字比填涂框宽（「较熟悉」约 9mm > 5.7mm）：以框中心居中绘制，
            // 单元格宽度单独给足以免被 breakText 截断；相邻文字靠加大的 bubbleGapMm 分开。
            cells += PrintText(
                text = label,
                leftMm = template.bubbleCenterXMm(index) - HEADER_TIER_LABEL_WIDTH_MM / 2,
                topMm = headerTextTop,
                widthMm = HEADER_TIER_LABEL_WIDTH_MM,
                heightMm = HEADER_TEXT_HEIGHT_MM,
                fontMm = 3.0,
                align = PrintTextAlign.Center,
            )
        }
        return cells
    }

    private fun bubbleLayouts(
        rowTop: Double,
        template: TemplateGeometry,
    ): List<BubbleLayout> {
        val bubbleTop = rowTop + (template.rowHeightMm - template.bubbleHeightMm) / 2
        return BUBBLE_LABELS.mapIndexed { index, label ->
            BubbleLayout(
                label = label,
                leftMm = template.bubbleLeftMm(index),
                topMm = bubbleTop,
                widthMm = template.bubbleWidthMm,
                heightMm = template.bubbleHeightMm,
            )
        }
    }

    private fun knowledgeNameLeftMm(contentLeft: Double, template: TemplateGeometry): Double =
        contentLeft + template.indexColumnWidthMm + template.columnGapMm

    private fun arucoMarkers(template: TemplateGeometry): List<ArucoMarkerLayout> {
        val width = template.pageWidthMm
        val height = template.pageHeightMm
        return listOf(
            aruco(0, ARUCO_INSET_MM, ARUCO_INSET_MM),
            aruco(1, width - ARUCO_INSET_MM - ARUCO_CODE_MM, ARUCO_INSET_MM),
            aruco(2, width - ARUCO_INSET_MM - ARUCO_CODE_MM, height - ARUCO_INSET_MM - ARUCO_CODE_MM),
            aruco(3, ARUCO_INSET_MM, height - ARUCO_INSET_MM - ARUCO_CODE_MM),
        )
    }

    private fun aruco(id: Int, left: Double, top: Double): ArucoMarkerLayout = ArucoMarkerLayout(
        id = id,
        code = PrintRect(
            leftMm = left,
            topMm = top,
            rightMm = left + ARUCO_CODE_MM,
            bottomMm = top + ARUCO_CODE_MM,
        ),
        quietZoneMm = ARUCO_QUIET_ZONE_MM,
    )

    /**
     * 右上第一行：`来源 第X页 第Y题`（2026-11-25 用户要求）。
     *
     * 与计划预览共用 [formatQuestionLabel]；X 为页码编号、Y 为题号，
     * 来源说明缺失时显示 `第X份试卷 第Y题`（2026-11-28 用户要求）。QR 载荷与页脚仍使用完整 pageId。
     */
    private fun pageLineText(question: RenderableQuestion): String =
        formatQuestionLabel(question.sourcePage, question.questionNumber)
}

/** C-013：左侧名称超过 10 个 Unicode 码点时截断为前 9 个码点 + “…”；右上保留全称。 */
internal fun truncateForLeftArea(name: String): String {
    val codePoints = name.codePoints().toArray()
    if (codePoints.size <= MAX_LEFT_NAME_CODE_POINTS) return name
    return String(codePoints, 0, MAX_LEFT_NAME_CODE_POINTS - 1) + "…"
}

private fun formatDate(date: LocalDate): String =
    "$date ${WEEKDAY_NAMES[date.dayOfWeek.value - 1]}"

private const val NAME_SEPARATOR = " / "
private const val MAX_LEFT_NAME_CODE_POINTS = 10
private const val HEADER_INDEX_WIDTH_MM = 7.0 // 表头「序号」的显示余量；行号列宽见 TemplateGeometry
private const val HEADER_TIER_LABEL_WIDTH_MM = 10.0 // 「较熟悉」约 9mm，留 1mm 余量
private const val HEADER_TEXT_HEIGHT_MM = 3.5 // 3.0mm 表头字的视觉行高；底边贴住第一条知识点行

// 2026-11-25 用户要求：每日题序徽标「【n】」置于左侧「序号」列的左边（题块最左缘）。
// 宽度按两位数字预留（全角括号约 1em、数字约 0.5em）；徽标落在 12–26mm 的页边空白带内。
private const val QUESTION_INDEX_WIDTH_MM = 12.0
private const val QUESTION_INDEX_GAP_MM = 1.0
private const val QUESTION_INDEX_FONT_MM = 3.8

private const val ATTACHMENT_LINES = 2
private const val QR_QUIET_ZONE_MODULES = 4
private const val PAGE_ID_LABEL_FONT_MM = 2.2
private const val ARUCO_CODE_MM = 10.0
private const val ARUCO_QUIET_ZONE_MM = 4.0
private const val ARUCO_INSET_MM = 12.0

private val BUBBLE_LABELS = listOf("A", "B", "C")
private val HEADER_TIER_LABELS = listOf("掌握", "较熟悉", "不会")
private val WEEKDAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
