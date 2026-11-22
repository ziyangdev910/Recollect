package com.recollect.feature.print

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
 * - 日期大标题与「本日共需复习 X 题」绘制在上边距内，不占内容区（C-063）；
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
    val dailyTotal: PrintText?,
    val legend: PrintText?,
    /**
     * 表头「序号 / 知识点 / A / B / C」按列拆开：知识点列与左侧名称文本左对齐，
     * A/B/C 分别对齐每行三个填涂格，避免整行字符串的字体宽度造成错位。
     */
    val headerCells: List<PrintText>,
    val items: List<PrintItemLayout>,
    val arucoMarkers: List<ArucoMarkerLayout>,
)

data class PrintItemLayout(
    val questionId: String,
    val blockTopMm: Double,
    val blockHeightMm: Double,
    val left: LeftAreaLayout,
    val rightTop: RightTopLayout,
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
    val sizeMm: Double,
)

data class RightTopLayout(
    val xMm: Double,
    val topMm: Double,
    val widthMm: Double,
    val heightMm: Double,
    val pageLine: PrintText, // 页码 + 题号，加粗
    val knowledgeText: PrintText, // 知识点全称，允许换行
)

data class NoteAreaLayout(
    val xMm: Double,
    val topMm: Double,
    val widthMm: Double,
    val heightMm: Double,
    val attachmentHeightMm: Double,
    val writingAreaHeightMm: Double,
    val title: PrintText,
    val attachments: List<PrintText>,
    val writingLines: List<PrintLine>,
)

data class QrReservationLayout(
    val leftMm: Double,
    val topMm: Double,
    val sizeMm: Double,
    val footerTopMm: Double,
    val footerHeightMm: Double,
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
    /** 水平对齐；默认左对齐，表头的序号与 A/B/C 在各自列宽内居中。 */
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
        val firstPageNumberByDate = pages
            .groupBy { it.date }
            .mapValues { (_, datePages) -> datePages.minOf { it.pageNumber } }
        val itemCountByDate = pages
            .groupBy { it.date }
            .mapValues { (_, datePages) -> datePages.sumOf { it.items.size } }

        return PrintLayout(
            planId = planId,
            pages = pages.map { page ->
                pageLayout(
                    planId = planId,
                    page = page,
                    questions = questions,
                    noteTexts = noteTexts,
                    template = template,
                    firstPageNumber = firstPageNumberByDate.getValue(page.date),
                    itemCount = itemCountByDate.getValue(page.date),
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
        firstPageNumber: Int,
        itemCount: Int,
    ): PrintPageLayout {
        val contentLeft = template.contentLeftMm
        val contentTop = template.contentTopMm +
            if (page.pageNumber == 1) template.legendHeightMm else 0.0
        val contentWidth = template.contentWidthMm
        val leftAreaWidth = template.leftAreaWidthMm
        val rightAreaLeft = contentLeft + leftAreaWidth
        val rightAreaWidth = template.rightAreaWidthMm

        val firstPageOfDate = page.pageNumber == firstPageNumber
        // 日期标题块绘制在内容区上边界之前的预留带内，不占用内容区高度（C-063/C-091）。
        val headingBlockTop = template.contentTopMm - template.dateHeadingBlockHeightMm
        // 每个日期第一页绘制日期大标题；同日后续页只重复日期小标题，不重复总题数（print-template.md）。
        val dateHeading = PrintText(
            text = formatDate(page.date),
            leftMm = contentLeft,
            topMm = headingBlockTop,
            widthMm = contentWidth,
            heightMm = template.dateHeadingHeightMm,
            fontMm = template.dateHeadingFontMm,
            bold = true,
        )
        val dailyTotal = if (firstPageOfDate) {
            PrintText(
                text = "本日共需复习 $itemCount 题",
                leftMm = contentLeft,
                topMm = headingBlockTop + template.dateHeadingHeightMm,
                widthMm = contentWidth,
                heightMm = template.dailyTotalHeightMm,
                fontMm = template.dailyTotalFontMm,
            )
        } else {
            null
        }

        val legend = if (page.pageNumber == 1) {
            PrintText(
                text = LEGEND_TEXT,
                leftMm = contentLeft,
                topMm = template.contentTopMm,
                widthMm = contentWidth,
                heightMm = template.legendHeightMm,
                fontMm = TemplateGeometry.ANNOTATION_FONT_MM,
            )
        } else {
            null
        }

        // 表头与左侧填涂行共用列几何：知识点左对齐名称列，A/B/C 对齐三格。
        val headerCells = buildHeaderCells(
            contentLeft = contentLeft,
            topMm = contentTop,
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
            dailyTotal = dailyTotal,
            legend = legend,
            headerCells = headerCells,
            items = page.items.map { placed ->
                val question = requireNotNull(questions[placed.questionId]) {
                    "打印输入缺少题目 ${placed.questionId}；计划层必须在分页前补齐投影（C-067）"
                }
                itemLayout(
                    planId = planId,
                    pageId = page.pageId,
                    pageNumber = page.pageNumber,
                    placedQuestionId = placed.questionId,
                    placedTop = placed.yOffsetMm,
                    placedHeight = placed.heightMm,
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
        pageNumber: Int,
        placedQuestionId: String,
        placedTop: Double,
        placedHeight: Double,
        question: RenderableQuestion,
        noteTexts: List<String>,
        template: TemplateGeometry,
        contentLeft: Double,
        contentWidth: Double,
        leftAreaWidth: Double,
        rightAreaLeft: Double,
        rightAreaWidth: Double,
    ): PrintItemLayout {
        val visibleBottom = placedTop + placedHeight - template.itemSpacingMm
        val footerTop = visibleBottom - template.qrFooterHeightMm
        val qrLeft = contentLeft + contentWidth - template.qrSizeMm
        val qrTop = footerTop + template.qrPaddingMm

        val rowCount = question.knowledgeCount
        val leftRows = (0 until rowCount).map { index ->
            val rowTop = placedTop + index * template.rowHeightMm
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
                    widthMm = INDEX_COLUMN_WIDTH_MM,
                    heightMm = template.rowHeightMm,
                    fontMm = 3.2,
                ),
                nameText = PrintText(
                    text = displayName,
                    leftMm = knowledgeNameLeftMm(contentLeft),
                    topMm = rowTop,
                    widthMm = NAME_COLUMN_WIDTH_MM,
                    heightMm = template.rowHeightMm,
                    fontMm = 3.2,
                ),
                bubbles = bubbleLayouts(rowTop, contentLeft, template),
            )
        }

        val knowledgeLineCount = question.knowledgeLineCount.coerceAtLeast(1)
        val rightTopHeight = template.questionFixedInfoHeightMm +
            knowledgeLineCount * template.rowHeightMm
        val textLeft = rightAreaLeft + template.textPaddingMm
        val rightTop = RightTopLayout(
            xMm = rightAreaLeft,
            topMm = placedTop,
            widthMm = rightAreaWidth,
            heightMm = rightTopHeight,
            pageLine = PrintText(
                text = pageLineText(pageNumber, question),
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

        val note = if (question.hasNoteArea) {
            noteAreaLayout(
                attachmentCount = question.attachmentCount,
                noteTexts = noteTexts,
                topMm = placedTop + rightTopHeight,
                template = template,
                rightAreaLeft = rightAreaLeft,
                rightAreaWidth = rightAreaWidth,
            )
        } else {
            // C-062：纸上没有笔记区；附件数量不参与绘制，也不在这里静默清零上游数据。
            null
        }

        return PrintItemLayout(
            questionId = placedQuestionId,
            blockTopMm = placedTop,
            blockHeightMm = placedHeight,
            left = LeftAreaLayout(
                xMm = contentLeft,
                widthMm = leftAreaWidth,
                rows = leftRows,
            ),
            rightTop = rightTop,
            note = note,
            qr = QrReservationLayout(
                leftMm = qrLeft,
                topMm = qrTop,
                sizeMm = template.qrSizeMm,
                footerTopMm = footerTop,
                footerHeightMm = template.qrFooterHeightMm,
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

    private fun noteAreaLayout(
        attachmentCount: Int,
        noteTexts: List<String>,
        topMm: Double,
        template: TemplateGeometry,
        rightAreaLeft: Double,
        rightAreaWidth: Double,
    ): NoteAreaLayout {
        val attachmentHeight = attachmentCountHeight(attachmentCount, template)
        val noteHeight = template.noteAreaBaseHeightMm + attachmentHeight
        val textLeft = rightAreaLeft + template.textPaddingMm
        val attachments = noteTexts.mapIndexed { index, text ->
            PrintText(
                text = text,
                leftMm = textLeft,
                topMm = topMm + index * template.attachmentHeightPerItemMm,
                widthMm = template.rightTextWidthMm,
                heightMm = template.attachmentHeightPerItemMm,
                fontMm = 2.8,
                maxLines = ATTACHMENT_LINES,
            )
        }
        val writingTop = topMm + attachmentHeight
        val title = PrintText(
            text = "复习笔记",
            leftMm = textLeft,
            topMm = writingTop,
            widthMm = template.rightTextWidthMm,
            heightMm = template.noteTitleHeightMm,
            fontMm = 3.2,
            bold = true,
        )
        return NoteAreaLayout(
            xMm = rightAreaLeft,
            topMm = topMm,
            widthMm = rightAreaWidth,
            heightMm = noteHeight,
            attachmentHeightMm = attachmentHeight,
            writingAreaHeightMm = template.noteAreaBaseHeightMm,
            title = title,
            attachments = attachments,
            writingLines = writingLines(writingTop, template, rightAreaLeft, rightAreaWidth),
        )
    }

    private fun attachmentCountHeight(count: Int, template: TemplateGeometry): Double =
        count * template.attachmentHeightPerItemMm

    private fun writingLines(
        writingTop: Double,
        template: TemplateGeometry,
        rightAreaLeft: Double,
        rightAreaWidth: Double,
    ): List<PrintLine> {
        val linesTop = writingTop + template.noteTitleHeightMm
        val available = template.noteAreaBaseHeightMm - template.noteTitleHeightMm
        val step = available / (WRITING_LINE_COUNT + 1)
        return (1..WRITING_LINE_COUNT).map { index ->
            PrintLine(
                leftMm = rightAreaLeft + template.textPaddingMm,
                topMm = linesTop + index * step,
                rightMm = rightAreaLeft + rightAreaWidth - template.textPaddingMm,
            )
        }
    }

    /**
     * 表头单元格：列 x 与左侧填涂行完全共用 [bubbleLeftMm] 与名称列偏移，
     * 因此文字始终落在实际列上，不依赖整行字符串的字体度量。
     */
    private fun buildHeaderCells(
        contentLeft: Double,
        topMm: Double,
        template: TemplateGeometry,
    ): List<PrintText> {
        val cells = mutableListOf(
            PrintText(
                text = "序号",
                leftMm = contentLeft,
                topMm = topMm,
                widthMm = INDEX_COLUMN_WIDTH_MM,
                heightMm = template.headerRowHeightMm,
                fontMm = 3.0,
                align = PrintTextAlign.Left,
            ),
            PrintText(
                text = "知识点",
                leftMm = knowledgeNameLeftMm(contentLeft),
                topMm = topMm,
                widthMm = NAME_COLUMN_WIDTH_MM,
                heightMm = template.headerRowHeightMm,
                fontMm = 3.0,
            ),
        )
        BUBBLE_LABELS.forEachIndexed { index, label ->
            cells += PrintText(
                text = label,
                leftMm = bubbleLeftMm(index, contentLeft),
                topMm = topMm,
                widthMm = BUBBLE_SIZE_MM,
                heightMm = template.headerRowHeightMm,
                fontMm = 3.0,
                align = PrintTextAlign.Center,
            )
        }
        return cells
    }

    private fun bubbleLayouts(
        rowTop: Double,
        contentLeft: Double,
        template: TemplateGeometry,
    ): List<BubbleLayout> {
        val bubbleTop = rowTop + (template.rowHeightMm - BUBBLE_SIZE_MM) / 2
        return BUBBLE_LABELS.mapIndexed { index, label ->
            BubbleLayout(
                label = label,
                leftMm = bubbleLeftMm(index, contentLeft),
                topMm = bubbleTop,
                sizeMm = BUBBLE_SIZE_MM,
            )
        }
    }

    private fun knowledgeNameLeftMm(contentLeft: Double): Double =
        contentLeft + INDEX_COLUMN_WIDTH_MM + COLUMN_GAP_MM

    private fun bubbleLeftMm(index: Int, contentLeft: Double): Double =
        knowledgeNameLeftMm(contentLeft) +
            NAME_COLUMN_WIDTH_MM + COLUMN_GAP_MM +
            index * (BUBBLE_SIZE_MM + BUBBLE_GAP_MM)

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
     * 右上第一行：「页码」只显示页序，不显示计划号；QR 载荷仍使用完整 pageId。
     * 来源页码非空时追加，用于定位题目在试卷/作业本的哪一面。
     */
    private fun pageLineText(pageNumber: Int, question: RenderableQuestion): String {
        val page = "页码 ${pageNumber.toString().padStart(2, '0')}　题号 ${question.questionNumber}"
        return if (question.sourcePage.isBlank()) page else "$page　来源页 ${question.sourcePage}"
    }
}

/** C-013：左侧名称超过 10 个 Unicode 码点时截断为前 9 个码点 + “…”；右上保留全称。 */
internal fun truncateForLeftArea(name: String): String {
    val codePoints = name.codePoints().toArray()
    if (codePoints.size <= MAX_LEFT_NAME_CODE_POINTS) return name
    return String(codePoints, 0, MAX_LEFT_NAME_CODE_POINTS - 1) + "…"
}

private fun formatDate(date: LocalDate): String =
    "$date ${WEEKDAY_NAMES[date.dayOfWeek.value - 1]}"

private const val LEGEND_TEXT = "A—掌握　B—较熟悉　C—不会"
private const val NAME_SEPARATOR = " / "
private const val MAX_LEFT_NAME_CODE_POINTS = 10
private const val INDEX_COLUMN_WIDTH_MM = 8.0 // scan-pipeline.md：序号列 8mm
private const val NAME_COLUMN_WIDTH_MM = 34.0 // scan-pipeline.md：知识点列 34mm
private const val COLUMN_GAP_MM = 2.0 // scan-pipeline.md：列间距 2mm
private const val BUBBLE_SIZE_MM = 3.5 // print-template.md：3.5–4mm
private const val BUBBLE_GAP_MM = 2.0 // scan-pipeline.md：框间距 2mm
private const val WRITING_LINE_COUNT = 3
private const val ATTACHMENT_LINES = 2
private const val QR_QUIET_ZONE_MODULES = 4
private const val PAGE_ID_LABEL_FONT_MM = 2.2
private const val ARUCO_CODE_MM = 10.0
private const val ARUCO_QUIET_ZONE_MM = 4.0
private const val ARUCO_INSET_MM = 12.0

private val BUBBLE_LABELS = listOf("A", "B", "C")
private val WEEKDAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
