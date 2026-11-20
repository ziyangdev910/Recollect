package com.recollect.feature.entry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.recollect.common.UiError
import com.recollect.common.UiState
import com.recollect.data.room.repository.KnowledgePointLibraryRepository
import com.recollect.data.room.repository.KnowledgePointTag
import kotlinx.coroutines.CancellationException

/** 标签库内容；按编号升序。 */
data class KnowledgePointLibraryContent(
    val tags: List<KnowledgePointTag>,
)

/**
 * 知识点标签库状态持有者：增删改查（D-12、app.md「KnowledgePoints」）。
 *
 * 删除由 Repository 在事务内再次校验「仍被未归档题目引用则拒绝」；
 * 这里的 [actionError] 只负责把异常转成用户可读提示，不改变列表内容。
 */
class KnowledgePointLibraryViewModel(
    private val repository: KnowledgePointLibraryRepository,
) {
    var state by mutableStateOf<UiState<KnowledgePointLibraryContent>>(UiState.Loading)
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
                UiState.Content(KnowledgePointLibraryContent(tags))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UiError("KNOWLEDGE_LOAD_FAILED", e.message ?: "读取标签库失败，请重试。", retryable = true)
                .toUiState()
        }
    }

    suspend fun add(name: String): Boolean = mutate {
        repository.create(name)
    }

    suspend fun rename(knowledgePointId: String, name: String): Boolean = mutate {
        repository.rename(knowledgePointId, name)
    }

    suspend fun delete(knowledgePointId: String): Boolean = mutate {
        repository.deleteWithTombstone(knowledgePointId)
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
