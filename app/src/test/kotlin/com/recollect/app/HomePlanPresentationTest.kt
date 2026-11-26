package com.recollect.app

import com.recollect.data.room.repository.PlanStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 首页流程引导的纯展示规则。
 *
 * 锁定环节推断、唯一高亮、常用操作排序与「生成计划」状态文案，
 * 避免后续改动把“下一步”入口又退回固定顺序。
 */
class HomePlanPresentationTest {

    @Test
    fun `stage follows the data available on home`() {
        assertEquals(ReviewStage.RECORD, currentReviewStage(0, null))
        assertEquals(ReviewStage.RECORD, currentReviewStage(null, null))
        assertEquals(ReviewStage.PLAN, currentReviewStage(12, null))
        assertEquals(ReviewStage.PRINT, currentReviewStage(12, PlanStatus.GENERATED))
        assertEquals(ReviewStage.SCAN, currentReviewStage(12, PlanStatus.PRINTED))
        assertEquals(ReviewStage.CONFIRM, currentReviewStage(12, PlanStatus.COMPLETED))
    }

    @Test
    fun `stage prefers the plan over a stale question count`() {
        // 题目统计读取失败（null）时，仍能凭计划状态判断该去打印或回读。
        assertEquals(ReviewStage.PRINT, currentReviewStage(null, PlanStatus.GENERATED))
        assertEquals(ReviewStage.SCAN, currentReviewStage(null, PlanStatus.PRINTED))
    }

    @Test
    fun `exactly one action is highlighted per stage`() {
        assertEquals(HomeHighlight.RECORD, homeHighlight(ReviewStage.RECORD))
        assertEquals(HomeHighlight.PLAN, homeHighlight(ReviewStage.PLAN))
        assertEquals(HomeHighlight.PLAN, homeHighlight(ReviewStage.PRINT))
        assertEquals(HomeHighlight.SCAN, homeHighlight(ReviewStage.SCAN))
        // 确认与扫描同属回读阶段：都前置「扫描回读」。
        assertEquals(HomeHighlight.SCAN, homeHighlight(ReviewStage.CONFIRM))
    }

    @Test
    fun `guided action moves to the front and the rest keep their order`() {
        assertEquals(
            listOf(
                HomeAction.RECORD,
                HomeAction.LIBRARY,
                HomeAction.PLAN,
                HomeAction.SCAN,
            ),
            orderedHomeActions(HomeHighlight.RECORD),
        )
        assertEquals(
            listOf(
                HomeAction.PLAN,
                HomeAction.RECORD,
                HomeAction.LIBRARY,
                HomeAction.SCAN,
            ),
            orderedHomeActions(HomeHighlight.PLAN),
        )
        assertEquals(
            listOf(
                HomeAction.SCAN,
                HomeAction.RECORD,
                HomeAction.LIBRARY,
                HomeAction.PLAN,
            ),
            orderedHomeActions(HomeHighlight.SCAN),
        )
        // 四个入口始终完整出现且不重复。
        HomeHighlight.entries.forEach { highlight ->
            val actions = orderedHomeActions(highlight)
            assertEquals(HomeAction.entries.size, actions.size)
            assertEquals(HomeAction.entries.toSet(), actions.toSet())
            assertEquals(guidedHomeAction(highlight), actions.first())
        }
    }

    @Test
    fun `plan action uses preview wording and progress is not a quick action`() {
        assertEquals("预览计划", HomeAction.PLAN.title)
        assertTrue(HomeAction.entries.none { it.title == "查看复习进度" })
    }

    @Test
    fun `guidance explains the current stage and the completed cycle`() {
        ReviewStage.entries.forEach { stage ->
            val text = stageGuidance(stage, cycleComplete = false)
            assertTrue(text.isNotBlank(), "$stage 必须有操作指引")
        }
        assertEquals(
            "本轮已完成。可以查看复习进度，或继续录入新题开始下一轮。",
            stageGuidance(ReviewStage.CONFIRM, cycleComplete = true),
        )
        // 完成态优先于环节本身，避免在已经收尾时仍提示“去确认”。
        assertEquals(
            stageGuidance(ReviewStage.RECORD, cycleComplete = true),
            stageGuidance(ReviewStage.SCAN, cycleComplete = true),
        )
    }
}
