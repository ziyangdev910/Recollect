package com.recollect.data.room.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移清单。
 *
 * v1 → v2：新增 `note_attachment_item`（saveDecisions 事务需要）。
 * 后续新增迁移时：
 * 1. 不允许 destructive migration；
 * 2. 在创建任一唯一索引前调用 [SchemaMigrationRules.requireUniqueColumn]，
 *    遇重复直接失败并提示用户，不得静默重编号；
 * 3. 迁移完成后同步更新 schema 导出与 `RoomSchemaTest`。
 */
object RecollectMigrations {

    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `note_attachment_item` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`questionId` TEXT NOT NULL, " +
                    "`sourceFeedbackId` INTEGER, " +
                    "`text` TEXT NOT NULL, " +
                    "`imageUri` TEXT, " +
                    "`order` INTEGER NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL)",
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
