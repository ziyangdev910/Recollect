package com.recollect.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.recollect.data.room.dao.AppSettingsDao
import com.recollect.data.room.dao.BackupDao
import com.recollect.data.room.dao.BackupRestoreDao
import com.recollect.data.room.dao.ConfirmationDecisionLogDao
import com.recollect.data.room.dao.CoolingOverrideEventDao
import com.recollect.data.room.dao.KnowledgeCategoryDao
import com.recollect.data.room.dao.KnowledgeNumberSequenceDao
import com.recollect.data.room.dao.NoteAttachmentItemDao
import com.recollect.data.room.dao.KnowledgePointDao
import com.recollect.data.room.dao.KnowledgePointTombstoneDao
import com.recollect.data.room.dao.PendingFeedbackDao
import com.recollect.data.room.dao.PlanDao
import com.recollect.data.room.dao.PlanPageDao
import com.recollect.data.room.dao.QuestionDao
import com.recollect.data.room.dao.QuestionKnowledgeCrossRefDao
import com.recollect.data.room.dao.ReviewFeedbackDao
import com.recollect.data.room.dao.ScanSessionDao
import com.recollect.data.room.dao.SourceTagDao
import com.recollect.data.room.entity.AppSettings
import com.recollect.data.room.entity.ConfirmationDecisionLog
import com.recollect.data.room.entity.CoolingOverrideEvent
import com.recollect.data.room.entity.KnowledgeCategory
import com.recollect.data.room.entity.KnowledgeCategoryDefaults
import com.recollect.data.room.entity.KnowledgeNumberSequence
import com.recollect.data.room.entity.KnowledgePoint
import com.recollect.data.room.entity.KnowledgePointTombstone
import com.recollect.data.room.entity.NoteAttachmentItem
import com.recollect.data.room.entity.PendingFeedback
import com.recollect.data.room.entity.Plan
import com.recollect.data.room.entity.PlanPage
import com.recollect.data.room.entity.Question
import com.recollect.data.room.entity.QuestionKnowledgeCrossRef
import com.recollect.data.room.entity.ReviewFeedback
import com.recollect.data.room.entity.ScanSession
import com.recollect.data.room.entity.SourceTag
import com.recollect.data.room.migration.RecollectMigrations

/**
 * 阶段 4.1 的 Room 数据库。
 *
 * 阶段 4.2 起登记 10 个实体；v5 增加标签库编号序列与墓碑
 * （`KnowledgeNumberSequence`、`KnowledgePointTombstone`，D-12 / C-028）；
 * v6 为题目来源页码增加编号 `sourcePageCode`；v7 取消其唯一索引，允许同一页的多道题共用编号；
 * v8 增加 `plan` 计划头（本周计划单例与打印状态），避免再次生成时静默替换当前计划；
 * v9 增加 `source_tag` 来源标签库，并把已有题目的来源说明去重后作为初始标签；
 * v10 删除 `isSupplemental`，取消「修正」与「补填」的区分；
 * v11 给 `plan` 增加被推迟题目的快照（题号 + 原因）；
 * v12 增加 `app_settings` 单行表，持久化「每日复习时长」并纳入备份；
 * v13 增加知识点分类 `knowledge_category`，并给知识点增加 `categoryId`，
 * 旧知识点统一归入「语文」，空库创建时种入 11 个默认分类（2026-11-23 用户要求）；
 * v14 给题目与知识点增加 `updatedAt`（用户修改时间），题库与知识点列表据此倒序展示
 * （2026-11-24 用户要求）。旧数据的 `updatedAt` 回填为 `createdAt`。
 * 不允许 destructive migration。
 */
@Database(
    entities = [
        KnowledgePoint::class,
        Question::class,
        QuestionKnowledgeCrossRef::class,
        ReviewFeedback::class,
        ConfirmationDecisionLog::class,
        PendingFeedback::class,
        ScanSession::class,
        CoolingOverrideEvent::class,
        PlanPage::class,
        NoteAttachmentItem::class,
        KnowledgePointTombstone::class,
        KnowledgeNumberSequence::class,
        Plan::class,
        SourceTag::class,
        AppSettings::class,
        KnowledgeCategory::class,
    ],
    version = RecollectDatabase.SCHEMA_VERSION,
    exportSchema = true,
)
@TypeConverters(RoomTypeConverters::class)
abstract class RecollectDatabase : RoomDatabase() {

    /** 备份导出的只读全表入口（backup.md 白名单）。 */
    abstract fun backupDao(): BackupDao

    /** 备份导入的清空与批量写入入口（覆盖式导入，单事务由 `RoomDatabaseImporter` 保证）。 */
    abstract fun backupRestoreDao(): BackupRestoreDao

    abstract fun knowledgePointDao(): KnowledgePointDao

    /** 知识点分类（schema v13）。 */
    abstract fun knowledgeCategoryDao(): KnowledgeCategoryDao

    abstract fun knowledgePointTombstoneDao(): KnowledgePointTombstoneDao

    abstract fun knowledgeNumberSequenceDao(): KnowledgeNumberSequenceDao

    abstract fun questionDao(): QuestionDao

    abstract fun questionKnowledgeCrossRefDao(): QuestionKnowledgeCrossRefDao

    abstract fun reviewFeedbackDao(): ReviewFeedbackDao

    abstract fun confirmationDecisionLogDao(): ConfirmationDecisionLogDao

    abstract fun pendingFeedbackDao(): PendingFeedbackDao

    abstract fun scanSessionDao(): ScanSessionDao

    abstract fun coolingOverrideEventDao(): CoolingOverrideEventDao

    abstract fun planPageDao(): PlanPageDao

    abstract fun planDao(): PlanDao

    abstract fun sourceTagDao(): SourceTagDao

    abstract fun noteAttachmentItemDao(): NoteAttachmentItemDao

    /** 应用设置单行表（每日复习时长，schema v12）。 */
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        /** Room schema 版本；备份 JSON 的 `roomSchemaVersion` 直接取此值（backup.md）。 */
        const val SCHEMA_VERSION = 14

        const val DATABASE_NAME = "recollect.db"

        fun build(context: Context): RecollectDatabase =
            Room.databaseBuilder(context, RecollectDatabase::class.java, DATABASE_NAME)
                .addMigrations(*RecollectMigrations.ALL)
                .addCallback(SeedDefaultCategoriesCallback)
                .build()
    }
}

/**
 * 空库创建时种入默认分类（v13）。
 *
 * 升级路径由 `MIGRATION_11_12` 之后的 `MIGRATION_12_13` 种入；这里只负责全新安装。
 * `createdAt` 递增 1 毫秒，保证展示顺序与 [KnowledgeCategoryDefaults.NAMES] 一致。
 */
private object SeedDefaultCategoriesCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        val base = System.currentTimeMillis()
        KnowledgeCategoryDefaults.NAMES.forEachIndexed { index, (id, name) ->
            db.execSQL(
                "INSERT OR IGNORE INTO `knowledge_category` (`id`, `name`, `createdAt`) " +
                    "VALUES (?, ?, ?)",
                arrayOf(id, name, base + index),
            )
        }
    }
}
