package com.recollect.app

import android.content.Context
import com.recollect.app.onboarding.DemoDataStore
import com.recollect.common.CrockfordPlanIdGenerator
import com.recollect.data.room.RecollectDatabase
import com.recollect.data.room.export.DatabaseExporter
import com.recollect.data.room.export.DatabaseImporter
import com.recollect.data.room.export.RoomDatabaseExporter
import com.recollect.data.room.export.RoomDatabaseImporter
import com.recollect.data.room.repository.EntryRepository
import com.recollect.data.room.repository.FeedbackRepository
import com.recollect.data.room.repository.KnowledgeCategoryRepository
import com.recollect.data.room.repository.KnowledgePointLibraryRepository
import com.recollect.data.room.repository.KnowledgePointRepository
import com.recollect.data.room.repository.MondayPlanWeekStartProvider
import com.recollect.data.room.repository.NoteAttachmentRepository
import com.recollect.data.room.repository.PlanRepository
import com.recollect.data.room.repository.PrintSnapshotRecord
import com.recollect.data.room.repository.QuestionLookupRepository
import com.recollect.data.room.repository.QuestionRepository
import com.recollect.data.room.repository.ReviewFeedbackHistoryRepository
import com.recollect.data.room.repository.RoomEntryRepository
import com.recollect.data.room.repository.RoomFeedbackRepository
import com.recollect.data.room.repository.RoomKnowledgeCategoryRepository
import com.recollect.data.room.repository.RoomKnowledgePointLibraryRepository
import com.recollect.data.room.repository.RoomKnowledgePointRepository
import com.recollect.data.room.repository.RoomNoteAttachmentRepository
import com.recollect.data.room.repository.RoomPlanRepository
import com.recollect.data.room.repository.RoomScanSessionRepository
import com.recollect.data.room.repository.ScanSessionRepository
import com.recollect.data.room.repository.SourceTagLibraryRepository
import com.recollect.data.room.repository.RoomQuestionLookupRepository
import com.recollect.data.room.repository.RoomQuestionRepository
import com.recollect.data.room.repository.RoomReviewFeedbackHistoryRepository
import com.recollect.data.room.repository.RoomSourceTagLibraryRepository
import com.recollect.data.room.repository.RoomReviewProgressRepository
import com.recollect.data.room.repository.ReviewProgressRepository
import com.recollect.data.room.repository.RoomSettingsRepository
import com.recollect.data.room.repository.RoomTransactionRunner
import com.recollect.data.room.repository.SettingsRepository
import com.recollect.data.room.usecase.ApplyFeedbackUseCase
import com.recollect.data.room.usecase.MaimemoNextDueCalculator
import com.recollect.feature.backup.BackupFileReader
import com.recollect.feature.backup.BackupSharer
import com.recollect.feature.planning.DefaultPlanSession
import com.recollect.feature.planning.GeneratePlanUseCase
import com.recollect.feature.planning.LoadPlanUseCase
import com.recollect.feature.planning.PlanDocumentGateway
import com.recollect.feature.planning.PlanSession
import com.recollect.feature.print.AndroidKnowledgeTextMeasurer
import com.recollect.feature.print.PlanPdfSharer
import com.recollect.feature.print.PrintSnapshotWriteResult
import com.recollect.feature.print.PrintSnapshotWriter
import com.recollect.feature.print.PrintedQuestionSnapshot
import com.recollect.pagination.DefaultPagePlanner
import com.recollect.pagination.PagePlanner
import com.recollect.pagination.TemplateGeometry
import com.recollect.scan.OpenCvScanEngine
import com.recollect.scan.ScanEngine
import com.recollect.scan.io.AndroidScanImageLoader
import com.recollect.scan.io.ScanImageLoader
import com.recollect.scan.io.ScanPreviewStore
import com.recollect.scan.ocr.MlKitNoteOcr
import com.recollect.scan.ocr.NoteOcr
import com.recollect.scan.ui.ScanManualEntry
import com.recollect.scan.ui.ScanRunResult
import com.recollect.scan.ui.ScanRunner
import com.recollect.scan.usecase.ScanManualEntryUseCase
import com.recollect.scan.usecase.ScanPageOutcome
import com.recollect.scan.usecase.ScanPageUseCase
import com.recollect.scheduler.DefaultScheduler
import com.recollect.scheduler.Scheduler
import java.time.Clock
import kotlinx.coroutines.Dispatchers
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

    /** 计划页到打印页的内存交接；进程重建后需重新生成计划。 */
    val printPlanSession = PrintPlanSession()

    private val database by lazy { RecollectDatabase.build(applicationContext) }

    /** 应用版本名；导出备份只作诊断字段，「关于」页展示给用户。 */
    val appVersionName: String by lazy { resolveAppVersionName() }

    /** 导出数据库（backup.md 白名单）；版本号只作诊断，不参与兼容判断。 */
    val databaseExporter: DatabaseExporter by lazy {
        RoomDatabaseExporter(
            backupDao = database.backupDao(),
            appVersionName = appVersionName,
            clock = clock,
        )
    }

    /** 应用设置（每日复习时长）；持久化在 Room，随备份导入/导出。 */
    val settingsRepository: SettingsRepository by lazy {
        RoomSettingsRepository(database.appSettingsDao())
    }

    /** 导出后的系统分享；`FileProvider` 与 `Intent` 只停留在 app 层（R-03）。 */
    val backupSharer: BackupSharer by lazy { AndroidBackupSharer(applicationContext) }

    /** 周计划 PDF 的系统分享（微信、QQ 等）；与备份分享同一套 `FileProvider`（R-03）。 */
    val planPdfSharer: PlanPdfSharer by lazy { AndroidPlanPdfSharer(applicationContext) }

    /**
     * 「生成计划」页的打印稿预览与分享（2026-11-27 用户要求）。
     * 与分享 PDF 使用同一份 A4 布局；页面内实时渲染、唯一操作是分享。
     */
    val planDocumentGateway: PlanDocumentGateway by lazy {
        AndroidPlanDocumentGateway(
            context = applicationContext,
            pdfSharer = planPdfSharer,
            snapshotWriter = printSnapshotWriter,
        )
    }

    /** 覆盖式导入：解析、校验与单事务恢复都在 data:room，app 层只负责装配。 */
    val databaseImporter: DatabaseImporter by lazy {
        RoomDatabaseImporter(
            restoreDao = database.backupRestoreDao(),
            transactionRunner = RoomTransactionRunner(database),
            clock = clock,
        )
    }

    /** 读取 SAF / 分享意图传入的备份文件；`ContentResolver` 只停留在 app 层。 */
    val backupFileReader: BackupFileReader by lazy {
        AndroidBackupFileReader(applicationContext)
    }

    /**
     * 首次复习演示的数据管理（2026-11-26 用户要求）：
     * 演示前导出当前数据库快照、载入 assets 中的初中示例题库，结束后覆盖导入快照还原。
     * 复用现有备份导出/导入端口，不新增存储依赖。
     */
    internal val firstRunDemoStore: DemoDataStore by lazy {
        DemoDataStore(
            context = applicationContext,
            exporter = databaseExporter,
            importer = databaseImporter,
        )
    }

    private fun resolveAppVersionName(): String = try {
        @Suppress("DEPRECATION")
        applicationContext.packageManager
            .getPackageInfo(applicationContext.packageName, 0)
            .versionName
            ?: "unknown"
    } catch (e: Exception) {
        "unknown"
    }

    val knowledgePointRepository: KnowledgePointRepository by lazy {
        RoomKnowledgePointRepository(database.knowledgePointDao())
    }

    /** 知识点分类（知识点之上一层，可增删改，schema v13）。 */
    val knowledgeCategoryRepository: KnowledgeCategoryRepository by lazy {
        RoomKnowledgeCategoryRepository(
            transactionRunner = RoomTransactionRunner(database),
            categoryDao = database.knowledgeCategoryDao(),
            knowledgePointDao = database.knowledgePointDao(),
            clock = clock,
        )
    }

    /** 知识点标签库（增删改查、编号序列与墓碑，D-12）。 */
    val knowledgePointLibraryRepository: KnowledgePointLibraryRepository by lazy {
        RoomKnowledgePointLibraryRepository(
            transactionRunner = RoomTransactionRunner(database),
            knowledgePointDao = database.knowledgePointDao(),
            categoryDao = database.knowledgeCategoryDao(),
            crossRefDao = database.questionKnowledgeCrossRefDao(),
            questionDao = database.questionDao(),
            sequenceDao = database.knowledgeNumberSequenceDao(),
            tombstoneDao = database.knowledgePointTombstoneDao(),
            clock = clock,
        )
    }

    /** 来源标签库（录入页单选来源、标签库增删改查）。 */
    val sourceTagLibraryRepository: SourceTagLibraryRepository by lazy {
        RoomSourceTagLibraryRepository(
            transactionRunner = RoomTransactionRunner(database),
            sourceTagDao = database.sourceTagDao(),
            questionDao = database.questionDao(),
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

    /** 复习进度页的只读投影（2026-11-22 用户要求）；不参与调度计算。 */
    val reviewProgressRepository: ReviewProgressRepository by lazy {
        RoomReviewProgressRepository(
            questionDao = database.questionDao(),
            crossRefDao = database.questionKnowledgeCrossRefDao(),
            knowledgePointDao = database.knowledgePointDao(),
            categoryDao = database.knowledgeCategoryDao(),
        )
    }

    /** 打印文档写出成功后回写 `Question.pageId/qrPayload`（print-template.md）。 */
    val printSnapshotWriter: PrintSnapshotWriter = object : PrintSnapshotWriter {
        override suspend fun save(
            planId: String,
            snapshots: List<PrintedQuestionSnapshot>,
        ): PrintSnapshotWriteResult {
            val saved = planRepository.savePrintSnapshots(
                planId = planId,
                snapshots = snapshots.map { snapshot ->
                    PrintSnapshotRecord(
                        questionId = snapshot.questionId,
                        pageId = snapshot.pageId,
                        qrPayload = snapshot.qrPayload,
                    )
                },
                printedAt = clock.instant(),
            )
            return if (saved) {
                PrintSnapshotWriteResult.SAVED
            } else {
                PrintSnapshotWriteResult.IGNORED_NOT_ACTIVE
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
            planDao = database.planDao(),
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

    /** 扫描页缩略图缓存：相机原帧仍按规范删除，只保留小预览供缩略图与悬浮预览。 */
    private val scanPreviewStore: ScanPreviewStore by lazy {
        ScanPreviewStore(applicationContext)
    }

    /**
     * 扫描页只依赖这两个入口，不直接接触 `Bitmap`/`Uri`/OpenCV/ML Kit（R-03）。
     * 解码在 IO 线程、识别在 Default 线程完成，位图在返回前回收。
     */
    val scanRunner: ScanRunner by lazy {
        ScanRunner { imageLocation ->
            val bitmap = scanImageLoader.load(imageLocation)
                ?: return@ScanRunner ScanRunResult.ImageUnreadable
            val previewLocation = scanPreviewStore.save(bitmap)
            try {
                val outcome = withContext(Dispatchers.Default) {
                    scanPageUseCase.scan(bitmap, scanEngine, scanNoteOcr)
                }
                val stored = outcome is ScanPageOutcome.Stored
                if (!stored) scanPreviewStore.delete(previewLocation)
                ScanRunResult.Completed(
                    outcome = outcome,
                    previewLocation = previewLocation.takeIf { stored },
                )
            } catch (e: Exception) {
                // 识别抛错时不保留无主缩略图；异常继续交给页面显示。
                scanPreviewStore.delete(previewLocation)
                throw e
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

    /** 扫描页缩略图的清理入口（删除单页 / 从首页重新进入时丢弃旧会话）。 */
    val scanPreviewDeleter: suspend (String) -> Unit = { location ->
        scanPreviewStore.delete(location)
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
            planPageDao = database.planPageDao(),
            planDao = database.planDao(),
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

    private val loadPlanUseCase: LoadPlanUseCase by lazy {
        LoadPlanUseCase(
            planRepository = planRepository,
            // 旧计划的推迟快照缺来源页码时，重开预览从题库补齐展示。
            sourcePages = { questionRepository.sourcePages() },
        )
    }

    /** 本周计划会话：已有计划时复用同一 planId，显式重新生成才替换旧计划。 */
    val planSession: PlanSession by lazy {
        DefaultPlanSession(
            generator = generatePlanUseCase,
            planRepository = planRepository,
            loader = loadPlanUseCase,
        )
    }
}
