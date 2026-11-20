package com.recollect.scan.omr

import com.recollect.model.Tier

/**
 * 单框读数与行状态判定（docs/specs/scan-pipeline.md「OMR 采样几何与阈值」）。
 *
 * 只做纯判定，不碰 Bitmap；采样与灰度换算由 `OpenCvScanEngine` 负责。
 */
enum class BubbleReading { HIT, MISS, UNREADABLE }

enum class DetectionStatus { OK, NO_MARK, CONFLICT, UNREADABLE }

object OmrClassifier {

    /** 暗像素比例阈值：`> 30%` 命中，`< 10%` 未命中，`10%–30%` 不可判定。 */
    fun classify(darkRatio: Double): BubbleReading = when {
        darkRatio > HIT_RATIO -> BubbleReading.HIT
        darkRatio < MISS_RATIO -> BubbleReading.MISS
        else -> BubbleReading.UNREADABLE
    }

    /**
     * 一行三框（A/B/C）→ 行状态与档位。
     * 多框命中优先判 `CONFLICT`；否则任一无判定判 `UNREADABLE`；
     * 0 命中 `NO_MARK`；恰好 1 命中 `OK`，档位按 A=掌握、B=较熟悉、C=不会。
     */
    fun rowOf(readings: List<BubbleReading>): Pair<DetectionStatus, Tier?> {
        val hits = readings.withIndex().filter { it.value == BubbleReading.HIT }
        return when {
            hits.size > 1 -> DetectionStatus.CONFLICT to null
            readings.any { it == BubbleReading.UNREADABLE } -> DetectionStatus.UNREADABLE to null
            hits.isEmpty() -> DetectionStatus.NO_MARK to null
            else -> DetectionStatus.OK to TIERS[hits.single().index]
        }
    }

    private const val HIT_RATIO = 0.30
    private const val MISS_RATIO = 0.10

    /** 列序：A—掌握、B—较熟悉、C—不会（D-07 图例）。 */
    private val TIERS = listOf(Tier.MASTERED, Tier.FAMILIAR, Tier.UNKNOWN)
}
