package com.recollect.scan.ownership

import com.recollect.data.room.repository.PlanPageFacts
import com.recollect.scan.QrResult
import com.recollect.scan.ScanErrorCode
import com.recollect.model.KnowledgeRow
import com.recollect.model.PlanPageRows
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * 归属校验（docs/specs/scan-pipeline.md「归属校验（C-011 方案二）」）：
 * QR 解码成功后、创建 PendingFeedback 前逐项校验；任一项失败不进入确认页。
 */
class ScanOwnershipValidatorTest {

    @Test
    fun `accepts a payload that belongs to the stored page`() {
        val facts = page()

        assertEquals(
            OwnershipResult.Valid(questionId = "q1", rowCount = 2),
            ScanOwnershipValidator.validate(qr(questionId = "q1"), lookupOf(facts)),
        )
    }

    @Test
    fun `rejects a page id that is not stored`() {
        assertEquals(
            OwnershipResult.Invalid(ScanErrorCode.OWNERSHIP_MISMATCH),
            ScanOwnershipValidator.validate(qr(questionId = "q1"), lookupOf(null)),
        )
    }

    @Test
    fun `rejects a question that is not printed on the page`() {
        assertEquals(
            OwnershipResult.Invalid(ScanErrorCode.OWNERSHIP_MISMATCH),
            ScanOwnershipValidator.validate(qr(questionId = "q9"), lookupOf(page())),
        )
    }

    @Test
    fun `rejects a page whose plan id does not match the payload`() {
        val facts = page(planId = "ABCDEF")

        assertEquals(
            OwnershipResult.Invalid(ScanErrorCode.OWNERSHIP_MISMATCH),
            ScanOwnershipValidator.validate(qr(questionId = "q1"), lookupOf(facts)),
        )
    }

    @Test
    fun `rejects a page without a knowledge row snapshot`() {
        val facts = page(knowledgeRows = emptyList())

        assertEquals(
            OwnershipResult.Invalid(ScanErrorCode.OWNERSHIP_MISMATCH),
            ScanOwnershipValidator.validate(qr(questionId = "q1"), lookupOf(facts)),
        )
    }

    @Test
    fun `rejects a payload whose checksum failed`() {
        assertEquals(
            OwnershipResult.Invalid(ScanErrorCode.QR_INVALID),
            ScanOwnershipValidator.validate(qr(questionId = "q1", checksumOk = false), lookupOf(page())),
        )
    }

    private fun qr(questionId: String, checksumOk: Boolean = true) = QrResult(
        raw = "1|K7M2Q9|K7M2Q9-01|$questionId|AA2AFA49",
        planId = if (checksumOk) "K7M2Q9" else null,
        pageId = if (checksumOk) "K7M2Q9-01" else null,
        questionId = if (checksumOk) questionId else null,
        centerX = 100f,
        centerY = 200f,
        checksumOk = checksumOk,
    )

    private fun page(
        planId: String = "K7M2Q9",
        knowledgeRows: List<PlanPageRows> = listOf(
            PlanPageRows(
                questionId = "q1",
                rows = listOf(
                    KnowledgeRow("kp-1", 1),
                    KnowledgeRow("kp-2", 2),
                ),
                triggeredKnowledgePointIds = listOf("kp-1"),
            ),
        ),
    ) = PlanPageFacts(
        planId = planId,
        pageId = "K7M2Q9-01",
        date = java.time.LocalDate.of(2026, 11, 16),
        questionIds = listOf("q1", "q2"),
        items = emptyList(),
        knowledgeRows = knowledgeRows,
    )

    private fun lookupOf(facts: PlanPageFacts?) = object : PlanPageLookup {
        override fun findPage(pageId: String): PlanPageFacts? =
            if (pageId == "K7M2Q9-01") facts else null
    }
}
