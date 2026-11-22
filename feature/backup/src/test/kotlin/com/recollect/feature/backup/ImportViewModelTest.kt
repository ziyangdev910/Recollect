package com.recollect.feature.backup

import com.recollect.data.room.export.DatabaseImporter
import com.recollect.data.room.export.ImportResult
import com.recollect.data.room.export.ImportSummary
import com.recollect.data.room.export.ImportValidation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/** 导入页状态机：读取校验 → 覆盖确认 → 写入 → 成功/失败（backup.md「替换式导入」）。 */
class ImportViewModelTest {

    private val summary = ImportSummary(
        sourceBackupFormatVersion = 1,
        sourceSchemaVersion = 10,
        degraded = false,
        entityCounts = mapOf("questions" to 1),
        skippedRows = emptyList(),
    )

    @Test
    fun `合法文件进入覆盖确认`() = runBlocking {
        val viewModel = ImportViewModel(
            FakeReader("{}"),
            FakeImporter(validation = ImportValidation.Valid(summary)),
        )

        viewModel.prepare("content://backup.json")

        val confirming = assertIs<ImportState.Confirming>(viewModel.state)
        assertEquals("{}", confirming.json)
        assertEquals(summary, confirming.summary)
    }

    @Test
    fun `不合法文件进入错误且不写库`() = runBlocking {
        val importer = FakeImporter(validation = ImportValidation.Invalid("备份格式版本不受支持。"))
        val viewModel = ImportViewModel(FakeReader("{}"), importer)

        viewModel.prepare("content://backup.json")

        val error = assertIs<ImportState.Error>(viewModel.state)
        assertEquals("备份格式版本不受支持。", error.message)
        assertEquals(0, importer.applyCalls)
    }

    @Test
    fun `读取失败进入错误`() = runBlocking {
        val viewModel = ImportViewModel(
            FakeReader(failure = IllegalStateException("无法打开所选文件。")),
            FakeImporter(validation = ImportValidation.Valid(summary)),
        )

        viewModel.prepare("content://backup.json")

        val error = assertIs<ImportState.Error>(viewModel.state)
        assertTrue(error.message.contains("无法打开所选文件"), error.message)
    }

    @Test
    fun `确认后覆盖导入成功`() = runBlocking {
        val importer = FakeImporter(
            validation = ImportValidation.Valid(summary),
            result = ImportResult.Success(summary),
        )
        val viewModel = ImportViewModel(FakeReader("{}"), importer)

        viewModel.prepare("content://backup.json")
        viewModel.confirm()

        val success = assertIs<ImportState.Success>(viewModel.state)
        assertEquals(summary, success.summary)
        assertEquals(1, importer.applyCalls)
    }

    @Test
    fun `取消确认不写库`() = runBlocking {
        val importer = FakeImporter(validation = ImportValidation.Valid(summary))
        val viewModel = ImportViewModel(FakeReader("{}"), importer)

        viewModel.prepare("content://backup.json")
        viewModel.cancel()

        assertIs<ImportState.Idle>(viewModel.state)
        assertEquals(0, importer.applyCalls)
    }

    @Test
    fun `写入失败进入错误`() = runBlocking {
        val importer = FakeImporter(
            validation = ImportValidation.Valid(summary),
            result = ImportResult.Failure("导入失败：编号冲突。"),
        )
        val viewModel = ImportViewModel(FakeReader("{}"), importer)

        viewModel.prepare("content://backup.json")
        viewModel.confirm()

        val error = assertIs<ImportState.Error>(viewModel.state)
        assertEquals("导入失败：编号冲突。", error.message)
    }

    private class FakeReader(
        private val content: String = "",
        private val failure: Exception? = null,
    ) : BackupFileReader {
        override suspend fun read(uri: String): String {
            failure?.let { throw it }
            return content
        }
    }

    private class FakeImporter(
        private val validation: ImportValidation,
        private val result: ImportResult = ImportResult.Success(
            ImportSummary(1, 10, false, emptyMap(), emptyList()),
        ),
    ) : DatabaseImporter {
        var applyCalls = 0

        override suspend fun validate(json: String): ImportValidation = validation

        override suspend fun apply(json: String): ImportResult {
            applyCalls++
            return result
        }
    }
}
