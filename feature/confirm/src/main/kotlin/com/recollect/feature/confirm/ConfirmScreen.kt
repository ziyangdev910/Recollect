package com.recollect.feature.confirm

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.recollect.data.room.entity.DetectionStatus
import com.recollect.data.room.entity.UserDecision
import com.recollect.model.Tier
import kotlinx.coroutines.launch

/**
 * 确认页（confirm-flow.md）。
 *
 * 每行提供「确认 / 修改复习效果 / 本次未复习」三个独立操作，**没有**一键全部接受（D-13、R-04）。
 * 存在未标记行时提交按钮禁用，并提示剩余行数；提交仍由 `ApplyFeedbackUseCase` 复核（C-079）。
 *
 * 2026-11-25 排版调整：顶部显示整批确认进度；每行把识别结果做成语义色标签，
 * 决策完成后收起操作按钮、只显示对勾与最终选择，降低长列表的视觉噪音。
 */
@Composable
fun ConfirmScreen(
    viewModel: ConfirmViewModel,
    /** 整批提交成功回调；由 app 层退回首页并提示「确认完成」。 */
    onConfirmed: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) { viewModel.load() }

    var tierRequest by remember { mutableStateOf<TierRequest?>(null) }

    tierRequest?.let { request ->
        TierDialog(
            title = when (request.action) {
                TierAction.ACCEPT -> "选择看到的结果"
                TierAction.CORRECTED -> "修改为哪一档"
            },
            onDismiss = { tierRequest = null },
            onPick = { tier ->
                when (request.action) {
                    TierAction.ACCEPT -> viewModel.accept(request.row, tier)
                    TierAction.CORRECTED -> viewModel.correct(request.row, tier)
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
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "正在读取待确认记录…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is ConfirmUiState.Empty -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = state.message,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }

        is ConfirmUiState.Error -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = state.message,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
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
            onSubmit = {
                scope.launch {
                    if (viewModel.submit()) onConfirmed()
                }
            },
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
    val totalRows = state.pages.sumOf { it.rows.size }
    val markedRows = (totalRows - state.unmarkedCount).coerceAtLeast(0)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ConfirmProgressHeader(
            planId = state.header.planId,
            pageCount = state.header.pageCount,
            rowCount = state.header.rowCount,
            markedRows = markedRows,
            totalRows = totalRows,
            unmarkedCount = state.unmarkedCount,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            state.pages.forEachIndexed { pageIndex, page ->
                item(key = "page-${page.pageId}") {
                    PageHeader(index = pageIndex, page = page)
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

/** 顶部进度：一屏内就能看到「已经确认了多少行」，提交按钮与剩余行数互相呼应。 */
@Composable
private fun ConfirmProgressHeader(
    planId: String,
    pageCount: Int,
    rowCount: Int,
    markedRows: Int,
    totalRows: Int,
    unmarkedCount: Int,
) {
    val colors = MaterialTheme.colorScheme
    val progress = if (totalRows == 0) 0f else markedRows.toFloat() / totalRows
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "计划 $planId",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "共 $pageCount 页 / $rowCount 题",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Text(
                text = "$markedRows / $totalRows",
                style = MaterialTheme.typography.titleMedium,
                color = colors.primary,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = colors.primary,
            trackColor = colors.surfaceVariant,
            // 0 行已标记时不画终点圆点，避免误读为“接近完成”。
            drawStopIndicator = {},
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "逐题核对纸面识别结果，完成后提交",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (unmarkedCount > 0) "还有 $unmarkedCount 行待标记" else "全部已标记",
                style = MaterialTheme.typography.labelSmall,
                color = if (unmarkedCount > 0) colors.tertiary else colors.primary,
            )
        }
    }
}

@Composable
private fun PageHeader(index: Int, page: ConfirmPageView) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "第 ${index + 1} 页 · ${page.pageId}",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "计划日 ${page.plannedDate}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ConfirmRowCard(
    row: ConfirmRowView,
    enabled: Boolean,
    viewModel: ConfirmViewModel,
    onNeedTier: (TierRequest) -> Unit,
) {
    // 单行一旦完成决策（确认 / 修改复习效果 / 本次未复习），锁定文本框与全部按钮并显示对勾，
    // 让用户明确知道“本题的确认已经结束”（2026-11-22 用户要求）。
    val decided = row.decision != UserDecision.UNMARKED
    val fieldEnabled = enabled && !decided
    val actionsEnabled = fieldEnabled && !row.isFuture
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        border = BorderStroke(1.dp, colors.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TriggerChip(triggered = row.triggered)
                Text(
                    text = knowledgeLabel(row),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    text = "题号 ${row.questionNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "来源页 ${row.sourcePage.ifBlank { "未填写" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                DetectionChip(row)
            }

            // 只保留一个可编辑文本框，默认填入 OCR 识别文本；原始 OCR 仍在数据库单独保存。
            OutlinedTextField(
                value = row.correctedText.orEmpty(),
                onValueChange = { viewModel.setCorrectedText(row, it) },
                enabled = fieldEnabled,
                label = { Text("校对笔记识别结果") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 2,
                maxLines = 6,
            )

            if (!decided) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalButton(
                        onClick = {
                            if (viewModel.accept(row) == MarkOutcome.NeedsTier) {
                                onNeedTier(TierRequest(row, TierAction.ACCEPT))
                            }
                        },
                        enabled = actionsEnabled,
                        modifier = Modifier.height(40.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                    ) {
                        Text("确认", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = { onNeedTier(TierRequest(row, TierAction.CORRECTED)) },
                        enabled = actionsEnabled,
                        modifier = Modifier.height(40.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                    ) {
                        Text("修改复习效果", style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(
                        onClick = { viewModel.notReviewed(row) },
                        enabled = fieldEnabled,
                        modifier = Modifier.height(40.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Text("本次未复习", style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DecisionChip(row)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        painter = painterResource(R.drawable.ic_row_confirmed),
                        contentDescription = "本题确认已完成",
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            // 决策完成后上方已经有最终选择标签，这里不再重复；只保留未来日期的限制说明。
            if (!decided) {
                Text(
                    text = if (row.isFuture) {
                        "计划日晚于今天，只能标记本次未复习"
                    } else {
                        decisionLabel(row)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (row.isFuture) colors.tertiary else colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TriggerChip(triggered: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (triggered) colors.primaryContainer else colors.secondaryContainer,
        contentColor = if (triggered) colors.onPrimaryContainer else colors.onSecondaryContainer,
    ) {
        Text(
            text = if (triggered) "触发" else "关联",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

private data class ChipTone(val container: Color, val content: Color)

@Composable
private fun detectionTone(row: ConfirmRowView): ChipTone {
    val colors = MaterialTheme.colorScheme
    if (row.decision == UserDecision.NOT_REVIEWED) {
        return ChipTone(colors.surfaceVariant, colors.onSurfaceVariant)
    }
    return when (row.detectionStatus) {
        DetectionStatus.OK -> when (row.detectedTier) {
            Tier.MASTERED -> ChipTone(colors.primaryContainer, colors.onPrimaryContainer)
            Tier.FAMILIAR -> ChipTone(colors.secondaryContainer, colors.onSecondaryContainer)
            Tier.UNKNOWN -> ChipTone(colors.errorContainer, colors.onErrorContainer)
            null -> ChipTone(colors.surfaceVariant, colors.onSurfaceVariant)
        }

        DetectionStatus.NO_MARK,
        DetectionStatus.CONFLICT,
        DetectionStatus.UNREADABLE,
        -> ChipTone(colors.surfaceVariant, colors.onSurfaceVariant)
    }
}

@Composable
private fun DetectionChip(row: ConfirmRowView) {
    val tone = detectionTone(row)
    Surface(
        shape = MaterialTheme.shapes.small,
        color = tone.container,
        contentColor = tone.content,
    ) {
        Text(
            text = detectionLabel(row),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun DecisionChip(row: ConfirmRowView) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (row.decision) {
        UserDecision.ACCEPT -> colors.primaryContainer to colors.onPrimaryContainer
        UserDecision.CORRECTED -> colors.secondaryContainer to colors.onSecondaryContainer
        UserDecision.NOT_REVIEWED -> colors.surfaceVariant to colors.onSurfaceVariant
        UserDecision.UNMARKED -> colors.surfaceVariant to colors.onSurfaceVariant
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content,
    ) {
        Text(
            text = decisionLabel(row),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun SubmitFooter(
    state: ConfirmUiState.Content,
    onSubmit: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLow,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.report?.let { report ->
                ReportBlock(report, state.pages)
            }
            state.message?.let { message ->
                Text(
                    text = message,
                    color = colors.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = when {
                    state.submitted -> "本次提交已完成"
                    state.unmarkedCount > 0 -> "还有 ${state.unmarkedCount} 行未标记"
                    else -> "全部行已标记，可以提交"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.unmarkedCount > 0) colors.onSurfaceVariant else colors.primary,
            )
            Button(
                onClick = onSubmit,
                enabled = state.canSubmit && !state.submitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
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
}

/**
 * 提交结果摘要：直接排在底部面板内，不再套一层卡片，避免“面板里再放卡片”。
 */
@Composable
private fun ReportBlock(report: SubmitReport, pages: List<ConfirmPageView>) {
    val colors = MaterialTheme.colorScheme
    val numbers = pages.flatMap { it.rows }.associate { it.questionId to it.questionNumber }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = if (report.skippedQuestionIds.isEmpty()) "提交结果" else "部分生效",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = "写入反馈 ${report.writtenFeedbackCount} 条 · 决定日志 ${report.writtenDecisionLogCount} 条",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        if (report.skippedQuestionIds.isNotEmpty()) {
            val skipped = report.skippedQuestionIds
                .map { numbers[it] ?: it }
                .distinct()
                .joinToString("、")
            Text(
                text = "被跳过的题目：$skipped",
                color = colors.error,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "这些题目的待确认行仍保留，数据修复后可重新提交。",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 4.dp),
            color = colors.outlineVariant,
        )
    }
}

@Composable
private fun TierDialog(
    title: String,
    onDismiss: () -> Unit,
    onPick: (Tier) -> Unit,
) {
    RecollectDialog(
        title = title,
        onDismissRequest = onDismiss,
        actions = {
            RecollectDialogAction(text = "取消", onClick = onDismiss)
        },
    ) {
        Tier.entries.forEach { tier ->
            Button(
                onClick = { onPick(tier) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
            ) {
                Text(tierLabel(tier))
            }
        }
    }
}

private data class TierRequest(
    val row: ConfirmRowView,
    val action: TierAction,
)

private enum class TierAction {
    ACCEPT,
    CORRECTED,
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
 * 标记为「本次未复习」后，该行直接显示「复习效果：本次未复习」（2026-11-22 用户要求）。
 */
private fun detectionLabel(row: ConfirmRowView): String = when {
    row.decision == UserDecision.NOT_REVIEWED -> "复习效果：本次未复习"
    else -> when (row.detectionStatus) {
        DetectionStatus.OK -> "复习效果：${tierName(row.detectedTier)}"
        DetectionStatus.NO_MARK -> "复习效果：未填涂"
        DetectionStatus.CONFLICT -> "复习效果：填涂冲突（无法判定）"
        DetectionStatus.UNREADABLE -> "复习效果：无法识别"
    }
}

private fun tierName(tier: Tier?): String = when (tier) {
    Tier.MASTERED -> "掌握"
    Tier.FAMILIAR -> "较熟悉"
    Tier.UNKNOWN -> "不会"
    null -> "未选择"
}

private fun decisionLabel(row: ConfirmRowView): String = when (row.decision) {
    UserDecision.UNMARKED -> "未标记"
    UserDecision.ACCEPT -> "已确认（${tierLabel(row.detectedTier ?: row.selectedTier)}）"
    UserDecision.CORRECTED -> "已修改复习效果（${tierLabel(row.selectedTier)}）"
    UserDecision.NOT_REVIEWED -> "本次未复习（不写反馈）"
}

private fun tierLabel(tier: Tier?): String = when (tier) {
    Tier.MASTERED -> "A — 掌握"
    Tier.FAMILIAR -> "B — 较熟悉"
    Tier.UNKNOWN -> "C — 不会"
    null -> "未选择"
}
