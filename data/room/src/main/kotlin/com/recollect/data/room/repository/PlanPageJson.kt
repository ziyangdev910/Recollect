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
