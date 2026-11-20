package com.recollect.scan.omr

import com.recollect.model.Tier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * OMR 阈值与行状态（docs/specs/scan-pipeline.md「OMR 采样几何与阈值」）：
 * 暗像素比例 > 30% 命中、< 10% 未命中、10%–30% 不可判定；
 * 一行 >1 命中 = CONFLICT、0 命中 = NO_MARK、恰好 1 个 = OK。
 */
class OmrClassifierTest {

    @Test
    fun `classifies dark ratio into hit miss and unreadable`() {
        assertEquals(BubbleReading.HIT, OmrClassifier.classify(0.31))
        assertEquals(BubbleReading.MISS, OmrClassifier.classify(0.09))
        assertEquals(BubbleReading.UNREADABLE, OmrClassifier.classify(0.10))
        assertEquals(BubbleReading.UNREADABLE, OmrClassifier.classify(0.30))
    }

    @Test
    fun `maps a row of three bubbles to status and tier`() {
        assertEquals(
            DetectionStatus.OK to Tier.MASTERED,
            OmrClassifier.rowOf(listOf(BubbleReading.HIT, BubbleReading.MISS, BubbleReading.MISS)),
        )
        assertEquals(
            DetectionStatus.NO_MARK to null,
            OmrClassifier.rowOf(listOf(BubbleReading.MISS, BubbleReading.MISS, BubbleReading.MISS)),
        )
        assertEquals(
            DetectionStatus.CONFLICT to null,
            OmrClassifier.rowOf(listOf(BubbleReading.HIT, BubbleReading.MISS, BubbleReading.HIT)),
        )
        assertEquals(
            DetectionStatus.UNREADABLE to null,
            OmrClassifier.rowOf(listOf(BubbleReading.MISS, BubbleReading.UNREADABLE, BubbleReading.MISS)),
        )
    }
}
