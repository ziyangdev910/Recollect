package com.recollect.feature.entry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.recollect.common.UiError
import com.recollect.common.UiState
import com.recollect.data.room.repository.EntryDraft
import com.recollect.data.room.repository.EntryRepository
import com.recollect.data.room.repository.KnowledgeCategoryItem
import com.recollect.data.room.repository.KnowledgeCategoryRepository
import com.recollect.data.room.repository.KnowledgePointLibraryRepository
import com.recollect.data.room.repository.KnowledgePointTag
import com.recollect.data.room.repository.SourceTagItem
import com.recollect.data.room.repository.SourceTagLibraryRepository
import kotlinx.coroutines.CancellationException

/** 录入/编辑页的初始内容；[questionId] 为 null 表示新建。 */
data class QuestionEditContent(
    val questionId: String?,
    val questionNumber: String,
    val sourcePageCode: Int,
    /** 无来源题目的默认编号（最近一条无来源题目）；新建页未选来源时显示。 */
    val defaultSourcePageCodeWithoutSource: Int,
    /** 有来源题目的默认编号（最近一条有来源题目）；新建页选中来源且未手改时显示。 */
    val defaultSourcePageCodeWithSource: Int,
    val sourcePageLabel: String,
    val estimatedReviewMinutes: Int,
    val selectedKnowledgePointIds: List<String>,
    val tags: List<KnowledgePointTag>,
    /** 知识点分类候选；知识点标签区据此筛选（2026-11-23 用户要求）。 */
    val knowledgeCategories: List<KnowledgeCategoryItem>,
    /** 来源标签库候选；录入页以单选标签填写来源。 */
    val sourceTags: List<SourceTagItem>,
    val printed: Boolean,
)

/**
 * 录入/编辑独立页面的状态持有者。
 *
 * 新建与编辑共用同一表单；编辑时从 Repository 读取题目与标签回填。
 * 校验在状态层先拦一次，Repository 事务内仍会复核（entry.md「双重校验」）。
 */
class QuestionEditViewModel(
    private val repository: EntryRepository,
    private val libraryRepository: KnowledgePointLibraryRepository,
    private val categoryRepository: KnowledgeCategoryRepository,
    private val sourceTagRepository: SourceTagLibraryRepository,
) {
    var state by mutableStateOf<UiState<QuestionEditContent>>(UiState.Loading)
        private set

    /** 保存失败的提示；不替换页面内容，便于表单原地展示。 */
    var actionError by mutableStateOf<String?>(null)
        private set

    var saving by mutableStateOf(false)
        private set

    fun clearActionError() {
        actionError = null
    }

    /** 加载标签库与待编辑题目；[questionId] 为 null 时使用默认页码编号与空表单。 */
    suspend fun load(questionId: String?) {
        state = try {
            val tags = libraryRepository.tags()
            val categories = categoryRepository.categories()
            val sourceTags = sourceTagRepository.tags()
            val defaultWithoutSource = repository.defaultSourcePageCode(hasSource = false)
            val defaultWithSource = repository.defaultSourcePageCode(hasSource = true)
            val content = if (questionId == null) {
                QuestionEditContent(
                    questionId = null,
                    questionNumber = "",
                    sourcePageCode = defaultWithoutSource,
                    defaultSourcePageCodeWithoutSource = defaultWithoutSource,
                    defaultSourcePageCodeWithSource = defaultWithSource,
                    sourcePageLabel = "",
                    estimatedReviewMinutes = EntryDraft.DEFAULT_REVIEW_MINUTES,
                    selectedKnowledgePointIds = emptyList(),
                    tags = tags,
                    knowledgeCategories = categories,
                    sourceTags = sourceTags,
                    printed = false,
                )
            } else {
                val summary = repository.summaries().firstOrNull { it.questionId == questionId }
                    ?: error("题目不存在或已被删除。")
                QuestionEditContent(
                    questionId = summary.questionId,
                    questionNumber = summary.questionNumber,
                    sourcePageCode = summary.sourcePageCode,
                    defaultSourcePageCodeWithoutSource = defaultWithoutSource,
                    defaultSourcePageCodeWithSource = defaultWithSource,
                    sourcePageLabel = summary.sourcePageLabel,
                    estimatedReviewMinutes = summary.estimatedReviewMinutes,
                    selectedKnowledgePointIds = summary.knowledgePointIds,
                    tags = tags,
                    knowledgeCategories = categories,
                    sourceTags = sourceTags,
                    printed = summary.printed,
                )
            }
            UiState.Content(content)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            UiError(
                code = "QUESTION_EDIT_LOAD_FAILED",
                message = e.message ?: "读取题目失败，请重试。",
                retryable = true,
            ).toUiState()
        }
    }

    /** 保存表单：新建调用 create，编辑调用 update 覆盖原题。成功返回 true。 */
    suspend fun save(
        questionNumber: String,
        sourcePageCode: Int,
        sourcePageLabel: String,
        knowledgePointIds: List<String>,
        estimatedReviewMinutes: Int,
    ): Boolean {
        actionError = null
        val content = (state as? UiState.Content)?.data ?: return false

        val trimmedNumber = questionNumber.trim()
        val selectedIds = knowledgePointIds.distinct()
        if (trimmedNumber.isEmpty()) {
            actionError = "题号不能为空。"
            return false
        }
        if (sourcePageCode < 1) {
            actionError = "页码编号必须从 1 开始。"
            return false
        }
        if (selectedIds.isEmpty()) {
            actionError = "请至少选择一个知识点标签。"
            return false
        }
        if (selectedIds.size > EntryDraft.MAX_KNOWLEDGE_POINTS) {
            actionError = "单题最多关联 ${EntryDraft.MAX_KNOWLEDGE_POINTS} 个知识点，请拆分为多道题目。"
            return false
        }

        saving = true
        return try {
            val draft = EntryDraft(
                questionNumber = trimmedNumber,
                sourcePageCode = sourcePageCode,
                sourcePageLabel = sourcePageLabel.trim(),
                knowledgePointIds = selectedIds,
                estimatedReviewMinutes = estimatedReviewMinutes,
            )
            if (content.questionId == null) {
                repository.create(draft)
            } else {
                repository.update(content.questionId, draft)
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            actionError = e.message ?: "保存失败，请重试。"
            false
        } finally {
            saving = false
        }
    }
}
