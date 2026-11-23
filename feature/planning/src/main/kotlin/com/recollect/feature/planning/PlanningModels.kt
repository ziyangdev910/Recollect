package com.recollect.feature.planning

import com.recollect.data.room.entity.NoteAttachmentItem
import com.recollect.pagination.PlannedPage
import com.recollect.pagination.RenderableQuestion
import com.recollect.pagination.TemplateGeometry
import java.time.LocalDate

/** 计划生成设置（planning.md `PlanningSettings`）；缺失的每日分钟按 60 处理。 */
data class PlanningSettings(
    val planStartDate: LocalDate,
    val planEndDate: LocalDate,
    val dailyAvailableMinutes: Map<LocalDate, Int> = emptyMap(),
    val coolingPeriodDays: Int = 7,
    // C-091 方案 A：生产模板为四角定位标记预留 14mm 角区（内容区从 26mm 开始）。
    val template: TemplateGeometry = TemplateGeometry.a4Print(),
) {
    /**
     * 把「每日复习时长」设置铺满计划范围内的每一天。
     *
     * 首页设置的是一周中每天统一的分钟数；缺失日仍按 60 分钟的默认值由生成用例补齐，
     * 这里显式写入每一天，避免默认值与用户设置混用。
     */
    fun withDailyMinutes(minutes: Int): PlanningSettings {
        val days = generateSequence(planStartDate) { it.plusDays(1) }
            .takeWhile { !it.isAfter(planEndDate) }
        return copy(dailyAvailableMinutes = days.associateWith { minutes })
    }
}

/** 打印输入（planning.md `PrintJob`）；打印层只消费本对象，不重新分页（R-02）。 */
data class PrintJob(
    val planId: String,
    val pages: List<PlannedPage>,
    val questions: Map<String, RenderableQuestion>,
    val notes: Map<String, List<NoteAttachmentItem>>,
    val template: TemplateGeometry,
)

/**
 * 计划预览的题目标识：`页码-第X题`（X 为题号）。
 *
 * 标注了来源的题目可能在不同页码上出现相同题号，只显示题号无法区分；
 * 没有来源页码时退回 `第X题`，避免出现前导分隔符。
 */
internal fun formatQuestionLabel(sourcePage: String, questionNumber: String): String = when {
    questionNumber == UNKNOWN_QUESTION_NUMBER -> UNKNOWN_QUESTION_NUMBER
    sourcePage.isBlank() -> "第${questionNumber}题"
    else -> "$sourcePage-第${questionNumber}题"
}

/** 缺题号时的占位；预览只展示可读文字，不暴露题目 id。 */
internal const val UNKNOWN_QUESTION_NUMBER = "未知题号"

/** PlanPreview 的单页投影；不携带调度状态。 */
data class PlanPreviewPage(
    val pageNumber: Int,
    val date: LocalDate,
    /** 形如 `2-第3题`；没有来源页码时为 `第3题`。 */
    val questionLabels: List<String>,
)

/**
 * PlanPreview 的警告投影（C-065 方案 B）。
 *
 * 只保留中文标签与用户文案；分页枚举代码不进入界面层。
 */
data class PlanPreviewWarning(
    val questionId: String,
    val questionNumber: String,
    val label: String,
    val message: String,
)

/**
 * 被推迟题目的预览投影（冷却期 / 当天时间不足 / 关联知识点失效）。
 *
 * 这些题目不会出现在打印稿里；预览页用中文标签与说明告诉用户少排了哪些题、为什么。
 */
data class PlanPreviewDeferred(
    val questionId: String,
    /** 与页面卡片相同的标识格式：`页码-第X题`；无来源页码时为 `第X题`。 */
    val questionLabel: String,
    val label: String,
    val message: String,
)

data class PlanPreviewContent(
    val planId: String,
    val pages: List<PlanPreviewPage>,
    val warnings: List<PlanPreviewWarning>,
    val deferred: List<PlanPreviewDeferred> = emptyList(),
)

/**
 * 计划生成结果。
 *
 * `PaginationResult.Failure` 不在本结果中出现：计划层按 C-047 抛 `PlanGenerationException`，
 * App 映射为可重试错误，且不写库。
 */
sealed interface PlanGenerationOutcome {
    data class Planned(
        val printJob: PrintJob,
        val preview: PlanPreviewContent,
    ) : PlanGenerationOutcome

    /** C-060：整份计划没有可打印题目；仍携带用户提示与被推迟题目，不生成空白页。 */
    data class Empty(
        val message: String,
        val warnings: List<PlanPreviewWarning>,
        val deferred: List<PlanPreviewDeferred> = emptyList(),
    ) : PlanGenerationOutcome
}
