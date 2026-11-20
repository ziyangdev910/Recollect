package com.recollect.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 知识点编号序列（D-12 / C-028，单行表）。
 *
 * 编号分配与 `KnowledgePoint` 插入必须同一事务：读取 [lastIssued] → +1 → 跳过墓碑 → 写回。
 * `lastIssued` 只增不减，删除知识点不回收编号。
 */
@Entity(tableName = "knowledge_number_sequence")
data class KnowledgeNumberSequence(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val lastIssued: Int,
) {
    companion object {
        const val SINGLE_ROW_ID = 1
    }
}
