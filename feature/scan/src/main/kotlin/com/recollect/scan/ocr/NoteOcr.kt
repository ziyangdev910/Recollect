package com.recollect.scan.ocr

import android.graphics.Bitmap
import com.recollect.scan.PageDetection

/**
 * 手写笔记识别端口（scan-pipeline.md 第 5 步）。
 *
 * 只允许读右下笔记区的**手写书写区**，不读印刷的附加摘要；结果只作为
 * `PendingFeedback.detectedRawOcr` 的输入，不参与调度（D-06、C-012）。
 */
interface NoteOcr {

    /** 返回 questionId → 原始 OCR 文本；无书写内容时返回空字符串。 */
    fun recognize(
        bitmap: Bitmap,
        page: PageDetection,
        areas: List<NoteAreaGeometry>,
    ): Map<String, String>
}

/** 题块内手写书写区的毫米几何（由打印布局投影得到，不由本层猜测）。 */
data class NoteAreaGeometry(
    val questionId: String,
    val leftMm: Double,
    val topMm: Double,
    val widthMm: Double,
    val heightMm: Double,
)

/** 第 1 步的占位实现：尚未接入 OCR 时返回空文本，不阻塞编排。 */
object NoNoteOcr : NoteOcr {
    override fun recognize(
        bitmap: Bitmap,
        page: PageDetection,
        areas: List<NoteAreaGeometry>,
    ): Map<String, String> = emptyMap()
}
