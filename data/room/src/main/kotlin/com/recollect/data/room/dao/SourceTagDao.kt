package com.recollect.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.recollect.data.room.entity.SourceTag

@Dao
interface SourceTagDao {

    @Query("SELECT * FROM source_tag ORDER BY name COLLATE NOCASE ASC")
    suspend fun all(): List<SourceTag>

    @Query("SELECT * FROM source_tag WHERE id = :id")
    suspend fun byId(id: String): SourceTag?

    @Query("SELECT * FROM source_tag WHERE name = :name")
    suspend fun byName(name: String): SourceTag?

    @Insert
    suspend fun insert(entity: SourceTag)

    @Update
    suspend fun update(entity: SourceTag)

    @Query("DELETE FROM source_tag WHERE id = :id")
    suspend fun delete(id: String)
}
