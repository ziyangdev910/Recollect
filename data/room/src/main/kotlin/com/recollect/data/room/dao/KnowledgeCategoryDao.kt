package com.recollect.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.recollect.data.room.entity.KnowledgeCategory

@Dao
interface KnowledgeCategoryDao {

    @Query("SELECT * FROM knowledge_category ORDER BY sortOrder ASC, createdAt ASC, id ASC")
    suspend fun all(): List<KnowledgeCategory>

    @Query("SELECT * FROM knowledge_category WHERE id = :id")
    suspend fun byId(id: String): KnowledgeCategory?

    @Query("SELECT * FROM knowledge_category WHERE name = :name LIMIT 1")
    suspend fun byName(name: String): KnowledgeCategory?

    @Insert
    suspend fun insert(entity: KnowledgeCategory)

    @Update
    suspend fun update(entity: KnowledgeCategory)

    @Query("DELETE FROM knowledge_category WHERE id = :id")
    suspend fun delete(id: String)

    /** 引用计数含休眠知识点：分类被任何知识点引用时不可删除。 */
    @Query("SELECT COUNT(*) FROM knowledge_point WHERE categoryId = :categoryId")
    suspend fun knowledgePointCount(categoryId: String): Int
}
