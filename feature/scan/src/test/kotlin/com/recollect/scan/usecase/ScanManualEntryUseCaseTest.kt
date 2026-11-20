package com.recollect.scan.usecase

import com.recollect.data.room.entity.DetectionStatus
import com.recollect.data.room.entity.ScanSessionStatus
import com.recollect.data.room.repository.PendingFeedbackInput
import com.recollect.data.room.repository.PendingFeedbackRow
import com.recollect.data.room.repository.PlanPageFacts
import com.recollect.data.room.repository.ScanSessionRepository
import com.recollect.data.room.repository.ScanSessionSnapshot
import com.recollect.model.KnowledgeRow
import com.recollect.model.PlanPageRows
import com.recollect.pagination.NoteWritingArea
import com.recollect.pagination.ScanItemGeometry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 手动回退（scan-pipeline.md「手动回退」、C-011 方案二）：
 * `pageId + 题号` 解析唯一题目，写入 `UNREADABLE` 行，不校验 QR 的版本/CRC。
 */
class ScanManualEntryUseCaseTest {

    private val repository = FakeRepository()
    private val useCase = ScanManualEntryUseCase(
        repository = repository,
        questionNumbers = { mapOf("q1" to "T-1", "q2" to "T-2") },
        clock = Clock.fixed(Instant.parse("2026-11-18T08:00:00Z"), ZoneOffset.UTC),
    )

    @Test
    fun `stores unreadable rows in snapshot order for the resolved question`() {
        repository.facts = facts()

        val outcome = runBlocking { useCase.submit("k7m2q9-01", " T-1 ") }

        assertTrue(outcome is ScanManualOutcome.Stored)
        assertEquals("q1", (outcome as ScanManualOutcome.Stored).questionId)
        val rows = requireNotNull(repository.appended)
        assertEquals(listOf("kp-1", "kp-2"), rows.map { it.knowledgePointId })
        assertEquals(listOf(true, false), rows.map { it.triggered })
        assertTrue(rows.all { it.detectionStatus == DetectionStatus.UNREADABLE })
        assertTrue(rows.all { it.detectedTier == null && it.detectedRawOcr == null })
        assertTrue(repository.readyMarked)
    }

    @Test
    fun `rejects a malformed page id`() {
        val outcome = runBlocking { useCase.submit("K7M2Q9-1", "T-1") }

        assertTrue(outcome is ScanManualOutcome.Rejected)
        assertTrue((outcome as ScanManualOutcome.Rejected).message.contains("格式错误"))
        assertNull(repository.appended)
    }

    @Test
    fun `rejects an empty question number`() {
        val outcome = runBlocking { useCase.submit("K7M2Q9-01", "  ") }

        assertTrue(outcome is ScanManualOutcome.Rejected)
        assertNull(repository.appended)
    }

    @Test
    fun `rejects a page without a print snapshot`() {
        repository.facts = null

        val outcome = runBlocking { useCase.submit("K7M2Q9-01", "T-1") }

        assertTrue(outcome is ScanManualOutcome.Rejected)
        assertNull(repository.appended)
    }

    @Test
    fun `rejects a question number that is not on the page`() {
        repository.facts = facts()

        val outcome = runBlocking { useCase.submit("K7M2Q9-01", "T-9") }

        assertTrue(outcome is ScanManualOutcome.Rejected)
        assertTrue((outcome as ScanManualOutcome.Rejected).message.contains("题号"))
        assertNull(repository.appended)
    }

    @Test
    fun `rejects a question number that matches more than one question`() {
        repository.facts = facts()
        val ambiguous = ScanManualEntryUseCase(
            repository = repository,
            questionNumbers = { mapOf("q1" to "T-1", "q2" to "T-1") },
            clock = Clock.fixed(Instant.parse("2026-11-18T08:00:00Z"), ZoneOffset.UTC),
        )

        val outcome = runBlocking { ambiguous.submit(PAGE_ID, "T-1") }

        assertTrue(outcome is ScanManualOutcome.Rejected)
        assertNull(repository.appended)
    }

    @Test
    fun `rejects an old page without knowledge rows`() {
        repository.facts = facts(knowledgeRows = emptyList())

        val outcome = runBlocking { useCase.submit("K7M2Q9-01", "T-1") }

        assertTrue(outcome is ScanManualOutcome.Rejected)
        assertTrue((outcome as ScanManualOutcome.Rejected).message.contains("重新生成"))
    }

    @Test
    fun `flags a page that was already scanned in the same session`() {
        repository.facts = facts()
        repository.existingPageIds = listOf(PAGE_ID)

        val outcome = runBlocking { useCase.submit(PAGE_ID, "T-1") }

        assertTrue(outcome is ScanManualOutcome.Stored)
        assertTrue((outcome as ScanManualOutcome.Stored).replacedPage)
    }

    @Test
    fun `flags late batch and future page`() {
        repository.facts = facts(date = SCAN_DAY.plusDays(3))
        repository.planEnd = SCAN_DAY.minusDays(8)

        val outcome = runBlocking { useCase.submit(PAGE_ID, "T-1") } as ScanManualOutcome.Stored

        assertTrue(outcome.lateBatch)
        assertTrue(outcome.pageIsFuture)
        assertEquals(SCAN_DAY.plusDays(3), outcome.pageDate)
    }

    @Test
    fun `rejects when the session can no longer accept pages`() {
        repository.facts = facts()
        repository.appendError = IllegalStateException("会话已结束")

        val outcome = runBlocking { useCase.submit(PAGE_ID, "T-1") }

        assertTrue(outcome is ScanManualOutcome.Rejected)
        assertTrue((outcome as ScanManualOutcome.Rejected).message.contains("会话"))
    }

    private fun facts(
        date: LocalDate = PAGE_DATE,
        knowledgeRows: List<PlanPageRows> = listOf(
            PlanPageRows(
                questionId = "q1",
                rows = listOf(KnowledgeRow("kp-1", 1), KnowledgeRow("kp-2", 2)),
                triggeredKnowledgePointIds = listOf("kp-1"),
            ),
        ),
    ) = PlanPageFacts(
        planId = PLAN_ID,
        pageId = PAGE_ID,
        date = date,
        questionIds = listOf("q1", "q2"),
        knowledgeRows = knowledgeRows,
        items = listOf(
            ScanItemGeometry(
                questionId = "q1",
                topMm = 41.0,
                heightMm = 30.0,
                rowCount = 2,
                noteArea = NoteWritingArea(92.2, 68.0, 88.8, 25.0),
            ),
        ),
    )

    private class FakeRepository : ScanSessionRepository {
        var facts: PlanPageFacts? = null
        var appended: List<PendingFeedbackInput>? = null
        var existingPageIds: List<String> = emptyList()
        var appendError: RuntimeException? = null
        var planEnd: LocalDate? = PAGE_DATE.plusDays(6)
        var readyMarked = false

        override suspend fun pageFacts(pageId: String): PlanPageFacts? =
            facts?.takeIf { it.pageId == pageId }

        override suspend fun startOrResumeSession(planId: String, now: Instant): ScanSessionSnapshot =
            ScanSessionSnapshot("session-1", planId, ScanSessionStatus.SCANNING, created = true)

        override suspend fun activeSession(): ScanSessionSnapshot? = null

        override suspend fun appendPage(
            sessionId: String,
            pageId: String,
            rows: List<PendingFeedbackInput>,
        ): Int {
            appendError?.let { throw it }
            appended = rows
            return rows.size
        }

        override suspend fun pendingRows(sessionId: String): List<PendingFeedbackRow> =
            existingPageIds.map { pageId ->
                PendingFeedbackRow(
                    planId = PLAN_ID,
                    pageId = pageId,
                    plannedDate = PAGE_DATE,
                    actualReviewDate = PAGE_DATE,
                    questionId = "q1",
                    knowledgePointId = "kp-1",
                    triggered = true,
                    detectedTier = null,
                    detectionStatus = DetectionStatus.UNREADABLE,
                    conflict = false,
                    detectedRawOcr = null,
                    userDecision = com.recollect.data.room.entity.UserDecision.UNMARKED,
                )
            }

        override suspend fun planEndDate(planId: String): LocalDate? = planEnd

        override suspend fun markReady(sessionId: String) {
            readyMarked = true
        }

        override suspend fun abandonStaleSessions(now: Instant) = Unit
    }

    private companion object {
        const val PLAN_ID = "K7M2Q9"
        const val PAGE_ID = "K7M2Q9-01"
        val SCAN_DAY: LocalDate = LocalDate.of(2026, 11, 18)
        val PAGE_DATE: LocalDate = LocalDate.of(2026, 11, 16)
    }
}
