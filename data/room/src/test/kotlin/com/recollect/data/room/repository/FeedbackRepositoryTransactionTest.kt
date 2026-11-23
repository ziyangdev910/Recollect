package com.recollect.data.room.repository

import com.recollect.data.room.entity.DetectionStatus
import com.recollect.data.room.entity.KnowledgePoint
import com.recollect.data.room.entity.PendingFeedback
import com.recollect.data.room.entity.Question
import com.recollect.data.room.entity.UserDecision
import com.recollect.model.KnowledgeState
import com.recollect.model.Tier
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 阶段 4.2：`FeedbackRepository.saveDecisions` 的单事务语义（repository.md「事务规则」）。
 *
 * - 成功时：ReviewFeedback、ConfirmationDecisionLog、KnowledgePoint、NoteAttachmentItem 落库，
 *   PendingFeedback 删除；`continuousScore` 以 `Float` 精确落库（C-050）；
 * - 任一步失败：全部回滚，`PendingFeedback` 保留供重试；
 * - 关联 id 失效：整题跳过、不补默认知识点，并在返回值中留下可见记录（C-055 方案 C）。
 */
class FeedbackRepositoryTransactionTest {

    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = Instant.parse("2026-11-18T12:00:00Z")
    private val clock = Clock.fixed(now, zone)
    private val plannedDate = LocalDate.of(2026, 11, 16)
    private val reviewDate = LocalDate.of(2026, 11, 18)
    private val planId = "AB12CD"
    private val pageId = "AB12CD-01"
    private val sessionId = "s1"

    private val stores = InMemoryStores()
    private val repository = RoomFeedbackRepository(
        transactionRunner = RollbackTransactionRunner(stores),
        reviewFeedbackDao = FakeReviewFeedbackDao(stores),
        confirmationDecisionLogDao = FakeConfirmationDecisionLogDao(stores),
        knowledgePointDao = FakeKnowledgePointDao(stores),
        questionDao = FakeQuestionDao(stores),
        pendingFeedbackDao = FakePendingFeedbackDao(stores),
        noteAttachmentItemDao = FakeNoteAttachmentItemDao(stores),
        scanSessionDao = FakeScanSessionDao(stores),
        planPageDao = FakePlanPageDao(stores),
        planDao = FakePlanDao(stores),
        clock = clock,
    )

    private fun knowledgePoint(id: String, number: Int = 1) = KnowledgePoint(
        id = id,
        number = number,
        name = "一次函数",
        currentTier = Tier.UNKNOWN,
        continuousScore = 0.5f,
        effectiveFeedbackCount = 0.0f,
        nextDueDate = plannedDate,
        activeQuestionCount = 1,
        dormant = false,
        difficulty = null,
        halfLifeDays = null,
        createdAt = now,
    )

    private fun question(id: String) = Question(
        id = id,
        pageId = "",
        questionNumber = "1-01",
        qrPayload = "",
        archived = false,
        lastScheduledDate = plannedDate,
        lastReviewedDate = null,
        estimatedReviewMinutes = 15,
        createdAt = now,
        archivedAt = null,
    )

    private fun pending(questionId: String, knowledgePointId: String) = PendingFeedback(
        sessionId = sessionId,
        planId = planId,
        pageId = pageId,
        plannedDate = plannedDate,
        actualReviewDate = reviewDate,
        questionId = questionId,
        knowledgePointId = knowledgePointId,
        triggered = true,
        detectedTier = Tier.MASTERED,
        detectionStatus = DetectionStatus.OK,
        detectedRawOcr = null,
        userDecision = UserDecision.ACCEPT,
        selectedTier = null,
        correctedText = null,
    )

    private fun state(id: String, score: Float) = KnowledgeState(
        id = id,
        number = 1,
        currentTier = Tier.MASTERED,
        continuousScore = score,
        nextDueDate = reviewDate.plusDays(1),
        effectiveFeedbackCount = 1.0f,
        dormant = false,
        activeQuestionCount = 1,
        difficulty = null,
        halfLifeDays = null,
        createdAt = now,
    )

    private fun decision(
        questionId: String,
        knowledgePointId: String,
        userDecision: UserDecision = UserDecision.ACCEPT,
        detectedTier: Tier? = Tier.MASTERED,
    ) = ConfirmedDecision(
        planId = planId,
        pageId = pageId,
        plannedDate = plannedDate,
        actualReviewDate = reviewDate,
        questionId = questionId,
        knowledgePointId = knowledgePointId,
        triggered = true,
        decision = userDecision,
        detectedTier = detectedTier,
        selectedTier = null,
        detectedRawOcr = null,
        correctedText = "修正后的笔记",
    )

    @Test
    fun `save decisions commits feedback log state note and pending deletion and rolls back on failure`() =
        runBlocking {
            stores.knowledgePoints["k1"] = knowledgePoint("k1")
            stores.questions["q1"] = question("q1")
            stores.pendingFeedbacks += pending("q1", "k1")

            val result = repository.saveDecisions(
                sessionId = sessionId,
                decisions = listOf(decision("q1", "k1")),
                updatedKnowledgeStates = listOf(state("k1", 0.8f)),
                questionLastReviewedDates = mapOf("q1" to reviewDate),
                noteAttachments = listOf(NoteAttachmentDraft("q1", "k1", "修正后的笔记")),
            )

            assertEquals(1, result.writtenFeedbackCount)
            assertEquals(1, result.writtenDecisionLogCount)
            assertEquals(emptyList(), result.skippedQuestionIds)

            val log = stores.decisionLogs.single()
            assertEquals(UserDecision.ACCEPT, log.decision)
            assertEquals(reviewDate, log.reviewedOn)
            assertEquals(now, log.decidedAt)
            // C-089 方案 A：计划号/页号/计划日必须按决策落库。
            assertEquals(planId, log.planId)
            assertEquals(pageId, log.pageId)
            assertEquals(plannedDate, log.plannedDate)

            val feedback = stores.reviewFeedbacks.single()
            assertEquals(Tier.MASTERED, feedback.tier)
            assertEquals(1.0f, feedback.effectiveWeight)
            assertEquals(1.0f, feedback.weightApplied)
            assertEquals(reviewDate, feedback.reviewedOn)
            assertEquals(planId, feedback.planId)
            assertEquals(pageId, feedback.pageId)
            assertEquals(plannedDate, feedback.plannedDate)

            assertEquals(0.8f, stores.knowledgePoints.getValue("k1").continuousScore, "C-050：Float 精确落库")
            assertEquals(reviewDate, stores.questions.getValue("q1").lastReviewedDate)

            val note = stores.noteItems.single()
            assertEquals("修正后的笔记", note.text)
            assertEquals(feedback.id, note.sourceFeedbackId)
            assertEquals(0, note.order)
            assertTrue(stores.pendingFeedbacks.isEmpty(), "提交成功后删除该会话的 PendingFeedback")

            // 第二批：状态、复习日、日志已写入后，笔记附件写入失败 → 全部回滚，PendingFeedback 保留。
            stores.pendingFeedbacks += pending("q1", "k1")
            stores.failOnNoteInsert = IllegalStateException("模拟写库失败")
            assertFailsWith<IllegalStateException> {
                repository.saveDecisions(
                    sessionId = sessionId,
                    decisions = listOf(decision("q1", "k1")),
                    updatedKnowledgeStates = listOf(state("k1", 0.9f)),
                    questionLastReviewedDates = mapOf("q1" to reviewDate.plusDays(1)),
                    noteAttachments = listOf(NoteAttachmentDraft("q1", "k1", "第二条修正")),
                )
            }

            assertEquals(1, stores.decisionLogs.size, "失败批次的决定日志不得残留")
            assertEquals(1, stores.reviewFeedbacks.size, "失败批次的反馈不得残留")
            assertEquals(0.8f, stores.knowledgePoints.getValue("k1").continuousScore, "知识点状态回滚")
            assertEquals(reviewDate, stores.questions.getValue("q1").lastReviewedDate, "复习日回滚")
            assertEquals(1, stores.noteItems.size, "失败批次的笔记附件不得残留")
            assertEquals(1, stores.pendingFeedbacks.size, "失败后 PendingFeedback 保留供重试")
            assertEquals(2, stores.transactionCount)
        }

    @Test
    fun `unresolved knowledge id skips the whole question and leaves a visible record`() = runBlocking {
        stores.knowledgePoints["k2"] = knowledgePoint("k2", number = 2)
        stores.questions["q1"] = question("q1")
        stores.questions["q2"] = question("q2")
        stores.pendingFeedbacks += pending("q1", "k-missing")
        stores.pendingFeedbacks += pending("q2", "k2")

        val result = repository.saveDecisions(
            sessionId = sessionId,
            decisions = listOf(
                decision("q1", "k-missing"),
                decision("q2", "k2"),
            ),
            updatedKnowledgeStates = listOf(state("k2", 0.8f)),
            questionLastReviewedDates = mapOf("q1" to reviewDate, "q2" to reviewDate),
        )

        assertEquals(listOf("q1"), result.skippedQuestionIds, "C-055：整题跳过并留可见记录")
        assertEquals(listOf("k-missing"), result.skippedKnowledgePointIds)
        assertEquals(1, result.writtenFeedbackCount)
        assertEquals(2, result.writtenDecisionLogCount, "C-078 方案 A：跳过行也写确认日志")

        assertEquals(1, stores.decisionLogs.count { it.questionId == "q1" }, "跳过题目仍写决定日志")
        assertTrue(stores.reviewFeedbacks.none { it.questionId == "q1" }, "跳过题目不得写反馈")
        assertEquals(1, stores.knowledgePoints.size, "不得补默认知识点")
        assertEquals(null, stores.questions.getValue("q1").lastReviewedDate, "跳过题目不得写复习日")
        assertEquals(reviewDate, stores.questions.getValue("q2").lastReviewedDate)

        assertEquals(
            listOf("q1" to "k-missing"),
            stores.pendingFeedbacks.map { it.questionId to it.knowledgePointId },
            "跳过题目的待确认行保留供数据修复后重试",
        )
    }

    @Test
    fun `state contributed only by a skipped question is not written`() = runBlocking {
        stores.knowledgePoints["k2"] = knowledgePoint("k2", number = 2)
        // q2 不存在：即使调用方传入 k2 的新状态，也不得落库（C-077 方案 A）。
        val original = stores.knowledgePoints.getValue("k2")

        val result = repository.saveDecisions(
            sessionId = sessionId,
            decisions = listOf(decision("q2", "k2")),
            updatedKnowledgeStates = listOf(state("k2", 0.9f)),
            questionLastReviewedDates = mapOf("q2" to reviewDate),
        )

        assertEquals(listOf("q2"), result.skippedQuestionIds)
        assertEquals(original, stores.knowledgePoints.getValue("k2"), "跳过题目的状态不得落库")
        assertEquals(1, stores.decisionLogs.size, "跳过行仍写确认日志")
    }
}
