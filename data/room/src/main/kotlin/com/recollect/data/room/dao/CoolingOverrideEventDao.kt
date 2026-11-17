package com.recollect.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.recollect.data.room.entity.CoolingOverrideEvent
import java.time.Instant

@Dao
interface CoolingOverrideEventDao {

    @Insert
    suspend fun insert(entity: CoolingOverrideEvent): Long

    @Query("SELECT * FROM cooling_override_event WHERE consumedAt IS NULL")
    suspend fun unconsumed(): List<CoolingOverrideEvent>

    @Query("UPDATE cooling_override_event SET consumedAt = :consumedAt WHERE id = :id")
    suspend fun consume(id: Long, consumedAt: Instant)
}
