package com.recollect.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.recollect.data.room.entity.Question
import java.time.Instant
import java.time.LocalDate

@Dao
interface QuestionDao {

    @Insert
    suspend fun insert(entity: Question)

    @Update
    suspend fun update(entity: Question)

    @Query("SELECT * FROM question WHERE archived = 0")
    suspend fun active(): List<Question>

    @Query("SELECT * FROM question WHERE id = :id")
    suspend fun byId(id: String): Question?

    @Query("UPDATE question SET pageId = :pageId, qrPayload = :qrPayload WHERE id = :id")
    suspend fun setPrintSnapshot(id: String, pageId: String, qrPayload: String)

    @Query("UPDATE question SET lastScheduledDate = :targetDate WHERE id = :id")
    suspend fun markScheduled(id: String, targetDate: LocalDate)

    @Query("UPDATE question SET archived = 1, archivedAt = :archivedAt WHERE id = :id")
    suspend fun markArchived(id: String, archivedAt: Instant)

    @Query("UPDATE question SET archived = 0, archivedAt = NULL WHERE id = :id")
    suspend fun markUnarchived(id: String)
}
