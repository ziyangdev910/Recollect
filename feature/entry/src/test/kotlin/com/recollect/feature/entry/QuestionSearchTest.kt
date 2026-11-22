package com.recollect.feature.entry

import com.recollect.data.room.repository.EntrySummary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 题库搜索（2026-11-22 用户要求）：题号、来源页码、知识点名称均可作为关键词。
 */
class QuestionSearchTest {

    private val questions = listOf(
        EntrySummary(
            questionId = "q1",
            questionNumber = "T-1",
            sourcePageCode = 2,
            sourcePageLabel = "作业本",
            knowledgeNames = listOf("一元二次方程"),
            knowledgePointIds = listOf("kp-1"),
        ),
        EntrySummary(
            questionId = "q2",
            questionNumber = "T-2",
            sourcePageCode = 3,
            sourcePageLabel = "",
            knowledgeNames = listOf("勾股定理", "相似三角形"),
            knowledgePointIds = listOf("kp-2", "kp-3"),
        ),
        EntrySummary(
            questionId = "q3",
            questionNumber = "W-10",
            sourcePageCode = 12,
            sourcePageLabel = "同步练习",
            knowledgeNames = listOf("二次函数"),
            knowledgePointIds = listOf("kp-4"),
        ),
    )

    private fun search(query: String): List<String> =
        filterQuestions(questions, query).map { it.questionId }

    @Test
    fun `blank query keeps the full list`() {
        assertEquals(listOf("q1", "q2", "q3"), search(""))
        assertEquals(listOf("q1", "q2", "q3"), search("   "))
    }

    @Test
    fun `matches question number case-insensitively`() {
        assertEquals(listOf("q1"), search("t-1"))
        assertEquals(listOf("q1", "q2"), search("T-"))
        assertEquals(listOf("q3"), search("w-10"))
    }

    @Test
    fun `matches source page code or label`() {
        assertEquals(listOf("q2"), search("3"))
        assertEquals(listOf("q1"), search("2-作业本"))
        assertEquals(listOf("q1"), search("作业"))
        assertEquals(listOf("q3"), search("同步"))
    }

    @Test
    fun `matches any linked knowledge point name`() {
        assertEquals(listOf("q1"), search("一元二次"))
        assertEquals(listOf("q2"), search("相似"))
        assertEquals(listOf("q2"), search("勾股定理"))
    }

    @Test
    fun `trims the query and returns empty when nothing matches`() {
        assertEquals(listOf("q2"), search("  勾股  "))
        assertTrue(search("不存在的关键词").isEmpty())
    }
}
