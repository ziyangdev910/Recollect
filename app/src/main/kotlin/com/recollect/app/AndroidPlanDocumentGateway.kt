package com.recollect.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import com.recollect.feature.planning.PlanDocumentGateway
import com.recollect.feature.planning.PlanPdfPreview
import com.recollect.feature.planning.PlanShareResult
import com.recollect.feature.planning.PrintJob
import com.recollect.feature.print.PdfShareOutcome
import com.recollect.feature.print.PlanPdfSharer
import com.recollect.feature.print.PrintLayoutBuilder
import com.recollect.feature.print.PrintRenderer
import com.recollect.feature.print.PrintSnapshotWriteResult
import com.recollect.feature.print.PrintSnapshotWriter
import com.recollect.feature.print.printSnapshots
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 「生成计划」页的打印稿端口实现（2026-11-27 用户要求）。
 *
 * 预览与分享复用 `feature:print` 的同一份 A4 布局（R-02：不重新分页）；
 * `PdfRenderer`/`FileProvider`/`Context` 只留在 app 层，ViewModel 不持有 Context（R-03）。
 * 分享成功后按与「已打印」相同的入口写入 `Question.pageId/qrPayload`。
 */
class AndroidPlanDocumentGateway(
    context: Context,
    private val pdfSharer: PlanPdfSharer,
    private val snapshotWriter: PrintSnapshotWriter,
) : PlanDocumentGateway {

    private val appContext: Context = context.applicationContext

    override suspend fun preview(printJob: PrintJob): PlanPdfPreview =
        withContext(Dispatchers.Default) {
            try {
                val input = printJob.toPrintPlanInput()
                val layout = PrintLayoutBuilder.build(
                    planId = input.planId,
                    pages = input.pages,
                    questions = input.questions,
                    noteTexts = input.noteTexts,
                    template = input.template,
                )
                if (layout.pages.isEmpty()) return@withContext PlanPdfPreview.Ready(emptyList())

                val renderer = PrintRenderer()
                val pages = layout.pages.map { page ->
                    val pxPerMm = PREVIEW_WIDTH_PX / page.pageWidthMm
                    val width = PREVIEW_WIDTH_PX
                    val height = (page.pageHeightMm * pxPerMm).roundToInt()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    renderer.draw(canvas, page, pxPerMm)
                    bitmap.asImageBitmap()
                }
                PlanPdfPreview.Ready(pages)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                PlanPdfPreview.Failed(e.message ?: "打印稿预览生成失败，请重试。")
            }
        }

    override suspend fun share(printJob: PrintJob): PlanShareResult {
        val input = printJob.toPrintPlanInput()
        val outcome = try {
            pdfSharer.share(input)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PdfShareOutcome.Failed(e.message ?: "生成 PDF 失败，请重试。")
        }
        return when (outcome) {
            is PdfShareOutcome.Failed -> PlanShareResult.Failed(outcome.message)

            is PdfShareOutcome.Shared -> {
                var recorded = false
                var message: String
                var isError = false
                try {
                    when (snapshotWriter.save(input.planId, input.printSnapshots())) {
                        PrintSnapshotWriteResult.SAVED -> {
                            recorded = true
                            message = "已生成 ${outcome.fileName}，请在分享面板中选择应用；本次打印已记录。"
                        }

                        PrintSnapshotWriteResult.IGNORED_NOT_ACTIVE -> {
                            isError = true
                            message = "已生成 ${outcome.fileName}，但本次计划已被新的计划替代，未更新打印快照。"
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    isError = true
                    message = "已生成 ${outcome.fileName}，但记录本次打印失败：${e.message ?: "请重试。"}"
                }
                PlanShareResult.Shared(
                    fileName = outcome.fileName,
                    snapshotRecorded = recorded,
                    message = message,
                    isError = isError,
                )
            }
        }
    }

    override suspend fun recordDemoPrinted(printJob: PrintJob): Boolean {
        val input = printJob.toPrintPlanInput()
        return try {
            snapshotWriter.save(input.planId, input.printSnapshots()) ==
                PrintSnapshotWriteResult.SAVED
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    private companion object {
        /** 预览页图宽度：足以在手机上清晰显示 A4 细节，又不至于为多页占用过多内存。 */
        const val PREVIEW_WIDTH_PX = 800
    }
}
