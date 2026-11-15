package com.recollect.pagination

import kotlin.test.Test
import kotlin.test.assertEquals

class TemplateGeometryTest {

    /**
     * 默认 A4 几何（pagination.md「可用高度（C-025 方案一）」、P-19）：
     * usable = 297 - 12 - 12；首页再扣 8mm 图例；内容区每页扣 7mm 表头；
     * 附件上限为 usable 的 30%；QR 底栏为 18 + 3。
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
    }
}
