package com.recollect.feature.print

import android.content.Context
import android.print.PrintAttributes
import android.print.pdf.PrintedPdfDocument
import java.io.OutputStream

/**
 * 使用 `PrintedPdfDocument` 把打印布局写成 PDF（print-template.md「验收」第 1 条）。
 *
 * A4 纵向、300 DPI、页边距 0；实际页边距由布局在 Canvas 内处理。
 */
object PrintPdfWriter {

    fun a4PrintAttributes(): PrintAttributes = PrintAttributes.Builder()
        .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        .setResolution(PrintAttributes.Resolution("300dpi", "300 DPI", 300, 300))
        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        .build()

    fun write(
        context: Context,
        layout: PrintLayout,
        attributes: PrintAttributes,
        output: OutputStream,
    ) {
        val document = PrintedPdfDocument(context, attributes)
        try {
            val renderer = PrintRenderer()
            layout.pages.forEachIndexed { index, page ->
                val pdfPage = document.startPage(index)
                renderer.draw(pdfPage.canvas, page)
                document.finishPage(pdfPage)
            }
            document.writeTo(output)
        } finally {
            document.close()
        }
    }
}
