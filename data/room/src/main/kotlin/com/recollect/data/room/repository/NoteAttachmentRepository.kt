package com.recollect.data.room.repository

import com.recollect.data.room.dao.NoteAttachmentItemDao
import com.recollect.data.room.entity.NoteAttachmentItem

/**
 * 笔记附件的只读端口（repository.md `NoteAttachmentRepository` 的本轮子集）。
 *
 * 写入随确认页「保存为笔记附件」补齐；打印/预览只读取最近 [limit] 条。
 */
interface NoteAttachmentRepository {
    suspend fun items(questionId: String, limit: Int = 3): List<NoteAttachmentItem>
}

class RoomNoteAttachmentRepository(
    private val noteAttachmentItemDao: NoteAttachmentItemDao,
) : NoteAttachmentRepository {
    override suspend fun items(questionId: String, limit: Int): List<NoteAttachmentItem> =
        noteAttachmentItemDao.latestForQuestion(questionId, limit)
}
