package com.recollect.data.room.schema

/**
 * 测试专用极简 JSON 解析器：只用于读取 Room 导出的 schema JSON。
 *
 * 项目未引入 JSON 依赖（R-05），schema 测试也不应为了读取一个构建产物而新增依赖。
 */
internal sealed interface JsonValue {
    data class Obj(val entries: Map<String, JsonValue>) : JsonValue

    data class Arr(val items: List<JsonValue>) : JsonValue

    data class Str(val value: String) : JsonValue

    data class Num(val value: Double) : JsonValue

    data class Bool(val value: Boolean) : JsonValue

    data object Null : JsonValue
}

internal fun parseJson(text: String): JsonValue = JsonParser(text).parse()

internal fun JsonValue.asObj(): Map<String, JsonValue> = (this as JsonValue.Obj).entries

internal fun JsonValue.asArr(): List<JsonValue> = (this as JsonValue.Arr).items

internal fun JsonValue.asString(): String = (this as JsonValue.Str).value

internal fun JsonValue.asInt(): Int = (this as JsonValue.Num).value.toInt()

internal fun JsonValue.asBool(): Boolean = (this as JsonValue.Bool).value

internal fun Map<String, JsonValue>.required(key: String): JsonValue =
    get(key) ?: error("schema JSON 缺少字段 \"$key\"")

private class JsonParser(private val text: String) {
    private var index = 0

    fun parse(): JsonValue {
        val value = parseValue()
        skipWhitespace()
        require(index == text.length) { "JSON 尾部存在多余内容（位置 $index）" }
        return value
    }

    private fun parseValue(): JsonValue {
        skipWhitespace()
        require(index < text.length) { "JSON 意外结束（位置 $index）" }
        return when (val ch = text[index]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> JsonValue.Str(parseString())
            't' -> parseLiteral("true", JsonValue.Bool(true))
            'f' -> parseLiteral("false", JsonValue.Bool(false))
            'n' -> parseLiteral("null", JsonValue.Null)
            else -> if (ch == '-' || ch.isDigit()) parseNumber() else {
                error("非法 JSON 字符 '$ch'（位置 $index）")
            }
        }
    }

    private fun parseObject(): JsonValue.Obj {
        expect('{')
        skipWhitespace()
        val entries = linkedMapOf<String, JsonValue>()
        if (peek() == '}') {
            index++
            return JsonValue.Obj(entries)
        }
        while (true) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            expect(':')
            entries[key] = parseValue()
            skipWhitespace()
            when (val ch = next()) {
                ',' -> continue
                '}' -> return JsonValue.Obj(entries)
                else -> error("对象分隔符非法 '$ch'（位置 ${index - 1}）")
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
            items += parseValue()
            skipWhitespace()
            when (val ch = next()) {
                ',' -> continue
                ']' -> return JsonValue.Arr(items)
                else -> error("数组分隔符非法 '$ch'（位置 ${index - 1}）")
            }
        }
    }

    private fun parseString(): String {
        expect('"')
        val builder = StringBuilder()
        while (true) {
            require(index < text.length) { "字符串未闭合" }
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
            require(index + 4 <= text.length) { "非法 \\u 转义" }
            val hex = text.substring(index, index + 4)
            index += 4
            hex.toInt(16).toChar()
        }

        else -> error("非法转义 '\\$ch'（位置 ${index - 1}）")
    }

    private fun parseNumber(): JsonValue.Num {
        val start = index
        while (index < text.length && text[index] in "-+.eE0123456789") index++
        return JsonValue.Num(text.substring(start, index).toDouble())
    }

    private fun parseLiteral(literal: String, value: JsonValue): JsonValue {
        require(text.startsWith(literal, index)) { "非法字面量（位置 $index）" }
        index += literal.length
        return value
    }

    private fun skipWhitespace() {
        while (index < text.length && text[index].isWhitespace()) index++
    }

    private fun peek(): Char? = text.getOrNull(index)

    private fun next(): Char {
        require(index < text.length) { "JSON 意外结束（位置 $index）" }
        return text[index++]
    }

    private fun expect(expected: Char) {
        val actual = next()
        require(actual == expected) { "期望 '$expected'，实际 '$actual'（位置 ${index - 1}）" }
    }
}
