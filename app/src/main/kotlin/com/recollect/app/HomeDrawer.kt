package com.recollect.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.ziyangdev910.recollect.R

/**
 * 首页左侧抽屉（2026-11-28 用户要求）。
 *
 * 由「拾知」标题左侧的三横线图标打开，按压暗层、系统返回键或任一条目收起。
 * 抽屉作为根级覆盖层压在顶栏与底部悬浮胶囊栏之上。
 * 顶部为应用 logo、名称与版本号；下方收纳原顶栏「更多」菜单中的条目（知识点与分类已移除）。
 */
@Composable
internal fun HomeDrawer(
    visible: Boolean,
    versionName: String,
    dailyReviewMinutes: Int,
    onDismiss: () -> Unit,
    onProgress: () -> Unit,
    onDailyMinutes: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onGuide: () -> Unit,
    onAbout: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(200)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.34f))
                    .pointerInput(Unit) { detectTapGestures { onDismiss() } },
            )
        }
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(
                animationSpec = tween(260),
                initialOffsetX = { -it },
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(220),
                targetOffsetX = { -it },
            ),
            modifier = Modifier.align(Alignment.CenterStart),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    // 收窄抽屉宽度；条目文字用 14sp，长标签以尾部次要文字呈现。
                    .fillMaxWidth(0.74f),
                shape = RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 20.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp),
                ) {
                    DrawerHeader(versionName = versionName)
                    Spacer(modifier = Modifier.size(6.dp))
                    DrawerDivider()
                    Spacer(modifier = Modifier.size(2.dp))
                    DrawerMenuItem(
                        iconRes = R.drawable.ic_home_progress,
                        label = "复习进度",
                        onClick = onProgress,
                    )
                    DrawerMenuItem(
                        iconRes = R.drawable.ic_clock,
                        label = "每日复习时长",
                        trailingText = "$dailyReviewMinutes 分钟",
                        onClick = onDailyMinutes,
                    )
                    DrawerDivider()
                    // 沿用仓库 2026-11-22 的约定：导入显示下载图形、导出显示上传图形。
                    DrawerMenuItem(
                        iconRes = R.drawable.ic_export,
                        label = "导入数据库",
                        onClick = onImport,
                    )
                    DrawerMenuItem(
                        iconRes = R.drawable.ic_import,
                        label = "导出数据库",
                        onClick = onExport,
                    )
                    DrawerDivider()
                    DrawerMenuItem(
                        iconRes = R.drawable.ic_guide,
                        label = "使用引导",
                        onClick = onGuide,
                    )
                    DrawerMenuItem(
                        iconRes = R.drawable.ic_info,
                        label = "关于",
                        onClick = onAbout,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DrawerHeader(versionName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        DrawerAppIcon()
        Column {
            Text(
                text = "拾知",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "v$versionName",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 与「关于」页一致的图标合成：品牌渐变底 + 启动图标前景 vector。 */
@Composable
private fun DrawerAppIcon() {
    DrawerIconFrame(size = 44.dp)
}

@Composable
private fun DrawerIconFrame(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF3E6B5A), Color(0xFF1F4034)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun DrawerMenuItem(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    trailingText: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        trailingText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = colors.onSurfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun DrawerDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
