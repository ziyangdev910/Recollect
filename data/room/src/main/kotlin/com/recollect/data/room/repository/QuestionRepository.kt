package com.recollect.data.room.repository

import com.recollect.data.room.dao.KnowledgePointDao
import com.recollect.data.room.dao.QuestionDao
import com.recollect.data.room.dao.QuestionKnowledgeCrossRefDao
import com.recollect.data.room.mapping.toSchedulerQuestion
import com.recollect.model.SchedulerQuestion

/**
 * 题目调度只读端口（repository.md `QuestionRepository` 的本轮子集）。
 *
 * 归档/取消归档、打印快照等写入随对应轮次补齐；本轮只提供计划生成所需的调度输入与题号。
 */
interface QuestionRepository {
    /** 未归档题目的调度输入；`linkedKnowledgeIds` 由 cross-ref 映射并按编号升序（C-046/C-056）。 */
    suspend fun schedulerQuestions(): List<SchedulerQuestion>

    /** 题号投影：`PlannedQuestion` 不含题号，预览/打印需由 data:room 补齐（repository.md）。 */
    suspend fun questionNumbers(): Map<String, String>

    /** 题目来源页码投影（试卷/作业本第几面），供打印稿与确认页定位；不是打印稿 pageId。 */
    suspend fun sourcePages(): Map<String, String>

    /** 最近一次打印快照（C-033 方案一、print-template.md）：写完打印文档后回写。 */
    suspend fun savePrintSnapshot(questionId: String, pageId: String, qrPayload: String)
}

class RoomQuestionRepository(
    private val questionDao: QuestionDao,
    private val crossRefDao: QuestionKnowledgeCrossRefDao,
    private val knowledgePointDao: KnowledgePointDao,
) : QuestionRepository {

    override suspend fun schedulerQuestions(): List<SchedulerQuestion> {
        val knowledgePoints = knowledgePointDao.allOrderedByNumber()
        val crossRefs = crossRefDao.all()
        return questionDao.active().map { it.toSchedulerQuestion(crossRefs, knowledgePoints) }
    }

    override suspend fun questionNumbers(): Map<String, String> =
        questionDao.active().associate { it.id to it.questionNumber }

    override suspend fun sourcePages(): Map<String, String> =
        questionDao.active().associate {
            it.id to formatSourcePage(it.sourcePageCode, it.sourcePage)
        }

    override suspend fun savePrintSnapshot(questionId: String, pageId: String, qrPayload: String) {
        questionDao.setPrintSnapshot(questionId, pageId, qrPayload)
    }
}
