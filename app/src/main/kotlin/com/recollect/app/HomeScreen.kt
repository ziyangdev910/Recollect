package com.recollect.app

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.recollect.app.ui.RecollectDialog
import com.recollect.app.ui.RecollectDialogAction
import com.recollect.feature.entry.NOTEBOOK_TONE_COUNT
import com.recollect.feature.entry.notebookTone
import org.ziyangdev910.recollect.R

/** 首页错题本卡片的数据投影；[id] 为知识点分类 id。 */
internal data class HomeCategory(
    val id: String,
    val name: String,
    val questionCount: Int,
    /** 新页面创建时选中的封面；null 表示按分类顺序/名称自动派生。 */
    val coverTone: Int? = null,
    val coverIcon: String? = null,
)

/**
 * 首页（2026-11-27 用户要求，按 reference/MainWindow.png 重做）。
 *
 * 知识点分类作为错题本排成三列卡片网格；底部胶囊栏固定「录入 / 打印计划 / 扫描回读」。
 * 设置、复习进度、知识点与分类管理等入口收进顶部「更多」菜单（由 `AppRoot` 渲染）。
 *
 * 长按分类卡片直接进入编辑态：卡片轻微抖动、可拖动改变顺序，右上角出现「编辑封面」笔按钮
 * 与「删除分类」红叉按钮；不再弹出长按菜单，点击顶栏「完成」退出编辑态（2026-11-27 用户要求）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HomeScreen(
    categories: List<HomeCategory>?,
    editing: Boolean,
    onEditingChange: (Boolean) -> Unit,
    onReorder: (List<String>) -> Unit,
    onEditCover: (HomeCategory, OfficialCover) -> Unit,
    onDeleteCategory: (HomeCategory) -> Unit,
    onOpenNotebook: (categoryId: String?, name: String) -> Unit,
    onAddCategory: () -> Unit,
    onCreateQuestion: () -> Unit,
    onCreatePlan: () -> Unit,
    onScan: () -> Unit,
    /** 首次演示用：报告「录入」按钮的位置，供黑底蒙层聚焦。 */
    onRecordButtonBounds: (Rect) -> Unit = {},
    /** 首次演示：高亮「录入」按钮（底部栏始终可见，无需滚动）。 */
    focusRecordButton: Boolean = false,
) {
    val gridState = rememberLazyGridState()
    var draggingId by remember { mutableStateOf<String?>(null) }
    var pressedId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    // 正在等待确认删除的分类；不为 null 时弹出紧凑的确认/拦截浮窗。
    var deleteTarget by remember { mutableStateOf<HomeCategory?>(null) }
    // 编辑模式下使用本地顺序，拖拽换位时立即交换并播放动画；退出编辑后回到数据库顺序。
    // 进入编辑的一帧内就用当前分类填充，避免网格先渲染空列表再跳回顶部。
    var editingOrder by remember(editing) {
        mutableStateOf(if (editing) categories.orEmpty() else null)
    }

    // 进入/退出编辑态，以及分类在编辑期间发生变化（例如删除成功后首页重读）时，
    // 同步本地顺序：保留拖拽后的相对次序，移除已不存在的分类，追加新分类。
    LaunchedEffect(editing, categories) {
        if (!editing) {
            editingOrder = null
            draggingId = null
            pressedId = null
            dragOffset = Offset.Zero
            deleteTarget = null
            return@LaunchedEffect
        }
        val source = categories.orEmpty()
        val current = editingOrder
        editingOrder = if (current == null) {
            source
        } else {
            val byId = source.associateBy { it.id }
            val kept = current.mapNotNull { byId[it.id] }
            val keptIds = kept.mapTo(mutableSetOf()) { it.id }
            kept + source.filter { it.id !in keptIds }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            // 底部为胶囊栏留出空间，避免最后一行被遮挡。
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 132.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (categories == null) {
                // 分类读取中：保留占位卡，避免网格高度跳变。
                items(3) { index ->
                    NotebookPlaceholder(toneIndex = index + 1)
                }
            } else if (editing) {
                val ordered = editingOrder.orEmpty()
                items(ordered, key = { it.id }) { category ->
                    val index = ordered.indexOf(category)
                    val dragging = draggingId == category.id
                    // 拖拽中的卡片跟随手指、不参与位置动画；其余卡片平滑让位。
                    Box(
                        modifier = if (dragging) {
                            Modifier
                        } else {
                            Modifier.animateItem(
                                placementSpec = tween(
                                    durationMillis = 220,
                                    easing = FastOutSlowInEasing,
                                ),
                            )
                        },
                    ) {
                        EditableCategoryCard(
                            category = category,
                            toneIndex = categoryTone(category),
                            iconRes = categoryIcon(category),
                            index = index,
                            dragging = dragging,
                            pressed = pressedId == category.id,
                            dragOffset = dragOffset,
                            onPressStart = { pressedId = category.id },
                            onPressEnd = {
                                if (pressedId == category.id) pressedId = null
                            },
                            onDragStart = {
                                draggingId = category.id
                                dragOffset = Offset.Zero
                            },
                            onDrag = { delta ->
                                dragOffset += delta
                                // 卡片中心进入另一张卡片后立即交换，而不是等松手。
                                val swap = liveSwap(
                                    gridState = gridState,
                                    categories = editingOrder.orEmpty(),
                                    draggedId = category.id,
                                    dragOffset = dragOffset,
                                )
                                if (swap != null) {
                                    editingOrder = swap.first
                                    dragOffset = swap.second
                                }
                            },
                            onDragEnd = {
                                val finalOrder = editingOrder.orEmpty()
                                draggingId = null
                                pressedId = null
                                dragOffset = Offset.Zero
                                onReorder(finalOrder.map { it.id })
                            },
                            onDragCancel = {
                                draggingId = null
                                pressedId = null
                                dragOffset = Offset.Zero
                            },
                            onEditCover = {
                                onEditCover(
                                    category,
                                    resolveOfficialCover(
                                        name = category.name,
                                        coverTone = category.coverTone,
                                        coverIcon = category.coverIcon,
                                        fallbackTone = defaultToneFor(category.name),
                                    ),
                                )
                            },
                            onDelete = { deleteTarget = category },
                        )
                    }
                }
            } else {
                items(categories, key = { it.id }) { category ->
                    NotebookCard(
                        title = category.name,
                        subtitle = "${category.questionCount} 题",
                        toneIndex = categoryTone(category),
                        iconRes = categoryIcon(category),
                        modifier = Modifier.combinedClickable(
                            onClick = { onOpenNotebook(category.id, category.name) },
                            // 长按直接进入编辑态（抖动 + 拖拽 + 卡片操作按钮），不再弹菜单。
                            onLongClick = { onEditingChange(true) },
                        ),
                    )
                }
            }
            if (!editing) {
                item(key = "manage") {
                    NotebookCard(
                        title = "添加分类",
                        subtitle = "",
                        toneIndex = 0,
                        iconRes = R.drawable.ic_add,
                        outlined = true,
                        modifier = Modifier.clickable(onClick = onAddCategory),
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }, key = "hint") {
                // Text(
                //     text = "每个分类是一个错题本。点开本子查看题目；底部可以直接录入、打印计划或扫描回读。",
                //     style = MaterialTheme.typography.bodySmall,
                //     color = MaterialTheme.colorScheme.onSurfaceVariant,
                //     textAlign = TextAlign.Center,
                //     modifier = Modifier
                //         .fillMaxWidth()
                //         .padding(top = 8.dp),
                // )
            }
        }

        HomeBottomBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            onCreateQuestion = onCreateQuestion,
            onCreatePlan = onCreatePlan,
            onScan = onScan,
            onRecordButtonBounds = onRecordButtonBounds,
            focusRecordButton = focusRecordButton,
        )

        // 删除确认浮窗：只有分类中没有题目时才允许删除；有题目时只提示原因。
        val target = deleteTarget
        if (editing && target != null) {
            if (target.questionCount > 0) {
                CompactDialog(
                    title = "无法删除",
                    message = "「${target.name}」中还有 ${target.questionCount} 道题，请先删除或移走这些题目。",
                    dismissLabel = "知道了",
                    onDismiss = { deleteTarget = null },
                )
            } else {
                CompactDialog(
                    title = "删除分类？",
                    message = "「${target.name}」将从首页移除，此操作不可撤销。",
                    confirmLabel = "删除",
                    destructive = true,
                    onConfirm = {
                        deleteTarget = null
                        onDeleteCategory(target)
                    },
                    onDismiss = { deleteTarget = null },
                )
            }
        }
    }
}

/** 编辑态卡片右上角的两个圆形操作按钮：白底笔（编辑封面）、红底白叉（删除分类）。 */
@Composable
private fun CategoryEditActions(
    onEditCover: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onEditCover,
            modifier = Modifier.size(21.dp),
            shape = CircleShape,
            color = Color.White,
            contentColor = Color(0xFF20231D),
            border = BorderStroke(0.75.dp, Color.Black.copy(alpha = 0.16f)),
            shadowElevation = 1.5.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit_cover),
                    contentDescription = "编辑封面",
                    modifier = Modifier.size(11.dp),
                )
            }
        }
        Surface(
            onClick = onDelete,
            modifier = Modifier.size(21.dp),
            shape = CircleShape,
            color = Color(0xFFD93A32),
            contentColor = Color.White,
            shadowElevation = 1.5.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "删除分类",
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

/** 统一紧凑弹窗：用于删除分类确认与不可删除提示。 */
@Composable
private fun CompactDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String = "取消",
    destructive: Boolean = false,
) {
    RecollectDialog(
        title = title,
        onDismissRequest = onDismiss,
        actions = {
            RecollectDialogAction(text = dismissLabel, onClick = onDismiss)
            if (onConfirm != null && confirmLabel != null) {
                RecollectDialogAction(
                    text = confirmLabel,
                    onClick = onConfirm,
                    destructive = destructive,
                )
            }
        },
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun categoryTone(category: HomeCategory): Int =
    category.coverTone
        ?.takeIf { it in 0 until NOTEBOOK_TONE_COUNT }
        ?: defaultToneFor(category.name)

private fun categoryIcon(category: HomeCategory): Int =
    coverIconRes(category.coverIcon) ?: subjectIconRes(category.name)

/** 抖动的平滑回转曲线（约 5Hz，半周期 100ms）。 */
private val JiggleEasing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)

/**
 * 编辑态下的卡片：5Hz 平滑抖动，按住略微放大；右上角依次放置「编辑封面」与「删除分类」按钮。
 * 拖动过程中卡片中心进入另一张卡片时立即交换，松手只负责落库。
 */
@Composable
private fun EditableCategoryCard(
    category: HomeCategory,
    toneIndex: Int,
    iconRes: Int,
    index: Int,
    dragging: Boolean,
    pressed: Boolean,
    dragOffset: Offset,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onEditCover: () -> Unit,
    onDelete: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "jiggle-${category.id}")
    val angle by transition.animateFloat(
        initialValue = if (index % 2 == 0) -1.1f else 1.1f,
        targetValue = if (index % 2 == 0) 1.1f else -1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 100, easing = JiggleEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "jiggle-angle",
    )
    val scale = when {
        dragging -> 1.06f
        pressed -> 1.04f
        else -> 1f
    }
    Box(
        modifier = Modifier
            .zIndex(if (dragging || pressed) 2f else 0f)
            .graphicsLayer {
                rotationZ = if (dragging) 0f else angle
                translationX = if (dragging) dragOffset.x else 0f
                translationY = if (dragging) dragOffset.y else 0f
                scaleX = scale
                scaleY = scale
            }
            // 按下即略微放大；只观察按下/抬起，不消费事件，避免影响拖拽。
            .pointerInput(category.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    onPressStart()
                    waitForUpOrCancellation()
                    onPressEnd()
                }
            }
            .pointerInput(category.id) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDrag = { change, delta ->
                        change.consume()
                        onDrag(delta)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragCancel() },
                )
            },
    ) {
        NotebookCard(
            title = category.name,
            subtitle = "${category.questionCount} 题",
            toneIndex = toneIndex,
            iconRes = iconRes,
            // 为右上角的两个操作按钮留出空间，标题与题数整体下移。
            contentTopPadding = 29.dp,
        )
        CategoryEditActions(
            onEditCover = onEditCover,
            onDelete = onDelete,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 4.dp),
        )
    }
}

/**
 * 拖拽中的卡片中心进入另一张卡片时，直接交换两张卡片的位置。
 * 返回交换后的顺序和修正后的拖拽位移，使卡片继续跟随手指而不跳回原槽位。
 */
private fun liveSwap(
    gridState: LazyGridState,
    categories: List<HomeCategory>,
    draggedId: String,
    dragOffset: Offset,
): Pair<List<HomeCategory>, Offset>? {
    val items = gridState.layoutInfo.visibleItemsInfo
    val dragged = items.firstOrNull { it.key == draggedId } ?: return null
    val center = Offset(
        dragged.offset.x + dragged.size.width / 2f + dragOffset.x,
        dragged.offset.y + dragged.size.height / 2f + dragOffset.y,
    )
    val target = items.firstOrNull { info ->
        info.key is String &&
            info.key != draggedId &&
            center.x >= info.offset.x &&
            center.x <= info.offset.x + info.size.width &&
            center.y >= info.offset.y &&
            center.y <= info.offset.y + info.size.height
    } ?: return null

    val from = categories.indexOfFirst { it.id == draggedId }
    val to = categories.indexOfFirst { it.id == target.key }
    if (from < 0 || to < 0 || from == to) return null

    // 直接交换两张卡片，其余卡片保持原位。
    val reordered = categories.toMutableList()
    reordered[from] = categories[to]
    reordered[to] = categories[from]

    // 拖动项从原槽位移到目标槽位；用位移补偿抵消布局变化，让卡片仍停在手指下。
    val adjustedOffset = dragOffset + Offset(
        (dragged.offset.x - target.offset.x).toFloat(),
        (dragged.offset.y - target.offset.y).toFloat(),
    )
    return reordered to adjustedOffset
}

/**
 * 单个错题本卡片：左上标题与题数，右下淡色学科图标作水印。
 * 手势由调用方通过 [modifier] 注入（普通点击/长按或编辑模式拖拽）。
 */
@Composable
private fun NotebookCard(
    title: String,
    subtitle: String,
    toneIndex: Int,
    iconRes: Int,
    modifier: Modifier = Modifier,
    outlined: Boolean = false,
    contentTopPadding: Dp = 12.dp,
) {
    val dark = isSystemInDarkTheme()
    val tone = notebookTone(toneIndex, dark)
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.92f)
            .clip(MaterialTheme.shapes.large)
            .then(modifier),
        shape = MaterialTheme.shapes.large,
        color = if (outlined) colors.surfaceContainerLow else tone.container,
        contentColor = tone.content,
        border = BorderStroke(
            1.dp,
            if (outlined) colors.outlineVariant else tone.content.copy(alpha = 0.08f),
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp, end = 12.dp, top = contentTopPadding, bottom = 12.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = tone.content.copy(alpha = 0.65f),
                )
            }
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = tone.content.copy(alpha = 0.18f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .size(52.dp),
            )
        }
    }
}

/**
 * 按分类名匹配学科图标：优先精确匹配默认 11 个分类，再按关键词兼容用户重命名或新建的分类；
 * 未命中时回退到通用书本图标。
 */
internal fun subjectIconRes(name: String): Int {
    val normalized = name.trim()
    return when {
        normalized.contains("音乐") -> R.drawable.ic_cover_music
        normalized.contains("美术") || normalized.contains("绘画") -> R.drawable.ic_cover_art
        normalized.contains("体育") -> R.drawable.ic_cover_sports
        normalized.contains("作文") || normalized.contains("写作") -> R.drawable.ic_edit_cover
        normalized.contains("语文") -> R.drawable.ic_subject_chinese
        normalized.contains("数学") || normalized.contains("代数") || normalized.contains("几何") ->
            R.drawable.ic_subject_math
        normalized.contains("英语") || normalized.contains("英文") -> R.drawable.ic_subject_english
        normalized.contains("物理") -> R.drawable.ic_subject_physics
        normalized.contains("化学") -> R.drawable.ic_subject_chemistry
        normalized.contains("生物") -> R.drawable.ic_subject_biology
        normalized.contains("政治") || normalized.contains("道德") || normalized.contains("法治") ->
            R.drawable.ic_subject_politics
        normalized.contains("历史") -> R.drawable.ic_subject_history
        normalized.contains("地理") -> R.drawable.ic_subject_geography
        normalized.contains("信息") || normalized.contains("计算机") || normalized.contains("编程") ->
            R.drawable.ic_subject_it
        normalized.contains("通用技术") || normalized.contains("技术") ->
            R.drawable.ic_subject_technology
        else -> R.drawable.ic_home_library
    }
}

/** 分类读取中的占位卡：只保留底色，不显示文字，避免页面闪烁。 */
@Composable
private fun NotebookPlaceholder(toneIndex: Int) {
    val tone = notebookTone(toneIndex, isSystemInDarkTheme())
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.92f)
            .clip(MaterialTheme.shapes.large)
            .background(tone.container.copy(alpha = 0.5f)),
    )
}

/** 底部胶囊栏：「录入题目 | 打印计划 | 扫描回读」。 */
@Composable
private fun HomeBottomBar(
    modifier: Modifier,
    onCreateQuestion: () -> Unit,
    onCreatePlan: () -> Unit,
    onScan: () -> Unit,
    onRecordButtonBounds: (Rect) -> Unit,
    focusRecordButton: Boolean,
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
            PillAction(
                iconRes = R.drawable.ic_home_entry,
                label = "录入题目",
                onClick = onCreateQuestion,
                highlighted = focusRecordButton,
                modifier = Modifier.onGloballyPositioned { onRecordButtonBounds(it.boundsInRoot()) },
            )
            PillDivider()
            PillAction(
                iconRes = R.drawable.ic_home_plan,
                label = "打印计划",
                onClick = onCreatePlan,
            )
            PillDivider()
            PillAction(
                iconRes = R.drawable.ic_home_scan,
                label = "扫描回读",
                onClick = onScan,
            )
        }
    }
}

@Composable
private fun PillAction(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val tint = if (highlighted) colors.primary else colors.onSurface
    Row(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = tint,
            maxLines = 1,
        )
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
