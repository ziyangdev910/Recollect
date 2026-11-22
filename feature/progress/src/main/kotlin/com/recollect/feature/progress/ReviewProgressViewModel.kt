package com.recollect.feature.progress

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.recollect.common.UiError
import com.recollect.common.UiState
import com.recollect.data.room.repository.ReviewProgressRepository
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.CancellationException

/**
 * 复习进度页状态持有者（手写 ViewModel，不引入 lifecycle-viewmodel 依赖）。
 *
 * 只读：从 [ReviewProgressRepository] 拉取未归档题目的投影，并用 [clock] 计算“今天”。
 * 不持有 `Context`，不访问调度器或打印层（R-03）。
 */
class ReviewProgressViewModel(
    private val repository: ReviewProgressRepository,
    private val clock: Clock,
) {
    var state by mutableStateOf<UiState<ReviewProgressContent>>(UiState.Loading)
        private set

    suspend fun refresh() {
        state = try {
            val questions = repository.questions()
            if (questions.isEmpty()) {
                UiState.Empty
            } else {
                UiState.Content(
                    ReviewProgressAggregator.build(questions, LocalDate.now(clock)),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UiError(
                code = "PROGRESS_LOAD_FAILED",
                message = e.message ?: "读取复习进度失败，请重试。",
                retryable = true,
            ).toUiState()
        }
    }
}
