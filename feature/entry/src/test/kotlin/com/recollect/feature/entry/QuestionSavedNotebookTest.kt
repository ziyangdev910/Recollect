package com.recollect.feature.entry

import com.recollect.data.room.repository.KnowledgeCategoryItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * 保存后进入哪个错题本：优先显式科目，否则取首个关联知识点（按编号升序）所属分类。
 */
class QuestionSavedNotebookTest {

    private val math = KnowledgeCategoryItem(
        id = "cat-math",
        name = "数学",
        linkedKnowledgePointCount = 0,
    )
    private val chinese = KnowledgeCategoryItem(
        id = "cat-chinese",
        name = "语文",
        linkedKnowledgePointCount = 0,
    )
    private val categories = listOf(chinese, math)
    private val tags = listOf(
        tag("kp-1", 1, "分数", categoryId = math.id),
        tag("kp-2", 2, "几何", categoryId = math.id),
        tag("kp-3", 3, "文言文", categoryId = chinese.id),
    )

    @Test
    fun `explicit subject wins over knowledge point categories`() {
        val result = resolveSavedNotebook(
            selectedSubjectId = chinese.id,
            selectedKnowledgePointIds = listOf("kp-1"),
            knowledgeTags = tags,
            knowledgeCategories = categories,
        )
        assertEquals(QuestionSavedNotebook(chinese.id, "语文"), result)
    }

    @Test
    fun `falls back to the first knowledge point category by number`() {
        val result = resolveSavedNotebook(
            selectedSubjectId = null,
            selectedKnowledgePointIds = listOf("kp-3", "kp-1"),
            knowledgeTags = tags,
            knowledgeCategories = categories,
        )
        assertEquals(QuestionSavedNotebook(math.id, "数学"), result)
    }

    @Test
    fun `returns null when no category can be resolved`() {
        assertNull(
            resolveSavedNotebook(
                selectedSubjectId = null,
                selectedKnowledgePointIds = emptyList(),
                knowledgeTags = tags,
                knowledgeCategories = categories,
            ),
        )
    }
}
