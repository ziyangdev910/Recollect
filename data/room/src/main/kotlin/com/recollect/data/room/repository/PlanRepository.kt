package com.recollect.data.room.repository

import com.recollect.data.room.dao.CoolingOverrideEventDao
import com.recollect.data.room.dao.KnowledgePointDao
import com.recollect.data.room.dao.PlanPageDao
import com.recollect.data.room.dao.QuestionDao
import com.recollect.data.room.entity.PlanPage
import com.recollect.model.KnowledgeRowsCodec
import com.recollect.model.PlanPageRows
import com.recollect.pagination.PlannedPage
import com.recollect.scheduler.DeferReason
import java.time.Clock

/**
 * 计划持久化（repository.md）。本轮只落地事务方法 [savePlan]；查询/清理随对应轮次补齐。
 */
interface PlanRepository {
    suspend fun savePlan(draft: PlanDraft)
}

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
    private val questionDao: QuestionDao,
    private val coolingOverrideEventDao: CoolingOverrideEventDao,
    private val knowledgePointDao: KnowledgePointDao,
    private val clock: Clock,
) : PlanRepository {

    override suspend fun savePlan(draft: PlanDraft) {
        requirePlanInvariants(draft)

        transactionRunner.inTransaction {
            draft.pages.forEach { planPageDao.insert(it.toEntity(draft.planId, draft.knowledgeRowsByQuestionId)) }

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
        )
    }
}
