package com.recollect.feature.backup

import com.recollect.data.room.export.BackupDocument

/**
 * 分享端口：把导出好的 JSON 文件交给系统分享面板（微信、文件管理器等）。
 *
 * 具体实现（写缓存文件 + `FileProvider` + `ACTION_SEND`）放 `app` 层，
 * 本模块的 ViewModel 不持有 `Context`、不接触 `Intent`（R-03）。
 */
interface BackupSharer {
    suspend fun share(document: BackupDocument): ShareOutcome
}

sealed interface ShareOutcome {
    /** 已把文件交给系统分享面板（无法得知用户最终选择了哪个目标）。 */
    data object Shared : ShareOutcome

    data class Failed(val message: String) : ShareOutcome
}
