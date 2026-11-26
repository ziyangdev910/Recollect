package com.recollect.feature.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.recollect.common.UiState
import com.recollect.common.formatQuestionLabelLines
import com.recollect.data.room.repository.EntrySummary
import kotlinx.coroutines.launch

/**
 * Entry 页面（entry.md）：题目列表占满页面。
 *
 * 新建/编辑使用独立的 [QuestionEditScreen]；本页只负责列表、进入编辑页与删除（归档）。
 * 页面只与 [EntryViewModel] 交互，不直接访问 Room/Context（R-03）。
 *
 * 2026-11-25 排版调整：搜索框带图标与清除入口；题目卡片以两行大标题展示「来源」与
 * 「第X页 第Y题」（无来源时第一行为「编号X」），耗时与知识点标签并排，整张卡片可进入编辑；
 * 空题库时把主操作放到页面中央，不再显示悬浮按钮。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryScreen(
    viewModel: EntryViewModel,
    listState: LazyListState,
    onCreatePlan: () -> Unit,
    onCreateQuestion: () -> Unit,
    onEditQuestion: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    // 从编辑页返回时复用同一 ViewModel 与 LazyListState；仅在首次进入或保存后刷新列表。
    LaunchedEffect(viewModel) { viewModel.refreshIfNeeded() }

    var pendingDelete by remember { mutableStateOf<EntrySummary?>(null) }

    val allQuestions = (viewModel.state as? UiState.Content)?.data?.questions.orEmpty()
    val filteredQuestions = filterQuestions(allQuestions, viewModel.query)
    val libraryEmpty = viewModel.state is UiState.Empty ||
        (viewModel.state is UiState.Content && allQuestions.isEmpty())

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 题号、来源页码与知识点都可作为关键词；输入时即时过滤。
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = viewModel::updateQuery,
                placeholder = { Text("搜索题号、页码或知识点") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = if (viewModel.query.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.updateQuery("") }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = "清除搜索",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    null
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when {
                        viewModel.query.isNotBlank() ->
                            "匹配 ${filteredQuestions.size} / ${allQuestions.size} 道题"
                        allQuestions.isNotEmpty() -> "共 ${allQuestions.size} 道题"
                        else -> "题库"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (allQuestions.isNotEmpty()) {
                    Text(
                        text = "按修改时间倒序排序",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            when (val state = viewModel.state) {
                UiState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }

                UiState.Empty -> EmptyLibrary(
                    modifier = Modifier.weight(1f),
                    onCreateQuestion = onCreateQuestion,
                )

                is UiState.Content -> {
                    when {
                        allQuestions.isEmpty() -> EmptyLibrary(
                            modifier = Modifier.weight(1f),
                            onCreateQuestion = onCreateQuestion,
                        )

                        filteredQuestions.isEmpty() -> NoMatch(
                            modifier = Modifier.weight(1f),
                            query = viewModel.query,
                            onClearQuery = { viewModel.updateQuery("") },
                        )

                        else -> LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            // 底部留出两个悬浮按钮的高度，避免遮挡最后一张卡片。
                            contentPadding = PaddingValues(bottom = 176.dp),
                        ) {
                            items(filteredQuestions, key = { it.questionId }) { row ->
                                QuestionCard(
                                    row = row,
                                    onEdit = {
                                        viewModel.clearActionError()
                                        onEditQuestion(row.questionId)
                                    },
                                    onDelete = {
                                        viewModel.clearActionError()
                                        pendingDelete = row
                                    },
                                )
                            }
                        }
                    }
                }

                is UiState.Error -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    if (state.retryable) {
                        TextButton(onClick = { scope.launch { viewModel.refresh() } }) {
                            Text("重试")
                        }
                    }
                }
            }
        }

        // 空题库时页面中央已经有「录入第 1 道题」，不再重复显示悬浮按钮。
        // 已有题目时，「预览计划」是下一步主操作，放在「录入题目」下方并高亮；
        // 「录入题目」退为中性，避免两个主按钮同时抢注意力。
        if (!libraryEmpty) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.clearActionError()
                        onCreateQuestion()
                    },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("录入题目")
                }
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.clearActionError()
                        onCreatePlan()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_plan),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("预览计划")
                }
            }
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

/** 空题库的教学式空态：说明下一步，并直接给出唯一的创建入口。 */
@Composable
private fun EmptyLibrary(
    modifier: Modifier = Modifier,
    onCreateQuestion: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "题库还是空的",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "先录入第一道题。之后可以按知识点到期情况，\n生成可打印的周计划。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onCreateQuestion) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("录入第 1 道题")
        }
    }
}

/** 搜索无结果时保留原查询，并提供一键清除。 */
@Composable
private fun NoMatch(
    modifier: Modifier = Modifier,
    query: String,
    onClearQuery: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "没有找到「$query」",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "可以换一个题号、页码或知识点关键词。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onClearQuery) { Text("清除搜索") }
    }
}

/**
 * 题目卡片：大标题分两行——来源（无来源时「编号X」）与「第X页 第Y题」；其下耗时与知识点标签并排。
 *
 * 整张卡片可点进入编辑；右侧的编辑/删除按钮保持独立点击区。
 */
@Composable
private fun QuestionCard(
    row: EntrySummary,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        border = BorderStroke(1.dp, colors.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    // 2026-11-25 用户要求：来源与「第X页 第Y题」分行显示；无来源时第一行为「编号X」。
                    val labelLines = formatQuestionLabelLines(row.sourcePage, row.questionNumber)
                    Column {
                        Text(
                            text = labelLines.first,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (labelLines.second.isNotBlank()) {
                            Text(
                                text = labelLines.second,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    // 耗时与知识点并排；FlowRow 让标签整体换行，大字号下不会把“分钟”拆成两行。
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        DurationChip(row.estimatedReviewMinutes)
                        row.knowledgeNames.forEach { name ->
                            KnowledgeChip(name)
                        }
                    }
                }
                IconButton(onClick = onEdit) {
                    Icon(
                        painter = painterResource(R.drawable.ic_edit),
                        contentDescription = "编辑",
                        tint = colors.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = "删除",
                        tint = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** 耗时标签：中性底色，与赭石色知识点标签并排；时钟图标 + 分钟数。 */
@Composable
private fun DurationChip(minutes: Int) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.small,
        color = colors.surfaceContainerHighest,
        contentColor = colors.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_clock),
                contentDescription = null,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = "$minutes 分钟",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}

/** 知识点标签：赭石色底，和主色的计划/按钮区分开。 */
@Composable
private fun KnowledgeChip(name: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.small,
        color = colors.secondaryContainer,
        contentColor = colors.onSecondaryContainer,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
