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
import com.recollect.model.Tier
import com.recollect.pagination.NoteWritingArea
import com.recollect.pagination.ScanItemGeometry
import com.recollect.scan.OmrRow
import com.recollect.scan.QrResult
import com.recollect.scan.ScanErrorCode
import com.recollect.scan.omr.DetectionStatus as ScanStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/**
 * 编排层（scan-pipeline.md、R-04）：校验失败必须在写库前短路；成功时按快照行序写入。
 */
class ScanPageUseCaseTest {

    private val repository = FakeRepository()
    private val useCase = ScanPageUseCase(
        repository = repository,
        clock = Clock.fixed(Instant.parse("2026-11-18T08:00:00Z"), ZoneOffset.UTC),
    )

    @Test
    fun `stores rows in snapshot order with tiers, triggers and raw ocr`() {
        repository.facts = facts()

        val outcome = runBlocking {
            useCase.store(
                planId = PLAN_ID,
                qrs = listOf(qr("q1")),
                omrRows = listOf(
                    OmrRow("q1", 0, ScanStatus.OK, Tier.MASTERED),
                    OmrRow("q1", 1, ScanStatus.NO_MARK, null),
                ),
                ocr = mapOf("q1" to "手写笔记"),
            )
        }

        assertTrue(outcome is ScanPageOutcome.Stored)
        val rows = requireNotNull(repository.appended)
        assertEquals(listOf("kp-1", "kp-2"), rows.map { it.knowledgePointId })
        assertEquals(listOf(true, false), rows.map { it.triggered })
        assertEquals(listOf(Tier.MASTERED, null), rows.map { it.detectedTier })
        assertEquals(listOf(DetectionStatus.OK, DetectionStatus.NO_MARK), rows.map { it.detectionStatus })
        assertTrue(rows.all { it.detectedRawOcr == "手写笔记" })
        assertTrue(repository.readyMarked, "写完应把会话标为 READY")
        assertEquals(false, (outcome as ScanPageOutcome.Stored).lateBatch)
        assertEquals(false, outcome.pageIsFuture)
    }

    @Test
    fun `flags late batch after plan end plus seven days`() {
        repository.facts = facts()
        repository.planEnd = PAGE_DATE.minusDays(8) // 扫描日 2026-11-18 晚于 end+7

        val outcome = runBlocking {
            useCase.store(PLAN_ID, listOf(qr("q1")), omrRows(), emptyMap())
        }

        assertEquals(true, (outcome as ScanPageOutcome.Stored).lateBatch)
    }

    @Test
    fun `flags a page planned after the scan day`() {
        repository.facts = facts(date = PAGE_DATE.plusDays(5)) // 2026-11-21，晚于扫描日 11-18
        repository.planEnd = PAGE_DATE.plusDays(6)

        val outcome = runBlocking {
            useCase.store(PLAN_ID, listOf(qr("q1")), omrRows(), emptyMap())
        }

        assertEquals(true, (outcome as ScanPageOutcome.Stored).pageIsFuture)
    }

    @Test
    fun `rejects an invalid checksum without writing`() {
        repository.facts = facts()

        val outcome = runBlocking {
            useCase.store(PLAN_ID, listOf(qr("q1", checksumOk = false)), omrRows(), emptyMap())
        }

        assertEquals(ScanPageOutcome.Rejected(ScanErrorCode.QR_INVALID), outcome)
        assertNull(repository.appended)
        assertEquals(0, repository.sessionsStarted)
    }

    @Test
    fun `rejects a question outside the printed page without writing`() {
        repository.facts = facts()

        val outcome = runBlocking {
            useCase.store(PLAN_ID, listOf(qr("q9")), omrRows(), emptyMap())
        }

        assertEquals(ScanPageOutcome.Rejected(ScanErrorCode.OWNERSHIP_MISMATCH), outcome)
        assertNull(repository.appended)
        assertEquals(0, repository.sessionsStarted)
    }

    @Test
    fun `rejects when omr row count does not match the snapshot`() {
        repository.facts = facts()

        val outcome = runBlocking {
            useCase.store(
                PLAN_ID,
                listOf(qr("q1")),
                listOf(OmrRow("q1", 0, ScanStatus.OK, Tier.MASTERED)),
                emptyMap(),
            )
        }

        assertEquals(ScanPageOutcome.Rejected(ScanErrorCode.OWNERSHIP_MISMATCH), outcome)
        assertNull(repository.appended)
    }

    @Test
    fun `rejects duplicate qr for the same question`() {
        repository.facts = facts()

        val outcome = runBlocking {
            useCase.store(PLAN_ID, listOf(qr("q1"), qr("q1")), omrRows(), emptyMap())
        }

        assertEquals(ScanPageOutcome.Rejected(ScanErrorCode.QR_MULTIPLE), outcome)
        assertNull(repository.appended)
    }

    @Test
    fun `rejects when a block has no qr`() {
        repository.facts = facts(questionIds = listOf("q1", "q2"))

        val outcome = runBlocking {
            useCase.store(
                PLAN_ID,
                listOf(qr("q1")),
                omrRows(),
                emptyMap(),
            )
        }

        assertEquals(ScanPageOutcome.Rejected(ScanErrorCode.QR_MULTIPLE), outcome)
        assertNull(repository.appended)
    }

    @Test
    fun `rejects a qr that belongs to another page`() {
        repository.facts = facts()

        val outcome = runBlocking {
            useCase.store(PLAN_ID, listOf(qr("q1", pageId = "K7M2Q9-02")), omrRows(), emptyMap())
        }

        assertEquals(ScanPageOutcome.Rejected(ScanErrorCode.OWNERSHIP_MISMATCH), outcome)
        assertNull(repository.appended)
    }

    private fun facts(
        questionIds: List<String> = listOf("q1"),
        date: java.time.LocalDate = PAGE_DATE,
    ) = PlanPageFacts(
        planId = PLAN_ID,
        pageId = PAGE_ID,
        date = date,
        questionIds = questionIds,
        knowledgeRows = listOf(
            PlanPageRows(
                questionId = "q1",
                rows = listOf(KnowledgeRow("kp-1", 1), KnowledgeRow("kp-2", 2)),
                triggeredKnowledgePointIds = listOf("kp-1"),
            ),
        ),
        items = questionIds.map { id ->
            ScanItemGeometry(
                questionId = id,
                topMm = 41.0,
                heightMm = 30.0,
                rowCount = if (id == "q1") 2 else 1,
                noteArea = NoteWritingArea(92.2, 68.0, 88.8, 25.0),
            )
        },
    )

    private fun omrRows() = listOf(
        OmrRow("q1", 0, ScanStatus.OK, Tier.MASTERED),
        OmrRow("q1", 1, ScanStatus.NO_MARK, null),
    )

    private fun qr(questionId: String, checksumOk: Boolean = true, pageId: String = PAGE_ID) = QrResult(
        raw = "raw",
        planId = if (checksumOk) PLAN_ID else null,
        pageId = if (checksumOk) pageId else null,
        questionId = if (checksumOk) questionId else null,
        centerX = 500f,
        // 60mm * 11.811 ≈ 708.7px，落在题块 41–71mm 内。
        centerY = 708.7f,
        checksumOk = checksumOk,
    )

    private fun <T> runBlocking(block: suspend () -> T): T {
        var result: Result<T>? = null
        val latch = CountDownLatch(1)
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(value: Result<T>) {
                result = value
                latch.countDown()
            }
        })
        check(latch.await(5, TimeUnit.SECONDS)) { "用例超时" }
        return result!!.getOrThrow()
    }

    private class FakeRepository : ScanSessionRepository {
        var facts: PlanPageFacts? = null
        var appended: List<PendingFeedbackInput>? = null
        var sessionsStarted = 0
        var readyMarked = false
        var planEnd: LocalDate? = PAGE_DATE.plusDays(6)

        override suspend fun pageFacts(pageId: String): PlanPageFacts? =
            facts?.takeIf { it.pageId == pageId }

        override suspend fun startOrResumeSession(planId: String, now: Instant): ScanSessionSnapshot {
            sessionsStarted++
            return ScanSessionSnapshot("session-1", planId, ScanSessionStatus.SCANNING, created = true)
        }

        override suspend fun activeSession(): ScanSessionSnapshot? = null

        override suspend fun discardActiveSession() = Unit

        override suspend fun removePage(sessionId: String, pageId: String) = Unit

        override suspend fun appendPage(
            sessionId: String,
            pageId: String,
            rows: List<PendingFeedbackInput>,
        ): Int {
            appended = rows
            return rows.size
        }

        override suspend fun pendingRows(sessionId: String): List<PendingFeedbackRow> = emptyList()

        override suspend fun planEndDate(planId: String): LocalDate? = planEnd

        override suspend fun markReady(sessionId: String) {
            readyMarked = true
        }

        override suspend fun abandonStaleSessions(now: Instant) = Unit
    }

    private companion object {
        const val PLAN_ID = "K7M2Q9"
        const val PAGE_ID = "K7M2Q9-01"
        val PAGE_DATE: LocalDate = LocalDate.of(2026, 11, 16)
    }
}
