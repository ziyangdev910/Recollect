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
            3.2,
            geometry.dailyTotalFontMm,
            tolerance,
            "每日总题数字号与图例同字号 3.2mm（2026-11-18 用户要求）",
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

    /**
     * C-091 方案 A：生产打印模板为四角 ArUco（内码 10mm + 白边 4mm）预留角区。
     * 标记内码距页边 12mm，白边外沿到 8mm、内沿到 26mm；内容区必须从 26mm 开始，
     * 避免标记/白边覆盖图例、每日总题数、表头与首行知识点。
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
            "contentHeightMm = usableHeightMm - headerRowHeightMm",
        )
        assertEquals(
            230.0,
            geometry.firstPageContentHeightMm,
            tolerance,
            "firstPageContentHeightMm = usableHeightMm - legendHeightMm - headerRowHeightMm",
        )
        assertEquals(63.2, geometry.leftAreaWidthMm, tolerance, "leftAreaWidthMm = 158 * 0.40")
        assertEquals(94.8, geometry.rightAreaWidthMm, tolerance, "rightAreaWidthMm = 158 * 0.60")
        assertEquals(
            88.8,
            geometry.rightTextWidthMm,
            tolerance,
            "rightTextWidthMm = 94.8 - 2*3",
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
        assertTrue(
            geometry.dateHeadingBlockHeightMm <= geometry.contentTopMm,
            "日期标题块（10.5mm）必须落在内容区上边界之前，不占用内容区高度",
        )
    }
}
