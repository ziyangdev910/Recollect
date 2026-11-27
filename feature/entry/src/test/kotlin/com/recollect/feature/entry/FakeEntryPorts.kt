package com.recollect.feature.entry

import com.recollect.data.room.repository.EntryDraft
import com.recollect.data.room.repository.EntryRepository
import com.recollect.data.room.repository.EntrySummary
import com.recollect.data.room.repository.KnowledgeCategoryItem
import com.recollect.data.room.repository.KnowledgeCategoryRepository
import com.recollect.data.room.repository.KnowledgePointLibraryRepository
import com.recollect.data.room.repository.KnowledgePointTag
import com.recollect.data.room.repository.SourceTagItem
import com.recollect.data.room.repository.SourceTagLibraryRepository

/** 录入端口的内存 fake：只模拟列表/新建/更新/归档，不伪造调度或分页结果。 */
internal class FakeEntryRepository(
    initial: List<EntrySummary> = emptyList(),
) : EntryRepository {
    val rows = initial.toMutableList()
    var defaultSourcePageCodeWithoutSource = 1
    var defaultSourcePageCodeWithSource = 1
    var createCalls = 0
    var updateCalls = 0
    var archiveCalls = 0
    var summaryCalls = 0
    var archiveFailure: Throwable? = null

    override suspend fun create(draft: EntryDraft): EntrySummary {
        createCalls++
        val summary = EntrySummary(
            questionId = "q-${rows.size + 1}",
            questionNumber = draft.questionNumber,
            sourcePageCode = draft.sourcePageCode,
            sourcePageLabel = draft.sourcePageLabel,
            knowledgeNames = draft.knowledgePointIds,
            knowledgePointIds = draft.knowledgePointIds,
            estimatedReviewMinutes = draft.estimatedReviewMinutes,
        )
        rows += summary
        return summary
    }

    override suspend fun update(questionId: String, draft: EntryDraft): EntrySummary {
        updateCalls++
        val index = rows.indexOfFirst { it.questionId == questionId }
        require(index >= 0) { "题目不存在。" }
        val updated = rows[index].copy(
            questionNumber = draft.questionNumber,
            sourcePageCode = draft.sourcePageCode,
            sourcePageLabel = draft.sourcePageLabel,
            knowledgeNames = draft.knowledgePointIds,
            knowledgePointIds = draft.knowledgePointIds,
            estimatedReviewMinutes = draft.estimatedReviewMinutes,
        )
        rows[index] = updated
        return updated
    }

    override suspend fun archive(questionId: String) {
        archiveCalls++
        archiveFailure?.let { throw it }
        rows.removeAll { it.questionId == questionId }
    }

    override suspend fun summaries(): List<EntrySummary> {
        summaryCalls++
        return rows.toList()
    }

    override suspend fun defaultSourcePageCode(hasSource: Boolean): Int =
        if (hasSource) defaultSourcePageCodeWithSource else defaultSourcePageCodeWithoutSource
}

/** 标签库端口的内存 fake。 */
internal class FakeLibraryRepository(
    initial: List<KnowledgePointTag> = emptyList(),
) : KnowledgePointLibraryRepository {
    private val stored = initial.toMutableList()

    override suspend fun tags(): List<KnowledgePointTag> = stored.sortedBy { it.number }

    override suspend fun create(name: String, categoryId: String): KnowledgePointTag {
        val tag = KnowledgePointTag(
            id = "kp-${stored.size + 1}",
            number = stored.size + 1,
            name = name,
            categoryId = categoryId,
            linkedQuestionCount = 0,
        )
        stored += tag
        return tag
    }

    override suspend fun update(knowledgePointId: String, name: String, categoryId: String) {
        val index = stored.indexOfFirst { it.id == knowledgePointId }
        stored[index] = stored[index].copy(name = name, categoryId = categoryId)
    }

    override suspend fun deleteWithTombstone(knowledgePointId: String, reason: String) {
        stored.removeAll { it.id == knowledgePointId }
    }
}

/** 分类端口的内存 fake。 */
internal class FakeCategoryRepository(
    initial: List<KnowledgeCategoryItem> = listOf(defaultCategory()),
) : KnowledgeCategoryRepository {
    val stored = initial.toMutableList()

    override suspend fun categories(): List<KnowledgeCategoryItem> = stored.toList()

    override suspend fun create(
        name: String,
        coverTone: Int?,
        coverIcon: String?,
    ): KnowledgeCategoryItem {
        val item = KnowledgeCategoryItem(
            id = "cat-${stored.size + 1}",
            name = name.trim(),
            linkedKnowledgePointCount = 0,
            coverTone = coverTone,
            coverIcon = coverIcon,
        )
        stored += item
        return item
    }

    override suspend fun updateDetails(
        categoryId: String,
        name: String,
        coverTone: Int?,
        coverIcon: String?,
    ) {
        val index = stored.indexOfFirst { it.id == categoryId }
        stored[index] = stored[index].copy(
            name = name.trim(),
            coverTone = coverTone,
            coverIcon = coverIcon,
        )
    }

    override suspend fun reorder(categoryIds: List<String>) {
        val byId = stored.associateBy { it.id }
        stored.clear()
        categoryIds.mapNotNull { byId[it] }.forEachIndexed { index, item ->
            stored += item.copy(sortOrder = index)
        }
    }

    override suspend fun rename(categoryId: String, name: String) {
        val index = stored.indexOfFirst { it.id == categoryId }
        stored[index] = stored[index].copy(name = name.trim())
    }

    override suspend fun delete(categoryId: String) {
        stored.removeAll { it.id == categoryId }
    }
}

internal const val DEFAULT_CATEGORY_ID = "cat-chinese"

internal fun defaultCategory() = KnowledgeCategoryItem(
    id = DEFAULT_CATEGORY_ID,
    name = "语文",
    linkedKnowledgePointCount = 0,
)

/** 来源标签库端口的内存 fake。 */
internal class FakeSourceTagRepository(
    initial: List<SourceTagItem> = emptyList(),
) : SourceTagLibraryRepository {
    val stored = initial.toMutableList()

    override suspend fun tags(): List<SourceTagItem> = stored.toList()

    override suspend fun create(name: String): SourceTagItem {
        val item = SourceTagItem(
            id = "source-${stored.size + 1}",
            name = name.trim(),
            linkedQuestionCount = 0,
        )
        stored += item
        return item
    }

    override suspend fun rename(sourceTagId: String, name: String) {
        val index = stored.indexOfFirst { it.id == sourceTagId }
        stored[index] = stored[index].copy(name = name.trim())
    }

    override suspend fun delete(sourceTagId: String) {
        stored.removeAll { it.id == sourceTagId }
    }
}

internal fun sourceTag(id: String, name: String, linkedQuestionCount: Int = 0) =
    SourceTagItem(id = id, name = name, linkedQuestionCount = linkedQuestionCount)

internal fun tag(
    id: String,
    number: Int,
    name: String,
    categoryId: String = DEFAULT_CATEGORY_ID,
) = KnowledgePointTag(
    id = id,
    number = number,
    name = name,
    categoryId = categoryId,
    linkedQuestionCount = 0,
)

internal fun entrySummary(
    questionId: String,
    questionNumber: String,
    sourcePageCode: Int = 1,
    sourcePageLabel: String = "",
    knowledgePointIds: List<String> = emptyList(),
    estimatedReviewMinutes: Int = 15,
    printed: Boolean = false,
) = EntrySummary(
    questionId = questionId,
    questionNumber = questionNumber,
    sourcePageCode = sourcePageCode,
    sourcePageLabel = sourcePageLabel,
    knowledgeNames = knowledgePointIds,
    knowledgePointIds = knowledgePointIds,
    estimatedReviewMinutes = estimatedReviewMinutes,
    printed = printed,
)
