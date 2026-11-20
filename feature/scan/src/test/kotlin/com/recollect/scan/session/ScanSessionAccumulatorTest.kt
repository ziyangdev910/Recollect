package com.recollect.scan.session

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * 扫描会话（docs/specs/scan-pipeline.md「扫描会话与批量回读（C-038、C-040、C-041）」）：
 * 同页重复扫描按 `(planId,pageId,questionId,knowledgePointId)` 去重保留最新；
 * 早于 planEndDate 扫描时对未来日期的行只允许 `NOT_REVIEWED`；
 * 扫描日晚于 `planEndDate + 7` 标记 `LATE_BATCH`。
 */
class ScanSessionAccumulatorTest {

    @Test
    fun `appends multiple pages in scan order`() {
        val session = ScanSessionAccumulator(planEndDate = END)

        session.addPage(SCAN_DAY, listOf(row(page = "P-01", question = "q1")))
        val state = session.addPage(SCAN_DAY, listOf(row(page = "P-02", question = "q2")))

        assertEquals(listOf("P-01", "P-02"), state.rows.map { it.pageId })
    }

    @Test
    fun `keeps the latest result when the same row is scanned twice`() {
        val session = ScanSessionAccumulator(planEndDate = END)

        session.addPage(SCAN_DAY, listOf(row(page = "P-01", question = "q1", tier = "MASTERED")))
        val state = session.addPage(SCAN_DAY, listOf(row(page = "P-01", question = "q1", tier = "UNKNOWN")))

        assertEquals(1, state.rows.size)
        assertEquals("UNKNOWN", state.rows.single().detectedTier)
    }

    @Test
    fun `flags rows planned after the scan day as future rows`() {
        val session = ScanSessionAccumulator(planEndDate = END)

        val state = session.addPage(
            SCAN_DAY,
            listOf(
                row(page = "P-01", question = "q1", plannedDate = SCAN_DAY.minusDays(1)),
                row(page = "P-02", question = "q2", plannedDate = SCAN_DAY.plusDays(1)),
            ),
        )

        assertEquals(1, state.futureRows.size)
        assertEquals("q2", state.futureRows.single().questionId)
    }

    @Test
    fun `marks late batch only after plan end plus seven days`() {
        val session = ScanSessionAccumulator(planEndDate = END)

        assertFalse(session.addPage(END.plusDays(7), emptyList()).lateBatch)
        assertTrue(session.addPage(END.plusDays(8), emptyList()).lateBatch)
    }

    private fun row(
        page: String,
        question: String,
        plannedDate: LocalDate = SCAN_DAY,
        tier: String = "MASTERED",
    ) = ScannedRow(
        planId = "K7M2Q9",
        pageId = page,
        questionId = question,
        knowledgePointId = "kp-1",
        plannedDate = plannedDate,
        detectedTier = tier,
    )

    private companion object {
        val END: LocalDate = LocalDate.of(2026, 11, 22)
        val SCAN_DAY: LocalDate = LocalDate.of(2026, 11, 18)
    }
}
