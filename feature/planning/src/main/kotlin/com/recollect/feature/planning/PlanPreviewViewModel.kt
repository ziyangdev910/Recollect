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
    private val session: PlanSession,
    private val clock: Clock,
    private val settingsFactory: (LocalDate) -> PlanningSettings = ::defaultPlanningSettings,
    /** 计划成功后把同一份 `PrintJob` 交给打印页；失败/空态时清空，避免打印旧计划。 */
    private val onPrintJobAvailable: (PrintJob?) -> Unit = {},
) {

    /**
     * 仅生成、不读取已有计划的适配构造（供既有单测注入生成桩）。
     * 生产路径必须使用 [session] 构造，才能复用本周已有计划。
     */
    constructor(
        generator: PlanGenerator,
        clock: Clock,
        settingsFactory: (LocalDate) -> PlanningSettings = ::defaultPlanningSettings,
        onPrintJobAvailable: (PrintJob?) -> Unit = {},
    ) : this(
        session = object : PlanSession {
            override suspend fun open(settings: PlanningSettings): PlanGenerationOutcome =
                generator.execute(settings)

            override suspend fun regenerate(settings: PlanningSettings): PlanGenerationOutcome =
                generator.execute(settings)
        },
        clock = clock,
        settingsFactory = settingsFactory,
        onPrintJobAvailable = onPrintJobAvailable,
    )
    var state by mutableStateOf<PlanPreviewUiState>(PlanPreviewUiState.Loading)
        private set

    /**
     * 页面进入时调用：只在尚未得到结果（[state] 仍为 [PlanPreviewUiState.Loading]）时生成。
     *
     * “预览 → 打印 → 返回预览”会复用同一个 ViewModel 实例，但 `LaunchedEffect`
     * 在重新进入组合时仍会再执行一次；这里用已有状态阻止返回时重新生成计划，
     * 避免计划号被刷新。用户主动“生成本周计划”会创建新实例（初始为 Loading），
     * 因此仍会重新生成；若上次生成被取消、状态停在 Loading，也会重新触发。
     */
    suspend fun refreshIfNeeded() {
        if (state !is PlanPreviewUiState.Loading) return
        refresh()
    }

    suspend fun refresh() = open(regenerate = false)

    /** 用户确认“重新生成本周计划”：生成新计划号，并把旧计划标记为已被替代。 */
    suspend fun regenerate() = open(regenerate = true)

    private suspend fun open(regenerate: Boolean) {
        state = PlanPreviewUiState.Loading
        onPrintJobAvailable(null)
        val settings = settingsFactory(LocalDate.now(clock))
        state = try {
            val outcome = if (regenerate) {
                session.regenerate(settings)
            } else {
                session.open(settings)
            }
            when (outcome) {
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
