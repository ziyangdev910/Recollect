package com.recollect.data.room.repository

/**
 * 计划生成时冻结的打印元数据（存于 `plan.renderMeta`）。
 *
 * 复用已有 `plan_page` 的题块几何/行快照，但题号、知识点名称、行数、附件文本等
 * 只能从生成时的打印投影取得；落库后才能在进程重建时按原布局重新打印同一份计划。
 */
data class PlanRenderMeta(
    val questionId: String,
    val questionNumber: String,
    val sourcePage: String,
    val knowledgeNames: List<String>,
    val knowledgeLineCount: Int,
    val attachmentCount: Int,
    val hasNoteArea: Boolean,
    val noteTexts: List<String>,
)

/**
 * `plan.renderMeta` 的编解码。
 *
 * 结构：`[{"questionId","questionNumber","sourcePage","knowledgeNames":["…"],
 * "knowledgeLineCount","attachmentCount","hasNoteArea","noteTexts":["…"]}]`。
 * 解码严格：缺字段或结构非法直接抛 [IllegalArgumentException]。
 */
object PlanRenderMetaJson {

    fun encode(items: List<PlanRenderMeta>): String = buildString {
        append('[')
        items.forEachIndexed { index, item ->
            if (index > 0) append(',')
            append("{\"questionId\":")
            appendQuoted(item.questionId)
            append(",\"questionNumber\":")
            appendQuoted(item.questionNumber)
            append(",\"sourcePage\":")
            appendQuoted(item.sourcePage)
            append(",\"knowledgeNames\":")
            appendStringArray(item.knowledgeNames)
            append(",\"knowledgeLineCount\":").append(item.knowledgeLineCount)
            append(",\"attachmentCount\":").append(item.attachmentCount)
            append(",\"hasNoteArea\":").append(item.hasNoteArea)
            append(",\"noteTexts\":")
            appendStringArray(item.noteTexts)
            append('}')
        }
        append(']')
    }

    fun decode(json: String): List<PlanRenderMeta> = Reader(json).read()

    private fun StringBuilder.appendStringArray(values: List<String>) {
        append('[')
        values.forEachIndexed { index, value ->
            if (index > 0) append(',')
            appendQuoted(value)
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

    private class Reader(private val json: String) {

        private var pos = 0

        fun read(): List<PlanRenderMeta> {
            skipWhitespace()
            expect('[')
            val items = mutableListOf<PlanRenderMeta>()
            skipWhitespace()
            if (peek() == ']') {
                pos++
                return finish(items)
            }
            while (true) {
                items += readItem()
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return finish(items)
                    }

                    else -> fail("数组结构非法")
                }
            }
        }

        private fun readItem(): PlanRenderMeta {
            expect('{')
            val fields = readObjectFields()
            skipWhitespace()
            expect('}')
            return PlanRenderMeta(
                questionId = fields.string("questionId"),
                questionNumber = fields.string("questionNumber"),
                sourcePage = fields.string("sourcePage"),
                knowledgeNames = fields.stringList("knowledgeNames"),
                knowledgeLineCount = fields.number("knowledgeLineCount").toInt(),
                attachmentCount = fields.number("attachmentCount").toInt(),
                hasNoteArea = fields.boolean("hasNoteArea"),
                noteTexts = fields.stringList("noteTexts"),
            )
        }

        private fun readObjectFields(): Map<String, Any?> {
            val fields = mutableMapOf<String, Any?>()
            skipWhitespace()
            if (peek() == '}') return fields
            while (true) {
                skipWhitespace()
                val key = readString()
                skipWhitespace()
                expect(':')
                skipWhitespace()
                fields[key] = readValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    '}' -> return fields
                    else -> fail("对象结构非法")
                }
            }
        }

        private fun readValue(): Any? = when (peek()) {
            '"' -> readString()
            '[' -> readStringArray()
            't', 'f' -> readBoolean()
            '-', in '0'..'9' -> readNumber()
            else -> fail("不支持的值")
        }

        private fun readStringArray(): List<String> {
            expect('[')
            val values = mutableListOf<String>()
            skipWhitespace()
            if (peek() == ']') {
                pos++
                return values
            }
            while (true) {
                skipWhitespace()
                values += readString()
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return values
                    }

                    else -> fail("字符串数组结构非法")
                }
            }
        }

        private fun readBoolean(): Boolean = when {
            json.startsWith("true", pos) -> {
                pos += 4
                true
            }

            json.startsWith("false", pos) -> {
                pos += 5
                false
            }

            else -> fail("布尔值非法")
        }

        private fun readNumber(): Double {
            val start = pos
            while (pos < json.length && (json[pos].isDigit() || json[pos] in "-+.eE")) pos++
            return json.substring(start, pos).toDoubleOrNull() ?: fail("数字非法")
        }

        private fun readString(): String {
            expect('"')
            val value = StringBuilder()
            while (true) {
                if (pos >= json.length) fail("字符串未闭合")
                when (val ch = json[pos++]) {
                    '"' -> return value.toString()
                    '\\' -> {
                        if (pos >= json.length) fail("转义不完整")
                        when (val escaped = json[pos++]) {
                            '"' -> value.append('"')
                            '\\' -> value.append('\\')
                            '/' -> value.append('/')
                            'b' -> value.append('\b')
                            'f' -> value.append('\u000C')
                            'n' -> value.append('\n')
                            'r' -> value.append('\r')
                            't' -> value.append('\t')
                            'u' -> {
                                if (pos + 4 > json.length) fail("转义不完整")
                                value.append(json.substring(pos, pos + 4).toInt(16).toChar())
                                pos += 4
                            }

                            else -> fail("非法转义：$escaped")
                        }
                    }

                    else -> value.append(ch)
                }
            }
        }

        private fun skipWhitespace() {
            while (pos < json.length && json[pos].isWhitespace()) pos++
        }

        private fun peek(): Char = if (pos < json.length) json[pos] else '\u0000'

        private fun expect(ch: Char) {
            if (peek() != ch) fail("期望 $ch")
            pos++
        }

        private fun finish(items: List<PlanRenderMeta>): List<PlanRenderMeta> {
            skipWhitespace()
            if (pos != json.length) fail("存在多余内容")
            return items
        }

        private fun fail(message: String): Nothing =
            throw IllegalArgumentException("renderMeta JSON 非法：$message（位置 $pos）")

        private fun Map<String, Any?>.string(key: String): String =
            this[key] as? String ?: fail("缺少字符串字段 $key")

        private fun Map<String, Any?>.number(key: String): Double =
            this[key] as? Double ?: fail("缺少数字字段 $key")

        private fun Map<String, Any?>.boolean(key: String): Boolean =
            this[key] as? Boolean ?: fail("缺少布尔字段 $key")

        @Suppress("UNCHECKED_CAST")
        private fun Map<String, Any?>.stringList(key: String): List<String> =
            this[key] as? List<String> ?: fail("缺少字符串数组字段 $key")
    }
}
