package com.recollect.feature.planning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

/**
 * 「打印计划」页（app.md）：选择计划日期范围 → 生成计划 → 页面内实时渲染可打印 PDF → 分享。
 *
 * 2026-11-27 用户要求：不再区分「本周计划 / 下周计划」两个固定窗口，改为可选日期范围
 * （最多 7 天），本周/下周的判断只用于填入默认范围；删除独立打印页，只保留分享 PDF。
 * 计划日期与分享按钮固定在页面底部悬浮栏，打印稿预览占满其余页面。
 */
@Composable
fun PlanPreviewScreen(
    viewModel: PlanPreviewViewModel,
    /** 分享面板返回且快照写入成功；由 app 层退回首页并提示「打印完成」。 */
    onShareCompleted: () -> Unit = {},
    /** 首次演示：不分享 PDF，改用已生成的演示打印稿继续回读。 */
    demoMode: Boolean = false,
    onDemoPrint: () -> Unit = {},
    /** 首次演示用：报告打印/分享主按钮的位置，供黑底蒙层聚焦。 */
    onPrimaryActionBounds: (Rect) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    var showRegenerateConfirm by remember { mutableStateOf(false) }
    var showDateRangeDialog by remember { mutableStateOf(false) }
    // 只在本次计划实例首次进入时加载/生成；返回本页时复用原计划，不刷新计划号。
    LaunchedEffect(viewModel) { viewModel.refreshIfNeeded() }

    // 分享面板会暂时让应用失焦；回到本页后再根据分享结果决定是否退回首页。
    var shareReturned by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && viewModel.shareLaunched) {
                shareReturned = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(shareReturned, viewModel.shareRecorded) {
        if (!completed && shareReturned && viewModel.shareRecorded) {
            completed = true
            onShareCompleted()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 打印稿预览一直铺到页面底部，底部只叠加透明的悬浮胶囊栏。
                .padding(start = 16.dp, end = 16.dp, top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            when (val state = viewModel.state) {
                PlanPreviewUiState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "正在按知识点到期情况生成计划…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is PlanPreviewUiState.Content -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "打印稿预览",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = "计划 ${state.planId} · ${state.pages.size} 页 · " +
                                "${state.pages.sumOf { it.questionLabels.size }} 题",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Column 会给非 weighted 子项无界高度；LazyColumn 必须拿到有界高度。
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        // 列表末尾留出悬浮胶囊栏的高度，最后一张打印稿可以滚到胶囊栏上方。
                        contentPadding = PaddingValues(bottom = 84.dp),
                    ) {
                        when (val pdf = state.pdfPreview) {
                            PlanPdfPreviewState.Loading, PlanPdfPreviewState.Unavailable -> item {
                                PdfPreviewPlaceholder()
                            }

                            is PlanPdfPreviewState.Failed -> item {
                                PdfPreviewFailed(pdf.message)
                            }

                            is PlanPdfPreviewState.Ready -> items(
                                pdf.pages,
                                key = { it.hashCode() },
                            ) { page ->
                                PdfPageImage(page)
                            }
                        }
                        if (state.deferred.isNotEmpty()) {
                            item { DeferredList(state.deferred) }
                        }
                        if (state.warnings.isNotEmpty()) {
                            item { WarningList(state.warnings) }
                        }
                        item {
                            // 显式重新生成：新计划号会替代该范围的当前计划；默认路径只复用已有计划。
                            TextButton(
                                onClick = { showRegenerateConfirm = true },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = REGENERATE_PLAN_LABEL,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                is PlanPreviewUiState.Empty -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    EmptyPlanCard(state.message)
                    DeferredList(state.deferred)
                    WarningList(state.warnings)
                    TextButton(
                        onClick = { showRegenerateConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = REGENERATE_PLAN_LABEL,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is PlanPreviewUiState.Error -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = state.error.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                    if (state.error.retryable) {
                        Button(onClick = { scope.launch { viewModel.refresh() } }) {
                            Text("重试")
                        }
                    }
                }
            }
        }

        // 透明悬浮胶囊栏：日期起止 + 上箭头与分享按钮浮在打印稿之上，不占页面底部的矩形区域。
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            viewModel.shareMessage?.let { message ->
                MessageCard(message = message, isError = viewModel.shareMessageIsError)
            }
            PlanBottomBar(
                startDate = viewModel.startDate,
                endDate = viewModel.endDate,
                onEditDates = { showDateRangeDialog = true },
                contentReady = viewModel.state is PlanPreviewUiState.Content,
                sharing = viewModel.sharing,
                demoMode = demoMode,
                onShare = { scope.launch { viewModel.sharePdf() } },
                onDemoPrint = onDemoPrint,
                onPrimaryActionBounds = onPrimaryActionBounds,
            )
        }
    }

    if (showDateRangeDialog) {
        PlanDateRangeDialog(
            startDate = viewModel.startDate,
            endDate = viewModel.endDate,
            onDismiss = { showDateRangeDialog = false },
            onConfirm = { start, end ->
                showDateRangeDialog = false
                scope.launch { viewModel.selectRange(start, end) }
            },
        )
    }

    // 打开已有计划时，若题库在计划生成后发生变化，先询问是否重新生成。
    if (viewModel.bankChanged) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBankChanged() },
            title = { Text("题库已更新") },
            text = { Text("当前题库与上次生成计划时不同，是否重新生成计划？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { viewModel.regenerate() }
                    },
                ) {
                    Text("重新生成")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissBankChanged() }) {
                    Text("保持当前计划")
                }
            },
        )
    }

    if (showRegenerateConfirm) {
        AlertDialog(
            onDismissRequest = { showRegenerateConfirm = false },
            title = { Text(REGENERATE_PLAN_LABEL) },
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

/** 透明悬浮胶囊栏：排版与首页底部三个按钮一致，左侧日期范围 + 上箭头，右侧分享/演示操作。 */
@Composable
private fun PlanBottomBar(
    startDate: LocalDate,
    endDate: LocalDate,
    onEditDates: () -> Unit,
    contentReady: Boolean,
    sharing: Boolean,
    demoMode: Boolean,
    onShare: () -> Unit,
    onDemoPrint: () -> Unit,
    onPrimaryActionBounds: (Rect) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    // 与首页底部一致的悬浮胶囊：胶囊自身悬浮于打印稿之上，周围保持透明，
    // 内部两个操作项使用无填充的图标 + 文字样式。
    Surface(
        shape = CircleShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 日期起止 + 向上的小箭头：提示点击此处可以修改计划日期范围。
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onEditDates)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "计划日期",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        text = "${startDate.monthValue}/${startDate.dayOfMonth} – " +
                            "${endDate.monthValue}/${endDate.dayOfMonth}",
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_expand_less),
                    contentDescription = "修改日期",
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            PillDivider()
            val primaryEnabled = if (demoMode) contentReady else contentReady && !sharing
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        enabled = primaryEnabled,
                        onClick = { if (demoMode) onDemoPrint() else onShare() },
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .onGloballyPositioned { onPrimaryActionBounds(it.boundsInRoot()) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(
                        if (demoMode) R.drawable.ic_print else R.drawable.ic_share,
                    ),
                    contentDescription = null,
                    tint = if (primaryEnabled) {
                        colors.onSurface
                    } else {
                        colors.onSurfaceVariant.copy(alpha = 0.4f)
                    },
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = when {
                        demoMode -> "演示打印稿"
                        sharing -> "生成中…"
                        else -> "分享 PDF"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (primaryEnabled) {
                        colors.onSurface
                    } else {
                        colors.onSurfaceVariant.copy(alpha = 0.4f)
                    },
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PillDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(18.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

/**
 * 修改计划日期范围：日历直接嵌在「计划日期」卡片里，点「开始」或「结束」切换当前编辑的一端，
 * 再在下方日历中点选日期；确认后重新打开该范围的计划（最多 7 天）。
 */
@Composable
private fun PlanDateRangeDialog(
    startDate: LocalDate,
    endDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit,
) {
    var draftStart by remember(startDate) { mutableStateOf(startDate) }
    var draftEnd by remember(endDate) { mutableStateOf(endDate) }
    var activeField by remember { mutableStateOf(DateField.START) }
    val days = ChronoUnit.DAYS.between(draftStart, draftEnd).toInt() + 1

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "计划日期",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "共 $days 天（最多 7 天）",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DateFieldButton(
                        label = "开始",
                        date = draftStart,
                        modifier = Modifier.weight(1f),
                        selected = activeField == DateField.START,
                        onClick = { activeField = DateField.START },
                    )
                    Text(
                        text = "至",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    DateFieldButton(
                        label = "结束",
                        date = draftEnd,
                        modifier = Modifier.weight(1f),
                        selected = activeField == DateField.END,
                        onClick = { activeField = DateField.END },
                    )
                }

                InlinePlanCalendar(
                    activeField = activeField,
                    startDate = draftStart,
                    endDate = draftEnd,
                    onStartChange = { date ->
                        // 修改开始日时保持原来的天数，超过 7 天则收回。
                        val span = ChronoUnit.DAYS.between(draftStart, draftEnd)
                        draftStart = date
                        draftEnd = date.plusDays(span.coerceIn(0, MAX_PLAN_DAYS - 1))
                    },
                    onEndChange = { date ->
                        draftEnd = date.coerceIn(
                            draftStart,
                            draftStart.plusDays(MAX_PLAN_DAYS - 1),
                        )
                    },
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) { Text("取消") }
                    TextButton(
                        onClick = { onConfirm(draftStart, draftEnd) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    ) { Text("确定") }
                }
            }
        }
    }
}

/** 卡片内嵌日历：按当前编辑的一端限制可选范围，并在选中日期时直接更新草稿。 */
@Composable
private fun InlinePlanCalendar(
    activeField: DateField,
    startDate: LocalDate,
    endDate: LocalDate,
    onStartChange: (LocalDate) -> Unit,
    onEndChange: (LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    // 开始日可在近一个月到未来两个月内选；结束日被限制在开始日 + 6 天内（范围最多 7 天）。
    val selectableFirst = if (activeField == DateField.START) {
        today.minusDays(31)
    } else {
        startDate
    }
    val selectableLast = if (activeField == DateField.START) {
        today.plusDays(62)
    } else {
        startDate.plusDays(MAX_PLAN_DAYS - 1)
    }
    val selectedDate = if (activeField == DateField.START) startDate else endDate
    // 切换编辑的一端时回到该日期所在月份。
    var displayMonth by remember(activeField) { mutableStateOf(YearMonth.from(selectedDate)) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${displayMonth.year}年${displayMonth.monthValue}月",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            MonthArrow(
                label = "‹",
                enabled = displayMonth > YearMonth.from(selectableFirst),
                onClick = { displayMonth = displayMonth.minusMonths(1) },
            )
            MonthArrow(
                label = "›",
                enabled = displayMonth < YearMonth.from(selectableLast),
                onClick = { displayMonth = displayMonth.plusMonths(1) },
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        val firstOfMonth = displayMonth.atDay(1)
        val leadingBlanks = firstOfMonth.dayOfWeek.value % 7
        val days = (1..displayMonth.lengthOfMonth()).map { displayMonth.atDay(it) }
        val cells = List(leadingBlanks) { null as LocalDate? } +
            days.map { it as LocalDate? }
        val padded = cells + List((7 - cells.size % 7) % 7) { null }
        padded.chunked(7).forEach { week ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp),
            ) {
                week.forEach { date ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                selected = date == selectedDate,
                                today = date == today,
                                enabled = !date.isBefore(selectableFirst) &&
                                    !date.isAfter(selectableLast),
                                onClick = {
                                    if (activeField == DateField.START) {
                                        onStartChange(date)
                                    } else {
                                        onEndChange(date)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthArrow(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) colors.onSurface else colors.onSurfaceVariant.copy(alpha = 0.3f),
        )
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    selected: Boolean,
    today: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .then(
                if (today && !selected) {
                    Modifier.border(1.dp, colors.primary, CircleShape)
                } else {
                    Modifier
                },
            )
            .background(if (selected) colors.primary else Color.Transparent)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = when {
                selected -> colors.onPrimary
                enabled -> colors.onSurface
                else -> colors.onSurfaceVariant.copy(alpha = 0.35f)
            },
        )
    }
}

private enum class DateField { START, END }

@Composable
private fun DateFieldButton(
    label: String,
    date: LocalDate,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.small,
        color = if (selected) colors.primaryContainer else colors.surfaceVariant,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        border = if (selected) {
            BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f))
        } else {
            null
        },
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) colors.primary else colors.onSurfaceVariant,
            )
            Text(
                text = "${date.monthValue}月${date.dayOfMonth}日 ${weekdayLabel(date.dayOfWeek)}",
                style = MaterialTheme.typography.titleSmall,
                color = if (selected) colors.onPrimaryContainer else colors.onSurface,
            )
        }
    }
}

/** 单页打印稿：与分享 PDF 使用同一份布局，白底纸张样式。 */
@Composable
private fun PdfPageImage(page: ImageBitmap) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Image(
            bitmap = page,
            contentDescription = "打印稿预览页",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PdfPreviewPlaceholder() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "正在渲染可打印 PDF…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PdfPreviewFailed(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(14.dp),
        )
    }
}

/** 被推迟题目：让“计划里少排了哪些题、为什么”有可读解释。 */
@Composable
private fun DeferredList(deferred: List<PlanPreviewDeferred>) {
    if (deferred.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.tertiaryContainer),
        border = BorderStroke(1.dp, colors.tertiary.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "有 ${deferred.size} 道题未排入本次计划",
                style = MaterialTheme.typography.titleSmall,
                color = colors.onTertiaryContainer,
            )
            deferred.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(colors.onTertiaryContainer, shape = MaterialTheme.shapes.extraSmall),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${item.questionLabel} · ${item.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onTertiaryContainer,
                        )
                    }
                    Text(
                        text = item.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onTertiaryContainer.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 11.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WarningList(warnings: List<PlanPreviewWarning>) {
    if (warnings.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "需要处理的问题",
            style = MaterialTheme.typography.titleMedium,
        )
        warnings.forEach { warning ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.errorContainer),
                border = BorderStroke(1.dp, colors.error.copy(alpha = 0.18f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "题号 ${warning.questionNumber} · ${warning.label}",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onErrorContainer,
                    )
                    Text(
                        text = warning.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onErrorContainer.copy(alpha = 0.85f),
                    )
                }
            }
        }
    }
}

/** 空计划：说明为什么没有可打印内容，并保留推迟/警告信息。 */
@Composable
private fun EmptyPlanCard(message: String) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.secondaryContainer),
        border = BorderStroke(1.dp, colors.secondary.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "本次没有可打印的题目",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSecondaryContainer,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSecondaryContainer.copy(alpha = 0.85f),
            )
        }
    }
}

/** 分享结果：成功用主色容器，失败用错误容器。 */
@Composable
private fun MessageCard(message: String, isError: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isError) colors.errorContainer else colors.primaryContainer,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) colors.onErrorContainer else colors.onPrimaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        )
    }
}

private fun weekdayLabel(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
    DayOfWeek.MONDAY -> "周一"
    DayOfWeek.TUESDAY -> "周二"
    DayOfWeek.WEDNESDAY -> "周三"
    DayOfWeek.THURSDAY -> "周四"
    DayOfWeek.FRIDAY -> "周五"
    DayOfWeek.SATURDAY -> "周六"
    DayOfWeek.SUNDAY -> "周日"
}
