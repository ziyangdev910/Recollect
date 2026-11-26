package com.recollect.feature.planning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import kotlinx.coroutines.launch

/**
 * PlanPreview 页面（app.md）：显示计划页与 overflow/文字提示。
 *
 * 只展示中文标签与 `message`，不展示分页内部代码（C-065 方案 B）；
 * 空计划显示可读空态并保留提示（C-060/C-061/C-062）。
 *
 * 2026-11-25 排版调整：顶部固定「计划号 + 打印」主卡；每日安排改成「日期栏 + 题目标签」的时间轴卡片，
 * 被推迟与需要处理的问题用语义色分区，生成入口降到页面底部，避免与打印主操作争夺注意力。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlanPreviewScreen(
    viewModel: PlanPreviewViewModel,
    onPrint: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var showRegenerateConfirm by remember { mutableStateOf(false) }
    // 只在本次计划实例首次进入时加载/生成；从打印页返回预览时复用原计划，不刷新计划号。
    LaunchedEffect(viewModel) { viewModel.refreshIfNeeded() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        WeekSelector(
            selected = viewModel.week,
            onSelect = { week -> scope.launch { viewModel.selectWeek(week) } },
        )

        when (val state = viewModel.state) {
            PlanPreviewUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "正在按知识点到期情况生成计划…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is PlanPreviewUiState.Content -> {
                PlanSummaryCard(
                    planId = state.planId,
                    pageCount = state.pages.size,
                    questionCount = state.pages.sumOf { it.questionLabels.size },
                    onPrint = onPrint,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "每日安排",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "共 ${state.pages.size} 页",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Column 会给非 weighted 子项无界高度；LazyColumn 必须拿到有界高度。
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.pages, key = { it.pageNumber }) { page ->
                        PlanPageCard(page)
                    }
                    if (state.deferred.isNotEmpty()) {
                        item { DeferredList(state.deferred) }
                    }
                    if (state.warnings.isNotEmpty()) {
                        item { WarningList(state.warnings) }
                    }
                    item {
                        // 显式重新生成：新计划号会替代该窗口的当前计划；默认路径只复用已有计划。
                        TextButton(
                            onClick = { showRegenerateConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = viewModel.week.regenerateLabel,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            is PlanPreviewUiState.Empty -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                EmptyPlanCard(state.message)
                DeferredList(state.deferred)
                WarningList(state.warnings)
                TextButton(
                    onClick = { showRegenerateConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = viewModel.week.regenerateLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is PlanPreviewUiState.Error -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = state.error.message,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
                if (state.error.retryable) {
                    Button(onClick = { scope.launch { viewModel.refresh() } }) {
                        Text("重试")
                    }
                }
            }
        }
    }

    // 打开已有计划时，若题库在计划生成后发生变化，先询问是否重新生成。
    if (viewModel.bankChanged) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBankChanged() },
            title = { Text("题库已更新") },
            text = { Text("当前题库与上次生成计划时不同，是否重新生成计划？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { viewModel.regenerate() }
                    },
                ) {
                    Text("重新生成")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissBankChanged() }) {
                    Text("保持当前计划")
                }
            },
        )
    }

    if (showRegenerateConfirm) {
        AlertDialog(
            onDismissRequest = { showRegenerateConfirm = false },
            title = { Text(viewModel.week.regenerateLabel) },
            text = {
                Text(
                    "将生成新的计划号。旧计划仍可回读，但不再是最新计划；" +
                        "已打印的旧稿如需继续使用，请保留。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRegenerateConfirm = false
                        scope.launch { viewModel.regenerate() }
                    },
                ) {
                    Text("重新生成")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateConfirm = false }) { Text("取消") }
            },
        )
    }
}

/** 顶部周选择栏：本周/下周各自加载或生成对应窗口的计划。 */
@Composable
private fun WeekSelector(
    selected: PlanWeek,
    onSelect: (PlanWeek) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlanWeek.entries.forEach { week ->
            FilterChip(
                selected = week == selected,
                onClick = { onSelect(week) },
                label = { Text(week.tabLabel) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 计划摘要卡：计划号、页数/题数与唯一的打印主操作。 */
@Composable
private fun PlanSummaryCard(
    planId: String,
    pageCount: Int,
    questionCount: Int,
    onPrint: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column {
                Text(
                    text = "计划 $planId",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "共 $pageCount 页 · $questionCount 题",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
            Button(
                onClick = onPrint,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_print),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("打印计划")
            }
        }
    }
}

/** 单页安排：左侧日期栏，右侧题目标识。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlanPageCard(page: PlanPreviewPage) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        border = BorderStroke(1.dp, colors.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.width(44.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = page.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.primary,
                )
                Text(
                    text = "${page.date.monthValue}月",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = weekdayLabel(page.date.dayOfWeek),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(56.dp)
                    .background(colors.outlineVariant),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "第 ${page.pageNumber} 页",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${page.questionLabels.size} 题",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    page.questionLabels.forEach { label ->
                        QuestionLabelChip(label)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionLabelChip(label: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.small,
        color = colors.surfaceVariant,
        contentColor = colors.onSurfaceVariant,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 被推迟题目：让“首页显示 N 题、计划只有 N-1 题”有可读解释。
 *
 * 只展示中文标签与说明，不暴露调度器内部原因代码。
 */
@Composable
private fun DeferredList(deferred: List<PlanPreviewDeferred>) {
    if (deferred.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.tertiaryContainer),
        border = BorderStroke(1.dp, colors.tertiary.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "有 ${deferred.size} 道题未排入本次计划",
                style = MaterialTheme.typography.titleSmall,
                color = colors.onTertiaryContainer,
            )
            deferred.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(colors.onTertiaryContainer, shape = MaterialTheme.shapes.extraSmall),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${item.questionLabel} · ${item.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onTertiaryContainer,
                        )
                    }
                    Text(
                        text = item.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onTertiaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 11.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WarningList(warnings: List<PlanPreviewWarning>) {
    if (warnings.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "需要处理的问题",
            style = MaterialTheme.typography.titleMedium,
        )
        warnings.forEach { warning ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.errorContainer),
                border = BorderStroke(1.dp, colors.error.copy(alpha = 0.18f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "题号 ${warning.questionNumber} · ${warning.label}",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onErrorContainer,
                    )
                    Text(
                        text = warning.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onErrorContainer.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

/** 空计划：说明为什么没有可打印内容，并保留推迟/警告信息。 */
@Composable
private fun EmptyPlanCard(message: String) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.secondaryContainer),
        border = BorderStroke(1.dp, colors.secondary.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "本次没有可打印的题目",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSecondaryContainer,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSecondaryContainer.copy(alpha = 0.85f),
            )
        }
    }
}

private fun weekdayLabel(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
    DayOfWeek.MONDAY -> "周一"
    DayOfWeek.TUESDAY -> "周二"
    DayOfWeek.WEDNESDAY -> "周三"
    DayOfWeek.THURSDAY -> "周四"
    DayOfWeek.FRIDAY -> "周五"
    DayOfWeek.SATURDAY -> "周六"
    DayOfWeek.SUNDAY -> "周日"
}
