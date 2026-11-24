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

    /**
     * 知识点列表：按最近一次用户修改时间倒序；时间相同时按编号升序，
     * 保证新增或编辑后的知识点立即出现在列表顶部（2026-11-24 用户要求）。
     */
    @Query("SELECT * FROM knowledge_point ORDER BY updatedAt DESC, number ASC")
    suspend fun allOrderedByUpdatedAt(): List<KnowledgePoint>

    @Query("SELECT * FROM knowledge_point WHERE id = :id")
    suspend fun byId(id: String): KnowledgePoint?

    /** 首个纵切的编号分配基线；序列表/墓碑（E-04/E-05/E-10）随完整录入补齐。 */
    @Query("SELECT COALESCE(MAX(number), 0) FROM knowledge_point")
    suspend fun maxNumber(): Int

    /** 删除知识点实体；编号墓碑由 Repository 在同一事务内先写入（D-12）。 */
    @Query("DELETE FROM knowledge_point WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM knowledge_point WHERE dormant = 0 AND activeQuestionCount > 0 ORDER BY number ASC")
    suspend fun printable(): List<KnowledgePoint>

    @Query("UPDATE knowledge_point SET activeQuestionCount = :count, dormant = :dormant WHERE id = :id")
    suspend fun updateActivity(id: String, count: Int, dormant: Boolean)
}
