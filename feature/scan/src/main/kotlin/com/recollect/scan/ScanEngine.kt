package com.recollect.scan

import android.graphics.Bitmap

/**
 * 回读引擎（docs/specs/scan-pipeline.md「ScanEngine 适配层」）。
 *
 * C-092 方案 C：ArUco/透视用 OpenCV，逐题 QR 用 ML Kit 条码，OMR 按规格几何自采样；
 * ViewModel/UseCase 只依赖本接口，不直接调用 OpenCV 或 ML Kit（R-03）。
 */
interface ScanEngine {

    /** ArUco 定位 + 透视矫正；失败抛 [ScanException]（`PAGE_NOT_FOUND`/`MULTIPLE_PAGES`/`PAGE_UNREADABLE`）。 */
    fun detectPage(bitmap: Bitmap): PageDetection

    /** 逐题 QR 解码；未检测到抛 [ScanException]`QR_NOT_FOUND`。 */
    fun decodeQrs(bitmap: Bitmap, page: PageDetection): List<QrResult>

    /** 左侧填涂区按题块/行采样；行状态见 [com.recollect.scan.omr.DetectionStatus]。 */
    fun readOmr(
        bitmap: Bitmap,
        page: PageDetection,
        blocks: List<ItemBlockGeometry>,
    ): List<OmrRow>
}
