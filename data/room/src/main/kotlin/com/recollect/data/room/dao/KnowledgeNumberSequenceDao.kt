package com.recollect.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.recollect.data.room.entity.KnowledgeNumberSequence

@Dao
interface KnowledgeNumberSequenceDao {

    @Query("SELECT * FROM knowledge_number_sequence WHERE id = 1")
    suspend fun get(): KnowledgeNumberSequence?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: KnowledgeNumberSequence)

    @Query("UPDATE knowledge_number_sequence SET lastIssued = :lastIssued WHERE id = 1")
    suspend fun updateLastIssued(lastIssued: Int)
}
