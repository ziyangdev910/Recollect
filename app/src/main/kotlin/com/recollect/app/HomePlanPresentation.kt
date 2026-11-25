package com.recollect.app

import com.recollect.data.room.repository.PlanStatus
import com.recollect.feature.planning.PlanWeek

/**
 * 首页计划卡的纯展示规则（C-100）。
 *
 * 规则只有一条：本周计划完成（COMPLETED）后，把下周计划升为卡片主区块、本周降为次要行；
 * 其余状态保持本周为主。判断只看计划状态，不看日期——周末若本周仍有未回读的页，
 * 首页应继续把本周放在主位提醒补回读；周一自然翻周后，原“下周”变成新的“本周”，
 * focus 会随新窗口的状态重新计算。
 */
internal fun homePlanFocus(thisWeekStatus: PlanStatus?): PlanWeek =
    if (thisWeekStatus == PlanStatus.COMPLETED) PlanWeek.NEXT else PlanWeek.CURRENT

/** 复习闭环的五个环节；首页用当前计划状态点亮“下一步”。 */
internal enum class ReviewStage(val label: String, val actionLabel: String) {
    RECORD("录入", "录入题目"),
    PLAN("生成", "生成计划"),
    PRINT("打印", "打印计划"),
    SCAN("扫描", "扫描回读"),
    CONFIRM("确认", "查看复习进度"),
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

/** 首页上被“下一步”点亮的那张卡片。 */
internal enum class HomeHighlight { RECORD, PLAN, SCAN }

/**
 * 把当前环节映射到首页要突出的卡片。
 *
 * 录入与扫描对应 2×2 操作卡；生成、打印，以及计划完成后（主卡已聚焦下一周）
 * 都对应计划主卡，确保任意时刻只有一张卡片承担引导。
 */
internal fun homeHighlight(stage: ReviewStage): HomeHighlight = when (stage) {
    ReviewStage.RECORD -> HomeHighlight.RECORD
    ReviewStage.PLAN, ReviewStage.PRINT, ReviewStage.CONFIRM -> HomeHighlight.PLAN
    ReviewStage.SCAN -> HomeHighlight.SCAN
}

/** 主/次区块与按钮文案里使用的周标题。 */
internal val PlanWeek.title: String
    get() = when (this) {
        PlanWeek.CURRENT -> "本周计划"
        PlanWeek.NEXT -> "下周计划"
    }

/** 主区块按钮：未生成时是“生成”，已有计划时是“预览并打印”。 */
internal fun planPrimaryActionLabel(week: PlanWeek, status: PlanStatus?): String =
    if (status == null) "生成${week.title}" else "预览并打印${week.title}"

/** 次区块是紧凑文本入口，已有计划时只说“预览”。 */
internal fun planSecondaryActionLabel(week: PlanWeek, status: PlanStatus?): String =
    if (status == null) "生成${week.title}" else "预览${week.title}"

/**
 * 主区块的状态说明。
 *
 * 下周区块只有在升为主区块（本周已完成）时才会展示说明，因此
 * “下周未生成”的文案直接承接“本周已完成，提前准备下周”的语境。
 */
internal fun planStatusMessage(week: PlanWeek, status: PlanStatus?): String = when (status) {
    null -> if (week == PlanWeek.CURRENT) {
        "按本周到期的知识点生成计划，打印到纸上开始复习。"
    } else {
        "本周计划已完成，提前生成并打印下周计划。"
    }

    PlanStatus.GENERATED -> "${week.title}已生成，打印后按日期完成复习。"
    PlanStatus.PRINTED -> "${week.title}已打印。按日期完成复习，完成后拍照回读。"
    PlanStatus.COMPLETED -> if (week == PlanWeek.CURRENT) {
        "本周计划已完成，可以生成下周计划或查看复习进度。"
    } else {
        "下周计划已完成。"
    }

    PlanStatus.SUPERSEDED -> "${week.title}已被新的计划替代，请查看最新计划。"
}
