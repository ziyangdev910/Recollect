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
