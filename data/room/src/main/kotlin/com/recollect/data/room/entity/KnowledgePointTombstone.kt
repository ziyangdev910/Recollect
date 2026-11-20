package com.recollect.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * 知识点编号墓碑（D-12 / C-028，字段见 docs/specs/data-model.md）。
 *
 * 记录「该编号曾经存在且已作废」，防止编号被再次分配，并保留审计。
 * 墓碑不随知识点删除而删除。
 */
@Entity(tableName = "knowledge_point_tombstone")
data class KnowledgePointTombstone(
    @PrimaryKey val number: Int,
    val knowledgePointId: String,
    val voidedAt: Instant,
    val reason: String?,
)
