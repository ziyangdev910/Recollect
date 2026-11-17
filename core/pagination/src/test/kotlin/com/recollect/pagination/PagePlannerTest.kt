package com.recollect.pagination

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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

    /**
     * P-08：跨多日（C-025）——每日从新页开始，pageNumber 全文档连续，
     * 同日多页 date 相同，日期升序；输出与输入 Map 的迭代顺序无关。
     *
     * 输入故意把 2026-11-18 放在 2026-11-16 之前。默认模板下每条 68.5mm：
     * 11-16 四条 → 首页 3 条（205.5 ≤ 258）、第 2 页 1 条；11-18 一条 → 第 3 页。
     */
    @Test
    fun `P-08 days start on new pages with continuous numbering and ascending dates`() {
        val firstDay = LocalDate.of(2026, 11, 16)
        val secondDay = LocalDate.of(2026, 11, 18)
        val firstDayQuestions = (1..4).map { compactQuestion("d1-q$it") }
        val secondDayQuestions = listOf(compactQuestion("d2-q1"))

        val result = DefaultPagePlanner().plan(
            planId = "plan-p08",
            dailyGroups = mapOf(
                secondDay to secondDayQuestions,
                firstDay to firstDayQuestions,
            ),
            template = TemplateGeometry(),
        )

        val success = assertIs<PaginationResult.Success>(result)
        assertEquals(
            listOf(1, 2, 3),
            success.pages.map { it.pageNumber },
            "pageNumber 必须从 1 起在全文档内连续递增",
        )
        assertEquals(
            listOf(firstDay, firstDay, secondDay),
            success.pages.map { it.date },
            "输出按日期升序；同一天的多页 date 相同",
        )
        assertEquals(
            listOf("plan-p08-01", "plan-p08-02", "plan-p08-03"),
            success.pages.map { it.pageId },
            "pageId = {planId}-{pageNumber:02d}",
        )
        assertEquals(
            listOf(
                listOf("d1-q1", "d1-q2", "d1-q3"),
                listOf("d1-q4"),
                listOf("d2-q1"),
            ),
            success.pages.map { page -> page.items.map { it.questionId } },
            "每个日期从新页开始，不得把后一天的条目拼到前一天页尾",
        )
        assertEquals(
            listOf("d1-q4"),
            success.pages[0].continuationMarkers.map { it.questionId },
            "同日跨页仍按 C-048 生成续下页标记",
        )
        assertTrue(success.overflowItems.isEmpty(), "默认高度 68.5mm 不应进入 overflowItems")
    }

    /**
     * P-10：首页内容区扣 legendHeightMm + headerRowHeightMm，后续页只扣 headerRowHeightMm（C-001、C-025）。
     *
     * 自定义模板：usable = 80、firstPageContent = 65、content = 75；每条高度 11mm。
     * 12 条同日：首页放 5 条（6 条 66 > 65），第 2 页放 6 条（7 条 77 > 75），第 3 页放 1 条。
     * 选 11mm 是为了让容量本身能区分“扣表头/不扣表头”：
     * 首页 65 放 5 条、去掉表头后的 70 会放 6 条；后续页 75 放 6 条、去掉表头后的 80 会放 7 条。
     * 首页首条 yOffset = 10 + 10 + 5 = 25；第 2 页首条 yOffset = 10 + 5 = 15（不扣图例）。
     */
    @Test
    fun `P-10 first page deducts legend and header while later pages deduct only header`() {
        val date = LocalDate.of(2026, 11, 19)
        val template = TemplateGeometry(
            pageHeightMm = 100.0,
            topMarginMm = 10.0,
            bottomMarginMm = 10.0,
            legendHeightMm = 10.0,
            headerRowHeightMm = 5.0,
            rowHeightMm = 2.0,
            questionFixedInfoHeightMm = 2.0,
            noteAreaBaseHeightMm = 1.0,
            itemSpacingMm = 1.0,
            qrSizeMm = 2.0,
            qrPaddingMm = 1.0,
        )
        val questions = (1..12).map {
            compactQuestion("p10-q$it", knowledgeCount = 2, knowledgeLineCount = 2)
        }

        val success = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan(
                planId = "plan-p10",
                dailyGroups = mapOf(date to questions),
                template = template,
            ),
        )

        assertEquals(3, success.pages.size, "首页 65mm 放 5 条，第 2 页 75mm 放 6 条，第 3 页放剩余 1 条")
        val firstPage = success.pages[0]
        assertEquals(
            (1..5).map { "p10-q$it" },
            firstPage.items.map { it.questionId },
            "首页内容区 = 80 - 10（图例）- 5（表头）= 65mm，只能放下 5 条 11mm 条目",
        )
        assertEquals(
            listOf(25.0, 36.0, 47.0, 58.0, 69.0),
            firstPage.items.map { it.yOffsetMm },
            "首页 yOffset = topMargin 10 + legend 10 + header 5 + 已放置高度",
        )

        val secondPage = success.pages[1]
        assertEquals(
            (6..11).map { "p10-q$it" },
            secondPage.items.map { it.questionId },
            "后续页内容区 = 80 - 5（表头）= 75mm，只能放下 6 条 11mm 条目",
        )
        assertEquals(
            listOf(15.0, 26.0, 37.0, 48.0, 59.0, 70.0),
            secondPage.items.map { it.yOffsetMm },
            "后续页 yOffset = topMargin 10 + header 5 + 已放置高度，不再扣图例",
        )

        val thirdPage = success.pages[2]
        assertEquals(
            listOf("p10-q12"),
            thirdPage.items.map { it.questionId },
            "剩余条目按顺序进入第 3 页",
        )
        assertEquals(
            listOf(15.0),
            thirdPage.items.map { it.yOffsetMm },
            "第 3 页同样只扣页级表头",
        )
        assertTrue(success.overflowItems.isEmpty(), "条目高度 11mm 远小于内容区，不应进入 overflowItems")
    }

    /**
     * P-13：条目能进后续页但进不了首页（首页扣了图例）时，首页仍存在、items 为空、
     * 恰好一个续下页标记；条目完整出现在下一页（C-025、C-048）。
     *
     * 自定义模板：usable = 80、firstPageContent = 65、content = 75；条目高度 70mm。
     * 70 > 65 且 70 ≤ 75：首页放不下，空白后续页放得下，必须保留首页承载图例。
     */
    @Test
    fun `P-13 item that only fits a later page keeps an empty first page with one marker`() {
        val date = LocalDate.of(2026, 11, 20)
        val template = TemplateGeometry(
            pageHeightMm = 100.0,
            topMarginMm = 10.0,
            bottomMarginMm = 10.0,
            legendHeightMm = 10.0,
            headerRowHeightMm = 5.0,
            rowHeightMm = 5.0,
            questionFixedInfoHeightMm = 10.0,
            noteAreaBaseHeightMm = 30.0,
            itemSpacingMm = 5.0,
            qrSizeMm = 15.0,
            qrPaddingMm = 5.0,
        )
        val question = RenderableQuestion(
            questionId = "p13-q1",
            questionNumber = "13-1",
            knowledgeCount = 1,
            knowledgeNames = listOf("知识点"),
            knowledgeLineCount = 1,
            attachmentCount = 0,
            hasNoteArea = true,
        )

        val success = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan(
                planId = "plan-p13",
                dailyGroups = mapOf(date to listOf(question)),
                template = template,
            ),
        )

        assertEquals(2, success.pages.size, "首页放不下、后续页放得下时必须保留首页并生成后续页")
        assertTrue(success.overflowItems.isEmpty(), "条目 70mm ≤ 后续页内容区 75mm，不得进入 overflowItems")

        val firstPage = success.pages[0]
        assertEquals("plan-p13-01", firstPage.pageId)
        assertEquals(1, firstPage.pageNumber)
        assertEquals(date, firstPage.date)
        assertTrue(firstPage.items.isEmpty(), "首页放不下条目时 items 必须为空，但页面仍存在以承载图例")
        assertEquals(
            listOf(
                ContinuationMarker(
                    fromPageId = "plan-p13-01",
                    toPageId = "plan-p13-02",
                    questionId = "p13-q1",
                    order = 0,
                ),
            ),
            firstPage.continuationMarkers,
            "首页必须恰好生成一个 order=0 的续下页标记，并指向实际放置页",
        )

        val secondPage = success.pages[1]
        assertEquals("plan-p13-02", secondPage.pageId)
        assertEquals(2, secondPage.pageNumber)
        assertEquals(date, secondPage.date)
        val item = secondPage.items.single()
        assertEquals("p13-q1", item.questionId)
        assertEquals(70.0, item.heightMm, 1e-9, "条目完整出现，不拆分、不压缩")
        assertEquals(
            15.0,
            item.yOffsetMm,
            1e-9,
            "后续页 yOffset = topMargin 10 + header 5，不扣首页图例",
        )
        assertTrue(secondPage.continuationMarkers.isEmpty(), "条目已在第 2 页放置，不应再有标记")
    }

    /**
     * P-14：单题高于 `contentHeightMm` → `OverflowItem(ITEM_TOO_TALL)`，不进 `pages`、不拆分、不压缩（C-016/C-025）。
     *
     * 自定义模板（默认几何不可达，见 [smallPageTemplate]）：usable = 100、content = 95、firstPageContent = 85、附件上限 30。
     * 过高条目（4 知识点 / 4 行名称 / 3 条附加）：max(4×5, 10+4×5+20+3×10) = 80；80 + 20（QR 底栏）+ 5（间距）= 105mm > 95。
     * 附件区 3×10 = 30 恰为上限，不触发 ATTACHMENT_OVER_LIMIT（P-11）。
     * 正常条目（1 知识点 / 1 行 / 无附加）：max(5, 10+5+20) + 20 + 5 = 60mm ≤ 首页 85mm，仍可落在第 1 页。
     */
    @Test
    fun `P-14 item taller than content height goes to overflow without split or compression`() {
        val date = LocalDate.of(2026, 11, 21)
        val template = smallPageTemplate()
        val tooTall = RenderableQuestion(
            questionId = "p14-q-tall",
            questionNumber = "14-1",
            knowledgeCount = 4,
            knowledgeNames = listOf("集合", "不等式", "函数", "导数"),
            knowledgeLineCount = 4,
            attachmentCount = 3,
            hasNoteArea = true,
        )
        val normal = compactQuestion("p14-q-normal")

        val success = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan(
                planId = "plan-p14",
                dailyGroups = mapOf(date to listOf(tooTall, normal)),
                template = template,
            ),
        )

        assertTrue(
            success.pages.none { page -> page.items.any { it.questionId == "p14-q-tall" } },
            "P-14：高于内容区的条目不得进入 pages（不拆分、不压缩）",
        )
        assertEquals(
            listOf(
                OverflowItem(
                    questionId = "p14-q-tall",
                    date = date,
                    heightMm = 105.0,
                    reason = OverflowReason.ITEM_TOO_TALL,
                ),
            ),
            success.overflowItems,
            "P-14：整条进入 ITEM_TOO_TALL，heightMm 保持公式原值（不压缩）",
        )
        assertEquals(
            listOf("p14-q-tall"),
            success.warnings.map { it.questionId },
            "overflow 条目必须同时给出含 questionId 的用户可读提示",
        )
        assertEquals(
            PaginationWarningCode.ITEM_TOO_TALL,
            success.warnings.single().code,
            "提示代码与 overflow 原因一致，且为稳定枚举（C-065 方案 B）",
        )
        assertTrue(
            success.warnings.single().message.contains("减少本题关联的知识点"),
            "C-066：整题过高应提示减少关联知识点",
        )
        assertTrue(
            success.warnings.single().message.contains("缩短知识点描述"),
            "C-066：整题过高应提示缩短知识点描述",
        )
        assertFalse(
            success.warnings.single().message.contains("拆分题目"),
            "C-066：整题过高不得再建议拆分题目",
        )

        val page = success.pages.single()
        assertEquals(
            listOf("p14-q-normal"),
            page.items.map { it.questionId },
            "P-14：过高条目被移出后，其余条目仍按原顺序正常分页",
        )
        assertEquals(60.0, page.items.single().heightMm, 1e-9, "正常条目高度不变")
        assertEquals(
            25.0,
            page.items.single().yOffsetMm,
            1e-9,
            "首页 yOffset = topMargin 10 + legend 10 + header 5，过高条目未占据任何位置",
        )
        assertTrue(
            page.continuationMarkers.isEmpty(),
            "P-14：过高条目进入 overflow 时不生成续下页标记（C-048）",
        )
    }

    /**
     * P-18：`itemHeight` 落在 `(contentHeightMm, usableHeightMm]` 时必须进入 `ITEM_TOO_TALL`，
     * 不得出现「既放不下又不在 overflow」的区间（C-016/C-025）。
     *
     * 用同一个自定义模板（见 [smallPageTemplate]）把区间两端都钉死：
     * - 恰好等于 contentHeightMm 的 95mm 条目（4 知识点 / 4 行 / 2 条附加）：
     *   max(20, 10+20+20+20) = 70；70 + 20 + 5 = 95mm；按严格「>」判定不得进入 overflow，
     *   首页 85mm 放不下时按 P-13 移到第 2 页（容量恰为 95mm）。
     * - 恰好等于 usableHeightMm 的 100mm 条目（3 知识点 / 3 行 / 3 条附加）：
     *   max(15, 10+15+20+30) = 75；75 + 20 + 5 = 100mm；必须进入 ITEM_TOO_TALL。
     */
    @Test
    fun `P-18 item above content height but within usable height still goes to overflow`() {
        val date = LocalDate.of(2026, 11, 22)
        val template = smallPageTemplate()
        val contentHeightMm = 95.0
        val usableHeightMm = 100.0
        assertEquals(template.contentHeightMm, contentHeightMm, 1e-9, "前置条件：内容区高度")
        assertEquals(template.usableHeightMm, usableHeightMm, 1e-9, "前置条件：整页可用高度")

        val normal = compactQuestion("p18-q-normal")
        val exactContent = RenderableQuestion(
            questionId = "p18-q-exact-content",
            questionNumber = "18-1",
            knowledgeCount = 4,
            knowledgeNames = listOf("集合", "不等式", "函数", "导数"),
            knowledgeLineCount = 4,
            attachmentCount = 2,
            hasNoteArea = true,
        )
        val exactUsable = RenderableQuestion(
            questionId = "p18-q-exact-usable",
            questionNumber = "18-2",
            knowledgeCount = 3,
            knowledgeNames = listOf("集合", "不等式", "函数"),
            knowledgeLineCount = 3,
            attachmentCount = 3,
            hasNoteArea = true,
        )

        val success = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan(
                planId = "plan-p18",
                dailyGroups = mapOf(date to listOf(normal, exactContent, exactUsable)),
                template = template,
            ),
        )

        assertEquals(
            listOf(
                OverflowItem(
                    questionId = "p18-q-exact-usable",
                    date = date,
                    heightMm = usableHeightMm,
                    reason = OverflowReason.ITEM_TOO_TALL,
                ),
            ),
            success.overflowItems,
            "P-18：恰好等于 usableHeightMm 的条目必须进入 overflowItems；等于 contentHeightMm 的不得进入",
        )
        assertTrue(
            success.pages.none { page -> page.items.any { it.questionId == "p18-q-exact-usable" } },
            "P-18：不得出现既放不进 pages 又不在 overflowItems 的条目",
        )
        assertEquals(
            listOf("p18-q-exact-usable"),
            success.warnings.map { it.questionId },
            "overflow 条目必须同时给出用户可读提示",
        )

        assertEquals(2, success.pages.size, "95mm 条目首页放不下但后续页放得下，应保留首页并续下页")
        val firstPage = success.pages[0]
        assertEquals(
            listOf("p18-q-normal"),
            firstPage.items.map { it.questionId },
            "正常条目不受影响，仍完整落在首页",
        )
        assertEquals(
            25.0,
            firstPage.items.single().yOffsetMm,
            1e-9,
            "首页 yOffset = topMargin 10 + legend 10 + header 5",
        )
        assertEquals(
            listOf(
                ContinuationMarker(
                    fromPageId = "plan-p18-01",
                    toPageId = "plan-p18-02",
                    questionId = "p18-q-exact-content",
                    order = 0,
                ),
            ),
            firstPage.continuationMarkers,
            "只有等于 contentHeightMm 的条目被移页；进入 overflow 的条目不生成 marker",
        )

        val secondPage = success.pages[1]
        assertEquals(
            listOf("p18-q-exact-content"),
            secondPage.items.map { it.questionId },
            "恰好等于 contentHeightMm 的条目可完整放在后续页",
        )
        assertEquals(
            contentHeightMm,
            secondPage.items.single().heightMm,
            1e-9,
            "P-18：区间下界是开区间，等于 contentHeightMm 时不进入 overflow、高度不变",
        )
        assertEquals(
            15.0,
            secondPage.items.single().yOffsetMm,
            1e-9,
            "后续页 yOffset = topMargin 10 + header 5",
        )
        assertTrue(secondPage.continuationMarkers.isEmpty(), "条目已在第 2 页放置，不应再有标记")
    }

    /**
     * C-060（已裁决：方案 B）：某日条目全部高于 `contentHeightMm` 时，分页器不产生任何页面
     * （包括仅含图例的空白页），只返回真实的打印页面与 overflow 列表；
     * 「整份计划没有可打印题目」的空态由计划层负责提示。
     *
     * 两个日期：11-23 只有一条 105mm 过高条目；11-24 一条 60mm 正常条目。
     * 预期只剩 11-24 的页面，且它是文档第 1 页（空白页不占页号，首页图例仍扣在第 1 页）。
     */
    @Test
    fun `C-060 day whose items all overflow produces no blank page`() {
        val tallDay = LocalDate.of(2026, 11, 23)
        val normalDay = LocalDate.of(2026, 11, 24)
        val template = smallPageTemplate()
        val tooTall = RenderableQuestion(
            questionId = "c060-q-tall",
            questionNumber = "60-1",
            knowledgeCount = 4,
            knowledgeNames = listOf("集合", "不等式", "函数", "导数"),
            knowledgeLineCount = 4,
            attachmentCount = 3,
            hasNoteArea = true,
        )

        val success = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan(
                planId = "plan-c060",
                dailyGroups = mapOf(tallDay to listOf(tooTall), normalDay to listOf(compactQuestion("c060-q-normal"))),
                template = template,
            ),
        )

        assertEquals(
            listOf(
                OverflowItem(
                    questionId = "c060-q-tall",
                    date = tallDay,
                    heightMm = 105.0,
                    reason = OverflowReason.ITEM_TOO_TALL,
                ),
            ),
            success.overflowItems,
            "过高条目仍以 overflow 形式返回，供计划层提示用户",
        )
        assertEquals(
            listOf(normalDay),
            success.pages.map { it.date },
            "C-060 方案 B：全部过高的日期不产生页面，不输出仅含图例的空白页",
        )
        val page = success.pages.single()
        assertEquals("plan-c060-01", page.pageId, "空白页不占页号，正常日期的页面从第 1 页起")
        assertEquals(1, page.pageNumber)
        assertEquals(listOf("c060-q-normal"), page.items.map { it.questionId })
        assertEquals(
            25.0,
            page.items.single().yOffsetMm,
            1e-9,
            "该页是文档第 1 页，仍扣 topMargin 10 + legend 10 + header 5",
        )
        assertTrue(page.continuationMarkers.isEmpty(), "过高条目不生成续下页标记")
    }

    /**
     * P-05：附加 1/2/3 条时右下笔记区随附加面积递增；附加面积超过 `usableHeightMm × 30%` 时
     * 整条进入 `overflowItems(ATTACHMENT_OVER_LIMIT)`，高度保持公式原值、不截断附件区（C-027）。
     *
     * 默认模板每条附件 10mm、上限 81.9mm，3 条也只有 30mm，永远触不到上限，因此必须自定义模板：
     * pageHeight=250、上下边距 10 → usable=230、附件上限=230×0.30=69mm；content=225、firstPageContent=215。
     * attachmentHeightPerItemMm=24 → 1 条 24、2 条 48 均在上限内，3 条 72 超限。
     * 条目高度 rightTop=10+5=15、noteBase=20：
     * - 1 条：rightBottom=20+24=44，max(5, 59)+20(QR)+5(间距) = 84mm；
     * - 2 条：rightBottom=20+48=68，max(5, 83)+25 = 108mm；
     * - 3 条：rightBottom=20+72=92，max(5, 107)+25 = 132mm（132 ≤ 225，本可放下，只因附件超限被移出）。
     * 前两条 84+108=192 ≤ 首页 215mm，落在首页；第 3 条整条不打印。
     */
    @Test
    fun `P-05 attachment area grows with count and over-limit item goes to ATTACHMENT_OVER_LIMIT`() {
        val date = LocalDate.of(2026, 11, 25)
        val template = TemplateGeometry(
            pageHeightMm = 250.0,
            topMarginMm = 10.0,
            bottomMarginMm = 10.0,
            legendHeightMm = 10.0,
            headerRowHeightMm = 5.0,
            rowHeightMm = 5.0,
            questionFixedInfoHeightMm = 10.0,
            noteAreaBaseHeightMm = 20.0,
            attachmentHeightPerItemMm = 24.0,
            itemSpacingMm = 5.0,
            qrSizeMm = 15.0,
            qrPaddingMm = 5.0,
        )
        assertEquals(230.0, template.usableHeightMm, 1e-9, "前置条件：可用高度 230mm")
        assertEquals(69.0, template.maxAttachmentHeightMm, 1e-9, "前置条件：附件上限 = 230 × 0.30")
        val questions = (1..3).map { count ->
            RenderableQuestion(
                questionId = "p05-q$count",
                questionNumber = "5-$count",
                knowledgeCount = 1,
                knowledgeNames = listOf("知识点"),
                knowledgeLineCount = 1,
                attachmentCount = count,
                hasNoteArea = true,
            )
        }

        val success = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan(
                planId = "plan-p05",
                dailyGroups = mapOf(date to questions),
                template = template,
            ),
        )

        assertEquals(
            listOf(
                OverflowItem(
                    questionId = "p05-q3",
                    date = date,
                    heightMm = 132.0,
                    reason = OverflowReason.ATTACHMENT_OVER_LIMIT,
                ),
            ),
            success.overflowItems,
            "附件面积 3×24=72mm 超过上限 69mm 时整条进入 ATTACHMENT_OVER_LIMIT；" +
                "heightMm 仍是含全部附加区的公式原值，不截断",
        )
        assertEquals(
            listOf("p05-q3"),
            success.warnings.map { it.questionId },
            "附件超限的条目必须同时给出含 questionId 的用户可读提示",
        )
        assertEquals(
            PaginationWarningCode.ATTACHMENT_OVER_LIMIT,
            success.warnings.single().code,
            "提示代码与被移出打印的原因一致（C-065 方案 B）",
        )
        assertTrue(
            success.warnings.single().message.contains("减少附加笔记"),
            "附加超限应提示减少附加笔记",
        )
        assertFalse(
            success.warnings.single().message.contains("拆分题目"),
            "C-066：附加超限提示不得建议拆分题目",
        )

        val page = success.pages.single()
        assertEquals(
            listOf("p05-q1", "p05-q2"),
            page.items.map { it.questionId },
            "1/2 条附件的条目仍按原顺序打印；第 3 条整条移出，不得截断附加区后塞进页面",
        )
        assertEquals(
            listOf(84.0, 108.0),
            page.items.map { it.heightMm },
            "P-05：附加 1→2 条时条目高度按每条 24mm 递增（84→108），与超限条目 132 共同体现 rightBottom 递增",
        )
        assertEquals(
            listOf(25.0, 109.0),
            page.items.map { it.yOffsetMm },
            "首页 yOffset = topMargin 10 + legend 10 + header 5 + 已放置高度；超限条目不占位置",
        )
        assertTrue(
            page.continuationMarkers.isEmpty(),
            "进入 overflow 的条目不生成续下页标记（C-048）",
        )
    }

    /**
     * P-11：附加区 30% 上限的边界——恰好 30% 通过；超过 1mm 进入 `ATTACHMENT_OVER_LIMIT`；不截断附件区。
     *
     * 默认模板上限 81.9mm，3 条附件也只有 30mm，不可达，因此必须自定义模板：
     * pageHeight=120、上下边距 10 → usable=100、附件上限=100×0.30=30mm；content=95、firstPageContent=85。
     * 两条附带条目（rightTop=10+5=15、noteBase=10）：
     * - 模板 A：每条 15mm、2 条 = 30mm，恰好等于上限 → 必须打印，heightMm=15+10+30+20+5=80mm；
     * - 模板 B：每条 15.5mm、2 条 = 31mm，超过 1mm → 整条 overflow；
     *   heightMm 仍是含 31mm 附加区的公式原值 81mm（若截断附件区到 30mm 会得到 80mm）。
     * 两条 80/81mm 都 ≤ 首页 85mm，即模板 B 的条目只因附件上限被移出，不是因为放不下。
     */
    @Test
    fun `P-11 attachment area at exactly 30 percent passes and one millimetre over overflows`() {
        val date = LocalDate.of(2026, 11, 26)
        val atLimit = attachmentBoundaryTemplate(perItemMm = 15.0)
        val overLimit = attachmentBoundaryTemplate(perItemMm = 15.5)
        assertEquals(100.0, atLimit.usableHeightMm, 1e-9, "前置条件：可用高度 100mm")
        assertEquals(30.0, atLimit.maxAttachmentHeightMm, 1e-9, "前置条件：附件上限 = 30mm")
        val question = RenderableQuestion(
            questionId = "p11-q1",
            questionNumber = "11-1",
            knowledgeCount = 1,
            knowledgeNames = listOf("知识点"),
            knowledgeLineCount = 1,
            attachmentCount = 2,
            hasNoteArea = true,
        )

        val passing = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan(
                planId = "plan-p11-pass",
                dailyGroups = mapOf(date to listOf(question)),
                template = atLimit,
            ),
        )
        assertTrue(
            passing.overflowItems.isEmpty(),
            "P-11：附加区恰好等于 usableHeightMm × 30%（30mm）时必须通过，不得进入 overflowItems",
        )
        assertTrue(passing.warnings.isEmpty(), "恰好 30% 不应产生警告")
        val placed = passing.pages.single().items.single()
        assertEquals("p11-q1", placed.questionId)
        assertEquals(
            80.0,
            placed.heightMm,
            1e-9,
            "rightTop 15 + 笔记区 10+30 + QR 底栏 20 + 间距 5 = 80mm，附加区全额计入",
        )
        assertEquals(25.0, placed.yOffsetMm, 1e-9, "首页 yOffset = topMargin 10 + legend 10 + header 5")

        val rejected = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan(
                planId = "plan-p11-over",
                dailyGroups = mapOf(date to listOf(question)),
                template = overLimit,
            ),
        )
        assertEquals(
            listOf(
                OverflowItem(
                    questionId = "p11-q1",
                    date = date,
                    heightMm = 81.0,
                    reason = OverflowReason.ATTACHMENT_OVER_LIMIT,
                ),
            ),
            rejected.overflowItems,
            "P-11：附加区 31mm 超过上限 1mm 即整条进入 ATTACHMENT_OVER_LIMIT；" +
                "heightMm=81 保留含 31mm 附加区的原值，不得截断成 80",
        )
        assertTrue(
            rejected.pages.isEmpty(),
            "C-060：该日唯一条目进入 overflow 时不建页，也不输出仅含图例的空白页",
        )
        assertEquals(
            PaginationWarningCode.ATTACHMENT_OVER_LIMIT,
            rejected.warnings.single().code,
            "超过 1mm 时提示代码与 overflow 原因一致（C-065 方案 B）",
        )
    }

    /**
     * C-061（已裁决：方案 B）：附加面积超限与整题过高同时成立时，按“整题过高”处理，
     * 提示按 C-066 引导减少关联知识点/缩短描述，而不是建议拆分题目。
     *
     * 复用 P-14/P-18 的短页模板（usable=100、content=95、附件上限=30），把每条附件改为 15mm：
     * 3 条附件面积 = 45mm > 30mm；条目高度 rightTop=15 + 笔记区 20+45 = 80，+20(QR)+5(间距) = 105mm > 95mm。
     * 两个条件同时成立，必须归为 ITEM_TOO_TALL，且 heightMm 保持公式原值。
     */
    @Test
    fun `C-061 item both too tall and over attachment limit is reported as ITEM_TOO_TALL`() {
        val date = LocalDate.of(2026, 11, 27)
        val template = smallPageTemplate().copy(attachmentHeightPerItemMm = 15.0)
        assertEquals(30.0, template.maxAttachmentHeightMm, 1e-9, "前置条件：附件上限 30mm")
        val question = RenderableQuestion(
            questionId = "c061-q1",
            questionNumber = "61-1",
            knowledgeCount = 1,
            knowledgeNames = listOf("知识点"),
            knowledgeLineCount = 1,
            attachmentCount = 3,
            hasNoteArea = true,
        )

        val success = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan(
                planId = "plan-c061",
                dailyGroups = mapOf(date to listOf(question)),
                template = template,
            ),
        )

        assertEquals(
            listOf(
                OverflowItem(
                    questionId = "c061-q1",
                    date = date,
                    heightMm = 105.0,
                    reason = OverflowReason.ITEM_TOO_TALL,
                ),
            ),
            success.overflowItems,
            "C-061 方案 B：附加区 45mm 超限且整题 105mm 过高时，必须归为 ITEM_TOO_TALL",
        )
        assertEquals(
            PaginationWarningCode.ITEM_TOO_TALL,
            success.warnings.single().code,
            "提示代码必须与裁决一致（C-065 方案 B）",
        )
        assertTrue(
            success.warnings.single().message.contains("减少本题关联的知识点"),
            "C-066：双条件成立时仍提示减少关联知识点",
        )
        assertFalse(
            success.warnings.single().message.contains("拆分题目"),
            "C-066：双条件成立时也不得建议拆分题目",
        )
        assertTrue(success.pages.isEmpty(), "整日唯一条目被移出时不建页（C-060）")
    }

    /**
     * C-062（已裁决：方案 B）：附加笔记上限只在打印笔记书写区时判定；
     * `hasNoteArea=false` 时纸上没有笔记区与附加笔记，附加数量不再导致整题移出。
     *
     * 复用 P-11 的边界模板：usable=100、附件上限=30mm、每条附件 15.5mm，2 条 = 31mm 超过 1mm。
     * - 打印笔记区（11-28）：rightBottom=10+31=41，height=15+41+20+5=81mm ≤ 95，但因附件超限进入 overflow；
     * - 不打印笔记区（11-29）：rightBottom=0，height=max(5, 15)+20+5=40mm，必须正常打印。
     * 11-28 全部条目溢出 → 不建页（C-060）；11-29 因此成为文档第 1 页，首页扣图例与表头。
     */
    @Test
    fun `C-062 attachment limit applies only when the note area is printed`() {
        val withNoteDate = LocalDate.of(2026, 11, 28)
        val withoutNoteDate = LocalDate.of(2026, 11, 29)
        val template = attachmentBoundaryTemplate(perItemMm = 15.5)
        fun question(id: String, hasNoteArea: Boolean) = RenderableQuestion(
            questionId = id,
            questionNumber = id,
            knowledgeCount = 1,
            knowledgeNames = listOf("知识点"),
            knowledgeLineCount = 1,
            attachmentCount = 2,
            hasNoteArea = hasNoteArea,
        )

        val success = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan(
                planId = "plan-c062",
                dailyGroups = mapOf(
                    withNoteDate to listOf(question("c062-q-note", hasNoteArea = true)),
                    withoutNoteDate to listOf(question("c062-q-no-note", hasNoteArea = false)),
                ),
                template = template,
            ),
        )

        assertEquals(
            listOf(
                OverflowItem(
                    questionId = "c062-q-note",
                    date = withNoteDate,
                    heightMm = 81.0,
                    reason = OverflowReason.ATTACHMENT_OVER_LIMIT,
                ),
            ),
            success.overflowItems,
            "C-062 方案 B：打印笔记区时附加上限仍生效；不打印笔记区时同一附加数量不得移出题目",
        )
        assertEquals(
            listOf("c062-q-note"),
            success.warnings.map { it.questionId },
            "只有打印笔记区且附件超限的条目需要提示减少附加笔记",
        )

        val page = success.pages.single()
        assertEquals(withoutNoteDate, page.date, "无笔记区且附件超限的条目必须正常建页")
        assertEquals("plan-c062-01", page.pageId)
        assertEquals(1, page.pageNumber)
        val item = page.items.single()
        assertEquals("c062-q-no-note", item.questionId)
        assertEquals(
            40.0,
            item.heightMm,
            1e-9,
            "C-062 方案 B：hasNoteArea=false 时 rightBottom=0，附加区不参与高度也不触发上限",
        )
        assertEquals(
            25.0,
            item.yOffsetMm,
            1e-9,
            "该页是文档第 1 页，仍扣 topMargin 10 + legend 10 + header 5",
        )
        assertTrue(page.continuationMarkers.isEmpty(), "条目未被移页，不应生成续下页标记")
    }

    /**
     * P-02：一日多条恰好填满内容区 → 无 overflow、无跨页、无续下页标记。
     *
     * 默认模板首页内容区 = 273（可用）- 8（图例）- 7（页级表头）= 258mm。
     * 三条高度（含 QR 底栏 21 与间距 3）：
     * - 2 行名称：61 + 2×7.5 = 76mm；
     * - 3 行名称：61 + 3×7.5 = 83.5mm；
     * - 1 行名称 + 3 条附加：61 + 7.5 + 30 = 98.5mm。
     * 76 + 83.5 + 98.5 = 258.0mm，恰好等于首页内容区：最后一条底边 =
     * 27 + 258 = 285 = 297 - 12（下页边距），不得再触发一次“放不下”判定。
     */
    @Test
    fun `P-02 items exactly filling the content area stay on one page without markers`() {
        val date = LocalDate.of(2026, 12, 1)
        val template = TemplateGeometry()
        val questions = listOf(
            RenderableQuestion(
                questionId = "p02-q1",
                questionNumber = "2-1",
                knowledgeCount = 1,
                knowledgeNames = listOf("一元二次方程"),
                knowledgeLineCount = 2,
                attachmentCount = 0,
                hasNoteArea = true,
            ),
            RenderableQuestion(
                questionId = "p02-q2",
                questionNumber = "2-2",
                knowledgeCount = 1,
                knowledgeNames = listOf("判别式"),
                knowledgeLineCount = 3,
                attachmentCount = 0,
                hasNoteArea = true,
            ),
            RenderableQuestion(
                questionId = "p02-q3",
                questionNumber = "2-3",
                knowledgeCount = 1,
                knowledgeNames = listOf("韦达定理"),
                knowledgeLineCount = 1,
                attachmentCount = 3,
                hasNoteArea = true,
            ),
        )
        assertEquals(258.0, template.firstPageContentHeightMm, 1e-9, "前置条件：首页内容区高度")

        val success = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan(
                planId = "plan-p02",
                dailyGroups = mapOf(date to questions),
                template = template,
            ),
        )

        val page = success.pages.single()
        assertEquals(1, page.pageNumber)
        assertEquals(date, page.date)
        assertEquals(
            listOf("p02-q1", "p02-q2", "p02-q3"),
            page.items.map { it.questionId },
            "恰好填满时必须三条都留在同一页，不得把最后一条判为放不下",
        )
        assertEquals(
            listOf(76.0, 83.5, 98.5),
            page.items.map { it.heightMm },
            "P-02：三条高度之和 258.0mm 恰好等于首页内容区",
        )
        assertEquals(
            listOf(27.0, 103.0, 186.5),
            page.items.map { it.yOffsetMm },
            "yOffsetMm = topMargin 12 + legend 8 + header 7 + 已放置高度",
        )
        assertEquals(
            285.0,
            page.items.last().yOffsetMm + page.items.last().heightMm,
            1e-9,
            "最后一条底边正好落在下页边距上（297 - 12），不越界、不跨页",
        )
        assertTrue(
            page.continuationMarkers.isEmpty(),
            "P-02：没有任何条目被移到后续页，不应生成续下页标记",
        )
        assertTrue(success.overflowItems.isEmpty(), "P-02：不得进入 overflowItems")
        assertTrue(success.warnings.isEmpty(), "P-02：不得产生 warnings")
    }

    /**
     * P-12：输入 Map 乱序 → 输出按日期升序，与输入迭代顺序无关（C-025）。
     *
     * 三个日期按 **逆序** 插入 `LinkedHashMap`（12-04 → 12-02 → 12-03），
     * 12-02 有 4 条 68.5mm 条目（首页 3 条、第 2 页 1 条），12-03 一条，12-04 两条。
     * 除断言页序外，再与“按升序插入同一内容”的规划结果整体比较：
     * `PaginationResult.Success` 是 data class，任何字段差异（页、marker、overflow、warnings）都会暴露。
     */
    @Test
    fun `P-12 input map iteration order does not change ascending date output`() {
        val dayA = LocalDate.of(2026, 12, 2)
        val dayB = LocalDate.of(2026, 12, 3)
        val dayC = LocalDate.of(2026, 12, 4)
        val dayAQuestions = (1..4).map { compactQuestion("p12-a$it") }
        val dayBQuestions = listOf(compactQuestion("p12-b1"))
        val dayCQuestions = (1..2).map { compactQuestion("p12-c$it") }

        val shuffledInput = linkedMapOf(
            dayC to dayCQuestions,
            dayA to dayAQuestions,
            dayB to dayBQuestions,
        )
        val sortedInput = linkedMapOf(
            dayA to dayAQuestions,
            dayB to dayBQuestions,
            dayC to dayCQuestions,
        )
        val planner = DefaultPagePlanner()

        val shuffled = assertIs<PaginationResult.Success>(
            planner.plan(planId = "plan-p12", dailyGroups = shuffledInput, template = TemplateGeometry()),
        )
        val sorted = assertIs<PaginationResult.Success>(
            planner.plan(planId = "plan-p12", dailyGroups = sortedInput, template = TemplateGeometry()),
        )

        assertEquals(
            sorted,
            shuffled,
            "P-12：输出必须与输入 Map 的迭代顺序完全无关（页、marker、overflow、warnings 全部一致）",
        )
        assertEquals(
            listOf(dayA, dayA, dayB, dayC),
            shuffled.pages.map { it.date },
            "P-12：日期必须升序，不得按输入插入顺序（12-04 → 12-02 → 12-03）输出",
        )
        assertEquals(
            listOf(1, 2, 3, 4),
            shuffled.pages.map { it.pageNumber },
            "页码在整份文档内按日期升序连续递增",
        )
        assertEquals(
            listOf("plan-p12-01", "plan-p12-02", "plan-p12-03", "plan-p12-04"),
            shuffled.pages.map { it.pageId },
            "pageId 必须跟随升序后的页码",
        )
        assertEquals(
            listOf(
                listOf("p12-a1", "p12-a2", "p12-a3"),
                listOf("p12-a4"),
                listOf("p12-b1"),
                listOf("p12-c1", "p12-c2"),
            ),
            shuffled.pages.map { page -> page.items.map { it.questionId } },
            "每个日期从新页开始，同日跨页仍连续",
        )
        assertEquals(
            listOf("p12-a4"),
            shuffled.pages[0].continuationMarkers.map { it.questionId },
            "乱序输入下同日续下页标记仍只出现在 12-02 的首页",
        )
        assertTrue(shuffled.overflowItems.isEmpty(), "这些条目均低于内容区，不应进入 overflowItems")
    }

    /**
     * P-15：`hasNoteArea=false` → `rightBottom=0`，附加数量不计入高度；附加上限也不判定
     * （C-062 方案 B）；附加数量不得被静默清零或丢弃。
     *
     * 默认模板、同一题、同样 2 条附加（每条 10mm）：
     * - 打印笔记区：rightBottom = 30 + 20 = 50，高度 = 14.5 + 50 + 21 + 3 = 88.5mm；
     * - 不打印笔记区：rightBottom = 0，高度 = max(7.5, 14.5) + 21 + 3 = 38.5mm，
     *   与“0 条附加”的紧凑题同高，证明 2 条附加没有计入高度。
     * 再用 P-11/C-062 的边界模板（usable=100、上限 30mm、每条 15.5mm）锁定上限不判定：
     * 2 条附加 = 31mm > 30mm，但无笔记区时条目仍须完整打印（高度 40mm），不得进入 overflow。
     */
    @Test
    fun `P-15 no note area zeroes right bottom and skips attachment limit`() {
        val date = LocalDate.of(2026, 12, 5)
        val template = TemplateGeometry()
        fun question(id: String, hasNoteArea: Boolean) = RenderableQuestion(
            questionId = id,
            questionNumber = id,
            knowledgeCount = 1,
            knowledgeNames = listOf("知识点"),
            knowledgeLineCount = 1,
            attachmentCount = 2,
            hasNoteArea = hasNoteArea,
        )

        val success = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan(
                planId = "plan-p15",
                dailyGroups = mapOf(date to listOf(question("p15-q-note", true), question("p15-q-no-note", false))),
                template = template,
            ),
        )

        assertEquals(2, success.pages.single().items.size, "两条都应落在首页，不得因无笔记区被丢弃")
        assertEquals(
            listOf(88.5, 38.5),
            success.pages.single().items.map { it.heightMm },
            "P-15：无笔记区时 rightBottom=0；38.5mm 与 0 条附加的紧凑题同高，证明附加数量未计入高度",
        )
        assertEquals(
            listOf(27.0, 115.5),
            success.pages.single().items.map { it.yOffsetMm },
            "按 88.5mm 递推：27 + 88.5 = 115.5",
        )
        assertTrue(success.overflowItems.isEmpty(), "默认模板下附件 20mm 远低于上限，不应进入 overflowItems")
        assertTrue(success.warnings.isEmpty(), "无笔记区的条目不应产生附件上限提示")

        // 无笔记区 + 附件面积超限：上限不判定，条目必须完整打印（C-062 方案 B）。
        val limitTemplate = attachmentBoundaryTemplate(perItemMm = 15.5)
        assertEquals(30.0, limitTemplate.maxAttachmentHeightMm, 1e-9, "前置条件：附件上限 30mm")
        val overLimitButNoNote = question("p15-q-over-limit", hasNoteArea = false)

        val noLimitSuccess = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan(
                planId = "plan-p15-limit",
                dailyGroups = mapOf(date to listOf(overLimitButNoNote)),
                template = limitTemplate,
            ),
        )

        assertEquals(
            2,
            overLimitButNoNote.attachmentCount,
            "P-15：附加数量仍为 2，不得被静默清零",
        )
        assertTrue(
            noLimitSuccess.overflowItems.isEmpty(),
            "P-15：2 × 15.5 = 31mm 虽超过 30mm 上限，但无笔记区时不得判定附加上限",
        )
        assertEquals(
            40.0,
            noLimitSuccess.pages.single().items.single().heightMm,
            1e-9,
            "无笔记区时高度 = rightTop 15 + QR 底栏 20 + 间距 5 = 40mm，附加区不参与",
        )
        assertEquals(
            25.0,
            noLimitSuccess.pages.single().items.single().yOffsetMm,
            1e-9,
            "首页 yOffset = topMargin 10 + legend 10 + header 5",
        )
        assertTrue(noLimitSuccess.warnings.isEmpty(), "无笔记区时不应提示减少附加笔记")
    }

    /**
     * P-16：`knowledgeLineCount` 1..4 → `rightTop` 按行数递增，不按 `knowledgeCount` 递增。
     *
     * 默认行高 7.5mm、右上第一行 7mm、笔记区 30mm、QR 底栏 21mm、间距 3mm。
     * 用加高页面模板（pageHeight=600，其余默认）让 7 条同处首页，避免续下页干扰高度断言：
     * - 行数序列（knowledgeCount 固定 4，lines=1..4）：61 + 7.5×lines = 68.5 / 76 / 83.5 / 91mm；
     * - 知识点数序列（lines 固定 1，knowledgeCount=2..4）：左侧最高 4×7.5=30mm，
     *   始终低于右侧 14.5 + 30 = 44.5mm，高度恒为 68.5mm。
     */
    @Test
    fun `P-16 height follows knowledge line count not knowledge count`() {
        val date = LocalDate.of(2026, 12, 7)
        val template = TemplateGeometry().copy(pageHeightMm = 600.0)
        assertEquals(561.0, template.firstPageContentHeightMm, 1e-9, "前置条件：加高首页内容区")
        val lineSeries = (1..4).map { lines ->
            RenderableQuestion(
                questionId = "p16-l$lines",
                questionNumber = "16-$lines",
                knowledgeCount = 4,
                knowledgeNames = List(4) { "知识点${it + 1}" },
                knowledgeLineCount = lines,
                attachmentCount = 0,
                hasNoteArea = true,
            )
        }
        val countSeries = (2..4).map { count ->
            RenderableQuestion(
                questionId = "p16-k$count",
                questionNumber = "16-k$count",
                knowledgeCount = count,
                knowledgeNames = List(count) { "知识点${it + 1}" },
                knowledgeLineCount = 1,
                attachmentCount = 0,
                hasNoteArea = true,
            )
        }

        val success = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan(
                planId = "plan-p16",
                dailyGroups = mapOf(date to lineSeries + countSeries),
                template = template,
            ),
        )

        val page = success.pages.single()
        assertEquals(
            listOf(68.5, 76.0, 83.5, 91.0),
            page.items.take(4).map { it.heightMm },
            "P-16：knowledgeCount 固定 4 时，高度仍按 knowledgeLineCount 每行递增 7.5mm",
        )
        assertEquals(
            listOf(68.5, 68.5, 68.5),
            page.items.drop(4).map { it.heightMm },
            "P-16：knowledgeLineCount 固定 1 时，knowledgeCount 2→4 不得改变高度",
        )
        assertEquals(
            listOf(27.0, 95.5, 171.5, 255.0, 346.0, 414.5, 483.0),
            page.items.map { it.yOffsetMm },
            "yOffsetMm = topMargin 12 + legend 8 + header 7 + 已放置高度",
        )
        assertEquals(
            551.5,
            page.items.last().yOffsetMm + page.items.last().heightMm,
            1e-9,
            "7 条合计 524.5mm，底边 551.5mm 仍在首页内容区内",
        )
        assertTrue(page.continuationMarkers.isEmpty(), "加高模板下 7 条同页，不应生成续下页标记")
        assertTrue(success.overflowItems.isEmpty(), "最大条目 91mm 远低于内容区，不应进入 overflowItems")
    }

    /**
     * P-19：`itemHeight` 包含 `qrFooterHeightMm`（= `qrSizeMm + qrPaddingMm`），QR 不挤占笔记书写区。
     *
     * 默认模板：rightTop 14.5 + 笔记区 30 = 44.5mm；QR 底栏 18 + 3 = 21mm；间距 3mm
     * → itemHeight = 44.5 + 21 + 3 = 68.5mm。
     * 把 QR 放大到 24 + 4 = 28mm 后，条目高度只按底栏增量 +7mm 变为 75.5mm，
     * 笔记书写区仍是 30mm（`height - qrFooter - itemSpacing` 恒为 44.5mm）。
     */
    @Test
    fun `P-19 item height includes qr footer without squeezing the note area`() {
        val date = LocalDate.of(2026, 12, 8)
        val defaultTemplate = TemplateGeometry()
        val largerQrTemplate = defaultTemplate.copy(qrSizeMm = 24.0, qrPaddingMm = 4.0)
        assertEquals(21.0, defaultTemplate.qrFooterHeightMm, 1e-9, "QR 底栏 = 18 + 3")
        assertEquals(28.0, largerQrTemplate.qrFooterHeightMm, 1e-9, "QR 底栏 = 24 + 4")
        val question = compactQuestion("p19-q1")

        val defaultSuccess = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan(
                planId = "plan-p19-default",
                dailyGroups = mapOf(date to listOf(question)),
                template = defaultTemplate,
            ),
        )
        val largerQrSuccess = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan(
                planId = "plan-p19-large-qr",
                dailyGroups = mapOf(date to listOf(question)),
                template = largerQrTemplate,
            ),
        )

        val defaultItem = defaultSuccess.pages.single().items.single()
        assertEquals(
            68.5,
            defaultItem.heightMm,
            1e-9,
            "P-19：itemHeight = 44.5（rightTop 14.5 + 笔记区 30）+ 21（QR 底栏）+ 3（间距）",
        )
        assertEquals(27.0, defaultItem.yOffsetMm, 1e-9, "首页 yOffset = topMargin 12 + legend 8 + header 7")

        val largerQrItem = largerQrSuccess.pages.single().items.single()
        assertEquals(
            75.5,
            largerQrItem.heightMm,
            1e-9,
            "P-19：QR 底栏从 21mm 增至 28mm 时，条目高度精确增加 7mm",
        )
        assertEquals(
            largerQrItem.heightMm - defaultItem.heightMm,
            largerQrTemplate.qrFooterHeightMm - defaultTemplate.qrFooterHeightMm,
            1e-9,
            "P-19：条目高度增量必须恰好等于 QR 底栏增量",
        )
        for ((template, item) in listOf(defaultTemplate to defaultItem, largerQrTemplate to largerQrItem)) {
            assertEquals(
                44.5,
                item.heightMm - template.qrFooterHeightMm - template.itemSpacingMm,
                1e-9,
                "P-19：QR 不挤占笔记书写区——扣除 QR 底栏与间距后仍为 rightTop 14.5 + 笔记区 30",
            )
        }
        assertTrue(
            largerQrItem.yOffsetMm + largerQrItem.heightMm <= 297.0 - 12.0,
            "P-19：含 QR 底栏的条目仍落在下页边距以内",
        )
        assertTrue(defaultSuccess.overflowItems.isEmpty(), "常规条目不应进入 overflowItems")
        assertTrue(largerQrSuccess.overflowItems.isEmpty(), "QR 变大不构成 overflow 原因")
    }

    /**
     * P-04：单题知识点 1/2/3/4 → `leftHeight` 按公式递增（每点一行 20mm）；
     * 页级表头只扣一次，不随条目重复扣除。
     *
     * 自定义模板（默认模板右侧恒占优，左侧差异不可见）：行高 20mm、右上第一行 0mm、
     * 笔记区基础 0mm、QR 与间距 0mm、图例 10mm、表头 5mm。
     * `rightTop = 0 + 1×20 = 20`，`rightBottom = 0`，`leftHeight = count × 20`，
     * 因此 `itemHeight = max(count×20, 20) = 20/40/60/80mm`。
     * 首页首条 yOffset = 10（上边距）+ 10（图例）+ 5（表头）= 25mm；之后只累加条目高度。
     */
    @Test
    fun `P-04 one to four knowledge points grow the left height and deduct the header once`() {
        val date = LocalDate.of(2026, 12, 9)
        val template = TemplateGeometry(
            pageHeightMm = 400.0,
            topMarginMm = 10.0,
            bottomMarginMm = 10.0,
            legendHeightMm = 10.0,
            headerRowHeightMm = 5.0,
            rowHeightMm = 20.0,
            questionFixedInfoHeightMm = 0.0,
            noteAreaBaseHeightMm = 0.0,
            itemSpacingMm = 0.0,
            qrSizeMm = 0.0,
            qrPaddingMm = 0.0,
        )
        assertEquals(380.0, template.usableHeightMm, 1e-9, "前置条件：可用高度")
        assertEquals(365.0, template.firstPageContentHeightMm, 1e-9, "前置条件：首页内容区高度")
        val questions = (1..4).map { count ->
            RenderableQuestion(
                questionId = "p04-k$count",
                questionNumber = "4-$count",
                knowledgeCount = count,
                knowledgeNames = List(count) { "知识点${it + 1}" },
                knowledgeLineCount = 1,
                attachmentCount = 0,
                hasNoteArea = true,
            )
        }

        val success = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan(
                planId = "plan-p04",
                dailyGroups = mapOf(date to questions),
                template = template,
            ),
        )

        val page = success.pages.single()
        assertEquals(
            listOf(20.0, 40.0, 60.0, 80.0),
            page.items.map { it.heightMm },
            "P-04：知识点 1/2/3/4 时 leftHeight 每点 +20mm，itemHeight 取左侧",
        )
        assertEquals(
            listOf(25.0, 45.0, 85.0, 145.0),
            page.items.map { it.yOffsetMm },
            "P-04：页级表头只扣一次（首条 25 = 10 + 10 + 5），其后只累加条目高度",
        )
        assertTrue(page.continuationMarkers.isEmpty(), "P-04：四条同页，不应生成续下页标记")
        assertTrue(success.overflowItems.isEmpty(), "P-04：80mm 远小于首页内容区，不应进入 overflowItems")
    }

    /**
     * P-06：左侧比右侧高时 `itemHeight` 取左侧，而不是右上/右下之和。
     *
     * 默认模板、关闭笔记区（`rightBottom = 0`）：
     * - 4 个知识点：left = 4×7.5 = 30；rightTop = 7 + 1×7.5 = 14.5；
     *   itemHeight = 30 + 21（QR 底栏）+ 3（间距）= 54mm（若误取右侧会得到 38.5mm）；
     * - 2 个知识点：left = 15；rightTop = 14.5；itemHeight = 15 + 24 = 39mm。
     */
    @Test
    fun `P-06 item height takes the taller left column`() {
        val date = LocalDate.of(2026, 12, 10)
        fun question(id: String, knowledgeCount: Int) = RenderableQuestion(
            questionId = id,
            questionNumber = id,
            knowledgeCount = knowledgeCount,
            knowledgeNames = List(knowledgeCount) { "知识点${it + 1}" },
            knowledgeLineCount = 1,
            attachmentCount = 0,
            hasNoteArea = false,
        )

        val success = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan(
                planId = "plan-p06",
                dailyGroups = mapOf(date to listOf(question("p06-q2", 2), question("p06-q4", 4))),
                template = TemplateGeometry(),
            ),
        )

        val page = success.pages.single()
        assertEquals(
            listOf(39.0, 54.0),
            page.items.map { it.heightMm },
            "P-06：leftHeight 15/30 高于 rightTop 14.5，itemHeight 必须取左侧并加 QR 底栏与间距",
        )
        assertEquals(
            listOf(27.0, 66.0),
            page.items.map { it.yOffsetMm },
            "P-06：首页 yOffset = 12 + 8 + 7，之后按左侧派生的条目高度累加",
        )
        assertTrue(success.overflowItems.isEmpty(), "P-06：条目低于内容区，不应进入 overflowItems")
    }

    /**
     * P-07：右侧比左侧高时 `itemHeight` 取 `rightTop + rightBottom` 之和。
     *
     * 默认模板：left = 1×7.5 = 7.5；rightTop = 7 + 3×7.5 = 29.5；
     * rightBottom = 30 + 2×10 = 50；itemHeight = 29.5 + 50 + 21（QR）+ 3（间距）= 103.5mm。
     */
    @Test
    fun `P-07 item height takes the right column sum when it is taller`() {
        val date = LocalDate.of(2026, 12, 11)
        val question = RenderableQuestion(
            questionId = "p07-q1",
            questionNumber = "7-1",
            knowledgeCount = 1,
            knowledgeNames = listOf("知识点"),
            knowledgeLineCount = 3,
            attachmentCount = 2,
            hasNoteArea = true,
        )

        val success = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan(
                planId = "plan-p07",
                dailyGroups = mapOf(date to listOf(question)),
                template = TemplateGeometry(),
            ),
        )

        val item = success.pages.single().items.single()
        assertEquals(
            103.5,
            item.heightMm,
            1e-9,
            "P-07：rightTop 29.5 + rightBottom 50 + QR 底栏 21 + 间距 3 = 103.5mm",
        )
        assertEquals(27.0, item.yOffsetMm, 1e-9, "首页 yOffset = 12 + 8 + 7")
        assertTrue(success.overflowItems.isEmpty(), "P-07：103.5mm 低于内容区，不应进入 overflowItems")
    }

    /**
     * P-17：同一题重复分页（模拟重启后重规划）输出完全相同；知识点名称与行序按输入原样保留，
     * 分页器不得按名称重新排序（C-015）。
     *
     * `knowledgeNames` 的编号升序是上游契约（`print-template.md`），分页器不消费名称；
     * 用“一旦被遍历就失败”的列表锁定“分页器不得读取或重排名称”，避免恒真断言。
     */
    @Test
    fun `P-17 repeated planning keeps knowledge name order and row order stable`() {
        val date = LocalDate.of(2026, 12, 12)
        val names: List<String> = object : AbstractList<String>() {
            override val size: Int = 2
            override fun get(index: Int): String =
                throw AssertionError("P-17：分页器不应读取 knowledgeNames")

            override fun iterator(): Iterator<String> =
                throw AssertionError("P-17：分页器不应遍历 knowledgeNames")
        }
        val firstQuestion = RenderableQuestion(
            questionId = "p17-q1",
            questionNumber = "17-1",
            knowledgeCount = 2,
            knowledgeNames = names,
            knowledgeLineCount = 2,
            attachmentCount = 0,
            hasNoteArea = true,
        )
        val secondQuestion = compactQuestion("p17-q2")
        val input = mapOf(date to listOf(firstQuestion, secondQuestion))

        val firstPlan = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan("plan-p17", input, TemplateGeometry()),
        )
        val replan = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan("plan-p17", input, TemplateGeometry()),
        )

        assertEquals(firstPlan, replan, "P-17：同一输入重复规划必须得到完全相同的结果")
        assertEquals(
            listOf(listOf("p17-q1", "p17-q2")),
            firstPlan.pages.map { page -> page.items.map { it.questionId } },
            "P-17：行序按输入题目顺序，不得因名称顺序被重排",
        )
        assertEquals(
            listOf("plan-p17-01"),
            replan.pages.map { it.pageId },
            "P-17：重启后重规划 pageId 不变",
        )
    }

    /**
     * P-20：非法计数字段抛 `IllegalArgumentException`，不静默截断或补 0。
     * 六个越界值逐一覆盖：知识点数 0/5、附加数 -1/4、右上视觉行数 0/5。
     */
    @Test
    fun `P-20 out of range counters throw IllegalArgumentException`() {
        val date = LocalDate.of(2026, 12, 13)
        fun question(
            id: String,
            knowledgeCount: Int = 1,
            attachmentCount: Int = 0,
            knowledgeLineCount: Int = 1,
        ) = RenderableQuestion(
            questionId = id,
            questionNumber = id,
            knowledgeCount = knowledgeCount,
            knowledgeNames = List(knowledgeCount.coerceAtLeast(0)) { "知识点${it + 1}" },
            knowledgeLineCount = knowledgeLineCount,
            attachmentCount = attachmentCount,
            hasNoteArea = true,
        )
        val cases = listOf(
            "knowledgeCount=0" to question("p20-k0", knowledgeCount = 0),
            "knowledgeCount=5" to question("p20-k5", knowledgeCount = 5),
            "attachmentCount=-1" to question("p20-a-1", attachmentCount = -1),
            "attachmentCount=4" to question("p20-a4", attachmentCount = 4),
            "knowledgeLineCount=0" to question("p20-l0", knowledgeLineCount = 0),
            "knowledgeLineCount=5" to question("p20-l5", knowledgeLineCount = 5),
        )

        cases.forEach { (label, invalidQuestion) ->
            assertFailsWith<IllegalArgumentException>(label) {
                DefaultPagePlanner().plan(
                    planId = "plan-p20",
                    dailyGroups = mapOf(date to listOf(invalidQuestion)),
                    template = TemplateGeometry(),
                )
            }
        }
    }

    /**
     * 验收门禁：模板几何非法时返回 `Failure(INVALID_TEMPLATE_GEOMETRY)`，不得静默产生负高度页。
     *
     * - 模板 A：`usableHeightMm = 20 - 10 - 10 = 0`；
     * - 模板 B：`usableHeightMm = 20 > 0`，但 `firstPageContentHeightMm = 20 - 10（图例）- 10（表头）= 0`；
     * - 模板 C：`usableHeightMm = 10 - 10 - 10 = -10`；
     * - 模板 D：`pageHeightMm = NaN`，派生高度也是 NaN。
     * 四种情况都必须返回 `Failure`，而不是抛异常或返回 `Success`；零值、负值与 NaN 都不能被放过。
     */
    @Test
    fun `acceptance invalid template returns INVALID_TEMPLATE_GEOMETRY failure`() {
        val date = LocalDate.of(2026, 12, 14)
        val question = compactQuestion("invalid-template-q1")
        val zeroUsable = TemplateGeometry(
            pageHeightMm = 20.0,
            topMarginMm = 10.0,
            bottomMarginMm = 10.0,
        )
        val zeroFirstPageContent = TemplateGeometry(
            pageHeightMm = 40.0,
            topMarginMm = 10.0,
            bottomMarginMm = 10.0,
            legendHeightMm = 10.0,
            headerRowHeightMm = 10.0,
        )
        val negativeUsable = TemplateGeometry(
            pageHeightMm = 10.0,
            topMarginMm = 10.0,
            bottomMarginMm = 10.0,
        )
        val nanPageHeight = TemplateGeometry(pageHeightMm = Double.NaN)
        assertEquals(0.0, zeroUsable.usableHeightMm, 1e-9, "前置条件：可用高度为 0")
        assertTrue(zeroFirstPageContent.usableHeightMm > 0.0, "前置条件：模板 B 可用高度为正")
        assertEquals(0.0, zeroFirstPageContent.firstPageContentHeightMm, 1e-9, "前置条件：首页内容区为 0")
        assertEquals(-10.0, negativeUsable.usableHeightMm, 1e-9, "前置条件：模板 C 可用高度为负")
        assertTrue(nanPageHeight.usableHeightMm.isNaN(), "前置条件：模板 D 可用高度为 NaN")

        listOf(zeroUsable, zeroFirstPageContent, negativeUsable, nanPageHeight).forEachIndexed { index, template ->
            val result = DefaultPagePlanner().plan(
                planId = "plan-invalid-$index",
                dailyGroups = mapOf(date to listOf(question)),
                template = template,
            )
            val failure = assertIs<PaginationResult.Failure>(
                result,
                "非法模板必须返回 Failure，不得抛异常或返回 Success",
            )
            assertEquals(
                PaginationFailureCode.INVALID_TEMPLATE_GEOMETRY,
                failure.code,
                "非法模板的失败码必须是 INVALID_TEMPLATE_GEOMETRY",
            )
            assertTrue(failure.message.isNotBlank(), "Failure 必须带有可读的 message")
        }

        // C-068 方案 A：模板校验先于计数校验与空态判断。
        val invalidCount = RenderableQuestion(
            questionId = "invalid-template-bad-count",
            questionNumber = "bad",
            knowledgeCount = 5,
            knowledgeNames = List(5) { "知识点${it + 1}" },
            knowledgeLineCount = 1,
            attachmentCount = 0,
            hasNoteArea = true,
        )
        val bothInvalid = DefaultPagePlanner().plan(
            planId = "plan-invalid-both",
            dailyGroups = mapOf(date to listOf(invalidCount)),
            template = zeroUsable,
        )
        val bothInvalidFailure = assertIs<PaginationResult.Failure>(
            bothInvalid,
            "C-068 方案 A：模板与计数同时非法时先报模板失败，不抛 IllegalArgumentException",
        )
        assertEquals(PaginationFailureCode.INVALID_TEMPLATE_GEOMETRY, bothInvalidFailure.code)

        val emptyWithInvalidTemplate = DefaultPagePlanner().plan(
            planId = "plan-invalid-empty",
            dailyGroups = emptyMap(),
            template = zeroUsable,
        )
        val emptyFailure = assertIs<PaginationResult.Failure>(
            emptyWithInvalidTemplate,
            "C-068 方案 A：模板非法时即使没有题目也返回模板失败，而不是空 Success",
        )
        assertEquals(PaginationFailureCode.INVALID_TEMPLATE_GEOMETRY, emptyFailure.code)
    }

    /**
     * 验收门禁：`pageNumber` 超过 99 时返回 `Failure(PLAN_TOO_LONG)`；恰好 99 页仍成功。
     *
     * 每个日期一条 68.5mm 条目、每日从新页开始，因此日期数 = 页数。
     * 99 个日期 → 99 页（成功）；100 个日期 → 100 页（失败）。
     */
    @Test
    fun `acceptance more than 99 pages returns PLAN_TOO_LONG failure`() {
        val start = LocalDate.of(2027, 1, 1)
        fun days(count: Int): Map<LocalDate, List<RenderableQuestion>> =
            (0 until count).associate { offset ->
                start.plusDays(offset.toLong()) to listOf(compactQuestion("long-q$offset"))
            }

        val atLimit = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan("plan-99", days(99), TemplateGeometry()),
            "恰好 99 页时不得判定计划过长",
        )
        assertEquals(99, atLimit.pages.size)
        assertEquals(99, atLimit.pages.last().pageNumber)

        val tooLong = DefaultPagePlanner().plan("plan-100", days(100), TemplateGeometry())
        val failure = assertIs<PaginationResult.Failure>(
            tooLong,
            "超过 99 页必须返回 Failure，不得截断或继续输出第 100 页",
        )
        assertEquals(
            PaginationFailureCode.PLAN_TOO_LONG,
            failure.code,
            "超过 99 页的失败码必须是 PLAN_TOO_LONG",
        )
        assertTrue(failure.message.isNotBlank(), "Failure 必须带有可读的 message")
    }

    /**
     * C-048 放置语义：`dailyGroups` 全空返回 `Success(emptyList(), emptyList(), emptyList())`；
     * 空日期（`List` 为空）不建页、不占页号，有可打印条目的日期仍是文档第 1 页并扣图例。
     */
    @Test
    fun `C-048 empty input and empty date lists produce no pages`() {
        val emptyPlan = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan("plan-empty", emptyMap(), TemplateGeometry()),
        )
        assertTrue(emptyPlan.pages.isEmpty(), "C-048：空输入不得建页")
        assertTrue(emptyPlan.overflowItems.isEmpty(), "C-048：空输入不得产生 overflow")
        assertTrue(emptyPlan.warnings.isEmpty(), "C-048：空输入不得产生 warnings")

        val emptyDay = LocalDate.of(2026, 12, 15)
        val normalDay = LocalDate.of(2026, 12, 16)
        val plan = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan(
                planId = "plan-empty-day",
                dailyGroups = mapOf(
                    emptyDay to emptyList(),
                    normalDay to listOf(compactQuestion("c048-q1")),
                ),
                template = TemplateGeometry(),
            ),
        )

        val page = plan.pages.single()
        assertEquals(listOf(normalDay), plan.pages.map { it.date }, "C-048：空日期不建页")
        assertEquals(1, page.pageNumber, "C-048：空日期不占页号，正常日期仍是第 1 页")
        assertEquals("plan-empty-day-01", page.pageId)
        assertEquals(27.0, page.items.single().yOffsetMm, 1e-9, "第 1 页仍扣图例 8mm 与表头 7mm")
        assertTrue(page.continuationMarkers.isEmpty(), "C-048：没有条目被移页，不应生成标记")
    }

    /**
     * 规格「单题高度公式」段：默认模板下参数取满（4 个知识点 / 4 行名称 / 3 条附加）的常规题
     * 必须仍能放进内容区；若默认模板把常规题普遍推到 overflow，属于模板参数错误，测试必须失败。
     *
     * 默认模板：left = 4×7.5 = 30；rightTop = 7 + 4×7.5 = 37；
     * rightBottom = 30 + 3×10 = 60；itemHeight = 97 + 21 + 3 = 121mm < 内容区 266mm。
     */
    @Test
    fun `acceptance default template keeps a maximum normal item within the content area`() {
        val date = LocalDate.of(2026, 12, 17)
        val maximumNormal = RenderableQuestion(
            questionId = "default-max",
            questionNumber = "max",
            knowledgeCount = 4,
            knowledgeNames = List(4) { "知识点${it + 1}" },
            knowledgeLineCount = 4,
            attachmentCount = 3,
            hasNoteArea = true,
        )
        val template = TemplateGeometry()

        val success = assertIs<PaginationResult.Success>(
            DefaultPagePlanner().plan(
                planId = "plan-default-max",
                dailyGroups = mapOf(date to listOf(maximumNormal)),
                template = template,
            ),
        )

        assertTrue(
            success.overflowItems.isEmpty(),
            "默认模板下参数取满的常规题不得被移出打印（规格要求模板错误在测试中失败）",
        )
        val item = success.pages.single().items.single()
        assertEquals(121.0, item.heightMm, 1e-9, "默认模板的参数取满题高度为 121mm")
        assertTrue(
            item.heightMm < template.contentHeightMm,
            "常规题高度 121mm 必须小于默认内容区 ${template.contentHeightMm}mm",
        )
    }

    /** P-11 专用模板：usable=100、附件上限=30mm；只让每条附件高度可调。 */
    private fun attachmentBoundaryTemplate(perItemMm: Double): TemplateGeometry = TemplateGeometry(
        pageHeightMm = 120.0,
        topMarginMm = 10.0,
        bottomMarginMm = 10.0,
        legendHeightMm = 10.0,
        headerRowHeightMm = 5.0,
        rowHeightMm = 5.0,
        questionFixedInfoHeightMm = 10.0,
        noteAreaBaseHeightMm = 10.0,
        attachmentHeightPerItemMm = perItemMm,
        itemSpacingMm = 5.0,
        qrSizeMm = 15.0,
        qrPaddingMm = 5.0,
    )

    /**
     * P-14/P-18 共用模板：默认几何下单题最高约 121mm，远小于 contentHeightMm=266，两条用例不可达。
     * 这里缩短页面：usable=100、firstPageUsable=90、content=95、firstPageContent=85、附件上限=30。
     */
    private fun smallPageTemplate(): TemplateGeometry = TemplateGeometry(
        pageHeightMm = 120.0,
        topMarginMm = 10.0,
        bottomMarginMm = 10.0,
        legendHeightMm = 10.0,
        headerRowHeightMm = 5.0,
        rowHeightMm = 5.0,
        questionFixedInfoHeightMm = 10.0,
        noteAreaBaseHeightMm = 20.0,
        attachmentHeightPerItemMm = 10.0,
        itemSpacingMm = 5.0,
        qrSizeMm = 15.0,
        qrPaddingMm = 5.0,
    )

    /** 精简条目；默认参数在默认模板下高度 68.5mm（1 个知识点、1 行名称、无附件）。 */
    private fun compactQuestion(
        id: String,
        knowledgeCount: Int = 1,
        knowledgeLineCount: Int = 1,
    ): RenderableQuestion =
        RenderableQuestion(
            questionId = id,
            questionNumber = id,
            knowledgeCount = knowledgeCount,
            knowledgeNames = List(knowledgeCount) { "知识点${it + 1}" },
            knowledgeLineCount = knowledgeLineCount,
            attachmentCount = 0,
            hasNoteArea = true,
        )
}
