package com.recollect.common

/** 缺题号时的占位；预览与打印只展示可读文字，不暴露题目 id。 */
const val UNKNOWN_QUESTION_NUMBER = "未知题号"

/**
 * 题目标识（2026-11-25 用户要求）：`来源 第X页 第Y题`。
 *
 * [sourcePage] 为数据层的来源页码展示串 `页码编号-来源说明`（说明为空时只有编号，如 `1`、`2-同步练习`）：
 * - X 取页码编号，来源取说明，Y 取 [questionNumber]；
 * - 有来源说明时输出 `同步练习 第12页 第T-1题`；
 * - 没有来源说明时页码写成「编号」，输出 `编号12 第T-1题`；
 * - 旧稿整串没有页码编号时（如 `P12`）整串作为来源，输出 `P12 第T-1题`；
 * - 没有来源时只输出 `第T-1题`；题号缺失时沿用 [UNKNOWN_QUESTION_NUMBER]。
 *
 * 计划预览与打印稿共用本函数，避免两处各写一套展示格式。
 */
fun formatQuestionLabel(sourcePage: String, questionNumber: String): String {
    val lines = formatQuestionLabelLines(sourcePage, questionNumber)
    return listOf(lines.first, lines.second)
        .filter { it.isNotBlank() }
        .joinToString(" ")
}

/**
 * 题目标识的分行展示（2026-11-25 用户要求）：
 * - 有来源说明：[first] 为来源，[second] 为 `第X页 第Y题`；
 * - 没有来源说明：[first] 为 `编号X`，[second] 为 `第Y题`；
 * - 旧稿整串没有页码编号：[first] 为整串，[second] 为 `第Y题`；
 * - 没有来源：[first] 为 `第Y题`，[second] 为空；题号缺失时 [first] 为 [UNKNOWN_QUESTION_NUMBER]。
 */
data class QuestionLabelLines(val first: String, val second: String)

fun formatQuestionLabelLines(sourcePage: String, questionNumber: String): QuestionLabelLines {
    val number = questionNumber.trim()
    if (number == UNKNOWN_QUESTION_NUMBER) {
        return QuestionLabelLines(first = UNKNOWN_QUESTION_NUMBER, second = "")
    }
    val question = if (number.isBlank()) UNKNOWN_QUESTION_NUMBER else "第${number}题"
    val (pageCode, rawLabel) = splitSourcePage(sourcePage)
    val label = rawLabel.takeIf { it.isNotBlank() }
    return when {
        label != null -> QuestionLabelLines(
            first = label,
            second = listOfNotNull(pageCode?.let { "第${it}页" }, question).joinToString(" "),
        )
        pageCode != null -> QuestionLabelLines(first = "编号${pageCode}", second = question)
        else -> QuestionLabelLines(first = question, second = "")
    }
}

/** 把 `页码编号-来源说明` 拆成（页码编号, 来源说明）；前缀不是纯数字时视为旧稿，整串作为来源。 */
internal fun splitSourcePage(display: String): Pair<String?, String> {
    val trimmed = display.trim()
    if (trimmed.isEmpty()) return null to ""
    // 说明为空时展示串只有编号（如 `12`），整串即页码编号。
    if (trimmed.all(Char::isDigit)) return trimmed to ""
    val separator = trimmed.indexOf('-')
    if (separator <= 0) return null to trimmed
    val code = trimmed.substring(0, separator)
    return if (code.all(Char::isDigit)) {
        code to trimmed.substring(separator + 1).trim()
    } else {
        null to trimmed
    }
}
