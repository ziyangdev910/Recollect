package com.recollect.data.room.repository

import com.recollect.data.room.dao.CoolingOverrideEventDao
import com.recollect.data.room.dao.KnowledgePointDao
import com.recollect.data.room.dao.PlanDao
import com.recollect.data.room.dao.PlanPageDao
import com.recollect.data.room.dao.QuestionDao
import com.recollect.data.room.entity.Plan
import com.recollect.data.room.entity.PlanPage
import com.recollect.model.KnowledgeRowsCodec
import com.recollect.model.PlanPageRows
import com.recollect.pagination.PlannedPage
import com.recollect.pagination.ScanItemGeometry
import com.recollect.scheduler.DeferReason
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/**
 * 计划持久化（repository.md）。本轮落地事务方法 [savePlan] 与计划生成所需的未消费冷却覆盖读取。
 */
interface PlanRepository {
    /** 未消费的一次性冷却覆盖；计划层只把 `questionId` 交给调度器（C-008 方案二）。 */
    suspend fun pendingCoolingOverrides(): List<PendingCoolingOverride>

    suspend fun savePlan(draft: PlanDraft)

    /** 本周仍可复用的计划（GENERATED/PRINTED）；显式重新生成后旧计划不再返回。 */
    suspend fun activePlan(weekStart: LocalDate, weekEnd: LocalDate): StoredPlan?

    suspend fun findPlan(planId: String): StoredPlan?

    /** 计划页面的可打印快照；按页码升序。 */
    suspend fun storedPages(planId: String): List<StoredPlanPage>

    /**
     * 用户确认“已打印”后原子写入每题快照并把计划标记为 PRINTED。
     *
     * 计划已被显式替代（SUPERSEDED）或不存在时返回 `false`，调用方不得提示“已记录”。
     */
    suspend fun savePrintSnapshots(
        planId: String,
        snapshots: List<PrintSnapshotRecord>,
        printedAt: Instant,
    ): Boolean
}

enum class PlanStatus {
    GENERATED,
    PRINTED,
    COMPLETED,
    SUPERSEDED,
}

data class StoredPlan(
    val planId: String,
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val status: PlanStatus,
    val printedAt: Instant?,
    /** 计划生成时间；用于判断题库在生成后是否被修改（题库变更提示）。 */
    val createdAt: Instant,
    val renderMeta: List<PlanRenderMeta>,
    /** 生成时被推迟的题目；旧计划为空列表。 */
    val deferredRecords: List<PlanDeferredRecord> = emptyList(),
)

data class StoredPlanPage(
    val pageId: String,
    val pageNumber: Int,
    val date: LocalDate,
    val itemCount: Int,
    val questionIds: List<String>,
    val geometry: List<ScanItemGeometry>,
)

data class PrintSnapshotRecord(
    val questionId: String,
    val pageId: String,
    val qrPayload: String,
)

/** 冷却覆盖的只读投影；不向上暴露 Room 实体（repository.md「边界」）。 */
data class PendingCoolingOverride(
    val id: Long,
    val questionId: String,
)

/**
 * `savePlan` 的单事务实现（repository.md「事务规则」）。
 *
 * 事务内依次写 `PlanPage`、`Question.lastScheduledDate`、消费 `CoolingOverrideEvent`、
 * 应用 `DeferredItem(COOLING).targetNextDueDates`；任一步抛错由 [TransactionRunner] 整体回滚。
 *
 * 冷却顺延只按调度器给出的目标日期落库：Repository 不 `+1`、不改写为固定 7 天、不夹取（C-057）。
 */
class RoomPlanRepository(
    private val transactionRunner: TransactionRunner,
    private val planPageDao: PlanPageDao,
    private val planDao: PlanDao,
    private val questionDao: QuestionDao,
    private val coolingOverrideEventDao: CoolingOverrideEventDao,
    private val knowledgePointDao: KnowledgePointDao,
    private val clock: Clock,
) : PlanRepository {

    override suspend fun pendingCoolingOverrides(): List<PendingCoolingOverride> =
        coolingOverrideEventDao.unconsumed().map {
            PendingCoolingOverride(id = it.id, questionId = it.questionId)
        }

    override suspend fun savePlan(draft: PlanDraft) {
        requirePlanInvariants(draft)

        transactionRunner.inTransaction {
            val active = planDao.activeForWeek(draft.weekStartDate, draft.weekEndDate)
            check(
                active == null || active.planId == draft.planId ||
                    active.planId == draft.supersedesPlanId,
            ) {
                "本周已有计划 ${active?.planId}；必须通过“重新生成本周计划”显式替换"
            }

            // 计划头与页面/调度写入同一事务：要么整份计划可见，要么完全不写。
            planDao.insert(
                Plan(
                    planId = draft.planId,
                    weekStart = draft.weekStartDate,
                    weekEnd = draft.weekEndDate,
                    createdAt = clock.instant(),
                    status = PlanStatus.GENERATED.name,
                    printedAt = null,
                    supersededBy = null,
                    renderMeta = PlanRenderMetaJson.encode(
                        draft.renderMetaByQuestionId.values.filter {
                            it.questionId in draft.scheduledDates.keys
                        },
                    ),
                    deferred = PlanDeferredJson.encode(draft.deferredRecords),
                ),
            )
            draft.supersedesPlanId?.let { oldPlanId ->
                planDao.supersede(oldPlanId, draft.planId)
            }

            draft.pages.forEach { page ->
                planPageDao.insert(
                    page.toEntity(
                        planId = draft.planId,
                        rowsByQuestionId = draft.knowledgeRowsByQuestionId,
                        geometry = draft.geometryByPage[page.pageId].orEmpty(),
                    ),
                )
            }

            draft.scheduledDates.forEach { (questionId, targetDate) ->
                checkNotNull(questionDao.byId(questionId)) { "计划引用的题目不存在：$questionId（C-056）" }
                questionDao.markScheduled(questionId, targetDate)
            }

            val consumedAt = clock.instant()
            draft.coolingEventIdsToConsume.forEach { coolingOverrideEventDao.consume(it, consumedAt) }

            draft.deferred
                .filter { it.reason == DeferReason.COOLING }
                .forEach { item ->
                    item.targetNextDueDates.forEach { (knowledgePointId, nextDueDate) ->
                        val state = checkNotNull(knowledgePointDao.byId(knowledgePointId)) {
                            "冷却顺延引用的知识点不存在：$knowledgePointId（C-056）"
                        }
                        knowledgePointDao.update(state.copy(nextDueDate = nextDueDate))
                    }
                }
        }
    }

    override suspend fun activePlan(weekStart: LocalDate, weekEnd: LocalDate): StoredPlan? =
        planDao.activeForWeek(weekStart, weekEnd)?.toStoredPlan()

    override suspend fun findPlan(planId: String): StoredPlan? =
        planDao.byId(planId)?.toStoredPlan()

    override suspend fun storedPages(planId: String): List<StoredPlanPage> =
        planPageDao.forPlan(planId).map { page ->
            StoredPlanPage(
                pageId = page.pageId,
                pageNumber = page.pageNumber,
                date = page.date,
                itemCount = page.itemCount,
                questionIds = PlanPageJson.decodeQuestionIds(page.questionIds),
                geometry = ItemGeometryJson.decode(page.itemGeometry),
            )
        }

    override suspend fun savePrintSnapshots(
        planId: String,
        snapshots: List<PrintSnapshotRecord>,
        printedAt: Instant,
    ): Boolean {
        val plan = planDao.byId(planId) ?: return false
        if (plan.status !in ACTIVE_STATUSES) return false
        transactionRunner.inTransaction {
            snapshots.forEach { snapshot ->
                questionDao.setPrintSnapshot(
                    id = snapshot.questionId,
                    pageId = snapshot.pageId,
                    qrPayload = snapshot.qrPayload,
                )
            }
            planDao.markPrinted(planId, printedAt)
        }
        return true
    }

    private fun Plan.toStoredPlan(): StoredPlan = StoredPlan(
        planId = planId,
        weekStart = weekStart,
        weekEnd = weekEnd,
        status = PlanStatus.valueOf(status),
        printedAt = printedAt,
        createdAt = createdAt,
        renderMeta = PlanRenderMetaJson.decode(renderMeta),
        deferredRecords = PlanDeferredJson.decode(deferred),
    )

    /**
     * 写库前的前置条件（C-056）：页 id 唯一、同一题在一份计划内只出现一次、
     * 每个已排入题目都有计划日与行快照。任一不满足直接拒绝，不进入事务。
     */
    private fun requirePlanInvariants(draft: PlanDraft) {
        val pageIds = draft.pages.map { it.pageId }
        require(pageIds.size == pageIds.distinct().size) { "plan_page.pageId 重复（C-056）" }

        val placedQuestionIds = draft.pages.flatMap { page -> page.items.map { it.questionId } }
        require(placedQuestionIds.size == placedQuestionIds.distinct().size) {
            "同一题目在一份计划中只能出现一次（D-01/C-056）"
        }
        require(draft.scheduledDates.keys == placedQuestionIds.toSet()) {
            "scheduledDates 必须与已排入题目一一对应（C-056）"
        }
        require(placedQuestionIds.all { draft.knowledgeRowsByQuestionId.containsKey(it) }) {
            "已排入题目缺少行快照；不得按当前关联重算（C-072）"
        }

        val eventIds = draft.coolingEventIdsToConsume
        require(eventIds.size == eventIds.distinct().size) { "冷却覆盖事件 id 重复（C-056）" }
    }

    private fun PlannedPage.toEntity(
        planId: String,
        rowsByQuestionId: Map<String, PlanPageRows>,
        geometry: List<com.recollect.pagination.ScanItemGeometry>,
    ): PlanPage {
        val questionIds = items.map { it.questionId }
        val pageRows = questionIds.map { questionId ->
            rowsByQuestionId.getValue(questionId)
        }
        return PlanPage(
            planId = planId,
            pageId = pageId,
            pageNumber = pageNumber,
            date = date,
            itemCount = items.size,
            questionIds = PlanPageJson.encodeQuestionIds(questionIds),
            knowledgeRows = KnowledgeRowsCodec.encode(pageRows),
            continuationMarkers = PlanPageJson.encodeContinuationMarkers(continuationMarkers),
            itemGeometry = ItemGeometryJson.encode(geometry),
        )
    }

    private companion object {
        // C-099：COMPLETED 仍可重印；打印确认只更新快照，不把状态降级回 PRINTED。
        val ACTIVE_STATUSES = setOf(
            PlanStatus.GENERATED.name,
            PlanStatus.PRINTED.name,
            PlanStatus.COMPLETED.name,
        )
    }
}
