package com.recollect.data.room.mapping

import com.recollect.data.room.entity.KnowledgePoint
import com.recollect.data.room.entity.PlanPage
import com.recollect.data.room.entity.Question
import com.recollect.data.room.entity.QuestionKnowledgeCrossRef
import com.recollect.model.Tier
import com.recollect.scheduler.PlannedQuestion
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * 阶段 3.2：Room Entity ↔ 纯 DTO 映射（C-046 方案 A）。
 *
 * 只断言字段对应与规格要求的数据前置条件；不写入、不查库、不涉及调度/分页算法。
 */
class ModelMappingTest {

    private val day = LocalDate.of(2026, 11, 17)
    private val instant = Instant.parse("2026-11-17T08:30:00Z")

    private fun knowledgePoint(
        id: String,
        number: Int,
        tier: Tier = Tier.UNKNOWN,
        score: Float = 0.5f,
    ) = KnowledgePoint(
        id = id,
        number = number,
        name = "知识点 $number 的全称",
        currentTier = tier,
        continuousScore = score,
        effectiveFeedbackCount = 0.0f,
        nextDueDate = day,
        activeQuestionCount = 1,
        dormant = false,
        difficulty = null,
        halfLifeDays = null,
        createdAt = instant,
    )

    private fun question(
        id: String = "q1",
        questionNumber: String = "T-01",
        estimatedReviewMinutes: Int = 15,
        lastReviewedDate: LocalDate? = day.minusDays(2),
    ) = Question(
        id = id,
        pageId = "",
        questionNumber = questionNumber,
        qrPayload = "",
        archived = false,
        lastScheduledDate = null,
        lastReviewedDate = lastReviewedDate,
        estimatedReviewMinutes = estimatedReviewMinutes,
        createdAt = instant,
        archivedAt = null,
    )

    /** 字段一一对应；`name` 不进入调度 DTO；冷启动阶段 difficulty/halfLifeDays 为 null。 */
    @Test
    fun `knowledge point maps every scheduler field and keeps continuousScore as Float`() {
        val entity = KnowledgePoint(
            id = "k1",
            number = 3,
            name = "全称不进入调度 DTO",
            currentTier = Tier.FAMILIAR,
            continuousScore = 0.642f,
            effectiveFeedbackCount = 2.5f,
            nextDueDate = day,
            activeQuestionCount = 2,
            dormant = false,
            difficulty = null,
            halfLifeDays = null,
            createdAt = instant,
        )

        val state = entity.toKnowledgeState()

        assertEquals("k1", state.id)
        assertEquals(3, state.number)
        assertEquals(Tier.FAMILIAR, state.currentTier)
        assertIs<Float>(state.continuousScore, "C-050：对外 continuousScore 必须是 Float")
        assertEquals(0.642f, state.continuousScore)
        assertEquals(2.5f, state.effectiveFeedbackCount)
        assertEquals(day, state.nextDueDate)
        assertEquals(false, state.dormant)
        assertEquals(2, state.activeQuestionCount)
        assertEquals(null, state.difficulty, "冷启动阶段 difficulty 保持 null")
        assertEquals(null, state.halfLifeDays, "冷启动阶段 halfLifeDays 保持 null")
        assertEquals(instant, state.createdAt)
    }

    /** 调度字段一一对应；非正的 estimatedReviewMinutes 按 15 处理（C-017）。 */
    @Test
    fun `question maps scheduler fields and turns non-positive estimated minutes into 15`() {
        val entity = question(estimatedReviewMinutes = 0)

        val mapped = entity.toSchedulerQuestion(listOf("k1"))

        assertEquals("q1", mapped.id)
        assertEquals(listOf("k1"), mapped.linkedKnowledgeIds)
        assertEquals(false, mapped.archived)
        assertEquals(day.minusDays(2), mapped.lastReviewedDate)
        assertEquals(15, mapped.estimatedReviewMinutes, "0 分钟按默认 15 处理")

        assertEquals(
            15,
            entity.copy(estimatedReviewMinutes = -5).toSchedulerQuestion(listOf("k1")).estimatedReviewMinutes,
            "负分钟按默认 15 处理",
        )
        assertEquals(
            20,
            entity.copy(estimatedReviewMinutes = 20).toSchedulerQuestion(listOf("k1")).estimatedReviewMinutes,
            "正数原样保留",
        )
    }

    /** 关联从 cross-ref 读取：只取本题、按 KnowledgePoint.number 升序、id 唯一（C-046/C-056）。 */
    @Test
    fun `linked knowledge ids are unique and ordered by knowledge number`() {
        val entity = question()
        val crossRefs = listOf(
            QuestionKnowledgeCrossRef(questionId = "q1", knowledgePointId = "k9"),
            QuestionKnowledgeCrossRef(questionId = "q2", knowledgePointId = "k3"), // 其它题，不得混入
            QuestionKnowledgeCrossRef(questionId = "q1", knowledgePointId = "k5"),
            QuestionKnowledgeCrossRef(questionId = "q1", knowledgePointId = "k3"),
        )
        val knowledgePoints = listOf(
            knowledgePoint("k9", number = 9),
            knowledgePoint("k5", number = 5),
            knowledgePoint("k3", number = 3),
        )

        val mapped = entity.toSchedulerQuestion(crossRefs, knowledgePoints)

        assertEquals(
            listOf("k3", "k5", "k9"),
            mapped.linkedKnowledgeIds,
            "按 KnowledgePoint.number 升序",
        )
        assertEquals(
            mapped.linkedKnowledgeIds.size,
            mapped.linkedKnowledgeIds.toSet().size,
            "linkedKnowledgeIds 必须唯一（C-056）",
        )
    }

    /** 右上名称与左侧同一 printable 集合：数量等于 knowledgeCount（C-067）。 */
    @Test
    fun `planned question renders names whose count equals knowledgeCount`() {
        val planned = PlannedQuestion(
            questionId = "q1",
            triggeredKnowledgeIds = listOf("k5"),
            nonTriggeredKnowledgeIds = listOf("k3", "k9"),
            estimatedMinutes = 12,
        )
        val names = listOf("知识点 3 的全称", "知识点 5 的全称", "知识点 9 的全称")

        val renderable = planned.toRenderableQuestion(
            questionNumber = question().questionNumber,
            knowledgeNames = names,
            knowledgeLineCount = 2,
            attachmentCount = 1,
        )

        assertEquals("q1", renderable.questionId)
        assertEquals("T-01", renderable.questionNumber, "questionNumber 由 Question 实体补齐")
        assertEquals(3, renderable.knowledgeCount)
        assertEquals(names, renderable.knowledgeNames)
        assertEquals(renderable.knowledgeCount, renderable.knowledgeNames.size, "C-067")
        assertEquals(2, renderable.knowledgeLineCount)
        assertEquals(1, renderable.attachmentCount)
        assertTrue(renderable.hasNoteArea)
    }

    /** 行快照：每题记录带 questionId，rows 顺序按打印快照保留，不按 number 重排（C-070/C-015）。 */
    @Test
    fun `plan page rows keep questionId and frozen row order`() {
        val snapshot =
            """[{"questionId":"q1","rows":[{"knowledgePointId":"k5","number":5},{"knowledgePointId":"k3","number":3}],"triggeredKnowledgePointIds":["k5"]},{"questionId":"q2","rows":[{"knowledgePointId":"k1","number":1}],"triggeredKnowledgePointIds":[]}]"""
        val page = PlanPage(
            planId = "PLAN01",
            pageId = "PLAN01-01",
            pageNumber = 1,
            date = day,
            itemCount = 2,
            questionIds = """["q1","q2"]""",
            knowledgeRows = snapshot,
            continuationMarkers = "[]",
        )

        val rows = page.toPlanPageRows()

        assertEquals(listOf("q1", "q2"), rows.map { it.questionId }, "每题记录带 questionId（C-070）")
        assertEquals(listOf(5, 3), rows[0].rows.map { it.number }, "rows 顺序冻结，不按 number 重排（C-015）")
        assertEquals(listOf("k5", "k3"), rows[0].rows.map { it.knowledgePointId })
        assertEquals(listOf("k5"), rows[0].triggeredKnowledgePointIds)
        assertEquals(listOf(1), rows[1].rows.map { it.number })
    }
}
