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

    /**
     * 本周仍可使用的计划：排除已显式替代的旧计划；多份残留时取最新创建的一份。
     *
     * COMPLETED 也算“可用”：本周复习做完后仍要能查看/重新打印该计划，
     * 首页据此显示“已完成”并引导生成下周计划（C-099）。
     */
    @Query(
        "SELECT * FROM plan " +
            "WHERE weekStart = :weekStart AND weekEnd = :weekEnd " +
            "AND status IN ('GENERATED', 'PRINTED', 'COMPLETED') " +
            "ORDER BY createdAt DESC LIMIT 1",
    )
    suspend fun activeForWeek(weekStart: LocalDate, weekEnd: LocalDate): Plan?

    /** 只在尚未结束时迁移为 COMPLETED；返回受影响行数（0 表示计划已替代/已完成）。 */
    @Query(
        "UPDATE plan SET status = 'COMPLETED' " +
            "WHERE planId = :planId AND status IN ('GENERATED', 'PRINTED')",
    )
    suspend fun markCompleted(planId: String): Int

    @Query(
        "UPDATE plan SET status = 'SUPERSEDED', supersededBy = :supersededBy " +
            "WHERE planId = :planId",
    )
    suspend fun supersede(planId: String, supersededBy: String)

    /**
     * 确认打印后写 printedAt；只在 GENERATED/PRINTED 时把状态置为 PRINTED。
     *
     * C-099：COMPLETED 的计划允许重印并更新快照，但不得把“已完成”降级回“待打印”。
     */
    @Query(
        "UPDATE plan SET status = 'PRINTED', printedAt = :printedAt " +
            "WHERE planId = :planId AND status IN ('GENERATED', 'PRINTED')",
    )
    suspend fun markPrinted(planId: String, printedAt: Instant)
}
