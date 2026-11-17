package com.recollect.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.recollect.data.room.entity.ReviewFeedback

@Dao
interface ReviewFeedbackDao {

    @Insert
    suspend fun insert(entity: ReviewFeedback): Long

    @Query("SELECT * FROM review_feedback WHERE knowledgePointId = :knowledgePointId ORDER BY timestamp ASC")
    suspend fun forKnowledgePoint(knowledgePointId: String): List<ReviewFeedback>

    @Query("SELECT * FROM review_feedback WHERE planId = :planId ORDER BY timestamp ASC")
    suspend fun forPlan(planId: String): List<ReviewFeedback>
}
