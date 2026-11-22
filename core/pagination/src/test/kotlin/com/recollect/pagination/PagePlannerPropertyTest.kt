package com.recollect.pagination

import java.time.LocalDate
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.RepetitionInfo
import org.junit.jupiter.api.TestInstance

/**
 * P-09 属性测试：随机 100 组输入下锁定 C-048 放置语义、C-060、C-061、C-062 的不变量。
 *
 * 断言：
 * - 条目不跨页、不重叠、不越出内容区下边界，且按公式原值整条放置；
 * - 每页的 `continuationMarkers` 与“该日期内落在后续页的条目”一一对应，`order` 与题目顺序一致；
 * - `overflowItems` 不出现在 `pages`，每题在“pages ∪ overflowItems”中恰好出现一次；
 * - `pageNumber` 全文档连续、`pageId` 唯一且格式固定、日期升序；
 * - C-060：全部条目溢出的日期不建页；C-061：两条件同时成立只输出一条 `ITEM_TOO_TALL`；
 * - C-062：无笔记区时附加上限不判定。
 *
 * 生成器由固定种子 `20261113 + repetition` 派生，单独重跑第 N 次也能复现同一组输入（C-002）。
 * 为覆盖已裁决分支，模板按 repetition 轮换：默认 A4 / 短页（每条附件 10mm）/ 短页（每条附件 15mm）；
 * 后两种显式构造“整日过高”“整题过高且附件超限”“无笔记区且附件超限”三种题目。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PagePlannerPropertyTest {

    private var c060AllOverflowDaySeen = false
    private var c061BothConditionsSeen = false
    private var c062NoNoteAreaOverLimitSeen = false

    @RepeatedTest(100)
    fun `P-09 random plans keep placement markers overflow and page numbering invariants`(
        repetitionInfo: RepetitionInfo,
    ) {
        val repetition = repetitionInfo.currentRepetition
        // 固定种子 + 序号派生独立随机流：单独重跑第 N 次也能复现同一组输入。
        val random = Random(PROPERTY_SEED + repetition)
        val template = when (repetition % 3) {
            0 -> TemplateGeometry()
            1 -> tightTemplate(attachmentHeightPerItemMm = 10.0)
            else -> tightTemplate(attachmentHeightPerItemMm = 15.0)
        }
        val base = LocalDate.of(2026, 12, 10).plusDays(random.nextInt(0, 30).toLong())
        var nextId = 0
        fun id(): String = "r$repetition-q${nextId++}"

        val groups = linkedMapOf<LocalDate, List<RenderableQuestion>>()
        repeat(random.nextInt(0, 6)) { index ->
            val date = base.plusDays(index.toLong())
            groups[date] = List(random.nextInt(0, 6)) { randomQuestion(id(), random) }
        }
        if (repetition % 3 != 0) {
            // 短页模板下必然整日溢出 → C-060：该日期不建页。
            groups[base.plusDays(20)] = listOf(maxParameterQuestion(id()))
        }
        if (repetition % 3 == 2) {
            // 短页 + 每条附件 15mm：整题 105mm > 95mm 且附件 45mm > 30mm → C-061。
            groups[base.plusDays(21)] = listOf(bothConditionsQuestion(id()), compactQuestion(id()))
            // 无笔记区 + 附件 45mm > 30mm → C-062：上限不判定，条目照常打印。
            groups[base.plusDays(22)] = listOf(noNoteAreaOverLimitQuestion(id()), compactQuestion(id()))
        }

        // 乱序插入，顺带锁定输出与输入 Map 迭代顺序无关（P-12）。
        val shuffledInput = linkedMapOf<LocalDate, List<RenderableQuestion>>()
        groups.keys.shuffled(random).forEach { shuffledInput[it] = groups.getValue(it) }

        val planner = DefaultPagePlanner()
        val success = assertIs<PaginationResult.Success>(
            planner.plan("plan-p09-$repetition", shuffledInput, template),
            "repetition=$repetition：随机输入必须返回 Success",
        )
        val sortedSuccess = assertIs<PaginationResult.Success>(
            planner.plan("plan-p09-$repetition", groups.toSortedMap(), template),
        )
        assertEquals(sortedSuccess, success, "repetition=$repetition：输出必须与输入 Map 的迭代顺序无关")

        val questionsById = groups.values.flatten().associateBy { it.questionId }
        assertEquals(
            groups.values.sumOf { it.size },
            questionsById.size,
            "repetition=$repetition：生成的题目 id 必须唯一",
        )

        // 页号连续、pageId 唯一且格式固定、日期升序。
        assertEquals(
            (1..success.pages.size).toList(),
            success.pages.map { it.pageNumber },
            "repetition=$repetition：pageNumber 必须从 1 起连续",
        )
        assertEquals(
            success.pages.map { page ->
                "plan-p09-$repetition-${page.pageNumber.toString().padStart(2, '0')}"
            },
            success.pages.map { it.pageId },
            "repetition=$repetition：pageId = {planId}-{pageNumber:02d}",
        )
        assertEquals(
            success.pages.size,
            success.pages.map { it.pageId }.toSet().size,
            "repetition=$repetition：pageId 必须唯一",
        )
        assertEquals(
            success.pages.map { it.date }.sorted(),
            success.pages.map { it.date },
            "repetition=$repetition：页面必须按日期升序",
        )

        for (date in groups.keys.sorted()) {
            val questions = groups.getValue(date)
            val datePages = success.pages.filter { it.date == date }
            val dateOverflow = success.overflowItems.filter { it.date == date }
            val expected = questions.associate { question ->
                val height = expectedHeightMm(question, template)
                question.questionId to (height to expectedOverflowReason(question, height, template))
            }
            val paginable = questions.filter { expected.getValue(it.questionId).second == null }
            val placedIds = datePages.flatMap { page -> page.items.map { it.questionId } }

            if (paginable.isEmpty()) {
                assertTrue(
                    datePages.isEmpty(),
                    "repetition=$repetition：$date 没有可打印条目时不得建页（C-060）",
                )
                if (questions.isNotEmpty()) c060AllOverflowDaySeen = true
            } else {
                assertTrue(
                    datePages.isNotEmpty(),
                    "repetition=$repetition：$date 有可打印条目时必须建页",
                )
                assertEquals(
                    paginable.map { it.questionId },
                    placedIds,
                    "repetition=$repetition：$date 的条目必须按输入顺序整条放置，不得跳过或重排",
                )
            }

            // overflow 原因、高度与条数（C-061 只输出一条）。
            questions.forEach { question ->
                val (height, reason) = expected.getValue(question.questionId)
                val attachmentArea = question.attachmentCount * template.attachmentHeightPerItemMm
                val entries = dateOverflow.filter { it.questionId == question.questionId }

                // C-062 的题目既可能被正常打印（本轮的构造情形），也可能因过高进入 overflow，
                // 覆盖标记必须在按原因分支之前记录，否则“可打印”路径会漏记。
                if (!question.hasNoteArea && attachmentArea > template.maxAttachmentHeightMm) {
                    c062NoNoteAreaOverLimitSeen = true
                    assertTrue(
                        entries.none { it.reason == OverflowReason.ATTACHMENT_OVER_LIMIT },
                        "repetition=$repetition：C-062 无笔记区时不得判定附加上限",
                    )
                }

                if (reason == null) {
                    assertTrue(
                        entries.isEmpty(),
                        "repetition=$repetition：${question.questionId} 可打印，不得出现在 overflowItems",
                    )
                    return@forEach
                }
                assertEquals(
                    1,
                    entries.size,
                    "repetition=$repetition：${question.questionId} 必须恰好一条 overflow 记录（C-061）",
                )
                assertEquals(
                    reason,
                    entries.single().reason,
                    "repetition=$repetition：${question.questionId} 的 overflow 原因",
                )
                assertEquals(
                    height,
                    entries.single().heightMm,
                    1e-9,
                    "repetition=$repetition：overflow 条目保持公式原值，不压缩",
                )
                assertEquals(date, entries.single().date)

                if (height > template.contentHeightMm &&
                    question.hasNoteArea &&
                    attachmentArea > template.maxAttachmentHeightMm
                ) {
                    c061BothConditionsSeen = true
                    assertEquals(
                        OverflowReason.ITEM_TOO_TALL,
                        entries.single().reason,
                        "repetition=$repetition：C-061 两条件同时成立时只报 ITEM_TOO_TALL",
                    )
                }
            }

            // 每页 marker 与“该日期内后续页的条目”一一对应。
            datePages.forEachIndexed { index, page ->
                val laterIds = datePages
                    .drop(index + 1)
                    .flatMap { laterPage -> laterPage.items.map { it.questionId } }
                assertEquals(
                    laterIds,
                    page.continuationMarkers.map { it.questionId },
                    "repetition=$repetition：$date 第 ${page.pageNumber} 页的 marker 必须与被移条目一一对应",
                )
                page.continuationMarkers.forEachIndexed { order, marker ->
                    assertEquals(page.pageId, marker.fromPageId, "repetition=$repetition：marker 来源页")
                    assertEquals(order, marker.order, "repetition=$repetition：marker order 按题目顺序从 0 递增")
                    val landing = datePages.first { candidate ->
                        candidate.items.any { it.questionId == marker.questionId }
                    }
                    assertEquals(
                        landing.pageId,
                        marker.toPageId,
                        "repetition=$repetition：marker 必须指向实际放置页",
                    )
                }
            }
        }

        // 条目几何：不重叠、不悬空、不越出内容区；高度等于公式原值。
        success.pages.forEach { page ->
            val capacity = template.contentHeightMm
            val start = template.topMarginMm + template.headerRowHeightMm
            var cursor = start
            page.items.forEach { item ->
                assertEquals(
                    cursor,
                    item.yOffsetMm,
                    1e-9,
                    "repetition=$repetition：条目必须紧接上一条，不得重叠或留空",
                )
                assertEquals(
                    expectedHeightMm(questionsById.getValue(item.questionId), template),
                    item.heightMm,
                    1e-9,
                    "repetition=$repetition：条目高度必须等于单题高度公式",
                )
                cursor += item.heightMm
            }
            assertTrue(
                cursor <= start + capacity + 1e-9,
                "repetition=$repetition：第 ${page.pageNumber} 页条目不得越过内容区下边界",
            )
        }

        // 贪心放置：前一页结束的下一条必须放不进前一页（不得跳过它去填更小的条目）。
        for (date in groups.keys.sorted()) {
            val datePages = success.pages.filter { it.date == date }
            for (index in 0 until datePages.size - 1) {
                val page = datePages[index]
                val nextFirst = datePages[index + 1].items.first()
                val usedHeight = page.items.sumOf { it.heightMm }
                assertTrue(
                    usedHeight + nextFirst.heightMm > template.contentHeightMm + 1e-9,
                    "repetition=$repetition：$date 第 ${page.pageNumber} 页结束过早或跳过了放不下的条目",
                )
            }
        }

        // 守恒与 warnings：每题恰好处理一次；overflow 不在 pages；warnings 一一对应且按 questionId 升序。
        val placedAll = success.pages.flatMap { page -> page.items.map { it.questionId } }
        val overflowAll = success.overflowItems.map { it.questionId }
        assertEquals(
            questionsById.keys,
            (placedAll + overflowAll).toSet(),
            "repetition=$repetition：每道题都必须恰好出现在 pages 或 overflowItems 中",
        )
        assertEquals(
            placedAll.size + overflowAll.size,
            (placedAll + overflowAll).toSet().size,
            "repetition=$repetition：条目不得被拆分或在多页重复出现",
        )
        assertTrue(
            success.overflowItems.none { it.questionId in placedAll },
            "repetition=$repetition：overflowItems 不得出现在 pages",
        )
        assertEquals(
            overflowAll.sorted(),
            success.warnings.map { it.questionId },
            "repetition=$repetition：warnings 按 questionId 升序且与 overflowItems 一一对应",
        )
        success.warnings.forEach { warning ->
            assertEquals(
                success.overflowItems.first { it.questionId == warning.questionId }.reason.name,
                warning.code.name,
                "repetition=$repetition：warning code 必须与 overflow 原因一致",
            )
        }
        assertEquals(
            groups.keys.sorted()
                .flatMap { date -> groups.getValue(date).map { it.questionId } }
                .filter { it in overflowAll },
            overflowAll,
            "repetition=$repetition：overflowItems 按日期升序、同日按输入题目顺序",
        )
    }

    @AfterAll
    fun `P-09 generator covers the adjudicated overflow branches`() {
        assertTrue(c060AllOverflowDaySeen, "P-09 生成器必须覆盖 C-060：整日条目全部溢出不建页")
        assertTrue(c061BothConditionsSeen, "P-09 生成器必须覆盖 C-061：两条件同时成立只输出一条 ITEM_TOO_TALL")
        assertTrue(c062NoNoteAreaOverLimitSeen, "P-09 生成器必须覆盖 C-062：无笔记区不判定附加上限")
    }

    private fun tightTemplate(attachmentHeightPerItemMm: Double): TemplateGeometry = TemplateGeometry(
        pageHeightMm = 120.0,
        topMarginMm = 10.0,
        bottomMarginMm = 10.0,
        headerRowHeightMm = 5.0,
        rowHeightMm = 5.0,
        questionFixedInfoHeightMm = 10.0,
        noteAreaBaseHeightMm = 20.0,
        attachmentHeightPerItemMm = attachmentHeightPerItemMm,
        itemSpacingMm = 5.0,
        qrSizeMm = 15.0,
        qrPaddingMm = 5.0,
    )

    private fun randomQuestion(id: String, random: Random): RenderableQuestion {
        val knowledgeCount = random.nextInt(1, 5)
        return RenderableQuestion(
            questionId = id,
            questionNumber = id,
            knowledgeCount = knowledgeCount,
            knowledgeNames = List(knowledgeCount) { "知识点${it + 1}" },
            knowledgeLineCount = random.nextInt(1, 5),
            attachmentCount = random.nextInt(0, 4),
            hasNoteArea = random.nextInt(100) < 75,
        )
    }

    /** 短页下必然整题过高：rightTop 30 + 笔记区 50 = 80，+20（QR）+5（间距）= 105mm > 95mm。 */
    private fun maxParameterQuestion(id: String): RenderableQuestion = RenderableQuestion(
        questionId = id,
        questionNumber = id,
        knowledgeCount = 4,
        knowledgeNames = List(4) { "知识点${it + 1}" },
        knowledgeLineCount = 4,
        attachmentCount = 3,
        hasNoteArea = true,
    )

    /** 短页 + 每条附件 15mm：高度 105mm > 95mm 且附件面积 45mm > 30mm，两条件同时成立。 */
    private fun bothConditionsQuestion(id: String): RenderableQuestion = RenderableQuestion(
        questionId = id,
        questionNumber = id,
        knowledgeCount = 1,
        knowledgeNames = listOf("知识点"),
        knowledgeLineCount = 1,
        attachmentCount = 3,
        hasNoteArea = true,
    )

    /** 短页 + 每条附件 15mm、无笔记区：附件 45mm > 30mm 但纸上没有附加笔记，上限不得判定。 */
    private fun noNoteAreaOverLimitQuestion(id: String): RenderableQuestion = RenderableQuestion(
        questionId = id,
        questionNumber = id,
        knowledgeCount = 1,
        knowledgeNames = listOf("知识点"),
        knowledgeLineCount = 1,
        attachmentCount = 3,
        hasNoteArea = false,
    )

    private fun compactQuestion(id: String): RenderableQuestion = RenderableQuestion(
        questionId = id,
        questionNumber = id,
        knowledgeCount = 1,
        knowledgeNames = listOf("知识点"),
        knowledgeLineCount = 1,
        attachmentCount = 0,
        hasNoteArea = true,
    )

    /** 独立按 pagination.md 的「单题高度公式」求期望高度，作为属性测试的判定基准。 */
    private fun expectedHeightMm(question: RenderableQuestion, template: TemplateGeometry): Double {
        val leftHeight = question.knowledgeCount * template.rowHeightMm
        val rightTop = template.questionFixedInfoHeightMm + question.knowledgeLineCount * template.rowHeightMm
        val attachmentArea = question.attachmentCount * template.attachmentHeightPerItemMm
        val rightBottom = if (question.hasNoteArea) {
            template.noteAreaBaseHeightMm + attachmentArea
        } else {
            0.0
        }
        return maxOf(leftHeight, rightTop + rightBottom) + template.qrFooterHeightMm + template.itemSpacingMm
    }

    /** C-061 方案 B：整题过高优先；附加上限只在打印笔记区时判定（C-062 方案 B）。 */
    private fun expectedOverflowReason(
        question: RenderableQuestion,
        heightMm: Double,
        template: TemplateGeometry,
    ): OverflowReason? = when {
        heightMm > template.contentHeightMm -> OverflowReason.ITEM_TOO_TALL
        question.hasNoteArea &&
            question.attachmentCount * template.attachmentHeightPerItemMm > template.maxAttachmentHeightMm ->
            OverflowReason.ATTACHMENT_OVER_LIMIT
        else -> null
    }

    private companion object {
        // 属性测试固定种子（规格验收门禁）；每次 repetition 用 SEED + 序号派生独立随机流。
        const val PROPERTY_SEED = 20261113L
    }
}
