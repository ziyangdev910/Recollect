package com.recollect.data.room.repository

import com.recollect.data.room.dao.PendingFeedbackDao
import com.recollect.data.room.dao.PlanPageDao
import com.recollect.data.room.dao.ScanSessionDao
import com.recollect.data.room.entity.DetectionStatus
import com.recollect.data.room.entity.PendingFeedback
import com.recollect.data.room.entity.ScanSession
import com.recollect.data.room.entity.ScanSessionStatus
import com.recollect.data.room.entity.UserDecision
import com.recollect.model.KnowledgeRowsCodec
import com.recollect.model.PlanPageRows
import com.recollect.model.Tier
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * 回读会话的持久化与归属读取（scan-pipeline.md「扫描会话与批量回读」、R-04）。
 *
 * 本层只写 `ScanSession` 与 `PendingFeedback`（临时态），**不写** `ReviewFeedback`、不改知识点状态；
 * 逐行决策默认为 `UNMARKED`，由确认页更新（D-13、R-04）。
 */
interface ScanSessionRepository {

    /** 按页 id 读取打印快照，供归属校验使用；无快照返回 `null`（C-072）。 */
    suspend fun pageFacts(pageId: String): PlanPageFacts?

    /** 同一计划已有 SCANNING/READY 会话则复用，否则新建。 */
    suspend fun startOrResumeSession(planId: String, now: Instant): ScanSessionSnapshot

    /** 最近一次未提交会话；从确认页返回扫描页或进程重建时用于恢复已扫页（app.md）。 */
    suspend fun activeSession(): ScanSessionSnapshot?

    /**
     * 丢弃最近一次未提交会话：清空其待确认行并标记为 ABANDONED。
     *
     * 从首页重新进入扫描页时调用，使扫描状态回到「一页未扫」
     * （2026-11-28 用户要求；从确认页返回时保留原会话，不调用本方法）。
     */
    suspend fun discardActiveSession()

    /** 删除会话中某一页的待确认行（扫描页缩略图右上角的删除按钮）。 */
    suspend fun removePage(sessionId: String, pageId: String)

    /**
     * 向会话追加一页的识别行；同一 `(sessionId,questionId,knowledgePointId)` 重复扫描保留最新。
     * `plannedDate` 取 `PlanPage.date`，`actualReviewDate` 默认同值（C-038）。
     *
     * @throws IllegalArgumentException 页面不存在、不属于该会话的计划、题号/知识点不在打印快照内
     * @throws IllegalStateException 会话不存在或已提交/废弃
     */
    suspend fun appendPage(sessionId: String, pageId: String, rows: List<PendingFeedbackInput>): Int

    /** 会话内待确认行（只读投影，不暴露 Room 实体）。 */
    suspend fun pendingRows(sessionId: String): List<PendingFeedbackRow>

    /** 计划最后一天；用于判定「晚扫描」（`planEndDate + 7` 之后）。 */
    suspend fun planEndDate(planId: String): LocalDate?

    /** 本页已写入，会话进入 READY（可进入确认页）。 */
    suspend fun markReady(sessionId: String)

    /** App 启动清理：超过 24 小时且非 SUBMITTED 的会话标为 ABANDONED。 */
    suspend fun abandonStaleSessions(now: Instant)
}

/** `plan_page` 的回读投影；`questionIds` 与 `knowledgeRows` 均从冻结快照解码（C-071/C-072）。 */
data class PlanPageFacts(
    val planId: String,
    val pageId: String,
    val date: LocalDate,
    val questionIds: List<String>,
    val knowledgeRows: List<PlanPageRows>,
    /** 每题题块与笔记书写区几何（C-093 方案 A）；旧稿为空。 */
    val items: List<com.recollect.pagination.ScanItemGeometry>,
)

/** 一页中一行的识别结果（写入 `PendingFeedback` 前的输入）。 */
data class PendingFeedbackInput(
    val questionId: String,
    val knowledgePointId: String,
    val triggered: Boolean,
    val detectedTier: Tier?,
    val detectionStatus: DetectionStatus,
    val conflict: Boolean = false,
    val detectedRawOcr: String? = null,
)

data class ScanSessionSnapshot(
    val sessionId: String,
    val planId: String,
    val status: ScanSessionStatus,
    val created: Boolean,
)

data class PendingFeedbackRow(
    val planId: String,
    val pageId: String,
    val plannedDate: LocalDate,
    val actualReviewDate: LocalDate?,
    val questionId: String,
    val knowledgePointId: String,
    val triggered: Boolean,
    val detectedTier: Tier?,
    val detectionStatus: DetectionStatus,
    val conflict: Boolean,
    val detectedRawOcr: String?,
    val userDecision: UserDecision,
)

class RoomScanSessionRepository(
    private val transactionRunner: TransactionRunner,
    private val planPageDao: PlanPageDao,
    private val pendingFeedbackDao: PendingFeedbackDao,
    private val scanSessionDao: ScanSessionDao,
    private val sessionIdFactory: () -> String = { UUID.randomUUID().toString() },
) : ScanSessionRepository {

    override suspend fun pageFacts(pageId: String): PlanPageFacts? {
        val page = planPageDao.byPageId(pageId) ?: return null
        return page.toFacts()
    }

    override suspend fun startOrResumeSession(planId: String, now: Instant): ScanSessionSnapshot {
        scanSessionDao.activeForPlan(planId)?.let { active ->
            return ScanSessionSnapshot(active.sessionId, active.planId, active.status, created = false)
        }
        val session = ScanSession(
            sessionId = sessionIdFactory(),
            planId = planId,
            startedAt = now,
            status = ScanSessionStatus.SCANNING,
            submittedAt = null,
        )
        scanSessionDao.insert(session)
        return ScanSessionSnapshot(session.sessionId, session.planId, session.status, created = true)
    }

    override suspend fun activeSession(): ScanSessionSnapshot? =
        scanSessionDao.latestActive()?.let { active ->
            ScanSessionSnapshot(active.sessionId, active.planId, active.status, created = false)
        }

    override suspend fun discardActiveSession() {
        val active = scanSessionDao.latestActive() ?: return
        transactionRunner.inTransaction {
            pendingFeedbackDao.deleteForSession(active.sessionId)
            scanSessionDao.updateStatus(active.sessionId, ScanSessionStatus.ABANDONED)
        }
    }

    override suspend fun removePage(sessionId: String, pageId: String) {
        pendingFeedbackDao.deleteForSessionPage(sessionId, pageId)
    }

    override suspend fun appendPage(
        sessionId: String,
        pageId: String,
        rows: List<PendingFeedbackInput>,
    ): Int {
        val session = scanSessionDao.byId(sessionId)
            ?: throw IllegalStateException("扫描会话不存在：$sessionId")
        check(session.status == ScanSessionStatus.SCANNING || session.status == ScanSessionStatus.READY) {
            "会话已结束，拒绝继续追加：${session.status}"
        }

        val page = planPageDao.byPageId(pageId)
            ?: throw IllegalArgumentException("打印页不存在：$pageId")
        require(page.planId == session.planId) { "打印页不属于该会话的计划：$pageId" }

        val facts = page.toFacts()
        rows.forEach { row ->
            require(row.questionId in facts.questionIds) { "题号不在该页：${row.questionId}" }
            val knowledgeIds = facts.knowledgeRows
                .firstOrNull { it.questionId == row.questionId }
                ?.rows
                ?.map { it.knowledgePointId }
                .orEmpty()
            require(row.knowledgePointId in knowledgeIds) {
                "知识点不在该题打印快照内：${row.questionId}/${row.knowledgePointId}"
            }
        }

        transactionRunner.inTransaction {
            pendingFeedbackDao.insertAll(
                rows.map { row ->
                    PendingFeedback(
                        sessionId = sessionId,
                        planId = page.planId,
                        pageId = page.pageId,
                        plannedDate = page.date,
                        actualReviewDate = page.date,
                        questionId = row.questionId,
                        knowledgePointId = row.knowledgePointId,
                        triggered = row.triggered,
                        detectedTier = row.detectedTier,
                        detectionStatus = row.detectionStatus,
                        conflict = row.conflict,
                        detectedRawOcr = row.detectedRawOcr,
                        userDecision = UserDecision.UNMARKED,
                        selectedTier = null,
                        correctedText = null,
                    )
                },
            )
        }
        return rows.size
    }

    override suspend fun pendingRows(sessionId: String): List<PendingFeedbackRow> =
        pendingFeedbackDao.forSession(sessionId).map { it.toRow() }

    override suspend fun planEndDate(planId: String): LocalDate? =
        planPageDao.forPlan(planId).maxOfOrNull { it.date }

    override suspend fun markReady(sessionId: String) {
        scanSessionDao.updateStatus(sessionId, ScanSessionStatus.READY)
    }

    override suspend fun abandonStaleSessions(now: Instant) {
        scanSessionDao.abandonStale(now.minus(STALE_AFTER))
    }

    private fun com.recollect.data.room.entity.PlanPage.toFacts(): PlanPageFacts = PlanPageFacts(
        planId = planId,
        pageId = pageId,
        date = date,
        questionIds = PlanPageJson.decodeQuestionIds(questionIds),
        knowledgeRows = KnowledgeRowsCodec.decode(knowledgeRows),
        items = ItemGeometryJson.decode(itemGeometry),
    )

    private fun PendingFeedback.toRow(): PendingFeedbackRow = PendingFeedbackRow(
        planId = planId,
        pageId = pageId,
        plannedDate = plannedDate,
        actualReviewDate = actualReviewDate,
        questionId = questionId,
        knowledgePointId = knowledgePointId,
        triggered = triggered,
        detectedTier = detectedTier,
        detectionStatus = detectionStatus,
        conflict = conflict,
        detectedRawOcr = detectedRawOcr,
        userDecision = userDecision,
    )
}

private val STALE_AFTER: Duration = Duration.ofHours(24)
