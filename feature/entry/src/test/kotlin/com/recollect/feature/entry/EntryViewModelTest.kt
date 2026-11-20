package com.recollect.feature.entry

import com.recollect.common.UiState
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Entry 列表页：读取题库与删除（归档）。新建/编辑由 [QuestionEditViewModelTest] 覆盖。
 */
class EntryViewModelTest {

    @Test
    fun `empty library shows the empty state`() = runBlocking {
        val viewModel = EntryViewModel(FakeEntryRepository())

        viewModel.refresh()

        assertEquals(UiState.Empty, viewModel.state)
    }

    @Test
    fun `refresh lists active questions`() = runBlocking {
        val viewModel = EntryViewModel(
            FakeEntryRepository(
                listOf(
                    entrySummary("q1", "T-1", sourcePageCode = 2, knowledgePointIds = listOf("kp-1")),
                    entrySummary("q2", "T-2"),
                ),
            ),
        )

        viewModel.refresh()

        val content = viewModel.state as UiState.Content
        assertEquals(listOf("T-1", "T-2"), content.data.questions.map { it.questionNumber })
        assertEquals("2", content.data.questions.first().sourcePage)
    }

    @Test
    fun `deleting removes the question from the list`() = runBlocking {
        val repository = FakeEntryRepository(listOf(entrySummary("q1", "T-1")))
        val viewModel = EntryViewModel(repository)
        viewModel.refresh()

        assertTrue(viewModel.delete("q1"))

        assertEquals(UiState.Empty, viewModel.state)
        assertEquals(1, repository.archiveCalls)
    }

    @Test
    fun `delete failure keeps the list and reports the reason`() = runBlocking {
        val repository = FakeEntryRepository(listOf(entrySummary("q1", "T-1")))
        repository.archiveFailure = IllegalStateException("删除失败。")
        val viewModel = EntryViewModel(repository)
        viewModel.refresh()

        assertFalse(viewModel.delete("q1"))

        assertEquals(1, (viewModel.state as UiState.Content).data.questions.size)
        assertTrue(viewModel.actionError.orEmpty().contains("删除失败"))
    }
}
