package com.recollect.feature.progress

import com.recollect.common.UiState
import com.recollect.data.room.repository.KnowledgePointReviewProgress
import com.recollect.data.room.repository.QuestionReviewProgress
import com.recollect.data.room.repository.ReviewProgressRepository
import com.recollect.model.Tier
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 复习进度页状态：加载、空态与可重试错误。
 */
class ReviewProgressViewModelTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-22T08:00:00Z"), ZoneOffset.UTC)

    private class FakeRepository(
        rows: List<QuestionReviewProgress> = emptyList(),
        private val failure: Throwable? = null,
    ) : ReviewProgressRepository {
        private val stored = rows

        override suspend fun questions(): List<QuestionReviewProgress> {
            failure?.let { throw it }
            return stored
        }
    }

    @Test
    fun `loads content using clock date`() = runBlocking {
        val row = QuestionReviewProgress(
            questionId = "q1",
            questionNumber = "T-1",
            sourcePage = "3-作业本",
            lastReviewedDate = LocalDate.of(2026, 11, 18),
            knowledgePoints = listOf(
                KnowledgePointReviewProgress(
                    id = "kp1",
                    number = 1,
                    name = "一元二次方程",
                    tier = Tier.FAMILIAR,
                    continuousScore = 0.6f,
                    effectiveFeedbackCount = 2f,
                    halfLifeDays = null,
                    nextDueDate = LocalDate.of(2026, 11, 25),
                    dormant = false,
                ),
            ),
        )

        val viewModel = ReviewProgressViewModel(FakeRepository(listOf(row)), clock)

        viewModel.refresh()

        val content = (viewModel.state as UiState.Content).data
        assertEquals(LocalDate.of(2026, 11, 22), content.today)
        assertEquals(1, content.stats.totalQuestions)
        assertEquals(1, content.questions.size)
        assertEquals("T-1", content.questions.single().questionNumber)
    }

    @Test
    fun `empty repository shows empty state`() = runBlocking {
        val viewModel = ReviewProgressViewModel(FakeRepository(), clock)

        viewModel.refresh()

        assertEquals(UiState.Empty, viewModel.state)
    }

    @Test
    fun `repository failure becomes retryable error`() = runBlocking {
        val viewModel = ReviewProgressViewModel(
            FakeRepository(failure = IllegalStateException("数据库不可用")),
            clock,
        )

        viewModel.refresh()

        val error = viewModel.state as UiState.Error
        assertEquals("PROGRESS_LOAD_FAILED", error.code)
        assertEquals("数据库不可用", error.message)
        assertTrue(error.retryable)
    }
}
