package com.recollect.common.qr

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * QR 解码的失败分支（docs/specs/scan-pipeline.md「QR 错误路径（C-030）」：
 * 字段数/版本/字符集/CRC 任一不符都归 `QR_INVALID`，不得降级为「信任纸面」）。
 */
class QrPayloadValidationTest {

    @Test
    fun `rejects checksum mismatch`() {
        val raw = "1|K7M2Q9|K7M2Q9-03|q-1_A|00000000"

        assertEquals(
            QrDecodeResult.Invalid(QrInvalidReason.CHECKSUM),
            QrPayloadCodec.decode(raw),
        )
    }

    @Test
    fun `rejects unsupported version`() {
        val raw = "2|K7M2Q9|K7M2Q9-03|q-1_A|AA2AFA49"

        assertEquals(
            QrDecodeResult.Invalid(QrInvalidReason.VERSION),
            QrPayloadCodec.decode(raw),
        )
    }

    @Test
    fun `rejects payload without exactly five fields`() {
        assertEquals(
            QrDecodeResult.Invalid(QrInvalidReason.FIELD_COUNT),
            QrPayloadCodec.decode("1|K7M2Q9|K7M2Q9-03|q-1_A"),
        )
        assertEquals(
            QrDecodeResult.Invalid(QrInvalidReason.FIELD_COUNT),
            QrPayloadCodec.decode("1|K7M2Q9|K7M2Q9-03|q-1_A|AA2AFA49|extra"),
        )
    }

    @Test
    fun `rejects plan id outside crockford base32`() {
        // I、L、O、U 不在 Crockford Base32 字符集内。
        assertEquals(
            QrDecodeResult.Invalid(QrInvalidReason.CHARSET),
            QrPayloadCodec.decode("1|K7M2QI|K7M2QI-03|q-1_A|AA2AFA49"),
        )
    }

    @Test
    fun `rejects page id that does not belong to the plan id`() {
        assertEquals(
            QrDecodeResult.Invalid(QrInvalidReason.CHARSET),
            QrPayloadCodec.decode("1|K7M2Q9|ABCDEF-03|q-1_A|AA2AFA49"),
        )
    }

    @Test
    fun `encoder refuses fields containing the separator`() {
        assertThrows(IllegalArgumentException::class.java) {
            QrPayloadCodec.encode("K7M2Q9", "K7M2Q9-03", "q|1")
        }
    }
}
