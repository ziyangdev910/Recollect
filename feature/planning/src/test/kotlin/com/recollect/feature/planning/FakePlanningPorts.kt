package com.recollect.feature.planning

import com.recollect.data.room.entity.NoteAttachmentItem
import com.recollect.data.room.repository.KnowledgePointRef
import com.recollect.data.room.repository.KnowledgePointRepository
import com.recollect.data.room.repository.NoteAttachmentRepository
import com.recollect.data.room.repository.PendingCoolingOverride
import com.recollect.data.room.repository.PlanDraft
import com.recollect.data.room.repository.PlanRepository
import com.recollect.data.room.repository.QuestionRepository
import com.recollect.model.KnowledgeState
import com.recollect.model.SchedulerQuestion

/**
 * 计划生成端口的最小 fake：只模拟本轮用到的读/写，不伪造调度或分页结果。
 */
internal class FakeKnowledgePointRepository(
    private val states: List<KnowledgeState>,
    private val catalog: List<KnowledgePointRef>,
) : KnowledgePointRepository {
    override suspend fun states(): List<KnowledgeState> = states
    override suspend fun catalog(): List<KnowledgePointRef> = catalog
}

internal class FakeQuestionRepository(
    private val schedulerQuestions: List<SchedulerQuestion>,
    private val numbers: Map<String, String>,
    private val sources: Map<String, String> = emptyMap(),
) : QuestionRepository {
    val savedPrintSnapshots = mutableListOf<Triple<String, String, String>>()

    override suspend fun schedulerQuestions(): List<SchedulerQuestion> = schedulerQuestions
    override suspend fun questionNumbers(): Map<String, String> = numbers
    override suspend fun sourcePages(): Map<String, String> = sources
    override suspend fun savePrintSnapshot(questionId: String, pageId: String, qrPayload: String) {
        savedPrintSnapshots += Triple(questionId, pageId, qrPayload)
    }
}

internal class FakeNoteAttachmentRepository(
    private val itemsByQuestion: Map<String, List<NoteAttachmentItem>> = emptyMap(),
) : NoteAttachmentRepository {
    override suspend fun items(questionId: String, limit: Int): List<NoteAttachmentItem> =
        itemsByQuestion[questionId].orEmpty().take(limit)
}

internal class FakePlanRepository(
    private val pendingOverrides: List<PendingCoolingOverride> = emptyList(),
) : PlanRepository {
    var savedDraft: PlanDraft? = null
        private set

    override suspend fun pendingCoolingOverrides(): List<PendingCoolingOverride> = pendingOverrides

    override suspend fun savePlan(draft: PlanDraft) {
        savedDraft = draft
    }
}
