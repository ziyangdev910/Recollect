package com.recollect.data.room.usecase

import com.recollect.data.room.entity.ScanSessionStatus
import com.recollect.data.room.entity.UserDecision
import com.recollect.data.room.repository.ConfirmedDecision
import com.recollect.data.room.repository.FeedbackRepository
import com.recollect.data.room.repository.KnowledgePointRepository
import com.recollect.data.room.repository.PlanWeekStartProvider
import com.recollect.data.room.repository.QuestionLookupRepository
import com.recollect.data.room.repository.ReviewFeedbackHistoryRepository
import com.recollect.data.room.repository.SaveDecisionsResult
import com.recollect.data.room.repository.TriggeredFeedbackRecord
import com.recollect.data.room.repository.effectiveTier
import com.recollect.model.KnowledgeState
import com.recollect.model.Tier
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 确认提交结果（feedback-usecase.md「接口」）。
 *
 * 4.2 的 `saveDecisions` 只返回写入条数与 C-055 跳过清单，不返回 `ReviewFeedback` 行 id；
 * 结果以 [saveResult] 承载写入统计，[updatedKnowledgeStates] 供确认页/测试核对。
 */
data class ApplyResult(
    val saveResult: SaveDecisionsResult,
    val updatedKnowledgeStates: List<KnowledgeState>,
)

/**
 * 前置条件或决策内容不合法（feedback-usecase.md「前置条件」）。
 *
 * 抛出时事务内不写任何行：前置校验与状态计算都先于 [FeedbackRepository.saveDecisions] 完成。
 */
class InvalidConfirmationException(message: String) : Exception(message)

/**
 * 人工确认后的知识点状态更新（feedback-usecase.md，C-019 方案二）。
 *
 * 5.x 收尾后：UseCase 负责前置校验（C-079 方案 A）、档位解析与状态计算；
 * 写入仍统一交给 [FeedbackRepository.saveDecisions]（C-074 方案 A）。
 * 被整题跳过的题目不贡献知识点状态（C-077 方案 A）；会话已提交时拒绝重复提交（C-080 方案 A）。
 */
class ApplyFeedbackUseCase(
    private val knowledgePointRepository: KnowledgePointRepository,
    private val reviewFeedbackHistoryRepository: ReviewFeedbackHistoryRepository,
    private val feedbackRepository: FeedbackRepository,
    private val nextDueCalculator: NextDueCalculator,
    private val questionLookupRepository: QuestionLookupRepository,
    private val planWeekStartProvider: PlanWeekStartProvider,
    private val clock: Clock,
    private val memoryStateInitializer: MemoryStateInitializer = DefaultMemoryStateInitializer(),
) {
    suspend fun execute(
        sessionId: String,
        decisions: List<ConfirmedDecision>,
    ): ApplyResult {
        validateDecisionShape(decisions)
        validateSession(sessionId, decisions)
        validatePendingRows(sessionId, decisions)
        validateReviewDates(decisions)

        val currentStates = knowledgePointRepository.states().associateBy { it.id }
        val skippedQuestionIds = skippableQuestionIds(decisions, currentStates)

        val updatedStates = linkedMapOf<String, KnowledgeState>()
        val questionLastReviewedDates = linkedMapOf<String, LocalDate>()
        // U-13：同一提交里同一知识点可能被多道题触发；事件序号与 DHP 历史必须把
        // 本批已处理的触发行算进去，不能两行都按「库内历史 + 1」。
        val triggeredInBatch = linkedMapOf<String, MutableList<TriggeredFeedbackRecord>>()

        for (decision in decisions) {
            if (decision.questionId in skippedQuestionIds) continue // C-077 方案 A：不贡献状态
            if (decision.decision == UserDecision.NOT_REVIEWED) continue // D-13/C-010

            val tier = requireNotNull(decision.effectiveTier())
            val current = updatedStates[decision.knowledgePointId]
                ?: currentStates[decision.knowledgePointId]
                ?: throw InvalidConfirmationException("知识点不存在：${decision.knowledgePointId}")

            val reviewedOn = requireNotNull(decision.actualReviewDate)
            questionLastReviewedDates.merge(decision.questionId, reviewedOn) { a, b -> maxOf(a, b) }

            updatedStates[decision.knowledgePointId] = if (decision.triggered) {
                val processedInBatch = triggeredInBatch.getOrPut(decision.knowledgePointId) { mutableListOf() }
                current.applyTriggered(
                    knowledgePointId = decision.knowledgePointId,
                    tier = tier,
                    reviewedOn = reviewedOn,
                    priorTriggeredInBatch = processedInBatch,
                ).also {
                    processedInBatch += TriggeredFeedbackRecord(tier = tier, reviewedOn = reviewedOn)
                }
            } else {
                // D-01：非触发关联知识点只按该行自身档位的增量 ×0.3 更新连续掌握度，
                // 档位、下次到期日与有效反馈计数都不变。
                val scoreAfter = (current.continuousScore.toDouble() + tier.scoreDelta() * 0.3)
                    .coerceIn(0.0, 1.0)
                current.copy(continuousScore = scoreAfter.toFloat()) // C-050：边界处转 Float
            }
        }

        val saveResult = feedbackRepository.saveDecisions(
            sessionId = sessionId,
            decisions = decisions,
            updatedKnowledgeStates = updatedStates.values.toList(),
            questionLastReviewedDates = questionLastReviewedDates,
        )
        return ApplyResult(
            saveResult = saveResult,
            updatedKnowledgeStates = updatedStates.values.toList(),
        )
    }

    /** 前置条件：非空、无未标记、无重复行、计划一致（feedback-usecase.md「前置条件」）。 */
    private fun validateDecisionShape(decisions: List<ConfirmedDecision>) {
        if (decisions.isEmpty()) {
            throw InvalidConfirmationException("确认提交至少需要一行决策（R-04）")
        }
        if (decisions.any { it.decision == UserDecision.UNMARKED }) {
            throw InvalidConfirmationException("存在未标记行；确认页必须逐项标记（D-13）")
        }
        val keys = decisions.map { it.questionId to it.knowledgePointId }
        if (keys.size != keys.distinct().size) {
            throw InvalidConfirmationException("同一题目与知识点在一份确认中重复（C-056）")
        }
        if (decisions.map { it.planId }.distinct().size != 1) {
            throw InvalidConfirmationException("一次确认只能属于同一份计划")
        }
    }

    /** 前置条件：会话存在、未提交、计划一致、可提交（C-079/C-080 方案 A）。 */
    private suspend fun validateSession(sessionId: String, decisions: List<ConfirmedDecision>) {
        val session = feedbackRepository.session(sessionId)
            ?: throw InvalidConfirmationException("扫描会话不存在：$sessionId")
        if (session.status == ScanSessionStatus.SUBMITTED) {
            throw InvalidConfirmationException("该扫描会话已提交，不能重复提交")
        }
        if (session.status != ScanSessionStatus.READY) {
            throw InvalidConfirmationException("扫描会话尚未就绪：${session.status}")
        }
        if (decisions.any { it.planId != session.planId }) {
            throw InvalidConfirmationException("决策与扫描会话的计划不一致")
        }
    }

    /** 前置条件：待确认行集合完整，计划日与触发标记逐行一致（feedback-usecase.md:132-135）。 */
    private suspend fun validatePendingRows(sessionId: String, decisions: List<ConfirmedDecision>) {
        val pending = feedbackRepository.pending(sessionId)
            .associateBy { it.questionId to it.knowledgePointId }
        val decisionKeys = decisions.map { it.questionId to it.knowledgePointId }.toSet()
        if (pending.keys != decisionKeys) {
            throw InvalidConfirmationException("待确认行与本次确认的行集合不一致")
        }
        decisions.forEach { decision ->
            val row = pending.getValue(decision.questionId to decision.knowledgePointId)
            if (row.plannedDate != decision.plannedDate) {
                throw InvalidConfirmationException("计划日与待确认行不一致：${decision.questionId}")
            }
            if (row.triggered != decision.triggered) {
                throw InvalidConfirmationException("触发标记与计划不一致：${decision.questionId}")
            }
        }
    }

    /** 前置条件：实际复习日的上下界（C-081 方案 A：按计划日所在周 + 用户设置的周起始日）。 */
    private fun validateReviewDates(decisions: List<ConfirmedDecision>) {
        val today = LocalDate.now(clock)
        val planStartDay = planWeekStartProvider.planStartDay()
        decisions.forEach { decision ->
            if (decision.decision == UserDecision.NOT_REVIEWED) {
                val reviewedOn = decision.actualReviewDate
                if (reviewedOn != null && reviewedOn > today) {
                    throw InvalidConfirmationException("实际复习日不能晚于今天：${decision.questionId}")
                }
                return@forEach
            }
            val reviewedOn = decision.actualReviewDate
                ?: throw InvalidConfirmationException("非本次未复习行必须有实际复习日")
            val weekStart = weekStartOf(decision.plannedDate, planStartDay)
            if (reviewedOn < weekStart) {
                throw InvalidConfirmationException("实际复习日不能早于计划周起始日：${decision.questionId}")
            }
            if (reviewedOn > today) {
                throw InvalidConfirmationException("实际复习日不能晚于今天：${decision.questionId}")
            }
            if (decision.effectiveTier() == null) {
                throw InvalidConfirmationException("决策没有可用的档位：${decision.decision}")
            }
        }
    }

    /**
     * 题目不存在或任一关联知识点不存在时，整题跳过（C-055/C-078）。
     * 题目存在但某行知识点不属于该题时，属于确认内容不合法（feedback-usecase.md:130）。
     */
    private suspend fun skippableQuestionIds(
        decisions: List<ConfirmedDecision>,
        currentStates: Map<String, KnowledgeState>,
    ): Set<String> {
        val skipped = mutableSetOf<String>()
        decisions.groupBy { it.questionId }.forEach { (questionId, rows) ->
            val linkedIds = questionLookupRepository.linkedKnowledgePointIds(questionId)
            if (linkedIds == null) {
                skipped += questionId
                return@forEach
            }
            if (rows.any { it.knowledgePointId !in currentStates }) {
                skipped += questionId
                return@forEach
            }
            rows.forEach { row ->
                if (row.knowledgePointId !in linkedIds) {
                    throw InvalidConfirmationException("知识点不属于该题：$questionId/${row.knowledgePointId}")
                }
            }
        }
        return skipped
    }

    private fun weekStartOf(date: LocalDate, startDay: DayOfWeek): LocalDate {
        val daysSinceStart = (date.dayOfWeek.value - startDay.value + 7) % 7
        return date.minusDays(daysSinceStart.toLong())
    }

    /** 触发行：全量更新档位、连续掌握度、有效反馈计数、下次到期日与记忆状态（D-01/D-03/D-05）。 */
    private suspend fun KnowledgeState.applyTriggered(
        knowledgePointId: String,
        tier: Tier,
        reviewedOn: LocalDate,
        priorTriggeredInBatch: List<TriggeredFeedbackRecord>,
    ): KnowledgeState {
        val scoreAfter = (continuousScore.toDouble() + tier.scoreDelta()).coerceIn(0.0, 1.0)
        val countAfter = effectiveFeedbackCount.toDouble() + tier.effectiveWeight()
        // 库内历史已按复习日升序；本批已处理的触发行按处理顺序接在其后。
        // 多取一条作为迁移兜底的间隔锚点（C-082 方案 A）。
        val combinedHistory =
            reviewFeedbackHistoryRepository.recentTriggered(
                knowledgePointId,
                limit = DHP_HISTORY_LOOKBACK + 1,
            ) + priorTriggeredInBatch
        val recentTriggered = combinedHistory.takeLast(DHP_HISTORY_LOOKBACK)
        val firstWindowAnchor = combinedHistory
            .getOrNull(combinedHistory.size - DHP_HISTORY_LOOKBACK - 1)
            ?.reviewedOn
        val eventCountAfter =
            reviewFeedbackHistoryRepository.triggeredCount(knowledgePointId) + priorTriggeredInBatch.size + 1
        val memoryState = memoryState()
            ?: initializeMemoryState(
                recentTriggered = recentTriggered,
                firstWindowAnchor = firstWindowAnchor,
                tier = tier,
                reviewedOn = reviewedOn,
                scoreAfter = scoreAfter.toFloat(),
                eventCountAfter = eventCountAfter,
            )
        val due = nextDueCalculator.nextDueDate(
            memoryState = memoryState,
            tier = tier,
            continuousScoreAfter = scoreAfter.toFloat(),
            feedbackEventCountAfter = eventCountAfter,
            effectiveFeedbackCountBefore = effectiveFeedbackCount,
            reviewedOn = reviewedOn,
            actualIntervalDays = actualIntervalDays(recentTriggered, reviewedOn),
        )
        return copy(
            currentTier = tier,
            continuousScore = scoreAfter.toFloat(), // C-050：边界处转 Float
            effectiveFeedbackCount = countAfter.toFloat(),
            nextDueDate = due.nextDueDate,
            difficulty = due.memoryState?.difficulty ?: difficulty,
            halfLifeDays = due.memoryState?.halfLifeDays ?: halfLifeDays,
        )
    }

    /**
     * 第 3 条 triggered 反馈后初始化 `difficulty/halfLifeDays`（C-018、C-037）。
     *
     * 历史不足 3 条（迁移兜底）时用当前档位重复 + 1/3/7 天间隔；其它事件序号不初始化。
     * 迁移窗口第一条的间隔从前一条触发反馈的复习日起算（C-082 方案 A）。
     */
    private fun KnowledgeState.initializeMemoryState(
        recentTriggered: List<TriggeredFeedbackRecord>,
        firstWindowAnchor: LocalDate?,
        tier: Tier,
        reviewedOn: LocalDate,
        scoreAfter: Float,
        eventCountAfter: Int,
    ): MemoryState? {
        val current = TriggeredFeedbackRecord(tier = tier, reviewedOn = reviewedOn)
        val records = when {
            eventCountAfter == 3 -> recentTriggered.takeLast(2) + current
            eventCountAfter > 3 && recentTriggered.size >= 3 -> recentTriggered.takeLast(3)
            else -> return null
        }
        val feedbacks = if (records.size == 3) {
            toTriggeredFeedbacks(records, firstWindowAnchor)
        } else {
            // C-037 迁移兜底：历史不足 3 条时用当前档位重复，缺失间隔按 1/3/7 兜底。
            listOf(1, 3, 7).map { TriggeredFeedback(tier = tier, intervalDays = it) }
        }
        return memoryStateInitializer.initial(
            recentTriggeredFeedbacks = feedbacks,
            continuousScoreAfterThird = scoreAfter,
        )
    }

    /**
     * 前三次实际间隔：首条从 `createdAt` 起算；迁移窗口的首条从紧邻的前一条复习日起算
     * （C-037、C-082 方案 A）。
     */
    private fun KnowledgeState.toTriggeredFeedbacks(
        records: List<TriggeredFeedbackRecord>,
        firstWindowAnchor: LocalDate?,
    ): List<TriggeredFeedback> {
        val createdOn = createdAt.atZone(ZoneId.systemDefault()).toLocalDate()
        return records.mapIndexed { index, record ->
            val previous = when {
                index > 0 -> records[index - 1].reviewedOn
                firstWindowAnchor != null -> firstWindowAnchor
                else -> createdOn
            }
            TriggeredFeedback(
                tier = record.tier,
                intervalDays = ChronoUnit.DAYS.between(previous, record.reviewedOn)
                    .coerceIn(1, MAX_DHP_INTERVAL_DAYS)
                    .toInt(),
            )
        }
    }

    /** 本次 triggered 反馈距上一次反馈的实际天数；无历史时从 `createdAt` 起算（C-037）。 */
    private fun KnowledgeState.actualIntervalDays(
        recentTriggered: List<TriggeredFeedbackRecord>,
        reviewedOn: LocalDate,
    ): Int {
        val previous = recentTriggered.lastOrNull()?.reviewedOn
            ?: createdAt.atZone(ZoneId.systemDefault()).toLocalDate()
        return ChronoUnit.DAYS.between(previous, reviewedOn)
            .coerceIn(1, MAX_DHP_INTERVAL_DAYS)
            .toInt()
    }

    private fun KnowledgeState.memoryState(): MemoryState? {
        val difficulty = difficulty ?: return null
        val halfLifeDays = halfLifeDays ?: return null
        return MemoryState(difficulty = difficulty, halfLifeDays = halfLifeDays)
    }
}

/** D-03：掌握 +0.3、较熟悉 −0.1、不会 −0.3；内部算术用 `Double`（C-050）。 */
private fun Tier.scoreDelta(): Double = when (this) {
    Tier.MASTERED -> 0.3
    Tier.FAMILIAR -> -0.1
    Tier.UNKNOWN -> -0.3
}

/** C-006：`FAMILIAR` 按 0.5 次计入有效反馈，其余 1.0 次。 */
private fun Tier.effectiveWeight(): Double = if (this == Tier.FAMILIAR) 0.5 else 1.0

/** DHP 初始化需要前 3 条 triggered 反馈（C-018/C-037）。 */
private const val DHP_HISTORY_LOOKBACK = 3

/** DHP 实际间隔上限（C-037）。 */
private const val MAX_DHP_INTERVAL_DAYS = 365L
