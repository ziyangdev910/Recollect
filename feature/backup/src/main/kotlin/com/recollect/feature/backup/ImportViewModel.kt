package com.recollect.feature.backup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.recollect.data.room.export.DatabaseImporter
import com.recollect.data.room.export.ImportResult
import com.recollect.data.room.export.ImportSummary
import com.recollect.data.room.export.ImportValidation
import kotlinx.coroutines.CancellationException

/**
 * 「导入数据库」状态（backup.md「替换式导入」）：
 *
 * `IDLE → READING → CONFIRMING → IMPORTING → SUCCESS/ERROR`。
 * 读取、校验和写入都走端口；ViewModel 不持有 `Context`（R-03）。
 */
sealed interface ImportState {
    data object Idle : ImportState

    data object Reading : ImportState

    /** 文件已通过校验，等待用户确认「将覆盖当前数据」。 */
    data class Confirming(
        val json: String,
        val summary: ImportSummary,
    ) : ImportState

    data object Importing : ImportState

    data class Success(val summary: ImportSummary) : ImportState

    data class Error(val message: String) : ImportState
}

/**
 * 导入状态持有者。
 *
 * 入口有两个：主界面导航栏「导入数据库」选择文件，以及系统「用其他方式打开」直接带入
 * `content://` URI（跳过选择步骤）。两者都调用 [prepare]；用户确认后调用 [confirm]。
 */
class ImportViewModel(
    private val fileReader: BackupFileReader,
    private val importer: DatabaseImporter,
) {
    var state by mutableStateOf<ImportState>(ImportState.Idle)
        private set

    /** 读取并校验所选文件；不合法时进入 [ImportState.Error]，不触碰本地数据。 */
    suspend fun prepare(uri: String) {
        if (state is ImportState.Reading || state is ImportState.Importing) return
        state = ImportState.Reading
        state = try {
            val json = fileReader.read(uri)
            when (val validation = importer.validate(json)) {
                is ImportValidation.Valid -> ImportState.Confirming(json, validation.summary)
                is ImportValidation.Invalid -> ImportState.Error(validation.message)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ImportState.Error("导入失败：${e.message ?: "无法读取所选文件。"}")
        }
    }

    /** 覆盖式导入；失败时本地数据整体回滚。 */
    suspend fun confirm() {
        val confirming = state as? ImportState.Confirming ?: return
        state = ImportState.Importing
        state = when (val result = importer.apply(confirming.json)) {
            is ImportResult.Success -> ImportState.Success(result.summary)
            is ImportResult.Failure -> ImportState.Error(result.message)
        }
    }

    /** 取消覆盖确认，回到空闲；不写库。 */
    fun cancel() {
        if (state is ImportState.Confirming) state = ImportState.Idle
    }

    /** 关闭错误提示。 */
    fun dismissError() {
        if (state is ImportState.Error) state = ImportState.Idle
    }

    /** 成功提示（Toast）已展示后清空状态，避免重复弹出。 */
    fun consumeSuccess() {
        if (state is ImportState.Success) state = ImportState.Idle
    }
}
