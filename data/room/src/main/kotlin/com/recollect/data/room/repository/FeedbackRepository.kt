package com.recollect.data.room.repository

import com.recollect.data.room.dao.ConfirmationDecisionLogDao
import com.recollect.data.room.dao.KnowledgePointDao
import com.recollect.data.room.dao.NoteAttachmentItemDao
import com.recollect.data.room.dao.PendingFeedbackDao
import com.recollect.data.room.dao.PlanDao
import com.recollect.data.room.dao.PlanPageDao
import com.recollect.data.room.dao.QuestionDao
import com.recollect.data.room.dao.ReviewFeedbackDao
import com.recollect.data.room.dao.ScanSessionDao
import com.recollect.data.room.entity.ConfirmationDecisionLog
import com.recollect.data.room.entity.NoteAttachmentItem
import com.recollect.data.room.entity.PendingFeedback
import com.recollect.data.room.entity.ReviewFeedback
import com.recollect.data.room.entity.ScanSessionStatus
import com.recollect.data.room.entity.UserDecision
import com.recollect.data.room.usecase.InvalidConfirmationException
import com.recollect.model.KnowledgeState
import com.recollect.model.Tier
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/**
 * 确认提交（repository.md）。本轮只落地事务方法 [saveDecisions] 与其所需的会话读取。
 */
interface FeedbackRepository {
    /** 该会话的待确认行；确认提交前的行集合/计划日/触发标记校验使用。 */
    suspend fun pending(sessionId: String): List<PendingFeedback>

    /** 扫描会话的只读投影；`null` 表示会话不存在（C-079 方案 A）。 */
    suspend fun session(sessionId: String): ConfirmationSession?

    /**
     * @param updatedKnowledgeStates 由 `ApplyFeedbackUseCase` 计算好的知识点状态；
     *   `continuousScore` 已按 C-050 转 `Float`，Repository 只落库、不重算。
     * @param questionLastReviewedDates 题目级实际复习日；只在非 `NOT_REVIEWED` 行成功后写。
     * @param noteAttachments 用户在确认页显式保存的笔记附件。
     */
    suspend fun saveDecisions(
        sessionId: String,
        decisions: List<ConfirmedDecision>,
        updatedKnowledgeStates: List<KnowledgeState>,
        questionLastReviewedDates: Map<String, LocalDate>,
        noteAttachments: List<NoteAttachmentDraft> = emptyList(),
    ): SaveDecisionsResult
}

/** 扫描会话的只读投影，不向上暴露 Room `@Entity`（repository.md「边界」）。 */
data class ConfirmationSession(
    val sessionId: String,
    val planId: String,
    val status: ScanSessionStatus,
)

/**
 * `saveDecisions` 的单事务实现（repository.md「事务规则」）。
 *
 * 事务内写 `ReviewFeedback`、`ConfirmationDecisionLog`、更新 `KnowledgePoint`、
 * 写 `NoteAttachmentItem`、删除 `PendingFeedback`，并在整批无跳过时把会话标记为已提交；
 * 任一步抛错由 [TransactionRunner] 整体回滚，`PendingFeedback` 保留供重试。
 *
 * 关联 id 解析不到的题目整题跳过、不补默认知识点，跳过清单通过 [SaveDecisionsResult] 返回（C-055）。
 * 其待确认行保留，供数据修复后重试；该题的确认日志仍写入（C-078 方案 A）。
 */
class RoomFeedbackRepository(
    private val transactionRunner: TransactionRunner,
    private val reviewFeedbackDao: ReviewFeedbackDao,
    private val confirmationDecisionLogDao: ConfirmationDecisionLogDao,
    private val knowledgePointDao: KnowledgePointDao,
    private val questionDao: QuestionDao,
    private val pendingFeedbackDao: PendingFeedbackDao,
    private val noteAttachmentItemDao: NoteAttachmentItemDao,
    private val scanSessionDao: ScanSessionDao,
    /** C-099：最后一页确认后把计划标记为 COMPLETED，与反馈/日志同一事务。 */
    private val planPageDao: PlanPageDao,
    private val planDao: PlanDao,
    private val clock: Clock,
) : FeedbackRepository {

    override suspend fun pending(sessionId: String): List<PendingFeedback> =
        pendingFeedbackDao.forSession(sessionId)

    override suspend fun session(sessionId: String): ConfirmationSession? =
        scanSessionDao.byId(sessionId)?.let {
            ConfirmationSession(sessionId = it.sessionId, planId = it.planId, status = it.status)
        }

    override suspend fun saveDecisions(
        sessionId: String,
        decisions: List<ConfirmedDecision>,
        updatedKnowledgeStates: List<KnowledgeState>,
        questionLastReviewedDates: Map<String, LocalDate>,
        noteAttachments: List<NoteAttachmentDraft>,
    ): SaveDecisionsResult {
        requireDecisionsInvariants(decisions, updatedKnowledgeStates)

        return transactionRunner.inTransaction {
            val decidedAt = clock.instant()
            val skippedQuestionIds = sortedSetOf<String>()
            val skippedKnowledgePointIds = sortedSetOf<String>()
            val feedbackIds = mutableMapOf<Pair<String, String>, Long>()
            var writtenFeedbackCount = 0
            var writtenDecisionLogCount = 0

            decisions.groupBy { it.questionId }.toSortedMap().forEach questionLoop@{ (questionId, rows) ->
                // C-078 方案 A：所有决策都写确认日志，包括整题被跳过的行与 NOT_REVIEWED。
                rows.forEach { decision ->
                    confirmationDecisionLogDao.insert(decision.toLogEntity(sessionId, decidedAt))
                    writtenDecisionLogCount++
                }

                val missingKnowledgePointIds = rows
                    .map { it.knowledgePointId }
                    .filter { knowledgePointDao.byId(it) == null }
                    .distinct()
                    .sorted()

                if (questionDao.byId(questionId) == null || missingKnowledgePointIds.isNotEmpty()) {
                    // C-055 方案 C：整题跳过，不写反馈/状态/复习日，也不补默认知识点。
                    skippedQuestionIds += questionId
                    skippedKnowledgePointIds += missingKnowledgePointIds
                    return@questionLoop
                }

                rows.forEach decisionLoop@{ decision ->
                    if (decision.decision == UserDecision.NOT_REVIEWED) return@decisionLoop

                    val tier = requireNotNull(decision.effectiveTier()) {
                        "决策 ${decision.decision} 没有可落库的档位：${decision.questionId}/${decision.knowledgePointId}"
                    }
                    val reviewedOn = requireNotNull(decision.actualReviewDate) {
                        "非 NOT_REVIEWED 行必须有实际复习日：${decision.questionId}/${decision.knowledgePointId}"
                    }
                    feedbackIds[decision.questionId to decision.knowledgePointId] =
                        reviewFeedbackDao.insert(decision.toFeedbackEntity(tier, reviewedOn, decidedAt))
                    writtenFeedbackCount++
                }
            }

            // C-077 方案 A：被整题跳过的题目所贡献的状态不落库。
            // 同一知识点若仍被未跳过的题目引用，则保留该状态（来自有效行）。
            val writtenQuestionIds = decisions.map { it.questionId }.distinct()
                .filterNot { it in skippedQuestionIds }
                .toSet()
            val writtenKnowledgePointIds = decisions
                .filter { it.questionId in writtenQuestionIds }
                .map { it.knowledgePointId }
                .toSet()

            updatedKnowledgeStates
                .filter { it.id in writtenKnowledgePointIds }
                .forEach { state ->
                    val existing = checkNotNull(knowledgePointDao.byId(state.id)) {
                        "状态更新的知识点不存在：${state.id}（C-056）"
                    }
                    knowledgePointDao.update(
                        existing.copy(
                            currentTier = state.currentTier,
                            continuousScore = state.continuousScore, // 已是 Float（C-050）
                            effectiveFeedbackCount = state.effectiveFeedbackCount,
                            nextDueDate = state.nextDueDate,
                            difficulty = state.difficulty,
                            halfLifeDays = state.halfLifeDays,
                        ),
                    )
                }

            questionLastReviewedDates.forEach { (questionId, reviewedOn) ->
                if (questionId !in writtenQuestionIds) return@forEach
                val existing = checkNotNull(questionDao.byId(questionId)) {
                    "复习日更新的题目不存在：$questionId（C-056）"
                }
                val previous = existing.lastReviewedDate
                if (previous == null || reviewedOn > previous) {
                    questionDao.update(existing.copy(lastReviewedDate = reviewedOn))
                }
            }

            noteAttachments.forEach { draft ->
                if (draft.questionId !in writtenQuestionIds) return@forEach
                noteAttachmentItemDao.insert(
                    NoteAttachmentItem(
                        questionId = draft.questionId,
                        sourceFeedbackId = feedbackIds[draft.questionId to draft.knowledgePointId],
                        text = draft.text,
                        imageUri = draft.imageUri,
                        order = noteAttachmentItemDao.nextOrder(draft.questionId),
                        createdAt = decidedAt,
                    ),
                )
            }

            if (skippedQuestionIds.isEmpty()) {
                pendingFeedbackDao.deleteForSession(sessionId)
                // C-080 方案 A：整批成功才把会话标为已提交，重复提交由 UseCase 拒绝。
                scanSessionDao.markSubmitted(sessionId, decidedAt)
                // C-099：计划的每一页都已确认成功后标记 COMPLETED；
                // 有跳过时整页并未真正完成，留待修复后的重试再判定。
                markCompletedIfAllPagesConfirmed(decisions.first().planId)
            } else {
                // 有跳过时保留待确认行（含被跳过题目），会话维持 READY 供修复后重试。
                writtenQuestionIds.forEach { pendingFeedbackDao.deleteForSessionQuestion(sessionId, it) }
            }

            SaveDecisionsResult(
                writtenFeedbackCount = writtenFeedbackCount,
                writtenDecisionLogCount = writtenDecisionLogCount,
                skippedQuestionIds = skippedQuestionIds.toList(),
                skippedKnowledgePointIds = skippedKnowledgePointIds.toList(),
            )
        }
    }

    /**
     * C-099：该计划的全部页面都已有确认日志时，把计划迁移为 COMPLETED。
     *
     * 比较的是 `plan_page` 的页集合与日志里的 `pageId` 集合，而不是计数，
     * 避免把其他计划/残留日志的页误算进来；计划已被替代或已完成时 `markCompleted` 不生效。
     */
    private suspend fun markCompletedIfAllPagesConfirmed(planId: String) {
        val pageIds = planPageDao.forPlan(planId).map { it.pageId }.toSet()
        if (pageIds.isEmpty()) return
        val confirmedPageIds = confirmationDecisionLogDao.confirmedPageIds(planId).toSet()
        if (confirmedPageIds.containsAll(pageIds)) {
            planDao.markCompleted(planId)
        }
    }

    private fun requireDecisionsInvariants(
        decisions: List<ConfirmedDecision>,
        updatedKnowledgeStates: List<KnowledgeState>,
    ) {
        if (decisions.isEmpty()) {
            throw InvalidConfirmationException("确认提交至少需要一行决策（R-04）")
        }
        if (decisions.any { it.decision == UserDecision.UNMARKED }) {
            throw InvalidConfirmationException("存在未标记行；确认页必须逐项标记（D-13）")
        }
        val keys = decisions.map { it.questionId to it.knowledgePointId }
        if (keys.size != keys.distinct().size) {
            throw InvalidConfirmationException("同一 (questionId, knowledgePointId) 在决策中重复（C-056）")
        }
        val stateIds = updatedKnowledgeStates.map { it.id }
        if (stateIds.size != stateIds.distinct().size) {
            throw InvalidConfirmationException("知识点状态 id 重复（C-056）")
        }
    }

    private fun ConfirmedDecision.toLogEntity(sessionId: String, decidedAt: Instant): ConfirmationDecisionLog =
        ConfirmationDecisionLog(
            sessionId = sessionId,
            planId = planId,
            pageId = pageId,
            questionId = questionId,
            knowledgePointId = knowledgePointId,
            plannedDate = plannedDate,
            reviewedOn = if (decision == UserDecision.NOT_REVIEWED) null else actualReviewDate,
            triggered = triggered,
            decision = decision,
            selectedTier = selectedTier,
            correctedText = correctedText,
            decidedAt = decidedAt,
        )

    private fun ConfirmedDecision.toFeedbackEntity(
        tier: Tier,
        reviewedOn: LocalDate,
        timestamp: Instant,
    ): ReviewFeedback = ReviewFeedback(
        knowledgePointId = knowledgePointId,
        questionId = questionId,
        planId = planId,
        pageId = pageId,
        plannedDate = plannedDate,
        reviewedOn = reviewedOn,
        tier = tier,
        triggered = triggered,
        rawOcrText = detectedRawOcr, // 只读，D-06
        correctedText = correctedText,
        effectiveWeight = if (tier == Tier.FAMILIAR) 0.5f else 1.0f, // C-006
        weightApplied = if (triggered) 1.0f else 0.3f, // 审计字段，不反向驱动
        timestamp = timestamp,
    )
}
