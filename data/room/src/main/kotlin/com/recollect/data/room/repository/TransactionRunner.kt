package com.recollect.data.room.repository

import androidx.room.RoomDatabase
import androidx.room.withTransaction

/**
 * 事务执行器：Repository 的跨表写操作只通过本接口进入同一个事务（repository.md「事务规则」）。
 *
 * 生产实现是 [RoomTransactionRunner]（Room `withTransaction`）；单元测试用可回滚的 fake，
 * 从而在 JVM 上验证「任一步失败全部回滚」，不需要 Robolectric 或设备。
 */
interface TransactionRunner {
    suspend fun <T> inTransaction(block: suspend () -> T): T
}

class RoomTransactionRunner(private val database: RoomDatabase) : TransactionRunner {
    override suspend fun <T> inTransaction(block: suspend () -> T): T =
        database.withTransaction { block() }
}
