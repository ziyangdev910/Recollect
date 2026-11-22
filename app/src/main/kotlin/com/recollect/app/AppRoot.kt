package com.recollect.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.recollect.app.navigation.AppNavigator
import com.recollect.app.navigation.AppRoute
import com.recollect.feature.confirm.ConfirmScreen
import com.recollect.feature.confirm.ConfirmSubmitter
import com.recollect.feature.confirm.ConfirmViewModel
import com.recollect.feature.entry.EntryScreen
import com.recollect.feature.entry.EntryViewModel
import com.recollect.feature.entry.KnowledgePointLibraryScreen
import com.recollect.feature.entry.KnowledgePointLibraryViewModel
import com.recollect.feature.entry.QuestionEditScreen
import com.recollect.feature.entry.QuestionEditViewModel
import com.recollect.feature.planning.PlanPreviewScreen
import com.recollect.feature.planning.PlanPreviewViewModel
import com.recollect.feature.planning.PrintJob
import com.recollect.feature.planning.defaultPlanningSettings
import com.recollect.feature.print.AndroidPrintLauncher
import com.recollect.feature.print.PrintPlanInput
import com.recollect.feature.print.PrintPlanScreen
import com.recollect.feature.print.PrintPlanViewModel
import com.recollect.scan.ui.ScanScreen
import com.recollect.scan.ui.ScanViewModel
import java.time.LocalDate

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

    // 计划预览页的 ViewModel 归 AppRoot 顶层所有：路由分支离开组合后，
    // 分支内 remember 的 ViewModel 会被丢弃；若在分支内创建，从打印页返回
    // 预览页时会重建 ViewModel 并触发 LaunchedEffect 重新生成计划，计划号随之变化。
    // 顶部持有同一实例即可让“预览 → 打印 → 返回预览”复用原计划；
    // 只有再次从 Home/Entry 点击“生成本周计划”时才递增 generation 重新生成。
    var planPreviewGeneration by remember(container) { mutableStateOf(0) }
    val planPreviewViewModel = remember(container, planPreviewGeneration) {
        PlanPreviewViewModel(
            session = container.planSession,
            clock = container.clock,
            onPrintJobAvailable = { container.printPlanSession.update(it) },
        )
    }

    // 首页按钮文案跟随本周计划状态：已有计划时只“查看并打印”，不再隐式生成新计划号。
    var hasActivePlan by remember(container) { mutableStateOf(false) }
    LaunchedEffect(current, container) {
        if (current == AppRoute.Home) {
            val settings = defaultPlanningSettings(LocalDate.now(container.clock))
            hasActivePlan = container.planRepository.activePlan(
                weekStart = settings.planStartDate,
                weekEnd = settings.planEndDate,
            ) != null
        }
    }
    val openPlanPreview = {
        planPreviewGeneration += 1
        navigator.push(AppRoute.PlanPreview)
    }

    // 题库页的 ViewModel 与列表滚动状态归 AppRoot 顶层所有：进入题目编辑页后，
    // 路由分支离开组合会丢弃分支内的 remember，返回时列表会重新加载并回到顶部。
    val entryViewModel = remember(container) {
        EntryViewModel(repository = container.entryRepository)
    }
    val entryListState = rememberLazyListState()

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
                        hasActivePlan = hasActivePlan,
                        onEntry = { navigator.push(AppRoute.Entry) },
                        onManageKnowledgePoints = { navigator.push(AppRoute.KnowledgePoints) },
                        onPlanPreview = openPlanPreview,
                        onScan = { navigator.push(AppRoute.Scan) },
                    )

                    AppRoute.Entry -> EntryScreen(
                        viewModel = entryViewModel,
                        listState = entryListState,
                        onCreatePlan = openPlanPreview,
                        onCreateQuestion = { navigator.push(AppRoute.QuestionEdit(null)) },
                        onEditQuestion = { questionId ->
                            navigator.push(AppRoute.QuestionEdit(questionId))
                        },
                    )

                    is AppRoute.QuestionEdit -> {
                        val viewModel = remember(container, route.questionId) {
                            QuestionEditViewModel(
                                repository = container.entryRepository,
                                libraryRepository = container.knowledgePointLibraryRepository,
                            )
                        }
                        QuestionEditScreen(
                            viewModel = viewModel,
                            questionId = route.questionId,
                            onManageTags = { navigator.push(AppRoute.KnowledgePoints) },
                            onSaved = {
                                // 保存后返回题库：静默更新列表数据，但不重置滚动位置。
                                entryViewModel.markNeedsRefresh()
                                navigator.pop()
                            },
                        )
                    }

                    AppRoute.KnowledgePoints -> {
                        val viewModel = remember(container) {
                            KnowledgePointLibraryViewModel(container.knowledgePointLibraryRepository)
                        }
                        KnowledgePointLibraryScreen(viewModel)
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
    }
}

@Composable
private fun HomeScreen(
    hasActivePlan: Boolean,
    onEntry: () -> Unit,
    onManageKnowledgePoints: () -> Unit,
    onPlanPreview: () -> Unit,
    onScan: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Text("拾知：把复习打印到纸上", style = MaterialTheme.typography.headlineSmall)
        Button(onClick = onEntry) { Text("录入题目") }
        Button(onClick = onManageKnowledgePoints) { Text("管理知识点") }
        // 打印入口只在“计划预览”页内提供：生成与打印是同一条流程的两个步骤，
        // 避免 Home 在没有内存计划时进入打印页只看到空态。
        Button(onClick = onPlanPreview) {
            Text(if (hasActivePlan) "查看并打印本周计划" else "生成本周计划")
        }
        Button(onClick = onScan) { Text("扫描回读") }
    }
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
    AppRoute.PlanPreview -> "计划预览"
    AppRoute.Print -> "打印"
    AppRoute.Scan -> "扫描"
    is AppRoute.Confirm -> "确认"
    AppRoute.Settings -> "设置"
    AppRoute.Backup -> "备份"
}
