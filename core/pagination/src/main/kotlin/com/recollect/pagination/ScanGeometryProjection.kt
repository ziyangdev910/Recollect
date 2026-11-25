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
     * 手写书写区（2026-11-24 新排版）：题块顶部信息段（左填涂/右信息取高者）
     * + 通栏附加笔记显示区 + 「复习笔记」标题起；书写区只占左侧，
     * 右侧 [TemplateGeometry.qrSizeMm] 宽的 QR 列不参与 OCR。
     */
    fun noteWritingArea(
        item: PlacedItem,
        question: RenderableQuestion,
        template: TemplateGeometry,
    ): NoteWritingArea {
        val topSection = template.topSectionHeightMm(
            knowledgeCount = question.knowledgeCount,
            knowledgeLineCount = question.knowledgeLineCount,
        )
        val attachmentHeight = template.notesDisplayHeightMm(question.attachmentCount)
        return NoteWritingArea(
            leftMm = template.contentLeftMm + template.textPaddingMm,
            topMm = item.yOffsetMm + topSection + attachmentHeight + template.noteTitleHeightMm,
            widthMm = template.noteWritingWidthMm,
            heightMm = template.noteAreaBaseHeightMm - template.noteTitleHeightMm,
        )
    }
}
