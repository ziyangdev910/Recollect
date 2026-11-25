package com.recollect.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 题目标识 `来源 第X页 第Y题`（2026-11-25 用户要求）。
 *
 * X 为页码编号、Y 为题号；没有来源说明时页码显示为「编号X」，旧稿没有页码编号时整串作为来源。
 */
class QuestionLabelTest {

    @Test
    fun `formats source page and number as 来源 第X页 第Y题`() {
        assertEquals("同步练习 第12页 第T-1题", formatQuestionLabel("12-同步练习", "T-1"))
    }

    @Test
    fun `uses 编号X when the question has no source label`() {
        assertEquals("编号12 第T-1题", formatQuestionLabel("12", "T-1"))
    }

    @Test
    fun `keeps legacy free text as the source when it has no page code`() {
        assertEquals("P12 第T-1题", formatQuestionLabel("P12", "T-1"))
    }

    @Test
    fun `falls back to the question number alone when the source is missing`() {
        assertEquals("第T-1题", formatQuestionLabel("", "T-1"))
        assertEquals("第T-1题", formatQuestionLabel("   ", "T-1"))
    }

    @Test
    fun `keeps unknown question placeholder readable`() {
        assertEquals(UNKNOWN_QUESTION_NUMBER, formatQuestionLabel("12-同步练习", UNKNOWN_QUESTION_NUMBER))
    }

    @Test
    fun `keeps dashes inside the source label`() {
        assertEquals("同步-练习 第12页 第T-1题", formatQuestionLabel("12-同步-练习", "T-1"))
    }

    @Test
    fun `splits the source and the page plus number into two lines`() {
        assertEquals(
            QuestionLabelLines(first = "同步练习", second = "第12页 第T-1题"),
            formatQuestionLabelLines("12-同步练习", "T-1"),
        )
        assertEquals(
            QuestionLabelLines(first = "编号12", second = "第T-1题"),
            formatQuestionLabelLines("12", "T-1"),
        )
        assertEquals(
            QuestionLabelLines(first = "第T-1题", second = ""),
            formatQuestionLabelLines("", "T-1"),
        )
    }
}
