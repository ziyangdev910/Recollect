package com.recollect.data.room.export

import com.recollect.data.room.dao.BackupDao
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
import com.recollect.data.room.entity.UserDecision
import com.recollect.model.Tier
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * 导出器单元测试（backup.md「验收」的纯 Kotlin 部分）：
 * 顶层字段、白名单实体、日期格式与字符串转义；临时态不得出现在导出文件中。
 *
 * 真机/模拟器上的 SAF 分享与文件写出不属于本测试，见 docs/agent-log 的验证记录。
 */
class RoomDatabaseExporterTest {

    private val clock = Clock.fixed(
        Instant.parse("2026-11-22T08:05:00Z"),
        ZoneId.of("Asia/Shanghai"),
    )

    @Test
    fun `空库导出含版本字段与白名单空数组`() = runBlocking {
        val document = exporter(FakeBackupDao()).export()

        assertEquals("recollect-20261122-1605.json", document.fileName)
        assertContains(document.json, "\"backupFormatVersion\": 1")
        assertContains(document.json, "\"roomSchemaVersion\": 17")
        assertContains(document.json, "\"appVersionName\": \"0.1.0\"")
        assertContains(document.json, "\"exportedAt\": \"2026-11-22T08:05:00Z\"")
        listOf(
            "knowledgeCategories",
            "knowledgePoints",
            "questions",
            "questionKnowledgeCrossRefs",
            "reviewFeedbacks",
            "confirmationDecisionLogs",
            "plans",
            "planPages",
            "noteAttachments",
            "knowledgePointTombstones",
            "knowledgeNumberSequences",
            "sourceTags",
        ).forEach { key ->
            assertContains(document.json, "\"$key\": []")
            assertEquals(0, document.entityCounts[key])
        }
        // v12：即使库中没有设置行，也必须导出默认的「每日复习时长」。
        assertContains(document.json, "\"appSettings\"")
        assertContains(document.json, "\"dailyReviewMinutes\": 60")
        assertEquals(1, document.entityCounts[RoomDatabaseExporter.KEY_APP_SETTINGS])
        assertFalse(document.json.contains("pendingFeedback"))
        assertFalse(document.json.contains("scanSession"))
        assertFalse(document.json.contains("coolingOverrideEvent"))
    }

    @Test
    fun `实体字段按属性名导出并统计行数`() = runBlocking {
        val dao = FakeBackupDao(
            knowledgeCategories = listOf(
                KnowledgeCategory(
                    id = "cat-chinese",
                    name = "语文",
                    createdAt = Instant.parse("2026-11-18T00:00:00Z"),
                    coverTone = 2,
                    coverIcon = "book",
                    // 首页卡片的拖拽顺序也必须随备份导出。
                    sortOrder = 3,
                ),
            ),
            knowledgePoints = listOf(
                KnowledgePoint(
                    id = "kp-1",
                    number = 3,
                    name = "二次函数",
                    currentTier = Tier.FAMILIAR,
                    continuousScore = 0.6f,
                    effectiveFeedbackCount = 3.0f,
                    nextDueDate = LocalDate.parse("2026-11-25"),
                    activeQuestionCount = 2,
                    dormant = false,
                    difficulty = null,
                    halfLifeDays = null,
                    createdAt = Instant.parse("2026-11-20T10:00:00Z"),
                    categoryId = "cat-chinese",
                ),
            ),
            questions = listOf(
                Question(
                    id = "q-1",
                    pageId = "",
                    questionNumber = "3-1",
                    qrPayload = "",
                    archived = true,
                    lastScheduledDate = null,
                    lastReviewedDate = LocalDate.parse("2026-11-21"),
                    estimatedReviewMinutes = 12,
                    createdAt = Instant.parse("2026-11-20T10:00:01Z"),
                    archivedAt = Instant.parse("2026-11-21T09:00:00Z"),
                    sourcePage = "作业本",
                    sourcePageCode = 3,
                ),
            ),
            crossRefs = listOf(QuestionKnowledgeCrossRef("q-1", "kp-1")),
            reviewFeedbacks = listOf(
                ReviewFeedback(
                    id = 7,
                    knowledgePointId = "kp-1",
                    questionId = "q-1",
                    planId = "ABCDEF",
                    pageId = "ABCDEF-01",
                    plannedDate = LocalDate.parse("2026-11-21"),
                    reviewedOn = LocalDate.parse("2026-11-21"),
                    tier = Tier.MASTERED,
                    triggered = true,
                    rawOcrText = null,
                    correctedText = "行1\n行2",
                    effectiveWeight = 1.0f,
                    weightApplied = 1.0f,
                    timestamp = Instant.parse("2026-11-21T12:00:00Z"),
                ),
            ),
            decisionLogs = listOf(
                ConfirmationDecisionLog(
                    id = 9,
                    sessionId = "s-1",
                    planId = "ABCDEF",
                    pageId = "ABCDEF-01",
                    questionId = "q-1",
                    knowledgePointId = "kp-1",
                    plannedDate = LocalDate.parse("2026-11-21"),
                    reviewedOn = null,
                    triggered = true,
                    decision = UserDecision.NOT_REVIEWED,
                    selectedTier = null,
                    correctedText = null,
                    decidedAt = Instant.parse("2026-11-21T12:00:01Z"),
                ),
            ),
            plans = listOf(
                Plan(
                    planId = "ABCDEF",
                    weekStart = LocalDate.parse("2026-11-16"),
                    weekEnd = LocalDate.parse("2026-11-22"),
                    createdAt = Instant.parse("2026-11-16T00:00:00Z"),
                    status = "PRINTED",
                    printedAt = Instant.parse("2026-11-16T08:00:00Z"),
                    supersededBy = null,
                    dailyMinutes = 45,
                ),
            ),
            planPages = listOf(
                PlanPage(
                    planId = "ABCDEF",
                    pageId = "ABCDEF-01",
                    pageNumber = 1,
                    date = LocalDate.parse("2026-11-21"),
                    itemCount = 1,
                    questionIds = "[\"q-1\"]",
                    knowledgeRows = "[]",
                    continuationMarkers = "[]",
                ),
            ),
            noteAttachments = listOf(
                NoteAttachmentItem(
                    id = 2,
                    questionId = "q-1",
                    sourceFeedbackId = 7,
                    text = "笔记",
                    imageUri = null,
                    order = 0,
                    createdAt = Instant.parse("2026-11-21T12:00:02Z"),
                ),
            ),
            tombstones = listOf(
                KnowledgePointTombstone(
                    number = 2,
                    knowledgePointId = "kp-deleted",
                    voidedAt = Instant.parse("2026-11-19T00:00:00Z"),
                    reason = "合并",
                ),
            ),
            numberSequences = listOf(KnowledgeNumberSequence(lastIssued = 3)),
            sourceTags = listOf(
                SourceTag(
                    id = "tag-1",
                    name = "作业本",
                    createdAt = Instant.parse("2026-11-18T00:00:00Z"),
                ),
            ),
        )

        val document = exporter(dao).export()

        assertEquals(1, document.entityCounts[RoomDatabaseExporter.KEY_KNOWLEDGE_CATEGORIES])
        assertEquals(1, document.entityCounts[RoomDatabaseExporter.KEY_KNOWLEDGE_POINTS])
        assertEquals(1, document.entityCounts[RoomDatabaseExporter.KEY_QUESTIONS])
        assertEquals(1, document.entityCounts[RoomDatabaseExporter.KEY_CROSS_REFS])
        assertEquals(1, document.entityCounts[RoomDatabaseExporter.KEY_REVIEW_FEEDBACKS])
        assertEquals(1, document.entityCounts[RoomDatabaseExporter.KEY_DECISION_LOGS])
        assertEquals(1, document.entityCounts[RoomDatabaseExporter.KEY_PLANS])
        assertEquals(1, document.entityCounts[RoomDatabaseExporter.KEY_PLAN_PAGES])
        assertEquals(1, document.entityCounts[RoomDatabaseExporter.KEY_NOTE_ATTACHMENTS])
        assertEquals(1, document.entityCounts[RoomDatabaseExporter.KEY_TOMBSTONES])
        assertEquals(1, document.entityCounts[RoomDatabaseExporter.KEY_NUMBER_SEQUENCES])
        assertEquals(1, document.entityCounts[RoomDatabaseExporter.KEY_SOURCE_TAGS])

        assertContains(document.json, "\"currentTier\": \"FAMILIAR\"")
        assertContains(document.json, "\"nextDueDate\": \"2026-11-25\"")
        assertContains(document.json, "\"halfLifeDays\": null")
        assertContains(document.json, "\"sourcePageCode\": 3")
        assertContains(document.json, "\"lastReviewedDate\": \"2026-11-21\"")
        assertContains(document.json, "\"archivedAt\": \"2026-11-21T09:00:00Z\"")
        assertContains(document.json, "\"correctedText\": \"行1\\n行2\"")
        assertContains(document.json, "\"decision\": \"NOT_REVIEWED\"")
        assertContains(document.json, "\"reviewedOn\": null")
        assertContains(document.json, "\"lastIssued\": 3")
        assertContains(document.json, "\"status\": \"PRINTED\"")
        assertContains(document.json, "\"dailyMinutes\": 45")
        assertContains(document.json, "\"categoryId\": \"cat-chinese\"")
        assertContains(document.json, "\"name\": \"语文\"")
        // v15：分类封面按可选字段导出；旧分类为 null 时不写这两个键。
        assertContains(document.json, "\"coverTone\": 2")
        assertContains(document.json, "\"coverIcon\": \"book\"")
        assertContains(document.json, "\"sortOrder\": 3")
        // v14：题目与知识点都导出用户修改时间。
        assertContains(document.json, "\"updatedAt\": \"2026-11-20T10:00:00Z\"")
        assertContains(document.json, "\"updatedAt\": \"2026-11-20T10:00:01Z\"")
    }

    @Test
    fun `字符串中的引号与反斜杠被正确转义`() = runBlocking {
        val dao = FakeBackupDao(
            sourceTags = listOf(
                SourceTag(
                    id = "tag-1",
                    name = "含\"引号\"与\\反斜杠",
                    createdAt = Instant.parse("2026-11-18T00:00:00Z"),
                ),
            ),
        )

        val json = exporter(dao).export().json

        assertContains(json, "\"含\\\"引号\\\"与\\\\反斜杠\"")
        assertTrue(json.isNotBlank())
    }

    private fun exporter(dao: BackupDao) = RoomDatabaseExporter(
        backupDao = dao,
        appVersionName = "0.1.0",
        clock = clock,
    )
}

/** 导出器只读 DAO 的测试替身；未使用的写方法由 BackupDao 的只读签名保证不存在。 */
private class FakeBackupDao(
    private val knowledgeCategories: List<KnowledgeCategory> = emptyList(),
    private val knowledgePoints: List<KnowledgePoint> = emptyList(),
    private val questions: List<Question> = emptyList(),
    private val crossRefs: List<QuestionKnowledgeCrossRef> = emptyList(),
    private val reviewFeedbacks: List<ReviewFeedback> = emptyList(),
    private val decisionLogs: List<ConfirmationDecisionLog> = emptyList(),
    private val plans: List<Plan> = emptyList(),
    private val planPages: List<PlanPage> = emptyList(),
    private val noteAttachments: List<NoteAttachmentItem> = emptyList(),
    private val tombstones: List<KnowledgePointTombstone> = emptyList(),
    private val numberSequences: List<KnowledgeNumberSequence> = emptyList(),
    private val sourceTags: List<SourceTag> = emptyList(),
    private val appSettings: List<AppSettings> = emptyList(),
) : BackupDao {
    override suspend fun knowledgeCategories() = knowledgeCategories
    override suspend fun knowledgePoints() = knowledgePoints
    override suspend fun questions() = questions
    override suspend fun crossRefs() = crossRefs
    override suspend fun reviewFeedbacks() = reviewFeedbacks
    override suspend fun decisionLogs() = decisionLogs
    override suspend fun plans() = plans
    override suspend fun planPages() = planPages
    override suspend fun noteAttachments() = noteAttachments
    override suspend fun tombstones() = tombstones
    override suspend fun numberSequences() = numberSequences
    override suspend fun sourceTags() = sourceTags
    override suspend fun appSettings() = appSettings
}
