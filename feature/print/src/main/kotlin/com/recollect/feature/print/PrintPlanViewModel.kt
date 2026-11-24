package com.recollect.feature.print

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate
import kotlinx.coroutines.CancellationException

/** 打印页的每日摘要；只用于展示，不参与排版。 */
data class PrintDaySummary(
    val date: LocalDate,
    val itemCount: Int,
)

sealed interface PrintPlanUiState {
    data object Empty : PrintPlanUiState

    data class Content(
        val input: PrintPlanInput,
        val pageCount: Int,
        val itemCount: Int,
        val days: List<PrintDaySummary>,
        val message: String? = null,
        val messageIsError: Boolean = false,
        /** C-094 方案 B：文档已生成，等待用户确认是否真的打印了纸张。 */
        val awaitingPrintedConfirmation: Boolean = false,
        /** 正在生成并分享 PDF；期间禁用分享按钮，避免并发写同一缓存文件。 */
        val pdfSharing: Boolean = false,
    ) : PrintPlanUiState
}

/**
 * 打印计划页状态（app.md：Print 页显示计划、调起系统打印、失败可恢复）。
 *
 * 不持有 Context；`PrintManager` 封装在 [PrintLauncher] 实现中（R-03）。
 * C-094 方案 B：文档写出成功只暂存快照，用户确认「已打印」后才写回题目快照。
 */
class PrintPlanViewModel(
    input: PrintPlanInput?,
    private val launcher: PrintLauncher,
    private val snapshotWriter: PrintSnapshotWriter,
    private val pdfSharer: PlanPdfSharer,
) {
    var state by mutableStateOf(initialState(input))
        private set

    private var pendingSnapshots: List<PrintedQuestionSnapshot>? = null

    fun print() {
        val current = state as? PrintPlanUiState.Content ?: return
        pendingSnapshots = null
        state = current.copy(awaitingPrintedConfirmation = false)
        val result = launcher.launch(current.input) { snapshots ->
            pendingSnapshots = snapshots
            val latest = state as? PrintPlanUiState.Content ?: return@launch
            state = latest.copy(
                awaitingPrintedConfirmation = snapshots.isNotEmpty(),
                message = "请确认是否已经完成打印：",
                messageIsError = false,
            )
        }
        state = when (result) {
            PrintLaunchResult.Launched -> current.copy(
                message = "已调起系统打印。",
                messageIsError = false,
            )

            is PrintLaunchResult.Failed -> current.copy(
                message = result.message,
                messageIsError = true,
                awaitingPrintedConfirmation = false,
            )
        }
    }

    /** 用户确认纸张已打印：把暂存快照写回题目。 */
    suspend fun confirmPrinted() {
        val snapshots = pendingSnapshots ?: return
        val current = state as? PrintPlanUiState.Content ?: return
        try {
            val result = snapshotWriter.save(current.input.planId, snapshots)
            pendingSnapshots = null
            state = when (result) {
                PrintSnapshotWriteResult.SAVED -> current.copy(
                    awaitingPrintedConfirmation = false,
                    message = "已记录本次打印。",
                    messageIsError = false,
                )

                PrintSnapshotWriteResult.IGNORED_NOT_ACTIVE -> current.copy(
                    awaitingPrintedConfirmation = false,
                    message = "本次计划已被新的计划替代，未更新打印快照。",
                    messageIsError = true,
                )
            }
        } catch (e: Exception) {
            state = current.copy(
                message = e.message ?: "记录打印快照失败，请重试。",
                messageIsError = true,
            )
        }
    }

    /**
     * 生成 PDF 并交给系统分享面板（微信、QQ 等）。
     *
     * 分享成功后与「已打印」确认走同一回写路径：写入每题 `pageId/qrPayload` 并把计划标记为
     * PRINTED，使分享出去、再打印成纸的稿件同样可以回读，首页标签也随之变为「已打印」。
     * 重复点击在 [PrintPlanUiState.Content.pdfSharing] 期间被忽略。
     */
    suspend fun sharePdf() {
        val current = state as? PrintPlanUiState.Content ?: return
        if (current.pdfSharing) return
        state = current.copy(pdfSharing = true, message = "正在生成 PDF…", messageIsError = false)
        val outcome = try {
            pdfSharer.share(current.input)
        } catch (e: CancellationException) {
            state = current.copy(pdfSharing = false)
            throw e
        } catch (e: Exception) {
            PdfShareOutcome.Failed(e.message ?: "生成 PDF 失败，请重试。")
        }
        val latest = state as? PrintPlanUiState.Content ?: return
        state = when (outcome) {
            is PdfShareOutcome.Shared -> {
                val record = recordSharedDocument(current.input, outcome.fileName)
                if (record.recorded) {
                    // 快照已按“已打印”落库，丢弃仍在等待确认的打印暂存，避免再次询问。
                    pendingSnapshots = null
                }
                latest.copy(
                    pdfSharing = false,
                    message = record.message,
                    messageIsError = record.isError,
                    awaitingPrintedConfirmation = if (record.recorded) {
                        false
                    } else {
                        latest.awaitingPrintedConfirmation
                    },
                )
            }

            is PdfShareOutcome.Failed -> latest.copy(
                pdfSharing = false,
                message = outcome.message,
                messageIsError = true,
            )
        }
    }

    /** 分享成功后的快照回写结果；与「已打印」使用同一 [PrintSnapshotWriter] 落库入口。 */
    private data class ShareRecord(
        val message: String,
        val isError: Boolean,
        val recorded: Boolean = false,
    )

    private suspend fun recordSharedDocument(input: PrintPlanInput, fileName: String): ShareRecord {
        return try {
            when (snapshotWriter.save(input.planId, input.printSnapshots())) {
                PrintSnapshotWriteResult.SAVED -> ShareRecord(
                    message = "已生成 $fileName，请在分享面板中选择应用；本次打印已记录。",
                    isError = false,
                    recorded = true,
                )

                PrintSnapshotWriteResult.IGNORED_NOT_ACTIVE -> ShareRecord(
                    message = "已生成 $fileName，但本次计划已被新的计划替代，未更新打印快照。",
                    isError = true,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ShareRecord(
                message = "已生成 $fileName，但记录本次打印失败：${e.message ?: "请重试。"}",
                isError = true,
            )
        }
    }

    /** 用户确认没有打印：丢弃快照，不写库。 */
    fun declinePrinted() {
        pendingSnapshots = null
        val current = state as? PrintPlanUiState.Content ?: return
        state = current.copy(
            awaitingPrintedConfirmation = false,
            message = "未记录本次打印。",
            messageIsError = false,
        )
    }

    private companion object {
        fun initialState(input: PrintPlanInput?): PrintPlanUiState {
            if (input == null || input.pages.isEmpty()) return PrintPlanUiState.Empty
            val days = input.pages
                .groupBy { it.date }
                .toSortedMap()
                .map { (date, pages) ->
                    PrintDaySummary(date = date, itemCount = pages.sumOf { it.items.size })
                }
            return PrintPlanUiState.Content(
                input = input,
                pageCount = input.pages.size,
                itemCount = input.pages.sumOf { it.items.size },
                days = days,
            )
        }
    }
}
