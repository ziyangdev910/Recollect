package com.recollect.data.room.repository

import com.recollect.data.room.entity.DetectionStatus
import com.recollect.data.room.entity.PlanPage
import com.recollect.data.room.entity.ScanSessionStatus
import com.recollect.data.room.entity.UserDecision
import com.recollect.model.KnowledgeRow
import com.recollect.model.KnowledgeRowsCodec
import com.recollect.model.PlanPageRows
import com.recollect.model.Tier
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

/**
 * 扫描会话持久化（scan-pipeline.md「扫描会话与批量回读」、R-04）：
 * 只写 `ScanSession`/`PendingFeedback`，逐行决策默认 `UNMARKED`；多页追加去重；整页写入同一事务。
 */
class ScanSessionRepositoryTest {

    private val stores = InMemoryStores()
    private val repository = RoomScanSessionRepository(
        transactionRunner = RollbackTransactionRunner(stores),
        planPageDao = FakePlanPageDao(stores),
        pendingFeedbackDao = FakePendingFeedbackDao(stores),
        scanSessionDao = FakeScanSessionDao(stores),
        sessionIdFactory = { SESSION_ID },
    )

    @Test
    fun `starts a session and reuses the active one for the same plan`() {
        val first = runTest { repository.startOrResumeSession(PLAN_ID, NOW) }
        val second = runTest { repository.startOrResumeSession(PLAN_ID, NOW.plusSeconds(60)) }

        assertTrue(first.created)
        assertEquals(SESSION_ID, first.sessionId)
        assertEquals(false, second.created)
        assertEquals(ScanSessionStatus.SCANNING, second.status)
        assertEquals(1, stores.scanSessions.size)
    }

    @Test
    fun `reads the frozen page snapshot and rejects unknown pages`() {
        stores.planPages[PAGE_ID] = page()

        val facts = runTest { repository.pageFacts(PAGE_ID) }

        assertEquals(PLAN_ID, facts?.planId)
        assertEquals(listOf("q1"), facts?.questionIds)
        assertEquals(listOf("kp-1", "kp-2"), facts?.knowledgeRows?.single()?.rows?.map { it.knowledgePointId })
        assertNull(runTest { repository.pageFacts("K7M2Q9-99") })
    }

    @Test
    fun `appends a page as unmarked pending feedback with the planned date`() {
        seedSession()
        stores.planPages[PAGE_ID] = page()

        val written = runTest {
            repository.appendPage(
                sessionId = SESSION_ID,
                pageId = PAGE_ID,
                rows = listOf(
                    input("q1", "kp-1", Tier.MASTERED, triggered = true),
                    input("q1", "kp-2", Tier.FAMILIAR, triggered = false),
                ),
            )
        }

        assertEquals(2, written)
        val rows = stores.pendingFeedbacks
        assertEquals(2, rows.size)
        assertTrue(rows.all { it.planId == PLAN_ID && it.pageId == PAGE_ID })
        assertTrue(rows.all { it.plannedDate == PAGE_DATE && it.actualReviewDate == PAGE_DATE })
        assertTrue(rows.all { it.userDecision == UserDecision.UNMARKED })
        assertEquals(listOf(true, false), rows.map { it.triggered })
    }

    @Test
    fun `keeps only the latest result when the same page is scanned twice`() {
        seedSession()
        stores.planPages[PAGE_ID] = page()

        runTest {
            repository.appendPage(SESSION_ID, PAGE_ID, listOf(input("q1", "kp-1", Tier.MASTERED)))
            repository.appendPage(SESSION_ID, PAGE_ID, listOf(input("q1", "kp-1", Tier.UNKNOWN)))
        }

        assertEquals(1, stores.pendingFeedbacks.size)
        assertEquals(Tier.UNKNOWN, stores.pendingFeedbacks.single().detectedTier)
    }

    @Test
    fun `rejects rows outside the printed snapshot`() {
        seedSession()
        stores.planPages[PAGE_ID] = page()

        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.appendPage(SESSION_ID, PAGE_ID, listOf(input("q9", "kp-1", Tier.MASTERED))) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.appendPage(SESSION_ID, PAGE_ID, listOf(input("q1", "kp-9", Tier.MASTERED))) }
        }
        assertEquals(0, stores.pendingFeedbacks.size)
    }

    @Test
    fun `rolls back the whole page when a row insert fails`() {
        seedSession()
        stores.planPages[PAGE_ID] = page()
        stores.failOnPendingFeedbackInsert = IllegalStateException("db down")
        stores.failOnPendingFeedbackInsertAfter = 1

        assertThrows(IllegalStateException::class.java) {
            runTest {
                repository.appendPage(
                    SESSION_ID,
                    PAGE_ID,
                    listOf(
                        input("q1", "kp-1", Tier.MASTERED),
                        input("q1", "kp-2", Tier.FAMILIAR),
                    ),
                )
            }
        }
        assertEquals(0, stores.pendingFeedbacks.size)
        assertEquals(1, stores.transactionCount)
    }

    @Test
    fun `decodes the persisted item geometry snapshot`() {
        val geometry = listOf(
            com.recollect.pagination.ScanItemGeometry(
                questionId = "q1",
                topMm = 41.0,
                heightMm = 30.0,
                rowCount = 2,
                noteArea = com.recollect.pagination.NoteWritingArea(92.2, 68.0, 88.8, 25.0),
            ),
        )
        stores.planPages[PAGE_ID] = page().copy(itemGeometry = ItemGeometryJson.encode(geometry))

        val facts = runTest { repository.pageFacts(PAGE_ID) }

        assertEquals(geometry, facts?.items)
    }

    @Test
    fun `reports an empty geometry for pages printed before the snapshot existed`() {
        stores.planPages[PAGE_ID] = page()

        assertEquals(emptyList<com.recollect.pagination.ScanItemGeometry>(), runTest { repository.pageFacts(PAGE_ID) }?.items)
    }

    @Test
    fun `reports the last planned date of the plan`() {
        stores.planPages[PAGE_ID] = page()
        stores.planPages["K7M2Q9-02"] = page().copy(
            pageId = "K7M2Q9-02",
            pageNumber = 2,
            date = PAGE_DATE.plusDays(2),
        )

        assertEquals(PAGE_DATE.plusDays(2), runTest { repository.planEndDate(PLAN_ID) })
        assertNull(runTest { repository.planEndDate("ABCDEF") })
    }

    @Test
    fun `marks the session ready after a page is appended`() {
        seedSession()
        stores.planPages[PAGE_ID] = page()

        runTest {
            repository.appendPage(SESSION_ID, PAGE_ID, listOf(input("q1", "kp-1", Tier.MASTERED)))
            repository.markReady(SESSION_ID)
        }

        assertEquals(
            ScanSessionStatus.READY,
            stores.scanSessions.getValue(SESSION_ID).status,
        )
    }

    @Test
    fun `abandons stale sessions but keeps fresh and submitted ones`() {
        seedSession()
        stores.scanSessions["old-submitted"] = com.recollect.data.room.entity.ScanSession(
            sessionId = "old-submitted",
            planId = PLAN_ID,
            startedAt = NOW.minusSeconds(48 * 3600),
            status = ScanSessionStatus.SUBMITTED,
            submittedAt = NOW.minusSeconds(47 * 3600),
        )
        stores.scanSessions["old-scanning"] = com.recollect.data.room.entity.ScanSession(
            sessionId = "old-scanning",
            planId = "ABCDEF",
            startedAt = NOW.minusSeconds(48 * 3600),
            status = ScanSessionStatus.SCANNING,
            submittedAt = null,
        )

        runTest { repository.abandonStaleSessions(NOW) }

        assertEquals(ScanSessionStatus.ABANDONED, stores.scanSessions.getValue("old-scanning").status)
        assertEquals(ScanSessionStatus.SUBMITTED, stores.scanSessions.getValue("old-submitted").status)
        assertEquals(ScanSessionStatus.SCANNING, stores.scanSessions.getValue(SESSION_ID).status)
    }

    private fun <T> runTest(block: suspend () -> T): T = runBlocking { block() }

    private fun seedSession() {
        stores.scanSessions[SESSION_ID] = com.recollect.data.room.entity.ScanSession(
            sessionId = SESSION_ID,
            planId = PLAN_ID,
            startedAt = NOW,
            status = ScanSessionStatus.SCANNING,
            submittedAt = null,
        )
    }

    private fun page() = PlanPage(
        planId = PLAN_ID,
        pageId = PAGE_ID,
        pageNumber = 1,
        date = PAGE_DATE,
        itemCount = 1,
        questionIds = PlanPageJson.encodeQuestionIds(listOf("q1")),
        knowledgeRows = KnowledgeRowsCodec.encode(
            listOf(
                PlanPageRows(
                    questionId = "q1",
                    rows = listOf(KnowledgeRow("kp-1", 1), KnowledgeRow("kp-2", 2)),
                    triggeredKnowledgePointIds = listOf("kp-1"),
                ),
            ),
        ),
        continuationMarkers = "[]",
    )

    private fun input(
        questionId: String,
        knowledgePointId: String,
        tier: Tier,
        triggered: Boolean = true,
    ) = PendingFeedbackInput(
        questionId = questionId,
        knowledgePointId = knowledgePointId,
        triggered = triggered,
        detectedTier = tier,
        detectionStatus = DetectionStatus.OK,
    )

    private companion object {
        const val PLAN_ID = "K7M2Q9"
        const val PAGE_ID = "K7M2Q9-01"
        const val SESSION_ID = "session-1"
        val PAGE_DATE: LocalDate = LocalDate.of(2026, 11, 16)
        val NOW: Instant = Instant.parse("2026-11-18T08:00:00Z")
    }
}
