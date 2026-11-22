package com.recollect.feature.print

import com.recollect.pagination.DefaultPagePlanner
import com.recollect.pagination.PaginationResult
import com.recollect.pagination.RenderableQuestion
import com.recollect.pagination.TemplateGeometry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * 打印布局（print-template.md、pagination.md）。
 *
 * PR-01：只消费 `PlannedPage`/`PlacedItem` 映射生成 A4 页面布局；
 * 日期大标题 +「本日共需复习 X 题」只出现在每个日期的第一页，绘制在上边距内、不占内容区；
 * 不绘制“续下页”；`hasNoteArea=false` 不绘制笔记区与附件；
 * `hasNoteArea=true` 时 3 条附件完整绘制；四角 ArUco 与题块底部 QR 预留区按模板几何。
 */
class PrintLayoutTest {

    // C-091 方案 A：生产模板为四角 ArUco 预留 14mm 角区。
    private val template = TemplateGeometry.a4Print()

    @Test
    fun `PR-01 按计划映射生成 A4 打印布局`() {
        val monday = LocalDate.of(2026, 11, 16)
        val tuesday = LocalDate.of(2026, 11, 17)
        val questions = listOf(
            RenderableQuestion(
                questionId = "q1",
                questionNumber = "T-1",
                knowledgeCount = 1,
                knowledgeNames = listOf("一元二次方程"),
                knowledgeLineCount = 1,
                attachmentCount = 0,
                hasNoteArea = true,
                sourcePage = "P12",
            ),
            RenderableQuestion(
                questionId = "q2",
                questionNumber = "T-2",
                knowledgeCount = 2,
                knowledgeNames = listOf("勾股定理", "相似三角形"),
                knowledgeLineCount = 2,
                attachmentCount = 3,
                hasNoteArea = false,
            ),
            RenderableQuestion(
                questionId = "q3",
                questionNumber = "T-3",
                knowledgeCount = 1,
                knowledgeNames = listOf("一个非常长的知识点名称用于验证左侧截断规则"),
                knowledgeLineCount = 2,
                attachmentCount = 3,
                hasNoteArea = true,
            ),
            RenderableQuestion(
                questionId = "q4",
                questionNumber = "T-4",
                knowledgeCount = 1,
                knowledgeNames = listOf("相似三角形"),
                knowledgeLineCount = 1,
                attachmentCount = 0,
                hasNoteArea = true,
                sourcePage = "P7",
            ),
            RenderableQuestion(
                questionId = "q5",
                questionNumber = "T-5",
                knowledgeCount = 1,
                knowledgeNames = listOf("一元一次方程"),
                knowledgeLineCount = 1,
                attachmentCount = 0,
                hasNoteArea = true,
            ),
        )
        val pagination = DefaultPagePlanner().plan(
            planId = "K7M2Q9",
            dailyGroups = linkedMapOf(
                // 周一 4 题：前 3 题在首页，第 4 题整条移到同日第 2 页（C-063 同日后续页）。
                monday to questions.take(4),
                tuesday to listOf(questions[4]),
            ),
            template = template,
        ) as PaginationResult.Success

        val noteTexts = mapOf(
            "q2" to listOf("附件笔记 1", "附件笔记 2", "附件笔记 3"),
            "q3" to listOf("附件 A", "附件 B", "附件 C"),
        )

        val layout = PrintLayoutBuilder.build(
            planId = "K7M2Q9",
            pages = pagination.pages,
            questions = questions.associateBy { it.questionId },
            noteTexts = noteTexts,
            template = template,
        )

        assertEquals(3, layout.pages.size)
        assertEquals(
            pagination.pages.map { page -> page.items.map { it.questionId } },
            layout.pages.map { page -> page.items.map { it.questionId } },
        )

        val first = layout.pages[0]
        assertEquals("K7M2Q9-01", first.pageId)
        assertEquals(210.0, first.pageWidthMm)
        assertEquals(297.0, first.pageHeightMm)
        // C-091 方案 A：内容区从 26mm 开始，避开 ArUco 内码 10mm + 白边 4mm。
        assertEquals(26.0, first.contentLeftMm, EPSILON)
        assertTrue(first.contentLeftMm >= 12.0 + 10.0 + 4.0)
        assertEquals(34.0, first.contentTopMm, EPSILON) // 预留 26 + 首页图例 8
        assertEquals(158.0, first.contentWidthMm, EPSILON)
        assertEquals(63.2, first.leftAreaWidthMm, EPSILON)
        assertEquals(89.2, first.rightAreaLeftMm, EPSILON)
        assertEquals(94.8, first.rightAreaWidthMm, EPSILON)
        assertEquals(88.8, first.rightTextWidthMm, EPSILON)

        // C-063：日期大标题 + 每日总题数绘制在上边距内，不占用内容区。
        val heading = first.dateHeading
        assertNotNull(heading)
        assertEquals("2026-11-16 周一", heading!!.text)
        assertEquals(5.0, heading.fontMm)
        assertEquals(7.0, heading.heightMm)
        assertEquals(15.5, heading.topMm, EPSILON) // 内容区上边界 26 - 标题块 10.5
        val dailyTotal = first.dailyTotal
        assertNotNull(dailyTotal)
        assertEquals("本日共需复习 4 题", dailyTotal!!.text)
        // 2026-11-18：字号与首页图例一致；高度与 top 不变，保持与日期大标题的间距。
        assertEquals(3.2, dailyTotal.fontMm)
        assertEquals(3.5, dailyTotal.heightMm)
        assertEquals(22.5, dailyTotal.topMm, EPSILON)
        assertTrue(dailyTotal.topMm + dailyTotal.heightMm <= template.contentTopMm)
        assertEquals(10.5, template.dateHeadingBlockHeightMm, EPSILON)

        // 图例仅文档首页一次。
        assertEquals("A—掌握　B—较熟悉　C—不会", first.legend?.text)
        assertEquals(26.0, first.legend!!.topMm, EPSILON)
        assertEquals(8.0, first.legend!!.heightMm, EPSILON)
        assertEquals(first.legend!!.fontMm, dailyTotal.fontMm, EPSILON)

        // 表头按列拆开，列 x 与左侧填涂行共用几何：知识点对齐名称列，A/B/C 对齐三格。
        val headerCells = first.headerCells
        assertEquals(listOf("序号", "知识点", "A", "B", "C"), headerCells.map { it.text })
        headerCells.forEach { cell ->
            assertEquals(34.0, cell.topMm, EPSILON)
            assertEquals(7.0, cell.heightMm, EPSILON)
        }
        assertEquals(26.0, headerCells[0].leftMm, EPSILON)
        assertEquals(8.0, headerCells[0].widthMm, EPSILON)
        assertEquals(PrintTextAlign.Left, headerCells[0].align)
        assertEquals(36.0, headerCells[1].leftMm, EPSILON)
        assertEquals(34.0, headerCells[1].widthMm, EPSILON)
        assertEquals(PrintTextAlign.Left, headerCells[1].align)
        assertEquals(72.0, headerCells[2].leftMm, EPSILON)
        assertEquals(77.5, headerCells[3].leftMm, EPSILON)
        assertEquals(83.0, headerCells[4].leftMm, EPSILON)
        headerCells.drop(2).forEach { cell ->
            assertEquals(3.5, cell.widthMm, EPSILON)
            assertEquals(PrintTextAlign.Center, cell.align)
        }

        // 不绘制“续下页”，也不把警告内部代码写进纸面（C-063/C-065）。
        val allTexts = layout.pages.flatMap { it.allTexts() }
        assertFalse(allTexts.any { it.contains("续下页") })
        assertFalse(allTexts.any { it.contains("ITEM_TOO_TALL") })
        assertFalse(allTexts.any { it.contains("ATTACHMENT_OVER_LIMIT") })
        assertFalse(allTexts.any { it.contains("KNOWLEDGE_TEXT_OVERFLOW") })

        // 条目位置与高度必须原样取自 PlacedItem，不在渲染层重算或补占位行（R-02/C-060）。
        val placed = pagination.pages.flatMap { it.items }
        val laidOut = layout.pages.flatMap { it.items }
        assertEquals(placed.size, laidOut.size)
        placed.zip(laidOut).forEach { (source, target) ->
            assertEquals(source.questionId, target.questionId)
            assertEquals(source.yOffsetMm, target.blockTopMm, EPSILON)
            assertEquals(source.heightMm, target.blockHeightMm, EPSILON)
        }

        val q1 = first.items[0]
        assertEquals(41.0, q1.blockTopMm, EPSILON) // 26 + 8 + 7
        assertEquals(74.5, q1.blockHeightMm, EPSILON)
        assertEquals(1, q1.left.rows.size)
        assertEquals(1, q1.left.rows[0].index)
        assertEquals("一元二次方程", q1.left.rows[0].knowledgeName)
        assertEquals(41.0, q1.left.rows[0].topMm, EPSILON)
        assertEquals(7.5, q1.left.rows[0].heightMm, EPSILON)
        assertEquals(listOf("A", "B", "C"), q1.left.rows[0].bubbles.map { it.label })
        assertEquals(72.0, q1.left.rows[0].bubbles[0].leftMm, EPSILON)
        assertEquals(77.5, q1.left.rows[0].bubbles[1].leftMm, EPSILON)
        assertEquals(83.0, q1.left.rows[0].bubbles[2].leftMm, EPSILON)
        assertEquals(3.5, q1.left.rows[0].bubbles[0].sizeMm, EPSILON)
        assertEquals(43.0, q1.left.rows[0].bubbles[0].topMm, EPSILON)

        // 表头与真实行严格对齐（本用例锁定错位回归）：序号/知识点左对齐，A/B/C 与格子同列。
        assertEquals(q1.left.rows[0].indexText.leftMm, headerCells[0].leftMm, EPSILON)
        assertEquals(q1.left.rows[0].nameText.leftMm, headerCells[1].leftMm, EPSILON)
        headerCells.drop(2).zip(q1.left.rows[0].bubbles).forEach { (cell, bubble) ->
            assertEquals(bubble.leftMm, cell.leftMm, EPSILON)
        }

        assertEquals(89.2, q1.rightTop.xMm, EPSILON)
        assertEquals(41.0, q1.rightTop.topMm, EPSILON)
        assertEquals(7.0 + 1 * 7.5, q1.rightTop.heightMm, EPSILON)
        // 页码只显示页序（不显示计划号），并展示来源页码。
        assertEquals("页码 01　题号 T-1　来源页 P12", q1.rightTop.pageLine.text)
        assertTrue(q1.rightTop.pageLine.bold)
        assertEquals("一元二次方程", q1.rightTop.knowledgeText.text)
        assertEquals(1, q1.rightTop.knowledgeText.maxLines)

        assertEquals(55.5, q1.note?.topMm!!, EPSILON)
        assertEquals(30.0, q1.note?.heightMm!!, EPSILON)
        assertEquals(30.0, q1.note?.writingAreaHeightMm!!, EPSILON)
        assertEquals(0.0, q1.note?.attachmentHeightMm!!, EPSILON)
        assertTrue(q1.note?.attachments!!.isEmpty())
        assertEquals(3, q1.note?.writingLines!!.size)

        assertEquals(18.0, q1.qr.sizeMm, EPSILON)
        assertEquals(166.0, q1.qr.leftMm, EPSILON) // 内容区右边 184 - 18
        assertEquals(88.5, q1.qr.topMm, EPSILON) // footer 85.5 + qrPadding 3
        assertEquals(27.0, q1.qr.footerHeightMm, EPSILON)
        assertEquals(4, q1.qr.quietZoneModules)

        // C-095 方案 B：完整 pageId 位于 QR 下方，间距为标签高度的一半（2mm），并与 QR 右缘对齐。
        val label = q1.qr.pageIdLabel
        assertEquals("K7M2Q9-01", label.text)
        assertEquals(166.0, label.leftMm, EPSILON)
        assertEquals(108.5, label.topMm, EPSILON) // QR 底 106.5 + 间距 2
        assertEquals(18.0, label.widthMm, EPSILON)
        assertEquals(2.2, label.fontMm, EPSILON)
        assertEquals(PrintTextAlign.Right, label.align)
        assertEquals(q1.qr.leftMm + q1.qr.sizeMm, label.leftMm + label.widthMm, EPSILON)
        assertEquals(q1.qr.topMm + q1.qr.sizeMm + 2.0, label.topMm, EPSILON)

        // C-062：纸上没有笔记区时不绘制笔记与附件，且不截断附件区（P-15 已在分页层处理高度）。
        val q2 = first.items[1]
        assertEquals(115.5, q2.blockTopMm, EPSILON)
        assertEquals(52.0, q2.blockHeightMm, EPSILON) // max(2*7.5, 7+2*7.5) + 27 + 3
        assertEquals(2, q2.left.rows.size)
        assertEquals(115.5, q2.left.rows[0].topMm, EPSILON)
        assertEquals(123.0, q2.left.rows[1].topMm, EPSILON)
        assertEquals("勾股定理", q2.left.rows[0].knowledgeName)
        assertEquals("相似三角形", q2.left.rows[1].knowledgeName)
        assertEquals("勾股定理 / 相似三角形", q2.rightTop.knowledgeText.text)
        assertEquals(2, q2.rightTop.knowledgeText.maxLines)
        assertNull(q2.note)

        // 首页放不下 q3 后，同日后续页先放 q3，再放被移下页的 q4。
        val second = layout.pages[1]
        assertEquals("K7M2Q9-02", second.pageId)
        assertEquals(monday, second.date)
        assertEquals(26.0, second.contentTopMm, EPSILON)
        assertEquals(listOf("序号", "知识点", "A", "B", "C"), second.headerCells.map { it.text })
        assertEquals(26.0, second.headerCells.first().topMm, EPSILON)
        assertEquals("2026-11-16 周一", second.dateHeading?.text)
        assertNull(second.dailyTotal)
        assertNull(second.legend)

        // 左侧名称只在左侧截断为 9 个码点 + “…”；右上保留全称（C-013）。
        // C-062：hasNoteArea=true 时附件区按 3 条完整绘制，不截断到 30% 上限。
        val q3 = second.items[0]
        assertEquals("q3", q3.questionId)
        assertEquals(33.0, q3.blockTopMm, EPSILON)
        assertEquals(112.0, q3.blockHeightMm, EPSILON) // max(7.5, 7+2*7.5+30+30) + 27 + 3
        assertEquals("一个非常长的知识点…", q3.left.rows[0].knowledgeName)
        assertEquals(
            "一个非常长的知识点名称用于验证左侧截断规则",
            q3.rightTop.knowledgeText.text,
        )
        assertEquals(55.0, q3.note?.topMm!!, EPSILON)
        assertEquals(60.0, q3.note?.heightMm!!, EPSILON)
        assertEquals(30.0, q3.note?.attachmentHeightMm!!, EPSILON)
        assertEquals(30.0, q3.note?.writingAreaHeightMm!!, EPSILON)
        assertEquals(3, q3.note?.attachments?.size)
        assertEquals(listOf("附件 A", "附件 B", "附件 C"), q3.note?.attachments?.map { it.text })
        assertEquals(118.0, q3.qr.topMm, EPSILON)

        val q4 = second.items[1]
        assertEquals("q4", q4.questionId)
        assertEquals(145.0, q4.blockTopMm, EPSILON)
        assertEquals(74.5, q4.blockHeightMm, EPSILON)
        assertEquals("页码 02　题号 T-4　来源页 P7", q4.rightTop.pageLine.text)
        assertEquals(159.5, q4.note?.topMm!!, EPSILON)
        assertEquals(192.5, q4.qr.topMm, EPSILON)

        // 新日期重新开始：标题与每日题数恢复，图例仍不重复。
        val third = layout.pages[2]
        assertEquals(tuesday, third.date)
        assertEquals("2026-11-17 周二", third.dateHeading?.text)
        assertEquals("本日共需复习 1 题", third.dailyTotal?.text)
        assertNull(third.legend)
        assertEquals(33.0, third.items.single().blockTopMm, EPSILON)

        // 四角 ArUco：内码 10mm、距页边 12mm、白边 4mm（print-template.md）。
        assertEquals(4, first.arucoMarkers.size)
        assertEquals(listOf(0, 1, 2, 3), first.arucoMarkers.map { it.id })
        first.arucoMarkers.forEach { marker ->
            assertEquals(10.0, marker.code.widthMm, EPSILON)
            assertEquals(10.0, marker.code.heightMm, EPSILON)
            assertEquals(4.0, marker.quietZoneMm, EPSILON)
        }
        assertEquals(12.0, first.arucoMarkers[0].code.leftMm, EPSILON)
        assertEquals(12.0, first.arucoMarkers[0].code.topMm, EPSILON)
        assertEquals(188.0, first.arucoMarkers[1].code.leftMm, EPSILON)
        assertEquals(12.0, first.arucoMarkers[1].code.topMm, EPSILON)
        assertEquals(188.0, first.arucoMarkers[2].code.leftMm, EPSILON)
        assertEquals(275.0, first.arucoMarkers[2].code.topMm, EPSILON)
        assertEquals(12.0, first.arucoMarkers[3].code.leftMm, EPSILON)
        assertEquals(275.0, first.arucoMarkers[3].code.topMm, EPSILON)
    }

    @Test
    fun `PR-02 打印快照按题携带 pageId 与二维码载荷`() {
        val monday = LocalDate.of(2026, 11, 16)
        val questions = listOf(
            RenderableQuestion("q1", "T-1", 1, listOf("一元二次方程"), 1, 0, true),
            RenderableQuestion("q2", "T-2", 1, listOf("勾股定理"), 1, 0, true),
        )
        val pagination = DefaultPagePlanner().plan(
            planId = "PR02AB",
            dailyGroups = linkedMapOf(monday to questions),
            template = template,
        ) as PaginationResult.Success
        val layout = PrintLayoutBuilder.build(
            planId = "PR02AB",
            pages = pagination.pages,
            questions = questions.associateBy { it.questionId },
            noteTexts = emptyMap(),
            template = template,
        )

        val snapshots = layout.printSnapshots()

        assertEquals(layout.pages.flatMap { it.items }.size, snapshots.size)
        assertEquals("PR02AB-01", snapshots.first().pageId)
        assertEquals("q1", snapshots.first().questionId)
        assertEquals(layout.pages.first().items.first().qrPayload, snapshots.first().qrPayload)
    }

    private fun PrintPageLayout.allTexts(): List<String> = buildList {
        dateHeading?.let { add(it.text) }
        dailyTotal?.let { add(it.text) }
        legend?.let { add(it.text) }
        headerCells.forEach { add(it.text) }
        items.forEach { item ->
            item.left.rows.forEach { row -> add(row.knowledgeName) }
            add(item.rightTop.pageLine.text)
            add(item.rightTop.knowledgeText.text)
            item.note?.let { note ->
                add(note.title.text)
                note.attachments.forEach { add(it.text) }
            }
        }
    }

    private companion object {
        const val EPSILON = 1e-9
    }
}
