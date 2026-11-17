package com.recollect.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.recollect.model.Tier
import java.time.Instant
import java.time.LocalDate

/**
 * 确认决定日志（字段见 docs/specs/data-model.md）。
 *
 * 每次提交对每一行写一条，包括 `NOT_REVIEWED`；这是 R-04 的审计落点，不等于写 [ReviewFeedback]。
 * 纳入备份导出（历史记录，不是未确认状态）。
 */
@Entity(tableName = "confirmation_decision_log")
data class ConfirmationDecisionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val planId: String,
    val pageId: String,
    val questionId: String,
    val knowledgePointId: String,
    val plannedDate: LocalDate, // 计划日（C-038）
    val reviewedOn: LocalDate?, // 实际复习日；NOT_REVIEWED 为 null（C-038/C-039）
    val triggered: Boolean,
    val decision: UserDecision, // 含 NOT_REVIEWED
    val selectedTier: Tier?,
    val correctedText: String?,
    val isSupplemental: Boolean,
    val decidedAt: Instant,
)
