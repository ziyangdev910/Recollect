package com.recollect.feature.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.recollect.common.UiState
import com.recollect.data.room.repository.EntrySummary
import kotlinx.coroutines.launch

/**
 * Entry 页面（entry.md）：题目列表占满页面。
 *
 * 新建/编辑使用独立的 [QuestionEditScreen]；本页只负责列表、进入编辑页与删除（归档）。
 * 页面只与 [EntryViewModel] 交互，不直接访问 Room/Context（R-03）。
 */
@Composable
fun EntryScreen(
    viewModel: EntryViewModel,
    onCreatePlan: () -> Unit,
    onCreateQuestion: () -> Unit,
    onEditQuestion: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) { viewModel.refresh() }

    var pendingDelete by remember { mutableStateOf<EntrySummary?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when (val state = viewModel.state) {
                        is UiState.Content -> "已有 ${state.data.questions.size} 道题"
                        else -> "题库"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onCreatePlan) { Text("生成计划") }
            }

            when (val state = viewModel.state) {
                UiState.Loading -> CircularProgressIndicator()
                UiState.Empty -> Text("题库还是空的。点击右下角「录入题目」新建第 1 道题。")
                is UiState.Content -> {
                    // 列表占满剩余空间；底部留出悬浮按钮的高度，避免遮挡最后一行。
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 96.dp),
                    ) {
                        items(state.data.questions, key = { it.questionId }) { row ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 8.dp),
                                    ) {
                                        Text("题号：${row.questionNumber}")
                                        Text("页码：${row.sourcePage}")
                                        Text("知识点：${row.knowledgeNames.joinToString(" / ")}")
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.clearActionError()
                                            onEditQuestion(row.questionId)
                                        },
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_edit),
                                            contentDescription = "编辑",
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.clearActionError()
                                            pendingDelete = row
                                        },
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_delete),
                                            contentDescription = "删除",
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    if (state.retryable) {
                        TextButton(onClick = { scope.launch { viewModel.refresh() } }) {
                            Text("重试")
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = {
                viewModel.clearActionError()
                onCreateQuestion()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
        ) {
            Text("+ 录入题目")
        }
    }

    pendingDelete?.let { row ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除题目") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("确定删除「${row.questionNumber}」吗？删除后退出复习计划，历史反馈保留。")
                    viewModel.actionError?.let { message ->
                        Text(message, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            if (viewModel.delete(row.questionId)) pendingDelete = null
                        }
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}
