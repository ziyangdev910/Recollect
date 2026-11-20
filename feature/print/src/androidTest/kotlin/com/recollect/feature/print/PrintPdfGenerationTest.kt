package com.recollect.feature.print

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.test.AndroidTestCase
import android.util.Log
import com.recollect.data.room.entity.NoteAttachmentItem
import com.recollect.feature.planning.PrintJob
import com.recollect.pagination.DefaultPagePlanner
import com.recollect.pagination.PaginationResult
import com.recollect.pagination.RenderableQuestion
import com.recollect.pagination.TemplateGeometry
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * 打印布局证据（不是 R-01 识别验收）。
 *
 * 在真实 Android 运行时用 `PrintedPdfDocument` 生成 PDF，并用 `PdfRenderer`
 * 断言页数与 `PrintJob.pages` 一致；文件保存到应用外部目录，供 `adb pull` 到 `evidence/print/`。
 *
 * 运行：`adb shell am instrument -w -e class com.recollect.feature.print.PrintPdfGenerationTest \
 *   com.recollect.feature.print.test/android.test.InstrumentationTestRunner`
 */
class PrintPdfGenerationTest : AndroidTestCase() {

    fun testGenerateLayoutPdf() {
        // C-091 方案 A：生产模板为四角 ArUco 预留 14mm 角区。
        val template = TemplateGeometry.a4Print()
        val monday = LocalDate.of(2026, 11, 16)
        val tuesday = LocalDate.of(2026, 11, 17)
        val questions = listOf(
            RenderableQuestion(
                "q1",
                "T-1",
                1,
                listOf("一元二次方程"),
                1,
                0,
                hasNoteArea = true,
                sourcePage = "P12",
            ),
            RenderableQuestion(
                "q2",
                "T-2",
                2,
                listOf("勾股定理", "相似三角形"),
                2,
                3,
                hasNoteArea = false,
                sourcePage = "P3",
            ),
            RenderableQuestion(
                "q3",
                "T-3",
                1,
                listOf("一个非常长的知识点名称用于验证左侧截断规则"),
                2,
                3,
                hasNoteArea = true,
            ),
            RenderableQuestion("q4", "T-4", 1, listOf("相似三角形"), 1, 0, hasNoteArea = true),
        )
        val pagination = DefaultPagePlanner().plan(
            planId = PLAN_ID,
            dailyGroups = linkedMapOf(
                monday to questions.take(3),
                tuesday to listOf(questions[3]),
            ),
            template = template,
        ) as PaginationResult.Success

        val notes = mapOf(
            "q2" to List(3) { index ->
                NoteAttachmentItem(
                    questionId = "q2",
                    sourceFeedbackId = null,
                    text = "附件笔记 ${index + 1}",
                    imageUri = null,
                    order = index,
                    createdAt = Instant.parse("2026-11-16T08:00:00Z"),
                )
            },
            "q3" to List(3) { index ->
                NoteAttachmentItem(
                    questionId = "q3",
                    sourceFeedbackId = null,
                    text = "长名题目附件 ${index + 1}",
                    imageUri = null,
                    order = index,
                    createdAt = Instant.parse("2026-11-16T08:00:00Z"),
                )
            },
        )
        val job = PrintJob(
            planId = PLAN_ID,
            pages = pagination.pages,
            questions = questions.associateBy { it.questionId },
            notes = notes,
            template = template,
        )

        val layout = PrintLayoutBuilder.build(
            planId = job.planId,
            pages = job.pages,
            questions = job.questions,
            noteTexts = job.notes.mapValues { (_, items) -> items.map { it.text } },
            template = job.template,
        )

        val directory = File(context.getExternalFilesDir(null) ?: context.filesDir, "print")
            .apply { mkdirs() }
        val file = File(directory, "recollect-print-$PLAN_ID.pdf")
        FileOutputStream(file).use { output ->
            PrintPdfWriter.write(context, layout, PrintPdfWriter.a4PrintAttributes(), output)
        }

        assertTrue("PDF 文件不存在：${file.absolutePath}", file.isFile)
        assertTrue("PDF 文件为空：${file.absolutePath}", file.length() > 0)

        val widthPx = (template.pageWidthMm * PrintRenderer.PIXELS_PER_MM_300_DPI).roundToInt()
        val heightPx = (template.pageHeightMm * PrintRenderer.PIXELS_PER_MM_300_DPI).roundToInt()
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            val renderer = PdfRenderer(descriptor)
            try {
                assertEquals(job.pages.size, renderer.pageCount)
                for (index in 0 until renderer.pageCount) {
                    val page = renderer.openPage(index)
                    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                    try {
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        FileOutputStream(
                            File(directory, "recollect-print-$PLAN_ID-page-${index + 1}.png"),
                        ).use { output ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                        }
                    } finally {
                        bitmap.recycle()
                        page.close()
                    }
                }
            } finally {
                renderer.close()
            }
        }

        val summary = "PRINT_PDF=${file.absolutePath} BYTES=${file.length()} PAGES=${job.pages.size}"
        Log.i(TAG, summary)
        println(summary)
    }

    private companion object {
        const val TAG = "PrintPdfGenerationTest"
        const val PLAN_ID = "K7M2Q9"
    }
}
