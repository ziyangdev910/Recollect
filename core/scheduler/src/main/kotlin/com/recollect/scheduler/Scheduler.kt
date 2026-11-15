package com.recollect.scheduler

import com.recollect.model.KnowledgeState
import com.recollect.model.SchedulerQuestion
import java.time.LocalDate

interface Scheduler {
    fun plan(
        knowledgeStates: List<KnowledgeState>,
        questions: List<SchedulerQuestion>,
        dailyAvailableMinutes: Map<LocalDate, Int>,
        dateRange: ClosedRange<LocalDate>,
        coolingPeriodDays: Int = 7,
        coolingOverrides: Set<String> = emptySet(), // C-008 方案二：未消费的一次性覆盖事件 questionId
    ): PlanResult
}

data class PlanResult(
    val dailyAssignments: Map<LocalDate, List<PlannedQuestion>>, // 按日期升序的 LinkedHashMap
    val deferred: List<DeferredItem>,
)

data class PlannedQuestion(
    val questionId: String,
    val triggeredKnowledgeIds: List<String>, // 本计划范围内到期
    val nonTriggeredKnowledgeIds: List<String>, // 关联但未到期；不含休眠知识点
    val estimatedMinutes: Int,
)

data class DeferredItem(
    val questionId: String,
    val reason: DeferReason, // COOLING / TIME_OVERFLOW / UNRESOLVED_KNOWLEDGE
    val knowledgePointIds: List<String> = emptyList(),
    val targetNextDueDates: Map<String, LocalDate> = emptyMap(), // 仅 COOLING 非空
)

enum class DeferReason {
    COOLING,
    TIME_OVERFLOW,
    UNRESOLVED_KNOWLEDGE, // C-055 方案 C：关联 id 失效，整题跳过并留下可见记录
}
