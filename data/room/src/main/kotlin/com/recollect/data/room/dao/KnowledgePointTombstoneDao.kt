package com.recollect.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.recollect.data.room.entity.KnowledgePointTombstone

@Dao
interface KnowledgePointTombstoneDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: KnowledgePointTombstone)

    @Query("SELECT COUNT(*) FROM knowledge_point_tombstone WHERE number = :number")
    suspend fun exists(number: Int): Boolean

    /** 空表种子基线：与现有最大编号取较大者（E-10）。 */
    @Query("SELECT COALESCE(MAX(number), 0) FROM knowledge_point_tombstone")
    suspend fun maxNumber(): Int
}
