package com.recollect.feature.planning

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.recollect.common.UiError
import com.recollect.data.room.repository.PlanGenerationException
import kotlinx.coroutines.CancellationException
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * PlanPreview 状态（app.md「屏幕清单与状态」）。
 *
 * `Empty` 携带 overflow 提示：整份计划无可打印题目时，提示不能因为“空态”被丢掉（C-060、C-061/C-062）。
 * `Content.warnings` 只含中文标签与 message，不含分页内部代码（C-065 方案 B）。
 */
sealed interface PlanPreviewUiState {
    data object Loading : PlanPreviewUiState
    data class Empty(
        val message: String,
        val warnings: List<PlanPreviewWarning>,
    ) : PlanPreviewUiState

    data class Content(
        val planId: String,
        val pages: List<PlanPreviewPage>,
        val warnings: List<PlanPreviewWarning>,
    ) : PlanPreviewUiState

    data class Error(val error: UiError) : PlanPreviewUiState
}

/**
 * PlanPreview 状态持有者（手写 ViewModel，不引入 lifecycle-viewmodel 依赖）。
 *
 * 不持有 `Context`（R-03）；计划生成失败按 C-047 映射为可重试 [UiError]。
 */
class PlanPreviewViewModel(
    private val generator: PlanGenerator,
    private val clock: Clock,
    private val settingsFactory: (LocalDate) -> PlanningSettings = ::defaultPlanningSettings,
    /** 计划成功后把同一份 `PrintJob` 交给打印页；失败/空态时清空，避免打印旧计划。 */
    private val onPrintJobAvailable: (PrintJob?) -> Unit = {},
) {
    var state by mutableStateOf<PlanPreviewUiState>(PlanPreviewUiState.Loading)
        private set

    suspend fun refresh() {
        state = PlanPreviewUiState.Loading
        onPrintJobAvailable(null)
        val settings = settingsFactory(LocalDate.now(clock))
        state = try {
            when (val outcome = generator.execute(settings)) {
                is PlanGenerationOutcome.Planned -> {
                    onPrintJobAvailable(outcome.printJob)
                    PlanPreviewUiState.Content(
                        planId = outcome.preview.planId,
                        pages = outcome.preview.pages,
                        warnings = outcome.preview.warnings,
                    )
                }
                is PlanGenerationOutcome.Empty -> PlanPreviewUiState.Empty(
                    message = outcome.message,
                    warnings = outcome.warnings,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: PlanGenerationException) {
            PlanPreviewUiState.Error(
                UiError(
                    code = e.code.uiErrorCode(),
                    message = e.message ?: "生成计划失败，请重试。",
                    retryable = true,
                ),
            )
        } catch (e: Exception) {
            PlanPreviewUiState.Error(
                UiError(
                    code = "PLAN_UNKNOWN",
                    message = e.message ?: "生成计划失败，请重试。",
                    retryable = true,
                ),
            )
        }
    }
}

/** 默认计划区间：本周末后的下一个周一，共 7 天（planning.md）。 */
fun defaultPlanningSettings(today: LocalDate): PlanningSettings {
    val start = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
    return PlanningSettings(planStartDate = start, planEndDate = start.plusDays(6))
}
