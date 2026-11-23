package com.recollect.app

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.recollect.app.navigation.AppNavigator
import com.recollect.app.navigation.AppRoute
import com.recollect.data.room.entity.AppSettings
import com.recollect.data.room.repository.PlanStatus
import com.recollect.feature.backup.BackupScreen
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
import com.recollect.feature.entry.QuestionEditScreen
import com.recollect.feature.entry.QuestionEditViewModel
import com.recollect.feature.entry.SourceTagLibraryScreen
import com.recollect.feature.entry.SourceTagLibraryViewModel
import com.recollect.feature.planning.PlanPreviewScreen
import com.recollect.feature.planning.PlanPreviewViewModel
import com.recollect.feature.planning.PlanWeek
import com.recollect.feature.planning.PrintJob
import com.recollect.feature.planning.currentWeekSettings
import com.recollect.feature.planning.nextWeekSettings
import com.recollect.feature.planning.settingsFor
import com.recollect.feature.progress.ReviewProgressAggregator
import com.recollect.feature.progress.ReviewProgressScreen
import com.recollect.feature.progress.ReviewProgressViewModel
import com.recollect.feature.print.AndroidPrintLauncher
import com.recollect.feature.print.PrintPlanInput
import com.recollect.feature.print.PrintPlanScreen
import com.recollect.feature.print.PrintPlanViewModel
import com.recollect.scan.ui.ScanScreen
import com.recollect.scan.ui.ScanViewModel
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

    // 根路由时不拦截系统返回，交给 Activity 默认结束进程级页面。
    BackHandler(enabled = canPop) {
        navigator.pop()
    }

    // R-03：PrintManager 封装在 launcher 中；ViewModel 只依赖 PrintLauncher 端口。
    val printContext = LocalContext.current
    val printLauncher = remember(printContext, container) {
        AndroidPrintLauncher(
            context = printContext,
            scope = container.applicationScope,
        )
    }

    // 导入会整体替换数据：homeRefreshKey 触发首页统计重读，dataRevision 让题库之外
    // 的页面级 ViewModel（知识点、来源、进度）在下次打开（或当前停留）时重建并重读。
    var homeRefreshKey by remember(container) { mutableStateOf(0) }
    var dataRevision by remember(container) { mutableStateOf(0) }

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
    // 本次预览打开的是本周还是下周：窗口只由首页入口决定，预览页内不会顺延到更远的周。
    var planPreviewWeek by remember(container) { mutableStateOf(PlanWeek.CURRENT) }
    // 每日复习时长（schema v12，随备份导入/导出）；初始值先取默认，首页加载后覆盖。
    var dailyReviewMinutes by remember(container) {
        mutableStateOf(AppSettings.DEFAULT_DAILY_REVIEW_MINUTES)
    }
    // 改设置后重建预览 ViewModel，让“下一份计划”用新的每日分钟数；
    // 已生成的计划不会被回溯修改（settings.md「变更生效」）。
    val planPreviewViewModel = remember(
        container,
        planPreviewGeneration,
        planPreviewWeek,
        dailyReviewMinutes,
    ) {
        PlanPreviewViewModel(
            session = container.planSession,
            clock = container.clock,
            settingsFactory = { today ->
                planPreviewWeek.settingsFor(today).withDailyMinutes(dailyReviewMinutes)
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

    // 首页概览：本周/下周两个计划窗口的状态与题目/知识点/到期统计。
    // 任一统计读取失败只显示“—”，不阻塞首页入口（计划按钮仍可用）。
    val initialThisWeek = remember(container) {
        currentWeekSettings(LocalDate.now(container.clock))
    }
    val initialNextWeek = remember(container) {
        nextWeekSettings(LocalDate.now(container.clock))
    }
    var thisWeekStart by remember(container) { mutableStateOf(initialThisWeek.planStartDate) }
    var thisWeekEnd by remember(container) { mutableStateOf(initialThisWeek.planEndDate) }
    var nextWeekStart by remember(container) { mutableStateOf(initialNextWeek.planStartDate) }
    var nextWeekEnd by remember(container) { mutableStateOf(initialNextWeek.planEndDate) }
    var thisWeekPlanStatus by remember(container) { mutableStateOf<PlanStatus?>(null) }
    var nextWeekPlanStatus by remember(container) { mutableStateOf<PlanStatus?>(null) }
    var questionCount by remember(container) { mutableStateOf<Int?>(null) }
    var knowledgeCount by remember(container) { mutableStateOf<Int?>(null) }
    var overdueCount by remember(container) { mutableStateOf<Int?>(null) }
    var dueWithinWeekCount by remember(container) { mutableStateOf<Int?>(null) }

    LaunchedEffect(current, container, homeRefreshKey) {
        if (current != AppRoute.Home) return@LaunchedEffect
        // 导入会覆盖设置，homeRefreshKey 变化时一并重读。
        dailyReviewMinutes = quietly { container.settingsRepository.dailyReviewMinutes() }
            ?: AppSettings.DEFAULT_DAILY_REVIEW_MINUTES
        val today = LocalDate.now(container.clock)
        val thisWeek = currentWeekSettings(today)
        val nextWeek = nextWeekSettings(today)
        thisWeekStart = thisWeek.planStartDate
        thisWeekEnd = thisWeek.planEndDate
        nextWeekStart = nextWeek.planStartDate
        nextWeekEnd = nextWeek.planEndDate
        thisWeekPlanStatus = quietly {
            container.planRepository.activePlan(
                weekStart = thisWeek.planStartDate,
                weekEnd = thisWeek.planEndDate,
            )?.status
        }
        nextWeekPlanStatus = quietly {
            container.planRepository.activePlan(
                weekStart = nextWeek.planStartDate,
                weekEnd = nextWeek.planEndDate,
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
    val openPlanPreview: (PlanWeek) -> Unit = { week ->
        planPreviewWeek = week
        planPreviewGeneration += 1
        navigator.push(AppRoute.PlanPreview)
    }

    // 题库页的 ViewModel 与列表滚动状态归 AppRoot 顶层所有：进入题目编辑页后，
    // 路由分支离开组合会丢弃分支内的 remember，返回时列表会重新加载并回到顶部。
    val entryViewModel = remember(container) {
        EntryViewModel(repository = container.entryRepository)
    }
    val entryListState = rememberLazyListState()

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

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(routeTitle(current)) },
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
                    // 2026-11-22 用户要求：主界面导航栏右侧提供「导入数据库」「导出数据库」
                    // 入口，标签文字在左、对应语义图标固定在标签最右侧。
                    actions = {
                        if (current == AppRoute.Home) {
                            // 2026-11-22 用户要求：导入与导出的图标互换
                            // （导入用原导出图形、导出用原导入图形）。
                            TextButton(
                                onClick = { importPicker.launch(IMPORT_MIME_TYPES) },
                            ) {
                                Text("导入数据库")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    painter = painterResource(R.drawable.ic_export),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            TextButton(onClick = { navigator.push(AppRoute.Backup) }) {
                                Text("导出数据库")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    painter = painterResource(R.drawable.ic_import),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
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
                    .padding(padding),
            ) {
                when (val route = current) {
                    AppRoute.Home -> HomeScreen(
                        thisWeekPlanStatus = thisWeekPlanStatus,
                        thisWeekStart = thisWeekStart,
                        thisWeekEnd = thisWeekEnd,
                        nextWeekPlanStatus = nextWeekPlanStatus,
                        nextWeekStart = nextWeekStart,
                        nextWeekEnd = nextWeekEnd,
                        questionCount = questionCount,
                        knowledgeCount = knowledgeCount,
                        overdueCount = overdueCount,
                        dueWithinWeekCount = dueWithinWeekCount,
                        dailyReviewMinutes = dailyReviewMinutes,
                        versionName = container.appVersionName,
                        onSaveDailyReviewMinutes = saveDailyReviewMinutes,
                        onEntry = { navigator.push(AppRoute.Entry) },
                        onCreateQuestion = { navigator.push(AppRoute.QuestionEdit(null)) },
                        onManageKnowledgePoints = { navigator.push(AppRoute.KnowledgePoints) },
                        onManageSources = { navigator.push(AppRoute.SourceTags) },
                        onProgress = { navigator.push(AppRoute.Progress) },
                        onPlanPreview = openPlanPreview,
                        onScan = { navigator.push(AppRoute.Scan) },
                        onAbout = { navigator.push(AppRoute.About) },
                    )

                    AppRoute.Entry -> {
                        // 导入成功时 markNeedsRefresh 已置位；当前正停留题库页时也立即重读。
                        LaunchedEffect(dataRevision) { entryViewModel.refreshIfNeeded() }
                        EntryScreen(
                            viewModel = entryViewModel,
                            listState = entryListState,
                            onCreatePlan = { openPlanPreview(PlanWeek.CURRENT) },
                            onCreateQuestion = { navigator.push(AppRoute.QuestionEdit(null)) },
                            onEditQuestion = { questionId ->
                                navigator.push(AppRoute.QuestionEdit(questionId))
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
                            onSaved = {
                                // 保存后返回题库：静默更新列表数据，但不重置滚动位置。
                                entryViewModel.markNeedsRefresh()
                                navigator.pop()
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
                        planWeek = planPreviewWeek,
                        onPrint = { navigator.push(AppRoute.Print) },
                    )

                    AppRoute.Print -> {
                        val input = container.printPlanSession.latest?.toPrintPlanInput()
                        val viewModel = remember(container, input, printLauncher) {
                            PrintPlanViewModel(
                                input = input,
                                launcher = printLauncher,
                                snapshotWriter = container.printSnapshotWriter,
                            )
                        }
                        PrintPlanScreen(viewModel)
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
                                navigator.push(AppRoute.Confirm(sessionId))
                            },
                        )
                    }

                    AppRoute.Backup -> {
                        val viewModel = remember(container) {
                            BackupViewModel(
                                exporter = container.databaseExporter,
                                sharer = container.backupSharer,
                            )
                        }
                        BackupScreen(viewModel)
                    }

                    AppRoute.About -> AboutScreen(
                        appName = "拾知",
                        versionName = container.appVersionName,
                    )

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
                        ConfirmScreen(viewModel)
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

        // 覆盖确认与失败提示叠加在任意当前页之上；成功不弹窗，只显示带对勾的 Toast。
        ImportDialogs(
            state = importViewModel.state,
            onConfirm = { importScope.launch { importViewModel.confirm() } },
            onCancel = { importViewModel.cancel() },
            onDismissError = { importViewModel.dismissError() },
        )
    }
}

/** 导入文件选择的 MIME 过滤：json 优先，同时容纳被文件管理器标成文本/二进制的 json。 */
private val IMPORT_MIME_TYPES = arrayOf(
    "application/json",
    "text/plain",
    "application/octet-stream",
)

/**
 * 首页仪表盘：本周计划主卡（含下周入口） + 数据概览 + 操作入口 + 底部设置入口。
 *
 * 操作入口顶部是「录入题目」「管理题库」两个主入口，其下为 2×2 小标签；
 * 最底部的卡片栏左侧修改每日复习时长，右侧进入关于页面。
 * 计划是主流程，放在最上方并用主色容器突出；题目/知识点/到期统计用于扫一眼掌握情况；
 * 统计读取失败时显示“—”，不影响任何入口。
 */
@Composable
private fun HomeScreen(
    thisWeekPlanStatus: PlanStatus?,
    thisWeekStart: LocalDate,
    thisWeekEnd: LocalDate,
    nextWeekPlanStatus: PlanStatus?,
    nextWeekStart: LocalDate,
    nextWeekEnd: LocalDate,
    questionCount: Int?,
    knowledgeCount: Int?,
    overdueCount: Int?,
    dueWithinWeekCount: Int?,
    dailyReviewMinutes: Int,
    versionName: String,
    onSaveDailyReviewMinutes: (Int) -> Unit,
    onEntry: () -> Unit,
    onCreateQuestion: () -> Unit,
    onManageKnowledgePoints: () -> Unit,
    onManageSources: () -> Unit,
    onProgress: () -> Unit,
    onPlanPreview: (PlanWeek) -> Unit,
    onScan: () -> Unit,
    onAbout: () -> Unit,
) {
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
        // 主卡 + 概览 + 六个快捷入口 + 底部设置入口；内容超出屏幕时可滚动。
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            PlanHeroCard(
                thisWeekStatus = thisWeekPlanStatus,
                thisWeekStart = thisWeekStart,
                thisWeekEnd = thisWeekEnd,
                nextWeekStatus = nextWeekPlanStatus,
                nextWeekStart = nextWeekStart,
                nextWeekEnd = nextWeekEnd,
                onPlanPreview = onPlanPreview,
            )
        }
        item {
            HomeStatsCard(
                questionCount = questionCount,
                knowledgeCount = knowledgeCount,
                overdueCount = overdueCount,
                dueWithinWeekCount = dueWithinWeekCount,
            )
        }
        item {
            // 顶部两个主入口：录入题目直达新建题目页，管理题库进入题库列表页。
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_home_entry,
                    title = "录入题目",
                    iconContainer = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onCreateQuestion,
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_home_library,
                    title = "管理题库",
                    iconContainer = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onEntry,
                )
            }
        }
        item {
            // 上排：操作类入口。
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_home_scan,
                    title = "扫描回读",
                    iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onScan,
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_home_progress,
                    title = "查看复习进度",
                    iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = onProgress,
                )
            }
        }
        item {
            // 下排：管理类入口（共用 surfaceVariant，形成成组语义）。
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_home_knowledge,
                    title = "管理知识点",
                    iconContainer = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onManageKnowledgePoints,
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_home_source,
                    title = "管理题目来源",
                    iconContainer = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onManageSources,
                )
            }
        }
        item {
            // 底部设置类入口：左侧修改每日复习时长（时钟图标），右侧进入关于页面（信息图标）。
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HomeSettingsCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_clock,
                    title = "每日复习时长",
                    subtitle = "当前 $dailyReviewMinutes 分钟",
                    onClick = { showDailyMinutesDialog = true },
                )
                HomeSettingsCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_info,
                    title = "关于",
                    subtitle = "版本 $versionName",
                    onClick = onAbout,
                )
            }
        }
    }
}

/**
 * 底部的设置类入口卡片：图标在左，文字整体左对齐。
 *
 * 与上方的快捷操作卡片区分开：这里承载的是“设置”而非“操作”，
 * 因此左侧图标使用 surfaceVariant 底、文字可带一行副标题。
 */
@Composable
private fun HomeSettingsCard(
    iconRes: Int,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(HOME_SETTINGS_CARD_HEIGHT),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
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

/**
 * 计划主卡（C-100）：本周与下周按状态互换主次。
 *
 * 本周计划完成（COMPLETED）后，下周计划升为卡片主区块（大标题、说明、通栏主按钮），
 * 本周降为次要行（仍可预览/重印）；其余状态保持本周为主。
 * 只提供“本周”和“下周”两个窗口；点击任一入口只会复用该窗口已有计划，
 * 不会因为再次点击而顺延到更远的周。
 */
@Composable
private fun PlanHeroCard(
    thisWeekStatus: PlanStatus?,
    thisWeekStart: LocalDate,
    thisWeekEnd: LocalDate,
    nextWeekStatus: PlanStatus?,
    nextWeekStart: LocalDate,
    nextWeekEnd: LocalDate,
    onPlanPreview: (PlanWeek) -> Unit,
) {
    val focus = homePlanFocus(thisWeekStatus)
    val current = PlanCardWindow(
        week = PlanWeek.CURRENT,
        start = thisWeekStart,
        end = thisWeekEnd,
        status = thisWeekStatus,
    )
    val next = PlanCardWindow(
        week = PlanWeek.NEXT,
        start = nextWeekStart,
        end = nextWeekEnd,
        status = nextWeekStatus,
    )
    val primary = if (focus == PlanWeek.CURRENT) current else next
    val secondary = if (focus == PlanWeek.CURRENT) next else current

    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PlanPrimaryBlock(window = primary, onPlanPreview = onPlanPreview)

            // 同一张卡内的次要入口：本周完成后与下周互换主次。
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.onPrimaryContainer.copy(alpha = 0.15f)),
            )
            PlanSecondaryRow(window = secondary, onPlanPreview = onPlanPreview)
        }
    }
}

/** 计划卡里的一个周窗口：本周或下周，携带该窗口的日期与状态。 */
private data class PlanCardWindow(
    val week: PlanWeek,
    val start: LocalDate,
    val end: LocalDate,
    val status: PlanStatus?,
)

/**
 * 主区块：大标题、状态胶囊、状态说明与通栏主按钮。
 *
 * 打印入口只在“计划预览”页内提供：生成与打印是同一条流程的两个步骤，
 * 避免 Home 在没有内存计划时进入打印页只看到空态。
 */
@Composable
private fun PlanPrimaryBlock(
    window: PlanCardWindow,
    onPlanPreview: (PlanWeek) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = window.week.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onPrimaryContainer,
                )
                Text(
                    text = "${window.start} — ${window.end}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
            StatusPill(planStatusLabel(window.status))
        }
        Text(
            text = planStatusMessage(window.week, window.status),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onPrimaryContainer.copy(alpha = 0.85f),
        )
        Button(
            onClick = { onPlanPreview(window.week) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_home_plan),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(planPrimaryActionLabel(window.week, window.status))
        }
    }
}

/**
 * 次区块：小标题、日期、紧凑状态胶囊与文本入口。
 *
 * 状态胶囊与按钮右缘对齐，胶囊与按钮都用紧凑高度，避免把整张主卡撑高。
 */
@Composable
private fun PlanSecondaryRow(
    window: PlanCardWindow,
    onPlanPreview: (PlanWeek) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = window.week.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onPrimaryContainer,
            )
            Text(
                text = "${window.start} — ${window.end}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onPrimaryContainer.copy(alpha = 0.75f),
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            StatusPill(planStatusLabel(window.status), compact = true)
            CompactPlanAction(
                label = planSecondaryActionLabel(window.week, window.status),
                onClick = { onPlanPreview(window.week) },
            )
        }
    }
}

@Composable
private fun StatusPill(text: String, compact: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surface.copy(alpha = 0.65f))
            .padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 2.dp else 4.dp,
            ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = colors.onPrimaryContainer,
        )
    }
}

/**
 * 次区块入口的紧凑操作按钮。
 *
 * 不用 Material Button/TextButton：它们有 48dp 的最小触摸高度，会把次区块撑高；
 * 这里用可点击容器固定 30dp，保证主卡紧凑，视觉上仍是胶囊形状。
 * 右侧不留内边距，文字右缘与状态胶囊的右缘对齐（两者都贴齐 Column 右缘）。
 */
@Composable
private fun CompactPlanAction(
    label: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(start = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = colors.onPrimaryContainer,
        )
    }
}

/** 一行四项概览；数据未加载或读取失败时显示“—”。 */
@Composable
private fun HomeStatsCard(
    questionCount: Int?,
    knowledgeCount: Int?,
    overdueCount: Int?,
    dueWithinWeekCount: Int?,
) {
    val colors = MaterialTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 12.dp),
        ) {
            HomeStat(
                label = "题目",
                value = questionCount,
                valueColor = colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            HomeStat(
                label = "知识点",
                value = knowledgeCount,
                valueColor = colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            HomeStat(
                label = "已逾期",
                value = overdueCount,
                valueColor = if ((overdueCount ?: 0) > 0) colors.error else colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            HomeStat(
                label = "7 天内",
                value = dueWithinWeekCount,
                valueColor = colors.primary,
                modifier = Modifier.weight(1f),
            )
        }
    }
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
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 首页快捷入口统一高度：通栏「录入题目」与 2×2 小标签保持同一高度。 */
private val HOME_ACTION_CARD_HEIGHT = 56.dp

/** 底部设置入口高度：左侧带副标题，略高于快捷操作卡片。 */
private val HOME_SETTINGS_CARD_HEIGHT = 64.dp

/**
 * 2×2 小标签入口：图标在左、标题居中，与通栏「录入题目」等高。
 *
 * 半屏宽度不再放副标题与箭头，标题单行显示，保证一屏内完整呈现四个入口。
 */
@Composable
private fun QuickActionCard(
    iconRes: Int,
    title: String,
    iconContainer: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(HOME_ACTION_CARD_HEIGHT),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun planStatusLabel(status: PlanStatus?): String = when (status) {
    null -> "未生成"
    PlanStatus.GENERATED -> "待打印"
    PlanStatus.PRINTED -> "已打印"
    PlanStatus.COMPLETED -> "已完成"
    PlanStatus.SUPERSEDED -> "已替代"
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
    AppRoute.Backup -> "导出数据库"
    AppRoute.About -> "关于"
}
