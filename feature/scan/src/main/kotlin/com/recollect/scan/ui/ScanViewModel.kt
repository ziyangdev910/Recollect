package com.recollect.scan.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.recollect.data.room.repository.ScanSessionRepository
import com.recollect.scan.ScanErrorCode
import com.recollect.scan.usecase.ScanManualOutcome
import com.recollect.scan.usecase.ScanPageOutcome
import java.time.Clock
import java.time.LocalDate

/**
 * 图片管线入口；由 app 层把「解码 → `ScanPageUseCase` → `ScanEngine`/`NoteOcr`」组装进来（R-03）。
 *
 * 入参是 URI 字符串而不是 `Bitmap`/`Uri`，因此 ViewModel 可在 JVM 上直接测试。
 */
fun interface ScanRunner {
    suspend fun run(imageLocation: String): ScanRunResult
}

sealed interface ScanRunResult {
    /**
     * @param previewLocation 识别用原图的缓存缩略图（相机临时帧已按规范删除）；
     *   识别失败或无法生成时为 `null`，只用于扫描页缩略图展示。
     */
    data class Completed(
        val outcome: ScanPageOutcome,
        val previewLocation: String? = null,
    ) : ScanRunResult

    /** 文件无法读取或解码；不进入识别、不写库。 */
    data object ImageUnreadable : ScanRunResult
}

/** 手动回退入口；由 app 层适配 [com.recollect.scan.usecase.ScanManualEntryUseCase]。 */
fun interface ScanManualEntry {
    suspend fun submit(pageId: String, questionNumber: String): ScanManualOutcome
}

/** 会话中已扫入的一页（按 `PendingFeedback.pageId` 聚合）。 */
data class ScannedPageSummary(
    val pageId: String,
    val plannedDate: LocalDate,
    val rowCount: Int,
    /** 该页的不同题目数（同一题多个知识点只算一题），扫描页显示「Y 题」。 */
    val questionCount: Int,
    val pageIsFuture: Boolean,
    /** 扫描时缓存的缩略图路径；手动补录或进程恢复后可能为 `null`。 */
    val previewLocation: String? = null,
)

data class ScanSessionSummary(
    val sessionId: String,
    val planId: String,
    val pages: List<ScannedPageSummary>,
) {
    val rowCount: Int
        get() = pages.sumOf { it.rowCount }

    val questionCount: Int
        get() = pages.sumOf { it.questionCount }
}

/** 阻断错误；[manualAllowed] 只对 QR 类失败开放手动回退（scan-pipeline.md）。 */
data class ScanErrorView(
    val code: ScanErrorCode,
    val message: String,
    val manualAllowed: Boolean,
)

data class ManualEntryState(
    val pageId: String = "",
    val questionNumber: String = "",
    val error: String? = null,
    val busy: Boolean = false,
)

data class ScanUiState(
    val loading: Boolean = true,
    val busy: Boolean = false,
    val session: ScanSessionSummary? = null,
    val message: String? = null,
    val messageIsError: Boolean = false,
    val error: ScanErrorView? = null,
    val manual: ManualEntryState? = null,
)

/**
 * 扫描回读页状态（app.md「Scan」、scan-pipeline.md）。
 *
 * 职责：选图 → 交给 [ScanRunner] → 展示结果/错误 → 手动回退 → 进入确认页。
 * 不持有 `Context`，不直接调用 OpenCV/ML Kit（R-03）；只写 `PendingFeedback`（R-04）。
 */
class ScanViewModel(
    private val runner: ScanRunner,
    private val manualEntry: ScanManualEntry,
    private val repository: ScanSessionRepository,
    private val clock: Clock,
    /** 删除页面或开始新会话时清理缩略图缓存；测试与无缓存场景传空实现。 */
    private val deletePreview: suspend (String) -> Unit = {},
) {

    var state by mutableStateOf(ScanUiState())
        private set

    /** 已扫页的扫描顺序（旧 → 新），用于「最新页面压入最右侧」的缩略图列表。 */
    private val scannedOrder = mutableListOf<String>()

    /** pageId → 缩略图路径；扫描页在确认页返回后仍保留同一 ViewModel 实例。 */
    private val previewLocations = mutableMapOf<String, String>()

    /**
     * 进入扫描页或从确认页返回后恢复未提交会话（app.md「全局错误与恢复」）。
     *
     * 从首页进入时先调用 [startNewScan] 丢弃旧会话；从确认页返回时不调用，保留原扫描状态。
     */
    suspend fun load() {
        state = state.copy(loading = true)
        val session = runCatching { loadSession() }.getOrNull()
        state = state.copy(loading = false, session = session)
    }

    /**
     * 从首页进入扫描页：丢弃上次未提交会话与缩略图，恢复为「一页未扫」
     * （2026-11-28 用户要求）。从确认页返回时不得调用本方法。
     */
    suspend fun startNewScan() {
        previewLocations.values.forEach { location ->
            runCatching { deletePreview(location) }
        }
        scannedOrder.clear()
        previewLocations.clear()
        state = ScanUiState(loading = false)
        runCatching { repository.discardActiveSession() }
    }

    /** 缩略图右上角的删除：从会话中移除该页，并清理它的缩略图。 */
    suspend fun removePage(pageId: String) {
        val session = state.session ?: return
        runCatching { repository.removePage(session.sessionId, pageId) }
        scannedOrder.remove(pageId)
        previewLocations.remove(pageId)?.let { location ->
            runCatching { deletePreview(location) }
        }
        state = state.copy(session = runCatching { loadSession() }.getOrNull())
    }

    /** 拍照或从相册选图后的入口；[imageLocation] 是页面传入的 URI 字符串，本类不持有平台上下文。 */
    suspend fun onImageSelected(imageLocation: String) {
        if (state.busy) return
        state = state.copy(busy = true, error = null, message = null, messageIsError = false)

        val result = try {
            runner.run(imageLocation)
        } catch (e: Exception) {
            state = state.copy(
                busy = false,
                message = e.message ?: "识别失败，请重试。",
                messageIsError = true,
            )
            return
        }

        when (result) {
            ScanRunResult.ImageUnreadable -> state = state.copy(
                busy = false,
                message = "无法读取所选图片，请重试或换一张。",
                messageIsError = true,
            )

            is ScanRunResult.Completed -> applyOutcome(result.outcome, result.previewLocation)
        }
    }

    /** QR 失败的降级入口；页面切换到手动输入表单（scan-pipeline.md「手动回退」）。 */
    fun startManualInput() {
        state = state.copy(manual = ManualEntryState(), error = null)
    }

    fun updateManualPageId(value: String) {
        val manual = state.manual ?: return
        state = state.copy(manual = manual.copy(pageId = value, error = null))
    }

    fun updateManualQuestionNumber(value: String) {
        val manual = state.manual ?: return
        state = state.copy(manual = manual.copy(questionNumber = value, error = null))
    }

    fun cancelManualInput() {
        state = state.copy(manual = null)
    }

    suspend fun submitManualInput() {
        val manual = state.manual ?: return
        if (manual.busy) return
        state = state.copy(manual = manual.copy(busy = true, error = null))

        val outcome = runCatching {
            manualEntry.submit(manual.pageId, manual.questionNumber)
        }.getOrElse { error ->
            state = state.copy(
                manual = manual.copy(busy = false, error = error.message ?: "补录失败，请重试。"),
            )
            return
        }

        when (outcome) {
            is ScanManualOutcome.Rejected -> {
                state = state.copy(manual = manual.copy(busy = false, error = outcome.message))
            }

            is ScanManualOutcome.Stored -> {
                // 手动补录没有原图，缩略图用纸页占位；已有的缩略图不被覆盖。
                rememberScannedPage(outcome.pageId, previewLocation = null)
                val session = runCatching { loadSession() }.getOrNull() ?: state.session
                val notices = buildList {
                    if (outcome.replacedPage) add("该页此前已扫描，本次结果已更新。")
                    if (outcome.lateBatch) add("已超过计划结束日 7 天，按晚复习记录。")
                    if (outcome.pageIsFuture) {
                        add("该页计划日为 ${outcome.pageDate}，晚于今天，确认页只能标记「本次未复习」。")
                    }
                }
                val suffix = if (notices.isEmpty()) "" else " " + notices.joinToString("")
                state = state.copy(
                    manual = null,
                    session = session,
                    message = "已补录 ${outcome.pageId} 的 1 道题，共 ${outcome.rowCount} 行；" +
                        "识别结果为空，请在确认页逐行标记。$suffix",
                    messageIsError = false,
                )
            }
        }
    }

    /** 页面级提示（例如系统相机不可用）；与识别结果共用同一条消息位。 */
    fun showMessage(message: String, isError: Boolean = true) {
        state = state.copy(message = message, messageIsError = isError)
    }

    /** 用户重新发起拍照/相册选择时清掉上一条页面提示，避免旧提示滞留。 */
    fun clearMessage() {
        state = state.copy(message = null, messageIsError = false)
    }

    /** 用户确认错误后回到拍照状态；重拍/相册按钮始终可用。 */
    fun dismissError() {
        state = state.copy(error = null)
    }

    private suspend fun applyOutcome(outcome: ScanPageOutcome, previewLocation: String?) {
        when (outcome) {
            is ScanPageOutcome.Rejected -> {
                state = state.copy(busy = false, error = errorView(outcome.code), message = null)
            }

            is ScanPageOutcome.Stored -> {
                // 重复扫描同一页会生成新缩略图，旧文件随之清理。
                val previousPreview = previewLocations[outcome.pageId]
                rememberScannedPage(outcome.pageId, previewLocation)
                if (previousPreview != null && previousPreview != previewLocation) {
                    runCatching { deletePreview(previousPreview) }
                }
                val session = runCatching { loadSession() }.getOrNull() ?: state.session
                // 「已写入…共…行」提示已按 2026-11-28 用户要求删除，
                // 只在重复扫描/晚复习/未来日期等需要用户注意时保留说明。
                val notices = buildList {
                    if (outcome.replacedPage) add("该页此前已扫描，本次结果已更新。")
                    if (outcome.lateBatch) add("已超过计划结束日 7 天，按晚复习记录。")
                    if (outcome.pageIsFuture) {
                        add("该页计划日为 ${outcome.pageDate}，晚于今天，确认页只能标记「本次未复习」。")
                    }
                }
                state = state.copy(
                    busy = false,
                    session = session,
                    error = null,
                    message = notices.joinToString("").ifEmpty { null },
                    messageIsError = false,
                )
            }
        }
    }

    /** 记录一页的扫描顺序；重复扫描的页移动到最新位置（列表最右侧）。 */
    private fun rememberScannedPage(pageId: String, previewLocation: String?) {
        scannedOrder.remove(pageId)
        scannedOrder.add(pageId)
        if (previewLocation != null) previewLocations[pageId] = previewLocation
    }

    private suspend fun loadSession(): ScanSessionSummary? {
        val active = repository.activeSession() ?: return null
        val rows = repository.pendingRows(active.sessionId)
        if (rows.isEmpty()) return null

        val today = LocalDate.now(clock)
        val grouped = rows.groupBy { it.pageId }
        // 本次进入后扫描的页按扫描先后排在最后（最新页在最右）；
        // 进程恢复的旧页没有顺序信息，排在它们之前并按 pageId 排序。
        val orderedIds = grouped.keys.filterNot { it in scannedOrder }.sorted() +
            scannedOrder.filter { it in grouped }
        val pages = orderedIds.map { pageId ->
            val pageRows = grouped.getValue(pageId)
            ScannedPageSummary(
                pageId = pageId,
                plannedDate = pageRows.first().plannedDate,
                rowCount = pageRows.size,
                questionCount = pageRows.map { it.questionId }.distinct().size,
                pageIsFuture = pageRows.first().plannedDate.isAfter(today),
                previewLocation = previewLocations[pageId],
            )
        }
        return ScanSessionSummary(active.sessionId, active.planId, pages)
    }

    private fun errorView(code: ScanErrorCode): ScanErrorView {
        val message = when (code) {
            ScanErrorCode.PAGE_NOT_FOUND ->
                "未找到页面四角标记，请将整页放入取景框后重拍。"
            ScanErrorCode.MULTIPLE_PAGES ->
                "图中检测到多页，请重拍，一次只拍一页。"
            ScanErrorCode.PAGE_UNREADABLE ->
                "页面图像不清晰或角度异常，请重拍。"
            ScanErrorCode.QR_NOT_FOUND ->
                "未检测到题目二维码，请重拍，或手动输入页面 ID + 题号。"
            ScanErrorCode.QR_UNREADABLE ->
                "二维码无法识别，请重拍，或手动输入页面 ID + 题号。"
            ScanErrorCode.QR_MULTIPLE ->
                "二维码与题目无法一一对应，请重拍，或手动输入页面 ID + 题号。"
            ScanErrorCode.QR_INVALID ->
                "二维码内容校验失败，请重拍，或手动输入页面 ID + 题号。"
            ScanErrorCode.OWNERSHIP_MISMATCH ->
                "该页不属于当前计划或题目归属不一致，请核对打印稿，必要时重新打印。"
        }
        val manualAllowed = code in setOf(
            ScanErrorCode.QR_NOT_FOUND,
            ScanErrorCode.QR_UNREADABLE,
            ScanErrorCode.QR_MULTIPLE,
            ScanErrorCode.QR_INVALID,
        )
        return ScanErrorView(code = code, message = message, manualAllowed = manualAllowed)
    }
}
