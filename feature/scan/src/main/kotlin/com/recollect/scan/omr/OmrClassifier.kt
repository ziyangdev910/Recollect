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
     * 一行三框（掌握 / 较熟悉 / 不会）→ 行状态与档位。
     *
     * 优先级（2026-11-22 修复）：
     * 1. 多于 1 个明确命中 → `CONFLICT`；
     * 2. 恰好 1 个明确命中 → `OK`，其余框即使落在 10%–30% 的模糊区间也不推翻唯一命中
     *    （低分辨率扫描时未填涂框边缘常落在这个区间，旧逻辑会把整行误判为 `UNREADABLE`）；
     * 3. 0 命中且存在模糊框 → `UNREADABLE`；
     * 4. 0 命中且全部未命中 → `NO_MARK`。
     *
     * 档位按索引 0=掌握、1=较熟悉、2=不会。
     */
    fun rowOf(readings: List<BubbleReading>): Pair<DetectionStatus, Tier?> {
        val hits = readings.withIndex().filter { it.value == BubbleReading.HIT }
        return when {
            hits.size > 1 -> DetectionStatus.CONFLICT to null
            hits.size == 1 -> DetectionStatus.OK to TIERS[hits.single().index]
            readings.any { it == BubbleReading.UNREADABLE } -> DetectionStatus.UNREADABLE to null
            else -> DetectionStatus.NO_MARK to null
        }
    }

    private const val HIT_RATIO = 0.30
    private const val MISS_RATIO = 0.10

    /** 列序：掌握、较熟悉、不会（与打印表头顺序一致，D-07）。 */
    private val TIERS = listOf(Tier.MASTERED, Tier.FAMILIAR, Tier.UNKNOWN)
}
