package com.recollect.scan.ocr

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.recollect.scan.PageDetection
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * 手写笔记 OCR（scan-pipeline.md 第 5 步）：ML Kit 中文离线模型，只读手写书写区。
 *
 * 裁剪范围由调用方按打印布局给出（`NoteAreaGeometry` 只覆盖横线书写区，不含标题与印刷附加摘要）。
 * 结果只写 `detectedRawOcr`，不参与调度（D-06）；原始文本只读，修正写入 `correctedText`（C-012）。
 */
class MlKitNoteOcr : NoteOcr {

    override fun recognize(
        bitmap: Bitmap,
        page: PageDetection,
        areas: List<NoteAreaGeometry>,
    ): Map<String, String> {
        if (areas.isEmpty()) return emptyMap()
        val client = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
        return try {
            areas.associate { area ->
                val text = runCatching { readArea(client, bitmap, area) }.getOrDefault("")
                area.questionId to text
            }
        } finally {
            client.close()
        }
    }

    private fun readArea(
        client: com.google.mlkit.vision.text.TextRecognizer,
        bitmap: Bitmap,
        area: NoteAreaGeometry,
    ): String {
        val left = (area.leftMm * PIXELS_PER_MM).roundToInt().coerceIn(0, bitmap.width - 1)
        val top = (area.topMm * PIXELS_PER_MM).roundToInt().coerceIn(0, bitmap.height - 1)
        val right = ((area.leftMm + area.widthMm) * PIXELS_PER_MM).roundToInt().coerceIn(left + 1, bitmap.width)
        val bottom = ((area.topMm + area.heightMm) * PIXELS_PER_MM).roundToInt().coerceIn(top + 1, bitmap.height)

        val crop = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
        return try {
            val result = Tasks.await(
                client.process(InputImage.fromBitmap(crop, 0)),
                TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )
            result.textBlocks
                .joinToString("\n") { block -> block.text }
                .trim()
        } finally {
            crop.recycle()
        }
    }

    private companion object {
        const val PIXELS_PER_MM = 300.0 / 25.4
        const val TIMEOUT_SECONDS = 20L
    }
}
