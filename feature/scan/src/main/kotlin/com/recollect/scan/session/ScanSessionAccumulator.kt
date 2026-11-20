package com.recollect.scan.session

import java.time.LocalDate

/**
 * 一页识别结果中的一行（对应一个 `PendingFeedback` 行）。
 * `detectedTier` 用字符串保留识别档位，避免本层依赖数据库类型。
 */
data class ScannedRow(
    val planId: String,
    val pageId: String,
    val questionId: String,
    val knowledgePointId: String,
    val plannedDate: LocalDate,
    val detectedTier: String?,
)

data class ScanSessionState(
    val rows: List<ScannedRow>,
    val futureRows: List<ScannedRow>,
    val lateBatch: Boolean,
)

/**
 * 扫描会话累加器（scan-pipeline.md「扫描会话与批量回读」）。
 *
 * - 同一 `(planId,pageId,questionId,knowledgePointId)` 重复扫描保留最新识别结果，位置沿用首次出现；
 * - 计划日晚于扫描日的行单独列出，确认层只允许标 `NOT_REVIEWED`（不得给未来日期写非 `NOT_REVIEWED` 反馈）；
 * - 扫描日晚于 `planEndDate + 7` 时整体标记 `LATE_BATCH`，`planEnd+1..+7` 属正常窗口。
 *
 * 本类只做确定性合并，不写库、不改知识点状态（R-04）。
 */
class ScanSessionAccumulator(private val planEndDate: LocalDate) {

    private val rows = LinkedHashMap<String, ScannedRow>()
    private var scanDate: LocalDate? = null

    fun addPage(scanDate: LocalDate, pageRows: List<ScannedRow>): ScanSessionState {
        val current = this.scanDate
        if (current == null || scanDate.isAfter(current)) {
            this.scanDate = scanDate
        }
        pageRows.forEach { row -> rows[keyOf(row)] = row }
        return state()
    }

    fun state(): ScanSessionState {
        val today = scanDate ?: return ScanSessionState(emptyList(), emptyList(), lateBatch = false)
        val all = rows.values.toList()
        return ScanSessionState(
            rows = all,
            futureRows = all.filter { it.plannedDate.isAfter(today) },
            lateBatch = today.isAfter(planEndDate.plusDays(7)),
        )
    }

    private fun keyOf(row: ScannedRow): String =
        "${row.planId}|${row.pageId}|${row.questionId}|${row.knowledgePointId}"
}
