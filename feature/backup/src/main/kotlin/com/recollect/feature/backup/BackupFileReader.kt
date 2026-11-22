package com.recollect.feature.backup

/**
 * 读取用户所选备份文件的端口。
 *
 * 具体实现（`ContentResolver` / SAF `Uri`）在 `app` 层；本模块只拿 [uri] 字符串，
 * ViewModel 不持有 `Context`（R-03）。
 */
interface BackupFileReader {
    suspend fun read(uri: String): String
}

/** 文件超过上限时的可读错误（backup.md 的 20MB 提示口径）。 */
class BackupFileTooLargeException(val sizeBytes: Long, val limitBytes: Long) :
    Exception("文件过大（${sizeBytes / (1024 * 1024)} MB），建议先导出/清理历史数据。")
