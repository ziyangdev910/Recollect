package com.recollect.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.recollect.data.room.entity.NoteAttachmentItem

@Dao
interface NoteAttachmentItemDao {

    @Insert
    suspend fun insert(entity: NoteAttachmentItem): Long

    // 同一题内的下一个展示顺序；空表时为 0。
    @Query("SELECT COALESCE(MAX(`order`), -1) + 1 FROM note_attachment_item WHERE questionId = :questionId")
    suspend fun nextOrder(questionId: String): Int

    // 打印取最近 3 条（createdAt DESC，同时间按 order）。
    @Query(
        "SELECT * FROM note_attachment_item WHERE questionId = :questionId " +
            "ORDER BY createdAt DESC, `order` ASC LIMIT :limit",
    )
    suspend fun latestForQuestion(questionId: String, limit: Int): List<NoteAttachmentItem>
}
