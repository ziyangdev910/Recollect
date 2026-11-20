package com.recollect.data.room.repository

/**
 * 来源页码的展示格式：系统编号 + 可选说明。
 *
 * 说明为空时只显示编号（不输出「-」）；例如 `1`、`2-同步练习`。
 * 编号由数据层分配，用户只能编辑说明文字。
 */
fun formatSourcePage(code: Int, label: String): String {
    val trimmed = label.trim()
    return if (trimmed.isEmpty()) code.toString() else "$code-$trimmed"
}
