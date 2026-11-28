package com.recollect.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.recollect.data.room.export.ImportSummary
import com.recollect.data.room.export.RoomDatabaseExporter

/**
 * 导入流程的对话框：校验中 / 覆盖确认 / 导入中 / 失败。
 *
 * 成功态不用对话框，由调用方以带对勾的 Toast 提示（2026-11-22 用户要求）。
 * 对话框只消费 [ImportState]，不直接访问文件或数据库（R-03）。
 */
@Composable
fun ImportDialogs(
    state: ImportState,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onDismissError: () -> Unit,
) {
    when (state) {
        ImportState.Idle, is ImportState.Success -> Unit

        ImportState.Reading -> ProgressDialog("正在校验备份文件…")

        ImportState.Importing -> ProgressDialog("正在导入并覆盖当前数据…")

        is ImportState.Confirming -> RecollectDialog(
            title = "导入数据库",
            onDismissRequest = onCancel,
            actions = {
                RecollectDialogAction(text = "取消", onClick = onCancel)
                RecollectDialogAction(text = "继续导入", onClick = onConfirm)
            },
        ) {
            Text(
                text = "将覆盖当前数据，是否继续？",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = summaryText(state.summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is ImportState.Error -> RecollectDialog(
            title = "导入失败",
            onDismissRequest = onDismissError,
            actions = {
                RecollectDialogAction(text = "知道了", onClick = onDismissError)
            },
        ) {
            Text(state.message)
        }
    }
}

@Composable
private fun ProgressDialog(message: String) {
    RecollectDialog(
        title = "请稍候",
        onDismissRequest = {},
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
            Text(message)
        }
    }
}

private fun summaryText(summary: ImportSummary): String {
    val counts = summary.entityCounts
    val knowledgePoints = counts[RoomDatabaseExporter.KEY_KNOWLEDGE_POINTS] ?: 0
    val questions = counts[RoomDatabaseExporter.KEY_QUESTIONS] ?: 0
    val crossRefs = counts[RoomDatabaseExporter.KEY_CROSS_REFS] ?: 0
    val degradedHint = if (summary.degraded) {
        "\n备份来自 schema ${summary.sourceSchemaVersion}，将按跨版本规则恢复：" +
            "不恢复历史反馈、决定日志与计划快照，并重置知识点调度状态。"
    } else {
        ""
    }
    return "将导入知识点 $knowledgePoints 个、题目 $questions 道、关联 $crossRefs 条。" +
        degradedHint
}
