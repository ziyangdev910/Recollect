package com.recollect.feature.print

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.test.AndroidTestCase
import com.recollect.feature.planning.PrintJob
import com.recollect.pagination.DefaultPagePlanner
import com.recollect.pagination.PaginationResult
import com.recollect.pagination.RenderableQuestion
import com.recollect.pagination.TemplateGeometry
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.aruco.Aruco
import org.opencv.aruco.DetectorParameters
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import kotlin.math.roundToInt

/** 诊断：确认打印页四角标记能否被 OpenCV `DICT_4X4_50` 检出。 */
class ArucoDetectionDiagnosticTest : AndroidTestCase() {

    fun testDetectPrintedMarkers() {
        val report = StringBuilder()
        report.appendLine("OPENCV_INIT=${OpenCVLoader.initDebug()}")
        val template = TemplateGeometry.a4Print()
        val questions = listOf(
            RenderableQuestion("q1", "D-1", 1, listOf("一元二次方程"), 1, 0, hasNoteArea = true),
        )
        val pagination = DefaultPagePlanner().plan(
            planId = "D9AG01",
            dailyGroups = linkedMapOf(LocalDate.of(2026, 11, 16) to questions),
            template = template,
        ) as PaginationResult.Success
        val job = PrintJob(
            planId = "D9AG01",
            pages = pagination.pages,
            questions = questions.associateBy { it.questionId },
            notes = emptyMap(),
            template = template,
        )
        val layout = PrintLayoutBuilder.build(
            planId = job.planId,
            pages = job.pages,
            questions = job.questions,
            noteTexts = emptyMap(),
            template = job.template,
        )
        val directory = File(context.getExternalFilesDir(null) ?: context.filesDir, "diagnostic")
            .apply { mkdirs() }
        val pdf = File(directory, "diag.pdf")
        FileOutputStream(pdf).use { PrintPdfWriter.write(context, layout, PrintPdfWriter.a4PrintAttributes(), it) }
        val widthPx = (template.pageWidthMm * PrintRenderer.PIXELS_PER_MM_300_DPI).roundToInt()
        val heightPx = (template.pageHeightMm * PrintRenderer.PIXELS_PER_MM_300_DPI).roundToInt()
        val page = render(pdf, widthPx, heightPx)

        val crop = Bitmap.createBitmap(page, 0, 0, 500, 500)
        FileOutputStream(File(directory, "corner-crop.png")).use { crop.compress(Bitmap.CompressFormat.PNG, 100, it) }
        save(page.copy(Bitmap.Config.ARGB_8888, false), File(directory, "diag-page.png"))

        report.appendLine("PAGE_DETECT=${describe(page)}")

        // 逐角裁剪，定位整页检测失败的具体角落。
        val crops = mapOf(
            "TL" to Bitmap.createBitmap(page, 0, 0, 400, 400),
            "TR" to Bitmap.createBitmap(page, page.width - 400, 0, 400, 400),
            "BR" to Bitmap.createBitmap(page, page.width - 400, page.height - 400, 400, 400),
            "BL" to Bitmap.createBitmap(page, 0, page.height - 400, 400, 400),
        )
        crops.forEach { (name, bitmap) ->
            report.appendLine("CORNER_$name=${describe(bitmap)}")
            save(bitmap, File(directory, "corner-$name.png"))
        }

        // 标准 drawMarker 输出 + 白边，验证检测链路本身。
        val dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_50)
        for (id in 0..3) {
            val markerMat = Mat()
            Aruco.drawMarker(dictionary, id, 200, markerMat)
            val markerBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(markerMat, markerBitmap)
            val padded = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
            padded.eraseColor(Color.WHITE)
            android.graphics.Canvas(padded).drawBitmap(markerBitmap, 50f, 50f, null)
            save(padded, File(directory, "drawmarker-$id.png"))
            report.appendLine("DRAWMARKER_$id=${describe(padded)}")
            markerMat.release()
        }

        // 同一图案用本仓库的 ArucoPattern 重绘（纯 Kotlin），与 drawMarker 对照。
        for (id in 0..3) {
            val ourBitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
            ourBitmap.eraseColor(Color.WHITE)
            val canvas = android.graphics.Canvas(ourBitmap)
            val modules = requireNotNull(ArucoPattern.modulesFor(id))
            val cell = 200f / ArucoPattern.MODULES
            val paint = android.graphics.Paint().apply { color = Color.BLACK }
            for (row in 0 until ArucoPattern.MODULES) {
                for (col in 0 until ArucoPattern.MODULES) {
                    if (!modules[row][col]) continue
                    canvas.drawRect(
                        50f + col * cell,
                        50f + row * cell,
                        50f + (col + 1) * cell,
                        50f + (row + 1) * cell,
                        paint,
                    )
                }
            }
            save(ourBitmap, File(directory, "ourpattern-$id.png"))
            report.appendLine("OURPATTERN_$id=${describe(ourBitmap)}")
        }

        // 放宽参数再试整页。
        val relaxed = DetectorParameters.create().apply {
            set_minMarkerPerimeterRate(0.005)
            set_adaptiveThreshConstant(7.0)
        }
        report.appendLine("PAGE_DETECT_RELAXED=${describe(page, relaxed)}")

        File(directory, "detect-report.txt").writeText(report.toString())
        println(report)
    }

    private fun describe(bitmap: Bitmap, parameters: DetectorParameters? = null): String {
        var mat: Mat? = null
        var gray: Mat? = null
        val corners = mutableListOf<Mat>()
        val ids = Mat()
        try {
            val dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_50)
            mat = Mat()
            Utils.bitmapToMat(bitmap, mat)
            gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
            if (parameters == null) {
                Aruco.detectMarkers(gray, dictionary, corners, ids)
            } else {
                Aruco.detectMarkers(gray, dictionary, corners, ids, parameters)
            }
            if (ids.empty()) return "none"
            val count = ids.rows().coerceAtLeast(ids.cols())
            val list = (0 until count).map { index ->
                if (ids.rows() == 1) ids.get(0, index)?.first()?.toInt()
                else ids.get(index, 0)?.first()?.toInt()
            }
            return "shape=${ids.rows()}x${ids.cols()} ids=$list"
        } finally {
            corners.forEach { it.release() }
            ids.release()
            mat?.release()
            gray?.release()
        }
    }

    private fun render(pdf: File, widthPx: Int, heightPx: Int): Bitmap {
        ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                val page = renderer.openPage(0)
                val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                try {
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                } finally {
                    page.close()
                }
                return bitmap
            }
        }
    }

    private fun save(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
