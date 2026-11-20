package com.recollect.scan.usecase

import android.graphics.Bitmap
import com.recollect.data.room.entity.DetectionStatus as EntityDetectionStatus
import com.recollect.data.room.repository.PendingFeedbackInput
import com.recollect.data.room.repository.PlanPageFacts
import com.recollect.data.room.repository.ScanSessionRepository
import com.recollect.pagination.ScanItemGeometry
import com.recollect.pagination.TemplateGeometry
import com.recollect.scan.ItemBlockGeometry
import com.recollect.scan.OmrRow
import com.recollect.scan.QrResult
import com.recollect.scan.ScanEngine
import com.recollect.scan.ScanErrorCode
import com.recollect.scan.ScanException
import com.recollect.scan.ocr.NoteAreaGeometry
import com.recollect.scan.ocr.NoteOcr
import com.recollect.scan.omr.DetectionStatus
import com.recollect.scan.ownership.OwnershipResult
import com.recollect.scan.ownership.PlanPageLookup
import com.recollect.scan.ownership.ScanOwnershipValidator
import java.time.Clock
import java.time.LocalDate

/**
 * 回读一页的编排（scan-pipeline.md、R-04）：引擎 → 归属校验 → 写 `PendingFeedback`。
 *
 * 题块与笔记区几何来自 `PlanPage` 的冻结快照（C-093 方案 A），不再由调用方传入；
 * 没有几何快照的旧稿按「不可回读」处理（同 C-072 的口径）。
 * 任何校验失败都在写库前返回 [ScanPageOutcome.Rejected]，不创建 `PendingFeedback`、不改知识点状态。
 */
class ScanPageUseCase(
    private val repository: ScanSessionRepository,
    private val clock: Clock,
    private val template: TemplateGeometry = TemplateGeometry.a4Print(),
) {

    /**
     * 设备路径：跑完图像管线后写库。
     *
     * 扫描页不需要外部传入计划号：`pageId` 全局唯一且内嵌 planId（C-014），
     * 归属以 `PlanPage` 快照为准（scan-pipeline.md「归属校验」第 2 项）。
     */
    suspend fun scan(
        bitmap: Bitmap,
        engine: ScanEngine,
        noteOcr: NoteOcr,
    ): ScanPageOutcome = scan(bitmap, engine, noteOcr, expectedPlanId = null)

    /** 设备路径（显式计划号）：供已有调用方/证据用例保留计划一致性校验。 */
    suspend fun scan(
        planId: String,
        bitmap: Bitmap,
        engine: ScanEngine,
        noteOcr: NoteOcr,
    ): ScanPageOutcome = scan(bitmap, engine, noteOcr, expectedPlanId = planId)

    private suspend fun scan(
        bitmap: Bitmap,
        engine: ScanEngine,
        noteOcr: NoteOcr,
        expectedPlanId: String?,
    ): ScanPageOutcome {
        // 暂以「下一次扫描」作为清理触发点；App 启动钩子待接入。
        repository.abandonStaleSessions(clock.instant())

        val page = try {
            engine.detectPage(bitmap)
        } catch (e: ScanException) {
            return ScanPageOutcome.Rejected(e.code)
        }
        val qrs = try {
            engine.decodeQrs(page.corrected, page)
        } catch (e: ScanException) {
            return ScanPageOutcome.Rejected(e.code)
        }
        val pageId = singlePageId(qrs) ?: return ScanPageOutcome.Rejected(ScanErrorCode.QR_INVALID)
        val facts = repository.pageFacts(pageId)
            ?: return ScanPageOutcome.Rejected(ScanErrorCode.OWNERSHIP_MISMATCH)
        val blocks = blocksOf(facts)
            ?: return ScanPageOutcome.Rejected(ScanErrorCode.OWNERSHIP_MISMATCH)

        val omrRows = try {
            engine.readOmr(page.corrected, page, blocks)
        } catch (e: ScanException) {
            return ScanPageOutcome.Rejected(e.code)
        }
        val ocr = runCatching { noteOcr.recognize(page.corrected, page, noteAreasOf(facts)) }
            .getOrDefault(emptyMap())

        return storeWithFacts(expectedPlanId ?: facts.planId, facts, qrs, omrRows, ocr)
    }

    /** 校验并写库；纯逻辑，可在 JVM 上直接测试。 */
    suspend fun store(
        planId: String,
        qrs: List<QrResult>,
        omrRows: List<OmrRow>,
        ocr: Map<String, String>,
    ): ScanPageOutcome {
        val pageId = singlePageId(qrs) ?: return ScanPageOutcome.Rejected(ScanErrorCode.QR_INVALID)
        val facts = repository.pageFacts(pageId)
            ?: return ScanPageOutcome.Rejected(ScanErrorCode.OWNERSHIP_MISMATCH)
        return storeWithFacts(planId, facts, qrs, omrRows, ocr)
    }

    private suspend fun storeWithFacts(
        planId: String,
        facts: PlanPageFacts,
        qrs: List<QrResult>,
        omrRows: List<OmrRow>,
        ocr: Map<String, String>,
    ): ScanPageOutcome {
        if (facts.planId != planId) {
            return ScanPageOutcome.Rejected(ScanErrorCode.OWNERSHIP_MISMATCH)
        }
        val blocks = blocksOf(facts)
            ?: return ScanPageOutcome.Rejected(ScanErrorCode.OWNERSHIP_MISMATCH)

        val lookup = object : PlanPageLookup {
            override fun findPage(pageId: String): PlanPageFacts? =
                if (pageId == facts.pageId) facts else null
        }
        qrs.forEach { qr ->
            when (val result = ScanOwnershipValidator.validate(qr, lookup)) {
                is OwnershipResult.Invalid -> return ScanPageOutcome.Rejected(result.code)
                is OwnershipResult.Valid -> Unit
            }
        }

        val bound = try {
            bindQrs(qrs, blocks, facts)
        } catch (e: ScanException) {
            return ScanPageOutcome.Rejected(e.code)
        }

        val inputs = buildInputs(blocks, facts, bound, omrRows, ocr)
            ?: return ScanPageOutcome.Rejected(ScanErrorCode.OWNERSHIP_MISMATCH)

        val scanDate = LocalDate.now(clock)
        val planEndDate = repository.planEndDate(planId)
        val lateBatch = planEndDate != null && scanDate.isAfter(planEndDate.plusDays(LATE_BATCH_DAYS))
        val pageIsFuture = facts.date.isAfter(scanDate)

        // 校验全部通过后才建会话与写库；失败路径零写入（R-04）。
        val session = repository.startOrResumeSession(planId, clock.instant())
        // 同一页重复扫描按行去重保留最新结果，页面提示用户已更新（scan-pipeline.md）。
        val replacedPage = repository.pendingRows(session.sessionId).any { it.pageId == facts.pageId }
        val written = repository.appendPage(session.sessionId, facts.pageId, inputs)
        repository.markReady(session.sessionId)
        return ScanPageOutcome.Stored(
            sessionId = session.sessionId,
            pageId = facts.pageId,
            rowCount = written,
            lateBatch = lateBatch,
            pageIsFuture = pageIsFuture,
            pageDate = facts.date,
            replacedPage = replacedPage,
        )
    }

    /** 一页扫描只能对应一个 pageId；缺失或不唯一都按无效载荷处理。 */
    private fun singlePageId(qrs: List<QrResult>): String? {
        val ids = qrs.mapNotNull { it.pageId }.distinct()
        return ids.singleOrNull()
    }

    private fun blocksOf(facts: PlanPageFacts): List<ItemBlockGeometry>? {
        if (facts.items.isEmpty()) return null
        return facts.items.map { item ->
            ItemBlockGeometry(
                questionId = item.questionId,
                topMm = item.topMm,
                heightMm = item.heightMm,
                rowCount = item.rowCount,
            )
        }
    }

    private fun noteAreasOf(facts: PlanPageFacts): List<NoteAreaGeometry> =
        facts.items.mapNotNull { it.toNoteArea() }

    private fun ScanItemGeometry.toNoteArea(): NoteAreaGeometry? {
        val note = noteArea ?: return null
        return NoteAreaGeometry(
            questionId = questionId,
            leftMm = note.leftMm,
            topMm = note.topMm,
            widthMm = note.widthMm,
            heightMm = note.heightMm,
        )
    }

    /** 每个题块必须恰好对应一个 QR，且 QR 中心落在题块（含页脚容差）内。 */
    private fun bindQrs(
        qrs: List<QrResult>,
        blocks: List<ItemBlockGeometry>,
        facts: PlanPageFacts,
    ): Map<String, QrResult> {
        val pairs = qrs.map { qr ->
            val questionId = qr.questionId ?: throw ScanException(ScanErrorCode.QR_INVALID)
            questionId to qr
        }
        if (pairs.map { it.first }.toSet().size != pairs.size) {
            throw ScanException(ScanErrorCode.QR_MULTIPLE)
        }

        val byQuestion = pairs.toMap()
        val blockIds = blocks.map { it.questionId }.toSet()
        if (byQuestion.keys != blockIds) throw ScanException(ScanErrorCode.QR_MULTIPLE)
        if (!blockIds.all { it in facts.questionIds }) {
            throw ScanException(ScanErrorCode.OWNERSHIP_MISMATCH)
        }

        val halfFooter = template.qrFooterHeightMm / 2
        byQuestion.forEach { (questionId, qr) ->
            val block = blocks.first { it.questionId == questionId }
            val centerMm = qr.centerY / PIXELS_PER_MM_300_DPI
            val inside = centerMm >= block.topMm - halfFooter &&
                centerMm <= block.topMm + block.heightMm + halfFooter
            if (!inside) throw ScanException(ScanErrorCode.QR_MULTIPLE)
        }
        return byQuestion
    }

    private fun buildInputs(
        blocks: List<ItemBlockGeometry>,
        facts: PlanPageFacts,
        bound: Map<String, QrResult>,
        omrRows: List<OmrRow>,
        ocr: Map<String, String>,
    ): List<PendingFeedbackInput>? {
        val inputs = mutableListOf<PendingFeedbackInput>()
        blocks.forEach { block ->
            if (block.questionId !in bound) return null
            val snapshot = facts.knowledgeRows.firstOrNull { it.questionId == block.questionId }
                ?: return null
            val rows = omrRows.filter { it.questionId == block.questionId }
            if (rows.size != snapshot.rows.size) return null

            val triggeredIds = snapshot.triggeredKnowledgePointIds.toSet()
            val rawOcr = ocr[block.questionId]
            snapshot.rows.forEachIndexed { index, knowledgeRow ->
                val row = rows.firstOrNull { it.rowIndex == index }
                inputs += PendingFeedbackInput(
                    questionId = block.questionId,
                    knowledgePointId = knowledgeRow.knowledgePointId,
                    triggered = knowledgeRow.knowledgePointId in triggeredIds,
                    detectedTier = row?.detectedTier,
                    detectionStatus = row?.status?.toEntity() ?: EntityDetectionStatus.UNREADABLE,
                    conflict = row?.status == DetectionStatus.CONFLICT,
                    detectedRawOcr = rawOcr,
                )
            }
        }
        return inputs
    }

    private fun DetectionStatus.toEntity(): EntityDetectionStatus = when (this) {
        DetectionStatus.OK -> EntityDetectionStatus.OK
        DetectionStatus.NO_MARK -> EntityDetectionStatus.NO_MARK
        DetectionStatus.CONFLICT -> EntityDetectionStatus.CONFLICT
        DetectionStatus.UNREADABLE -> EntityDetectionStatus.UNREADABLE
    }

    private companion object {
        const val PIXELS_PER_MM_300_DPI = 300.0 / 25.4
        const val LATE_BATCH_DAYS = 7L
    }
}

sealed interface ScanPageOutcome {
    data class Stored(
        val sessionId: String,
        val pageId: String,
        val rowCount: Int,
        /** 扫描日晚于 `planEndDate + 7`（C-041：正常窗口为 `planEnd+1..+7`）。 */
        val lateBatch: Boolean,
        /** 计划日晚于扫描日：该页只能标 `NOT_REVIEWED`，不得写非 `NOT_REVIEWED` 反馈。 */
        val pageIsFuture: Boolean,
        val pageDate: LocalDate,
        /** 该页此前已在同一会话中扫描过，本次是覆盖更新（scan-pipeline.md「重复扫描」）。 */
        val replacedPage: Boolean = false,
    ) : ScanPageOutcome

    data class Rejected(val code: ScanErrorCode) : ScanPageOutcome
}
