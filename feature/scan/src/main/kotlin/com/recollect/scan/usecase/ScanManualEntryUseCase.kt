package com.recollect.scan.usecase

import com.recollect.data.room.entity.DetectionStatus
import com.recollect.data.room.repository.PendingFeedbackInput
import com.recollect.data.room.repository.ScanSessionRepository
import com.recollect.scan.manual.ScanManualCodec
import java.time.Clock
import java.time.LocalDate

/**
 * 手动回退：`pageId + 题号` 定位一题（scan-pipeline.md「手动回退」、C-011 方案二）。
 *
 * 没有 QR 与图像，因此：
 * - 不校验版本/CRC（这两个只适用于 QR 载荷）；
 * - 归属按 `PlanPage` 快照校验（页面存在、题号属于该页、行快照非空）；
 * - 写入的逐行结果一律为 `UNREADABLE`、`detectedTier = null`，由确认页逐行人工修正/补填（R-04、D-13）。
 */
class ScanManualEntryUseCase(
    private val repository: ScanSessionRepository,
    /** 题号投影（`QuestionRepository.questionNumbers()`）；作为读端口注入，便于单元测试。 */
    private val questionNumbers: suspend () -> Map<String, String>,
    private val clock: Clock,
) {

    suspend fun submit(pageIdInput: String, questionNumberInput: String): ScanManualOutcome {
        val pageId = ScanManualCodec.normalizePageId(pageIdInput)
            ?: return ScanManualOutcome.Rejected(
                "页面 ID 格式错误：应为 6 位计划号 + “-” + 2 位页码，例如 K7M2Q9-01。",
            )
        val questionNumber = questionNumberInput.trim()
        if (questionNumber.isEmpty()) {
            return ScanManualOutcome.Rejected("请输入纸面题号，例如 T-1。")
        }

        val facts = repository.pageFacts(pageId)
            ?: return ScanManualOutcome.Rejected("页面或题号不存在，请核对页脚的页面 ID。")
        if (facts.knowledgeRows.isEmpty() || facts.items.isEmpty()) {
            return ScanManualOutcome.Rejected(
                "该页缺少打印快照，无法回读；请重新生成计划并按新稿打印。",
            )
        }

        val questionId = resolveQuestionId(facts.questionIds, questionNumber)
            ?: return ScanManualOutcome.Rejected("页面或题号不存在，请核对纸面题号。")
        val snapshot = facts.knowledgeRows.firstOrNull { it.questionId == questionId }
            ?: return ScanManualOutcome.Rejected("页面或题号不存在，请核对纸面题号。")
        if (snapshot.rows.isEmpty()) {
            return ScanManualOutcome.Rejected("该题缺少打印行快照；请重新生成计划并按新稿打印。")
        }

        val triggered = snapshot.triggeredKnowledgePointIds.toSet()
        val inputs = snapshot.rows.map { row ->
            PendingFeedbackInput(
                questionId = questionId,
                knowledgePointId = row.knowledgePointId,
                triggered = row.knowledgePointId in triggered,
                detectedTier = null,
                detectionStatus = DetectionStatus.UNREADABLE,
                conflict = false,
                detectedRawOcr = null,
            )
        }

        val scanDate = LocalDate.now(clock)
        val planEndDate = repository.planEndDate(facts.planId)
        val lateBatch = planEndDate != null && scanDate.isAfter(planEndDate.plusDays(LATE_BATCH_DAYS))
        val pageIsFuture = facts.date.isAfter(scanDate)

        val session = repository.startOrResumeSession(facts.planId, clock.instant())
        val replacedPage = repository.pendingRows(session.sessionId).any { it.pageId == pageId }
        val written = try {
            repository.appendPage(session.sessionId, pageId, inputs)
        } catch (e: IllegalArgumentException) {
            return ScanManualOutcome.Rejected(e.message ?: "页面或题号不存在。")
        } catch (e: IllegalStateException) {
            return ScanManualOutcome.Rejected(e.message ?: "扫描会话已结束，无法继续追加。")
        }
        repository.markReady(session.sessionId)

        return ScanManualOutcome.Stored(
            sessionId = session.sessionId,
            pageId = pageId,
            questionId = questionId,
            rowCount = written,
            lateBatch = lateBatch,
            pageIsFuture = pageIsFuture,
            pageDate = facts.date,
            replacedPage = replacedPage,
        )
    }

    /** 按纸面题号在该页的 `questionIds` 中匹配唯一题目；题号来自题目表投影。 */
    private suspend fun resolveQuestionId(questionIds: List<String>, questionNumber: String): String? {
        if (questionIds.isEmpty()) return null
        val numbers = questionNumbers()
        val matches = questionIds.filter { id -> numbers[id]?.trim() == questionNumber }
        return matches.singleOrNull()
    }

    private companion object {
        const val LATE_BATCH_DAYS = 7L
    }
}

sealed interface ScanManualOutcome {

    data class Stored(
        val sessionId: String,
        val pageId: String,
        val questionId: String,
        val rowCount: Int,
        val lateBatch: Boolean,
        val pageIsFuture: Boolean,
        val pageDate: LocalDate,
        val replacedPage: Boolean,
    ) : ScanManualOutcome

    /** 用户可读的阻断提示；不写 `PendingFeedback`、不进入确认页。 */
    data class Rejected(val message: String) : ScanManualOutcome
}
