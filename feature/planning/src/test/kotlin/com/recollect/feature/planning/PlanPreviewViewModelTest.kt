package com.recollect.feature.planning

import com.recollect.data.room.repository.PlanGenerationException
import com.recollect.pagination.PaginationFailureCode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
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
                            pages = listOf(PlanPreviewPage(1, today, listOf("T-1"))),
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
        assertEquals(1, content.pages.single().questionNumbers.size)
        assertEquals("T-1", content.pages.single().questionNumbers.single())
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
                        pages = listOf(PlanPreviewPage(1, today, listOf("T-1"))),
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
                            pages = listOf(PlanPreviewPage(1, today, listOf("T-1"))),
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
}
