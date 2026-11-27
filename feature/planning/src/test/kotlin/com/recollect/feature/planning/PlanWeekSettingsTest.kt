package com.recollect.feature.planning

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * 首页两个计划窗口的默认值：本周必须是今天所在自然周，下周紧随其后。
 *
 * 旧实现用 `TemporalAdjusters.next(MONDAY)`，在周一当天也会跳到下周一，
 * 导致首页“本周计划”永远显示下周，且周一无法生成当周计划。
 */
class PlanWeekSettingsTest {

    private val monday = LocalDate.of(2026, 11, 23)

    @Test
    fun `monday current week starts on the same monday`() {
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
        val settings = currentWeekSettings(monday)
        assertEquals(LocalDate.of(2026, 11, 23), settings.planStartDate)
        assertEquals(LocalDate.of(2026, 11, 29), settings.planEndDate)
    }

    @Test
    fun `midweek current week still starts on monday`() {
        val wednesday = LocalDate.of(2026, 11, 25)
        assertEquals(DayOfWeek.WEDNESDAY, wednesday.dayOfWeek)
        val settings = currentWeekSettings(wednesday)
        assertEquals(LocalDate.of(2026, 11, 23), settings.planStartDate)
        assertEquals(LocalDate.of(2026, 11, 29), settings.planEndDate)
    }

    @Test
    fun `sunday stays in the week that is ending`() {
        val sunday = LocalDate.of(2026, 11, 22)
        assertEquals(DayOfWeek.SUNDAY, sunday.dayOfWeek)
        val settings = currentWeekSettings(sunday)
        assertEquals(LocalDate.of(2026, 11, 16), settings.planStartDate)
        assertEquals(LocalDate.of(2026, 11, 22), settings.planEndDate)
    }

    @Test
    fun `next week always starts the day after the current week ends`() {
        listOf(monday, LocalDate.of(2026, 11, 22), LocalDate.of(2026, 11, 25)).forEach { today ->
            val current = currentWeekSettings(today)
            val next = nextWeekSettings(today)
            assertEquals(current.planEndDate.plusDays(1), next.planStartDate, "today=$today")
            assertEquals(next.planStartDate.plusDays(6), next.planEndDate, "today=$today")
        }
    }

    @Test
    fun `week helpers still provide the default date range`() {
        val sunday = LocalDate.of(2026, 11, 22)
        assertEquals(7, currentWeekSettings(sunday).let { it.planEndDate.toEpochDay() - it.planStartDate.toEpochDay() + 1 })
        assertEquals(7, nextWeekSettings(sunday).let { it.planEndDate.toEpochDay() - it.planStartDate.toEpochDay() + 1 })
        // 2026-11-27 用户要求：日期范围可选，但最多 7 天，保持周计划的容量语义。
        assertEquals(7L, MAX_PLAN_DAYS)
        assertEquals("重新生成计划", REGENERATE_PLAN_LABEL)
    }
}
