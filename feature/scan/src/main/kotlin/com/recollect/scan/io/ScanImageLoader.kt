package com.recollect.scan.io

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 相机/相册图片读取端口（app.md「权限与文件」）。
 *
 * 入参是页面传入的 URI 字符串，避免 `ViewModel` 依赖 `Uri`/`Context`（R-03）。
 * 解码在 IO 线程完成，按长边保留至少 [AndroidScanImageLoader.MIN_LONG_EDGE_PX] 像素。
 */
fun interface ScanImageLoader {
    suspend fun load(imageLocation: String): Bitmap?
}

class AndroidScanImageLoader(private val context: Context) : ScanImageLoader {

    override suspend fun load(imageLocation: String): Bitmap? = withContext(Dispatchers.IO) {
        val uri = runCatching { Uri.parse(imageLocation) }.getOrNull() ?: return@withContext null
        val bitmap = runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight)
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        }.getOrNull()

        // 相机临时文件只服务本次识别，读取后立即删除（scan-pipeline.md：原始帧不落盘）。
        if (uri.scheme == "file") {
            runCatching { uri.path?.let { java.io.File(it).delete() } }
        }
        bitmap
    }

    companion object {
        /** 透视输出为 2480×3508（300 DPI），采样后长边不得低于该量级。 */
        const val MIN_LONG_EDGE_PX = 2480

        /** 2 的幂采样，保证长边 ≥ [MIN_LONG_EDGE_PX]，同时避免超大相机图占满内存。 */
        fun sampleSizeFor(width: Int, height: Int): Int {
            val longEdge = maxOf(width, height)
            var sample = 1
            while (longEdge / (sample * 2) >= MIN_LONG_EDGE_PX) {
                sample *= 2
            }
            return sample
        }
    }
}
