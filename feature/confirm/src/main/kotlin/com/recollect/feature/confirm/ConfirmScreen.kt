package com.recollect.feature.confirm

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.recollect.common.formatQuestionLabel
import com.recollect.data.room.entity.DetectionStatus
import com.recollect.data.room.entity.UserDecision
import com.recollect.model.Tier
import kotlinx.coroutines.launch

/**
 * 确认页（confirm-flow.md）。
 *
 * 每行仍有「接受 / 修改复习效果 / 本次未复习」三种决策，**没有**一键全部接受（D-13、R-04）。
 * 存在未标记行时提交按钮禁用；提交仍由 `ApplyFeedbackUseCase` 复核（C-079）。
 *
 * 2026-11-25 排版调整：顶部显示整批确认进度。
 *
 * 2026-11-28 排版调整（用户要求）：
 * - 同一题的多个知识点合并为一张紧凑卡片，标题统一为「来源 第X页 第Y题」，
 *   去掉触发、题号、来源页等分散标签；
 * - 复习效果收进胶囊，点击右侧铅笔图标打开紧凑的「修改【知识点】掌握程度」弹窗，
 *   四个档位改为一行可选标签，默认选中当前状态（未填涂或日期超前时为「未复习」）；
 *   弹窗只暂存选择，必须再点卡片「确认」才完成这道题的校对；
 * - 笔记识别结果改为紧凑文本框；卡片确认后折叠为一行「复习笔记」文本；
 * - 底部改为右对齐的胶囊「完成」按钮，并删去「还有 X 行未标记」提示。
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
        ReviewEffectDialog(
            row = request.row,
            allowTiers = !request.row.isFuture,
            onDismiss = { tierRequest = null },
            onPick = { tier ->
                // 只暂存选择；卡片上的「确认」才真正完成校对（2026-11-28 用户要求）。
                viewModel.setPendingEffect(
                    request.row,
                    if (tier == null) EffectChoice.NotReviewed else EffectChoice.Rated(tier),
                )
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
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp, top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ConfirmProgressHeader(
                planId = state.header.planId,
                pageCount = state.header.pageCount,
                rowCount = state.header.rowCount,
                markedRows = markedRows,
                totalRows = totalRows,
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                // 底部为悬浮「」按钮预留空间，最后一张卡片可完整滚出按钮下方。
                contentPadding = PaddingValues(top = 2.dp, bottom = 92.dp),
            ) {
                state.pages.forEachIndexed { pageIndex, page ->
                    item(key = "page-${page.pageId}") {
                        PageHeader(index = pageIndex, page = page)
                    }
                    // 同一题的多个知识点合并成一张卡片，标题只出现一次。
                    val questionCards = page.rows.groupBy { it.questionId }.values.toList()
                    items(
                        items = questionCards,
                        key = { rows -> "q-${page.pageId}-${rows.first().questionId}" },
                    ) { rows ->
                        ConfirmQuestionCard(
                            rows = rows,
                            enabled = !state.submitted && !state.submitting,
                            viewModel = viewModel,
                            onNeedTier = onNeedTier,
                        )
                    }
                }
            }
        }

        SubmitOverlay(
            state = state,
            onSubmit = onSubmit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

/** 顶部进度：一屏内就能看到「已经确认了多少行」，占用的高度尽量小。 */
@Composable
private fun ConfirmProgressHeader(
    planId: String,
    pageCount: Int,
    rowCount: Int,
    markedRows: Int,
    totalRows: Int,
) {
    val colors = MaterialTheme.colorScheme
    val progress = if (totalRows == 0) 0f else markedRows.toFloat() / totalRows
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "计划 $planId",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "共 $pageCount 页 / $rowCount 题",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$markedRows / $totalRows",
                style = MaterialTheme.typography.titleSmall,
                color = colors.primary,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = colors.primary,
            trackColor = colors.surfaceVariant,
            // 0 行已标记时不画终点圆点，避免误读为“接近完成”。
            drawStopIndicator = {},
        )
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
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "计划日 ${page.plannedDate}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 一道题的校对卡片：标题只保留「来源 第X页 第Y题」，下面逐知识点列出复习效果。
 *
 * 点铅笔图标修改对应知识点的复习效果；卡片底部只保留右对齐的「确认」按钮，
 * 确认后收起文本框、显示对勾与「复习笔记」。
 */
@Composable
private fun ConfirmQuestionCard(
    rows: List<ConfirmRowView>,
    enabled: Boolean,
    viewModel: ConfirmViewModel,
    onNeedTier: (TierRequest) -> Unit,
) {
    val first = rows.first()
    val decided = rows.all { it.decision != UserDecision.UNMARKED }
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        border = BorderStroke(1.dp, colors.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatQuestionLabel(first.sourcePage, first.questionNumber),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (decided) {
                    Icon(
                        painter = painterResource(R.drawable.ic_row_confirmed),
                        contentDescription = "本题确认已完成",
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            rows.forEach { row ->
                KnowledgeEffectRow(
                    row = row,
                    // 已确认的行锁定复习效果，不再允许修改（2026-11-28 用户要求）。
                    enabled = enabled && row.decision == UserDecision.UNMARKED,
                    onEdit = { onNeedTier(TierRequest(row)) },
                )
            }

            if (decided) {
                Text(
                    text = reviewNoteText(first.correctedText),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            } else {
                CompactNoteField(
                    value = first.correctedText.orEmpty(),
                    onValueChange = { viewModel.setCorrectedText(rows, it) },
                    enabled = enabled,
                )
            }

            if (!decided) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilledTonalButton(
                        onClick = {
                            // 弹窗里的选择只暂存；这里才把所有未标记行落定为最终决策。
                            // 仍有行没有可用档位时，先让用户为该行选择，不自动确认。
                            val undecided = rows.filter { it.decision == UserDecision.UNMARKED }
                            val needsChoice = undecided.firstOrNull {
                                it.pendingEffect == null &&
                                    (it.isFuture || it.detectedTier == null)
                            }
                            if (needsChoice != null) {
                                onNeedTier(TierRequest(needsChoice))
                            } else {
                                undecided.forEach { viewModel.confirm(it) }
                            }
                        },
                        enabled = enabled,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                    ) {
                        Text("确认", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

/** 知识点 + 复习效果胶囊 + 编辑图标。 */
@Composable
private fun KnowledgeEffectRow(
    row: ConfirmRowView,
    enabled: Boolean,
    onEdit: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = knowledgeLabel(row),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        EffectChip(row)
        EffectEditButton(onClick = onEdit, enabled = enabled)
    }
}

/** 紧凑笔记框：没有识别到文字时以「笔记识别结果为空」占位。 */
@Composable
private fun CompactNoteField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.bodySmall.copy(color = colors.onSurface)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        textStyle = textStyle,
        cursorBrush = SolidColor(colors.primary),
        minLines = 1,
        maxLines = 5,
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.outlineVariant),
            ) {
                Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    if (value.isEmpty()) {
                        Text(
                            text = "笔记识别结果为空",
                            style = textStyle,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

@Composable
private fun EffectEditButton(onClick: () -> Unit, enabled: Boolean) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_confirm_edit),
            contentDescription = "修改复习反馈",
            tint = if (enabled) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp),
        )
    }
}

private data class ChipTone(val container: Color, val content: Color)

/**
 * 当前用于展示的效果：已落定的决策优先；未标记时展示弹窗暂存的选择，
 * 再退回识别结果的预览。
 */
private fun currentChoice(row: ConfirmRowView): EffectChoice? = when (row.decision) {
    UserDecision.ACCEPT -> (row.detectedTier ?: row.selectedTier)?.let(EffectChoice::Rated)
    UserDecision.CORRECTED -> row.selectedTier?.let(EffectChoice::Rated)
    UserDecision.NOT_REVIEWED -> EffectChoice.NotReviewed
    UserDecision.UNMARKED -> row.pendingEffect
        ?: row.detectedTier?.let(EffectChoice::Rated)
}

private fun currentTier(row: ConfirmRowView): Tier? =
    (currentChoice(row) as? EffectChoice.Rated)?.tier

@Composable
private fun effectTone(row: ConfirmRowView): ChipTone {
    val colors = MaterialTheme.colorScheme
    if (currentChoice(row) == EffectChoice.NotReviewed) {
        return ChipTone(colors.surfaceVariant, colors.onSurfaceVariant)
    }
    return when (currentTier(row)) {
        Tier.MASTERED -> ChipTone(colors.primaryContainer, colors.onPrimaryContainer)
        Tier.FAMILIAR -> ChipTone(colors.secondaryContainer, colors.onSecondaryContainer)
        Tier.UNKNOWN -> ChipTone(colors.errorContainer, colors.onErrorContainer)
        null -> ChipTone(colors.surfaceVariant, colors.onSurfaceVariant)
    }
}

@Composable
private fun EffectChip(row: ConfirmRowView) {
    val tone = effectTone(row)
    Surface(
        shape = RoundedCornerShape(50),
        color = tone.container,
        contentColor = tone.content,
    ) {
        Text(
            text = effectLabel(row),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            maxLines = 1,
        )
    }
}

/** 底部悬浮区：错误提示 + 右对齐的胶囊「完成」。 */
@Composable
private fun SubmitOverlay(
    state: ConfirmUiState.Content,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        state.message?.let { message ->
            Text(
                text = message,
                color = colors.error,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        Button(
            onClick = onSubmit,
            enabled = state.canSubmit && !state.submitting,
            shape = CircleShape,
            // 悬浮胶囊使用正常的主色背景，未全部标记时由默认禁用色变灰（2026-11-28 用户要求）。
            colors = ButtonDefaults.buttonColors(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            modifier = Modifier.height(40.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_row_confirmed),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when {
                    state.submitted -> "已提交"
                    state.submitting -> "提交中…"
                    else -> "完成"
                },
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** 四个复习效果选项，从左至右：掌握 / 较熟悉 / 不会 / 未复习。 */
private val effectChoices: List<Tier?> = listOf(
    Tier.MASTERED,
    Tier.FAMILIAR,
    Tier.UNKNOWN,
    null,
)

/**
 * 修改复习效果悬浮窗（2026-11-28 用户要求）：四个档位改为一行可选标签，
 * 默认勾选当前状态；未填涂或计划日超前的行默认「未复习」。
 */
@Composable
private fun ReviewEffectDialog(
    row: ConfirmRowView,
    /** 计划日晚于今天的行只能标记「本次未复习」。 */
    allowTiers: Boolean,
    onDismiss: () -> Unit,
    onPick: (Tier?) -> Unit,
) {
    var picked by remember(row) { mutableStateOf(defaultEffect(row)) }
    val knowledge = row.knowledgeName ?: "知识点已删除"
    RecollectDialog(
        title = "修改【$knowledge】掌握程度",
        onDismissRequest = onDismiss,
        subtitle = if (allowTiers) null else "计划日晚于今天，只能标记本次未复习",
        actions = {
            RecollectDialogAction(text = "取消", onClick = onDismiss)
            Spacer(modifier = Modifier.width(4.dp))
            Button(
                onClick = { onPick(picked) },
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 18.dp),
            ) {
                Text("确认", style = MaterialTheme.typography.labelMedium)
            }
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            effectChoices.forEach { choice ->
                EffectOption(
                    label = effectName(choice),
                    selected = choice == picked,
                    enabled = allowTiers || choice == null,
                    onClick = { picked = choice },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** 与科目选择一致的可选标签：选中时用主色底、较粗描边并在前面加对勾。 */
@Composable
private fun EffectOption(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val container = when {
        !enabled -> colors.surfaceVariant.copy(alpha = 0.5f)
        selected -> colors.primaryContainer
        else -> colors.surface
    }
    val contentColor = when {
        !enabled -> colors.onSurfaceVariant.copy(alpha = 0.45f)
        selected -> colors.onPrimaryContainer
        else -> colors.onSurfaceVariant
    }
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = container,
        contentColor = contentColor,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) contentColor.copy(alpha = 0.58f) else colors.outlineVariant,
        ),
    ) {
        Text(
            text = if (selected) "✓ $label" else label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

private data class TierRequest(
    val row: ConfirmRowView,
)

private fun knowledgeLabel(row: ConfirmRowView): String {
    val name = row.knowledgeName ?: "知识点已删除（${row.knowledgePointId}）"
    val number = row.knowledgeNumber?.toString() ?: "?"
    return "$number · $name"
}

/**
 * 复习效果识别结果：用户核对纸面填涂与识别是否一致（2026-11-22 用户要求）。
 *
 * 卡片上的胶囊显示当前生效的效果：已修改看用户选择，已接受看识别结果；
 * 未标记时展示识别预览；标记为「本次未复习」后显示「复习反馈：未复习」。
 * 不再显示内部术语“OMR”；可直接读取「掌握 / 较熟悉 / 不会」。
 */
private fun effectLabel(row: ConfirmRowView): String {
    when (val choice = currentChoice(row)) {
        EffectChoice.NotReviewed -> return "复习反馈：未复习"
        is EffectChoice.Rated -> return "复习反馈：${tierName(choice.tier)}"
        null -> Unit
    }
    return when (row.detectionStatus) {
        DetectionStatus.OK -> "复习反馈：未选择"
        DetectionStatus.NO_MARK -> "复习反馈：未填涂"
        DetectionStatus.CONFLICT -> "复习反馈：填涂冲突"
        DetectionStatus.UNREADABLE -> "复习反馈：无法识别"
    }
}

private fun tierName(tier: Tier): String = when (tier) {
    Tier.MASTERED -> "掌握"
    Tier.FAMILIAR -> "较熟悉"
    Tier.UNKNOWN -> "不会"
}

private fun effectName(tier: Tier?): String = tier?.let(::tierName) ?: "未复习"

/**
 * 弹窗默认选中：当前决策档位；未标记时取识别结果；
 * 未填涂、识别失败或计划日超前时默认「未复习」。
 */
private fun defaultEffect(row: ConfirmRowView): Tier? {
    if (row.isFuture) return null
    return when (row.decision) {
        UserDecision.ACCEPT -> row.detectedTier ?: row.selectedTier
        UserDecision.CORRECTED -> row.selectedTier
        UserDecision.NOT_REVIEWED -> null
        UserDecision.UNMARKED -> when (val effect = row.pendingEffect) {
            is EffectChoice.Rated -> effect.tier
            EffectChoice.NotReviewed -> null
            null -> row.detectedTier
        }
    }
}
