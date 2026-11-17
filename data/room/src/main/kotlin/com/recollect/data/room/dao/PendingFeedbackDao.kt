package com.recollect.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.recollect.data.room.entity.PendingFeedback
import java.time.LocalDate

@Dao
interface PendingFeedbackDao {

    // appendPending：按 (sessionId,questionId,knowledgePointId) 去重，保留最新识别结果。
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<PendingFeedback>)

    @Query("SELECT * FROM pending_feedback WHERE sessionId = :sessionId")
    suspend fun forSession(sessionId: String): List<PendingFeedback>

    @Query(
        "UPDATE pending_feedback SET actualReviewDate = :date " +
            "WHERE sessionId = :sessionId AND questionId = :questionId AND knowledgePointId = :knowledgePointId",
    )
    suspend fun updateActualReviewDate(
        sessionId: String,
        questionId: String,
        knowledgePointId: String,
        date: LocalDate?,
    )

    @Query("DELETE FROM pending_feedback WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)

    // C-055 方案 C：整题跳过时保留该题的待确认行，供数据修复后重试。
    @Query("DELETE FROM pending_feedback WHERE sessionId = :sessionId AND questionId = :questionId")
    suspend fun deleteForSessionQuestion(sessionId: String, questionId: String)
}
