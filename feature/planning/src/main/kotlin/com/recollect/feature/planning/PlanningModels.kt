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
)

/** 打印输入（planning.md `PrintJob`）；打印层只消费本对象，不重新分页（R-02）。 */
data class PrintJob(
    val planId: String,
    val pages: List<PlannedPage>,
    val questions: Map<String, RenderableQuestion>,
    val notes: Map<String, List<NoteAttachmentItem>>,
    val template: TemplateGeometry,
)

/** PlanPreview 的单页投影；不携带调度状态。 */
data class PlanPreviewPage(
    val pageNumber: Int,
    val date: LocalDate,
    val questionNumbers: List<String>,
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

data class PlanPreviewContent(
    val planId: String,
    val pages: List<PlanPreviewPage>,
    val warnings: List<PlanPreviewWarning>,
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

    /** C-060：整份计划没有可打印题目；仍携带用户提示，不生成空白页。 */
    data class Empty(
        val message: String,
        val warnings: List<PlanPreviewWarning>,
    ) : PlanGenerationOutcome
}
