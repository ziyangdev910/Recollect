package com.recollect.data.room.repository

import com.recollect.scheduler.DeferReason

/**
 * 计划生成时被推迟题目的冻结快照（存于 `plan.deferred`）。
 *
 * 调度器把冷却期、当天容量不足、关联知识点失效的题目放进 `PlanResult.deferred`；
 * 这些题目不会出现在打印稿里，但用户需要知道“为什么少了一道题”。
 * 计划生成后按 `questionId + 题号 + 来源页码 + 原因` 落库，重开 App 查看同一计划时仍能展示，
 * 并按 `页码-第X题` 区分不同页码上的相同题号。
 */
data class PlanDeferredRecord(
    val questionId: String,
    val questionNumber: String,
    /** 来源页码（已格式化为 `编号-说明`），空串表示题目未标注来源。 */
    val sourcePage: String = "",
    val reason: DeferReason,
)

/**
 * `plan.deferred` 的编解码。
 *
 * 结构：`[{"questionId":"…","questionNumber":"…","sourcePage":"…","reason":"TIME_OVERFLOW"}]`。
 * 解码严格：缺 `questionId`/`questionNumber`/`reason`、结构非法或未知原因直接抛
 * [IllegalArgumentException]；`sourcePage` 为后加字段，缺失时按空串兼容旧计划。
 */
object PlanDeferredJson {

    fun encode(items: List<PlanDeferredRecord>): String = buildString {
        append('[')
        items.forEachIndexed { index, item ->
            if (index > 0) append(',')
            append("{\"questionId\":")
            appendQuoted(item.questionId)
            append(",\"questionNumber\":")
            appendQuoted(item.questionNumber)
            append(",\"sourcePage\":")
            appendQuoted(item.sourcePage)
            append(",\"reason\":")
            appendQuoted(item.reason.name)
            append('}')
        }
        append(']')
    }

    fun decode(json: String): List<PlanDeferredRecord> = Reader(json).read()

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

        fun read(): List<PlanDeferredRecord> {
            skipWhitespace()
            expect('[')
            val items = mutableListOf<PlanDeferredRecord>()
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

        private fun readItem(): PlanDeferredRecord {
            expect('{')
            val fields = mutableMapOf<String, String>()
            skipWhitespace()
            if (peek() != '}') {
                while (true) {
                    skipWhitespace()
                    val key = readString()
                    skipWhitespace()
                    expect(':')
                    skipWhitespace()
                    fields[key] = readString()
                    skipWhitespace()
                    when (peek()) {
                        ',' -> pos++
                        '}' -> break
                        else -> fail("对象结构非法")
                    }
                }
            }
            expect('}')
            val questionId = fields["questionId"] ?: fail("缺少 questionId")
            val questionNumber = fields["questionNumber"] ?: fail("缺少 questionNumber")
            val reasonName = fields["reason"] ?: fail("缺少 reason")
            val reason = try {
                DeferReason.valueOf(reasonName)
            } catch (e: IllegalArgumentException) {
                fail("未知的推迟原因：$reasonName")
            }
            return PlanDeferredRecord(
                questionId = questionId,
                questionNumber = questionNumber,
                sourcePage = fields["sourcePage"].orEmpty(),
                reason = reason,
            )
        }

        private fun readString(): String {
            expect('"')
            val value = StringBuilder()
            while (true) {
                if (pos >= json.length) fail("字符串未闭合")
                when (val ch = json[pos++]) {
                    '"' -> return value.toString()
                    '\\' -> {
                        if (pos >= json.length) fail("转义序列未闭合")
                        when (val esc = json[pos++]) {
                            '"' -> value.append('"')
                            '\\' -> value.append('\\')
                            '/' -> value.append('/')
                            'b' -> value.append('\b')
                            'f' -> value.append('\u000C')
                            'n' -> value.append('\n')
                            'r' -> value.append('\r')
                            't' -> value.append('\t')
                            'u' -> {
                                if (pos + 4 > json.length) fail("\\u 转义不完整")
                                val hex = json.substring(pos, pos + 4)
                                pos += 4
                                value.append(hex.toIntOrNull(16)?.toChar() ?: fail("\\u 转义非法"))
                            }

                            else -> fail("未知转义：\\$esc")
                        }
                    }

                    else -> value.append(ch)
                }
            }
        }

        private fun skipWhitespace() {
            while (pos < json.length && json[pos].isWhitespace()) pos++
        }

        private fun peek(): Char = if (pos < json.length) json[pos] else fail("JSON 意外结束")

        private fun expect(expected: Char) {
            if (peek() != expected) fail("期望 '$expected'，实际 '${peek()}'")
            pos++
        }

        private fun finish(items: List<PlanDeferredRecord>): List<PlanDeferredRecord> {
            skipWhitespace()
            if (pos != json.length) fail("JSON 末尾存在多余内容")
            return items
        }

        private fun fail(message: String): Nothing =
            throw IllegalArgumentException("plan.deferred 解码失败：$message")
    }
}
