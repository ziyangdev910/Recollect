package com.recollect.app.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 压暗到约 70%：既保留聚焦对比，也能看清整页布局与周围文字。
private val ScrimColor = Color(0xB3000000)
private val CardColor = Color(0xFF141815)
private val CardBorderColor = Color(0xFF3B4A3F)
private val PrimaryText = Color(0xFFF4F1E6)
private val SecondaryText = Color(0xFFC9C7B9)
private val Accent = Color(0xFFA9D2C0)
private val OnAccent = Color(0xFF10352A)
private val MutedText = Color(0xFF8F9588)

/**
 * 首次复习演示的交互式黑底聚焦层（2026-11-26 用户要求）。
 *
 * 蒙层只压暗 [spotlight] 之外的真实页面，高亮区域保持可点击；
 * 顶部说明条告诉用户现在在哪一步、要点哪里。用户完成真实控件上的操作后，
 * 外层状态机才进入下一段，不再自动切页。
 */
@Composable
internal fun DemoTourOverlay(
    state: DemoTourState,
    spotlight: Rect?,
    contentTopPx: Float,
    canFinish: Boolean,
    bodyOverride: String? = null,
    /** 从设置里手动重看演示时，欢迎卡提示当前数据不会丢失。 */
    preserveDataNotice: Boolean = false,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onUse: () -> Unit,
    onRetryRestore: () -> Unit,
    onRetrySheet: () -> Unit,
    onDismiss: () -> Unit,
    onBarHeightChanged: (Int) -> Unit,
) {
    if (state is DemoTourState.Inactive) return

    when (state) {
        DemoTourState.Inactive -> Unit
        DemoTourState.Preparing -> CenteredCard(
            title = "正在准备示例数据",
            body = "只创建演示用的知识点与来源标签；题目会由你亲手录入，结束后自动恢复原本的数据库。",
        )
        DemoTourState.PreparingScanSheet -> CenteredCard(
            title = "正在准备演示打印稿",
            body = "按刚刚生成的计划绘制一张已填涂的 A4 打印稿，马上用它做扫描回读。",
        )
        DemoTourState.Restoring -> CenteredCard(
            title = "正在恢复数据库",
            body = "正在清除演示数据并还原演示前的数据库，请稍候。",
        )
        is DemoTourState.PrepareFailed -> ErrorCard(
            title = "示例数据准备失败",
            body = state.message,
            primaryLabel = "开始使用",
            onPrimary = onDismiss,
            secondaryLabel = null,
            onSecondary = {},
        )
        is DemoTourState.RestoreFailed -> ErrorCard(
            title = "示例数据清理失败",
            body = "${state.message}演示前的快照仍保留，重启应用后会再次尝试清理。",
            primaryLabel = "重试清理",
            onPrimary = onRetryRestore,
            secondaryLabel = "暂时隐藏",
            onSecondary = onDismiss,
        )
        is DemoTourState.SheetFailed -> ErrorCard(
            title = "演示打印稿准备失败",
            body = state.message,
            primaryLabel = "重试生成打印稿",
            onPrimary = onRetrySheet,
            secondaryLabel = "跳过演示",
            onSecondary = onSkip,
        )
        is DemoTourState.Running -> if (state.stop == DemoTourStop.WELCOME) {
            WelcomeOverlay(
                onStart = onNext,
                onSkip = onSkip,
                showDataNotice = preserveDataNotice,
            )
        } else {
            RunningOverlay(
                state = state,
                spotlight = spotlight,
                contentTopPx = contentTopPx,
                canFinish = canFinish,
                bodyOverride = bodyOverride,
                onNext = onNext,
                onSkip = onSkip,
                onUse = onUse,
                onBarHeightChanged = onBarHeightChanged,
            )
        }
    }
}

/**
 * 欢迎卡片：第 1 步之前先告诉用户即将开始引导，确认后才进入真实页面操作。
 */
@Composable
private fun WelcomeOverlay(
    onStart: () -> Unit,
    onSkip: () -> Unit,
    showDataNotice: Boolean,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 背景遮罩作为兄弟节点拦截卡片外的触摸，卡片按钮保持可点。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScrimColor)
                .pointerInput(Unit) { detectTapGestures { } },
        )
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 22.dp),
            shape = RoundedCornerShape(22.dp),
            color = CardColor,
            border = BorderStroke(1.dp, CardBorderColor),
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = DemoTourStop.WELCOME.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = PrimaryText,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = DemoTourStop.WELCOME.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondaryText,
                    lineHeight = 22.sp,
                )
                if (showDataNotice) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Accent.copy(alpha = 0.14f),
                    ) {
                        Text(
                            text = "您的数据不会丢失：演示期间会临时使用示例数据，结束后自动恢复当前题库、计划与设置。",
                            style = MaterialTheme.typography.labelMedium,
                            color = Accent,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "跳过演示",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onSkip)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    PrimaryButton("开始引导", onClick = onStart)
                }
            }
        }
    }
}

@Composable
private fun RunningOverlay(
    state: DemoTourState.Running,
    spotlight: Rect?,
    contentTopPx: Float,
    canFinish: Boolean,
    bodyOverride: String?,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onUse: () -> Unit,
    onBarHeightChanged: (Int) -> Unit,
) {
    val density = LocalDensity.current
    // 按目标类型适配镂空圆角：文本框用输入框自身的圆角、整卡用卡片圆角，
    // 按钮/FAB 仍保持按钮的胶囊轮廓；确认页整块内容不做圆角。
    val holeRadiusPx = spotlight?.let { hole ->
        with(density) {
            when (state.stop.target) {
                DemoTarget.FORM_NUMBER,
                DemoTarget.FORM_PAGE_CODE,
                DemoTarget.FORM_MINUTES,
                -> 6.dp.toPx()

                DemoTarget.HOME_RECORD -> 14.dp.toPx()
                // 题库页的「预览计划」是大号 Extended FAB，使用主题 large 形状。
                DemoTarget.ENTRY_PLAN -> 18.dp.toPx()
                // 来源与知识点使用与确认结果一致的直角边框。
                DemoTarget.FORM_SOURCE,
                DemoTarget.FORM_KNOWLEDGE,
                -> 0f

                DemoTarget.CONFIRM_CONTENT -> 0f
                else -> (hole.height / 2f).coerceAtMost(28.dp.toPx())
            }
        }
    } ?: 0f

    Box(modifier = Modifier.fillMaxSize()) {
        ScrimHole(spotlight, holeRadiusPx)

        val topDp = with(density) { contentTopPx.toDp() }
        InstructionBar(
            stop = state.stop,
            canFinish = canFinish,
            bodyOverride = bodyOverride,
            onNext = onNext,
            onSkip = onSkip,
            onUse = onUse,
            onBarHeightChanged = onBarHeightChanged,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(y = topDp)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
        )
    }
}

/**
 * 四块蒙层围出镂空；镂空内不挂 pointerInput，触摸会落到真实页面上。
 * 没有可聚焦目标时整屏拦截，避免用户误操作。
 */
@Composable
private fun ScrimHole(hole: Rect?, radiusPx: Float) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()
        if (hole == null || hole.width <= 0f || hole.height <= 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ScrimColor)
                    .pointerInput(Unit) { detectTapGestures { } },
            )
            return@BoxWithConstraints
        }
        val top = hole.top.coerceIn(0f, screenHeight)
        val bottom = hole.bottom.coerceIn(0f, screenHeight)
        val left = hole.left.coerceIn(0f, screenWidth)
        val right = hole.right.coerceIn(0f, screenWidth)
        val radius = radiusPx.coerceIn(
            0f,
            (minOf(right - left, bottom - top) / 2f).coerceAtLeast(0f),
        )

        // 用 even-odd 路径一次画出「全屏矩形减去圆角镂空」，镂空边缘自带圆角。
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scrim = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(0f, 0f, screenWidth, screenHeight))
                addRoundRect(
                    RoundRect(
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom,
                        radiusX = radius,
                        radiusY = radius,
                    ),
                )
            }
            drawPath(scrim, ScrimColor)
            drawRoundRect(
                color = Accent.copy(alpha = 0.9f),
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                cornerRadius = CornerRadius(radius),
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }

        // 透明命中层只负责拦住镂空之外的触摸；镂空内（含圆角附近）保持可点击。
        ScrimBlocker(x = 0f, y = 0f, width = screenWidth, height = top)
        ScrimBlocker(x = 0f, y = bottom, width = screenWidth, height = screenHeight - bottom)
        ScrimBlocker(x = 0f, y = top, width = left, height = bottom - top)
        ScrimBlocker(x = right, y = top, width = screenWidth - right, height = bottom - top)
    }
}

@Composable
private fun ScrimBlocker(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
) {
    if (width <= 0f || height <= 0f) return
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset(
                x = with(density) { x.toDp() },
                y = with(density) { y.toDp() },
            )
            .size(
                width = with(density) { width.toDp() },
                height = with(density) { height.toDp() },
            )
            .pointerInput(Unit) { detectTapGestures { } },
    )
}

@Composable
private fun InstructionBar(
    stop: DemoTourStop,
    canFinish: Boolean,
    bodyOverride: String?,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onUse: () -> Unit,
    onBarHeightChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.onSizeChanged { onBarHeightChanged(it.height) },
        shape = RoundedCornerShape(18.dp),
        color = CardColor,
        border = BorderStroke(1.dp, CardBorderColor),
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stop.stage.progressLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Accent,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "跳过演示",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedText,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onSkip)
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
            Text(
                text = stop.title,
                style = MaterialTheme.typography.titleSmall,
                color = PrimaryText,
                fontWeight = FontWeight.SemiBold,
            )
            val bodyText = buildString {
                append(
                    bodyOverride ?: if (stop.mode == DemoStopMode.FINISH && canFinish) {
                        stop.completedBody ?: stop.body
                    } else {
                        stop.body
                    },
                )
                stop.tip?.let { tip ->
                    append('\n')
                    append(tip)
                }
            }
            Text(
                text = bodyText,
                style = MaterialTheme.typography.bodySmall,
                color = SecondaryText,
                lineHeight = 17.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                when (stop.mode) {
                    DemoStopMode.NEXT -> PrimaryButton("下一步", onClick = onNext)
                    DemoStopMode.WAIT_TAP -> Text(
                        text = "请点击高亮区域",
                        style = MaterialTheme.typography.labelSmall,
                        color = Accent,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                    DemoStopMode.FINISH -> PrimaryButton(
                        label = "开始使用",
                        onClick = onUse,
                        enabled = canFinish,
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(36.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent,
            contentColor = OnAccent,
            disabledContainerColor = Accent.copy(alpha = 0.28f),
            disabledContentColor = OnAccent.copy(alpha = 0.6f),
        ),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun CenteredCard(title: String, body: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 背景遮罩作为兄弟节点，避免拦截卡片上按钮的点击。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScrimColor)
                .pointerInput(Unit) { detectTapGestures { } },
        )
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 22.dp),
            shape = RoundedCornerShape(20.dp),
            color = CardColor,
            border = BorderStroke(1.dp, CardBorderColor),
            shadowElevation = 12.dp,
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Accent,
                    strokeWidth = 2.dp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = PrimaryText,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = SecondaryText,
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String?,
    onSecondary: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 背景遮罩作为兄弟节点，避免拦截卡片上按钮的点击。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ScrimColor)
                .pointerInput(Unit) { detectTapGestures { } },
        )
        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 22.dp),
            shape = RoundedCornerShape(20.dp),
            color = CardColor,
            border = BorderStroke(1.dp, CardBorderColor),
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryText,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondaryText,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (secondaryLabel != null) {
                        TextButton(onClick = onSecondary) {
                            Text(secondaryLabel, color = MutedText)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    PrimaryButton(primaryLabel, onClick = onPrimary)
                }
            }
        }
    }
}
