package com.recollect.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.recollect.app.navigation.AppNavigator
import com.recollect.app.navigation.AppRoute
import com.recollect.data.room.repository.PlanStatus
import com.recollect.feature.confirm.ConfirmScreen
import com.recollect.feature.confirm.ConfirmSubmitter
import com.recollect.feature.confirm.ConfirmViewModel
import com.recollect.feature.entry.EntryScreen
import com.recollect.feature.entry.EntryViewModel
import com.recollect.feature.entry.KnowledgePointLibraryScreen
import com.recollect.feature.entry.KnowledgePointLibraryViewModel
import com.recollect.feature.entry.QuestionEditScreen
import com.recollect.feature.entry.QuestionEditViewModel
import com.recollect.feature.entry.SourceTagLibraryViewModel
import com.recollect.feature.planning.PlanPreviewScreen
import com.recollect.feature.planning.PlanPreviewViewModel
import com.recollect.feature.planning.PrintJob
import com.recollect.feature.planning.defaultPlanningSettings
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

    // 首页概览：计划状态、本周窗口与题目/知识点/到期统计。
    // 任一统计读取失败只显示“—”，不阻塞首页入口（计划按钮仍可用）。
    val initialHomeSettings = remember(container) {
        defaultPlanningSettings(LocalDate.now(container.clock))
    }
    var homeWeekStart by remember(container) { mutableStateOf(initialHomeSettings.planStartDate) }
    var homeWeekEnd by remember(container) { mutableStateOf(initialHomeSettings.planEndDate) }
    var planStatus by remember(container) { mutableStateOf<PlanStatus?>(null) }
    var questionCount by remember(container) { mutableStateOf<Int?>(null) }
    var knowledgeCount by remember(container) { mutableStateOf<Int?>(null) }
    var overdueCount by remember(container) { mutableStateOf<Int?>(null) }
    var dueWithinWeekCount by remember(container) { mutableStateOf<Int?>(null) }

    LaunchedEffect(current, container) {
        if (current != AppRoute.Home) return@LaunchedEffect
        val today = LocalDate.now(container.clock)
        val settings = defaultPlanningSettings(today)
        homeWeekStart = settings.planStartDate
        homeWeekEnd = settings.planEndDate
        planStatus = quietly {
            container.planRepository.activePlan(
                weekStart = settings.planStartDate,
                weekEnd = settings.planEndDate,
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
                        planStatus = planStatus,
                        weekStart = homeWeekStart,
                        weekEnd = homeWeekEnd,
                        questionCount = questionCount,
                        knowledgeCount = knowledgeCount,
                        overdueCount = overdueCount,
                        dueWithinWeekCount = dueWithinWeekCount,
                        onEntry = { navigator.push(AppRoute.Entry) },
                        onManageKnowledgePoints = { navigator.push(AppRoute.KnowledgePoints) },
                        onProgress = { navigator.push(AppRoute.Progress) },
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
                                sourceTagRepository = container.sourceTagLibraryRepository,
                            )
                        }
                        // 来源标签库在录入页内以全屏窗口打开：不离开当前页面，未保存的表单内容不会丢失。
                        val sourceTagViewModel = remember(container) {
                            SourceTagLibraryViewModel(container.sourceTagLibraryRepository)
                        }
                        QuestionEditScreen(
                            viewModel = viewModel,
                            sourceTagViewModel = sourceTagViewModel,
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

                    AppRoute.Progress -> {
                        val viewModel = remember(container) {
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

/**
 * 首页仪表盘：本周计划主卡 + 数据概览 + 2×2 快捷操作。
 *
 * 计划是主流程，放在最上方并用主色容器突出；题目/知识点/到期统计用于扫一眼掌握情况；
 * 其余入口收纳为带图标的快捷卡片。统计读取失败时显示“—”，不影响任何入口。
 */
@Composable
private fun HomeScreen(
    planStatus: PlanStatus?,
    weekStart: LocalDate,
    weekEnd: LocalDate,
    questionCount: Int?,
    knowledgeCount: Int?,
    overdueCount: Int?,
    dueWithinWeekCount: Int?,
    onEntry: () -> Unit,
    onManageKnowledgePoints: () -> Unit,
    onProgress: () -> Unit,
    onPlanPreview: () -> Unit,
    onScan: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PlanHeroCard(
                planStatus = planStatus,
                weekStart = weekStart,
                weekEnd = weekEnd,
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
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_home_entry,
                    title = "录入题目",
                    subtitle = "添加新题与知识点",
                    iconContainer = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = onEntry,
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_home_progress,
                    title = "查看复习进度",
                    subtitle = "掌握度与到期时间",
                    iconContainer = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = onProgress,
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_home_scan,
                    title = "扫描回读",
                    subtitle = "拍照回读填涂结果",
                    iconContainer = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = onScan,
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    iconRes = R.drawable.ic_home_knowledge,
                    title = "管理知识点",
                    subtitle = "维护标签库",
                    iconContainer = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onManageKnowledgePoints,
                )
            }
        }
    }
}

/** 本周计划主卡：状态、周窗口与唯一主操作。 */
@Composable
private fun PlanHeroCard(
    planStatus: PlanStatus?,
    weekStart: LocalDate,
    weekEnd: LocalDate,
    onPlanPreview: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
                        text = "$weekStart — $weekEnd",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                }
                StatusPill(planStatusLabel(planStatus))
            }
            Text(
                text = planStatusMessage(planStatus),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onPrimaryContainer.copy(alpha = 0.85f),
            )
            // 打印入口只在“计划预览”页内提供：生成与打印是同一条流程的两个步骤，
            // 避免 Home 在没有内存计划时进入打印页只看到空态。
            Button(
                onClick = onPlanPreview,
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
                Text(if (planStatus == null) "生成本周计划" else "查看并打印本周计划")
            }
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.surface.copy(alpha = 0.65f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
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
                .padding(horizontal = 4.dp, vertical = 16.dp),
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

@Composable
private fun QuickActionCard(
    iconRes: Int,
    title: String,
    subtitle: String,
    iconContainer: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    PlanStatus.COMPLETED -> "本周计划已完成，可以查看复习进度。"
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
    AppRoute.Progress -> "复习进度"
    AppRoute.PlanPreview -> "计划预览"
    AppRoute.Print -> "打印"
    AppRoute.Scan -> "扫描"
    is AppRoute.Confirm -> "确认"
    AppRoute.Settings -> "设置"
    AppRoute.Backup -> "备份"
}
