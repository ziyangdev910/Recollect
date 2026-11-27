package com.recollect.feature.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.recollect.common.UiState
import com.recollect.common.formatQuestionLabelLines
import com.recollect.data.room.repository.EntrySummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.launch

/**
 * 错题本页面（2026-11-27 用户要求，参照 reference/EntryWindow.png 重做）。
 *
 * 顶部为「全部错题 / 知识分类」选项卡，其下是来源、知识点与录入时间筛选；
 * 题目按日期分组显示为卡片；底部胶囊栏只保留「录入」「打印」两个操作（去掉「复习」）。
 * 从首页每个分类卡片进入时，[categoryId] 限定只显示该分类下的题目。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryScreen(
    viewModel: EntryViewModel,
    listState: LazyListState,
    onCreatePlan: () -> Unit,
    onCreateQuestion: () -> Unit,
    onEditQuestion: (String) -> Unit,
    /** 当前错题本对应的知识点分类；null 表示「全部错题」。 */
    categoryId: String? = null,
    /** 知识点分类 id → 名称，用于「知识分类」分组与筛选。 */
    categoryNames: Map<String, String> = emptyMap(),
    /** 顶部「更多」菜单中的知识点管理入口。 */
    onManageKnowledge: () -> Unit = {},
    /** 首次演示用：报告「打印」按钮的位置，供黑底蒙层聚焦。 */
    onPlanButtonBounds: (Rect) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    // 从编辑页返回时复用同一 ViewModel 与 LazyListState；仅在首次进入或保存后刷新列表。
    LaunchedEffect(viewModel) { viewModel.refreshIfNeeded() }

    // 切换错题本时回到默认视图；同一错题本内进入编辑页再返回时保留搜索与筛选。
    LaunchedEffect(categoryId) { viewModel.selectNotebook(categoryId) }

    var mainMenuExpanded by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<EntrySummary?>(null) }

    val sourceFilter = viewModel.sourceFilter
    val knowledgeFilter = viewModel.knowledgeFilter
    val newestFirst = viewModel.newestFirst
    val tab = if (viewModel.groupByKnowledge) EntryTab.KNOWLEDGE else EntryTab.ALL

    val allQuestions = (viewModel.state as? UiState.Content)?.data?.questions.orEmpty()
    val notebookQuestions = remember(allQuestions, categoryId) {
        if (categoryId == null) {
            allQuestions
        } else {
            allQuestions.filter { categoryId in it.categoryIds }
        }
    }
    val searched = filterQuestions(notebookQuestions, viewModel.query)
    val filtered = remember(searched, sourceFilter, knowledgeFilter, newestFirst) {
        val rows = searched
            .filter { sourceFilter == null || it.sourcePageLabel == sourceFilter }
            .filter { knowledgeFilter == null || knowledgeFilter in it.knowledgeNames }
        if (newestFirst) rows.sortedByDescending { it.updatedAt } else rows.sortedBy { it.updatedAt }
    }
    val sourceOptions = remember(notebookQuestions) {
        notebookQuestions.map { it.sourcePageLabel }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val knowledgeOptions = remember(notebookQuestions) {
        notebookQuestions.flatMap { it.knowledgeNames }.distinct().sorted()
    }
    val hasFilter = sourceFilter != null || knowledgeFilter != null || !newestFirst ||
        viewModel.query.isNotBlank()
    val libraryEmpty = viewModel.state is UiState.Empty ||
        (viewModel.state is UiState.Content && notebookQuestions.isEmpty())

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            EntryHeader(
                tab = tab,
                onTabChange = { viewModel.updateGroupByKnowledge(it == EntryTab.KNOWLEDGE) },
                onToggleSearch = { viewModel.toggleSearchVisible() },
                onManageKnowledge = {
                    mainMenuExpanded = false
                    onManageKnowledge()
                },
                menuExpanded = mainMenuExpanded,
                onMenuExpandedChange = { mainMenuExpanded = it },
            )

            if (viewModel.searchVisible) {
                OutlinedTextField(
                    value = viewModel.query,
                    onValueChange = viewModel::updateQuery,
                    placeholder = { Text("搜索来源、页码、题号或知识点") },
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 8.dp),
                )
            }

            FilterRow(
                sourceOptions = sourceOptions,
                knowledgeOptions = knowledgeOptions,
                sourceFilter = sourceFilter,
                knowledgeFilter = knowledgeFilter,
                newestFirst = newestFirst,
                hasFilter = hasFilter,
                onSourceChange = viewModel::updateSourceFilter,
                onKnowledgeChange = viewModel::updateKnowledgeFilter,
                onToggleTimeOrder = viewModel::toggleTimeOrder,
                onClearFilters = viewModel::clearFilters,
            )

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
                        notebookQuestions.isEmpty() -> EmptyLibrary(
                            modifier = Modifier.weight(1f),
                            onCreateQuestion = onCreateQuestion,
                        )

                        filtered.isEmpty() -> NoMatch(
                            modifier = Modifier.weight(1f),
                            query = viewModel.query,
                            hasFilter = hasFilter,
                            onClear = viewModel::clearFilters,
                        )

                        else -> EntryQuestionList(
                            tab = tab,
                            rows = filtered,
                            categoryNames = categoryNames,
                            listState = listState,
                            onEdit = { row ->
                                viewModel.clearActionError()
                                onEditQuestion(row.questionId)
                            },
                            onDelete = { row ->
                                viewModel.clearActionError()
                                pendingDelete = row
                            },
                        )
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

        EntryBottomBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 20.dp),
            onCreateQuestion = onCreateQuestion,
            onCreatePlan = onCreatePlan,
            onPlanButtonBounds = onPlanButtonBounds,
        )
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

private enum class EntryTab { ALL, KNOWLEDGE }

private const val NO_CATEGORY_KEY = "__none__"

/** 顶部选项卡与右侧搜索/更多入口。 */
@Composable
private fun EntryHeader(
    tab: EntryTab,
    onTabChange: (EntryTab) -> Unit,
    onToggleSearch: () -> Unit,
    onManageKnowledge: () -> Unit,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EntryTabLabel(
            text = "全部错题",
            selected = tab == EntryTab.ALL,
            onClick = { onTabChange(EntryTab.ALL) },
        )
        Spacer(modifier = Modifier.width(18.dp))
        EntryTabLabel(
            text = "知识分类",
            selected = tab == EntryTab.KNOWLEDGE,
            onClick = { onTabChange(EntryTab.KNOWLEDGE) },
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onToggleSearch) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = "搜索",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            IconButton(onClick = { onMenuExpandedChange(true) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_more),
                    contentDescription = "更多",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { onMenuExpandedChange(false) },
            ) {
                DropdownMenuItem(
                    text = { Text("管理知识点与分类") },
                    onClick = onManageKnowledge,
                )
            }
        }
    }
}

@Composable
private fun EntryTabLabel(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            // 用最大固有宽度锁住文字宽度；中文可以在任意字间断行，Min 会把选项卡压成一列。
            .width(IntrinsicSize.Max)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) colors.onSurface else androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}

/** 来源 / 知识点 / 录入时间筛选行。 */
@Composable
private fun FilterRow(
    sourceOptions: List<String>,
    knowledgeOptions: List<String>,
    sourceFilter: String?,
    knowledgeFilter: String?,
    newestFirst: Boolean,
    hasFilter: Boolean,
    onSourceChange: (String?) -> Unit,
    onKnowledgeChange: (String?) -> Unit,
    onToggleTimeOrder: () -> Unit,
    onClearFilters: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterPill(
            label = sourceFilter ?: "来源",
            active = sourceFilter != null,
            options = listOf("全部来源") + sourceOptions,
            selectedOption = sourceFilter ?: "全部来源",
            onSelect = { option ->
                onSourceChange(option.takeUnless { it == "全部来源" })
            },
        )
        FilterPill(
            label = knowledgeFilter ?: "知识点",
            active = knowledgeFilter != null,
            options = listOf("全部知识点") + knowledgeOptions,
            selectedOption = knowledgeFilter ?: "全部知识点",
            onSelect = { option ->
                onKnowledgeChange(option.takeUnless { it == "全部知识点" })
            },
        )
        FilterPill(
            label = if (newestFirst) "最近录入" else "最早录入",
            active = !newestFirst,
            options = listOf("最近录入", "最早录入"),
            selectedOption = if (newestFirst) "最近录入" else "最早录入",
            onSelect = { option ->
                val shouldBeNewest = option == "最近录入"
                if (shouldBeNewest != newestFirst) onToggleTimeOrder()
            },
        )
        if (hasFilter) {
            TextButton(
                onClick = onClearFilters,
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                Text("清除", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/** 带下拉菜单的筛选胶囊。 */
@Composable
private fun FilterPill(
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
            border = BorderStroke(1.dp, if (active) colors.primary.copy(alpha = 0.2f) else colors.outlineVariant),
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

/** 题目列表：全部错题按日期分组，知识分类按分类分组（同题可出现在多个分类下）。 */
@Composable
private fun EntryQuestionList(
    tab: EntryTab,
    rows: List<EntrySummary>,
    categoryNames: Map<String, String>,
    listState: LazyListState,
    onEdit: (EntrySummary) -> Unit,
    onDelete: (EntrySummary) -> Unit,
) {
    val sections: List<Pair<String, List<EntrySummary>>> = when (tab) {
        EntryTab.ALL -> rows
            .groupBy { dateGroupLabel(it.updatedAt) }
            .toList()

        EntryTab.KNOWLEDGE -> rows
            .flatMap { row ->
                val ids = row.categoryIds.ifEmpty { setOf(NO_CATEGORY_KEY) }
                ids.map { id -> id to row }
            }
            .groupBy({ it.first }, { it.second })
            .map { (id, items) ->
                val name = if (id == NO_CATEGORY_KEY) {
                    "未分类"
                } else {
                    categoryNames[id] ?: "未分类"
                }
                name to items
            }
            .sortedBy { it.first }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        // 底部留出胶囊栏的高度，避免遮挡最后一张卡片。
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 128.dp),
    ) {
        sections.forEach { (title, items) ->
            item(key = "header-$title") {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            items(items, key = { row -> "$title-${row.questionId}" }) { row ->
                QuestionCard(
                    row = row,
                    onEdit = { onEdit(row) },
                    onDelete = { onDelete(row) },
                )
            }
        }
        item(key = "end") {
            Text(
                text = "— 到底了 —",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            )
        }
    }
}

private fun dateGroupLabel(updatedAt: Instant): String {
    val zone = ZoneId.systemDefault()
    val date = updatedAt.atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    return when (date) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> "更早"
    }
}

/** 空错题本的教学式空态：说明下一步，并直接给出唯一的创建入口。 */
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
            text = "这个本子还没有题目",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "先录入第一道题。之后可以按知识点到期情况，\n生成可打印的计划。",
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

/** 搜索或筛选无结果时保留条件，并提供一键清除。 */
@Composable
private fun NoMatch(
    modifier: Modifier = Modifier,
    query: String,
    hasFilter: Boolean,
    onClear: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = if (query.isNotBlank()) "没有找到「$query」" else "没有符合筛选条件的题目",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (hasFilter) "可以换一个关键词，或清除筛选条件。" else "可以先录入新题。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onClear) { Text("清除条件") }
    }
}

/**
 * 题目卡片：大标题分两行——来源（无来源时「编号X」）与「第X页 第Y题」；其下耗时与知识点标签并排。
 *
 * 整张卡片可点进入编辑；右上角的「更多」菜单提供编辑与删除（归档）。
 */
@Composable
private fun QuestionCard(
    row: EntrySummary,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var menuExpanded by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onEdit),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLowest,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // 来源与「第X页 第Y题」分行显示；无来源时第一行为「编号X」。
                    val labelLines = formatQuestionLabelLines(row.sourcePage, row.questionNumber)
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
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more),
                            contentDescription = "题目操作",
                            tint = colors.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("编辑") },
                            onClick = {
                                menuExpanded = false
                                onEdit()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("删除", color = colors.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }
            FlowRow(
                modifier = Modifier.padding(end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                DurationChip(row.estimatedReviewMinutes)
                row.knowledgeNames.forEach { name ->
                    KnowledgeChip(name)
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

/** 底部胶囊栏：「录入 | 打印」。 */
@Composable
private fun EntryBottomBar(
    modifier: Modifier,
    onCreateQuestion: () -> Unit,
    onCreatePlan: () -> Unit,
    onPlanButtonBounds: (Rect) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EntryPillAction(
                iconRes = R.drawable.ic_add,
                label = "录入",
                onClick = onCreateQuestion,
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(18.dp)
                    .background(colors.outlineVariant),
            )
            EntryPillAction(
                iconRes = R.drawable.ic_print,
                label = "打印",
                onClick = onCreatePlan,
                modifier = Modifier.onGloballyPositioned {
                    onPlanButtonBounds(it.boundsInRoot())
                },
            )
        }
    }
}

@Composable
private fun EntryPillAction(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}
