package com.recollect.feature.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.recollect.common.UiState
import com.recollect.data.room.repository.SourceTagItem
import kotlinx.coroutines.launch

/**
 * 来源标签库（2026-11-22 用户要求）：名称列表、新增、重命名、删除。
 *
 * 录入题目时来源以单选标签填写；删除前确认，仍被未归档题目引用的标签不可删除。
 * 重命名会同步更新已有题目的来源名称（Repository 事务内完成）。
 * 页面只与 [SourceTagLibraryViewModel] 交互，不直接访问 Room/Context（R-03）。
 */
@Composable
fun SourceTagLibraryScreen(
    viewModel: SourceTagLibraryViewModel,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) { viewModel.refresh() }

    var dialog by remember { mutableStateOf<SourceDialog?>(null) }

    val tags = (viewModel.state as? UiState.Content)?.data?.tags.orEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "共 ${tags.size} 个来源",
                style = MaterialTheme.typography.titleSmall,
            )
            Button(
                onClick = {
                    viewModel.clearActionError()
                    dialog = SourceDialog.Add
                },
            ) {
                Text("新增来源")
            }
        }

        viewModel.actionError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        when (val state = viewModel.state) {
            UiState.Loading -> CircularProgressIndicator()
            UiState.Empty -> Text("来源标签库为空。点击「新增来源」创建第一个来源标签。")
            is UiState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                if (state.retryable) {
                    TextButton(onClick = { scope.launch { viewModel.refresh() } }) {
                        Text("重试")
                    }
                }
            }
            is UiState.Content -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(tags, key = { it.id }) { tag ->
                        SourceTagRow(
                            tag = tag,
                            onRename = {
                                viewModel.clearActionError()
                                dialog = SourceDialog.Rename(tag)
                            },
                            onDelete = {
                                viewModel.clearActionError()
                                dialog = SourceDialog.Delete(tag)
                            },
                        )
                    }
                }
            }
        }
    }

    when (val current = dialog) {
        null -> Unit
        SourceDialog.Add -> NameDialog(
            title = "新增来源",
            initialName = "",
            confirmLabel = "创建",
            errorMessage = viewModel.actionError,
            onDismiss = { dialog = null },
            onConfirm = { name ->
                scope.launch {
                    if (viewModel.add(name)) dialog = null
                }
            },
        )
        is SourceDialog.Rename -> NameDialog(
            title = "重命名来源",
            initialName = current.tag.name,
            confirmLabel = "保存",
            errorMessage = viewModel.actionError,
            onDismiss = { dialog = null },
            onConfirm = { name ->
                scope.launch {
                    if (viewModel.rename(current.tag.id, name)) dialog = null
                }
            },
        )
        is SourceDialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("删除来源「${current.tag.name}」") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "确定删除「${current.tag.name}」吗？若该来源仍被题目引用，将无法删除。",
                    )
                    viewModel.actionError?.let { message ->
                        Text(message, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            if (viewModel.delete(current.tag.id)) dialog = null
                        }
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) { Text("取消") }
            },
        )
    }
}

/**
 * 来源标签库的全屏窗口：从录入页打开，关闭后录入页仍保留未保存的表单内容。
 *
 * 导航栏与「知识点」页保持一致：左侧语义图标（关闭）+ 标题的 TopAppBar。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceTagManagerDialog(
    viewModel: SourceTagLibraryViewModel,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("来源标签") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = "关闭",
                            )
                        }
                    },
                )
                SourceTagLibraryScreen(
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SourceTagRow(
    tag: SourceTagItem,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tag.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = if (tag.linkedQuestionCount > 0) {
                        "关联 ${tag.linkedQuestionCount} 道题，不能删除"
                    } else {
                        "未被题目引用"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (tag.linkedQuestionCount > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            IconButton(onClick = onRename) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = "重命名",
                )
            }
            IconButton(
                enabled = tag.linkedQuestionCount == 0,
                onClick = onDelete,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = "删除",
                )
            }
        }
    }
}

@Composable
private fun NameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("来源名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                errorMessage?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name) },
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

private sealed interface SourceDialog {
    data object Add : SourceDialog
    data class Rename(val tag: SourceTagItem) : SourceDialog
    data class Delete(val tag: SourceTagItem) : SourceDialog
}
