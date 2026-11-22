package com.recollect.feature.confirm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.recollect.data.room.entity.DetectionStatus
import com.recollect.data.room.entity.UserDecision
import com.recollect.model.Tier
import kotlinx.coroutines.launch

/**
 * 确认页（confirm-flow.md）。
 *
 * 每行提供「接受 / 修正 / 本次未复习 / 补填」四个独立操作，**没有**一键全部接受（D-13、R-04）。
 * 存在未标记行时提交按钮禁用，并提示剩余行数；提交仍由 `ApplyFeedbackUseCase` 复核（C-079）。
 */
@Composable
fun ConfirmScreen(viewModel: ConfirmViewModel) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) { viewModel.load() }

    var tierRequest by remember { mutableStateOf<TierRequest?>(null) }

    tierRequest?.let { request ->
        TierDialog(
            title = when (request.action) {
                TierAction.ACCEPT -> "选择看到的结果"
                TierAction.CORRECTED -> "修正为哪一档"
                TierAction.SUPPLEMENTED -> "补填为哪一档"
            },
            onDismiss = { tierRequest = null },
            onPick = { tier ->
                when (request.action) {
                    TierAction.ACCEPT -> viewModel.accept(request.row, tier)
                    TierAction.CORRECTED -> viewModel.correct(request.row, tier)
                    TierAction.SUPPLEMENTED -> viewModel.supplement(request.row, tier)
                }
                tierRequest = null
            },
        )
    }

    when (val state = viewModel.state) {
        ConfirmUiState.Loading -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator()
            Text("正在读取待确认记录…")
        }

        is ConfirmUiState.Empty -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(state.message, style = MaterialTheme.typography.titleMedium)
        }

        is ConfirmUiState.Error -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(state.message, color = MaterialTheme.colorScheme.error)
            if (state.retryable) {
                Button(onClick = { scope.launch { viewModel.load() } }) {
                    Text("重试")
                }
            }
        }

        is ConfirmUiState.Content -> ConfirmContent(
            state = state,
            viewModel = viewModel,
            onNeedTier = { tierRequest = it },
            onSubmit = { scope.launch { viewModel.submit() } },
        )
    }
}

@Composable
private fun ConfirmContent(
    state: ConfirmUiState.Content,
    viewModel: ConfirmViewModel,
    onNeedTier: (TierRequest) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "计划 ${state.header.planId} · 共 ${state.header.pageCount} 页 / ${state.header.rowCount} 行",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "请逐题核对纸面填涂的识别结果。",
            style = MaterialTheme.typography.bodySmall,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.pages.forEach { page ->
                item(key = "page-${page.pageId}") {
                    Text(
                        text = "页 ${page.pageId} · 计划日 ${page.plannedDate}",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                items(page.rows, key = { row -> "${row.questionId}|${row.knowledgePointId}" }) { row ->
                    ConfirmRowCard(
                        row = row,
                        enabled = !state.submitted && !state.submitting,
                        viewModel = viewModel,
                        onNeedTier = onNeedTier,
                    )
                }
            }
        }

        SubmitFooter(state = state, onSubmit = onSubmit)
    }
}

@Composable
private fun ConfirmRowCard(
    row: ConfirmRowView,
    enabled: Boolean,
    viewModel: ConfirmViewModel,
    onNeedTier: (TierRequest) -> Unit,
) {
    val actionsEnabled = enabled && !row.isFuture
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (row.triggered) "触发" else "关联",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (row.triggered) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                )
                Text(
                    text = knowledgeLabel(row),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "题号 ${row.questionNumber}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Text(
                text = "来源页 ${row.sourcePage.ifBlank { "未填写" }}",
                style = MaterialTheme.typography.bodySmall,
            )

            // 2026-11-22 用户要求：卡片直接展示该知识点的复习效果识别结果（不再叫“OMR”）。
            Text(
                text = detectionLabel(row),
                style = MaterialTheme.typography.bodySmall,
            )

            // 只保留一个可编辑文本框，默认填入 OCR 识别文本；原始 OCR 仍在数据库单独保存。
            OutlinedTextField(
                value = row.correctedText.orEmpty(),
                onValueChange = { viewModel.setCorrectedText(row, it) },
                enabled = enabled,
                label = { Text("请核对笔记识别结果") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 3,
                maxLines = 6,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ConfirmActionButton(
                    label = "接受",
                    enabled = actionsEnabled,
                    onClick = {
                        if (viewModel.accept(row) == MarkOutcome.NeedsTier) {
                            onNeedTier(TierRequest(row, TierAction.ACCEPT))
                        }
                    },
                )
                ConfirmActionButton(
                    label = "修正",
                    enabled = actionsEnabled,
                    onClick = { onNeedTier(TierRequest(row, TierAction.CORRECTED)) },
                )
                ConfirmActionButton(
                    label = "本次未复习",
                    enabled = enabled,
                    onClick = { viewModel.notReviewed(row) },
                )
                ConfirmActionButton(
                    label = "补填",
                    enabled = actionsEnabled,
                    onClick = { onNeedTier(TierRequest(row, TierAction.SUPPLEMENTED)) },
                )
            }

            Text(
                text = if (row.isFuture) {
                    "计划日晚于今天：只能标记本次未复习"
                } else {
                    "当前：${decisionLabel(row)}"
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ConfirmActionButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(label)
    }
}

@Composable
private fun SubmitFooter(
    state: ConfirmUiState.Content,
    onSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        state.report?.let { report ->
            ReportCard(report, state.pages)
        }
        state.message?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }
        Text(
            text = when {
                state.submitted -> "本次提交已完成"
                state.unmarkedCount > 0 -> "还有 ${state.unmarkedCount} 行未标记"
                else -> "全部行已标记"
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = onSubmit,
            enabled = state.canSubmit && !state.submitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                when {
                    state.submitted -> "已提交"
                    state.submitting -> "提交中…"
                    else -> "确认提交"
                },
            )
        }
    }
}

@Composable
private fun ReportCard(report: SubmitReport, pages: List<ConfirmPageView>) {
    val numbers = pages.flatMap { it.rows }.associate { it.questionId to it.questionNumber }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (report.skippedQuestionIds.isEmpty()) "提交结果" else "部分生效",
                style = MaterialTheme.typography.titleSmall,
            )
            Text("写入反馈 ${report.writtenFeedbackCount} 条 · 决定日志 ${report.writtenDecisionLogCount} 条")
            if (report.skippedQuestionIds.isNotEmpty()) {
                val skipped = report.skippedQuestionIds
                    .map { numbers[it] ?: it }
                    .distinct()
                    .joinToString("、")
                Text("被跳过的题目：$skipped", color = MaterialTheme.colorScheme.error)
                Text("这些题目的待确认行仍保留，数据修复后可重新提交。")
            }
        }
    }
}

@Composable
private fun TierDialog(
    title: String,
    onDismiss: () -> Unit,
    onPick: (Tier) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Tier.entries.forEach { tier ->
                    Button(
                        onClick = { onPick(tier) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(tierLabel(tier))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

private data class TierRequest(
    val row: ConfirmRowView,
    val action: TierAction,
)

private enum class TierAction {
    ACCEPT,
    CORRECTED,
    SUPPLEMENTED,
}

private fun knowledgeLabel(row: ConfirmRowView): String {
    val name = row.knowledgeName ?: "知识点已删除（${row.knowledgePointId}）"
    val number = row.knowledgeNumber?.toString() ?: "?"
    return "$number · $name"
}

/**
 * 复习效果识别结果：用户核对纸面填涂与识别是否一致（2026-11-22 用户要求）。
 *
 * 不再显示内部术语“OMR”；可直接读取「掌握 / 较熟悉 / 不会」。
 */
private fun detectionLabel(row: ConfirmRowView): String = when (row.detectionStatus) {
    DetectionStatus.OK -> "复习效果识别：${tierName(row.detectedTier)}"
    DetectionStatus.NO_MARK -> "复习效果识别：未填涂"
    DetectionStatus.CONFLICT -> "复习效果识别：填涂冲突（无法判定）"
    DetectionStatus.UNREADABLE -> "复习效果识别：无法识别"
}

private fun tierName(tier: Tier?): String = when (tier) {
    Tier.MASTERED -> "掌握"
    Tier.FAMILIAR -> "较熟悉"
    Tier.UNKNOWN -> "不会"
    null -> "未选择"
}

private fun decisionLabel(row: ConfirmRowView): String = when (row.decision) {
    UserDecision.UNMARKED -> "未标记"
    UserDecision.ACCEPT -> "已接受（${tierLabel(row.detectedTier ?: row.selectedTier)}）"
    UserDecision.CORRECTED -> "已修正（${tierLabel(row.selectedTier)}）"
    UserDecision.NOT_REVIEWED -> "本次未复习（不写反馈）"
    UserDecision.SUPPLEMENTED -> "已补填（${tierLabel(row.selectedTier)}）"
}

private fun tierLabel(tier: Tier?): String = when (tier) {
    Tier.MASTERED -> "A — 掌握"
    Tier.FAMILIAR -> "B — 较熟悉"
    Tier.UNKNOWN -> "C — 不会"
    null -> "未选择"
}
