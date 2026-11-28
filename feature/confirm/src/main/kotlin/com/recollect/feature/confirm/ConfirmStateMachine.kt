package com.recollect.feature.confirm

import com.recollect.data.room.entity.DetectionStatus
import com.recollect.data.room.entity.UserDecision
import com.recollect.data.room.repository.ConfirmedDecision
import com.recollect.model.Tier
import java.time.LocalDate

/** 确认页一行的稳定标识（与 `PendingFeedback` 复合主键一致）。 */
data class ConfirmRowKey(
    val questionId: String,
    val knowledgePointId: String,
)

/**
 * 确认页的只读行投影（confirm-flow.md「页面结构」）。
 *
 * 只包含渲染与决策所需的字段；知识点状态不在这里更新（R-04）。
 */
data class ConfirmRow(
    val planId: String,
    val pageId: String,
    val plannedDate: LocalDate,
    val actualReviewDate: LocalDate?,
    val questionId: String,
    val questionNumber: String,
    /** 题目来源页码（试卷/作业本第几面），不是打印稿 pageId。 */
    val sourcePage: String = "",
    val knowledgePointId: String,
    val knowledgeNumber: Int?,
    val knowledgeName: String?,
    val triggered: Boolean,
    val detectedTier: Tier?,
    val detectionStatus: DetectionStatus,
    val conflict: Boolean,
    val detectedRawOcr: String?,
) {
    val key: ConfirmRowKey
        get() = ConfirmRowKey(questionId = questionId, knowledgePointId = knowledgePointId)

    /** 计划日晚于确认当天时只能标记本次未复习（confirm-flow.md「提交前」）。 */
    fun isFuture(today: LocalDate): Boolean = plannedDate.isAfter(today)
}

/**
 * 用户在弹窗里选择、但尚未通过卡片「确认」落定的复习效果（2026-11-28 用户要求）。
 *
 * 修改掌握程度只暂存选择，卡片仍显示「确认」按钮，必须手动确认完成校对；
 * 已确认的行锁定效果，不再接受修改。
 */
sealed interface EffectChoice {
    data class Rated(val tier: Tier) : EffectChoice
    data object NotReviewed : EffectChoice
}

/** 单行当前编辑状态；[decision] 为 `UNMARKED` 时表示尚未标记。 */
data class RowDecision(
    val decision: UserDecision = UserDecision.UNMARKED,
    val selectedTier: Tier? = null,
    val correctedText: String? = null,
    val pendingEffect: EffectChoice? = null,
)

/** 逐项标记的结果；[NeedsTier] 表示需要界面弹出档位（掌握 / 较熟悉 / 不会 / 未复习）选择。 */
sealed interface MarkOutcome {
    data object Applied : MarkOutcome
    data object NeedsTier : MarkOutcome
    data class Rejected(val message: String) : MarkOutcome
}

/**
 * 确认页状态机（confirm-flow.md「状态机」、D-13、R-04）。
 *
 * 纯 Kotlin：不读写数据库、不持有 Context。逐行决策独立保存，**不提供**一键全部接受；
 * 只有所有行都完成有效标记时才允许提交。提交前校验仍由 `ApplyFeedbackUseCase` 复核（C-079）。
 */
class ConfirmStateMachine(
    val rows: List<ConfirmRow>,
    val today: LocalDate,
) {
    init {
        require(rows.map { it.key }.distinct().size == rows.size) {
            "确认行重复：同一题目与知识点在一份确认中只能出现一次"
        }
    }

    private val rowByKey = rows.associateBy { it.key }

    /**
     * 修正文本默认填入 OCR 识别原文（2026-11-22 用户要求）：确认页只显示一个可编辑文本框，
     * 用户无需再次抄写；原始 OCR 仍单独保存在 `detectedRawOcr`，编辑只改 `correctedText`。
     */
    private val decisions = rows.associate {
        it.key to RowDecision(correctedText = it.detectedRawOcr?.ifBlank { null })
    }.toMutableMap()

    /** 未标记行数；确认页底部提示与提交按钮禁用都依赖它。 */
    val unmarkedCount: Int
        get() = rows.count { decisions.getValue(it.key).decision == UserDecision.UNMARKED }

    /** 所有行都完成有效标记时才可提交（界面层双保险；UseCase 仍会复核）。 */
    val canSubmit: Boolean
        get() = rows.isNotEmpty() && rows.all { row ->
            val decision = decisions.getValue(row.key)
            val decisionValid = when (decision.decision) {
                UserDecision.UNMARKED -> false
                UserDecision.NOT_REVIEWED -> true
                UserDecision.ACCEPT ->
                    row.detectedTier != null || decision.selectedTier != null
                UserDecision.CORRECTED ->
                    decision.selectedTier != null
            }
            val dateValid = decision.decision == UserDecision.NOT_REVIEWED ||
                (row.actualReviewDate ?: row.plannedDate) <= today
            decisionValid && dateValid
        }

    fun decisionOf(key: ConfirmRowKey): RowDecision =
        decisions[key] ?: error("未知的确认行：$key")

    /**
     * 接受：优先使用 OMR 档位；OMR 为空时必须先选择档位（C-022）。
     *
     * @param tier 仅在 [ConfirmRow.detectedTier] 为空时参与。
     */
    fun accept(key: ConfirmRowKey, tier: Tier? = null): MarkOutcome {
        val row = rowByKey[key] ?: return unknownRow(key)
        rejectFuture(row)?.let { return it }
        val existing = decisions.getValue(key)
        val next = when {
            row.detectedTier != null -> existing.copy(
                decision = UserDecision.ACCEPT,
                selectedTier = null,
                pendingEffect = null,
            )
            tier != null -> existing.copy(
                decision = UserDecision.ACCEPT,
                selectedTier = tier,
                pendingEffect = null,
            )
            else -> return MarkOutcome.NeedsTier
        }
        decisions[key] = next
        return MarkOutcome.Applied
    }

    /** 修改复习效果：必须选择档位；保留已填写的修正文本。 */
    fun correct(key: ConfirmRowKey, tier: Tier?): MarkOutcome {
        val row = rowByKey[key] ?: return unknownRow(key)
        rejectFuture(row)?.let { return it }
        if (tier == null) return MarkOutcome.NeedsTier
        decisions[key] = decisions.getValue(key).copy(
            decision = UserDecision.CORRECTED,
            selectedTier = tier,
            pendingEffect = null,
        )
        return MarkOutcome.Applied
    }

    /** 本次未复习：未来日期的行也允许；只写决定日志（D-13、C-010）。 */
    fun notReviewed(key: ConfirmRowKey): MarkOutcome {
        rowByKey[key] ?: return unknownRow(key)
        decisions[key] = decisions.getValue(key).copy(
            decision = UserDecision.NOT_REVIEWED,
            selectedTier = null,
            pendingEffect = null,
        )
        return MarkOutcome.Applied
    }

    /**
     * 在弹窗里选择复习效果：只暂存为待确认状态，**不**直接落定。
     *
     * 修改掌握程度不能自动确认；已确认的行锁定效果，不接受再次修改（2026-11-28 用户要求）。
     */
    fun setPendingEffect(key: ConfirmRowKey, effect: EffectChoice?) {
        rowByKey[key] ?: return
        val existing = decisions.getValue(key)
        // 已确认行不允许再改效果。
        if (existing.decision != UserDecision.UNMARKED) return
        decisions[key] = existing.copy(pendingEffect = effect)
    }

    /**
     * 卡片「确认」：把该行落定为最终决策。
     *
     * 优先使用弹窗中暂存的效果；没有暂存时接受识别档位；
     * 无识别档位则需要用户先在弹窗里选择（[MarkOutcome.NeedsTier]）。
     */
    fun confirm(key: ConfirmRowKey): MarkOutcome {
        val row = rowByKey[key] ?: return unknownRow(key)
        val state = decisions.getValue(key)
        if (state.decision != UserDecision.UNMARKED) return MarkOutcome.Applied
        return when (val effect = state.pendingEffect) {
            is EffectChoice.Rated ->
                if (row.detectedTier == effect.tier) accept(key) else correct(key, effect.tier)
            EffectChoice.NotReviewed -> notReviewed(key)
            null -> when {
                // 未来日期的行只能标记本次未复习，确认即按唯一可用选项落定。
                row.isFuture(today) -> notReviewed(key)
                row.detectedTier != null -> accept(key)
                else -> MarkOutcome.NeedsTier
            }
        }
    }

    /** 修正文本可编辑；原始 OCR 不经过这里（D-06）。 */
    fun setCorrectedText(key: ConfirmRowKey, text: String?) {
        rowByKey[key] ?: return
        decisions[key] = decisions.getValue(key).copy(
            correctedText = text?.ifBlank { null },
        )
    }

    /**
     * 生成提交给 `ApplyFeedbackUseCase` 的逐行决策。
     *
     * `NOT_REVIEWED` 携带空的 `actualReviewDate`，确保只写决定日志、不写复习日；
     * 其余行默认使用行上的实际复习日（批次默认等于计划日）。
     *
     * @throws IllegalStateException 仍有未标记或无效行时。
     */
    fun toDecisions(): List<ConfirmedDecision> {
        check(canSubmit) { "存在未标记或未完成的行，不能提交（D-13）" }
        return rows.map { row ->
            val decision = decisions.getValue(row.key)
            ConfirmedDecision(
                planId = row.planId,
                pageId = row.pageId,
                plannedDate = row.plannedDate,
                actualReviewDate = if (decision.decision == UserDecision.NOT_REVIEWED) {
                    null
                } else {
                    row.actualReviewDate ?: row.plannedDate
                },
                questionId = row.questionId,
                knowledgePointId = row.knowledgePointId,
                triggered = row.triggered,
                decision = decision.decision,
                detectedTier = row.detectedTier,
                selectedTier = decision.selectedTier,
                detectedRawOcr = row.detectedRawOcr,
                correctedText = decision.correctedText,
            )
        }
    }

    private fun rejectFuture(row: ConfirmRow): MarkOutcome.Rejected? =
        if (row.isFuture(today)) {
            MarkOutcome.Rejected("未来日期的行只能标记“本次未复习”")
        } else {
            null
        }

    private fun unknownRow(key: ConfirmRowKey): MarkOutcome.Rejected =
        MarkOutcome.Rejected("确认行不存在：${key.questionId}/${key.knowledgePointId}")
}
