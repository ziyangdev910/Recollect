package com.recollect.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.recollect.model.Tier
import java.time.Instant
import java.time.LocalDate

/**
 * 历史反馈（只增不改，字段见 docs/specs/data-model.md）。
 *
 * 只在确认阶段写一次；`NOT_REVIEWED` 不写本表（C-010）。
 * `rawOcrText` 只读、不参与任何调度计算（D-06）；`effectiveWeight` 值域 `{0.5, 1.0}`（C-006）。
 */
@Entity(tableName = "review_feedback")
data class ReviewFeedback(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val knowledgePointId: String,
    val questionId: String,
    val planId: String, // 来源计划（C-038/C-040）
    val pageId: String, // 来源页（C-038/C-040）
    val plannedDate: LocalDate, // 计划日（C-038）
    val reviewedOn: LocalDate, // 实际复习日（C-038）
    val tier: Tier, // 本次填涂/确认档位
    val triggered: Boolean, // 本行是否触发该知识点（C-009）
    val rawOcrText: String?, // 只读，D-06
    val correctedText: String?, // 可编辑，D-06
    val effectiveWeight: Float, // 1.0，或 FAMILIAR 的 0.5（C-006/C-010）
    val weightApplied: Float, // 审计：触发=1.0，非触发=0.3；不反向驱动其它字段
    val timestamp: Instant,
)
