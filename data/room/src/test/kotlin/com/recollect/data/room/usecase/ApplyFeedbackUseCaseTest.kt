package com.recollect.data.room.usecase

import com.recollect.data.room.entity.DetectionStatus
import com.recollect.data.room.entity.KnowledgePoint
import com.recollect.data.room.entity.PendingFeedback
import com.recollect.data.room.entity.Plan
import com.recollect.data.room.entity.PlanPage
import com.recollect.data.room.entity.Question
import com.recollect.data.room.entity.ReviewFeedback
import com.recollect.data.room.entity.ScanSession
import com.recollect.data.room.entity.ScanSessionStatus
import com.recollect.data.room.entity.UserDecision
import com.recollect.data.room.repository.ConfirmedDecision
import com.recollect.data.room.repository.FakeConfirmationDecisionLogDao
import com.recollect.data.room.repository.FakeKnowledgePointDao
import com.recollect.data.room.repository.FakeNoteAttachmentItemDao
import com.recollect.data.room.repository.FakePendingFeedbackDao
import com.recollect.data.room.repository.FakePlanDao
import com.recollect.data.room.repository.FakePlanPageDao
import com.recollect.data.room.repository.FakeQuestionDao
import com.recollect.data.room.repository.FakeQuestionLookupRepository
import com.recollect.data.room.repository.FakeReviewFeedbackDao
import com.recollect.data.room.repository.FakeScanSessionDao
import com.recollect.data.room.repository.InMemoryStores
import com.recollect.data.room.repository.PlanWeekStartProvider
import com.recollect.data.room.repository.RollbackTransactionRunner
import com.recollect.data.room.repository.RoomFeedbackRepository
import com.recollect.data.room.repository.RoomKnowledgePointRepository
import com.recollect.data.room.repository.RoomReviewFeedbackHistoryRepository
import com.recollect.data.room.mapping.toKnowledgeState
import com.recollect.data.room.mapping.toSchedulerQuestion
import com.recollect.model.Tier
import com.recollect.scheduler.DefaultScheduler
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking

/**
 * 阶段 4.3a：`ApplyFeedbackUseCase` 的 U-01～U-05（feedback-usecase.md 用例表）。
 *
 * 状态在 UseCase 内计算并按 C-050 在边界转 `Float`；写入统一走 4.2 的 `saveDecisions`
 * （单事务、Repository 只落库，C-074 方案 A）。测试使用模块内的内存 fake DAO，
 * 不 mock 识别结果（R-01/R-04）。
 */
class ApplyFeedbackUseCaseTest {

    private val zone = ZoneId.of("Asia/Shanghai")
    private val createdAtInstant = Instant.parse("2026-11-16T12:00:00Z")
    // “今天”晚于所有用例的复习日；需要测试“不能晚于今天”时单独把 today 往回拨。
    private var today = Instant.parse("2027-01-01T12:00:00Z")
    private val clock = object : Clock() {
        // 显式限定外层属性：Clock 自带 `zone` 属性会让裸 `zone` 递归调用 getZone()。
        override fun getZone(): ZoneId = this@ApplyFeedbackUseCaseTest.zone
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = today
    }
    private val plannedDate = LocalDate.of(2026, 11, 16)
    private val reviewDate = LocalDate.of(2026, 11, 16)
    private val planId = "AB12CD"
    private val pageId = "AB12CD-01"
    private val sessionId = "s1"
    private var planStartDay = DayOfWeek.MONDAY
    private var sessionSequence = 0

    private val stores = InMemoryStores()
    private val feedbackRepository = RoomFeedbackRepository(
        transactionRunner = RollbackTransactionRunner(stores),
        reviewFeedbackDao = FakeReviewFeedbackDao(stores),
        confirmationDecisionLogDao = FakeConfirmationDecisionLogDao(stores),
        knowledgePointDao = FakeKnowledgePointDao(stores),
        questionDao = FakeQuestionDao(stores),
        pendingFeedbackDao = FakePendingFeedbackDao(stores),
        noteAttachmentItemDao = FakeNoteAttachmentItemDao(stores),
        scanSessionDao = FakeScanSessionDao(stores),
        planPageDao = FakePlanPageDao(stores),
        planDao = FakePlanDao(stores),
        clock = clock,
    )
    private val useCase = ApplyFeedbackUseCase(
        knowledgePointRepository = RoomKnowledgePointRepository(FakeKnowledgePointDao(stores)),
        reviewFeedbackHistoryRepository = RoomReviewFeedbackHistoryRepository(FakeReviewFeedbackDao(stores)),
        feedbackRepository = feedbackRepository,
        nextDueCalculator = MaimemoNextDueCalculator(),
        questionLookupRepository = FakeQuestionLookupRepository(stores),
        planWeekStartProvider = PlanWeekStartProvider { planStartDay },
        clock = clock,
    )

    /** 种入扫描会话、题目关联与待确认行（C-079 方案 A 的前置条件）。 */
    private fun seedSession(
        sessionId: String,
        decisions: List<ConfirmedDecision>,
        status: ScanSessionStatus = ScanSessionStatus.READY,
        pendingTransform: (PendingFeedback) -> PendingFeedback = { it },
    ) {
        stores.scanSessions[sessionId] = ScanSession(
            sessionId = sessionId,
            planId = decisions.firstOrNull()?.planId ?: planId,
            startedAt = createdAtInstant,
            status = status,
            submittedAt = null,
        )
        decisions.forEach { decision ->
            stores.questionLinks.getOrPut(decision.questionId) { mutableSetOf() }
                .add(decision.knowledgePointId)
            stores.pendingFeedbacks += pendingTransform(
                pending(
                    decision.questionId,
                    decision.knowledgePointId,
                    sessionId = sessionId,
                    triggered = decision.triggered,
                ).copy(
                    planId = decision.planId,
                    pageId = decision.pageId,
                    plannedDate = decision.plannedDate,
                ),
            )
        }
    }

    /** 每次提交默认使用新的会话，模拟“一份计划一次扫描会话”。 */
    private suspend fun submit(
        decisions: List<ConfirmedDecision>,
        sessionId: String = "s${++sessionSequence}",
        status: ScanSessionStatus = ScanSessionStatus.READY,
    ): ApplyResult {
        seedSession(sessionId, decisions, status)
        return useCase.execute(sessionId, decisions)
    }

    private fun knowledgePoint(
        id: String,
        number: Int = 1,
        tier: Tier = Tier.UNKNOWN,
        score: Float = 0.5f,
        count: Float = 0.0f,
        nextDueDate: LocalDate = plannedDate,
        difficulty: Int? = null,
        halfLifeDays: Float? = null,
        createdAt: Instant = createdAtInstant,
    ) = KnowledgePoint(
        id = id,
        number = number,
        name = "一次函数$number",
        currentTier = tier,
        continuousScore = score,
        effectiveFeedbackCount = count,
        nextDueDate = nextDueDate,
        activeQuestionCount = 1,
        dormant = false,
        difficulty = difficulty,
        halfLifeDays = halfLifeDays,
        createdAt = createdAtInstant,
    )

    private fun question(id: String) = Question(
        id = id,
        pageId = "",
        questionNumber = "1-01",
        qrPayload = "",
        archived = false,
        lastScheduledDate = plannedDate,
        lastReviewedDate = null,
        estimatedReviewMinutes = 15,
        createdAt = createdAtInstant,
        archivedAt = null,
    )

    private fun reviewFeedback(
        questionId: String,
        knowledgePointId: String,
        tier: Tier,
        triggered: Boolean,
        reviewedOn: LocalDate,
    ) = ReviewFeedback(
        knowledgePointId = knowledgePointId,
        questionId = questionId,
        planId = planId,
        pageId = pageId,
        plannedDate = plannedDate,
        reviewedOn = reviewedOn,
        tier = tier,
        triggered = triggered,
        rawOcrText = null,
        correctedText = null,
        effectiveWeight = if (tier == Tier.FAMILIAR) 0.5f else 1.0f,
        weightApplied = if (triggered) 1.0f else 0.3f,
        timestamp = createdAtInstant,
    )

    private fun pending(
        questionId: String,
        knowledgePointId: String,
        sessionId: String = this.sessionId,
        triggered: Boolean = true,
        userDecision: UserDecision = UserDecision.UNMARKED,
        detectedTier: Tier? = null,
    ) = PendingFeedback(
        sessionId = sessionId,
        planId = planId,
        pageId = pageId,
        plannedDate = plannedDate,
        actualReviewDate = plannedDate,
        questionId = questionId,
        knowledgePointId = knowledgePointId,
        triggered = triggered,
        detectedTier = detectedTier,
        detectionStatus = if (detectedTier == null) DetectionStatus.NO_MARK else DetectionStatus.OK,
        detectedRawOcr = null,
        userDecision = userDecision,
        selectedTier = null,
        correctedText = null,
    )

    private fun decision(
        questionId: String,
        knowledgePointId: String,
        triggered: Boolean = true,
        userDecision: UserDecision = UserDecision.ACCEPT,
        detectedTier: Tier? = Tier.MASTERED,
        selectedTier: Tier? = null,
        detectedRawOcr: String? = null,
        correctedText: String? = null,
        reviewedOn: LocalDate = reviewDate,
        pageId: String = this.pageId,
    ) = ConfirmedDecision(
        planId = planId,
        pageId = pageId,
        plannedDate = plannedDate,
        actualReviewDate = reviewedOn,
        questionId = questionId,
        knowledgePointId = knowledgePointId,
        triggered = triggered,
        decision = userDecision,
        detectedTier = detectedTier,
        selectedTier = selectedTier,
        detectedRawOcr = detectedRawOcr,
        correctedText = correctedText,
    )

    @Test
    fun u01_triggered_accept_mastered_updates_tier_score_count_and_due_date() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1")

        val result = submit(
            decisions = listOf(decision("q1", "k1")),
        )

        val feedback = stores.reviewFeedbacks.single()
        assertEquals(Tier.MASTERED, feedback.tier)
        assertEquals(1.0f, feedback.effectiveWeight, "C-006：非 FAMILIAR 计整次有效反馈")
        assertEquals(1.0f, feedback.weightApplied, "触发行权重 1.0")
        assertEquals(reviewDate, feedback.reviewedOn)

        // C-050：内部用 Double 计算，落库断言用精确转换后的 Float。
        val expectedScore = (0.5f.toDouble() + 0.3).toFloat()
        val updated = stores.knowledgePoints.getValue("k1")
        assertEquals(Tier.MASTERED, updated.currentTier)
        assertEquals(expectedScore, updated.continuousScore)
        assertEquals(1.0f, updated.effectiveFeedbackCount)
        assertEquals(reviewDate.plusDays(1), updated.nextDueDate, "冷启动第 1 次反馈间隔 1 天（D-05）")

        assertEquals(expectedScore, result.updatedKnowledgeStates.single().continuousScore)
        assertEquals(1, result.saveResult.writtenFeedbackCount)
        assertEquals(reviewDate, stores.questions.getValue("q1").lastReviewedDate)
    }

    @Test
    fun u02_triggered_corrected_unknown_uses_selected_tier_and_keeps_ocr_texts() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1")

        submit(
            decisions = listOf(
                decision(
                    "q1",
                    "k1",
                    userDecision = UserDecision.CORRECTED,
                    detectedTier = Tier.MASTERED, // OMR 原判
                    selectedTier = Tier.UNKNOWN, // 用户修正
                    detectedRawOcr = "认识",
                    correctedText = "不认识",
                ),
            ),
        )

        val feedback = stores.reviewFeedbacks.single()
        assertEquals(Tier.UNKNOWN, feedback.tier, "CORRECTED 以用户选择档位为准")
        assertEquals("认识", feedback.rawOcrText, "D-06：原始 OCR 只读保留")
        assertEquals("不认识", feedback.correctedText, "D-06：修正文本保留")

        val expectedScore = (0.5f.toDouble() + (-0.3)).toFloat()
        val updated = stores.knowledgePoints.getValue("k1")
        assertEquals(Tier.UNKNOWN, updated.currentTier)
        assertEquals(expectedScore, updated.continuousScore)
        assertEquals(1.0f, updated.effectiveFeedbackCount, "UNKNOWN 按整次有效反馈")
    }

    @Test
    fun u03_triggered_a_and_non_triggered_b_write_two_feedbacks_and_only_scale_b_score() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1", number = 1)
        stores.knowledgePoints["k2"] = knowledgePoint("k2", number = 2)
        stores.questions["q1"] = question("q1")

        submit(
            decisions = listOf(
                decision("q1", "k1", triggered = true, detectedTier = Tier.MASTERED),
                decision("q1", "k2", triggered = false, detectedTier = Tier.MASTERED),
            ),
        )

        assertEquals(2, stores.reviewFeedbacks.size, "A/B 各写一条反馈")
        val feedbackA = stores.reviewFeedbacks.single { it.knowledgePointId == "k1" }
        val feedbackB = stores.reviewFeedbacks.single { it.knowledgePointId == "k2" }
        assertEquals(1.0f, feedbackA.weightApplied, "触发行权重 1.0")
        assertEquals(0.3f, feedbackB.weightApplied, "非触发行权重 0.3（D-01，审计字段）")
        assertEquals(false, feedbackB.triggered)

        val expectedA = (0.5f.toDouble() + 0.3).toFloat()
        val expectedB = (0.5f.toDouble() + 0.3 * 0.3).toFloat()
        val a = stores.knowledgePoints.getValue("k1")
        val b = stores.knowledgePoints.getValue("k2")
        assertEquals(expectedA, a.continuousScore)
        assertEquals(1.0f, a.effectiveFeedbackCount)
        assertEquals(expectedB, b.continuousScore, "非触发按该行自身档位增量 ×0.3（D-01）")
        assertEquals(Tier.UNKNOWN, b.currentTier, "非触发不改档位")
        assertEquals(0.0f, b.effectiveFeedbackCount, "非触发不推进有效反馈计数")
        assertEquals(plannedDate, b.nextDueDate, "非触发不改下次到期日")
    }

    @Test
    fun u04_non_triggered_familiar_scales_minus_point_one_by_point_three() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1", number = 1)
        stores.knowledgePoints["k2"] = knowledgePoint("k2", number = 2)
        stores.questions["q1"] = question("q1")

        submit(
            decisions = listOf(
                decision("q1", "k1", triggered = true, detectedTier = Tier.MASTERED),
                decision("q1", "k2", triggered = false, detectedTier = Tier.FAMILIAR),
            ),
        )

        val feedbackB = stores.reviewFeedbacks.single { it.knowledgePointId == "k2" }
        assertEquals(0.5f, feedbackB.effectiveWeight, "C-006：FAMILIAR 按半次计入有效反馈")
        assertEquals(0.3f, feedbackB.weightApplied)

        val expected = (0.5f.toDouble() + (-0.1 * 0.3)).toFloat()
        val b = stores.knowledgePoints.getValue("k2")
        assertEquals(expected, b.continuousScore, "非触发 FAMILIAR：−0.1×0.3")
        assertEquals(Tier.UNKNOWN, b.currentTier, "非触发不改档位")
        assertEquals(0.0f, b.effectiveFeedbackCount, "非触发不推进冷启动")
        assertEquals(plannedDate, b.nextDueDate)
    }

    @Test
    fun u05_non_triggered_not_reviewed_writes_only_decision_log_and_keeps_state() = runBlocking {
        // 用例表 U-05 的场景：一题关联 A（触发、已填涂）与 B（非触发、本次未复习）。
        stores.knowledgePoints["k1"] = knowledgePoint("k1", number = 1)
        stores.knowledgePoints["k2"] = knowledgePoint("k2", number = 2)
        stores.questions["q1"] = question("q1")

        val result = submit(
            decisions = listOf(
                decision("q1", "k1", triggered = true, detectedTier = Tier.MASTERED),
                decision(
                    "q1",
                    "k2",
                    triggered = false, // 非触发行
                    userDecision = UserDecision.NOT_REVIEWED,
                    detectedTier = null,
                ).copy(actualReviewDate = null),
            ),
        )

        assertEquals(1, stores.reviewFeedbacks.size, "只有触发行 A 写反馈，非触发的 NOT_REVIEWED 不写（D-13/C-010）")
        val feedbackA = stores.reviewFeedbacks.single()
        assertEquals("k1", feedbackA.knowledgePointId)
        assertEquals(Tier.MASTERED, feedbackA.tier)

        // B 完全不变：档位、连续掌握度、有效反馈计数、下次到期日都不动。
        val unchangedB = stores.knowledgePoints.getValue("k2")
        assertEquals(Tier.UNKNOWN, unchangedB.currentTier)
        assertEquals(0.5f, unchangedB.continuousScore)
        assertEquals(0.0f, unchangedB.effectiveFeedbackCount)
        assertEquals(plannedDate, unchangedB.nextDueDate)

        // 两行都写 ConfirmationDecisionLog，包括 NOT_REVIEWED。
        val logB = stores.decisionLogs.single { it.knowledgePointId == "k2" }
        assertEquals(UserDecision.NOT_REVIEWED, logB.decision)
        assertEquals(false, logB.triggered)
        assertEquals(null, logB.reviewedOn, "NOT_REVIEWED 的 reviewedOn 为空（C-038/C-039）")

        val updatedA = stores.knowledgePoints.getValue("k1")
        assertEquals(Tier.MASTERED, updatedA.currentTier)
        assertEquals((0.5f.toDouble() + 0.3).toFloat(), updatedA.continuousScore)
        assertEquals(1.0f, updatedA.effectiveFeedbackCount)
        assertEquals(reviewDate.plusDays(1), updatedA.nextDueDate)
        assertEquals(reviewDate, stores.questions.getValue("q1").lastReviewedDate, "题目复习日由触发行 A 更新")

        assertEquals(1, result.saveResult.writtenFeedbackCount)
        assertEquals(2, result.saveResult.writtenDecisionLogCount)
    }

    @Test
    fun u06_cold_start_mastered_uses_one_three_seven_days_and_reaches_three() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1")

        val first = reviewDate
        val second = first.plusDays(1)
        val third = second.plusDays(3)

        submit(listOf(decision("q1", "k1", reviewedOn = first)))
        assertEquals(1.0f, stores.knowledgePoints.getValue("k1").effectiveFeedbackCount)
        assertEquals(
            first.plusDays(1),
            stores.knowledgePoints.getValue("k1").nextDueDate,
            "冷启动第 1 次反馈间隔 1 天（D-05）",
        )

        submit(listOf(decision("q1", "k1", reviewedOn = second)))
        assertEquals(2.0f, stores.knowledgePoints.getValue("k1").effectiveFeedbackCount)
        assertEquals(
            second.plusDays(3),
            stores.knowledgePoints.getValue("k1").nextDueDate,
            "冷启动第 2 次反馈间隔 3 天（D-05）",
        )

        submit(listOf(decision("q1", "k1", reviewedOn = third)))
        val updated = stores.knowledgePoints.getValue("k1")
        assertEquals(3.0f, updated.effectiveFeedbackCount, "第 3 次后全 MASTERED 达到 3.0")
        assertEquals(
            third.plusDays(7),
            updated.nextDueDate,
            "冷启动第 3 次反馈间隔 7 天（D-05）",
        )
    }

    @Test
    fun u07_next_feedback_after_reaching_three_uses_dhp_interval() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1")

        val first = reviewDate
        val second = first.plusDays(1)
        val third = second.plusDays(3)
        val fourth = third.plusDays(7)

        submit(listOf(decision("q1", "k1", reviewedOn = first)))
        submit(listOf(decision("q1", "k1", reviewedOn = second)))

        submit(listOf(decision("q1", "k1", reviewedOn = third)))
        val afterThird = stores.knowledgePoints.getValue("k1")
        // C-087 方案 A：第 3 条后的初始化按公式给出独立期望值，而不是只判非空。
        assertEquals(1, afterThird.difficulty, "d_init = 1（全 MASTERED，第 3 次后掌握度 1.0）")
        assertEquals(114.27466583251953f, afterThird.halfLifeDays, "h_init 按 d_init/h0 与前三次间隔计算")
        assertEquals(third.plusDays(7), afterThird.nextDueDate, "达到 3.0 的这次仍按冷启动 7 天")

        submit(listOf(decision("q1", "k1", reviewedOn = fourth)))
        val updated = stores.knowledgePoints.getValue("k1")
        assertEquals(4.0f, updated.effectiveFeedbackCount)
        // C-086 方案 A：期望半衰期与间隔独立硬编码，不由落库值反推。
        assertEquals(243.53787231445312f, updated.halfLifeDays, "第 4 次 MASTERED 后的半衰期")
        assertEquals(
            fourth.plusDays(30),
            updated.nextDueDate,
            "达到 3.0 后的下一次反馈用 DHP：clamp(round(-h*log2(0.9)),1,30) = 30",
        )
    }

    @Test
    fun u12_reaching_three_with_familiar_keeps_cold_start_until_the_next_feedback() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1")

        val first = reviewDate
        val second = first.plusDays(1)
        val third = second.plusDays(3)
        val fourth = third.plusDays(7)
        val fifth = fourth.plusDays(7)
        val sixth = fifth.plusDays(7)
        val seventh = sixth.plusDays(7)

        suspend fun reviewFamiliar(reviewedOn: LocalDate) {
            submit(
                listOf(decision("q1", "k1", detectedTier = Tier.FAMILIAR, reviewedOn = reviewedOn)),
            )
        }

        reviewFamiliar(first)
        reviewFamiliar(second)
        reviewFamiliar(third)
        reviewFamiliar(fourth)
        reviewFamiliar(fifth)

        val afterFifth = stores.knowledgePoints.getValue("k1")
        assertEquals(2.5f, afterFifth.effectiveFeedbackCount, "全 FAMILIAR 第 5 次后为 2.5")
        assertEquals(fifth.plusDays(7), afterFifth.nextDueDate, "2.5 时仍按冷启动 7 天")

        reviewFamiliar(sixth)
        val afterSixth = stores.knowledgePoints.getValue("k1")
        assertEquals(3.0f, afterSixth.effectiveFeedbackCount, "第 6 次 FAMILIAR 后到达 3.0")
        assertEquals(
            sixth.plusDays(7),
            afterSixth.nextDueDate,
            "达到 3.0 的这次仍按冷启动设置到期日（D-05/C-018）",
        )

        reviewFamiliar(seventh)
        val afterSeventh = stores.knowledgePoints.getValue("k1")
        assertEquals(3.5f, afterSeventh.effectiveFeedbackCount)
        // C-086 方案 A：独立硬编码期望半衰期与间隔。
        assertEquals(127.32052612304688f, afterSeventh.halfLifeDays, "第 7 次 FAMILIAR 后的半衰期")
        assertEquals(
            seventh.plusDays(19),
            afterSeventh.nextDueDate,
            "达到 3.0 后的下一次反馈才切换 DHP：clamp(round(-h*log2(0.9)),1,30) = 19",
        )
    }

    @Test
    fun u08_familiar_cold_start_uses_one_three_seven_days_and_switches_at_seventh() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1")

        val dates = listOf(
            reviewDate,
            reviewDate.plusDays(1),
            reviewDate.plusDays(4),
            reviewDate.plusDays(11),
            reviewDate.plusDays(18),
            reviewDate.plusDays(25),
            reviewDate.plusDays(32),
        )

        suspend fun reviewFamiliar(reviewedOn: LocalDate) {
            submit(
                listOf(decision("q1", "k1", detectedTier = Tier.FAMILIAR, reviewedOn = reviewedOn)),
            )
        }

        reviewFamiliar(dates[0])
        assertEquals(0.5f, stores.knowledgePoints.getValue("k1").effectiveFeedbackCount)
        assertEquals(dates[0].plusDays(1), stores.knowledgePoints.getValue("k1").nextDueDate, "第 1 次 1 天")

        reviewFamiliar(dates[1])
        assertEquals(1.0f, stores.knowledgePoints.getValue("k1").effectiveFeedbackCount)
        assertEquals(dates[1].plusDays(3), stores.knowledgePoints.getValue("k1").nextDueDate, "第 2 次 3 天")

        reviewFamiliar(dates[2])
        assertEquals(1.5f, stores.knowledgePoints.getValue("k1").effectiveFeedbackCount)
        assertEquals(dates[2].plusDays(7), stores.knowledgePoints.getValue("k1").nextDueDate, "第 3 次 7 天")

        reviewFamiliar(dates[3])
        reviewFamiliar(dates[4])
        reviewFamiliar(dates[5])
        val afterSixth = stores.knowledgePoints.getValue("k1")
        assertEquals(3.0f, afterSixth.effectiveFeedbackCount, "全 FAMILIAR 第 6 次后才到 3.0")
        assertEquals(dates[5].plusDays(7), afterSixth.nextDueDate, "到达 3.0 的这次仍按冷启动")

        reviewFamiliar(dates[6])
        val afterSeventh = stores.knowledgePoints.getValue("k1")
        assertEquals(3.5f, afterSeventh.effectiveFeedbackCount)
        // C-086 方案 A：独立硬编码期望半衰期与间隔。
        assertEquals(127.32052612304688f, afterSeventh.halfLifeDays, "第 7 次 FAMILIAR 后的半衰期")
        assertEquals(
            dates[6].plusDays(19),
            afterSeventh.nextDueDate,
            "第 7 次切换为 DHP：clamp(round(-h*log2(0.9)),1,30) = 19",
        )
    }

    @Test
    fun u09_unmarked_row_aborts_without_writing_any_row() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1")
        val original = stores.knowledgePoints.getValue("k1")

        assertFailsWith<InvalidConfirmationException> {
            submit(
                listOf(
                    decision("q1", "k1", detectedTier = null, userDecision = UserDecision.UNMARKED),
                ),
            )
        }

        assertEquals(0, stores.reviewFeedbacks.size, "UNMARKED 不得写 ReviewFeedback")
        assertEquals(0, stores.decisionLogs.size, "UNMARKED 不得写确认决定日志")
        assertEquals(original, stores.knowledgePoints.getValue("k1"), "知识点状态不得变化")
        assertEquals(null, stores.questions.getValue("q1").lastReviewedDate)
        assertEquals(0, stores.transactionCount, "前置校验失败时不应进入写入事务")
    }

    @Test
    fun u10_not_reviewed_keeps_dhp_state_and_due_date() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint(
            "k1",
            tier = Tier.FAMILIAR,
            score = 0.84f,
            count = 2.5f,
            nextDueDate = plannedDate.plusDays(5),
            difficulty = 7,
            halfLifeDays = 12.5f,
        )
        stores.questions["q1"] = question("q1")
        val original = stores.knowledgePoints.getValue("k1")

        submit(
            listOf(
                decision(
                    "q1",
                    "k1",
                    userDecision = UserDecision.NOT_REVIEWED,
                    detectedTier = null,
                ).copy(actualReviewDate = null),
            ),
        )

        assertEquals(0, stores.reviewFeedbacks.size, "NOT_REVIEWED 不写 ReviewFeedback")
        val log = stores.decisionLogs.single()
        assertEquals(UserDecision.NOT_REVIEWED, log.decision)
        assertEquals(null, log.reviewedOn)
        assertEquals(original, stores.knowledgePoints.getValue("k1"), "NOT_REVIEWED 不改任何状态")
        assertEquals(null, stores.questions.getValue("q1").lastReviewedDate)
    }

    @Test
    fun u11_non_triggered_row_keeps_tier_due_date_and_count_but_scales_score() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1", number = 1)
        val nonTriggeredDueDate = plannedDate.plusDays(5)
        stores.knowledgePoints["k2"] = knowledgePoint(
            "k2",
            number = 2,
            tier = Tier.FAMILIAR,
            score = 0.8f,
            count = 1.5f,
            nextDueDate = nonTriggeredDueDate,
            difficulty = 4,
            halfLifeDays = 20.0f,
        )
        stores.questions["q1"] = question("q1")

        submit(
            listOf(
                decision("q1", "k1", triggered = true, detectedTier = Tier.MASTERED),
                decision("q1", "k2", triggered = false, detectedTier = Tier.MASTERED),
            ),
        )

        val b = stores.knowledgePoints.getValue("k2")
        val expectedScore = (0.8f.toDouble() + 0.3 * 0.3).toFloat() // C-050：Double 计算后转 Float
        assertEquals(expectedScore, b.continuousScore, "非触发按该行档位增量 ×0.3（D-01/D-03）")
        assertEquals(Tier.FAMILIAR, b.currentTier, "非触发不改档位")
        assertEquals(nonTriggeredDueDate, b.nextDueDate, "非触发不改下次到期日")
        assertEquals(1.5f, b.effectiveFeedbackCount, "非触发不推进有效反馈计数")
        assertEquals(4, b.difficulty, "非触发不改 DHP 难度")
        assertEquals(20.0f, b.halfLifeDays, "非触发不改 DHP 半衰期")

        val feedbackB = stores.reviewFeedbacks.single { it.knowledgePointId == "k2" }
        assertEquals(0.3f, feedbackB.weightApplied)
        assertEquals(false, feedbackB.triggered)
    }

    @Test
    fun u13_same_session_multiple_rows_writes_one_feedback_per_row_and_clears_pending() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1", number = 1)
        stores.knowledgePoints["k2"] = knowledgePoint("k2", number = 2)
        stores.questions["q1"] = question("q1")
        stores.questions["q2"] = question("q2")
        stores.questions["q3"] = question("q3")
        stores.questions["q4"] = question("q4")

        val result = submit(
            decisions = listOf(
                decision("q1", "k1", detectedTier = Tier.MASTERED),
                decision("q2", "k1", detectedTier = Tier.MASTERED),
                decision("q3", "k2", detectedTier = Tier.FAMILIAR),
                decision(
                    "q4",
                    "k2",
                    userDecision = UserDecision.NOT_REVIEWED,
                    detectedTier = null,
                ).copy(actualReviewDate = null),
            ),
        )

        assertEquals(3, stores.reviewFeedbacks.size, "每行写一条 ReviewFeedback，NOT_REVIEWED 除外")
        assertEquals(
            setOf("q1" to "k1", "q2" to "k1", "q3" to "k2"),
            stores.reviewFeedbacks.map { it.questionId to it.knowledgePointId }.toSet(),
        )
        assertEquals(4, stores.decisionLogs.size, "包括 NOT_REVIEWED，全部写 ConfirmationDecisionLog")
        assertEquals(3, result.saveResult.writtenFeedbackCount)
        assertEquals(4, result.saveResult.writtenDecisionLogCount)
        assertEquals(emptyList(), stores.pendingFeedbacks, "提交成功后清空该 session 的 PendingFeedback")

        // k1 在同一提交中被两道题触发：事件序号应为连续的第 1、2 条，第 2 条按 3 天推进（D-05）。
        val k1 = stores.knowledgePoints.getValue("k1")
        assertEquals(Tier.MASTERED, k1.currentTier)
        assertEquals(1.0f, k1.continuousScore, "0.5 + 0.3 + 0.3 夹到 1.0")
        assertEquals(2.0f, k1.effectiveFeedbackCount)
        assertEquals(
            reviewDate.plusDays(3),
            k1.nextDueDate,
            "同一提交内的第二条 triggered 反馈按事件序号 2 推进 3 天，不与第一行共用事件序号 1",
        )

        val k2 = stores.knowledgePoints.getValue("k2")
        assertEquals(0.5f, k2.effectiveFeedbackCount, "FAMILIAR 按 0.5 次计入")
        assertEquals(reviewDate.plusDays(1), k2.nextDueDate)

        assertEquals(reviewDate, stores.questions.getValue("q1").lastReviewedDate)
        assertEquals(reviewDate, stores.questions.getValue("q2").lastReviewedDate)
        assertEquals(reviewDate, stores.questions.getValue("q3").lastReviewedDate)
        assertEquals(null, stores.questions.getValue("q4").lastReviewedDate, "NOT_REVIEWED 不更新复习日")
    }

    @Test
    fun u15_accept_with_null_detected_tier_uses_selected_tier_and_half_weight() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1")

        val result = submit(
            decisions = listOf(
                decision(
                    "q1",
                    "k1",
                    userDecision = UserDecision.ACCEPT,
                    detectedTier = null, // OMR 未识别
                    selectedTier = Tier.FAMILIAR, // 用户选档补足（C-022）
                ),
            ),
        )

        val feedback = stores.reviewFeedbacks.single()
        assertEquals(Tier.FAMILIAR, feedback.tier, "不得写入空档位")
        assertEquals(0.5f, feedback.effectiveWeight, "FAMILIAR 按半次计入有效反馈（C-006）")
        assertEquals(1.0f, feedback.weightApplied, "触发行权重仍为 1.0")

        val updated = stores.knowledgePoints.getValue("k1")
        assertEquals(Tier.FAMILIAR, updated.currentTier)
        assertEquals((0.5f.toDouble() - 0.1).toFloat(), updated.continuousScore)
        assertEquals(0.5f, updated.effectiveFeedbackCount)
        assertEquals(reviewDate.plusDays(1), updated.nextDueDate)

        val log = stores.decisionLogs.single()
        assertEquals(UserDecision.ACCEPT, log.decision)
        assertEquals(Tier.FAMILIAR, log.selectedTier)
        assertEquals(1, result.saveResult.writtenFeedbackCount)
        assertEquals(1, result.saveResult.writtenDecisionLogCount)
    }

    @Test
    fun u16_history_with_only_non_triggered_feedback_keeps_first_triggered_interval_at_one_day() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1")
        // 历史只有非触发行：它们不推进冷启动阶梯（D-05、U-16）。
        stores.reviewFeedbacks += reviewFeedback(
            questionId = "q0a",
            knowledgePointId = "k1",
            tier = Tier.FAMILIAR,
            triggered = false,
            reviewedOn = reviewDate.minusDays(5),
        )
        stores.reviewFeedbacks += reviewFeedback(
            questionId = "q0b",
            knowledgePointId = "k1",
            tier = Tier.UNKNOWN,
            triggered = false,
            reviewedOn = reviewDate.minusDays(2),
        )

        submit(
            decisions = listOf(decision("q1", "k1", detectedTier = Tier.MASTERED)),
        )

        val feedback = stores.reviewFeedbacks.last { it.questionId == "q1" }
        assertEquals(Tier.MASTERED, feedback.tier)
        assertEquals(true, feedback.triggered)

        val updated = stores.knowledgePoints.getValue("k1")
        assertEquals(1.0f, updated.effectiveFeedbackCount, "只有本次 triggered 计入有效反馈")
        assertEquals(
            reviewDate.plusDays(1),
            updated.nextDueDate,
            "历史非触发行不推进事件序号；本次仍是事件 1（1 天）",
        )
    }

    @Test
    fun u17_third_triggered_feedback_initializes_memory_state_then_familiar_half_strength_and_unknown_plus_two() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1")

        val first = reviewDate // createdAt 同日 → 首个间隔按 1 天兜底
        val second = first.plusDays(1)
        val third = second.plusDays(3)
        val fourth = third.plusDays(7) // 第 3 次后的冷启动到期日

        submit(listOf(decision("q1", "k1", reviewedOn = first)))
        submit(listOf(decision("q1", "k1", reviewedOn = second)))
        submit(listOf(decision("q1", "k1", reviewedOn = third)))

        val afterThird = stores.knowledgePoints.getValue("k1")
        // C-087 方案 A：初始化按公式给出独立期望值（全 MASTERED，scoreAfterThird=1.0）。
        assertEquals(1, afterThird.difficulty, "d_init = 1")
        assertEquals(114.27466583251953f, afterThird.halfLifeDays, "h_init")
        assertEquals(3.0f, afterThird.effectiveFeedbackCount)
        assertEquals(third.plusDays(7), afterThird.nextDueDate, "第 3 次仍按冷启动 7 天（C-018）")

        // 第 4 次 FAMILIAR：半强度 recall，难度不变（C-086 方案 A：独立期望值）。
        submit(
            listOf(decision("q1", "k1", detectedTier = Tier.FAMILIAR, reviewedOn = fourth)),
        )
        val afterFamiliar = stores.knowledgePoints.getValue("k1")
        assertEquals(178.90626525878906f, afterFamiliar.halfLifeDays, "FAMILIAR 取半强度回忆（C-018）")
        assertEquals(1, afterFamiliar.difficulty, "FAMILIAR 不改难度")
        assertEquals(
            fourth.plusDays(27),
            afterFamiliar.nextDueDate,
            "达到 3.0 后按 DHP 计算到期日：clamp(round(-h*log2(0.9)),1,30) = 27",
        )

        // 第 5 次 UNKNOWN：难度 +2，半衰期用 forget 公式。
        val fifth = fourth.plusDays(27)
        submit(
            listOf(decision("q1", "k1", detectedTier = Tier.UNKNOWN, reviewedOn = fifth)),
        )
        val afterUnknown = stores.knowledgePoints.getValue("k1")
        assertEquals(3, afterUnknown.difficulty, "UNKNOWN 难度 +2（C-018/C-037）")
        assertEquals(11.457879066467285f, afterUnknown.halfLifeDays, "UNKNOWN 用 forget 公式更新半衰期")
    }

    @Test
    fun u18_late_batch_uses_actual_review_date_for_due_date_and_last_reviewed_date() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1")
        val monday = plannedDate // 2026-11-16，周一
        val wednesday = monday.plusDays(2)

        submit(
            decisions = listOf(decision("q1", "k1", reviewedOn = wednesday)),
        )

        val feedback = stores.reviewFeedbacks.single()
        assertEquals(monday, feedback.plannedDate)
        assertEquals(wednesday, feedback.reviewedOn, "ReviewFeedback.reviewedOn 记实际复习日")
        assertEquals(
            2L,
            ChronoUnit.DAYS.between(feedback.plannedDate, feedback.reviewedOn),
            "lateDays = 实际复习日 - 计划日 = 2",
        )
        assertEquals(
            wednesday.plusDays(1),
            stores.knowledgePoints.getValue("k1").nextDueDate,
            "nextDueDate 以实际复习日为基准，而不是计划日",
        )
        assertEquals(
            wednesday,
            stores.questions.getValue("q1").lastReviewedDate,
            "Question.lastReviewedDate 记实际复习日（冷却判定用）",
        )
    }

    @Test
    fun u19_question_with_all_not_reviewed_rows_stays_out_of_cooling_for_the_next_plan() = runBlocking {
        val oldReviewedDate = plannedDate.minusDays(30)
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1").copy(lastReviewedDate = oldReviewedDate)

        val result = submit(
            decisions = listOf(
                decision(
                    "q1",
                    "k1",
                    userDecision = UserDecision.NOT_REVIEWED,
                    detectedTier = null,
                ).copy(actualReviewDate = null),
            ),
        )

        assertEquals(0, stores.reviewFeedbacks.size, "全部 NOT_REVIEWED 不写 ReviewFeedback")
        assertEquals(1, stores.decisionLogs.size, "仍写 ConfirmationDecisionLog")
        assertEquals(
            oldReviewedDate,
            stores.questions.getValue("q1").lastReviewedDate,
            "NOT_REVIEWED 不更新 lastReviewedDate",
        )
        assertEquals(0, result.saveResult.writtenFeedbackCount)

        // 下一份计划：因为没有写入新的复习日，题目不进入 7 天冷却，应正常排入。
        val state = stores.knowledgePoints.getValue("k1").toKnowledgeState().copy(nextDueDate = plannedDate)
        val scheduled = DefaultScheduler().plan(
            knowledgeStates = listOf(state),
            questions = listOf(stores.questions.getValue("q1").toSchedulerQuestion(listOf("k1"))),
            dailyAvailableMinutes = mapOf(plannedDate to 60),
            dateRange = plannedDate..plannedDate,
            coolingPeriodDays = 7,
            coolingOverrides = emptySet(),
        )

        assertEquals(
            listOf("q1"),
            scheduled.dailyAssignments.getValue(plannedDate).map { it.questionId },
            "未实际复习的题目不因冷却被跳过",
        )
        assertEquals(emptyList(), scheduled.deferred)
    }

    // ---- C-077 / C-078：整题跳过时状态不落库，但确认日志仍写 ----

    @Test
    fun c077_skipped_question_does_not_update_knowledge_state_and_still_logs() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        // q1 不存在：待确认行仍种入，提交时按 C-055 整题跳过。
        val original = stores.knowledgePoints.getValue("k1")

        val result = submit(listOf(decision("q1", "k1")))

        assertEquals(emptyList(), stores.reviewFeedbacks, "跳过题目不得写反馈")
        assertEquals(1, stores.decisionLogs.size, "C-078 方案 A：跳过行仍写确认日志")
        assertEquals(original, stores.knowledgePoints.getValue("k1"), "C-077 方案 A：不写知识点状态")
        assertEquals(listOf("q1"), result.saveResult.skippedQuestionIds)
        assertEquals(emptyList(), result.updatedKnowledgeStates)
        assertEquals(1, stores.pendingFeedbacks.size, "待确认行保留供修复后重试")
    }

    @Test
    fun c078_missing_knowledge_point_skips_whole_question_without_throwing() = runBlocking {
        stores.questions["q1"] = question("q1")
        // k1 不存在：整题跳过，不再整批抛错。

        val result = submit(listOf(decision("q1", "k1")))

        assertEquals(emptyList(), stores.reviewFeedbacks)
        assertEquals(1, stores.decisionLogs.size)
        assertEquals(listOf("q1"), result.saveResult.skippedQuestionIds)
        assertEquals(listOf("k1"), result.saveResult.skippedKnowledgePointIds)
        assertEquals(1, stores.pendingFeedbacks.size)
    }

    // ---- C-079 / C-080 / C-081：前置校验与会话语义 ----

    @Test
    fun c079_empty_decisions_are_rejected_before_any_transaction() = runBlocking {
        assertFailsWith<InvalidConfirmationException> {
            useCase.execute("no-session", emptyList())
        }
        assertEquals(0, stores.transactionCount)
    }

    @Test
    fun c079_duplicate_rows_are_rejected() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1")

        assertFailsWith<InvalidConfirmationException> {
            submit(listOf(decision("q1", "k1"), decision("q1", "k1")))
        }
        assertEquals(0, stores.reviewFeedbacks.size)
    }

    @Test
    fun c079_review_date_after_today_is_rejected() = runBlocking {
        today = Instant.parse("2026-11-16T12:00:00Z")
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1")

        assertFailsWith<InvalidConfirmationException> {
            submit(listOf(decision("q1", "k1", reviewedOn = plannedDate.plusDays(2))))
        }
        assertEquals(0, stores.reviewFeedbacks.size)
    }

    @Test
    fun c079_missing_review_date_and_missing_tier_are_rejected() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1")

        assertFailsWith<InvalidConfirmationException> {
            submit(listOf(decision("q1", "k1").copy(actualReviewDate = null)))
        }
        assertFailsWith<InvalidConfirmationException> {
            submit(
                listOf(
                    decision("q1", "k1", userDecision = UserDecision.CORRECTED, selectedTier = null),
                ),
            )
        }
        assertEquals(0, stores.reviewFeedbacks.size)
    }

    @Test
    fun c079_pending_planned_date_and_triggered_flag_must_match() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1")
        val d = decision("q1", "k1")

        seedSession("s-planned", listOf(d)) {
            it.copy(plannedDate = d.plannedDate.minusDays(1))
        }
        assertFailsWith<InvalidConfirmationException> {
            useCase.execute("s-planned", listOf(d))
        }

        seedSession("s-triggered", listOf(d)) { it.copy(triggered = false) }
        assertFailsWith<InvalidConfirmationException> {
            useCase.execute("s-triggered", listOf(d))
        }
        assertEquals(0, stores.reviewFeedbacks.size)
    }

    @Test
    fun c079_pending_row_set_must_be_complete() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1")
        val d = decision("q1", "k1")
        seedSession("s-extra", listOf(d))
        stores.pendingFeedbacks += pending("q2", "k2", sessionId = "s-extra")

        assertFailsWith<InvalidConfirmationException> {
            useCase.execute("s-extra", listOf(d))
        }
        assertEquals(0, stores.reviewFeedbacks.size)
    }

    @Test
    fun c079_knowledge_point_must_belong_to_the_question() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.knowledgePoints["k2"] = knowledgePoint("k2", number = 2)
        stores.questions["q1"] = question("q1")
        val d = decision("q1", "k2")
        seedSession("s-unlinked", listOf(d))
        stores.questionLinks.getValue("q1").remove("k2")

        assertFailsWith<InvalidConfirmationException> {
            useCase.execute("s-unlinked", listOf(d))
        }
        assertEquals(0, stores.reviewFeedbacks.size)
    }

    @Test
    fun c080_repeated_submission_of_a_submitted_session_is_rejected_without_writes() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1")
        val decisions = listOf(decision("q1", "k1"))

        submit(decisions, sessionId = "fixed-session")
        assertEquals(ScanSessionStatus.SUBMITTED, stores.scanSessions.getValue("fixed-session").status)
        val feedbackCount = stores.reviewFeedbacks.size
        val logCount = stores.decisionLogs.size

        assertFailsWith<InvalidConfirmationException> {
            useCase.execute("fixed-session", decisions)
        }
        assertEquals(feedbackCount, stores.reviewFeedbacks.size, "重复提交不得再写反馈")
        assertEquals(logCount, stores.decisionLogs.size, "重复提交不得再写日志")
    }

    @Test
    fun c081_review_before_plan_week_start_is_rejected() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1")
        val sunday = plannedDate.minusDays(1) // 计划日周一，周一为周起始

        assertFailsWith<InvalidConfirmationException> {
            submit(listOf(decision("q1", "k1", reviewedOn = sunday)))
        }
        assertEquals(0, stores.reviewFeedbacks.size)
    }

    @Test
    fun c081_plan_week_start_follows_the_configured_day() = runBlocking {
        planStartDay = DayOfWeek.SUNDAY
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1")
        val sunday = plannedDate.minusDays(1) // 周起始为周日时，11-15 属于同一计划周

        submit(listOf(decision("q1", "k1", reviewedOn = sunday)))

        assertEquals(sunday, stores.reviewFeedbacks.single().reviewedOn)
    }

    // ---- C-082：迁移兜底窗口的首条间隔 ----

    @Test
    fun c082_migration_fallback_uses_previous_reviewed_on_as_interval_anchor() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint(
            "k1",
            score = 0.5f,
            count = 3.0f,
            difficulty = null,
            halfLifeDays = null,
            createdAt = Instant.parse("2024-01-01T12:00:00Z"),
        )
        stores.questions["q1"] = question("q1")
        listOf(2, 6, 10, 14).forEach { day ->
            stores.reviewFeedbacks += reviewFeedback(
                questionId = "q0$day",
                knowledgePointId = "k1",
                tier = Tier.MASTERED,
                triggered = true,
                reviewedOn = LocalDate.of(2026, 2, day),
            )
        }

        submit(
            listOf(
                decision(
                    "q1",
                    "k1",
                    reviewedOn = LocalDate.of(2026, 2, 16),
                ).copy(plannedDate = LocalDate.of(2026, 2, 16)),
            ),
        )

        val updated = stores.knowledgePoints.getValue("k1")
        assertEquals(
            273.3910827636719f,
            updated.halfLifeDays,
            "迁移窗口首条间隔从 02-02 起算（4 天），不是 createdAt（否则会被夹到 365 天）",
        )
    }

    @Test
    fun u13_transaction_failure_rolls_back_every_row_and_keeps_pending_feedback() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1", number = 1)
        stores.knowledgePoints["k2"] = knowledgePoint("k2", number = 2)
        stores.questions["q1"] = question("q1")
        stores.questions["q2"] = question("q2")
        val k1Before = stores.knowledgePoints.getValue("k1")
        val k2Before = stores.knowledgePoints.getValue("k2")

        stores.failOnKnowledgePointUpdate = IllegalStateException("模拟写库失败")

        assertFailsWith<IllegalStateException> {
            submit(
                decisions = listOf(
                    decision("q1", "k1"),
                    decision("q2", "k2"),
                ),
            )
        }

        assertEquals(0, stores.reviewFeedbacks.size, "失败提交不得残留 ReviewFeedback")
        assertEquals(0, stores.decisionLogs.size, "失败提交不得残留 ConfirmationDecisionLog")
        assertEquals(k1Before, stores.knowledgePoints.getValue("k1"), "知识点状态回滚")
        assertEquals(k2Before, stores.knowledgePoints.getValue("k2"), "知识点状态回滚")
        assertEquals(null, stores.questions.getValue("q1").lastReviewedDate)
        assertEquals(null, stores.questions.getValue("q2").lastReviewedDate)
        assertEquals(
            setOf("q1" to "k1", "q2" to "k2"),
            stores.pendingFeedbacks.map { it.questionId to it.knowledgePointId }.toSet(),
            "回滚后待确认行保留，供重试",
        )
    }

    private fun plan(status: String = "PRINTED") = Plan(
        planId = planId,
        weekStart = plannedDate,
        weekEnd = plannedDate.plusDays(6),
        createdAt = createdAtInstant,
        status = status,
        printedAt = createdAtInstant,
        supersededBy = null,
    )

    private fun planPage(pageNumber: Int) = PlanPage(
        planId = planId,
        pageId = "$planId-${pageNumber.toString().padStart(2, '0')}",
        pageNumber = pageNumber,
        date = plannedDate.plusDays(pageNumber - 1L),
        itemCount = 1,
        questionIds = "[]",
        knowledgeRows = "[]",
        continuationMarkers = "[]",
    )

    /** C-099：每一页都确认成功后，计划才迁移为 COMPLETED。 */
    @Test
    fun c099_plan_becomes_completed_only_after_every_page_is_confirmed() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1")
        stores.questions["q2"] = question("q2")
        stores.plans[planId] = plan()
        val firstPage = "$planId-01"
        val secondPage = "$planId-02"
        stores.planPages[firstPage] = planPage(1)
        stores.planPages[secondPage] = planPage(2)

        submit(listOf(decision("q1", "k1", pageId = firstPage)))
        assertEquals(
            "PRINTED",
            stores.plans.getValue(planId).status,
            "只确认其中一页时计划不能算完成",
        )

        submit(listOf(decision("q2", "k1", pageId = secondPage)))
        assertEquals(
            "COMPLETED",
            stores.plans.getValue(planId).status,
            "全部页面确认后计划应标记为已完成",
        )
    }

    /** C-099：已被显式替代的计划不得改回 COMPLETED。 */
    @Test
    fun c099_superseded_plan_is_not_marked_completed() = runBlocking {
        stores.knowledgePoints["k1"] = knowledgePoint("k1")
        stores.questions["q1"] = question("q1")
        stores.plans[planId] = plan(status = "SUPERSEDED")
        val page = "$planId-01"
        stores.planPages[page] = planPage(1)

        submit(listOf(decision("q1", "k1", pageId = page)))

        assertEquals(
            "SUPERSEDED",
            stores.plans.getValue(planId).status,
            "已被替代的计划不得改回 COMPLETED",
        )
    }
}
