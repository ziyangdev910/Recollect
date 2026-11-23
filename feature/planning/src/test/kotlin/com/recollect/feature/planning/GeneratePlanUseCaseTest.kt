package com.recollect.feature.planning

import com.recollect.common.PlanIdGenerator
import com.recollect.data.room.repository.PlanGenerationException
import com.recollect.model.KnowledgeState
import com.recollect.model.SchedulerQuestion
import com.recollect.model.Tier
import com.recollect.pagination.DefaultPagePlanner
import com.recollect.pagination.PagePlanner
import com.recollect.pagination.PaginationFailureCode
import com.recollect.pagination.PaginationResult
import com.recollect.scheduler.DefaultScheduler
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 阶段 5.2 首个纵切：1 题 1 知识点 → 1 页 1 条（planning.md 固定流程）。
 */
class GeneratePlanUseCaseTest {

    private val zone = ZoneId.of("Asia/Shanghai")
    private val today = LocalDate.of(2026, 11, 18)
    private val clock = Clock.fixed(today.atStartOfDay(zone).toInstant(), zone)

    private fun knowledgeState(): KnowledgeState = KnowledgeState(
        id = "kp-1",
        number = 1,
        currentTier = Tier.UNKNOWN,
        continuousScore = 0.5f,
        nextDueDate = today.plusDays(1),
        effectiveFeedbackCount = 0.0f,
        dormant = false,
        activeQuestionCount = 1,
        difficulty = null,
        halfLifeDays = null,
        createdAt = Instant.parse("2026-11-18T00:00:00Z"),
    )

    private fun useCase(
        planRepository: FakePlanRepository = FakePlanRepository(),
        measurer: com.recollect.common.KnowledgeTextMeasurer = com.recollect.common.KnowledgeTextMeasurer { _, _, _ -> 1 },
        pagePlanner: PagePlanner = DefaultPagePlanner(),
        questions: List<SchedulerQuestion> = listOf(
            SchedulerQuestion(
                id = "q-1",
                linkedKnowledgeIds = listOf("kp-1"),
                archived = false,
                lastReviewedDate = null,
                estimatedReviewMinutes = 15,
            ),
        ),
        states: List<KnowledgeState> = listOf(knowledgeState()),
        catalog: List<com.recollect.data.room.repository.KnowledgePointRef> = listOf(
            com.recollect.data.room.repository.KnowledgePointRef(id = "kp-1", number = 1, name = "分数"),
        ),
        numbers: Map<String, String> = mapOf("q-1" to "T-1"),
        sources: Map<String, String> = mapOf("q-1" to "P9"),
    ): GeneratePlanUseCase = GeneratePlanUseCase(
        knowledgePointRepository = FakeKnowledgePointRepository(states = states, catalog = catalog),
        questionRepository = FakeQuestionRepository(
            schedulerQuestions = questions,
            numbers = numbers,
            sources = sources,
        ),
        planRepository = planRepository,
        noteAttachmentRepository = FakeNoteAttachmentRepository(),
        scheduler = DefaultScheduler(),
        pagePlanner = pagePlanner,
        textMeasurer = measurer,
        planIdGenerator = PlanIdGenerator { "ABC123" },
        clock = clock,
    )

    private val settings = PlanningSettings(
        planStartDate = today,
        planEndDate = today.plusDays(6),
    )

    @Test
    fun `one question and one knowledge point produce one page with one item`() = runBlocking {
        val planRepository = FakePlanRepository()

        val outcome = useCase(planRepository).execute(settings)

        val planned = outcome as? PlanGenerationOutcome.Planned
            ?: error("预期生成可打印计划，实际为 $outcome")
        assertEquals(1, planned.preview.pages.size, "应只有 1 页")
        assertEquals(1, planned.preview.pages.single().questionLabels.size, "该页应只有 1 条")
        assertEquals("P9-第T-1题", planned.preview.pages.single().questionLabels.single())
        assertEquals("ABC123-01", planned.printJob.pages.single().pageId)
        assertEquals(1, planned.printJob.questions.getValue("q-1").knowledgeCount)
        assertEquals(listOf("分数"), planned.printJob.questions.getValue("q-1").knowledgeNames)
        assertEquals("P9", planned.printJob.questions.getValue("q-1").sourcePage, "来源页码随打印投影传递")
        assertTrue(planRepository.savedDraft != null, "成功计划必须在同一事务入口保存")
    }

    @Test
    fun `pagination failure becomes PlanGenerationException before any write`() = runBlocking {
        val failingPlanner = object : PagePlanner {
            override fun plan(
                planId: String,
                dailyGroups: Map<LocalDate, List<com.recollect.pagination.RenderableQuestion>>,
                template: com.recollect.pagination.TemplateGeometry,
            ): PaginationResult = PaginationResult.Failure(
                code = PaginationFailureCode.INVALID_TEMPLATE_GEOMETRY,
                message = "模板几何非法。",
            )
        }
        val planRepository = FakePlanRepository()

        val error = assertThrows(PlanGenerationException::class.java) {
            runBlocking { useCase(planRepository, pagePlanner = failingPlanner).execute(settings) }
        }

        assertEquals(PaginationFailureCode.INVALID_TEMPLATE_GEOMETRY, error.code)
        assertEquals("模板几何非法。", error.message)
        assertNull(planRepository.savedDraft, "分页失败不得进入事务或写任何页")
    }

    @Test
    fun `empty plan yields a readable empty state and writes nothing`() = runBlocking {
        val planRepository = FakePlanRepository()

        val outcome = useCase(planRepository, questions = emptyList()).execute(settings)

        val empty = outcome as? PlanGenerationOutcome.Empty
            ?: error("预期空计划，实际为 $outcome")
        assertTrue(empty.message.contains("没有需要复习的题目"), "空态必须给用户可读提示")
        assertTrue(empty.warnings.isEmpty())
        assertNull(planRepository.savedDraft, "空计划不得写库")
    }

    @Test
    fun `all items overflowing keeps the question and reason in the empty state`() = runBlocking {
        val smallTemplate = com.recollect.pagination.TemplateGeometry(
            pageHeightMm = 50.0,
            topMarginMm = 10.0,
            bottomMarginMm = 10.0,
            headerRowHeightMm = 7.0,
        )
        val planRepository = FakePlanRepository()

        val outcome = useCase(planRepository).execute(settings.copy(template = smallTemplate))

        val empty = outcome as? PlanGenerationOutcome.Empty
            ?: error("全部 overflow 时不得生成空白页，实际为 $outcome")
        assertEquals(1, empty.warnings.size, "overflow 提示必须原样保留（C-061/C-062）")
        val warning = empty.warnings.single()
        assertEquals("T-1", warning.questionNumber, "提示必须能定位到题目")
        assertEquals("题目过高", warning.label)
        assertTrue(warning.message.contains("减少本题关联的知识点"))
        assertTrue(warning.message.contains("缩短知识点描述"))
        assertNull(planRepository.savedDraft, "无可打印题目时不得写库")
    }

    @Test
    fun `knowledge text overflow is shown as a readable warning without internal codes`() = runBlocking {
        val measurer = com.recollect.common.KnowledgeTextMeasurer { _, _, _ -> 5 }
        val planRepository = FakePlanRepository()

        val outcome = useCase(planRepository, measurer = measurer).execute(settings)

        val planned = outcome as? PlanGenerationOutcome.Planned
            ?: error("有可打印题目时应生成计划，实际为 $outcome")
        val warning = planned.preview.warnings.single()
        assertEquals("知识点文字过多", warning.label, "界面只展示中文标签（C-065 方案 B）")
        assertEquals("T-1", warning.questionNumber)
        assertTrue(warning.message.contains("不要拆分题目"), "文案按 C-066 引导减少关联/缩短描述")
        assertEquals(4, planned.printJob.questions.getValue("q-1").knowledgeLineCount, "传给分页器的行数封顶 4")
        assertEquals(1, planned.printJob.questions.getValue("q-1").knowledgeCount)
        assertEquals(1, planned.printJob.questions.getValue("q-1").knowledgeNames.size, "名称数量必须等于知识点数量（C-067）")
    }

    @Test
    fun `capacity overflow keeps the extra question as a deferred hint and freezes it`() = runBlocking {
        val states = listOf(
            knowledgeState(),
            knowledgeState().copy(id = "kp-2", number = 2),
        )
        val catalog = listOf(
            com.recollect.data.room.repository.KnowledgePointRef(id = "kp-1", number = 1, name = "分数"),
            com.recollect.data.room.repository.KnowledgePointRef(id = "kp-2", number = 2, name = "几何"),
        )
        val questions = listOf(
            SchedulerQuestion(
                id = "q-1",
                linkedKnowledgeIds = listOf("kp-1"),
                archived = false,
                lastReviewedDate = null,
                estimatedReviewMinutes = 40,
            ),
            SchedulerQuestion(
                id = "q-2",
                linkedKnowledgeIds = listOf("kp-2"),
                archived = false,
                lastReviewedDate = null,
                estimatedReviewMinutes = 40,
            ),
        )
        val planRepository = FakePlanRepository()

        val outcome = useCase(
            planRepository = planRepository,
            questions = questions,
            states = states,
            catalog = catalog,
            numbers = mapOf("q-1" to "T-1", "q-2" to "T-2"),
            sources = mapOf("q-1" to "P9", "q-2" to "P10"),
        ).execute(settings)

        val planned = outcome as? PlanGenerationOutcome.Planned
            ?: error("预期生成计划，实际为 $outcome")
        assertEquals(
            listOf("P9-第T-1题"),
            planned.preview.pages.flatMap { it.questionLabels },
            "60 分钟容量只排得下第一题",
        )
        val deferred = planned.preview.deferred.single()
        assertEquals("q-2", deferred.questionId)
        assertEquals("P10-第T-2题", deferred.questionLabel, "推迟题与页面卡片用同一题目标识")
        assertEquals("复习时间不足", deferred.label, "界面只用中文标签")
        assertTrue(deferred.message.contains("缩短预估复习时长"), "界面给出可执行的调整建议")
        assertEquals(
            listOf(
                com.recollect.data.room.repository.PlanDeferredRecord(
                    questionId = "q-2",
                    questionNumber = "T-2",
                    sourcePage = "P10",
                    reason = com.recollect.scheduler.DeferReason.TIME_OVERFLOW,
                ),
            ),
            planRepository.savedDraft?.deferredRecords,
            "推迟快照必须随计划落库，重开预览仍能看到",
        )
    }

    @Test
    fun `merge dedupes by question and code and sorts by question then code`() {
        val merged = mergePlanWarnings(
            paginationWarnings = listOf(
                com.recollect.pagination.PaginationWarning(
                    "q-2",
                    com.recollect.pagination.PaginationWarningCode.ATTACHMENT_OVER_LIMIT,
                    "第二题附加超限",
                ),
                com.recollect.pagination.PaginationWarning(
                    "q-1",
                    com.recollect.pagination.PaginationWarningCode.ITEM_TOO_TALL,
                    "第一题过高",
                ),
                com.recollect.pagination.PaginationWarning(
                    "q-1",
                    com.recollect.pagination.PaginationWarningCode.KNOWLEDGE_TEXT_OVERFLOW,
                    "重复警告",
                ),
            ),
            textWarnings = listOf(
                com.recollect.pagination.PaginationWarning(
                    "q-1",
                    com.recollect.pagination.PaginationWarningCode.KNOWLEDGE_TEXT_OVERFLOW,
                    "计划层文字溢出",
                ),
            ),
        )

        assertEquals(
            listOf(
                "q-1 ITEM_TOO_TALL",
                "q-1 KNOWLEDGE_TEXT_OVERFLOW",
                "q-2 ATTACHMENT_OVER_LIMIT",
            ),
            merged.map { "${it.questionId} ${it.code}" },
        )
        assertEquals("重复警告", merged[1].message, "同 questionId + code 只保留一条")
    }

    @Test
    fun `names match the printable knowledge count and follow the knowledge number order`() = runBlocking {
        val twoStates = listOf(
            knowledgeState(),
            knowledgeState().copy(id = "kp-2", number = 2),
        )
        val catalog = listOf(
            com.recollect.data.room.repository.KnowledgePointRef(id = "kp-2", number = 2, name = "几何"),
            com.recollect.data.room.repository.KnowledgePointRef(id = "kp-1", number = 1, name = "分数"),
        )
        val planRepository = FakePlanRepository()

        val outcome = useCase(
            planRepository = planRepository,
            questions = listOf(
                SchedulerQuestion(
                    id = "q-1",
                    linkedKnowledgeIds = listOf("kp-2", "kp-1"),
                    archived = false,
                    lastReviewedDate = null,
                    estimatedReviewMinutes = 15,
                ),
            ),
            states = twoStates,
            catalog = catalog,
        ).execute(settings)

        val planned = outcome as? PlanGenerationOutcome.Planned
            ?: error("预期生成计划，实际为 $outcome")
        val renderable = planned.printJob.questions.getValue("q-1")
        assertEquals(2, renderable.knowledgeCount)
        assertEquals(listOf("分数", "几何"), renderable.knowledgeNames, "名称按知识点编号升序（C-067）")
        assertEquals(
            listOf(1, 2),
            planRepository.savedDraft?.knowledgeRowsByQuestionId?.getValue("q-1")?.rows?.map { it.number },
            "左侧行快照与右上名称使用同一集合",
        )
    }
}
