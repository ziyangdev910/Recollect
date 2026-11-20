package com.recollect.data.room.repository

import com.recollect.pagination.NoteWritingArea
import com.recollect.pagination.ScanItemGeometry

/**
 * `plan_page.itemGeometry` 的编解码（C-093 方案 A）。
 *
 * 结构：`[{"questionId","topMm","heightMm","rowCount","note":{"leftMm","topMm","widthMm","heightMm"}|null}]`，
 * 顺序即题目顺序。旧稿（迁移前）为 `"[]"`，表示「没有几何快照，不可回读」。
 * 解码严格：字段缺失或结构非法直接抛 [IllegalArgumentException]，回读端不得猜测几何。
 */
object ItemGeometryJson {

    fun encode(items: List<ScanItemGeometry>): String = buildString {
        append('[')
        items.forEachIndexed { index, item ->
            if (index > 0) append(',')
            append("{\"questionId\":")
            appendQuoted(item.questionId)
            append(",\"topMm\":").append(item.topMm)
            append(",\"heightMm\":").append(item.heightMm)
            append(",\"rowCount\":").append(item.rowCount)
            append(",\"note\":")
            val note = item.noteArea
            if (note == null) {
                append("null")
            } else {
                append("{\"leftMm\":").append(note.leftMm)
                append(",\"topMm\":").append(note.topMm)
                append(",\"widthMm\":").append(note.widthMm)
                append(",\"heightMm\":").append(note.heightMm)
                append('}')
            }
            append('}')
        }
        append(']')
    }

    fun decode(json: String): List<ScanItemGeometry> = Reader(json).read()

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

    /** 覆盖本结构所需语法的最小读取器。 */
    private class Reader(private val json: String) {

        private var pos = 0

        fun read(): List<ScanItemGeometry> {
            skipWhitespace()
            expect('[')
            val items = mutableListOf<ScanItemGeometry>()
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

        private fun readItem(): ScanItemGeometry {
            expect('{')
            val fields = readObjectFields()
            skipWhitespace()
            expect('}')
            return ScanItemGeometry(
                questionId = fields.string("questionId"),
                topMm = fields.number("topMm"),
                heightMm = fields.number("heightMm"),
                rowCount = fields.number("rowCount").toInt(),
                noteArea = fields.noteArea(),
            )
        }

        private fun readObjectFields(): MutableMap<String, Any?> {
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

        private fun readValue(): Any? = when (val ch = peek()) {
            '{' -> {
                pos++
                val nested = readObjectFields()
                skipWhitespace()
                expect('}')
                nested
            }

            '"' -> readString()
            'n' -> {
                expectLiteral("null")
                null
            }

            else -> if (ch == '-' || ch.isDigit()) readNumber() else fail("非法值")
        }

        private fun Map<*, *>.string(key: String): String =
            this[key] as? String ?: fail("缺少字段 $key")

        private fun Map<*, *>.noteArea(): NoteWritingArea? {
            val note = this["note"] ?: return null
            val obj = note as? Map<*, *> ?: fail("note 结构非法")
            return NoteWritingArea(
                leftMm = obj.number("leftMm"),
                topMm = obj.number("topMm"),
                widthMm = obj.number("widthMm"),
                heightMm = obj.number("heightMm"),
            )
        }

        private fun Map<*, *>.number(key: String): Double =
            this[key] as? Double ?: fail("缺少字段 $key")

        private fun readNumber(): Double {
            val start = pos
            if (peek() == '-') pos++
            while (pos < json.length && (json[pos].isDigit() || json[pos] in ".eE+-")) pos++
            return json.substring(start, pos).toDoubleOrNull() ?: fail("数字非法")
        }

        private fun expectLiteral(literal: String) {
            if (!json.startsWith(literal, pos)) fail("期望 $literal")
            pos += literal.length
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

                            else -> fail("非法转义")
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

        private fun finish(items: List<ScanItemGeometry>): List<ScanItemGeometry> {
            skipWhitespace()
            if (pos != json.length) fail("存在多余内容")
            return items
        }

        private fun fail(message: String): Nothing =
            throw IllegalArgumentException("itemGeometry $message：位置 $pos")
    }
}
