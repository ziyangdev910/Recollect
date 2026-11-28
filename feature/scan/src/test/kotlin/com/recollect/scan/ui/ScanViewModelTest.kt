package com.recollect.scan.ui

import com.recollect.data.room.entity.DetectionStatus
import com.recollect.data.room.entity.ScanSessionStatus
import com.recollect.data.room.entity.UserDecision
import com.recollect.data.room.repository.PendingFeedbackInput
import com.recollect.data.room.repository.PendingFeedbackRow
import com.recollect.data.room.repository.PlanPageFacts
import com.recollect.data.room.repository.ScanSessionRepository
import com.recollect.data.room.repository.ScanSessionSnapshot
import com.recollect.scan.ScanErrorCode
import com.recollect.scan.usecase.ScanManualOutcome
import com.recollect.scan.usecase.ScanPageOutcome
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * 扫描回读页状态（app.md「Scan」、scan-pipeline.md）：
 * 会话恢复、识别成功/失败、QR 失败开放手动回退、手动补录结果进入同一会话。
 */
class ScanViewModelTest {

    private val repository = FakeRepository()
    private var runnerResult: ScanRunResult = ScanRunResult.ImageUnreadable
    private var manualOutcome: ScanManualOutcome = ScanManualOutcome.Rejected("未调用")
    private var lastManualInput: Pair<String, String>? = null
    private val removedPreviews = mutableListOf<String>()

    private val viewModel = ScanViewModel(
        runner = ScanRunner { runnerResult },
        manualEntry = ScanManualEntry { pageId, questionNumber ->
            lastManualInput = pageId to questionNumber
            manualOutcome
        },
        repository = repository,
        clock = Clock.fixed(Instant.parse("2026-11-18T08:00:00Z"), ZoneOffset.UTC),
        deletePreview = { removedPreviews += it },
    )

    @Test
    fun `load restores the active session pages`() = runBlocking {
        repository.active = ScanSessionSnapshot("s-1", PLAN_ID, ScanSessionStatus.READY, false)
        repository.rows = listOf(
            row(PAGE_ID, PAGE_DATE),
            row(PAGE_ID, PAGE_DATE),
            row("$PLAN_ID-02", SCAN_DAY.plusDays(1)),
        )

        viewModel.load()

        val session = requireNotNull(viewModel.state.session)
        assertEquals("s-1", session.sessionId)
        assertEquals(2, session.pages.size)
        assertEquals(3, session.rowCount)
        assertTrue(session.pages.first { it.pageId == "$PLAN_ID-02" }.pageIsFuture)
    }

    @Test
    fun `load without an active session shows the empty hint`() = runBlocking {
        viewModel.load()

        assertNull(viewModel.state.session)
        assertFalse(viewModel.state.loading)
    }

    @Test
    fun `stored scan shows the result and refreshes the session`() = runBlocking {
        runnerResult = ScanRunResult.Completed(
            ScanPageOutcome.Stored(
                sessionId = "s-1",
                pageId = PAGE_ID,
                rowCount = 2,
                lateBatch = false,
                pageIsFuture = false,
                pageDate = PAGE_DATE,
                replacedPage = true,
            ),
        )
        repository.active = ScanSessionSnapshot("s-1", PLAN_ID, ScanSessionStatus.READY, false)
        repository.rows = listOf(row(PAGE_ID, PAGE_DATE), row(PAGE_ID, PAGE_DATE))

        viewModel.onImageSelected("content://photo.jpg")

        val state = viewModel.state
        assertFalse(state.busy)
        assertNull(state.error)
        assertTrue(state.message!!.contains("该页此前已扫描"))
        assertEquals(PAGE_ID, requireNotNull(state.session).pages.single().pageId)
    }

    @Test
    fun `a clean scan no longer shows the written rows message`() = runBlocking {
        runnerResult = ScanRunResult.Completed(
            ScanPageOutcome.Stored(
                sessionId = "s-1",
                pageId = PAGE_ID,
                rowCount = 2,
                lateBatch = false,
                pageIsFuture = false,
                pageDate = PAGE_DATE,
                replacedPage = false,
            ),
            previewLocation = "cache/scan-previews/p1.jpg",
        )
        repository.active = ScanSessionSnapshot("s-1", PLAN_ID, ScanSessionStatus.READY, false)
        repository.rows = listOf(row(PAGE_ID, PAGE_DATE), row(PAGE_ID, PAGE_DATE))

        viewModel.onImageSelected("content://photo.jpg")

        val state = viewModel.state
        assertNull(state.message)
        val page = requireNotNull(state.session).pages.single()
        assertEquals("cache/scan-previews/p1.jpg", page.previewLocation)
    }

    @Test
    fun `question count treats multiple knowledge rows of one question as one question`() = runBlocking {
        repository.active = ScanSessionSnapshot("s-1", PLAN_ID, ScanSessionStatus.READY, false)
        repository.rows = listOf(
            row(PAGE_ID, PAGE_DATE, questionId = "q1"),
            row(PAGE_ID, PAGE_DATE, questionId = "q1"),
            row(PAGE_ID, PAGE_DATE, questionId = "q2"),
        )

        viewModel.load()

        val session = requireNotNull(viewModel.state.session)
        assertEquals(2, session.pages.single().questionCount)
        assertEquals(2, session.questionCount)
    }

    @Test
    fun `the most recently scanned page becomes the last in the list`() = runBlocking {
        repository.active = ScanSessionSnapshot("s-1", PLAN_ID, ScanSessionStatus.READY, false)
        repository.rows = listOf(row(PAGE_ID, PAGE_DATE), row("$PLAN_ID-02", PAGE_DATE))
        viewModel.load()

        runnerResult = ScanRunResult.Completed(
            ScanPageOutcome.Stored(
                sessionId = "s-1",
                pageId = PAGE_ID,
                rowCount = 1,
                lateBatch = false,
                pageIsFuture = false,
                pageDate = PAGE_DATE,
                replacedPage = false,
            ),
            previewLocation = "cache/scan-previews/p1.jpg",
        )
        viewModel.onImageSelected("content://photo.jpg")

        // 重复扫描旧页后，该页移动到列表末尾（UI 中最右侧）。
        assertEquals(PAGE_ID, requireNotNull(viewModel.state.session).pages.last().pageId)
    }

    @Test
    fun `startNewScan discards the old session and deletes previews`() = runBlocking {
        repository.active = ScanSessionSnapshot("s-1", PLAN_ID, ScanSessionStatus.READY, false)
        repository.rows = listOf(row(PAGE_ID, PAGE_DATE))
        runnerResult = ScanRunResult.Completed(
            ScanPageOutcome.Stored(
                sessionId = "s-1",
                pageId = PAGE_ID,
                rowCount = 1,
                lateBatch = false,
                pageIsFuture = false,
                pageDate = PAGE_DATE,
            ),
            previewLocation = "cache/scan-previews/p1.jpg",
        )
        viewModel.onImageSelected("content://photo.jpg")

        viewModel.startNewScan()

        assertNull(viewModel.state.session)
        assertTrue(repository.discarded)
        assertEquals(listOf("cache/scan-previews/p1.jpg"), removedPreviews)
    }

    @Test
    fun `removePage drops that page from the session`() = runBlocking {
        repository.active = ScanSessionSnapshot("s-1", PLAN_ID, ScanSessionStatus.READY, false)
        repository.rows = listOf(row(PAGE_ID, PAGE_DATE), row("$PLAN_ID-02", PAGE_DATE))
        viewModel.load()

        viewModel.removePage(PAGE_ID)

        assertEquals(listOf(PAGE_ID), repository.removedPages)
        val pages = requireNotNull(viewModel.state.session).pages
        assertEquals(listOf("$PLAN_ID-02"), pages.map { it.pageId })
    }

    @Test
    fun `a qr failure offers manual input`() = runBlocking {
        runnerResult = ScanRunResult.Completed(ScanPageOutcome.Rejected(ScanErrorCode.QR_NOT_FOUND))

        viewModel.onImageSelected("content://photo.jpg")

        val error = requireNotNull(viewModel.state.error)
        assertEquals(ScanErrorCode.QR_NOT_FOUND, error.code)
        assertTrue(error.manualAllowed)
        assertTrue(error.message.contains("手动输入"))
    }

    @Test
    fun `a page failure does not offer manual input`() = runBlocking {
        runnerResult = ScanRunResult.Completed(ScanPageOutcome.Rejected(ScanErrorCode.PAGE_NOT_FOUND))

        viewModel.onImageSelected("content://photo.jpg")

        assertFalse(requireNotNull(viewModel.state.error).manualAllowed)
    }

    @Test
    fun `an ownership failure blocks without manual input`() = runBlocking {
        runnerResult = ScanRunResult.Completed(
            ScanPageOutcome.Rejected(ScanErrorCode.OWNERSHIP_MISMATCH),
        )

        viewModel.onImageSelected("content://photo.jpg")

        val error = requireNotNull(viewModel.state.error)
        assertFalse(error.manualAllowed)
        assertTrue(error.message.contains("重新打印"))
    }

    @Test
    fun `an unreadable image shows a retry message and does not write`() = runBlocking {
        runnerResult = ScanRunResult.ImageUnreadable

        viewModel.onImageSelected("content://missing.jpg")

        assertTrue(viewModel.state.messageIsError)
        assertNull(viewModel.state.error)
    }

    @Test
    fun `manual rejection stays in the dialog with an inline error`() = runBlocking {
        manualOutcome = ScanManualOutcome.Rejected("页面或题号不存在，请核对纸面题号。")
        viewModel.startManualInput()

        viewModel.submitManualInput()

        val manual = requireNotNull(viewModel.state.manual)
        assertEquals("页面或题号不存在，请核对纸面题号。", manual.error)
        assertFalse(manual.busy)
        assertEquals("" to "", lastManualInput)
    }

    @Test
    fun `manual success closes the dialog and opens the session`() = runBlocking {
        manualOutcome = ScanManualOutcome.Stored(
            sessionId = "s-1",
            pageId = PAGE_ID,
            questionId = "q1",
            rowCount = 2,
            lateBatch = false,
            pageIsFuture = false,
            pageDate = PAGE_DATE,
            replacedPage = false,
        )
        repository.active = ScanSessionSnapshot("s-1", PLAN_ID, ScanSessionStatus.READY, false)
        repository.rows = listOf(row(PAGE_ID, PAGE_DATE), row(PAGE_ID, PAGE_DATE))
        viewModel.startManualInput()
        viewModel.updateManualPageId("k7m2q9-01")
        viewModel.updateManualQuestionNumber("T-1")

        viewModel.submitManualInput()

        assertNull(viewModel.state.manual)
        assertEquals(PAGE_ID, requireNotNull(viewModel.state.session).pages.single().pageId)
        assertEquals("k7m2q9-01" to "T-1", lastManualInput)
        assertTrue(viewModel.state.message!!.contains("确认页逐行标记"))
    }

    @Test
    fun `dismiss error returns to the capture state`() = runBlocking {
        runnerResult = ScanRunResult.Completed(ScanPageOutcome.Rejected(ScanErrorCode.QR_INVALID))
        viewModel.onImageSelected("content://photo.jpg")

        viewModel.dismissError()

        assertNull(viewModel.state.error)
    }

    private fun row(pageId: String, date: LocalDate, questionId: String = "q1") = PendingFeedbackRow(
        planId = PLAN_ID,
        pageId = pageId,
        plannedDate = date,
        actualReviewDate = date,
        questionId = questionId,
        knowledgePointId = "kp-1",
        triggered = true,
        detectedTier = null,
        detectionStatus = DetectionStatus.UNREADABLE,
        conflict = false,
        detectedRawOcr = null,
        userDecision = UserDecision.UNMARKED,
    )

    private class FakeRepository : ScanSessionRepository {
        var active: ScanSessionSnapshot? = null
        var rows: List<PendingFeedbackRow> = emptyList()
        var discarded = false
        var removedPages: List<String> = emptyList()

        override suspend fun pageFacts(pageId: String): PlanPageFacts? = null

        override suspend fun startOrResumeSession(planId: String, now: Instant): ScanSessionSnapshot =
            active ?: ScanSessionSnapshot("s-1", planId, ScanSessionStatus.SCANNING, created = true)

        override suspend fun activeSession(): ScanSessionSnapshot? = active

        override suspend fun discardActiveSession() {
            discarded = true
            active = null
            rows = emptyList()
        }

        override suspend fun removePage(sessionId: String, pageId: String) {
            removedPages = removedPages + pageId
            rows = rows.filterNot { it.pageId == pageId }
        }

        override suspend fun appendPage(
            sessionId: String,
            pageId: String,
            rows: List<PendingFeedbackInput>,
        ): Int = rows.size

        override suspend fun pendingRows(sessionId: String): List<PendingFeedbackRow> = rows

        override suspend fun planEndDate(planId: String): LocalDate? = PAGE_DATE

        override suspend fun markReady(sessionId: String) = Unit

        override suspend fun abandonStaleSessions(now: Instant) = Unit
    }

    private companion object {
        const val PLAN_ID = "K7M2Q9"
        const val PAGE_ID = "K7M2Q9-01"
        val SCAN_DAY: LocalDate = LocalDate.of(2026, 11, 18)
        val PAGE_DATE: LocalDate = LocalDate.of(2026, 11, 16)
    }
}
