package com.recollect.data.room.entity

import androidx.room.Entity
import com.recollect.model.Tier
import java.time.LocalDate

/**
 * 回读暂存（临时状态，字段见 docs/specs/data-model.md）。
 *
 * 复合主键承载「一次回读一页多题、每题最多 4 个知识点」的逐行决策（C-022）。
 * 确认成功后按 `sessionId` 删除；备份不导出（C-031）。
 */
@Entity(
    tableName = "pending_feedback",
    primaryKeys = ["sessionId", "questionId", "knowledgePointId"],
)
data class PendingFeedback(
    val sessionId: String,
    val planId: String, // 所属计划（C-040）
    val pageId: String, // 所属页（C-040）
    val plannedDate: LocalDate, // 该页的计划日（C-038）
    val actualReviewDate: LocalDate? = plannedDate, // NOT_REVIEWED 可为 null（C-038）
    val questionId: String,
    val knowledgePointId: String,
    val triggered: Boolean, // 触发行/非触发行（C-009）
    val detectedTier: Tier?, // OK 时非空
    val detectionStatus: DetectionStatus, // OK / NO_MARK / CONFLICT / UNREADABLE
    val conflict: Boolean = false, // 多组填涂冲突标记
    val detectedRawOcr: String?, // 只读
    val userDecision: UserDecision = UserDecision.UNMARKED,
    val selectedTier: Tier?, // CORRECTED/SUPPLEMENTED 必填；ACCEPT 在 detectedTier 为空时必填
    val correctedText: String?, // 可编辑修正文本
    val isSupplemental: Boolean = false,
)

enum class UserDecision { UNMARKED, ACCEPT, CORRECTED, NOT_REVIEWED, SUPPLEMENTED }

enum class DetectionStatus { OK, NO_MARK, CONFLICT, UNREADABLE }
