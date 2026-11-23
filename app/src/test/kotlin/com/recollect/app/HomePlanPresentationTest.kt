package com.recollect.app

import com.recollect.data.room.repository.PlanStatus
import com.recollect.feature.planning.PlanWeek
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * C-100：首页计划卡的主次切换规则。
 *
 * 规则只看本周状态：COMPLETED 时下周升为主区块，其余状态保持本周为主。
 * 这里锁定 focus、按钮文案与状态说明，避免后续改动把“下周升级”又退回成小按钮。
 */
class HomePlanPresentationTest {

    @Test
    fun `focus stays on current week until it is completed`() {
        assertEquals(PlanWeek.CURRENT, homePlanFocus(null))
        assertEquals(PlanWeek.CURRENT, homePlanFocus(PlanStatus.GENERATED))
        assertEquals(PlanWeek.CURRENT, homePlanFocus(PlanStatus.PRINTED))
        assertEquals(PlanWeek.CURRENT, homePlanFocus(PlanStatus.SUPERSEDED))
    }

    @Test
    fun `focus moves to next week once current week is completed`() {
        assertEquals(PlanWeek.NEXT, homePlanFocus(PlanStatus.COMPLETED))
    }

    @Test
    fun `week titles match the two windows`() {
        assertEquals("本周计划", PlanWeek.CURRENT.title)
        assertEquals("下周计划", PlanWeek.NEXT.title)
    }

    @Test
    fun `primary action label depends on whether a plan exists`() {
        assertEquals("生成本周计划", planPrimaryActionLabel(PlanWeek.CURRENT, null))
        assertEquals(
            "预览并打印本周计划",
            planPrimaryActionLabel(PlanWeek.CURRENT, PlanStatus.PRINTED),
        )
        assertEquals("生成下周计划", planPrimaryActionLabel(PlanWeek.NEXT, null))
        assertEquals(
            "预览并打印下周计划",
            planPrimaryActionLabel(PlanWeek.NEXT, PlanStatus.GENERATED),
        )
    }

    @Test
    fun `secondary action label is a compact preview entry`() {
        assertEquals("生成本周计划", planSecondaryActionLabel(PlanWeek.CURRENT, null))
        assertEquals("预览本周计划", planSecondaryActionLabel(PlanWeek.CURRENT, PlanStatus.COMPLETED))
        assertEquals("生成下周计划", planSecondaryActionLabel(PlanWeek.NEXT, null))
        assertEquals("预览下周计划", planSecondaryActionLabel(PlanWeek.NEXT, PlanStatus.PRINTED))
    }

    @Test
    fun `current week messages still describe the week being reviewed`() {
        assertEquals(
            "按本周到期的知识点生成计划，打印到纸上开始复习。",
            planStatusMessage(PlanWeek.CURRENT, null),
        )
        assertEquals(
            "本周计划已生成，打印后按日期完成复习。",
            planStatusMessage(PlanWeek.CURRENT, PlanStatus.GENERATED),
        )
        assertEquals(
            "本周计划已打印。按日期完成复习，完成后拍照回读。",
            planStatusMessage(PlanWeek.CURRENT, PlanStatus.PRINTED),
        )
        assertEquals(
            "本周计划已完成，可以生成下周计划或查看复习进度。",
            planStatusMessage(PlanWeek.CURRENT, PlanStatus.COMPLETED),
        )
        assertEquals(
            "本周计划已被新的计划替代，请查看最新计划。",
            planStatusMessage(PlanWeek.CURRENT, PlanStatus.SUPERSEDED),
        )
    }

    @Test
    fun `next week messages explain why it became the focus`() {
        // 下周区块只有在升为主区块时才会展示说明，因此承接“本周已完成”的语境。
        assertEquals(
            "本周计划已完成，提前生成并打印下周计划。",
            planStatusMessage(PlanWeek.NEXT, null),
        )
        assertEquals(
            "下周计划已生成，打印后按日期完成复习。",
            planStatusMessage(PlanWeek.NEXT, PlanStatus.GENERATED),
        )
        assertEquals(
            "下周计划已打印。按日期完成复习，完成后拍照回读。",
            planStatusMessage(PlanWeek.NEXT, PlanStatus.PRINTED),
        )
        assertEquals(
            "下周计划已完成。",
            planStatusMessage(PlanWeek.NEXT, PlanStatus.COMPLETED),
        )
        assertEquals(
            "下周计划已被新的计划替代，请查看最新计划。",
            planStatusMessage(PlanWeek.NEXT, PlanStatus.SUPERSEDED),
        )
    }
}
