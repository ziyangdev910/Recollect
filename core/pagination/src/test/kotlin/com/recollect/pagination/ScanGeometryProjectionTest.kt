package com.recollect.pagination

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * 回读几何投影（C-093 方案 A）：题块位置取自分页结果，笔记书写区按打印布局规则推导。
 */
class ScanGeometryProjectionTest {

    private val template = TemplateGeometry.a4Print()

    @Test
    fun `projects block bounds and writing area from the print layout`() {
        val item = PlacedItem("q1", yOffsetMm = 41.0, heightMm = 30.0)
        val question = RenderableQuestion(
            questionId = "q1",
            questionNumber = "T-1",
            knowledgeCount = 1,
            knowledgeNames = listOf("一元二次方程"),
            knowledgeLineCount = 2,
            attachmentCount = 0,
            hasNoteArea = true,
        )

        val geometry = ScanGeometryProjection.of(item, question, template)

        assertEquals(41.0, geometry.topMm)
        assertEquals(30.0, geometry.heightMm)
        assertEquals(1, geometry.rowCount)
        val note = requireNotNull(geometry.noteArea)
        // 2026-11-24 新排版：题块顶 41 + 顶部信息段 max(1×7.5, 7+2×7.5)=22 = 63；再加标题 5mm。
        assertEquals(68.0, note.topMm)
        assertEquals(template.noteAreaBaseHeightMm - template.noteTitleHeightMm, note.heightMm)
        // 书写区通栏起于内容区左边，右侧让出 QR 列与间距。
        assertEquals(template.contentLeftMm + template.textPaddingMm, note.leftMm)
        assertEquals(template.noteWritingWidthMm, note.widthMm)
        assertEquals(template.contentWidthMm - template.qrSizeMm - template.noteQrColumnGapMm, note.widthMm)
    }

    @Test
    fun `adds attachment height above the writing area`() {
        val item = PlacedItem("q1", yOffsetMm = 41.0, heightMm = 40.0)
        val question = RenderableQuestion(
            questionId = "q1",
            questionNumber = "T-1",
            knowledgeCount = 1,
            knowledgeNames = listOf("一元二次方程"),
            knowledgeLineCount = 1,
            attachmentCount = 3,
            hasNoteArea = true,
        )

        val note = requireNotNull(ScanGeometryProjection.of(item, question, template).noteArea)

        // 41 + max(1×7.5, 7+1×7.5)=14.5 = 55.5；3 条附加摘要 ×10 = 30；再加标题 5。
        assertEquals(90.5, note.topMm)
    }

    @Test
    fun `omits the note area when the page has none`() {
        val item = PlacedItem("q1", yOffsetMm = 41.0, heightMm = 30.0)
        val question = RenderableQuestion("q1", "T-1", 1, listOf("x"), 1, 0, hasNoteArea = false)

        assertNull(ScanGeometryProjection.of(item, question, template).noteArea)
    }
}
