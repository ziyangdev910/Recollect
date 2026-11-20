package com.recollect.feature.confirm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.recollect.data.room.entity.DetectionStatus
import com.recollect.data.room.entity.ScanSessionStatus
import com.recollect.data.room.entity.UserDecision
import com.recollect.data.room.repository.ConfirmedDecision
import com.recollect.data.room.repository.FeedbackRepository
import com.recollect.data.room.repository.KnowledgePointRef
import com.recollect.data.room.repository.KnowledgePointRepository
import com.recollect.data.room.repository.PendingFeedbackRow
import com.recollect.data.room.repository.QuestionRepository
import com.recollect.data.room.repository.ScanSessionRepository
import com.recollect.data.room.usecase.ApplyResult
import com.recollect.data.room.usecase.InvalidConfirmationException
import com.recollect.model.Tier
import kotlinx.coroutines.CancellationException
import java.time.Clock
import java.time.LocalDate

/** 提交入口；由 app 层用 `ApplyFeedbackUseCase` 适配，ViewModel 不直接依赖事务细节。 */
fun interface ConfirmSubmitter {
    suspend fun submit(sessionId: String, decisions: List<ConfirmedDecision>): ApplyResult
}

data class ConfirmHeader(
    val sessionId: String,
    val planId: String,
    val pageCount: Int,
    val rowCount: Int,
)

/** 页面上一行；[decision] 为核心状态机的当前值。 */
data class ConfirmRowView(
    val questionId: String,
    val knowledgePointId: String,
    val questionNumber: String,
    val sourcePage: String,
    val knowledgeNumber: Int?,
    val knowledgeName: String?,
    val triggered: Boolean,
    val detectedTier: Tier?,
    val detectionStatus: DetectionStatus,
    val conflict: Boolean,
    val detectedRawOcr: String?,
    val plannedDate: LocalDate,
    val actualReviewDate: LocalDate?,
    val isFuture: Boolean,
    val decision: UserDecision,
    val selectedTier: Tier?,
    val correctedText: String?,
) {
    val key: ConfirmRowKey
        get() = ConfirmRowKey(questionId = questionId, knowledgePointId = knowledgePointId)
}

data class ConfirmPageView(
    val pageId: String,
    val plannedDate: LocalDate,
    val rows: List<ConfirmRowView>,
)

/** 一次提交的可见结果；[skippedQuestionIds] 非空时页面展示被跳过清单（C-077/C-078）。 */
data class SubmitReport(
    val writtenFeedbackCount: Int,
    val writtenDecisionLogCount: Int,
    val skippedQuestionIds: List<String>,
)

sealed interface ConfirmUiState {
    data object Loading : ConfirmUiState

    data class Empty(val message: String) : ConfirmUiState

    data class Error(
        val message: String,
        val retryable: Boolean,
    ) : ConfirmUiState

    data class Content(
        val header: ConfirmHeader,
        val pages: List<ConfirmPageView>,
        val unmarkedCount: Int,
        val canSubmit: Boolean,
        val submitting: Boolean,
        val submitted: Boolean,
        val report: SubmitReport?,
        val message: String?,
    ) : ConfirmUiState
}

/**
 * 确认页状态持有者（手写 ViewModel，不引入 lifecycle-viewmodel 依赖）。
 *
 * 不持有 `Context`（R-03）。逐行决策在 [ConfirmStateMachine] 中完成；本类只负责
 * 加载待确认行、把决策交给 `ApplyFeedbackUseCase`，并把 C-077/C-078 的跳过清单暴露给页面。
 */
class ConfirmViewModel(
    private val sessionId: String,
    private val scanSessionRepository: ScanSessionRepository,
    private val feedbackRepository: FeedbackRepository,
    private val knowledgePointRepository: KnowledgePointRepository,
    private val questionRepository: QuestionRepository,
    private val submitter: ConfirmSubmitter,
    private val clock: Clock,
) {
    var state by mutableStateOf<ConfirmUiState>(ConfirmUiState.Loading)
        private set

    private var machine: ConfirmStateMachine? = null
    private var header: ConfirmHeader? = null
    private var report: SubmitReport? = null
    private var message: String? = null
    private var submitting = false
    private var submitted = false

    suspend fun load() {
        state = ConfirmUiState.Loading
        report = null
        message = null
        submitting = false
        submitted = false
        machine = null

        try {
            val session = feedbackRepository.session(sessionId)
            if (session == null) {
                state = ConfirmUiState.Error("扫描会话不存在，请返回重新扫描。", retryable = false)
                return
            }
            when (session.status) {
                ScanSessionStatus.SUBMITTED -> {
                    // C-080 方案 A：已提交会话拒绝重复提交。
                    state = ConfirmUiState.Error("该扫描会话已提交，不能重复提交。", retryable = false)
                    return
                }

                ScanSessionStatus.SCANNING -> {
                    state = ConfirmUiState.Error("扫描尚未完成，暂不能确认。", retryable = true)
                    return
                }

                ScanSessionStatus.ABANDONED -> {
                    state = ConfirmUiState.Error("扫描会话已过期，请重新扫描。", retryable = false)
                    return
                }

                ScanSessionStatus.READY -> Unit
            }

            val pending = scanSessionRepository.pendingRows(sessionId)
            if (pending.isEmpty()) {
                state = ConfirmUiState.Empty("没有待确认的复习记录。")
                return
            }

            val rows = pending.toConfirmRows()
            machine = ConfirmStateMachine(rows = rows, today = LocalDate.now(clock))
            header = ConfirmHeader(
                sessionId = sessionId,
                planId = session.planId,
                pageCount = rows.map { it.pageId }.distinct().size,
                rowCount = rows.size,
            )
            publish()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            state = ConfirmUiState.Error(
                message = e.message ?: "读取待确认记录失败，请重试。",
                retryable = true,
            )
        }
    }

    fun accept(row: ConfirmRowView, tier: Tier? = null): MarkOutcome =
        applyOutcome(machine?.accept(row.key, tier))

    fun correct(row: ConfirmRowView, tier: Tier? = null): MarkOutcome =
        applyOutcome(machine?.correct(row.key, tier))

    fun supplement(row: ConfirmRowView, tier: Tier? = null): MarkOutcome =
        applyOutcome(machine?.supplement(row.key, tier))

    fun notReviewed(row: ConfirmRowView): MarkOutcome =
        applyOutcome(machine?.notReviewed(row.key))

    fun setCorrectedText(row: ConfirmRowView, text: String?) {
        if (submitted) return
        machine?.setCorrectedText(row.key, text)
        publish()
    }

    /**
     * 提交整批决策。
     *
     * - 存在未标记行时状态机先阻止（按钮禁用 + 本方法提示），UseCase 仍会再拒绝（C-079）；
     * - 部分题目被跳过时展示清单并重新加载剩余待确认行，不伪装成全部成功（C-078）。
     */
    suspend fun submit() {
        val current = machine ?: return
        if (submitting || submitted) return
        if (!current.canSubmit) {
            message = "还有 ${current.unmarkedCount} 行未标记"
            publish()
            return
        }

        submitting = true
        message = null
        report = null
        publish()

        try {
            val result = submitter.submit(sessionId, current.toDecisions())
            report = SubmitReport(
                writtenFeedbackCount = result.saveResult.writtenFeedbackCount,
                writtenDecisionLogCount = result.saveResult.writtenDecisionLogCount,
                skippedQuestionIds = result.saveResult.skippedQuestionIds,
            )
            submitting = false
            if (result.saveResult.skippedQuestionIds.isEmpty()) {
                submitted = true
                publish()
            } else {
                message = "部分题目被跳过，剩余待确认行已保留；请修复数据后重试。"
                reloadPendingRows()
            }
        } catch (e: CancellationException) {
            submitting = false
            throw e
        } catch (e: InvalidConfirmationException) {
            submitting = false
            message = e.message ?: "提交被拒绝，请检查逐行标记。"
            publish()
        } catch (e: Exception) {
            submitting = false
            message = e.message ?: "提交失败，请重试。"
            publish()
        }
    }

    private suspend fun reloadPendingRows() {
        try {
            val pending = scanSessionRepository.pendingRows(sessionId)
            if (pending.isEmpty()) {
                state = ConfirmUiState.Empty("没有待确认的复习记录。")
                return
            }
            val rows = pending.toConfirmRows()
            machine = ConfirmStateMachine(rows = rows, today = LocalDate.now(clock))
            header = header?.copy(
                pageCount = rows.map { it.pageId }.distinct().size,
                rowCount = rows.size,
            )
            publish()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            state = ConfirmUiState.Error(
                message = e.message ?: "重新读取待确认记录失败，请重试。",
                retryable = true,
            )
        }
    }

    private fun applyOutcome(outcome: MarkOutcome?): MarkOutcome {
        if (submitted) return MarkOutcome.Rejected("该扫描会话已提交")
        return when (outcome) {
            null -> MarkOutcome.Rejected("页面尚未加载完成")
            MarkOutcome.Applied -> {
                message = null
                publish()
                MarkOutcome.Applied
            }

            MarkOutcome.NeedsTier -> MarkOutcome.NeedsTier
            is MarkOutcome.Rejected -> {
                message = outcome.message
                publish()
                outcome
            }
        }
    }

    private suspend fun List<PendingFeedbackRow>.toConfirmRows(): List<ConfirmRow> {
        val catalog = knowledgePointRepository.catalog().associateBy { it.id }
        val questionNumbers = questionRepository.questionNumbers()
        val sourcePages = questionRepository.sourcePages()
        val today = LocalDate.now(clock)
        return map { it.toConfirmRow(catalog, questionNumbers, sourcePages, today) }
            .sortedWith(
                compareBy(
                    { it.pageId },
                    { it.knowledgeNumber ?: Int.MAX_VALUE },
                    { it.questionNumber },
                    { it.knowledgePointId },
                ),
            )
    }

    private fun PendingFeedbackRow.toConfirmRow(
        catalog: Map<String, KnowledgePointRef>,
        questionNumbers: Map<String, String>,
        sourcePages: Map<String, String>,
        today: LocalDate,
    ): ConfirmRow {
        val knowledgePoint = catalog[knowledgePointId]
        return ConfirmRow(
            planId = planId,
            pageId = pageId,
            plannedDate = plannedDate,
            actualReviewDate = actualReviewDate,
            questionId = questionId,
            questionNumber = questionNumbers[questionId] ?: questionId,
            sourcePage = sourcePages[questionId].orEmpty(),
            knowledgePointId = knowledgePointId,
            knowledgeNumber = knowledgePoint?.number,
            knowledgeName = knowledgePoint?.name,
            triggered = triggered,
            detectedTier = detectedTier,
            detectionStatus = detectionStatus,
            conflict = conflict,
            detectedRawOcr = detectedRawOcr,
        )
    }

    private fun publish() {
        val current = machine ?: return
        val currentHeader = header ?: return
        state = ConfirmUiState.Content(
            header = currentHeader,
            pages = current.rows
                .groupBy { it.pageId }
                .toSortedMap()
                .map { (pageId, rows) ->
                    ConfirmPageView(
                        pageId = pageId,
                        plannedDate = rows.first().plannedDate,
                        rows = rows.map { it.toView(current) },
                    )
                },
            unmarkedCount = current.unmarkedCount,
            canSubmit = current.canSubmit && !submitting && !submitted,
            submitting = submitting,
            submitted = submitted,
            report = report,
            message = message,
        )
    }

    private fun ConfirmRow.toView(machine: ConfirmStateMachine): ConfirmRowView {
        val decision = machine.decisionOf(key)
        return ConfirmRowView(
            questionId = questionId,
            knowledgePointId = knowledgePointId,
            questionNumber = questionNumber,
            sourcePage = sourcePage,
            knowledgeNumber = knowledgeNumber,
            knowledgeName = knowledgeName,
            triggered = triggered,
            detectedTier = detectedTier,
            detectionStatus = detectionStatus,
            conflict = conflict,
            detectedRawOcr = detectedRawOcr,
            plannedDate = plannedDate,
            actualReviewDate = actualReviewDate,
            isFuture = isFuture(machine.today),
            decision = decision.decision,
            selectedTier = decision.selectedTier,
            correctedText = decision.correctedText,
        )
    }
}
