package com.recollect.feature.progress

import com.recollect.model.Tier
import java.time.LocalDate

/** 复习进度页的展示内容；由 [ReviewProgressAggregator] 从只读投影生成。 */
data class ReviewProgressContent(
    val today: LocalDate,
    val stats: ReviewProgressStats,
    /** 今天起 [ReviewProgressAggregator.FORECAST_DAYS] 天的到期题数分布。 */
    val forecast: List<DueForecastDay>,
    /** 按到期紧迫度排序的题目进度。 */
    val questions: List<QuestionProgressUi>,
)

data class ReviewProgressStats(
    val totalQuestions: Int,
    val overdueQuestions: Int,
    val dueTodayQuestions: Int,
    /** 今天起 7 天内到期（含今天）的题数。 */
    val dueWithinWeekQuestions: Int,
)

data class DueForecastDay(
    val date: LocalDate,
    val count: Int,
    val isToday: Boolean,
)

data class QuestionProgressUi(
    val questionId: String,
    val questionNumber: String,
    val sourcePage: String,
    val lastReviewedDate: LocalDate?,
    /** 题目最早到期的知识点日期；没有可调度的关联知识点时为 `null`。 */
    val dueDate: LocalDate?,
    val daysUntilDue: Long?,
    val knowledgePoints: List<KnowledgePointProgressUi>,
) {
    /** 卡片副标题：已掌握（MASTERED 档）的知识点数量。 */
    val masterySummary: String
        get() {
            val mastered = knowledgePoints.count { it.tier == Tier.MASTERED }
            return "已掌握 $mastered/${knowledgePoints.size} 个知识点"
        }
}

data class KnowledgePointProgressUi(
    val id: String,
    val number: Int,
    val name: String,
    val tier: Tier,
    val tierLabel: String,
    /** 连续掌握度百分比（0～100）。 */
    val masteryPercent: Int,
    /** 冷启动阶段显示“冷启动 x/3”，切换记忆模型后显示半衰期。 */
    val feedbackLabel: String,
    val nextDueDate: LocalDate,
    val daysUntilDue: Long,
    val dormant: Boolean,
)
