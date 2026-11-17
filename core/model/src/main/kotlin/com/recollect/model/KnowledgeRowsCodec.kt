package com.recollect.model

// PlanPage.knowledgeRows 的行快照 DTO 与编解码（C-046 方案 A、C-015）。
// JSON 结构见 docs/specs/data-model.md；rows 的顺序即打印时的行序，解码不得按 number 重排。
// questionId 来自 data-model.md 与 repository.md 的记录结构（C-070 方案 A，2026-11-17 已裁决）。

data class KnowledgeRow(
    val knowledgePointId: String,
    val number: Int,
)

data class PlanPageRows(
    val questionId: String,
    val rows: List<KnowledgeRow>,
    val triggeredKnowledgePointIds: List<String>,
)

object KnowledgeRowsCodec {

    /** 解析 `plan_page.knowledgeRows` 的 JSON；结构非法时抛 [IllegalArgumentException]。 */
    fun decode(json: String): List<PlanPageRows> = Reader(json).readPlanPageRowsList()

    /** 输出紧凑 JSON（UTF-8 存储由调用方负责）；字段与元素顺序保持入参顺序。 */
    fun encode(rows: List<PlanPageRows>): String = buildString {
        appendArray(rows) { page ->
            append("{\"questionId\":")
            appendQuoted(page.questionId)
            append(",\"rows\":")
            appendArray(page.rows) { row ->
                append("{\"knowledgePointId\":")
                appendQuoted(row.knowledgePointId)
                append(",\"number\":")
                append(row.number)
                append('}')
            }
            append(",\"triggeredKnowledgePointIds\":")
            appendArray(page.triggeredKnowledgePointIds) { appendQuoted(it) }
            append('}')
        }
    }
}

private inline fun <T> StringBuilder.appendArray(
    items: List<T>,
    appendItem: (T) -> Unit,
) {
    append('[')
    items.forEachIndexed { index, item ->
        if (index > 0) append(',')
        appendItem(item)
    }
    append(']')
}

private fun StringBuilder.appendQuoted(value: String) {
    append('"')
    value.forEach { ch ->
        when (ch) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else ->
                if (ch < ' ') {
                    append("\\u").append(ch.code.toString(16).padStart(4, '0'))
                } else {
                    append(ch)
                }
        }
    }
    append('"')
}

/** 只覆盖 knowledgeRows 所需语法的最小 JSON 读取器；未知字段跳过。 */
private class Reader(private val json: String) {

    private var pos = 0

    fun readPlanPageRowsList(): List<PlanPageRows> {
        skipWhitespace()
        val rows = readArray { readPlanPageRows() }
        skipWhitespace()
        if (pos != json.length) fail("unexpected trailing content")
        return rows
    }

    private fun readPlanPageRows(): PlanPageRows {
        var questionId: String? = null
        var rows: List<KnowledgeRow>? = null
        var triggeredKnowledgePointIds: List<String>? = null
        readObject { key ->
            when (key) {
                "questionId" -> questionId = readString()
                "rows" -> rows = readArray { readKnowledgeRow() }
                "triggeredKnowledgePointIds" -> triggeredKnowledgePointIds = readArray { readString() }
                else -> skipValue()
            }
        }
        return PlanPageRows(
            questionId = questionId ?: fail("missing questionId"),
            rows = rows ?: fail("missing rows"),
            triggeredKnowledgePointIds = triggeredKnowledgePointIds
                ?: fail("missing triggeredKnowledgePointIds"),
        )
    }

    private fun readKnowledgeRow(): KnowledgeRow {
        var knowledgePointId: String? = null
        var number: Int? = null
        readObject { key ->
            when (key) {
                "knowledgePointId" -> knowledgePointId = readString()
                "number" -> number = readInt()
                else -> skipValue()
            }
        }
        return KnowledgeRow(
            knowledgePointId = knowledgePointId ?: fail("missing knowledgePointId"),
            number = number ?: fail("missing number"),
        )
    }

    private inline fun readObject(consumeField: (String) -> Unit) {
        expect('{')
        skipWhitespace()
        if (peek() == '}') {
            pos++
            return
        }
        while (true) {
            val key = readString()
            expect(':')
            consumeField(key)
            skipWhitespace()
            when (next()) {
                ',' -> skipWhitespace()
                '}' -> return
                else -> fail("expected ',' or '}'")
            }
        }
    }

    private inline fun <T> readArray(element: () -> T): List<T> {
        expect('[')
        val result = mutableListOf<T>()
        skipWhitespace()
        if (peek() == ']') {
            pos++
            return result
        }
        while (true) {
            result += element()
            skipWhitespace()
            when (next()) {
                ',' -> skipWhitespace()
                ']' -> return result
                else -> fail("expected ',' or ']'")
            }
        }
    }

    private fun readString(): String {
        skipWhitespace()
        if (next() != '"') fail("expected string")
        val value = StringBuilder()
        while (true) {
            when (val ch = next()) {
                '"' -> return value.toString()
                '\\' -> {
                    when (val escape = next()) {
                        '"' -> value.append('"')
                        '\\' -> value.append('\\')
                        '/' -> value.append('/')
                        'b' -> value.append('\b')
                        'f' -> value.append('\u000C')
                        'n' -> value.append('\n')
                        'r' -> value.append('\r')
                        't' -> value.append('\t')
                        'u' -> {
                            if (pos + 4 > json.length) fail("truncated unicode escape")
                            val code = json.substring(pos, pos + 4).toIntOrNull(16)
                                ?: fail("invalid unicode escape")
                            value.append(code.toChar())
                            pos += 4
                        }
                        else -> fail("invalid escape '\\$escape'")
                    }
                }
                else -> value.append(ch)
            }
        }
    }

    private fun readInt(): Int {
        skipWhitespace()
        val start = pos
        if (peek() == '-') pos++
        while (pos < json.length && json[pos] in '0'..'9') pos++
        if (pos == start || (pos == start + 1 && json[start] == '-')) fail("expected integer")
        return json.substring(start, pos).toIntOrNull() ?: fail("integer out of range")
    }

    private fun skipValue() {
        skipWhitespace()
        when (peek()) {
            '"' -> readString()
            '{' -> readObject { skipValue() }
            '[' -> readArray { skipValue() }
            else -> {
                val start = pos
                while (pos < json.length &&
                    json[pos] !in ",]}" &&
                    !json[pos].isWhitespace()
                ) {
                    pos++
                }
                if (pos == start) fail("unexpected value")
            }
        }
    }

    private fun peek(): Char =
        if (pos < json.length) json[pos] else fail("unexpected end of JSON")

    private fun next(): Char {
        val ch = peek()
        pos++
        return ch
    }

    private fun expect(expected: Char) {
        skipWhitespace()
        if (next() != expected) fail("expected '$expected'")
    }

    private fun skipWhitespace() {
        while (pos < json.length && json[pos].isWhitespace()) pos++
    }

    private fun fail(message: String): Nothing =
        throw IllegalArgumentException("Invalid knowledgeRows JSON at offset $pos: $message")
}
