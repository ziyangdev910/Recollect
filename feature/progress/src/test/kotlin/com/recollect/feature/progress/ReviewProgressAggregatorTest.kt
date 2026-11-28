package com.recollect.feature.progress

import com.recollect.data.room.repository.KnowledgePointReviewProgress
import com.recollect.data.room.repository.QuestionReviewProgress
import com.recollect.model.Tier
import java.time.Instant
import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 复习进度聚合：题目到期口径、统计、14 天分布与排序。
 *
 * 口径以调度决策为准：题目由任一到期知识点触发（D-01），归档题目不参与（D-11）。
 */
class ReviewProgressAggregatorTest {

    private val today = LocalDate.of(2026, 11, 22)

    private fun knowledgePoint(
        id: String,
        number: Int,
        nextDueDate: LocalDate,
        tier: Tier = Tier.UNKNOWN,
        continuousScore: Float = 0f,
        effectiveFeedbackCount: Float = 0f,
        halfLifeDays: Float? = null,
        dormant: Boolean = false,
        categoryId: String = "cat-math",
        categoryName: String = "数学",
    ) = KnowledgePointReviewProgress(
        id = id,
        number = number,
        name = "知识点$number",
        categoryId = categoryId,
        categoryName = categoryName,
        tier = tier,
        continuousScore = continuousScore,
        effectiveFeedbackCount = effectiveFeedbackCount,
        halfLifeDays = halfLifeDays,
        nextDueDate = nextDueDate,
        dormant = dormant,
    )

    private fun question(
        id: String,
        number: String,
        knowledgePoints: List<KnowledgePointReviewProgress>,
        lastReviewedDate: LocalDate? = null,
        sourcePage: String = "1",
        updatedAt: Instant = Instant.EPOCH,
    ) = QuestionReviewProgress(
        questionId = id,
        questionNumber = number,
        sourcePage = sourcePage,
        lastReviewedDate = lastReviewedDate,
        updatedAt = updatedAt,
        knowledgePoints = knowledgePoints,
    )

    @Test
    fun `question due date is earliest non-dormant knowledge point`() {
        val rows = listOf(
            question(
                id = "q1",
                number = "T-1",
                knowledgePoints = listOf(
                    knowledgePoint("kp1", 1, LocalDate.of(2026, 11, 24)),
                    knowledgePoint("kp2", 2, LocalDate.of(2026, 11, 20)),
                ),
                lastReviewedDate = LocalDate.of(2026, 11, 15),
                sourcePage = "2-同步练习",
            ),
            question(
                id = "q2",
                number = "T-2",
                knowledgePoints = listOf(
                    knowledgePoint("kp3", 3, LocalDate.of(2026, 11, 1), dormant = true),
                    knowledgePoint("kp4", 4, LocalDate.of(2026, 11, 22)),
                ),
            ),
        )

        val content = ReviewProgressAggregator.build(rows, today)

        val first = content.questions.first { it.questionId == "q1" }
        assertEquals(LocalDate.of(2026, 11, 20), first.dueDate)
        assertEquals(-2L, first.daysUntilDue)
        assertEquals(LocalDate.of(2026, 11, 15), first.lastReviewedDate)
        assertEquals("2-同步练习", first.sourcePage)

        // 休眠知识点的到期日不把题目算作逾期；剩余活动知识点的到期日决定题目状态。
        val second = content.questions.first { it.questionId == "q2" }
        assertEquals(LocalDate.of(2026, 11, 22), second.dueDate)
        assertEquals(0L, second.daysUntilDue)
    }

    @Test
    fun `question with only dormant knowledge points has no due date and sorts last`() {
        val rows = listOf(
            question(
                id = "dormant",
                number = "T-9",
                knowledgePoints = listOf(
                    knowledgePoint("kp9", 9, LocalDate.of(2026, 11, 1), dormant = true),
                ),
            ),
            question(
                id = "due",
                number = "T-1",
                knowledgePoints = listOf(knowledgePoint("kp1", 1, LocalDate.of(2026, 11, 25))),
            ),
        )

        val content = ReviewProgressAggregator.build(rows, today)

        assertEquals(listOf("due", "dormant"), content.questions.map { it.questionId })
        assertNull(content.questions.last().dueDate)
        assertNull(content.questions.last().daysUntilDue)
    }

    @Test
    fun `stats and forecast count each question once`() {
        val rows = listOf(
            // 逾期 2 天
            question("overdue", "T-1", listOf(knowledgePoint("kp1", 1, LocalDate.of(2026, 11, 20)))),
            // 今天到期
            question("today", "T-2", listOf(knowledgePoint("kp2", 2, LocalDate.of(2026, 11, 22)))),
            // 6 天后到期（7 天内）
            question("soon", "T-3", listOf(knowledgePoint("kp3", 3, LocalDate.of(2026, 11, 28)))),
            // 18 天后到期（14 天分布之外）
            question("later", "T-4", listOf(knowledgePoint("kp4", 4, LocalDate.of(2026, 12, 10)))),
        )

        val content = ReviewProgressAggregator.build(rows, today)

        assertEquals(ReviewProgressStats(
            totalQuestions = 4,
            overdueQuestions = 1,
            dueTodayQuestions = 1,
            dueWithinWeekQuestions = 2,
        ), content.stats)

        assertEquals(ReviewProgressAggregator.FORECAST_DAYS, content.forecast.size)
        assertTrue(content.forecast.first().isToday)
        assertEquals(today, content.forecast.first().date)
        assertEquals(1, content.forecast[0].count) // 今天
        assertEquals(1, content.forecast[6].count) // 11-28
        assertEquals(0, content.forecast[2].count) // 逾期题目不计入
        assertEquals(2, content.forecast.sumOf { it.count })
        assertFalse(content.forecast.drop(1).any { it.isToday })
    }

    @Test
    fun `questions are sorted by due date then question number`() {
        val rows = listOf(
            question("later", "T-3", listOf(knowledgePoint("kp3", 3, LocalDate.of(2026, 11, 30)))),
            question("today-b", "T-2", listOf(knowledgePoint("kp2", 2, LocalDate.of(2026, 11, 22)))),
            question("overdue", "T-9", listOf(knowledgePoint("kp9", 9, LocalDate.of(2026, 11, 19)))),
            question("today-a", "T-1", listOf(knowledgePoint("kp1", 1, LocalDate.of(2026, 11, 22)))),
        )

        val content = ReviewProgressAggregator.build(rows, today)

        assertEquals(
            listOf("overdue", "today-a", "today-b", "later"),
            content.questions.map { it.questionId },
        )
    }

    @Test
    fun `carries category names and modified time for filtering`() {
        val modified = Instant.parse("2026-11-20T08:00:00Z")
        val rows = listOf(
            question(
                id = "q1",
                number = "T-1",
                knowledgePoints = listOf(
                    knowledgePoint("kp1", 1, today, categoryName = "数学"),
                    knowledgePoint("kp2", 2, today, categoryName = "语文"),
                ),
                updatedAt = modified,
            ),
        )

        val ui = ReviewProgressAggregator.build(rows, today).questions.single()

        assertEquals(listOf("数学", "语文"), ui.categoryNames)
        assertEquals(modified, ui.updatedAt)
    }

    @Test
    fun `mastery percent and feedback label follow tier and interval model`() {
        val rows = listOf(
            question(
                id = "q1",
                number = "T-1",
                knowledgePoints = listOf(
                    knowledgePoint(
                        "cold",
                        1,
                        today,
                        tier = Tier.FAMILIAR,
                        continuousScore = 0.756f,
                        effectiveFeedbackCount = 2f,
                    ),
                    knowledgePoint(
                        "dhp",
                        2,
                        today,
                        tier = Tier.MASTERED,
                        continuousScore = 1.5f,
                        effectiveFeedbackCount = 3f,
                        halfLifeDays = 12.34f,
                    ),
                    knowledgePoint(
                        "clamped",
                        3,
                        today,
                        tier = Tier.UNKNOWN,
                        continuousScore = -0.5f,
                        effectiveFeedbackCount = 3f,
                    ),
                ),
            ),
        )

        val content = ReviewProgressAggregator.build(rows, today)
        val knowledge = content.questions.single().knowledgePoints.associateBy { it.id }

        assertEquals(76, knowledge.getValue("cold").masteryPercent)
        assertEquals("较熟悉", knowledge.getValue("cold").tierLabel)
        assertEquals("冷启动 2.0/3", knowledge.getValue("cold").feedbackLabel)

        assertEquals(100, knowledge.getValue("dhp").masteryPercent)
        assertEquals("掌握", knowledge.getValue("dhp").tierLabel)
        assertEquals("半衰期 12.3 天", knowledge.getValue("dhp").feedbackLabel)

        assertEquals(0, knowledge.getValue("clamped").masteryPercent)
        assertEquals("不会", knowledge.getValue("clamped").tierLabel)
        assertEquals("记忆模型", knowledge.getValue("clamped").feedbackLabel)

        val question = content.questions.single()
        assertEquals("已掌握 1/3 个知识点", question.masterySummary)
    }
}
