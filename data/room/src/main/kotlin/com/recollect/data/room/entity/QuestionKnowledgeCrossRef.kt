package com.recollect.data.room.entity

import androidx.room.Entity

/**
 * 题目-知识点关联（复合主键 questionId + knowledgePointId，见 docs/specs/data-model.md）。
 *
 * 复合主键保证同一题对同一知识点只有一条关联，是 data:room 对 id 唯一性的落点（C-056）。
 * 行序不存于此表；打印行序由 `KnowledgePoint.number` 升序生成并冻结到行快照（C-015）。
 * `weight` 目前恒为 1.0，不参与 D-01 的 0.3 传播。
 */
@Entity(
    tableName = "question_knowledge_cross_ref",
    primaryKeys = ["questionId", "knowledgePointId"],
)
data class QuestionKnowledgeCrossRef(
    val questionId: String,
    val knowledgePointId: String,
    val weight: Float = 1.0f,
)
