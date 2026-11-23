package com.recollect.feature.entry

import com.recollect.common.UiState
import com.recollect.data.room.repository.KnowledgeCategoryInUseException
import com.recollect.data.room.repository.KnowledgeCategoryItem
import com.recollect.data.room.repository.KnowledgeCategoryRepository
import com.recollect.data.room.repository.KnowledgePointInUseException
import com.recollect.data.room.repository.KnowledgePointLibraryRepository
import com.recollect.data.room.repository.KnowledgePointTag
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 标签库与分类增删改查（D-12、app.md「KnowledgePoints」；2026-11-23 用户要求）。
 */
class KnowledgePointLibraryViewModelTest {

    private class FakeLibraryRepository(
        tags: List<KnowledgePointTag> = emptyList(),
    ) : KnowledgePointLibraryRepository {
        private val stored = tags.toMutableList()
        var nextNumber = (tags.maxOfOrNull { it.number } ?: 0) + 1

        override suspend fun tags(): List<KnowledgePointTag> = stored.sortedBy { it.number }

        override suspend fun create(name: String, categoryId: String): KnowledgePointTag {
            val tag = KnowledgePointTag(
                id = "kp-$nextNumber",
                number = nextNumber++,
                name = name.trim(),
                categoryId = categoryId,
                linkedQuestionCount = 0,
            )
            stored += tag
            return tag
        }

        override suspend fun update(knowledgePointId: String, name: String, categoryId: String) {
            val index = stored.indexOfFirst { it.id == knowledgePointId }
            stored[index] = stored[index].copy(name = name.trim(), categoryId = categoryId)
        }

        override suspend fun deleteWithTombstone(knowledgePointId: String, reason: String) {
            val tag = stored.first { it.id == knowledgePointId }
            if (tag.linkedQuestionCount > 0) {
                throw KnowledgePointInUseException("该知识点仍被题目引用，不能删除。请先解除题目的关联。")
            }
            stored.remove(tag)
        }
    }

    private class FakeCategoryRepository(
        categories: List<KnowledgeCategoryItem> = emptyList(),
    ) : KnowledgeCategoryRepository {
        val stored = categories.toMutableList()

        override suspend fun categories(): List<KnowledgeCategoryItem> = stored.toList()

        override suspend fun create(name: String): KnowledgeCategoryItem {
            val item = KnowledgeCategoryItem(
                id = "cat-${stored.size + 1}",
                name = name.trim(),
                linkedKnowledgePointCount = 0,
            )
            stored += item
            return item
        }

        override suspend fun rename(categoryId: String, name: String) {
            val index = stored.indexOfFirst { it.id == categoryId }
            stored[index] = stored[index].copy(name = name.trim())
        }

        override suspend fun delete(categoryId: String) {
            val item = stored.first { it.id == categoryId }
            if (item.linkedKnowledgePointCount > 0) {
                throw KnowledgeCategoryInUseException("该分类下仍有知识点，不能删除。")
            }
            stored.remove(item)
        }
    }

    private val chinese = KnowledgeCategoryItem("cat-chinese", "语文", 0)

    private fun viewModel(
        library: FakeLibraryRepository = FakeLibraryRepository(),
        category: FakeCategoryRepository = FakeCategoryRepository(listOf(chinese)),
    ) = KnowledgePointLibraryViewModel(library, category)

    @Test
    fun `empty library shows empty state`() = runBlocking {
        val viewModel = viewModel(category = FakeCategoryRepository())

        viewModel.refresh()

        assertEquals(UiState.Empty, viewModel.state)
    }

    @Test
    fun `add and update keep the category`() = runBlocking {
        val viewModel = viewModel()

        assertTrue(viewModel.add("分数", chinese.id))
        run {
            val content = viewModel.state as UiState.Content
            assertEquals(listOf("分数"), content.data.tags.map { it.name })
            assertEquals(chinese.id, content.data.tags.single().categoryId)
        }

        val id = (viewModel.state as UiState.Content).data.tags.single().id
        assertTrue(viewModel.update(id, "分式", chinese.id))

        val content = viewModel.state as UiState.Content
        assertEquals(listOf("分式"), content.data.tags.map { it.name })
        assertEquals(1, content.data.tags.single().number)
        assertNull(viewModel.actionError)
    }

    @Test
    fun `delete removes an unreferenced tag`() = runBlocking {
        val viewModel = viewModel(
            library = FakeLibraryRepository(
                listOf(
                    KnowledgePointTag(
                        id = "kp-1",
                        number = 1,
                        name = "分数",
                        categoryId = chinese.id,
                        linkedQuestionCount = 0,
                    ),
                ),
            ),
        )

        viewModel.refresh()

        assertTrue(viewModel.delete("kp-1"))

        val content = viewModel.state as UiState.Content
        assertTrue(content.data.tags.isEmpty(), "分类仍在，页面保持内容态")
    }

    @Test
    fun `delete keeps a referenced tag and surfaces the reason`() = runBlocking {
        val viewModel = viewModel(
            library = FakeLibraryRepository(
                listOf(
                    KnowledgePointTag(
                        id = "kp-1",
                        number = 1,
                        name = "分数",
                        categoryId = chinese.id,
                        linkedQuestionCount = 2,
                    ),
                ),
            ),
        )

        viewModel.refresh()

        assertFalse(viewModel.delete("kp-1"))

        val content = viewModel.state as UiState.Content
        assertEquals(1, content.data.tags.size)
        assertTrue(viewModel.actionError.orEmpty().contains("仍被题目引用"))
    }

    @Test
    fun `categories can be added renamed and deleted`() = runBlocking {
        val viewModel = viewModel()

        assertTrue(viewModel.addCategory("数学"))
        val math = (viewModel.state as UiState.Content).data.categories.first { it.name == "数学" }

        assertTrue(viewModel.renameCategory(math.id, "高数"))
        assertTrue(
            (viewModel.state as UiState.Content).data.categories.any { it.name == "高数" },
        )

        assertTrue(viewModel.deleteCategory(math.id))
        assertFalse(
            (viewModel.state as UiState.Content).data.categories.any { it.id == math.id },
        )
    }
}
