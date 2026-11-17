package com.recollect.data.room.repository

import com.recollect.data.room.dao.KnowledgePointDao
import com.recollect.data.room.dao.ReviewFeedbackDao
import com.recollect.data.room.mapping.toKnowledgeState
import com.recollect.model.KnowledgeState
import com.recollect.model.Tier
import java.time.LocalDate

/**
 * 知识点状态的只读端口（repository.md `KnowledgePointRepository` 的本轮子集）。
 *
 * 状态更新由 `ApplyFeedbackUseCase` 计算、经 [FeedbackRepository.saveDecisions] 落库；
 * 本接口只提供计算所需的当前状态，不在这里重算（C-074 方案 A）。
 */
interface KnowledgePointRepository {
    suspend fun states(): List<KnowledgeState>
}

class RoomKnowledgePointRepository(
    private val knowledgePointDao: KnowledgePointDao,
) : KnowledgePointRepository {
    override suspend fun states(): List<KnowledgeState> =
        knowledgePointDao.allOrderedByNumber().map { it.toKnowledgeState() }
}

/**
 * 一条已写入的 triggered 反馈（DHP 初始化/更新所需的最小投影）。
 *
 * 只保留档位与实际复习日；不向上暴露 Room 实体（repository.md「边界」）。
 */
data class TriggeredFeedbackRecord(
    val tier: Tier,
    val reviewedOn: LocalDate,
)

/**
 * 反馈历史的只读端口（feedback-usecase.md `ReviewFeedbackRepository` 的本轮子集）。
 *
 * 冷启动事件序号按 `triggered = true` 的反馈条数计算；非触发行不推进阶梯（D-05、U-16）。
 * [recentTriggered] 返回按复习日升序排列的最近 [limit] 条 triggered 反馈，供 DHP 初始化与
 * 实际间隔计算使用（C-037）。
 */
interface ReviewFeedbackHistoryRepository {
    suspend fun triggeredCount(knowledgePointId: String): Int
    suspend fun recentTriggered(knowledgePointId: String, limit: Int): List<TriggeredFeedbackRecord>
}

class RoomReviewFeedbackHistoryRepository(
    private val reviewFeedbackDao: ReviewFeedbackDao,
) : ReviewFeedbackHistoryRepository {
    override suspend fun triggeredCount(knowledgePointId: String): Int =
        reviewFeedbackDao.forKnowledgePoint(knowledgePointId).count { it.triggered }

    override suspend fun recentTriggered(
        knowledgePointId: String,
        limit: Int,
    ): List<TriggeredFeedbackRecord> =
        reviewFeedbackDao.forKnowledgePoint(knowledgePointId)
            .filter { it.triggered }
            .sortedWith(compareBy({ it.reviewedOn }, { it.timestamp }))
            .takeLast(limit)
            .map { TriggeredFeedbackRecord(tier = it.tier, reviewedOn = it.reviewedOn) }
}
