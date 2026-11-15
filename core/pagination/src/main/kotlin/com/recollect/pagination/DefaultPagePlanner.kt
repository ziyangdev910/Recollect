package com.recollect.pagination

import java.time.LocalDate

/**
 * 分页实现：按 `docs/specs/pagination.md` 的单题高度公式算高，并按 C-048 放置语义分页。
 *
 * 已实现：每天一页起步、按容量逐页放置、遇到第一条放不下的条目就结束当前页、被移条目
 * 在来源页生成 `ContinuationMarker`（P-01、P-03）。
 * 尚未实现：overflow 与附件上限（P-05/P-11/P-14/P-18）、日期排序（P-08/P-12）、
 * 首页单独保留（P-13）、模板校验与 `PLAN_TOO_LONG`（验收门禁）。
 */
class DefaultPagePlanner : PagePlanner {
    override fun plan(
        planId: String,
        dailyGroups: Map<LocalDate, List<RenderableQuestion>>,
        template: TemplateGeometry,
    ): PaginationResult {
        val pages = mutableListOf<PlannedPage>()
        for ((date, questions) in dailyGroups) {
            // 空日期不建页（C-048 放置语义）。
            if (questions.isEmpty()) continue
            pages += planDate(
                planId = planId,
                date = date,
                questions = questions,
                template = template,
                startPageNumber = pages.size + 1,
            )
        }

        return PaginationResult.Success(
            pages = pages,
            overflowItems = emptyList(),
            warnings = emptyList(),
        )
    }

    /** 为一个日期分页；页号在整份文档内连续（C-048）。 */
    private fun planDate(
        planId: String,
        date: LocalDate,
        questions: List<RenderableQuestion>,
        template: TemplateGeometry,
        startPageNumber: Int,
    ): List<PlannedPage> {
        val heights = questions.map { itemHeightMm(it, template) }

        // 逐页放置：遇到第一条放不下的条目就结束当前页，该条及后续条目按原顺序进入下一页。
        val indicesByPage = mutableListOf<List<Int>>()
        val landingPageByIndex = IntArray(questions.size) { -1 }
        var next = 0
        while (next < questions.size) {
            val pageNumber = startPageNumber + indicesByPage.size
            val capacity = contentCapacityMm(pageNumber, template)

            val placedOnPage = mutableListOf<Int>()
            var usedHeight = 0.0
            var index = next
            while (index < questions.size) {
                if (usedHeight + heights[index] > capacity) break
                placedOnPage += index
                landingPageByIndex[index] = indicesByPage.size
                usedHeight += heights[index]
                index++
            }

            if (placedOnPage.isEmpty()) {
                // 空白页也放不下 → 应进入 overflowItems(ITEM_TOO_TALL)，属 P-14/P-18，本轮未实现；
                // 这里结束当日分页，避免无限建页。不生成 marker、不打印。
                break
            }

            indicesByPage += placedOnPage
            next = index
        }

        return indicesByPage.mapIndexed { pageIndex, indices ->
            val pageNumber = startPageNumber + pageIndex
            val pageId = pageIdOf(planId, pageNumber)
            var cursor = template.topMarginMm +
                (if (pageNumber == 1) template.legendHeightMm else 0.0) +
                template.headerRowHeightMm

            val items = indices.map { index ->
                val item = PlacedItem(
                    questionId = questions[index].questionId,
                    yOffsetMm = cursor,
                    heightMm = heights[index],
                )
                cursor += heights[index]
                item
            }

            // 本页未放置的条目（按原题目顺序）各生成一个 marker，指向其实际放置页。
            val movedIndices = indicesByPage.drop(pageIndex + 1).flatten()
            val markers = movedIndices.mapIndexed { order, index ->
                ContinuationMarker(
                    fromPageId = pageId,
                    toPageId = pageIdOf(planId, startPageNumber + landingPageByIndex[index]),
                    questionId = questions[index].questionId,
                    order = order,
                )
            }

            PlannedPage(
                pageId = pageId,
                pageNumber = pageNumber,
                date = date,
                items = items,
                continuationMarkers = markers,
            )
        }
    }

    private fun pageIdOf(planId: String, pageNumber: Int): String =
        "$planId-${pageNumber.toString().padStart(2, '0')}"

    /** 首页扣图例后的内容区高度；后续页只扣页级表头（C-025、C-001）。 */
    private fun contentCapacityMm(pageNumber: Int, template: TemplateGeometry): Double =
        if (pageNumber == 1) template.firstPageContentHeightMm else template.contentHeightMm

    /** pagination.md「单题高度公式」，必须原样实现。 */
    private fun itemHeightMm(question: RenderableQuestion, template: TemplateGeometry): Double {
        val leftHeight = question.knowledgeCount * template.rowHeightMm
        val rightTop = template.questionFixedInfoHeightMm +
            question.knowledgeLineCount * template.rowHeightMm
        val attachmentArea = question.attachmentCount * template.attachmentHeightPerItemMm
        val rightBottom = if (question.hasNoteArea) {
            template.noteAreaBaseHeightMm + attachmentArea
        } else {
            0.0
        }
        return maxOf(leftHeight, rightTop + rightBottom) +
            template.qrFooterHeightMm +
            template.itemSpacingMm
    }
}
