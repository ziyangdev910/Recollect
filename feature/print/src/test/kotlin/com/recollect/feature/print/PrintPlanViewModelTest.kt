package com.recollect.feature.print

import com.recollect.pagination.PlacedItem
import com.recollect.pagination.PlannedPage
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
        questions = emptyMap(),
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
        val viewModel = PrintPlanViewModel(
            input = null,
            launcher = launcher,
            snapshotWriter = RecordingSnapshotWriter(),
        )

        assertTrue(viewModel.state is PrintPlanUiState.Empty)

        viewModel.print()

        assertNull(launcher.lastInput, "没有计划时不得调起系统打印")
    }

    @Test
    fun `plan summary groups by day and forwards the same input to the launcher`() {
        val launcher = RecordingPrintLauncher()
        val input = input()
        val viewModel = PrintPlanViewModel(
            input = input,
            launcher = launcher,
            snapshotWriter = RecordingSnapshotWriter(),
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
        assertEquals("已调起系统打印；完成或取消后返回本页。", after.message)
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
        val viewModel = PrintPlanViewModel(input = input(), launcher = launcher, snapshotWriter = writer)
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
    fun `declining printed confirmation writes nothing`() {
        val launcher = RecordingPrintLauncher()
        val writer = RecordingSnapshotWriter()
        val viewModel = PrintPlanViewModel(input = input(), launcher = launcher, snapshotWriter = writer)

        viewModel.print()
        launcher.onDocumentGenerated?.invoke(
            listOf(PrintedQuestionSnapshot("q1", "ABC123-01", "payload")),
        )
        viewModel.declinePrinted()

        val content = viewModel.state as PrintPlanUiState.Content
        assertFalse(content.awaitingPrintedConfirmation)
        assertTrue(writer.saved.isEmpty(), "用户选择未打印时不得写快照")
        assertEquals("未记录本次打印；如已打印，可再次打开系统打印。", content.message)
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

    private class RecordingSnapshotWriter : PrintSnapshotWriter {
        val saved = mutableListOf<List<PrintedQuestionSnapshot>>()

        override suspend fun save(snapshots: List<PrintedQuestionSnapshot>) {
            saved += snapshots
        }
    }
}
