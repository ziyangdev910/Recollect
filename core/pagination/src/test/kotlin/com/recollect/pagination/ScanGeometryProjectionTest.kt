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
        // 题块顶 41 + 固定信息 7 + 2 行 × 7.5 = 63；再加标题 5mm。
        assertEquals(68.0, note.topMm)
        assertEquals(template.noteAreaBaseHeightMm - template.noteTitleHeightMm, note.heightMm)
        assertEquals(template.contentLeftMm + template.leftAreaWidthMm + template.textPaddingMm, note.leftMm)
        assertEquals(template.rightTextWidthMm, note.widthMm)
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

        // 41 + 7 + 7.5 = 55.5；3 条附加摘要 ×10 = 30；再加标题 5。
        assertEquals(90.5, note.topMm)
    }

    @Test
    fun `omits the note area when the page has none`() {
        val item = PlacedItem("q1", yOffsetMm = 41.0, heightMm = 30.0)
        val question = RenderableQuestion("q1", "T-1", 1, listOf("x"), 1, 0, hasNoteArea = false)

        assertNull(ScanGeometryProjection.of(item, question, template).noteArea)
    }
}
