package com.recollect.feature.planning

import com.recollect.data.room.repository.PlanStatus
import com.recollect.data.room.repository.StoredPlan
import com.recollect.pagination.ScanItemGeometry
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PlanSessionTest {

    private val start = LocalDate.of(2026, 11, 23)
    private val end = LocalDate.of(2026, 11, 29)
    private val settings = PlanningSettings(planStartDate = start, planEndDate = end)

    private val renderMeta = com.recollect.data.room.repository.PlanRenderMeta(
        questionId = "q1",
        questionNumber = "T-1",
        sourcePage = "P1",
        knowledgeNames = listOf("一次函数"),
        knowledgeLineCount = 1,
        attachmentCount = 0,
        hasNoteArea = true,
        noteTexts = emptyList(),
    )

    @Test
    fun `open reuses the active plan without generating a new one`() = runBlocking {
        val repository = FakePlanRepository().apply {
            storedPlans["PLAN01"] = StoredPlan(
                planId = "PLAN01",
                weekStart = start,
                weekEnd = end,
                status = PlanStatus.GENERATED,
                printedAt = null,
                createdAt = java.time.Instant.EPOCH,
                renderMeta = listOf(renderMeta),
            )
            storedPages["PLAN01"] = listOf(
                com.recollect.data.room.repository.StoredPlanPage(
                    pageId = "PLAN01-01",
                    pageNumber = 1,
                    date = start,
                    itemCount = 1,
                    questionIds = listOf("q1"),
                    geometry = listOf(ScanItemGeometry("q1", 20.0, 70.0, 1, null)),
                ),
            )
        }
        val generator = CountingGenerator("NEW002")
        val session = DefaultPlanSession(generator, repository, LoadPlanUseCase(repository))

        val outcome = session.open(settings) as PlanGenerationOutcome.Planned

        assertEquals("PLAN01", outcome.preview.planId)
        assertEquals(0, generator.executeCalls, "已有计划时不得重新生成")
    }

    @Test
    fun `open generates only when the week has no active plan`() = runBlocking {
        val repository = FakePlanRepository()
        val generator = CountingGenerator("NEW002")
        val session = DefaultPlanSession(generator, repository, LoadPlanUseCase(repository))

        val outcome = session.open(settings) as PlanGenerationOutcome.Planned

        assertEquals("NEW002", outcome.preview.planId)
        assertEquals(1, generator.executeCalls)
        assertEquals(0, generator.regenerateCalls)
    }

    @Test
    fun `regenerate asks the generator to supersede the active plan`() = runBlocking {
        val repository = FakePlanRepository().apply {
            storedPlans["PLAN01"] = StoredPlan(
                planId = "PLAN01",
                weekStart = start,
                weekEnd = end,
                status = PlanStatus.PRINTED,
                printedAt = null,
                createdAt = java.time.Instant.EPOCH,
                renderMeta = listOf(renderMeta),
            )
        }
        val generator = CountingGenerator("NEW002")
        val session = DefaultPlanSession(generator, repository, LoadPlanUseCase(repository))

        val outcome = session.regenerate(settings) as PlanGenerationOutcome.Planned

        assertEquals("NEW002", outcome.preview.planId)
        assertEquals(1, generator.regenerateCalls)
        assertEquals("PLAN01", generator.previousPlanId)
        assertTrue(generator.executeCalls == 0, "显式重新生成必须走 regenerate 路径")
    }

    private class CountingGenerator(
        private val planId: String,
    ) : PlanGenerator {
        var executeCalls = 0
        var regenerateCalls = 0
        var previousPlanId: String? = null

        override suspend fun execute(settings: PlanningSettings): PlanGenerationOutcome {
            executeCalls++
            return planned(settings)
        }

        override suspend fun regenerate(
            settings: PlanningSettings,
            previousPlanId: String?,
        ): PlanGenerationOutcome {
            regenerateCalls++
            this.previousPlanId = previousPlanId
            return planned(settings)
        }

        private fun planned(settings: PlanningSettings) = PlanGenerationOutcome.Planned(
            printJob = PrintJob(
                planId = planId,
                pages = emptyList(),
                questions = emptyMap(),
                notes = emptyMap(),
                template = settings.template,
            ),
            preview = PlanPreviewContent(planId, emptyList(), emptyList()),
        )
    }
}
