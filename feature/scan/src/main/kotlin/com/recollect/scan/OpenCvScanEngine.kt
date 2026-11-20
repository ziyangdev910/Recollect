package com.recollect.scan

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import com.google.android.gms.tasks.Tasks
import com.recollect.model.Tier
import com.recollect.pagination.TemplateGeometry
import com.recollect.scan.omr.BubbleReading
import com.recollect.scan.omr.DetectionStatus
import com.recollect.scan.omr.OmrClassifier
import com.recollect.common.qr.QrDecodeResult
import com.recollect.common.qr.QrPayloadCodec
import org.opencv.android.Utils
import org.opencv.aruco.Aruco
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.util.concurrent.TimeUnit
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * C-092 方案 C：自研回读引擎。
 *
 * - 定位与透视：OpenCV `DICT_4X4_50` 检测四角标记，按标记中心与打印几何做单应性变换，
 *   统一输出 2480×3508（300 DPI）。
 * - 逐题 QR：ML Kit 条码（`barcode-scanning:17.2.0`），载荷校验由 [QrPayloadCodec] 负责。
 * - OMR：按 scan-pipeline.md 的列宽与 2.5mm 采样窗自行采样，阈值见 [OmrClassifier]。
 */
class OpenCvScanEngine(
    private val template: TemplateGeometry,
) : ScanEngine {

    private val pxPerMm: Double = PIXELS_PER_MM_300_DPI

    override fun detectPage(bitmap: Bitmap): PageDetection {
        val markers = detectMarkers(bitmap)
        if (markers.isEmpty()) throw ScanException(ScanErrorCode.PAGE_NOT_FOUND)

        val known = markers.filterKeys { it in ARUCO_IDS }
        if (markers.size > ARUCO_IDS.size || known.keys != ARUCO_IDS.toSet()) {
            if (markers.size > ARUCO_IDS.size) throw ScanException(ScanErrorCode.MULTIPLE_PAGES)
            throw ScanException(ScanErrorCode.PAGE_NOT_FOUND)
        }

        val source = Mat(4, 1, org.opencv.core.CvType.CV_32FC2)
        val target = Mat(4, 1, org.opencv.core.CvType.CV_32FC2)
        val srcPoints = FloatArray(8)
        val dstPoints = FloatArray(8)
        ARUCO_IDS.forEachIndexed { index, id ->
            val center = requireNotNull(known[id])
            srcPoints[index * 2] = center.x
            srcPoints[index * 2 + 1] = center.y
            dstPoints[index * 2] = (markerCenterMm(id, horizontal = true) * pxPerMm).toFloat()
            dstPoints[index * 2 + 1] = (markerCenterMm(id, horizontal = false) * pxPerMm).toFloat()
        }
        source.put(0, 0, srcPoints)
        target.put(0, 0, dstPoints)

        val widthPx = (template.pageWidthMm * pxPerMm).roundToInt()
        val heightPx = (template.pageHeightMm * pxPerMm).roundToInt()
        val corrected = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        var srcMat: Mat? = null
        var warped: Mat? = null
        try {
            val homography = Imgproc.getPerspectiveTransform(source, target)
            srcMat = Mat()
            Utils.bitmapToMat(bitmap, srcMat)
            warped = Mat()
            Imgproc.warpPerspective(srcMat, warped, homography, Size(widthPx.toDouble(), heightPx.toDouble()))
            Utils.matToBitmap(warped, corrected)
            homography.release()
        } catch (t: Throwable) {
            corrected.recycle()
            throw ScanException(ScanErrorCode.PAGE_UNREADABLE, cause = t)
        } finally {
            source.release()
            target.release()
            srcMat?.release()
            warped?.release()
        }

        val rotation = rotationOf(requireNotNull(known[0]), requireNotNull(known[1]))
        return PageDetection(
            corners = ARUCO_IDS.map { requireNotNull(known[it]) },
            corrected = corrected,
            rotation = rotation,
        )
    }

    override fun decodeQrs(bitmap: Bitmap, page: PageDetection): List<QrResult> {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val scanner = BarcodeScanning.getClient(options)
        val barcodes = try {
            Tasks.await(
                scanner.process(InputImage.fromBitmap(page.corrected, 0)),
                QR_TIMEOUT_SECONDS,
                TimeUnit.SECONDS,
            )
        } catch (t: Throwable) {
            throw ScanException(ScanErrorCode.QR_UNREADABLE, cause = t)
        } finally {
            scanner.close()
        }
        if (barcodes.isEmpty()) throw ScanException(ScanErrorCode.QR_NOT_FOUND)

        return barcodes.map { barcode ->
            val raw = barcode.rawValue
                ?: throw ScanException(ScanErrorCode.QR_UNREADABLE)
            val box = barcode.boundingBox
            val centerX = box?.exactCenterX() ?: 0f
            val centerY = box?.exactCenterY() ?: 0f
            when (val decoded = QrPayloadCodec.decode(raw)) {
                is QrDecodeResult.Valid -> QrResult(
                    raw = raw,
                    planId = decoded.payload.planId,
                    pageId = decoded.payload.pageId,
                    questionId = decoded.payload.questionId,
                    centerX = centerX,
                    centerY = centerY,
                    checksumOk = true,
                )

                is QrDecodeResult.Invalid -> QrResult(
                    raw = raw,
                    planId = null,
                    pageId = null,
                    questionId = null,
                    centerX = centerX,
                    centerY = centerY,
                    checksumOk = false,
                )
            }
        }
    }

    override fun readOmr(
        bitmap: Bitmap,
        page: PageDetection,
        blocks: List<ItemBlockGeometry>,
    ): List<OmrRow> {
        val rows = mutableListOf<OmrRow>()
        blocks.forEach { block ->
            for (index in 0 until block.rowCount) {
                val topMm = block.topMm + index * template.rowHeightMm
                val readings = (0 until BUBBLE_COLUMNS).map { column ->
                    sampleBubble(bitmap, topMm, column)
                }
                val (status, tier) = OmrClassifier.rowOf(readings)
                rows += OmrRow(
                    questionId = block.questionId,
                    rowIndex = index,
                    status = status,
                    detectedTier = tier,
                )
            }
        }
        return rows
    }

    private fun sampleBubble(bitmap: Bitmap, rowTopMm: Double, column: Int): BubbleReading {
        val centerXMm = template.contentLeftMm + SEQUENCE_COLUMN_MM + COLUMN_GAP_MM +
            KNOWLEDGE_COLUMN_MM + COLUMN_GAP_MM +
            column * (BUBBLE_MM + COLUMN_GAP_MM) + BUBBLE_MM / 2
        val centerYMm = rowTopMm + template.rowHeightMm / 2
        val halfPx = (SAMPLE_MM / 2 * pxPerMm).roundToInt().coerceAtLeast(1)
        val centerX = (centerXMm * pxPerMm).roundToInt()
        val centerY = (centerYMm * pxPerMm).roundToInt()

        val left = (centerX - halfPx).coerceIn(0, bitmap.width - 1)
        val top = (centerY - halfPx).coerceIn(0, bitmap.height - 1)
        val right = (centerX + halfPx).coerceIn(left + 1, bitmap.width)
        val bottom = (centerY + halfPx).coerceIn(top + 1, bitmap.height)
        val width = right - left
        val height = bottom - top
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, left, top, width, height)

        var dark = 0
        pixels.forEach { pixel ->
            val gray = 0.299 * Color.red(pixel) + 0.587 * Color.green(pixel) + 0.114 * Color.blue(pixel)
            if (gray < DARK_THRESHOLD) dark++
        }
        return OmrClassifier.classify(dark.toDouble() / pixels.size)
    }

    /** 4×4_50 检测；返回 id → 标记中心（图像坐标）。 */
    private fun detectMarkers(bitmap: Bitmap): Map<Int, PointF> {
        if (!openCvLoaded) throw ScanException(ScanErrorCode.PAGE_UNREADABLE, "OpenCV 原生库未加载")
        var mat: Mat? = null
        var gray: Mat? = null
        val corners = mutableListOf<Mat>()
        val ids = Mat()
        try {
            val dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_4X4_50)
            mat = Mat()
            Utils.bitmapToMat(bitmap, mat)
            gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
            Aruco.detectMarkers(gray, dictionary, corners, ids)
            if (ids.empty()) return emptyMap()

            // OpenCV 返回的 ids 可能是 N×1 或 1×N，按实际行列数读取。
            val count = ids.rows().coerceAtLeast(ids.cols())
            val result = mutableMapOf<Int, PointF>()
            for (index in 0 until count) {
                val id = if (ids.rows() == 1) {
                    ids.get(0, index)?.first()?.toInt()
                } else {
                    ids.get(index, 0)?.first()?.toInt()
                } ?: continue
                val square = corners[index]
                var sumX = 0.0
                var sumY = 0.0
                for (point in 0 until 4) {
                    val value = square.get(0, point) ?: continue
                    sumX += value[0]
                    sumY += value[1]
                }
                result[id] = PointF((sumX / 4).toFloat(), (sumY / 4).toFloat())
            }
            return result
        } finally {
            corners.forEach { it.release() }
            ids.release()
            mat?.release()
            gray?.release()
        }
    }

    /** 标记中心在页面上的毫米坐标；id 0/1/2/3 = 左上/右上/右下/左下。 */
    private fun markerCenterMm(id: Int, horizontal: Boolean): Double {
        val inset = ARUCO_INSET_MM + ARUCO_CODE_MM / 2
        return when (id) {
            0 -> inset
            1 -> if (horizontal) template.pageWidthMm - inset else inset
            2 -> if (horizontal) template.pageWidthMm - inset else template.pageHeightMm - inset
            else -> if (horizontal) inset else template.pageHeightMm - inset
        }
    }

    private fun rotationOf(leftTop: PointF, rightTop: PointF): Int {
        val angle = Math.toDegrees(atan2(rightTop.y - leftTop.y.toDouble(), rightTop.x - leftTop.x.toDouble()))
        return ((angle / 90).roundToInt() * 90).mod(360)
    }

    private companion object {
        /** OpenCV 4.5.3 的 Java 绑定需要显式加载 `opencv_java4`；只加载一次。 */
        val openCvLoaded: Boolean by lazy {
            runCatching { org.opencv.android.OpenCVLoader.initDebug() }.getOrDefault(false)
        }

        const val PIXELS_PER_MM_300_DPI = 300.0 / 25.4
        const val QR_TIMEOUT_SECONDS = 30L

        val ARUCO_IDS = listOf(0, 1, 2, 3)

        // 规格列宽：序号 8mm、知识点 34mm、A/B/C 各 3.5mm、间距 2mm（scan-pipeline.md）。
        const val SEQUENCE_COLUMN_MM = 8.0
        const val KNOWLEDGE_COLUMN_MM = 34.0
        const val BUBBLE_MM = 3.5
        const val COLUMN_GAP_MM = 2.0
        const val BUBBLE_COLUMNS = 3
        const val SAMPLE_MM = 2.5
        const val DARK_THRESHOLD = 128

        // 打印层 ArUco 内码 10mm、距页边 12mm（print-template.md）。
        const val ARUCO_INSET_MM = 12.0
        const val ARUCO_CODE_MM = 10.0
    }
}
