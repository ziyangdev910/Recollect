package com.recollect.app

import android.content.Context
import android.net.Uri
import com.recollect.feature.backup.BackupFileReader
import com.recollect.feature.backup.BackupFileTooLargeException
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 读取 SAF / 分享意图传入的 JSON 文件（app 层）。
 *
 * 逐块读取并在超过 20MB 时停止，避免一次读入超大文件导致内存问题；
 * 文件内容只作为字符串交给导入端口，本类不解析 JSON。
 */
class AndroidBackupFileReader(context: Context) : BackupFileReader {

    private val appContext: Context = context.applicationContext

    override suspend fun read(uri: String): String = withContext(Dispatchers.IO) {
        val input = appContext.contentResolver.openInputStream(Uri.parse(uri))
            ?: throw IOException("无法打开所选文件。")
        input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0L
            while (true) {
                val read = stream.read(buffer)
                if (read <= 0) break
                total += read
                if (total > MAX_BYTES) {
                    throw BackupFileTooLargeException(total, MAX_BYTES)
                }
                output.write(buffer, 0, read)
            }
            String(output.toByteArray(), Charsets.UTF_8)
        }
    }

    private companion object {
        const val MAX_BYTES = 20L * 1024 * 1024
    }
}
