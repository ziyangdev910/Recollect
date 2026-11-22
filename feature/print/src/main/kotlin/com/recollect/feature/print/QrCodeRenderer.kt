package com.recollect.feature.print

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

/**
 * 每题二维码绘制（print-template.md「ArUco 与 QR 几何」）：版本 5、纠错 M、边长 18mm。
 *
 * 只负责把 [QrPayloadCodec][com.recollect.common.qr.QrPayloadCodec] 生成的载荷画进预留框。
 * 模块直接铺满 18mm 预留框（版本 5 共 37 模块，单模块约 0.49mm）；quiet zone 由框外
 * 空白页边距提供，避免 QRCodeWriter 在框内再留白导致可见二维码缩小、与 pageId 标签错位。
 */
object QrCodeRenderer {

    private val hints = mapOf(
        EncodeHintType.QR_VERSION to 5,
        EncodeHintType.CHARACTER_SET to "UTF-8",
    )

    fun draw(
        canvas: Canvas,
        payload: String,
        leftMm: Double,
        topMm: Double,
        sizeMm: Double,
        pxPerMm: Double,
    ) {
        // 直接用模块矩阵绘制：QRCodeWriter 会在框内居中并加入自带 quiet zone，
        // 导致可见二维码小于预留框，pageId 标签与二维码右缘无法视觉对齐。
        val matrix = Encoder.encode(payload, ErrorCorrectionLevel.M, hints).matrix
        val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
        try {
            val pixels = IntArray(matrix.width * matrix.height)
            var index = 0
            for (y in 0 until matrix.height) {
                for (x in 0 until matrix.width) {
                    pixels[index++] = if (matrix.get(x, y).toInt() == 1) {
                        Color.BLACK
                    } else {
                        Color.WHITE
                    }
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
