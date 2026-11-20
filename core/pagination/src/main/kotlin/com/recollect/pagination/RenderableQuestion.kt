package com.recollect.pagination

/**
 * 打印投影（docs/specs/pagination.md）：只保留打印所需字段，不含任何调度状态。
 *
 * @property knowledgeCount 1..4，只计 printable 知识点（!dormant && activeQuestionCount > 0）
 * @property knowledgeNames 全称，按 KnowledgePoint.number 升序（C-013/C-015）
 * @property knowledgeLineCount 右上全称换行后的视觉行数，由 feature/print 计算，本模块不猜测换行
 * @property attachmentCount 0..3
 * @property sourcePage 题目来源页码（试卷/作业本第几面），用于打印稿与确认页定位；不是 pageId
 */
data class RenderableQuestion(
    val questionId: String,
    val questionNumber: String,
    val knowledgeCount: Int,
    val knowledgeNames: List<String>,
    val knowledgeLineCount: Int,
    val attachmentCount: Int,
    val hasNoteArea: Boolean = true,
    val sourcePage: String = "",
)
