package com.recollect.data.room.repository

import com.recollect.data.room.entity.UserDecision
import com.recollect.model.Tier
import java.time.LocalDate

/**
 * 确认页逐行决策（feedback-usecase.md 的 `ConfirmedDecision`）。
 *
 * `NOT_REVIEWED` 只写 `ConfirmationDecisionLog`，不写 `ReviewFeedback`（D-13）。
 */
data class ConfirmedDecision(
    val planId: String,
    val pageId: String,
    val plannedDate: LocalDate,
    val actualReviewDate: LocalDate?, // 默认 = plannedDate；NOT_REVIEWED 可为 null（C-038/C-039）
    val questionId: String,
    val knowledgePointId: String,
    val triggered: Boolean,
    val decision: UserDecision,
    val detectedTier: Tier?,
    val selectedTier: Tier?,
    val detectedRawOcr: String?,
    val correctedText: String?,
)

/**
 * 解析本行要落库的有效档位（feedback-usecase.md 处理顺序第 3 步，C-022）。
 *
 * `ACCEPT` 优先使用 OMR 档位，OMR 为空时用用户选择的档位；`CORRECTED` 使用用户选择的档位。
 * 状态更新用例与写入入口共用这一份规则，避免两处漂移（C-083 方案 C）。
 */
fun ConfirmedDecision.effectiveTier(): Tier? = when (decision) {
    UserDecision.ACCEPT -> detectedTier ?: selectedTier
    UserDecision.CORRECTED -> selectedTier
    UserDecision.NOT_REVIEWED, UserDecision.UNMARKED -> null
}

/**
 * 待写入的笔记附件条目。
 *
 * 是否保存由确认页/UseCase 决定（planning.md「笔记附件生命周期」）；Repository 只负责
 * 在同一事务内落库，不把每条修正文本自动变成打印附件。
 */
data class NoteAttachmentDraft(
    val questionId: String,
    val knowledgePointId: String, // 用于关联本事务写入的 ReviewFeedback
    val text: String,
    val imageUri: String? = null,
)

/**
 * 提交结果。[skippedQuestionIds]/[skippedKnowledgePointIds] 是 C-055 方案 C 的可见记录：
 * 关联 id 解析不到的整题被跳过，未写任何反馈，也不补默认知识点。
 */
data class SaveDecisionsResult(
    val writtenFeedbackCount: Int,
    val writtenDecisionLogCount: Int,
    val skippedQuestionIds: List<String>,
    val skippedKnowledgePointIds: List<String>,
)
