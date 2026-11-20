package com.recollect.scan.manual

/**
 * 手动回退输入的规范化（scan-pipeline.md「手动回退」、confirm-flow.md）。
 *
 * 规则：
 * - 大小写不敏感；
 * - `I`/`L → 1`、`O → 0`（Crockford Base32 的易混字符）；
 * - 只允许 `A-Z a-z 0-9 -`；
 * - 结构必须为 `6 位计划号-2 位页码`（例如 `K7M2Q9-01`）。
 */
object ScanManualCodec {

    private val PAGE_ID_PATTERN = Regex("[0-9A-HJKMNP-TV-Z]{6}-[0-9]{2}")

    /** 返回规范化后的 `pageId`；格式不合法返回 `null`。 */
    fun normalizePageId(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        if (!trimmed.all { isAllowed(it) }) return null

        val normalized = buildString(trimmed.length) {
            trimmed.uppercase().forEach { char ->
                append(
                    when (char) {
                        'I', 'L' -> '1'
                        'O' -> '0'
                        else -> char
                    },
                )
            }
        }
        return normalized.takeIf { PAGE_ID_PATTERN.matches(it) }
    }

    /** 只接受 ASCII 字母、数字与连字符；避免中文输入法下的全角字符被当成合法输入。 */
    private fun isAllowed(char: Char): Boolean =
        char == '-' || (char in '0'..'9') || (char in 'A'..'Z') || (char in 'a'..'z')
}
