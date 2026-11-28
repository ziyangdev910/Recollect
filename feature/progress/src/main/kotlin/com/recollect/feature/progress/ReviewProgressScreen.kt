package com.recollect.feature.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.recollect.common.UiState
import com.recollect.common.formatQuestionLabelLines
import com.recollect.model.Tier
import kotlinx.coroutines.launch

/**
 * 复习进度页：可视化每道题的复习进度与到期时间。
 *
 * 页面只与 [ReviewProgressViewModel] 交互，不直接访问 Room/Context（R-03）。
 * 数据口径见 [ReviewProgressAggregator]。
 */
@Composable
fun ReviewProgressScreen(
    viewModel: ReviewProgressViewModel,
    /** 科目卡片配色（container to content），由 app 层按首页分类封面解析；null 时使用主题中性色。 */
    subjectTone: ((String) -> Pair<Color, Color>)? = null,
) {
    LaunchedEffect(viewModel) { viewModel.refresh() }
    val scope = rememberCoroutineScope()

    when (val state = viewModel.state) {
        UiState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        UiState.Empty -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "还没有复习进度",
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "录入题目后，这里会显示每道题的掌握度、\n到期时间与未来两周的安排。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        is UiState.Content -> ProgressList(state.data, subjectTone)

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

/** 「题目进度」列表的排序方式；默认保持原有的按到期日。 */
private enum class ProgressSort(val label: String) {
    DUE("按到期日"),
    NEWEST("最近修改"),
    OLDEST("最早修改"),
}

@Composable
private fun ProgressList(
    content: ReviewProgressContent,
    subjectTone: ((String) -> Pair<Color, Color>)?,
) {
    // 筛选只作用于「题目进度」列表；上方统计与未来分布保持全量口径。
    var subjectFilter by remember { mutableStateOf<String?>(null) }
    var sort by remember { mutableStateOf(ProgressSort.DUE) }
    // 科目筛选与「添加题目」页一致：点开后在筛选行下方展开标签卡片。
    var subjectExpanded by remember { mutableStateOf(false) }
    val categoryOptions = remember(content.questions) {
        content.questions.flatMap { it.categoryNames }.distinct().sorted()
    }
    LaunchedEffect(categoryOptions) {
        if (subjectFilter != null && subjectFilter !in categoryOptions) {
            subjectFilter = null
        }
    }
    val filtered = remember(content.questions, subjectFilter, sort) {
        val rows = subjectFilter?.let { subject ->
            content.questions.filter { subject in it.categoryNames }
        } ?: content.questions
        when (sort) {
            ProgressSort.DUE -> rows
            ProgressSort.NEWEST -> rows.sortedByDescending { it.updatedAt }
            ProgressSort.OLDEST -> rows.sortedBy { it.updatedAt }
        }
    }
    val hasFilter = subjectFilter != null || sort != ProgressSort.DUE

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { StatsCard(content.stats) }
        item { ForecastCard(content) }
        item {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "题目进度",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "共 ${filtered.size} 题",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            ProgressFilterRow(
                subjectFilter = subjectFilter,
                sort = sort,
                hasFilter = hasFilter,
                subjectExpanded = subjectExpanded,
                onToggleSubject = { subjectExpanded = !subjectExpanded },
                onSortChange = { sort = it },
                onClear = {
                    subjectFilter = null
                    sort = ProgressSort.DUE
                    subjectExpanded = false
                },
            )
        }
        if (subjectExpanded) {
            item {
                SubjectFilterPanel(
                    categories = categoryOptions,
                    selected = subjectFilter,
                    subjectTone = subjectTone,
                    onSelect = { value ->
                        subjectFilter = value
                        subjectExpanded = false
                    },
                )
            }
        }
        if (filtered.isEmpty()) {
            item {
                Text(
                    text = "没有符合筛选条件的题目。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                )
            }
        } else {
            items(filtered, key = { it.questionId }) { question ->
                QuestionProgressCard(question)
            }
        }
    }
}

/** 与错题本列表一致的筛选行：科目胶囊 + 修改时间排序胶囊 + 可选「清除」。 */
@Composable
private fun ProgressFilterRow(
    subjectFilter: String?,
    sort: ProgressSort,
    hasFilter: Boolean,
    subjectExpanded: Boolean,
    onToggleSubject: () -> Unit,
    onSortChange: (ProgressSort) -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            item {
                ProgressTogglePill(
                    label = subjectFilter ?: "科目",
                    active = subjectFilter != null,
                    expanded = subjectExpanded,
                    onClick = onToggleSubject,
                )
            }
            item {
                ProgressFilterPill(
                    label = sort.label,
                    active = sort != ProgressSort.DUE,
                    options = ProgressSort.entries.map { it.label },
                    selectedOption = sort.label,
                    onSelect = { label ->
                        onSortChange(ProgressSort.entries.first { it.label == label })
                    },
                )
            }
        }
        if (hasFilter) {
            TextButton(
                onClick = onClear,
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                Text("清除", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** 无下拉菜单的科目筛选胶囊：点开后在筛选行下方展开与「添加题目」页一致的科目卡片。 */
@Composable
private fun ProgressTogglePill(
    label: String,
    active: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = if (active) colors.primaryContainer else colors.surfaceContainerLow,
        contentColor = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant,
        border = BorderStroke(
            1.dp,
            if (active) colors.primary.copy(alpha = 0.2f) else colors.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp),
            )
            Icon(
                painter = painterResource(R.drawable.ic_expand_more),
                contentDescription = if (expanded) "收起" else "展开",
                modifier = Modifier
                    .size(16.dp)
                    .rotate(if (expanded) 180f else 0f),
            )
        }
    }
}

/** 科目筛选卡片面板：颜色与首页分类封面一致（由 app 层传入）。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectFilterPanel(
    categories: List<String>,
    selected: String?,
    subjectTone: ((String) -> Pair<Color, Color>)?,
    onSelect: (String?) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SubjectFilterCard(
            label = "全部科目",
            selected = selected == null,
            container = if (selected == null) colors.primaryContainer else colors.surfaceContainerLow,
            contentColor = if (selected == null) colors.onPrimaryContainer else colors.onSurface,
            onClick = { onSelect(null) },
        )
        categories.forEach { name ->
            val isSelected = name == selected
            val tone = subjectTone?.invoke(name)
            SubjectFilterCard(
                label = name,
                selected = isSelected,
                container = tone?.first
                    ?: if (isSelected) colors.primaryContainer else colors.surfaceContainerLow,
                contentColor = tone?.second
                    ?: if (isSelected) colors.onPrimaryContainer else colors.onSurface,
                onClick = { onSelect(name) },
            )
        }
    }
}

/** 与「添加题目」页科目选项同款的小卡片。 */
@Composable
private fun SubjectFilterCard(
    label: String,
    selected: Boolean,
    container: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = container,
        contentColor = contentColor,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) {
                contentColor.copy(alpha = 0.58f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (selected) {
                Text(text = "✓", style = MaterialTheme.typography.labelMedium)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }
    }
}

/** 带下拉菜单的筛选胶囊；与错题本列表的 FilterPill 保持同一视觉。 */
@Composable
private fun ProgressFilterPill(
    label: String,
    active: Boolean,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            shape = CircleShape,
            color = if (active) colors.primaryContainer else colors.surfaceContainerLow,
            contentColor = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant,
            border = BorderStroke(
                1.dp,
                if (active) colors.primary.copy(alpha = 0.2f) else colors.outlineVariant,
            ),
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { expanded = true }
                    .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 140.dp),
                )
                Icon(
                    painter = painterResource(R.drawable.ic_expand_more),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = if (option == selectedOption) {
                                    colors.primary
                                } else {
                                    colors.onSurface
                                },
                            )
                        },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsCard(stats: ReviewProgressStats) {
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
                .padding(horizontal = 8.dp, vertical = 18.dp),
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        border = BorderStroke(1.dp, colors.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
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
                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                        .background(
                            if (day.isToday) colors.primary else colors.primary.copy(alpha = 0.32f),
                        ),
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
    // 不能按 "-" 直接拆分来源：无来源题目的 sourcePage 只有编号（如 "1"），
    // 旧实现用 split("-")[1] 会在滚动到这类卡片时抛 IndexOutOfBoundsException。
    val labelLines = formatQuestionLabelLines(question.sourcePage, question.questionNumber)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = buildString {
                        append(labelLines.first)
                        if (labelLines.second.isNotBlank()) {
                            append('\n')
                            append(labelLines.second)
                        }
                    },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                DueBadge(daysUntilDue = question.daysUntilDue)
            }
            Text(
                text = buildString {
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
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = palette.accent,
                trackColor = colors.surfaceVariant,
                // 0% 时不画终点圆点，避免把“未掌握”误读为接近满格。
                drawStopIndicator = {},
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
