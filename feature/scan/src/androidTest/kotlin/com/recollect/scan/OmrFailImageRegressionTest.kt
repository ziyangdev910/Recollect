package com.recollect.scan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.test.AndroidTestCase
import com.recollect.pagination.TemplateGeometry
import com.recollect.scan.omr.DetectionStatus
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

/**
 * 真实失败样张回归：`evidence/print/fail_1.png`（计划 GHB2EB 第 1 页）。
 *
 * 纸面填涂：T-9=掌握、T-10=较熟悉、T-1=不会。该图是低分辨率整页图，
 * 用于锁定 OMR 在非 300 DPI 原始图上的取样稳定性。
 */
class OmrFailImageRegressionTest : AndroidTestCase() {

    fun testFailImageReadsMasteredFamiliarUnknown() {
        val template = TemplateGeometry.a4Print()
        val bitmap = context.assets.open("fail_1.png").use { BitmapFactory.decodeStream(it) }
        val engine = OpenCvScanEngine(template)
        val report = StringBuilder()
        try {
            report.appendLine("SOURCE=${bitmap.width}x${bitmap.height}")
            val page = engine.detectPage(bitmap)
            report.appendLine("CORNERS=${page.corners.joinToString { "(${it.x},${it.y})" }}")
            report.appendLine("ROTATION=${page.rotation}")
            report.appendLine("CORRECTED=${page.corrected.width}x${page.corrected.height}")

            val blocks = listOf(
                ItemBlockGeometry("q1", 33.0, 74.5, 1),
                ItemBlockGeometry("q2", 107.5, 74.5, 1),
                ItemBlockGeometry("q3", 182.0, 74.5, 1),
            )
            val pxPerMm = 300.0 / 25.4
            val halfPx = (template.bubbleSampleSizeMm / 2 * pxPerMm).roundToInt().coerceAtLeast(1)
            blocks.forEach { block ->
                val ratios = (0 until TemplateGeometry.BUBBLE_COLUMNS).map { column ->
                    darkRatio(
                        bitmap = page.corrected,
                        centerX = (template.bubbleCenterXMm(column) * pxPerMm).roundToInt(),
                        centerY = ((block.topMm + template.rowHeightMm / 2) * pxPerMm).roundToInt(),
                        halfPx = halfPx,
                    )
                }
                report.appendLine("RATIOS=${block.questionId} ${ratios.joinToString { "%.3f".format(it) }}")
            }

            val rows = engine.readOmr(page.corrected, page, blocks)
            report.appendLine("OMR=${rows.joinToString { "${it.questionId}#${it.rowIndex}=${it.status}/${it.detectedTier}" }}")

            val directory = File(context.getExternalFilesDir(null) ?: context.filesDir, "scan-evidence")
                .apply { mkdirs() }
            FileOutputStream(File(directory, "fail_1-corrected.png")).use { output ->
                page.corrected.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
            File(directory, "fail_1-report.txt").writeText(report.toString())
            println(report)

            assertEquals(DetectionStatus.OK, rows[0].status)
            assertEquals(DetectionStatus.OK, rows[1].status)
            assertEquals(DetectionStatus.OK, rows[2].status)
            assertEquals(com.recollect.model.Tier.MASTERED, rows[0].detectedTier)
            assertEquals(com.recollect.model.Tier.FAMILIAR, rows[1].detectedTier)
            assertEquals(com.recollect.model.Tier.UNKNOWN, rows[2].detectedTier)
        } finally {
            bitmap.recycle()
        }
    }

    private fun darkRatio(bitmap: Bitmap, centerX: Int, centerY: Int, halfPx: Int): Double {
        val left = (centerX - halfPx).coerceIn(0, bitmap.width - 1)
        val top = (centerY - halfPx).coerceIn(0, bitmap.height - 1)
        val right = (centerX + halfPx).coerceIn(left + 1, bitmap.width)
        val bottom = (centerY + halfPx).coerceIn(top + 1, bitmap.height)
        val width = right - left
        val height = bottom - top
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, left, top, width, height)
        var dark = 0
        pixels.forEach { pixel ->
            val gray = 0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)
            if (gray < 128) dark++
        }
        return dark.toDouble() / pixels.size
    }
}
