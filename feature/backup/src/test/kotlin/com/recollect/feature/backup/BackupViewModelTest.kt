package com.recollect.feature.backup

import com.recollect.data.room.export.BackupDocument
import com.recollect.data.room.export.DatabaseExporter
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlinx.coroutines.runBlocking

/** 导出页状态机（backup.md「导出/导入 UI」）：成功、失败与仅重试分享。 */
class BackupViewModelTest {

    private val document = BackupDocument(
        fileName = "recollect-20261122-1605.json",
        json = "{}",
        entityCounts = mapOf("questions" to 0),
        exportedAt = Instant.parse("2026-11-22T08:05:00Z"),
    )

    @Test
    fun `导出并分享成功后进入 Exported`() = runBlocking {
        val sharer = FakeSharer(ShareOutcome.Shared)
        val viewModel = BackupViewModel(FakeExporter(document), sharer)

        viewModel.exportAndShare()

        val state = assertIs<BackupState.Exported>(viewModel.state)
        assertSame(document, state.document)
        assertSame(ShareOutcome.Shared, state.shareOutcome)
        assertEquals(1, sharer.calls)
    }

    @Test
    fun `导出失败进入 Error 且不调用分享`() = runBlocking {
        val sharer = FakeSharer(ShareOutcome.Shared)
        val viewModel = BackupViewModel(
            FakeExporter(failure = IllegalStateException("磁盘已满")),
            sharer,
        )

        viewModel.exportAndShare()

        val state = assertIs<BackupState.Error>(viewModel.state)
        assertEquals("磁盘已满", state.message)
        assertEquals(0, sharer.calls)
    }

    @Test
    fun `分享失败后重试分享不重新导出`() = runBlocking {
        val exporter = FakeExporter(document)
        val sharer = FakeSharer(ShareOutcome.Shared, failuresBeforeSuccess = 1)
        val viewModel = BackupViewModel(exporter, sharer)

        viewModel.exportAndShare()
        assertIs<ShareOutcome.Failed>((viewModel.state as BackupState.Exported).shareOutcome)

        viewModel.retryShare()

        val state = assertIs<BackupState.Exported>(viewModel.state)
        assertSame(ShareOutcome.Shared, state.shareOutcome)
        assertEquals(1, exporter.calls)
        assertEquals(2, sharer.calls)
    }

    private class FakeExporter(
        private val document: BackupDocument? = null,
        private val failure: Exception? = null,
    ) : DatabaseExporter {
        var calls = 0

        override suspend fun export(): BackupDocument {
            calls++
            failure?.let { throw it }
            return checkNotNull(document)
        }
    }

    private class FakeSharer(
        private val success: ShareOutcome,
        private var failuresBeforeSuccess: Int = 0,
    ) : BackupSharer {
        var calls = 0

        override suspend fun share(document: BackupDocument): ShareOutcome {
            calls++
            return if (failuresBeforeSuccess > 0) {
                failuresBeforeSuccess--
                ShareOutcome.Failed("未找到可分享的应用")
            } else {
                success
            }
        }
    }
}
