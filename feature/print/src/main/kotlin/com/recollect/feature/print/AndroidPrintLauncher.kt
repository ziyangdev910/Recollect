package com.recollect.feature.print

import android.content.Context
import android.print.PrintManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 系统打印适配器实现（R-03：`PrintManager`/`Context` 只在这里出现，ViewModel 不持有）。
 *
 * C-094 方案 B：文档写出成功只把每题的 `pageId/qrPayload` 交给调用方，
 * 不在适配器里直接落库；用户确认「已打印」后由 ViewModel 调用 `PrintSnapshotWriter`。
 */
class AndroidPrintLauncher(
    private val context: Context,
    private val scope: CoroutineScope,
) : PrintLauncher {

    override fun launch(
        input: PrintPlanInput,
        onDocumentGenerated: (List<PrintedQuestionSnapshot>) -> Unit,
    ): PrintLaunchResult {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: return PrintLaunchResult.Failed("系统打印服务不可用。")
        return try {
            printManager.print(
                "拾知周计划 ${input.planId}",
                RecollectPrintDocumentAdapter(
                    context = context,
                    planId = input.planId,
                    pages = input.pages,
                    questions = input.questions,
                    noteTexts = input.noteTexts,
                    template = input.template,
                    onWritten = { snapshots ->
                        // onWrite 在 binder 线程完成，回到主线程再更新 Compose 状态。
                        scope.launch(Dispatchers.Main) { onDocumentGenerated(snapshots) }
                    },
                ),
                PrintPdfWriter.a4PrintAttributes(),
            )
            PrintLaunchResult.Launched
        } catch (t: Throwable) {
            PrintLaunchResult.Failed(t.message ?: "无法调起系统打印。")
        }
    }
}
