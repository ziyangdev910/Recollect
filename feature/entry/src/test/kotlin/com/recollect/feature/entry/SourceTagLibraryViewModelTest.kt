package com.recollect.feature.entry

import com.recollect.common.UiState
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 来源标签库 ViewModel：列表、新增、重命名、删除与错误提示（2026-11-22 用户要求）。
 */
class SourceTagLibraryViewModelTest {

    private fun fixture() = FakeSourceTagRepository(
        listOf(sourceTag("source-1", "作业本")),
    ).let { SourceTagLibraryViewModel(it) to it }

    @Test
    fun `refresh lists source tags and reports empty`() = runBlocking {
        val (viewModel, _) = fixture()
        viewModel.refresh()
        assertEquals(
            listOf("作业本"),
            (viewModel.state as UiState.Content).data.tags.map { it.name },
        )

        val (empty, _) = FakeSourceTagRepository().let { SourceTagLibraryViewModel(it) to it }
        empty.refresh()
        assertEquals(UiState.Empty, empty.state)
    }

    @Test
    fun `add rename and delete refresh the list`() = runBlocking {
        val (viewModel, repository) = fixture()
        viewModel.refresh()

        assertTrue(viewModel.add(" 同步练习 "))
        assertEquals(listOf("作业本", "同步练习"), repository.stored.map { it.name })

        val id = repository.stored.first { it.name == "同步练习" }.id
        assertTrue(viewModel.rename(id, "练习册"))
        assertTrue(repository.stored.any { it.name == "练习册" })

        assertTrue(viewModel.delete(id))
        assertEquals(listOf("作业本"), repository.stored.map { it.name })
    }

    @Test
    fun `failed operation keeps the list and exposes a message`() = runBlocking {
        val failing = object : com.recollect.data.room.repository.SourceTagLibraryRepository {
            override suspend fun tags() = emptyList<com.recollect.data.room.repository.SourceTagItem>()
            override suspend fun create(name: String) = error("来源标签「$name」已存在。")
            override suspend fun rename(sourceTagId: String, name: String) = Unit
            override suspend fun delete(sourceTagId: String) = Unit
        }
        val viewModel = SourceTagLibraryViewModel(failing)
        viewModel.refresh()

        assertFalse(viewModel.add("作业本"))
        assertEquals("来源标签「作业本」已存在。", viewModel.actionError)
        assertEquals(UiState.Empty, viewModel.state)
    }
}
