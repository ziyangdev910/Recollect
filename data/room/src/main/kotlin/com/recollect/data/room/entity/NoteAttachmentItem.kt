package com.recollect.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * 笔记附件条目（C-026 方案二，字段见 docs/specs/data-model.md）。
 *
 * `text` 使用 `ReviewFeedback.correctedText` 的快照，不用 `rawOcrText`（D-06）。
 * 由确认页「保存为笔记附件」创建；打印时按 `createdAt DESC`、同时间按 `order` 取最近 3 条。
 */
@Entity(tableName = "note_attachment_item")
data class NoteAttachmentItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: String,
    val sourceFeedbackId: Long?, // 来源 ReviewFeedback；可为空
    val text: String, // 可编辑修正文本 correctedText 的快照
    val imageUri: String?, // 原始笔记图片/文件 URI；可为空
    val order: Int, // 同一题内的展示顺序
    val createdAt: Instant,
)
