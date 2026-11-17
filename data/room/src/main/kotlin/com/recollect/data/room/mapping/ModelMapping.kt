package com.recollect.data.room.mapping

import com.recollect.data.room.entity.KnowledgePoint
import com.recollect.data.room.entity.PlanPage
import com.recollect.data.room.entity.Question
import com.recollect.data.room.entity.QuestionKnowledgeCrossRef
import com.recollect.model.KnowledgeRowsCodec
import com.recollect.model.KnowledgeState
import com.recollect.model.PlanPageRows
import com.recollect.model.SchedulerQuestion
import com.recollect.pagination.RenderableQuestion
import com.recollect.scheduler.PlannedQuestion

// Room Entity ↔ 纯 DTO 映射（C-046 方案 A）。core:model 保持无 Room/Android 依赖。

/**
 * 知识点实体 → 调度状态。
 *
 * `name` 等展示字段不进入调度 DTO。`continuousScore` 在实体中已是 `Float`；
 * 若上游用 `Double` 计算，写入 Room 前必须转 `Float`（C-050）。
 */
fun KnowledgePoint.toKnowledgeState(): KnowledgeState = KnowledgeState(
    id = id,
    number = number,
    currentTier = currentTier,
    continuousScore = continuousScore,
    nextDueDate = nextDueDate,
    effectiveFeedbackCount = effectiveFeedbackCount,
    dormant = dormant,
    activeQuestionCount = activeQuestionCount,
    difficulty = difficulty,
    halfLifeDays = halfLifeDays,
    createdAt = createdAt,
)

/**
 * 题目实体 → 调度题目；`linkedKnowledgeIds` 由调用方从 cross-ref 解析后传入（C-046）。
 *
 * 非正的 `estimatedReviewMinutes` 按 15 分钟处理（C-017）。
 */
fun Question.toSchedulerQuestion(linkedKnowledgeIds: List<String>): SchedulerQuestion =
    SchedulerQuestion(
        id = id,
        linkedKnowledgeIds = linkedKnowledgeIds,
        archived = archived,
        lastReviewedDate = lastReviewedDate,
        estimatedReviewMinutes = if (estimatedReviewMinutes <= 0) DEFAULT_REVIEW_MINUTES else estimatedReviewMinutes,
    )

/**
 * 题目实体 + cross-ref + 知识点 → 调度题目：只取本题关联，按 `KnowledgePoint.number` 升序。
 *
 * - 重复关联 id 直接失败，不静默去重/合并（C-055/C-056）；正常库表由复合主键保证唯一。
 * - 解析不到的知识点不补、不丢：保留原 id 并排在已解析 id 之后，交由调度器按
 *   `UNRESOLVED_KNOWLEDGE` 整题跳过并留下可见记录（C-055 方案 C）。
 */
fun Question.toSchedulerQuestion(
    crossRefs: List<QuestionKnowledgeCrossRef>,
    knowledgePoints: List<KnowledgePoint>,
): SchedulerQuestion {
    val linkedIds = crossRefs
        .filter { it.questionId == id }
        .map { it.knowledgePointId }
    require(linkedIds.size == linkedIds.distinct().size) {
        "Question $id 的 linkedKnowledgeIds 重复；data:room 必须保证唯一（C-056）"
    }

    val byId = knowledgePoints.associateBy { it.id }
    val resolved = linkedIds.mapNotNull { byId[it] }
        .sortedBy { it.number }
        .map { it.id }
    val unresolved = linkedIds.filterNot { byId.containsKey(it) }.sorted()

    return toSchedulerQuestion(resolved + unresolved)
}

/**
 * 调度计划中的题目 → 打印投影。
 *
 * `questionNumber` 由调用方从 `Question.questionNumber` 补齐（`PlannedQuestion` 没有该字段）。
 * `knowledgeNames` 必须与左侧填涂行属于同一 printable 集合、按 `number` 升序；
 * 其数量必须等于 [PlannedQuestion.triggeredKnowledgeIds] 与
 * [PlannedQuestion.nonTriggeredKnowledgeIds] 之和，即 `knowledgeCount`（C-067）。
 * 数量不符时直接失败，不静默补齐/截断。
 */
fun PlannedQuestion.toRenderableQuestion(
    questionNumber: String,
    knowledgeNames: List<String>,
    knowledgeLineCount: Int,
    attachmentCount: Int,
): RenderableQuestion {
    val printableKnowledgeCount = triggeredKnowledgeIds.size + nonTriggeredKnowledgeIds.size
    require(knowledgeNames.size == printableKnowledgeCount) {
        "Question $questionId 的 knowledgeNames 数量 ${knowledgeNames.size} != " +
            "printable 知识点数量 $printableKnowledgeCount；data:room 必须保证同一集合（C-067）"
    }

    return RenderableQuestion(
        questionId = questionId,
        questionNumber = questionNumber,
        knowledgeCount = knowledgeNames.size,
        knowledgeNames = knowledgeNames,
        knowledgeLineCount = knowledgeLineCount,
        attachmentCount = attachmentCount,
    )
}

/**
 * 打印页实体 → 每题一条的行快照记录。
 *
 * `knowledgeRows` 已由计划/打印层按打印顺序冻结；这里只解码，不按 `number` 重排（C-015/C-070）。
 *
 * 注：`docs/specs/repository.md` 把该映射写成 `PlannedPage.toPlanPageRows(): PlanPageRows`，
 * 但分页器的 `PlannedPage` 不含行快照且一页可能有多题；可映射的来源是 Room `PlanPage.knowledgeRows`。
 * 差异已记入 `docs/conflicts.md` C-071，等待裁决。
 */
fun PlanPage.toPlanPageRows(): List<PlanPageRows> = KnowledgeRowsCodec.decode(knowledgeRows)

private const val DEFAULT_REVIEW_MINUTES = 15
