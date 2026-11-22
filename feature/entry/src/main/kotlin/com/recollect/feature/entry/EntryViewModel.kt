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

    /**
     * 题库搜索关键词（2026-11-22 用户要求）。
     *
     * 放在 ViewModel 中：进入编辑页再返回时页面分支重建，但 ViewModel 由 `AppRoot` 持有，
     * 搜索条件与列表位置一样不会被清空。
     */
    var query by mutableStateOf("")
        private set

    /** 新建/编辑保存后置位；返回题库页时据此静默更新列表，而不重置滚动位置。 */
    private var needsRefresh = false

    fun clearActionError() {
        actionError = null
    }

    fun updateQuery(value: String) {
        query = value
    }

    fun markNeedsRefresh() {
        needsRefresh = true
    }

    /**
     * 页面进入时调用：首次进入（[state] 仍为 Loading）或保存过题目时才读取列表。
     *
     * 从编辑页返回时复用同一实例与列表状态，不重新加载、不回到顶部；
     * 保存后只做一次静默刷新（保留当前 Content，不在加载中闪回 Loading）。
     */
    suspend fun refreshIfNeeded() {
        if (state !is UiState.Loading && !needsRefresh) return
        needsRefresh = false
        refresh()
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
