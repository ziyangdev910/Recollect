package com.recollect.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.recollect.data.room.entity.Plan
import java.time.Instant
import java.time.LocalDate

@Dao
interface PlanDao {

    @Insert
    suspend fun insert(plan: Plan)

    @Query("SELECT * FROM plan WHERE planId = :planId")
    suspend fun byId(planId: String): Plan?

    /** 本周仍可使用的计划：排除已显式替代的旧计划；多份残留时取最新创建的一份。 */
    @Query(
        "SELECT * FROM plan " +
            "WHERE weekStart = :weekStart AND weekEnd = :weekEnd " +
            "AND status IN ('GENERATED', 'PRINTED') " +
            "ORDER BY createdAt DESC LIMIT 1",
    )
    suspend fun activeForWeek(weekStart: LocalDate, weekEnd: LocalDate): Plan?

    @Query(
        "UPDATE plan SET status = 'SUPERSEDED', supersededBy = :supersededBy " +
            "WHERE planId = :planId",
    )
    suspend fun supersede(planId: String, supersededBy: String)

    @Query("UPDATE plan SET status = 'PRINTED', printedAt = :printedAt WHERE planId = :planId")
    suspend fun markPrinted(planId: String, printedAt: Instant)
}
