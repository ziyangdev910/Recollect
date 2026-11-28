package com.recollect.feature.confirm

/**
 * 卡片确认后的笔记摘要（2026-11-28 用户要求）。
 *
 * - 单行笔记接在「复习笔记：」后面，方便一行读完；
 * - 多行笔记换行开始，保留原始换行；
 * - 没有识别到笔记时显示「复习笔记：无」。
 */
internal fun reviewNoteText(text: String?): String {
    val trimmed = text?.trim().orEmpty()
    return when {
        trimmed.isEmpty() -> "复习笔记：无"
        trimmed.contains('\n') -> "复习笔记：\n$trimmed"
        else -> "复习笔记：$trimmed"
    }
}
