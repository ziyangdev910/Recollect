package com.recollect.data.room.repository

import com.recollect.data.room.dao.QuestionKnowledgeCrossRefDao
import com.recollect.data.room.entity.KnowledgeCategory
import com.recollect.data.room.entity.KnowledgePoint
import com.recollect.data.room.entity.Question
import com.recollect.data.room.entity.QuestionKnowledgeCrossRef
import com.recollect.model.Tier
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 复习进度只读投影：只含未归档题目，关联知识点按编号升序，来源页按 `编号-说明` 展示。
 */
class ReviewProgressRepositoryTest {

    private class FakeCrossRefDao(private val stores: InMemoryStores) : QuestionKnowledgeCrossRefDao {
        override suspend fun insert(entity: QuestionKnowledgeCrossRef) {
            stores.questionLinks.getOrPut(entity.questionId) { mutableSetOf() } += entity.knowledgePointId
        }

        override suspend fun all(): List<QuestionKnowledgeCrossRef> =
            stores.questionLinks.flatMap { (questionId, ids) ->
                ids.map { QuestionKnowledgeCrossRef(questionId = questionId, knowledgePointId = it) }
            }

        override suspend fun forQuestion(questionId: String): List<QuestionKnowledgeCrossRef> =
            stores.questionLinks[questionId].orEmpty().map {
                QuestionKnowledgeCrossRef(questionId = questionId, knowledgePointId = it)
            }

        override suspend fun countForKnowledgePoint(knowledgePointId: String): Int =
            stores.questionLinks.values.count { knowledgePointId in it }

        override suspend fun deleteForQuestion(questionId: String) {
            stores.questionLinks.remove(questionId)
        }

        override suspend fun deleteForKnowledgePoint(knowledgePointId: String) {
            stores.questionLinks.values.forEach { it.remove(knowledgePointId) }
        }
    }

    private val stores = InMemoryStores()
    private val repository = RoomReviewProgressRepository(
        questionDao = FakeQuestionDao(stores),
        crossRefDao = FakeCrossRefDao(stores),
        knowledgePointDao = FakeKnowledgePointDao(stores),
        categoryDao = FakeKnowledgeCategoryDao(stores),
    )

    private fun insertQuestion(
        id: String,
        number: String,
        archived: Boolean = false,
        sourcePageCode: Int = 1,
        sourcePageLabel: String = "",
        lastReviewedDate: LocalDate? = null,
        updatedAt: Instant = Instant.parse("2026-11-05T00:00:00Z"),
    ) {
        stores.questions[id] = Question(
            id = id,
            pageId = "",
            questionNumber = number,
            qrPayload = "",
            archived = archived,
            lastScheduledDate = null,
            lastReviewedDate = lastReviewedDate,
            estimatedReviewMinutes = 15,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
            archivedAt = if (archived) Instant.parse("2026-11-10T00:00:00Z") else null,
            sourcePage = sourcePageLabel,
            sourcePageCode = sourcePageCode,
            updatedAt = updatedAt,
        )
    }

    private fun insertKnowledgePoint(
        id: String,
        number: Int,
        nextDueDate: LocalDate,
        categoryId: String = "cat-math",
        categoryName: String = "数学",
    ) {
        stores.knowledgeCategories[categoryId] = KnowledgeCategory(
            id = categoryId,
            name = categoryName,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )
        stores.knowledgePoints[id] = KnowledgePoint(
            id = id,
            number = number,
            name = "知识点$number",
            currentTier = Tier.FAMILIAR,
            continuousScore = 0.5f,
            effectiveFeedbackCount = 2f,
            nextDueDate = nextDueDate,
            activeQuestionCount = 1,
            dormant = false,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
            categoryId = categoryId,
        )
    }

    private fun link(questionId: String, knowledgePointId: String) {
        stores.questionLinks.getOrPut(questionId) { mutableSetOf() } += knowledgePointId
    }

    @Test
    fun `aggregates active questions with knowledge points in number order`() = runBlocking {
        insertKnowledgePoint("kp-2", number = 2, nextDueDate = LocalDate.of(2026, 11, 25))
        insertKnowledgePoint("kp-1", number = 1, nextDueDate = LocalDate.of(2026, 11, 24))
        insertQuestion(
            id = "q1",
            number = "T-1",
            sourcePageCode = 3,
            sourcePageLabel = "作业本",
            lastReviewedDate = LocalDate.of(2026, 11, 18),
        )
        link("q1", "kp-2")
        link("q1", "kp-1")
        insertQuestion(id = "archived", number = "T-2", archived = true)
        link("archived", "kp-1")

        val rows = repository.questions()

        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("q1", row.questionId)
        assertEquals("T-1", row.questionNumber)
        assertEquals("3-作业本", row.sourcePage)
        assertEquals(LocalDate.of(2026, 11, 18), row.lastReviewedDate)
        assertEquals(Instant.parse("2026-11-05T00:00:00Z"), row.updatedAt)
        assertEquals(listOf("kp-1", "kp-2"), row.knowledgePoints.map { it.id })
        assertEquals(listOf(1, 2), row.knowledgePoints.map { it.number })
        assertEquals(LocalDate.of(2026, 11, 24), row.knowledgePoints.first().nextDueDate)
        assertEquals(Tier.FAMILIAR, row.knowledgePoints.first().tier)
        assertEquals("cat-math", row.knowledgePoints.first().categoryId)
        assertEquals("数学", row.knowledgePoints.first().categoryName)
    }

    @Test
    fun `question without links yields empty knowledge list`() = runBlocking {
        insertQuestion(id = "q1", number = "T-1", sourcePageCode = 0)

        val rows = repository.questions()

        assertEquals(1, rows.size)
        assertTrue(rows.single().knowledgePoints.isEmpty())
        assertEquals("0", rows.single().sourcePage)
    }
}
