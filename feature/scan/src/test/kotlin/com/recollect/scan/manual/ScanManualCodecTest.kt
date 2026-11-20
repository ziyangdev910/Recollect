package com.recollect.scan.manual

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * 手动回退的页面 ID 规范化（scan-pipeline.md「手动回退」）：
 * 大小写不敏感、`I/L → 1`、`O → 0`、只允许 `A-Z a-z 0-9 -`、结构为 `6 位计划号-2 位页码`。
 */
class ScanManualCodecTest {

    @Test
    fun `normalizes lowercase and confusable characters`() {
        assertEquals("K7M2Q9-01", ScanManualCodec.normalizePageId(" k7m2q9-0l "))
        // 页码里的 O 同样映射为 0（Crockford 易混字符）。
        assertEquals("K7M2Q9-00", ScanManualCodec.normalizePageId("k7m2q9-0o"))
    }

    @Test
    fun `accepts a canonical page id`() {
        assertEquals("K7M2Q9-12", ScanManualCodec.normalizePageId("K7M2Q9-12"))
    }

    @Test
    fun `rejects wrong length or structure`() {
        assertNull(ScanManualCodec.normalizePageId("K7M2Q9-1"))
        assertNull(ScanManualCodec.normalizePageId("K7M2Q9-001"))
        assertNull(ScanManualCodec.normalizePageId("K7M2Q9"))
        assertNull(ScanManualCodec.normalizePageId("K7M2Q901"))
        assertNull(ScanManualCodec.normalizePageId(""))
    }

    @Test
    fun `rejects characters outside the allowed set`() {
        assertNull(ScanManualCodec.normalizePageId("K7M2Q9-01 页"))
        assertNull(ScanManualCodec.normalizePageId("K7M2Q9－01"))
        assertNull(ScanManualCodec.normalizePageId("K7M2Q9_01"))
    }

    @Test
    fun `rejects the crockford-excluded letter u`() {
        assertNull(ScanManualCodec.normalizePageId("KUM2Q9-01"))
    }

}
