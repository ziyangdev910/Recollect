package com.recollect.feature.print

import com.recollect.pagination.PlannedPage
import com.recollect.pagination.RenderableQuestion
import com.recollect.pagination.TemplateGeometry

/**
 * 打印页的输入（R-02：打印层只消费分页结果，不重新分页）。
 *
 * 与 `feature:planning` 的 `PrintJob` 分离，避免打印 feature 依赖兄弟 feature；
 * app 层负责把 `PrintJob` 映射成本类型。
 */
data class PrintPlanInput(
    val planId: String,
    val pages: List<PlannedPage>,
    val questions: Map<String, RenderableQuestion>,
    val noteTexts: Map<String, List<String>>,
    val template: TemplateGeometry,
)

/** 打印成功后写回题目的最近一次打印快照（print-template.md：`pageId`/`qrPayload`）。 */
data class PrintedQuestionSnapshot(
    val questionId: String,
    val pageId: String,
    val qrPayload: String,
)

/** 从已构建的打印布局提取每题快照；纯函数，供适配器与单元测试共用。 */
fun PrintLayout.printSnapshots(): List<PrintedQuestionSnapshot> =
    pages.flatMap { page ->
        page.items.map { item ->
            PrintedQuestionSnapshot(
                questionId = item.questionId,
                pageId = page.pageId,
                qrPayload = item.qrPayload,
            )
        }
    }

/**
 * 由同一份计划输入重算每题快照。
 *
 * 分享 PDF 时用它写回 `pageId/qrPayload`，与系统打印的「已打印」确认走同一回写路径；
 * 布局只由 `PrintLayoutBuilder` 构建一次，不重新分页（R-02）。
 */
fun PrintPlanInput.printSnapshots(): List<PrintedQuestionSnapshot> =
    PrintLayoutBuilder.build(
        planId = planId,
        pages = pages,
        questions = questions,
        noteTexts = noteTexts,
        template = template,
    ).printSnapshots()

/**
 * 调起系统打印的端口；实现持有 Context/PrintManager，ViewModel 只依赖本接口（R-03）。
 *
 * C-094 方案 B：文档写出成功只通过 [onDocumentGenerated] 把快照交给 ViewModel，
 * 由用户在页面上确认「已打印」后再调用 [PrintSnapshotWriter] 落库。
 */
fun interface PrintLauncher {
    fun launch(
        input: PrintPlanInput,
        onDocumentGenerated: (List<PrintedQuestionSnapshot>) -> Unit,
    ): PrintLaunchResult
}

sealed interface PrintLaunchResult {
    data object Launched : PrintLaunchResult
    data class Failed(val message: String) : PrintLaunchResult
}

/**
 * 导出并分享 PDF 的端口；实现持有 `Context`/`FileProvider`/`Intent`，ViewModel 只依赖本接口（R-03）。
 *
 * 分享成功后由 `PrintPlanViewModel` 走与「已打印」相同的快照回写路径：
 * 分享出去的 PDF 就是可回读的打印稿，计划标记为 PRINTED，首页标签随之更新。
 */
fun interface PlanPdfSharer {
    suspend fun share(input: PrintPlanInput): PdfShareOutcome
}

sealed interface PdfShareOutcome {
    /** 已把 PDF 交给系统分享面板（无法得知用户最终选择了哪个目标）。 */
    data class Shared(val fileName: String) : PdfShareOutcome

    data class Failed(val message: String) : PdfShareOutcome
}

/** 打印文档写出成功后回写题目快照；实现放在 app 层，避免 feature 依赖数据库。 */
fun interface PrintSnapshotWriter {
    /**
     * 只为仍可用的计划写入快照；旧计划已被重新生成替代时返回 [PrintSnapshotWriteResult.IGNORED_NOT_ACTIVE]，
     * 不得覆盖最新计划的 `Question.pageId/qrPayload`。
     */
    suspend fun save(
        planId: String,
        snapshots: List<PrintedQuestionSnapshot>,
    ): PrintSnapshotWriteResult
}

enum class PrintSnapshotWriteResult {
    SAVED,
    IGNORED_NOT_ACTIVE,
}
