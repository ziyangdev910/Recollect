package com.recollect.feature.confirm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 全应用统一弹窗样式（2026-11-28 用户要求）。
 *
 * 以「计划日期」弹窗为基准：20dp 圆角、16dp 内边距、titleMedium 标题 +
 * labelMedium 副标题、紧凑的 TextButton 操作行。各模块各保留一份同款实现，
 * 沿用仓库既有做法，避免 feature 之间互相依赖。
 */
@Composable
internal fun RecollectDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    scrollable: Boolean = false,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = modifier
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
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (scrollable) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 440.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        content = content,
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        content = content,
                    )
                }
                if (actions != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions,
                    )
                }
            }
        }
    }
}

/** 弹窗操作按钮：与「计划日期」的取消/确定一样使用紧凑内边距。 */
@Composable
internal fun RecollectDialogAction(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            color = if (destructive && enabled) {
                MaterialTheme.colorScheme.error
            } else {
                Color.Unspecified
            },
        )
    }
}
