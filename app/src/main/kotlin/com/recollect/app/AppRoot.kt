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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.recollect.app.navigation.AppNavigator
import com.recollect.app.navigation.AppRoute
import com.recollect.app.ui.theme.RecollectTheme
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
                            Text(
                                text = routeTitle(current),
                                fontWeight = FontWeight.SemiBold,
                            )
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
                        // 2026-11-22 用户要求：主界面导航栏右侧提供「导入数据库」「导出数据库」
                        // 入口，标签文字在左、对应语义图标固定在标签最右侧。
                        actions = {
                            if (current == AppRoute.Home) {
                                // 2026-11-22 用户要求：导入与导出的图标互换
                                // （导入用原导出图形、导出用原导入图形）。
                                TextButton(
                                    onClick = { importPicker.launch(IMPORT_MIME_TYPES) },
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                ) {
                                    Text(
                                        "导入数据库",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        painter = painterResource(R.drawable.ic_export),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                TextButton(
                                    onClick = { navigator.push(AppRoute.Backup) },
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                ) {
                                    Text(
                                        "导出数据库",
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        painter = painterResource(R.drawable.ic_import),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
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
                            questionCount = questionCount,
                            knowledgeCount = knowledgeCount,
                            overdueCount = overdueCount,
                            dueWithinWeekCount = dueWithinWeekCount,
                            dailyReviewMinutes = dailyReviewMinutes,
                            versionName = container.appVersionName,
                            onSaveDailyReviewMinutes = saveDailyReviewMinutes,
                            onEntry = { navigator.push(AppRoute.Entry) },
                            onCreateQuestion = {
                                // 从首页进入：保存成功后强制进入题库页（见 QuestionEdit 分支）。
                                navigator.push(AppRoute.QuestionEdit(null, fromHome = true))
                            },
                            onCreatePlan = { openPlanPreview() },
                            onProgress = { navigator.push(AppRoute.Progress) },
                            onScan = { navigator.push(AppRoute.Scan) },
                            onAbout = { navigator.push(AppRoute.About) },
                        )

                        AppRoute.Entry -> {
                            // 导入成功时 markNeedsRefresh 已置位；当前正停留题库页时也立即重读。
                            LaunchedEffect(dataRevision) { entryViewModel.refreshIfNeeded() }
                            EntryScreen(
                                viewModel = entryViewModel,
                                listState = entryListState,
                                onCreatePlan = { openPlanPreview() },
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
                            onPrint = { navigator.push(AppRoute.Print) },
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
                                    showConfirmCompleteToast(printContext)
                                    navigator.resetTo(AppRoute.Home)
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

            // 覆盖确认与失败提示叠加在任意当前页之上；成功不弹窗，只显示带对勾的 Toast。
            ImportDialogs(
                state = importViewModel.state,
                onConfirm = { importScope.launch { importViewModel.confirm() } },
                onCancel = { importViewModel.cancel() },
                onDismissError = { importViewModel.dismissError() },
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
    versionName: String,
    onSaveDailyReviewMinutes: (Int) -> Unit,
    onEntry: () -> Unit,
    onCreateQuestion: () -> Unit,
    onCreatePlan: () -> Unit,
    onProgress: () -> Unit,
    onScan: () -> Unit,
    onAbout: () -> Unit,
) {
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
            )
        }
        item {
            HomeSection(title = "设置") {
                HomeSettingsPanel(
                    dailyReviewMinutes = dailyReviewMinutes,
                    versionName = versionName,
                    onEditDailyMinutes = { showDailyMinutesDialog = true },
                    onAbout = onAbout,
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
) {
    val colors = MaterialTheme.colorScheme
    val hint = if (cycleComplete) {
        "本轮已完成"
    } else {
        "第 ${currentStage.ordinal + 1} / ${ReviewStage.entries.size} 步"
    }
    Surface(
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
                            .fillMaxHeight(),
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

/** 设置区：两项设置放在同一面板内，用分割线区分，避免再堆两张卡片。 */
@Composable
private fun HomeSettingsPanel(
    dailyReviewMinutes: Int,
    versionName: String,
    onEditDailyMinutes: () -> Unit,
    onAbout: () -> Unit,
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
            HorizontalDivider(
                modifier = Modifier.padding(start = 60.dp),
                color = colors.outlineVariant,
            )
            HomeSettingsRow(
                iconRes = R.drawable.ic_info,
                title = "关于",
                subtitle = "版本 $versionName",
                onClick = onAbout,
            )
        }
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
    AppRoute.Backup -> "导出数据库"
    AppRoute.About -> "关于"
    AppRoute.License -> "开源许可"
}

/** 取承载 Compose 的 Activity：`LocalView.context` 可能被主题等逐层包装，需要解包。 */
private tailrec fun Context.findHostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findHostActivity()
    else -> null
}
