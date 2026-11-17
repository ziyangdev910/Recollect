package com.recollect.data.room.repository

import java.time.DayOfWeek

/**
 * 计划周起始日的只读端口（C-081 方案 A）。
 *
 * 设置页允许用户修改计划周起始日（默认周一，下一份计划生效，见 settings.md）。
 * 确认提交时按“计划日所在周 + 该起始日”计算下界，不硬编码周一。
 */
fun interface PlanWeekStartProvider {
    fun planStartDay(): DayOfWeek
}

/** 设置尚未接入前的默认实现：计划周从周一开始（settings.md 默认值）。 */
class MondayPlanWeekStartProvider : PlanWeekStartProvider {
    override fun planStartDay(): DayOfWeek = DayOfWeek.MONDAY
}
