package com.recollect.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.recollect.app.navigation.AppNavigator
import com.recollect.app.navigation.AppRoute
import com.recollect.app.onboarding.DemoDataResult
import com.recollect.app.onboarding.DemoSheetRenderer
import com.recollect.app.onboarding.DemoStopMode
import com.recollect.app.onboarding.DemoTarget
import com.recollect.app.onboarding.DemoTourOverlay
import com.recollect.app.onboarding.DemoTourState
import com.recollect.app.onboarding.DemoTourStop
import com.recollect.app.onboarding.blocksInteraction
import com.recollect.app.ui.theme.RecollectTheme
import com.recollect.data.room.entity.AppSettings
import com.recollect.data.room.repository.PlanStatus
import com.recollect.feature.backup.BackupDialog
import com.recollect.feature.backup.BackupViewModel
import com.recollect.feature.backup.ImportDialogs
import com.recollect.feature.backup.ImportState
import com.recollect.feature.backup.ImportViewModel
import com.recollect.feature.confirm.ConfirmScreen
import com.recollect.feature.confirm.ConfirmSubmitter
import com.recollect.feature.confirm.ConfirmViewModel
import com.recollect.feature.entry.EntryScreen
import com.recollect.feature.entry.EntryViewModel
import com.recollect.feature.entry.KnowledgePointLibraryScreen
import com.recollect.feature.entry.KnowledgePointLibraryViewModel
import com.recollect.feature.entry.QuestionEditPrefill
import com.recollect.feature.entry.QuestionEditScreen
import com.recollect.feature.entry.QuestionEditTourTarget
import com.recollect.feature.entry.QuestionEditViewModel
import com.recollect.feature.entry.SourceTagLibraryScreen
import com.recollect.feature.entry.SourceTagLibraryViewModel
import com.recollect.feature.planning.PlanPreviewScreen
import com.recollect.feature.planning.PlanPreviewUiState
import com.recollect.feature.planning.PlanPreviewViewModel
import com.recollect.feature.planning.PrintJob
import com.recollect.feature.planning.currentWeekSettings
import com.recollect.feature.planning.settingsFor
import com.recollect.feature.progress.ReviewProgressAggregator
import com.recollect.feature.progress.ReviewProgressScreen
import com.recollect.feature.progress.ReviewProgressViewModel
import com.recollect.feature.print.AndroidPrintLauncher
import com.recollect.feature.print.PrintPlanInput
import com.recollect.feature.print.PrintPlanScreen
import com.recollect.feature.print.PrintPlanViewModel
import com.recollect.scan.ui.DemoScanSheet
import com.recollect.scan.ui.ScanScreen
import com.recollect.scan.ui.ScanViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import org.ziyangdev910.recollect.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Compose 壳（阶段 5.1/5.2）。
 *
 * 本阶段提供 Home → Entry → PlanPreview 的首个可跑纵切；其余路由保持占位。
 * [container] 由 `MainActivity` 注入，屏幕级 ViewModel 从这里取 Repository/UseCase。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    container: AppContainer,
    navigator: AppNavigator,
    importUri: String? = null,
    onImportUriConsumed: () -> Unit = {},
    /** 由系统「用其他方式打开」触发的导入成功并留下 Toast 后，退出整个应用。 */
    onExternalImportFinished: () -> Unit = {},
) {
    // 在 AppRoot 顶层读取导航状态，让顶层作用域订阅返回栈变化：
    // 页面 push 后 BackHandler 才会被启用，pop 回根路由后再交还系统。
    val current = navigator.current
    val canPop = navigator.canPop

    // 首次复习演示（2026-11-26 用户要求）：用户亲手完成录入 → 生成 → 打印 → 扫描 → 确认。
    // 演示前把当前数据库导出成快照、只种入演示用知识点/来源标签；结束或跳过时覆盖导入快照还原。
    var demoTourState by remember(container) {
        mutableStateOf<DemoTourState>(DemoTourState.Inactive)
    }
    val demoTourActive = demoTourState.blocksInteraction
    // 各真实控件的屏幕坐标，供黑底蒙层挖出可点击窗口。
    var demoTargets by remember(container) {
        mutableStateOf<Map<DemoTarget, Rect>>(emptyMap())
    }
    // 顶部说明条下方的内容区：外层盒子顶部用于摆放说明条，内层用于「确认」整页聚焦。
    var demoContentTopPx by remember(container) { mutableStateOf(0f) }
    var demoContentBounds by remember(container) { mutableStateOf<Rect?>(null) }
    var demoBarHeightPx by remember(container) { mutableStateOf(0) }
    var demoConfirmSubmitted by remember(container) { mutableStateOf(false) }
    var demoSheet by remember(container) { mutableStateOf<DemoScanSheet?>(null) }
    // 手动重看演示时，欢迎卡额外提示「您的数据不会丢失」。
    var demoPreserveDataNotice by remember(container) { mutableStateOf(false) }
    val demoDensity = LocalDensity.current
    val demoBarHeightDp = with(demoDensity) { demoBarHeightPx.toDp() }
    val reportDemoTarget: (DemoTarget, Rect) -> Unit = { target, rect ->
        demoTargets = demoTargets + (target to rect)
    }

    // 录入/编辑题目页的表单用 `imePadding` 自己避让键盘（保留顶栏）；系统默认会把整个
    // 窗口平移（本机解析为 adjustPan），与表单的 imePadding 叠加后会在输入框下方留下
    // 一大块空白。因此只在该页显式要求 adjustResize（不让系统平移）；其余页面没有做
    // IME 避让，保持系统默认的 adjustPan，由系统把获得焦点的输入框平移进可见区域。
    // 只改 adjust 位，保留系统设置的其他状态位。
    val hostView = LocalView.current
    LaunchedEffect(current) {
        val window = hostView.context.findHostActivity()?.window ?: return@LaunchedEffect
        val adjustMask = WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST
        val currentMode = window.attributes.softInputMode
        val desiredAdjust = if (current is AppRoute.QuestionEdit) {
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        } else {
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        }
        if (currentMode and adjustMask != desiredAdjust) {
            window.setSoftInputMode((currentMode and adjustMask.inv()) or desiredAdjust)
        }
    }

    // 根路由时不拦截系统返回，交给 Activity 默认结束进程级页面。
    BackHandler(enabled = canPop) {
        navigator.pop()
    }

    // 演示期间吞掉返回键，避免用户误退出自动漫游（跳过着陆点由卡片按钮提供）。
    BackHandler(enabled = demoTourActive) { /* 只拦截，不处理 */ }

    // R-03：PrintManager 封装在 launcher 中；ViewModel 只依赖 PrintLauncher 端口。
    val printContext = LocalContext.current
    val printLauncher = remember(printContext, container) {
        AndroidPrintLauncher(
            context = printContext,
            scope = container.applicationScope,
        )
    }
    // 开源许可证全文打包在 assets；只在首次组合时读取一次。
    val licenseTexts = remember(printContext) { readLicenseTexts(printContext) }

    // 导入会整体替换数据：homeRefreshKey 触发首页统计重读，dataRevision 让题库之外
    // 的页面级 ViewModel（知识点、来源、进度）在下次打开（或当前停留）时重建并重读。
    var homeRefreshKey by remember(container) { mutableStateOf(0) }
    var dataRevision by remember(container) { mutableStateOf(0) }
    // 2026-11-26 用户要求：导出数据库改为悬浮在首页之上的弹窗，用本地状态控制显示，
    // 不进入路由返回栈；关闭（返回图标/系统返回）即销毁本次 ViewModel 状态。
    var showBackupDialog by remember(container) { mutableStateOf(false) }

    // 导入数据库：两个入口（导航栏「导入数据库」与系统「用其他方式打开」）
    // 共用同一个状态机；覆盖确认、失败提示与成功 Toast 都由它驱动。
    val importScope = rememberCoroutineScope()
    val importViewModel = remember(container) {
        ImportViewModel(
            fileReader = container.backupFileReader,
            importer = container.databaseImporter,
        )
    }
    // 本次导入是否来自系统「用其他方式打开」：成功后需要自动退出应用（SAF 入口不退出）。
    var importFromExternal by remember(container) { mutableStateOf(false) }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            importFromExternal = false
            importScope.launch { importViewModel.prepare(it.toString()) }
        }
    }
    LaunchedEffect(importUri) {
        if (!importUri.isNullOrBlank()) {
            importFromExternal = true
            // 先完成读取/校验再清空 URI：若先清空，importUri 变化会取消本协程，
            // 状态会停在「正在校验」。
            importViewModel.prepare(importUri)
            onImportUriConsumed()
        }
    }
    // 计划预览页的 ViewModel 归 AppRoot 顶层所有：路由分支离开组合后，
    // 分支内 remember 的 ViewModel 会被丢弃；若在分支内创建，从打印页返回
    // 预览页时会重建 ViewModel 并触发 LaunchedEffect 重新生成计划，计划号随之变化。
    // 顶部持有同一实例即可让“预览 → 打印 → 返回预览”复用原计划；
    // 只有再次从 Home/Entry 点击某个窗口时才递增 generation 重新生成。
    var planPreviewGeneration by remember(container) { mutableStateOf(0) }
    // 每日复习时长（schema v12，随备份导入/导出）；初始值先取默认，首页加载后覆盖。
    var dailyReviewMinutes by remember(container) {
        mutableStateOf(AppSettings.DEFAULT_DAILY_REVIEW_MINUTES)
    }
    // 改设置后重建预览 ViewModel，让“下一份计划”用新的每日分钟数；
    // 已生成的计划不会被回溯修改（settings.md「变更生效」）。
    val planPreviewViewModel = remember(
        container,
        planPreviewGeneration,
        dailyReviewMinutes,
    ) {
        PlanPreviewViewModel(
            session = container.planSession,
            clock = container.clock,
            // 默认周由预览页读取本周计划状态自动决定：本周已完成确认则进入下周。
            initialWeek = null,
            settingsFactory = { week, today ->
                week.settingsFor(today).withDailyMinutes(dailyReviewMinutes)
            },
            currentQuestionBank = {
                container.entryRepository.summaries()
                    .associate { it.questionId to it.updatedAt }
            },
            onPrintJobAvailable = { container.printPlanSession.update(it) },
        )
    }
    val settingsScope = rememberCoroutineScope()
    val saveDailyReviewMinutes: (Int) -> Unit = { minutes ->
        settingsScope.launch {
            val saved = quietly {
                container.settingsRepository.updateDailyReviewMinutes(minutes)
                true
            } ?: false
            if (saved) {
                dailyReviewMinutes = minutes.coerceIn(
                    AppSettings.MIN_DAILY_REVIEW_MINUTES,
                    AppSettings.MAX_DAILY_REVIEW_MINUTES,
                )
            }
        }
    }

    // 首页概览：本周计划状态与题目/知识点/到期统计（计划窗口的选择已移到计划预览页）。
    // 任一统计读取失败只显示“—”，不阻塞首页入口（预览计划仍可用）。
    var thisWeekPlanStatus by remember(container) { mutableStateOf<PlanStatus?>(null) }
    var questionCount by remember(container) { mutableStateOf<Int?>(null) }
    var knowledgeCount by remember(container) { mutableStateOf<Int?>(null) }
    var overdueCount by remember(container) { mutableStateOf<Int?>(null) }
    var dueWithinWeekCount by remember(container) { mutableStateOf<Int?>(null) }

    // 首页每次重新获得焦点（回到该路由或应用从后台恢复）都重读，打印/确认完成后的状态才会立即反映。
    val lifecycleOwner = LocalLifecycleOwner.current
    var homeFocusRevision by remember(container) { mutableStateOf(0) }
    DisposableEffect(lifecycleOwner, current) {
        if (current != AppRoute.Home) return@DisposableEffect onDispose {}
        var firstResume = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (firstResume) firstResume = false else homeFocusRevision++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(current, container, homeRefreshKey, homeFocusRevision) {
        if (current != AppRoute.Home) return@LaunchedEffect
        // 导入会覆盖设置，homeRefreshKey 变化时一并重读。
        dailyReviewMinutes = quietly { container.settingsRepository.dailyReviewMinutes() }
            ?: AppSettings.DEFAULT_DAILY_REVIEW_MINUTES
        val today = LocalDate.now(container.clock)
        val thisWeek = currentWeekSettings(today)
        thisWeekPlanStatus = quietly {
            container.planRepository.activePlan(
                weekStart = thisWeek.planStartDate,
                weekEnd = thisWeek.planEndDate,
            )?.status
        }
        questionCount = quietly { container.entryRepository.summaries().size }
        knowledgeCount = quietly { container.knowledgePointLibraryRepository.tags().size }
        val stats = quietly {
            val rows = container.reviewProgressRepository.questions()
            if (rows.isEmpty()) null else ReviewProgressAggregator.build(rows, today).stats
        }
        overdueCount = stats?.overdueQuestions
        dueWithinWeekCount = stats?.dueWithinWeekQuestions
    }
    // 每次从首页/题库进入预览都重建 ViewModel：默认周由预览页读取本周计划状态决定。
    val openPlanPreview: () -> Unit = {
        planPreviewGeneration += 1
        navigator.push(AppRoute.PlanPreview)
    }

    // 题库页的 ViewModel 与列表滚动状态归 AppRoot 顶层所有：进入题目编辑页后，
    // 路由分支离开组合会丢弃分支内的 remember，返回时列表会重新加载并回到顶部。
    val entryViewModel = remember(container) {
        EntryViewModel(repository = container.entryRepository)
    }
    val entryListState = rememberLazyListState()

    // ---- 首次复习演示状态机（2026-11-26 用户要求）----
    val demoStore = container.firstRunDemoStore
    val demoScope = rememberCoroutineScope()

    // 演示种子只包含两个知识点和一个来源标签；题号、页码、时长等由 prefill 填进表单，
    // 用户仍然需要亲手点「保存」，题目才会真正进入题库。
    val demoPrefill = remember {
        QuestionEditPrefill(
            questionNumber = "12",
            sourcePageCode = 42,
            sourcePageLabel = "人教版九年级数学上册",
            knowledgePointIds = listOf("demo-kp-01"),
            estimatedReviewMinutes = 6,
        )
    }

    // 演示数据变化（种入最小数据 / 还原快照）后，让首页统计与各页面缓存全部失效重读。
    val refreshAfterDemoDataChange: () -> Unit = {
        homeRefreshKey++
        dataRevision++
        entryViewModel.markNeedsRefresh()
        planPreviewGeneration++
        container.printPlanSession.update(null)
    }

    fun resetDemoPresentationState() {
        demoTargets = emptyMap()
        demoConfirmSubmitted = false
        demoSheet = null
        demoBarHeightPx = 0
        demoContentBounds = null
    }

    // 结束或跳过演示：回到首页、覆盖导入启动前快照，成功后才记为已完成。
    // 不展示「演示结束」页，也不弹清理 Toast；还原完成后直接回到空首页。
    val restoreDemoData: () -> Unit = {
        demoTourState = DemoTourState.Restoring
        // 先回首页，释放扫描页、打印页与确认页的临时状态。
        navigator.resetTo(AppRoute.Home)
        demoScope.launch {
            when (val result = demoStore.restore()) {
                is DemoDataResult.Success -> {
                    dailyReviewMinutes = quietly { container.settingsRepository.dailyReviewMinutes() }
                        ?: AppSettings.DEFAULT_DAILY_REVIEW_MINUTES
                    refreshAfterDemoDataChange()
                    demoStore.markCompleted()
                    DemoSheetRenderer.clear(printContext)
                    resetDemoPresentationState()
                    demoPreserveDataNotice = false
                    demoTourState = DemoTourState.Inactive
                }
                is DemoDataResult.Failure ->
                    demoTourState = DemoTourState.RestoreFailed(result.message)
            }
        }
    }

    val demoStop = (demoTourState as? DemoTourState.Running)?.stop

    val setDemoStop: (DemoTourStop) -> Unit = { stop ->
        demoTourState = DemoTourState.Running(stop)
    }

    // 只有文字说明段由说明条上的「下一步」推进；操作段等真实按钮回调。
    val advanceDemoText: () -> Unit = {
        val stop = demoStop
        if (stop?.mode == DemoStopMode.NEXT) {
            stop.next()?.let(setDemoStop)
        }
    }

    // 打印页点「使用演示打印稿」后：用同一份计划渲染已填涂的打印稿，再进入扫描页。
    val prepareDemoSheetAndOpenScan: () -> Unit = {
        val input = container.printPlanSession.latest?.toPrintPlanInput()
        if (input == null) {
            demoTourState = DemoTourState.SheetFailed("计划已失效，请返回重新生成计划。")
        } else {
            demoTourState = DemoTourState.PreparingScanSheet
            demoScope.launch {
                try {
                    val sheet = DemoSheetRenderer.render(printContext, input)
                    demoSheet = DemoScanSheet(
                        image = sheet.image,
                        imageLocation = sheet.imageLocation,
                    )
                    setDemoStop(DemoTourStop.SCAN_SHEET)
                    navigator.resetTo(AppRoute.Scan)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    demoTourState = DemoTourState.SheetFailed(
                        e.message ?: "演示打印稿生成失败，请重试。",
                    )
                }
            }
        }
    }

    val dismissDemoOverlay: () -> Unit = {
        resetDemoPresentationState()
        demoPreserveDataNotice = false
        demoTourState = DemoTourState.Inactive
    }

    // 黑底蒙层的聚焦窗口：欢迎卡不挖洞；确认步覆盖整块内容区，其余聚焦真实控件。
    val demoTourSpotlight: Rect? = when (val state = demoTourState) {
        is DemoTourState.Running -> when (state.stop.target) {
            DemoTarget.WELCOME -> null
            // 提交成功后整页压暗，只保留顶部说明条与「开始使用」，不再留内容区镂空。
            DemoTarget.CONFIRM_CONTENT -> if (demoConfirmSubmitted) {
                null
            } else {
                demoContentBounds
            }
            else -> demoTargets[state.stop.target]
        }
        else -> null
    }

    // 计划页目标要等自动生成完成；尚未就绪时说明条显示等待文案。
    val demoBodyOverride: String? = when {
        demoStop == DemoTourStop.PLAN_PRINT &&
            planPreviewViewModel.state !is PlanPreviewUiState.Content ->
            "正在按知识点生成计划，请稍候…"
        else -> null
    }

    /**
     * 开始一次演示。[replay] 为 true 表示用户从设置里的「使用引导」手动重看：
     * 当前数据库会被快照，演示期间临时使用示例数据，结束后原样还原。
     */
    val startDemo: (Boolean) -> Unit = { replay ->
        demoTourState = DemoTourState.Preparing
        demoPreserveDataNotice = replay
        demoScope.launch {
            val pending = if (demoStore.hasPendingSnapshot()) {
                demoStore.restore()
            } else {
                DemoDataResult.Success
            }
            if (pending is DemoDataResult.Failure) {
                demoTourState = DemoTourState.RestoreFailed(pending.message)
                return@launch
            }
            DemoSheetRenderer.clear(printContext)
            when (val prepared = demoStore.prepare()) {
                is DemoDataResult.Success -> {
                    dailyReviewMinutes = quietly { container.settingsRepository.dailyReviewMinutes() }
                        ?: AppSettings.DEFAULT_DAILY_REVIEW_MINUTES
                    refreshAfterDemoDataChange()
                    resetDemoPresentationState()
                    navigator.resetTo(AppRoute.Home)
                    demoTourState = DemoTourState.Running(DemoTourStop.WELCOME)
                }
                is DemoDataResult.Failure -> {
                    // 演示数据未写入（导入单事务失败会整体回滚），直接跳过演示，避免每次启动重试。
                    demoStore.markCompleted()
                    demoTourState = DemoTourState.PrepareFailed(prepared.message)
                }
            }
        }
    }

    // 首次启动与「使用引导」共用同一套开始逻辑；这里只处理冷启动。
    // 若上次演示在进程被杀时中断，无论引导是否已完成，都先还原用户数据。
    LaunchedEffect(Unit) {
        val hadPending = demoStore.hasPendingSnapshot()
        val pending = if (hadPending) {
            demoStore.restore()
        } else {
            DemoDataResult.Success
        }
        if (pending is DemoDataResult.Failure) {
            demoTourState = DemoTourState.RestoreFailed(pending.message)
            return@LaunchedEffect
        }
        if (hadPending) {
            // 上次演示（包括手动重看）中断：已还原用户数据，立即刷新所有缓存。
            dailyReviewMinutes = quietly { container.settingsRepository.dailyReviewMinutes() }
                ?: AppSettings.DEFAULT_DAILY_REVIEW_MINUTES
            refreshAfterDemoDataChange()
        }
        if (demoStore.isCompleted()) return@LaunchedEffect
        if (current != AppRoute.Home || !importUri.isNullOrBlank()) return@LaunchedEffect
        startDemo(false)
    }

    // 导入成功：提示后让所有展示数据的页面失效。
    // 首页统计与题库立即重读；知识点/来源/进度页的 ViewModel 以 dataRevision 为键重建；
    // 计划预览丢弃缓存并清空打印交接，避免继续打印导入前的旧计划。
    val importState = importViewModel.state
    LaunchedEffect(importState) {
        if (importState is ImportState.Success) {
            showImportSuccessToast(printContext)
            homeRefreshKey++
            dataRevision++
            entryViewModel.markNeedsRefresh()
            planPreviewGeneration++
            container.printPlanSession.update(null)
            if (importFromExternal) {
                // 2026-11-22 用户要求：从系统「用其他方式打开」导入成功后，
                // 让 Toast 先显示，再退出应用（应用内 SAF 导入不退出）。
                delay(800)
                onExternalImportFinished()
            }
            importViewModel.consumeSuccess()
        }
    }

    RecollectTheme {
        // 所有文本框统一处理：Compose 的光标/选区手柄（光标下方的小三角/水滴）是独立窗口里的
        // Popup，只按输入框创建时的位置锚定一次，键盘弹出后不会跟随输入框移动。这里把
        // handleColor 设为透明，整个应用都不再绘制该手柄；光标、选区高亮、输入与拖动仍正常。
        val baseSelectionColors = LocalTextSelectionColors.current
        val selectionColors = remember(baseSelectionColors) {
            TextSelectionColors(
                handleColor = Color.Transparent,
                backgroundColor = baseSelectionColors.backgroundColor,
            )
        }
        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    TopAppBar(
                        title = {
                            // 2026-11-26 用户要求：仅在首页标题「拾知」的右下角以
                            // 小字号、非强调色显示版本号；其余页面标题保持原样。
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = routeTitle(current),
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (current == AppRoute.Home) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "v${container.appVersionName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 1.dp),
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        navigationIcon = {
                            if (canPop) {
                                IconButton(onClick = { navigator.pop() }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_arrow_back),
                                        contentDescription = "返回",
                                    )
                                }
                            }
                        },
                    )
                },
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        // 记录内容区顶部：演示说明条从这里开始摆放。
                        .onGloballyPositioned {
                            if (demoTourActive) demoContentTopPx = it.boundsInRoot().top
                        },
                ) {
                    // 演示期间给顶部说明条留出高度，真实页面从说明条下方开始；
                    // 这样聚焦窗口不会被说明条盖住，确认页也能完整滚动与点击。
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = if (demoTourActive) demoBarHeightDp else 0.dp)
                            .onGloballyPositioned {
                                if (demoTourActive) demoContentBounds = it.boundsInRoot()
                            },
                    ) {
                    when (val route = current) {
                        AppRoute.Home -> HomeScreen(
                            thisWeekPlanStatus = thisWeekPlanStatus,
                            questionCount = questionCount,
                            knowledgeCount = knowledgeCount,
                            overdueCount = overdueCount,
                            dueWithinWeekCount = dueWithinWeekCount,
                            dailyReviewMinutes = dailyReviewMinutes,
                            onSaveDailyReviewMinutes = saveDailyReviewMinutes,
                            onEntry = { navigator.push(AppRoute.Entry) },
                            onCreateQuestion = {
                                // 首次演示：用户点中「录入题目」后，把聚焦切到预填好的表单。
                                if (demoStop == DemoTourStop.HOME_RECORD) {
                                    setDemoStop(DemoTourStop.FORM_NUMBER)
                                }
                                // 从首页进入：保存成功后强制进入题库页（见 QuestionEdit 分支）。
                                navigator.push(AppRoute.QuestionEdit(null, fromHome = true))
                            },
                            onCreatePlan = { openPlanPreview() },
                            onProgress = { navigator.push(AppRoute.Progress) },
                            onScan = { navigator.push(AppRoute.Scan) },
                            onAbout = { navigator.push(AppRoute.About) },
                            // 2026-11-26 用户要求：导入/导出数据库从标题栏移到设置区块；
                            // 导出改为悬浮在首页之上的设置风格弹窗，不进入页面返回栈。
                            onImport = { importPicker.launch(IMPORT_MIME_TYPES) },
                            onExport = { showBackupDialog = true },
                            onGuide = { startDemo(true) },
                            onRecordCardBounds = {
                                if (demoTourActive) reportDemoTarget(DemoTarget.HOME_RECORD, it)
                            },
                            focusRecordCard = demoTourActive &&
                                demoStop == DemoTourStop.HOME_RECORD,
                        )

                        AppRoute.Entry -> {
                            // 导入成功时 markNeedsRefresh 已置位；当前正停留题库页时也立即重读。
                            LaunchedEffect(dataRevision) { entryViewModel.refreshIfNeeded() }
                            EntryScreen(
                                viewModel = entryViewModel,
                                listState = entryListState,
                                onCreatePlan = {
                                    if (demoStop == DemoTourStop.ENTRY_PLAN) {
                                        setDemoStop(DemoTourStop.PLAN_PRINT)
                                    }
                                    openPlanPreview()
                                },
                                onCreateQuestion = { navigator.push(AppRoute.QuestionEdit(null)) },
                                onEditQuestion = { questionId ->
                                    navigator.push(AppRoute.QuestionEdit(questionId))
                                },
                                onPlanButtonBounds = {
                                    if (demoTourActive) {
                                        reportDemoTarget(DemoTarget.ENTRY_PLAN, it)
                                    }
                                },
                            )
                        }

                        is AppRoute.QuestionEdit -> {
                            val viewModel = remember(container, route.questionId) {
                                QuestionEditViewModel(
                                    repository = container.entryRepository,
                                    libraryRepository = container.knowledgePointLibraryRepository,
                                    categoryRepository = container.knowledgeCategoryRepository,
                                    sourceTagRepository = container.sourceTagLibraryRepository,
                                )
                            }
                            // 来源标签库与知识点标签库都在录入页内以全屏窗口打开：
                            // 不离开当前页面，未保存的表单内容不会丢失。
                            val sourceTagViewModel = remember(container) {
                                SourceTagLibraryViewModel(container.sourceTagLibraryRepository)
                            }
                            val knowledgePointViewModel = remember(container) {
                                KnowledgePointLibraryViewModel(
                                    repository = container.knowledgePointLibraryRepository,
                                    categoryRepository = container.knowledgeCategoryRepository,
                                )
                            }
                            QuestionEditScreen(
                                viewModel = viewModel,
                                sourceTagViewModel = sourceTagViewModel,
                                knowledgePointViewModel = knowledgePointViewModel,
                                questionId = route.questionId,
                                prefill = if (demoTourActive && route.questionId == null) {
                                    demoPrefill
                                } else {
                                    null
                                },
                                tourFocus = if (demoTourActive) {
                                    when (demoStop) {
                                        DemoTourStop.FORM_NUMBER -> QuestionEditTourTarget.NUMBER
                                        DemoTourStop.FORM_PAGE_CODE -> QuestionEditTourTarget.PAGE_CODE
                                        DemoTourStop.FORM_MINUTES -> QuestionEditTourTarget.MINUTES
                                        DemoTourStop.FORM_SOURCE -> QuestionEditTourTarget.SOURCE
                                        DemoTourStop.FORM_KNOWLEDGE -> QuestionEditTourTarget.KNOWLEDGE
                                        DemoTourStop.FORM_SAVE -> QuestionEditTourTarget.SAVE
                                        else -> null
                                    }
                                } else {
                                    null
                                },
                                onTourTarget = { target, rect ->
                                    if (demoTourActive) {
                                        reportDemoTarget(
                                            when (target) {
                                                QuestionEditTourTarget.NUMBER -> DemoTarget.FORM_NUMBER
                                                QuestionEditTourTarget.PAGE_CODE -> DemoTarget.FORM_PAGE_CODE
                                                QuestionEditTourTarget.MINUTES -> DemoTarget.FORM_MINUTES
                                                QuestionEditTourTarget.SOURCE -> DemoTarget.FORM_SOURCE
                                                QuestionEditTourTarget.KNOWLEDGE -> DemoTarget.FORM_KNOWLEDGE
                                                QuestionEditTourTarget.SAVE -> DemoTarget.FORM_SAVE
                                            },
                                            rect,
                                        )
                                    }
                                },
                                onSaved = {
                                    // 首次演示：保存后进入题库页，聚焦「预览计划」。
                                    if (demoTourActive && demoStop == DemoTourStop.FORM_SAVE) {
                                        setDemoStop(DemoTourStop.ENTRY_PLAN)
                                    }
                                    // 两种入口都先提示保存成功（绿色对勾 +「保存成功」）。
                                    showSaveSuccessToast(printContext)
                                    // 静默更新题库列表数据，但不重置滚动位置。
                                    entryViewModel.markNeedsRefresh()
                                    if (route.fromHome) {
                                        // 从首页进入：强制跳转题库页（替换当前页，
                                        // 返回键回到首页），而不是关闭当前页。
                                        navigator.replace(AppRoute.Entry)
                                    } else {
                                        // 从题库页进入：保留原有逻辑，关闭当前页即可。
                                        navigator.pop()
                                    }
                                },
                            )
                        }

                        AppRoute.KnowledgePoints -> {
                            // 导入成功后重建并重读标签库，打开该页即显示新数据。
                            val viewModel = remember(container, dataRevision) {
                                KnowledgePointLibraryViewModel(
                                    repository = container.knowledgePointLibraryRepository,
                                    categoryRepository = container.knowledgeCategoryRepository,
                                )
                            }
                            KnowledgePointLibraryScreen(viewModel)
                        }

                        AppRoute.SourceTags -> {
                            val viewModel = remember(container, dataRevision) {
                                SourceTagLibraryViewModel(container.sourceTagLibraryRepository)
                            }
                            SourceTagLibraryScreen(viewModel)
                        }

                        AppRoute.Progress -> {
                            val viewModel = remember(container, dataRevision) {
                                ReviewProgressViewModel(
                                    repository = container.reviewProgressRepository,
                                    clock = container.clock,
                                )
                            }
                            ReviewProgressScreen(viewModel)
                        }

                        AppRoute.PlanPreview -> PlanPreviewScreen(
                            viewModel = planPreviewViewModel,
                            onPrint = {
                                if (demoStop == DemoTourStop.PLAN_PRINT) {
                                    setDemoStop(DemoTourStop.PRINT_DEMO)
                                }
                                navigator.push(AppRoute.Print)
                            },
                            onPrintButtonBounds = {
                                if (demoTourActive) {
                                    reportDemoTarget(DemoTarget.PLAN_PRINT, it)
                                }
                            },
                        )

                        AppRoute.Print -> {
                            val input = container.printPlanSession.latest?.toPrintPlanInput()
                            val viewModel = remember(container, input, printLauncher) {
                                PrintPlanViewModel(
                                    input = input,
                                    launcher = printLauncher,
                                    snapshotWriter = container.printSnapshotWriter,
                                    pdfSharer = container.planPdfSharer,
                                )
                            }
                            PrintPlanScreen(
                                viewModel = viewModel,
                                onPrintCompleted = {
                                    showPrintCompleteToast(printContext)
                                    navigator.resetTo(AppRoute.Home)
                                },
                                demoMode = demoTourActive,
                                onDemoPrint = prepareDemoSheetAndOpenScan,
                                onDemoPrintButtonBounds = {
                                    if (demoTourActive) {
                                        reportDemoTarget(DemoTarget.PRINT_DEMO, it)
                                    }
                                },
                            )
                        }

                        AppRoute.Scan -> {
                            val viewModel = remember(container) {
                                ScanViewModel(
                                    runner = container.scanRunner,
                                    manualEntry = container.scanManualEntry,
                                    repository = container.scanSessionRepository,
                                    clock = container.clock,
                                )
                            }
                            ScanScreen(
                                viewModel = viewModel,
                                onOpenConfirm = { sessionId ->
                                    if (demoStop == DemoTourStop.SCAN_CONFIRM) {
                                        setDemoStop(DemoTourStop.CONFIRM)
                                    }
                                    navigator.push(AppRoute.Confirm(sessionId))
                                },
                                // 演示使用预生成的打印稿，不打开相机，也不请求权限。
                                autoRequestCamera = !demoTourActive,
                                demoSheet = if (demoTourActive) demoSheet else null,
                                demoScanButtonBounds = {
                                    if (demoTourActive) {
                                        reportDemoTarget(DemoTarget.SCAN_SHEET, it)
                                    }
                                },
                                confirmButtonBounds = {
                                    if (demoTourActive) {
                                        reportDemoTarget(DemoTarget.SCAN_CONFIRM, it)
                                    }
                                },
                                onSessionReady = {
                                    if (demoStop == DemoTourStop.SCAN_SHEET) {
                                        setDemoStop(DemoTourStop.SCAN_CONFIRM)
                                    }
                                },
                            )
                        }

                        AppRoute.About -> AboutScreen(
                            appName = "拾知",
                            versionName = container.appVersionName,
                            onOpenLicense = { navigator.push(AppRoute.License) },
                        )

                        AppRoute.License -> LicenseScreen(texts = licenseTexts)

                        is AppRoute.Confirm -> {
                            val viewModel = remember(container, route.sessionId) {
                                ConfirmViewModel(
                                    sessionId = route.sessionId,
                                    scanSessionRepository = container.scanSessionRepository,
                                    feedbackRepository = container.feedbackRepository,
                                    knowledgePointRepository = container.knowledgePointRepository,
                                    questionRepository = container.questionRepository,
                                    submitter = ConfirmSubmitter { sessionId, decisions ->
                                        container.applyFeedbackUseCase.execute(sessionId, decisions)
                                    },
                                    clock = container.clock,
                                )
                            }
                            ConfirmScreen(
                                viewModel = viewModel,
                                onConfirmed = {
                                    if (demoTourActive) {
                                        // 演示：停留在确认页，整页压暗并让「开始使用」变为可用；
                                        // 用户点击后才还原数据库。
                                        demoConfirmSubmitted = true
                                    } else {
                                        showConfirmCompleteToast(printContext)
                                        navigator.resetTo(AppRoute.Home)
                                    }
                                },
                            )
                        }

                        else -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(routeTitle(route))
                        }
                    }
                    }
                }
            }

            // 覆盖确认与失败提示叠加在任意当前页之上；成功不弹窗，只显示带对勾的 Toast。
            ImportDialogs(
                state = importViewModel.state,
                onConfirm = { importScope.launch { importViewModel.confirm() } },
                onCancel = { importViewModel.cancel() },
                onDismissError = { importViewModel.dismissError() },
            )

            // 2026-11-26 用户要求：导出数据库改为悬浮在主页面上方的设置风格弹窗。
            // ViewModel 只在弹窗显示期间存在，关闭后丢弃，重新打开时从空闲状态开始。
            if (showBackupDialog) {
                val backupViewModel = remember {
                    BackupViewModel(
                        exporter = container.databaseExporter,
                        sharer = container.backupSharer,
                    )
                }
                BackupDialog(
                    viewModel = backupViewModel,
                    onDismiss = { showBackupDialog = false },
                )
            }

            // 首次复习演示的黑底聚焦层：作为根级最后一个子节点叠加在所有页面之上。
            // 蒙层只压暗高亮区域之外；用户亲自点击真实控件，操作成功后由状态机推进。
            DemoTourOverlay(
                state = demoTourState,
                spotlight = demoTourSpotlight,
                contentTopPx = demoContentTopPx,
                canFinish = demoConfirmSubmitted,
                bodyOverride = demoBodyOverride,
                preserveDataNotice = demoPreserveDataNotice,
                onNext = advanceDemoText,
                onSkip = restoreDemoData,
                onUse = { if (demoConfirmSubmitted) restoreDemoData() },
                onRetryRestore = restoreDemoData,
                onRetrySheet = prepareDemoSheetAndOpenScan,
                onDismiss = dismissDemoOverlay,
                onBarHeightChanged = { demoBarHeightPx = it },
            )
        }
    }
}

/** 导入文件选择的 MIME 过滤：json 优先，同时容纳被文件管理器标成文本/二进制的 json。 */
private val IMPORT_MIME_TYPES = arrayOf(
    "application/json",
    "text/plain",
    "application/octet-stream",
)

/**
 * 首页仪表盘：复习流程 → 常用操作 → 学习概览 → 设置。
 *
 * 流程条固定在最上方，流程图下方说明当前该做什么；常用操作把当前环节对应的入口
 * 前置并高亮，其余入口保持固定顺序。统计读取失败时显示“—”，不影响任何入口。
 */
@Composable
private fun HomeScreen(
    thisWeekPlanStatus: PlanStatus?,
    questionCount: Int?,
    knowledgeCount: Int?,
    overdueCount: Int?,
    dueWithinWeekCount: Int?,
    dailyReviewMinutes: Int,
    onSaveDailyReviewMinutes: (Int) -> Unit,
    onEntry: () -> Unit,
    onCreateQuestion: () -> Unit,
    onCreatePlan: () -> Unit,
    onProgress: () -> Unit,
    onScan: () -> Unit,
    onAbout: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    /** 设置面板的「使用引导」：重新播放演示，并在结束后恢复当前用户数据。 */
    onGuide: () -> Unit,
    /** 首次演示用：把「录入题目」快捷卡的位置交给黑底聚焦层。 */
    onRecordCardBounds: (Rect) -> Unit = {},
    /** 首次演示用：把「录入题目」所在快捷卡区滚动到说明条下方的可见区域。 */
    focusRecordCard: Boolean = false,
) {
    val listState = rememberLazyListState()
    // 快捷卡区是 LazyColumn 的第 3 个 item（概览、流程、快捷卡）。
    // 演示开始滚到快捷卡；演示结束回到顶部，避免停留在一个为演示滚动的中间位置。
    LaunchedEffect(focusRecordCard) {
        if (focusRecordCard) {
            listState.animateScrollToItem(2)
        } else {
            listState.scrollToItem(0)
        }
    }
    val stage = currentReviewStage(questionCount, thisWeekPlanStatus)
    val highlight = homeHighlight(stage)
    val cycleComplete = thisWeekPlanStatus == PlanStatus.COMPLETED
    var showDailyMinutesDialog by remember { mutableStateOf(false) }
    if (showDailyMinutesDialog) {
        DailyReviewMinutesDialog(
            current = dailyReviewMinutes,
            onDismiss = { showDailyMinutesDialog = false },
            onConfirm = { minutes ->
                showDailyMinutesDialog = false
                onSaveDailyReviewMinutes(minutes)
            },
        )
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        // 首页按「概览入口 → 流程 → 快捷操作 → 统计 → 设置」组织。
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            OverviewCard(
                onProgress = onProgress,
                questionCount = questionCount,
                knowledgeCount = knowledgeCount,
                overdueCount = overdueCount,
                dueWithinWeekCount = dueWithinWeekCount,
            )
        }
        item {
            FlowGuideCard(
                currentStage = stage,
                cycleComplete = cycleComplete,
                guidance = stageGuidance(stage, cycleComplete),
            )
        }
        item {
            // 快捷卡片直接跟在流程条下方，不再显示「常用操作」区块标题。
            QuickActionsGrid(
                highlight = highlight,
                onEntry = onEntry,
                onCreateQuestion = onCreateQuestion,
                onCreatePlan = onCreatePlan,
                onScan = onScan,
                onRecordCardBounds = onRecordCardBounds,
            )
        }
        item {
            HomeSection(title = "设置") {
                HomeSettingsPanel(
                    dailyReviewMinutes = dailyReviewMinutes,
                    onEditDailyMinutes = { showDailyMinutesDialog = true },
                    onAbout = onAbout,
                    onImport = onImport,
                    onExport = onExport,
                    onGuide = onGuide,
                )
            }
        }
    }
}

/**
 * 页面顶部的学习概览卡：与复习流程卡同一套表面样式，
 * 左侧「学习概览」、右侧透明的「查看复习进度」按钮，字号与流程卡标题行一致；
 * 四项统计（题目 / 知识点 / 已逾期 / 7 天内）并入卡内，成为首页最上方的信息区。
 */
@Composable
private fun OverviewCard(
    onProgress: () -> Unit,
    questionCount: Int?,
    knowledgeCount: Int?,
    overdueCount: Int?,
    dueWithinWeekCount: Int?,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLow,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            // 与复习流程卡一致：标题行 20dp 高，标题与下方内容间距 14dp。
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "学习概览",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                ProgressHeaderAction(onClick = onProgress)
            }
            HomeStatsRow(
                questionCount = questionCount,
                knowledgeCount = knowledgeCount,
                overdueCount = overdueCount,
                dueWithinWeekCount = dueWithinWeekCount,
            )
        }
    }
}

/** 首页区块标题：标题左对齐，右侧可放一个透明背景的文字按钮。 */
@Composable
private fun HomeSection(
    title: String,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            action?.invoke()
        }
        content()
    }
}

/**
 * 「学习概览」标题右侧的进度入口：透明背景、文字贴齐区块右缘，
 * 与下方统计卡片的右边缘对齐。
 */
@Composable
private fun ProgressHeaderAction(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Text(
            text = "查看复习进度",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

/**
 * 复习流程条：把「录入 → 生成 → 打印 → 扫描 → 确认」画成一条可读的路径，
 * 已完成与当前步骤用品牌色区分，帮助第一次使用的学生知道下一步做什么。
 */
@Composable
private fun FlowGuideCard(
    currentStage: ReviewStage,
    cycleComplete: Boolean,
    guidance: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val hint = if (cycleComplete) {
        "本轮已完成"
    } else {
        "第 ${currentStage.ordinal + 1} / ${ReviewStage.entries.size} 步"
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLow,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "复习流程",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primary,
                )
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                // 连接线画在圆点后方，让五个环节读起来是一条路径而不是五个按钮。
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(0.84f)
                        .padding(top = 13.dp)
                        .height(1.dp)
                        .background(colors.outlineVariant),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val currentIndex = currentStage.ordinal
                    ReviewStage.entries.forEachIndexed { index, stage ->
                        val active = !cycleComplete && index == currentIndex
                        val done = cycleComplete || index < currentIndex
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            active -> colors.primary
                                            done -> colors.primaryContainer
                                            else -> colors.surfaceVariant
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = (index + 1).toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when {
                                        active -> colors.onPrimary
                                        done -> colors.onPrimaryContainer
                                        else -> colors.onSurfaceVariant
                                    },
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stage.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) colors.onSurface else colors.onSurfaceVariant,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = colors.outlineVariant)
            Text(
                text = guidance,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/**
 * 快捷操作网格：当前环节对应的入口移到首位并高亮，其余入口保持固定顺序。
 *
 * 同一时刻只有一个入口使用主色容器，避免首页出现多个视觉重点。
 */
@Composable
private fun QuickActionsGrid(
    highlight: HomeHighlight,
    onEntry: () -> Unit,
    onCreateQuestion: () -> Unit,
    onCreatePlan: () -> Unit,
    onScan: () -> Unit,
    onRecordCardBounds: (Rect) -> Unit,
) {
    val actions = orderedHomeActions(highlight)
    val guided = guidedHomeAction(highlight)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        actions.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEach { action ->
                    val (iconRes, onClick) = when (action) {
                        HomeAction.RECORD -> R.drawable.ic_home_entry to onCreateQuestion
                        HomeAction.LIBRARY -> R.drawable.ic_home_library to onEntry
                        HomeAction.PLAN -> R.drawable.ic_home_plan to onCreatePlan
                        HomeAction.SCAN -> R.drawable.ic_home_scan to onScan
                    }
                    QuickActionCard(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(
                                if (action == HomeAction.RECORD) {
                                    Modifier.onGloballyPositioned {
                                        onRecordCardBounds(it.boundsInRoot())
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                        iconRes = iconRes,
                        title = action.title,
                        highlighted = action == guided,
                        onClick = onClick,
                    )
                }
            }
        }
    }
}

/**
 * 设置区：上方是每日复习时长设置行；下方并排三个等宽入口（导入数据库 / 导出数据库 / 关于），
 * 每个入口各占原设置面板总宽度的三分之一。版本号显示在首页标题旁，此处不再重复。
 */
@Composable
private fun HomeSettingsPanel(
    dailyReviewMinutes: Int,
    onEditDailyMinutes: () -> Unit,
    onAbout: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onGuide: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLow,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column {
            HomeSettingsRow(
                iconRes = R.drawable.ic_clock,
                title = "每日复习时长",
                subtitle = "当前 $dailyReviewMinutes 分钟",
                onClick = onEditDailyMinutes,
            )
            HorizontalDivider(color = colors.outlineVariant)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                HomeSettingsAction(
                    modifier = Modifier.weight(1f),
                    // 2026-11-22 用户要求：导入与导出的图标互换
                    // （导入用原导出图形、导出用原导入图形），移动位置后保持。
                    iconRes = R.drawable.ic_export,
                    title = "导入数据库",
                    onClick = onImport,
                )
                HomeSettingsAction(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_import,
                    title = "导出数据库",
                    onClick = onExport,
                )
                HomeSettingsAction(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_guide,
                    title = "使用引导",
                    onClick = onGuide,
                )
                HomeSettingsAction(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_info,
                    title = "关于",
                    onClick = onAbout,
                )
            }
        }
    }
}

/** 设置面板底部的等宽入口：图标在上、文字在下，整体居中。 */
@Composable
private fun HomeSettingsAction(
    modifier: Modifier,
    iconRes: Int,
    title: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            // 四个入口等分时可用宽度更窄，不设水平内边距，保证 1.3 倍字号下标签完整。
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(MaterialTheme.shapes.small)
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HomeSettingsRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(MaterialTheme.shapes.small)
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * 修改每日复习时长的悬浮窗。
 *
 * 输入框只接受数字并限制 1～240 分钟；文本框下方简要说明该设置的作用与生效时机。
 */
@Composable
private fun DailyReviewMinutesDialog(
    current: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by remember(current) { mutableStateOf(current.toString()) }
    val value = text.toIntOrNull()
    val valid = value != null &&
        value in AppSettings.MIN_DAILY_REVIEW_MINUTES..AppSettings.MAX_DAILY_REVIEW_MINUTES
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置每日复习时长") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { input -> text = input.filter(Char::isDigit).take(3) },
                    singleLine = true,
                    label = { Text("每日复习时长（分钟）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = text.isNotEmpty() && !valid,
                    supportingText = if (text.isNotEmpty() && !valid) {
                        { Text("请输入 ${AppSettings.MIN_DAILY_REVIEW_MINUTES}～${AppSettings.MAX_DAILY_REVIEW_MINUTES} 之间的分钟数") }
                    } else {
                        null
                    },
                )
                Text(
                    text = "决定每天能安排多少分钟的复习内容。当天需复习题目总耗时超出复习时长时会被推迟到后续日期。" +
                        "修改后对下一份生成的计划生效，已生成或已打印的计划不变。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { value?.let(onConfirm) },
                enabled = valid,
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

/** 一行四项概览；数据未加载或读取失败时显示“—”。 */
@Composable
private fun HomeStatsRow(
    questionCount: Int?,
    knowledgeCount: Int?,
    overdueCount: Int?,
    dueWithinWeekCount: Int?,
) {
    val colors = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth()) {
        HomeStat(
            label = "题目",
            value = questionCount,
            valueColor = colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        StatDivider()
        HomeStat(
            label = "知识点",
            value = knowledgeCount,
            valueColor = colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        StatDivider()
        HomeStat(
            label = "已逾期",
            value = overdueCount,
            valueColor = if ((overdueCount ?: 0) > 0) colors.error else colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        StatDivider()
        HomeStat(
            label = "7 天内",
            value = dueWithinWeekCount,
            valueColor = colors.primary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(30.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

@Composable
private fun HomeStat(
    label: String,
    value: Int?,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value?.toString() ?: "—",
            style = MaterialTheme.typography.headlineSmall,
            color = valueColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 快捷入口卡片：左侧图标块，右侧纵向右对齐显示标题与可选的「下一步」标签。
 *
 * 高度由内容决定，同一行的两张卡片用 `IntrinsicSize.Min` 对齐；
 * 高亮时整卡换成主色容器、图标块填主色，并显示「下一步」；
 * 未高亮的入口保持中性，让首页只有一个视觉重点。
 */
@Composable
private fun QuickActionCard(
    iconRes: Int,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val container = if (highlighted) colors.primaryContainer else colors.surfaceContainerLow
    val borderColor = if (highlighted) {
        colors.primary.copy(alpha = 0.18f)
    } else {
        colors.outlineVariant
    }
    val titleColor = if (highlighted) colors.onPrimaryContainer else colors.onSurface
    val iconBackground = if (highlighted) colors.primary else colors.surfaceVariant
    val iconTint = if (highlighted) colors.onPrimary else colors.onSurfaceVariant
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(iconBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (highlighted) {
                    Text(
                        text = "下一步",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primary,
                    )
                }
            }
        }
    }
}

/** 首页概览的静默读取：统计失败不影响入口；协程取消照常向上传播。 */
private suspend fun <T> quietly(block: suspend () -> T): T? =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

/** app 层把计划层的 `PrintJob` 映射为打印 feature 的输入，避免 feature 之间直接依赖。 */
private fun PrintJob.toPrintPlanInput(): PrintPlanInput = PrintPlanInput(
    planId = planId,
    pages = pages,
    questions = questions,
    noteTexts = notes.mapValues { (_, items) -> items.map { it.text } },
    template = template,
)

private fun routeTitle(route: AppRoute): String = when (route) {
    AppRoute.Home -> "拾知"
    AppRoute.Entry -> "题库"
    is AppRoute.QuestionEdit -> if (route.questionId == null) "录入题目" else "编辑题目"
    AppRoute.KnowledgePoints -> "知识点"
    AppRoute.SourceTags -> "来源标签"
    AppRoute.Progress -> "复习进度"
    AppRoute.PlanPreview -> "计划预览"
    AppRoute.Print -> "打印"
    AppRoute.Scan -> "扫描"
    is AppRoute.Confirm -> "确认"
    AppRoute.Settings -> "设置"
    AppRoute.About -> "关于"
    AppRoute.License -> "开源许可"
}

/** 取承载 Compose 的 Activity：`LocalView.context` 可能被主题等逐层包装，需要解包。 */
private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}
