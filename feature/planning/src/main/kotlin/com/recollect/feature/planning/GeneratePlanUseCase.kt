package com.recollect.feature.planning

import com.recollect.common.KnowledgeTextMeasurer
import com.recollect.common.PlanIdGenerator
import com.recollect.common.UNKNOWN_QUESTION_NUMBER
import com.recollect.common.formatQuestionLabel
import com.recollect.data.room.entity.NoteAttachmentItem
import com.recollect.data.room.mapping.toRenderableQuestion
import com.recollect.data.room.repository.KnowledgePointRef
import com.recollect.data.room.repository.KnowledgePointRepository
import com.recollect.data.room.repository.NoteAttachmentRepository
import com.recollect.data.room.repository.PlanDraft
import com.recollect.data.room.repository.PlanGenerationException
import com.recollect.data.room.repository.PlanRenderMeta
import com.recollect.data.room.repository.PlanRepository
import com.recollect.data.room.repository.QuestionRepository
import com.recollect.model.KnowledgeRow
import com.recollect.model.PlanPageRows
import com.recollect.pagination.PagePlanner
import com.recollect.pagination.PaginationFailureCode
import com.recollect.pagination.PaginationResult
import com.recollect.pagination.ScanGeometryProjection
import com.recollect.pagination.PaginationWarning
import com.recollect.pagination.PaginationWarningCode
import com.recollect.pagination.RenderableQuestion
import com.recollect.pagination.TemplateGeometry
import com.recollect.scheduler.PlannedQuestion
import com.recollect.scheduler.Scheduler
import java.time.Clock
import java.time.LocalDate

/**
 * 计划编排（planning.md「GeneratePlanUseCase」）。
 *
 * 只做编排与事务入口：调度、分页、文本测量分别由 `core:scheduler`、`core:pagination` 与打印层实现。
 *
 * C-047：`PaginationResult.Failure` 在进入事务前转 `PlanGenerationException`，不写任何页。
 * C-060：整份计划无可打印题目时返回 `PlanGenerationOutcome.Empty`，不产生空白页。
 * C-064：文本测量产生的 `KNOWLEDGE_TEXT_OVERFLOW` 与分页警告按 `questionId + code` 去重后合并。
 * C-067：`knowledgeNames` 与左侧行快照同一 printable 集合、按编号升序，数量等于知识点数量。
 */
/** 计划生成端口；`PlanPreview` 只依赖本接口，便于状态测试注入失败/空态。 */
interface PlanGenerator {
    suspend fun execute(settings: PlanningSettings): PlanGenerationOutcome

    /**
     * 显式重新生成：新计划保存时把 [previousPlanId] 标记为 SUPERSEDED。
     *
     * 默认实现与 [execute] 相同，仅用于测试桩；生产实现必须原子地替代旧计划。
     */
    suspend fun regenerate(
        settings: PlanningSettings,
        previousPlanId: String?,
    ): PlanGenerationOutcome = execute(settings)
}

class GeneratePlanUseCase(
    private val knowledgePointRepository: KnowledgePointRepository,
    private val questionRepository: QuestionRepository,
    private val planRepository: PlanRepository,
    private val noteAttachmentRepository: NoteAttachmentRepository,
    private val scheduler: Scheduler,
    private val pagePlanner: PagePlanner,
    private val textMeasurer: KnowledgeTextMeasurer,
    private val planIdGenerator: PlanIdGenerator,
    private val clock: Clock,
) : PlanGenerator {

    override suspend fun execute(settings: PlanningSettings): PlanGenerationOutcome =
        generate(settings, supersedesPlanId = null)

    override suspend fun regenerate(
        settings: PlanningSettings,
        previousPlanId: String?,
    ): PlanGenerationOutcome = generate(settings, supersedesPlanId = previousPlanId)

    private suspend fun generate(
        settings: PlanningSettings,
        supersedesPlanId: String?,
    ): PlanGenerationOutcome {
        require(!settings.planStartDate.isAfter(settings.planEndDate)) {
            "计划开始日 ${settings.planStartDate} 不能晚于结束日 ${settings.planEndDate}"
        }

        val states = knowledgePointRepository.states()
        val catalogById = knowledgePointRepository.catalog().associateBy { it.id }
        val questions = questionRepository.schedulerQuestions()
        val questionNumbers = questionRepository.questionNumbers()
        val sourcePages = questionRepository.sourcePages()
        val overrides = planRepository.pendingCoolingOverrides()

        val planResult = scheduler.plan(
            knowledgeStates = states,
            questions = questions,
            dailyAvailableMinutes = dailyMinutes(settings),
            dateRange = settings.planStartDate..settings.planEndDate,
            coolingPeriodDays = settings.coolingPeriodDays,
            coolingOverrides = overrides.map { it.questionId }.toSet(),
        )

        val planId = planIdGenerator.next()
        val renderables = linkedMapOf<String, RenderableQuestion>()
        val rowsByQuestionId = linkedMapOf<String, PlanPageRows>()
        val notesByQuestionId = linkedMapOf<String, List<NoteAttachmentItem>>()
        val textWarnings = mutableListOf<PaginationWarning>()

        planResult.dailyAssignments.values.forEach { plannedQuestions ->
            plannedQuestions.forEach { planned ->
                val knowledgePoints = knowledgePointsOf(planned, catalogById)
                val names = knowledgePoints.map { it.name }
                val measuredLines = textMeasurer.lineCount(
                    knowledgeNames = names,
                    widthMm = settings.template.rightTextWidthMm,
                    fontMm = KNOWLEDGE_NAME_FONT_MM,
                )
                if (measuredLines > MAX_KNOWLEDGE_LINES) {
                    textWarnings += PaginationWarning(
                        questionId = planned.questionId,
                        code = PaginationWarningCode.KNOWLEDGE_TEXT_OVERFLOW,
                        message = KNOWLEDGE_TEXT_OVERFLOW_MESSAGE,
                    )
                }

                val notes = noteAttachmentRepository.items(planned.questionId, MAX_ATTACHMENTS)
                renderables[planned.questionId] = planned.toRenderableQuestion(
                    questionNumber = questionNumbers[planned.questionId] ?: UNKNOWN_QUESTION_NUMBER,
                    knowledgeNames = names,
                    knowledgeLineCount = measuredLines.coerceIn(1, MAX_KNOWLEDGE_LINES),
                    attachmentCount = notes.size,
                    sourcePage = sourcePages[planned.questionId].orEmpty(),
                )
                rowsByQuestionId[planned.questionId] = PlanPageRows(
                    questionId = planned.questionId,
                    rows = knowledgePoints.map { KnowledgeRow(it.id, it.number) },
                    triggeredKnowledgePointIds = planned.triggeredKnowledgeIds,
                )
                notesByQuestionId[planned.questionId] = notes
            }
        }

        // 计划生成时冻结打印元数据：进程重建后重印同一计划时不再依赖当前题库/附件。
        val renderMetaByQuestionId = renderables.mapValues { (questionId, renderable) ->
            PlanRenderMeta(
                questionId = questionId,
                questionNumber = renderable.questionNumber,
                sourcePage = renderable.sourcePage,
                knowledgeNames = renderable.knowledgeNames,
                knowledgeLineCount = renderable.knowledgeLineCount,
                attachmentCount = renderable.attachmentCount,
                hasNoteArea = renderable.hasNoteArea,
                noteTexts = notesByQuestionId[questionId]?.map { it.text }.orEmpty(),
            )
        }

        // 分页器消费打印投影；日期顺序取自调度结果（LinkedHashMap，已按日期升序）。
        val renderableGroups = planResult.dailyAssignments.mapValues { (_, plannedQuestions) ->
            plannedQuestions.map { renderables.getValue(it.questionId) }
        }
        val pagination = when (val result = pagePlanner.plan(planId, renderableGroups, settings.template)) {
            is PaginationResult.Failure -> throw PlanGenerationException(result.code, result.message)
            is PaginationResult.Success -> result
        }
        val warnings = mergePlanWarnings(pagination.warnings, textWarnings)
        val previewWarnings = warnings.toPreviewWarnings(questionNumbers)
        // 推迟项与计划头一起冻结：下次“查看并打印本周计划”仍能说明哪些题没排入。
        val deferredPreview = planResult.deferred.map { it.toPreviewDeferred(questionNumbers, sourcePages) }
        val deferredRecords = planResult.deferred.map { it.toDeferredRecord(questionNumbers, sourcePages) }

        if (pagination.pages.isEmpty()) {
            // C-060：整份计划没有可打印题目 → 空态或提示，不生成空白页、不写库。
            return PlanGenerationOutcome.Empty(
                message = if (planResult.dailyAssignments.isEmpty()) {
                    EMPTY_PLAN_MESSAGE
                } else {
                    NO_PRINTABLE_ITEM_MESSAGE
                },
                warnings = previewWarnings,
                deferred = deferredPreview,
            )
        }

        val scheduledDates = pagination.pages.flatMap { page ->
            page.items.map { item -> item.questionId to page.date }
        }.toMap()
        val consumedOverrideIds = overrides
            .filter { it.questionId in scheduledDates }
            .map { it.id }
        // 回读几何随打印快照一起冻结（C-093 方案 A）。
        val geometryByPage = pagination.pages.associate { page ->
            page.pageId to page.items.map { item ->
                ScanGeometryProjection.of(
                    item = item,
                    question = renderables.getValue(item.questionId),
                    template = settings.template,
                )
            }
        }
        val draft = PlanDraft.fromPagination(
            planId = planId,
            result = pagination,
            knowledgeRowsByQuestionId = rowsByQuestionId.filterKeys { it in scheduledDates },
            scheduledDates = scheduledDates,
            coolingEventIdsToConsume = consumedOverrideIds,
            deferred = planResult.deferred,
            deferredRecords = deferredRecords,
            geometryByPage = geometryByPage,
            weekStartDate = settings.planStartDate,
            weekEndDate = settings.planEndDate,
            renderMetaByQuestionId = renderMetaByQuestionId,
            supersedesPlanId = supersedesPlanId,
        )
        planRepository.savePlan(draft)

        return PlanGenerationOutcome.Planned(
            printJob = PrintJob(
                planId = planId,
                pages = pagination.pages,
                questions = renderables.filterKeys { it in scheduledDates },
                notes = notesByQuestionId.filterKeys { it in scheduledDates },
                template = settings.template,
            ),
            preview = PlanPreviewContent(
                planId = planId,
                pages = pagination.pages.map { page ->
                    PlanPreviewPage(
                        pageNumber = page.pageNumber,
                        date = page.date,
                        questionLabels = page.items.map { item ->
                            formatQuestionLabel(
                                sourcePage = sourcePages[item.questionId].orEmpty(),
                                questionNumber = questionNumbers[item.questionId]
                                    ?: UNKNOWN_QUESTION_NUMBER,
                            )
                        },
                    )
                },
                warnings = previewWarnings,
                deferred = deferredPreview,
            ),
        )
    }

    /** C-067：左侧行与右上名称必须是同一 printable 集合，按编号升序。 */
    private fun knowledgePointsOf(
        planned: PlannedQuestion,
        catalogById: Map<String, KnowledgePointRef>,
    ): List<KnowledgePointRef> {
        val printableIds = (planned.triggeredKnowledgeIds + planned.nonTriggeredKnowledgeIds).distinct()
        return printableIds.map { id ->
            catalogById[id] ?: throw IllegalStateException(
                "计划引用的知识点 $id 不存在；data:room 必须在进入计划前保证关联可解析（C-056/C-067）",
            )
        }.sortedBy { it.number }
    }

    /** planning.md：`dailyAvailableMinutes` 缺失按 60 分钟/天；调度器对缺失日按 0 处理，不能漏传。 */
    private fun dailyMinutes(settings: PlanningSettings): Map<LocalDate, Int> {
        val days = generateSequence(settings.planStartDate) { it.plusDays(1) }
            .takeWhile { !it.isAfter(settings.planEndDate) }
        return days.associateWith { settings.dailyAvailableMinutes[it] ?: DEFAULT_DAILY_MINUTES }
    }

    private companion object {
        const val DEFAULT_DAILY_MINUTES = 60
        const val MAX_KNOWLEDGE_LINES = 4
        const val MAX_ATTACHMENTS = 3
        const val KNOWLEDGE_NAME_FONT_MM = 3.2
        const val EMPTY_PLAN_MESSAGE = "当前没有需要复习的题目；先录入题目和知识点，再来生成计划。"
        const val NO_PRINTABLE_ITEM_MESSAGE = "本计划没有可打印的题目；请按下方提示调整后重试。"
        const val KNOWLEDGE_TEXT_OVERFLOW_MESSAGE =
            "本题知识点名称超过 4 行，右上区域将截断；请减少本题关联的知识点，或缩短知识点描述（不要拆分题目）。"
    }
}

/**
 * C-064 方案 A：按 `questionId + code` 去重，按 `questionId` 字典序、同题按 `code` 枚举序升序。
 */
internal fun mergePlanWarnings(
    paginationWarnings: List<PaginationWarning>,
    textWarnings: List<PaginationWarning>,
): List<PaginationWarning> = (paginationWarnings + textWarnings)
    .distinctBy { it.questionId to it.code }
    .sortedWith(compareBy({ it.questionId }, { it.code.ordinal }))

/** C-065 方案 B：界面只拿中文标签与 message，不展示内部代码。 */
internal fun List<PaginationWarning>.toPreviewWarnings(
    questionNumbers: Map<String, String>,
): List<PlanPreviewWarning> = map { warning ->
    PlanPreviewWarning(
        questionId = warning.questionId,
        questionNumber = questionNumbers[warning.questionId] ?: "未知题号",
        label = warning.code.chineseLabel(),
        message = warning.message,
    )
}

internal fun PaginationWarningCode.chineseLabel(): String = when (this) {
    PaginationWarningCode.ITEM_TOO_TALL -> "题目过高"
    PaginationWarningCode.ATTACHMENT_OVER_LIMIT -> "附加笔记过多"
    PaginationWarningCode.KNOWLEDGE_TEXT_OVERFLOW -> "知识点文字过多"
}

/** C-047：把分页失败映射为可重试 UI 错误所需的稳定代码。 */
fun PaginationFailureCode.uiErrorCode(): String = "PLAN_$name"
