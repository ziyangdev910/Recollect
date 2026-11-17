package com.recollect.data.room.migration

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 迁移脚本的共享守卫。
 *
 * data-model.md 要求：新增 `knowledge_point.number` 与 `plan_page.pageId` 唯一索引前必须先清理重复值；
 * 迁移遇到重复必须失败并提示，不得静默重编号。后续每个 `Migration` 在 `CREATE UNIQUE INDEX`
 * 之前都必须调用这里的检查。
 */
object SchemaMigrationRules {

    /**
     * 纯函数版本：供迁移前的数据快照/导入校验复用，也便于单元测试。
     */
    fun requireUniqueValues(table: String, column: String, values: List<Any?>) {
        val duplicates = values
            .filterNotNull()
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
            .toList()
        failIfDuplicated(table, column, duplicates)
    }

    /**
     * 迁移版本：直接在旧库上检查，返回重复示例而不是自动修复。
     */
    fun requireUniqueColumn(
        db: SupportSQLiteDatabase,
        table: String,
        column: String,
        sampleLimit: Int = 10,
    ) {
        val duplicates = mutableListOf<Any?>()
        db.query(
            "SELECT $column FROM $table " +
                "WHERE $column IS NOT NULL GROUP BY $column HAVING COUNT(*) > 1 LIMIT $sampleLimit",
        ).use { cursor ->
            while (cursor.moveToNext()) {
                duplicates += cursor.getString(0)
            }
        }
        failIfDuplicated(table, column, duplicates)
    }

    private fun failIfDuplicated(table: String, column: String, duplicates: List<Any?>) {
        check(duplicates.isEmpty()) {
            "迁移中止：$table.$column 存在重复值（${duplicates.joinToString()}）。" +
                "创建唯一索引前必须先由用户确认并清理重复数据；不得静默重编号或删除行。"
        }
    }
}
