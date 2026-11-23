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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.recollect.app.navigation.AppNavigator
import com.recollect.app.navigation.AppRoute
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
    val planPreviewViewModel = remember(container, planPreviewGeneration, planPreviewWeek) {
        PlanPreviewViewModel(
            session = container.planSession,
            clock = container.clock,
            settingsFactory = { today -> planPreviewWeek.settingsFor(today) },
            onPrintJobAvailable = { container.printPlanSession.update(it) },
        )
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
                        onEntry = { navigator.push(AppRoute.Entry) },
                        onCreateQuestion = { navigator.push(AppRoute.QuestionEdit(null)) },
                        onManageKnowledgePoints = { navigator.push(AppRoute.KnowledgePoints) },
                        onManageSources = { navigator.push(AppRoute.SourceTags) },
                        onProgress = { navigator.push(AppRoute.Progress) },
                        onPlanPreview = openPlanPreview,
                        onScan = { navigator.push(AppRoute.Scan) },
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
                                sourceTagRepository = container.sourceTagLibraryRepository,
                            )
                        }
                        // 来源标签库与知识点标签库都在录入页内以全屏窗口打开：
                        // 不离开当前页面，未保存的表单内容不会丢失。
                        val sourceTagViewModel = remember(container) {
                            SourceTagLibraryViewModel(container.sourceTagLibraryRepository)
                        }
                        val knowledgePointViewModel = remember(container) {
                            KnowledgePointLibraryViewModel(container.knowledgePointLibraryRepository)
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
                            KnowledgePointLibraryViewModel(container.knowledgePointLibraryRepository)
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
 * 首页仪表盘：本周计划主卡（含下周入口） + 数据概览 + 快捷操作。
 *
 * 快捷操作顶部是「录入题目」「管理题库」两个主入口，其下为 2×2 小标签。
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
    onEntry: () -> Unit,
    onCreateQuestion: () -> Unit,
    onManageKnowledgePoints: () -> Unit,
    onManageSources: () -> Unit,
    onProgress: () -> Unit,
    onPlanPreview: (PlanWeek) -> Unit,
    onScan: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // 主卡 + 概览 + 六个等高快捷入口在常见手机上一屏完整显示；更小屏幕仍可滚动。
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
            Text(
                text = "快捷操作",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp),
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
    }
}

/**
 * 计划主卡：本周状态与主操作，附下周计划入口。
 *
 * 只提供“本周”和“下周”两个窗口；下周计划存在时复用同一计划号，
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
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "本周计划",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onPrimaryContainer,
                    )
                    Text(
                        text = "$thisWeekStart — $thisWeekEnd",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                }
                StatusPill(planStatusLabel(thisWeekStatus))
            }
            Text(
                text = planStatusMessage(thisWeekStatus),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onPrimaryContainer.copy(alpha = 0.85f),
            )
            // 打印入口只在“计划预览”页内提供：生成与打印是同一条流程的两个步骤，
            // 避免 Home 在没有内存计划时进入打印页只看到空态。
            Button(
                onClick = { onPlanPreview(PlanWeek.CURRENT) },
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
                Text(if (thisWeekStatus == null) "生成本周计划" else "查看并打印本周计划")
            }

            // 下周计划：同一张卡内的次要入口，周日提前准备下周、工作日提前排下周都可以。
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.onPrimaryContainer.copy(alpha = 0.15f)),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "下周计划",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onPrimaryContainer,
                    )
                    Text(
                        text = "$nextWeekStart — $nextWeekEnd",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                }
                // 状态与本周主卡同款胶囊：从日期行移出，放在生成/查看按钮之上。
                // 下周是次要入口，胶囊与按钮都用紧凑高度，避免把整张主卡撑高。
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    StatusPill(planStatusLabel(nextWeekStatus), compact = true)
                    // C-099：本周计划完成后，把“生成/查看下周计划”提升为主按钮，
                    // 引导用户接着准备下一周；其余状态保持次要文本样式。
                    CompactPlanAction(
                        label = if (nextWeekStatus == null) "生成下周计划" else "查看下周计划",
                        primary = thisWeekStatus == PlanStatus.COMPLETED,
                        onClick = { onPlanPreview(PlanWeek.NEXT) },
                    )
                }
            }
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
 * 下周入口的紧凑操作按钮。
 *
 * 不用 Material Button/TextButton：它们有 48dp 的最小触摸高度，会把下周区块撑高；
 * 这里用可点击容器固定 30dp，保证主卡紧凑，视觉上仍是胶囊形状。
 * 右侧不留内边距，文字右缘与状态胶囊的右缘对齐（两者都贴齐 Column 右缘）。
 */
@Composable
private fun CompactPlanAction(
    label: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(50))
            .background(if (primary) colors.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = if (primary) colors.onPrimary else colors.onPrimaryContainer,
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

private fun planStatusMessage(status: PlanStatus?): String = when (status) {
    null -> "按本周到期的知识点生成计划，打印到纸上开始复习。"
    PlanStatus.GENERATED -> "计划已生成，打印后按日期完成复习。"
    PlanStatus.PRINTED -> "已打印。按日期完成复习，完成后拍照回读。"
    PlanStatus.COMPLETED -> "本周计划已完成，可以生成下周计划或查看复习进度。"
    PlanStatus.SUPERSEDED -> "本周计划已被新的计划替代，请查看最新计划。"
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
}
