package com.recollect.model

import java.time.Instant
import java.time.LocalDate

// core:model 的纯 DTO/枚举（C-046 方案 A、C-053 方案 A）；不依赖 Android。
// 字段定义见 docs/specs/scheduler.md。

enum class Tier { UNKNOWN, FAMILIAR, MASTERED } // 由弱到强；groupTier 取最高档位

data class KnowledgeState(
    val id: String,
    val number: Int,
    val currentTier: Tier,
    val continuousScore: Float, // [0,1]；对外 Float，内部算术用 Double（C-050）
    val nextDueDate: LocalDate,
    val effectiveFeedbackCount: Float, // 0.5 的倍数；见 C-006
    val dormant: Boolean,
    val activeQuestionCount: Int,
    val difficulty: Int?, // 墨墨 DHP；冷启动阶段 null（C-018）
    val halfLifeDays: Float?, // 墨墨 DHP；冷启动阶段 null（C-018）
    val createdAt: Instant, // DHP 第一段间隔起点（C-037）
)

data class SchedulerQuestion(
    val id: String,
    val linkedKnowledgeIds: List<String>, // 1..4；由 data/room 从 cross-ref 映射（C-046）
    val archived: Boolean,
    val lastReviewedDate: LocalDate?, // 实际复习日；冷却判定用（C-039）
    val estimatedReviewMinutes: Int = 15, // C-017；缺失或 <=0 按 15 分钟
)
