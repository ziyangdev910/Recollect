package com.recollect.pagination

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PagePlannerTest {

    /**
     * P-01：一日一条、高度正常 → 一页一条。
     * 默认模板下：leftHeight = 2 × 7.5 = 15；rightTop = 7 + 3 × 7.5 = 29.5；
     * rightBottom = 30 + 0；itemHeight = max(15, 59.5) + 21 + 3 = 83.5mm。
     * 首页首条 yOffsetMm = topMargin 12 + legend 8 + headerRow 7 = 27mm（C-048）。
     */
    @Test
    fun `P-01 one question on one day occupies exactly one page`() {
        val date = LocalDate.of(2026, 11, 16)
        val question = RenderableQuestion(
            questionId = "q1",
            questionNumber = "1-1",
            knowledgeCount = 2,
            knowledgeNames = listOf("一元二次方程", "判别式"),
            knowledgeLineCount = 3,
            attachmentCount = 0,
            hasNoteArea = true,
        )

        val result = DefaultPagePlanner().plan(
            planId = "plan-p01",
            dailyGroups = mapOf(date to listOf(question)),
            template = TemplateGeometry(),
        )

        val success = assertIs<PaginationResult.Success>(result)
        val page = success.pages.single()
        assertEquals("plan-p01-01", page.pageId, "pageId = {planId}-{pageNumber:02d}")
        assertEquals(1, page.pageNumber, "一页一条时首页的 pageNumber 为 1")
        assertEquals(date, page.date, "页面日期等于该条目所属日期")

        val item = page.items.single()
        assertEquals("q1", item.questionId)
        assertEquals(
            83.5,
            item.heightMm,
            1e-9,
            "itemHeight = max(leftHeight, rightTop + rightBottom) + qrFooterHeightMm + itemSpacingMm",
        )
        assertEquals(
            27.0,
            item.yOffsetMm,
            1e-9,
            "C-048：yOffsetMm = topMarginMm + legendHeightMm（首页）+ headerRowHeightMm + 已放置高度",
        )

        assertTrue(page.continuationMarkers.isEmpty(), "一页一条时不应出现续下页标记")
        assertTrue(success.overflowItems.isEmpty(), "高度正常的条目不应进入 overflowItems")
        assertTrue(success.warnings.isEmpty(), "高度正常的条目不应产生 warnings")
    }

    /**
     * P-03：一日四条、每条 68.5mm，第 4 条放不下 → 整条移到下一页；
     * 第 1 页恰好一个 `ContinuationMarker(order=0)`，`fromPageId`/`toPageId` 正确（C-048）。
     *
     * 默认模板：首页内容区 258mm、后续页 266mm。
     * 条目高度 = max(1×7.5, 7 + 1×7.5 + 30) + 21 + 3 = 68.5mm。
     * 3 × 68.5 = 205.5 ≤ 258，第 4 条 205.5 + 68.5 = 274 > 258 → 整条移页；
     * 单条 68.5 ≤ 266，因此落在第 2 页完整放置，而不是进入 overflowItems。
     */
    @Test
    fun `P-03 fourth item that does not fit moves whole to next page with one marker`() {
        val date = LocalDate.of(2026, 11, 17)
        val questions = (1..4).map { index ->
            RenderableQuestion(
                questionId = "q$index",
                questionNumber = "3-$index",
                knowledgeCount = 1,
                knowledgeNames = listOf("知识点$index"),
                knowledgeLineCount = 1,
                attachmentCount = 0,
                hasNoteArea = true,
            )
        }

        val result = DefaultPagePlanner().plan(
            planId = "plan-p03",
            dailyGroups = mapOf(date to questions),
            template = TemplateGeometry(),
        )

        val success = assertIs<PaginationResult.Success>(result)
        assertEquals(2, success.pages.size, "第 4 条放不下时必须整条移到下一页")
        assertTrue(
            success.overflowItems.isEmpty(),
            "68.5mm 远小于后续页内容区 266mm，不得进入 overflowItems",
        )

        val firstPage = success.pages[0]
        assertEquals("plan-p03-01", firstPage.pageId)
        assertEquals(1, firstPage.pageNumber)
        assertEquals(date, firstPage.date)
        assertEquals(
            listOf("q1", "q2", "q3"),
            firstPage.items.map { it.questionId },
            "C-048：遇到第一条放不下的条目就结束当前页，不得跳过它去填后续条目",
        )
        assertEquals(
            listOf(27.0, 95.5, 164.0),
            firstPage.items.map { it.yOffsetMm },
            "yOffsetMm 从 topMargin+legend+header 起算，按已放置高度依次累加 68.5mm",
        )

        assertEquals(
            listOf(
                ContinuationMarker(
                    fromPageId = "plan-p03-01",
                    toPageId = "plan-p03-02",
                    questionId = "q4",
                    order = 0,
                ),
            ),
            firstPage.continuationMarkers,
            "第 1 页必须恰好一个 order=0 的续下页标记，且指向实际放置页",
        )

        val secondPage = success.pages[1]
        assertEquals("plan-p03-02", secondPage.pageId)
        assertEquals(2, secondPage.pageNumber)
        assertEquals(date, secondPage.date)
        assertEquals(
            listOf("q4"),
            secondPage.items.map { it.questionId },
            "被移走的条目必须按原顺序完整出现在下一页，不拆分",
        )
        assertEquals(
            68.5,
            secondPage.items.single().heightMm,
            1e-9,
            "不拆分条目、不压缩高度",
        )
        assertEquals(
            19.0,
            secondPage.items.single().yOffsetMm,
            1e-9,
            "后续页只扣 headerRowHeightMm，不扣首页图例",
        )
        assertTrue(secondPage.continuationMarkers.isEmpty(), "第 2 页之后没有剩余条目，不应生成标记")
    }
}
