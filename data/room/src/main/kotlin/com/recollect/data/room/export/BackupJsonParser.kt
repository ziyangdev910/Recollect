package com.recollect.data.room.export

/**
 * 备份文件 JSON 解析器（纯 Kotlin，不引入 JSON 依赖，R-05）。
 *
 * 只用于导入：把用户选择的文件解析成 [JsonValue]，再交给导入器逐字段校验。
 * 解析失败抛 [BackupParseException]，消息可直接展示给用户。
 */
internal class BackupParseException(message: String) : Exception(message)

internal fun parseBackupJson(text: String): JsonValue = BackupJsonParser(text).parse()

private class BackupJsonParser(private val text: String) {
    private var index = 0

    fun parse(): JsonValue {
        val value = parseValue()
        skipWhitespace()
        if (index != text.length) fail("JSON 尾部存在多余内容")
        return value
    }

    private fun parseValue(): JsonValue {
        skipWhitespace()
        if (index >= text.length) fail("JSON 意外结束")
        return when (val ch = text[index]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> JsonValue.Str(parseString())
            't' -> parseLiteral("true", JsonValue.Bool(true))
            'f' -> parseLiteral("false", JsonValue.Bool(false))
            'n' -> parseLiteral("null", JsonValue.Null)
            else -> if (ch == '-' || ch.isDigit()) parseNumber() else fail("非法字符 '$ch'")
        }
    }

    private fun parseObject(): JsonValue.Obj {
        expect('{')
        skipWhitespace()
        val entries = mutableListOf<Pair<String, JsonValue>>()
        if (peek() == '}') {
            index++
            return JsonValue.Obj(entries)
        }
        while (true) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            expect(':')
            entries.add(key to parseValue())
            skipWhitespace()
            when (val ch = next()) {
                ',' -> continue
                '}' -> return JsonValue.Obj(entries)
                else -> fail("对象分隔符非法 '$ch'")
            }
        }
    }

    private fun parseArray(): JsonValue.Arr {
        expect('[')
        skipWhitespace()
        val items = mutableListOf<JsonValue>()
        if (peek() == ']') {
            index++
            return JsonValue.Arr(items)
        }
        while (true) {
            items.add(parseValue())
            skipWhitespace()
            when (val ch = next()) {
                ',' -> continue
                ']' -> return JsonValue.Arr(items)
                else -> fail("数组分隔符非法 '$ch'")
            }
        }
    }

    private fun parseString(): String {
        expect('"')
        val builder = StringBuilder()
        while (true) {
            if (index >= text.length) fail("字符串未闭合")
            when (val ch = text[index++]) {
                '"' -> return builder.toString()
                '\\' -> builder.append(parseEscape())
                else -> builder.append(ch)
            }
        }
    }

    private fun parseEscape(): Char = when (val ch = next()) {
        '"' -> '"'
        '\\' -> '\\'
        '/' -> '/'
        'b' -> '\b'
        'f' -> '\u000C'
        'n' -> '\n'
        'r' -> '\r'
        't' -> '\t'
        'u' -> {
            if (index + 4 > text.length) fail("非法 \\u 转义")
            val hex = text.substring(index, index + 4)
            index += 4
            hex.toIntOrNull(16)?.toChar() ?: fail("非法 \\u 转义")
        }

        else -> fail("非法转义 '\\$ch'")
    }

    private fun parseNumber(): JsonValue.Num {
        val start = index
        while (index < text.length && text[index] in "-+.eE0123456789") index++
        val raw = text.substring(start, index)
        val isInteger = raw.none { it == '.' || it == 'e' || it == 'E' }
        if (isInteger) {
            val value = raw.toLongOrNull() ?: fail("非法数字 '$raw'")
            return JsonValue.Num(value)
        }
        val value = raw.toDoubleOrNull() ?: fail("非法数字 '$raw'")
        return JsonValue.Num(value)
    }

    private fun parseLiteral(literal: String, value: JsonValue): JsonValue {
        if (!text.startsWith(literal, index)) fail("非法字面量")
        index += literal.length
        return value
    }

    private fun skipWhitespace() {
        while (index < text.length && text[index].isWhitespace()) index++
    }

    private fun peek(): Char? = text.getOrNull(index)

    private fun next(): Char {
        if (index >= text.length) fail("JSON 意外结束")
        return text[index++]
    }

    private fun expect(expected: Char) {
        val actual = next()
        if (actual != expected) fail("期望 '$expected'，实际 '$actual'")
    }

    private fun fail(message: String): Nothing =
        throw BackupParseException("备份文件 JSON 格式错误：$message（位置 $index）")
}
