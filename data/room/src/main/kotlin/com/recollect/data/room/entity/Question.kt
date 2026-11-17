package com.recollect.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * 题目实体（字段见 docs/specs/data-model.md）。
 *
 * 本实体没有 `linkedKnowledgeIds` 列，关联由 [QuestionKnowledgeCrossRef] 承载。
 * `pageId`/`qrPayload` 是最近一次打印快照，由打印层在打印成功后写入。
 */
@Entity(tableName = "question")
data class Question(
    @PrimaryKey val id: String,
    val pageId: String, // 最近一次打印所在页面；打印成功后写
    val questionNumber: String,
    val qrPayload: String, // 最近一次 QR；打印成功后写
    val archived: Boolean,
    val lastScheduledDate: LocalDate?,
    val lastReviewedDate: LocalDate?, // 实际复习日（C-039）
    val estimatedReviewMinutes: Int = 15, // 缺失或 <= 0 按 15（C-017）
    val createdAt: Instant,
    val archivedAt: Instant?,
)
