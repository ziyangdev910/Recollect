package com.recollect.scan

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.test.AndroidTestCase
import com.recollect.data.room.repository.PendingFeedbackInput
import com.recollect.data.room.repository.PendingFeedbackRow
import com.recollect.data.room.repository.PlanPageFacts
import com.recollect.data.room.repository.ScanSessionRepository
import com.recollect.data.room.repository.ScanSessionSnapshot
import com.recollect.model.KnowledgeRow
import com.recollect.model.PlanPageRows
import com.recollect.scan.ocr.MlKitNoteOcr
import com.recollect.scan.ocr.NoteAreaGeometry
import com.recollect.scan.usecase.ScanPageOutcome
import com.recollect.scan.usecase.ScanPageUseCase
import com.recollect.feature.planning.PrintJob
import com.recollect.feature.print.PrintLayoutBuilder
import com.recollect.feature.print.PrintPdfWriter
import com.recollect.feature.print.PrintRenderer
import com.recollect.pagination.DefaultPagePlanner
import com.recollect.pagination.PaginationResult
import com.recollect.pagination.RenderableQuestion
import com.recollect.pagination.ScanGeometryProjection
import com.recollect.pagination.TemplateGeometry
import com.recollect.scan.omr.DetectionStatus
import java.io.File
import java.io.FileOutputStream
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlin.coroutines.startCoroutine
import kotlin.math.roundToInt

/**
 * 阶段 6.3 设备验证：真实打印页（PDF → 2480×3508）驱动 `OpenCvScanEngine`。
 *
 * 覆盖：四角 ArUco 定位、A4 透视矫正、左侧填涂区采样（未涂页面应全部 `NO_MARK`）、
 * 以及「页面尚未生成 QR」时走 `QR_NOT_FOUND` 而不是返回空列表。
 */
class OpenCvScanEngineDeviceTest : AndroidTestCase() {

    fun testDetectsArucoWarpsA4AndReadsEmptyBubbles() {
        val template = TemplateGeometry.a4Print()
        val questions = listOf(
            RenderableQuestion("q7", "S-1", 1, listOf("一元二次方程"), 1, 0, hasNoteArea = true),
            RenderableQuestion("q8", "S-2", 2, listOf("勾股定理", "相似三角形"), 2, 0, hasNoteArea = false),
        )
        val pagination = DefaultPagePlanner().plan(
            planId = PLAN_ID,
            dailyGroups = linkedMapOf(LocalDate.of(2026, 11, 16) to questions),
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

        val directory = File(context.getExternalFilesDir(null) ?: context.filesDir, "scan-evidence")
            .apply { mkdirs() }
        val pdf = File(directory, "scan-$PLAN_ID.pdf")
        FileOutputStream(pdf).use { PrintPdfWriter.write(context, layout, PrintPdfWriter.a4PrintAttributes(), it) }

        val widthPx = (template.pageWidthMm * PrintRenderer.PIXELS_PER_MM_300_DPI).roundToInt()
        val heightPx = (template.pageHeightMm * PrintRenderer.PIXELS_PER_MM_300_DPI).roundToInt()
        val pageBitmap = renderFirstPage(pdf, widthPx, heightPx)

        val engine = OpenCvScanEngine(template)
        val report = StringBuilder()
        try {
            val page = engine.detectPage(pageBitmap)
            report.appendLine("ARUCO_CORNERS=${page.corners.joinToString { "(${it.x},${it.y})" }}")
            report.appendLine("ROTATION=${page.rotation}")
            report.appendLine("CORRECTED=${page.corrected.width}x${page.corrected.height}")

            assertEquals("必须检测到四角标记", 4, page.corners.size)
            assertEquals("矫正后必须是 300 DPI A4", widthPx to heightPx, page.corrected.width to page.corrected.height)
            assertEquals("正拍页面旋转应为 0", 0, page.rotation)

            val blocks = pagination.pages[0].items.map { item ->
                ItemBlockGeometry(
                    questionId = item.questionId,
                    topMm = item.yOffsetMm,
                    heightMm = item.heightMm,
                    rowCount = requireNotNull(job.questions[item.questionId]).knowledgeCount,
                )
            }
            val rows = engine.readOmr(page.corrected, page, blocks)
            report.appendLine("OMR_ROWS=${rows.joinToString { "${it.questionId}#${it.rowIndex}=${it.status}/${it.detectedTier}" }}")
            assertEquals("行数应等于可打印知识点数", 3, rows.size)
            assertTrue("未涂页面必须全部 NO_MARK，实际：${rows.map { it.status }}", rows.all { it.status == DetectionStatus.NO_MARK })

            val qrs = engine.decodeQrs(page.corrected, page)
            report.appendLine("QR_RESULTS=${qrs.joinToString { "${it.questionId}@(${it.centerX},${it.centerY}) checksum=${it.checksumOk}" }}")
            assertEquals("每题一个 QR", questions.size, qrs.size)
            assertEquals("题号应与打印页一致", questions.map { it.questionId }.toSet(), qrs.mapNotNull { it.questionId }.toSet())
            assertTrue("载荷 CRC 必须通过", qrs.all { it.checksumOk })
            assertTrue("pageId 必须来自打印机载荷", qrs.all { it.pageId == pagination.pages[0].pageId })

            // 编排整链：引擎 → 归属校验 → 写库（内存仓库），失败路径不写库由单测覆盖。
            val facts = PlanPageFacts(
                planId = PLAN_ID,
                pageId = pagination.pages[0].pageId,
                date = pagination.pages[0].date,
                questionIds = questions.map { it.questionId },
                knowledgeRows = listOf(
                    PlanPageRows("q7", listOf(KnowledgeRow("kp7", 1)), listOf("kp7")),
                    PlanPageRows("q8", listOf(KnowledgeRow("kp8-1", 2), KnowledgeRow("kp8-2", 3)), listOf("kp8-1")),
                ),
                items = pagination.pages[0].items.map { item ->
                    ScanGeometryProjection.of(
                        item = item,
                        question = requireNotNull(job.questions[item.questionId]),
                        template = template,
                    )
                },
            )
            // 笔记区几何来自持久化快照（C-093 方案 A），只覆盖横线书写区。
            val noteAreas = facts.items.mapNotNull { item ->
                item.noteArea?.let { note ->
                    NoteAreaGeometry(
                        questionId = item.questionId,
                        leftMm = note.leftMm,
                        topMm = note.topMm,
                        widthMm = note.widthMm,
                        heightMm = note.heightMm,
                    )
                }
            }
            val noteOcr = MlKitNoteOcr()
            val ocr = noteOcr.recognize(page.corrected, page, noteAreas)
            report.appendLine("OCR_RESULTS=${ocr.mapValues { it.value }}")
            assertTrue("空白书写区不应识别出文字，实际：$ocr", ocr.values.all { it.isBlank() })

            val repo = InMemoryScanRepository(facts)
            val useCase = ScanPageUseCase(repo, Clock.systemUTC(), template)
            val outcome = awaitSuspend {
                useCase.scan(
                    planId = PLAN_ID,
                    bitmap = pageBitmap,
                    engine = engine,
                    noteOcr = noteOcr,
                )
            }
            report.appendLine("STORE_OUTCOME=$outcome")
            assertTrue("编排应写库成功，实际：$outcome", outcome is ScanPageOutcome.Stored)
            assertEquals(3, (outcome as ScanPageOutcome.Stored).rowCount)
            assertEquals(listOf("kp7", "kp8-1", "kp8-2"), repo.appended.map { it.knowledgePointId })
            assertEquals(listOf(true, true, false), repo.appended.map { it.triggered })
            assertTrue("空白手写区不得写入 OCR 文本", repo.appended.all { it.detectedRawOcr.isNullOrBlank() })
        } finally {
            pageBitmap.recycle()
            File(directory, "scan-$PLAN_ID-report.txt").writeText(report.toString())
            println(report)
        }
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

    private fun <T> awaitSuspend(block: suspend () -> T): T {
        var result: Result<T>? = null
        val latch = java.util.concurrent.CountDownLatch(1)
        block.startCoroutine(object : kotlin.coroutines.Continuation<T> {
            override val context = kotlin.coroutines.EmptyCoroutineContext

            override fun resumeWith(value: Result<T>) {
                result = value
                latch.countDown()
            }
        })
        assertTrue("编排超时", latch.await(60, java.util.concurrent.TimeUnit.SECONDS))
        return result!!.getOrThrow()
    }

    private class InMemoryScanRepository(private val facts: PlanPageFacts) : ScanSessionRepository {
        var appended: List<PendingFeedbackInput> = emptyList()

        override suspend fun pageFacts(pageId: String): PlanPageFacts? =
            facts.takeIf { it.pageId == pageId }

        override suspend fun startOrResumeSession(planId: String, now: Instant): ScanSessionSnapshot =
            ScanSessionSnapshot("device-session", planId, com.recollect.data.room.entity.ScanSessionStatus.SCANNING, true)

        override suspend fun activeSession(): ScanSessionSnapshot? = null

        override suspend fun appendPage(
            sessionId: String,
            pageId: String,
            rows: List<PendingFeedbackInput>,
        ): Int {
            appended = rows
            return rows.size
        }

        override suspend fun pendingRows(sessionId: String): List<PendingFeedbackRow> = emptyList()

        override suspend fun planEndDate(planId: String): LocalDate = LocalDate.of(2026, 11, 16)

        override suspend fun markReady(sessionId: String) = Unit

        override suspend fun abandonStaleSessions(now: Instant) = Unit
    }

    private companion object {
        const val PLAN_ID = "SCAN01"
    }
}
