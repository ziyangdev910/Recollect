package com.recollect.feature.entry

import com.recollect.common.UiState
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 录入/编辑独立页面：加载回填、保存覆盖与表单校验。
 */
class QuestionEditViewModelTest {

    private val tags = listOf(
        tag("kp-1", 1, "分数"),
        tag("kp-2", 2, "几何"),
    )

    private val sourceTags = listOf(
        sourceTag("source-1", "作业本"),
        sourceTag("source-2", "同步练习"),
    )

    private fun fixture(
        rows: List<com.recollect.data.room.repository.EntrySummary> = emptyList(),
        defaultCode: Int = 4,
        defaultWithSourceCode: Int = 9,
    ): Pair<QuestionEditViewModel, FakeEntryRepository> {
        val repository = FakeEntryRepository(rows).apply {
            defaultSourcePageCodeWithoutSource = defaultCode
            defaultSourcePageCodeWithSource = defaultWithSourceCode
        }
        return QuestionEditViewModel(
            repository = repository,
            libraryRepository = FakeLibraryRepository(tags),
            sourceTagRepository = FakeSourceTagRepository(sourceTags),
        ) to repository
    }

    @Test
    fun `new question loads defaults`() = runBlocking {
        val (viewModel, _) = fixture(defaultCode = 4)

        viewModel.load(questionId = null)

        val content = (viewModel.state as UiState.Content).data
        assertEquals(null, content.questionId)
        assertEquals("", content.questionNumber)
        assertEquals(4, content.sourcePageCode)
        assertEquals(4, content.defaultSourcePageCodeWithoutSource)
        assertEquals(9, content.defaultSourcePageCodeWithSource)
        assertEquals("", content.sourcePageLabel)
        assertEquals(15, content.estimatedReviewMinutes)
        assertTrue(content.selectedKnowledgePointIds.isEmpty())
        assertEquals(tags, content.tags)
        assertEquals(sourceTags, content.sourceTags)
        assertFalse(content.printed)
    }

    @Test
    fun `edit loads the question and prefills the form`() = runBlocking {
        val (viewModel, _) = fixture(
            rows = listOf(
                entrySummary(
                    questionId = "q1",
                    questionNumber = "T-1",
                    sourcePageCode = 2,
                    sourcePageLabel = "作业本",
                    knowledgePointIds = listOf("kp-1"),
                    estimatedReviewMinutes = 25,
                    printed = true,
                ),
            ),
        )

        viewModel.load(questionId = "q1")

        val content = (viewModel.state as UiState.Content).data
        assertEquals("q1", content.questionId)
        assertEquals("T-1", content.questionNumber)
        assertEquals(2, content.sourcePageCode)
        assertEquals("作业本", content.sourcePageLabel)
        assertEquals(25, content.estimatedReviewMinutes)
        assertEquals(listOf("kp-1"), content.selectedKnowledgePointIds)
        assertEquals(sourceTags, content.sourceTags)
        assertTrue(content.printed)
    }

    @Test
    fun `saving a new question calls create`() = runBlocking {
        val (viewModel, repository) = fixture()
        viewModel.load(questionId = null)

        val saved = viewModel.save(
            questionNumber = "T-1",
            sourcePageCode = 1,
            sourcePageLabel = "作业本",
            knowledgePointIds = listOf("kp-1", "kp-2"),
            estimatedReviewMinutes = 20,
        )

        assertTrue(saved)
        assertEquals(1, repository.createCalls)
        assertEquals(0, repository.updateCalls)
        val row = repository.rows.single()
        assertEquals("T-1", row.questionNumber)
        assertEquals("1-作业本", row.sourcePage)
        assertEquals(20, row.estimatedReviewMinutes)
    }

    @Test
    fun `saving an existing question calls update`() = runBlocking {
        val (viewModel, repository) = fixture(
            rows = listOf(
                entrySummary(
                    questionId = "q1",
                    questionNumber = "T-1",
                    sourcePageCode = 2,
                    sourcePageLabel = "作业本",
                    knowledgePointIds = listOf("kp-1"),
                    estimatedReviewMinutes = 20,
                ),
            ),
        )
        viewModel.load(questionId = "q1")

        val saved = viewModel.save(
            questionNumber = "T-1b",
            sourcePageCode = 3,
            sourcePageLabel = "练习",
            knowledgePointIds = listOf("kp-2"),
            estimatedReviewMinutes = 30,
        )

        assertTrue(saved)
        assertEquals(0, repository.createCalls)
        assertEquals(1, repository.updateCalls)
        val row = repository.rows.single()
        assertEquals("T-1b", row.questionNumber)
        assertEquals("3-练习", row.sourcePage)
        assertEquals(listOf("kp-2"), row.knowledgePointIds)
        assertEquals(30, row.estimatedReviewMinutes)
    }

    @Test
    fun `blank question number is rejected before saving`() = runBlocking {
        val (viewModel, repository) = fixture()
        viewModel.load(questionId = null)

        val saved = viewModel.save("   ", 1, "", listOf("kp-1"), 15)

        assertFalse(saved)
        assertEquals("题号不能为空。", viewModel.actionError)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `empty tag selection is rejected before saving`() = runBlocking {
        val (viewModel, repository) = fixture()
        viewModel.load(questionId = null)

        val saved = viewModel.save("T-1", 1, "", emptyList(), 15)

        assertFalse(saved)
        assertTrue(viewModel.actionError.orEmpty().contains("至少选择"))
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `page code below one is rejected before saving`() = runBlocking {
        val (viewModel, repository) = fixture()
        viewModel.load(questionId = null)

        val saved = viewModel.save("T-1", 0, "", listOf("kp-1"), 15)

        assertFalse(saved)
        assertEquals("页码编号必须从 1 开始。", viewModel.actionError)
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun `missing question shows a load error`() = runBlocking {
        val (viewModel, _) = fixture()

        viewModel.load(questionId = "missing")

        val error = viewModel.state as? UiState.Error
            ?: error("预期错误态，实际为 ${viewModel.state}")
        assertEquals("题目不存在或已被删除。", error.message)
    }
}
