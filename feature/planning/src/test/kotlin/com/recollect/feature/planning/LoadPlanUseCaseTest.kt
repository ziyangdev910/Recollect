package com.recollect.feature.planning

import com.recollect.data.room.repository.PlanRenderMeta
import com.recollect.data.room.repository.PlanStatus
import com.recollect.data.room.repository.StoredPlan
import com.recollect.data.room.repository.StoredPlanPage
import com.recollect.pagination.ScanItemGeometry
import com.recollect.pagination.TemplateGeometry
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class LoadPlanUseCaseTest {

    private val date = LocalDate.of(2026, 11, 23)
    private val meta = PlanRenderMeta(
        questionId = "q1",
        questionNumber = "T-1",
        sourcePage = "8-P21",
        knowledgeNames = listOf("一次函数", "二次函数"),
        knowledgeLineCount = 2,
        attachmentCount = 1,
        hasNoteArea = true,
        noteTexts = listOf("附件笔记"),
    )

    @Test
    fun `load rebuilds the same print job from persisted plan and render metadata`() = runBlocking {
        val repository = FakePlanRepository().apply {
            storedPlans["K7M2Q9"] = StoredPlan(
                planId = "K7M2Q9",
                weekStart = date,
                weekEnd = date,
                status = PlanStatus.PRINTED,
                printedAt = null,
                renderMeta = listOf(meta),
            )
            storedPages["K7M2Q9"] = listOf(
                StoredPlanPage(
                    pageId = "K7M2Q9-01",
                    pageNumber = 1,
                    date = date,
                    itemCount = 1,
                    questionIds = listOf("q1"),
                    geometry = listOf(ScanItemGeometry("q1", 20.0, 90.0, 2, null)),
                ),
            )
        }

        val outcome = LoadPlanUseCase(repository)
            .load("K7M2Q9", TemplateGeometry.a4Print()) as PlanGenerationOutcome.Planned

        assertEquals("K7M2Q9", outcome.printJob.planId)
        assertEquals("K7M2Q9-01", outcome.printJob.pages.single().pageId)
        assertEquals(20.0, outcome.printJob.pages.single().items.single().yOffsetMm)
        assertEquals(90.0, outcome.printJob.pages.single().items.single().heightMm)
        val question = outcome.printJob.questions.getValue("q1")
        assertEquals(listOf("一次函数", "二次函数"), question.knowledgeNames)
        assertEquals(2, question.knowledgeLineCount)
        assertEquals(1, question.attachmentCount)
        assertEquals("附件笔记", outcome.printJob.notes.getValue("q1").single().text)
        assertEquals(listOf("T-1"), outcome.preview.pages.single().questionNumbers)
    }

    @Test
    fun `load rejects a plan whose render metadata is missing`() = runBlocking {
        val repository = FakePlanRepository().apply {
            storedPlans["K7M2Q9"] = StoredPlan(
                planId = "K7M2Q9",
                weekStart = date,
                weekEnd = date,
                status = PlanStatus.GENERATED,
                printedAt = null,
                renderMeta = emptyList(),
            )
            storedPages["K7M2Q9"] = listOf(
                StoredPlanPage(
                    pageId = "K7M2Q9-01",
                    pageNumber = 1,
                    date = date,
                    itemCount = 1,
                    questionIds = listOf("q1"),
                    geometry = listOf(ScanItemGeometry("q1", 20.0, 90.0, 1, null)),
                ),
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { LoadPlanUseCase(repository).load("K7M2Q9", TemplateGeometry.a4Print()) }
        }
    }
}
