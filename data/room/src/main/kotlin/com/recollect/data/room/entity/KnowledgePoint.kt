package com.recollect.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.recollect.model.Tier
import java.time.Instant
import java.time.LocalDate

/**
 * 知识点实体（字段见 docs/specs/data-model.md）。
 *
 * `number` 全局唯一、创建后不可变、不复用（D-12 / C-028）；唯一索引由 data:room 保证（C-056）。
 * `continuousScore`/`effectiveFeedbackCount` 落库为 `Float`（C-050）。
 * `categoryId` 为知识点分类（2026-11-23 用户要求）；旧数据在 v13 迁移时统一归入「语文」。
 */
@Entity(
    tableName = "knowledge_point",
    indices = [Index(value = ["number"], unique = true)],
)
data class KnowledgePoint(
    @PrimaryKey val id: String,
    val number: Int, // 全局唯一、创建后不变、不复用（D-12）
    val name: String,
    val currentTier: Tier,
    val continuousScore: Float,
    val effectiveFeedbackCount: Float,
    val nextDueDate: LocalDate,
    val activeQuestionCount: Int,
    val dormant: Boolean,
    val difficulty: Int?, // 冷启动阶段为 null（C-018）
    val halfLifeDays: Float?, // 冷启动阶段为 null（C-018）
    val createdAt: Instant, // DHP 第一段间隔起点（C-037）
    @ColumnInfo(defaultValue = KnowledgeCategoryDefaults.DEFAULT_CATEGORY_ID_SQL)
    val categoryId: String = KnowledgeCategoryDefaults.DEFAULT_CATEGORY_ID,
)
