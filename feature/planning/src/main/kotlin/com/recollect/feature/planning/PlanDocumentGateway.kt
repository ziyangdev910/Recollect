package com.recollect.feature.planning

import androidx.compose.ui.graphics.ImageBitmap

/**
 * 「生成计划」页上的可打印 PDF 预览（2026-11-27 用户要求）。
 *
 * 页面直接展示与分享 PDF 相同的打印布局；实现位于 app 层，
 * 由 `feature:print` 的 `PrintLayoutBuilder` / `PrintRenderer` 绘制，不重新分页（R-02）。
 */
sealed interface PlanPdfPreview {
    data class Ready(val pages: List<ImageBitmap>) : PlanPdfPreview

    data class Failed(val message: String) : PlanPdfPreview
}

/** PDF 分享结果；[snapshotRecorded] 为 true 时本次分享已按「已打印」写入 pageId/QR。 */
sealed interface PlanShareResult {
    data class Shared(
        val fileName: String,
        val snapshotRecorded: Boolean,
        val message: String,
        val isError: Boolean,
    ) : PlanShareResult

    data class Failed(val message: String) : PlanShareResult
}

/**
 * 打印稿的预览、分享与演示回写端口。
 *
 * 持有 `Context`/`FileProvider`/`Intent` 的实现放在 app 层（R-03）；
 * ViewModel 只依赖本接口，因此可在 JVM 单测中注入空实现。
 */
interface PlanDocumentGateway {
    suspend fun preview(printJob: PrintJob): PlanPdfPreview

    suspend fun share(printJob: PrintJob): PlanShareResult

    /** 首次演示：把当前布局按「已打印」写回快照，不打开系统分享面板。 */
    suspend fun recordDemoPrinted(printJob: PrintJob): Boolean
}
