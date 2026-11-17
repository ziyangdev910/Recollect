package com.recollect.pagination

import java.time.LocalDate

/**
 * 分页实现：按 `docs/specs/pagination.md` 的单题高度公式算高，并按 C-048 放置语义分页。
 *
 * 已实现：日期升序、每天从新页起步、按容量逐页放置、遇到第一条放不下的条目就结束当前页、
 * 被移条目在来源页生成 `ContinuationMarker`（P-01、P-03、P-08）；首页扣图例与页级表头、
 * 后续页只扣页级表头（P-10）；首页放不下但后续页放得下时保留空首页以承载图例（P-13）；
 * 单题高于 `contentHeightMm` 时整条进入 `overflowItems(ITEM_TOO_TALL)`（P-14、P-18）；
 * 打印笔记书写区时，附加面积超过 `usableHeightMm × maxAttachmentHeightRatio` 时整条进入
 * `overflowItems(ATTACHMENT_OVER_LIMIT)`（P-05、P-11、C-027）。两条同时成立时按
 * C-061 方案 B 归为 `ITEM_TOO_TALL`；不打印笔记区时附加数量不参与上限判定（C-062 方案 B）。
 * overflow 条目不打印、不拆分、不压缩、不生成 marker，但仍给出 warnings。
 *
 * 验收门禁：模板几何非法（`usableHeightMm <= 0` 或 `firstPageContentHeightMm <= 0`）返回
 * `Failure(INVALID_TEMPLATE_GEOMETRY)`；页数超过 99 返回 `Failure(PLAN_TOO_LONG)`；
 * 非法计数字段抛 `IllegalArgumentException`（P-20），不静默截断或补 0。
 */
class DefaultPagePlanner : PagePlanner {
    override fun plan(
        planId: String,
        dailyGroups: Map<LocalDate, List<RenderableQuestion>>,
        template: TemplateGeometry,
    ): PaginationResult {
        // 模板校验先于输入计数校验：几何非法时无法计算任何高度，直接返回 Failure。
        if (!(template.usableHeightMm > 0.0) || !(template.firstPageContentHeightMm > 0.0)) {
            return PaginationResult.Failure(
                code = PaginationFailureCode.INVALID_TEMPLATE_GEOMETRY,
                message = "模板几何非法：可用高度 ${template.usableHeightMm}mm、" +
                    "首页内容区 ${template.firstPageContentHeightMm}mm，均必须大于 0；请检查模板参数。",
            )
        }

        val sortedGroups = dailyGroups.toSortedMap()
        // P-20：非法计数字段抛 IllegalArgumentException，不静默截断或补 0。
        sortedGroups.values.forEach { questions ->
            questions.forEach { question ->
                require(question.knowledgeCount in 1..4) {
                    "RenderableQuestion ${question.questionId} 的 knowledgeCount=${question.knowledgeCount}，" +
                        "必须在 1..4（P-20）"
                }
                require(question.attachmentCount in 0..3) {
                    "RenderableQuestion ${question.questionId} 的 attachmentCount=${question.attachmentCount}，" +
                        "必须在 0..3（P-20）"
                }
                require(question.knowledgeLineCount in 1..4) {
                    "RenderableQuestion ${question.questionId} 的 knowledgeLineCount=" +
                        "${question.knowledgeLineCount}，必须在 1..4（P-20）"
                }
            }
        }

        val pages = mutableListOf<PlannedPage>()
        val overflowItems = mutableListOf<OverflowItem>()
        // 输出与输入 Map 的迭代顺序无关：日期升序（C-025、P-08）。
        for ((date, questions) in sortedGroups) {
            // 空日期不建页（C-048 放置语义）。
            if (questions.isEmpty()) continue

            val heights = questions.map { itemHeightMm(it, template) }
            // 先判定 overflow（附件超限或单题过高），再对剩余条目按 C-048 分页。
            // 进入 overflowItems 的条目：不打印、不拆分/压缩、不生成 marker。
            val overflowReasons = questions.mapIndexed { index, question ->
                overflowReasonOf(question, heights[index], template)
            }
            questions.forEachIndexed { index, question ->
                val reason = overflowReasons[index] ?: return@forEachIndexed
                overflowItems += OverflowItem(
                    questionId = question.questionId,
                    date = date,
                    heightMm = heights[index],
                    reason = reason,
                )
            }
            val paginable = questions.indices.filter { overflowReasons[it] == null }
            if (paginable.isEmpty()) continue

            pages += planDate(
                planId = planId,
                date = date,
                questions = questions,
                heights = heights,
                paginable = paginable,
                template = template,
                startPageNumber = pages.size + 1,
            )
        }

        // 验收门禁：pageNumber 从 1 连续递增，超过 99 时返回 Failure（PLAN_TOO_LONG），
        // 不截断页数、不继续输出第 100 页。
        if (pages.size > MAX_PAGE_NUMBER) {
            return PaginationResult.Failure(
                code = PaginationFailureCode.PLAN_TOO_LONG,
                message = "计划需要 ${pages.size} 页，超过单份计划 $MAX_PAGE_NUMBER 页上限；请拆分计划。",
            )
        }

        return PaginationResult.Success(
            pages = pages,
            overflowItems = overflowItems,
            // C-048/C-064：overflow 不打印，但仍须给出含 questionId 与原因的用户可读提示；
            // 按 questionId 字典序、同 questionId 按 code 枚举序升序（合并计划层文字警告后同样适用）。
            warnings = overflowItems
                .map { item ->
                    PaginationWarning(
                        questionId = item.questionId,
                        // C-065 方案 B：稳定代码只给程序/日志使用；界面映射为中文标签。
                        code = when (item.reason) {
                            OverflowReason.ITEM_TOO_TALL -> PaginationWarningCode.ITEM_TOO_TALL
                            OverflowReason.ATTACHMENT_OVER_LIMIT -> PaginationWarningCode.ATTACHMENT_OVER_LIMIT
                        },
                        message = overflowMessage(item, template),
                    )
                }
                .sortedWith(compareBy({ it.questionId }, { it.code.ordinal })),
        )
    }

    /** 为一个日期分页；页号在整份文档内连续（C-048）。 */
    private fun planDate(
        planId: String,
        date: LocalDate,
        questions: List<RenderableQuestion>,
        heights: List<Double>,
        paginable: List<Int>,
        template: TemplateGeometry,
        startPageNumber: Int,
    ): List<PlannedPage> {
        // 逐页放置：遇到第一条放不下的条目就结束当前页，该条及后续条目按原顺序进入下一页。
        val indicesByPage = mutableListOf<List<Int>>()
        val landingPageByIndex = IntArray(questions.size) { -1 }
        var cursor = 0
        while (cursor < paginable.size) {
            val pageNumber = startPageNumber + indicesByPage.size
            val capacity = contentCapacityMm(pageNumber, template)

            val placedOnPage = mutableListOf<Int>()
            var usedHeight = 0.0
            var position = cursor
            while (position < paginable.size) {
                val index = paginable[position]
                if (usedHeight + heights[index] > capacity) break
                placedOnPage += index
                landingPageByIndex[index] = indicesByPage.size
                usedHeight += heights[index]
                position++
            }

            if (placedOnPage.isEmpty()) {
                // 只剩「首页扣图例后放不下、空白后续页放得下」这一种情况（P-13、C-048）：
                // 保留首页以承载图例，条目留到下一页。
                // 进入本函数的条目都不高于 contentHeightMm，后续页容量恰为 contentHeightMm，必定能放下。
                check(pageNumber == 1) {
                    "page $pageNumber (capacity $capacity mm) fits no paginable item; " +
                        "items above contentHeightMm must be filtered into overflowItems"
                }
                indicesByPage.add(emptyList())
                continue
            }

            indicesByPage += placedOnPage
            cursor = position
        }

        return indicesByPage.mapIndexed { pageIndex, indices ->
            val pageNumber = startPageNumber + pageIndex
            val pageId = pageIdOf(planId, pageNumber)
            var itemCursor = template.topMarginMm +
                (if (pageNumber == 1) template.legendHeightMm else 0.0) +
                template.headerRowHeightMm

            val items = indices.map { index ->
                val item = PlacedItem(
                    questionId = questions[index].questionId,
                    yOffsetMm = itemCursor,
                    heightMm = heights[index],
                )
                itemCursor += heights[index]
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

    /**
     * 用户可读提示：只描述原因与可执行的修改方向，不改写条目高度，也不建议拆分题目（C-066）。
     * 整题过高 → 减少本题关联的知识点或缩短知识点描述（减少右上名称行数）；
     * 附加超限 → 减少附加笔记。
     */
    private fun overflowMessage(item: OverflowItem, template: TemplateGeometry): String =
        when (item.reason) {
            OverflowReason.ITEM_TOO_TALL ->
                "题目高度 ${item.heightMm}mm 超过单页内容区 ${template.contentHeightMm}mm，整题不打印；" +
                    "请减少本题关联的知识点，或缩短知识点描述（减少右上名称行数）。"
            OverflowReason.ATTACHMENT_OVER_LIMIT ->
                "附加笔记区超过模板上限 ${template.maxAttachmentHeightMm}mm，整题不打印；请减少附加笔记。"
        }

    /**
     * 单题移出打印的原因（C-027、C-016/C-025）：
     * 单题高度超过内容区（含页级表头）→ `ITEM_TOO_TALL`；
     * 否则仅在打印笔记书写区时判断附件面积是否超过
     * `usableHeightMm × maxAttachmentHeightRatio` → `ATTACHMENT_OVER_LIMIT`。
     *
     * 判定基准必须是 `contentHeightMm` 而不是 `usableHeightMm`，否则
     * `(contentHeightMm, usableHeightMm]` 会成为「既放不下又不在 overflow」的区间（P-14、P-18）。
     * 两条同时成立时按 C-061 方案 B：单题过高优先；提示按 C-066 引导减少关联知识点或缩短描述。
     * `hasNoteArea=false` 时纸上没有笔记区与附加笔记，附件数量不参与上限判定（C-062 方案 B）。
     */
    private fun overflowReasonOf(
        question: RenderableQuestion,
        heightMm: Double,
        template: TemplateGeometry,
    ): OverflowReason? = when {
        heightMm > template.contentHeightMm -> OverflowReason.ITEM_TOO_TALL

        question.hasNoteArea &&
            attachmentAreaMm(question, template) > template.maxAttachmentHeightMm ->
            OverflowReason.ATTACHMENT_OVER_LIMIT

        else -> null
    }

    /** 附加笔记区总高度；超过上限时整条不打印，不截断（C-027）。 */
    private fun attachmentAreaMm(question: RenderableQuestion, template: TemplateGeometry): Double =
        question.attachmentCount * template.attachmentHeightPerItemMm

    /** pagination.md「单题高度公式」，必须原样实现。 */
    private fun itemHeightMm(question: RenderableQuestion, template: TemplateGeometry): Double {
        val leftHeight = question.knowledgeCount * template.rowHeightMm
        val rightTop = template.questionFixedInfoHeightMm +
            question.knowledgeLineCount * template.rowHeightMm
        val rightBottom = if (question.hasNoteArea) {
            template.noteAreaBaseHeightMm + attachmentAreaMm(question, template)
        } else {
            0.0
        }
        return maxOf(leftHeight, rightTop + rightBottom) +
            template.qrFooterHeightMm +
            template.itemSpacingMm
    }

    private companion object {
        /** 单份计划的最大页数；`pageNumber` 为 1..99（pagination.md 分页规则）。 */
        const val MAX_PAGE_NUMBER = 99
    }
}
