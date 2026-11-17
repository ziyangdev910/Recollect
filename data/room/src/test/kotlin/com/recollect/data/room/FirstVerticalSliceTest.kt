package com.recollect.data.room

import com.recollect.data.room.entity.KnowledgePoint
import com.recollect.data.room.entity.Question
import com.recollect.data.room.entity.QuestionKnowledgeCrossRef
import com.recollect.data.room.mapping.toKnowledgeState
import com.recollect.data.room.mapping.toRenderableQuestion
import com.recollect.data.room.mapping.toSchedulerQuestion
import com.recollect.model.Tier
import com.recollect.pagination.DefaultPagePlanner
import com.recollect.pagination.PaginationResult
import com.recollect.pagination.RenderableQuestion
import com.recollect.pagination.TemplateGeometry
import com.recollect.scheduler.DefaultScheduler
import com.recollect.scheduler.PlanResult
import java.time.Instant
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * 阶段 3.3：首个跨模块纵切（core:model DTO → core:scheduler → data:room 映射 → core:pagination）。
 *
 * 场景：2 个知识点、2 道题；q1 同时关联 k1、k2（用于验证 D-01 的“一题多知识点只打印一次”），
 * q2 只关联 k2；两个知识点都在 11-16 到期，计划范围 11-16..11-17（11-17 无到期题目）。
 *
 * 断言：
 * - 1 页 2 条，`pageId = "{planId}-01"`（C-014/C-069，分页器只拼接）；
 * - D-01：同一题在整份计划中只出现一次，即使它关联了两个知识点；
 * - D-08：按日分页，每页只属于一个日期，只有“有题目的日期”建页；
 * - C-047：若分页器返回 `Failure`，原样记录 code/message 并让测试失败，不吞掉、不改写成 Success。
 */
class FirstVerticalSliceTest {

    private val start = LocalDate.of(2026, 11, 16)
    private val end = LocalDate.of(2026, 11, 17)
    private val planId = "AB12CD" // 6 位 Crockford Base32；分页器只拼接、不校验（C-069）
    private val createdAt = Instant.parse("2026-11-10T08:00:00Z")

    @Test
    fun `two knowledge points and two questions produce one page with two items`() {
        val k1 = knowledgePoint(id = "k1", number = 1, name = "一次函数")
        val k2 = knowledgePoint(id = "k2", number = 2, name = "二次函数")
        val knowledgePoints = listOf(k1, k2)

        val q1 = question(id = "q1", questionNumber = "1-01")
        val q2 = question(id = "q2", questionNumber = "1-02")
        val crossRefs = listOf(
            QuestionKnowledgeCrossRef(questionId = "q1", knowledgePointId = "k1"),
            QuestionKnowledgeCrossRef(questionId = "q1", knowledgePointId = "k2"),
            QuestionKnowledgeCrossRef(questionId = "q2", knowledgePointId = "k2"),
        )

        // 1) core:model 的调度 DTO（C-053）由 data:room 映射构造（C-046）；
        //    linkedKnowledgeIds 按 KnowledgePoint.number 升序（C-067 的前置条件）。
        val states = knowledgePoints.map { it.toKnowledgeState() }
        val schedulerQuestions = listOf(q1, q2).map {
            it.toSchedulerQuestion(crossRefs, knowledgePoints)
        }

        // 2) Scheduler.plan。
        val plan = DefaultScheduler().plan(
            knowledgeStates = states,
            questions = schedulerQuestions,
            dailyAvailableMinutes = mapOf(start to 60, end to 60),
            dateRange = start..end,
        )

        assertEquals(emptyList(), plan.deferred, "本场景不应有顺延/溢出")
        assertEquals(setOf(start), plan.dailyAssignments.keys, "两个知识点都在 11-16 到期，11-17 无题")
        assertEquals(
            listOf("q1", "q2"),
            plan.dailyAssignments.getValue(start).map { it.questionId },
            "两道题都应落在 11-16",
        )

        // 3) PlannedQuestion → RenderableQuestion（名称与左侧同一 printable 集合、按 number 升序，C-067）。
        val questionNumberOf = mapOf("q1" to q1.questionNumber, "q2" to q2.questionNumber)
        val dailyGroups = plan.toDailyGroups(knowledgePoints, questionNumberOf)

        // 4) PagePlanner.plan。
        val result = DefaultPagePlanner().plan(planId, dailyGroups, TemplateGeometry())

        // C-047：失败时必须显式记录 code/message 并停下，不得吞掉或改为 Success。
        val success = when (result) {
            is PaginationResult.Success -> result
            is PaginationResult.Failure -> fail(
                "C-047：PagePlanner 返回 Failure(code=${result.code})，message=${result.message}",
            )
        }

        // D-08：只有 11-16 有题，因此只有 1 页；11-17 不产生空白页。
        assertEquals(1, success.pages.size, "D-08：按日分页，无题日期不建页")
        val page = success.pages.single()

        // pageId 格式 "{planId}-{pageNumber:02d}"（C-014），全局唯一由计划层保证（C-069）。
        assertEquals("$planId-01", page.pageId, "pageId 必须是 {planId}-{pageNumber:02d}")
        assertEquals(1, page.pageNumber)
        assertEquals(start, page.date, "D-08：页面日期来自调度结果")

        // 1 页 2 条。
        assertEquals(2, page.items.size)
        assertEquals(listOf("q1", "q2"), page.items.map { it.questionId })
        assertEquals(emptyList(), success.overflowItems)
        assertEquals(emptyList(), success.warnings)

        // D-01：同一题只出现一次，即使 q1 同时关联 k1、k2。
        val printedQuestionIds = success.pages.flatMap { it.items }.map { it.questionId }
        assertEquals(
            printedQuestionIds.size,
            printedQuestionIds.distinct().size,
            "D-01：同一题目只打印一次",
        )
        assertEquals(1, printedQuestionIds.count { it == "q1" })
        assertEquals(1, printedQuestionIds.count { it == "q2" })
        assertEquals(
            2,
            dailyGroups.getValue(start).single { it.questionId == "q1" }.knowledgeCount,
            "q1 关联 2 个知识点，打印时仍是一条题目、两行知识点",
        )

        // D-08：每页只属于一个日期，页内题目必须来自该日期的调度结果；有题日期与页面日期一一对应。
        val scheduledByDate = plan.dailyAssignments.mapValues { (_, items) ->
            items.map { it.questionId }.toSet()
        }
        success.pages.forEach { plannedPage ->
            val scheduled = scheduledByDate.getValue(plannedPage.date)
            assertTrue(
                plannedPage.items.all { it.questionId in scheduled },
                "D-08：页 ${plannedPage.pageId} 只能包含其日期 ${plannedPage.date} 的调度题目",
            )
        }
        assertEquals(
            scheduledByDate.keys,
            success.pages.map { it.date }.toSet(),
            "D-08：每个有题目的日期各自起始新页，页面日期集合与调度日期集合一致",
        )
    }

    /**
     * 纵切中的编排胶水（规格里由计划层承担，本测试只验证模块组合）：
     * 把调度结果按日期转成打印投影，名称与左侧同一 printable 集合、按 `number` 升序（C-067）。
     */
    private fun PlanResult.toDailyGroups(
        knowledgePoints: List<KnowledgePoint>,
        questionNumbers: Map<String, String>,
    ): Map<LocalDate, List<RenderableQuestion>> {
        val numberOf = knowledgePoints.associate { it.id to it.number }
        val nameOf = knowledgePoints.associate { it.id to it.name }
        return dailyAssignments.mapValues { (_, plannedQuestions) ->
            plannedQuestions.map { planned ->
                val printableIds = (planned.triggeredKnowledgeIds + planned.nonTriggeredKnowledgeIds)
                    .sortedBy { id -> numberOf.getValue(id) }
                planned.toRenderableQuestion(
                    questionNumber = questionNumbers.getValue(planned.questionId),
                    knowledgeNames = printableIds.map { id -> nameOf.getValue(id) },
                    knowledgeLineCount = 1, // 右上名称换行数由打印层测量；本纵切只验证组合
                    attachmentCount = 0,
                )
            }
        }
    }

    private fun knowledgePoint(id: String, number: Int, name: String) = KnowledgePoint(
        id = id,
        number = number,
        name = name,
        currentTier = Tier.UNKNOWN,
        continuousScore = 0.5f,
        effectiveFeedbackCount = 0.0f,
        nextDueDate = start,
        activeQuestionCount = 1, // printable：!dormant && activeQuestionCount > 0
        dormant = false,
        difficulty = null,
        halfLifeDays = null,
        createdAt = createdAt,
    )

    private fun question(id: String, questionNumber: String) = Question(
        id = id,
        pageId = "",
        questionNumber = questionNumber,
        qrPayload = "",
        archived = false,
        lastScheduledDate = null,
        lastReviewedDate = null, // 无冷却历史，本场景不触发 D-04
        estimatedReviewMinutes = 15,
        createdAt = createdAt,
        archivedAt = null,
    )
}
