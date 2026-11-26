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
    FORM_NUMBER,
    FORM_PAGE_CODE,
    FORM_MINUTES,
    FORM_SOURCE,
    FORM_KNOWLEDGE,
    FORM_SAVE,
    ENTRY_PLAN,
    PLAN_PRINT,
    PRINT_DEMO,
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
        body = "点击高亮的「录入题目」，我们用一道初中数学示例题开始。",
        mode = DemoStopMode.WAIT_TAP,
        target = DemoTarget.HOME_RECORD,
    ),
    FORM_NUMBER(
        stage = DemoTourStage.RECORD,
        title = "填写题号",
        body = "题号已预填为 12；实际使用时按教辅或试卷上的题号填写即可。",
        mode = DemoStopMode.NEXT,
        target = DemoTarget.FORM_NUMBER,
    ),
    FORM_PAGE_CODE(
        stage = DemoTourStage.RECORD,
        title = "填写页码编号",
        body = "页码编号 42 用来定位题目所在的位置；若本题来自试卷，则此处数字代表试卷编号，用 +/− 修改。",
        mode = DemoStopMode.NEXT,
        target = DemoTarget.FORM_PAGE_CODE,
    ),
    FORM_MINUTES(
        stage = DemoTourStage.RECORD,
        title = "预估复习时长",
        body = "这道题预计需复习 6 分钟。拾知按每天可用的复习时长把题目排进计划。",
        mode = DemoStopMode.NEXT,
        target = DemoTarget.FORM_MINUTES,
    ),
    FORM_SOURCE(
        stage = DemoTourStage.RECORD,
        title = "选择题目来源",
        body = "来源用于在复习时找到题目在哪。示例选择了《人教版九年级数学上册》。",
        mode = DemoStopMode.NEXT,
        target = DemoTarget.FORM_SOURCE,
    ),
    FORM_KNOWLEDGE(
        stage = DemoTourStage.RECORD,
        title = "关联知识点",
        body = "拾知按知识点掌握度安排复习。示例题关联了「一元二次方程」；实际使用时可以再关联其他知识点。",
        mode = DemoStopMode.NEXT,
        target = DemoTarget.FORM_KNOWLEDGE,
    ),
    FORM_SAVE(
        stage = DemoTourStage.RECORD,
        title = "保存这道题",
        body = "确认无误后，点击高亮的「保存」。保存后会回到题库。",
        mode = DemoStopMode.WAIT_TAP,
        target = DemoTarget.FORM_SAVE,
    ),
    ENTRY_PLAN(
        stage = DemoTourStage.PLAN,
        title = "第 2 步 · 生成计划",
        body = "题目已进入题库。点击高亮的「预览计划」，让拾知按知识点到期情况排出一份计划。",
        mode = DemoStopMode.WAIT_TAP,
        target = DemoTarget.ENTRY_PLAN,
    ),
    PLAN_PRINT(
        stage = DemoTourStage.PLAN,
        title = "计划已生成",
        body = "到期的知识点已被排进本周计划。点击高亮的「打印计划」，查看 A4 打印稿。",
        mode = DemoStopMode.WAIT_TAP,
        target = DemoTarget.PLAN_PRINT,
    ),
    PRINT_DEMO(
        stage = DemoTourStage.PRINT,
        title = "第 3 步 · 打印计划",
        body = "演示时不连接打印机，点击「使用演示打印稿」继续。",
        mode = DemoStopMode.WAIT_TAP,
        target = DemoTarget.PRINT_DEMO,
        tip = "真实使用时，点击这里会打开系统打印面板；同时提供借助微信等平台分享PDF打印稿的功能。",
    ),
    SCAN_SHEET(
        stage = DemoTourStage.SCAN,
        title = "第 4 步 · 扫描回读",
        body = "扫描框里已经放好已填涂完毕的演示打印稿。点击「识别这张打印稿」。",
        mode = DemoStopMode.WAIT_TAP,
        target = DemoTarget.SCAN_SHEET,
        tip = "本次演示不调用相机；真实使用时，需要在此页面使用相机逐页拍摄打印稿。",
    ),
    SCAN_CONFIRM(
        stage = DemoTourStage.SCAN,
        title = "识别完成",
        body = "拾知读出了填涂结果。点击「进入确认页」，逐题校对这份计划的识别结果。",
        mode = DemoStopMode.WAIT_TAP,
        target = DemoTarget.SCAN_CONFIRM,
    ),
    CONFIRM(
        stage = DemoTourStage.CONFIRM,
        title = "第 5 步 · 确认结果",
        body = "逐行点击「确认」接受识别结果（或修改复习效果），再点底部「确认提交」。",
        mode = DemoStopMode.FINISH,
        target = DemoTarget.CONFIRM_CONTENT,
        completedBody = "确认已提交。你已经走完一次完整的复习流程，在拾知的陪伴下开始你的高效学习之旅吧！",
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
