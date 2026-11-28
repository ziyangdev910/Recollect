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
    data class Entry(val categoryId: String? = null, val categoryName: String? = null) : AppRoute
    // 录入/编辑题目自 2026-11-28 起改为吸附在页面底部的抽屉，不再占用路由。
    data object KnowledgePoints : AppRoute
    data object SourceTags : AppRoute
    data object Progress : AppRoute
    data class CategoryCreate(
        val categoryId: String? = null,
        val name: String? = null,
        val coverTone: Int? = null,
        val coverIcon: String? = null,
    ) : AppRoute
    data object PlanPreview : AppRoute
    data object Scan : AppRoute
    data class Confirm(val sessionId: String) : AppRoute
    data object Settings : AppRoute
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
| Home | 错题本网格首页（2026-11-27 用户要求，按 reference/MainWindow.png 重做）：三列卡片网格，每个知识点分类一张卡片（显示该分类题目数，并配对应的淡色学科图标：语文书本、数学计算器、英语翻译、物理闪电、化学烧瓶、生物叶片、政治建筑、历史时钟、地理地球、信息技术电脑、通用技术扳手；自定义分类按名称关键词匹配，未命中用通用书本图标），末尾「添加分类」卡进入添加分类页（选择封面并创建分类）；长按任一分类卡片直接进入编辑态（不再弹长按菜单）：卡片轻微抖动并可拖动换位，卡片换位时播放位置交换动画，编辑态隐藏「添加分类」，每张卡片右上角从左到右为白底圆圈的笔按钮（进入预填的「编辑封面」页，按钮为「保存」）与红色圆底白叉按钮（分类题目数为 0 时弹紧凑确认浮窗后删除，题目数大于 0 时只提示不可删除；仓储仍拒绝仍有知识点的分类），顶栏右侧变为「完成」，退出编辑后两个按钮消失；打开「编辑封面 / 添加分类」页时首页保持在下方组合并继续抖动，封面页作为覆盖整个应用的全屏新页面打开（自带状态栏内边距，顶栏不收起，内容不位移），关闭或保存返回时首页原样露出，不会重建编辑态或出现展示态闪帧，同时重读分类使封面/名称等修改立即显示（离开首页去其他页面才退出），顺序写入 `sortOrder`；底部固定胶囊栏「录入 | 打印计划 | 扫描回读」（「录入」进入新建题目，「打印计划」进入打印计划页，「扫描回读」进入扫描页）；标题「拾知」左侧为三横线图标（透明底，点击打开左侧抽屉），右下角保留 `v{versionName}`；左侧抽屉顶部为应用 logo、名称与版本号，条目为「复习进度 / 每日复习时长 / 导入数据库 / 导出数据库 / 使用引导 / 关于」（每条都有图标，抽屉宽度约占屏幕 74% 并压在顶栏与底部胶囊栏之上），不再提供「知识点与分类」入口；原学习概览、复习流程条、快捷操作卡与首页设置面板删除；每次回到首页或应用恢复前台时重读题目总数与各分类数量；首次启动的交互式复习演示仍从首页「录入」按钮开始 | 分类读取失败（占位卡）、题目统计读取失败（显示“—”） |
| Progress | 复习进度可视化（只读）：题目总数/已逾期/今天到期/7 天内统计（描边统计面板）、未来 14 天到期分布（圆角柱图，今天用主色）、逐题来源/页码/题号/上次复习/最早到期与每个知识点的档位、连续掌握度、下次到期日、冷启动或半衰期；「题目进度」列表上方提供与错题本列表一致的筛选胶囊：科目（全部科目 + 当前题目涉及的分类，点开后展开与「添加题目」页一致、按首页分类封面底色着色的小卡片）与排序（按到期日 / 最近修改 / 最早修改），有筛选时显示「清除」，标题右侧显示筛选后的题目数；筛选与排序只作用于题目列表，统计与未来分布保持全量；题目卡片用共享 `formatQuestionLabelLines` 生成两行标题（无来源题目显示「第X份试卷 第Y题」），不再按 `-` 拆分来源字符串，滚动到无来源题目不会崩溃；空题库显示教学式空态 | 空题库、无来源题目、无匹配筛选、加载失败（可重试） |
| Entry | 错题本页（2026-11-27 用户要求，按 reference/EntryWindow.png 重做）：从首页分类卡片进入时只显示该分类下的题目（`categoryId`，null 为「全部错题」）；筛选胶囊「来源 / 知识点 / 最近录入·最早录入」横向可滚动，有筛选时在胶囊右侧固定显示「清除」，搜索按钮位于同一行最右侧并在上方展开搜索框；来源与知识点点开后在同一行下方展开与「添加题目」页一致的标签小卡片（来源选中用主色、知识点选中用赭石色，含「全部来源 / 全部知识点」和勾选标记），时间排序仍为胶囊下拉（2026-11-28 删除「全部错题 / 知识分类」选项卡，列表固定按日期分组）；题目卡片按「来源」与「第X页 第Y题」两行标题（16sp `titleMedium`，与复习进度页一致）+ 耗时/知识点标签展示，扁平列表按修改时间倒序 / 正序排列（无日期分组标签，切换排序时原地平滑重排），卡片右下角以中性色显示按当前列表顺序生成的临时「#N」并以「— 到底了 —」收尾，卡片右上「更多」提供编辑与删除（归档）；底部胶囊栏只保留「录入 | 打印」（去掉复习），「打印」进入打印计划页；空本子显示教学式空态与「录入第 1 道题」；搜索与列表滚动位置在进入编辑页后返回时保留（切换错题本时重置筛选） | 空错题本、无匹配、加载失败 |
| 录入 / 编辑抽屉（不再占用路由） | 吸附在内容区底部、盖住底部悬浮胶囊栏的抽屉，背景压暗。自上而下：题目卡片实时预览（与错题本列表同款，无「更多」与临时序号）、并排的科目与来源、第X页 / 第Y题 / 复习时长三个并排紧凑圆角文本框、位于最下方的知识点；底部中间悬浮「取消 / 保存」胶囊（样式同主页底栏）。点开科目、来源、知识点时在并排行或选择行下方整行展开选项，同一时刻只展开一个；「管理来源」「管理知识点」常驻在选择框内、箭头左侧。来源未选时来源行显示「未选择（来自试卷）」、页码框显示「第X份试卷」；选中来源后页码框显示「第X页」。科目只筛选知识点、不持久化；未选科目时不预选任何分类、候选中也没有「全部科目」，再次点击已选科目可清除；从具体分类错题本进入新建时科目预填为该分类，从主页进入时不填。编辑复用同一抽屉。保存成功后带对勾 Toast「保存成功」并收起抽屉：Home 入口保存后进入该题所属科目的错题本页（优先显式科目，未选科目时取首个关联知识点分类，无法解析回退全部错题页；返回键回 Home），Entry 入口保存后留在原错题本页并静默刷新（2026-11-28 用户要求） | 关联超限、已打印旧稿提示、来源标签库为空、当前科目无知识点、复习时长超出 1～240 |
| KnowledgePoints | 顶部为横向分类筛选选项卡（「全部」+ 各分类），再下方一行依次为「共 N 个标签」、新增标签；编号/名称/所属分类列表（按用户修改时间倒序，新增/编辑后立即置顶，2026-11-24 用户要求），编辑（可改名称与分类）、删除；新增/编辑标签必须选择分类；分类筛选与顶部导航栏间距收紧为 8dp（2026-11-23 用户要求）；2026-11-24 用户要求删去搜索框；2026-11-27 用户要求删除「管理分类」入口，分类新增/封面由首页「添加分类」与编辑态卡片右上角笔按钮承担 | 被题目引用不可删、删除确认、无分类时无法新增标签 |
| SourceTags | `AppRoute.SourceTags` 独立页面（顶部标题「来源标签」）：名称列表、新增、rename、删除确认（2026-11-24 用户要求删去搜索框）；2026-11-24 起首页不再提供「管理题目来源」入口，路由与页面保留（录入抽屉「来源」框内的「管理来源」仍可打开全屏窗口增删改查） | 被题目引用不可删、名称重复、空库 |
| 来源标签（录入抽屉内全屏窗口） | 导航栏与「知识点」页一致（左侧语义关闭图标 + 标题的 TopAppBar）；名称列表、新增、rename、删除确认（2026-11-24 用户要求删去搜索框）；关闭后抽屉保留未保存内容 | 被题目引用不可删、名称重复、空库 |
| CategoryCreate | 「添加分类 / 编辑封面」页（2026-11-27 用户要求，参照 reference/CardEditWindow.png）：作为覆盖整个应用的独立全屏页面打开，自带状态栏与导航栏内边距；首页顶栏保留在其下方、不收起，进入/返回时页面内容不发生位移；页面自带左上关闭与右上「创建」（编辑已有分类时为「保存」，表单预填名称与封面）；中部为所选封面的实时预览（名称、0 道题、淡色提示图标）；下方「学习本名称」输入框；「选择封面」4 列网格，提供 16 个由常用底色与提示图标组成的封面（书本、计算器、翻译、闪电、烧瓶、叶片、建筑、时钟、地球、电脑、扳手、通用书本，以及音乐、美术、体育、写作），图标位于每格右下角；页面收紧预览与间距，首屏至少显示两行封面；不提供「我的封面」与「上传封面」；创建/保存成功后写入 `coverTone`/`coverIcon` 并返回首页，首页按所选封面渲染该分类 | 名称为空（创建禁用）、重名（错误提示）、创建/保存失败 |
| PlanPreview | 「打印计划」页（2026-11-27 用户要求）：打印稿预览一直铺到页面底部，顶部紧凑显示「打印稿预览」与计划号/页数/题数；页面底部悬浮胶囊栏与首页底部三个按钮一致（日期起止 + 向上的小箭头 | 分享 PDF，操作项为无填充的图标 + 文字，中间以细分隔线分开），胶囊悬浮在打印稿之上而不是占用整条矩形区域；点击日期区域打开「计划日期」卡片——开始/结束两个日期字段可直接切换当前编辑的一端，日历直接嵌在卡片内（不弹二级日期框），选中日期时即时更新草稿，确认后加载该范围已有计划或生成；日期范围最多 7 天，默认按本周计划状态填入本周或下周（本周 COMPLETED 填下一自然周，否则填当前自然周；2026-11-28 恢复原有规则）；计划生成后在页面内实时渲染可打印 PDF 的逐页预览（`PlanDocumentGateway.preview`，与分享 PDF 使用同一份 A4 布局）；打开已有计划时（进入页默认范围加载、手动切换日期范围、或切回已有范围），始终检测题库与复习时长：任一未归档题目在计划 `createdAt` 之后被新增/编辑（修改题目的预估复习时长同样刷新 `updatedAt`）、计划内已排入/推迟/告警的题目被归档，或「每日复习时长」与计划头记录的生成值不同，弹出「题库已更新 / 复习时长已更新 / 计划已过期，是否重新生成计划？」确认框（可保持当前计划）；「重新生成计划」二次确认保留；被推迟题目与 overflow 警告用语义色卡片展示；分享成功后（页面重新获得焦点且快照写入成功）退回首页并提示「打印完成」；原独立打印页删除 | 容量不足、`KNOWLEDGE_TEXT_OVERFLOW`、`ITEM_TOO_TALL`、`INVALID_TEMPLATE_GEOMETRY`、`PLAN_TOO_LONG`、被推迟题目、PDF 预览/分享失败 |
| Scan | 全屏扫描回读页（2026-11-27 用户要求，按 reference/CaptureWindow.png / CaptureWindow2.png 重做）：深色全屏相机取景（`CAMERA` 权限，拒绝时相册回退），三分法参考线 + 中央「平行纸面拍照 / 题目对齐参考线」，左上角关闭；底部左右为相册与「完成」，中间为快门（`ScanCameraController`）；始终为多页录入，不区分「录单页 / 录多页」；录入一页或多页后，中央显示「请拍第 N 页」，页面显示已扫页数/行数与带页数徽标的缩略图，右下「完成 ›」进入确认页；识别期间显示进度胶囊，QR 类失败可手动输入 `pageId + 题号`；首次演示的 `demoSheet` 在取景框展示预生成打印稿、不调用相机（见「首次复习演示」） | 一页一拍、`PAGE_*` 错误、重复页提示、相机权限拒绝 |

> 警告代码（`PaginationWarningCode`）只给程序/日志使用；PlanPreview 必须把代码映射为中文标签，并展示给用户看的 `message`，不得直接显示枚举名（C-065 方案 B）。`KNOWLEDGE_TEXT_OVERFLOW` 由计划层产出并与分页警告合并（C-064 方案 A）。
>
> 题目标识统一为「来源 第X页 第Y题」（X 为页码编号、Y 为题号；没有来源说明时按试卷份数显示「第X份试卷」，无来源页码时只显示「第Y题」），同时用于计划页面卡片与“未排入计划”卡片，避免不同来源页上的相同题号无法区分；格式化函数 `formatQuestionLabel` 位于 `core:common`，计划预览与打印稿共用（2026-11-25 用户要求）。推迟卡片读 `PlanPreviewDeferred`：生成时随 `plan.deferred` 冻结 `questionId/题号/来源页码/原因`，重开计划从快照重建；旧快照缺来源页码时从当前题库兜底补齐展示（不改写快照）。
| Confirm | plan 级多页确认、批次实际复习日期、逐行决策；顶部显示整批确认进度条、已标记/总行数与剩余行数；每行用语义色标签展示触发/关联、识别结果与最终决策，决策完成后收起操作按钮、只保留最终选择与对勾；底部提交面板显示剩余行数并承载唯一提交按钮；整批全部提交成功后退回首页并以带对勾 Toast 提示「确认完成」（2026-11-26 用户要求）；首次演示中提交成功不退回首页、不弹该 Toast，只把说明条「开始使用」置为可用，点击后还原数据库（见「首次复习演示」） | `UNMARKED`、`LATE_BATCH`、未来日期行只能 NOT_REVIEWED |
| Settings | 每日分钟、计划周、扫描窗口、保留策略 | 见 `settings.md` |
| Backup | 由 Home 左侧抽屉中的「导出数据库」打开悬浮在首页上方的导出弹窗（图标与导入互换后显示上传图形，2026-11-22 用户要求；入口 2026-11-26 从顶部导航栏移入设置面板；同日由独立页面改为设置风格弹窗——Material 3 `AlertDialog`，样式同「每日复习时长」弹窗，底部「关闭」与系统返回键均可关闭，不显示返回图标，不进入路由返回栈，关闭后再次打开回到空闲状态）；弹窗顶部为导出说明与通栏「导出并分享」主按钮；导出完成后展示文件名、大小、备份格式版本与分享结果（不展示导出范围与各实体行数，2026-11-22 用户要求）；分享失败可只重试分享。导入入口为首页左侧抽屉中的「导入数据库」（图标互换后显示下载图形）：SAF 选文件或系统「用其他方式打开」json 文件，校验合法后弹「将覆盖当前数据，是否继续？」，确认后覆盖并以带对勾 Toast 提示「导入成功」，同时刷新首页/题库/知识点/来源/复习进度/计划预览的数据缓存 | 见 `backup.md` 的导出/导入状态机（`IDLE → EXPORTING → EXPORTED/ERROR`；`IDLE → READING → CONFIRMING → IMPORTING → SUCCESS/ERROR`） |
| About | 由 Home 左侧抽屉中的「关于」进入（2026-11-26 移入设置面板；`AppRoute.About`，顶部标题「关于」）：展示应用图标、名称「拾知」、版本号、作者 `@ziyangdev910`、开源仓库 `https://github.com/ziyangdev910/recollect`（整行可点，右侧箭头，在系统浏览器打开）、开源许可证 `GPL-3.0 with Classpath Exception`（SPDX：`GPL-3.0-only WITH Classpath-exception-2.0`，整行可点进入 `AppRoute.License`）与简要功能说明；底部「致谢」卡片说明记忆调度算法在思想上受 Jingyong Su 等人提出的 SSP-MMC-Plus 工作启发，附论文完整引用（DOI 可点击打开网页），并声明记忆模型公式参考墨墨团队以 MIT 许可开源的 SSP-MMC 实现、排程与工程代码为独立编写且未复制或改编其实现代码；只读，不申请新权限（2026-11-23 用户要求；致谢与开源许可证栏目 2026-11-24 用户要求；2026-11-25 信息列表重排） | 版本号读取失败（显示 `unknown`） |
| License | 由 About 页「查看许可证全文」进入（`AppRoute.License`，顶部标题「开源许可」）：三个选项卡分别展示打包在 `app/src/main/assets/licenses/` 的 GPL-3.0 全文（`LICENSE.txt`）、Classpath 例外（`LICENSE-EXCEPTION.txt`）与 NOTICE（`NOTICE.txt`）；正文等宽小字号、整页可滚动；只读，不申请新权限（2026-11-24 用户要求） | 资源读取失败（显示错误说明，不崩溃） |

## 首次复习演示（首次启动）

2026-11-26 用户要求；与普通复习流程使用同一套页面与用例，不改变调度、打印、扫描与确认的数据语义。

### 触发与数据

- **触发**：两种情况：①`AppRoot` 首次组合时，若 `first_run_onboarding` 中 `review_flow_demo_completed` 为 false，且本次从 Home 启动、没有系统「用其他方式打开」json 的导入意图，进入演示；②用户在首页左侧抽屉点击「使用引导」手动重看。演示期间拦截系统返回键；任意时刻可点说明条上的「跳过演示」。
- **准备**：`DemoDataStore` 用 `DatabaseExporter` 把当前数据库导出为 JSON 快照（`filesDir/first-run-demo/restore.json`），再用 `DatabaseImporter` 覆盖导入 `app/src/main/assets/demo/seed.json`。种子只含 11 个默认分类、2 个数学知识点与 1 个来源标签，**不预置题目与计划**；每日复习时长 60 分钟。
- **还原**：点「开始使用」或「跳过演示」时用同一快照覆盖导入恢复演示前数据库；成功后写完成标记并直接回到空首页。不显示独立「演示结束」页，不弹「示例数据已清除」Toast。
- **恢复**：演示中进程被杀时快照保留；下次启动先还原快照再重新准备演示，避免示例数据残留。准备失败时导入整体回滚并写完成标记、跳过演示；还原失败时保留快照并提供「重试清理」与「暂时隐藏」。
- **手动重看**：从首页左侧抽屉「使用引导」进入时，仍先导出当前数据库快照、临时导入示例数据，结束或跳过时原样还原当前题库、计划与设置；欢迎卡显示「您的数据不会丢失」提示。手动重看中途进程被杀时，下次启动同样先还原快照。

### 界面与交互

- 进入演示时先显示居中的欢迎卡（标题「欢迎使用拾知」）：说明接下来用 5 步从录入一道题开始，完整走一遍生成计划、打印、扫描回读与确认；背景全屏压暗、不挖镂空。点「开始引导」进入第 1 步，点「跳过演示」直接还原。欢迎卡不计入 5 步。手动重看时，欢迎卡额外显示「您的数据不会丢失：演示期间会临时使用示例数据，结束后自动恢复当前题库、计划与设置。」
- `DemoTourOverlay` 作为 `AppRoot` 根级最后子节点，叠加在所有页面与弹窗之上；顶部说明条位于内容区顶部，高度随文字自适应，内容区按对应高度留出 padding，真实页面从说明条下方开始。
- 说明条使用紧凑排版：`labelSmall` 进度、`titleSmall` 标题、`bodySmall` 正文（17sp 行高）、36dp 主按钮，跳过入口为紧凑文字；垂直内边距 8dp、元素间距 4dp，尽量减小内容区为它让出的高度。
- 蒙层约 70% 黑（`0xB3000000`），只在目标控件处挖出圆角镂空（even-odd 路径一次绘制，描边同圆角）；圆角按目标类型适配：文本框 6dp、整卡 14dp、来源/知识点分组 0dp（直角，与确认结果一致）、底部胶囊按钮取高度的一半、确认页整块内容 0dp。镂空内不挂 `pointerInput`，用户可直接点击真实控件，其余区域由透明命中层拦截触摸。目标包括录入抽屉字段、抽屉「保存」胶囊、首页「录入」、错题本「打印」、打印计划「演示打印稿」、扫描「识别这张打印稿」「完成，进入确认页」。
- 说明条显示「第 N / 5 步 + 阶段名」、标题、说明与「跳过演示」。文字段（`NEXT`）提供「下一步」移动聚焦；操作段（`WAIT_TAP`）不提供推进按钮，等用户点击真实控件；第五步（`FINISH`）的「开始使用」默认置灰，提交成功后可用。
- 表单聚焦顺序：题号 → 页码 → 复习时长 → 来源 → 知识点 → 「保存」；聚焦变化时用 `BringIntoViewRequester` 把目标滚动到说明条下方的可见区域。抽屉为底部胶囊预留了独立高度，聚焦字段与选择行只会滚到胶囊上方，不会被「取消 / 保存」遮挡（2026-11-28 用户要求）。进入演示时首页自动把「录入题目」卡滚到可见位置，演示结束后首页回到顶部。

### 五步演示

> 欢迎卡确认后从第 1 步开始；以下每一步都由用户点击真实控件推进。

1. **录入**：首页聚焦「录入」按钮；点击后在首页底部打开录入抽屉，表单由 `QuestionEditPrefill` 预填（题号 12、页码 42、来源《人教版九年级数学上册》、知识点「一元二次方程」、6 分钟）。用户点「保存」后收起抽屉并进入示例题所属的数学错题本页。
2. **生成**：错题本页聚焦底部「打印」，点击进入「打印计划」页（`AppRoute.PlanPreview`）并按保存的题目生成计划（示例为 1 页 1 题）。
3. **打印**：「打印计划」页在演示焦点下渲染计划与打印稿预览，底部悬浮栏的主按钮为「演示打印稿」。点击后 `PlanDocumentGateway.recordDemoPrinted()` 走与分享 PDF 相同的 `PrintSnapshotWriter` 回写；`DemoSheetRenderer` 用同一份 `PrintPlanInput` 经 `PrintLayoutBuilder`/`PrintRenderer` 渲染 300 DPI A4 稿，在填涂框内涂黑（示例为「掌握」），写入 `cacheDir/demo-sheets/demo-page.png` 并经 `FileProvider` 生成 content URI。
4. **扫描**：扫描页在演示模式（`demoSheet != null`）于全屏取景框内展示该打印稿，不打开相机、不请求 `CAMERA`；点击「识别这张打印稿」仍走 `ScanViewModel.onImageSelected` 与真实 QR/OMR/OCR 管线。识别成功后收起识别按钮，改为「完成，进入确认页」。
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

- Confirm 顶部显示 `PlanCompletion`：计划页数、已扫页数、已复习条数、漏复习条数、晚复习条数（见 `repository.md`）；首页自 2026-11-26 起不再显示计划卡。
- `MISSED` 只展示派生结果，不落库。
- 未扫描页在扫描窗口内提示“还有 N 页未扫描”；超过 `planEnd+7` 标 `LATE_BATCH`。

## 全局错误与恢复

- 所有 Repository/UseCase 错误映射为 `UiError(code, message, retryable)`；UI 显示可读文案和“重试/返回”。
- 进程重建：`ScanSession`/`PendingFeedback`/`Confirm` 状态可恢复；`PlanPreview` 可重新生成。
- Entry → 录入抽屉 → 收起：底层错题本复用 ViewModel 与 `LazyListState`，不重新加载、保留滚动位置；保存成功后先弹带对勾 Toast「保存成功」，再静默刷新列表并收起抽屉，停留在原 Entry（2026-11-28 用户要求）。
- Home → 录入抽屉（「录入题目」入口）：保存成功后先弹带对勾 Toast「保存成功」，再进入该题所属科目的错题本页（`navigator.push(AppRoute.Entry(categoryId, categoryName))`；优先显式科目，未选科目时取首个关联知识点分类，无法解析时回退全部错题页），返回键回到 Home，抽屉同时收起（2026-11-28 用户要求）。
- 录入抽屉内「管理来源」：来源标签库以全屏窗口（`Dialog`）打开，不进入返回栈；关闭后仅刷新来源候选，题号/页码/来源选择/知识点/复习时长等未保存内容保持原样。
- 录入抽屉内「管理知识点」：知识点标签库以全屏窗口打开；关闭后刷新标签与分类候选，未保存内容与科目筛选保持原样（失效的科目回到「全部科目」）。
- 录入抽屉打开期间：点击压暗区域、按返回键或点「取消」只收起抽屉；首次演示的还原/跳过也会同时收起抽屉。
- Home → About / Progress / 每日复习时长 / 导入 / 导出 / 使用引导：全部收进首页左侧抽屉（2026-11-28 从右上角「更多」菜单移入；「知识点与分类」入口删除）；导出仍打开悬浮弹窗、不进入返回栈，导入仍走 SAF。
- Home → SourceTags：2026-11-24 起首页不再提供「管理题目来源」入口；`AppRoute.SourceTags` 独立页面（顶部标题「来源标签」）与返回栈行为保留，来源标签仍需在录入/编辑页内以全屏窗口增删改查。
- Home → PlanPreview：计划日期范围（最多 7 天）内已有 GENERATED/PRINTED/COMPLETED 计划时复用同一计划号；不会再次点击入口就生成新计划。只有生成页的「重新生成计划」经确认后才生成新计划号并替代该范围的旧计划（C-097 的复用语义保持不变，窗口粒度由固定两周改为用户选择的范围）。计划头记录生成时的每日复习时长（`plan.dailyMinutes`，schema v17）；修改设置后再次进入时提示重新生成，旧计划该值为 0（未知）时不提示。题库在生成后变化仍沿用原有提示。
- 分享 PDF：系统分享面板打开即视为文档已导出，按 `PrintSnapshotWriter` 的同一入口写入 pageId/QR 并标记 PRINTED（2026-11-27 起不再有「已打印」确认）；计划已被替代时仍生成文件，但提示“未更新打印快照”，首页状态不变。分享成功后，页面重新获得焦点且快照写入成功时退回首页并提示「打印完成」。
- 生成页内的打印稿预览与分享 PDF 使用同一份 A4 布局（`PlanDocumentGateway.preview`/`share`）；预览失败不影响分享，页面以错误卡片提示。
- 不在 ViewModel 持有 `Context`；文件/相机/PDF 渲染操作通过 UseCase/Repository/端口注入。

## 验收

- 冷启动 → Home 无计划空态；录入 1 题 1 知识点 → 生成计划 → 分享 PDF → 扫描 → 确认 → Home 数量更新。
- 旋转/进程重建后 Confirm 的批次日期与逐行决策不丢。
- 返回栈、权限拒绝、SAF 取消、PDF 生成失败均有明确恢复路径。
- 首次启动演示：预填表单保存 → 生成计划 → 演示打印 → 真实扫描稿识别 → 确认提交 → 「开始使用」还原空库；「跳过演示」同样还原；重启不再出现；演示中强停后重启先还原再重演。
