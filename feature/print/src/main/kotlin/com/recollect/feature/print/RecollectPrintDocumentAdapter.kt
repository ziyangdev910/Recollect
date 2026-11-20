package com.recollect.feature.print

import android.content.Context
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import com.recollect.pagination.PlannedPage
import com.recollect.pagination.RenderableQuestion
import com.recollect.pagination.TemplateGeometry

/**
 * `PrintDocumentAdapter`（print-template.md「职责边界」）。
 *
 * 只接收计划层 `PrintJob` 的字段（页/题目/笔记文本/模板），不在 `onLayout` 查库或重新分页（R-02）；
 * `onLayout` 只报告页数，固定 A4/300 DPI/0 页边距；没有可打印页时直接失败，不生成空白页（C-060）。
 */
class RecollectPrintDocumentAdapter(
    context: Context,
    planId: String,
    pages: List<PlannedPage>,
    questions: Map<String, RenderableQuestion>,
    noteTexts: Map<String, List<String>>,
    template: TemplateGeometry,
    /** 文档写出成功后回调；用于把 `pageId/qrPayload` 写回题目（print-template.md）。 */
    private val onWritten: ((List<PrintedQuestionSnapshot>) -> Unit)? = null,
) : PrintDocumentAdapter() {

    private val appContext: Context = context.applicationContext
    private val layout: PrintLayout = PrintLayoutBuilder.build(
        planId = planId,
        pages = pages,
        questions = questions,
        noteTexts = noteTexts,
        template = template,
    )
    private val attributes: PrintAttributes = PrintPdfWriter.a4PrintAttributes()

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback?,
        extras: android.os.Bundle?,
    ) {
        if (callback == null) return
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        if (layout.pages.isEmpty()) {
            callback.onLayoutFailed("没有可打印的页面；请先生成可打印的计划。")
            return
        }
        val info = PrintDocumentInfo.Builder("recollect-${layout.planId}.pdf")
            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
            .setPageCount(layout.pages.size)
            .build()
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor?,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback?,
    ) {
        if (callback == null) return
        if (destination == null) {
            callback.onWriteFailed("打印输出不可用。")
            return
        }
        if (cancellationSignal?.isCanceled == true) {
            callback.onWriteCancelled()
            return
        }
        try {
            ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output ->
                PrintPdfWriter.write(appContext, layout, attributes, output)
            }
            callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            // print-template.md：打印成功后写 Question.pageId/qrPayload。
            // 适配器不直接访问数据库；由调用方注入的回调决定落库方式。
            onWritten?.let { callbackSnapshot ->
                try {
                    callbackSnapshot(layout.printSnapshots())
                } catch (t: Throwable) {
                    android.util.Log.w(TAG, "打印快照回调失败", t)
                }
            }
        } catch (t: Throwable) {
            callback.onWriteFailed(t.message)
        }
    }

    private companion object {
        const val TAG = "RecollectPrint"
    }
}
