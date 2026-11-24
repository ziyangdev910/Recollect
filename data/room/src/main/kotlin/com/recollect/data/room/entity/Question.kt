package com.recollect.data.room.entity

import androidx.room.ColumnInfo
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
    /**
     * 来源页码的说明文字（如「作业本」「同步练习」），可空。
     * 展示/打印时与 [sourcePageCode] 组成 `编号-说明`；说明为空时只显示编号。
     * 不是复习打印稿的 pageId。
     */
    @ColumnInfo(defaultValue = "''")
    val sourcePage: String = "",
    /**
     * 来源页码编号：由用户通过 +/− 调整，默认沿用上次使用的值（不自增）。
     * 同一页的多道题共用同一编号；与 [sourcePage] 一起组成页码。
     */
    @ColumnInfo(defaultValue = "0")
    val sourcePageCode: Int = 0,
    /**
     * 最近一次由用户新建或编辑的时间；题库按此倒序展示（2026-11-24 用户要求）。
     * 打印快照、复习日与归档等系统写回不改变该值。
     */
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Instant = createdAt,
)
