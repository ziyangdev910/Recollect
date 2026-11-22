package com.recollect.feature.progress

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.recollect.common.UiState
import com.recollect.model.Tier
import kotlinx.coroutines.launch

/**
 * 复习进度页：可视化每道题的复习进度与到期时间。
 *
 * 页面只与 [ReviewProgressViewModel] 交互，不直接访问 Room/Context（R-03）。
 * 数据口径见 [ReviewProgressAggregator]。
 */
@Composable
fun ReviewProgressScreen(viewModel: ReviewProgressViewModel) {
    LaunchedEffect(viewModel) { viewModel.refresh() }
    val scope = rememberCoroutineScope()

    when (val state = viewModel.state) {
        UiState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        UiState.Empty -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "题库还是空的。录入题目后，这里会显示每道题的复习进度与到期时间。",
                textAlign = TextAlign.Center,
            )
        }

        is UiState.Content -> ProgressList(state.data)

        is UiState.Error -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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

@Composable
private fun ProgressList(content: ReviewProgressContent) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { StatsCard(content.stats) }
        item { ForecastCard(content) }
        item {
            Text(
                text = "题目进度 · 按到期时间排序",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        items(content.questions, key = { it.questionId }) { question ->
            QuestionProgressCard(question)
        }
    }
}

@Composable
private fun StatsCard(stats: ReviewProgressStats) {
    val colors = MaterialTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp),
        ) {
            StatTile(
                label = "题目总数",
                value = stats.totalQuestions.toString(),
                valueColor = colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "已逾期",
                value = stats.overdueQuestions.toString(),
                valueColor = if (stats.overdueQuestions > 0) colors.error else colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "今天到期",
                value = stats.dueTodayQuestions.toString(),
                valueColor = colors.primary,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "7 天内",
                value = stats.dueWithinWeekQuestions.toString(),
                valueColor = colors.tertiary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
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
private fun ForecastCard(content: ReviewProgressContent) {
    val colors = MaterialTheme.colorScheme
    val upcoming = content.forecast.sumOf { it.count }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "未来 ${ReviewProgressAggregator.FORECAST_DAYS} 天到期分布",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "共 $upcoming 题",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            if (upcoming == 0) {
                Text(
                    text = "未来 ${ReviewProgressAggregator.FORECAST_DAYS} 天没有到期题目。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            } else {
                ForecastChart(content.forecast)
            }
            Text(
                text = "按每道题最早到期的知识点统计；\n已逾期题目不计入柱状图。",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ForecastChart(days: List<DueForecastDay>) {
    val colors = MaterialTheme.colorScheme
    val maxCount = days.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        days.forEach { day ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (day.count > 0) day.count.toString() else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.isToday) colors.primary else colors.onSurfaceVariant,
                    maxLines = 1,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((6f + 64f * day.count / maxCount).dp)
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        .background(if (day.isToday) colors.primary else colors.secondaryContainer),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.isToday) colors.primary else colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun QuestionProgressCard(question: QuestionProgressUi) {
    val colors = MaterialTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "题号 ${question.questionNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                DueBadge(daysUntilDue = question.daysUntilDue)
            }
            Text(
                text = buildString {
                    append("来源页 ")
                    append(question.sourcePage)
                    append(" · ")
                    append(question.lastReviewedDate?.let { "上次复习 $it" } ?: "尚未复习")
                    question.dueDate?.let { append(" · 最早到期 $it") }
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            Text(
                text = question.masterySummary,
                style = MaterialTheme.typography.labelLarge,
            )
            HorizontalDivider(color = colors.outlineVariant)
            question.knowledgePoints.forEach { knowledgePoint ->
                KnowledgePointProgressRow(knowledgePoint)
            }
        }
    }
}

/** 题目级到期徽标：逾期 / 今天 / N 天后 / 未安排。 */
@Composable
private fun DueBadge(daysUntilDue: Long?) {
    val colors = MaterialTheme.colorScheme
    val badge = when {
        daysUntilDue == null -> BadgeStyle("未安排", colors.surfaceVariant, colors.onSurfaceVariant)
        daysUntilDue < 0 -> BadgeStyle(
            "已逾期 ${-daysUntilDue} 天",
            colors.errorContainer,
            colors.onErrorContainer,
        )
        daysUntilDue == 0L -> BadgeStyle(
            "今天到期",
            colors.tertiaryContainer,
            colors.onTertiaryContainer,
        )
        daysUntilDue <= ReviewProgressAggregator.UPCOMING_WINDOW_DAYS -> BadgeStyle(
            "$daysUntilDue 天后",
            colors.secondaryContainer,
            colors.onSecondaryContainer,
        )
        else -> BadgeStyle("$daysUntilDue 天后", colors.surfaceVariant, colors.onSurfaceVariant)
    }
    Badge(text = badge.text, container = badge.container, content = badge.content)
}

@Composable
private fun KnowledgePointProgressRow(knowledgePoint: KnowledgePointProgressUi) {
    val colors = MaterialTheme.colorScheme
    val palette = tierPalette(knowledgePoint.tier)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = buildString {
                    append("#${knowledgePoint.number} ${knowledgePoint.name}")
                    if (knowledgePoint.dormant) append("（休眠）")
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Badge(
                text = knowledgePoint.tierLabel,
                container = palette.container,
                content = palette.content,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { knowledgePoint.masteryPercent / 100f },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp),
                color = palette.accent,
                trackColor = colors.surfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${knowledgePoint.masteryPercent}%",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "到期 ${knowledgePoint.nextDueDate} · " +
                    relativeDueLabel(knowledgePoint.daysUntilDue),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = knowledgePoint.feedbackLabel,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Badge(text: String, container: Color, content: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            maxLines = 1,
        )
    }
}

private data class BadgeStyle(val text: String, val container: Color, val content: Color)

private data class TierPalette(
    /** 进度条颜色。 */
    val accent: Color,
    val container: Color,
    val content: Color,
)

@Composable
private fun tierPalette(tier: Tier): TierPalette {
    val colors = MaterialTheme.colorScheme
    return when (tier) {
        Tier.MASTERED -> TierPalette(colors.primary, colors.primaryContainer, colors.onPrimaryContainer)
        Tier.FAMILIAR -> TierPalette(colors.tertiary, colors.tertiaryContainer, colors.onTertiaryContainer)
        Tier.UNKNOWN -> TierPalette(colors.error, colors.errorContainer, colors.onErrorContainer)
    }
}

private fun relativeDueLabel(daysUntilDue: Long): String = when {
    daysUntilDue < 0 -> "已逾期 ${-daysUntilDue} 天"
    daysUntilDue == 0L -> "今天"
    else -> "$daysUntilDue 天后"
}
