package com.recollect.feature.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.recollect.data.room.repository.KnowledgeCategoryItem
import com.recollect.data.room.repository.KnowledgePointTag
import kotlinx.coroutines.launch

/**
 * 知识点标签库（app.md「KnowledgePoints」）：分类筛选、编号/名称列表、新增、编辑、删除。
 *
 * 每个知识点就是一个标签，必须归属一个分类；分类本身也可增删改（2026-11-23 用户要求）。
 * 删除前确认，仍被未归档题目引用的标签不可删除（D-12）。
 * 页面只与 [KnowledgePointLibraryViewModel] 交互，不直接访问 Room/Context（R-03）。
 */
@Composable
fun KnowledgePointLibraryScreen(
    viewModel: KnowledgePointLibraryViewModel,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel) { viewModel.refresh() }

    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var dialog by remember { mutableStateOf<LibraryDialog?>(null) }
    var showCategoryManager by remember { mutableStateOf(false) }

    val tags = (viewModel.state as? UiState.Content)?.data?.tags.orEmpty()
    val categories = (viewModel.state as? UiState.Content)?.data?.categories.orEmpty()

    // 分类被删除或改名后，失效的筛选回到「全部」。
    LaunchedEffect(categories) {
        if (selectedCategoryId != null && categories.none { it.id == selectedCategoryId }) {
            selectedCategoryId = null
        }
    }

    val filtered = remember(tags, selectedCategoryId) {
        tags.filter { tag ->
            selectedCategoryId == null || tag.categoryId == selectedCategoryId
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            // 2026-11-23 用户要求：缩小列表内容与顶部导航栏之间的间距。
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 分类筛选选项卡：位于「共 N 个标签」与「新增标签」按钮之上。
        KnowledgeCategoryFilterRow(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onSelect = { selectedCategoryId = it },
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "共 ${filtered.size} 个标签",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = {
                    viewModel.clearActionError()
                    showCategoryManager = true
                },
            ) {
                Text("管理分类")
            }
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
                    Text(
                        if (tags.isEmpty()) {
                            "标签库为空。点击「新增标签」创建第一个知识点标签。"
                        } else {
                            "当前筛选下没有匹配的标签。"
                        },
                    )
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
                                categoryName = categories.firstOrNull { it.id == tag.categoryId }?.name
                                    ?: "未分类",
                                onEdit = {
                                    viewModel.clearActionError()
                                    dialog = LibraryDialog.Edit(tag)
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
        LibraryDialog.Add -> KnowledgePointDialog(
            title = "新增标签",
            initialName = "",
            initialCategoryId = selectedCategoryId,
            categories = categories,
            confirmLabel = "创建",
            errorMessage = viewModel.actionError,
            onDismiss = { dialog = null },
            onConfirm = { name, categoryId ->
                scope.launch {
                    if (viewModel.add(name, categoryId)) dialog = null
                }
            },
        )
        is LibraryDialog.Edit -> KnowledgePointDialog(
            title = "编辑标签 #${current.tag.number}",
            initialName = current.tag.name,
            initialCategoryId = current.tag.categoryId,
            categories = categories,
            confirmLabel = "保存",
            errorMessage = viewModel.actionError,
            onDismiss = { dialog = null },
            onConfirm = { name, categoryId ->
                scope.launch {
                    if (viewModel.update(current.tag.id, name, categoryId)) dialog = null
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

    if (showCategoryManager) {
        CategoryManagerDialog(
            viewModel = viewModel,
            onDismiss = {
                showCategoryManager = false
                viewModel.clearActionError()
            },
        )
    }
}

/**
 * 知识点标签库的全屏窗口：从录入页打开，关闭后录入页仍保留未保存的表单内容。
 *
 * 交互与「来源标签」窗口一致：左侧语义图标（关闭）+ 标题的 TopAppBar。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgePointManagerDialog(
    viewModel: KnowledgePointLibraryViewModel,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("知识点") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                painter = painterResource(R.drawable.ic_close),
                                contentDescription = "关闭",
                            )
                        }
                    },
                )
                KnowledgePointLibraryScreen(
                    viewModel = viewModel,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TagRow(
    tag: KnowledgePointTag,
    categoryName: String,
    onEdit: () -> Unit,
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
                    text = buildString {
                        append(categoryName)
                        append(" · ")
                        append(
                            if (tag.linkedQuestionCount > 0) {
                                "关联 ${tag.linkedQuestionCount} 道题，不能删除"
                            } else {
                                "未被题目引用"
                            },
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (tag.linkedQuestionCount > 0) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = "编辑",
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

/**
 * 新增/编辑知识点对话框：名称 + 分类必选（2026-11-23 用户要求）。
 *
 * 没有分类时无法保存，提示先去「管理分类」创建分类。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KnowledgePointDialog(
    title: String,
    initialName: String,
    initialCategoryId: String?,
    categories: List<KnowledgeCategoryItem>,
    confirmLabel: String,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var categoryId by remember {
        mutableStateOf(initialCategoryId ?: categories.firstOrNull()?.id)
    }
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
                Text("分类（必选）", style = MaterialTheme.typography.titleSmall)
                if (categories.isEmpty()) {
                    Text(
                        "暂无分类。请先点「管理分类」创建分类，再新增知识点。",
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        categories.forEach { category ->
                            FilterChip(
                                selected = category.id == categoryId,
                                onClick = { categoryId = category.id },
                                label = { Text(category.name) },
                            )
                        }
                    }
                }
                errorMessage?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && categoryId != null && categories.isNotEmpty(),
                onClick = { categoryId?.let { onConfirm(name, it) } },
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/** 分类管理：新增、重命名、删除；仍有知识点的分类不可删除。 */
@Composable
private fun CategoryManagerDialog(
    viewModel: KnowledgePointLibraryViewModel,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val categories = (viewModel.state as? UiState.Content)?.data?.categories.orEmpty()
    var dialog by remember { mutableStateOf<CategoryDialog?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理分类") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "共 ${categories.size} 个分类",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    TextButton(
                        onClick = {
                            viewModel.clearActionError()
                            dialog = CategoryDialog.Add
                        },
                    ) {
                        Text("新增分类")
                    }
                }
                viewModel.actionError?.let { message ->
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
                if (categories.isEmpty()) {
                    Text("暂无分类，请先新增分类。")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(categories, key = { it.id }) { category ->
                            CategoryRow(
                                category = category,
                                onRename = {
                                    viewModel.clearActionError()
                                    dialog = CategoryDialog.Rename(category)
                                },
                                onDelete = {
                                    viewModel.clearActionError()
                                    dialog = CategoryDialog.Delete(category)
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        },
    )

    when (val current = dialog) {
        null -> Unit
        CategoryDialog.Add -> CategoryNameDialog(
            title = "新增分类",
            initialName = "",
            confirmLabel = "创建",
            errorMessage = viewModel.actionError,
            onDismiss = { dialog = null },
            onConfirm = { name ->
                scope.launch {
                    if (viewModel.addCategory(name)) dialog = null
                }
            },
        )
        is CategoryDialog.Rename -> CategoryNameDialog(
            title = "重命名分类",
            initialName = current.category.name,
            confirmLabel = "保存",
            errorMessage = viewModel.actionError,
            onDismiss = { dialog = null },
            onConfirm = { name ->
                scope.launch {
                    if (viewModel.renameCategory(current.category.id, name)) dialog = null
                }
            },
        )
        is CategoryDialog.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("删除分类「${current.category.name}」") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "确定删除「${current.category.name}」吗？" +
                            "分类下仍有知识点时无法删除。",
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
                            if (viewModel.deleteCategory(current.category.id)) dialog = null
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
private fun CategoryRow(
    category: KnowledgeCategoryItem,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(category.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${category.linkedKnowledgePointCount} 个知识点",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRename) {
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = "重命名",
            )
        }
        IconButton(
            enabled = category.linkedKnowledgePointCount == 0,
            onClick = onDelete,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = "删除",
            )
        }
    }
}

@Composable
private fun CategoryNameDialog(
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
                    label = { Text("分类名称") },
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
    data class Edit(val tag: KnowledgePointTag) : LibraryDialog
    data class Delete(val tag: KnowledgePointTag) : LibraryDialog
}

private sealed interface CategoryDialog {
    data object Add : CategoryDialog
    data class Rename(val category: KnowledgeCategoryItem) : CategoryDialog
    data class Delete(val category: KnowledgeCategoryItem) : CategoryDialog
}
