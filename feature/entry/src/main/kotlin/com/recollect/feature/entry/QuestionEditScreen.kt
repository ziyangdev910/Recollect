package com.recollect.feature.entry

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
    questionId: String?,
    onManageTags: () -> Unit,
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
            content = state.data,
            onManageTags = onManageTags,
            onSaved = onSaved,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionForm(
    viewModel: QuestionEditViewModel,
    content: QuestionEditContent,
    onManageTags: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var questionNumber by remember(content) { mutableStateOf(content.questionNumber) }
    var sourcePageCode by remember(content) { mutableStateOf(content.sourcePageCode) }
    var sourcePageLabel by remember(content) { mutableStateOf(content.sourcePageLabel) }
    var minutes by remember(content) {
        mutableStateOf(content.estimatedReviewMinutes.toString())
    }
    var selectionLimitReached by remember(content) { mutableStateOf(false) }
    val selectedIds = remember(content) {
        mutableStateListOf<String>().apply { addAll(content.selectedKnowledgePointIds) }
    }

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

        OutlinedTextField(
            value = questionNumber,
            onValueChange = { questionNumber = it },
            label = { Text("题号，例如 T-1") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        // 页码 = 用户可调的编号 + 可选说明；同一页的多道题可保持同一编号。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 页码编号控件自带 +/−（文本框右侧上下堆叠，与输入区等高）。
            OutlinedTextField(
                value = sourcePageCode.toString(),
                onValueChange = {},
                label = { Text("页码编号") },
                singleLine = true,
                readOnly = true,
                // 约占总宽度的 1/3；说明字段占 2/3。
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    Column(
                        modifier = Modifier
                            .width(28.dp)
                            .height(40.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        PageCodeStepButton(
                            label = "+",
                            enabled = true,
                            onClick = { sourcePageCode++ },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        )
                        PageCodeStepButton(
                            label = "−",
                            enabled = sourcePageCode > 1,
                            onClick = { if (sourcePageCode > 1) sourcePageCode-- },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                        )
                    }
                },
            )
            OutlinedTextField(
                value = sourcePageLabel,
                onValueChange = { sourcePageLabel = it },
                label = { Text("说明（可空）") },
                singleLine = true,
                modifier = Modifier.weight(2f),
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
            TextButton(onClick = onManageTags) { Text("管理知识点") }
        }
        if (content.tags.isEmpty()) {
            Text(
                text = "标签库为空。请先到标签库添加知识点标签，再回来关联。",
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                content.tags.forEach { tag ->
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
            selectedIds.size in 1..EntryDraft.MAX_KNOWLEDGE_POINTS
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            Button(
                enabled = canSave && !viewModel.saving,
                onClick = {
                    scope.launch {
                        val saved = viewModel.save(
                            questionNumber = questionNumber,
                            sourcePageCode = sourcePageCode,
                            sourcePageLabel = sourcePageLabel,
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
