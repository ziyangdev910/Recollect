package com.recollect.feature.backup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.recollect.data.room.export.BackupDocument
import com.recollect.data.room.export.DatabaseExporter
import kotlinx.coroutines.CancellationException

/**
 * 「导出并分享数据库」页的状态（backup.md「导出/导入 UI」的导出状态机）。
 *
 * `IDLE → EXPORTING → EXPORTED/ERROR`；分享结果与导出结果一起展示，
 * 导出文件只落在应用缓存目录，失败不影响本地数据库。
 */
sealed interface BackupState {
    data object Idle : BackupState

    data object Exporting : BackupState

    data class Exported(
        val document: BackupDocument,
        val shareOutcome: ShareOutcome,
    ) : BackupState

    data class Error(val message: String) : BackupState
}

/**
 * 导出页状态持有者（手写 ViewModel，不引入 lifecycle-viewmodel 依赖）。
 *
 * 只依赖 [DatabaseExporter] 与 [BackupSharer] 两个端口；不持有 `Context`（R-03）。
 * 重复点击在 [BackupState.Exporting] 期间被忽略，避免并发写同一缓存文件。
 */
class BackupViewModel(
    private val exporter: DatabaseExporter,
    private val sharer: BackupSharer,
) {
    var state by mutableStateOf<BackupState>(BackupState.Idle)
        private set

    suspend fun exportAndShare() {
        if (state is BackupState.Exporting) return
        state = BackupState.Exporting
        state = try {
            val document = exporter.export()
            BackupState.Exported(document = document, shareOutcome = sharer.share(document))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            BackupState.Error(e.message ?: "导出数据库失败，请重试。")
        }
    }

    /** 分享失败后允许仅重试分享，不重新读取数据库。 */
    suspend fun retryShare() {
        val exported = state as? BackupState.Exported ?: return
        state = BackupState.Exporting
        state = BackupState.Exported(
            document = exported.document,
            shareOutcome = sharer.share(exported.document),
        )
    }
}
