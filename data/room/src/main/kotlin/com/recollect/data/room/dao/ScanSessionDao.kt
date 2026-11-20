package com.recollect.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.recollect.data.room.entity.ScanSession
import com.recollect.data.room.entity.ScanSessionStatus
import java.time.Instant

@Dao
interface ScanSessionDao {

    @Insert
    suspend fun insert(entity: ScanSession)

    @Update
    suspend fun update(entity: ScanSession)

    @Query("SELECT * FROM scan_session WHERE sessionId = :sessionId")
    suspend fun byId(sessionId: String): ScanSession?

    // startSession：同一 plan 已有 SCANNING/READY 会话则复用。
    @Query(
        "SELECT * FROM scan_session WHERE planId = :planId AND status IN ('SCANNING', 'READY') " +
            "ORDER BY startedAt DESC LIMIT 1",
    )
    suspend fun activeForPlan(planId: String): ScanSession?

    /** 扫描页恢复入口：进程重建后按最近一次未提交会话继续追加（app.md「全局错误与恢复」）。 */
    @Query(
        "SELECT * FROM scan_session WHERE status IN ('SCANNING', 'READY') " +
            "ORDER BY startedAt DESC LIMIT 1",
    )
    suspend fun latestActive(): ScanSession?

    @Query("UPDATE scan_session SET status = 'SUBMITTED', submittedAt = :submittedAt WHERE sessionId = :sessionId")
    suspend fun markSubmitted(sessionId: String, submittedAt: Instant)

    @Query("UPDATE scan_session SET status = :status WHERE sessionId = :sessionId")
    suspend fun updateStatus(sessionId: String, status: ScanSessionStatus)

    // App 启动时把超过 24h 且非 SUBMITTED 的会话标为 ABANDONED。
    @Query("UPDATE scan_session SET status = 'ABANDONED' WHERE status != 'SUBMITTED' AND startedAt < :threshold")
    suspend fun abandonStale(threshold: Instant)
}
