package com.recollect.app.onboarding

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import com.recollect.feature.print.PrintLayoutBuilder
import com.recollect.feature.print.PrintPlanInput
import com.recollect.feature.print.PrintRenderer
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 首次演示的扫描稿：预览用缩略图 + 扫描引擎读取的完整 PNG（FileProvider URI）。 */
internal data class DemoSheetFiles(
    val image: ImageBitmap,
    val imageLocation: String,
)

/**
 * 按真实打印布局生成一张已填涂的 A4 打印稿（2026-11-26 用户要求）。
 *
 * 使用与打印页相同的 `PrintLayoutBuilder` / `PrintRenderer`：
 * 四角 ArUco、每题二维码、题号与填涂框都来自刚刚生成的计划，扫描端无需任何伪造数据。
 * 在填涂区按行涂黑（第一行「掌握」、第二行「较熟悉」），真实 ScanEngine 会读出结果。
 */
internal object DemoSheetRenderer {

    private const val PX_PER_MM = 300.0 / 25.4

    suspend fun render(context: Context, input: PrintPlanInput): DemoSheetFiles =
        withContext(Dispatchers.Default) {
            val layout = PrintLayoutBuilder.build(
                planId = input.planId,
                pages = input.pages,
                questions = input.questions,
                noteTexts = input.noteTexts,
                template = input.template,
            )
            val page = layout.pages.firstOrNull()
                ?: error("演示计划没有可打印页。")
            val width = (page.pageWidthMm * PX_PER_MM).roundToInt()
            val height = (page.pageHeightMm * PX_PER_MM).roundToInt()
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
                val canvas = Canvas(bitmap)
                PrintRenderer().draw(canvas, page, PX_PER_MM)

                // 已填涂：按知识点行依次涂「掌握 / 较熟悉 / 不会」。
                val fill = Paint().apply {
                    color = Color.BLACK
                    style = Paint.Style.FILL
                }
                val rows = page.items.flatMap { it.left.rows }
                rows.forEachIndexed { index, row ->
                    val column = when (index) {
                        0 -> 0
                        1 -> 1
                        else -> index % 3
                    }
                    val bubble = row.bubbles.getOrNull(column) ?: return@forEachIndexed
                    val inset = 0.45
                    canvas.drawRect(
                        ((bubble.leftMm + inset) * PX_PER_MM).toFloat(),
                        ((bubble.topMm + inset) * PX_PER_MM).toFloat(),
                        ((bubble.leftMm + bubble.widthMm - inset) * PX_PER_MM).toFloat(),
                        ((bubble.topMm + bubble.heightMm - inset) * PX_PER_MM).toFloat(),
                        fill,
                    )
                }

                val directory = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
                val file = File(directory, SHEET_FILE)
                FileOutputStream(file).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                }

                // 预览只用缩略图，交给 Compose；完整 PNG 留给 ScanEngine。
                val previewOptions = BitmapFactory.Options().apply { inSampleSize = 4 }
                val preview = BitmapFactory.decodeFile(file.absolutePath, previewOptions)
                    ?: error("演示打印稿无法读取。")
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )
                DemoSheetFiles(
                    image = preview.asImageBitmap(),
                    imageLocation = uri.toString(),
                )
            } finally {
                bitmap.recycle()
            }
        }

    /** 清理演示打印稿缓存；准备与结束演示时都调用，避免残留大图。 */
    fun clear(context: Context) {
        File(context.cacheDir, CACHE_DIR).deleteRecursively()
    }

    private const val CACHE_DIR = "demo-sheets"
    private const val SHEET_FILE = "demo-page.png"
}
