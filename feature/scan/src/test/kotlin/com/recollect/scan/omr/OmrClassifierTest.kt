package com.recollect.scan.omr

import com.recollect.model.Tier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * OMR 阈值与行状态（docs/specs/scan-pipeline.md「OMR 采样几何与阈值」）：
 * 暗像素比例 > 30% 命中、< 10% 未命中、10%–30% 模糊；
 * 一行 >1 命中 = CONFLICT；恰好 1 命中 = OK（其余框的模糊读数不推翻唯一命中）；
 * 0 命中且有模糊框 = UNREADABLE；0 命中且全部未命中 = NO_MARK。
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
        assertEquals(
            DetectionStatus.CONFLICT to null,
            OmrClassifier.rowOf(listOf(BubbleReading.HIT, BubbleReading.HIT, BubbleReading.UNREADABLE)),
        )
    }

    /**
     * 2026-11-22 回归：低分辨率扫描时未填涂框边缘可能落到 10%–30%，
     * 只要同一行存在唯一明确命中，就必须读成该档位，不能整行退化为 UNREADABLE。
     */
    @Test
    fun `a single clear hit wins even when other bubbles are ambiguous`() {
        assertEquals(
            DetectionStatus.OK to Tier.MASTERED,
            OmrClassifier.rowOf(listOf(BubbleReading.HIT, BubbleReading.MISS, BubbleReading.UNREADABLE)),
        )
        assertEquals(
            DetectionStatus.OK to Tier.FAMILIAR,
            OmrClassifier.rowOf(listOf(BubbleReading.UNREADABLE, BubbleReading.HIT, BubbleReading.UNREADABLE)),
        )
        assertEquals(
            DetectionStatus.OK to Tier.UNKNOWN,
            OmrClassifier.rowOf(listOf(BubbleReading.UNREADABLE, BubbleReading.MISS, BubbleReading.HIT)),
        )
    }
}
