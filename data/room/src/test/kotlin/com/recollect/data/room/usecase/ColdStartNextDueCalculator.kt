package com.recollect.data.room.usecase

import com.recollect.model.Tier
import java.time.LocalDate

/**
 * 冷启动阶梯：按反馈事件序号取 1/3/7 天（D-05）。
 *
 * C-085 方案 B：4.3a 的兼容实现移到测试源集作为对照，不再进入生产装配；
 * 生产使用 [MaimemoNextDueCalculator]。
 */
class ColdStartNextDueCalculator : NextDueCalculator {
    override fun nextDueDate(
        memoryState: MemoryState?,
        tier: Tier,
        continuousScoreAfter: Float,
        feedbackEventCountAfter: Int,
        effectiveFeedbackCountBefore: Float,
        reviewedOn: LocalDate,
        actualIntervalDays: Int,
    ): NextDueResult = NextDueResult(
        nextDueDate = reviewedOn.plusDays(
            when (feedbackEventCountAfter) {
                1 -> 1L
                2 -> 3L
                else -> 7L
            },
        ),
        memoryState = null,
    )
}
