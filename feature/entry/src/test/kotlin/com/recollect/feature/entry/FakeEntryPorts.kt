package com.recollect.feature.entry

import com.recollect.data.room.repository.EntryDraft
import com.recollect.data.room.repository.EntryRepository
import com.recollect.data.room.repository.EntrySummary
import com.recollect.data.room.repository.KnowledgePointLibraryRepository
import com.recollect.data.room.repository.KnowledgePointTag

/** 录入端口的内存 fake：只模拟列表/新建/更新/归档，不伪造调度或分页结果。 */
internal class FakeEntryRepository(
    initial: List<EntrySummary> = emptyList(),
) : EntryRepository {
    val rows = initial.toMutableList()
    var defaultSourcePageCodeValue = 1
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

    override suspend fun defaultSourcePageCode(): Int = defaultSourcePageCodeValue
}

/** 标签库端口的内存 fake。 */
internal class FakeLibraryRepository(
    initial: List<KnowledgePointTag> = emptyList(),
) : KnowledgePointLibraryRepository {
    private val stored = initial.toMutableList()

    override suspend fun tags(): List<KnowledgePointTag> = stored.sortedBy { it.number }

    override suspend fun create(name: String): KnowledgePointTag {
        val tag = KnowledgePointTag(
            id = "kp-${stored.size + 1}",
            number = stored.size + 1,
            name = name,
            linkedQuestionCount = 0,
        )
        stored += tag
        return tag
    }

    override suspend fun rename(knowledgePointId: String, name: String) {
        val index = stored.indexOfFirst { it.id == knowledgePointId }
        stored[index] = stored[index].copy(name = name)
    }

    override suspend fun deleteWithTombstone(knowledgePointId: String, reason: String) {
        stored.removeAll { it.id == knowledgePointId }
    }
}

internal fun tag(id: String, number: Int, name: String) =
    KnowledgePointTag(id = id, number = number, name = name, linkedQuestionCount = 0)

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
