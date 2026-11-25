package com.recollect.feature.backup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.recollect.data.room.export.BackupDocument
import com.recollect.data.room.export.RoomDatabaseExporter
import kotlinx.coroutines.launch

/**
 * 「导出并分享数据库」页（backup.md「导出/导入 UI」）。
 *
 * 将白名单实体导出为一个 JSON 文件，写入应用缓存并通过系统分享面板发送。
 * 页面只与 [BackupViewModel] 交互，不直接访问 Room/Context/Intent（R-03）。
 */
@Composable
fun BackupScreen(viewModel: BackupViewModel) {
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { IntroHeader() }
        item {
            Button(
                onClick = { scope.launch { viewModel.exportAndShare() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = viewModel.state !is BackupState.Exporting,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_share),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("导出并分享")
            }
        }
        item {
            StateCard(
                state = viewModel.state,
                onRetryExport = { scope.launch { viewModel.exportAndShare() } },
                onRetryShare = { scope.launch { viewModel.retryShare() } },
            )
        }
        item { PrivacyNote() }
    }
}

/** 页头说明：先讲清楚导出了什么、发到哪里，再把主按钮交给用户。 */
@Composable
private fun IntroHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "导出并分享数据库",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "把本机题库、知识点、题目来源、复习反馈、每日复习时长与打印页快照导出为一个 JSON 文件，" +
                "生成后自动打开系统分享面板，可发送给微信、文件管理器或其他应用。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StateCard(
    state: BackupState,
    onRetryExport: () -> Unit,
    onRetryShare: () -> Unit,
) {
    when (state) {
        BackupState.Idle -> Unit

        BackupState.Exporting -> Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = "正在读取数据库并生成备份文件…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        is BackupState.Exported -> ExportedCard(
            document = state.document,
            shareOutcome = state.shareOutcome,
            onRetryShare = onRetryShare,
        )

        is BackupState.Error -> {
            val colors = MaterialTheme.colorScheme
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.errorContainer),
                border = BorderStroke(1.dp, colors.error.copy(alpha = 0.2f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "导出失败",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onErrorContainer,
                    )
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onErrorContainer,
                    )
                    Text(
                        text = "本地数据库未受影响，可重试。",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onErrorContainer.copy(alpha = 0.8f),
                    )
                    OutlinedButton(onClick = onRetryExport) { Text("重新导出") }
                }
            }
        }
    }
}

@Composable
private fun ExportedCard(
    document: BackupDocument,
    shareOutcome: ShareOutcome,
    onRetryShare: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        border = BorderStroke(1.dp, colors.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "导出完成",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            DetailRow("文件名", document.fileName)
            DetailRow("文件大小", formatSize(document.sizeBytes))
            DetailRow("备份格式版本", RoomDatabaseExporter.BACKUP_FORMAT_VERSION.toString())
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            when (shareOutcome) {
                ShareOutcome.Shared -> Text(
                    text = "已打开系统分享面板，请选择要发送到的应用。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primary,
                )

                is ShareOutcome.Failed -> Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "文件已生成，但打开分享面板失败：${shareOutcome.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.error,
                    )
                    OutlinedButton(onClick = onRetryShare) { Text("重试分享") }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PrivacyNote() {
    Text(
        text = "备份文件未加密，包含个人学习数据，请妥善保管。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

private fun formatSize(bytes: Int): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
    else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
}
