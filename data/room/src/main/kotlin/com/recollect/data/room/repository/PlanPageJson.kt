package com.recollect.data.room.repository

import com.recollect.pagination.ContinuationMarker

/**
 * `plan_page.questionIds` 与 `continuationMarkers` 的序列化（data-model.md）。
 *
 * `knowledgeRows` 不在这里拼接：它由 `core:model` 的 `KnowledgeRowsCodec` 编码（C-046/C-071）。
 */
internal object PlanPageJson {

    fun encodeQuestionIds(questionIds: List<String>): String = buildString {
        append('[')
        questionIds.forEachIndexed { index, questionId ->
            if (index > 0) append(',')
            appendQuoted(questionId)
        }
        append(']')
    }

    /** 解析 `plan_page.questionIds`；结构非法时抛 [IllegalArgumentException]（回读端不得猜测）。 */
    fun decodeQuestionIds(json: String): List<String> {
        val reader = QuestionIdsReader(json)
        return reader.readArray()
    }

    fun encodeContinuationMarkers(markers: List<ContinuationMarker>): String = buildString {
        append('[')
        markers.forEachIndexed { index, marker ->
            if (index > 0) append(',')
            append("{\"fromPageId\":")
            appendQuoted(marker.fromPageId)
            append(",\"toPageId\":")
            appendQuoted(marker.toPageId)
            append(",\"questionId\":")
            appendQuoted(marker.questionId)
            append(",\"order\":")
            append(marker.order)
            append('}')
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
}

/** 只覆盖 `questionIds` 所需语法的最小读取器；未知/非法结构直接抛错。 */
private class QuestionIdsReader(private val json: String) {

    private var pos = 0

    fun readArray(): List<String> {
        skipWhitespace()
        expect('[')
        val result = mutableListOf<String>()
        skipWhitespace()
        if (peek() == ']') {
            pos++
            finish()
            return result
        }
        while (true) {
            skipWhitespace()
            result += readString()
            skipWhitespace()
            when (peek()) {
                ',' -> pos++
                ']' -> {
                    pos++
                    finish()
                    return result
                }

                else -> throw IllegalArgumentException("questionIds 数组结构非法：位置 $pos")
            }
        }
    }

    private fun readString(): String {
        expect('"')
        val value = StringBuilder()
        while (true) {
            if (pos >= json.length) throw IllegalArgumentException("questionIds 字符串未闭合")
            val ch = json[pos++]
            when (ch) {
                '"' -> return value.toString()
                '\\' -> {
                    if (pos >= json.length) throw IllegalArgumentException("questionIds 转义不完整")
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
                            if (pos + 4 > json.length) throw IllegalArgumentException("questionIds 转义不完整")
                            value.append(json.substring(pos, pos + 4).toInt(16).toChar())
                            pos += 4
                        }

                        else -> throw IllegalArgumentException("questionIds 非法转义：$escaped")
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
        if (peek() != ch) throw IllegalArgumentException("questionIds 期望 $ch，位置 $pos")
        pos++
    }

    private fun finish() {
        skipWhitespace()
        if (pos != json.length) throw IllegalArgumentException("questionIds 存在多余内容：位置 $pos")
    }
}
