package com.recollect.common.qr

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * QR 载荷 `version|planId|pageId|questionId|checksum`（docs/specs/scan-pipeline.md「QR 载荷与校验算法」）。
 */
class QrPayloadCodecTest {

    @Test
    fun `round trips a valid payload with crc32 checksum`() {
        val encoded = QrPayloadCodec.encode(
            planId = "K7M2Q9",
            pageId = "K7M2Q9-03",
            questionId = "q-1_A",
        )

        // CRC-32/IEEE 802.3（0xEDB88320）对前四个字段的 UTF-8 字节，8 位大写十六进制。
        assertEquals("1|K7M2Q9|K7M2Q9-03|q-1_A|AA2AFA49", encoded)
        assertEquals(
            QrDecodeResult.Valid(QrPayload("K7M2Q9", "K7M2Q9-03", "q-1_A")),
            QrPayloadCodec.decode(encoded),
        )
    }
}
