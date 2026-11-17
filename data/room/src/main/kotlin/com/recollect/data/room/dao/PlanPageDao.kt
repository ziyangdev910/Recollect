package com.recollect.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.recollect.data.room.entity.PlanPage

@Dao
interface PlanPageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PlanPage)

    @Query("SELECT * FROM plan_page WHERE pageId = :pageId")
    suspend fun byPageId(pageId: String): PlanPage?

    @Query("SELECT * FROM plan_page WHERE planId = :planId ORDER BY pageNumber ASC")
    suspend fun forPlan(planId: String): List<PlanPage>

    @Query("SELECT COUNT(*) FROM plan_page WHERE planId = :planId")
    suspend fun countForPlan(planId: String): Int

    @Query("DELETE FROM plan_page WHERE planId = :planId")
    suspend fun deleteForPlan(planId: String)
}
