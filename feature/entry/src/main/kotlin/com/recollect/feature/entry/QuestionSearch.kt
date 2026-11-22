package com.recollect.feature.entry

import com.recollect.data.room.repository.EntrySummary

/**
 * 题库搜索（2026-11-22 用户要求）。
 *
 * 与知识点页的搜索保持一致：大小写不敏感、空查询返回原列表。
 * 关键词可匹配题号、来源页码（`编号-说明`，说明为空时只有编号）或任一关联知识点名称。
 * 只过滤展示列表，不改变 Repository 的归档过滤与计划排序。
 */
internal fun filterQuestions(
    questions: List<EntrySummary>,
    rawQuery: String,
): List<EntrySummary> {
    val keyword = rawQuery.trim()
    if (keyword.isEmpty()) return questions
    return questions.filter { question ->
        question.questionNumber.contains(keyword, ignoreCase = true) ||
            question.sourcePage.contains(keyword, ignoreCase = true) ||
            question.knowledgeNames.any { name -> name.contains(keyword, ignoreCase = true) }
    }
}
