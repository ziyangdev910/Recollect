package com.recollect.data.room.schema

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * 阶段 4.1：Room schema 与 docs/specs/data-model.md 一一对应。
 *
 * 断言来源是 Room KSP 导出的 schema JSON（`exportSchema = true`），不是对注解的复述；
 * 这样字段名、SQLite 类型、主键、索引与迁移后实际建出的表保持一致。
 *
 * 阶段 4.2 起锁定 10 个实体：4.1 的 9 个 + `NoteAttachmentItem`（saveDecisions 事务需要）。
 * NoteAttachment、KnowledgePointTombstone、KnowledgeNumberSequence 随对应 UseCase 补齐。
 */
class RoomSchemaTest {

    private data class Column(val affinity: String, val notNull: Boolean)

    private data class IndexSpec(val columns: List<String>, val unique: Boolean)

    private val database: Map<String, JsonValue> by lazy {
        val dir = System.getProperty("room.schema.dir")?.let(::File) ?: File("schemas")
        val file = dir.resolve("com.recollect.data.room.RecollectDatabase/12.json")
        assertTrue(
            file.isFile,
            "Room schema 未生成：${file.absolutePath}。需要 @Database(exportSchema = true) 与 KSP room.schemaLocation。",
        )
        parseJson(file.readText()).asObj().required("database").asObj()
    }

    private val entities: Map<String, Map<String, JsonValue>> by lazy {
        database.required("entities").asArr().associate {
            val entity = it.asObj()
            entity.required("tableName").asString() to entity
        }
    }

    @Test
    fun schemaMatchesDataModel() {
        assertEquals(12, database.required("version").asInt(), "schema 版本")

        assertEquals(
            setOf(
                "knowledge_point",
                "question",
                "question_knowledge_cross_ref",
                "review_feedback",
                "confirmation_decision_log",
                "pending_feedback",
                "scan_session",
                "cooling_override_event",
                "plan_page",
                "note_attachment_item",
                "knowledge_point_tombstone",
                "knowledge_number_sequence",
                "plan",
                "source_tag",
                "app_settings",
            ),
            entities.keys,
            "v12 应落 data-model.md 已定义的 15 个实体（含来源标签库与每日复习时长设置）",
        )

        assertTable(
            table = "knowledge_point",
            columns = mapOf(
                "id" to Column("TEXT", true),
                "number" to Column("INTEGER", true),
                "name" to Column("TEXT", true),
                "currentTier" to Column("TEXT", true),
                "continuousScore" to Column("REAL", true),
                "effectiveFeedbackCount" to Column("REAL", true),
                "nextDueDate" to Column("TEXT", true),
                "activeQuestionCount" to Column("INTEGER", true),
                "dormant" to Column("INTEGER", true),
                "difficulty" to Column("INTEGER", false),
                "halfLifeDays" to Column("REAL", false),
                "createdAt" to Column("INTEGER", true),
            ),
            primaryKey = listOf("id"),
            autoGenerate = false,
            indices = mapOf(
                "index_knowledge_point_number" to IndexSpec(listOf("number"), unique = true),
            ),
        )

        assertTable(
            table = "question",
            columns = mapOf(
                "id" to Column("TEXT", true),
                "pageId" to Column("TEXT", true),
                "questionNumber" to Column("TEXT", true),
                "qrPayload" to Column("TEXT", true),
                "archived" to Column("INTEGER", true),
                "lastScheduledDate" to Column("TEXT", false),
                "lastReviewedDate" to Column("TEXT", false),
                "estimatedReviewMinutes" to Column("INTEGER", true),
                "createdAt" to Column("INTEGER", true),
                "archivedAt" to Column("INTEGER", false),
                "sourcePage" to Column("TEXT", true),
                "sourcePageCode" to Column("INTEGER", true),
            ),
            primaryKey = listOf("id"),
            autoGenerate = false,
        )

        assertTable(
            table = "question_knowledge_cross_ref",
            columns = mapOf(
                "questionId" to Column("TEXT", true),
                "knowledgePointId" to Column("TEXT", true),
                "weight" to Column("REAL", true),
            ),
            primaryKey = listOf("questionId", "knowledgePointId"),
            autoGenerate = false,
        )

        assertTable(
            table = "review_feedback",
            columns = mapOf(
                "id" to Column("INTEGER", true),
                "knowledgePointId" to Column("TEXT", true),
                "questionId" to Column("TEXT", true),
                "planId" to Column("TEXT", true),
                "pageId" to Column("TEXT", true),
                "plannedDate" to Column("TEXT", true),
                "reviewedOn" to Column("TEXT", true),
                "tier" to Column("TEXT", true),
                "triggered" to Column("INTEGER", true),
                "rawOcrText" to Column("TEXT", false),
                "correctedText" to Column("TEXT", false),
                "effectiveWeight" to Column("REAL", true),
                "weightApplied" to Column("REAL", true),
                "timestamp" to Column("INTEGER", true),
            ),
            primaryKey = listOf("id"),
            autoGenerate = true,
        )

        assertTable(
            table = "confirmation_decision_log",
            columns = mapOf(
                "id" to Column("INTEGER", true),
                "sessionId" to Column("TEXT", true),
                "planId" to Column("TEXT", true),
                "pageId" to Column("TEXT", true),
                "questionId" to Column("TEXT", true),
                "knowledgePointId" to Column("TEXT", true),
                "plannedDate" to Column("TEXT", true),
                "reviewedOn" to Column("TEXT", false),
                "triggered" to Column("INTEGER", true),
                "decision" to Column("TEXT", true),
                "selectedTier" to Column("TEXT", false),
                "correctedText" to Column("TEXT", false),
                "decidedAt" to Column("INTEGER", true),
            ),
            primaryKey = listOf("id"),
            autoGenerate = true,
        )

        assertTable(
            table = "pending_feedback",
            columns = mapOf(
                "sessionId" to Column("TEXT", true),
                "planId" to Column("TEXT", true),
                "pageId" to Column("TEXT", true),
                "plannedDate" to Column("TEXT", true),
                "actualReviewDate" to Column("TEXT", false),
                "questionId" to Column("TEXT", true),
                "knowledgePointId" to Column("TEXT", true),
                "triggered" to Column("INTEGER", true),
                "detectedTier" to Column("TEXT", false),
                "detectionStatus" to Column("TEXT", true),
                "conflict" to Column("INTEGER", true),
                "detectedRawOcr" to Column("TEXT", false),
                "userDecision" to Column("TEXT", true),
                "selectedTier" to Column("TEXT", false),
                "correctedText" to Column("TEXT", false),
            ),
            primaryKey = listOf("sessionId", "questionId", "knowledgePointId"),
            autoGenerate = false,
        )

        assertTable(
            table = "scan_session",
            columns = mapOf(
                "sessionId" to Column("TEXT", true),
                "planId" to Column("TEXT", true),
                "startedAt" to Column("INTEGER", true),
                "status" to Column("TEXT", true),
                "submittedAt" to Column("INTEGER", false),
            ),
            primaryKey = listOf("sessionId"),
            autoGenerate = false,
        )

        assertTable(
            table = "cooling_override_event",
            columns = mapOf(
                "id" to Column("INTEGER", true),
                "sessionId" to Column("TEXT", true),
                "questionId" to Column("TEXT", true),
                "createdAt" to Column("INTEGER", true),
                "consumedAt" to Column("INTEGER", false),
            ),
            primaryKey = listOf("id"),
            autoGenerate = true,
        )

        assertTable(
            table = "plan_page",
            columns = mapOf(
                "planId" to Column("TEXT", true),
                "pageId" to Column("TEXT", true),
                "pageNumber" to Column("INTEGER", true),
                "date" to Column("TEXT", true),
                "itemCount" to Column("INTEGER", true),
                "questionIds" to Column("TEXT", true),
                "knowledgeRows" to Column("TEXT", true),
                "continuationMarkers" to Column("TEXT", true),
                "itemGeometry" to Column("TEXT", true),
            ),
            primaryKey = listOf("planId", "pageId"),
            autoGenerate = false,
            indices = mapOf(
                "index_plan_page_pageId" to IndexSpec(listOf("pageId"), unique = true),
            ),
        )

        assertTable(
            table = "note_attachment_item",
            columns = mapOf(
                "id" to Column("INTEGER", true),
                "questionId" to Column("TEXT", true),
                "sourceFeedbackId" to Column("INTEGER", false),
                "text" to Column("TEXT", true),
                "imageUri" to Column("TEXT", false),
                "order" to Column("INTEGER", true),
                "createdAt" to Column("INTEGER", true),
            ),
            primaryKey = listOf("id"),
            autoGenerate = true,
        )

        assertTable(
            table = "knowledge_point_tombstone",
            columns = mapOf(
                "number" to Column("INTEGER", true),
                "knowledgePointId" to Column("TEXT", true),
                "voidedAt" to Column("INTEGER", true),
                "reason" to Column("TEXT", false),
            ),
            primaryKey = listOf("number"),
            autoGenerate = false,
        )

        assertTable(
            table = "knowledge_number_sequence",
            columns = mapOf(
                "id" to Column("INTEGER", true),
                "lastIssued" to Column("INTEGER", true),
            ),
            primaryKey = listOf("id"),
            autoGenerate = false,
        )

        assertTable(
            table = "plan",
            columns = mapOf(
                "planId" to Column("TEXT", true),
                "weekStart" to Column("TEXT", true),
                "weekEnd" to Column("TEXT", true),
                "createdAt" to Column("INTEGER", true),
                "status" to Column("TEXT", true),
                "printedAt" to Column("INTEGER", false),
                "supersededBy" to Column("TEXT", false),
                "renderMeta" to Column("TEXT", true),
                "deferred" to Column("TEXT", true),
            ),
            primaryKey = listOf("planId"),
            autoGenerate = false,
        )

        assertTable(
            table = "source_tag",
            columns = mapOf(
                "id" to Column("TEXT", true),
                "name" to Column("TEXT", true),
                "createdAt" to Column("INTEGER", true),
            ),
            primaryKey = listOf("id"),
            autoGenerate = false,
            indices = mapOf(
                "index_source_tag_name" to IndexSpec(listOf("name"), unique = true),
            ),
        )

        assertTable(
            table = "app_settings",
            columns = mapOf(
                "id" to Column("INTEGER", true),
                "dailyReviewMinutes" to Column("INTEGER", true),
            ),
            primaryKey = listOf("id"),
            autoGenerate = false,
        )

        // C-050：两个状态字段必须落库为 Float（schema 中为 REAL），不得退化为 REAL 以外类型。
        assertAffinity("knowledge_point", "continuousScore", "REAL")
        assertAffinity("knowledge_point", "effectiveFeedbackCount", "REAL")
        assertAffinity("knowledge_point", "halfLifeDays", "REAL")
        assertAffinity("question_knowledge_cross_ref", "weight", "REAL")
    }

    private fun assertTable(
        table: String,
        columns: Map<String, Column>,
        primaryKey: List<String>,
        autoGenerate: Boolean,
        indices: Map<String, IndexSpec> = emptyMap(),
    ) {
        val entity = entities[table] ?: error("schema 缺少表 $table")

        val fieldEntries = entity.required("fields").asArr().map { it.asObj() }
        val actualColumns = fieldEntries.associate { field ->
            field.required("fieldPath").asString() to Column(
                affinity = field.required("affinity").asString(),
                // Room schema 对可空列省略 notNull（缺省即 false）。
                notNull = field["notNull"]?.asBool() ?: false,
            )
        }
        assertEquals(columns, actualColumns, "$table 字段名/类型/可空性")

        fieldEntries.forEach { field ->
            assertEquals(
                field.required("fieldPath").asString(),
                field.required("columnName").asString(),
                "$table 不应使用与属性名不同的列名",
            )
        }

        val primaryKeyJson = entity.required("primaryKey").asObj()
        assertEquals(
            primaryKey,
            primaryKeyJson.required("columnNames").asArr().map { it.asString() },
            "$table 主键",
        )
        assertEquals(
            autoGenerate,
            primaryKeyJson.required("autoGenerate").asBool(),
            "$table 主键自增",
        )

        // Room schema 对空索引/空外键数组省略该字段（缺省即空）。
        val actualIndices = (entity["indices"]?.asArr() ?: emptyList()).associate { index ->
            val entry = index.asObj()
            entry.required("name").asString() to IndexSpec(
                columns = entry.required("columnNames").asArr().map { it.asString() },
                unique = entry.required("unique").asBool(),
            )
        }
        assertEquals(indices, actualIndices, "$table 索引")

        assertTrue(
            (entity["foreignKeys"]?.asArr() ?: emptyList()).isEmpty(),
            "$table 不应声明 data-model.md 之外的数据库外键；关联完整性由 data:room 的 DAO/Repository 保证（C-056）",
        )
    }

    private fun assertAffinity(table: String, field: String, affinity: String) {
        val entity = entities[table] ?: error("schema 缺少表 $table")
        val fieldEntry = entity.required("fields").asArr()
            .map { it.asObj() }
            .singleOrNull { it.required("fieldPath").asString() == field }
            ?: error("$table 缺少字段 $field")
        assertEquals(affinity, fieldEntry.required("affinity").asString(), "$table.$field 类型")
    }
}
