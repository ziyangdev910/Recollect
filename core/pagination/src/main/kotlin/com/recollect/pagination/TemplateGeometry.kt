package com.recollect.pagination

/**
 * 打印模板几何（docs/specs/pagination.md）。
 * 默认值对应 A4 纵向、左右 12mm 页边距；所有派生量均为毫米。
 *
 * 本轮实现高度/QR/附件上限派生量与 C-063 的日期标题几何；宽度派生量
 * （contentWidthMm 等）留待对应测试先红后绿时补充，不在本轮顺手实现。
 */
data class TemplateGeometry(
    val pageWidthMm: Double = 210.0,
    val pageHeightMm: Double = 297.0,
    val horizontalMarginMm: Double = 12.0, // 左右页边距
    val topMarginMm: Double = 12.0,
    val bottomMarginMm: Double = 12.0,
    val leftRatio: Double = 0.40,
    val textPaddingMm: Double = 3.0, // 右上文本区内边距
    val legendHeightMm: Double = 8.0, // 首页图例；仅文档首页扣除
    val headerRowHeightMm: Double = 7.0, // 每页左侧表头，每页扣除一次
    val rowHeightMm: Double = 7.5, // 每知识点行高
    val questionFixedInfoHeightMm: Double = 7.0, // 右上第一行
    val noteAreaBaseHeightMm: Double = 30.0,
    val attachmentHeightPerItemMm: Double = 10.0,
    val itemSpacingMm: Double = 3.0,
    val qrSizeMm: Double = 18.0,
    val qrPaddingMm: Double = 3.0,
    val maxAttachmentHeightRatio: Double = 0.30, // C-027
    val dateHeadingFontMm: Double = 5.0, // 日期大标题字号（C-063）
    val dateHeadingHeightMm: Double = 7.0, // 日期大标题行高（C-063）
) {
    val qrFooterHeightMm: Double
        get() = qrSizeMm + qrPaddingMm

    val usableHeightMm: Double
        get() = pageHeightMm - topMarginMm - bottomMarginMm

    val firstPageUsableHeightMm: Double
        get() = usableHeightMm - legendHeightMm

    val contentHeightMm: Double
        get() = usableHeightMm - headerRowHeightMm

    val firstPageContentHeightMm: Double
        get() = firstPageUsableHeightMm - headerRowHeightMm

    val maxAttachmentHeightMm: Double
        get() = usableHeightMm * maxAttachmentHeightRatio

    /** 每日总题数字号：日期大标题字号的一半（C-063）。 */
    val dailyTotalFontMm: Double
        get() = dateHeadingFontMm / 2

    /** 每日总题数高度：日期大标题行高的一半（C-063）。 */
    val dailyTotalHeightMm: Double
        get() = dateHeadingHeightMm / 2

    /** 日期大标题 + 每日总题数合计高度；打印层默认绘制在上边距内，不占内容区（C-063）。 */
    val dateHeadingBlockHeightMm: Double
        get() = dateHeadingHeightMm + dailyTotalHeightMm
}
