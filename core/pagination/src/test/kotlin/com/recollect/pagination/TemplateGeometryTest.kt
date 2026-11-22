package com.recollect.pagination

import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TemplateGeometryTest {

    /**
     * 默认 A4 几何（pagination.md「可用高度（C-025 方案一）」、P-19）：
     * usable = 297 - 12 - 12；每页内容区扣 7mm 表头（2026-11-22 删除首页图例，
     * 所有页面容量相同）；附件上限为 usable 的 30%；
     * QR 底栏为 18 + 3 + pageId 标签间距 2 + 标签高度 4。
     * C-063：日期大标题 5mm 字号 / 7mm 行高；2026-11-22 删除每日总题数。
     */
    @Test
    fun `default template derives page heights, attachment limit and QR footer`() {
        val geometry = TemplateGeometry()
        val tolerance = 1e-9

        assertEquals(
            273.0,
            geometry.usableHeightMm,
            tolerance,
            "usableHeightMm = 297 - 12 - 12",
        )
        assertEquals(
            266.0,
            geometry.contentHeightMm,
            tolerance,
            "contentHeightMm = usableHeightMm - headerRowHeightMm",
        )
        assertEquals(
            81.9,
            geometry.maxAttachmentHeightMm,
            tolerance,
            "maxAttachmentHeightMm = usableHeightMm * 0.30",
        )
        assertEquals(
            27.0,
            geometry.qrFooterHeightMm,
            tolerance,
            "qrFooterHeightMm = qrSizeMm(18) + qrPaddingMm(3) + pageIdLabelGapMm(2) + pageIdLabelHeightMm(4)",
        )
        assertEquals(5.0, geometry.dateHeadingFontMm, tolerance, "日期大标题默认字号 5mm")
        assertEquals(7.0, geometry.dateHeadingHeightMm, tolerance, "日期大标题默认行高 7mm")
        assertEquals(
            7.0,
            geometry.dateHeadingBlockHeightMm,
            tolerance,
            "2026-11-22 删除每日总题数后，标题块高度只剩日期大标题 7mm",
        )
        assertTrue(
            geometry.dateHeadingBlockHeightMm <= geometry.topMarginMm,
            "默认模板的日期标题块（7mm）必须落在上边距（12mm）内，不占用内容区高度",
        )
    }

    /**
     * C-091 方案 A：生产打印模板为四角 ArUco（内码 10mm + 白边 4mm）预留角区。
     * 标记内码距页边 12mm，白边外沿到 8mm、内沿到 26mm；内容区必须从 26mm 开始，
     * 避免标记/白边覆盖日期标题、表头与首行知识点。
     */
    @Test
    fun `print template reserves corner area for aruco markers`() {
        val geometry = TemplateGeometry.a4Print()
        val tolerance = 1e-9

        assertEquals(26.0, geometry.contentLeftMm, tolerance, "内容区左边界 = 12 + 14")
        assertEquals(26.0, geometry.contentTopMm, tolerance, "内容区上边界 = 12 + 14")
        assertEquals(
            158.0,
            geometry.contentWidthMm,
            tolerance,
            "contentWidthMm = 210 - 2*12 - 2*14",
        )
        assertEquals(
            245.0,
            geometry.usableHeightMm,
            tolerance,
            "usableHeightMm = 297 - 12 - 12 - 2*14",
        )
        assertEquals(
            238.0,
            geometry.contentHeightMm,
            tolerance,
            "contentHeightMm = usableHeightMm - headerRowHeightMm（每页相同）",
        )
        assertEquals(66.36, geometry.leftAreaWidthMm, tolerance, "leftAreaWidthMm = 158 * 0.42")
        assertEquals(91.64, geometry.rightAreaWidthMm, tolerance, "rightAreaWidthMm = 158 * 0.58")

        // 2026-11-22：填涂格改为答题卡式 16:9 宽格；序号列/列间距收窄，三格仍落在左区内。
        // 同日用户要求把框上方文字改为「掌握 / 较熟悉 / 不会」，框间距由 1.55mm 扩大到 3.1mm。
        assertEquals(6.0, geometry.indexColumnWidthMm, tolerance)
        assertEquals(34.0, geometry.knowledgeColumnWidthMm, tolerance)
        assertEquals(1.5, geometry.columnGapMm, tolerance)
        assertEquals(5.7, geometry.bubbleWidthMm, tolerance)
        assertEquals(3.2, geometry.bubbleHeightMm, tolerance)
        assertEquals(3.1, geometry.bubbleGapMm, tolerance)
        assertEquals(2.5, geometry.bubbleSampleSizeMm, tolerance)
        assertEquals(
            16.0 / 9.0,
            geometry.bubbleWidthMm / geometry.bubbleHeightMm,
            0.01,
            "填涂格宽高比应为 16:9",
        )
        assertTrue(
            geometry.bubbleSampleSizeMm < geometry.bubbleHeightMm,
            "OMR 2.5mm 采样窗必须落在填涂格高度内",
        )
        assertEquals(
            listOf(71.85, 80.65, 89.45),
            geometry.bubbleCentersXMm.map { (it * 100).roundToInt() / 100.0 },
            "掌握/较熟悉/不会 三格中心 x（a4Print）",
        )
        assertTrue(
            geometry.bubbleLeftMm(2) + geometry.bubbleWidthMm <=
                geometry.contentLeftMm + geometry.leftAreaWidthMm + 1e-9,
            "三格右缘不得越过左侧填涂区右边界",
        )
        assertEquals(
            85.64,
            geometry.rightTextWidthMm,
            tolerance,
            "rightTextWidthMm = 91.64 - 2*3",
        )
        assertEquals(
            73.5,
            geometry.maxAttachmentHeightMm,
            tolerance,
            "maxAttachmentHeightMm = usableHeightMm * 0.30",
        )
        assertTrue(
            geometry.contentLeftMm >= 12.0 + 10.0 + 4.0,
            "内容区左边界必须越过 ArUco 白边外沿（12 + 10 + 4 = 26mm）",
        )
        assertTrue(
            geometry.contentTopMm >= 12.0 + 10.0 + 4.0,
            "内容区上边界必须越过 ArUco 白边外沿（12 + 10 + 4 = 26mm）",
        )
        assertEquals(
            7.0,
            geometry.dateHeadingBlockHeightMm,
            tolerance,
            "2026-11-22 删除每日总题数后，日期标题块只有 7mm",
        )
        assertTrue(
            geometry.dateHeadingBlockHeightMm <= geometry.contentTopMm,
            "日期标题块（7mm）必须落在内容区上边界之前，不占用内容区高度",
        )
    }
}
