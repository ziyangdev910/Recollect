package com.recollect.feature.print

import com.recollect.common.qr.QrPayloadCodec
import com.recollect.pagination.PlacedItem
import com.recollect.pagination.PlannedPage
import com.recollect.pagination.RenderableQuestion
import com.recollect.pagination.TemplateGeometry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * 打印层每题 QR 载荷（print-template.md、scan-pipeline.md「QR 载荷与校验算法（C-030）」）：
 * `version|planId|pageId|questionId|CRC32`，载荷必须由打印层生成并交给渲染层。
 */
class PrintQrPayloadTest {

    @Test
    fun `builds the c030 payload for every printed question`() {
        val page = PlannedPage(
            pageId = "K7M2Q9-01",
            pageNumber = 1,
            date = LocalDate.of(2026, 11, 16),
            items = listOf(
                PlacedItem("q1", yOffsetMm = 41.0, heightMm = 30.0),
                PlacedItem("q2", yOffsetMm = 74.0, heightMm = 30.0),
            ),
            continuationMarkers = emptyList(),
        )
        val questions = mapOf(
            "q1" to RenderableQuestion("q1", "T-1", 1, listOf("一元二次方程"), 1, 0, hasNoteArea = true),
            "q2" to RenderableQuestion("q2", "T-2", 2, listOf("勾股定理", "相似三角形"), 2, 0, hasNoteArea = false),
        )

        val layout = PrintLayoutBuilder.build(
            planId = "K7M2Q9",
            pages = listOf(page),
            questions = questions,
            noteTexts = emptyMap(),
            template = TemplateGeometry.a4Print(),
        )

        val payloads = layout.pages.single().items.associate { it.questionId to it.qrPayload }
        assertEquals(QrPayloadCodec.encode("K7M2Q9", "K7M2Q9-01", "q1"), payloads.getValue("q1"))
        assertEquals(QrPayloadCodec.encode("K7M2Q9", "K7M2Q9-01", "q2"), payloads.getValue("q2"))
    }
}
