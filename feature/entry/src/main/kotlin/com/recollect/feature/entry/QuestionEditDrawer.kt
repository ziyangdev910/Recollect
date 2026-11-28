package com.recollect.feature.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.recollect.common.UiState
import com.recollect.data.room.repository.EntryDraft
import com.recollect.data.room.repository.EntrySummary
import com.recollect.data.room.repository.KnowledgeCategoryItem
import com.recollect.data.room.repository.KnowledgePointTag
import com.recollect.data.room.repository.formatSourcePage
import kotlinx.coroutines.launch

/**
 * 「录入题目 / 编辑题目」底部抽屉（2026-11-28 用户要求）。
 *
 * 相对于原独立路由，改为吸附在页面底部、盖住悬浮胶囊栏的抽屉：
 * 顶部实时预览题目卡片，下方依次是「科目 / 来源 / 第X页 · 第Y题 · 复习时长 / 知识点」，
 * 每个选择行被点开时在其下方就地展开选项；「取消 / 保存」胶囊悬浮在底部中间。
 *
 * 科目只是知识点分类的筛选入口，不单独持久化；题目所属分类仍由关联知识点推导。
 * 从某个知识分类错题本进入新建时，由调用方通过 [initialSubjectCategoryId] 预填科目。
 * 编辑既有题目复用同一抽屉，回填全部字段并在保存后覆盖原题。
 */
@Composable
fun QuestionEditDrawer(
    viewModel: QuestionEditViewModel,
    sourceTagViewModel: SourceTagLibraryViewModel,
    knowledgePointViewModel: KnowledgePointLibraryViewModel,
    questionId: String?,
    initialSubjectCategoryId: String? = null,
    prefill: QuestionEditPrefill? = null,
    tourFocus: QuestionEditTourTarget? = null,
    onTourTarget: (QuestionEditTourTarget, Rect) -> Unit = { _, _ -> },
    subjectToneIndex: (KnowledgeCategoryItem) -> Int = { category ->
        category.coverTone?.takeIf { it in 0 until NOTEBOOK_TONE_COUNT }
            ?: category.name.hashCode().mod(NOTEBOOK_TONE_COUNT)
    },
    onDismiss: () -> Unit,
    /** 保存成功后回调题目所属科目；调用方据此进入对应科目的错题本。 */
    onSaved: (QuestionSavedNotebook?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(viewModel, questionId, prefill) { viewModel.load(questionId, prefill) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight * 0.92f)
                // 抽屉自身消费空白处的点击，避免穿透到背后的蒙层把抽屉关掉。
                .pointerInput(Unit) { detectTapGestures { } },
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 20.dp,
        ) {
            when (val state = viewModel.state) {
                UiState.Loading -> DrawerMessage {
                    CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                }

                UiState.Empty -> DrawerMessage {
                    Text("题目不存在或已被删除。")
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }

                is UiState.Error -> DrawerMessage {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.retryable) {
                            TextButton(onClick = {
                                scope.launch { viewModel.load(questionId, prefill) }
                            }) {
                                Text("重试")
                            }
                        }
                        TextButton(onClick = onDismiss) { Text("关闭") }
                    }
                }

                is UiState.Content -> QuestionDrawerForm(
                    viewModel = viewModel,
                    sourceTagViewModel = sourceTagViewModel,
                    knowledgePointViewModel = knowledgePointViewModel,
                    content = state.data,
                    initialSubjectCategoryId = initialSubjectCategoryId,
                    tourFocus = tourFocus,
                    onTourTarget = onTourTarget,
                    subjectToneIndex = subjectToneIndex,
                    onDismiss = onDismiss,
                    onSaved = onSaved,
                )
            }
        }
    }
}

@Composable
private fun DrawerMessage(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        content()
    }
}

/** 抽屉内当前展开的选择行；同一时刻只展开一个，保持抽屉紧凑。 */
private enum class DrawerSection { NONE, SUBJECT, SOURCE, KNOWLEDGE }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionDrawerForm(
    viewModel: QuestionEditViewModel,
    sourceTagViewModel: SourceTagLibraryViewModel,
    knowledgePointViewModel: KnowledgePointLibraryViewModel,
    content: QuestionEditContent,
    initialSubjectCategoryId: String?,
    tourFocus: QuestionEditTourTarget?,
    onTourTarget: (QuestionEditTourTarget, Rect) -> Unit,
    subjectToneIndex: (KnowledgeCategoryItem) -> Int,
    onDismiss: () -> Unit,
    onSaved: (QuestionSavedNotebook?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    val numberRequester = remember { BringIntoViewRequester() }
    val pageCodeRequester = remember { BringIntoViewRequester() }
    val minutesRequester = remember { BringIntoViewRequester() }
    val sourceRequester = remember { BringIntoViewRequester() }
    val knowledgeRequester = remember { BringIntoViewRequester() }
    val saveRequester = remember { BringIntoViewRequester() }

    var questionNumber by remember(content) { mutableStateOf(content.questionNumber) }
    var pageCodeText by remember(content) { mutableStateOf(content.sourcePageCode.toString()) }
    var pageCodeTouched by remember(content) { mutableStateOf(false) }
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
    var minutes by remember(content) { mutableStateOf(content.estimatedReviewMinutes.toString()) }
    val selectedIds = remember(content) {
        mutableStateListOf<String>().apply { addAll(content.selectedKnowledgePointIds) }
    }
    var sourceTags by remember(content) { mutableStateOf(content.sourceTags) }
    var knowledgeTags by remember(content) { mutableStateOf(content.tags) }
    var knowledgeCategories by remember(content) { mutableStateOf(content.knowledgeCategories) }
    var selectedSubjectId by remember(content, initialSubjectCategoryId) {
        mutableStateOf(resolveInitialSubject(content, initialSubjectCategoryId))
    }
    var expandedSection by remember(content) { mutableStateOf(DrawerSection.NONE) }
    var selectionLimitReached by remember(content) { mutableStateOf(false) }
    var showSourceManager by remember { mutableStateOf(false) }
    var showKnowledgeManager by remember { mutableStateOf(false) }

    val hasSource = selectedSourceId != null || legacySourceName.isNotBlank()
    val currentPageCode = pageCodeText.toIntOrNull() ?: 0
    val minutesValue = minutes.toIntOrNull()
    val minutesValid = minutesValue != null && minutesValue in 1..EntryDraft.MAX_REVIEW_MINUTES
    val selectedSourceName = sourceTags.firstOrNull { it.id == selectedSourceId }?.name
        ?: legacySourceName
    val selectedKnowledge = knowledgeTags
        .filter { it.id in selectedIds }
        .sortedBy { it.number }
    val filteredKnowledgeTags = knowledgeTags.filter {
        selectedSubjectId == null || it.categoryId == selectedSubjectId
    }
    val selectedSubject = knowledgeCategories.firstOrNull { it.id == selectedSubjectId }
    val dark = isSystemInDarkTheme()

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

    fun selectSubject(category: KnowledgeCategoryItem?) {
        selectedSubjectId = category?.id
        if (category != null) {
            // 科目切换后只保留该分类下的知识点，避免表单里留下当前列表看不到的关联。
            val allowed = knowledgeTags
                .filter { it.categoryId == category.id }
                .mapTo(HashSet()) { it.id }
            selectedIds.retainAll(allowed)
            selectionLimitReached = false
        }
        expandedSection = DrawerSection.NONE
    }

    // 首次演示：聚焦到某个选择行时自动展开它的选项，并滚动到可见位置。
    LaunchedEffect(tourFocus) {
        when (tourFocus) {
            QuestionEditTourTarget.NUMBER -> numberRequester.bringIntoView()
            QuestionEditTourTarget.PAGE_CODE -> pageCodeRequester.bringIntoView()
            QuestionEditTourTarget.MINUTES -> minutesRequester.bringIntoView()
            QuestionEditTourTarget.SOURCE -> {
                expandedSection = DrawerSection.SOURCE
                sourceRequester.bringIntoView()
            }
            QuestionEditTourTarget.KNOWLEDGE -> {
                expandedSection = DrawerSection.KNOWLEDGE
                knowledgeRequester.bringIntoView()
            }
            QuestionEditTourTarget.SAVE -> saveRequester.bringIntoView()
            null -> Unit
        }
    }

    val canSave = questionNumber.isNotBlank() &&
        selectedIds.size in 1..EntryDraft.MAX_KNOWLEDGE_POINTS &&
        currentPageCode >= 1 &&
        minutesValid &&
        !viewModel.saving

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 抽屉拖拽条。
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(colors.outlineVariant),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // 在滚动视口外为底部胶囊预留高度：内容（含演示聚焦的字段与选择行）
                    // 只会滚动到胶囊上方，不会被「取消 / 保存」盖住。
                    .padding(bottom = 84.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (content.printed) {
                    Text(
                        text = "该题已打印过旧计划，修改关联后请重新生成计划并打印。",
                        color = colors.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                // 题目卡片实时预览：与错题本列表使用同一张卡片，无「更多」菜单与序号。
                EntryQuestionCard(
                    row = EntrySummary(
                        questionId = content.questionId.orEmpty(),
                        questionNumber = questionNumber,
                        sourcePageCode = currentPageCode,
                        sourcePageLabel = selectedSourceName,
                        knowledgeNames = selectedKnowledge.map { it.name },
                        knowledgePointIds = selectedIds.toList(),
                        estimatedReviewMinutes = minutesValue ?: 0,
                    ),
                )

                // 科目与来源并排；点开任一选择框时，选项在整行下方展开。
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    SelectorRow(
                        label = "科目",
                        value = selectedSubject?.name ?: "未选择",
                        stacked = true,
                        container = selectedSubject?.let {
                            notebookTone(subjectToneIndex(it), dark).container
                        },
                        contentColor = selectedSubject?.let {
                            notebookTone(subjectToneIndex(it), dark).content
                        },
                        expanded = expandedSection == DrawerSection.SUBJECT,
                        // 科目名通常较短；来源与「管理来源」需要更多横向空间。
                        modifier = Modifier.weight(0.8f),
                        onClick = {
                            expandedSection = if (expandedSection == DrawerSection.SUBJECT) {
                                DrawerSection.NONE
                            } else {
                                DrawerSection.SUBJECT
                            }
                        },
                    )
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .bringIntoViewRequester(sourceRequester)
                            .onGloballyPositioned {
                                onTourTarget(QuestionEditTourTarget.SOURCE, it.boundsInRoot())
                            },
                    ) {
                        SelectorRow(
                            label = "来源",
                            // 未选来源时题目按试卷口径展示，字段本身也明确提示这一点。
                            value = selectedSourceName.ifBlank { "未选择（来自试卷）" },
                            stacked = true,
                            expanded = expandedSection == DrawerSection.SOURCE,
                            onClick = {
                                expandedSection = if (expandedSection == DrawerSection.SOURCE) {
                                    DrawerSection.NONE
                                } else {
                                    DrawerSection.SOURCE
                                }
                            },
                            action = {
                                CompactManageButton(
                                    label = "管理来源",
                                    onClick = { showSourceManager = true },
                                )
                            },
                        )
                    }
                }

                // 科目选项：不再提供「全部科目」；再次点击已选科目即清除筛选。
                if (expandedSection == DrawerSection.SUBJECT) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        knowledgeCategories.forEach { category ->
                            val selected = category.id == selectedSubjectId
                            val tone = notebookTone(subjectToneIndex(category), dark)
                            OptionCard(
                                label = category.name,
                                selected = selected,
                                container = tone.container,
                                contentColor = tone.content,
                                onClick = {
                                    if (selected) {
                                        selectSubject(null)
                                    } else {
                                        selectSubject(category)
                                    }
                                },
                            )
                        }
                    }
                }

                // 来源选项：整行展开；单选，再次点击已选标签可取消。
                if (expandedSection == DrawerSection.SOURCE) {
                    val hasLegacyChip = legacySourceName.isNotBlank() &&
                        sourceTags.none { it.name == legacySourceName }
                    if (sourceTags.isEmpty() && !hasLegacyChip) {
                        Text(
                            text = "来源标签库为空。可先添加标签，再回来选择。",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.error,
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            sourceTags.forEach { tag ->
                                OptionCard(
                                    label = tag.name,
                                    selected = tag.id == selectedSourceId,
                                    container = if (tag.id == selectedSourceId) {
                                        colors.primaryContainer
                                    } else {
                                        colors.surfaceContainerLow
                                    },
                                    contentColor = if (tag.id == selectedSourceId) {
                                        colors.onPrimaryContainer
                                    } else {
                                        colors.onSurface
                                    },
                                    onClick = {
                                        val deselect = tag.id == selectedSourceId
                                        selectedSourceId = if (deselect) null else tag.id
                                        legacySourceName = ""
                                        applyDefaultCodeForSource(hasSource = !deselect)
                                        expandedSection = DrawerSection.NONE
                                    },
                                )
                            }
                            if (hasLegacyChip) {
                                OptionCard(
                                    label = legacySourceName,
                                    selected = selectedSourceId == null,
                                    container = colors.surfaceContainerLow,
                                    contentColor = colors.onSurface,
                                    onClick = {
                                        legacySourceName = ""
                                        applyDefaultCodeForSource(hasSource = false)
                                        expandedSection = DrawerSection.NONE
                                    },
                                )
                            }
                        }
                    }
                }

                // 页码（无来源时写「份试卷」）、第Y题与复习时长：三个紧凑圆角文本框并排。
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompactField(
                        prefix = "第",
                        // 未选来源时题目按试卷份数定位，字段文案随之切换（2026-11-28 用户要求）。
                        suffix = if (hasSource) "页" else "份试卷",
                        value = pageCodeText,
                        placeholder = if (hasSource) "页码" else "份数",
                        numeric = true,
                        onValueChange = { input ->
                            pageCodeText = input.filter(Char::isDigit).take(5)
                            pageCodeTouched = true
                        },
                        modifier = Modifier
                            // 「份试卷」比「页」宽，未选来源时给页码框略多空间。
                            .weight(if (hasSource) 1f else 1.25f)
                            .bringIntoViewRequester(pageCodeRequester)
                            .onGloballyPositioned {
                                onTourTarget(QuestionEditTourTarget.PAGE_CODE, it.boundsInRoot())
                            },
                    )
                    CompactField(
                        prefix = "第",
                        suffix = "题",
                        value = questionNumber,
                        // 题号不预填示例值，留空等待用户输入。
                        placeholder = "",
                        numeric = false,
                        onValueChange = { questionNumber = it },
                        modifier = Modifier
                            .weight(1f)
                            .bringIntoViewRequester(numberRequester)
                            .onGloballyPositioned {
                                onTourTarget(QuestionEditTourTarget.NUMBER, it.boundsInRoot())
                            },
                    )
                    CompactField(
                        prefix = null,
                        suffix = "分钟",
                        value = minutes,
                        placeholder = "时长",
                        numeric = true,
                        onValueChange = { minutes = it.filter(Char::isDigit).take(3) },
                        modifier = Modifier
                            .weight(1f)
                            .bringIntoViewRequester(minutesRequester)
                            .onGloballyPositioned {
                                onTourTarget(QuestionEditTourTarget.MINUTES, it.boundsInRoot())
                            },
                    )
                }
                if (currentPageCode < 1) {
                    FieldError("请填写不小于 1 的页码。")
                }
                if (!minutesValid) {
                    FieldError("复习时长需在 1～${EntryDraft.MAX_REVIEW_MINUTES} 分钟之间。")
                }


                // 知识点：位于表单最下方；选中项以一个个小卡片排在行内，点开在下方展开完整选项。
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(knowledgeRequester)
                        .onGloballyPositioned {
                            onTourTarget(QuestionEditTourTarget.KNOWLEDGE, it.boundsInRoot())
                        },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SelectorRow(
                        label = "知识点",
                        value = if (selectedKnowledge.isEmpty()) "未选择" else "",
                        expanded = expandedSection == DrawerSection.KNOWLEDGE,
                        onClick = {
                            expandedSection = if (expandedSection == DrawerSection.KNOWLEDGE) {
                                DrawerSection.NONE
                            } else {
                                DrawerSection.KNOWLEDGE
                            }
                        },
                        action = {
                            CompactManageButton(
                                label = "管理知识点",
                                onClick = { showKnowledgeManager = true },
                            )
                        },
                        content = if (selectedKnowledge.isEmpty()) {
                            null
                        } else {
                            {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    selectedKnowledge.forEach { tag ->
                                        KnowledgeChipCard(tag.name)
                                    }
                                }
                            }
                        },
                    )
                    if (expandedSection == DrawerSection.KNOWLEDGE) {
                        when {
                            knowledgeTags.isEmpty() -> Text(
                                text = "知识点标签库为空。可先添加标签，再回来关联。",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.error,
                            )

                            filteredKnowledgeTags.isEmpty() -> Text(
                                text = "当前科目下暂无知识点，可切换其他科目。",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )

                            else -> FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                filteredKnowledgeTags.forEach { tag ->
                                    val selected = tag.id in selectedIds
                                    OptionCard(
                                        label = tag.name,
                                        selected = selected,
                                        container = if (selected) {
                                            colors.secondaryContainer
                                        } else {
                                            colors.surfaceContainerLow
                                        },
                                        contentColor = if (selected) {
                                            colors.onSecondaryContainer
                                        } else {
                                            colors.onSurface
                                        },
                                        onClick = {
                                            if (selected) {
                                                selectedIds.remove(tag.id)
                                                selectionLimitReached = false
                                            } else if (
                                                selectedIds.size < EntryDraft.MAX_KNOWLEDGE_POINTS
                                            ) {
                                                selectedIds.add(tag.id)
                                                selectionLimitReached = false
                                            } else {
                                                selectionLimitReached = true
                                            }
                                        },
                                    )
                                }
                            }
                        }
                        Text(
                            text = "已选 ${selectedIds.size} / ${EntryDraft.MAX_KNOWLEDGE_POINTS}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant,
                        )
                        if (selectionLimitReached) {
                            FieldError(
                                "单题最多关联 ${EntryDraft.MAX_KNOWLEDGE_POINTS} 个知识点，请拆分为多道题目。",
                            )
                        }
                    }
                }

                viewModel.actionError?.let { message ->
                    Text(message, color = colors.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // 取消 / 保存胶囊：悬浮在抽屉底部中间，滚动内容为它预留了底部间距。
        DrawerActionPills(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
            canSave = canSave,
            saving = viewModel.saving,
            saveModifier = Modifier
                .bringIntoViewRequester(saveRequester)
                .onGloballyPositioned {
                    onTourTarget(QuestionEditTourTarget.SAVE, it.boundsInRoot())
                },
            onCancel = onDismiss,
            onSave = {
                scope.launch {
                    val saved = viewModel.save(
                        questionNumber = questionNumber,
                        sourcePageCode = currentPageCode,
                        sourcePageLabel = selectedSourceName,
                        knowledgePointIds = selectedIds.toList(),
                        estimatedReviewMinutes = minutesValue ?: EntryDraft.DEFAULT_REVIEW_MINUTES,
                    )
                    if (saved) {
                        onSaved(
                            resolveSavedNotebook(
                                selectedSubjectId = selectedSubjectId,
                                selectedKnowledgePointIds = selectedIds.toList(),
                                knowledgeTags = knowledgeTags,
                                knowledgeCategories = knowledgeCategories,
                            ),
                        )
                    }
                }
            },
        )
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
                    selectedSourceId = null
                    applyDefaultCodeForSource(hasSource = false)
                }
                if (legacySourceName.isNotBlank()) {
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
                val managerState = knowledgePointViewModel.state
                val latest: List<KnowledgePointTag> = when (managerState) {
                    is UiState.Content -> managerState.data.tags
                    UiState.Empty -> emptyList()
                    else -> knowledgeTags
                }
                knowledgeTags = latest
                knowledgeCategories = when (managerState) {
                    is UiState.Content -> managerState.data.categories
                    else -> knowledgeCategories
                }
                val availableIds = latest.mapTo(HashSet()) { it.id }
                selectedIds.retainAll(availableIds)
                if (selectedSubjectId != null &&
                    knowledgeCategories.none { it.id == selectedSubjectId }
                ) {
                    selectedSubjectId = null
                }
                selectionLimitReached = false
            },
        )
    }
}

/**
 * 新建题目时的科目初值：
 * - 从某个知识分类错题本进入时由调用方预填；
 * - 从主页进入不填默认值（2026-11-28 用户要求）；
 * - 编辑既有题目时，若关联知识点同属一个分类，用该分类展示科目。
 */
private fun resolveInitialSubject(
    content: QuestionEditContent,
    initialSubjectCategoryId: String?,
): String? {
    if (initialSubjectCategoryId != null &&
        content.knowledgeCategories.any { it.id == initialSubjectCategoryId }
    ) {
        return initialSubjectCategoryId
    }
    if (content.questionId == null) return null
    val categoryIds = content.selectedKnowledgePointIds
        .mapNotNull { id -> content.tags.firstOrNull { it.id == id }?.categoryId }
        .distinct()
    return categoryIds.singleOrNull()
}

/** 选择行：左侧为字段名，右侧为当前值或已选小卡片，末尾是展开箭头。 */
@Composable
private fun SelectorRow(
    label: String,
    value: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color? = null,
    contentColor: Color? = null,
    /** 两栏并排时使用上下结构：字段名与操作在首行，当前值单独一行。 */
    stacked: Boolean = false,
    /** 常驻在选择框内、展开箭头左侧的次要操作（如「管理知识点」「管理来源」）。 */
    action: (@Composable () -> Unit)? = null,
    content: (@Composable () -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val bg = container ?: colors.surfaceContainerLow
    val fg = contentColor ?: colors.onSurface
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = bg,
        contentColor = fg,
        border = BorderStroke(
            1.dp,
            if (container != null) fg.copy(alpha = 0.10f) else colors.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (stacked) 12.dp else 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (stacked) 4.dp else 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = fg.copy(alpha = 0.72f),
                )
                if (stacked) {
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.width(10.dp))
                    if (value.isNotBlank()) {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                action?.invoke()
                Icon(
                    painter = painterResource(R.drawable.ic_expand_more),
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = fg.copy(alpha = 0.66f),
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(if (expanded) 180f else 0f),
                )
            }
            if (stacked && value.isNotBlank()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            content?.invoke()
        }
    }
}

/** 选择框内的次要操作：常驻在展开箭头左侧，点击不触发整行展开。 */
@Composable
private fun CompactManageButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
        )
    }
}

/** 选项小卡片：科目使用分类封面底色，其他选项使用主题色。 */
@Composable
internal fun OptionCard(
    label: String,
    selected: Boolean,
    container: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = container,
        contentColor = contentColor,
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) contentColor.copy(alpha = 0.58f) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (selected) {
                Text(text = "✓", style = MaterialTheme.typography.labelMedium)
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }
    }
}

/** 已选知识点在知识点行内的小卡片。 */
@Composable
private fun KnowledgeChipCard(name: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colors.secondaryContainer,
        contentColor = colors.onSecondaryContainer,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 紧凑圆角文本框：高度贴近一行文字，前后可带「第 / 页」「分钟」等单位。 */
@Composable
private fun CompactField(
    prefix: String?,
    suffix: String?,
    value: String,
    placeholder: String,
    numeric: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceContainerLow,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            prefix?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
                ),
                cursorBrush = SolidColor(colors.primary),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty() && placeholder.isNotBlank()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                                maxLines = 1,
                            )
                        }
                        inner()
                    }
                },
            )
            suffix?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FieldError(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

/** 底部悬浮的「取消 / 保存」胶囊；样式与主页底部胶囊栏一致。 */
@Composable
private fun DrawerActionPills(
    canSave: Boolean,
    saving: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    saveModifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PillTextAction(
                label = "取消",
                color = colors.onSurface,
                onClick = onCancel,
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(18.dp)
                    .background(colors.outlineVariant),
            )
            PillTextAction(
                label = if (saving) "保存中…" else "保存",
                color = if (canSave) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.45f),
                enabled = canSave,
                onClick = onSave,
                modifier = saveModifier,
            )
        }
    }
}

@Composable
private fun PillTextAction(
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            maxLines = 1,
        )
    }
}

/** 保存成功后应进入的错题本（科目）投影。 */
data class QuestionSavedNotebook(
    val categoryId: String,
    val categoryName: String,
)

/**
 * 解析保存后应进入的错题本：优先取显式选择的科目；
 * 未选科目时取第一个关联知识点（按编号升序）所属的分类。
 *
 * 用户可能同时关联多个分类的知识点；此时进入首个知识点所在的错题本，
 * 题目本身仍会出现在所有关联分类的错题本中。无可解析分类时返回 null。
 */
internal fun resolveSavedNotebook(
    selectedSubjectId: String?,
    selectedKnowledgePointIds: List<String>,
    knowledgeTags: List<KnowledgePointTag>,
    knowledgeCategories: List<KnowledgeCategoryItem>,
): QuestionSavedNotebook? {
    val tagsById = knowledgeTags.associateBy { it.id }
    val categoryId = selectedSubjectId
        ?: selectedKnowledgePointIds
            .sortedBy { tagsById[it]?.number ?: Int.MAX_VALUE }
            .firstNotNullOfOrNull { tagsById[it]?.categoryId }
        ?: return null
    val categoryName = knowledgeCategories.firstOrNull { it.id == categoryId }?.name
        ?: return null
    return QuestionSavedNotebook(categoryId, categoryName)
}
