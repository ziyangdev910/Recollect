package com.recollect.pagination

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TemplateGeometryTest {

    /**
     * 默认 A4 几何（pagination.md「可用高度（C-025 方案一）」、P-19）：
     * usable = 297 - 12 - 12；首页再扣 8mm 图例；内容区每页扣 7mm 表头；
     * 附件上限为 usable 的 30%；QR 底栏为 18 + 3。
     * C-063：日期大标题 5mm 字号 / 7mm 行高；每日总题数为其一半（2.5mm / 3.5mm），
     * 合计 10.5mm ≤ 默认上边距 12mm，绘制在上边距内，不占用内容区高度。
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
            265.0,
            geometry.firstPageUsableHeightMm,
            tolerance,
            "firstPageUsableHeightMm = usableHeightMm - legendHeightMm",
        )
        assertEquals(
            266.0,
            geometry.contentHeightMm,
            tolerance,
            "contentHeightMm = usableHeightMm - headerRowHeightMm",
        )
        assertEquals(
            258.0,
            geometry.firstPageContentHeightMm,
            tolerance,
            "firstPageContentHeightMm = firstPageUsableHeightMm - headerRowHeightMm",
        )
        assertEquals(
            81.9,
            geometry.maxAttachmentHeightMm,
            tolerance,
            "maxAttachmentHeightMm = usableHeightMm * 0.30",
        )
        assertEquals(
            21.0,
            geometry.qrFooterHeightMm,
            tolerance,
            "qrFooterHeightMm = qrSizeMm + qrPaddingMm",
        )
        assertEquals(5.0, geometry.dateHeadingFontMm, tolerance, "日期大标题默认字号 5mm")
        assertEquals(7.0, geometry.dateHeadingHeightMm, tolerance, "日期大标题默认行高 7mm")
        assertEquals(
            2.5,
            geometry.dailyTotalFontMm,
            tolerance,
            "C-063：每日总题数字号 = 日期标题字号 / 2",
        )
        assertEquals(
            3.5,
            geometry.dailyTotalHeightMm,
            tolerance,
            "C-063：每日总题数高度 = 日期标题高度 / 2",
        )
        assertEquals(
            10.5,
            geometry.dateHeadingBlockHeightMm,
            tolerance,
            "C-063：日期标题 + 每日总题数合计 7 + 3.5 = 10.5mm",
        )
        assertTrue(
            geometry.dateHeadingBlockHeightMm <= geometry.topMarginMm,
            "默认模板的日期标题块（10.5mm）必须落在上边距（12mm）内，不占用内容区高度",
        )
    }
}
