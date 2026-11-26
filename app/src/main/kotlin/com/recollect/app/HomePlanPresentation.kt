package com.recollect.app

import com.recollect.data.room.repository.PlanStatus

/** 复习闭环的五个环节；首页用当前计划状态点亮“下一步”。 */
internal enum class ReviewStage(val label: String) {
    RECORD("录入"),
    PLAN("生成"),
    PRINT("打印"),
    SCAN("扫描"),
    CONFIRM("确认"),
}

/**
 * 由首页现有数据推断用户此刻所处的环节：
 * 没有题目先录入；有题目但本周无计划先生成；已生成去打印；已打印去回读；
 * 计划完成后本轮结束，提示开始下一轮。
 */
internal fun currentReviewStage(questionCount: Int?, status: PlanStatus?): ReviewStage = when {
    questionCount == 0 -> ReviewStage.RECORD
    status == null -> if (questionCount == null) ReviewStage.RECORD else ReviewStage.PLAN
    status == PlanStatus.GENERATED -> ReviewStage.PRINT
    status == PlanStatus.PRINTED -> ReviewStage.SCAN
    status == PlanStatus.COMPLETED -> ReviewStage.CONFIRM
    else -> ReviewStage.PLAN
}

/** 首页常用操作里被前置并高亮的那张卡片。 */
internal enum class HomeHighlight { RECORD, PLAN, SCAN }

/**
 * 把当前环节映射到首页要前置的卡片：
 * 录入 → 录入题目；生成/打印 → 预览计划；扫描/确认 → 扫描回读。
 */
internal fun homeHighlight(stage: ReviewStage): HomeHighlight = when (stage) {
    ReviewStage.RECORD -> HomeHighlight.RECORD
    ReviewStage.PLAN, ReviewStage.PRINT -> HomeHighlight.PLAN
    ReviewStage.SCAN, ReviewStage.CONFIRM -> HomeHighlight.SCAN
}

/** 流程图下方的操作指引：说明这一步在做什么、下一步点哪里。 */
internal fun stageGuidance(stage: ReviewStage, cycleComplete: Boolean): String =
    if (cycleComplete) {
        "本轮已完成。可以查看复习进度，或继续录入新题开始下一轮。"
    } else {
        when (stage) {
            ReviewStage.RECORD -> "先录入至少一道题；攒够题目后，就能生成可打印的复习计划。"
            ReviewStage.PLAN -> "题目已就绪。点击「预览计划」，按知识点到期情况查看安排。"
            ReviewStage.PRINT -> "计划已就绪。在计划预览中选择本周或下周打印，然后按日期完成纸面复习。"
            ReviewStage.SCAN -> "计划已打印。完成本周复习后，用「扫描回读」拍照录回纸面填涂。"
            ReviewStage.CONFIRM -> "从「扫描回读」页进入「确认」页，逐行核对识别结果并提交。"
        }
    }

/**
 * 常用操作里的入口。枚举顺序就是未被引导时的固定顺序；
 * 被引导的入口会被移到首位，其余顺序不变。
 */
internal enum class HomeAction(val title: String) {
    RECORD("录入题目"),
    LIBRARY("管理题库"),
    PLAN("预览计划"),
    SCAN("扫描回读"),
}

/** 当前环节对应的操作入口。 */
internal fun guidedHomeAction(highlight: HomeHighlight): HomeAction = when (highlight) {
    HomeHighlight.RECORD -> HomeAction.RECORD
    HomeHighlight.PLAN -> HomeAction.PLAN
    HomeHighlight.SCAN -> HomeAction.SCAN
}

/** 首位是被引导的入口，后面按固定顺序排列其余入口。 */
internal fun orderedHomeActions(highlight: HomeHighlight): List<HomeAction> {
    val guided = guidedHomeAction(highlight)
    return listOf(guided) + HomeAction.entries.filter { it != guided }
}
