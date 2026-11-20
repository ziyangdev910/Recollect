package com.recollect.feature.entry

import com.recollect.common.UiState
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
 * 标签库增删改查（D-12、app.md「KnowledgePoints」）。
 */
class KnowledgePointLibraryViewModelTest {

    private class FakeLibraryRepository(
        tags: List<KnowledgePointTag> = emptyList(),
    ) : KnowledgePointLibraryRepository {
        private val stored = tags.toMutableList()
        var nextNumber = (tags.maxOfOrNull { it.number } ?: 0) + 1

        override suspend fun tags(): List<KnowledgePointTag> = stored.sortedBy { it.number }

        override suspend fun create(name: String): KnowledgePointTag {
            val tag = KnowledgePointTag(
                id = "kp-$nextNumber",
                number = nextNumber++,
                name = name.trim(),
                linkedQuestionCount = 0,
            )
            stored += tag
            return tag
        }

        override suspend fun rename(knowledgePointId: String, name: String) {
            val index = stored.indexOfFirst { it.id == knowledgePointId }
            stored[index] = stored[index].copy(name = name.trim())
        }

        override suspend fun deleteWithTombstone(knowledgePointId: String, reason: String) {
            val tag = stored.first { it.id == knowledgePointId }
            if (tag.linkedQuestionCount > 0) {
                throw KnowledgePointInUseException("该知识点仍被题目引用，不能删除。请先解除题目的关联。")
            }
            stored.remove(tag)
        }
    }

    @Test
    fun `empty library shows empty state`() = runBlocking {
        val viewModel = KnowledgePointLibraryViewModel(FakeLibraryRepository())

        viewModel.refresh()

        assertEquals(UiState.Empty, viewModel.state)
    }

    @Test
    fun `add and rename update the list`() = runBlocking {
        val viewModel = KnowledgePointLibraryViewModel(FakeLibraryRepository())

        assertTrue(viewModel.add("分数"))
        run {
            val content = viewModel.state as UiState.Content
            assertEquals(listOf("分数"), content.data.tags.map { it.name })
        }

        val id = (viewModel.state as UiState.Content).data.tags.single().id
        assertTrue(viewModel.rename(id, "分式"))

        val content = viewModel.state as UiState.Content
        assertEquals(listOf("分式"), content.data.tags.map { it.name })
        assertEquals(1, content.data.tags.single().number)
        assertNull(viewModel.actionError)
    }

    @Test
    fun `delete removes an unreferenced tag`() = runBlocking {
        val viewModel = KnowledgePointLibraryViewModel(
            FakeLibraryRepository(
                listOf(KnowledgePointTag("kp-1", 1, "分数", linkedQuestionCount = 0)),
            ),
        )

        viewModel.refresh()

        assertTrue(viewModel.delete("kp-1"))

        assertEquals(UiState.Empty, viewModel.state)
    }

    @Test
    fun `delete keeps a referenced tag and surfaces the reason`() = runBlocking {
        val viewModel = KnowledgePointLibraryViewModel(
            FakeLibraryRepository(
                listOf(KnowledgePointTag("kp-1", 1, "分数", linkedQuestionCount = 2)),
            ),
        )

        viewModel.refresh()

        assertFalse(viewModel.delete("kp-1"))

        val content = viewModel.state as UiState.Content
        assertEquals(1, content.data.tags.size)
        assertTrue(viewModel.actionError.orEmpty().contains("仍被题目引用"))
    }
}
