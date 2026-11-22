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
    /**
     * 左侧填涂区宽度占比。
     *
     * 2026-11-22 用户要求把填涂框上方的 A/B/C 改为「掌握 / 较熟悉 / 不会」并保持文字间距：
     * 标签宽于 5.7mm 填涂框，不能靠缩小字号塞进框内；改为扩大框间距到 3.1mm，
     * 左侧占比由 0.40 调到 0.42 以容纳三列加宽后的间距（知识点列仍保持 34mm）。
     * 打印与扫描必须共用本类，不得在任一模块另写常量。
     */
    val leftRatio: Double = 0.42,
    val textPaddingMm: Double = 3.0, // 右上文本区内边距
    val headerRowHeightMm: Double = 7.0, // 每页左侧表头，每页扣除一次
    val rowHeightMm: Double = 7.5, // 每知识点行高
    /**
     * 左侧列几何：序号列、知识点列与列间距。
     *
     * 2026-11-22 用户要求把填涂格改为答题卡式 16:9 宽格，序号列由 8mm 收窄到 6mm、
     * 列间距由 2mm 收窄到 1.5mm，为三格腾出宽度；知识点列保持 34mm 不截断名称。
     * 打印与扫描必须共用本类，不得在任一模块另写常量。
     */
    val indexColumnWidthMm: Double = 6.0,
    val knowledgeColumnWidthMm: Double = 34.0,
    val columnGapMm: Double = 1.5,
    /** 填涂格宽高（5.7 : 3.2 ≈ 16:9），横向更长便于填涂。 */
    val bubbleWidthMm: Double = 5.7,
    val bubbleHeightMm: Double = 3.2,
    /**
     * 三格之间的水平间距（2026-11-22 用户要求由 1.55mm 扩大到 3.1mm）。
     *
     * 表头文字「掌握 / 较熟悉 / 不会」宽 6–9mm，大于单格宽度；扩大间距后相邻文字
     * 仍各有约 1.3mm 空白，不会挤在一起。OMR 采样中心随 [bubbleCenterXMm] 一起平移。
     */
    val bubbleGapMm: Double = 3.1,
    /** OMR 单框采样窗边长；必须小于 [bubbleHeightMm]，保证落在框内。 */
    val bubbleSampleSizeMm: Double = 2.5,
    val questionFixedInfoHeightMm: Double = 7.0, // 右上第一行
    val noteAreaBaseHeightMm: Double = 30.0,
    val noteTitleHeightMm: Double = 5.0, // 笔记区「复习笔记」标题高度；书写区在其下方
    val attachmentHeightPerItemMm: Double = 10.0,
    val itemSpacingMm: Double = 3.0,
    val qrSizeMm: Double = 18.0,
    val qrPaddingMm: Double = 3.0,
    /** C-095 方案 B：QR 下方 pageId 标签的高度（2.2mm 字号，视觉行高 4mm）。 */
    val pageIdLabelHeightMm: Double = 4.0,
    /** pageId 标签与 QR 的垂直间距；2026-11-21 用户要求缩短为标签高度的一半。 */
    val pageIdLabelGapMm: Double = 2.0,
    val maxAttachmentHeightRatio: Double = 0.30, // C-027
    val dateHeadingFontMm: Double = 5.0, // 日期大标题字号（C-063）
    val dateHeadingHeightMm: Double = 7.0, // 日期大标题行高（C-063）
    val cornerReserveMm: Double = 0.0, // C-091 方案 A：四角标记预留
) {
    /** 题块页脚：QR 上方留白 + QR + 标签间距 + 标签高度。 */
    val qrFooterHeightMm: Double
        get() = qrSizeMm + qrPaddingMm + pageIdLabelGapMm + pageIdLabelHeightMm

    /** 内容区左边界；需越过四角标记白边（C-091 方案 A）。 */
    val contentLeftMm: Double
        get() = horizontalMarginMm + cornerReserveMm

    /** 内容区上边界（每页表头从此处开始）；需越过四角标记白边（C-091 方案 A）。 */
    val contentTopMm: Double
        get() = topMarginMm + cornerReserveMm

    val usableHeightMm: Double
        get() = pageHeightMm - topMarginMm - bottomMarginMm - 2 * cornerReserveMm

    val contentWidthMm: Double
        get() = pageWidthMm - 2 * horizontalMarginMm - 2 * cornerReserveMm

    val leftAreaWidthMm: Double
        get() = contentWidthMm * leftRatio

    /** 第 [column] 个填涂格（0=A、1=B、2=C）在页面上的左边界（mm）。 */
    fun bubbleLeftMm(column: Int): Double =
        contentLeftMm + indexColumnWidthMm + columnGapMm +
            knowledgeColumnWidthMm + columnGapMm +
            column * (bubbleWidthMm + bubbleGapMm)

    /** 第 [column] 个填涂格中心 x（mm）；OMR 采样以此为准。 */
    fun bubbleCenterXMm(column: Int): Double = bubbleLeftMm(column) + bubbleWidthMm / 2

    /**
     * 三个填涂格中心 x（掌握 / 较熟悉 / 不会）。
     *
     * 随计划几何快照冻结（C-093 方案 A 扩展）：扫描端优先读取快照，
     * 这样即使打印与扫描之间的模板列宽发生变化，旧稿仍按旧中心回读。
     */
    val bubbleCentersXMm: List<Double>
        get() = List(BUBBLE_COLUMNS) { bubbleCenterXMm(it) }

    val rightAreaWidthMm: Double
        get() = contentWidthMm * (1 - leftRatio)

    /** 右上文本区实际可用宽度（扣除内边距）；打印层按此宽度换行。 */
    val rightTextWidthMm: Double
        get() = rightAreaWidthMm - 2 * textPaddingMm

    val contentHeightMm: Double
        get() = usableHeightMm - headerRowHeightMm

    val maxAttachmentHeightMm: Double
        get() = usableHeightMm * maxAttachmentHeightRatio

    /** 日期大标题高度；绘制在上边距内，不占内容区（C-063；2026-11-22 删除每日总题数）。 */
    val dateHeadingBlockHeightMm: Double
        get() = dateHeadingHeightMm

    companion object {
        /** 一行填涂格数量：3（掌握 / 较熟悉 / 不会）。 */
        const val BUBBLE_COLUMNS: Int = 3

        /** ArUco 内码 10mm + 白边 4mm，距页边 12mm；内容区需内缩到 26mm（C-091 方案 A）。 */
        const val PRINT_CORNER_RESERVE_MM: Double = 14.0

        /** 生产打印模板：A4 默认值 + 四角标记预留（C-091 方案 A）。 */
        fun a4Print(): TemplateGeometry = TemplateGeometry(
            cornerReserveMm = PRINT_CORNER_RESERVE_MM,
        )
    }
}
