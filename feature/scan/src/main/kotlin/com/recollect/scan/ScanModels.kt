package com.recollect.scan

import android.graphics.Bitmap
import android.graphics.PointF
import com.recollect.model.Tier
import com.recollect.scan.omr.DetectionStatus

/**
 * 回读管线数据（docs/specs/scan-pipeline.md「ScanEngine 适配层」）。
 * 只描述引擎输出，不涉及数据库；`PendingFeedback` 的写入由会话层负责（R-04）。
 */
data class PageDetection(
    val corners: List<PointF>, // 左上/右上/右下/左下（已按 ArUco 0～3 排序）
    val corrected: Bitmap, // 2480×3508
    val rotation: Int, // 0/90/180/270
)

data class QrResult(
    val raw: String,
    val planId: String?,
    val pageId: String?,
    val questionId: String?,
    val centerX: Float,
    val centerY: Float,
    val checksumOk: Boolean,
)

data class ItemBlockGeometry(
    val questionId: String,
    val topMm: Double,
    val heightMm: Double,
    val rowCount: Int,
)

data class OmrRow(
    val questionId: String,
    val rowIndex: Int,
    val status: DetectionStatus,
    val detectedTier: Tier?,
)

/** 回读错误码（scan-pipeline.md「QR 错误路径」）。 */
enum class ScanErrorCode {
    PAGE_NOT_FOUND,
    MULTIPLE_PAGES,
    PAGE_UNREADABLE,
    QR_NOT_FOUND,
    QR_UNREADABLE,
    QR_MULTIPLE,
    QR_INVALID,
    OWNERSHIP_MISMATCH,
}

class ScanException(
    val code: ScanErrorCode,
    message: String = code.name,
    cause: Throwable? = null,
) : Exception(message, cause)
