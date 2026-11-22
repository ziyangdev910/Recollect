package com.recollect.data.room.repository

import com.recollect.data.room.entity.KnowledgePoint
import com.recollect.data.room.entity.Question
import com.recollect.data.room.mapping.toKnowledgeState
import com.recollect.data.room.mapping.toSchedulerQuestion
import com.recollect.model.KnowledgeRow
import com.recollect.model.KnowledgeRowsCodec
import com.recollect.model.PlanPageRows
import com.recollect.model.Tier
import com.recollect.pagination.PaginationFailureCode
import com.recollect.pagination.PaginationResult
import com.recollect.pagination.PlacedItem
import com.recollect.pagination.PlannedPage
import com.recollect.scheduler.DefaultScheduler
import com.recollect.scheduler.DeferReason
import com.recollect.scheduler.DeferredItem
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 阶段 4.2：`PlanRepository.savePlan` 的单事务语义（repository.md「事务规则」）。
 *
 * - 成功时：PlanPage、`Question.lastScheduledDate`、`CoolingOverrideEvent` 消费与冷却顺延一起落库；
 * - 冷却顺延按调度器给出的日期原样写入，不改写天数、不夹取（C-057）；
 * - 任一步失败：整体回滚，不留下半份计划（先写回滚测试再实现）；
 * - C-047：分页失败在进入事务前转 `PlanGenerationException`，不写库。
 */
class PlanRepositoryTransactionTest {

    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = Instant.parse("2026-11-15T10:00:00Z")
    private val clock = Clock.fixed(now, zone)
    private val plannedDay = LocalDate.of(2026, 11, 16)
    private val planId = "AB12CD"

    private val stores = InMemoryStores()
    private val repository = RoomPlanRepository(
        transactionRunner = RollbackTransactionRunner(stores),
        planPageDao = FakePlanPageDao(stores),
        planDao = FakePlanDao(stores),
        questionDao = FakeQuestionDao(stores),
        coolingOverrideEventDao = FakeCoolingOverrideEventDao(stores),
        knowledgePointDao = FakeKnowledgePointDao(stores),
        clock = clock,
    )

    private fun knowledgePoint(id: String = "k1") = KnowledgePoint(
        id = id,
        number = 1,
        name = "一次函数",
        currentTier = Tier.UNKNOWN,
        continuousScore = 0.5f,
        effectiveFeedbackCount = 0.0f,
        nextDueDate = plannedDay,
        activeQuestionCount = 1,
        dormant = false,
        difficulty = null,
        halfLifeDays = null,
        createdAt = now,
    )

    private fun question(id: String) = Question(
        id = id,
        pageId = "",
        questionNumber = "1-01",
        qrPayload = "",
        archived = false,
        lastScheduledDate = null,
        lastReviewedDate = null,
        estimatedReviewMinutes = 15,
        createdAt = now,
        archivedAt = null,
    )

    private fun plannedPage(pageNumber: Int, date: LocalDate, questionId: String) = PlannedPage(
        pageId = "$planId-%02d".format(pageNumber),
        pageNumber = pageNumber,
        date = date,
        items = listOf(PlacedItem(questionId = questionId, yOffsetMm = 20.0, heightMm = 68.5)),
        continuationMarkers = emptyList(),
    )

    @Test
    fun `save plan registers the week and explicit regeneration supersedes the previous plan`() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint()
        stores.questions["q1"] = question("q1")
        val meta = PlanRenderMeta(
            questionId = "q1",
            questionNumber = "1-01",
            sourcePage = "P4",
            knowledgeNames = listOf("一次函数"),
            knowledgeLineCount = 1,
            attachmentCount = 0,
            hasNoteArea = true,
            noteTexts = emptyList(),
        )
        fun pageFor(id: String) = PlannedPage(
            pageId = "$id-01",
            pageNumber = 1,
            date = plannedDay,
            items = listOf(PlacedItem(questionId = "q1", yOffsetMm = 20.0, heightMm = 68.5)),
            continuationMarkers = emptyList(),
        )
        fun draft(id: String, supersedes: String? = null) = PlanDraft(
            planId = id,
            pages = listOf(pageFor(id)),
            knowledgeRowsByQuestionId = mapOf("q1" to rows("q1", "k1")),
            scheduledDates = mapOf("q1" to plannedDay),
            coolingEventIdsToConsume = emptyList(),
            deferred = emptyList(),
            weekStartDate = plannedDay,
            weekEndDate = plannedDay,
            renderMetaByQuestionId = mapOf("q1" to meta),
            supersedesPlanId = supersedes,
        )

        repository.savePlan(draft("OLD001"))
        assertEquals(PlanStatus.GENERATED.name, stores.plans.getValue("OLD001").status)
        assertEquals("OLD001", repository.activePlan(plannedDay, plannedDay)?.planId)

        repository.savePlan(draft("NEW002", supersedes = "OLD001"))

        assertEquals(PlanStatus.SUPERSEDED.name, stores.plans.getValue("OLD001").status)
        assertEquals("NEW002", stores.plans.getValue("OLD001").supersededBy)
        assertEquals("NEW002", repository.activePlan(plannedDay, plannedDay)?.planId)
        assertEquals(listOf("q1"), repository.storedPages("NEW002").single().questionIds)
        assertEquals(meta, repository.findPlan("NEW002")?.renderMeta?.single())
    }

    @Test
    fun `print snapshots only write for the active plan and mark it printed`() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint()
        stores.questions["q1"] = question("q1")
        fun draft(id: String, supersedes: String? = null) = PlanDraft(
            planId = id,
            pages = listOf(
                PlannedPage(
                    pageId = "$id-01",
                    pageNumber = 1,
                    date = plannedDay,
                    items = listOf(PlacedItem("q1", 20.0, 68.5)),
                    continuationMarkers = emptyList(),
                ),
            ),
            knowledgeRowsByQuestionId = mapOf("q1" to rows("q1", "k1")),
            scheduledDates = mapOf("q1" to plannedDay),
            coolingEventIdsToConsume = emptyList(),
            deferred = emptyList(),
            weekStartDate = plannedDay,
            weekEndDate = plannedDay,
            supersedesPlanId = supersedes,
        )
        repository.savePlan(draft("OLD001"))
        repository.savePlan(draft("NEW002", supersedes = "OLD001"))

        assertTrue(
            repository.savePrintSnapshots(
                planId = "NEW002",
                snapshots = listOf(
                    PrintSnapshotRecord("q1", "NEW002-01", "payload"),
                ),
                printedAt = now,
            ),
        )
        assertEquals("NEW002-01", stores.questions.getValue("q1").pageId)
        assertEquals("payload", stores.questions.getValue("q1").qrPayload)
        assertEquals(PlanStatus.PRINTED.name, stores.plans.getValue("NEW002").status)
        assertEquals(now, stores.plans.getValue("NEW002").printedAt)

        assertFalse(
            repository.savePrintSnapshots(
                planId = "OLD001",
                snapshots = listOf(
                    PrintSnapshotRecord("q1", "OLD001-01", "old"),
                ),
                printedAt = now,
            ),
            "旧计划不得覆盖当前计划的最新打印快照",
        )
        assertEquals("NEW002-01", stores.questions.getValue("q1").pageId)
        assertEquals("payload", stores.questions.getValue("q1").qrPayload)
    }

    private fun rows(questionId: String, knowledgePointId: String) = PlanPageRows(
        questionId = questionId,
        rows = listOf(KnowledgeRow(knowledgePointId = knowledgePointId, number = 1)),
        triggeredKnowledgePointIds = listOf(knowledgePointId),
    )

    @Test
    fun `save plan freezes the read-back geometry snapshot`() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint()
        stores.questions["q1"] = question("q1")
        val geometry = listOf(
            com.recollect.pagination.ScanItemGeometry(
                questionId = "q1",
                topMm = 20.0,
                heightMm = 68.5,
                rowCount = 1,
                noteArea = com.recollect.pagination.NoteWritingArea(92.2, 33.5, 88.8, 25.0),
            ),
        )

        repository.savePlan(
            PlanDraft(
                planId = planId,
                pages = listOf(plannedPage(pageNumber = 1, date = plannedDay, questionId = "q1")),
                knowledgeRowsByQuestionId = mapOf("q1" to rows("q1", "k1")),
                scheduledDates = mapOf("q1" to plannedDay),
                coolingEventIdsToConsume = emptyList(),
                deferred = emptyList(),
                geometryByPage = mapOf("$planId-01" to geometry),
            ),
        )

        val page = stores.planPages.getValue("$planId-01")
        assertEquals(geometry, ItemGeometryJson.decode(page.itemGeometry), "C-093 方案 A：几何随打印快照冻结")
    }

    @Test
    fun `save plan commits every write and rolls back fully when a later step fails`() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint()
        stores.questions["q1"] = question("q1")
        stores.questions["q2"] = question("q2")
        val eventId = stores.insertCoolingEvent(sessionId = "s1", questionId = "q1", createdAt = now)
        val deferredDate = plannedDay.plusDays(2) // 调度器给出的顺延目标，非 Repository 自行推导

        repository.savePlan(
            PlanDraft(
                planId = planId,
                pages = listOf(plannedPage(pageNumber = 1, date = plannedDay, questionId = "q1")),
                knowledgeRowsByQuestionId = mapOf("q1" to rows("q1", "k1")),
                scheduledDates = mapOf("q1" to plannedDay),
                coolingEventIdsToConsume = listOf(eventId),
                deferred = listOf(
                    DeferredItem(
                        questionId = "q1",
                        reason = DeferReason.COOLING,
                        knowledgePointIds = listOf("k1"),
                        targetNextDueDates = mapOf("k1" to deferredDate),
                    ),
                ),
            ),
        )

        val page = stores.planPages.getValue("$planId-01")
        assertEquals(1, page.itemCount)
        assertEquals("""["q1"]""", page.questionIds, "questionIds 按打印顺序序列化")
        assertEquals(
            KnowledgeRowsCodec.encode(listOf(rows("q1", "k1"))),
            page.knowledgeRows,
            "行快照原样冻结，不由 Repository 重算（C-071/C-072）",
        )
        assertEquals("[]", page.continuationMarkers)
        assertEquals(plannedDay, stores.questions.getValue("q1").lastScheduledDate)
        assertEquals(now, stores.coolingEvents.getValue(eventId).consumedAt, "C-008：命中的覆盖事件被消费")
        assertEquals(
            deferredDate,
            stores.knowledgePoints.getValue("k1").nextDueDate,
            "C-057：冷却顺延原样落库，不改写天数、不夹取",
        )

        // 第二份计划：页面与 lastScheduledDate 已写入后，冷却顺延一步失败 → 全部回滚。
        stores.failOnKnowledgePointUpdate = IllegalStateException("模拟写库失败")
        assertFailsWith<IllegalStateException> {
            repository.savePlan(
                PlanDraft(
                    planId = planId,
                    pages = listOf(plannedPage(pageNumber = 2, date = plannedDay.plusDays(1), questionId = "q2")),
                    knowledgeRowsByQuestionId = mapOf("q2" to rows("q2", "k1")),
                    scheduledDates = mapOf("q2" to plannedDay.plusDays(1)),
                    coolingEventIdsToConsume = emptyList(),
                    deferred = listOf(
                        DeferredItem(
                            questionId = "q2",
                            reason = DeferReason.COOLING,
                            knowledgePointIds = listOf("k1"),
                            targetNextDueDates = mapOf("k1" to deferredDate.plusDays(1)),
                        ),
                    ),
                ),
            )
        }

        assertNull(stores.planPages["$planId-02"], "失败计划的 PlanPage 不得残留")
        assertNull(stores.questions.getValue("q2").lastScheduledDate, "失败计划的 lastScheduledDate 不得残留")
        assertEquals(
            deferredDate,
            stores.knowledgePoints.getValue("k1").nextDueDate,
            "失败计划的冷却顺延不得残留",
        )
        assertEquals(2, stores.transactionCount, "两次 savePlan 各进入一次事务")
    }

    @Test
    fun `u14 cooling deferral advances one day per consecutive plan and zero disables cooling`() = runBlocking {
        val scheduler = DefaultScheduler()
        val lastReviewed = LocalDate.of(2026, 11, 10)
        val firstDay = LocalDate.of(2026, 11, 11)
        stores.knowledgePoints["k1"] = knowledgePoint().copy(nextDueDate = firstDay)
        stores.questions["q1"] = question("q1").copy(lastReviewedDate = lastReviewed)

        fun currentStates() = listOf(stores.knowledgePoints.getValue("k1").toKnowledgeState())
        fun currentQuestions() = listOf(
            stores.questions.getValue("q1").toSchedulerQuestion(listOf("k1")),
        )

        // 第 1 份计划：11-10 复习 + 7 天冷却 → 11-17 前都在冷却期，知识点顺延 1 天。
        val firstPlan = scheduler.plan(
            knowledgeStates = currentStates(),
            questions = currentQuestions(),
            dailyAvailableMinutes = mapOf(firstDay to 60),
            dateRange = firstDay..firstDay,
            coolingPeriodDays = 7,
            coolingOverrides = emptySet(),
        )
        assertEquals(emptyMap(), firstPlan.dailyAssignments, "冷却期内不排入")
        val firstDeferred = firstPlan.deferred.single()
        assertEquals(DeferReason.COOLING, firstDeferred.reason)
        assertEquals(mapOf("k1" to firstDay.plusDays(1)), firstDeferred.targetNextDueDates)

        repository.savePlan(
            PlanDraft(
                planId = planId,
                pages = emptyList(),
                knowledgeRowsByQuestionId = emptyMap(),
                scheduledDates = emptyMap(),
                coolingEventIdsToConsume = emptyList(),
                deferred = firstPlan.deferred,
            ),
        )
        assertEquals(firstDay.plusDays(1), stores.knowledgePoints.getValue("k1").nextDueDate)

        // 第 2 份计划：仍处冷却期，基于更新后的到期日再各 +1 天（C-021）。
        val secondDay = firstDay.plusDays(1)
        val secondPlan = scheduler.plan(
            knowledgeStates = currentStates(),
            questions = currentQuestions(),
            dailyAvailableMinutes = mapOf(secondDay to 60),
            dateRange = secondDay..secondDay,
            coolingPeriodDays = 7,
            coolingOverrides = emptySet(),
        )
        val secondDeferred = secondPlan.deferred.single()
        assertEquals(DeferReason.COOLING, secondDeferred.reason)
        assertEquals(mapOf("k1" to secondDay.plusDays(1)), secondDeferred.targetNextDueDates)

        repository.savePlan(
            PlanDraft(
                planId = planId,
                pages = emptyList(),
                knowledgeRowsByQuestionId = emptyMap(),
                scheduledDates = emptyMap(),
                coolingEventIdsToConsume = emptyList(),
                deferred = secondPlan.deferred,
            ),
        )
        assertEquals(
            firstDay.plusDays(2),
            stores.knowledgePoints.getValue("k1").nextDueDate,
            "连续两份计划各顺延 1 天",
        )

        // C-057 方案 D：0 = 关闭冷却，题目在到期日正常排入，不产生冷却顺延。
        val thirdDay = firstDay.plusDays(2)
        val withoutCooling = scheduler.plan(
            knowledgeStates = currentStates(),
            questions = currentQuestions(),
            dailyAvailableMinutes = mapOf(thirdDay to 60),
            dateRange = thirdDay..thirdDay,
            coolingPeriodDays = 0,
            coolingOverrides = emptySet(),
        )
        assertEquals(
            listOf("q1"),
            withoutCooling.dailyAssignments.getValue(thirdDay).map { it.questionId },
            "冷却天数为 0 时正常排程",
        )
        assertEquals(emptyList(), withoutCooling.deferred)
        assertFailsWith<IllegalArgumentException> {
            scheduler.plan(
                knowledgeStates = currentStates(),
                questions = currentQuestions(),
                dailyAvailableMinutes = mapOf(secondDay to 60),
                dateRange = secondDay..secondDay,
                coolingPeriodDays = -1,
                coolingOverrides = emptySet(),
            )
        }
        Unit // 测试方法必须返回 Unit，JUnit 才会收集
    }

    @Test
    fun `pagination failure becomes PlanGenerationException before any transaction`() = runBlocking {
        val failure = PaginationResult.Failure(
            code = PaginationFailureCode.PLAN_TOO_LONG,
            message = "计划超过 99 页，请缩短日期范围或减少每日题目",
        )

        val exception = assertFailsWith<PlanGenerationException> {
            PlanDraft.fromPagination(
                planId = planId,
                result = failure,
                knowledgeRowsByQuestionId = emptyMap(),
                scheduledDates = emptyMap(),
                coolingEventIdsToConsume = emptyList(),
                deferred = emptyList(),
            )
        }

        assertEquals(PaginationFailureCode.PLAN_TOO_LONG, exception.code)
        assertEquals(failure.message, exception.message)
        assertEquals(0, stores.transactionCount, "C-047：失败不得进入事务")
        assertEquals(0, stores.planPages.size, "C-047：失败不得写库")
    }
}
