package com.recollect.data.room.repository

import com.recollect.data.room.dao.KnowledgePointDao
import com.recollect.data.room.dao.QuestionDao
import com.recollect.data.room.dao.QuestionKnowledgeCrossRefDao
import com.recollect.model.Tier
import java.time.LocalDate

/**
 * 复习进度只读端口（2026-11-22 用户要求）。
 *
 * 按未归档题目聚合其关联知识点的调度状态，供复习进度页展示；只读，不参与调度计算。
 * 调度单元是知识点（D-01），因此题目的“到期”取任一未休眠关联知识点最早的 `nextDueDate`；
 * 归档题目退出调度（D-11），不出现在本投影中。
 */
interface ReviewProgressRepository {
    /** 未归档题目及其关联知识点状态；题目与知识点均按编号升序。 */
    suspend fun questions(): List<QuestionReviewProgress>
}

/** 一道题的复习进度投影；知识点只包含仍可解析的关联。 */
data class QuestionReviewProgress(
    val questionId: String,
    val questionNumber: String,
    /** 展示用来源页：`编号-说明`；说明为空时只有编号。 */
    val sourcePage: String,
    /** 实际复习日；尚未复习为 `null`。 */
    val lastReviewedDate: LocalDate?,
    val knowledgePoints: List<KnowledgePointReviewProgress>,
)

/** 知识点调度状态中与进度展示相关的只读投影。 */
data class KnowledgePointReviewProgress(
    val id: String,
    val number: Int,
    val name: String,
    val tier: Tier,
    val continuousScore: Float,
    val effectiveFeedbackCount: Float,
    val halfLifeDays: Float?,
    val nextDueDate: LocalDate,
    val dormant: Boolean,
)

class RoomReviewProgressRepository(
    private val questionDao: QuestionDao,
    private val crossRefDao: QuestionKnowledgeCrossRefDao,
    private val knowledgePointDao: KnowledgePointDao,
) : ReviewProgressRepository {

    override suspend fun questions(): List<QuestionReviewProgress> {
        val knowledgeById = knowledgePointDao.allOrderedByNumber().associateBy { it.id }
        val refsByQuestion = crossRefDao.all().groupBy { it.questionId }
        return questionDao.active().map { question ->
            val linked = refsByQuestion[question.id].orEmpty()
                .mapNotNull { knowledgeById[it.knowledgePointId] }
                .sortedBy { it.number }
            QuestionReviewProgress(
                questionId = question.id,
                questionNumber = question.questionNumber,
                sourcePage = formatSourcePage(question.sourcePageCode, question.sourcePage),
                lastReviewedDate = question.lastReviewedDate,
                knowledgePoints = linked.map { knowledgePoint ->
                    KnowledgePointReviewProgress(
                        id = knowledgePoint.id,
                        number = knowledgePoint.number,
                        name = knowledgePoint.name,
                        tier = knowledgePoint.currentTier,
                        continuousScore = knowledgePoint.continuousScore,
                        effectiveFeedbackCount = knowledgePoint.effectiveFeedbackCount,
                        halfLifeDays = knowledgePoint.halfLifeDays,
                        nextDueDate = knowledgePoint.nextDueDate,
                        dormant = knowledgePoint.dormant,
                    )
                },
            )
        }
    }
}
