package com.recollect.common.qr

import java.util.Locale
import java.util.zip.CRC32

/**
 * 每题 QR 的载荷编解码（docs/specs/scan-pipeline.md「QR 载荷与校验算法（C-030）」）。
 *
 * 格式：`version|planId|pageId|questionId|checksum`，字段分隔符 `|` 不得出现在字段内。
 * 校验码是前四个字段 UTF-8 字节的 CRC-32（IEEE 802.3，多项式 `0xEDB88320`），8 位大写十六进制。
 */
object QrPayloadCodec {

    const val VERSION = 1

    /** Crockford Base32：去掉 I、L、O、U。 */
    private const val CROCKFORD_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    private val planIdPattern = Regex("^[$CROCKFORD_ALPHABET]{6}$")
    private val pageIdPattern = Regex("^[$CROCKFORD_ALPHABET]{6}-[0-9]{2}$")
    private val questionIdPattern = Regex("^[A-Za-z0-9_-]+$")

    /**
     * 生成载荷；任一字段非法时抛 [IllegalArgumentException]（生成端发现即抛错，不做转义）。
     */
    fun encode(planId: String, pageId: String, questionId: String): String {
        require(planIdPattern.matches(planId)) { "planId 必须是 6 位 Crockford Base32" }
        require(pageIdPattern.matches(pageId)) { "pageId 必须是 {planId}-{两位页码}" }
        require(pageId.startsWith("$planId-")) { "pageId 必须以 planId 开头" }
        require(questionIdPattern.matches(questionId)) { "questionId 字符集不合法" }

        val body = "$VERSION|$planId|$pageId|$questionId"
        return "$body|${checksum(body)}"
    }

    /** 解码并校验；任何一项不符都返回 [QrDecodeResult.Invalid]，不降级为「信任纸面」。 */
    fun decode(raw: String): QrDecodeResult {
        val fields = raw.split('|')
        if (fields.size != 5) return QrDecodeResult.Invalid(QrInvalidReason.FIELD_COUNT)

        val (version, planId, pageId, questionId, checksum) = fields
        if (version != VERSION.toString()) return QrDecodeResult.Invalid(QrInvalidReason.VERSION)
        if (!planIdPattern.matches(planId) ||
            !pageIdPattern.matches(pageId) ||
            !pageId.startsWith("$planId-") ||
            !questionIdPattern.matches(questionId)
        ) {
            return QrDecodeResult.Invalid(QrInvalidReason.CHARSET)
        }

        val body = "$version|$planId|$pageId|$questionId"
        if (checksum != checksum(body)) return QrDecodeResult.Invalid(QrInvalidReason.CHECKSUM)

        return QrDecodeResult.Valid(QrPayload(planId, pageId, questionId))
    }

    private fun checksum(body: String): String {
        val crc = CRC32()
        crc.update(body.toByteArray(Charsets.UTF_8))
        return String.format(Locale.ROOT, "%08X", crc.value)
    }
}

data class QrPayload(
    val planId: String,
    val pageId: String,
    val questionId: String,
)

sealed interface QrDecodeResult {
    data class Valid(val payload: QrPayload) : QrDecodeResult

    data class Invalid(val reason: QrInvalidReason) : QrDecodeResult
}

enum class QrInvalidReason { FIELD_COUNT, VERSION, CHARSET, CHECKSUM }
