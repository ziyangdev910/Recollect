package com.recollect.feature.print

import com.recollect.pagination.DefaultPagePlanner
import com.recollect.pagination.PaginationResult
import com.recollect.pagination.PlacedItem
import com.recollect.pagination.PlannedPage
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
 * 日期大标题绘制在上边距内、不占内容区；2026-11-22 删除首页图例与「本日共需复习 X 题」，
 * 填涂框上方每页直接标注「掌握 / 较熟悉 / 不会」；
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
                knowledgeLineCount = 4,
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
        assertEquals(26.0, first.contentTopMm, EPSILON) // 2026-11-22 删除首页图例，所有页从 26mm 开始
        assertEquals(158.0, first.contentWidthMm, EPSILON)
        assertEquals(66.36, first.leftAreaWidthMm, EPSILON)
        assertEquals(92.36, first.rightAreaLeftMm, EPSILON)
        assertEquals(91.64, first.rightAreaWidthMm, EPSILON)
        assertEquals(85.64, first.rightTextWidthMm, EPSILON)

        // C-063：只保留日期大标题，绘制在上边距内，不占用内容区；每日总题数已删除。
        val heading = first.dateHeading
        assertNotNull(heading)
        assertEquals("2026-11-16 周一", heading!!.text)
        assertEquals(5.0, heading.fontMm)
        assertEquals(7.0, heading.heightMm)
        assertEquals(19.0, heading.topMm, EPSILON) // 内容区上边界 26 - 标题块 7
        assertEquals(7.0, template.dateHeadingBlockHeightMm, EPSILON)

        // 表头按列拆开，列 x 与左侧填涂行共用几何：知识点对齐名称列，
        // 「掌握 / 较熟悉 / 不会」以三格中心居中（2026-11-22 用户要求）。
        val headerCells = first.headerCells
        assertEquals(listOf("序号", "知识点", "掌握", "较熟悉", "不会"), headerCells.map { it.text })
        headerCells.forEach { cell ->
            // 2026-11-22：表头贴近第一道题，文字放在表头带底部（26 + 7 - 3.5 = 29.5）。
            assertEquals(29.5, cell.topMm, EPSILON)
            assertEquals(3.5, cell.heightMm, EPSILON)
        }
        assertEquals(26.0, headerCells[0].leftMm, EPSILON)
        assertEquals(7.0, headerCells[0].widthMm, EPSILON)
        assertEquals(PrintTextAlign.Left, headerCells[0].align)
        assertEquals(33.5, headerCells[1].leftMm, EPSILON)
        assertEquals(34.0, headerCells[1].widthMm, EPSILON)
        assertEquals(PrintTextAlign.Left, headerCells[1].align)
        assertEquals(66.85, headerCells[2].leftMm, EPSILON)
        assertEquals(75.65, headerCells[3].leftMm, EPSILON)
        assertEquals(84.45, headerCells[4].leftMm, EPSILON)
        headerCells.drop(2).forEach { cell ->
            assertEquals(10.0, cell.widthMm, EPSILON)
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
        assertEquals(33.0, q1.blockTopMm, EPSILON) // 26 + 7
        // 2026-11-24 新排版：顶部 14.5 + 笔记行 30 + 间距 5；QR 与书写区并排，不再占页脚。
        assertEquals(49.5, q1.blockHeightMm, EPSILON)
        assertEquals(1, q1.left.rows.size)
        assertEquals(1, q1.left.rows[0].index)
        assertEquals("一元二次方程", q1.left.rows[0].knowledgeName)
        assertEquals(33.0, q1.left.rows[0].topMm, EPSILON)
        assertEquals(7.5, q1.left.rows[0].heightMm, EPSILON)
        assertEquals(listOf("A", "B", "C"), q1.left.rows[0].bubbles.map { it.label })
        assertEquals(69.0, q1.left.rows[0].bubbles[0].leftMm, EPSILON)
        assertEquals(77.8, q1.left.rows[0].bubbles[1].leftMm, EPSILON)
        assertEquals(86.6, q1.left.rows[0].bubbles[2].leftMm, EPSILON)
        // 2026-11-22：答题卡式 16:9 宽格（5.7 × 3.2），框间距扩大为 3.1mm 以容纳档位文字。
        q1.left.rows[0].bubbles.forEach { bubble ->
            assertEquals(5.7, bubble.widthMm, EPSILON)
            assertEquals(3.2, bubble.heightMm, EPSILON)
            assertEquals(16.0 / 9.0, bubble.widthMm / bubble.heightMm, 0.01)
        }
        assertEquals(92.3, q1.left.rows[0].bubbles[2].leftMm + 5.7, EPSILON)
        assertTrue(q1.left.rows[0].bubbles[2].leftMm + 5.7 <= q1.rightTop.xMm)
        assertEquals(35.15, q1.left.rows[0].bubbles[0].topMm, EPSILON) // 33 + (7.5-3.2)/2

        // 表头与真实行严格对齐（本用例锁定错位回归）：序号/知识点左对齐，档位文字与格子中心对齐。
        assertEquals(q1.left.rows[0].indexText.leftMm, headerCells[0].leftMm, EPSILON)
        assertEquals(q1.left.rows[0].nameText.leftMm, headerCells[1].leftMm, EPSILON)
        headerCells.drop(2).zip(q1.left.rows[0].bubbles).forEach { (cell, bubble) ->
            assertEquals(bubble.leftMm + bubble.widthMm / 2, cell.leftMm + cell.widthMm / 2, EPSILON)
        }

        assertEquals(92.36, q1.rightTop.xMm, EPSILON)
        assertEquals(33.0, q1.rightTop.topMm, EPSILON)
        // 顶部信息段取 max(左填涂 7.5, 右信息 14.5) = 14.5。
        assertEquals(7.0 + 1 * 7.5, q1.rightTop.heightMm, EPSILON)
        // 2026-11-24 用户要求：题目开头不再显示「页码 XX」，保留题号与来源页。
        assertEquals("题号 T-1　来源页 P12", q1.rightTop.pageLine.text)
        assertTrue(q1.rightTop.pageLine.bold)
        assertEquals("一元二次方程", q1.rightTop.knowledgeText.text)
        assertEquals(1, q1.rightTop.knowledgeText.maxLines)

        // 下方先通栏显示历史笔记（本题为空），再是书写区；书写区右侧让出 QR 列。
        assertNull(q1.notesDisplay)
        assertEquals(47.5, q1.note?.topMm!!, EPSILON) // 33 + 顶部 14.5
        assertEquals(30.0, q1.note?.heightMm!!, EPSILON)
        assertEquals(26.0, q1.note?.xMm!!, EPSILON)
        assertEquals(137.0, q1.note?.widthMm!!, EPSILON) // 158 - 18(QR) - 3(间距)
        assertEquals("复习笔记", q1.note?.title?.text)
        assertEquals(3, q1.note?.writingLines!!.size)
        // 2026-11-25：横线间距 8mm，最后一条线距书写行底 1mm（47.5 + 13/21/29）。
        assertEquals(listOf(60.5, 68.5, 76.5), q1.note?.writingLines?.map { it.topMm })
        assertEquals(
            q1.note!!.topMm + q1.note!!.heightMm - 1.0,
            q1.note!!.writingLines.last().topMm,
            EPSILON,
        )

        assertEquals(18.0, q1.qr.sizeMm, EPSILON)
        assertEquals(166.0, q1.qr.leftMm, EPSILON) // 内容区右边 184 - 18
        // 2026-11-25：QR + pageId 标签组合底边与最后一条横线（76.5）对齐。
        assertEquals(52.5, q1.qr.topMm, EPSILON) // 76.5 - 24
        assertEquals(4, q1.qr.quietZoneModules)

        // C-095 方案 B：完整 pageId 位于 QR 下方，间距为标签高度的一半（2mm），并与 QR 右缘对齐。
        val label = q1.qr.pageIdLabel
        assertEquals("K7M2Q9-01", label.text)
        assertEquals(166.0, label.leftMm, EPSILON)
        assertEquals(72.5, label.topMm, EPSILON) // QR 底 70.5 + 间距 2
        assertEquals(18.0, label.widthMm, EPSILON)
        assertEquals(2.2, label.fontMm, EPSILON)
        assertEquals(PrintTextAlign.Right, label.align)
        assertEquals(q1.qr.leftMm + q1.qr.sizeMm, label.leftMm + label.widthMm, EPSILON)
        assertEquals(q1.qr.topMm + q1.qr.sizeMm + 2.0, label.topMm, EPSILON)
        // 2026-11-25：QR-计划标签组合的底边与最后一条横线水平对齐。
        assertEquals(q1.note!!.writingLines.last().topMm, label.topMm + label.heightMm, EPSILON)
        // 书写区与 QR 列不重叠：书写区右缘 = QR 左缘 - 列间距。
        assertEquals(q1.qr.leftMm - 3.0, q1.note!!.xMm + q1.note!!.widthMm, EPSILON)
        // QR 必须完整落在题块内（回读归属绑定依赖这一点）。
        assertTrue(q1.qr.topMm + 18.0 + 2.0 + 4.0 <= q1.blockTopMm + q1.blockHeightMm)

        // C-062：纸上没有笔记区时不绘制笔记与附件，且不截断附件区（P-15 已在分页层处理高度）。
        val q2 = first.items[1]
        assertEquals(82.5, q2.blockTopMm, EPSILON) // 33 + 49.5
        assertEquals(54.0, q2.blockHeightMm, EPSILON) // 顶部 max(2*7.5, 7+2*7.5)=22 + QR 预留带 27 + 5
        assertEquals(2, q2.left.rows.size)
        assertEquals(82.5, q2.left.rows[0].topMm, EPSILON)
        assertEquals(90.0, q2.left.rows[1].topMm, EPSILON)
        assertEquals("勾股定理", q2.left.rows[0].knowledgeName)
        assertEquals("相似三角形", q2.left.rows[1].knowledgeName)
        assertEquals("勾股定理 / 相似三角形", q2.rightTop.knowledgeText.text)
        assertEquals(2, q2.rightTop.knowledgeText.maxLines)
        assertNull(q2.note)
        // C-062：无笔记区时不绘制历史笔记，QR 退化为题块底部预留带。
        assertNull(q2.notesDisplay)
        assertEquals(82.5 + 22.0 + 3.0, q2.qr.topMm, EPSILON)

        // 新排版下 q3 能留在首页，q4 放不下才整条移到同日第 2 页。
        val second = layout.pages[1]
        assertEquals("K7M2Q9-02", second.pageId)
        assertEquals(monday, second.date)
        assertEquals(26.0, second.contentTopMm, EPSILON)
        assertEquals(
            listOf("序号", "知识点", "掌握", "较熟悉", "不会"),
            second.headerCells.map { it.text },
        )
        assertEquals(29.5, second.headerCells.first().topMm, EPSILON)
        assertEquals("2026-11-16 周一", second.dateHeading?.text)
        assertEquals(19.0, second.dateHeading!!.topMm, EPSILON)

        // 左侧名称只在左侧截断为 9 个码点 + “…”；右上保留全称（C-013）。
        // C-062：hasNoteArea=true 时附件区按 3 条完整绘制，不截断到 30% 上限。
        val q3 = first.items[2]
        assertEquals("q3", q3.questionId)
        assertEquals(136.5, q3.blockTopMm, EPSILON) // 33 + 49.5(q1) + 54(q2)
        // 顶部 max(7.5, 7+2*7.5=22) + 附加 30 + 笔记行 30 + 间距 5 = 87mm。
        assertEquals(87.0, q3.blockHeightMm, EPSILON)
        assertEquals("一个非常长的知识点…", q3.left.rows[0].knowledgeName)
        assertEquals(
            "一个非常长的知识点名称用于验证左侧截断规则",
            q3.rightTop.knowledgeText.text,
        )
        // 2026-11-24：历史笔记显示区在书写区上方通栏，宽 158mm。
        assertEquals(158.5, q3.notesDisplay?.topMm!!, EPSILON) // 136.5 + 顶部 22
        assertEquals(30.0, q3.notesDisplay?.heightMm!!, EPSILON)
        assertEquals(26.0, q3.notesDisplay?.xMm!!, EPSILON)
        assertEquals(158.0, q3.notesDisplay?.widthMm!!, EPSILON)
        assertEquals(3, q3.notesDisplay?.texts?.size)
        assertEquals(
            listOf("附件 A", "附件 B", "附件 C"),
            q3.notesDisplay?.texts?.map { it.text },
        )
        assertEquals(188.5, q3.note?.topMm!!, EPSILON) // 158.5 + 附加 30
        assertEquals(30.0, q3.note?.heightMm!!, EPSILON)
        // QR 组合底边贴最后一条横线：188.5 + 29 − 24 = 193.5。
        assertEquals(193.5, q3.qr.topMm, EPSILON)
        assertEquals(
            q3.note!!.topMm + 29.0,
            q3.qr.pageIdLabel.topMm + q3.qr.pageIdLabel.heightMm,
            EPSILON,
        )

        val q4 = second.items.single()
        assertEquals("q4", q4.questionId)
        assertEquals(33.0, q4.blockTopMm, EPSILON) // 新页只扣页级表头
        assertEquals(72.0, q4.blockHeightMm, EPSILON) // 顶部 max(7.5, 7+4*7.5=37) + 30 + 5
        assertEquals("题号 T-4　来源页 P7", q4.rightTop.pageLine.text)
        assertEquals(70.0, q4.note?.topMm!!, EPSILON) // 33 + 37
        assertEquals(75.0, q4.qr.topMm, EPSILON) // 70 + 29 − 24

        // 新日期重新开始：只绘制日期标题；没有图例与每日总题数。
        val third = layout.pages[2]
        assertEquals(tuesday, third.date)
        assertEquals("2026-11-17 周二", third.dateHeading?.text)
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

    @Test
    fun `PR-03 header anchors to the first item even when an old plan keeps legacy offsets`() {
        val date = LocalDate.of(2026, 11, 16)
        val question = RenderableQuestion(
            questionId = "q1",
            questionNumber = "T-1",
            knowledgeCount = 1,
            knowledgeNames = listOf("一元二次方程"),
            knowledgeLineCount = 1,
            attachmentCount = 0,
            hasNoteArea = true,
        )
        // 模拟旧计划：首题仍带已删除的 8mm 图例偏移（26 + 8 + 7 = 41）。
        val page = PlannedPage(
            pageId = "K7M2Q9-01",
            pageNumber = 1,
            date = date,
            items = listOf(PlacedItem(questionId = "q1", yOffsetMm = 41.0, heightMm = 74.5)),
            continuationMarkers = emptyList(),
        )

        val layout = PrintLayoutBuilder.build(
            planId = "K7M2Q9",
            pages = listOf(page),
            questions = mapOf("q1" to question),
            noteTexts = emptyMap(),
            template = template,
        )

        val first = layout.pages.single()
        assertEquals(41.0, first.items.single().blockTopMm, EPSILON)
        first.headerCells.forEach { cell ->
            // 表头底边贴住首题，而不是固定在 contentTop + 7mm。
            assertEquals(41.0 - 3.5, cell.topMm, EPSILON)
        }
    }

    private fun PrintPageLayout.allTexts(): List<String> = buildList {
        dateHeading?.let { add(it.text) }
        headerCells.forEach { add(it.text) }
        items.forEach { item ->
            item.left.rows.forEach { row -> add(row.knowledgeName) }
            add(item.rightTop.pageLine.text)
            add(item.rightTop.knowledgeText.text)
            item.notesDisplay?.texts?.forEach { add(it.text) }
            item.note?.let { note -> add(note.title.text) }
        }
    }

    private companion object {
        const val EPSILON = 1e-9
    }
}
