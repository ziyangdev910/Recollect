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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.recollect.common.UiState
import com.recollect.data.room.repository.KnowledgePointTag
import kotlinx.coroutines.launch

/**
 * 知识点标签库（app.md「KnowledgePoints」）：编号/名称列表、搜索、新增、重命名、删除。
 *
 * 每个知识点就是一个标签；删除前确认，仍被未归档题目引用的标签不可删除（D-12）。
 * 页面只与 [KnowledgePointLibraryViewModel] 交互，不直接访问 Room/Context（R-03）。
 */
@Composable
fun KnowledgePointLibraryScreen(viewModel: KnowledgePointLibraryViewModel) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) { viewModel.refresh() }

    var query by remember { mutableStateOf("") }
    var dialog by remember { mutableStateOf<LibraryDialog?>(null) }

    val tags = (viewModel.state as? UiState.Content)?.data?.tags.orEmpty()
    val filtered = remember(tags, query) {
        val keyword = query.trim()
        if (keyword.isEmpty()) {
            tags
        } else {
            tags.filter {
                it.name.contains(keyword, ignoreCase = true) ||
                    it.number.toString() == keyword
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("搜索标签（名称或编号）") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "共 ${tags.size} 个标签",
                style = MaterialTheme.typography.titleSmall,
            )
            Button(
                onClick = {
                    viewModel.clearActionError()
                    dialog = LibraryDialog.Add
                },
            ) {
                Text("新增标签")
            }
        }

        viewModel.actionError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        when (val state = viewModel.state) {
            UiState.Loading -> CircularProgressIndicator()
            UiState.Empty -> Text("标签库为空。点击「新增标签」创建第一个知识点标签。")
            is UiState.Error -> {
                Text(state.message, color = MaterialTheme.colorScheme.error)
                if (state.retryable) {
                    TextButton(onClick = { scope.launch { viewModel.refresh() } }) {
                        Text("重试")
                    }
                }
            }
            is UiState.Content -> {
                if (filtered.isEmpty()) {
                    Text("没有匹配的标签。")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(filtered, key = { it.id }) { tag ->
                            TagRow(
                                tag = tag,
                                onRename = {
                                    viewModel.clearActionError()
                                    dialog = LibraryDialog.Rename(tag)
                                },
                                onDelete = {
                                    viewModel.clearActionError()
                                    dialog = LibraryDialog.Delete(tag)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    when (val current = dialog) {
        null -> Unit
        LibraryDialog.Add -> NameDialog(
            title = "新增标签",
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
        is LibraryDialog.Rename -> NameDialog(
            title = "重命名标签 #${current.tag.number}",
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
        is LibraryDialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("删除标签 #${current.tag.number}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "确定删除「${current.tag.name}」吗？删除后编号不再复用；" +
                            "若该标签仍被题目引用，将无法删除。",
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

@Composable
private fun TagRow(
    tag: KnowledgePointTag,
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
                Text("#${tag.number}  ${tag.name}", style = MaterialTheme.typography.bodyLarge)
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
                    label = { Text("标签名称") },
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

private sealed interface LibraryDialog {
    data object Add : LibraryDialog
    data class Rename(val tag: KnowledgePointTag) : LibraryDialog
    data class Delete(val tag: KnowledgePointTag) : LibraryDialog
}
