package com.recollect.scan.io

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/**
 * 打开系统相册选择器前，把公共存储里尚未入库的图片补交给 MediaStore。
 *
 * 系统照片选择器（含本机 GMS 回移版）只读 MediaStore：通过 adb、文件管理器或
 * 微信等应用直接落盘、但未被媒体扫描的图片不会出现。这里先枚举外部存储中的
 * 图片文件，只对 MediaStore 尚未收录的路径调用 [MediaScannerConnection.scanFile]，
 * 避免每次全量重扫。
 *
 * 目录枚举从外部存储根目录开始，跳过 `Android/data`、`Android/obb`、隐藏目录
 * 与音视频专用目录；文件数与耗时都有上限，超时后扫描器仍会在后台继续，
 * 不阻塞相册选择器打开。
 */
class GalleryMediaRefresher(private val context: Context) {

    /** 返回本次实际补扫的文件数；无权限或枚举失败时返回 0。 */
    suspend fun refresh(): Int = withContext(Dispatchers.IO) {
        val candidates = runCatching { findUnindexedImages() }.getOrDefault(emptyList())
        if (candidates.isEmpty()) return@withContext 0
        runCatching { scan(candidates) }.getOrDefault(0)
    }

    private fun findUnindexedImages(): List<String> {
        @Suppress("DEPRECATION")
        val root = Environment.getExternalStorageDirectory() ?: return emptyList()
        val indexed = indexedImagePaths()
        val result = LinkedHashSet<String>()
        val deadline = System.currentTimeMillis() + ENUMERATION_BUDGET_MS
        val visited = HashSet<String>()
        walk(root, depth = 0, deadline = deadline, indexed = indexed, result = result, visited = visited)
        return result.toList()
    }

    private fun walk(
        directory: File,
        depth: Int,
        deadline: Long,
        indexed: Set<String>,
        result: MutableSet<String>,
        visited: MutableSet<String>,
    ) {
        if (result.size >= MAX_CANDIDATES || System.currentTimeMillis() > deadline) return
        if (depth > MAX_DEPTH) return
        // 规范化路径用于防止符号链接造成的循环；拿不到时直接跳过该目录。
        val canonical = runCatching { directory.canonicalPath }.getOrNull() ?: return
        if (!visited.add(canonical)) return
        val children = runCatching { directory.listFiles() }.getOrNull() ?: return

        for (child in children) {
            if (result.size >= MAX_CANDIDATES || System.currentTimeMillis() > deadline) return
            val name = child.name
            if (name.startsWith(".")) continue
            if (child.isDirectory) {
                if (depth == 0 && name in SKIP_TOP_LEVEL_DIRECTORIES) continue
                if (directory.name == "Android" && name in ANDROID_PRIVATE_DIRECTORIES) continue
                walk(child, depth + 1, deadline, indexed, result, visited)
            } else if (isImageFile(name) && child.length() > 0) {
                val path = child.absolutePath
                if (path !in indexed) result.add(path)
            }
        }
    }

    /** 读取 MediaStore 已收录的图片路径；权限不足或列不可用时返回空集合。 */
    private fun indexedImagePaths(): Set<String> {
        val paths = HashSet<String>()
        runCatching {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media.DATA),
                null,
                null,
                null,
            )?.use { cursor ->
                val dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                if (dataColumn < 0) return@use
                while (cursor.moveToNext()) {
                    cursor.getString(dataColumn)?.let(paths::add)
                }
            }
        }
        return paths
    }

    private suspend fun scan(paths: List<String>): Int {
        val completed = AtomicInteger(0)
        val scanned = AtomicInteger(0)
        withTimeoutOrNull(SCAN_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                MediaScannerConnection.scanFile(
                    context.applicationContext,
                    paths.toTypedArray(),
                    null,
                ) { _, uri ->
                    if (uri != null) scanned.incrementAndGet()
                    if (completed.incrementAndGet() >= paths.size && continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
            }
        }
        return scanned.get()
    }

    private fun isImageFile(name: String): Boolean =
        name.substringAfterLast('.', "").lowercase() in IMAGE_EXTENSIONS

    private companion object {
        /** 单次最多补扫的文件数，避免在超大相册上阻塞过久。 */
        const val MAX_CANDIDATES = 2_000

        /** 目录枚举的时间预算；超时后先打开选择器，未扫完的文件留待下次。 */
        const val ENUMERATION_BUDGET_MS = 2_000L

        /** 等待 MediaScannerService 回调的上限。 */
        const val SCAN_TIMEOUT_MS = 8_000L

        /** 防止异常深的目录结构拖慢枚举。 */
        const val MAX_DEPTH = 10

        val IMAGE_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "webp", "bmp", "gif", "heic", "heif", "avif", "dng", "jfif",
        )

        /** 不会存放用户图片来源的系统目录。 */
        val SKIP_TOP_LEVEL_DIRECTORIES = setOf(
            "Music", "Ringtones", "Alarms", "Notifications", "Podcasts", "LOST.DIR",
        )

        /** Android 下的私有目录不属于可扫描的公共媒体目录。 */
        val ANDROID_PRIVATE_DIRECTORIES = setOf("data", "obb")
    }
}
