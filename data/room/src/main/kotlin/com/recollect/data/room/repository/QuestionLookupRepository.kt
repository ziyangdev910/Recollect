package com.recollect.data.room.repository

import com.recollect.data.room.dao.QuestionDao
import com.recollect.data.room.dao.QuestionKnowledgeCrossRefDao

/**
 * 确认提交前校验“知识点属于该题”的只读端口（feedback-usecase.md 前置条件）。
 *
 * 返回 `null` 表示题目本身不存在（按 C-055 整题跳过，不在这里抛错）；
 * 返回空集合表示题目存在但没有关联知识点（属于数据异常，调用方按整题跳过处理）。
 */
interface QuestionLookupRepository {
    suspend fun linkedKnowledgePointIds(questionId: String): Set<String>?
}

class RoomQuestionLookupRepository(
    private val questionDao: QuestionDao,
    private val crossRefDao: QuestionKnowledgeCrossRefDao,
) : QuestionLookupRepository {
    override suspend fun linkedKnowledgePointIds(questionId: String): Set<String>? {
        if (questionDao.byId(questionId) == null) return null
        return crossRefDao.forQuestion(questionId).map { it.knowledgePointId }.toSet()
    }
}
