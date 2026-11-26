package com.recollect.feature.planning

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.recollect.common.UiError
import com.recollect.data.room.repository.PlanGenerationException
import com.recollect.data.room.repository.PlanStatus
import com.recollect.data.room.repository.StoredPlan
import kotlinx.coroutines.CancellationException
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
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
        val deferred: List<PlanPreviewDeferred> = emptyList(),
    ) : PlanPreviewUiState

    data class Content(
        val planId: String,
        val pages: List<PlanPreviewPage>,
        val warnings: List<PlanPreviewWarning>,
        val deferred: List<PlanPreviewDeferred> = emptyList(),
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
    /**
     * 初始选中的周；传 null 时按本周计划状态自动选择：
     * 本周已 COMPLETED（完成确认）则进入下周，否则进入本周。
     */
    initialWeek: PlanWeek? = PlanWeek.CURRENT,
    private val settingsFactory: (PlanWeek, LocalDate) -> PlanningSettings = { week, today ->
        week.settingsFor(today)
    },
    /** 当前题库：questionId → 最近修改时间；为 null 时跳过“题库已更新”检查。 */
    private val currentQuestionBank: suspend () -> Map<String, Instant>? = { null },
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
        settingsFactory: (LocalDate) -> PlanningSettings = ::currentWeekSettings,
        onPrintJobAvailable: (PrintJob?) -> Unit = {},
    ) : this(
        session = object : PlanSession {
            override suspend fun activePlan(settings: PlanningSettings): StoredPlan? = null

            override suspend fun open(settings: PlanningSettings): PlanGenerationOutcome =
                generator.execute(settings)

            override suspend fun regenerate(settings: PlanningSettings): PlanGenerationOutcome =
                generator.execute(settings)
        },
        clock = clock,
        initialWeek = PlanWeek.CURRENT,
        settingsFactory = { _, today -> settingsFactory(today) },
        onPrintJobAvailable = onPrintJobAvailable,
    )
    var state by mutableStateOf<PlanPreviewUiState>(PlanPreviewUiState.Loading)
        private set

    /** 预览页顶部选中的周；切换后重新加载/生成该窗口的计划。 */
    var week by mutableStateOf(initialWeek ?: PlanWeek.CURRENT)
        private set

    /** 加载到已有计划、且题库在计划生成后发生变化；页面据此询问是否重新生成。 */
    var bankChanged by mutableStateOf(false)
        private set

    private var initialWeekResolved = initialWeek != null
    private var dismissedBankNoticeFor: PlanWeek? = null

    /**
     * 切换「本周计划 / 下周计划」。
     *
     * 与从首页打开某个窗口一致：已有计划则复用，没有则生成；不会把旧计划号带到另一周。
     */
    suspend fun selectWeek(target: PlanWeek) {
        if (target == week) return
        // 首次加载/切换尚未完成时忽略重复点击，避免并发打开两个窗口。
        if (state is PlanPreviewUiState.Loading) return
        initialWeekResolved = true
        week = target
        bankChanged = false
        open(regenerate = false)
    }

    /**
     * 页面进入时调用：只在尚未得到结果（[state] 仍为 [PlanPreviewUiState.Loading]）时生成。
     *
     * 首次进入时若未指定初始周，先读取本周计划状态决定默认落在本周还是下周；
     * “预览 → 打印 → 返回预览”会复用同一个 ViewModel 实例，此时状态已不是 Loading，
     * 不会重新生成计划。
     */
    suspend fun refreshIfNeeded() {
        if (state !is PlanPreviewUiState.Loading) return
        if (!initialWeekResolved) {
            initialWeekResolved = true
            val currentStatus = quietly {
                session.activePlan(currentWeekSettings(LocalDate.now(clock)))?.status
            }
            if (currentStatus == PlanStatus.COMPLETED) {
                week = PlanWeek.NEXT
            }
        }
        refresh()
    }

    suspend fun refresh() = open(regenerate = false)

    /** 用户确认“重新生成本周计划”：生成新计划号，并把旧计划标记为已被替代。 */
    suspend fun regenerate() {
        bankChanged = false
        dismissedBankNoticeFor = null
        open(regenerate = true)
    }

    /** 用户选择沿用当前计划：关闭提示，本轮不再就同一周重复询问。 */
    fun dismissBankChanged() {
        if (bankChanged) dismissedBankNoticeFor = week
        bankChanged = false
    }

    private suspend fun open(regenerate: Boolean) {
        state = PlanPreviewUiState.Loading
        bankChanged = false
        onPrintJobAvailable(null)
        val settings = settingsFactory(week, LocalDate.now(clock))
        // 只有读取已有计划时才比较题库；刚生成的新计划天然与当前题库一致。
        val existingPlan = if (regenerate) null else quietly { session.activePlan(settings) }
        try {
            val outcome = if (regenerate) {
                session.regenerate(settings)
            } else {
                session.open(settings)
            }
            val handledQuestionIds = when (outcome) {
                is PlanGenerationOutcome.Planned -> {
                    onPrintJobAvailable(outcome.printJob)
                    state = PlanPreviewUiState.Content(
                        planId = outcome.preview.planId,
                        pages = outcome.preview.pages,
                        warnings = outcome.preview.warnings,
                        deferred = outcome.preview.deferred,
                    )
                    buildSet {
                        addAll(outcome.printJob.questions.keys)
                        addAll(outcome.preview.deferred.map { it.questionId })
                        addAll(outcome.preview.warnings.map { it.questionId })
                    }
                }
                is PlanGenerationOutcome.Empty -> {
                    state = PlanPreviewUiState.Empty(
                        message = outcome.message,
                        warnings = outcome.warnings,
                        deferred = outcome.deferred,
                    )
                    buildSet {
                        addAll(outcome.deferred.map { it.questionId })
                        addAll(outcome.warnings.map { it.questionId })
                    }
                }
            }
            if (existingPlan != null) {
                detectBankChanged(existingPlan, handledQuestionIds)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: PlanGenerationException) {
            state = PlanPreviewUiState.Error(
                UiError(
                    code = e.code.uiErrorCode(),
                    message = e.message ?: "生成计划失败，请重试。",
                    retryable = true,
                ),
            )
        } catch (e: Exception) {
            state = PlanPreviewUiState.Error(
                UiError(
                    code = "PLAN_UNKNOWN",
                    message = e.message ?: "生成计划失败，请重试。",
                    retryable = true,
                ),
            )
        }
    }

    /**
     * 题库是否与计划生成时不同。
     *
     * 计划只排入「当天到期」的题目，未到期题目本就不在计划中，因此不能拿全部题库与
     * 计划题目做集合比较。这里只判断两类真实变化：
     * - 任一未归档题目在计划生成后被新增/编辑（`updatedAt` 晚于计划 `createdAt`）；
     * - 计划中已排入/推迟/告警的题目现在被归档（`handledQuestionIds` 中已不在题库）。
     */
    private suspend fun detectBankChanged(existingPlan: StoredPlan, handledQuestionIds: Set<String>) {
        if (dismissedBankNoticeFor == week) return
        val bank = quietly { currentQuestionBank() } ?: return
        val editedOrAdded = bank.values.any { it.isAfter(existingPlan.createdAt) }
        val plannedQuestionRemoved = handledQuestionIds.any { it !in bank.keys }
        bankChanged = editedOrAdded || plannedQuestionRemoved
    }

    private suspend fun <T> quietly(block: suspend () -> T): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
}

/**
 * 首页可以查看/生成的两个计划窗口：今天所在的自然周与紧随其后的下一自然周。
 *
 * 只提供这两周，不允许生成更远的计划；已经存在的计划按窗口复用，不会因为再次点击而顺延。
 */
enum class PlanWeek {
    CURRENT,
    NEXT,
}

/** 今天所在自然周（周一至周日）。与首页“本周计划”标题一一对应。 */
fun currentWeekSettings(today: LocalDate): PlanningSettings {
    val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return PlanningSettings(planStartDate = start, planEndDate = start.plusDays(6))
}

/** 紧随其后的下一自然周（周一至周日）。 */
fun nextWeekSettings(today: LocalDate): PlanningSettings {
    val start = currentWeekSettings(today).planStartDate.plusWeeks(1)
    return PlanningSettings(planStartDate = start, planEndDate = start.plusDays(6))
}

fun PlanWeek.settingsFor(today: LocalDate): PlanningSettings = when (this) {
    PlanWeek.CURRENT -> currentWeekSettings(today)
    PlanWeek.NEXT -> nextWeekSettings(today)
}

/** 预览页顶部两个选项的文案。 */
val PlanWeek.tabLabel: String
    get() = when (this) {
        PlanWeek.CURRENT -> "本周计划"
        PlanWeek.NEXT -> "下周计划"
    }

/** 预览页“重新生成…”按钮与确认框的文案：随打开窗口变化，避免把下周计划叫成本周计划。 */
val PlanWeek.regenerateLabel: String
    get() = when (this) {
        PlanWeek.CURRENT -> "重新生成本周计划"
        PlanWeek.NEXT -> "重新生成下周计划"
    }
