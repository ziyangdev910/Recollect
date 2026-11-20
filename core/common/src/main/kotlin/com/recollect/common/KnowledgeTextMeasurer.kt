package com.recollect.common

/**
 * 右上知识点全称的文本测量（print-template.md「文本测量与换行」）。
 *
 * 接口在 `core:common`，实现由打印层（`feature/print`）提供；计划层只用它得到视觉行数，
 * 不在分页器内猜换行（R-02）。
 */
fun interface KnowledgeTextMeasurer {
    /**
     * @param knowledgeNames 与左侧填涂行同一 printable 集合、按编号升序
     * @param widthMm 右上文本区宽度
     * @param fontMm 初始字号（毫米）
     * @return 换行后的视觉行数；测量实现不截断文本，超出上限由计划层给出警告
     */
    fun lineCount(knowledgeNames: List<String>, widthMm: Double, fontMm: Double): Int
}
