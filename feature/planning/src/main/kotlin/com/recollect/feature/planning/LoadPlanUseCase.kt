package com.recollect.feature.planning

import com.recollect.data.room.entity.NoteAttachmentItem
import com.recollect.data.room.repository.PlanRepository
import com.recollect.data.room.repository.PlanStatus
import com.recollect.pagination.PlacedItem
import com.recollect.pagination.PlannedPage
import com.recollect.pagination.RenderableQuestion
import com.recollect.pagination.TemplateGeometry
import java.time.Instant

/**
 * 从已持久化的计划重建打印投影（本周计划复用）。
 *
 * 只读取生成时冻结的 `plan.renderMeta`、`plan_page` 与题块几何，不重新调度、不重新分页。
 * 这样“查看本周计划”与最初生成时得到同一计划号、同一布局，也不会覆盖题目打印快照。
 */
class LoadPlanUseCase(
    private val planRepository: PlanRepository,
) {

    suspend fun load(planId: String, template: TemplateGeometry): PlanGenerationOutcome.Planned {
        val plan = planRepository.findPlan(planId)
            ?: throw IllegalStateException("计划不存在：$planId")
        require(plan.status != PlanStatus.SUPERSEDED) { "计划已被重新生成替代：$planId" }

        val pages = planRepository.storedPages(planId)
        require(pages.isNotEmpty()) { "计划没有可打印页面：$planId" }

        val metaById = plan.renderMeta.associateBy { it.questionId }
        val questionIds = pages.flatMap { page -> page.questionIds }.distinct()
        require(questionIds.all { it in metaById }) {
            "计划缺少打印元数据：${questionIds.filterNot { it in metaById }}"
        }

        val questions = questionIds.associateWith { questionId ->
            val meta = metaById.getValue(questionId)
            RenderableQuestion(
                questionId = questionId,
                questionNumber = meta.questionNumber,
                knowledgeCount = meta.knowledgeNames.size,
                knowledgeNames = meta.knowledgeNames,
                knowledgeLineCount = meta.knowledgeLineCount,
                attachmentCount = meta.attachmentCount,
                hasNoteArea = meta.hasNoteArea,
                sourcePage = meta.sourcePage,
            )
        }
        val notes = questionIds.associateWith { questionId ->
            metaById.getValue(questionId).noteTexts.mapIndexed { index, text ->
                NoteAttachmentItem(
                    questionId = questionId,
                    sourceFeedbackId = null,
                    text = text,
                    imageUri = null,
                    order = index,
                    createdAt = Instant.EPOCH,
                )
            }
        }.filterValues { it.isNotEmpty() }

        val plannedPages = pages.map { page ->
            val geometryById = page.geometry.associateBy { it.questionId }
            val items = page.questionIds.map { questionId ->
                val geometry = geometryById[questionId]
                    ?: throw IllegalStateException("计划页 ${page.pageId} 缺少题块几何：$questionId")
                PlacedItem(
                    questionId = questionId,
                    yOffsetMm = geometry.topMm,
                    heightMm = geometry.heightMm,
                )
            }
            PlannedPage(
                pageId = page.pageId,
                pageNumber = page.pageNumber,
                date = page.date,
                items = items,
                // C-063 起纸面不绘制续下页；重建时不需要分页内部标记。
                continuationMarkers = emptyList(),
            )
        }

        return PlanGenerationOutcome.Planned(
            printJob = PrintJob(
                planId = plan.planId,
                pages = plannedPages,
                questions = questions,
                notes = notes,
                template = template,
            ),
            preview = PlanPreviewContent(
                planId = plan.planId,
                pages = pages.map { page ->
                    PlanPreviewPage(
                        pageNumber = page.pageNumber,
                        date = page.date,
                        questionNumbers = page.questionIds.map { questionId ->
                            questions.getValue(questionId).questionNumber
                        },
                    )
                },
                warnings = emptyList(),
            ),
        )
    }
}
