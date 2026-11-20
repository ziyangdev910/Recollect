package com.recollect.feature.print

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlin.math.roundToInt

/**
 * 每题二维码绘制（print-template.md「ArUco 与 QR 几何」）：版本 5、纠错 M、边长 18mm。
 *
 * 只负责把 [QrPayloadCodec][com.recollect.common.qr.QrPayloadCodec] 生成的载荷画进预留框；
 * 预留在题块底部 21mm 区域的右侧，周围是空白页边距，可提供 ≥4 modules 的 quiet zone
 * （版本 5 共 37 模块，18mm 下单模块约 0.49mm，4 模块约 1.95mm，小于 3mm 预留内边距）。
 */
object QrCodeRenderer {

    private val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.QR_VERSION to 5,
        EncodeHintType.CHARACTER_SET to "UTF-8",
        EncodeHintType.MARGIN to 0,
    )

    fun draw(
        canvas: Canvas,
        payload: String,
        leftMm: Double,
        topMm: Double,
        sizeMm: Double,
        pxPerMm: Double,
    ) {
        val sizePx = (sizeMm * pxPerMm).roundToInt().coerceAtLeast(1)
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
        try {
            val pixels = IntArray(matrix.width * matrix.height)
            var index = 0
            for (y in 0 until matrix.height) {
                for (x in 0 until matrix.width) {
                    pixels[index++] = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }
            bitmap.setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)

            // 不做滤波，保证 300 DPI 下模块边缘锐利。
            val paint = Paint().apply {
                isFilterBitmap = false
                isAntiAlias = false
            }
            canvas.drawBitmap(
                bitmap,
                null,
                RectF(
                    (leftMm * pxPerMm).toFloat(),
                    (topMm * pxPerMm).toFloat(),
                    ((leftMm + sizeMm) * pxPerMm).toFloat(),
                    ((topMm + sizeMm) * pxPerMm).toFloat(),
                ),
                paint,
            )
        } finally {
            bitmap.recycle()
        }
    }
}
