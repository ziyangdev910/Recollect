package com.recollect.data.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * 来源标签（如「作业本」「同步练习」），供录入页单选填写题目来源。
 *
 * 来源不是调度实体：题目只保存标签名称（`Question.sourcePage`），
 * 标签库负责候选值与增删改查。名称唯一，重命名时由 Repository 同步题目引用。
 */
@Entity(
    tableName = "source_tag",
    indices = [Index(value = ["name"], unique = true)],
)
data class SourceTag(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Instant,
)
