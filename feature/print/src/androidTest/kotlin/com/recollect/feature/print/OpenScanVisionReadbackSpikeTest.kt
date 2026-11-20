package com.recollect.feature.print

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.test.AndroidTestCase
import android.util.Log
import com.recollect.feature.planning.PrintJob
import com.recollect.pagination.DefaultPagePlanner
import com.recollect.pagination.PaginationResult
import com.recollect.pagination.RenderableQuestion
import com.recollect.pagination.TemplateGeometry
import org.openscanvision.core.OpenScanVision
import org.openscanvision.core.ScanOptions
import org.openscanvision.core.ScanResult
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.math.roundToInt

/**
 * 阶段 6.2 spike：C-051 方案 A 的 OpenScanVision 回读**原始输出**记录。
 *
 * 这不是通过性断言用例，不修改任何断言来迁就实现：
 * 它的唯一职责是把「打印 PDF → PdfRenderer 2480×3508 位图 → OpenScanVision 1.0.0」这条链路上
 * 库真实返回的内容原样写出（文本报告 + 矫正图），供 R-01 判断 850×540 适配是否可行。
 *
 * 运行：
 * `adb shell am instrument -w -r -e class com.recollect.feature.print.OpenScanVisionReadbackSpikeTest \
 *   com.recollect.feature.print.test/android.test.InstrumentationTestRunner`
 */
class OpenScanVisionReadbackSpikeTest : AndroidTestCase() {

    fun testReadBackRenderedA4Page() {
        val directory = spikeDirectory()
        val template = TemplateGeometry.a4Print()
        val (pdfFile, pageBitmap) = renderSpikePage(directory, template)

        val report = StringBuilder()
        report.appendLine("SPIKE=C-051-A OpenScanVision 1.0.0")
        report.appendLine("PDF=${pdfFile.absolutePath} bytes=${pdfFile.length()}")
        report.appendLine("INPUT_PNG=${File(directory, "spike-$PLAN_ID-page-1.png").absolutePath}")
        report.appendLine("INPUT_BITMAP=${pageBitmap.width}x${pageBitmap.height}")

        val initialized = try {
            OpenScanVision.initialize(context)
        } catch (t: Throwable) {
            report.appendLine("INITIALIZE_EXCEPTION=${t.javaClass.name}: ${t.message}")
            false
        }
        report.appendLine("OPENCV_INITIALIZED=$initialized")

        report.appendLine("ARUCO_RAW=${describeMarkers(pageBitmap)}")

        val startedMs = System.currentTimeMillis()
        val result = try {
            awaitScan(pageBitmap)
        } catch (t: Throwable) {
            report.appendLine("SCAN_EXCEPTION=${t.javaClass.name}: ${t.message}")
            null
        }
        report.appendLine("SCAN_ELAPSED_MS=${System.currentTimeMillis() - startedMs}")
        when (result) {
            null -> Unit
            is ScanResult.Success -> {
                report.appendLine("RESULT=Success")
                report.appendLine("TOKEN=${result.token}")
                report.appendLine("TEMPLATE_USED=${result.templateUsed}")
                report.appendLine("FILLED_INDICES=${result.filledIndices}")
                report.appendLine("CONFIDENCE=${result.confidence}")
                report.appendLine("LATENCY_MS=${result.latencyMs}")
                report.appendLine("QR_PAYLOAD=${result.qrPayload}")
                report.appendLine("QR_CORNERS=${result.qrCorners?.joinToString()}")
                report.appendLine("JSON=${result.json}")
                result.warpedBitmap?.let { warped ->
                    report.appendLine("WARPED_SIZE=${warped.width}x${warped.height}")
                    FileOutputStream(File(directory, "spike-$PLAN_ID-warped.png")).use { output ->
                        warped.compress(Bitmap.CompressFormat.PNG, 100, output)
                    }
                }
                result.annotatedBitmap?.let { annotated ->
                    report.appendLine("ANNOTATED_SIZE=${annotated.width}x${annotated.height}")
                    FileOutputStream(File(directory, "spike-$PLAN_ID-annotated.png")).use { output ->
                        annotated.compress(Bitmap.CompressFormat.PNG, 100, output)
                    }
                }
            }

            is ScanResult.Error -> {
                report.appendLine("RESULT=Error")
                report.appendLine("ERROR=${result.javaClass.simpleName}")
                report.appendLine("ERROR_TO_STRING=$result")
            }
        }

        val reportFile = File(directory, "spike-$PLAN_ID-report.txt")
        reportFile.writeText(report.toString())
        Log.i(TAG, report.toString())
        println(report.toString())
        assertTrue("报告未写出：${reportFile.absolutePath}", reportFile.isFile && reportFile.length() > 0)
    }

    private fun spikeDirectory(): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "print-spike").apply { mkdirs() }

    /**
     * 生成一页真实打印 PDF 并渲染为 2480×3508；样本只用于触发真实渲染路径，
     * 不是 R-01 的验收数据（验收需要真实打印稿照片）。
     */
    private fun renderSpikePage(directory: File, template: TemplateGeometry): Pair<File, Bitmap> {
        val date = LocalDate.of(2026, 11, 16)
        val questions = listOf(
            RenderableQuestion("q7", "S-1", 1, listOf("一元二次方程"), 1, 0, hasNoteArea = true),
            RenderableQuestion(
                "q8",
                "S-2",
                2,
                listOf("勾股定理", "相似三角形"),
                2,
                0,
                hasNoteArea = false,
            ),
        )
        val pagination = DefaultPagePlanner().plan(
            planId = PLAN_ID,
            dailyGroups = linkedMapOf(date to questions),
            template = template,
        ) as PaginationResult.Success
        val job = PrintJob(
            planId = PLAN_ID,
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

        val pdfFile = File(directory, "spike-$PLAN_ID.pdf")
        FileOutputStream(pdfFile).use { output ->
            PrintPdfWriter.write(context, layout, PrintPdfWriter.a4PrintAttributes(), output)
        }

        val widthPx = (template.pageWidthMm * PrintRenderer.PIXELS_PER_MM_300_DPI).roundToInt()
        val heightPx = (template.pageHeightMm * PrintRenderer.PIXELS_PER_MM_300_DPI).roundToInt()
        val bitmap = renderFirstPage(pdfFile, widthPx, heightPx)
        FileOutputStream(File(directory, "spike-$PLAN_ID-page-1.png")).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        return pdfFile to bitmap
    }

    private fun renderFirstPage(pdf: File, widthPx: Int, heightPx: Int): Bitmap {
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

    /**
     * 库没有公开 ArUco 角点，公开 API 只有 [ScanResult]。spike 阶段通过反射调用
     * （Kotlin `internal` 在字节码中仍是 public）记录原始检测结果；这只是诊断记录，不是生产用法。
     */
    private fun describeMarkers(bitmap: Bitmap): String = try {
        val detector = Class.forName("org.openscanvision.core.internal.omr.CardDetector")
        val instance = detector.getField("INSTANCE").get(null)
        val method = detector.getMethod("detectArUcoMarkersFull", Bitmap::class.java)
        @Suppress("UNCHECKED_CAST")
        val markers = method.invoke(instance, bitmap) as Map<Int, List<PointF>>
        if (markers.isEmpty()) {
            "none"
        } else {
            markers.entries.joinToString("; ") { (id, corners) ->
                "id=$id corners=${corners.joinToString() { "(${it.x},${it.y})" }}"
            }
        }
    } catch (t: Throwable) {
        "reflection_failed: ${t.javaClass.name}: ${t.message}"
    }

    private fun awaitScan(bitmap: Bitmap): ScanResult {
        var outcome: Result<ScanResult>? = null
        val latch = CountDownLatch(1)
        suspend { OpenScanVision.scan(bitmap, ScanOptions.Default, "recollect-spike") }
            .startCoroutine(object : Continuation<ScanResult> {
                override val context = EmptyCoroutineContext

                override fun resumeWith(result: Result<ScanResult>) {
                    outcome = result
                    latch.countDown()
                }
            })
        assertTrue("OpenScanVision.scan 超时（120s）", latch.await(120, TimeUnit.SECONDS))
        return outcome!!.getOrThrow()
    }

    private companion object {
        const val TAG = "OsvSpikeTest"
        const val PLAN_ID = "SP1K3M"
    }
}
