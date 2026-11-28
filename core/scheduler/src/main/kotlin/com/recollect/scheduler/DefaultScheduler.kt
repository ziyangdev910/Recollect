package com.recollect.scheduler

import com.recollect.model.KnowledgeState
import com.recollect.model.SchedulerQuestion
import com.recollect.model.Tier
import java.time.LocalDate

class DefaultScheduler : Scheduler {

    override fun plan(
        knowledgeStates: List<KnowledgeState>,
        questions: List<SchedulerQuestion>,
        dailyAvailableMinutes: Map<LocalDate, Int>,
        dateRange: ClosedRange<LocalDate>,
        coolingPeriodDays: Int,
        coolingOverrides: Set<String>,
    ): PlanResult {
        // C-057 方案 D：0 = 关闭冷却；负数非法。参数校验先于空范围短路。
        require(coolingPeriodDays >= 0) {
            "coolingPeriodDays must be >= 0 (0 disables cooling); was $coolingPeriodDays"
        }

        // C-049、C-058 方案 A：dateRange.start > endInclusive 时返回全空 PlanResult，不抛异常；
        // 必须在候选/冷却判定之前短路，否则仍会生成 COOLING 顺延。
        if (dateRange.start > dateRange.endInclusive) {
            return PlanResult(dailyAssignments = emptyMap(), deferred = emptyList())
        }

        val statesById = knowledgeStates.associateBy { it.id }
        val deferred = mutableListOf<DeferredItem>()
        val candidates = mutableListOf<Candidate>()

        for (question in questions) {
            // C-058 方案 A：归档题先跳过，不参与关联数校验与关联解析。
            if (question.archived) continue

            // D-12、C-049：单题关联知识点至多 4 个；收到超限输入必须拒绝。
            // 0 个关联的题目会因无到期知识点自然跳过，不进入候选。
            require(question.linkedKnowledgeIds.size <= MAX_LINKED_KNOWLEDGE_POINTS) {
                "SchedulerQuestion ${question.id} links ${question.linkedKnowledgeIds.size} knowledge points; " +
                    "maximum is $MAX_LINKED_KNOWLEDGE_POINTS (D-12)"
            }

            // C-056 方案 A：id 唯一性由 data/room 保证；scheduler 不做去重/合并。
            // C-055 方案 C：任何关联 id 解析不到时整题跳过，并留下可见的 deferred 记录。
            val linkedIds = question.linkedKnowledgeIds
            val unresolvedIds = linkedIds.filter { it !in statesById }
            if (unresolvedIds.isNotEmpty()) {
                deferred += DeferredItem(
                    questionId = question.id,
                    reason = DeferReason.UNRESOLVED_KNOWLEDGE,
                    knowledgePointIds = unresolvedIds.sorted(),
                )
                continue
            }

            val linked = linkedIds.map { statesById.getValue(it) }
            val dueInRange = linked.filter {
                it.isPrintable() && !it.nextDueDate.isAfter(dateRange.endInclusive)
            }
            if (dueInRange.isEmpty()) continue

            val targetDate = maxOf(dueInRange.minOf { it.nextDueDate }, dateRange.start)
            val triggeredIds = dueInRange.map { it.id }.sorted()
            val nonTriggeredStates = linked.filter { it.id !in triggeredIds && it.isPrintable() }
            val nonTriggeredIds = nonTriggeredStates.map { it.id }.sorted()

            if (question.isInCoolingPeriod(targetDate, coolingPeriodDays, coolingOverrides)) {
                deferred += DeferredItem(
                    questionId = question.id,
                    reason = DeferReason.COOLING,
                    knowledgePointIds = triggeredIds,
                    targetNextDueDates = dueInRange
                        .sortedBy { it.id }
                        .associate { it.id to it.nextDueDate.plusDays(1) },
                )
                continue
            }

            candidates += Candidate(
                question = question,
                triggered = dueInRange,
                triggeredIds = triggeredIds,
                nonTriggeredIds = nonTriggeredIds,
                targetDate = targetDate,
                // D-02：档位差只作排序权重，不淘汰。C-054 已裁决方案 A：
                // 只比较本期到期点与本期未到期、仍会打印的关联点；同题内都已到期的跨档不计。
                hasTierGap = dueInRange.any { triggered ->
                    nonTriggeredStates.any { other -> other.currentTier != triggered.currentTier }
                },
            )
        }

        // 第 3 层时间装箱（2026-11-28 用户要求调整）：
        // 按日期升序逐日 first-fit；当天容量不足的候选顺延到后续日期继续尝试，
        // 但目标日晚于当前日期的候选不得提前回填当前日期。
        // 每日候选顺序只来自第 2 层排序结果，装箱阶段不得改序。
        val pending = candidates.sortedWith(CANDIDATE_ORDER).toMutableList()
        val assignmentsByDate = LinkedHashMap<LocalDate, MutableList<PlannedQuestion>>()

        var date = dateRange.start
        while (!date.isAfter(dateRange.endInclusive)) {
            var remainingMinutes = dailyAvailableMinutes[date] ?: 0
            val assigned = mutableListOf<PlannedQuestion>()
            val stillPending = mutableListOf<Candidate>()
            for (candidate in pending) {
                // 目标日尚未到来：保留到后续日期，不得提前占用今天。
                if (candidate.targetDate.isAfter(date)) {
                    stillPending += candidate
                    continue
                }
                val minutes = candidate.question.estimatedMinutes()
                if (remainingMinutes >= minutes) {
                    assigned += PlannedQuestion(
                        questionId = candidate.question.id,
                        triggeredKnowledgeIds = candidate.triggeredIds,
                        nonTriggeredKnowledgeIds = candidate.nonTriggeredIds,
                        estimatedMinutes = minutes,
                    )
                    remainingMinutes -= minutes
                } else {
                    // 今天剩余容量不足：保留到后续日期继续尝试（同日仍继续尝试更小的候选）。
                    stillPending += candidate
                }
            }
            pending.clear()
            pending.addAll(stillPending)
            if (assigned.isNotEmpty()) assignmentsByDate[date] = assigned
            date = date.plusDays(1)
        }

        // 整个日期范围都未能排入的候选记为 TIME_OVERFLOW。
        val timeDeferred = pending.map { candidate ->
            DeferredItem(
                questionId = candidate.question.id,
                reason = DeferReason.TIME_OVERFLOW,
            )
        }

        return PlanResult(
            dailyAssignments = assignmentsByDate,
            deferred = (deferred + timeDeferred)
                .sortedWith(compareBy({ it.reason.ordinal }, { it.questionId })),
        )
    }

    // 第 2 层排序键（scheduler.md，C-059 方案 B）：groupTier → hasTierGap
    // → 组内 min continuousScore → targetDate → questionId。
    // 最高档位分组优先；档位差只在同 groupTier 组内作第二键。禁止跨组比较 continuousScore（D-03）。
    private data class Candidate(
        val question: SchedulerQuestion,
        val triggered: List<KnowledgeState>,
        val triggeredIds: List<String>,
        val nonTriggeredIds: List<String>,
        val targetDate: LocalDate,
        val hasTierGap: Boolean,
    ) {
        val groupTier: Tier = triggered.maxByOrNull { it.currentTier.ordinal }!!.currentTier
        val groupScore: Float = triggered
            .filter { it.currentTier == groupTier }
            .minOf { it.continuousScore }
    }

    private fun KnowledgeState.isPrintable(): Boolean = !dormant && activeQuestionCount > 0

    // C-017：estimatedReviewMinutes 缺失或 <= 0 时按 15 分钟处理。
    private fun SchedulerQuestion.estimatedMinutes(): Int = estimatedReviewMinutes.takeIf { it > 0 } ?: 15

    // D-04、C-057 方案 D：0 = 无条件关闭冷却；正数按严格大于判定；
    // 命中的一次性覆盖（未消费）只在本次 plan() 跳过冷却。
    private fun SchedulerQuestion.isInCoolingPeriod(
        targetDate: LocalDate,
        coolingPeriodDays: Int,
        coolingOverrides: Set<String>,
    ): Boolean = coolingPeriodDays > 0 &&
        id !in coolingOverrides &&
        lastReviewedDate?.plusDays(coolingPeriodDays.toLong())?.isAfter(targetDate) == true

    private companion object {
        const val MAX_LINKED_KNOWLEDGE_POINTS = 4 // D-12

        val CANDIDATE_ORDER: Comparator<Candidate> = compareByDescending<Candidate> { it.groupTier.ordinal }
            .thenByDescending { it.hasTierGap }
            .thenBy { it.groupScore }
            .thenBy { it.targetDate }
            .thenBy { it.question.id }
    }
}
