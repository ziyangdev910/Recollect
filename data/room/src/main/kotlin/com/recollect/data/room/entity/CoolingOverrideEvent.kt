package com.recollect.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * 一次性冷却覆盖事件（C-008 方案二，字段见 docs/specs/data-model.md）。
 *
 * 确认页的「本次提前复习」创建；`plan()` 通过 `coolingOverrides` 接收未消费事件的 questionId。
 * 同一题至多保留一个未消费事件；临时操作态，不导出备份（C-031）。
 */
@Entity(tableName = "cooling_override_event")
data class CoolingOverrideEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String, // 由确认页创建；记录来源会话
    val questionId: String,
    val createdAt: Instant,
    val consumedAt: Instant?, // 实际排入计划后置为消费时间；未消费可继续用于下一份计划
)
