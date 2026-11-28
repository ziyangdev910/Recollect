package com.recollect.feature.planning

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.recollect.common.UiError
import com.recollect.data.room.repository.PlanGenerationException
import com.recollect.data.room.repository.PlanStatus
import com.recollect.data.room.repository.StoredPlan
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.CancellationException

/**
 * PlanPreview 状态（app.md「屏幕清单与状态」）。
 *
 * `Empty` 携带 overflow 提示：整份计划无可打印题目时，提示不能因为“空态”被丢掉（C-060、C-061/C-062）。
 * `Content.warnings` 只含中文标签与 message，不含分页内部代码（C-065 方案 B）。
 * `Content.pdfPreview` 是「生成计划」页内实时渲染的打印稿页图（2026-11-27 用户要求）。
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
        val pdfPreview: PlanPdfPreviewState = PlanPdfPreviewState.Unavailable,
    ) : PlanPreviewUiState

    data class Error(val error: UiError) : PlanPreviewUiState
}

/** 打印稿预览的加载状态。 */
sealed interface PlanPdfPreviewState {
    /** 未接入预览端口（JVM 单测或旧调用方）。 */
    data object Unavailable : PlanPdfPreviewState
    data object Loading : PlanPdfPreviewState
    data class Ready(val pages: List<ImageBitmap>) : PlanPdfPreviewState
    data class Failed(val message: String) : PlanPdfPreviewState
}

/**
 * PlanPreview 状态持有者（手写 ViewModel，不引入 lifecycle-viewmodel 依赖）。
 *
 * 不持有 `Context`（R-03）；计划生成失败按 C-047 映射为可重试 [UiError]。
 *
 * 2026-11-27 用户要求：不再区分「本周计划 / 下周计划」两个固定窗口，改为可选计划日期范围
 * （最多 7 天）；本周/下周的判断只用于填入默认范围。打印稿直接在页面内渲染，唯一操作是分享 PDF。
 */
class PlanPreviewViewModel(
    private val session: PlanSession,
    private val clock: Clock,
    /**
     * 初始计划开始日；与 [initialEndDate] 同为 null 时按本周计划状态自动选择：
     * 本周已 COMPLETED（完成确认）则默认填下一自然周，否则填今天所在自然周。
     */
    initialStartDate: LocalDate? = null,
    initialEndDate: LocalDate? = null,
    private val settingsFactory: (LocalDate, LocalDate, LocalDate) -> PlanningSettings =
        { start, end, _ -> PlanningSettings(planStartDate = start, planEndDate = end) },
    /** 当前题库：questionId → 最近修改时间；为 null 时跳过“题库已更新”检查。 */
    private val currentQuestionBank: suspend () -> Map<String, Instant>? = { null },
    /** 当前每日复习时长（分钟）；0 表示未知。与计划头记录值不同时提示重新生成。 */
    private val currentDailyMinutes: Int = 0,
    /** 计划成功后把同一份 `PrintJob` 交给打印稿预览/分享；失败/空态时清空，避免打印旧计划。 */
    private val onPrintJobAvailable: (PrintJob?) -> Unit = {},
    /** 打印稿预览、分享与演示回写端口；为 null 时不渲染 PDF 预览。 */
    private val documentGateway: PlanDocumentGateway? = null,
) {

    /**
     * 仅生成、不读取已有计划的适配构造（供既有单测注入生成桩）。
     * 生产路径必须使用 [session] 构造，才能复用范围内已有计划。
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
        initialStartDate = currentWeekSettings(LocalDate.now(clock)).planStartDate,
        initialEndDate = currentWeekSettings(LocalDate.now(clock)).planEndDate,
        settingsFactory = { _, _, today -> settingsFactory(today) },
        onPrintJobAvailable = onPrintJobAvailable,
    )

    var state by mutableStateOf<PlanPreviewUiState>(PlanPreviewUiState.Loading)
        private set

    /** 当前选中的计划日期范围；切换后重新加载/生成该范围内的计划。 */
    var startDate by mutableStateOf(
        initialStartDate ?: currentWeekSettings(LocalDate.now(clock)).planStartDate,
    )
        private set

    var endDate by mutableStateOf(
        initialEndDate ?: currentWeekSettings(LocalDate.now(clock)).planEndDate,
    )
        private set

    /** 分享面板已成功打开，等待用户返回本页；返回后据此判断“分享完成”。 */
    var shareLaunched by mutableStateOf(false)
        private set

    /** 本次分享已写回打印快照（与「已打印」同一落库路径）；为 false 时不得提示完成。 */
    var shareRecorded by mutableStateOf(false)
        private set

    /** 正在生成并分享 PDF；期间禁用分享按钮，避免并发写同一缓存文件。 */
    var sharing by mutableStateOf(false)
        private set

    var shareMessage by mutableStateOf<String?>(null)
        private set

    var shareMessageIsError by mutableStateOf(false)
        private set

    /** 加载到已有计划、且题库在计划生成后发生变化；页面据此询问是否重新生成。 */
    var bankChanged by mutableStateOf(false)
        private set

    /** 加载到已有计划、且每日复习时长与生成时不同；页面据此询问是否重新生成。 */
    var settingsChanged by mutableStateOf(false)
        private set

    private var initialRangeResolved = initialStartDate != null && initialEndDate != null
    private var dismissedChangeRange: Pair<LocalDate, LocalDate>? = null
    private var currentPrintJob: PrintJob? = null

    /**
     * 选择计划日期范围（最多 7 天）。
     *
     * 与从首页打开生成页一致：范围内已有计划则复用，没有则生成；不会把旧计划号带到另一段范围。
     */
    suspend fun selectRange(start: LocalDate, end: LocalDate) {
        val safeStart = start
        val safeEnd = end.coerceIn(safeStart, safeStart.plusDays(MAX_PLAN_DAYS - 1L))
        if (safeStart == startDate && safeEnd == endDate) return
        // 首次加载/切换尚未完成时忽略重复操作，避免并发打开两段范围。
        if (state is PlanPreviewUiState.Loading) return
        initialRangeResolved = true
        startDate = safeStart
        endDate = safeEnd
        bankChanged = false
        settingsChanged = false
        open(regenerate = false)
    }

    /**
     * 页面进入时调用：只在尚未得到结果（[state] 仍为 [PlanPreviewUiState.Loading]）时生成。
     *
     * 首次进入时若未指定日期范围，先读取本周计划状态决定默认范围；
     * 页面重建/返回时状态已不是 Loading，不会重新生成计划。
     */
    suspend fun refreshIfNeeded() {
        if (state !is PlanPreviewUiState.Loading) return
        if (!initialRangeResolved) {
            initialRangeResolved = true
            val currentStatus = quietly {
                session.activePlan(currentWeekSettings(LocalDate.now(clock)))?.status
            }
            // 保留原有默认规则：仅本周计划 COMPLETED（完成确认）时才填下一自然周；
            // PRINTED（已打印但未确认）仍留在本周。无论默认范围如何，
            // 随后加载已有计划时都会执行题库/时长变化检测。
            val defaults = if (currentStatus == PlanStatus.COMPLETED) {
                nextWeekSettings(LocalDate.now(clock))
            } else {
                currentWeekSettings(LocalDate.now(clock))
            }
            startDate = defaults.planStartDate
            endDate = defaults.planEndDate
        }
        refresh()
    }

    suspend fun refresh() = open(regenerate = false)

    /** 用户确认“重新生成计划”：生成新计划号，并把旧计划标记为已被替代。 */
    suspend fun regenerate() {
        bankChanged = false
        settingsChanged = false
        dismissedChangeRange = null
        open(regenerate = true)
    }

    /** 用户选择沿用当前计划：关闭提示，本轮不再就同一日期范围重复询问。 */
    fun dismissPlanChanges() {
        if (bankChanged || settingsChanged) dismissedChangeRange = startDate to endDate
        bankChanged = false
        settingsChanged = false
    }

    /**
     * 生成 PDF 并交给系统分享面板（微信、QQ 等）。
     *
     * 分享成功后与「已打印」走同一回写路径：写入每题 `pageId/qrPayload` 并把计划标记为
     * PRINTED，使分享出去、再打印成纸的稿件同样可以回读。重复点击在 [sharing] 期间被忽略。
     */
    suspend fun sharePdf() {
        if (sharing) return
        val job = currentPrintJob ?: return
        val gateway = documentGateway ?: return
        shareLaunched = false
        shareRecorded = false
        shareMessage = null
        shareMessageIsError = false
        sharing = true
        val outcome = try {
            gateway.share(job)
        } catch (e: CancellationException) {
            sharing = false
            throw e
        } catch (e: Exception) {
            PlanShareResult.Failed(e.message ?: "生成 PDF 失败，请重试。")
        }
        sharing = false
        when (outcome) {
            is PlanShareResult.Shared -> {
                // 分享面板已打开：页面重新获得焦点后即可提示完成（快照写入成功时）。
                shareLaunched = true
                shareRecorded = outcome.snapshotRecorded
                shareMessage = outcome.message
                shareMessageIsError = outcome.isError
            }

            is PlanShareResult.Failed -> {
                shareMessage = outcome.message
                shareMessageIsError = true
            }
        }
    }

    fun clearShareMessage() {
        shareMessage = null
        shareMessageIsError = false
    }

    private suspend fun open(regenerate: Boolean) {
        state = PlanPreviewUiState.Loading
        bankChanged = false
        settingsChanged = false
        currentPrintJob = null
        onPrintJobAvailable(null)
        val settings = settingsFactory(startDate, endDate, LocalDate.now(clock))
        // 只有读取已有计划时才比较题库；刚生成的新计划天然与当前题库一致。
        val existingPlan = if (regenerate) null else quietly { session.activePlan(settings) }
        try {
            val outcome = if (regenerate) {
                session.regenerate(settings)
            } else {
                session.open(settings)
            }
            var printJobToRender: PrintJob? = null
            var planIdToRender: String? = null
            val handledQuestionIds = when (outcome) {
                is PlanGenerationOutcome.Planned -> {
                    currentPrintJob = outcome.printJob
                    onPrintJobAvailable(outcome.printJob)
                    state = PlanPreviewUiState.Content(
                        planId = outcome.preview.planId,
                        pages = outcome.preview.pages,
                        warnings = outcome.preview.warnings,
                        deferred = outcome.preview.deferred,
                        pdfPreview = if (documentGateway == null) {
                            PlanPdfPreviewState.Unavailable
                        } else {
                            PlanPdfPreviewState.Loading
                        },
                    )
                    printJobToRender = outcome.printJob
                    planIdToRender = outcome.preview.planId
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
            // 先检测题库/时长变化，再渲染可能耗时的 PDF 预览：
            // 只要加载的是已有计划，进入页或切换范围时都会立即进入提示判断。
            if (existingPlan != null) {
                detectPlanChanges(existingPlan, handledQuestionIds)
            }
            val job = printJobToRender
            val renderPlanId = planIdToRender
            if (job != null && renderPlanId != null && documentGateway != null) {
                renderPdfPreview(job, renderPlanId)
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

    /** 生成当前计划的打印稿页图；计划在渲染期间被替换时丢弃旧结果。 */
    private suspend fun renderPdfPreview(printJob: PrintJob, planId: String) {
        val gateway = documentGateway ?: return
        val preview = try {
            gateway.preview(printJob)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PlanPdfPreview.Failed(e.message ?: "打印稿预览生成失败。")
        }
        val latest = state as? PlanPreviewUiState.Content ?: return
        if (latest.planId != planId) return
        state = latest.copy(
            pdfPreview = when (preview) {
                is PlanPdfPreview.Ready -> PlanPdfPreviewState.Ready(preview.pages)
                is PlanPdfPreview.Failed -> PlanPdfPreviewState.Failed(preview.message)
            },
        )
    }

    /**
     * 已有计划是否与当前数据不一致：
     * - 题库：任一未归档题目在计划生成后被新增/编辑（`updatedAt` 晚于计划 `createdAt`），
     *   或计划中已排入/推迟/告警的题目现在被归档；
     * - 每日复习时长：计划头记录了生成时的分钟数，且与当前设置不同。
     *
     * 旧计划（v17 前）的 `dailyMinutes` 为 0（未知），不据此提示，避免误报。
     */
    private suspend fun detectPlanChanges(
        existingPlan: StoredPlan,
        handledQuestionIds: Set<String>,
    ) {
        if (dismissedChangeRange == (startDate to endDate)) return
        detectBankChanged(existingPlan, handledQuestionIds)
        settingsChanged = currentDailyMinutes > 0 &&
            existingPlan.dailyMinutes > 0 &&
            existingPlan.dailyMinutes != currentDailyMinutes
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

/** 计划范围最多 7 天（2026-11-27 用户要求：允许选日期范围，但保持周计划的容量语义）。 */
const val MAX_PLAN_DAYS = 7L

/** 今天所在自然周（周一至周日）；只用于填入默认计划范围。 */
fun currentWeekSettings(today: LocalDate): PlanningSettings {
    val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return PlanningSettings(planStartDate = start, planEndDate = start.plusDays(6))
}

/** 紧随其后的下一自然周（周一至周日）；只用于填入默认计划范围。 */
fun nextWeekSettings(today: LocalDate): PlanningSettings {
    val start = currentWeekSettings(today).planStartDate.plusWeeks(1)
    return PlanningSettings(planStartDate = start, planEndDate = start.plusDays(6))
}

/** 「生成计划」页重新生成入口的文案。 */
const val REGENERATE_PLAN_LABEL = "重新生成计划"
