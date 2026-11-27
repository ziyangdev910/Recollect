package com.recollect.feature.entry

import com.recollect.common.UiState
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
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
    fun `refreshIfNeeded keeps the list when returning from the edit screen`() = runBlocking {
        val repository = FakeEntryRepository(listOf(entrySummary("q1", "T-1")))
        val viewModel = EntryViewModel(repository)

        viewModel.refreshIfNeeded()
        assertEquals(1, repository.summaryCalls)
        val content = viewModel.state as UiState.Content

        // 进入编辑页再返回：页面重新组合，但不得重新读取列表或替换内容。
        viewModel.refreshIfNeeded()
        assertEquals(1, repository.summaryCalls, "返回题库页不应重新加载")
        assertEquals(content, viewModel.state, "返回题库页应保留原列表内容")

        // 保存题目后返回：静默更新一次，内容仍在（不闪回 Loading）。
        repository.rows += entrySummary("q2", "T-2")
        viewModel.markNeedsRefresh()
        viewModel.refreshIfNeeded()
        assertEquals(2, repository.summaryCalls, "保存后应静默刷新一次")
        assertEquals(2, (viewModel.state as UiState.Content).data.questions.size)
    }

    @Test
    fun `filters survive the edit screen and reset when switching notebooks`() {
        val viewModel = EntryViewModel(FakeEntryRepository())

        viewModel.selectNotebook("cat-math")
        viewModel.updateQuery("T-1")
        viewModel.updateSourceFilter("作业本")
        viewModel.updateKnowledgeFilter("函数")
        viewModel.updateGroupByKnowledge(true)
        viewModel.toggleSearchVisible()
        viewModel.toggleTimeOrder()

        // 进入编辑页再返回：错题本未变，搜索与筛选必须保留。
        viewModel.selectNotebook("cat-math")
        assertEquals("T-1", viewModel.query)
        assertEquals("作业本", viewModel.sourceFilter)
        assertEquals("函数", viewModel.knowledgeFilter)
        assertTrue(viewModel.groupByKnowledge)
        assertTrue(viewModel.searchVisible)
        assertFalse(viewModel.newestFirst)

        // 切换到另一个错题本：全部重置为默认视图。
        viewModel.selectNotebook("cat-english")
        assertEquals("", viewModel.query)
        assertNull(viewModel.sourceFilter)
        assertNull(viewModel.knowledgeFilter)
        assertFalse(viewModel.groupByKnowledge)
        assertFalse(viewModel.searchVisible)
        assertTrue(viewModel.newestFirst)
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
