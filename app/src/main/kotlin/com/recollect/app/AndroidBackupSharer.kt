package com.recollect.app

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.recollect.data.room.export.BackupDocument
import com.recollect.feature.backup.BackupSharer
import com.recollect.feature.backup.ShareOutcome
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 分享端口实现（app 层）：写缓存文件 → `FileProvider` → 系统分享面板。
 *
 * 放在 app 层而不是 feature：只有这里持有 `Context`、`Intent` 与 `FileProvider`（R-03）。
 * 文件写入应用私有缓存目录，无需存储权限；分享时授予目标应用一次性只读权限。
 */
class AndroidBackupSharer(context: Context) : BackupSharer {

    private val appContext: Context = context.applicationContext

    override suspend fun share(document: BackupDocument): ShareOutcome = withContext(Dispatchers.IO) {
        try {
            val directory = File(appContext.cacheDir, EXPORT_DIRECTORY).apply { mkdirs() }
            pruneOldExports(directory, keep = MAX_KEEP_FILES)
            val file = File(directory, document.fileName)
            file.writeText(document.json, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                file,
            )
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "拾知数据库备份")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            withContext(Dispatchers.Main) {
                appContext.startActivity(
                    Intent.createChooser(send, "导出数据库")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            ShareOutcome.Shared
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ShareOutcome.Failed(e.message ?: "无法打开系统分享面板。")
        }
    }

    /** 缓存目录只保留最近几次导出，避免重复导出长期占用空间。 */
    private fun pruneOldExports(directory: File, keep: Int) {
        val files = directory.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }
            ?: return
        files.drop(keep).forEach { it.delete() }
    }

    private companion object {
        const val EXPORT_DIRECTORY = "backup-exports"
        const val MAX_KEEP_FILES = 5
    }
}
