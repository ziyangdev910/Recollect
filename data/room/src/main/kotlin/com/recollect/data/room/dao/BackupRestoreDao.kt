package com.recollect.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.recollect.data.room.entity.ConfirmationDecisionLog
import com.recollect.data.room.entity.KnowledgeNumberSequence
import com.recollect.data.room.entity.KnowledgePoint
import com.recollect.data.room.entity.KnowledgePointTombstone
import com.recollect.data.room.entity.NoteAttachmentItem
import com.recollect.data.room.entity.Plan
import com.recollect.data.room.entity.PlanPage
import com.recollect.data.room.entity.Question
import com.recollect.data.room.entity.QuestionKnowledgeCrossRef
import com.recollect.data.room.entity.ReviewFeedback
import com.recollect.data.room.entity.SourceTag

/**
 * 导入（覆盖式恢复）的写入口（backup.md「替换式导入」）。
 *
 * 所有删除与插入都由 `RoomDatabaseImporter` 放进同一个事务：任一步失败整体回滚。
 * 除白名单表外，还清空 `PendingFeedback`、`ScanSession`、`CoolingOverrideEvent`，
 * 避免导入后残留旧会话的未确认行或旧冷却覆盖事件。
 */
@Dao
interface BackupRestoreDao {

    @Query("DELETE FROM knowledge_point")
    suspend fun clearKnowledgePoints()

    @Query("DELETE FROM question")
    suspend fun clearQuestions()

    @Query("DELETE FROM question_knowledge_cross_ref")
    suspend fun clearCrossRefs()

    @Query("DELETE FROM review_feedback")
    suspend fun clearReviewFeedbacks()

    @Query("DELETE FROM confirmation_decision_log")
    suspend fun clearDecisionLogs()

    @Query("DELETE FROM plan")
    suspend fun clearPlans()

    @Query("DELETE FROM plan_page")
    suspend fun clearPlanPages()

    @Query("DELETE FROM note_attachment_item")
    suspend fun clearNoteAttachments()

    @Query("DELETE FROM knowledge_point_tombstone")
    suspend fun clearTombstones()

    @Query("DELETE FROM knowledge_number_sequence")
    suspend fun clearNumberSequences()

    @Query("DELETE FROM source_tag")
    suspend fun clearSourceTags()

    @Query("DELETE FROM pending_feedback")
    suspend fun clearPendingFeedbacks()

    @Query("DELETE FROM scan_session")
    suspend fun clearScanSessions()

    @Query("DELETE FROM cooling_override_event")
    suspend fun clearCoolingEvents()

    @Insert
    suspend fun insertKnowledgePoints(rows: List<KnowledgePoint>)

    @Insert
    suspend fun insertQuestions(rows: List<Question>)

    @Insert
    suspend fun insertCrossRefs(rows: List<QuestionKnowledgeCrossRef>)

    @Insert
    suspend fun insertReviewFeedbacks(rows: List<ReviewFeedback>)

    @Insert
    suspend fun insertDecisionLogs(rows: List<ConfirmationDecisionLog>)

    @Insert
    suspend fun insertPlans(rows: List<Plan>)

    @Insert
    suspend fun insertPlanPages(rows: List<PlanPage>)

    @Insert
    suspend fun insertNoteAttachments(rows: List<NoteAttachmentItem>)

    @Insert
    suspend fun insertTombstones(rows: List<KnowledgePointTombstone>)

    @Insert
    suspend fun insertNumberSequences(rows: List<KnowledgeNumberSequence>)

    @Insert
    suspend fun insertSourceTags(rows: List<SourceTag>)
}
