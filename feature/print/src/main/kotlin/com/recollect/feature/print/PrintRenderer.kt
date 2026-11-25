package com.recollect.feature.print

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

/**
 * 把 [PrintPageLayout] 绘制到 Canvas（print-template.md）。
 *
 * 渲染层只读布局结果，不重新分页、不压缩行高；放不下由 `PagePlanner` 决定移页或溢出（R-02）。
 * 默认坐标换算使用 PDF 点（72/25.4 mm），由 `PrintedPdfDocument` 输出矢量文本；
 * 若需要 300 DPI 位图，可传入 `300 / 25.4`。
 */
class PrintRenderer {

    fun draw(
        canvas: Canvas,
        page: PrintPageLayout,
        pxPerMm: Double = PDF_POINTS_PER_MM,
    ) {
        canvas.drawColor(Color.WHITE)

        page.dateHeading?.let { drawText(canvas, it, pxPerMm) }
        page.headerCells.forEach { cell -> drawText(canvas, cell, pxPerMm) }

        page.items.forEach { item -> drawItem(canvas, item, pxPerMm) }

        // 四角定位标记最后绘制，保证白边与标记不被正文覆盖。
        page.arucoMarkers.forEach { marker -> drawAruco(canvas, marker, pxPerMm) }
    }

    private fun drawItem(canvas: Canvas, item: PrintItemLayout, pxPerMm: Double) {
        val bubblePaint = strokePaint(pxPerMm)
        item.left.rows.forEach { row ->
            drawText(canvas, row.indexText, pxPerMm)
            drawText(canvas, row.nameText, pxPerMm)
            row.bubbles.forEach { bubble ->
                canvas.drawRect(
                    rect(
                        bubble.leftMm,
                        bubble.topMm,
                        bubble.leftMm + bubble.widthMm,
                        bubble.topMm + bubble.heightMm,
                        pxPerMm,
                    ),
                    bubblePaint,
                )
            }
        }

        // 2026-11-25 用户要求：每日题序徽标绘制在题目信息区左上角。
        drawText(canvas, item.questionIndexText, pxPerMm)
        drawText(canvas, item.rightTop.pageLine, pxPerMm)
        drawText(canvas, item.rightTop.knowledgeText, pxPerMm)

        // 2026-11-24：历史笔记显示区在书写区上方通栏绘制。
        item.notesDisplay?.texts?.forEach { text -> drawText(canvas, text, pxPerMm) }

        item.note?.let { note ->
            drawText(canvas, note.title, pxPerMm)
            val linePaint = strokePaint(pxPerMm, widthMm = 0.2)
            note.writingLines.forEach { line ->
                val y = (line.topMm * pxPerMm).toFloat()
                canvas.drawLine(
                    (line.leftMm * pxPerMm).toFloat(),
                    y,
                    (line.rightMm * pxPerMm).toFloat(),
                    y,
                    linePaint,
                )
            }
        }

        // C-095 方案 B：QR 下方以 2.2mm 小字右对齐显示完整 pageId，供扫码失败时手动输入。
        drawText(canvas, item.qr.pageIdLabel, pxPerMm)

        // 每题 QR：题块底部预留区右侧，载荷由布局层生成（D-09、C-030）。
        QrCodeRenderer.draw(
            canvas = canvas,
            payload = item.qrPayload,
            leftMm = item.qr.leftMm,
            topMm = item.qr.topMm,
            sizeMm = item.qr.sizeMm,
            pxPerMm = pxPerMm,
        )
    }

    private fun drawAruco(canvas: Canvas, marker: ArucoMarkerLayout, pxPerMm: Double) {
        val code = marker.code
        val quiet = marker.quietZoneMm
        // 4mm 白边（quiet zone），避免邻近文字进入定位标记。
        canvas.drawRect(
            rect(
                code.leftMm - quiet,
                code.topMm - quiet,
                code.rightMm + quiet,
                code.bottomMm + quiet,
                pxPerMm,
            ),
            fillPaint(Color.WHITE),
        )
        val modules = ArucoPattern.modulesFor(marker.id) ?: return
        val cellMm = (code.rightMm - code.leftMm) / ArucoPattern.MODULES
        val black = fillPaint(Color.BLACK)
        for (row in 0 until ArucoPattern.MODULES) {
            for (col in 0 until ArucoPattern.MODULES) {
                if (!modules[row][col]) continue
                val left = code.leftMm + col * cellMm
                val top = code.topMm + row * cellMm
                canvas.drawRect(rect(left, top, left + cellMm, top + cellMm, pxPerMm), black)
            }
        }
    }

    private fun drawText(canvas: Canvas, block: PrintText, pxPerMm: Double) {
        if (block.text.isEmpty() || block.maxLines <= 0) return
        val widthPx = (block.widthMm * pxPerMm).toFloat()

        val candidates = (listOf(block.fontMm) + block.fontFallbacksMm).distinct()
        var paint = textPaint(candidates.first(), block.bold, pxPerMm)
        var laidOut = layoutText(paint, block.text, widthPx, block.maxLines)
        if (laidOut.truncated) {
            for (candidate in candidates.drop(1)) {
                val nextPaint = textPaint(candidate, block.bold, pxPerMm)
                val nextLayout = layoutText(nextPaint, block.text, widthPx, block.maxLines)
                paint = nextPaint
                laidOut = nextLayout
                if (!nextLayout.truncated) break
            }
        }

        val lineHeightPx = (block.heightMm / block.maxLines * pxPerMm).toFloat()
        val metrics = paint.fontMetrics
        val textOffset = (lineHeightPx - (metrics.descent - metrics.ascent)) / 2 - metrics.ascent
        val leftPx = (block.leftMm * pxPerMm).toFloat()
        laidOut.lines.forEachIndexed { index, line ->
            val baseline = (block.topMm * pxPerMm).toFloat() + index * lineHeightPx + textOffset
            val lineLeftPx = when (block.align) {
                PrintTextAlign.Left -> leftPx
                PrintTextAlign.Center -> leftPx + (widthPx - paint.measureText(line)) / 2f
                PrintTextAlign.Right -> leftPx + widthPx - paint.measureText(line)
            }
            canvas.drawText(line, lineLeftPx, baseline, paint)
        }
    }

    /**
     * 折行并限制视觉行数；最后一行放不下时以“…”截断（print-template.md）。
     */
    private fun layoutText(
        paint: Paint,
        text: String,
        widthPx: Float,
        maxLines: Int,
    ): LaidOutText {
        val all = breakLines(paint, text, widthPx)
        if (all.size <= maxLines) return LaidOutText(all, truncated = false)
        val kept = all.take(maxLines).toMutableList()
        var last = kept.last()
        while (last.isNotEmpty() && paint.measureText("$last…") > widthPx) {
            last = last.dropLast(1)
        }
        kept[kept.lastIndex] = "$last…"
        return LaidOutText(kept, truncated = true)
    }

    private data class LaidOutText(
        val lines: List<String>,
        val truncated: Boolean,
    )

    private fun breakLines(paint: Paint, text: String, widthPx: Float): List<String> {
        val lines = mutableListOf<String>()
        var remaining = text
        while (remaining.isNotEmpty()) {
            val measured = paint.breakText(remaining, true, widthPx, null)
            if (measured <= 0) {
                lines += remaining
                break
            }
            lines += remaining.substring(0, measured)
            remaining = remaining.substring(measured)
        }
        return lines
    }

    private fun textPaint(fontMm: Double, bold: Boolean, pxPerMm: Double): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = (fontMm * pxPerMm).toFloat()
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    private fun strokePaint(pxPerMm: Double, widthMm: Double = 0.3): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = (widthMm * pxPerMm).toFloat()
        }

    private fun fillPaint(color: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }

    private fun rect(
        leftMm: Double,
        topMm: Double,
        rightMm: Double,
        bottomMm: Double,
        pxPerMm: Double,
    ): RectF = RectF(
        (leftMm * pxPerMm).toFloat(),
        (topMm * pxPerMm).toFloat(),
        (rightMm * pxPerMm).toFloat(),
        (bottomMm * pxPerMm).toFloat(),
    )

    companion object {
        /** PDF 点：1 英寸 = 25.4mm = 72pt。 */
        const val PDF_POINTS_PER_MM = 72.0 / 25.4

        /** 扫描阶段按 300 DPI 渲染位图（print-template.md）。 */
        const val PIXELS_PER_MM_300_DPI = 300.0 / 25.4
    }
}
