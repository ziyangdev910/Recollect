package com.recollect.feature.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
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
    onPrint: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) { viewModel.refresh() }

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
                                Text("第 ${page.pageNumber} 页 · ${page.date} · ${page.questionNumbers.size} 条")
                                page.questionNumbers.forEach { number ->
                                    Text("· 题号 $number")
                                }
                            }
                        }
                    }
                }
                WarningList(state.warnings)
            }

            is PlanPreviewUiState.Empty -> {
                Text(state.message, style = MaterialTheme.typography.titleMedium)
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
