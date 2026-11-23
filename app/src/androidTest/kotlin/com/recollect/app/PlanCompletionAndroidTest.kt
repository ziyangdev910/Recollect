package com.recollect.app

import android.test.AndroidTestCase
import com.recollect.data.room.RecollectDatabase
import com.recollect.data.room.entity.KnowledgePoint
import com.recollect.data.room.entity.Plan
import com.recollect.data.room.entity.PlanPage
import com.recollect.data.room.entity.Question
import com.recollect.data.room.entity.ScanSession
import com.recollect.data.room.entity.ScanSessionStatus
import com.recollect.data.room.entity.UserDecision
import com.recollect.data.room.repository.ConfirmedDecision
import com.recollect.data.room.repository.RoomFeedbackRepository
import com.recollect.data.room.repository.RoomTransactionRunner
import com.recollect.model.Tier
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking

/**
 * C-099：在真实 Room 数据库上验证计划完成判定。
 *
 * 关掉所有页面并确认提交后，`plan.status` 从 PRINTED 迁移为 COMPLETED；
 * 只确认其中一页、或计划已被显式替代时保持原状态。
 *
 * 运行：
 * `adb shell am instrument -w -e class com.recollect.app.PlanCompletionAndroidTest \
 *   org.recollect.test/androidx.test.runner.AndroidJUnitRunner`
 */
class PlanCompletionAndroidTest : AndroidTestCase() {

    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = Instant.parse("2026-11-23T12:00:00Z")
    private val clock: Clock = Clock.fixed(now, zone)
    private val plannedDate = LocalDate.of(2026, 11, 23)
    private val planId = "AB12CD"

    fun testPlanBecomesCompletedOnlyAfterEveryPageIsConfirmed() = runBlocking {
        context.deleteDatabase(RecollectDatabase.DATABASE_NAME)
        val database = RecollectDatabase.build(context)
        try {
            val repository = RoomFeedbackRepository(
                transactionRunner = RoomTransactionRunner(database),
                reviewFeedbackDao = database.reviewFeedbackDao(),
                confirmationDecisionLogDao = database.confirmationDecisionLogDao(),
                knowledgePointDao = database.knowledgePointDao(),
                questionDao = database.questionDao(),
                pendingFeedbackDao = database.pendingFeedbackDao(),
                noteAttachmentItemDao = database.noteAttachmentItemDao(),
                scanSessionDao = database.scanSessionDao(),
                planPageDao = database.planPageDao(),
                planDao = database.planDao(),
                clock = clock,
            )
            seed(database, status = "PRINTED")

            repository.saveDecisions(
                sessionId = "s1",
                decisions = listOf(decision("q1", "p1")),
                updatedKnowledgeStates = emptyList(),
                questionLastReviewedDates = emptyMap(),
            )
            assertEquals(
                "只确认其中一页时计划不能算完成",
                "PRINTED",
                database.planDao().byId(planId)?.status,
            )

            database.scanSessionDao().insert(
                ScanSession("s2", planId, now, ScanSessionStatus.READY, null),
            )
            repository.saveDecisions(
                sessionId = "s2",
                decisions = listOf(decision("q2", "p2")),
                updatedKnowledgeStates = emptyList(),
                questionLastReviewedDates = emptyMap(),
            )
            assertEquals(
                "全部页面确认后计划应标记为已完成",
                "COMPLETED",
                database.planDao().byId(planId)?.status,
            )
        } finally {
            database.close()
            context.deleteDatabase(RecollectDatabase.DATABASE_NAME)
        }
    }

    fun testSupersededPlanIsNotMarkedCompleted() = runBlocking {
        context.deleteDatabase(RecollectDatabase.DATABASE_NAME)
        val database = RecollectDatabase.build(context)
        try {
            val repository = RoomFeedbackRepository(
                transactionRunner = RoomTransactionRunner(database),
                reviewFeedbackDao = database.reviewFeedbackDao(),
                confirmationDecisionLogDao = database.confirmationDecisionLogDao(),
                knowledgePointDao = database.knowledgePointDao(),
                questionDao = database.questionDao(),
                pendingFeedbackDao = database.pendingFeedbackDao(),
                noteAttachmentItemDao = database.noteAttachmentItemDao(),
                scanSessionDao = database.scanSessionDao(),
                planPageDao = database.planPageDao(),
                planDao = database.planDao(),
                clock = clock,
            )
            seed(database, status = "SUPERSEDED", pageCount = 1)

            repository.saveDecisions(
                sessionId = "s1",
                decisions = listOf(decision("q1", "p1")),
                updatedKnowledgeStates = emptyList(),
                questionLastReviewedDates = emptyMap(),
            )
            assertEquals(
                "已被替代的计划不得改回 COMPLETED",
                "SUPERSEDED",
                database.planDao().byId(planId)?.status,
            )
        } finally {
            database.close()
            context.deleteDatabase(RecollectDatabase.DATABASE_NAME)
        }
    }

    private suspend fun seed(database: RecollectDatabase, status: String, pageCount: Int = 2) {
        database.knowledgePointDao().insert(
            KnowledgePoint(
                id = "k1",
                number = 1,
                name = "一次函数",
                currentTier = Tier.UNKNOWN,
                continuousScore = 0.5f,
                effectiveFeedbackCount = 0f,
                nextDueDate = plannedDate,
                activeQuestionCount = 2,
                dormant = false,
                difficulty = null,
                halfLifeDays = null,
                createdAt = now,
            ),
        )
        listOf("q1", "q2").forEachIndexed { index, questionId ->
            database.questionDao().insert(
                Question(
                    id = questionId,
                    pageId = "",
                    questionNumber = "${index + 1}",
                    qrPayload = "",
                    archived = false,
                    lastScheduledDate = plannedDate,
                    lastReviewedDate = null,
                    createdAt = now,
                    archivedAt = null,
                ),
            )
        }
        database.planDao().insert(
            Plan(
                planId = planId,
                weekStart = plannedDate,
                weekEnd = plannedDate.plusDays(6),
                createdAt = now,
                status = status,
                printedAt = now,
                supersededBy = null,
            ),
        )
        repeat(pageCount) { index ->
            val pageNumber = index + 1
            database.planPageDao().insert(
                PlanPage(
                    planId = planId,
                    pageId = "p$pageNumber",
                    pageNumber = pageNumber,
                    date = plannedDate.plusDays(index.toLong()),
                    itemCount = 1,
                    questionIds = "[]",
                    knowledgeRows = "[]",
                    continuationMarkers = "[]",
                ),
            )
        }
        database.scanSessionDao().insert(
            ScanSession("s1", planId, now, ScanSessionStatus.READY, null),
        )
    }

    private fun decision(questionId: String, pageId: String) = ConfirmedDecision(
        planId = planId,
        pageId = pageId,
        plannedDate = plannedDate,
        actualReviewDate = plannedDate,
        questionId = questionId,
        knowledgePointId = "k1",
        triggered = true,
        decision = UserDecision.ACCEPT,
        detectedTier = Tier.MASTERED,
        selectedTier = null,
        detectedRawOcr = null,
        correctedText = null,
    )
}
