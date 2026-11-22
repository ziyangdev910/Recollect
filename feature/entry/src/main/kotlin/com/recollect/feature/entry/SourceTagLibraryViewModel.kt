package com.recollect.feature.entry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.recollect.common.UiError
import com.recollect.common.UiState
import com.recollect.data.room.repository.SourceTagItem
import com.recollect.data.room.repository.SourceTagLibraryRepository
import kotlinx.coroutines.CancellationException

/** 来源标签库内容；按名称升序。 */
data class SourceTagLibraryContent(
    val tags: List<SourceTagItem>,
)

/**
 * 来源标签库状态持有者：增删改查（2026-11-22 用户要求）。
 *
 * 删除由 Repository 在事务内再次校验「仍被未归档题目引用则拒绝」；
 * 重命名会在同一事务内同步题目引用。这里的 [actionError] 只把异常转成用户可读提示。
 */
class SourceTagLibraryViewModel(
    private val repository: SourceTagLibraryRepository,
) {
    var state by mutableStateOf<UiState<SourceTagLibraryContent>>(UiState.Loading)
        private set

    /** 上一次写操作失败的用户提示；成功或重新操作时清空。 */
    var actionError by mutableStateOf<String?>(null)
        private set

    suspend fun refresh() {
        state = try {
            val tags = repository.tags()
            if (tags.isEmpty()) {
                UiState.Empty
            } else {
                UiState.Content(SourceTagLibraryContent(tags))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UiError("SOURCE_TAG_LOAD_FAILED", e.message ?: "读取来源标签失败，请重试。", retryable = true)
                .toUiState()
        }
    }

    suspend fun add(name: String): Boolean = mutate {
        repository.create(name)
    }

    suspend fun rename(sourceTagId: String, name: String): Boolean = mutate {
        repository.rename(sourceTagId, name)
    }

    suspend fun delete(sourceTagId: String): Boolean = mutate {
        repository.delete(sourceTagId)
    }

    fun clearActionError() {
        actionError = null
    }

    private suspend fun mutate(block: suspend () -> Unit): Boolean {
        actionError = null
        return try {
            block()
            refresh()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            actionError = e.message ?: "操作失败，请重试。"
            false
        }
    }
}
