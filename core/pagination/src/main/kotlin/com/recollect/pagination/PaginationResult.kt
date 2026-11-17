package com.recollect.pagination

import java.time.LocalDate

/**
 * 分页结果（C-047）：失败不混入 pages/warnings，调用方必须显式处理 Failure。
 */
sealed interface PaginationResult {
    data class Success(
        val pages: List<PlannedPage>,
        val overflowItems: List<OverflowItem>,
        val warnings: List<PaginationWarning>,
    ) : PaginationResult

    data class Failure(
        val code: PaginationFailureCode,
        val message: String,
    ) : PaginationResult
}

enum class PaginationFailureCode { INVALID_TEMPLATE_GEOMETRY, PLAN_TOO_LONG }

data class PlannedPage(
    val pageId: String, // "{planId}-{pageNumber:02d}"
    val pageNumber: Int, // 1..99，全文档连续
    val date: LocalDate,
    val items: List<PlacedItem>,
    val continuationMarkers: List<ContinuationMarker>, // 内部移页元数据；C-063 起不再渲染“续下页”
)

data class PlacedItem(
    val questionId: String,
    val yOffsetMm: Double, // 从页面顶边起算（含 topMarginMm）
    val heightMm: Double,
)

/**
 * 内部移页元数据（C-063）：仅用于分页结果自检与后续统计，不在纸上绘制“续下页”文本。
 * 用户可见的每日题数由打印层按 `PlannedPage.items` 统计，不依赖本类型。
 */
data class ContinuationMarker(
    val fromPageId: String,
    val toPageId: String,
    val questionId: String,
    val order: Int, // 当页内从 0 递增，按题目顺序
)

data class OverflowItem(
    val questionId: String,
    val date: LocalDate,
    val heightMm: Double,
    val reason: OverflowReason,
)

enum class OverflowReason { ITEM_TOO_TALL, ATTACHMENT_OVER_LIMIT }

/**
 * 打印警告（C-064、C-065）：
 * - `code` 是稳定代码，只给程序/日志使用；界面必须映射为中文标签，不直接展示枚举名。
 * - `message` 是给用户看的中文文案。
 * - 分页器只产出 overflow 两类；`KNOWLEDGE_TEXT_OVERFLOW` 由计划层产出并与本列表合并。
 */
data class PaginationWarning(
    val questionId: String,
    val code: PaginationWarningCode,
    val message: String,
)

enum class PaginationWarningCode {
    /** 分页器产出：整题高于内容区。 */
    ITEM_TOO_TALL,

    /** 分页器产出：附加笔记面积超过模板上限。 */
    ATTACHMENT_OVER_LIMIT,

    /** 计划层产出并合并（C-064）；分页器不产出该代码。 */
    KNOWLEDGE_TEXT_OVERFLOW,
}
