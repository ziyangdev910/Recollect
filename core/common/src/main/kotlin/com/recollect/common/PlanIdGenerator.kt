package com.recollect.common

import kotlin.random.Random

/**
 * 计划号生成器（planning.md「ID、时区与默认设置」）。
 *
 * 6 位 Crockford Base32（去掉 I/L/O/U），由计划层保证格式；分页器只拼接、不校验（C-069）。
 */
fun interface PlanIdGenerator {
    fun next(): String
}

/**
 * 生产实现：从 Crockford 字母表随机取 6 位。
 *
 * 与 `PlanPage.planId` 的冲突重试由计划层在持久化边界处理；本生成器只负责格式。
 */
class CrockfordPlanIdGenerator(
    private val random: Random = Random.Default,
) : PlanIdGenerator {

    override fun next(): String = buildString(PLAN_ID_LENGTH) {
        repeat(PLAN_ID_LENGTH) {
            append(ALPHABET[random.nextInt(ALPHABET.length)])
        }
    }

    companion object {
        const val PLAN_ID_LENGTH = 6
        const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    }
}
