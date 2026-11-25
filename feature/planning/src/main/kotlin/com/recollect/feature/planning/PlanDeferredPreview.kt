package com.recollect.feature.planning

import com.recollect.common.UNKNOWN_QUESTION_NUMBER
import com.recollect.common.formatQuestionLabel
import com.recollect.data.room.repository.PlanDeferredRecord
import com.recollect.scheduler.DeferReason
import com.recollect.scheduler.DeferredItem

/**
 * 调度器推迟原因 → 预览页中文文案。
 *
 * 界面只展示中文标签与说明，不展示 `DeferReason` 内部枚举名（与 C-065 的警告投影一致）。
 */
internal fun DeferReason.previewLabel(): String = when (this) {
    DeferReason.COOLING -> "冷却期"
    DeferReason.TIME_OVERFLOW -> "复习时间不足"
    DeferReason.UNRESOLVED_KNOWLEDGE -> "知识点缺失"
}

internal fun DeferReason.previewMessage(): String = when (this) {
    DeferReason.COOLING ->
        "该知识点上次复习后仍在7天冷却期内，到期日已顺延1天。"
    DeferReason.TIME_OVERFLOW ->
        "请缩短本题预估复习时长或延长每日复习时长，重新生成计划。"
    DeferReason.UNRESOLVED_KNOWLEDGE ->
        "请检查并重新关联知识点。"
}

/** 生成时的推迟项 → 预览投影（题号/来源页来自题库投影，缺失时沿用“未知题号”）。 */
internal fun DeferredItem.toPreviewDeferred(
    questionNumbers: Map<String, String>,
    sourcePages: Map<String, String>,
): PlanPreviewDeferred = PlanPreviewDeferred(
    questionId = questionId,
    questionLabel = formatQuestionLabel(
        sourcePage = sourcePages[questionId].orEmpty(),
        questionNumber = questionNumbers[questionId] ?: UNKNOWN_QUESTION_NUMBER,
    ),
    label = reason.previewLabel(),
    message = reason.previewMessage(),
)

/** 生成时的推迟项 → 落库快照（题号与来源页随计划冻结，重开计划不再查题库）。 */
internal fun DeferredItem.toDeferredRecord(
    questionNumbers: Map<String, String>,
    sourcePages: Map<String, String>,
): PlanDeferredRecord = PlanDeferredRecord(
    questionId = questionId,
    questionNumber = questionNumbers[questionId] ?: UNKNOWN_QUESTION_NUMBER,
    sourcePage = sourcePages[questionId].orEmpty(),
    reason = reason,
)

/**
 * 已持久化的推迟快照 → 预览投影。
 *
 * [fallbackSourcePages] 只在快照缺来源页码时使用（改动前生成的旧计划），
 * 让「未排入计划」卡片与页面卡片一样显示 `来源 第X页 第Y题`。
 */
internal fun PlanDeferredRecord.toPreviewDeferred(
    fallbackSourcePages: Map<String, String> = emptyMap(),
): PlanPreviewDeferred =
    PlanPreviewDeferred(
        questionId = questionId,
        questionLabel = formatQuestionLabel(
            sourcePage = sourcePage.ifBlank { fallbackSourcePages[questionId].orEmpty() },
            questionNumber = questionNumber,
        ),
        label = reason.previewLabel(),
        message = reason.previewMessage(),
    )
