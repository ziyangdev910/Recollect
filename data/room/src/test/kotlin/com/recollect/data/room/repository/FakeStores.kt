package com.recollect.data.room.repository

import com.recollect.data.room.dao.ConfirmationDecisionLogDao
import com.recollect.data.room.dao.CoolingOverrideEventDao
import com.recollect.data.room.dao.KnowledgePointDao
import com.recollect.data.room.dao.NoteAttachmentItemDao
import com.recollect.data.room.dao.PendingFeedbackDao
import com.recollect.data.room.dao.PlanDao
import com.recollect.data.room.dao.PlanPageDao
import com.recollect.data.room.dao.QuestionDao
import com.recollect.data.room.dao.ReviewFeedbackDao
import com.recollect.data.room.dao.ScanSessionDao
import com.recollect.data.room.entity.ConfirmationDecisionLog
import com.recollect.data.room.entity.CoolingOverrideEvent
import com.recollect.data.room.entity.KnowledgePoint
import com.recollect.data.room.entity.NoteAttachmentItem
import com.recollect.data.room.entity.PendingFeedback
import com.recollect.data.room.entity.Plan
import com.recollect.data.room.entity.PlanPage
import com.recollect.data.room.entity.Question
import com.recollect.data.room.entity.ReviewFeedback
import com.recollect.data.room.entity.ScanSession
import com.recollect.data.room.entity.ScanSessionStatus
import com.recollect.data.room.entity.UserDecision
import java.time.Instant
import java.time.LocalDate

/**
 * 测试用内存存储 + fake DAO（C-052 方案 C：JVM 单元测试，不引入 Robolectric）。
 *
 * fake DAO 只模拟 data:room 实际使用的 DAO 方法；事务回滚由 [RollbackTransactionRunner] 快照恢复，
 * 用来验证 Repository 把所有写操作放进同一个事务（repository.md「任一步失败 → 整体回滚」）。
 */
internal class InMemoryStores {

    val plans = linkedMapOf<String, Plan>() // key = planId
    val planPages = linkedMapOf<String, PlanPage>() // key = pageId
    val questions = linkedMapOf<String, Question>() // key = id
    val knowledgePoints = linkedMapOf<String, KnowledgePoint>() // key = id
    val coolingEvents = linkedMapOf<Long, CoolingOverrideEvent>() // key = id
    val reviewFeedbacks = mutableListOf<ReviewFeedback>()
    val decisionLogs = mutableListOf<ConfirmationDecisionLog>()
    val pendingFeedbacks = mutableListOf<PendingFeedback>()
    val noteItems = mutableListOf<NoteAttachmentItem>()
    val scanSessions = linkedMapOf<String, ScanSession>() // key = sessionId
    val questionLinks = linkedMapOf<String, MutableSet<String>>() // questionId -> knowledgePointIds
    private var nextEventId = 1L
    private var nextReviewFeedbackId = 1L
    private var nextDecisionLogId = 1L
    private var nextNoteItemId = 1L

    var transactionCount = 0

    /** 模拟数据库异常：置为非空后下一次对应写操作抛错。 */
    var failOnKnowledgePointUpdate: Throwable? = null
    var failOnNoteInsert: Throwable? = null
    /** 非空时，第 [failOnPendingFeedbackInsertAfter] 行之后的插入抛错，用来验证整页回滚。 */
    var failOnPendingFeedbackInsert: Throwable? = null
    var failOnPendingFeedbackInsertAfter: Int = 0

    fun insertCoolingEvent(sessionId: String, questionId: String, createdAt: Instant): Long {
        val id = nextEventId++
        coolingEvents[id] = CoolingOverrideEvent(
            id = id,
            sessionId = sessionId,
            questionId = questionId,
            createdAt = createdAt,
            consumedAt = null,
        )
        return id
    }

    data class Snapshot(
        val plans: Map<String, Plan>,
        val planPages: Map<String, PlanPage>,
        val questions: Map<String, Question>,
        val knowledgePoints: Map<String, KnowledgePoint>,
        val coolingEvents: Map<Long, CoolingOverrideEvent>,
        val reviewFeedbacks: List<ReviewFeedback>,
        val decisionLogs: List<ConfirmationDecisionLog>,
        val pendingFeedbacks: List<PendingFeedback>,
        val noteItems: List<NoteAttachmentItem>,
        val scanSessions: Map<String, ScanSession>,
    )

    fun snapshot(): Snapshot = Snapshot(
        plans = plans.toMap(),
        planPages = planPages.toMap(),
        questions = questions.toMap(),
        knowledgePoints = knowledgePoints.toMap(),
        coolingEvents = coolingEvents.toMap(),
        reviewFeedbacks = reviewFeedbacks.toList(),
        decisionLogs = decisionLogs.toList(),
        pendingFeedbacks = pendingFeedbacks.toList(),
        noteItems = noteItems.toList(),
        scanSessions = scanSessions.toMap(),
    )

    fun restore(snapshot: Snapshot) {
        plans.clear()
        plans.putAll(snapshot.plans)
        planPages.clear()
        planPages.putAll(snapshot.planPages)
        questions.clear()
        questions.putAll(snapshot.questions)
        knowledgePoints.clear()
        knowledgePoints.putAll(snapshot.knowledgePoints)
        coolingEvents.clear()
        coolingEvents.putAll(snapshot.coolingEvents)
        reviewFeedbacks.clear()
        reviewFeedbacks.addAll(snapshot.reviewFeedbacks)
        decisionLogs.clear()
        decisionLogs.addAll(snapshot.decisionLogs)
        pendingFeedbacks.clear()
        pendingFeedbacks.addAll(snapshot.pendingFeedbacks)
        noteItems.clear()
        noteItems.addAll(snapshot.noteItems)
        scanSessions.clear()
        scanSessions.putAll(snapshot.scanSessions)
    }

    fun insertReviewFeedback(entity: ReviewFeedback): Long {
        val id = nextReviewFeedbackId++
        reviewFeedbacks += entity.copy(id = id)
        return id
    }

    fun insertDecisionLog(entity: ConfirmationDecisionLog): Long {
        val id = nextDecisionLogId++
        decisionLogs += entity.copy(id = id)
        return id
    }

    fun insertNoteItem(entity: NoteAttachmentItem): Long {
        failOnNoteInsert?.let { throw it }
        val id = nextNoteItemId++
        noteItems += entity.copy(id = id)
        return id
    }
}

internal class FakeReviewFeedbackDao(private val stores: InMemoryStores) : ReviewFeedbackDao {
    override suspend fun insert(entity: ReviewFeedback): Long = stores.insertReviewFeedback(entity)

    override suspend fun forKnowledgePoint(knowledgePointId: String): List<ReviewFeedback> =
        stores.reviewFeedbacks.filter { it.knowledgePointId == knowledgePointId }

    override suspend fun forPlan(planId: String): List<ReviewFeedback> =
        stores.reviewFeedbacks.filter { it.planId == planId }
}

internal class FakeConfirmationDecisionLogDao(private val stores: InMemoryStores) : ConfirmationDecisionLogDao {
    override suspend fun insert(entity: ConfirmationDecisionLog): Long = stores.insertDecisionLog(entity)

    override suspend fun forPlan(planId: String): List<ConfirmationDecisionLog> =
        stores.decisionLogs.filter { it.planId == planId }

    override suspend fun forSession(sessionId: String): List<ConfirmationDecisionLog> =
        stores.decisionLogs.filter { it.sessionId == sessionId }

    override suspend fun reviewedCount(planId: String): Int =
        stores.decisionLogs.count { it.planId == planId && it.decision != UserDecision.NOT_REVIEWED }

    override suspend fun scannedPageCount(planId: String): Int =
        stores.decisionLogs.filter { it.planId == planId }.map { it.pageId }.distinct().size
}

internal class FakePendingFeedbackDao(private val stores: InMemoryStores) : PendingFeedbackDao {
    override suspend fun insertAll(rows: List<PendingFeedback>) {
        rows.forEachIndexed { index, row ->
            stores.failOnPendingFeedbackInsert?.let { failure ->
                if (index >= stores.failOnPendingFeedbackInsertAfter) throw failure
            }
            stores.pendingFeedbacks.removeAll {
                it.sessionId == row.sessionId &&
                    it.questionId == row.questionId &&
                    it.knowledgePointId == row.knowledgePointId
            }
            stores.pendingFeedbacks += row
        }
    }

    override suspend fun forSession(sessionId: String): List<PendingFeedback> =
        stores.pendingFeedbacks.filter { it.sessionId == sessionId }

    override suspend fun updateActualReviewDate(
        sessionId: String,
        questionId: String,
        knowledgePointId: String,
        date: LocalDate?,
    ) {
        val index = stores.pendingFeedbacks.indexOfFirst {
            it.sessionId == sessionId && it.questionId == questionId && it.knowledgePointId == knowledgePointId
        }
        if (index >= 0) {
            stores.pendingFeedbacks[index] = stores.pendingFeedbacks[index].copy(actualReviewDate = date)
        }
    }

    override suspend fun deleteForSession(sessionId: String) {
        stores.pendingFeedbacks.removeAll { it.sessionId == sessionId }
    }

    override suspend fun deleteForSessionQuestion(sessionId: String, questionId: String) {
        stores.pendingFeedbacks.removeAll { it.sessionId == sessionId && it.questionId == questionId }
    }
}

internal class FakeNoteAttachmentItemDao(private val stores: InMemoryStores) : NoteAttachmentItemDao {
    override suspend fun insert(entity: NoteAttachmentItem): Long = stores.insertNoteItem(entity)

    override suspend fun nextOrder(questionId: String): Int =
        (stores.noteItems.filter { it.questionId == questionId }.maxOfOrNull { it.order } ?: -1) + 1

    override suspend fun latestForQuestion(questionId: String, limit: Int): List<NoteAttachmentItem> =
        stores.noteItems.filter { it.questionId == questionId }
            .sortedWith(compareByDescending<NoteAttachmentItem> { it.createdAt }.thenBy { it.order })
            .take(limit)
}

internal class RollbackTransactionRunner(private val stores: InMemoryStores) : TransactionRunner {
    override suspend fun <T> inTransaction(block: suspend () -> T): T {
        stores.transactionCount++
        val snapshot = stores.snapshot()
        return try {
            block()
        } catch (t: Throwable) {
            stores.restore(snapshot)
            throw t
        }
    }
}

internal class FakePlanDao(private val stores: InMemoryStores) : PlanDao {
    override suspend fun insert(plan: Plan) {
        stores.plans[plan.planId] = plan
    }

    override suspend fun byId(planId: String): Plan? = stores.plans[planId]

    override suspend fun activeForWeek(weekStart: LocalDate, weekEnd: LocalDate): Plan? =
        stores.plans.values
            .filter {
                it.weekStart == weekStart && it.weekEnd == weekEnd &&
                    it.status in setOf("GENERATED", "PRINTED")
            }
            .maxByOrNull { it.createdAt }

    override suspend fun supersede(planId: String, supersededBy: String) {
        stores.plans[planId]?.let {
            stores.plans[planId] = it.copy(status = "SUPERSEDED", supersededBy = supersededBy)
        }
    }

    override suspend fun markPrinted(planId: String, printedAt: Instant) {
        stores.plans[planId]?.let {
            stores.plans[planId] = it.copy(status = "PRINTED", printedAt = printedAt)
        }
    }
}

internal class FakePlanPageDao(private val stores: InMemoryStores) : PlanPageDao {
    override suspend fun insert(entity: PlanPage) {
        stores.planPages[entity.pageId] = entity
    }

    override suspend fun byPageId(pageId: String): PlanPage? = stores.planPages[pageId]

    override suspend fun forPlan(planId: String): List<PlanPage> =
        stores.planPages.values.filter { it.planId == planId }.sortedBy { it.pageNumber }

    override suspend fun countForPlan(planId: String): Int =
        stores.planPages.values.count { it.planId == planId }

    override suspend fun deleteForPlan(planId: String) {
        stores.planPages.values.removeAll { it.planId == planId }
    }
}

internal class FakeQuestionDao(private val stores: InMemoryStores) : QuestionDao {
    override suspend fun insert(entity: Question) {
        stores.questions[entity.id] = entity
    }

    override suspend fun update(entity: Question) {
        stores.questions[entity.id] = entity
    }

    override suspend fun active(): List<Question> =
        stores.questions.values.filterNot { it.archived }

    override suspend fun byId(id: String): Question? = stores.questions[id]

    override suspend fun latestSourcePageCode(): Int? =
        stores.questions.values.lastOrNull()?.sourcePageCode

    override suspend fun setPrintSnapshot(id: String, pageId: String, qrPayload: String) {
        stores.questions[id]?.let { stores.questions[id] = it.copy(pageId = pageId, qrPayload = qrPayload) }
    }

    override suspend fun markScheduled(id: String, targetDate: LocalDate) {
        stores.questions[id]?.let { stores.questions[id] = it.copy(lastScheduledDate = targetDate) }
    }

    override suspend fun markArchived(id: String, archivedAt: Instant) {
        stores.questions[id]?.let {
            stores.questions[id] = it.copy(archived = true, archivedAt = archivedAt)
        }
    }

    override suspend fun markUnarchived(id: String) {
        stores.questions[id]?.let { stores.questions[id] = it.copy(archived = false, archivedAt = null) }
    }
}

internal class FakeCoolingOverrideEventDao(private val stores: InMemoryStores) : CoolingOverrideEventDao {
    override suspend fun insert(entity: CoolingOverrideEvent): Long {
        val id = if (entity.id == 0L) (stores.coolingEvents.keys.maxOrNull() ?: 0L) + 1 else entity.id
        stores.coolingEvents[id] = entity.copy(id = id)
        return id
    }

    override suspend fun unconsumed(): List<CoolingOverrideEvent> =
        stores.coolingEvents.values.filter { it.consumedAt == null }

    override suspend fun consume(id: Long, consumedAt: Instant) {
        stores.coolingEvents[id]?.let { stores.coolingEvents[id] = it.copy(consumedAt = consumedAt) }
    }
}

internal class FakeScanSessionDao(private val stores: InMemoryStores) : ScanSessionDao {
    override suspend fun insert(entity: ScanSession) {
        stores.scanSessions[entity.sessionId] = entity
    }

    override suspend fun update(entity: ScanSession) {
        stores.scanSessions[entity.sessionId] = entity
    }

    override suspend fun byId(sessionId: String): ScanSession? = stores.scanSessions[sessionId]

    override suspend fun activeForPlan(planId: String): ScanSession? =
        stores.scanSessions.values
            .filter { it.planId == planId && it.status in setOf(ScanSessionStatus.SCANNING, ScanSessionStatus.READY) }
            .maxByOrNull { it.startedAt }

    override suspend fun latestActive(): ScanSession? =
        stores.scanSessions.values
            .filter { it.status in setOf(ScanSessionStatus.SCANNING, ScanSessionStatus.READY) }
            .maxByOrNull { it.startedAt }

    override suspend fun markSubmitted(sessionId: String, submittedAt: Instant) {
        stores.scanSessions[sessionId]?.let {
            stores.scanSessions[sessionId] = it.copy(status = ScanSessionStatus.SUBMITTED, submittedAt = submittedAt)
        }
    }

    override suspend fun updateStatus(sessionId: String, status: ScanSessionStatus) {
        stores.scanSessions[sessionId]?.let { stores.scanSessions[sessionId] = it.copy(status = status) }
    }

    override suspend fun abandonStale(threshold: Instant) {
        stores.scanSessions.replaceAll { _, session ->
            if (session.status != ScanSessionStatus.SUBMITTED && session.startedAt < threshold) {
                session.copy(status = ScanSessionStatus.ABANDONED)
            } else {
                session
            }
        }
    }
}

internal class FakeQuestionLookupRepository(private val stores: InMemoryStores) : QuestionLookupRepository {
    override suspend fun linkedKnowledgePointIds(questionId: String): Set<String>? =
        if (stores.questions[questionId] == null) null else stores.questionLinks[questionId].orEmpty().toSet()
}

internal class FakeKnowledgePointDao(private val stores: InMemoryStores) : KnowledgePointDao {
    override suspend fun insert(entity: KnowledgePoint) {
        stores.knowledgePoints[entity.id] = entity
    }

    override suspend fun update(entity: KnowledgePoint) {
        stores.failOnKnowledgePointUpdate?.let { throw it }
        stores.knowledgePoints[entity.id] = entity
    }

    override suspend fun allOrderedByNumber(): List<KnowledgePoint> =
        stores.knowledgePoints.values.sortedBy { it.number }

    override suspend fun byId(id: String): KnowledgePoint? = stores.knowledgePoints[id]

    override suspend fun maxNumber(): Int =
        stores.knowledgePoints.values.maxOfOrNull { it.number } ?: 0

    override suspend fun delete(id: String) {
        stores.knowledgePoints.remove(id)
    }

    override suspend fun printable(): List<KnowledgePoint> =
        stores.knowledgePoints.values
            .filter { !it.dormant && it.activeQuestionCount > 0 }
            .sortedBy { it.number }

    override suspend fun updateActivity(id: String, count: Int, dormant: Boolean) {
        stores.knowledgePoints[id]?.let {
            stores.knowledgePoints[id] = it.copy(activeQuestionCount = count, dormant = dormant)
        }
    }
}
