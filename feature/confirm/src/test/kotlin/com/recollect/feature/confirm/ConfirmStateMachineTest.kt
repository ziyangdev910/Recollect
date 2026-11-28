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
 * 本测试只锁定「逐项标记」这一核心用例：三种决策各自独立生效、未标记行阻止提交、
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
    fun each_row_is_marked_individually_across_three_decisions_and_unmarked_rows_block_submit() {
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

        // 修改复习效果必须选档位；修正文本保留。
        assertEquals(MarkOutcome.NeedsTier, machine.correct(key("q3", "k3"), null))
        machine.setCorrectedText(key("q3", "k3"), "笔迹不清")
        assertEquals(MarkOutcome.Applied, machine.correct(key("q3", "k3"), Tier.MASTERED))
        assertEquals("笔迹不清", machine.decisionOf(key("q3", "k3")).correctedText)

        // 本次未复习。
        assertEquals(MarkOutcome.Applied, machine.notReviewed(key("q4", "k4")))

        // 修改复习效果同样适用于 OMR 已识别的行，以用户选择的档位为准。
        assertEquals(MarkOutcome.NeedsTier, machine.correct(key("q6", "k6"), null))
        assertEquals(MarkOutcome.Applied, machine.correct(key("q6", "k6"), Tier.UNKNOWN))
        assertEquals(UserDecision.CORRECTED, machine.decisionOf(key("q6", "k6")).decision)

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

        assertEquals(UserDecision.CORRECTED, decisions.getValue("q6").decision)
        assertEquals(Tier.UNKNOWN, decisions.getValue("q6").selectedTier)
    }

    /**
     * 2026-11-22 用户要求：确认页只保留一个可编辑文本框，默认填入 OCR 识别文本；
     * 原始 OCR 仍单独保存，用户编辑只改 `correctedText`（D-06）。
     */
    @Test
    fun corrected_text_defaults_to_raw_ocr_and_edits_keep_raw_untouched() {
        val rows = listOf(
            row("q1", "k1").copy(detectedRawOcr = "手写：x+1=2"),
            row("q2", "k2").copy(detectedRawOcr = "另一条笔记"),
        )
        val machine = ConfirmStateMachine(rows = rows, today = today)

        assertEquals(
            "手写：x+1=2",
            machine.decisionOf(key("q1", "k1")).correctedText,
            "可编辑文本应默认填入识别原文",
        )

        machine.setCorrectedText(key("q1", "k1"), "手写：x+1=3")
        machine.accept(key("q1", "k1"))
        machine.accept(key("q2", "k2"))
        val decisions = machine.toDecisions().associateBy { it.questionId }

        assertEquals("手写：x+1=2", decisions.getValue("q1").detectedRawOcr, "原始 OCR 必须保持只读")
        assertEquals("手写：x+1=3", decisions.getValue("q1").correctedText, "编辑后的文本写入修正文本")
        assertEquals("另一条笔记", decisions.getValue("q2").correctedText, "未编辑时修正文本保留识别原文")
    }

    /**
     * 2026-11-28 用户要求：修改掌握程度只暂存，不算完成校对；
     * 必须手动点击卡片「确认」才落定，已确认的行锁定效果、不再允许修改。
     */
    @Test
    fun editing_effect_only_stages_a_choice_until_the_card_is_confirmed() {
        val machine = ConfirmStateMachine(
            rows = listOf(row("q1", "k1", detectedTier = Tier.MASTERED)),
            today = today,
        )

        machine.setPendingEffect(key("q1", "k1"), EffectChoice.Rated(Tier.FAMILIAR))
        assertEquals(
            UserDecision.UNMARKED,
            machine.decisionOf(key("q1", "k1")).decision,
            "选好效果后仍应等待用户手动确认",
        )
        assertEquals(1, machine.unmarkedCount)
        assertFalse(machine.canSubmit)

        assertEquals(MarkOutcome.Applied, machine.confirm(key("q1", "k1")))
        val decision = machine.decisionOf(key("q1", "k1"))
        assertEquals(UserDecision.CORRECTED, decision.decision)
        assertEquals(Tier.FAMILIAR, decision.selectedTier)
        assertNull(decision.pendingEffect)
        assertTrue(machine.canSubmit)

        // 已确认的行锁定复习效果：再次修改不生效，决策保持不变。
        machine.setPendingEffect(key("q1", "k1"), EffectChoice.Rated(Tier.MASTERED))
        val locked = machine.decisionOf(key("q1", "k1"))
        assertEquals(UserDecision.CORRECTED, locked.decision)
        assertEquals(Tier.FAMILIAR, locked.selectedTier)
        assertNull(locked.pendingEffect)
        assertTrue(machine.canSubmit)
    }

    /** 暂存档位与识别结果相同时，手动确认按“接受”落定。 */
    @Test
    fun staged_choice_equal_to_detection_is_committed_as_accept() {
        val machine = ConfirmStateMachine(
            rows = listOf(row("q1", "k1", detectedTier = Tier.MASTERED)),
            today = today,
        )
        machine.setPendingEffect(key("q1", "k1"), EffectChoice.Rated(Tier.MASTERED))
        assertEquals(MarkOutcome.Applied, machine.confirm(key("q1", "k1")))
        assertEquals(UserDecision.ACCEPT, machine.decisionOf(key("q1", "k1")).decision)
    }

    /** 未在弹窗里选择时，卡片「确认」直接接受识别结果。 */
    @Test
    fun confirming_without_a_staged_choice_accepts_the_detected_tier() {
        val machine = ConfirmStateMachine(
            rows = listOf(row("q1", "k1", detectedTier = Tier.UNKNOWN)),
            today = today,
        )
        assertEquals(MarkOutcome.Applied, machine.confirm(key("q1", "k1")))
        val decision = machine.decisionOf(key("q1", "k1"))
        assertEquals(UserDecision.ACCEPT, decision.decision)
        assertNull(decision.selectedTier, "接受识别结果时不覆盖 detectedTier")
    }

    /** 没有识别档位时必须先在弹窗选择；未来的行确认即标记本次未复习。 */
    @Test
    fun confirm_requests_a_choice_when_detection_is_missing_and_marks_future_rows_not_reviewed() {
        val machine = ConfirmStateMachine(
            rows = listOf(
                row("q1", "k1", detectedTier = null),
                row("q2", "k2", plannedDate = today.plusDays(1), detectedTier = Tier.MASTERED),
            ),
            today = today,
        )
        assertEquals(MarkOutcome.NeedsTier, machine.confirm(key("q1", "k1")))

        machine.setPendingEffect(key("q1", "k1"), EffectChoice.Rated(Tier.MASTERED))
        assertEquals(MarkOutcome.Applied, machine.confirm(key("q1", "k1")))
        assertEquals(UserDecision.CORRECTED, machine.decisionOf(key("q1", "k1")).decision)

        assertEquals(MarkOutcome.Applied, machine.confirm(key("q2", "k2")))
        assertEquals(UserDecision.NOT_REVIEWED, machine.decisionOf(key("q2", "k2")).decision)
    }
}
