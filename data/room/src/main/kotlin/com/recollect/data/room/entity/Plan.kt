package com.recollect.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * 计划头（本周计划单例）。
 *
 * `plan_page` 保存可回读的页面快照；本表只记录本周当前计划及其打印状态，
 * 用于避免重复生成计划号、阻止旧计划覆盖最新打印快照。
 *
 * `renderMeta` 是计划生成时冻结的打印元数据（题号、知识点名称、行数、附件文本等），
 * 使进程重建后仍能按原布局重新打印同一份计划。
 */
@Entity(tableName = "plan")
data class Plan(
    @PrimaryKey val planId: String,
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val createdAt: Instant,
    /** `GENERATED` / `PRINTED` / `COMPLETED` / `SUPERSEDED`；见 `PlanStatus`。 */
    val status: String,
    val printedAt: Instant?,
    val supersededBy: String?,
    val renderMeta: String = "[]",
)
