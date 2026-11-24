package com.recollect.feature.print

import com.recollect.pagination.PlacedItem
import com.recollect.pagination.PlannedPage
import com.recollect.pagination.RenderableQuestion
import com.recollect.pagination.TemplateGeometry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * 打印计划页状态（app.md「屏幕清单与状态」：Print 页显示计划、调起系统打印、失败有提示）。
 *
 * 只验证页面状态与打印端口的交接；真正的 PrintManager/PDF 由 androidTest 与设备验收覆盖。
 */
class PrintPlanViewModelTest {

    private val monday = LocalDate.of(2026, 11, 16)
    private val tuesday = LocalDate.of(2026, 11, 17)

    private fun input() = PrintPlanInput(
        planId = "ABC123",
        pages = listOf(
            page("ABC123-01", 1, monday, listOf("q1", "q2")),
            page("ABC123-02", 2, tuesday, listOf("q3")),
        ),
        questions = listOf("q1", "q2", "q3").associateWith { questionId ->
            RenderableQuestion(
                questionId = questionId,
                questionNumber = questionId.removePrefix("q"),
                knowledgeCount = 1,
                knowledgeNames = listOf("示例知识点"),
                knowledgeLineCount = 1,
                attachmentCount = 0,
            )
        },
        noteTexts = emptyMap(),
        template = TemplateGeometry.a4Print(),
    )

    private fun page(
        pageId: String,
        number: Int,
        date: LocalDate,
        questionIds: List<String>,
    ) = PlannedPage(
        pageId = pageId,
        pageNumber = number,
        date = date,
        items = questionIds.mapIndexed { index, questionId ->
            PlacedItem(questionId = questionId, yOffsetMm = index * 10.0, heightMm = 10.0)
        },
        continuationMarkers = emptyList(),
    )

    @Test
    fun `without a plan shows empty and does not call the launcher`() {
        val launcher = RecordingPrintLauncher()
        val sharer = RecordingPdfSharer()
        val viewModel = PrintPlanViewModel(
            input = null,
            launcher = launcher,
            snapshotWriter = RecordingSnapshotWriter(),
            pdfSharer = sharer,
        )

        assertTrue(viewModel.state is PrintPlanUiState.Empty)

        viewModel.print()

        assertNull(launcher.lastInput, "没有计划时不得调起系统打印")

        // 没有计划时也不能分享 PDF。
        runBlocking { viewModel.sharePdf() }
        assertNull(sharer.lastInput, "没有计划时不得生成分享 PDF")
    }

    @Test
    fun `plan summary groups by day and forwards the same input to the launcher`() {
        val launcher = RecordingPrintLauncher()
        val input = input()
        val viewModel = PrintPlanViewModel(
            input = input,
            launcher = launcher,
            snapshotWriter = RecordingSnapshotWriter(),
            pdfSharer = RecordingPdfSharer(),
        )

        val content = viewModel.state as PrintPlanUiState.Content
        assertEquals(2, content.pageCount)
        assertEquals(3, content.itemCount)
        assertEquals(
            listOf(PrintDaySummary(monday, 2), PrintDaySummary(tuesday, 1)),
            content.days,
        )

        viewModel.print()

        assertSame(input, launcher.lastInput, "打印端口必须收到同一份计划输入")
        val after = viewModel.state as PrintPlanUiState.Content
        assertEquals("已调起系统打印。", after.message)
        assertFalse(after.messageIsError)
    }

    @Test
    fun `launcher failure is shown as an error message`() {
        val launcher = RecordingPrintLauncher(
            result = PrintLaunchResult.Failed("系统打印服务不可用。"),
        )
        val viewModel = PrintPlanViewModel(
            input = input(),
            launcher = launcher,
            snapshotWriter = RecordingSnapshotWriter(),
            pdfSharer = RecordingPdfSharer(),
        )

        viewModel.print()

        val after = viewModel.state as PrintPlanUiState.Content
        assertEquals("系统打印服务不可用。", after.message)
        assertTrue(after.messageIsError)
    }

    @Test
    fun `document generation waits for printed confirmation before writing snapshots`() = runBlocking {
        val launcher = RecordingPrintLauncher()
        val writer = RecordingSnapshotWriter()
        val viewModel = PrintPlanViewModel(
            input = input(),
            launcher = launcher,
            snapshotWriter = writer,
            pdfSharer = RecordingPdfSharer(),
        )
        val snapshots = listOf(
            PrintedQuestionSnapshot("q1", "ABC123-01", "1|ABC123|ABC123-01|q1|CRC"),
        )

        viewModel.print()
        launcher.onDocumentGenerated?.invoke(snapshots)

        var content = viewModel.state as PrintPlanUiState.Content
        assertTrue(content.awaitingPrintedConfirmation, "文档生成后应等待用户确认已打印")
        assertTrue(writer.saved.isEmpty(), "用户确认前不得写快照")

        viewModel.confirmPrinted()

        content = viewModel.state as PrintPlanUiState.Content
        assertFalse(content.awaitingPrintedConfirmation)
        assertEquals(listOf(snapshots), writer.saved)
        assertEquals("已记录本次打印。", content.message)
    }

    @Test
    fun `superseded plan does not overwrite the latest print snapshot`() = runBlocking {
        val launcher = RecordingPrintLauncher()
        val writer = RecordingSnapshotWriter(PrintSnapshotWriteResult.IGNORED_NOT_ACTIVE)
        val viewModel = PrintPlanViewModel(
            input = input(),
            launcher = launcher,
            snapshotWriter = writer,
            pdfSharer = RecordingPdfSharer(),
        )

        viewModel.print()
        launcher.onDocumentGenerated?.invoke(
            listOf(PrintedQuestionSnapshot("q1", "ABC123-01", "payload")),
        )
        viewModel.confirmPrinted()

        val content = viewModel.state as PrintPlanUiState.Content
        assertFalse(content.awaitingPrintedConfirmation)
        assertTrue(content.messageIsError)
        assertEquals("本次计划已被新的计划替代，未更新打印快照。", content.message)
        assertEquals(listOf("ABC123"), writer.planIds, "写入前必须带上计划号")
    }

    @Test
    fun `declining printed confirmation writes nothing`() {
        val launcher = RecordingPrintLauncher()
        val writer = RecordingSnapshotWriter()
        val viewModel = PrintPlanViewModel(
            input = input(),
            launcher = launcher,
            snapshotWriter = writer,
            pdfSharer = RecordingPdfSharer(),
        )

        viewModel.print()
        launcher.onDocumentGenerated?.invoke(
            listOf(PrintedQuestionSnapshot("q1", "ABC123-01", "payload")),
        )
        viewModel.declinePrinted()

        val content = viewModel.state as PrintPlanUiState.Content
        assertFalse(content.awaitingPrintedConfirmation)
        assertTrue(writer.saved.isEmpty(), "用户选择未打印时不得写快照")
        assertEquals("未记录本次打印。", content.message)
    }

    @Test
    fun `sharing pdf forwards the plan input and records the print snapshots`() = runBlocking {
        val sharer = RecordingPdfSharer()
        val writer = RecordingSnapshotWriter()
        val input = input()
        val viewModel = PrintPlanViewModel(
            input = input,
            launcher = RecordingPrintLauncher(),
            snapshotWriter = writer,
            pdfSharer = sharer,
        )

        viewModel.sharePdf()

        assertSame(input, sharer.lastInput, "分享端口必须收到同一份计划输入")
        val content = viewModel.state as PrintPlanUiState.Content
        assertFalse(content.pdfSharing)
        assertFalse(content.messageIsError)
        assertEquals(
            "已生成 拾知周计划-ABC123.pdf，请在分享面板中选择应用；本次打印已记录。",
            content.message,
        )
        assertFalse(content.awaitingPrintedConfirmation, "分享不触发打印确认按钮")
        assertEquals(listOf("ABC123"), writer.planIds, "分享成功后必须写回当前计划")
        val saved = writer.saved.single()
        assertEquals(listOf("q1", "q2", "q3"), saved.map { it.questionId })
        assertEquals("ABC123-01", saved.first().pageId)
        assertTrue(saved.all { it.qrPayload.isNotBlank() }, "每题必须带二维码载荷")
    }

    @Test
    fun `share pdf failure is shown as an error and records nothing`() = runBlocking {
        val sharer = RecordingPdfSharer(PdfShareOutcome.Failed("无法打开系统分享面板。"))
        val writer = RecordingSnapshotWriter()
        val viewModel = PrintPlanViewModel(
            input = input(),
            launcher = RecordingPrintLauncher(),
            snapshotWriter = writer,
            pdfSharer = sharer,
        )

        viewModel.sharePdf()

        val content = viewModel.state as PrintPlanUiState.Content
        assertEquals("无法打开系统分享面板。", content.message)
        assertTrue(content.messageIsError)
        assertFalse(content.pdfSharing)
        assertFalse(content.awaitingPrintedConfirmation, "分享失败不得触发打印确认")
        assertTrue(writer.saved.isEmpty(), "分享失败不得写打印快照")
    }

    @Test
    fun `sharing pdf clears a pending print confirmation once snapshots are recorded`() = runBlocking {
        val launcher = RecordingPrintLauncher()
        val writer = RecordingSnapshotWriter()
        val viewModel = PrintPlanViewModel(
            input = input(),
            launcher = launcher,
            snapshotWriter = writer,
            pdfSharer = RecordingPdfSharer(),
        )

        viewModel.print()
        launcher.onDocumentGenerated?.invoke(
            listOf(PrintedQuestionSnapshot("q1", "ABC123-01", "payload")),
        )
        assertTrue((viewModel.state as PrintPlanUiState.Content).awaitingPrintedConfirmation)

        viewModel.sharePdf()

        val content = viewModel.state as PrintPlanUiState.Content
        assertFalse(content.awaitingPrintedConfirmation, "分享已按已打印落库，不应再询问纸张是否打印")
        assertEquals(listOf("ABC123"), writer.planIds)
    }

    @Test
    fun `share pdf on a superseded plan reports that the snapshot was not updated`() = runBlocking {
        val sharer = RecordingPdfSharer()
        val writer = RecordingSnapshotWriter(PrintSnapshotWriteResult.IGNORED_NOT_ACTIVE)
        val viewModel = PrintPlanViewModel(
            input = input(),
            launcher = RecordingPrintLauncher(),
            snapshotWriter = writer,
            pdfSharer = sharer,
        )

        viewModel.sharePdf()

        val content = viewModel.state as PrintPlanUiState.Content
        assertTrue(content.messageIsError)
        assertEquals(
            "已生成 拾知周计划-ABC123.pdf，但本次计划已被新的计划替代，未更新打印快照。",
            content.message,
        )
    }

    private class RecordingPrintLauncher(
        var result: PrintLaunchResult = PrintLaunchResult.Launched,
    ) : PrintLauncher {
        var lastInput: PrintPlanInput? = null
            private set
        var onDocumentGenerated: ((List<PrintedQuestionSnapshot>) -> Unit)? = null
            private set

        override fun launch(
            input: PrintPlanInput,
            onDocumentGenerated: (List<PrintedQuestionSnapshot>) -> Unit,
        ): PrintLaunchResult {
            lastInput = input
            this.onDocumentGenerated = onDocumentGenerated
            return result
        }
    }

    private class RecordingPdfSharer(
        var outcome: PdfShareOutcome = PdfShareOutcome.Shared("拾知周计划-ABC123.pdf"),
    ) : PlanPdfSharer {
        var lastInput: PrintPlanInput? = null
            private set

        override suspend fun share(input: PrintPlanInput): PdfShareOutcome {
            lastInput = input
            return outcome
        }
    }

    private class RecordingSnapshotWriter(
        var result: PrintSnapshotWriteResult = PrintSnapshotWriteResult.SAVED,
    ) : PrintSnapshotWriter {
        val saved = mutableListOf<List<PrintedQuestionSnapshot>>()
        val planIds = mutableListOf<String>()

        override suspend fun save(
            planId: String,
            snapshots: List<PrintedQuestionSnapshot>,
        ): PrintSnapshotWriteResult {
            planIds += planId
            saved += snapshots
            return result
        }
    }
}
