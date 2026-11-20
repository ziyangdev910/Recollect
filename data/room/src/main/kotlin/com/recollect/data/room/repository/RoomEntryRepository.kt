package com.recollect.data.room.repository

import com.recollect.data.room.dao.KnowledgePointDao
import com.recollect.data.room.dao.QuestionDao
import com.recollect.data.room.dao.QuestionKnowledgeCrossRefDao
import com.recollect.data.room.entity.KnowledgePoint
import com.recollect.data.room.entity.Question
import com.recollect.data.room.entity.QuestionKnowledgeCrossRef
import java.time.Clock
import java.util.UUID

/**
 * 录入事务：新建/编辑题目与知识点标签关联在同一个事务内完成（entry.md「职责」）。
 *
 * 标签必须已存在于标签库；知识点编号由标签库创建时分配，这里只建立关联并重算
 * `activeQuestionCount`（data-model.md：关联变化后由 Repository 重算）。
 * 关联数量在 Repository 再校验一次（E-03），不依赖 UI 的拦截。
 * 题目只归档、不硬删除（D-11），归档不影响历史反馈与知识点状态。
 */
class RoomEntryRepository(
    private val transactionRunner: TransactionRunner,
    private val knowledgePointDao: KnowledgePointDao,
    private val questionDao: QuestionDao,
    private val crossRefDao: QuestionKnowledgeCrossRefDao,
    private val clock: Clock,
) : EntryRepository {

    override suspend fun create(draft: EntryDraft): EntrySummary {
        val validated = validate(draft)

        return transactionRunner.inTransaction {
            val selected = resolveKnowledgePoints(validated.knowledgePointIds)
            val questionId = UUID.randomUUID().toString()
            val now = clock.instant()

            questionDao.insert(
                Question(
                    id = questionId,
                    pageId = "",
                    questionNumber = validated.questionNumber,
                    qrPayload = "",
                    archived = false,
                    lastScheduledDate = null,
                    lastReviewedDate = null,
                    estimatedReviewMinutes = validated.estimatedReviewMinutes,
                    createdAt = now,
                    archivedAt = null,
                    sourcePage = validated.sourcePageLabel,
                    sourcePageCode = validated.sourcePageCode,
                ),
            )
            insertLinks(questionId, selected.map { it.id })
            recomputeActivity(selected.map { it.id })

            summaryOf(questionId, validated, selected, printed = false)
        }
    }

    override suspend fun update(questionId: String, draft: EntryDraft): EntrySummary {
        val validated = validate(draft)

        return transactionRunner.inTransaction {
            val existing = questionDao.byId(questionId)
                ?: throw EntryQuestionNotFoundException("题目不存在或已被删除。")
            val selected = resolveKnowledgePoints(validated.knowledgePointIds)
            val previousLinkIds = crossRefDao.forQuestion(questionId)
                .map { it.knowledgePointId }

            questionDao.update(
                existing.copy(
                    questionNumber = validated.questionNumber,
                    estimatedReviewMinutes = validated.estimatedReviewMinutes,
                    sourcePage = validated.sourcePageLabel,
                    sourcePageCode = validated.sourcePageCode,
                ),
            )
            crossRefDao.deleteForQuestion(questionId)
            insertLinks(questionId, selected.map { it.id })
            recomputeActivity((previousLinkIds + selected.map { it.id }).distinct())

            summaryOf(questionId, validated, selected, printed = existing.pageId.isNotEmpty())
        }
    }

    override suspend fun archive(questionId: String) {
        transactionRunner.inTransaction {
            questionDao.byId(questionId)
                ?: throw EntryQuestionNotFoundException("题目不存在或已被删除。")
            val linkedIds = crossRefDao.forQuestion(questionId).map { it.knowledgePointId }
            questionDao.markArchived(questionId, clock.instant())
            recomputeActivity(linkedIds)
        }
    }

    override suspend fun summaries(): List<EntrySummary> {
        val knowledgePoints = knowledgePointDao.allOrderedByNumber().associateBy { it.id }
        val refsByQuestion = crossRefDao.all().groupBy { it.questionId }
        return questionDao.active().map { question ->
            val resolved = refsByQuestion[question.id].orEmpty()
                .mapNotNull { ref ->
                    knowledgePoints[ref.knowledgePointId]?.let { knowledgePoint -> knowledgePoint }
                }
                .sortedBy { it.number }
            EntrySummary(
                questionId = question.id,
                questionNumber = question.questionNumber,
                sourcePageCode = question.sourcePageCode,
                sourcePageLabel = question.sourcePage,
                knowledgeNames = resolved.map { it.name },
                knowledgePointIds = resolved.map { it.id },
                estimatedReviewMinutes = question.estimatedReviewMinutes,
                printed = question.pageId.isNotEmpty(),
            )
        }
    }

    override suspend fun defaultSourcePageCode(): Int =
        questionDao.latestSourcePageCode()?.coerceAtLeast(1) ?: 1

    /** 校验并归一化录入/编辑入参；越界关联与非法页码在这里抛出。 */
    private fun validate(draft: EntryDraft): ValidatedDraft {
        val questionNumber = draft.questionNumber.trim()
        require(questionNumber.isNotEmpty()) { "题号不能为空。" }
        require(draft.sourcePageCode >= 1) { "页码编号必须从 1 开始。" }
        val knowledgePointIds = draft.knowledgePointIds.distinct()
        if (knowledgePointIds.isEmpty()) {
            throw InvalidKnowledgePointLinkException("请至少选择一个知识点标签。")
        }
        if (knowledgePointIds.size > EntryDraft.MAX_KNOWLEDGE_POINTS) {
            throw InvalidKnowledgePointLinkException(
                "单题最多关联 ${EntryDraft.MAX_KNOWLEDGE_POINTS} 个知识点，请拆分为多道题目。",
            )
        }
        val minutes = draft.estimatedReviewMinutes
            .takeIf { it > 0 }
            ?: EntryDraft.DEFAULT_REVIEW_MINUTES
        return ValidatedDraft(
            questionNumber = questionNumber,
            sourcePageCode = draft.sourcePageCode,
            sourcePageLabel = draft.sourcePageLabel.trim(),
            knowledgePointIds = knowledgePointIds,
            estimatedReviewMinutes = minutes,
        )
    }

    private suspend fun resolveKnowledgePoints(
        knowledgePointIds: List<String>,
    ): List<KnowledgePoint> =
        knowledgePointIds.map { id ->
            knowledgePointDao.byId(id)
                ?: throw InvalidKnowledgePointLinkException("知识点不存在或已作废，请刷新标签库后重试。")
        }

    private suspend fun insertLinks(questionId: String, knowledgePointIds: List<String>) {
        knowledgePointIds.forEach { knowledgePointId ->
            crossRefDao.insert(
                QuestionKnowledgeCrossRef(
                    questionId = questionId,
                    knowledgePointId = knowledgePointId,
                ),
            )
        }
    }

    private fun summaryOf(
        questionId: String,
        validated: ValidatedDraft,
        selected: List<KnowledgePoint>,
        printed: Boolean,
    ): EntrySummary {
        val ordered = selected.sortedBy { it.number }
        return EntrySummary(
            questionId = questionId,
            questionNumber = validated.questionNumber,
            sourcePageCode = validated.sourcePageCode,
            sourcePageLabel = validated.sourcePageLabel,
            knowledgeNames = ordered.map { it.name },
            knowledgePointIds = ordered.map { it.id },
            estimatedReviewMinutes = validated.estimatedReviewMinutes,
            printed = printed,
        )
    }

    /**
     * 按未归档题目的实际关联数重算标签的活跃题目计数；有引用即取消休眠（D-11）。
     * 只统计未归档题目，避免归档题目把知识点留在可打印集合里。
     */
    private suspend fun recomputeActivity(knowledgePointIds: List<String>) {
        val activeQuestionIds = questionDao.active().mapTo(mutableSetOf()) { it.id }
        val activeRefs = crossRefDao.all().filter { it.questionId in activeQuestionIds }
        knowledgePointIds.forEach { knowledgePointId ->
            val count = activeRefs.count { it.knowledgePointId == knowledgePointId }
            knowledgePointDao.updateActivity(knowledgePointId, count, dormant = count == 0)
        }
    }

    private data class ValidatedDraft(
        val questionNumber: String,
        val sourcePageCode: Int,
        val sourcePageLabel: String,
        val knowledgePointIds: List<String>,
        val estimatedReviewMinutes: Int,
    )
}
