package com.recollect.feature.entry

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.recollect.common.UiState
import com.recollect.data.room.repository.EntryDraft
import com.recollect.data.room.repository.KnowledgePointTag
import kotlinx.coroutines.launch

/**
 * 录入/编辑题目的独立页面（entry.md「题库增删改查」）。
 *
 * 新建与编辑共用同一表单；编辑时由 [QuestionEditViewModel] 回填题号、页码、标签与预估时长。
 * 页面只与 ViewModel 交互，不直接访问 Room/Context（R-03）。
 */
@Composable
fun QuestionEditScreen(
    viewModel: QuestionEditViewModel,
    sourceTagViewModel: SourceTagLibraryViewModel,
    knowledgePointViewModel: KnowledgePointLibraryViewModel,
    questionId: String?,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel, questionId) { viewModel.load(questionId) }

    when (val state = viewModel.state) {
        UiState.Loading -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        UiState.Empty -> Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text("题目不存在或已被删除。")
        }

        is UiState.Error -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(state.message, color = MaterialTheme.colorScheme.error)
            if (state.retryable) {
                TextButton(onClick = { scope.launch { viewModel.load(questionId) } }) {
                    Text("重试")
                }
            }
        }

        is UiState.Content -> QuestionForm(
            viewModel = viewModel,
            sourceTagViewModel = sourceTagViewModel,
            knowledgePointViewModel = knowledgePointViewModel,
            content = state.data,
            onSaved = onSaved,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionForm(
    viewModel: QuestionEditViewModel,
    sourceTagViewModel: SourceTagLibraryViewModel,
    knowledgePointViewModel: KnowledgePointLibraryViewModel,
    content: QuestionEditContent,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var questionNumber by remember(content) { mutableStateOf(content.questionNumber) }
    // 页码编号用文本保存：带来源时允许键盘输入（含清空中间态），无来源时只读 + +/−。
    var pageCodeText by remember(content) { mutableStateOf(content.sourcePageCode.toString()) }
    // 用户手动改过编号后，切换来源不再自动套用另一类默认值。
    var pageCodeTouched by remember(content) { mutableStateOf(false) }
    // 来源以标签 id 记录选中，窗口内重命名后仍保持选中；旧题自由文本来源用 legacyName 承载。
    var selectedSourceId by remember(content) {
        mutableStateOf(
            content.sourceTags.firstOrNull { it.name == content.sourcePageLabel }?.id,
        )
    }
    var legacySourceName by remember(content) {
        mutableStateOf(
            content.sourcePageLabel.takeIf { label ->
                label.isNotBlank() && content.sourceTags.none { it.name == label }
            }.orEmpty(),
        )
    }
    var minutes by remember(content) {
        mutableStateOf(content.estimatedReviewMinutes.toString())
    }
    val hasSource = selectedSourceId != null || legacySourceName.isNotBlank()
    val currentPageCode = pageCodeText.toIntOrNull() ?: 0
    // 2026-11-22 用户要求：预估复习时长必须是 >= 1 的整数，否则禁用保存。
    val reviewMinutesValid = minutes.toIntOrNull()?.let { it >= 1 } == true

    /** 新建页切换「有无来源」时，在用户未手改编号的前提下套用对应类的默认记忆。 */
    fun applyDefaultCodeForSource(hasSource: Boolean) {
        if (content.questionId == null && !pageCodeTouched) {
            val target = if (hasSource) {
                content.defaultSourcePageCodeWithSource
            } else {
                content.defaultSourcePageCodeWithoutSource
            }
            pageCodeText = target.toString()
        }
    }
    var selectionLimitReached by remember(content) { mutableStateOf(false) }
    val selectedIds = remember(content) {
        mutableStateListOf<String>().apply { addAll(content.selectedKnowledgePointIds) }
    }
    // 来源候选独立于 content：窗口内增删改后只刷新候选，不重建表单。
    var sourceTags by remember(content) { mutableStateOf(content.sourceTags) }
    // 知识点候选同样独立于 content：窗口内增删改后只刷新候选，不重建表单。
    var knowledgeTags by remember(content) { mutableStateOf(content.tags) }
    var showSourceManager by remember { mutableStateOf(false) }
    var showKnowledgeManager by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (content.printed) {
            Text(
                text = "该题已打印过旧计划，修改关联后请重新生成计划并打印。",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // 题号与页码编号同行并排，各占一半宽度。
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = questionNumber,
                onValueChange = { questionNumber = it },
                label = { Text("题号，例如 T-1") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )

            // 页码编号：带来源时可直接用键盘输入；无来源时只读，靠右侧 +/− 调整。
            OutlinedTextField(
                value = pageCodeText,
                onValueChange = { input ->
                    if (hasSource) {
                        pageCodeText = input.filter(Char::isDigit).take(5)
                        pageCodeTouched = true
                    }
                },
                label = { Text("页码编号") },
                singleLine = true,
                readOnly = !hasSource,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    // 文本框右侧上下堆叠的 +/−，与输入区等高。
                    Column(
                        modifier = Modifier
                            .width(28.dp)
                            .height(40.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        PageCodeStepButton(
                            label = "+",
                            enabled = true,
                            onClick = {
                                pageCodeText = (maxOf(currentPageCode, 0) + 1).toString()
                                pageCodeTouched = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        )
                        PageCodeStepButton(
                            label = "−",
                            enabled = currentPageCode > 1,
                            onClick = {
                                if (currentPageCode > 1) {
                                    pageCodeText = (currentPageCode - 1).toString()
                                    pageCodeTouched = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        )
                    }
                },
            )
        }
        if (hasSource && currentPageCode < 1) {
            Text(
                text = "请输入不小于 1 的页码编号。",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // 来源：与知识点一样从标签库单选（可空），点击已选标签可取消选择。
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "来源（可不选）",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { showSourceManager = true }) { Text("管理来源") }
        }
        val selectedSourceName = sourceTags.firstOrNull { it.id == selectedSourceId }?.name
            ?: legacySourceName
        // 旧题目的自由文本来源可能不在标签库中：保留为可取消选中的临时标签。
        val hasLegacyChip = legacySourceName.isNotBlank() &&
            sourceTags.none { it.name == legacySourceName }
        if (sourceTags.isEmpty() && !hasLegacyChip) {
            Text(
                text = "来源标签库为空。请先到来源标签库添加标签，再回来选择。",
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                sourceTags.forEach { tag ->
                    val selected = tag.id == selectedSourceId
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) {
                                selectedSourceId = null
                                applyDefaultCodeForSource(hasSource = false)
                            } else {
                                selectedSourceId = tag.id
                                legacySourceName = ""
                                applyDefaultCodeForSource(hasSource = true)
                            }
                        },
                        label = { Text(tag.name) },
                    )
                }
                if (hasLegacyChip) {
                    val selected = selectedSourceId == null
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) {
                                legacySourceName = ""
                                applyDefaultCodeForSource(hasSource = false)
                            }
                        },
                        label = { Text(legacySourceName) },
                    )
                }
            }
            Text(
                text = "已选来源：${selectedSourceName.ifBlank { "未选择" }}",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "知识点标签（最多选 ${EntryDraft.MAX_KNOWLEDGE_POINTS} 个）",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { showKnowledgeManager = true }) { Text("管理知识点") }
        }
        if (knowledgeTags.isEmpty()) {
            Text(
                text = "知识点标签库为空。请点击「管理知识点」添加标签，再回来关联。",
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                knowledgeTags.forEach { tag ->
                    val selected = tag.id in selectedIds
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) {
                                selectedIds.remove(tag.id)
                                selectionLimitReached = false
                            } else if (selectedIds.size < EntryDraft.MAX_KNOWLEDGE_POINTS) {
                                selectedIds.add(tag.id)
                                selectionLimitReached = false
                            } else {
                                selectionLimitReached = true
                            }
                        },
                        label = { Text(tag.name) },
                    )
                }
            }
            Text(
                text = "已选 ${selectedIds.size} / ${EntryDraft.MAX_KNOWLEDGE_POINTS}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (selectionLimitReached) {
            Text(
                text = "单题最多关联 ${EntryDraft.MAX_KNOWLEDGE_POINTS} 个知识点，请拆分为多道题目。",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        viewModel.actionError?.let { message ->
            Text(message, color = MaterialTheme.colorScheme.error)
        }

        val canSave = questionNumber.isNotBlank() &&
            selectedIds.size in 1..EntryDraft.MAX_KNOWLEDGE_POINTS &&
            currentPageCode >= 1 &&
            reviewMinutesValid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = minutes,
                onValueChange = { minutes = it.filter(Char::isDigit) },
                label = { Text("预估复习时长（分钟）") },
                singleLine = true,
                isError = !reviewMinutesValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            Button(
                enabled = canSave && !viewModel.saving,
                onClick = {
                    scope.launch {
                        val saved = viewModel.save(
                            questionNumber = questionNumber,
                            sourcePageCode = currentPageCode,
                            sourcePageLabel = sourceTags.firstOrNull { it.id == selectedSourceId }?.name
                                ?: legacySourceName,
                            knowledgePointIds = selectedIds.toList(),
                            estimatedReviewMinutes = minutes.toIntOrNull()
                                ?: EntryDraft.DEFAULT_REVIEW_MINUTES,
                        )
                        if (saved) onSaved()
                    }
                },
            ) {
                Text("保存")
            }
        }
        if (!reviewMinutesValid) {
            Text(
                text = "预估复习时长必须输入不小于 1 的整数。",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (showSourceManager) {
        SourceTagManagerDialog(
            viewModel = sourceTagViewModel,
            onDismiss = {
                showSourceManager = false
                // 窗口内可能新增/重命名/删除来源：关闭时同步最新候选，已填表单保持不变。
                val latest = (sourceTagViewModel.state as? UiState.Content)?.data?.tags
                    ?: sourceTags
                sourceTags = latest
                if (selectedSourceId != null && latest.none { it.id == selectedSourceId }) {
                    // 已选来源被删除：清空选择，避免保存出不在标签库中的名称。
                    selectedSourceId = null
                    applyDefaultCodeForSource(hasSource = false)
                }
                if (legacySourceName.isNotBlank()) {
                    // 旧文本来源恰好被新增为标签：直接采纳为新标签的选中项。
                    latest.firstOrNull { it.name == legacySourceName }?.let { tag ->
                        selectedSourceId = tag.id
                        legacySourceName = ""
                        applyDefaultCodeForSource(hasSource = true)
                    }
                }
            },
        )
    }

    if (showKnowledgeManager) {
        KnowledgePointManagerDialog(
            viewModel = knowledgePointViewModel,
            onDismiss = {
                showKnowledgeManager = false
                // 窗口内可能新增/重命名/删除知识点：关闭时同步最新候选，已填表单保持不变。
                val latest: List<KnowledgePointTag> =
                    when (val managerState = knowledgePointViewModel.state) {
                        is UiState.Content -> managerState.data.tags
                        UiState.Empty -> emptyList()
                        else -> knowledgeTags
                    }
                knowledgeTags = latest
                // 已选知识点被删除：从选择中移除，避免保存出不在标签库中的关联。
                val availableIds = latest.mapTo(HashSet()) { it.id }
                selectedIds.retainAll(availableIds)
                selectionLimitReached = false
            },
        )
    }
}

/** 页码编号的小号 +/− 按钮；不使用 Material Button，避免把堆叠高度撑到 48dp 以上。 */
@Composable
private fun PageCodeStepButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
