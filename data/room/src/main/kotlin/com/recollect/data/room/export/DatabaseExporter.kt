package com.recollect.data.room.export

import com.recollect.data.room.RecollectDatabase
import com.recollect.data.room.dao.BackupDao
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 导出结果：文件名、JSON 正文与各实体行数（备份格式见 backup.md）。
 *
 * 文件名使用 SAF/分享建议名 `recollect-YYYYMMDD-HHmm.json`；JSON 只含白名单实体。
 */
data class BackupDocument(
    val fileName: String,
    val json: String,
    /** JSON 数组键名 → 导出行数，按 `data` 内的固定顺序。 */
    val entityCounts: Map<String, Int>,
    val exportedAt: Instant,
) {
    val sizeBytes: Int
        get() = json.toByteArray(Charsets.UTF_8).size
}

/** 数据库导出端口：`feature:backup` 只依赖本接口，不接触 Room（R-03）。 */
interface DatabaseExporter {
    suspend fun export(): BackupDocument
}

/**
 * 从 Room 读取白名单实体并生成备份 JSON（backup.md「导出」）。
 *
 * 只读：不写任何表；不导出 `PendingFeedback` / `ScanSession` / `CoolingOverrideEvent`。
 * `backupFormatVersion` 固定为 1（当前唯一版本，迁移表为空）；
 * `roomSchemaVersion` 跟随 [RecollectDatabase.SCHEMA_VERSION]，供导入端做字段级降级判断。
 */
class RoomDatabaseExporter(
    private val backupDao: BackupDao,
    private val appVersionName: String,
    private val clock: Clock,
) : DatabaseExporter {

    override suspend fun export(): BackupDocument = withContext(Dispatchers.IO) {
        val exportedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS)
        val knowledgeCategories = backupDao.knowledgeCategories()
        val knowledgePoints = backupDao.knowledgePoints()
        val questions = backupDao.questions()
        val crossRefs = backupDao.crossRefs()
        val reviewFeedbacks = backupDao.reviewFeedbacks()
        val decisionLogs = backupDao.decisionLogs()
        val plans = backupDao.plans()
        val planPages = backupDao.planPages()
        val noteAttachments = backupDao.noteAttachments()
        val tombstones = backupDao.tombstones()
        val numberSequences = backupDao.numberSequences()
        val sourceTags = backupDao.sourceTags()
        // v12：即使旧库没有设置行，也导出默认行，保证备份始终含「每日复习时长」。
        val appSettings = backupDao.appSettings().ifEmpty {
            listOf(com.recollect.data.room.entity.AppSettings())
        }

        val counts = linkedMapOf(
            KEY_KNOWLEDGE_CATEGORIES to knowledgeCategories.size,
            KEY_KNOWLEDGE_POINTS to knowledgePoints.size,
            KEY_QUESTIONS to questions.size,
            KEY_CROSS_REFS to crossRefs.size,
            KEY_REVIEW_FEEDBACKS to reviewFeedbacks.size,
            KEY_DECISION_LOGS to decisionLogs.size,
            KEY_PLANS to plans.size,
            KEY_PLAN_PAGES to planPages.size,
            KEY_NOTE_ATTACHMENTS to noteAttachments.size,
            KEY_TOMBSTONES to tombstones.size,
            KEY_NUMBER_SEQUENCES to numberSequences.size,
            KEY_SOURCE_TAGS to sourceTags.size,
            KEY_APP_SETTINGS to appSettings.size,
        )

        val data = jsonObject(
            KEY_KNOWLEDGE_CATEGORIES to knowledgeCategories.map { it.toJson() }.asJsonArray(),
            KEY_KNOWLEDGE_POINTS to knowledgePoints.map { it.toJson() }.asJsonArray(),
            KEY_QUESTIONS to questions.map { it.toJson() }.asJsonArray(),
            KEY_CROSS_REFS to crossRefs.map { it.toJson() }.asJsonArray(),
            KEY_REVIEW_FEEDBACKS to reviewFeedbacks.map { it.toJson() }.asJsonArray(),
            KEY_DECISION_LOGS to decisionLogs.map { it.toJson() }.asJsonArray(),
            KEY_PLANS to plans.map { it.toJson() }.asJsonArray(),
            KEY_PLAN_PAGES to planPages.map { it.toJson() }.asJsonArray(),
            KEY_NOTE_ATTACHMENTS to noteAttachments.map { it.toJson() }.asJsonArray(),
            KEY_TOMBSTONES to tombstones.map { it.toJson() }.asJsonArray(),
            KEY_NUMBER_SEQUENCES to numberSequences.map { it.toJson() }.asJsonArray(),
            KEY_SOURCE_TAGS to sourceTags.map { it.toJson() }.asJsonArray(),
            KEY_APP_SETTINGS to appSettings.map { it.toJson() }.asJsonArray(),
        )

        val envelope = jsonObject(
            "backupFormatVersion" to JsonValue.Num(BACKUP_FORMAT_VERSION),
            "roomSchemaVersion" to JsonValue.Num(RecollectDatabase.SCHEMA_VERSION),
            "appVersionName" to appVersionName.asJsonString(),
            "exportedAt" to exportedAt.asJsonInstant(),
            "data" to data,
        )

        val stamp = exportedAt.atZone(clock.zone).format(FILE_NAME_STAMP)
        BackupDocument(
            fileName = "recollect-$stamp.json",
            json = envelope.render(),
            entityCounts = counts,
            exportedAt = exportedAt,
        )
    }

    private fun com.recollect.data.room.entity.KnowledgeCategory.toJson() = jsonObject(
        "id" to id.asJsonString(),
        "name" to name.asJsonString(),
        "createdAt" to createdAt.asJsonInstant(),
    )

    private fun com.recollect.data.room.entity.KnowledgePoint.toJson() = jsonObject(
        "id" to id.asJsonString(),
        "number" to number.asJsonNumber(),
        "name" to name.asJsonString(),
        "currentTier" to currentTier.name.asJsonString(),
        "continuousScore" to continuousScore.asJsonNumber(),
        "effectiveFeedbackCount" to effectiveFeedbackCount.asJsonNumber(),
        "nextDueDate" to nextDueDate.asJsonDate(),
        "activeQuestionCount" to activeQuestionCount.asJsonNumber(),
        "dormant" to dormant.asJsonBool(),
        "difficulty" to difficulty.asJsonNumber(),
        "halfLifeDays" to halfLifeDays.asJsonNumber(),
        "createdAt" to createdAt.asJsonInstant(),
        "categoryId" to categoryId.asJsonString(),
        "updatedAt" to updatedAt.asJsonInstant(),
    )

    private fun com.recollect.data.room.entity.Question.toJson() = jsonObject(
        "id" to id.asJsonString(),
        "pageId" to pageId.asJsonString(),
        "questionNumber" to questionNumber.asJsonString(),
        "qrPayload" to qrPayload.asJsonString(),
        "archived" to archived.asJsonBool(),
        "lastScheduledDate" to lastScheduledDate.asJsonDate(),
        "lastReviewedDate" to lastReviewedDate.asJsonDate(),
        "estimatedReviewMinutes" to estimatedReviewMinutes.asJsonNumber(),
        "createdAt" to createdAt.asJsonInstant(),
        "archivedAt" to archivedAt.asJsonInstant(),
        "sourcePage" to sourcePage.asJsonString(),
        "sourcePageCode" to sourcePageCode.asJsonNumber(),
        "updatedAt" to updatedAt.asJsonInstant(),
    )

    private fun com.recollect.data.room.entity.QuestionKnowledgeCrossRef.toJson() = jsonObject(
        "questionId" to questionId.asJsonString(),
        "knowledgePointId" to knowledgePointId.asJsonString(),
        "weight" to weight.asJsonNumber(),
    )

    private fun com.recollect.data.room.entity.ReviewFeedback.toJson() = jsonObject(
        "id" to id.asJsonNumber(),
        "knowledgePointId" to knowledgePointId.asJsonString(),
        "questionId" to questionId.asJsonString(),
        "planId" to planId.asJsonString(),
        "pageId" to pageId.asJsonString(),
        "plannedDate" to plannedDate.asJsonDate(),
        "reviewedOn" to reviewedOn.asJsonDate(),
        "tier" to tier.name.asJsonString(),
        "triggered" to triggered.asJsonBool(),
        "rawOcrText" to rawOcrText.asJsonString(),
        "correctedText" to correctedText.asJsonString(),
        "effectiveWeight" to effectiveWeight.asJsonNumber(),
        "weightApplied" to weightApplied.asJsonNumber(),
        "timestamp" to timestamp.asJsonInstant(),
    )

    private fun com.recollect.data.room.entity.ConfirmationDecisionLog.toJson() = jsonObject(
        "id" to id.asJsonNumber(),
        "sessionId" to sessionId.asJsonString(),
        "planId" to planId.asJsonString(),
        "pageId" to pageId.asJsonString(),
        "questionId" to questionId.asJsonString(),
        "knowledgePointId" to knowledgePointId.asJsonString(),
        "plannedDate" to plannedDate.asJsonDate(),
        "reviewedOn" to reviewedOn.asJsonDate(),
        "triggered" to triggered.asJsonBool(),
        "decision" to decision.name.asJsonString(),
        "selectedTier" to selectedTier?.name.asJsonString(),
        "correctedText" to correctedText.asJsonString(),
        "decidedAt" to decidedAt.asJsonInstant(),
    )

    private fun com.recollect.data.room.entity.Plan.toJson() = jsonObject(
        "planId" to planId.asJsonString(),
        "weekStart" to weekStart.asJsonDate(),
        "weekEnd" to weekEnd.asJsonDate(),
        "createdAt" to createdAt.asJsonInstant(),
        "status" to status.asJsonString(),
        "printedAt" to printedAt.asJsonInstant(),
        "supersededBy" to supersededBy.asJsonString(),
        "renderMeta" to renderMeta.asJsonString(),
        "deferred" to deferred.asJsonString(),
    )

    private fun com.recollect.data.room.entity.PlanPage.toJson() = jsonObject(
        "planId" to planId.asJsonString(),
        "pageId" to pageId.asJsonString(),
        "pageNumber" to pageNumber.asJsonNumber(),
        "date" to date.asJsonDate(),
        "itemCount" to itemCount.asJsonNumber(),
        "questionIds" to questionIds.asJsonString(),
        "knowledgeRows" to knowledgeRows.asJsonString(),
        "continuationMarkers" to continuationMarkers.asJsonString(),
        "itemGeometry" to itemGeometry.asJsonString(),
    )

    private fun com.recollect.data.room.entity.NoteAttachmentItem.toJson() = jsonObject(
        "id" to id.asJsonNumber(),
        "questionId" to questionId.asJsonString(),
        "sourceFeedbackId" to sourceFeedbackId.asJsonNumber(),
        "text" to text.asJsonString(),
        "imageUri" to imageUri.asJsonString(),
        "order" to order.asJsonNumber(),
        "createdAt" to createdAt.asJsonInstant(),
    )

    private fun com.recollect.data.room.entity.KnowledgePointTombstone.toJson() = jsonObject(
        "number" to number.asJsonNumber(),
        "knowledgePointId" to knowledgePointId.asJsonString(),
        "voidedAt" to voidedAt.asJsonInstant(),
        "reason" to reason.asJsonString(),
    )

    private fun com.recollect.data.room.entity.KnowledgeNumberSequence.toJson() = jsonObject(
        "id" to id.asJsonNumber(),
        "lastIssued" to lastIssued.asJsonNumber(),
    )

    private fun com.recollect.data.room.entity.SourceTag.toJson() = jsonObject(
        "id" to id.asJsonString(),
        "name" to name.asJsonString(),
        "createdAt" to createdAt.asJsonInstant(),
    )

    private fun com.recollect.data.room.entity.AppSettings.toJson() = jsonObject(
        "id" to id.asJsonNumber(),
        "dailyReviewMinutes" to dailyReviewMinutes.asJsonNumber(),
    )

    private fun java.time.LocalDate?.asJsonDate(): JsonValue = this?.toString().asJsonString()

    private fun Instant?.asJsonInstant(): JsonValue =
        this?.truncatedTo(ChronoUnit.SECONDS)?.toString().asJsonString()

    companion object {
        const val BACKUP_FORMAT_VERSION = 1

        const val KEY_KNOWLEDGE_CATEGORIES = "knowledgeCategories"
        const val KEY_KNOWLEDGE_POINTS = "knowledgePoints"
        const val KEY_QUESTIONS = "questions"
        const val KEY_CROSS_REFS = "questionKnowledgeCrossRefs"
        const val KEY_REVIEW_FEEDBACKS = "reviewFeedbacks"
        const val KEY_DECISION_LOGS = "confirmationDecisionLogs"
        const val KEY_PLANS = "plans"
        const val KEY_PLAN_PAGES = "planPages"
        const val KEY_NOTE_ATTACHMENTS = "noteAttachments"
        const val KEY_TOMBSTONES = "knowledgePointTombstones"
        const val KEY_NUMBER_SEQUENCES = "knowledgeNumberSequences"
        const val KEY_SOURCE_TAGS = "sourceTags"
        const val KEY_APP_SETTINGS = "appSettings"

        private val FILE_NAME_STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")
    }
}
