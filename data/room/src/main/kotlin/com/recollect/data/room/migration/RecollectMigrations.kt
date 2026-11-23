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
 *
 * v9 的 `source_tag.name` 唯一索引在迁移内先 `GROUP BY trim(sourcePage)` 去重种入，
 * 再建索引，不会触发唯一冲突。
 * v10 删除三张表的 `isSupplemental` 列，并把历史 `SUPPLEMENTED` 决策归并为 `CORRECTED`。
 * v11 给 `plan` 增加被推迟题目的快照 `deferred`，旧计划为空数组。
 * v12 新增 `app_settings` 单行表（每日复习时长），并写入默认 60 分钟。
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

    /**
     * v7 → v8：新增计划头 `plan`（本周计划单例）。
     *
     * 迁移前已有的 `plan_page` 不补计划头：它们仍可被 QR 回读，但不再参与“当前计划”的复用；
     * 新计划首次生成后由 `plan` 表接管。`renderMeta` 只对新计划写入。
     */
    val MIGRATION_7_8: Migration = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `plan` (" +
                    "`planId` TEXT NOT NULL, " +
                    "`weekStart` TEXT NOT NULL, " +
                    "`weekEnd` TEXT NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "`status` TEXT NOT NULL, " +
                    "`printedAt` INTEGER, " +
                    "`supersededBy` TEXT, " +
                    "`renderMeta` TEXT NOT NULL, " +
                    "PRIMARY KEY(`planId`))",
            )
        }
    }

    /**
     * v8 → v9：新增来源标签库 `source_tag`（名称唯一）。
     *
     * 旧题目的来源说明（`question.sourcePage`）是自由文本；迁移时去重后种入标签库，
     * 使升级后的录入页仍能单选这些历史来源。空白说明不种入。
     */
    val MIGRATION_8_9: Migration = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `source_tag` (" +
                    "`id` TEXT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`createdAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_source_tag_name` " +
                    "ON `source_tag` (`name`)",
            )
            db.execSQL(
                "INSERT OR IGNORE INTO `source_tag` (`id`, `name`, `createdAt`) " +
                    "SELECT lower(hex(randomblob(16))), trim(`sourcePage`), " +
                    "CAST(strftime('%s', 'now') AS INTEGER) * 1000 " +
                    "FROM `question` WHERE trim(`sourcePage`) <> '' " +
                    "GROUP BY trim(`sourcePage`)",
            )
        }
    }

    /**
     * v9 → v10：取消「修正」与「补填」的区分（2026-11-22 用户要求）。
     *
     * 三张表中的 `isSupplemental` 列被删除；历史 `SUPPLEMENTED` 决策归并为 `CORRECTED`，
     * 其余字段原样保留。SQLite 在 minSdk 26 上不支持 `DROP COLUMN`，因此重建表后回填。
     */
    val MIGRATION_9_10: Migration = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `review_feedback_new` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`knowledgePointId` TEXT NOT NULL, " +
                    "`questionId` TEXT NOT NULL, " +
                    "`planId` TEXT NOT NULL, " +
                    "`pageId` TEXT NOT NULL, " +
                    "`plannedDate` TEXT NOT NULL, " +
                    "`reviewedOn` TEXT NOT NULL, " +
                    "`tier` TEXT NOT NULL, " +
                    "`triggered` INTEGER NOT NULL, " +
                    "`rawOcrText` TEXT, " +
                    "`correctedText` TEXT, " +
                    "`effectiveWeight` REAL NOT NULL, " +
                    "`weightApplied` REAL NOT NULL, " +
                    "`timestamp` INTEGER NOT NULL)",
            )
            db.execSQL(
                "INSERT INTO `review_feedback_new` (" +
                    "`id`, `knowledgePointId`, `questionId`, `planId`, `pageId`, `plannedDate`, " +
                    "`reviewedOn`, `tier`, `triggered`, `rawOcrText`, `correctedText`, " +
                    "`effectiveWeight`, `weightApplied`, `timestamp`) " +
                    "SELECT `id`, `knowledgePointId`, `questionId`, `planId`, `pageId`, `plannedDate`, " +
                    "`reviewedOn`, `tier`, `triggered`, `rawOcrText`, `correctedText`, " +
                    "`effectiveWeight`, `weightApplied`, `timestamp` FROM `review_feedback`",
            )
            db.execSQL("DROP TABLE `review_feedback`")
            db.execSQL("ALTER TABLE `review_feedback_new` RENAME TO `review_feedback`")

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `confirmation_decision_log_new` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`sessionId` TEXT NOT NULL, " +
                    "`planId` TEXT NOT NULL, " +
                    "`pageId` TEXT NOT NULL, " +
                    "`questionId` TEXT NOT NULL, " +
                    "`knowledgePointId` TEXT NOT NULL, " +
                    "`plannedDate` TEXT NOT NULL, " +
                    "`reviewedOn` TEXT, " +
                    "`triggered` INTEGER NOT NULL, " +
                    "`decision` TEXT NOT NULL, " +
                    "`selectedTier` TEXT, " +
                    "`correctedText` TEXT, " +
                    "`decidedAt` INTEGER NOT NULL)",
            )
            db.execSQL(
                "INSERT INTO `confirmation_decision_log_new` (" +
                    "`id`, `sessionId`, `planId`, `pageId`, `questionId`, `knowledgePointId`, " +
                    "`plannedDate`, `reviewedOn`, `triggered`, `decision`, `selectedTier`, " +
                    "`correctedText`, `decidedAt`) " +
                    "SELECT `id`, `sessionId`, `planId`, `pageId`, `questionId`, `knowledgePointId`, " +
                    "`plannedDate`, `reviewedOn`, `triggered`, " +
                    "CASE WHEN `decision` = 'SUPPLEMENTED' THEN 'CORRECTED' ELSE `decision` END, " +
                    "`selectedTier`, `correctedText`, `decidedAt` FROM `confirmation_decision_log`",
            )
            db.execSQL("DROP TABLE `confirmation_decision_log`")
            db.execSQL(
                "ALTER TABLE `confirmation_decision_log_new` RENAME TO `confirmation_decision_log`",
            )

            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `pending_feedback_new` (" +
                    "`sessionId` TEXT NOT NULL, " +
                    "`planId` TEXT NOT NULL, " +
                    "`pageId` TEXT NOT NULL, " +
                    "`plannedDate` TEXT NOT NULL, " +
                    "`actualReviewDate` TEXT, " +
                    "`questionId` TEXT NOT NULL, " +
                    "`knowledgePointId` TEXT NOT NULL, " +
                    "`triggered` INTEGER NOT NULL, " +
                    "`detectedTier` TEXT, " +
                    "`detectionStatus` TEXT NOT NULL, " +
                    "`conflict` INTEGER NOT NULL, " +
                    "`detectedRawOcr` TEXT, " +
                    "`userDecision` TEXT NOT NULL, " +
                    "`selectedTier` TEXT, " +
                    "`correctedText` TEXT, " +
                    "PRIMARY KEY(`sessionId`, `questionId`, `knowledgePointId`))",
            )
            db.execSQL(
                "INSERT INTO `pending_feedback_new` (" +
                    "`sessionId`, `planId`, `pageId`, `plannedDate`, `actualReviewDate`, " +
                    "`questionId`, `knowledgePointId`, `triggered`, `detectedTier`, " +
                    "`detectionStatus`, `conflict`, `detectedRawOcr`, `userDecision`, " +
                    "`selectedTier`, `correctedText`) " +
                    "SELECT `sessionId`, `planId`, `pageId`, `plannedDate`, `actualReviewDate`, " +
                    "`questionId`, `knowledgePointId`, `triggered`, `detectedTier`, " +
                    "`detectionStatus`, `conflict`, `detectedRawOcr`, " +
                    "CASE WHEN `userDecision` = 'SUPPLEMENTED' THEN 'CORRECTED' ELSE `userDecision` END, " +
                    "`selectedTier`, `correctedText` FROM `pending_feedback`",
            )
            db.execSQL("DROP TABLE `pending_feedback`")
            db.execSQL("ALTER TABLE `pending_feedback_new` RENAME TO `pending_feedback`")
        }
    }

    /**
     * v10 → v11：`plan` 增加被推迟题目的快照（题号 + 原因）。
     *
     * 预览页重新打开已生成的计划时据此说明哪些题没有排入；旧计划为空数组。
     */
    val MIGRATION_10_11: Migration = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `plan` ADD COLUMN `deferred` TEXT NOT NULL DEFAULT '[]'")
        }
    }

    /**
     * v11 → v12：新增应用设置单行表 `app_settings`（每日复习时长）。
     *
     * 表内固定写入 id=1、默认 60 分钟的一行，保证升级后导出文件立即包含该字段。
     */
    val MIGRATION_11_12: Migration = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `app_settings` (" +
                    "`id` INTEGER NOT NULL, " +
                    "`dailyReviewMinutes` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "INSERT OR REPLACE INTO `app_settings` (`id`, `dailyReviewMinutes`) VALUES (1, 60)",
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9,
        MIGRATION_9_10,
        MIGRATION_10_11,
        MIGRATION_11_12,
    )
}
