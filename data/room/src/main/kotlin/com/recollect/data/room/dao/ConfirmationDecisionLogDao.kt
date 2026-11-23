package com.recollect.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.recollect.data.room.entity.ConfirmationDecisionLog

@Dao
interface ConfirmationDecisionLogDao {

    @Insert
    suspend fun insert(entity: ConfirmationDecisionLog): Long

    @Query("SELECT * FROM confirmation_decision_log WHERE planId = :planId ORDER BY decidedAt ASC")
    suspend fun forPlan(planId: String): List<ConfirmationDecisionLog>

    @Query("SELECT * FROM confirmation_decision_log WHERE sessionId = :sessionId ORDER BY decidedAt ASC")
    suspend fun forSession(sessionId: String): List<ConfirmationDecisionLog>

    // PlanCompletion.reviewedItems：非 NOT_REVIEWED 行数。
    @Query("SELECT COUNT(*) FROM confirmation_decision_log WHERE planId = :planId AND decision != 'NOT_REVIEWED'")
    suspend fun reviewedCount(planId: String): Int

    // PlanCompletion.scannedPages：该 plan 出现过的 pageId 去重数。
    @Query("SELECT COUNT(DISTINCT pageId) FROM confirmation_decision_log WHERE planId = :planId")
    suspend fun scannedPageCount(planId: String): Int

    // C-099：计划完成判定用；与 plan_page 的页集合比较，避免把其他计划的页计入。
    @Query("SELECT DISTINCT pageId FROM confirmation_decision_log WHERE planId = :planId")
    suspend fun confirmedPageIds(planId: String): List<String>
}
