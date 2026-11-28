package com.recollect.scan.io

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 扫描页缩略图缓存（2026-11-28 用户要求：扫描页以缩略图列表展示已扫页面）。
 *
 * 相机原始帧按 scan-pipeline.md「原始帧不落盘」在识别后立即删除，缩略图另存为
 * `cacheDir/scan-previews` 下的小 JPEG，仅供扫描页的缩略图与悬浮预览使用。
 * 识别失败、删除页面或从首页重新进入扫描页时会删除对应文件。
 */
class ScanPreviewStore(private val context: Context) {

    /** 把识别用的位图缩成小预览并写入缓存；失败时返回 `null`，不影响识别主流程。 */
    suspend fun save(source: Bitmap): String? = withContext(Dispatchers.IO) {
        runCatching {
            pruneStale()
            val longEdge = maxOf(source.width, source.height)
            val scale = if (longEdge > MAX_LONG_EDGE_PX) {
                MAX_LONG_EDGE_PX.toFloat() / longEdge
            } else {
                1f
            }
            val scaled = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    source,
                    (source.width * scale).toInt().coerceAtLeast(1),
                    (source.height * scale).toInt().coerceAtLeast(1),
                    true,
                )
            } else {
                source
            }
            val dir = File(context.cacheDir, DIRECTORY).apply { mkdirs() }
            val file = File(dir, "${UUID.randomUUID()}.jpg")
            try {
                file.outputStream().use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                }
            } finally {
                if (scaled !== source) scaled.recycle()
            }
            file.absolutePath
        }.getOrNull()
    }

    suspend fun delete(location: String?) = withContext(Dispatchers.IO) {
        location?.let { runCatching { File(it).delete() } }
        Unit
    }

    /** 缓存目录里超过一天的文件随下次写入清理，避免长期累积。 */
    private fun pruneStale() {
        val dir = File(context.cacheDir, DIRECTORY)
        val threshold = System.currentTimeMillis() - STALE_AFTER_MS
        runCatching {
            dir.listFiles()?.forEach { file ->
                if (file.lastModified() < threshold) file.delete()
            }
        }
    }

    private companion object {
        const val DIRECTORY = "scan-previews"
        const val MAX_LONG_EDGE_PX = 1280
        const val JPEG_QUALITY = 82
        const val STALE_AFTER_MS = 24L * 60 * 60 * 1000
    }
}
