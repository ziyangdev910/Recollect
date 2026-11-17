package com.recollect.data.room.entity

import androidx.room.Entity
import androidx.room.Index
import java.time.LocalDate

/**
 * 打印页快照实体（字段见 docs/specs/data-model.md）。
 *
 * `pageId` 全局唯一索引使手动输入「页面 ID + 题号」可只凭 pageId 解析 planId（C-011/C-014/C-056）。
 * `knowledgeRows` 是每题目一条记录的行快照 JSON，行序冻结打印时的顺序（C-015、C-070）。
 */
@Entity(
    tableName = "plan_page",
    primaryKeys = ["planId", "pageId"],
    indices = [Index(value = ["pageId"], unique = true)],
)
data class PlanPage(
    val planId: String,
    val pageId: String, // = "{planId}-{pageNumber:02d}"，全局唯一（C-014）
    val pageNumber: Int, // 1..99
    val date: LocalDate,
    val itemCount: Int,
    val questionIds: String, // JSON 数组，本页题目的打印顺序
    val knowledgeRows: String, // 行快照 JSON；由 KnowledgeRowsCodec 编解码（C-046）
    val continuationMarkers: String, // JSON 数组（C-016）
)
