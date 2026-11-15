package com.recollect.pagination

import java.time.LocalDate

/**
 * 分页规划器（docs/specs/pagination.md）。只返回映射，不写数据库（R-02）。
 */
interface PagePlanner {
    fun plan(
        planId: String,
        dailyGroups: Map<LocalDate, List<RenderableQuestion>>,
        template: TemplateGeometry,
    ): PaginationResult
}
