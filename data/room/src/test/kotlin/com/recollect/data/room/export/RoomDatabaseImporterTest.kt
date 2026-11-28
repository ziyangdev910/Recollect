package com.recollect.data.room.export

import com.recollect.data.room.dao.BackupRestoreDao
import com.recollect.data.room.entity.AppSettings
import com.recollect.data.room.entity.ConfirmationDecisionLog
import com.recollect.data.room.entity.KnowledgeCategory
import com.recollect.data.room.entity.KnowledgeCategoryDefaults
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
import com.recollect.data.room.repository.TransactionRunner
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * 导入器单元测试（backup.md「替换式导入」与 B-01 降级矩阵）：
 * 合法性校验、覆盖式写入、编号校验与跨 schema 降级。
 */
class RoomDatabaseImporterTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-22T08:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `合法备份通过校验并给出摘要`() = runBlocking {
        val importer = importer(FakeRestoreDao())

        val result = importer.validate(backupJson())

        val valid = assertIs<ImportValidation.Valid>(result)
        assertEquals(2, valid.summary.entityCounts[RoomDatabaseExporter.KEY_KNOWLEDGE_CATEGORIES])
        assertEquals(1, valid.summary.entityCounts[RoomDatabaseExporter.KEY_KNOWLEDGE_POINTS])
        assertEquals(1, valid.summary.entityCounts[RoomDatabaseExporter.KEY_QUESTIONS])
        assertEquals(1, valid.summary.entityCounts[RoomDatabaseExporter.KEY_CROSS_REFS])
        assertTrue(!valid.summary.degraded)
    }

    @Test
    fun `非法 JSON 与不受支持的版本被拒绝`() = runBlocking {
        val importer = importer(FakeRestoreDao())

        assertIs<ImportValidation.Invalid>(importer.validate("{ not json"))
        val higherVersion = backupJson().replace("\"backupFormatVersion\": 1", "\"backupFormatVersion\": 2")
        assertIs<ImportValidation.Invalid>(importer.validate(higherVersion))
    }

    @Test
    fun `缺少必填字段被拒绝`() = runBlocking {
        val importer = importer(FakeRestoreDao())
        val broken = backupJson().replace("\"questionNumber\": \"3-1\",", "")

        val result = importer.validate(broken)

        val invalid = assertIs<ImportValidation.Invalid>(result)
        assertTrue(invalid.message.contains("questionNumber"), invalid.message)
    }

    @Test
    fun `编号与墓碑冲突被拒绝`() = runBlocking {
        val importer = importer(FakeRestoreDao())
        // 知识点编号 3 改成与墓碑 2 相同。
        val conflict = backupJson().replace("\"number\": 3,", "\"number\": 2,")

        val result = importer.validate(conflict)

        val invalid = assertIs<ImportValidation.Invalid>(result)
        assertTrue(invalid.message.contains("墓碑冲突"), invalid.message)
    }

    @Test
    fun `覆盖导入先清空再写入`() = runBlocking {
        val dao = FakeRestoreDao()
        val importer = importer(dao)

        val result = importer.apply(backupJson())

        assertIs<ImportResult.Success>(result)
        assertTrue(dao.cleared)
        assertEquals(2, dao.knowledgeCategories.size)
        assertEquals("cat-chinese", dao.knowledgePoints.single().categoryId)
        assertEquals(1, dao.knowledgePoints.size)
        assertEquals(1, dao.questions.size)
        assertEquals(1, dao.crossRefs.size)
        assertEquals(1, dao.sourceTags.size)
        assertEquals(1, dao.numberSequences.size)
        // 每日复习时长随备份恢复。
        assertEquals(45, dao.appSettings.single().dailyReviewMinutes)
        // 编号序列按恢复后的知识点编号校正。
        assertEquals(4, dao.numberSequences.single().lastIssued)
    }

    @Test
    fun `跨 schema 备份按 B-01 降级`() = runBlocking {
        val dao = FakeRestoreDao()
        val importer = importer(dao)
        val oldSchema = backupJson(schemaVersion = 9, withHistory = true)

        val validation = importer.validate(oldSchema)
        val valid = assertIs<ImportValidation.Valid>(validation)
        assertTrue(valid.summary.degraded)

        val result = importer.apply(oldSchema)
        val success = assertIs<ImportResult.Success>(result)
        assertTrue(success.summary.degraded)

        assertEquals(0, dao.reviewFeedbacks.size)
        assertEquals(0, dao.decisionLogs.size)
        assertEquals(0, dao.plans.size)
        assertEquals(0, dao.planPages.size)
        val point = dao.knowledgePoints.single()
        assertEquals("UNKNOWN", point.currentTier.name)
        assertEquals(0f, point.continuousScore)
        assertEquals(0f, point.effectiveFeedbackCount)
        assertEquals(null, point.difficulty)
        // 2026-11-22 + 1 天。
        assertEquals("2026-11-23", point.nextDueDate.toString())
        assertEquals("", dao.questions.single().pageId)
        assertEquals("", dao.questions.single().qrPayload)
        assertEquals(null, dao.questions.single().lastScheduledDate)
        assertEquals(null, dao.noteAttachments.single().sourceFeedbackId)
        // v14 之前的备份没有 updatedAt，回退为 createdAt。
        assertEquals(point.createdAt, point.updatedAt)
        assertEquals(dao.questions.single().createdAt, dao.questions.single().updatedAt)
    }

    @Test
    fun `旧备份没有分类时补默认分类并归入语文`() = runBlocking {
        val dao = FakeRestoreDao()
        val importer = importer(dao)

        val result = importer.apply(backupJson(schemaVersion = 12, withCategories = false))

        assertIs<ImportResult.Success>(result)
        assertEquals(
            KnowledgeCategoryDefaults.NAMES.size,
            dao.knowledgeCategories.size,
            "旧备份没有分类时种入全部默认分类",
        )
        assertEquals(
            KnowledgeCategoryDefaults.DEFAULT_CATEGORY_ID,
            dao.knowledgePoints.single().categoryId,
        )
    }

    @Test
    fun `悬空关联被跳过而不产生半恢复引用`() = runBlocking {
        val dao = FakeRestoreDao()
        val importer = importer(dao)
        val dangling = backupJson().replace(
            "\"knowledgePointId\": \"kp-1\"",
            "\"knowledgePointId\": \"kp-missing\"",
        )

        val result = importer.apply(dangling)

        val success = assertIs<ImportResult.Success>(result)
        assertEquals(0, dao.crossRefs.size)
        assertEquals(1, success.summary.skippedRows.size)
    }

    private fun importer(dao: BackupRestoreDao) = RoomDatabaseImporter(
        restoreDao = dao,
        transactionRunner = object : TransactionRunner {
            override suspend fun <T> inTransaction(block: suspend () -> T): T = block()
        },
        clock = clock,
    )

    private fun backupJson(
        schemaVersion: Int = 17,
        withHistory: Boolean = false,
        withCategories: Boolean = true,
    ): String {
        val categories = if (withCategories) {
            """
            [
              { "id": "cat-chinese", "name": "语文", "createdAt": "2026-11-01T00:00:00Z" },
              { "id": "cat-math", "name": "数学", "createdAt": "2026-11-01T00:00:01Z" }
            ]
            """
        } else {
            "[]"
        }
        val categoryField = if (withCategories) ",\n                \"categoryId\": \"cat-chinese\"" else ""
        val reviewFeedback = if (withHistory) {
            """
            {
              "id": 1,
              "knowledgePointId": "kp-1",
              "questionId": "q-1",
              "planId": "ABCDEF",
              "pageId": "ABCDEF-01",
              "plannedDate": "2026-11-21",
              "reviewedOn": "2026-11-21",
              "tier": "MASTERED",
              "triggered": true,
              "rawOcrText": null,
              "correctedText": null,
              "effectiveWeight": 1.0,
              "weightApplied": 1.0,
              "timestamp": "2026-11-21T12:00:00Z"
            }
            """
        } else {
            ""
        }
        val decisionLog = if (withHistory) {
            """
            {
              "id": 1,
              "sessionId": "s-1",
              "planId": "ABCDEF",
              "pageId": "ABCDEF-01",
              "questionId": "q-1",
              "knowledgePointId": "kp-1",
              "plannedDate": "2026-11-21",
              "reviewedOn": null,
              "triggered": true,
              "decision": "NOT_REVIEWED",
              "selectedTier": null,
              "correctedText": null,
              "decidedAt": "2026-11-21T12:00:01Z"
            }
            """
        } else {
            ""
        }
        val plan = if (withHistory) {
            """
            {
              "planId": "ABCDEF",
              "weekStart": "2026-11-16",
              "weekEnd": "2026-11-22",
              "createdAt": "2026-11-16T00:00:00Z",
              "status": "PRINTED",
              "printedAt": "2026-11-16T08:00:00Z",
              "supersededBy": null,
              "renderMeta": "[]",
              "dailyMinutes": 45
            }
            """
        } else {
            ""
        }
        val planPage = if (withHistory) {
            """
            {
              "planId": "ABCDEF",
              "pageId": "ABCDEF-01",
              "pageNumber": 1,
              "date": "2026-11-21",
              "itemCount": 1,
              "questionIds": "[\"q-1\"]",
              "knowledgeRows": "[]",
              "continuationMarkers": "[]",
              "itemGeometry": "[]"
            }
            """
        } else {
            ""
        }
        return """
        {
          "backupFormatVersion": 1,
          "roomSchemaVersion": $schemaVersion,
          "appVersionName": "0.1.0",
          "exportedAt": "2026-11-22T08:00:00Z",
          "data": {
            "knowledgeCategories": $categories,
            "knowledgePoints": [
              {
                "id": "kp-1",
                "number": 3,
                "name": "二次函数",
                "currentTier": "FAMILIAR",
                "continuousScore": 0.6,
                "effectiveFeedbackCount": 3.0,
                "nextDueDate": "2026-11-25",
                "activeQuestionCount": 1,
                "dormant": false,
                "difficulty": 2,
                "halfLifeDays": 10.0,
                "createdAt": "2026-11-20T10:00:00Z"$categoryField
              }
            ],
            "questions": [
              {
                "id": "q-1",
                "pageId": "ABCDEF-01",
                "questionNumber": "3-1",
                "qrPayload": "ABCDEF-01|q-1|CRC",
                "archived": false,
                "lastScheduledDate": "2026-11-21",
                "lastReviewedDate": "2026-11-21",
                "estimatedReviewMinutes": 12,
                "createdAt": "2026-11-20T10:00:01Z",
                "archivedAt": null,
                "sourcePage": "作业本",
                "sourcePageCode": 3
              }
            ],
            "questionKnowledgeCrossRefs": [
              { "questionId": "q-1", "knowledgePointId": "kp-1", "weight": 1.0 }
            ],
            "reviewFeedbacks": [$reviewFeedback],
            "confirmationDecisionLogs": [$decisionLog],
            "plans": [$plan],
            "planPages": [$planPage],
            "noteAttachments": [
              {
                "id": 1,
                "questionId": "q-1",
                "sourceFeedbackId": 1,
                "text": "笔记",
                "imageUri": null,
                "order": 0,
                "createdAt": "2026-11-21T12:00:02Z"
              }
            ],
            "knowledgePointTombstones": [
              {
                "number": 2,
                "knowledgePointId": "kp-deleted",
                "voidedAt": "2026-11-19T00:00:00Z",
                "reason": "合并"
              }
            ],
            "knowledgeNumberSequences": [ { "id": 1, "lastIssued": 4 } ],
            "sourceTags": [
              { "id": "tag-1", "name": "作业本", "createdAt": "2026-11-18T00:00:00Z" }
            ],
            "appSettings": [
              { "id": 1, "dailyReviewMinutes": 45 }
            ]
          }
        }
        """.trimIndent()
    }
}

/** 记录清空/写入的导入 DAO 测试替身；不依赖 Room。 */
private class FakeRestoreDao : BackupRestoreDao {
    var cleared = false
    var knowledgeCategories: List<KnowledgeCategory> = emptyList()
    var knowledgePoints: List<KnowledgePoint> = emptyList()
    var questions: List<Question> = emptyList()
    var crossRefs: List<QuestionKnowledgeCrossRef> = emptyList()
    var reviewFeedbacks: List<ReviewFeedback> = emptyList()
    var decisionLogs: List<ConfirmationDecisionLog> = emptyList()
    var plans: List<Plan> = emptyList()
    var planPages: List<PlanPage> = emptyList()
    var noteAttachments: List<NoteAttachmentItem> = emptyList()
    var tombstones: List<KnowledgePointTombstone> = emptyList()
    var numberSequences: List<KnowledgeNumberSequence> = emptyList()
    var sourceTags: List<SourceTag> = emptyList()
    var appSettings: List<AppSettings> = emptyList()

    override suspend fun clearKnowledgeCategories() = Unit
    override suspend fun clearKnowledgePoints() { cleared = true }
    override suspend fun clearQuestions() = Unit
    override suspend fun clearCrossRefs() = Unit
    override suspend fun clearReviewFeedbacks() = Unit
    override suspend fun clearDecisionLogs() = Unit
    override suspend fun clearPlans() = Unit
    override suspend fun clearPlanPages() = Unit
    override suspend fun clearNoteAttachments() = Unit
    override suspend fun clearTombstones() = Unit
    override suspend fun clearNumberSequences() = Unit
    override suspend fun clearSourceTags() = Unit
    override suspend fun clearPendingFeedbacks() = Unit
    override suspend fun clearScanSessions() = Unit
    override suspend fun clearCoolingEvents() = Unit
    override suspend fun clearAppSettings() = Unit

    override suspend fun insertKnowledgeCategories(rows: List<KnowledgeCategory>) { knowledgeCategories = rows }
    override suspend fun insertKnowledgePoints(rows: List<KnowledgePoint>) { knowledgePoints = rows }
    override suspend fun insertQuestions(rows: List<Question>) { questions = rows }
    override suspend fun insertCrossRefs(rows: List<QuestionKnowledgeCrossRef>) { crossRefs = rows }
    override suspend fun insertReviewFeedbacks(rows: List<ReviewFeedback>) { reviewFeedbacks = rows }
    override suspend fun insertDecisionLogs(rows: List<ConfirmationDecisionLog>) { decisionLogs = rows }
    override suspend fun insertPlans(rows: List<Plan>) { plans = rows }
    override suspend fun insertPlanPages(rows: List<PlanPage>) { planPages = rows }
    override suspend fun insertNoteAttachments(rows: List<NoteAttachmentItem>) { noteAttachments = rows }
    override suspend fun insertTombstones(rows: List<KnowledgePointTombstone>) { tombstones = rows }
    override suspend fun insertNumberSequences(rows: List<KnowledgeNumberSequence>) { numberSequences = rows }
    override suspend fun insertSourceTags(rows: List<SourceTag>) { sourceTags = rows }
    override suspend fun insertAppSettings(rows: List<AppSettings>) { appSettings = rows }
}
