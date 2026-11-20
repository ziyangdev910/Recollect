package com.recollect.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.recollect.data.room.dao.ConfirmationDecisionLogDao
import com.recollect.data.room.dao.CoolingOverrideEventDao
import com.recollect.data.room.dao.KnowledgeNumberSequenceDao
import com.recollect.data.room.dao.NoteAttachmentItemDao
import com.recollect.data.room.dao.KnowledgePointDao
import com.recollect.data.room.dao.KnowledgePointTombstoneDao
import com.recollect.data.room.dao.PendingFeedbackDao
import com.recollect.data.room.dao.PlanPageDao
import com.recollect.data.room.dao.QuestionDao
import com.recollect.data.room.dao.QuestionKnowledgeCrossRefDao
import com.recollect.data.room.dao.ReviewFeedbackDao
import com.recollect.data.room.dao.ScanSessionDao
import com.recollect.data.room.entity.ConfirmationDecisionLog
import com.recollect.data.room.entity.CoolingOverrideEvent
import com.recollect.data.room.entity.KnowledgeNumberSequence
import com.recollect.data.room.entity.KnowledgePoint
import com.recollect.data.room.entity.KnowledgePointTombstone
import com.recollect.data.room.entity.NoteAttachmentItem
import com.recollect.data.room.entity.PendingFeedback
import com.recollect.data.room.entity.PlanPage
import com.recollect.data.room.entity.Question
import com.recollect.data.room.entity.QuestionKnowledgeCrossRef
import com.recollect.data.room.entity.ReviewFeedback
import com.recollect.data.room.entity.ScanSession
import com.recollect.data.room.migration.RecollectMigrations

/**
 * 阶段 4.1 的 Room 数据库。
 *
 * 阶段 4.2 起登记 10 个实体；v5 增加标签库编号序列与墓碑
 * （`KnowledgeNumberSequence`、`KnowledgePointTombstone`，D-12 / C-028）；
 * v6 为题目来源页码增加编号 `sourcePageCode`；v7 取消其唯一索引，允许同一页的多道题共用编号。
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
    ],
    version = 7,
    exportSchema = true,
)
@TypeConverters(RoomTypeConverters::class)
abstract class RecollectDatabase : RoomDatabase() {

    abstract fun knowledgePointDao(): KnowledgePointDao

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

    abstract fun noteAttachmentItemDao(): NoteAttachmentItemDao

    companion object {
        const val DATABASE_NAME = "recollect.db"

        fun build(context: Context): RecollectDatabase =
            Room.databaseBuilder(context, RecollectDatabase::class.java, DATABASE_NAME)
                .addMigrations(*RecollectMigrations.ALL)
                .build()
    }
}
