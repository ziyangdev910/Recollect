package com.recollect.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.recollect.data.room.entity.KnowledgePoint

@Dao
interface KnowledgePointDao {

    @Insert
    suspend fun insert(entity: KnowledgePoint)

    @Update
    suspend fun update(entity: KnowledgePoint)

    @Query("SELECT * FROM knowledge_point ORDER BY number ASC")
    suspend fun allOrderedByNumber(): List<KnowledgePoint>

    @Query("SELECT * FROM knowledge_point WHERE id = :id")
    suspend fun byId(id: String): KnowledgePoint?

    @Query("SELECT * FROM knowledge_point WHERE dormant = 0 AND activeQuestionCount > 0 ORDER BY number ASC")
    suspend fun printable(): List<KnowledgePoint>

    @Query("UPDATE knowledge_point SET activeQuestionCount = :count, dormant = :dormant WHERE id = :id")
    suspend fun updateActivity(id: String, count: Int, dormant: Boolean)
}
