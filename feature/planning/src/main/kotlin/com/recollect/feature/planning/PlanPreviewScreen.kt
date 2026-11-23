package com.recollect.feature.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * PlanPreview 页面（app.md）：显示计划页与 overflow/文字提示。
 *
 * 只展示中文标签与 `message`，不展示分页内部代码（C-065 方案 B）；
 * 空计划显示可读空态并保留提示（C-060/C-061/C-062）。
 */
@Composable
fun PlanPreviewScreen(
    viewModel: PlanPreviewViewModel,
    /** 本次预览打开的是哪一周：决定“重新生成…”文案，避免把下周计划叫成本周计划。 */
    planWeek: PlanWeek = PlanWeek.CURRENT,
    onPrint: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var showRegenerateConfirm by remember { mutableStateOf(false) }
    // 只在本次计划实例首次进入时加载/生成；从打印页返回预览时复用原计划，不刷新计划号。
    LaunchedEffect(viewModel) { viewModel.refreshIfNeeded() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (val state = viewModel.state) {
            PlanPreviewUiState.Loading -> CircularProgressIndicator()

            is PlanPreviewUiState.Content -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "计划号 ${state.planId} · 共 ${state.pages.size} 页",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = onPrint) {
                        Text("打印计划")
                    }
                }
                // Column 会给非 weighted 子项无界高度；LazyColumn 必须拿到有界高度。
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.pages, key = { it.pageNumber }) { page ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("第 ${page.pageNumber} 页 · ${page.date} · ${page.questionLabels.size} 条")
                                // 题目标识统一为「页码-第X题」，避免不同来源页的相同题号无法区分。
                                page.questionLabels.forEach { label ->
                                    Text("· $label")
                                }
                            }
                        }
                    }
                    if (state.deferred.isNotEmpty()) {
                        item { DeferredList(state.deferred) }
                    }
                }
                WarningList(state.warnings)

                // 显式重新生成：新计划号会替代该窗口的当前计划；默认路径只复用已有计划。
                TextButton(onClick = { showRegenerateConfirm = true }) {
                    Text(planWeek.regenerateLabel)
                }
            }

            is PlanPreviewUiState.Empty -> {
                Text(state.message, style = MaterialTheme.typography.titleMedium)
                DeferredList(state.deferred)
                WarningList(state.warnings)
            }

            is PlanPreviewUiState.Error -> {
                Text(state.error.message, color = MaterialTheme.colorScheme.error)
                if (state.error.retryable) {
                    Button(onClick = { scope.launch { viewModel.refresh() } }) {
                        Text("重试")
                    }
                }
            }
        }
    }

    if (showRegenerateConfirm) {
        AlertDialog(
            onDismissRequest = { showRegenerateConfirm = false },
            title = { Text(planWeek.regenerateLabel) },
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

/**
 * 被推迟题目：让“首页显示 N 题、计划只有 N-1 题”有可读解释。
 *
 * 只展示中文标签与说明，不暴露调度器内部原因代码。
 */
@Composable
private fun DeferredList(deferred: List<PlanPreviewDeferred>) {
    if (deferred.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "有 ${deferred.size} 道题未排入本次计划",
                style = MaterialTheme.typography.titleMedium,
            )
            // Text(
            //     text = "由于每日可用复习时长不足，以下题目本次被推迟安排：",
            //     style = MaterialTheme.typography.bodySmall,
            // )
            deferred.forEach { item ->
                Column {
                    Text("${item.questionLabel} · ${item.label}")
                    Text(item.message, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun WarningList(warnings: List<PlanPreviewWarning>) {
    if (warnings.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("需要处理的问题", style = MaterialTheme.typography.titleMedium)
        warnings.forEach { warning ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("题号 ${warning.questionNumber} · ${warning.label}")
                    Text(warning.message, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
