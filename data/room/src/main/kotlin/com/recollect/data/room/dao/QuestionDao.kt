package com.recollect.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.recollect.data.room.entity.Question
import java.time.Instant
import java.time.LocalDate

@Dao
interface QuestionDao {

    @Insert
    suspend fun insert(entity: Question)

    @Update
    suspend fun update(entity: Question)

    @Query("SELECT * FROM question WHERE archived = 0")
    suspend fun active(): List<Question>

    /**
     * 题库列表：按最近一次用户修改时间倒序；时间相同时保持创建（插入）顺序，
     * 保证新增或编辑后的题目立即出现在列表顶部（2026-11-24 用户要求）。
     */
    @Query(
        "SELECT * FROM question WHERE archived = 0 " +
            "ORDER BY updatedAt DESC, rowid ASC",
    )
    suspend fun activeOrderedByUpdatedAt(): List<Question>

    @Query("SELECT * FROM question WHERE id = :id")
    suspend fun byId(id: String): Question?

    /**
     * 最近一条「无来源」题目使用的页码编号；无来源录入卡的默认值，不被带来源题目影响。
     */
    @Query(
        "SELECT sourcePageCode FROM question WHERE trim(sourcePage) = '' " +
            "ORDER BY createdAt DESC, id DESC LIMIT 1",
    )
    suspend fun latestSourcePageCodeWithoutSource(): Int?

    /**
     * 最近一条「有来源」题目使用的页码编号；新建页选中来源且用户未手改编号时采用。
     */
    @Query(
        "SELECT sourcePageCode FROM question WHERE trim(sourcePage) <> '' " +
            "ORDER BY createdAt DESC, id DESC LIMIT 1",
    )
    suspend fun latestSourcePageCodeWithSource(): Int?

    /** 未归档题目中引用该来源名称的题数；用于来源标签库的删除校验。 */
    @Query("SELECT COUNT(*) FROM question WHERE archived = 0 AND sourcePage = :sourcePage")
    suspend fun activeSourcePageCount(sourcePage: String): Int

    /** 来源标签重命名时同步更新题目引用（归档题目一并更新，取消归档后仍保持一致）。 */
    @Query("UPDATE question SET sourcePage = :newName WHERE sourcePage = :oldName")
    suspend fun renameSourcePageReferences(oldName: String, newName: String)

    @Query("UPDATE question SET pageId = :pageId, qrPayload = :qrPayload WHERE id = :id")
    suspend fun setPrintSnapshot(id: String, pageId: String, qrPayload: String)

    @Query("UPDATE question SET lastScheduledDate = :targetDate WHERE id = :id")
    suspend fun markScheduled(id: String, targetDate: LocalDate)

    @Query("UPDATE question SET archived = 1, archivedAt = :archivedAt WHERE id = :id")
    suspend fun markArchived(id: String, archivedAt: Instant)

    @Query("UPDATE question SET archived = 0, archivedAt = NULL WHERE id = :id")
    suspend fun markUnarchived(id: String)
}
