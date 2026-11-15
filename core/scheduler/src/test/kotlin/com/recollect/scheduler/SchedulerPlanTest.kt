package com.recollect.scheduler

import com.recollect.model.KnowledgeState
import com.recollect.model.SchedulerQuestion
import com.recollect.model.Tier
import java.time.Instant
import java.time.LocalDate
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.RepetitionInfo
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SchedulerPlanTest {

    @Test
    fun `S-01 due knowledge point schedules its question on target date`() {
        val rangeStart = LocalDate.of(2026, 11, 16)
        val rangeEnd = LocalDate.of(2026, 11, 22)

        val k1 = KnowledgeState(
            id = "k1",
            number = 1,
            currentTier = Tier.UNKNOWN,
            continuousScore = 0f,
            nextDueDate = LocalDate.of(2026, 11, 16),
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 1,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )
        val q1 = SchedulerQuestion(
            id = "q1",
            linkedKnowledgeIds = listOf("k1"),
            archived = false,
            lastReviewedDate = null,
        )
        val dailyAvailableMinutes = (0L..6L).associate { rangeStart.plusDays(it) to 60 }

        val result = DefaultScheduler().plan(
            knowledgeStates = listOf(k1),
            questions = listOf(q1),
            dailyAvailableMinutes = dailyAvailableMinutes,
            dateRange = rangeStart..rangeEnd,
        )

        val day = assertNotNull(result.dailyAssignments[LocalDate.of(2026, 11, 16)])
        assertEquals(listOf("q1"), day.map { it.questionId })
        assertEquals(listOf("k1"), day.single().triggeredKnowledgeIds)
        assertEquals(15, day.single().estimatedMinutes)
        assertTrue(result.deferred.isEmpty())
    }

    @Test
    fun `S-03 question linking one due and one not due knowledge point is scheduled once`() {
        val rangeStart = LocalDate.of(2026, 11, 16)
        val rangeEnd = LocalDate.of(2026, 11, 22)
        val dueDate = rangeStart
        // 必须晚于 dateRange.endInclusive，才是“本期未到期”的非触发行。
        val laterDueDate = LocalDate.of(2026, 11, 25)

        fun state(id: String, number: Int, tier: Tier, score: Float, nextDue: LocalDate, feedback: Float) =
            KnowledgeState(
                id = id,
                number = number,
                currentTier = tier,
                continuousScore = score,
                nextDueDate = nextDue,
                effectiveFeedbackCount = feedback,
                dormant = false,
                activeQuestionCount = 1,
                difficulty = null,
                halfLifeDays = null,
                createdAt = Instant.parse("2026-11-01T00:00:00Z"),
            )

        val due = state("k-due", number = 1, Tier.UNKNOWN, 0.2f, dueDate, 0.5f)
        val notDue = state("k-not-due", number = 2, Tier.FAMILIAR, 0.7f, laterDueDate, 2.5f)
        val q1 = SchedulerQuestion(
            id = "q1",
            linkedKnowledgeIds = listOf(due.id, notDue.id),
            archived = false,
            lastReviewedDate = null,
        )
        val dailyAvailableMinutes = (0L..6L).associate { rangeStart.plusDays(it) to 60 }

        val result = DefaultScheduler().plan(
            knowledgeStates = listOf(due, notDue),
            questions = listOf(q1),
            dailyAvailableMinutes = dailyAvailableMinutes,
            dateRange = rangeStart..rangeEnd,
        )

        val allScheduledIds = result.dailyAssignments.values.flatMap { day -> day.map { it.questionId } }
        assertEquals(listOf("q1"), allScheduledIds, "同一题目在一份计划内只排入一次")
        val planned = assertNotNull(result.dailyAssignments[dueDate]).single()
        assertEquals(listOf("k-due"), planned.triggeredKnowledgeIds, "只有本期到期的知识点是触发点")
        assertEquals(listOf("k-not-due"), planned.nonTriggeredKnowledgeIds, "未到期知识点作为非触发行打印")
        assertTrue(result.deferred.isEmpty())
    }

    @Test
    fun `S-11 non-triggered knowledge point is printed but its scheduling state is not changed`() {
        val rangeStart = LocalDate.of(2026, 11, 16)
        val rangeEnd = LocalDate.of(2026, 11, 22)
        val dueDate = rangeStart
        // 必须晚于 dateRange.endInclusive，才是“本期未到期”的非触发行。
        val notDueDate = LocalDate.of(2026, 11, 25)

        fun state(id: String, number: Int, tier: Tier, score: Float, nextDue: LocalDate, feedback: Float) =
            KnowledgeState(
                id = id,
                number = number,
                currentTier = tier,
                continuousScore = score,
                nextDueDate = nextDue,
                effectiveFeedbackCount = feedback,
                dormant = false,
                activeQuestionCount = 1,
                difficulty = null,
                halfLifeDays = null,
                createdAt = Instant.parse("2026-11-01T00:00:00Z"),
            )

        val due = state("k-due", number = 1, Tier.UNKNOWN, 0.2f, dueDate, 0.5f)
        val notDue = state("k-not-due", number = 2, Tier.FAMILIAR, 0.7f, notDueDate, 2.5f)
        val notDueBeforePlan = notDue.copy()
        val states = listOf(due, notDue)
        val q1 = SchedulerQuestion(
            id = "q1",
            linkedKnowledgeIds = listOf(due.id, notDue.id),
            archived = false,
            lastReviewedDate = null,
        )
        val dailyAvailableMinutes = (0L..6L).associate { rangeStart.plusDays(it) to 60 }

        val result = DefaultScheduler().plan(
            knowledgeStates = states,
            questions = listOf(q1),
            dailyAvailableMinutes = dailyAvailableMinutes,
            dateRange = rangeStart..rangeEnd,
        )

        val planned = assertNotNull(result.dailyAssignments[dueDate]).single()
        assertEquals(listOf("k-not-due"), planned.nonTriggeredKnowledgeIds)
        assertTrue(
            planned.triggeredKnowledgeIds.none { it == "k-not-due" },
            "未到期知识点不得成为触发点",
        )
        // D-01 的状态更新断言（档位/下次到期日/有效反馈数不变）由 feedback-usecase U-11 承载；
        // scheduler 是纯函数，这里锁定它在 plan() 前后不改动输入状态。
        assertEquals(notDueBeforePlan, states[1], "调度器不得改写知识点的调度状态")
        assertEquals(Tier.FAMILIAR, states[1].currentTier)
        assertEquals(notDueDate, states[1].nextDueDate)
        assertEquals(2.5f, states[1].effectiveFeedbackCount)
    }

    @Test
    fun `S-08 archived question never appears in dailyAssignments`() {
        val rangeStart = LocalDate.of(2026, 11, 16)
        val rangeEnd = LocalDate.of(2026, 11, 22)

        // k1 同时关联归档题与活跃题，仍到期：归档题不得借关联知识点混入计划。
        val k1 = KnowledgeState(
            id = "k1",
            number = 1,
            currentTier = Tier.UNKNOWN,
            continuousScore = 0f,
            nextDueDate = LocalDate.of(2026, 11, 16),
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 2,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )
        val archivedQuestion = SchedulerQuestion(
            id = "q-archived",
            linkedKnowledgeIds = listOf("k1"),
            archived = true,
            lastReviewedDate = null,
        )
        val activeQuestion = SchedulerQuestion(
            id = "q-active",
            linkedKnowledgeIds = listOf("k1"),
            archived = false,
            lastReviewedDate = null,
        )
        val dailyAvailableMinutes = (0L..6L).associate { rangeStart.plusDays(it) to 60 }

        val result = DefaultScheduler().plan(
            knowledgeStates = listOf(k1),
            questions = listOf(archivedQuestion, activeQuestion),
            dailyAvailableMinutes = dailyAvailableMinutes,
            dateRange = rangeStart..rangeEnd,
        )

        val scheduledIds = result.dailyAssignments.values.flatMap { day -> day.map { it.questionId } }
        assertEquals(listOf("q-active"), scheduledIds)
        assertTrue(scheduledIds.none { it == "q-archived" })
    }

    @Test
    fun `S-09 dormant or zero-active knowledge points are neither scheduled nor non-triggered`() {
        val rangeStart = LocalDate.of(2026, 11, 16)
        val rangeEnd = LocalDate.of(2026, 11, 22)
        val dueDate = LocalDate.of(2026, 11, 16)

        fun state(id: String, number: Int, dormant: Boolean, activeQuestionCount: Int) = KnowledgeState(
            id = id,
            number = number,
            currentTier = Tier.UNKNOWN,
            continuousScore = 0f,
            nextDueDate = dueDate,
            effectiveFeedbackCount = 0f,
            dormant = dormant,
            activeQuestionCount = activeQuestionCount,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )

        val dormantState = state("k-dormant", number = 1, dormant = true, activeQuestionCount = 1)
        val zeroActiveState = state("k-zero", number = 2, dormant = false, activeQuestionCount = 0)
        val activeState = state("k-active", number = 3, dormant = false, activeQuestionCount = 1)

        val dormantOnly = SchedulerQuestion(
            id = "q-dormant-only",
            linkedKnowledgeIds = listOf(dormantState.id),
            archived = false,
            lastReviewedDate = null,
        )
        val zeroActiveOnly = SchedulerQuestion(
            id = "q-zero-only",
            linkedKnowledgeIds = listOf(zeroActiveState.id),
            archived = false,
            lastReviewedDate = null,
        )
        val mixed = SchedulerQuestion(
            id = "q-mixed",
            linkedKnowledgeIds = listOf(dormantState.id, zeroActiveState.id, activeState.id),
            archived = false,
            lastReviewedDate = null,
        )
        val dailyAvailableMinutes = (0L..6L).associate { rangeStart.plusDays(it) to 60 }

        val result = DefaultScheduler().plan(
            knowledgeStates = listOf(dormantState, zeroActiveState, activeState),
            questions = listOf(dormantOnly, zeroActiveOnly, mixed),
            dailyAvailableMinutes = dailyAvailableMinutes,
            dateRange = rangeStart..rangeEnd,
        )

        val day = assertNotNull(result.dailyAssignments[dueDate])
        assertEquals(listOf("q-mixed"), day.map { it.questionId })
        assertEquals(listOf("k-active"), day.single().triggeredKnowledgeIds)
        assertTrue(day.single().nonTriggeredKnowledgeIds.isEmpty())

        val scheduledIds = result.dailyAssignments.values.flatMap { dayItems -> dayItems.map { it.questionId } }
        assertTrue(scheduledIds.none { it == "q-dormant-only" || it == "q-zero-only" })
    }

    @Test
    fun `S-02 cooling question is deferred and every triggered knowledge point slides one day`() {
        val rangeStart = LocalDate.of(2026, 11, 16)
        val rangeEnd = LocalDate.of(2026, 11, 22)
        val targetDate = LocalDate.of(2026, 11, 16)

        fun state(id: String, number: Int) = KnowledgeState(
            id = id,
            number = number,
            currentTier = Tier.UNKNOWN,
            continuousScore = 0f,
            nextDueDate = targetDate,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 1,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )

        val k1 = state("k1", number = 1)
        val k2 = state("k2", number = 2)

        // 冷却未结束：2026-11-10 + 7 = 2026-11-17 > targetDate，题目不得排入。
        val coolingQuestion = SchedulerQuestion(
            id = "q-cooling",
            linkedKnowledgeIds = listOf("k1", "k2"),
            archived = false,
            lastReviewedDate = LocalDate.of(2026, 11, 10),
        )
        // 边界：2026-11-09 + 7 == targetDate；冷却判定是严格大于，故仍正常排入。
        val boundaryQuestion = SchedulerQuestion(
            id = "q-boundary",
            linkedKnowledgeIds = listOf("k1", "k2"),
            archived = false,
            lastReviewedDate = LocalDate.of(2026, 11, 9),
        )
        val dailyAvailableMinutes = (0L..6L).associate { rangeStart.plusDays(it) to 60 }

        val result = DefaultScheduler().plan(
            knowledgeStates = listOf(k1, k2),
            questions = listOf(coolingQuestion, boundaryQuestion),
            dailyAvailableMinutes = dailyAvailableMinutes,
            dateRange = rangeStart..rangeEnd,
            coolingPeriodDays = 7,
        )

        val scheduledIds = result.dailyAssignments.values.flatMap { day -> day.map { it.questionId } }
        assertEquals(listOf("q-boundary"), scheduledIds, "冷却期内不排入；lastReviewedDate + 7 == targetDate 应正常排入")

        val day = assertNotNull(result.dailyAssignments[targetDate])
        assertEquals(listOf("k1", "k2"), day.single().triggeredKnowledgeIds)

        assertEquals(
            listOf(
                DeferredItem(
                    questionId = "q-cooling",
                    reason = DeferReason.COOLING,
                    knowledgePointIds = listOf("k1", "k2"),
                    targetNextDueDates = mapOf(
                        "k1" to LocalDate.of(2026, 11, 17),
                        "k2" to LocalDate.of(2026, 11, 17),
                    ),
                ),
            ),
            result.deferred,
        )
    }

    @Test
    fun `S-19 cooling override skips cooling only for the matching unconsumed question id`() {
        val rangeStart = LocalDate.of(2026, 11, 16)
        val rangeEnd = LocalDate.of(2026, 11, 22)
        val targetDate = LocalDate.of(2026, 11, 16)

        val k1 = KnowledgeState(
            id = "k1",
            number = 1,
            currentTier = Tier.UNKNOWN,
            continuousScore = 0f,
            nextDueDate = targetDate,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 1,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )
        // 2026-11-10 + 7 = 2026-11-17 > targetDate，默认处于冷却期。
        val q1 = SchedulerQuestion(
            id = "q1",
            linkedKnowledgeIds = listOf("k1"),
            archived = false,
            lastReviewedDate = LocalDate.of(2026, 11, 10),
        )
        val dailyAvailableMinutes = (0L..6L).associate { rangeStart.plusDays(it) to 60 }

        fun plan(overrides: Set<String>) = DefaultScheduler().plan(
            knowledgeStates = listOf(k1),
            questions = listOf(q1),
            dailyAvailableMinutes = dailyAvailableMinutes,
            dateRange = rangeStart..rangeEnd,
            coolingPeriodDays = 7,
            coolingOverrides = overrides,
        )

        // 命中未消费的一次性覆盖事件：本次跳过冷却，正常排入。
        val overridden = plan(setOf("q1"))
        assertEquals(listOf("q1"), overridden.dailyAssignments[targetDate]?.map { it.questionId })
        assertTrue(overridden.deferred.isEmpty())

        // 未命中（或事件已被消费、不在集合中）：冷却照常生效。
        val missed = plan(setOf("q-other"))
        assertTrue(missed.dailyAssignments.isEmpty())
        assertEquals(listOf("q1"), missed.deferred.map { it.questionId })
        assertEquals(DeferReason.COOLING, missed.deferred.single().reason)

        // 覆盖只在本次 plan() 生效：下一份计划没有覆盖事件时重新进入冷却。
        val nextPlan = plan(emptySet())
        assertTrue(nextPlan.dailyAssignments.isEmpty())
        assertEquals(listOf("q1"), nextPlan.deferred.map { it.questionId })
    }

    @Test
    fun `S-04 tier gap sorts first within the same group tier but never eliminates candidates`() {
        val rangeStart = LocalDate.of(2026, 11, 16)
        val rangeEnd = LocalDate.of(2026, 11, 22)
        val targetDate = LocalDate.of(2026, 11, 16)

        fun state(id: String, tier: Tier, score: Float, dueDate: LocalDate): KnowledgeState = KnowledgeState(
            id = id,
            number = 1,
            currentTier = tier,
            continuousScore = score,
            nextDueDate = dueDate,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 1,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )

        // 三题同属 MASTERED 组：
        // q-gap 另关联本期未到期的 FAMILIAR（有档位差），组内分 0.9；
        // q-no-gap 组内分 0.1（最低）；q-same-tier 另关联本期未到期的同档 MASTERED（无档位差），组内分 0.8。
        // 若删掉档位差键，q-no-gap 会因分数更低排前；若把档位差误写成“存在任何非触发点”，
        // q-same-tier 会被错误提前到 q-no-gap 之前。
        val masteredHigh = state("k-mastered-high", Tier.MASTERED, 0.9f, targetDate)
        val familiarLater = state("k-familiar-later", Tier.FAMILIAR, 0.4f, LocalDate.of(2026, 11, 25))
        val qGap = SchedulerQuestion(
            id = "q-gap",
            linkedKnowledgeIds = listOf("k-mastered-high", "k-familiar-later"),
            archived = false,
            lastReviewedDate = null,
        )
        val masteredLow = state("k-mastered-low", Tier.MASTERED, 0.1f, targetDate)
        val qNoGap = SchedulerQuestion(
            id = "q-no-gap",
            linkedKnowledgeIds = listOf("k-mastered-low"),
            archived = false,
            lastReviewedDate = null,
        )
        val masteredMid = state("k-mastered-mid", Tier.MASTERED, 0.8f, targetDate)
        val masteredLater = state("k-mastered-later", Tier.MASTERED, 0.2f, LocalDate.of(2026, 11, 25))
        val qSameTier = SchedulerQuestion(
            id = "q-same-tier",
            linkedKnowledgeIds = listOf("k-mastered-mid", "k-mastered-later"),
            archived = false,
            lastReviewedDate = null,
        )
        val dailyAvailableMinutes = (0L..6L).associate { rangeStart.plusDays(it) to 60 }

        // 输入顺序故意打乱；输出必须按排序键而不是输入顺序。
        val result = DefaultScheduler().plan(
            knowledgeStates = listOf(masteredHigh, familiarLater, masteredLow, masteredMid, masteredLater),
            questions = listOf(qSameTier, qNoGap, qGap),
            dailyAvailableMinutes = dailyAvailableMinutes,
            dateRange = rangeStart..rangeEnd,
        )

        val day = assertNotNull(result.dailyAssignments[targetDate])
        assertEquals(
            listOf("q-gap", "q-no-gap", "q-same-tier"),
            day.map { it.questionId },
            "同档位组内只有 q-gap 有档位差；q-no-gap(0.1) 在 q-same-tier(0.8) 前，后者不因存在同档非触发点被提前",
        )
        assertEquals(listOf("k-mastered-high"), day.first().triggeredKnowledgeIds)
        assertEquals(listOf("k-familiar-later"), day.first().nonTriggeredKnowledgeIds)
    }

    @Test
    fun `C-059 higher group tier precedes a lower tier question even when the latter has a tier gap`() {
        val rangeStart = LocalDate.of(2026, 11, 16)
        val rangeEnd = LocalDate.of(2026, 11, 22)
        val targetDate = rangeStart

        fun state(id: String, tier: Tier, score: Float, dueDate: LocalDate) = KnowledgeState(
            id = id,
            number = 1,
            currentTier = tier,
            continuousScore = score,
            nextDueDate = dueDate,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 1,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )

        val highDue = state("k-high", Tier.MASTERED, 0.1f, targetDate)
        val lowDue = state("k-low", Tier.UNKNOWN, 0.99f, targetDate)
        val familiarLater = state("k-familiar-later", Tier.FAMILIAR, 0.4f, LocalDate.of(2026, 11, 25))

        // 低档位题虽然带档位差标记，也不得越过任何高档位题。
        val qLowGap = SchedulerQuestion(
            id = "q-low-gap",
            linkedKnowledgeIds = listOf("k-low", "k-familiar-later"),
            archived = false,
            lastReviewedDate = null,
        )
        val qHighNoGap = SchedulerQuestion(
            id = "q-high-no-gap",
            linkedKnowledgeIds = listOf("k-high"),
            archived = false,
            lastReviewedDate = null,
        )

        val result = DefaultScheduler().plan(
            knowledgeStates = listOf(highDue, lowDue, familiarLater),
            questions = listOf(qLowGap, qHighNoGap),
            dailyAvailableMinutes = (0L..6L).associate { rangeStart.plusDays(it) to 60 },
            dateRange = rangeStart..rangeEnd,
        )

        assertEquals(
            listOf("q-high-no-gap", "q-low-gap"),
            assertNotNull(result.dailyAssignments[targetDate]).map { it.questionId },
            "最高档位分组优先于档位差；低档位题即使带档位差标记也不得越过高档位题",
        )
    }

    @Test
    fun `S-05b time overflow is first-fit and keeps trying later candidates`() {
        val rangeStart = LocalDate.of(2026, 11, 16)
        val rangeEnd = LocalDate.of(2026, 11, 22)
        val targetDate = rangeStart

        val k1 = KnowledgeState(
            id = "k1",
            number = 1,
            currentTier = Tier.UNKNOWN,
            continuousScore = 0f,
            nextDueDate = targetDate,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 3,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )

        fun question(id: String, minutes: Int) = SchedulerQuestion(
            id = id,
            linkedKnowledgeIds = listOf("k1"),
            archived = false,
            lastReviewedDate = null,
            estimatedReviewMinutes = minutes,
        )

        val q1 = question("q1", 20)
        val q2 = question("q2", 20)
        val q3 = question("q3", 10)
        // 输入顺序故意打乱：输出必须按候排序键，而不是输入顺序。
        val dailyAvailableMinutes = mapOf(targetDate to 30)

        val result = DefaultScheduler().plan(
            knowledgeStates = listOf(k1),
            questions = listOf(q3, q2, q1),
            dailyAvailableMinutes = dailyAvailableMinutes,
            dateRange = rangeStart..rangeEnd,
        )

        val day = assertNotNull(result.dailyAssignments[targetDate])
        assertEquals(
            listOf("q1", "q3"),
            day.map { it.questionId },
            "容量 30：q1(20) 排入后，q2(20) 溢出但不得停止整日，q3(10) 仍应排入",
        )
        assertEquals(listOf(20, 10), day.map { it.estimatedMinutes })
        assertEquals(
            listOf(DeferredItem(questionId = "q2", reason = DeferReason.TIME_OVERFLOW)),
            result.deferred,
        )
    }

    @Test
    fun `S-15 missing or non-positive estimated minutes default to 15`() {
        val rangeStart = LocalDate.of(2026, 11, 16)
        val rangeEnd = LocalDate.of(2026, 11, 22)
        val targetDate = rangeStart

        val k1 = KnowledgeState(
            id = "k1",
            number = 1,
            currentTier = Tier.UNKNOWN,
            continuousScore = 0f,
            nextDueDate = targetDate,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 4,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )

        // 缺失：使用 SchedulerQuestion 的默认值；0 与 -5：按 15 分钟处理；25：自定义值原样参与累加。
        val missing = SchedulerQuestion(
            id = "q1",
            linkedKnowledgeIds = listOf("k1"),
            archived = false,
            lastReviewedDate = null,
        )

        fun question(id: String, minutes: Int) = SchedulerQuestion(
            id = id,
            linkedKnowledgeIds = listOf("k1"),
            archived = false,
            lastReviewedDate = null,
            estimatedReviewMinutes = minutes,
        )

        val zero = question("q2", 0)
        val negative = question("q3", -5)
        val custom = question("q4", 25)
        // 容量恰好等于归一化后的总和 15+15+15+25=70：任何未归一化的值都会让断言暴露不一致。
        val dailyAvailableMinutes = mapOf(targetDate to 70)

        val result = DefaultScheduler().plan(
            knowledgeStates = listOf(k1),
            questions = listOf(custom, negative, zero, missing),
            dailyAvailableMinutes = dailyAvailableMinutes,
            dateRange = rangeStart..rangeEnd,
        )

        val day = assertNotNull(result.dailyAssignments[targetDate])
        assertEquals(listOf("q1", "q2", "q3", "q4"), day.map { it.questionId })
        assertEquals(
            listOf(15, 15, 15, 25),
            day.map { it.estimatedMinutes },
            "缺失/0/-5 一律按 15 分钟；自定义 25 分钟原样参与容量累加",
        )
        assertTrue(result.deferred.isEmpty(), "归一化后恰好填满 70 分钟容量，不应有溢出")
    }

    @Test
    fun `S-17 overflow never spills to later days and later candidates never backfill earlier days`() {
        val day1 = LocalDate.of(2026, 11, 16)
        val day2 = LocalDate.of(2026, 11, 17)

        fun state(id: String, number: Int, dueDate: LocalDate) = KnowledgeState(
            id = id,
            number = number,
            currentTier = Tier.UNKNOWN,
            continuousScore = 0f,
            nextDueDate = dueDate,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 2,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )

        fun question(id: String, knowledgeId: String, minutes: Int) = SchedulerQuestion(
            id = id,
            linkedKnowledgeIds = listOf(knowledgeId),
            archived = false,
            lastReviewedDate = null,
            estimatedReviewMinutes = minutes,
        )

        val k1 = state("k1", number = 1, dueDate = day1)
        val k2 = state("k2", number = 2, dueDate = day2)
        val q1 = question("q1", "k1", 20)
        val q2 = question("q2", "k1", 20)
        // q3 只需 10 分钟，恰好能填满第 1 天剩余容量；但它属于第 2 天，不得回填第 1 天。
        val q3 = question("q3", "k2", 10)
        val dailyAvailableMinutes = mapOf(day1 to 30, day2 to 60)

        val result = DefaultScheduler().plan(
            knowledgeStates = listOf(k1, k2),
            questions = listOf(q1, q2, q3),
            dailyAvailableMinutes = dailyAvailableMinutes,
            dateRange = day1..day2,
        )

        assertEquals(
            listOf("q1"),
            assertNotNull(result.dailyAssignments[day1]).map { it.questionId },
            "第 1 天容量 30：只有 q1(20) 能排入，q3 属于第 2 天，不得回填第 1 天余量",
        )
        assertEquals(
            listOf(DeferredItem(questionId = "q2", reason = DeferReason.TIME_OVERFLOW)),
            result.deferred,
            "第 1 天溢出的 q2 只记 TIME_OVERFLOW，不顺延到第 2 天",
        )
        assertEquals(
            listOf("q3"),
            assertNotNull(result.dailyAssignments[day2]).map { it.questionId },
            "第 2 天只排入自己的候选 q3",
        )
        val allScheduledIds = result.dailyAssignments.values.flatMap { day -> day.map { it.questionId } }
        assertTrue(allScheduledIds.none { it == "q2" }, "q2 不得借后续日期混入计划")
    }

    @Test
    fun `S-05 S-18 continuous score is compared only inside the same group tier`() {
        val rangeStart = LocalDate.of(2026, 11, 16)
        val rangeEnd = LocalDate.of(2026, 11, 22)
        val targetDate = LocalDate.of(2026, 11, 16)

        fun state(id: String, tier: Tier, score: Float): KnowledgeState = KnowledgeState(
            id = id,
            number = 1,
            currentTier = tier,
            continuousScore = score,
            nextDueDate = targetDate,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 1,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )

        val masteredHigh = state("k-mastered-high", Tier.MASTERED, 0.9f)
        val unknownLow = state("k-unknown-low", Tier.UNKNOWN, 0.1f)
        val masteredMid = state("k-mastered-mid", Tier.MASTERED, 0.5f)
        val unknownHigh = state("k-unknown-high", Tier.UNKNOWN, 0.99f)

        // S-18：q1 同时触发 MASTERED 0.9 与 UNKNOWN 0.1；组内分数只能取 0.9。
        val q1 = SchedulerQuestion(
            id = "q1",
            linkedKnowledgeIds = listOf("k-mastered-high", "k-unknown-low"),
            archived = false,
            lastReviewedDate = null,
        )
        // q2 只触发 MASTERED 0.5，必须排在 q1 之前（0.5 < 0.9）。
        val q2 = SchedulerQuestion(
            id = "q2",
            linkedKnowledgeIds = listOf("k-mastered-mid"),
            archived = false,
            lastReviewedDate = null,
        )
        // S-05：UNKNOWN 0.99 不得越过任何 MASTERED 组。
        val q3 = SchedulerQuestion(
            id = "q3",
            linkedKnowledgeIds = listOf("k-unknown-high"),
            archived = false,
            lastReviewedDate = null,
        )
        // 时间容量给足，本轮只验证排序，不与装箱混在一起。
        val dailyAvailableMinutes = (0L..6L).associate { rangeStart.plusDays(it) to 200 }

        // 输入顺序故意打乱：q1 在最前，q3 在中间。
        val result = DefaultScheduler().plan(
            knowledgeStates = listOf(masteredHigh, unknownLow, masteredMid, unknownHigh),
            questions = listOf(q1, q3, q2),
            dailyAvailableMinutes = dailyAvailableMinutes,
            dateRange = rangeStart..rangeEnd,
        )

        val day = assertNotNull(result.dailyAssignments[targetDate])
        assertEquals(
            listOf("q2", "q1", "q3"),
            day.map { it.questionId },
            "MASTERED 组按组内最小分升序：q2(0.5) 在 q1(0.9) 前；q1 不得混入 UNKNOWN 的 0.1；" +
                "UNKNOWN 0.99 也不得排到 MASTERED 之前",
        )
        assertEquals(listOf("k-mastered-high", "k-unknown-low"), day[1].triggeredKnowledgeIds)
    }

    @Test
    fun `S-13 overdue knowledge point lands on range start and daily keys stay ascending`() {
        val rangeStart = LocalDate.of(2026, 11, 16)
        val rangeEnd = LocalDate.of(2026, 11, 22)
        val laterDueDate = LocalDate.of(2026, 11, 18)

        fun state(id: String, number: Int, dueDate: LocalDate) = KnowledgeState(
            id = id,
            number = number,
            currentTier = Tier.UNKNOWN,
            continuousScore = 0f,
            nextDueDate = dueDate,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 1,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )

        // k-overdue 早于计划首日 6 天到期：必须落到 dateRange.start，不得丢弃。
        val kOverdue = state("k-overdue", number = 1, dueDate = LocalDate.of(2026, 11, 10))
        val kLater = state("k-later", number = 2, dueDate = laterDueDate)

        val qOverdue = SchedulerQuestion(
            id = "q-overdue",
            linkedKnowledgeIds = listOf("k-overdue"),
            archived = false,
            lastReviewedDate = null,
        )
        val qLater = SchedulerQuestion(
            id = "q-later",
            linkedKnowledgeIds = listOf("k-later"),
            archived = false,
            lastReviewedDate = null,
        )
        val dailyAvailableMinutes = (0L..6L).associate { rangeStart.plusDays(it) to 60 }

        // 输入顺序与目标日相反：输出顺序只能来自日期，不能来自输入。
        val result = DefaultScheduler().plan(
            knowledgeStates = listOf(kOverdue, kLater),
            questions = listOf(qLater, qOverdue),
            dailyAvailableMinutes = dailyAvailableMinutes,
            dateRange = rangeStart..rangeEnd,
        )

        assertEquals(
            listOf(rangeStart, laterDueDate),
            result.dailyAssignments.keys.toList(),
            "逾期落到计划首日；日期键升序；没有条目的日期不出现",
        )
        val firstDay = assertNotNull(result.dailyAssignments[rangeStart])
        assertEquals(listOf("q-overdue"), firstDay.map { it.questionId })
        assertEquals(listOf("k-overdue"), firstDay.single().triggeredKnowledgeIds)
        assertEquals(
            listOf("q-later"),
            assertNotNull(result.dailyAssignments[laterDueDate]).map { it.questionId },
        )

        val allScheduledIds = result.dailyAssignments.values.flatMap { day -> day.map { it.questionId } }
        assertEquals(
            listOf("q-overdue", "q-later"),
            allScheduledIds,
            "逾期题目只在计划首日排入一次，不得在后续日期重复出现",
        )
        assertTrue(result.deferred.isEmpty())
    }

    @Test
    fun `S-14 question with several knowledge points due in range is scheduled once on the earliest date`() {
        val rangeStart = LocalDate.of(2026, 11, 16)
        val rangeEnd = LocalDate.of(2026, 11, 22)
        val earlyDueDate = LocalDate.of(2026, 11, 17)
        val lateDueDate = LocalDate.of(2026, 11, 19)

        fun state(id: String, number: Int, dueDate: LocalDate) = KnowledgeState(
            id = id,
            number = number,
            currentTier = Tier.UNKNOWN,
            continuousScore = 0f,
            nextDueDate = dueDate,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 1,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )

        val kEarly = state("k1", number = 1, dueDate = earlyDueDate)
        val kLate = state("k2", number = 2, dueDate = lateDueDate)
        // k3 在计划范围之后到期：不作为 triggered，但仍会作为非触发行打印（C-009）。
        val kOutOfRange = state("k3", number = 3, dueDate = LocalDate.of(2026, 11, 25))

        val q1 = SchedulerQuestion(
            id = "q1",
            linkedKnowledgeIds = listOf("k1", "k2", "k3"),
            archived = false,
            lastReviewedDate = null,
        )
        val dailyAvailableMinutes = (0L..6L).associate { rangeStart.plusDays(it) to 60 }

        val result = DefaultScheduler().plan(
            knowledgeStates = listOf(kLate, kEarly, kOutOfRange),
            questions = listOf(q1),
            dailyAvailableMinutes = dailyAvailableMinutes,
            dateRange = rangeStart..rangeEnd,
        )

        assertEquals(
            listOf(earlyDueDate),
            result.dailyAssignments.keys.toList(),
            "两个知识点在范围内到期时只排入最早的 2026-11-17 这一天，不得再出现在 2026-11-19",
        )
        val day = assertNotNull(result.dailyAssignments[earlyDueDate])
        assertEquals(listOf("q1"), day.map { it.questionId })
        assertEquals(
            listOf("k1", "k2"),
            day.single().triggeredKnowledgeIds,
            "范围内到期的两个知识点都计入 triggered，并按 id 字典序",
        )
        assertEquals(
            listOf("k3"),
            day.single().nonTriggeredKnowledgeIds,
            "范围外到期的知识点只作为非触发行打印",
        )

        val allScheduledIds = result.dailyAssignments.values.flatMap { dayItems -> dayItems.map { it.questionId } }
        assertEquals(listOf("q1"), allScheduledIds, "同一题目在一份计划内只排入一次")
        assertTrue(result.deferred.isEmpty())
    }

    @Test
    fun `S-16 shuffled input order yields an identical plan`() {
        val day1 = LocalDate.of(2026, 11, 16)
        val day2 = LocalDate.of(2026, 11, 18)
        val day3 = LocalDate.of(2026, 11, 20)
        val rangeEnd = LocalDate.of(2026, 11, 22)

        fun state(
            id: String,
            number: Int,
            tier: Tier,
            score: Float,
            dueDate: LocalDate,
            activeQuestionCount: Int,
        ) = KnowledgeState(
            id = id,
            number = number,
            currentTier = tier,
            continuousScore = score,
            nextDueDate = dueDate,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = activeQuestionCount,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )

        val k1 = state("k1", number = 1, Tier.UNKNOWN, 0f, day1, activeQuestionCount = 2)
        val k2 = state("k2", number = 2, Tier.FAMILIAR, 0.2f, day2, activeQuestionCount = 1)
        val k3 = state("k3", number = 3, Tier.MASTERED, 0.4f, day3, activeQuestionCount = 1)

        fun question(id: String, knowledgeId: String, lastReviewedDate: LocalDate? = null) = SchedulerQuestion(
            id = id,
            linkedKnowledgeIds = listOf(knowledgeId),
            archived = false,
            lastReviewedDate = lastReviewedDate,
        )

        // q1 与 q2 除 id 外排序键完全相同：只能靠 id 字典序稳定 tie-break。
        val q1 = question("q1", "k1")
        val q2 = question("q2", "k1")
        val q3 = question("q3", "k2")
        // q4 处于冷却期（2026-11-15 + 7 = 2026-11-22 > 2026-11-20），只进 deferred。
        val q4 = question("q4", "k3", lastReviewedDate = LocalDate.of(2026, 11, 15))

        val planA = DefaultScheduler().plan(
            knowledgeStates = listOf(k1, k2, k3),
            questions = listOf(q1, q2, q3, q4),
            dailyAvailableMinutes = linkedMapOf(day1 to 20, day2 to 60, day3 to 60),
            dateRange = day1..rangeEnd,
        )
        // 同一份输入，List 与 Map 的迭代顺序全部打乱。
        val planB = DefaultScheduler().plan(
            knowledgeStates = listOf(k3, k1, k2),
            questions = listOf(q4, q3, q2, q1),
            dailyAvailableMinutes = linkedMapOf(day3 to 60, day1 to 20, day2 to 60),
            dateRange = day1..rangeEnd,
        )

        assertEquals(planA, planB, "输入 List/Map 的迭代顺序不得影响计划结果")
        assertEquals(
            listOf(day1, day2),
            planA.dailyAssignments.keys.toList(),
            "日期键升序；没有条目的 2026-11-20 不出现",
        )
        assertEquals(listOf(day1, day2), planB.dailyAssignments.keys.toList())
        assertEquals(listOf("q1"), planA.dailyAssignments[day1]?.map { it.questionId })
        assertEquals(listOf("q3"), planA.dailyAssignments[day2]?.map { it.questionId })
        assertEquals(
            listOf(
                DeferredItem(
                    questionId = "q4",
                    reason = DeferReason.COOLING,
                    knowledgePointIds = listOf("k3"),
                    targetNextDueDates = mapOf("k3" to day3.plusDays(1)),
                ),
                DeferredItem(questionId = "q2", reason = DeferReason.TIME_OVERFLOW),
            ),
            planA.deferred,
            "deferred 先按 reason（COOLING 在前）再按 questionId 升序，与输入顺序无关",
        )
    }

    @Test
    fun `S-10 more than four linked knowledge points is rejected and zero links never become candidates`() {
        val rangeStart = LocalDate.of(2026, 11, 16)
        val rangeEnd = LocalDate.of(2026, 11, 22)

        fun state(id: String, number: Int) = KnowledgeState(
            id = id,
            number = number,
            currentTier = Tier.UNKNOWN,
            continuousScore = 0f,
            nextDueDate = rangeStart,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 1,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )

        val states = (1..5).map { number -> state("k$number", number) }
        val tooManyLinks = SchedulerQuestion(
            id = "q-too-many-links",
            linkedKnowledgeIds = states.map { it.id },
            archived = false,
            lastReviewedDate = null,
        )
        val dailyAvailableMinutes = (0L..6L).associate { rangeStart.plusDays(it) to 120 }

        // D-12：单题关联知识点至多 4 个；调度器收到 5 个时必须拒绝，而不是静默截断或照常排入。
        assertFailsWith<IllegalArgumentException>("单题关联 5 个知识点必须抛 IllegalArgumentException") {
            DefaultScheduler().plan(
                knowledgeStates = states,
                questions = listOf(tooManyLinks),
                dailyAvailableMinutes = dailyAvailableMinutes,
                dateRange = rangeStart..rangeEnd,
            )
        }

        // C-049：0 个关联的题目直接跳过，不进入候选，也不产生 deferred。
        val noLinks = SchedulerQuestion(
            id = "q-no-links",
            linkedKnowledgeIds = emptyList(),
            archived = false,
            lastReviewedDate = null,
        )
        val result = DefaultScheduler().plan(
            knowledgeStates = states,
            questions = listOf(noLinks),
            dailyAvailableMinutes = dailyAvailableMinutes,
            dateRange = rangeStart..rangeEnd,
        )

        assertTrue(result.dailyAssignments.isEmpty(), "0 个关联的题目不得出现在任何 dailyAssignments")
        assertTrue(result.deferred.isEmpty(), "0 个关联的题目不得产生 deferred")
    }

    @Test
    fun `boundary empty date range returns fully empty plan without exception`() {
        val day = LocalDate.of(2026, 11, 16)

        val k1 = KnowledgeState(
            id = "k1",
            number = 1,
            currentTier = Tier.UNKNOWN,
            continuousScore = 0f,
            nextDueDate = day,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 1,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )
        // 若范围非空，该题会进入冷却并产生 COOLING 顺延；空范围必须先短路为全空结果。
        val q1 = SchedulerQuestion(
            id = "q1",
            linkedKnowledgeIds = listOf("k1"),
            archived = false,
            lastReviewedDate = LocalDate.of(2026, 11, 15),
        )

        val result = DefaultScheduler().plan(
            knowledgeStates = listOf(k1),
            questions = listOf(q1),
            dailyAvailableMinutes = mapOf(day to 60),
            dateRange = day.plusDays(1)..day,
        )

        assertEquals(emptyMap(), result.dailyAssignments)
        assertTrue(
            result.deferred.isEmpty(),
            "日期范围起止倒置时必须返回全空 PlanResult，不抛异常，也不产生冷却顺延",
        )
    }

    @Test
    fun `boundary negative daily capacity behaves as zero and overflows every candidate`() {
        val day = LocalDate.of(2026, 11, 16)

        val k1 = KnowledgeState(
            id = "k1",
            number = 1,
            currentTier = Tier.UNKNOWN,
            continuousScore = 0f,
            nextDueDate = day,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 1,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )
        val q1 = SchedulerQuestion(
            id = "q1",
            linkedKnowledgeIds = listOf("k1"),
            archived = false,
            lastReviewedDate = null,
            estimatedReviewMinutes = 20,
        )

        val result = DefaultScheduler().plan(
            knowledgeStates = listOf(k1),
            questions = listOf(q1),
            dailyAvailableMinutes = mapOf(day to -30),
            dateRange = day..day,
        )

        assertTrue(result.dailyAssignments.isEmpty(), "负容量按 0 处理，任何正时长题目都排不进")
        assertEquals(
            listOf(DeferredItem(questionId = "q1", reason = DeferReason.TIME_OVERFLOW)),
            result.deferred,
        )
    }

    @Test
    fun `boundary missing daily capacity behaves as zero and overflows every candidate`() {
        val day = LocalDate.of(2026, 11, 16)

        val k1 = KnowledgeState(
            id = "k1",
            number = 1,
            currentTier = Tier.UNKNOWN,
            continuousScore = 0f,
            nextDueDate = day,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 1,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )
        val q1 = SchedulerQuestion(
            id = "q1",
            linkedKnowledgeIds = listOf("k1"),
            archived = false,
            lastReviewedDate = null,
            estimatedReviewMinutes = 20,
        )

        val result = DefaultScheduler().plan(
            knowledgeStates = listOf(k1),
            questions = listOf(q1),
            dailyAvailableMinutes = emptyMap(),
            dateRange = day..day,
        )

        assertTrue(result.dailyAssignments.isEmpty(), "缺失容量按 0 处理，不得当成无限容量")
        assertEquals(
            listOf(DeferredItem(questionId = "q1", reason = DeferReason.TIME_OVERFLOW)),
            result.deferred,
            "缺失容量当天应把候选记为 TIME_OVERFLOW，而不是静默丢弃",
        )
    }

    @Test
    fun `C-049 knowledge id lists are lexicographically sorted regardless of input order`() {
        val day = LocalDate.of(2026, 11, 16)
        val rangeEnd = LocalDate.of(2026, 11, 22)

        fun state(id: String, number: Int, dueDate: LocalDate) = KnowledgeState(
            id = id,
            number = number,
            currentTier = Tier.UNKNOWN,
            continuousScore = 0f,
            nextDueDate = dueDate,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 1,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )

        // 字典序：k10 < k2、k30 < k4；输入顺序故意相反。
        val k2 = state("k2", number = 2, dueDate = day)
        val k10 = state("k10", number = 10, dueDate = day)
        val k4 = state("k4", number = 4, dueDate = LocalDate.of(2026, 11, 25))
        val k30 = state("k30", number = 30, dueDate = LocalDate.of(2026, 11, 25))

        val q1 = SchedulerQuestion(
            id = "q1",
            linkedKnowledgeIds = listOf("k2", "k10", "k4", "k30"),
            archived = false,
            lastReviewedDate = null,
        )

        val result = DefaultScheduler().plan(
            knowledgeStates = listOf(k2, k10, k4, k30),
            questions = listOf(q1),
            dailyAvailableMinutes = (0L..6L).associate { day.plusDays(it) to 60 },
            dateRange = day..rangeEnd,
        )

        val planned = assertNotNull(result.dailyAssignments[day]).single()
        assertEquals(
            listOf("k10", "k2"),
            planned.triggeredKnowledgeIds,
            "触发知识点必须按字符串字典序，而不是输入顺序",
        )
        assertEquals(
            listOf("k30", "k4"),
            planned.nonTriggeredKnowledgeIds,
            "非触发知识点必须按字符串字典序，而不是输入顺序",
        )
    }

    @Test
    fun `C-049 deferred items sharing a reason are sorted by question id`() {
        val day = LocalDate.of(2026, 11, 16)

        val k1 = KnowledgeState(
            id = "k1",
            number = 1,
            currentTier = Tier.UNKNOWN,
            continuousScore = 0f,
            nextDueDate = day,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 1,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )
        // 两题都处于冷却期；输入顺序 q-b 在前，输出必须按 questionId 字典序 q-a、q-b。
        fun question(id: String) = SchedulerQuestion(
            id = id,
            linkedKnowledgeIds = listOf("k1"),
            archived = false,
            lastReviewedDate = LocalDate.of(2026, 11, 15),
        )

        val result = DefaultScheduler().plan(
            knowledgeStates = listOf(k1),
            questions = listOf(question("q-b"), question("q-a")),
            dailyAvailableMinutes = mapOf(day to 60),
            dateRange = day..day,
        )

        assertEquals(
            listOf("q-a", "q-b"),
            result.deferred.map { it.questionId },
            "同一 reason 内必须按 questionId 字典序升序，不得沿用输入顺序",
        )
        assertTrue(result.deferred.all { it.reason == DeferReason.COOLING })
        assertTrue(result.dailyAssignments.isEmpty())
    }

    @Test
    fun `S-18 group tier takes the highest tier and group score takes that tier's minimum`() {
        val day = LocalDate.of(2026, 11, 16)

        fun state(id: String, tier: Tier, score: Float) = KnowledgeState(
            id = id,
            number = 1,
            currentTier = tier,
            continuousScore = score,
            nextDueDate = day,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 1,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )

        val masteredHigh = state("k-m9", Tier.MASTERED, 0.9f)
        val masteredLow = state("k-m3", Tier.MASTERED, 0.3f)
        val masteredMid = state("k-m5", Tier.MASTERED, 0.5f)
        val unknownLow = state("k-u1", Tier.UNKNOWN, 0.1f)
        val mastered8 = state("k-m8", Tier.MASTERED, 0.8f)
        val unknownLowest = state("k-u05", Tier.UNKNOWN, 0.05f)

        // qMixed 的两个触发点都在 MASTERED 组：组内分必须取 0.3（而不是 0.9），排在 qPure(0.5) 前。
        val qMixed = SchedulerQuestion("qMixed", listOf("k-m9", "k-m3"), false, null)
        val qPure = SchedulerQuestion("qPure", listOf("k-m5"), false, null)
        // qCross 同时触发 UNKNOWN 与 MASTERED：必须归入 MASTERED 组（0.8），排在纯 UNKNOWN(0.05) 前。
        val qCross = SchedulerQuestion("qCross", listOf("k-u1", "k-m8"), false, null)
        val qUnknown = SchedulerQuestion("qUnknown", listOf("k-u05"), false, null)

        val result = DefaultScheduler().plan(
            knowledgeStates = listOf(
                masteredHigh, masteredLow, masteredMid, unknownLow, mastered8, unknownLowest,
            ),
            questions = listOf(qUnknown, qCross, qPure, qMixed),
            dailyAvailableMinutes = mapOf(day to 60),
            dateRange = day..day,
        )

        assertEquals(
            listOf("qMixed", "qPure", "qCross", "qUnknown"),
            assertNotNull(result.dailyAssignments[day]).map { it.questionId },
            "MASTERED 组内按最小分 0.3/0.5/0.8 升序；混合题按最高档位归组，不得混入 UNKNOWN 分",
        )
    }

    @Test
    fun `S-20 unresolved knowledge id skips the whole question and leaves a visible deferred record`() {
        val day = LocalDate.of(2026, 11, 16)

        val k1 = KnowledgeState(
            id = "k1",
            number = 1,
            currentTier = Tier.UNKNOWN,
            continuousScore = 0f,
            nextDueDate = day,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 3,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )
        // 三题同一天；容量 0 让正常候选记 TIME_OVERFLOW，便于同时锁定 deferred 的 reason 顺序。
        val qCooling = SchedulerQuestion(
            id = "q-cooling",
            linkedKnowledgeIds = listOf("k1"),
            archived = false,
            lastReviewedDate = LocalDate.of(2026, 11, 15),
        )
        val qOverflow = SchedulerQuestion(
            id = "q-overflow",
            linkedKnowledgeIds = listOf("k1"),
            archived = false,
            lastReviewedDate = null,
        )
        // 即使还有一个到期知识点，只要存在解析不到的关联，整题都不得进入计划。
        // 两个未解析 id 的输入顺序故意与字典序相反。
        val qBroken = SchedulerQuestion(
            id = "q-broken",
            linkedKnowledgeIds = listOf("k1", "k-z", "k-a"),
            archived = false,
            lastReviewedDate = null,
        )

        val result = DefaultScheduler().plan(
            knowledgeStates = listOf(k1),
            questions = listOf(qBroken, qOverflow, qCooling),
            dailyAvailableMinutes = mapOf(day to 0),
            dateRange = day..day,
        )

        assertTrue(result.dailyAssignments.isEmpty(), "关联失效的题不得整题排入")
        assertEquals(
            listOf("q-cooling", "q-overflow", "q-broken"),
            result.deferred.map { it.questionId },
            "deferred 顺序必须是 COOLING → TIME_OVERFLOW → UNRESOLVED_KNOWLEDGE",
        )
        val unresolved = result.deferred.last()
        assertEquals(DeferReason.UNRESOLVED_KNOWLEDGE, unresolved.reason)
        assertEquals(
            listOf("k-a", "k-z"),
            unresolved.knowledgePointIds,
            "未解析 id 必须按字典序升序，而不是输入顺序",
        )
        assertTrue(unresolved.targetNextDueDates.isEmpty(), "关联失效不是冷却顺延，不得带下次到期日")

        // 容量充足时，健康题正常排入，坏关联题仍被整题跳过（不是因容量不足才消失）。
        val healthy = SchedulerQuestion(
            id = "q-healthy",
            linkedKnowledgeIds = listOf("k1"),
            archived = false,
            lastReviewedDate = null,
        )
        val roomy = DefaultScheduler().plan(
            knowledgeStates = listOf(k1),
            questions = listOf(qBroken, healthy),
            dailyAvailableMinutes = mapOf(day to 60),
            dateRange = day..day,
        )
        assertEquals(
            listOf("q-healthy"),
            assertNotNull(roomy.dailyAssignments[day]).map { it.questionId },
        )
        assertEquals(
            listOf("q-broken"),
            roomy.deferred.map { it.questionId },
            "容量充足时坏关联题也必须整题跳过，而不是被当作可排入候选",
        )
        assertEquals(DeferReason.UNRESOLVED_KNOWLEDGE, roomy.deferred.single().reason)
    }

    @Test
    fun `S-21 archived question with too many links is skipped before validation`() {
        val day = LocalDate.of(2026, 11, 16)

        fun state(number: Int) = KnowledgeState(
            id = "k$number",
            number = number,
            currentTier = Tier.UNKNOWN,
            continuousScore = 0f,
            nextDueDate = day,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 1,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )

        val states = (1..5).map { state(it) }
        val archived = SchedulerQuestion(
            id = "q-archived",
            linkedKnowledgeIds = states.map { it.id },
            archived = true,
            lastReviewedDate = null,
        )

        // C-058 方案 A：归档题先跳过，不因 >4 关联让整份计划失败。
        val result = DefaultScheduler().plan(
            knowledgeStates = states,
            questions = listOf(archived),
            dailyAvailableMinutes = mapOf(day to 60),
            dateRange = day..day,
        )

        assertTrue(result.dailyAssignments.isEmpty())
        assertTrue(result.deferred.isEmpty(), "归档题不参与计划，也不产生 deferred 记录")
    }

    @Test
    fun `S-21 empty date range with too many links returns fully empty plan`() {
        val day = LocalDate.of(2026, 11, 16)

        fun state(number: Int) = KnowledgeState(
            id = "k$number",
            number = number,
            currentTier = Tier.UNKNOWN,
            continuousScore = 0f,
            nextDueDate = day,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 1,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )

        val states = (1..5).map { state(it) }
        val active = SchedulerQuestion(
            id = "q-active",
            linkedKnowledgeIds = states.map { it.id },
            archived = false,
            lastReviewedDate = null,
        )

        // C-058 方案 A：空范围先短路为全空，不触发 >4 校验。
        val result = DefaultScheduler().plan(
            knowledgeStates = states,
            questions = listOf(active),
            dailyAvailableMinutes = mapOf(day to 60),
            dateRange = day.plusDays(1)..day,
        )

        assertTrue(result.dailyAssignments.isEmpty())
        assertTrue(result.deferred.isEmpty())
    }

    @Test
    fun `C-058 validation order is observable across cooling empty range and archived questions`() {
        val day = LocalDate.of(2026, 11, 16)

        val k1 = KnowledgeState(
            id = "k1",
            number = 1,
            currentTier = Tier.UNKNOWN,
            continuousScore = 0f,
            nextDueDate = day,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 1,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )
        val broken = SchedulerQuestion(
            id = "q-broken",
            linkedKnowledgeIds = listOf("k1", "k-missing"),
            archived = false,
            lastReviewedDate = null,
        )
        val archivedBroken = broken.copy(id = "q-archived-broken", archived = true)

        // 1) 负冷却天数先于空范围校验：即使范围倒置也必须抛异常。
        assertFailsWith<IllegalArgumentException>("负冷却天数必须先于空范围校验") {
            DefaultScheduler().plan(
                knowledgeStates = listOf(k1),
                questions = listOf(broken),
                dailyAvailableMinutes = mapOf(day to 60),
                dateRange = day.plusDays(1)..day,
                coolingPeriodDays = -1,
            )
        }

        // 2) 空范围先于关联完整性：悬空关联不得产生 deferred。
        val emptyRange = DefaultScheduler().plan(
            knowledgeStates = listOf(k1),
            questions = listOf(broken),
            dailyAvailableMinutes = mapOf(day to 60),
            dateRange = day.plusDays(1)..day,
            coolingPeriodDays = 0,
        )
        assertTrue(emptyRange.dailyAssignments.isEmpty())
        assertTrue(emptyRange.deferred.isEmpty(), "空范围必须全空，不得校验关联完整性")

        // 3) 归档题先于关联完整性：悬空关联不得产生 UNRESOLVED 记录。
        val archivedOnly = DefaultScheduler().plan(
            knowledgeStates = listOf(k1),
            questions = listOf(archivedBroken),
            dailyAvailableMinutes = mapOf(day to 60),
            dateRange = day..day,
        )
        assertTrue(archivedOnly.dailyAssignments.isEmpty())
        assertTrue(archivedOnly.deferred.isEmpty(), "归档题必须跳过关联完整性校验")
    }

    @Test
    fun `S-22 zero cooling period disables cooling and negative is illegal`() {
        val day = LocalDate.of(2026, 11, 16)

        val k1 = KnowledgeState(
            id = "k1",
            number = 1,
            currentTier = Tier.UNKNOWN,
            continuousScore = 0f,
            nextDueDate = day,
            effectiveFeedbackCount = 0f,
            dormant = false,
            activeQuestionCount = 1,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        )
        // 实际复习日晚于目标日（重排过去日期/补确认场景）：旧公式会把 0 天也算成冷却，
        // 因此这里能区分“0 无条件关闭”与“0 按公式计算”；1 天仍应冷却。
        val q1 = SchedulerQuestion(
            id = "q1",
            linkedKnowledgeIds = listOf("k1"),
            archived = false,
            lastReviewedDate = day.plusDays(1),
        )

        fun plan(coolingDays: Int) = DefaultScheduler().plan(
            knowledgeStates = listOf(k1),
            questions = listOf(q1),
            dailyAvailableMinutes = mapOf(day to 60),
            dateRange = day..day,
            coolingPeriodDays = coolingDays,
        )

        // C-057 方案 D：0 = 无条件关闭冷却，正常参与排程。
        assertEquals(
            listOf("q1"),
            assertNotNull(plan(0).dailyAssignments[day]).map { it.questionId },
        )
        assertTrue(plan(0).deferred.isEmpty())

        // 1 天冷却仍生效，用来区分“0 关闭”与“0 被夹到 1”。
        val oneDay = plan(1)
        assertTrue(oneDay.dailyAssignments.isEmpty())
        assertEquals(listOf("q1"), oneDay.deferred.map { it.questionId })
        assertEquals(DeferReason.COOLING, oneDay.deferred.single().reason)

        // 负数为非法输入。
        assertFailsWith<IllegalArgumentException>("冷却天数不得为负数") { plan(-1) }
    }

    @RepeatedTest(100)
    fun `property plan keeps archived dormant uniqueness and date ordering invariants`(
        repetitionInfo: RepetitionInfo,
    ) {
        val repetition = repetitionInfo.currentRepetition
        // 每次 repetition 由固定种子 + 序号派生独立随机流：单独重跑第 N 次也能复现同一组数据。
        val random = Random(PROPERTY_SEED + repetition)
        val rangeStart = LocalDate.of(2026, 11, 16).plusDays(random.nextInt(0, 31).toLong())
        val dayCount = random.nextInt(0, 14)
        val rangeEnd = rangeStart.plusDays(dayCount.toLong())

        val states = (1..random.nextInt(1, 9)).map { number ->
            KnowledgeState(
                id = "k$number",
                number = number,
                currentTier = Tier.entries[random.nextInt(Tier.entries.size)],
                continuousScore = random.nextInt(0, 1001) / 1000f,
                nextDueDate = rangeStart.plusDays(random.nextInt(-10, 15).toLong()),
                effectiveFeedbackCount = random.nextInt(0, 13) * 0.5f,
                dormant = random.nextInt(100) < 15,
                activeQuestionCount = random.nextInt(0, 4),
                difficulty = if (random.nextBoolean()) random.nextInt(1, 6) else null,
                halfLifeDays = if (random.nextBoolean()) {
                    random.nextInt(1, 61).toFloat()
                } else {
                    null
                },
                createdAt = Instant.parse("2026-11-01T00:00:00Z"),
            )
        }

        val knowledgeIds = states.map { it.id }
        // 关联数限制在 0..4，覆盖 S-10 的合法边界（0 个跳过、4 个仍合法），不生成非法输入。
        val questions = (1..random.nextInt(1, 11)).map { number ->
            val linkedCount = random.nextInt(0, minOf(4, knowledgeIds.size) + 1)
            SchedulerQuestion(
                id = "q$number",
                linkedKnowledgeIds = knowledgeIds.shuffled(random).take(linkedCount),
                archived = random.nextInt(100) < 15,
                lastReviewedDate = if (random.nextBoolean()) {
                    rangeStart.plusDays(random.nextInt(-10, 11).toLong())
                } else {
                    null
                },
                estimatedReviewMinutes = random.nextInt(-5, 41),
            )
        }

        // 部分日期缺容量（按 0）、含负容量与零容量，覆盖装箱边界。
        val dailyAvailableMinutes = buildMap {
            for (offset in 0..dayCount) {
                if (random.nextInt(100) < 80) {
                    put(rangeStart.plusDays(offset.toLong()), random.nextInt(-20, 201))
                }
            }
        }

        val result = DefaultScheduler().plan(
            knowledgeStates = states,
            questions = questions,
            dailyAvailableMinutes = dailyAvailableMinutes,
            dateRange = rangeStart..rangeEnd,
        )

        val archivedQuestionIds = questions.filter { it.archived }.map { it.id }.toSet()
        val unprintableKnowledgeIds = states
            .filter { it.dormant || it.activeQuestionCount <= 0 }
            .map { it.id }
            .toSet()
        val allScheduledQuestionIds = mutableListOf<String>()
        var previousDate: LocalDate? = null

        for ((date, planned) in result.dailyAssignments) {
            val previous = previousDate
            if (previous != null) {
                assertTrue(
                    previous < date,
                    "repetition=$repetition：dailyAssignments 日期键必须严格升序（$previous → $date）",
                )
            }
            previousDate = date

            val dailyQuestionIds = planned.map { it.questionId }
            assertEquals(
                dailyQuestionIds.size,
                dailyQuestionIds.toSet().size,
                "repetition=$repetition：同一题目在同一天 $date 不得重复出现",
            )

            for (item in planned) {
                assertTrue(
                    item.questionId !in archivedQuestionIds,
                    "repetition=$repetition：归档题目 ${item.questionId} 不得出现在 dailyAssignments",
                )
                assertTrue(
                    item.triggeredKnowledgeIds.none { it in unprintableKnowledgeIds },
                    "repetition=$repetition：休眠或零活跃知识点不得作为触发点出现在 ${item.questionId}",
                )
                assertTrue(
                    item.nonTriggeredKnowledgeIds.none { it in unprintableKnowledgeIds },
                    "repetition=$repetition：休眠或零活跃知识点不得作为非触发点出现在 ${item.questionId}",
                )
            }
            allScheduledQuestionIds += dailyQuestionIds
        }

        assertEquals(
            allScheduledQuestionIds.size,
            allScheduledQuestionIds.toSet().size,
            "repetition=$repetition：同一题目在一份计划内至多出现一次",
        )

        // 守恒断言：归档题之外，只要题目至少有一个 printable 到期知识点，就必须恰好出现在
        // dailyAssignments 或 deferred 中一次；防止过宽过滤把候选静默丢掉。
        val printableById = states
            .filter { !it.dormant && it.activeQuestionCount > 0 }
            .associateBy { it.id }
        val expectedHandledQuestionIds = questions
            .filter { !it.archived }
            .filter { question ->
                question.linkedKnowledgeIds.any { id ->
                    printableById[id]?.let { !it.nextDueDate.isAfter(rangeEnd) } == true
                }
            }
            .map { it.id }
            .toSet()
        val handledQuestionIds = allScheduledQuestionIds + result.deferred.map { it.questionId }
        assertEquals(
            expectedHandledQuestionIds,
            handledQuestionIds.toSet(),
            "repetition=$repetition：每道应处理的题都必须恰好出现在计划或 deferred 中",
        )
        assertEquals(
            handledQuestionIds.size,
            handledQuestionIds.toSet().size,
            "repetition=$repetition：同一题在 assignments 与 deferred 合并后不得重复",
        )
    }

    private companion object {
        // 属性测试固定种子（规格验收门禁）；每次 repetition 用 SEED + 序号派生独立随机流。
        const val PROPERTY_SEED = 20261113L
    }
}
