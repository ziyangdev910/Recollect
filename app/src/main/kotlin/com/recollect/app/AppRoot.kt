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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.input.pointer.pointerInput
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
import com.recollect.app.ui.RecollectDialog
import com.recollect.app.ui.RecollectDialogAction
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
import com.recollect.feature.planning.PlanningSettings
import com.recollect.feature.planning.PrintJob
import com.recollect.feature.progress.ReviewProgressAggregator
import com.recollect.feature.progress.ReviewProgressScreen
import com.recollect.feature.progress.ReviewProgressViewModel
import com.recollect.feature.print.PrintPlanInput
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

    // 首页「调整顺序」模式：顶栏右侧改为「完成」，返回键先退出编辑而不是结束应用。
    var homeEditing by remember(container) { mutableStateOf(false) }

    // 根路由时不拦截系统返回，交给 Activity 默认结束进程级页面。
    BackHandler(enabled = canPop) {
        navigator.pop()
    }

    // 首页调整顺序时，返回键先退出编辑模式。
    BackHandler(enabled = current == AppRoute.Home && homeEditing) {
        homeEditing = false
    }
    // 离开首页去其他页面时退出编辑；但进入「编辑封面 / 添加分类」页再返回首页时
    // 保留原先的抖动、可拖动编辑态（2026-11-27 用户要求）。
    LaunchedEffect(current) {
        if (current != AppRoute.Home && current !is AppRoute.CategoryCreate) {
            homeEditing = false
        }
    }

    // 演示期间吞掉返回键，避免用户误退出自动漫游（跳过着陆点由卡片按钮提供）。
    BackHandler(enabled = demoTourActive) { /* 只拦截，不处理 */ }

    // R-03：打印稿生成/分享封装在端口实现中；ViewModel 只依赖 PlanDocumentGateway。
    val printContext = LocalContext.current
    // 开源许可证全文打包在 assets；只在首次组合时读取一次。
    val licenseTexts = remember(printContext) { readLicenseTexts(printContext) }

    // 导入会整体替换数据：homeRefreshKey 触发首页统计重读，dataRevision 让题库之外
    // 的页面级 ViewModel（知识点、来源、进度）在下次打开（或当前停留）时重建并重读。
    var homeRefreshKey by remember(container) { mutableStateOf(0) }
    var dataRevision by remember(container) { mutableStateOf(0) }
    // 2026-11-26 用户要求：导出数据库改为悬浮在首页之上的弹窗，用本地状态控制显示，
    // 不进入路由返回栈；关闭（返回图标/系统返回）即销毁本次 ViewModel 状态。
    var showBackupDialog by remember(container) { mutableStateOf(false) }
    // 2026-11-27 用户要求：设置入口收进首页顶栏「更多」菜单，每日复习时长弹窗在此层管理。
    var showDailyMinutesDialog by remember(container) { mutableStateOf(false) }

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
            // 默认日期范围由预览页读取本周计划状态自动决定：本周已完成确认则填入下周。
            initialStartDate = null,
            initialEndDate = null,
            settingsFactory = { start, end, _ ->
                PlanningSettings(planStartDate = start, planEndDate = end)
                    .withDailyMinutes(dailyReviewMinutes)
            },
            currentQuestionBank = {
                container.entryRepository.summaries()
                    .associate { it.questionId to it.updatedAt }
            },
            onPrintJobAvailable = { container.printPlanSession.update(it) },
            documentGateway = container.planDocumentGateway,
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

    // 首页错题本：全部错题数量 + 每个知识点分类的题目数（2026-11-27 用户要求）。
    var homeCategories by remember(container) { mutableStateOf<List<HomeCategory>?>(null) }

    // 首页每次重新获得焦点（回到该路由或应用从后台恢复）都重读，录入/导入后的数量才会立即反映。
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
        val summaries = quietly { container.entryRepository.summaries() }
        val categories = quietly { container.knowledgeCategoryRepository.categories() }
        if (categories != null) {
            homeCategories = categories.map { category ->
                HomeCategory(
                    id = category.id,
                    name = category.name,
                    questionCount = summaries?.count { category.id in it.categoryIds } ?: 0,
                    coverTone = category.coverTone,
                    coverIcon = category.coverIcon,
                )
            }
        }
    }
    // 每次从首页/题库进入预览都重建 ViewModel：默认日期范围由预览页读取本周计划状态决定。
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

    // 生成页点「使用演示打印稿」后：记录打印快照，用同一份计划渲染已填涂的打印稿，再进入扫描页。
    val prepareDemoSheetAndOpenScan: () -> Unit = {
        val job = container.printPlanSession.latest
        val input = job?.toPrintPlanInput()
        if (job == null || input == null) {
            demoTourState = DemoTourState.SheetFailed("计划已失效，请返回重新生成计划。")
        } else {
            demoTourState = DemoTourState.PreparingScanSheet
            demoScope.launch {
                try {
                    if (!container.planDocumentGateway.recordDemoPrinted(job)) {
                        demoTourState = DemoTourState.SheetFailed(
                            "演示打印快照写入失败，请返回重新生成计划。",
                        )
                        return@launch
                    }
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
                // 扫描页是全屏深色取景，状态栏区域也使用同一底色。
                containerColor = if (current is AppRoute.Scan) {
                    Color.Black
                } else {
                    MaterialTheme.colorScheme.background
                },
                topBar = {
                    // 扫描页按 reference/CaptureWindow.png 做全屏取景，不显示应用标题栏。
                    // 「添加分类 / 编辑封面」已改为盖在整页之上的全屏新页面，这里保留首页顶栏，
                    // 避免顶栏收起/恢复造成内容位移。
                    if (current !is AppRoute.Scan) {
                        TopAppBar(
                            title = {
                                // 2026-11-26 用户要求：仅在首页标题「拾知」的右下角以
                                // 小字号、非强调色显示版本号；其余页面标题保持原样。
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = routeTitle(route = current),
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
                            actions = {
                                if (current == AppRoute.Home) {
                                    if (homeEditing) {
                                        TextButton(onClick = { homeEditing = false }) {
                                            Text("完成")
                                        }
                                    } else {
                                        HomeOverflowMenu(
                                            dailyReviewMinutes = dailyReviewMinutes,
                                            onProgress = { navigator.push(AppRoute.Progress) },
                                            onKnowledge = { navigator.push(AppRoute.KnowledgePoints) },
                                            onDailyMinutes = { showDailyMinutesDialog = true },
                                            onImport = { importPicker.launch(IMPORT_MIME_TYPES) },
                                            onExport = { showBackupDialog = true },
                                            onGuide = { startDemo(true) },
                                            onAbout = { navigator.push(AppRoute.About) },
                                        )
                                    }
                                }
                            },
                        )
                    }
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
                    // 首页在「添加分类 / 编辑封面」页打开期间保持组合：返回时原样露出，
                    // 抖动动画与编辑态不会重建，也不会出现一帧退回展示态。
                    if (current == AppRoute.Home || current is AppRoute.CategoryCreate) {
                        HomeScreen(
                            categories = homeCategories,
                            editing = homeEditing,
                            onEditingChange = { homeEditing = it },
                            onReorder = { ids ->
                                // 先在内存中按新顺序更新，退出编辑时不会闪回旧顺序；
                                // 再写库并触发首页重读。
                                homeCategories = homeCategories?.let { categories ->
                                    val byId = categories.associateBy { it.id }
                                    ids.mapNotNull { byId[it] }
                                }
                                settingsScope.launch {
                                    quietly {
                                        container.knowledgeCategoryRepository.reorder(ids)
                                    }
                                    homeRefreshKey++
                                }
                            },
                            onEditCover = { category, cover ->
                                navigator.push(
                                    AppRoute.CategoryCreate(
                                        categoryId = category.id,
                                        name = category.name,
                                        coverTone = cover.toneIndex,
                                        coverIcon = cover.iconKey,
                                    ),
                                )
                            },
                            onDeleteCategory = { category ->
                                // 分类下无题目时由首页确认浮窗放行；仓储仍会拒绝仍有知识点的分类，
                                // 失败时保持首页数据并提示原因。
                                settingsScope.launch {
                                    try {
                                        container.knowledgeCategoryRepository.delete(category.id)
                                        homeCategories = homeCategories?.filterNot { it.id == category.id }
                                        homeRefreshKey++
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        showErrorToast(
                                            printContext,
                                            e.message ?: "删除分类失败，请重试。",
                                        )
                                    }
                                }
                            },
                            onOpenNotebook = { categoryId, name ->
                                navigator.push(
                                    AppRoute.Entry(categoryId = categoryId, categoryName = name),
                                )
                            },
                            onAddCategory = { navigator.push(AppRoute.CategoryCreate()) },
                            onCreateQuestion = {
                                // 首次演示：用户点中「录入」后，把聚焦切到预填好的表单。
                                if (demoStop == DemoTourStop.HOME_RECORD) {
                                    setDemoStop(DemoTourStop.FORM_NUMBER)
                                }
                                // 从首页进入：保存成功后强制进入题库页（见 QuestionEdit 分支）。
                                navigator.push(AppRoute.QuestionEdit(null, fromHome = true))
                            },
                            onCreatePlan = { openPlanPreview() },
                            onScan = { navigator.push(AppRoute.Scan) },
                            onRecordButtonBounds = {
                                if (demoTourActive) reportDemoTarget(DemoTarget.HOME_RECORD, it)
                            },
                            focusRecordButton = demoTourActive &&
                                demoStop == DemoTourStop.HOME_RECORD,
                        )
                    }

                    when (val route = current) {
                        AppRoute.Home -> Unit

                        is AppRoute.Entry -> {
                            // 导入成功时 markNeedsRefresh 已置位；当前正停留题库页时也立即重读。
                            LaunchedEffect(dataRevision) { entryViewModel.refreshIfNeeded() }
                            EntryScreen(
                                viewModel = entryViewModel,
                                listState = entryListState,
                                categoryId = route.categoryId,
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
                                        navigator.replace(AppRoute.Entry())
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

                        is AppRoute.CategoryCreate -> Unit

                        AppRoute.PlanPreview -> PlanPreviewScreen(
                            viewModel = planPreviewViewModel,
                            onShareCompleted = {
                                showPrintCompleteToast(printContext)
                                navigator.resetTo(AppRoute.Home)
                            },
                            // 首次演示不打开系统分享，用「使用演示打印稿」继续回读。
                            demoMode = demoTourActive,
                            onDemoPrint = prepareDemoSheetAndOpenScan,
                            onPrimaryActionBounds = {
                                if (demoTourActive) {
                                    reportDemoTarget(DemoTarget.PLAN_PRINT, it)
                                }
                            },
                        )

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
                                onClose = { navigator.pop() },
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

            // 「添加分类 / 编辑封面」是盖在整页之上的全屏新页面：不占用 Scaffold 的内容内边距，
            // 因此进入时顶栏不会收起、内容不会上移；首页仍在下方保持组合，返回时原样露出。
            (current as? AppRoute.CategoryCreate)?.let { categoryRoute ->
                val editTarget = categoryRoute.categoryId?.let { id ->
                    CategoryEditTarget(
                        categoryId = id,
                        name = categoryRoute.name.orEmpty(),
                        coverTone = categoryRoute.coverTone ?: 0,
                        coverIcon = categoryRoute.coverIcon ?: "book",
                    )
                }
                val viewModel = remember(container, categoryRoute) {
                    CategoryCreateViewModel(
                        repository = container.knowledgeCategoryRepository,
                        editTarget = editTarget,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        // 挡住下方首页：空点击也会消费，空白区域不会打开卡片或被拖动。
                        .pointerInput(Unit) { detectTapGestures { } },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                    ) {
                        CategoryCreateScreen(
                            viewModel = viewModel,
                            onClose = { navigator.pop() },
                            onCreated = {
                                // 分类创建/保存后首页重新读取分类、封面与顺序。
                                homeRefreshKey++
                                navigator.pop()
                            },
                        )
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

            // 顶栏「更多」菜单中的每日复习时长设置。
            if (showDailyMinutesDialog) {
                DailyReviewMinutesDialog(
                    current = dailyReviewMinutes,
                    onDismiss = { showDailyMinutesDialog = false },
                    onConfirm = { minutes ->
                        showDailyMinutesDialog = false
                        saveDailyReviewMinutes(minutes)
                    },
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
 * 首页顶栏「更多」菜单（2026-11-27 用户要求）：收纳复习进度、知识点管理、每日复习时长、
 * 导入/导出数据库、使用引导与关于，主界面只保留错题本网格与底部胶囊栏。
 */
@Composable
private fun HomeOverflowMenu(
    dailyReviewMinutes: Int,
    onProgress: () -> Unit,
    onKnowledge: () -> Unit,
    onDailyMinutes: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onGuide: () -> Unit,
    onAbout: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = "更多",
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("复习进度") },
                onClick = {
                    expanded = false
                    onProgress()
                },
            )
            DropdownMenuItem(
                text = { Text("知识点与分类") },
                onClick = {
                    expanded = false
                    onKnowledge()
                },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("每日复习时长（$dailyReviewMinutes 分钟）") },
                onClick = {
                    expanded = false
                    onDailyMinutes()
                },
            )
            DropdownMenuItem(
                text = { Text("导入数据库") },
                onClick = {
                    expanded = false
                    onImport()
                },
            )
            DropdownMenuItem(
                text = { Text("导出数据库") },
                onClick = {
                    expanded = false
                    onExport()
                },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("使用引导") },
                onClick = {
                    expanded = false
                    onGuide()
                },
            )
            DropdownMenuItem(
                text = { Text("关于") },
                onClick = {
                    expanded = false
                    onAbout()
                },
            )
        }
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
    RecollectDialog(
        title = "设置每日复习时长",
        onDismissRequest = onDismiss,
        scrollable = true,
        actions = {
            RecollectDialogAction(text = "取消", onClick = onDismiss)
            RecollectDialogAction(
                text = "保存",
                enabled = valid,
                onClick = { value?.let(onConfirm) },
            )
        },
    ) {
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

private fun routeTitle(route: AppRoute): String = when (route) {
    AppRoute.Home -> "拾知"
    is AppRoute.Entry -> route.categoryName ?: "全部错题"
    is AppRoute.QuestionEdit -> if (route.questionId == null) "录入题目" else "编辑题目"
    AppRoute.KnowledgePoints -> "知识点"
    AppRoute.SourceTags -> "来源标签"
    AppRoute.Progress -> "复习进度"
    is AppRoute.CategoryCreate -> if (route.categoryId == null) "添加分类" else "编辑封面"
    AppRoute.PlanPreview -> "打印计划"
    AppRoute.Scan -> "扫描回读"
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
