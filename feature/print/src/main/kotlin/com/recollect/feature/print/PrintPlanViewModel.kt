package com.recollect.feature.print

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate

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
                message = "打印文档已生成；请确认纸张是否已经打印。",
                messageIsError = false,
            )
        }
        state = when (result) {
            PrintLaunchResult.Launched -> current.copy(
                message = "已调起系统打印；完成或取消后返回本页。",
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
            snapshotWriter.save(snapshots)
            pendingSnapshots = null
            state = current.copy(
                awaitingPrintedConfirmation = false,
                message = "已记录本次打印。",
                messageIsError = false,
            )
        } catch (e: Exception) {
            state = current.copy(
                message = e.message ?: "记录打印快照失败，请重试。",
                messageIsError = true,
            )
        }
    }

    /** 用户确认没有打印：丢弃快照，不写库。 */
    fun declinePrinted() {
        pendingSnapshots = null
        val current = state as? PrintPlanUiState.Content ?: return
        state = current.copy(
            awaitingPrintedConfirmation = false,
            message = "未记录本次打印；如已打印，可再次打开系统打印。",
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
