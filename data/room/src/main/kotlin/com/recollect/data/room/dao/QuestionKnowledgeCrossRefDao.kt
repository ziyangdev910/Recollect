package com.recollect.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.recollect.data.room.entity.QuestionKnowledgeCrossRef

@Dao
interface QuestionKnowledgeCrossRefDao {

    // 复合主键冲突即重复关联；失败而不是静默覆盖（C-056）。
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: QuestionKnowledgeCrossRef)

    @Query("SELECT * FROM question_knowledge_cross_ref")
    suspend fun all(): List<QuestionKnowledgeCrossRef>

    @Query("SELECT * FROM question_knowledge_cross_ref WHERE questionId = :questionId")
    suspend fun forQuestion(questionId: String): List<QuestionKnowledgeCrossRef>

    @Query("SELECT COUNT(*) FROM question_knowledge_cross_ref WHERE knowledgePointId = :knowledgePointId")
    suspend fun countForKnowledgePoint(knowledgePointId: String): Int

    @Query("DELETE FROM question_knowledge_cross_ref WHERE questionId = :questionId")
    suspend fun deleteForQuestion(questionId: String)

    @Query("DELETE FROM question_knowledge_cross_ref WHERE knowledgePointId = :knowledgePointId")
    suspend fun deleteForKnowledgePoint(knowledgePointId: String)
}
