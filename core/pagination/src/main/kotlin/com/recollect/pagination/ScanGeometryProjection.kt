package com.recollect.pagination

/**
 * 回读所需的题块与笔记区几何（scan-pipeline.md「OMR 采样几何与阈值」「QR 与题块绑定」）。
 *
 * 打印布局与计划持久化共用本投影，避免回读端另算一套公式（C-093 方案 A）。
 * 所有坐标都是页面毫米，原点在纸张左上角。
 */
data class NoteWritingArea(
    val leftMm: Double,
    val topMm: Double,
    val widthMm: Double,
    val heightMm: Double,
)

data class ScanItemGeometry(
    val questionId: String,
    val topMm: Double, // 题块纵向起点（即 PlacedItem.yOffsetMm）
    val heightMm: Double,
    val rowCount: Int,
    val noteArea: NoteWritingArea?, // `hasNoteArea = false` 时为 null
)

object ScanGeometryProjection {

    /** 按打印布局规则投影一个题块；`question` 必须是本页打印时使用的同一投影。 */
    fun of(
        item: PlacedItem,
        question: RenderableQuestion,
        template: TemplateGeometry,
    ): ScanItemGeometry = ScanItemGeometry(
        questionId = item.questionId,
        topMm = item.yOffsetMm,
        heightMm = item.heightMm,
        rowCount = question.knowledgeCount,
        noteArea = if (question.hasNoteArea) noteWritingArea(item, question, template) else null,
    )

    /**
     * 手写书写区：题块顶 + 右上信息高度 + 附加摘要高度 + 标题高度起，
     * 高为笔记区基础高度扣除标题后的部分；横向取右上文本区。
     */
    fun noteWritingArea(
        item: PlacedItem,
        question: RenderableQuestion,
        template: TemplateGeometry,
    ): NoteWritingArea {
        val knowledgeLineCount = question.knowledgeLineCount.coerceAtLeast(1)
        val noteTop = item.yOffsetMm +
            template.questionFixedInfoHeightMm +
            knowledgeLineCount * template.rowHeightMm
        val attachmentHeight = question.attachmentCount * template.attachmentHeightPerItemMm
        return NoteWritingArea(
            leftMm = template.contentLeftMm + template.leftAreaWidthMm + template.textPaddingMm,
            topMm = noteTop + attachmentHeight + template.noteTitleHeightMm,
            widthMm = template.rightTextWidthMm,
            heightMm = template.noteAreaBaseHeightMm - template.noteTitleHeightMm,
        )
    }
}
