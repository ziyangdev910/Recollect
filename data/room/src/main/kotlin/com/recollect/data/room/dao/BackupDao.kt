package com.recollect.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.recollect.data.room.entity.AppSettings
import com.recollect.data.room.entity.ConfirmationDecisionLog
import com.recollect.data.room.entity.KnowledgeCategory
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
 * 数据库导出用的只读全表查询（backup.md 白名单）。
 *
 * 临时态 `PendingFeedback`、`ScanSession`、`CoolingOverrideEvent` 刻意不在此列出：
 * 未确认决策不得通过备份绕过 R-04，一次性冷却覆盖导入后重新触发即可（C-031 / C-008）。
 * `plan` 与 `source_tag` 是 v8/v9 新增的计划头与来源标签库，恢复计划页与题目来源时需要，
 * 与本文件同步登记到 backup.md。
 */
@Dao
interface BackupDao {

    /** 知识点分类（v13 起纳入备份，随知识点一起恢复）。 */
    @Query("SELECT * FROM knowledge_category ORDER BY createdAt ASC, id ASC")
    suspend fun knowledgeCategories(): List<KnowledgeCategory>

    @Query("SELECT * FROM knowledge_point ORDER BY number ASC")
    suspend fun knowledgePoints(): List<KnowledgePoint>

    @Query("SELECT * FROM question ORDER BY questionNumber ASC, id ASC")
    suspend fun questions(): List<Question>

    @Query("SELECT * FROM question_knowledge_cross_ref ORDER BY questionId ASC, knowledgePointId ASC")
    suspend fun crossRefs(): List<QuestionKnowledgeCrossRef>

    @Query("SELECT * FROM review_feedback ORDER BY timestamp ASC, id ASC")
    suspend fun reviewFeedbacks(): List<ReviewFeedback>

    @Query("SELECT * FROM confirmation_decision_log ORDER BY decidedAt ASC, id ASC")
    suspend fun decisionLogs(): List<ConfirmationDecisionLog>

    @Query("SELECT * FROM plan ORDER BY createdAt ASC, planId ASC")
    suspend fun plans(): List<Plan>

    @Query("SELECT * FROM plan_page ORDER BY planId ASC, pageNumber ASC")
    suspend fun planPages(): List<PlanPage>

    @Query("SELECT * FROM note_attachment_item ORDER BY questionId ASC, `order` ASC, id ASC")
    suspend fun noteAttachments(): List<NoteAttachmentItem>

    @Query("SELECT * FROM knowledge_point_tombstone ORDER BY number ASC")
    suspend fun tombstones(): List<KnowledgePointTombstone>

    @Query("SELECT * FROM knowledge_number_sequence ORDER BY id ASC")
    suspend fun numberSequences(): List<KnowledgeNumberSequence>

    @Query("SELECT * FROM source_tag ORDER BY name COLLATE NOCASE ASC")
    suspend fun sourceTags(): List<SourceTag>

    /** 应用设置（每日复习时长）；v12 起纳入备份。 */
    @Query("SELECT * FROM app_settings ORDER BY id ASC")
    suspend fun appSettings(): List<AppSettings>
}
