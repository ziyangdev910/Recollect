package com.recollect.pagination

/**
 * 打印模板几何（docs/specs/pagination.md）。
 * 默认值对应 A4 纵向、左右 12mm 页边距；所有派生量均为毫米。
 *
 * 本类实现高度/QR/附件上限派生量、C-063 的日期标题几何，以及
 * `pagination.md` 定义的宽度派生量（计划层用 `rightTextWidthMm` 测量右上名称）。
 *
 * C-091 方案 A：`cornerReserveMm` 为四角定位标记预留的额外内缩量。生产打印模板用
 * [a4Print]，在 12mm 页边距基础上再内缩 14mm，使内容区从 26mm 开始，
 * 避开 ArUco 内码（10mm）与其 4mm 白边；`cornerReserveMm = 0` 保持原有纯几何语义。
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
    val noteTitleHeightMm: Double = 5.0, // 笔记区「复习笔记」标题高度；书写区在其下方
    val attachmentHeightPerItemMm: Double = 10.0,
    val itemSpacingMm: Double = 3.0,
    val qrSizeMm: Double = 18.0,
    val qrPaddingMm: Double = 3.0,
    val maxAttachmentHeightRatio: Double = 0.30, // C-027
    val dateHeadingFontMm: Double = 5.0, // 日期大标题字号（C-063）
    val dateHeadingHeightMm: Double = 7.0, // 日期大标题行高（C-063）
    val cornerReserveMm: Double = 0.0, // C-091 方案 A：四角标记预留
) {
    val qrFooterHeightMm: Double
        get() = qrSizeMm + qrPaddingMm

    /** 内容区左边界；需越过四角标记白边（C-091 方案 A）。 */
    val contentLeftMm: Double
        get() = horizontalMarginMm + cornerReserveMm

    /** 内容区上边界（首页图例从此处开始）；需越过四角标记白边（C-091 方案 A）。 */
    val contentTopMm: Double
        get() = topMarginMm + cornerReserveMm

    val usableHeightMm: Double
        get() = pageHeightMm - topMarginMm - bottomMarginMm - 2 * cornerReserveMm

    val firstPageUsableHeightMm: Double
        get() = usableHeightMm - legendHeightMm

    val contentWidthMm: Double
        get() = pageWidthMm - 2 * horizontalMarginMm - 2 * cornerReserveMm

    val leftAreaWidthMm: Double
        get() = contentWidthMm * leftRatio

    val rightAreaWidthMm: Double
        get() = contentWidthMm * (1 - leftRatio)

    /** 右上文本区实际可用宽度（扣除内边距）；打印层按此宽度换行。 */
    val rightTextWidthMm: Double
        get() = rightAreaWidthMm - 2 * textPaddingMm

    val contentHeightMm: Double
        get() = usableHeightMm - headerRowHeightMm

    val firstPageContentHeightMm: Double
        get() = firstPageUsableHeightMm - headerRowHeightMm

    val maxAttachmentHeightMm: Double
        get() = usableHeightMm * maxAttachmentHeightRatio

    /**
     * 每日总题数字号：与首页图例同字号（3.2mm）。
     *
     * 2026-11-18 按用户要求由「日期大标题字号的一半」改为与图例一致；
     * 高度仍保持 [dailyTotalHeightMm]，因此与日期大标题的间距不变。
     */
    val dailyTotalFontMm: Double
        get() = ANNOTATION_FONT_MM

    /** 每日总题数高度：日期大标题行高的一半（C-063）。 */
    val dailyTotalHeightMm: Double
        get() = dateHeadingHeightMm / 2

    /** 日期大标题 + 每日总题数合计高度；打印层默认绘制在上边距内，不占内容区（C-063）。 */
    val dateHeadingBlockHeightMm: Double
        get() = dateHeadingHeightMm + dailyTotalHeightMm

    companion object {
        /** ArUco 内码 10mm + 白边 4mm，距页边 12mm；内容区需内缩到 26mm（C-091 方案 A）。 */
        const val PRINT_CORNER_RESERVE_MM: Double = 14.0

        /** 图例与每日总题数共用的注记字号（mm）。 */
        const val ANNOTATION_FONT_MM: Double = 3.2

        /** 生产打印模板：A4 默认值 + 四角标记预留（C-091 方案 A）。 */
        fun a4Print(): TemplateGeometry = TemplateGeometry(
            cornerReserveMm = PRINT_CORNER_RESERVE_MM,
        )
    }
}
