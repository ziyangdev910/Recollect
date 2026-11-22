package com.recollect.data.room.export

/**
 * 导出用的极简 JSON 值模型与序列化器（纯 Kotlin，不引入 JSON 依赖，R-05）。
 *
 * 只负责写、不负责读；对象键顺序按调用方给定的顺序输出，便于人工核对导出文件。
 * 数值用 `toString()` 输出：Long/Int 无小数，Float/Double 保留 Kotlin 的十进制表示。
 */
internal sealed interface JsonValue {
    data class Obj(val entries: List<Pair<String, JsonValue>>) : JsonValue
    data class Arr(val items: List<JsonValue>) : JsonValue
    data class Str(val value: String) : JsonValue
    data class Num(val value: Number) : JsonValue
    data class Bool(val value: Boolean) : JsonValue
    data object Null : JsonValue
}

internal fun jsonObject(vararg entries: Pair<String, JsonValue>): JsonValue =
    JsonValue.Obj(entries.toList())

internal fun List<JsonValue>.asJsonArray(): JsonValue = JsonValue.Arr(this)

internal fun String?.asJsonString(): JsonValue =
    if (this == null) JsonValue.Null else JsonValue.Str(this)

internal fun Number?.asJsonNumber(): JsonValue =
    if (this == null) JsonValue.Null else JsonValue.Num(this)

internal fun Boolean?.asJsonBool(): JsonValue =
    if (this == null) JsonValue.Null else JsonValue.Bool(this)

internal fun JsonValue.render(): String = buildString { appendJson(this, this@render, indent = 0) }

private fun appendJson(out: StringBuilder, value: JsonValue, indent: Int) {
    when (value) {
        is JsonValue.Obj -> {
            if (value.entries.isEmpty()) {
                out.append("{}")
                return
            }
            out.append('{')
            value.entries.forEachIndexed { index, (key, child) ->
                if (index > 0) out.append(',')
                newLine(out, indent + 1)
                appendString(out, key)
                out.append(": ")
                appendJson(out, child, indent + 1)
            }
            newLine(out, indent)
            out.append('}')
        }

        is JsonValue.Arr -> {
            if (value.items.isEmpty()) {
                out.append("[]")
                return
            }
            out.append('[')
            value.items.forEachIndexed { index, child ->
                if (index > 0) out.append(',')
                newLine(out, indent + 1)
                appendJson(out, child, indent + 1)
            }
            newLine(out, indent)
            out.append(']')
        }

        is JsonValue.Str -> appendString(out, value.value)
        is JsonValue.Num -> out.append(value.value.toString())
        is JsonValue.Bool -> out.append(if (value.value) "true" else "false")
        JsonValue.Null -> out.append("null")
    }
}

private fun newLine(out: StringBuilder, indent: Int) {
    out.append('\n')
    repeat(indent) { out.append("  ") }
}

private fun appendString(out: StringBuilder, value: String) {
    out.append('"')
    for (ch in value) {
        when (ch) {
            '"' -> out.append("\\\"")
            '\\' -> out.append("\\\\")
            '\n' -> out.append("\\n")
            '\r' -> out.append("\\r")
            '\t' -> out.append("\\t")
            '\b' -> out.append("\\b")
            '\u000C' -> out.append("\\f")
            else -> {
                if (ch < ' ') {
                    out.append("\\u")
                    out.append(ch.code.toString(16).padStart(4, '0'))
                } else {
                    out.append(ch)
                }
            }
        }
    }
    out.append('"')
}
