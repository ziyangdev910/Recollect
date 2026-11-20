package com.recollect.feature.print

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import com.recollect.common.KnowledgeTextMeasurer

/**
 * `KnowledgeTextMeasurer` 的平台实现（print-template.md「文本测量与换行」）。
 *
 * 用 `Paint` + `Typeface.DEFAULT` 按右上文本区宽度逐行折行；字号 3.2mm 起，
 * 超过 4 行依次降到 3.0mm、2.8mm；仍超过 4 行时返回实际行数（>4），
 * 由计划层产出 `KNOWLEDGE_TEXT_OVERFLOW` 警告（C-064）。本类只测量，不截断文本。
 *
 * app.md 把实现归属 `feature/print`；`feature:planning` 只依赖 `core:common` 的接口。
 */
class AndroidKnowledgeTextMeasurer(context: Context) : KnowledgeTextMeasurer {

    private val mmToPx = context.resources.displayMetrics.xdpi / MM_PER_INCH

    override fun lineCount(knowledgeNames: List<String>, widthMm: Double, fontMm: Double): Int {
        if (knowledgeNames.isEmpty()) return 0
        val text = knowledgeNames.joinToString(NAME_SEPARATOR)
        val widthPx = (widthMm * mmToPx).toFloat()

        val initial = countLines(text, fontMm.toFloat() * mmToPx, widthPx)
        if (initial <= MAX_VISUAL_LINES) return initial

        for (fallbackFontMm in FALLBACK_FONT_MM) {
            val lines = countLines(text, (fallbackFontMm * mmToPx).toFloat(), widthPx)
            if (lines <= MAX_VISUAL_LINES) return lines
        }
        return countLines(text, (FALLBACK_FONT_MM.last() * mmToPx).toFloat(), widthPx)
    }

    private fun countLines(text: String, textSizePx: Float, widthPx: Float): Int {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.DEFAULT
            textSize = textSizePx
        }
        var remaining = text
        var lines = 0
        while (remaining.isNotEmpty()) {
            val measured = paint.breakText(remaining, true, widthPx, null)
            if (measured <= 0) break
            lines++
            remaining = remaining.substring(measured)
        }
        return lines
    }

    private companion object {
        const val MM_PER_INCH = 25.4f
        const val MAX_VISUAL_LINES = 4
        const val NAME_SEPARATOR = " / "
        val FALLBACK_FONT_MM = listOf(3.0, 2.8)
    }
}
