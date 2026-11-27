package com.recollect.feature.planning

import com.recollect.data.room.repository.PlanGenerationException
import com.recollect.data.room.repository.PlanStatus
import com.recollect.data.room.repository.StoredPlan
import com.recollect.pagination.PaginationFailureCode
import com.recollect.pagination.RenderableQuestion
import com.recollect.pagination.TemplateGeometry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * PlanPreview 状态（app.md「屏幕清单与状态」）：计划失败必须走 UiError 可重试通道。
 */
class PlanPreviewViewModelTest {

    private val today = LocalDate.of(2026, 11, 18)
    private val clock = Clock.fixed(
        today.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant(),
        ZoneId.of("Asia/Shanghai"),
    )

    @Test
    fun `planning failure becomes a retryable UiError`() = runBlocking {
        val viewModel = PlanPreviewViewModel(
            generator = object : PlanGenerator {
                override suspend fun execute(settings: PlanningSettings): PlanGenerationOutcome {
                    throw PlanGenerationException(
                        code = PaginationFailureCode.PLAN_TOO_LONG,
                        message = "计划超过 99 页，请拆分计划。",
                    )
                }
            },
            clock = clock,
        )

        viewModel.refresh()

        val error = viewModel.state as? PlanPreviewUiState.Error
            ?: error("预期错误态，实际为 ${viewModel.state}")
        assertEquals("PLAN_PLAN_TOO_LONG", error.error.code)
        assertEquals("计划超过 99 页，请拆分计划。", error.error.message)
        assertTrue(error.error.retryable, "计划失败必须可重试（C-047）")
    }

    @Test
    fun `planned outcome exposes one page with one item`() = runBlocking {
        val viewModel = PlanPreviewViewModel(
            generator = object : PlanGenerator {
                override suspend fun execute(settings: PlanningSettings): PlanGenerationOutcome =
                    PlanGenerationOutcome.Planned(
                        printJob = PrintJob(
                            planId = "ABC123",
                            pages = emptyList(),
                            questions = emptyMap(),
                            notes = emptyMap(),
                            template = settings.template,
                        ),
                        preview = PlanPreviewContent(
                            planId = "ABC123",
                            pages = listOf(PlanPreviewPage(1, today, listOf("第T-1题"))),
                            warnings = emptyList(),
                        ),
                    )
            },
            clock = clock,
        )

        viewModel.refresh()

        val content = viewModel.state as? PlanPreviewUiState.Content
            ?: error("预期内容态，实际为 ${viewModel.state}")
        assertEquals(1, content.pages.size)
        assertEquals(1, content.pages.single().questionLabels.size)
        assertEquals("第T-1题", content.pages.single().questionLabels.single())
    }

    @Test
    fun `planned outcome publishes the print job and failure clears it`() = runBlocking {
        var published: PrintJob? = null
        val generator = object : PlanGenerator {
            var fail = false

            override suspend fun execute(settings: PlanningSettings): PlanGenerationOutcome {
                if (fail) {
                    throw PlanGenerationException(
                        code = PaginationFailureCode.PLAN_TOO_LONG,
                        message = "计划超过 99 页，请拆分计划。",
                    )
                }
                return PlanGenerationOutcome.Planned(
                    printJob = PrintJob(
                        planId = "ABC123",
                        pages = emptyList(),
                        questions = emptyMap(),
                        notes = emptyMap(),
                        template = settings.template,
                    ),
                    preview = PlanPreviewContent(
                        planId = "ABC123",
                        pages = listOf(PlanPreviewPage(1, today, listOf("第T-1题"))),
                        warnings = emptyList(),
                    ),
                )
            }
        }
        val viewModel = PlanPreviewViewModel(
            generator = generator,
            clock = clock,
            onPrintJobAvailable = { published = it },
        )

        viewModel.refresh()
        assertEquals("ABC123", published?.planId, "计划成功后应把 PrintJob 交给打印页")

        generator.fail = true
        viewModel.refresh()
        assertNull(published, "计划失败时必须清掉上一次可打印的计划")
    }

    @Test
    fun `refreshIfNeeded keeps the same plan when returning from the print screen`() = runBlocking {
        var calls = 0
        val viewModel = PlanPreviewViewModel(
            generator = object : PlanGenerator {
                override suspend fun execute(settings: PlanningSettings): PlanGenerationOutcome {
                    calls += 1
                    val planId = if (calls == 1) "ABC123" else "XYZ789"
                    return PlanGenerationOutcome.Planned(
                        printJob = PrintJob(
                            planId = planId,
                            pages = emptyList(),
                            questions = emptyMap(),
                            notes = emptyMap(),
                            template = settings.template,
                        ),
                        preview = PlanPreviewContent(
                            planId = planId,
                            pages = listOf(PlanPreviewPage(1, today, listOf("第T-1题"))),
                            warnings = emptyList(),
                        ),
                    )
                }
            },
            clock = clock,
        )

        viewModel.refreshIfNeeded()
        assertEquals(1, calls)
        val firstPlanId = (viewModel.state as PlanPreviewUiState.Content).planId

        // 模拟从打印页返回预览页：页面重新进入组合，LaunchedEffect 再次调用。
        viewModel.refreshIfNeeded()
        assertEquals(1, calls, "返回预览页不得重新生成计划")
        assertEquals(
            firstPlanId,
            (viewModel.state as PlanPreviewUiState.Content).planId,
            "返回预览页后计划号必须保持不变",
        )

        // 用户再次点击“生成本周计划”走 refresh，允许生成新计划。
        viewModel.refresh()
        assertEquals(2, calls)
    }

    @Test
    fun `selecting a new date range reopens that range and caps it at seven days`() = runBlocking {
        val requestedStarts = mutableListOf<LocalDate>()
        val requestedEnds = mutableListOf<LocalDate>()

        fun planned(settings: PlanningSettings): PlanGenerationOutcome.Planned {
            requestedStarts += settings.planStartDate
            requestedEnds += settings.planEndDate
            return PlanGenerationOutcome.Planned(
                printJob = PrintJob(
                    planId = "ABC123",
                    pages = emptyList(),
                    questions = emptyMap(),
                    notes = emptyMap(),
                    template = settings.template,
                ),
                preview = PlanPreviewContent(
                    planId = "ABC123",
                    pages = listOf(PlanPreviewPage(1, settings.planStartDate, listOf("第T-1题"))),
                    warnings = emptyList(),
                ),
            )
        }

        val session = object : PlanSession {
            override suspend fun activePlan(settings: PlanningSettings): StoredPlan? = null

            override suspend fun open(settings: PlanningSettings): PlanGenerationOutcome =
                planned(settings)

            override suspend fun regenerate(settings: PlanningSettings): PlanGenerationOutcome =
                planned(settings)
        }
        val viewModel = PlanPreviewViewModel(
            session = session,
            clock = clock,
        )

        viewModel.refresh()
        assertEquals(currentWeekSettings(today).planStartDate, viewModel.startDate)
        val currentStart = requestedStarts.single()

        viewModel.selectRange(currentStart.plusWeeks(1), currentStart.plusWeeks(2))

        assertEquals(currentStart.plusWeeks(1), viewModel.startDate)
        // 最多 7 天：结束日被收回到开始日 +6 天。
        assertEquals(currentStart.plusWeeks(1).plusDays(6), viewModel.endDate)
        assertEquals(2, requestedStarts.size, "切换日期范围必须重新打开该范围的计划")
        assertEquals(currentStart.plusWeeks(1), requestedStarts.last())
        assertTrue(viewModel.state is PlanPreviewUiState.Content)
    }

    @Test
    fun `empty outcome keeps warnings visible in the empty state`() = runBlocking {
        val warning = PlanPreviewWarning(
            questionId = "q-1",
            questionNumber = "T-1",
            label = "题目过高",
            message = "整题不打印；请减少本题关联的知识点。",
        )
        val viewModel = PlanPreviewViewModel(
            generator = object : PlanGenerator {
                override suspend fun execute(settings: PlanningSettings): PlanGenerationOutcome =
                    PlanGenerationOutcome.Empty(
                        message = "本计划没有可打印的题目。",
                        warnings = listOf(warning),
                    )
            },
            clock = clock,
        )

        viewModel.refresh()

        val empty = viewModel.state as? PlanPreviewUiState.Empty
            ?: error("预期空态，实际为 ${viewModel.state}")
        assertEquals("本计划没有可打印的题目。", empty.message)
        assertEquals(listOf(warning), empty.warnings, "空态不得丢掉 overflow 提示（C-060/C-061）")
    }

    @Test
    fun `deferred questions stay visible in content and empty states`() = runBlocking {
        val deferred = PlanPreviewDeferred(
            questionId = "q-2",
            questionLabel = "第T-2题",
            label = "复习时间不足",
            message = "当天可用复习时间不够，本次未排入。",
        )
        var asEmpty = false
        val viewModel = PlanPreviewViewModel(
            generator = object : PlanGenerator {
                override suspend fun execute(settings: PlanningSettings): PlanGenerationOutcome =
                    if (asEmpty) {
                        PlanGenerationOutcome.Empty(
                            message = "本计划没有可打印的题目。",
                            warnings = emptyList(),
                            deferred = listOf(deferred),
                        )
                    } else {
                        PlanGenerationOutcome.Planned(
                            printJob = PrintJob(
                                planId = "ABC123",
                                pages = emptyList(),
                                questions = emptyMap(),
                                notes = emptyMap(),
                                template = settings.template,
                            ),
                            preview = PlanPreviewContent(
                                planId = "ABC123",
                                pages = listOf(PlanPreviewPage(1, today, listOf("第T-1题"))),
                                warnings = emptyList(),
                                deferred = listOf(deferred),
                            ),
                        )
                    }
            },
            clock = clock,
        )

        viewModel.refresh()
        val content = viewModel.state as? PlanPreviewUiState.Content
            ?: error("预期内容态，实际为 ${viewModel.state}")
        assertEquals(listOf(deferred), content.deferred, "预览页必须展示被推迟题目的提示")

        asEmpty = true
        viewModel.refresh()
        val empty = viewModel.state as? PlanPreviewUiState.Empty
            ?: error("预期空态，实际为 ${viewModel.state}")
        assertEquals(listOf(deferred), empty.deferred, "空计划也要说明有哪些题被推迟")
    }

    @Test
    fun `opening a plan generated before a bank edit prompts to regenerate`() = runBlocking {
        val planCreatedAt = Instant.parse("2026-11-20T00:00:00Z")
        val viewModel = PlanPreviewViewModel(
            session = scriptedSession(
                existing = storedPlan(planCreatedAt, PlanStatus.GENERATED),
                outcome = plannedOutcome(questionIds = listOf("q1")),
            ),
            clock = clock,
            initialStartDate = today,
            initialEndDate = today.plusDays(6),
            currentQuestionBank = {
                mapOf(
                    "q1" to planCreatedAt.minusSeconds(60),
                    "q2" to planCreatedAt.plusSeconds(60),
                )
            },
        )

        viewModel.refresh()

        assertTrue(viewModel.bankChanged, "计划生成后新增/编辑题目时必须询问是否重新生成")
    }

    @Test
    fun `not due questions alone do not trigger the bank prompt`() = runBlocking {
        val planCreatedAt = Instant.parse("2026-11-20T00:00:00Z")
        val viewModel = PlanPreviewViewModel(
            session = scriptedSession(
                existing = storedPlan(planCreatedAt, PlanStatus.GENERATED),
                outcome = plannedOutcome(questionIds = listOf("q1")),
            ),
            clock = clock,
            initialStartDate = today,
            initialEndDate = today.plusDays(6),
            // q2 未到期、本就不在计划里：不能因此误报“题库已更新”。
            currentQuestionBank = {
                mapOf(
                    "q1" to planCreatedAt.minusSeconds(60),
                    "q2" to planCreatedAt.minusSeconds(60),
                )
            },
        )

        viewModel.refresh()

        assertFalse(viewModel.bankChanged)
    }

    @Test
    fun `archiving a planned question triggers the bank prompt`() = runBlocking {
        val planCreatedAt = Instant.parse("2026-11-20T00:00:00Z")
        val viewModel = PlanPreviewViewModel(
            session = scriptedSession(
                existing = storedPlan(planCreatedAt, PlanStatus.GENERATED),
                outcome = plannedOutcome(questionIds = listOf("q1")),
            ),
            clock = clock,
            initialStartDate = today,
            initialEndDate = today.plusDays(6),
            currentQuestionBank = {
                mapOf("q2" to planCreatedAt.minusSeconds(60))
            },
        )

        viewModel.refresh()

        assertTrue(viewModel.bankChanged, "计划中的题目被归档后必须询问是否重新生成")
    }

    @Test
    fun `freshly generated plan does not trigger the bank prompt`() = runBlocking {
        val now = Instant.parse("2026-11-20T00:00:00Z")
        val viewModel = PlanPreviewViewModel(
            session = scriptedSession(
                existing = null,
                outcome = plannedOutcome(questionIds = listOf("q1")),
            ),
            clock = clock,
            initialStartDate = today,
            initialEndDate = today.plusDays(6),
            currentQuestionBank = {
                mapOf("q1" to now.plusSeconds(60))
            },
        )

        viewModel.refresh()

        assertFalse(viewModel.bankChanged, "刚生成的计划不应询问是否重新生成")
    }

    @Test
    fun `completed current week opens next week by default`() = runBlocking {
        val planCreatedAt = Instant.parse("2026-11-20T00:00:00Z")
        val viewModel = PlanPreviewViewModel(
            session = scriptedSession(
                existing = storedPlan(planCreatedAt, PlanStatus.COMPLETED),
                outcome = plannedOutcome(questionIds = listOf("q1")),
            ),
            clock = clock,
            initialStartDate = null,
            initialEndDate = null,
        )

        viewModel.refreshIfNeeded()

        assertEquals(
            nextWeekSettings(today).planStartDate,
            viewModel.startDate,
            "本周已完成确认时默认填入下周日期范围",
        )
    }

    @Test
    fun `unfinished current week opens current week by default`() = runBlocking {
        val planCreatedAt = Instant.parse("2026-11-20T00:00:00Z")
        val viewModel = PlanPreviewViewModel(
            session = scriptedSession(
                existing = storedPlan(planCreatedAt, PlanStatus.PRINTED),
                outcome = plannedOutcome(questionIds = listOf("q1")),
            ),
            clock = clock,
            initialStartDate = null,
            initialEndDate = null,
        )

        viewModel.refreshIfNeeded()

        assertEquals(
            currentWeekSettings(today).planStartDate,
            viewModel.startDate,
            "本周未完成确认时默认填入本周日期范围",
        )
    }

    private fun storedPlan(createdAt: Instant, status: PlanStatus): StoredPlan = StoredPlan(
        planId = "PLAN01",
        weekStart = today,
        weekEnd = today.plusDays(6),
        status = status,
        printedAt = null,
        createdAt = createdAt,
        renderMeta = emptyList(),
    )

    private fun scriptedSession(
        existing: StoredPlan?,
        outcome: PlanGenerationOutcome,
    ): PlanSession = object : PlanSession {
        override suspend fun activePlan(settings: PlanningSettings): StoredPlan? = existing

        override suspend fun open(settings: PlanningSettings): PlanGenerationOutcome = outcome

        override suspend fun regenerate(settings: PlanningSettings): PlanGenerationOutcome = outcome
    }

    private fun plannedOutcome(questionIds: List<String>): PlanGenerationOutcome.Planned {
        val questions = questionIds.associateWith { id ->
            RenderableQuestion(
                questionId = id,
                questionNumber = id,
                knowledgeCount = 1,
                knowledgeNames = listOf("知识点"),
                knowledgeLineCount = 1,
                attachmentCount = 0,
            )
        }
        return PlanGenerationOutcome.Planned(
            printJob = PrintJob(
                planId = "PLAN01",
                pages = emptyList(),
                questions = questions,
                notes = emptyMap(),
                template = TemplateGeometry.a4Print(),
            ),
            preview = PlanPreviewContent(
                planId = "PLAN01",
                pages = emptyList(),
                warnings = emptyList(),
            ),
        )
    }
}
