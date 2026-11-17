package com.recollect.data.room.usecase

import com.recollect.model.Tier
import java.time.LocalDate
import kotlin.math.exp
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * 墨墨 DHP 简化模型的记忆状态（feedback-usecase.md「接口」）。
 *
 * 冷启动阶段为 `null`；第 3 条 triggered 反馈后由 `MemoryStateInitializer` 初始化（C-018）。
 */
data class MemoryState(
    val difficulty: Int,
    val halfLifeDays: Float,
)

/**
 * 下一次到期日计算结果（feedback-usecase.md「接口」）。
 *
 * [memoryState] 为 `null` 时知识点保持原有 `difficulty/halfLifeDays`。
 */
data class NextDueResult(
    val nextDueDate: LocalDate,
    val memoryState: MemoryState?,
)

/**
 * 冷启动前三次反馈的档位与实际间隔（feedback-usecase.md「接口」）。
 *
 * `intervalDays` 为实际天数，至少 1、至多 365（C-037）。
 */
data class TriggeredFeedback(
    val tier: Tier,
    val intervalDays: Int,
)

/**
 * 用前三条 triggered 反馈估计 DHP 初始状态（C-018、C-037、research/maimemo-model.md 第 4 节）。
 */
interface MemoryStateInitializer {
    fun initial(
        recentTriggeredFeedbacks: List<TriggeredFeedback>,
        continuousScoreAfterThird: Float,
    ): MemoryState
}

/**
 * 下一次到期日计算器（feedback-usecase.md「接口」）。
 *
 * 实现只做公式计算，不读数据库（规格「冷启动与切换边界」）。
 *
 * `actualIntervalDays` 是本次反馈距上一次 triggered 反馈的实际天数（首条按 `createdAt` 起算）；
 * 必填入参，由状态更新用例计算并传入（C-037、C-084 方案 A）。
 */
interface NextDueCalculator {
    fun nextDueDate(
        memoryState: MemoryState?,
        tier: Tier,
        continuousScoreAfter: Float,
        feedbackEventCountAfter: Int,
        effectiveFeedbackCountBefore: Float,
        reviewedOn: LocalDate,
        actualIntervalDays: Int,
    ): NextDueResult
}

/**
 * 冷启动 + 墨墨 DHP 简化模型的到期日计算（D-05、C-018、C-037）。
 *
 * - 事件序号 1/2/3 固定 1/3/7 天；第 3 条反馈写入后记忆状态初始化，本次仍按冷启动。
 * - 事件序号 > 3：先按实际间隔更新 `h`（FAMILIAR 半强度、UNKNOWN 难度 +2），再决定到期日。
 * - `effectiveFeedbackCountBefore >= 3.0f` 才用 DHP 计算间隔；达到 3.0 的这次仍按冷启动 7 天
 *   （D-05/C-018，C-050 精确比较，不做 epsilon）。
 */
class MaimemoNextDueCalculator : NextDueCalculator {
    override fun nextDueDate(
        memoryState: MemoryState?,
        tier: Tier,
        continuousScoreAfter: Float,
        feedbackEventCountAfter: Int,
        effectiveFeedbackCountBefore: Float,
        reviewedOn: LocalDate,
        actualIntervalDays: Int,
    ): NextDueResult {
        if (feedbackEventCountAfter <= 3) {
            return NextDueResult(
                nextDueDate = reviewedOn.plusDays(coldStartIntervalDays(feedbackEventCountAfter)),
                memoryState = memoryState, // 第 3 次时由 UseCase 传入刚初始化的状态（C-037）
            )
        }

        val updated = memoryState?.let { update(it, tier, actualIntervalDays) }
        val intervalDays = if (updated != null && effectiveFeedbackCountBefore >= SWITCH_THRESHOLD) {
            dhpIntervalDays(updated.halfLifeDays)
        } else {
            7L // 3.0 之前继续更新 DHP 状态，但到期日仍按冷启动 7 天
        }
        return NextDueResult(
            nextDueDate = reviewedOn.plusDays(intervalDays),
            memoryState = updated,
        )
    }
}

/** 冷启动阶梯：事件序号 1/2/3 分别为 1/3/7 天，之后仍按 7 天（D-05）。 */
private fun coldStartIntervalDays(feedbackEventCountAfter: Int): Long = when (feedbackEventCountAfter) {
    1 -> 1L
    2 -> 3L
    else -> 7L
}

/** 用前三次 triggered 反馈初始化 `difficulty/halfLifeDays`（C-037，research 第 4 节）。 */
class DefaultMemoryStateInitializer : MemoryStateInitializer {
    override fun initial(
        recentTriggeredFeedbacks: List<TriggeredFeedback>,
        continuousScoreAfterThird: Float,
    ): MemoryState {
        require(recentTriggeredFeedbacks.size == 3) {
            "DHP 初始化需要前 3 条 triggered 反馈"
        }

        var difficulty = initialDifficulty(
            recentTriggeredFeedbacks = recentTriggeredFeedbacks,
            continuousScoreAfterThird = continuousScoreAfterThird,
        )
        var halfLifeDays = startHalfLifeDays(difficulty)

        recentTriggeredFeedbacks.forEach { feedback ->
            val intervalDays = feedback.intervalDays.coerceIn(1, 365).toDouble()
            val recallProbability = 2.0.pow(-intervalDays / halfLifeDays)
            when (feedback.tier) {
                Tier.MASTERED -> halfLifeDays = recallHalfLife(difficulty, halfLifeDays, recallProbability)
                Tier.FAMILIAR -> {
                    val recalled = recallHalfLife(difficulty, halfLifeDays, recallProbability)
                    halfLifeDays += 0.5 * (recalled - halfLifeDays) // 半强度回忆（C-018）
                }
                Tier.UNKNOWN -> {
                    halfLifeDays = forgetHalfLife(difficulty, halfLifeDays, recallProbability)
                    difficulty = (difficulty + 2).coerceAtMost(MAX_DIFFICULTY)
                }
            }
            halfLifeDays = halfLifeDays.coerceIn(MIN_HALF_LIFE_DAYS, MAX_HALF_LIFE_DAYS)
        }

        return MemoryState(
            difficulty = difficulty,
            halfLifeDays = halfLifeDays.toFloat(),
        )
    }
}

/** `d_init = clamp(round(1 + 9*(1 - avgTier) - 2*(score - 0.5)), 1, 10)`（C-037）。 */
private fun initialDifficulty(
    recentTriggeredFeedbacks: List<TriggeredFeedback>,
    continuousScoreAfterThird: Float,
): Int {
    val averageTier = recentTriggeredFeedbacks
        .map { it.tier.tierScore() }
        .average()
    val raw = 1.0 + 9.0 * (1.0 - averageTier) - 2.0 * (continuousScoreAfterThird.toDouble() - 0.5)
    return raw.roundToInt().coerceIn(MIN_DIFFICULTY, MAX_INITIAL_DIFFICULTY)
}

private fun Tier.tierScore(): Double = when (this) {
    Tier.MASTERED -> 1.0
    Tier.FAMILIAR -> 0.5
    Tier.UNKNOWN -> 0.0
}

/** `h0 = -1 / log2(max(0.925 - 0.05*d, 0.025))`（research 第 2 节）。 */
private fun startHalfLifeDays(difficulty: Int): Double =
    -1.0 / log2(max(0.925 - 0.05 * difficulty, 0.025))

/** `h' = h * (1 + exp(3.81) * d^(-0.534) * h^(-0.127) * (1-p)^0.97)`（认识）。 */
private fun recallHalfLife(difficulty: Int, halfLifeDays: Double, recallProbability: Double): Double =
    halfLifeDays * (
        1.0 +
            exp(3.81) *
            difficulty.toDouble().pow(-0.534) *
            halfLifeDays.pow(-0.127) *
            (1.0 - recallProbability).pow(0.97)
        )

/** `h' = exp(-0.041) * d^(-0.041) * h^0.377 * (1-p)^(-0.227)`（忘记）。 */
private fun forgetHalfLife(difficulty: Int, halfLifeDays: Double, recallProbability: Double): Double =
    exp(-0.041) *
        difficulty.toDouble().pow(-0.041) *
        halfLifeDays.pow(0.377) *
        (1.0 - recallProbability).pow(-0.227)

/** 每次 triggered 反馈后的 DHP 更新；UNKNOWN 同时把难度 +2（C-037）。 */
private fun update(memoryState: MemoryState, tier: Tier, actualIntervalDays: Int): MemoryState {
    var difficulty = memoryState.difficulty.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
    var halfLifeDays = memoryState.halfLifeDays.toDouble()
        .coerceIn(MIN_HALF_LIFE_DAYS, MAX_HALF_LIFE_DAYS)
    val intervalDays = actualIntervalDays.coerceIn(1, 365).toDouble()
    val recallProbability = 2.0.pow(-intervalDays / halfLifeDays)

    when (tier) {
        Tier.MASTERED -> halfLifeDays = recallHalfLife(difficulty, halfLifeDays, recallProbability)
        Tier.FAMILIAR -> {
            val recalled = recallHalfLife(difficulty, halfLifeDays, recallProbability)
            halfLifeDays += 0.5 * (recalled - halfLifeDays) // 半强度回忆（C-018）
        }
        Tier.UNKNOWN -> {
            halfLifeDays = forgetHalfLife(difficulty, halfLifeDays, recallProbability)
            difficulty = (difficulty + 2).coerceAtMost(MAX_DIFFICULTY)
        }
    }

    return MemoryState(
        difficulty = difficulty,
        halfLifeDays = halfLifeDays.coerceIn(MIN_HALF_LIFE_DAYS, MAX_HALF_LIFE_DAYS).toFloat(),
    )
}

/** `interval = clamp(round(-h * log2(0.9)), 1, 30)`（90% 保持率，C-018）。 */
private fun dhpIntervalDays(halfLifeDays: Float): Long =
    (-halfLifeDays.toDouble() * log2(0.9)).roundToInt().coerceIn(1, MAX_INTERVAL_DAYS).toLong()

/** C-050：精确比较，不做 epsilon；0.5 在二进制浮点中可精确表示。 */
private const val SWITCH_THRESHOLD = 3.0f

private const val MIN_DIFFICULTY = 1
private const val MAX_INITIAL_DIFFICULTY = 10
private const val MAX_DIFFICULTY = 18
private const val MIN_HALF_LIFE_DAYS = 0.5
private const val MAX_HALF_LIFE_DAYS = 365.0
private const val MAX_INTERVAL_DAYS = 30
