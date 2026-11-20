package com.recollect.scan.ownership

import com.recollect.data.room.repository.PlanPageFacts
import com.recollect.scan.QrResult
import com.recollect.scan.ScanErrorCode

/** 读端口：由 `data:room` 实现；本层不直接查库。 */
interface PlanPageLookup {
    fun findPage(pageId: String): PlanPageFacts?
}

sealed interface OwnershipResult {
    /** 校验通过；`rowCount` 用于 OMR 行组绑定。 */
    data class Valid(val questionId: String, val rowCount: Int) : OwnershipResult

    data class Invalid(val code: ScanErrorCode) : OwnershipResult
}

/**
 * 归属校验：版本/CRC 由载荷解码负责，这里检查页面存在、题号归属与行快照。
 * 任一项失败都不创建 `PendingFeedback`、不进入确认页。
 */
object ScanOwnershipValidator {

    fun validate(qr: QrResult, lookup: PlanPageLookup): OwnershipResult {
        if (!qr.checksumOk) return OwnershipResult.Invalid(ScanErrorCode.QR_INVALID)

        val planId = qr.planId ?: return OwnershipResult.Invalid(ScanErrorCode.QR_INVALID)
        val pageId = qr.pageId ?: return OwnershipResult.Invalid(ScanErrorCode.QR_INVALID)
        val questionId = qr.questionId ?: return OwnershipResult.Invalid(ScanErrorCode.QR_INVALID)

        val facts = lookup.findPage(pageId)
            ?: return OwnershipResult.Invalid(ScanErrorCode.OWNERSHIP_MISMATCH)

        if (facts.planId != planId || facts.pageId != pageId) {
            return OwnershipResult.Invalid(ScanErrorCode.OWNERSHIP_MISMATCH)
        }
        if (questionId !in facts.questionIds) {
            return OwnershipResult.Invalid(ScanErrorCode.OWNERSHIP_MISMATCH)
        }

        val rows = facts.knowledgeRows.firstOrNull { it.questionId == questionId }?.rows
            ?: return OwnershipResult.Invalid(ScanErrorCode.OWNERSHIP_MISMATCH)
        if (rows.isEmpty()) {
            return OwnershipResult.Invalid(ScanErrorCode.OWNERSHIP_MISMATCH)
        }

        return OwnershipResult.Valid(questionId = questionId, rowCount = rows.size)
    }
}
