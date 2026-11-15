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
    val continuationMarkers: List<ContinuationMarker>, // 按 order 升序
)

data class PlacedItem(
    val questionId: String,
    val yOffsetMm: Double, // 从页面顶边起算（含 topMarginMm）
    val heightMm: Double,
)

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

data class PaginationWarning(
    val questionId: String,
    val code: String,
    val message: String,
)
