package com.recollect.feature.confirm

import com.recollect.data.room.entity.DetectionStatus
import com.recollect.data.room.entity.UserDecision
import com.recollect.model.Tier
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 确认页状态机（confirm-flow.md「状态机」、D-13、R-04）。
 *
 * 本测试只锁定「逐项标记」这一核心用例：四种决策各自独立生效、未标记行阻止提交、
 * 未来日期的行只能标记本次未复习，以及 NOT_REVIEWED 不携带实际复习日。
 */
class ConfirmStateMachineTest {

    private val today = LocalDate.of(2026, 11, 18)

    private fun key(questionId: String, knowledgePointId: String) =
        ConfirmRowKey(questionId = questionId, knowledgePointId = knowledgePointId)

    private fun row(
        questionId: String,
        knowledgePointId: String,
        plannedDate: LocalDate = today,
        detectedTier: Tier? = Tier.MASTERED,
        detectionStatus: DetectionStatus = if (detectedTier == null) {
            DetectionStatus.NO_MARK
        } else {
            DetectionStatus.OK
        },
    ) = ConfirmRow(
        planId = "AB12CD",
        pageId = "AB12CD-01",
        plannedDate = plannedDate,
        actualReviewDate = plannedDate,
        questionId = questionId,
        questionNumber = "T-$questionId",
        knowledgePointId = knowledgePointId,
        knowledgeNumber = questionId.last().digitToInt(),
        knowledgeName = "知识点$knowledgePointId",
        triggered = true,
        detectedTier = detectedTier,
        detectionStatus = detectionStatus,
        conflict = false,
        detectedRawOcr = null,
    )

    @Test
    fun each_row_is_marked_individually_across_four_decisions_and_unmarked_rows_block_submit() {
        val rows = listOf(
            row("q1", "k1", detectedTier = Tier.MASTERED),
            row("q2", "k2", detectedTier = null),
            row("q3", "k3", detectedTier = Tier.UNKNOWN),
            row("q4", "k4", detectedTier = Tier.FAMILIAR),
            row("q5", "k5", plannedDate = today.plusDays(1), detectedTier = Tier.MASTERED),
            row("q6", "k6", detectedTier = Tier.MASTERED),
        )
        val machine = ConfirmStateMachine(rows = rows, today = today)

        // D-13：初始全部未标记，不能提交；逐项操作不会顺带标记其它行。
        assertEquals(6, machine.unmarkedCount)
        assertFalse(machine.canSubmit)
        assertEquals(MarkOutcome.Applied, machine.accept(key("q1", "k1")))
        assertEquals(5, machine.unmarkedCount)
        assertEquals(
            UserDecision.UNMARKED,
            machine.decisionOf(key("q2", "k2")).decision,
            "不得一键全部接受，其它行必须保持未标记",
        )

        // C-022：OMR 有档位时接受直接使用 OMR；OMR 为空时必须先选档位。
        assertEquals(MarkOutcome.NeedsTier, machine.accept(key("q2", "k2")))
        assertEquals(MarkOutcome.Applied, machine.accept(key("q2", "k2"), Tier.FAMILIAR))
        assertEquals(Tier.FAMILIAR, machine.decisionOf(key("q2", "k2")).selectedTier)

        // 修正必须选档位；修正文本保留。
        assertEquals(MarkOutcome.NeedsTier, machine.correct(key("q3", "k3"), null))
        machine.setCorrectedText(key("q3", "k3"), "笔迹不清")
        assertEquals(MarkOutcome.Applied, machine.correct(key("q3", "k3"), Tier.MASTERED))
        assertEquals("笔迹不清", machine.decisionOf(key("q3", "k3")).correctedText)

        // 本次未复习。
        assertEquals(MarkOutcome.Applied, machine.notReviewed(key("q4", "k4")))

        // 补填必须选档位，并标记为补填（C-088）。
        assertEquals(MarkOutcome.NeedsTier, machine.supplement(key("q6", "k6"), null))
        assertEquals(MarkOutcome.Applied, machine.supplement(key("q6", "k6"), Tier.UNKNOWN))
        assertEquals(UserDecision.SUPPLEMENTED, machine.decisionOf(key("q6", "k6")).decision)

        // 计划日晚于今天时只能标记本次未复习。
        assertEquals(
            MarkOutcome.Rejected("未来日期的行只能标记“本次未复习”"),
            machine.accept(key("q5", "k5")),
        )
        assertEquals(MarkOutcome.Applied, machine.notReviewed(key("q5", "k5")))
        assertTrue(machine.canSubmit)

        val decisions = machine.toDecisions().associateBy { it.questionId }
        assertEquals(UserDecision.ACCEPT, decisions.getValue("q1").decision)
        assertNull(decisions.getValue("q1").selectedTier)

        assertEquals(UserDecision.ACCEPT, decisions.getValue("q2").decision)
        assertEquals(Tier.FAMILIAR, decisions.getValue("q2").selectedTier)

        assertEquals(UserDecision.CORRECTED, decisions.getValue("q3").decision)
        assertEquals(Tier.MASTERED, decisions.getValue("q3").selectedTier)
        assertEquals("笔迹不清", decisions.getValue("q3").correctedText)

        assertEquals(UserDecision.NOT_REVIEWED, decisions.getValue("q4").decision)
        assertNull(decisions.getValue("q4").actualReviewDate, "NOT_REVIEWED 不写复习日")
        assertEquals(UserDecision.NOT_REVIEWED, decisions.getValue("q5").decision)
        assertNull(decisions.getValue("q5").actualReviewDate)

        assertEquals(UserDecision.SUPPLEMENTED, decisions.getValue("q6").decision)
        assertEquals(Tier.UNKNOWN, decisions.getValue("q6").selectedTier)
        assertTrue(decisions.getValue("q6").isSupplemental)
    }
}
