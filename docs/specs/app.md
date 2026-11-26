# App 壳、UI 与导航规格（app）

关联决策：D-10、D-11、D-13
关联裁决：C-046、C-052
关联规格：`settings.md`、`planning.md`、`repository.md`、`backup.md`、`scan-pipeline.md`、`confirm-flow.md`

## 模块图

```text
:app
 ├─ :feature:entry
 ├─ :feature:planning
 ├─ :feature:progress
 ├─ :feature:print
 ├─ :feature:scan
 ├─ :feature:confirm
 ├─ :feature:settings
 ├─ :feature:backup
 ├─ :data:backup
 ├─ :data:repository
 ├─ :data:room
 ├─ :core:model
 ├─ :core:common
 ├─ :core:scheduler
 └─ :core:pagination
```

依赖方向固定为 `app → feature → data → core`；`core:scheduler`/`core:pagination` 仍禁止 `android.*`。

- `core:common`：`Clock` 抽象、`PlanIdGenerator`、`KnowledgeTextMeasurer` 接口、通用 `Result`/错误类型；纯 Kotlin。`KnowledgeTextMeasurer` 的实现放在 `feature/print`。
- `core:model`：纯 DTO/枚举与 `KnowledgeRowsCodec`（见 `repository.md`）；Room Entity ↔ DTO 映射在 `data:room`；不依赖 Android（C-046 方案 A）。
- 测试栈：纯 Kotlin 与 Android 单元测试用 JUnit 5；`androidTest` 用 JUnit 4（C-052 方案 C）。

## 构建与打包

- debug：`isMinifyEnabled = false`、`isShrinkResources = false`，保持类名与堆栈可读，便于调试。
- release：`isMinifyEnabled = true`、`isShrinkResources = true`，规则见 `app/proguard-rules.pro`（OpenCV JNI 类保留）。
- ABI：只打包 `arm64-v8a` 与 `armeabi-v7a`，去掉 x86/x86_64 模拟器原生库以减小 APK；如需在 x86 模拟器运行，临时移除该过滤。
- 签名：release 读取仓库根目录 `keystore.properties`（已 gitignore）指向的正式 keystore；该文件缺失时回退 debug 签名，仅用于本地 `adb install`，不可分发。更换签名后旧安装无法覆盖，需先在应用内导出数据、卸载旧包、安装新包再导入。
- `debug_compile_and_install.bat`：构建 debug APK（`app/build/outputs/apk/debug/app-debug.apk`）并 `adb install -r` 到已连接的实机。
- `release_compile_and_install.bat`：构建 minify 后的 release APK（`app/build/outputs/apk/release/app-release.apk`）并 `adb install -r` 到已连接的实机。

## DI 与导航

- **不使用 Hilt/Koin**，避免引入设计决策外的新依赖（R-05）。`app` 内使用手写 `AppContainer` 持有 Repository、UseCase、Scheduler、PagePlanner、ScanEngine、TextMeasurer 等单例。
- **不引入 navigation-compose**。`AppNavigator` 用 `mutableStateOf<AppRoute>` + 简单返回栈实现；`BackHandler` 处理返回。
- `MainActivity` 只创建 `AppContainer`、`AppNavigator` 和 `AppRoot`；不直接访问 Room/CameraX/PrintManager/ML Kit（R-03）。
- 界面固定竖屏：`MainActivity` 声明 `android:screenOrientation="portrait"`，即使系统开启自动旋转也不切换横屏
  （2026-11-22 用户要求）。

```kotlin
sealed interface AppRoute {
    data object Home : AppRoute
    data object Entry : AppRoute
    data class QuestionEdit(val questionId: String?, val fromHome: Boolean = false) : AppRoute
    data object KnowledgePoints : AppRoute
    data object SourceTags : AppRoute
    data object Progress : AppRoute
    data object PlanPreview : AppRoute
    data object Print : AppRoute
    data object Scan : AppRoute
    data class Confirm(val sessionId: String) : AppRoute
    data object Settings : AppRoute
    data object Backup : AppRoute
    data object About : AppRoute
    data object License : AppRoute
}

class AppNavigator(initial: AppRoute = AppRoute.Home) {
    var current by mutableStateOf(initial); private set
    private val backStack = ArrayDeque<AppRoute>()
    fun push(route: AppRoute)
    fun pop(): Boolean
    fun replace(route: AppRoute)
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data class Error(val code: String, val message: String, val retryable: Boolean) : UiState<Nothing>
}
```

## 屏幕清单与状态

每个屏幕使用 `UiState`（`Loading` / `Content` / `Empty` / `Error`）与 `UiEvent`；错误必须可重试。

| 屏幕 | 内容 | 关键状态 |
|---|---|---|
| Home | 仪表盘式首页（2026-11-26 重排）：①页面最上方为「学习概览」卡：左侧「学习概览」、右侧透明背景的「查看复习进度」按钮，表面样式与字号（titleSmall / labelSmall）与复习流程卡标题行一致；标题行下方无分隔线，题目 / 知识点 / 已逾期 / 7 天内四项统计直接排列（读取失败显示“—”，不阻塞入口），卡片顶部到标题、标题到统计的间距与复习流程卡标题行到流程图一致（16dp / 14dp）（2026-11-26 用户要求）；②复习流程条——录入 → 生成 → 打印 → 扫描 → 确认，点亮当前环节并显示「第 N / 5 步」，流程图下方用一句话说明当前该做什么（操作指引）；③流程条下方直接排列四张快捷操作卡片（录入题目 / 管理题库 / 预览计划 / 扫描回读），不再显示「常用操作」区块标题：卡片不显示副标题，左侧图标块，右侧纵向右对齐显示按钮标签与可选的「下一步」标签；卡片高度由内容决定（同一行用 `IntrinsicSize.Min` 对齐），不再有固定高度或按钮底部留白（2026-11-26 用户要求）；当前环节对应的入口被移到首位、使用主色容器并显示「下一步」，其余入口保持固定顺序——录入时前置「录入题目」，生成/打印时前置「预览计划」，扫描/确认时前置「扫描回读」（2026-11-26 用户要求）；首页不再出现本周/下周计划卡，计划窗口改由计划预览页选择；④设置面板：上方为「每日复习时长」行（右侧箭头）；下方以分割线隔开，并排四个等宽入口——「导入数据库」/「导出数据库」/「使用引导」/「关于」（图标在上、文字在下，各占面板总宽度的四分之一；标题使用 labelSmall 且不设水平内边距，保证 1.3 倍字号下完整显示），版本号不再放在「关于」行副标题中；点击「使用引导」会重新播放首次复习演示，演示前快照当前数据库、结束后原样恢复，欢迎卡额外提示「您的数据不会丢失」（2026-11-26 用户要求）；首页标题「拾知」右下角以 labelSmall 字号与 `onSurfaceVariant` 非强调色显示 `v{versionName}`（2026-11-26 用户要求）；顶部导航栏不再提供「导入数据库」「导出数据库」入口（2026-11-26 用户要求，取代 2026-11-22 的导航栏入口）；首页允许滚动；每次回到首页或应用恢复前台都重读计划状态与统计；首次启动时叠加交互式首次复习演示（见「首次复习演示」） | 无计划、计划已完成、待扫描、统计加载失败、`LATE_BATCH`、每日时长非法值 |
| Progress | 复习进度可视化（只读）：题目总数/已逾期/今天到期/7 天内统计（描边统计面板）、未来 14 天到期分布（圆角柱图，今天用主色）、逐题题号/来源页/上次复习/最早到期与每个知识点的档位、连续掌握度、下次到期日、冷启动或半衰期；空题库显示教学式空态 | 空题库、加载失败（可重试） |
| Entry | 题库页（2026-11-25 重排）：顶部搜索框带搜索图标与清除按钮（题号/页码/知识点，大小写不敏感），下方「共 N 道题 / 匹配 X / N」与「按最近修改排序」；题目卡片以大标题分两行展示来源与「第X页 第Y题」（无来源说明时第一行为「编号X」、第二行为「第Y题」），下方耗时标签与知识点标签并排，整卡点击进入编辑，右侧保留编辑/删除按钮（2026-11-25 用户要求）；列表按 `updatedAt` 倒序；空题库显示教学式空态与「录入第 1 道题」按钮（此时隐藏悬浮按钮），非空时右下角纵向排列两个 Extended FAB：「录入题目」使用中性容器淡化，其下「生成计划」使用主色容器高亮（已有题目时生成计划是下一步，2026-11-26 用户要求）；搜索无结果显示查询词与「清除搜索」 | 空题库、无匹配、加载失败 |
| QuestionEdit | 题号、编号（未选来源时标题「试卷编号」，选中来源后标题「页码编号」，2026-11-24 用户要求）与复习时长三栏并排、各占三分之一，题号示例放入占位符、时长单位「分钟」放在输入区后缀（2026-11-25 用户要求）；来源（单选标签，可空）、关联知识点（≤4）、笔记附件；「知识点标签」标题下为横向分类筛选选项卡（「全部」+ 各分类，2026-11-23 用户要求），筛选卡与下方标签条目之间以分割线分隔，标签卡只显示当前分类下的知识点；保存改为页面底部通栏主按钮（48dp），保存成功后先以带对勾 Toast 提示「保存成功」，再按入口分流：从 Home 的「录入题目」进入（`fromHome = true`）时以题库页替换当前页（返回键回 Home），从 Entry 进入时关闭当前页返回 Entry（2026-11-24 用户要求） | 关联超限、已打印旧稿提示、来源标签库为空、当前分类无知识点 |
| KnowledgePoints | 顶部为横向分类筛选选项卡（「全部」+ 各分类），再下方一行依次为「共 N 个标签」、管理分类、新增标签；编号/名称/所属分类列表（按用户修改时间倒序，新增/编辑后立即置顶，2026-11-24 用户要求），编辑（可改名称与分类）、删除；「管理分类」可新增/重命名/删除分类（有知识点的分类不可删）；新增/编辑标签必须选择分类；分类筛选与顶部导航栏间距收紧为 8dp（2026-11-23 用户要求）；2026-11-24 用户要求删去搜索框 | 被题目引用不可删、删除确认、分类有知识点不可删、分类名称重复、无分类时无法新增标签 |
| SourceTags | `AppRoute.SourceTags` 独立页面（顶部标题「来源标签」）：名称列表、新增、rename、删除确认（2026-11-24 用户要求删去搜索框）；2026-11-24 起首页不再提供「管理题目来源」入口，路由与页面保留（录入页内的来源标签窗口仍可增删改查） | 被题目引用不可删、名称重复、空库 |
| 来源标签（录入页内全屏窗口） | 导航栏与「知识点」页一致（左侧语义关闭图标 + 标题的 TopAppBar）；名称列表、新增、rename、删除确认（2026-11-24 用户要求删去搜索框）；关闭后录入页保留未保存内容 | 被题目引用不可删、名称重复、空库 |
| PlanPreview | 顶部「本周计划 / 下周计划」选项栏（选中项高亮，2026-11-26 用户要求）：进入时若本周计划已 COMPLETED（完成确认）则默认选中下周计划，否则选中本周计划；切换时加载该窗口已有计划，没有则生成；若加载的是已有计划且题库在计划生成后发生变化（题目新增/编辑，或计划内的题目被归档），加载后弹出「题库已更新，是否重新生成计划？」——确认走重新生成，取消则保持当前计划且同一周不再重复询问；其下为计划摘要卡（计划号、页数/题数与通栏「打印计划」主按钮）；「每日安排」列表按页显示为「日期栏（日/月/星期）+ 题目标签」卡片；overflow 警告与“有 N 道题未排入本次计划”分别用错误/朱砂容器展示，统一用「来源 第X页 第Y题」标识题目（X 为页码编号、Y 为题号；没有来源说明时显示「编号X 第Y题」，无来源页码时只显示「第Y题」），推迟卡片逐条给出题目标识、中文原因与处理建议；按当前选项显示“重新生成本周计划/重新生成下周计划”的二次确认（C-098），入口降到页面底部 | 容量不足、`KNOWLEDGE_TEXT_OVERFLOW`、`ITEM_TOO_TALL`、`INVALID_TEMPLATE_GEOMETRY`、`PLAN_TOO_LONG`、被推迟题目（冷却期/复习时间不足/知识点缺失） |

> 警告代码（`PaginationWarningCode`）只给程序/日志使用；PlanPreview 必须把代码映射为中文标签，并展示给用户看的 `message`，不得直接显示枚举名（C-065 方案 B）。`KNOWLEDGE_TEXT_OVERFLOW` 由计划层产出并与分页警告合并（C-064 方案 A）。
>
> 题目标识统一为「来源 第X页 第Y题」（X 为页码编号、Y 为题号；没有来源说明时页码显示为「编号X」，无来源页码时只显示「第Y题」），同时用于计划页面卡片与“未排入计划”卡片，避免不同来源页上的相同题号无法区分；格式化函数 `formatQuestionLabel` 位于 `core:common`，计划预览与打印稿共用（2026-11-25 用户要求）。推迟卡片读 `PlanPreviewDeferred`：生成时随 `plan.deferred` 冻结 `questionId/题号/来源页码/原因`，重开计划从快照重建；旧快照缺来源页码时从当前题库兜底补齐展示（不改写快照）。
| Print | 由 PlanPreview 进入；计划摘要卡（计划号、页数/题数、每日安排）+ 通栏「打开系统打印」主按钮与「分享 PDF」次按钮；系统打印返回后把「纸张打印好了吗？」确认卡提升为主内容（已打印/未打印/重新打开打印）；「分享 PDF」把同一份 A4 布局写成缓存 PDF 并打开系统分享面板（微信、QQ 等），分享成功后与「已打印」一样写入 pageId/QR 并把 GENERATED 计划标记为 PRINTED（2026-11-24 用户要求）；确认「已打印」写回成功后立即退回首页并以带对勾 Toast 提示「打印完成」；「分享 PDF」打开系统面板后，页面重新获得焦点且快照已写入时同样退回首页并提示「打印完成」（2026-11-26 用户要求）；首次演示的 `demoMode` 不显示系统打印与分享，改为「使用演示打印稿」，走同一快照回写后生成演示扫描稿（见「首次复习演示」） | 打印中、失败、等待「已打印/未打印」确认、PDF 生成中、分享失败；确认「已打印」或分享成功后只为当前计划写 pageId/QR，SUPERSEDED 计划不写（C-094 方案 B、C-097） |
| Scan | 顶部流程说明（拍摄 → 识别 → 确认）；相机/相册、当前 `ScanSession`、已扫页；识别成功后以主色结果卡突出「进入确认页」，权限/错误/结果提示使用语义色容器；首次演示的 `demoSheet` 模式在扫描框展示预生成打印稿、不调用相机，识别仍走真实 QR/OMR/OCR 管线（见「首次复习演示」） | 一页一拍、`PAGE_*` 错误、重复页提示 |
| Confirm | plan 级多页确认、批次实际复习日期、逐行决策；顶部显示整批确认进度条、已标记/总行数与剩余行数；每行用语义色标签展示触发/关联、识别结果与最终决策，决策完成后收起操作按钮、只保留最终选择与对勾；底部提交面板显示剩余行数并承载唯一提交按钮；整批全部提交成功后退回首页并以带对勾 Toast 提示「确认完成」（2026-11-26 用户要求）；首次演示中提交成功不退回首页、不弹该 Toast，只把说明条「开始使用」置为可用，点击后还原数据库（见「首次复习演示」） | `UNMARKED`、`LATE_BATCH`、未来日期行只能 NOT_REVIEWED |
| Settings | 每日分钟、计划周、扫描窗口、保留策略 | 见 `settings.md` |
| Backup | 由 Home「设置」面板中的「导出数据库」等宽入口打开悬浮在首页上方的导出弹窗（图标与导入互换后显示上传图形，2026-11-22 用户要求；入口 2026-11-26 从顶部导航栏移入设置面板；同日由独立页面改为设置风格弹窗——Material 3 `AlertDialog`，样式同「每日复习时长」弹窗，底部「关闭」与系统返回键均可关闭，不显示返回图标，不进入路由返回栈，关闭后再次打开回到空闲状态）；弹窗顶部为导出说明与通栏「导出并分享」主按钮；导出完成后展示文件名、大小、备份格式版本与分享结果（不展示导出范围与各实体行数，2026-11-22 用户要求）；分享失败可只重试分享。导入入口为设置面板中相邻的「导入数据库」等宽入口（图标互换后显示下载图形）：SAF 选文件或系统「用其他方式打开」json 文件，校验合法后弹「将覆盖当前数据，是否继续？」，确认后覆盖并以带对勾 Toast 提示「导入成功」，同时刷新首页/题库/知识点/来源/复习进度/计划预览的数据缓存 | 见 `backup.md` 的导出/导入状态机（`IDLE → EXPORTING → EXPORTED/ERROR`；`IDLE → READING → CONFIRMING → IMPORTING → SUCCESS/ERROR`） |
| About | 由 Home「设置」面板中的「关于」等宽入口进入（2026-11-26 移入设置面板；`AppRoute.About`，顶部标题「关于」）：展示应用图标、名称「拾知」、版本号、作者 `@ziyangdev910`、开源仓库 `https://github.com/ziyangdev910/recollect`（整行可点，右侧箭头，在系统浏览器打开）、开源许可证 `GPL-3.0 with Classpath Exception`（SPDX：`GPL-3.0-only WITH Classpath-exception-2.0`，整行可点进入 `AppRoute.License`）与简要功能说明；底部「致谢」卡片说明记忆调度算法在思想上受 Jingyong Su 等人提出的 SSP-MMC-Plus 工作启发，附论文完整引用（DOI 可点击打开网页），并声明记忆模型公式参考墨墨团队以 MIT 许可开源的 SSP-MMC 实现、排程与工程代码为独立编写且未复制或改编其实现代码；只读，不申请新权限（2026-11-23 用户要求；致谢与开源许可证栏目 2026-11-24 用户要求；2026-11-25 信息列表重排） | 版本号读取失败（显示 `unknown`） |
| License | 由 About 页「查看许可证全文」进入（`AppRoute.License`，顶部标题「开源许可」）：三个选项卡分别展示打包在 `app/src/main/assets/licenses/` 的 GPL-3.0 全文（`LICENSE.txt`）、Classpath 例外（`LICENSE-EXCEPTION.txt`）与 NOTICE（`NOTICE.txt`）；正文等宽小字号、整页可滚动；只读，不申请新权限（2026-11-24 用户要求） | 资源读取失败（显示错误说明，不崩溃） |

## 首次复习演示（首次启动）

2026-11-26 用户要求；与普通复习流程使用同一套页面与用例，不改变调度、打印、扫描与确认的数据语义。

### 触发与数据

- **触发**：两种情况：①`AppRoot` 首次组合时，若 `first_run_onboarding` 中 `review_flow_demo_completed` 为 false，且本次从 Home 启动、没有系统「用其他方式打开」json 的导入意图，进入演示；②用户在首页设置面板点击「使用引导」手动重看。演示期间拦截系统返回键；任意时刻可点说明条上的「跳过演示」。
- **准备**：`DemoDataStore` 用 `DatabaseExporter` 把当前数据库导出为 JSON 快照（`filesDir/first-run-demo/restore.json`），再用 `DatabaseImporter` 覆盖导入 `app/src/main/assets/demo/seed.json`。种子只含 11 个默认分类、2 个数学知识点与 1 个来源标签，**不预置题目与计划**；每日复习时长 60 分钟。
- **还原**：点「开始使用」或「跳过演示」时用同一快照覆盖导入恢复演示前数据库；成功后写完成标记并直接回到空首页。不显示独立「演示结束」页，不弹「示例数据已清除」Toast。
- **恢复**：演示中进程被杀时快照保留；下次启动先还原快照再重新准备演示，避免示例数据残留。准备失败时导入整体回滚并写完成标记、跳过演示；还原失败时保留快照并提供「重试清理」与「暂时隐藏」。
- **手动重看**：从首页设置面板「使用引导」进入时，仍先导出当前数据库快照、临时导入示例数据，结束或跳过时原样还原当前题库、计划与设置；欢迎卡显示「您的数据不会丢失」提示。手动重看中途进程被杀时，下次启动同样先还原快照。

### 界面与交互

- 进入演示时先显示居中的欢迎卡（标题「欢迎使用拾知」）：说明接下来用 5 步从录入一道题开始，完整走一遍生成计划、打印、扫描回读与确认；背景全屏压暗、不挖镂空。点「开始引导」进入第 1 步，点「跳过演示」直接还原。欢迎卡不计入 5 步。手动重看时，欢迎卡额外显示「您的数据不会丢失：演示期间会临时使用示例数据，结束后自动恢复当前题库、计划与设置。」
- `DemoTourOverlay` 作为 `AppRoot` 根级最后子节点，叠加在所有页面与弹窗之上；顶部说明条位于内容区顶部，高度随文字自适应，内容区按对应高度留出 padding，真实页面从说明条下方开始。
- 说明条使用紧凑排版：`labelSmall` 进度、`titleSmall` 标题、`bodySmall` 正文（17sp 行高）、36dp 主按钮，跳过入口为紧凑文字；垂直内边距 8dp、元素间距 4dp，尽量减小内容区为它让出的高度。
- 蒙层约 70% 黑（`0xB3000000`），只在目标控件处挖出圆角镂空（even-odd 路径一次绘制，描边同圆角）；圆角按目标类型适配：文本框 6dp、整卡 14dp、来源/知识点分组 0dp（直角，与确认结果一致）、Extended FAB 18dp、普通按钮取高度的一半（胶囊）、确认页整块内容 0dp。镂空内不挂 `pointerInput`，用户可直接点击真实控件，其余区域由透明命中层拦截触摸。目标包括录入页字段、表单「保存」、题库「预览计划」、计划「打印计划」、打印「使用演示打印稿」、扫描「识别这张打印稿」「进入确认页」。
- 说明条显示「第 N / 5 步 + 阶段名」、标题、说明与「跳过演示」。文字段（`NEXT`）提供「下一步」移动聚焦；操作段（`WAIT_TAP`）不提供推进按钮，等用户点击真实控件；第五步（`FINISH`）的「开始使用」默认置灰，提交成功后可用。
- 表单聚焦顺序：题号 → 页码编号 → 复习时长 → 来源 → 知识点 → 「保存」；聚焦变化时用 `BringIntoViewRequester` 把目标滚动到说明条下方的可见区域。进入演示时首页自动把「录入题目」卡滚到可见位置，演示结束后首页回到顶部。

### 五步演示

> 欢迎卡确认后从第 1 步开始；以下每一步都由用户点击真实控件推进。

1. **录入**：首页聚焦「录入题目」卡；点击后进入 `QuestionEdit`，表单由 `QuestionEditPrefill` 预填（题号 12、页码编号 42、来源《人教版九年级数学上册》、知识点「一元二次方程」、6 分钟）。用户点「保存」后按普通流程进入题库。
2. **生成**：题库页聚焦「预览计划」，点击进入 `PlanPreview` 并按保存的题目生成计划（示例为 1 页 1 题）。
3. **打印**：计划预览聚焦「打印计划」；打印页在演示模式（`demoMode = true`）不显示系统打印与分享按钮，改为「使用演示打印稿」。点击后 `PrintPlanViewModel.recordDemoPrinted()` 走与「已打印」相同的 `PrintSnapshotWriter` 回写；`DemoSheetRenderer` 用同一份 `PrintPlanInput` 经 `PrintLayoutBuilder`/`PrintRenderer` 渲染 300 DPI A4 稿，在填涂框内涂黑（示例为「掌握」），写入 `cacheDir/demo-sheets/demo-page.png` 并经 `FileProvider` 生成 content URI。
4. **扫描**：扫描页在演示模式（`demoSheet != null`）于扫描框内展示该打印稿，不打开相机、不请求 `CAMERA`；点击「识别这张打印稿」仍走 `ScanViewModel.onImageSelected` 与真实 QR/OMR/OCR 管线。识别成功后收起打印稿预览，结果卡与「进入确认页」置顶。
5. **确认**：进入确认页后聚焦整块内容区（保持完全可操作），说明条位于顶部；用户逐行「确认 / 修改复习效果 / 本次未复习」并点「确认提交」。提交成功后整页压暗（无镂空），只保留顶部说明条与「开始使用」；`onConfirmed` 在演示中只把「开始使用」置为可用，不退回首页、不显示「确认完成」Toast；点击「开始使用」后还原数据库并回到空首页。

> 演示示例只关联 1 个知识点，使确认页在单屏内可完成；种子仍保留第 2 个知识点供标签库展示。演示打印稿是渲染图而非真实纸张照片，不改变 R-01 的打印稿回读验收口径。

### 失败状态

- `PrepareFailed`：示例种子准备失败（未写入任何演示数据），说明卡提供「开始使用」；随后写完成标记，不会每次启动重试。
- `RestoreFailed`：快照还原失败，说明卡提供「重试清理」与「暂时隐藏」；隐藏后不写完成标记，下次启动继续尝试。
- `SheetFailed`：渲染演示打印稿失败，说明卡提供「重试生成打印稿」与「跳过演示」。

## 权限与文件

- `CAMERA`：仅扫描页使用；拒绝时允许相册选图。
- 文件导入/导出使用 SAF，不申请存储权限。「导出并分享」只写应用私有缓存并经 `FileProvider` 授予一次性读权限。
- 导入还支持系统 `ACTION_VIEW` + `application/json`（`android:label="导入到拾知数据库"`）：文件管理器以
  content URI 打开 json 时直接进入 `AppRoot` 的导入流程，跳过 SAF 选择；URI 由 `MainActivity` 处理后交给
  `ImportViewModel`，消费后立即清空以避免重复触发。该入口导入成功、Toast 显示后由 `MainActivity`
  调用 `finishAndRemoveTask()` 退出应用（2026-11-22 用户要求）；应用内 SAF 导入不退出。
- 笔记图片使用 Photo Picker 或 SAF，复制到 app 私有目录。
- 首次演示的已填涂打印稿写入应用缓存 `demo-sheets/`，经 `FileProvider` 交给本应用扫描页读取，不申请新权限。
- 不申请网络权限；端侧推理。

## 计划完成度 UI

- Home 的计划卡片与 Confirm 顶部都显示 `PlanCompletion`：计划页数、已扫页数、已复习条数、漏复习条数、晚复习条数（见 `repository.md`）。
- `MISSED` 只展示派生结果，不落库。
- 未扫描页在扫描窗口内提示“还有 N 页未扫描”；超过 `planEnd+7` 标 `LATE_BATCH`。

## 全局错误与恢复

- 所有 Repository/UseCase 错误映射为 `UiError(code, message, retryable)`；UI 显示可读文案和“重试/返回”。
- 进程重建：`ScanSession`/`PendingFeedback`/`Confirm` 状态可恢复；`PlanPreview` 可重新生成。
- Entry → QuestionEdit → 返回 Entry：复用 ViewModel 与 `LazyListState`，不重新加载、保留滚动位置；保存成功后先弹带对勾 Toast「保存成功」，再静默刷新列表并关闭当前页回到 Entry（2026-11-24 用户要求）。
- Home → QuestionEdit（「录入题目」入口，`fromHome = true`）：保存成功后先弹带对勾 Toast「保存成功」，再以 `replace(AppRoute.Entry)` 强制进入题库页（替换录入页，返回键回到 Home），不从录入页返回（2026-11-24 用户要求）。
- QuestionEdit 内「管理来源」：来源标签库以全屏窗口（`Dialog`）打开，不进入返回栈；关闭后仅刷新来源候选，题号/页码/来源选择/知识点/预估分钟等未保存内容保持原样。
- QuestionEdit 内「管理知识点」：知识点标签库（含分类管理）以全屏窗口打开；关闭后刷新标签与分类候选，未保存内容与分类筛选保持原样（失效的分类筛选回到「全部」）。
- Home → About：点击「设置」面板中的「关于」等宽入口进入关于页；返回由全局返回栈处理。首页「每日复习时长」卡片点击弹出悬浮窗，保存后写入 Room `app_settings` 并用于下一份计划。
- Home → Backup / Import：点击「设置」面板中的「导出数据库」在首页上方打开导出弹窗（不进入返回栈）；点击「导入数据库」用 SAF 选择文件（2026-11-26 起入口从顶部导航栏移入设置面板，导出改为悬浮弹窗）。
- Home → SourceTags：2026-11-24 起首页不再提供「管理题目来源」入口；`AppRoute.SourceTags` 独立页面（顶部标题「来源标签」）与返回栈行为保留，来源标签仍需在录入/编辑页内以全屏窗口增删改查。删除入口后进入 Entry/QuestionEdit 时仍会重新读取来源候选。
- Home → PlanPreview：本周或下周已有 GENERATED/PRINTED/COMPLETED 计划时进入预览复用同一计划号；不会再次点击首页按钮就生成新计划。只有预览页的“重新生成本周计划/重新生成下周计划”经确认后才生成新计划号并替代该窗口的旧计划（C-097/C-098）。
- 打印确认：App 内“已记录本次打印”只在计划仍为当前计划时出现；若计划已被重新生成替代，显示“本次计划已被新的计划替代，未更新打印快照。”
- 分享 PDF：系统分享面板打开即视为文档已导出，按与「已打印」相同的入口写入 pageId/QR 并标记 PRINTED；计划已被替代时仍生成文件，但提示“未更新打印快照”，首页状态不变（2026-11-24 用户要求）。
- PlanPreview → Print → 返回 PlanPreview 复用同一份计划，不重新生成、计划号不变；只有再次从 Home/Entry 点击某个窗口的生成入口才生成新计划。
- 不在 ViewModel 持有 `Context`；文件/相机/打印操作通过 UseCase/Repository 注入。

## 验收

- 冷启动 → Home 无计划空态；录入 1 题 1 知识点 → 生成计划 → 打印 → 扫描 → 确认 → Home 显示完成度。
- 旋转/进程重建后 Confirm 的批次日期与逐行决策不丢。
- 返回栈、权限拒绝、SAF 取消、打印失败均有明确恢复路径。
- 首次启动演示：预填表单保存 → 生成计划 → 演示打印 → 真实扫描稿识别 → 确认提交 → 「开始使用」还原空库；「跳过演示」同样还原；重启不再出现；演示中强停后重启先还原再重演。
