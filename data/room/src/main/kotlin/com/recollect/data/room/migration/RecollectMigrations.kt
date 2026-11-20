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

    /** v2 → v3：`plan_page` 增加回读几何快照（C-093 方案 A）；旧稿为空数组。 */
    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `plan_page` ADD COLUMN `itemGeometry` TEXT NOT NULL DEFAULT '[]'",
            )
        }
    }

    /** v3 → v4：`question` 增加题目来源页码（试卷/作业本第几面）；旧题为空字符串。 */
    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `question` ADD COLUMN `sourcePage` TEXT NOT NULL DEFAULT ''",
            )
        }
    }

    /**
     * v4 → v5：标签库编号序列与墓碑（D-12 / C-028）。
     *
     * 序号以现有最大编号为基线种子，首个标签从 `maxNumber + 1` 开始；
     * 旧库没有删除记录，因此墓碑表为空。
     */
    val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `knowledge_point_tombstone` (" +
                    "`number` INTEGER NOT NULL, " +
                    "`knowledgePointId` TEXT NOT NULL, " +
                    "`voidedAt` INTEGER NOT NULL, " +
                    "`reason` TEXT, " +
                    "PRIMARY KEY(`number`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `knowledge_number_sequence` (" +
                    "`id` INTEGER NOT NULL, " +
                    "`lastIssued` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "INSERT OR REPLACE INTO `knowledge_number_sequence` (`id`, `lastIssued`) " +
                    "SELECT 1, COALESCE(MAX(`number`), 0) FROM `knowledge_point`",
            )
        }
    }

    /**
     * v5 → v6：来源页码拆为「系统编号 + 可选说明」（`sourcePageCode`）。
     *
     * 旧行的自由文本保留为说明文字；按创建时间顺序补 1..N 的唯一编号，并建立唯一索引。
     */
    val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `question` ADD COLUMN `sourcePageCode` INTEGER NOT NULL DEFAULT 0",
            )
            val cursor = db.query("SELECT `id` FROM `question` ORDER BY `createdAt` ASC, `id` ASC")
            var code = 0
            cursor.use {
                while (it.moveToNext()) {
                    code++
                    db.execSQL(
                        "UPDATE `question` SET `sourcePageCode` = ? WHERE `id` = ?",
                        arrayOf(code, it.getString(0)),
                    )
                }
            }
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_question_sourcePageCode` " +
                    "ON `question` (`sourcePageCode`)",
            )
        }
    }

    /**
     * v6 → v7：来源页码编号改为用户通过 +/− 调整，同一页可复用同一编号。
     *
     * 编号不再唯一，删除 v6 建立的唯一索引；已有编号原样保留。
     */
    val MIGRATION_6_7: Migration = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP INDEX IF EXISTS `index_question_sourcePageCode`")
        }
    }

    val ALL: Array<Migration> =
        arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
}
