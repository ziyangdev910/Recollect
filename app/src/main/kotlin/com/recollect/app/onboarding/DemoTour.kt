package com.recollect.app.onboarding

/**
 * 首次打开应用时的复习流程演示（2026-11-26 用户要求）。
 *
 * 演示不是自动翻页：每一段都把黑底蒙层挖到真实控件上，由用户自己填写/点击，
 * 完成当前动作后才进入下一段。表单内容提前填好，扫描页展示一张按示例数据生成、
 * 已填涂的打印稿；演示结束或跳过时用启动前快照还原数据库。
 */
internal enum class DemoTourStage(val number: Int, val label: String) {
    RECORD(1, "录入题目"),
    PLAN(2, "生成计划"),
    PRINT(3, "打印计划"),
    SCAN(4, "扫描回读"),
    CONFIRM(5, "确认结果"),
    ;

    val progressLabel: String get() = "第 $number / 5 步"
}

/** 黑底蒙层要挖出的真实控件。 */
internal enum class DemoTarget {
    /** 欢迎卡片：不聚焦任何真实控件，只显示一张居中的说明卡。 */
    WELCOME,
    HOME_RECORD,
    FORM_SUBJECT,
    FORM_SOURCE,
    FORM_PAGE_CODE,
    FORM_NUMBER,
    FORM_MINUTES,
    FORM_KNOWLEDGE,
    FORM_SAVE,
    ENTRY_PLAN,
    PLAN_PRINT,
    HOME_SCAN,
    SCAN_SHEET,
    SCAN_CONFIRM,

    /** 确认页整块内容区：保持可操作，只把顶部说明条与四周压暗。 */
    CONFIRM_CONTENT,
}

/** 当前段如何前进。 */
internal enum class DemoStopMode {
    /** 文字说明段：点说明条上的「下一步」移动聚焦。 */
    NEXT,

    /** 操作段：等用户在真实页面点击高亮控件。 */
    WAIT_TAP,

    /** 第五步：用户提交确认后，说明条上的「开始使用」才可用。 */
    FINISH,
}

internal enum class DemoTourStop(
    val stage: DemoTourStage,
    val title: String,
    val body: String,
    val mode: DemoStopMode,
    val target: DemoTarget,
    val tip: String? = null,
    /** 第五步提交成功后的说明。 */
    val completedBody: String? = null,
) {
    WELCOME(
        stage = DemoTourStage.RECORD,
        title = "欢迎使用拾知！",
        body = "现在开始使用引导，带你从录入一道题开始，完整走一遍生成计划、打印、扫描回读和确认的完整复习流程。跟着提示操作即可，随时可以跳过。",
        mode = DemoStopMode.NEXT,
        target = DemoTarget.WELCOME,
    ),
    HOME_RECORD(
        stage = DemoTourStage.RECORD,
        title = "第 1 步 · 录入题目",
        body = "点击高亮的「录入题目」，开始录入第一道题。",
        mode = DemoStopMode.WAIT_TAP,
        target = DemoTarget.HOME_RECORD,
    ),
    FORM_SUBJECT(
        stage = DemoTourStage.RECORD,
        title = "选择科目",
        body = "先选择科目，此处选择会影响下方可关联的知识点。示例已选好「数学」（可在主页编辑科目列表）。",
        mode = DemoStopMode.NEXT,
        target = DemoTarget.FORM_SUBJECT,
        tip = "主页上的卡片与科目一一对应。可在主页点击“添加分类”来新增科目，或长按某一卡片来编辑科目名称及卡片背景、更换卡片顺序或删除科目卡片。", 
    ),
    FORM_SOURCE(
        stage = DemoTourStage.RECORD,
        title = "选择题目来源",
        body = "选择来源，以便复习时找到题目在哪本书中或哪张试卷上。示例选择了教材《人教版九年级数学上册》。",
        mode = DemoStopMode.NEXT,
        target = DemoTarget.FORM_SOURCE,
    ),
    FORM_PAGE_CODE(
        stage = DemoTourStage.RECORD,
        title = "填写页码",
        body = "当来源为某本书籍时，表示题目所在页的页码；当来源为试卷时，表示试卷的编号。",
        mode = DemoStopMode.NEXT,
        target = DemoTarget.FORM_PAGE_CODE,
    ),
    FORM_NUMBER(
        stage = DemoTourStage.RECORD,
        title = "填写题号",
        body = "按书本或试卷上的题号填写即可。",
        mode = DemoStopMode.NEXT,
        target = DemoTarget.FORM_NUMBER,
    ),
    FORM_MINUTES(
        stage = DemoTourStage.RECORD,
        title = "填写预估复习时长",
        body = "预估复习时长帮助拾知合理规划每日复习的题量。",
        mode = DemoStopMode.NEXT,
        target = DemoTarget.FORM_MINUTES,
    ),
    FORM_KNOWLEDGE(
        stage = DemoTourStage.RECORD,
        title = "关联知识点",
        body = "选择导致本题不会做掌握度最差的知识点，可选1-4个。",
        mode = DemoStopMode.NEXT,
        target = DemoTarget.FORM_KNOWLEDGE,
    ),
    FORM_SAVE(
        stage = DemoTourStage.RECORD,
        title = "保存这道题",
        body = "确认无误后，点击高亮的「保存」。保存后会回到该科目的题库。",
        mode = DemoStopMode.WAIT_TAP,
        target = DemoTarget.FORM_SAVE,
    ),
    ENTRY_PLAN(
        stage = DemoTourStage.PLAN,
        title = "第 2 步 · 生成计划",
        body = "点击此处或主页上的「打印」按钮，让拾知按知识点到期情况生成一份可打印的逐日计划。",
        mode = DemoStopMode.WAIT_TAP,
        target = DemoTarget.ENTRY_PLAN,
    ),
    PLAN_PRINT(
        stage = DemoTourStage.PRINT,
        title = "第 3 步 · 打印计划",
        body = "在此页预览生成的 PDF 打印稿，点击「分享 PDF」按钮，可把PDF文件分享到微信、QQ 等社交平台。",
        mode = DemoStopMode.WAIT_TAP,
        target = DemoTarget.PLAN_PRINT,
        tip = "演示中将跳过分享、打印过程。",
    ),
    HOME_SCAN(
        stage = DemoTourStage.SCAN,
        title = "第 4 步 · 从首页扫描",
        body = "分享成功后将自动回到首页。点击底部的「扫描」按钮进入扫描页。",
        mode = DemoStopMode.WAIT_TAP,
        target = DemoTarget.HOME_SCAN,
    ),
    SCAN_SHEET(
        stage = DemoTourStage.SCAN,
        title = "扫描演示稿",
        body = "演示打印稿已经放进取景框，点击下方快门按钮，拍摄并识别这张打印稿。",
        mode = DemoStopMode.WAIT_TAP,
        target = DemoTarget.SCAN_SHEET,
        tip = "真实使用时，可在此页面逐页拍摄并识别多张打印稿。",
    ),
    SCAN_CONFIRM(
        stage = DemoTourStage.SCAN,
        title = "识别完成",
        body = "在识别完成后，点击右下角「完成」按钮，进入确认页，逐题校对识别结果是否正确。",
        mode = DemoStopMode.WAIT_TAP,
        target = DemoTarget.SCAN_CONFIRM,
    ),
    CONFIRM(
        stage = DemoTourStage.CONFIRM,
        title = "第 5 步 · 确认结果",
        body = "逐行校对复习反馈与笔记识别结果是否正确，完成后点击“确认”按钮；全部完成后，点击界面右下角的「完成」按钮，结束确认过程。",
        mode = DemoStopMode.FINISH,
        target = DemoTarget.CONFIRM_CONTENT,
        completedBody = "一次完整的复习流程就是这样，在拾知的陪伴下开始你的高效学习之旅吧！",
    ),
    ;

    fun next(): DemoTourStop? = entries.getOrNull(ordinal + 1)
}

/** 首次演示的界面状态。 */
internal sealed interface DemoTourState {
    /** 未在演示（首次已看过、或准备失败后跳过）。 */
    data object Inactive : DemoTourState

    /** 正在导出原库快照并载入最小演示种子。 */
    data object Preparing : DemoTourState

    /** 正在根据计划生成扫描用的演示打印稿。 */
    data object PreparingScanSheet : DemoTourState

    /** 正在逐段引导用户操作。 */
    data class Running(val stop: DemoTourStop) : DemoTourState

    /** 正在还原启动前的数据库快照。 */
    data object Restoring : DemoTourState

    /** 还原失败：保留快照，下次启动会再次尝试。 */
    data class RestoreFailed(val message: String) : DemoTourState

    /** 示例数据准备失败：没有写入任何演示数据，直接跳过演示。 */
    data class PrepareFailed(val message: String) : DemoTourState

    /** 演示打印稿生成失败：可重试或跳过演示。 */
    data class SheetFailed(val message: String) : DemoTourState
}

/** 演示是否占据屏幕（用于拦截返回键与内容区留白）。 */
internal val DemoTourState.blocksInteraction: Boolean
    get() = this !is DemoTourState.Inactive
