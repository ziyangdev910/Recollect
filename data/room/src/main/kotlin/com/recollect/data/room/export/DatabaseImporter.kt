package com.recollect.data.room.export

import com.recollect.data.room.RecollectDatabase
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
import com.recollect.data.room.entity.UserDecision
import com.recollect.data.room.repository.TransactionRunner
import com.recollect.model.Tier
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 导入摘要：源版本、是否按 B-01 降级、将写入的各实体行数与跳过的悬空行。 */
data class ImportSummary(
    val sourceBackupFormatVersion: Int,
    val sourceSchemaVersion: Int,
    val degraded: Boolean,
    val entityCounts: Map<String, Int>,
    val skippedRows: List<String>,
)

sealed interface ImportValidation {
    data class Valid(val summary: ImportSummary) : ImportValidation

    data class Invalid(val message: String) : ImportValidation
}

sealed interface ImportResult {
    data class Success(val summary: ImportSummary) : ImportResult

    data class Failure(val message: String) : ImportResult
}

/**
 * 导入端口（backup.md「替换式导入」）：
 *
 * - [validate] 只解析与校验，不写库；给界面做「将覆盖当前数据」确认前的合法性判断。
 * - [apply] 重新解析并在单事务内清空本地数据、写入备份内容；任一步失败整体回滚。
 */
interface DatabaseImporter {
    suspend fun validate(json: String): ImportValidation

    suspend fun apply(json: String): ImportResult
}

/**
 * Room 导入实现。
 *
 * 版本口径（B-01）：
 * - `backupFormatVersion` 必须等于当前 `1`，高于当前直接拒绝；
 * - `roomSchemaVersion` 等于 [RecollectDatabase.SCHEMA_VERSION] 时全量恢复；
 *   不同时按逐字段矩阵降级：保留题目/知识点/关联/笔记/来源标签/墓碑/编号序列，
 *   不恢复 `ReviewFeedback`、`ConfirmationDecisionLog`、`Plan` 与 `PlanPage`，
 *   并重置知识点调度状态、清空题目打印快照。
 */
class RoomDatabaseImporter(
    private val restoreDao: BackupRestoreDao,
    private val transactionRunner: TransactionRunner,
    private val clock: Clock,
) : DatabaseImporter {

    override suspend fun validate(json: String): ImportValidation = withContext(Dispatchers.IO) {
        try {
            ImportValidation.Valid(parse(json).summary)
        } catch (e: CancellationException) {
            throw e
        } catch (e: BackupParseException) {
            ImportValidation.Invalid(e.message ?: "备份文件不合法。")
        } catch (e: Exception) {
            ImportValidation.Invalid("备份文件不合法：${e.message ?: "无法解析文件内容"}")
        }
    }

    override suspend fun apply(json: String): ImportResult {
        val parsed = try {
            withContext(Dispatchers.IO) { parse(json) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: BackupParseException) {
            return ImportResult.Failure(e.message ?: "备份文件不合法。")
        } catch (e: Exception) {
            return ImportResult.Failure("导入失败：${e.message ?: "无法解析文件内容"}")
        }
        return try {
            transactionRunner.inTransaction { restore(parsed) }
            ImportResult.Success(parsed.summary)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ImportResult.Failure("导入失败：${e.message ?: "数据库写入失败"}")
        }
    }

    private suspend fun restore(parsed: ParsedBackup) {
        // 先清临时态，再清白名单表；与插入同处一个事务（backup.md「替换式导入」）。
        restoreDao.clearPendingFeedbacks()
        restoreDao.clearScanSessions()
        restoreDao.clearCoolingEvents()
        restoreDao.clearCrossRefs()
        restoreDao.clearReviewFeedbacks()
        restoreDao.clearDecisionLogs()
        restoreDao.clearPlanPages()
        restoreDao.clearPlans()
        restoreDao.clearNoteAttachments()
        restoreDao.clearQuestions()
        restoreDao.clearKnowledgePoints()
        restoreDao.clearKnowledgeCategories()
        restoreDao.clearTombstones()
        restoreDao.clearNumberSequences()
        restoreDao.clearSourceTags()
        restoreDao.clearAppSettings()

        restoreDao.insertKnowledgeCategories(parsed.knowledgeCategories)
        restoreDao.insertKnowledgePoints(parsed.knowledgePoints)
        restoreDao.insertQuestions(parsed.questions)
        restoreDao.insertCrossRefs(parsed.crossRefs)
        restoreDao.insertReviewFeedbacks(parsed.reviewFeedbacks)
        restoreDao.insertDecisionLogs(parsed.decisionLogs)
        restoreDao.insertPlans(parsed.plans)
        restoreDao.insertPlanPages(parsed.planPages)
        restoreDao.insertNoteAttachments(parsed.noteAttachments)
        restoreDao.insertTombstones(parsed.tombstones)
        restoreDao.insertNumberSequences(parsed.numberSequences)
        restoreDao.insertSourceTags(parsed.sourceTags)
        restoreDao.insertAppSettings(parsed.appSettings)
    }

    private fun parse(json: String): ParsedBackup {
        val root = parseBackupJson(json).asObject("备份文件")
        val formatVersion = root.int("backupFormatVersion", "备份文件")
        if (formatVersion > RoomDatabaseExporter.BACKUP_FORMAT_VERSION) {
            fail(
                "备份格式版本 $formatVersion 高于当前支持的版本 " +
                    "${RoomDatabaseExporter.BACKUP_FORMAT_VERSION}，请升级拾知后再导入。",
            )
        }
        if (formatVersion < 1) fail("备份格式版本 $formatVersion 不受支持。")
        val schemaVersion = root.int("roomSchemaVersion", "备份文件")
        if (schemaVersion < 1) fail("备份文件的 schema 版本不合法。")
        val data = root.obj("data", "备份文件")

        val backupCategories = data.array("knowledgeCategories").mapIndexed { index, row ->
            parseKnowledgeCategory(row.asObject("knowledgeCategories[$index]"))
        }
        var knowledgePoints = data.array("knowledgePoints").mapIndexed { index, row ->
            parseKnowledgePoint(row.asObject("knowledgePoints[$index]"))
        }
        var questions = data.array("questions").mapIndexed { index, row ->
            parseQuestion(row.asObject("questions[$index]"))
        }
        var crossRefs = data.array("questionKnowledgeCrossRefs").mapIndexed { index, row ->
            parseCrossRef(row.asObject("questionKnowledgeCrossRefs[$index]"))
        }
        var reviewFeedbacks = data.array("reviewFeedbacks").mapIndexed { index, row ->
            parseReviewFeedback(row.asObject("reviewFeedbacks[$index]"))
        }
        var decisionLogs = data.array("confirmationDecisionLogs").mapIndexed { index, row ->
            parseDecisionLog(row.asObject("confirmationDecisionLogs[$index]"))
        }
        var plans = data.array("plans").mapIndexed { index, row ->
            parsePlan(row.asObject("plans[$index]"))
        }
        var planPages = data.array("planPages").mapIndexed { index, row ->
            parsePlanPage(row.asObject("planPages[$index]"))
        }
        var noteAttachments = data.array("noteAttachments").mapIndexed { index, row ->
            parseNoteAttachment(row.asObject("noteAttachments[$index]"))
        }
        val tombstones = data.array("knowledgePointTombstones").mapIndexed { index, row ->
            parseTombstone(row.asObject("knowledgePointTombstones[$index]"))
        }
        val sourceTags = data.array("sourceTags").mapIndexed { index, row ->
            parseSourceTag(row.asObject("sourceTags[$index]"))
        }
        // 每日复习时长属于用户偏好而非调度历史，跨 schema 也保留；
        // 缺失（v11 及更早备份）或为空时回退到默认行，保证恢复后 DB 含该字段。
        val appSettings = data.array("appSettings")
            .mapIndexed { index, row -> parseAppSettings(row.asObject("appSettings[$index]")) }
            .firstOrNull()
            ?.copy(id = AppSettings.SINGLE_ROW_ID)
            ?: AppSettings()

        validateNumbers(knowledgePoints, tombstones)

        // v13：分类随备份恢复；旧备份（v12 及更早）没有分类，补默认分类，
        // 并保证每个知识点的 categoryId 都指向一个存在的分类（缺失时归入「语文」）。
        val (knowledgeCategories, normalizedPoints) = resolveCategories(
            categories = backupCategories,
            knowledgePoints = knowledgePoints,
        )
        knowledgePoints = normalizedPoints

        // 悬空关联跳过并记录，不产生半恢复的引用（backup.md「悬空引用」）。
        val questionIds = questions.mapTo(mutableSetOf()) { it.id }
        val knowledgePointIds = knowledgePoints.mapTo(mutableSetOf()) { it.id }
        val allCrossRefs = crossRefs
        crossRefs = allCrossRefs.filter { it.questionId in questionIds && it.knowledgePointId in knowledgePointIds }
        val skippedRows = (allCrossRefs - crossRefs.toSet()).map {
            "questionKnowledgeCrossRefs(${it.questionId}, ${it.knowledgePointId})"
        }

        val degraded = schemaVersion != RecollectDatabase.SCHEMA_VERSION
        if (degraded) {
            val today = LocalDate.now(clock)
            val activeCounts = activeQuestionCounts(questions, crossRefs)
            knowledgePoints = knowledgePoints.map { point ->
                point.copy(
                    currentTier = Tier.UNKNOWN,
                    continuousScore = 0f,
                    effectiveFeedbackCount = 0f,
                    difficulty = null,
                    halfLifeDays = null,
                    nextDueDate = today.plusDays(1),
                    activeQuestionCount = activeCounts[point.id] ?: 0,
                    dormant = (activeCounts[point.id] ?: 0) == 0,
                )
            }
            questions = questions.map {
                it.copy(
                    pageId = "",
                    qrPayload = "",
                    lastScheduledDate = null,
                    lastReviewedDate = null,
                )
            }
            reviewFeedbacks = emptyList()
            decisionLogs = emptyList()
            plans = emptyList()
            planPages = emptyList()
            noteAttachments = noteAttachments.map { it.copy(sourceFeedbackId = null) }
        }

        // 编号序列按恢复后的知识点/墓碑校正（backup.md「替换式导入」）。
        val backupSequences = data.array("knowledgeNumberSequences").mapIndexed { index, row ->
            parseSequence(row.asObject("knowledgeNumberSequences[$index]"))
        }
        val maxIssued = maxOf(
            knowledgePoints.maxOfOrNull { it.number } ?: 0,
            tombstones.maxOfOrNull { it.number } ?: 0,
            backupSequences.maxOfOrNull { it.lastIssued } ?: 0,
        )
        val sequences = listOf(KnowledgeNumberSequence(lastIssued = maxIssued))

        val counts = linkedMapOf(
            RoomDatabaseExporter.KEY_KNOWLEDGE_CATEGORIES to knowledgeCategories.size,
            RoomDatabaseExporter.KEY_KNOWLEDGE_POINTS to knowledgePoints.size,
            RoomDatabaseExporter.KEY_QUESTIONS to questions.size,
            RoomDatabaseExporter.KEY_CROSS_REFS to crossRefs.size,
            RoomDatabaseExporter.KEY_REVIEW_FEEDBACKS to reviewFeedbacks.size,
            RoomDatabaseExporter.KEY_DECISION_LOGS to decisionLogs.size,
            RoomDatabaseExporter.KEY_PLANS to plans.size,
            RoomDatabaseExporter.KEY_PLAN_PAGES to planPages.size,
            RoomDatabaseExporter.KEY_NOTE_ATTACHMENTS to noteAttachments.size,
            RoomDatabaseExporter.KEY_TOMBSTONES to tombstones.size,
            RoomDatabaseExporter.KEY_NUMBER_SEQUENCES to sequences.size,
            RoomDatabaseExporter.KEY_SOURCE_TAGS to sourceTags.size,
            RoomDatabaseExporter.KEY_APP_SETTINGS to 1,
        )

        return ParsedBackup(
            knowledgeCategories = knowledgeCategories,
            knowledgePoints = knowledgePoints,
            questions = questions,
            crossRefs = crossRefs,
            reviewFeedbacks = reviewFeedbacks,
            decisionLogs = decisionLogs,
            plans = plans,
            planPages = planPages,
            noteAttachments = noteAttachments,
            tombstones = tombstones,
            numberSequences = sequences,
            sourceTags = sourceTags,
            appSettings = listOf(appSettings),
            summary = ImportSummary(
                sourceBackupFormatVersion = formatVersion,
                sourceSchemaVersion = schemaVersion,
                degraded = degraded,
                entityCounts = counts,
                skippedRows = skippedRows,
            ),
        )
    }

    /** 备份内编号不得重复，也不得与墓碑冲突；冲突时拒绝导入，不静默重编号。 */
    private fun validateNumbers(
        knowledgePoints: List<KnowledgePoint>,
        tombstones: List<KnowledgePointTombstone>,
    ) {
        val duplicates = knowledgePoints.groupBy { it.number }
            .filterValues { it.size > 1 }
            .keys
            .sorted()
        if (duplicates.isNotEmpty()) {
            fail("备份内知识点编号重复：${duplicates.joinToString(", ")}。")
        }
        val tombstoneNumbers = tombstones.mapTo(mutableSetOf()) { it.number }
        val conflicts = knowledgePoints.map { it.number }
            .filter { it in tombstoneNumbers }
            .sorted()
        if (conflicts.isNotEmpty()) {
            fail("知识点编号与墓碑冲突：${conflicts.joinToString(", ")}。")
        }
    }

    private fun activeQuestionCounts(
        questions: List<Question>,
        crossRefs: List<QuestionKnowledgeCrossRef>,
    ): Map<String, Int> {
        val activeQuestionIds = questions.filterNot { it.archived }.mapTo(mutableSetOf()) { it.id }
        val counts = mutableMapOf<String, Int>()
        crossRefs.filter { it.questionId in activeQuestionIds }.forEach { link ->
            counts[link.knowledgePointId] = (counts[link.knowledgePointId] ?: 0) + 1
        }
        return counts
    }

    private fun parseKnowledgeCategory(row: Map<String, JsonValue>): KnowledgeCategory {
        val context = "知识点分类"
        return KnowledgeCategory(
            id = row.string("id", context),
            name = row.string("name", context),
            createdAt = row.instant("createdAt", context),
            // v15 之前的备份没有封面；null 表示按分类名称派生默认封面。
            coverTone = row.optionalInt("coverTone", context),
            coverIcon = row.optionalString("coverIcon", context),
            // v16 之前的备份没有排序；0 让 all() 回退到 createdAt。
            sortOrder = row.optionalInt("sortOrder", context) ?: 0,
        )
    }

    private fun parseKnowledgePoint(row: Map<String, JsonValue>): KnowledgePoint {
        val context = "知识点"
        return KnowledgePoint(
            id = row.string("id", context),
            number = row.int("number", context),
            name = row.string("name", context),
            currentTier = row.tier("currentTier", context),
            continuousScore = row.float("continuousScore", context),
            effectiveFeedbackCount = row.float("effectiveFeedbackCount", context),
            nextDueDate = row.date("nextDueDate", context),
            activeQuestionCount = row.int("activeQuestionCount", context),
            dormant = row.bool("dormant", context),
            difficulty = row.optionalInt("difficulty", context),
            halfLifeDays = row.optionalFloat("halfLifeDays", context),
            createdAt = row.instant("createdAt", context),
            // v13 之前的备份没有分类，统一归入「语文」。
            categoryId = row.optionalString("categoryId", context)
                ?: KnowledgeCategoryDefaults.DEFAULT_CATEGORY_ID,
            // v14 之前的备份没有修改时间，回退为创建时间。
            updatedAt = row.optionalInstant("updatedAt", context)
                ?: row.instant("createdAt", context),
        )
    }

    /**
     * 分类缺失时补默认分类，并把指向不存在分类的知识点归入「语文」。
     *
     * 新备份（v13+）总是带完整分类；这里的兜底只针对旧备份或手工编辑过的文件，
     * 保证恢复后不会出现无分类的知识点。
     */
    private fun resolveCategories(
        categories: List<KnowledgeCategory>,
        knowledgePoints: List<KnowledgePoint>,
    ): Pair<List<KnowledgeCategory>, List<KnowledgePoint>> {
        val seeded = if (categories.isEmpty()) {
            val base = clock.instant()
            KnowledgeCategoryDefaults.NAMES.mapIndexed { index, (id, name) ->
                KnowledgeCategory(id = id, name = name, createdAt = base.plusSeconds(index.toLong()))
            }
        } else {
            categories
        }
        val fallbackId = KnowledgeCategoryDefaults.DEFAULT_CATEGORY_ID
        // 旧备份没有分类、或分类被用户改得没有「语文」时，补一个「语文」承接旧知识点。
        val resolvedCategories = if (seeded.any { it.id == fallbackId }) {
            seeded
        } else {
            seeded + KnowledgeCategory(
                id = fallbackId,
                name = KnowledgeCategoryDefaults.NAMES.first { it.first == fallbackId }.second,
                createdAt = clock.instant(),
            )
        }
        val knownIds = resolvedCategories.mapTo(mutableSetOf()) { it.id }
        val points = knowledgePoints.map {
            if (it.categoryId in knownIds) it else it.copy(categoryId = fallbackId)
        }
        return resolvedCategories to points
    }

    private fun parseQuestion(row: Map<String, JsonValue>): Question {
        val context = "题目"
        val minutes = row.optionalInt("estimatedReviewMinutes", context) ?: 15
        return Question(
            id = row.string("id", context),
            pageId = row.optionalString("pageId", context) ?: "",
            questionNumber = row.string("questionNumber", context),
            qrPayload = row.optionalString("qrPayload", context) ?: "",
            archived = row.bool("archived", context),
            lastScheduledDate = row.optionalDate("lastScheduledDate", context),
            lastReviewedDate = row.optionalDate("lastReviewedDate", context),
            estimatedReviewMinutes = if (minutes > 0) minutes else 15,
            createdAt = row.instant("createdAt", context),
            archivedAt = row.optionalInstant("archivedAt", context),
            sourcePage = row.optionalString("sourcePage", context) ?: "",
            sourcePageCode = row.optionalInt("sourcePageCode", context) ?: 0,
            // v14 之前的备份没有修改时间，回退为创建时间。
            updatedAt = row.optionalInstant("updatedAt", context)
                ?: row.instant("createdAt", context),
        )
    }

    private fun parseCrossRef(row: Map<String, JsonValue>): QuestionKnowledgeCrossRef {
        val context = "题目知识点关联"
        return QuestionKnowledgeCrossRef(
            questionId = row.string("questionId", context),
            knowledgePointId = row.string("knowledgePointId", context),
            weight = row.optionalFloat("weight", context) ?: 1.0f,
        )
    }

    private fun parseReviewFeedback(row: Map<String, JsonValue>): ReviewFeedback {
        val context = "复习反馈"
        return ReviewFeedback(
            id = row.long("id", context),
            knowledgePointId = row.string("knowledgePointId", context),
            questionId = row.string("questionId", context),
            planId = row.string("planId", context),
            pageId = row.string("pageId", context),
            plannedDate = row.date("plannedDate", context),
            reviewedOn = row.date("reviewedOn", context),
            tier = row.tier("tier", context),
            triggered = row.bool("triggered", context),
            rawOcrText = row.optionalString("rawOcrText", context),
            correctedText = row.optionalString("correctedText", context),
            effectiveWeight = row.float("effectiveWeight", context),
            weightApplied = row.float("weightApplied", context),
            timestamp = row.instant("timestamp", context),
        )
    }

    private fun parseDecisionLog(row: Map<String, JsonValue>): ConfirmationDecisionLog {
        val context = "确认决定日志"
        return ConfirmationDecisionLog(
            id = row.long("id", context),
            sessionId = row.string("sessionId", context),
            planId = row.string("planId", context),
            pageId = row.string("pageId", context),
            questionId = row.string("questionId", context),
            knowledgePointId = row.string("knowledgePointId", context),
            plannedDate = row.date("plannedDate", context),
            reviewedOn = row.optionalDate("reviewedOn", context),
            triggered = row.bool("triggered", context),
            decision = row.userDecision("decision", context),
            selectedTier = row.optionalTier("selectedTier", context),
            correctedText = row.optionalString("correctedText", context),
            decidedAt = row.instant("decidedAt", context),
        )
    }

    private fun parsePlan(row: Map<String, JsonValue>): Plan {
        val context = "计划"
        return Plan(
            planId = row.string("planId", context),
            weekStart = row.date("weekStart", context),
            weekEnd = row.date("weekEnd", context),
            createdAt = row.instant("createdAt", context),
            status = row.string("status", context),
            printedAt = row.optionalInstant("printedAt", context),
            supersededBy = row.optionalString("supersededBy", context),
            renderMeta = row.optionalString("renderMeta", context) ?: "[]",
            deferred = row.optionalString("deferred", context) ?: "[]",
            // v17：旧备份缺少该字段时记 0（未知）；新备份保留生成时的每日复习时长。
            dailyMinutes = row.optionalInt("dailyMinutes", context) ?: 0,
        )
    }

    private fun parsePlanPage(row: Map<String, JsonValue>): PlanPage {
        val context = "打印页"
        return PlanPage(
            planId = row.string("planId", context),
            pageId = row.string("pageId", context),
            pageNumber = row.int("pageNumber", context),
            date = row.date("date", context),
            itemCount = row.int("itemCount", context),
            questionIds = row.string("questionIds", context),
            knowledgeRows = row.string("knowledgeRows", context),
            continuationMarkers = row.string("continuationMarkers", context),
            itemGeometry = row.optionalString("itemGeometry", context) ?: "[]",
        )
    }

    private fun parseNoteAttachment(row: Map<String, JsonValue>): NoteAttachmentItem {
        val context = "笔记附件"
        return NoteAttachmentItem(
            id = row.long("id", context),
            questionId = row.string("questionId", context),
            sourceFeedbackId = row.optionalLong("sourceFeedbackId", context),
            text = row.string("text", context),
            imageUri = row.optionalString("imageUri", context),
            order = row.int("order", context),
            createdAt = row.instant("createdAt", context),
        )
    }

    private fun parseTombstone(row: Map<String, JsonValue>): KnowledgePointTombstone {
        val context = "知识点墓碑"
        return KnowledgePointTombstone(
            number = row.int("number", context),
            knowledgePointId = row.string("knowledgePointId", context),
            voidedAt = row.instant("voidedAt", context),
            reason = row.optionalString("reason", context),
        )
    }

    private fun parseSequence(row: Map<String, JsonValue>): KnowledgeNumberSequence =
        KnowledgeNumberSequence(
            id = row.optionalInt("id", "编号序列") ?: KnowledgeNumberSequence.SINGLE_ROW_ID,
            lastIssued = row.int("lastIssued", "编号序列"),
        )

    private fun parseSourceTag(row: Map<String, JsonValue>): SourceTag {
        val context = "来源标签"
        return SourceTag(
            id = row.string("id", context),
            name = row.string("name", context),
            createdAt = row.instant("createdAt", context),
        )
    }

    private fun parseAppSettings(row: Map<String, JsonValue>): AppSettings {
        val context = "应用设置"
        val minutes = row.optionalInt("dailyReviewMinutes", context)
            ?: AppSettings.DEFAULT_DAILY_REVIEW_MINUTES
        return AppSettings(
            id = row.optionalInt("id", context) ?: AppSettings.SINGLE_ROW_ID,
            dailyReviewMinutes = minutes.coerceIn(
                AppSettings.MIN_DAILY_REVIEW_MINUTES,
                AppSettings.MAX_DAILY_REVIEW_MINUTES,
            ),
        )
    }

    private data class ParsedBackup(
        val knowledgeCategories: List<KnowledgeCategory>,
        val knowledgePoints: List<KnowledgePoint>,
        val questions: List<Question>,
        val crossRefs: List<QuestionKnowledgeCrossRef>,
        val reviewFeedbacks: List<ReviewFeedback>,
        val decisionLogs: List<ConfirmationDecisionLog>,
        val plans: List<Plan>,
        val planPages: List<PlanPage>,
        val noteAttachments: List<NoteAttachmentItem>,
        val tombstones: List<KnowledgePointTombstone>,
        val numberSequences: List<KnowledgeNumberSequence>,
        val sourceTags: List<SourceTag>,
        val appSettings: List<AppSettings>,
        val summary: ImportSummary,
    )
}

private fun fail(message: String): Nothing = throw BackupParseException(message)

private fun JsonValue.asObject(context: String): Map<String, JsonValue> =
    (this as? JsonValue.Obj)?.entries?.toMap() ?: fail("$context 不是 JSON 对象。")

private fun Map<String, JsonValue>.field(key: String, context: String): JsonValue =
    this[key] ?: fail("$context 缺少字段 $key。")

private fun Map<String, JsonValue>.array(key: String): List<JsonValue> {
    val value = this[key] ?: return emptyList()
    return (value as? JsonValue.Arr)?.items ?: fail("备份文件的 $key 不是数组。")
}

private fun Map<String, JsonValue>.obj(key: String, context: String): Map<String, JsonValue> =
    field(key, context).asObject("$context.$key")

private fun Map<String, JsonValue>.string(key: String, context: String): String {
    val value = field(key, context)
    return (value as? JsonValue.Str)?.value ?: fail("$context 的 $key 不是字符串。")
}

private fun Map<String, JsonValue>.optionalString(key: String, context: String): String? =
    when (val value = this[key]) {
        null, JsonValue.Null -> null
        is JsonValue.Str -> value.value
        else -> fail("$context 的 $key 不是字符串或 null。")
    }

private fun Map<String, JsonValue>.number(key: String, context: String): JsonValue.Num {
    val value = field(key, context)
    return value as? JsonValue.Num ?: fail("$context 的 $key 不是数字。")
}

private fun Map<String, JsonValue>.int(key: String, context: String): Int =
    number(key, context).value.toInt()

private fun Map<String, JsonValue>.optionalInt(key: String, context: String): Int? =
    if (this[key] == null || this[key] == JsonValue.Null) null else int(key, context)

private fun Map<String, JsonValue>.long(key: String, context: String): Long =
    number(key, context).value.toLong()

private fun Map<String, JsonValue>.optionalLong(key: String, context: String): Long? =
    if (this[key] == null || this[key] == JsonValue.Null) null else long(key, context)

private fun Map<String, JsonValue>.float(key: String, context: String): Float =
    number(key, context).value.toFloat()

private fun Map<String, JsonValue>.optionalFloat(key: String, context: String): Float? =
    if (this[key] == null || this[key] == JsonValue.Null) null else float(key, context)

private fun Map<String, JsonValue>.bool(key: String, context: String): Boolean {
    val value = field(key, context)
    return (value as? JsonValue.Bool)?.value ?: fail("$context 的 $key 不是布尔值。")
}

private fun Map<String, JsonValue>.date(key: String, context: String): LocalDate =
    parseDate(string(key, context), "$context 的 $key")

private fun Map<String, JsonValue>.optionalDate(key: String, context: String): LocalDate? =
    optionalString(key, context)?.let { parseDate(it, "$context 的 $key") }

private fun Map<String, JsonValue>.instant(key: String, context: String): Instant =
    parseInstant(string(key, context), "$context 的 $key")

private fun Map<String, JsonValue>.optionalInstant(key: String, context: String): Instant? =
    optionalString(key, context)?.let { parseInstant(it, "$context 的 $key") }

private fun Map<String, JsonValue>.tier(key: String, context: String): Tier {
    val name = string(key, context)
    return enumValues<Tier>().firstOrNull { it.name == name }
        ?: fail("$context 的 $key 不是合法档位：$name。")
}

private fun Map<String, JsonValue>.optionalTier(key: String, context: String): Tier? =
    optionalString(key, context)?.let { name ->
        enumValues<Tier>().firstOrNull { it.name == name }
            ?: fail("$context 的 $key 不是合法档位：$name。")
    }

private fun Map<String, JsonValue>.userDecision(key: String, context: String): UserDecision {
    val name = string(key, context)
    return enumValues<UserDecision>().firstOrNull { it.name == name }
        ?: fail("$context 的 $key 不是合法决定：$name。")
}

private fun parseDate(value: String, context: String): LocalDate = try {
    LocalDate.parse(value)
} catch (e: DateTimeParseException) {
    fail("$context 不是合法日期：$value。")
}

private fun parseInstant(value: String, context: String): Instant = try {
    Instant.parse(value)
} catch (e: DateTimeParseException) {
    fail("$context 不是合法时间：$value。")
}
