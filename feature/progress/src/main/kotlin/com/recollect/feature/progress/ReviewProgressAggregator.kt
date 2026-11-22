package com.recollect.feature.progress

import com.recollect.data.room.repository.KnowledgePointReviewProgress
import com.recollect.data.room.repository.QuestionReviewProgress
import com.recollect.model.Tier
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * 纯 Kotlin 的复习进度聚合：把只读投影转成页面模型。
 *
 * 统计口径：
 * - 题目到期日 = 任一未休眠关联知识点最早的 `nextDueDate`（调度以知识点为单元，D-01）；
 * - 已逾期 / 今天到期 / 7 天内均按题目计，一道题只计一次；
 * - 14 天分布只统计今天及以后，已逾期题目不计入柱子。
 */
object ReviewProgressAggregator {

    const val FORECAST_DAYS = 14

    /** “7 天内”包含今天，即 offset 0～6。 */
    const val UPCOMING_WINDOW_DAYS = 7L

    private const val COLD_START_TARGET = 3f

    fun build(rows: List<QuestionReviewProgress>, today: LocalDate): ReviewProgressContent {
        val questions = rows
            .map { it.toUi(today) }
            .sortedWith(
                compareBy<QuestionProgressUi, LocalDate?>(nullsLast()) { it.dueDate }
                    .thenBy { it.questionNumber },
            )

        val forecast = (0 until FORECAST_DAYS).map { offset ->
            val date = today.plusDays(offset.toLong())
            DueForecastDay(
                date = date,
                count = questions.count { it.dueDate == date },
                isToday = offset == 0,
            )
        }

        val stats = ReviewProgressStats(
            totalQuestions = questions.size,
            overdueQuestions = questions.count { it.daysUntilDue?.let { days -> days < 0 } == true },
            dueTodayQuestions = questions.count { it.daysUntilDue == 0L },
            dueWithinWeekQuestions = questions.count {
                it.daysUntilDue?.let { days -> days in 0 until UPCOMING_WINDOW_DAYS } == true
            },
        )

        return ReviewProgressContent(
            today = today,
            stats = stats,
            forecast = forecast,
            questions = questions,
        )
    }

    private fun QuestionReviewProgress.toUi(today: LocalDate): QuestionProgressUi {
        val dueDate = knowledgePoints
            .filterNot { it.dormant }
            .minOfOrNull { it.nextDueDate }
        return QuestionProgressUi(
            questionId = questionId,
            questionNumber = questionNumber,
            sourcePage = sourcePage,
            lastReviewedDate = lastReviewedDate,
            dueDate = dueDate,
            daysUntilDue = dueDate?.let { ChronoUnit.DAYS.between(today, it) },
            knowledgePoints = knowledgePoints.map { it.toUi(today) },
        )
    }

    private fun KnowledgePointReviewProgress.toUi(today: LocalDate): KnowledgePointProgressUi =
        KnowledgePointProgressUi(
            id = id,
            number = number,
            name = name,
            tier = tier,
            tierLabel = tier.label,
            masteryPercent = (continuousScore.coerceIn(0f, 1f) * 100).roundToInt(),
            feedbackLabel = feedbackLabel(effectiveFeedbackCount, halfLifeDays),
            nextDueDate = nextDueDate,
            daysUntilDue = ChronoUnit.DAYS.between(today, nextDueDate),
            dormant = dormant,
        )

    private fun feedbackLabel(effectiveFeedbackCount: Float, halfLifeDays: Float?): String =
        when {
            effectiveFeedbackCount < COLD_START_TARGET ->
                "冷启动 ${oneDecimal(effectiveFeedbackCount.coerceAtLeast(0f))}/3"
            halfLifeDays != null -> "半衰期 ${oneDecimal(halfLifeDays)} 天"
            else -> "记忆模型"
        }

    /** 避免 `String.format` 的 Locale 差异；只保留一位小数。 */
    private fun oneDecimal(value: Float): String {
        val scaled = (value * 10).roundToInt()
        return "${scaled / 10}.${scaled % 10}"
    }

    private val Tier.label: String
        get() = when (this) {
            Tier.UNKNOWN -> "不会"
            Tier.FAMILIAR -> "较熟悉"
            Tier.MASTERED -> "掌握"
        }
}
