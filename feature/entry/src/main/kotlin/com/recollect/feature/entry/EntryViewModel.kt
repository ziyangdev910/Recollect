package com.recollect.feature.entry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.recollect.common.UiError
import com.recollect.common.UiState
import com.recollect.data.room.repository.EntryRepository
import com.recollect.data.room.repository.EntrySummary
import kotlinx.coroutines.CancellationException

/** Entry 列表内容；知识点以标签库投影展示。 */
data class EntryContent(
    val questions: List<EntrySummary>,
)

/**
 * Entry 列表状态持有者（手写 ViewModel，不引入 lifecycle-viewmodel 依赖）。
 *
 * 新建/编辑在独立的 [QuestionEditViewModel] 中完成；本页只负责列表与删除（归档）。
 * 不持有 `Context`（R-03）。
 */
class EntryViewModel(
    private val repository: EntryRepository,
) {
    var state by mutableStateOf<UiState<EntryContent>>(UiState.Loading)
        private set

    /** 删除失败的用户提示；不替换列表内容，便于确认框内直接展示。 */
    var actionError by mutableStateOf<String?>(null)
        private set

    fun clearActionError() {
        actionError = null
    }

    suspend fun refresh() {
        state = try {
            val questions = repository.summaries()
            if (questions.isEmpty()) {
                UiState.Empty
            } else {
                UiState.Content(EntryContent(questions))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UiError("ENTRY_LOAD_FAILED", e.message ?: "读取题库失败，请重试。", retryable = true)
                .toUiState()
        }
    }

    /** 删除（归档）题目：退出调度并从题库列表隐藏，历史反馈保留（D-11）。 */
    suspend fun delete(questionId: String): Boolean {
        actionError = null
        return try {
            repository.archive(questionId)
            refresh()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            actionError = e.message ?: "删除失败，请重试。"
            false
        }
    }
}
