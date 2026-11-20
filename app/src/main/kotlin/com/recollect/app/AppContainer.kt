package com.recollect.app

import android.content.Context
import com.recollect.common.CrockfordPlanIdGenerator
import com.recollect.data.room.RecollectDatabase
import com.recollect.data.room.repository.EntryRepository
import com.recollect.data.room.repository.FeedbackRepository
import com.recollect.data.room.repository.KnowledgePointLibraryRepository
import com.recollect.data.room.repository.KnowledgePointRepository
import com.recollect.data.room.repository.MondayPlanWeekStartProvider
import com.recollect.data.room.repository.NoteAttachmentRepository
import com.recollect.data.room.repository.PlanRepository
import com.recollect.data.room.repository.QuestionLookupRepository
import com.recollect.data.room.repository.QuestionRepository
import com.recollect.data.room.repository.ReviewFeedbackHistoryRepository
import com.recollect.data.room.repository.RoomEntryRepository
import com.recollect.data.room.repository.RoomFeedbackRepository
import com.recollect.data.room.repository.RoomKnowledgePointLibraryRepository
import com.recollect.data.room.repository.RoomKnowledgePointRepository
import com.recollect.data.room.repository.RoomNoteAttachmentRepository
import com.recollect.data.room.repository.RoomPlanRepository
import com.recollect.data.room.repository.RoomScanSessionRepository
import com.recollect.data.room.repository.ScanSessionRepository
import com.recollect.data.room.repository.RoomQuestionLookupRepository
import com.recollect.data.room.repository.RoomQuestionRepository
import com.recollect.data.room.repository.RoomReviewFeedbackHistoryRepository
import com.recollect.data.room.repository.RoomTransactionRunner
import com.recollect.data.room.usecase.ApplyFeedbackUseCase
import com.recollect.data.room.usecase.MaimemoNextDueCalculator
import com.recollect.feature.planning.GeneratePlanUseCase
import com.recollect.feature.print.AndroidKnowledgeTextMeasurer
import com.recollect.feature.print.PrintSnapshotWriter
import com.recollect.feature.print.PrintedQuestionSnapshot
import com.recollect.pagination.DefaultPagePlanner
import com.recollect.pagination.PagePlanner
import com.recollect.pagination.TemplateGeometry
import com.recollect.scan.OpenCvScanEngine
import com.recollect.scan.ScanEngine
import com.recollect.scan.io.AndroidScanImageLoader
import com.recollect.scan.io.ScanImageLoader
import com.recollect.scan.ocr.MlKitNoteOcr
import com.recollect.scan.ocr.NoteOcr
import com.recollect.scan.ui.ScanManualEntry
import com.recollect.scan.ui.ScanRunResult
import com.recollect.scan.ui.ScanRunner
import com.recollect.scan.usecase.ScanManualEntryUseCase
import com.recollect.scan.usecase.ScanPageUseCase
import com.recollect.scheduler.DefaultScheduler
import com.recollect.scheduler.Scheduler
import java.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext

/**
 * 手写依赖容器（app.md「DI 与导航」）。
 *
 * 不引入 Hilt/Koin；Repository、UseCase、Scheduler、PagePlanner 都是进程级单例。
 * 调度入参/出参使用 `core:model` 的 DTO（C-053），这里不复制、不重定义任何 DTO。
 *
 * C-047：容器不捕获分页失败。`PaginationResult.Failure` 由 `data:room` 的
 * `PlanDraft.fromPagination` 转成 `PlanGenerationException`，App 层只负责映射为可重试 UI 错误。
 */
class AppContainer(context: Context) {

    private val applicationContext: Context = context.applicationContext

    /** 业务时钟；ViewModel 只用它取“今天”，不持有 Context。 */
    val clock: Clock = Clock.systemDefaultZone()

    /** 打印快照回写等短任务的进程级作用域。 */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** 计划页到打印页的内存交接；进程重建后需重新生成计划。 */
    val printPlanSession = PrintPlanSession()

    private val database by lazy { RecollectDatabase.build(applicationContext) }

    val knowledgePointRepository: KnowledgePointRepository by lazy {
        RoomKnowledgePointRepository(database.knowledgePointDao())
    }

    /** 知识点标签库（增删改查、编号序列与墓碑，D-12）。 */
    val knowledgePointLibraryRepository: KnowledgePointLibraryRepository by lazy {
        RoomKnowledgePointLibraryRepository(
            transactionRunner = RoomTransactionRunner(database),
            knowledgePointDao = database.knowledgePointDao(),
            crossRefDao = database.questionKnowledgeCrossRefDao(),
            questionDao = database.questionDao(),
            sequenceDao = database.knowledgeNumberSequenceDao(),
            tombstoneDao = database.knowledgePointTombstoneDao(),
            clock = clock,
        )
    }

    val reviewFeedbackHistoryRepository: ReviewFeedbackHistoryRepository by lazy {
        RoomReviewFeedbackHistoryRepository(database.reviewFeedbackDao())
    }

    val questionLookupRepository: QuestionLookupRepository by lazy {
        RoomQuestionLookupRepository(database.questionDao(), database.questionKnowledgeCrossRefDao())
    }

    val questionRepository: QuestionRepository by lazy {
        RoomQuestionRepository(
            questionDao = database.questionDao(),
            crossRefDao = database.questionKnowledgeCrossRefDao(),
            knowledgePointDao = database.knowledgePointDao(),
        )
    }

    /** 打印文档写出成功后回写 `Question.pageId/qrPayload`（print-template.md）。 */
    val printSnapshotWriter: PrintSnapshotWriter = object : PrintSnapshotWriter {
        override suspend fun save(snapshots: List<PrintedQuestionSnapshot>) {
            snapshots.forEach { snapshot ->
                questionRepository.savePrintSnapshot(
                    questionId = snapshot.questionId,
                    pageId = snapshot.pageId,
                    qrPayload = snapshot.qrPayload,
                )
            }
        }
    }

    val entryRepository: EntryRepository by lazy {
        RoomEntryRepository(
            transactionRunner = RoomTransactionRunner(database),
            knowledgePointDao = database.knowledgePointDao(),
            questionDao = database.questionDao(),
            crossRefDao = database.questionKnowledgeCrossRefDao(),
            clock = clock,
        )
    }

    val noteAttachmentRepository: NoteAttachmentRepository by lazy {
        RoomNoteAttachmentRepository(database.noteAttachmentItemDao())
    }

    val planRepository: PlanRepository by lazy {
        RoomPlanRepository(
            transactionRunner = RoomTransactionRunner(database),
            planPageDao = database.planPageDao(),
            questionDao = database.questionDao(),
            coolingOverrideEventDao = database.coolingOverrideEventDao(),
            knowledgePointDao = database.knowledgePointDao(),
            clock = clock,
        )
    }

    /** 回读会话与归属读取（C-092 方案 C；只写临时态，不写 ReviewFeedback）。 */
    val scanSessionRepository: ScanSessionRepository by lazy {
        RoomScanSessionRepository(
            transactionRunner = RoomTransactionRunner(database),
            planPageDao = database.planPageDao(),
            pendingFeedbackDao = database.pendingFeedbackDao(),
            scanSessionDao = database.scanSessionDao(),
        )
    }

    /** C-092 方案 C：自研回读引擎（OpenCV + ML Kit），只在扫描用例内部使用（R-03）。 */
    private val scanEngine: ScanEngine by lazy { OpenCvScanEngine(TemplateGeometry.a4Print()) }

    private val scanNoteOcr: NoteOcr by lazy { MlKitNoteOcr() }

    private val scanPageUseCase by lazy {
        ScanPageUseCase(repository = scanSessionRepository, clock = clock)
    }

    private val scanManualEntryUseCase by lazy {
        ScanManualEntryUseCase(
            repository = scanSessionRepository,
            questionNumbers = { questionRepository.questionNumbers() },
            clock = clock,
        )
    }

    private val scanImageLoader: ScanImageLoader by lazy {
        AndroidScanImageLoader(applicationContext)
    }

    /**
     * 扫描页只依赖这两个入口，不直接接触 `Bitmap`/`Uri`/OpenCV/ML Kit（R-03）。
     * 解码在 IO 线程、识别在 Default 线程完成，位图在返回前回收。
     */
    val scanRunner: ScanRunner by lazy {
        ScanRunner { imageLocation ->
            val bitmap = scanImageLoader.load(imageLocation)
                ?: return@ScanRunner ScanRunResult.ImageUnreadable
            try {
                ScanRunResult.Completed(
                    withContext(Dispatchers.Default) {
                        scanPageUseCase.scan(bitmap, scanEngine, scanNoteOcr)
                    },
                )
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
    }

    val scanManualEntry: ScanManualEntry by lazy {
        ScanManualEntry { pageId, questionNumber ->
            scanManualEntryUseCase.submit(pageId, questionNumber)
        }
    }

    val feedbackRepository: FeedbackRepository by lazy {
        RoomFeedbackRepository(
            transactionRunner = RoomTransactionRunner(database),
            reviewFeedbackDao = database.reviewFeedbackDao(),
            confirmationDecisionLogDao = database.confirmationDecisionLogDao(),
            knowledgePointDao = database.knowledgePointDao(),
            questionDao = database.questionDao(),
            pendingFeedbackDao = database.pendingFeedbackDao(),
            noteAttachmentItemDao = database.noteAttachmentItemDao(),
            scanSessionDao = database.scanSessionDao(),
            clock = clock,
        )
    }

    val applyFeedbackUseCase: ApplyFeedbackUseCase by lazy {
        ApplyFeedbackUseCase(
            knowledgePointRepository = knowledgePointRepository,
            reviewFeedbackHistoryRepository = reviewFeedbackHistoryRepository,
            feedbackRepository = feedbackRepository,
            nextDueCalculator = MaimemoNextDueCalculator(),
            questionLookupRepository = questionLookupRepository,
            planWeekStartProvider = MondayPlanWeekStartProvider(),
            clock = clock,
        )
    }

    val scheduler: Scheduler = DefaultScheduler()

    val pagePlanner: PagePlanner = DefaultPagePlanner()

    /** 首个纵切的平台文本测量实现；接口定义在 `core:common`（print-template.md）。 */
    val knowledgeTextMeasurer = AndroidKnowledgeTextMeasurer(applicationContext)

    val generatePlanUseCase: GeneratePlanUseCase by lazy {
        GeneratePlanUseCase(
            knowledgePointRepository = knowledgePointRepository,
            questionRepository = questionRepository,
            planRepository = planRepository,
            noteAttachmentRepository = noteAttachmentRepository,
            scheduler = scheduler,
            pagePlanner = pagePlanner,
            textMeasurer = knowledgeTextMeasurer,
            planIdGenerator = CrockfordPlanIdGenerator(),
            clock = clock,
        )
    }
}
