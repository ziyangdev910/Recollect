package com.recollect.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * 计划级扫描会话（临时操作态，字段见 docs/specs/data-model.md）。
 *
 * 一次 plan 扫描共用一个 `sessionId`；多页扫描向同一 session 追加 [PendingFeedback]。
 * 备份不导出；导入时清空（C-031）。
 */
@Entity(tableName = "scan_session")
data class ScanSession(
    @PrimaryKey val sessionId: String,
    val planId: String,
    val startedAt: Instant,
    val status: ScanSessionStatus, // SCANNING / READY / SUBMITTED / ABANDONED
    val submittedAt: Instant?,
)

enum class ScanSessionStatus { SCANNING, READY, SUBMITTED, ABANDONED }
