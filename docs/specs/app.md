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
| Home | 仪表盘式首页（2026-11-25 重排）：自上而下为①计划主卡——本周为主区块（真实本周窗口、短日期、语义色状态胶囊、状态说明与通栏主按钮“生成本周计划/预览并打印本周计划”），卡内下周为次要行（日期、compact 状态胶囊、48dp 文本入口）；本周计划 COMPLETED 时交换主次（下周升为主区块，本周降为“已完成/预览本周计划”），其余状态保持本周为主，只提供本周与下周两个窗口（C-098/C-099/C-100）；②复习流程条——录入 → 生成 → 打印 → 扫描 → 确认，按计划状态点亮当前环节，右侧显示「下一步：录入题目 / 生成计划 / 打印计划 / 扫描回读」，计划完成时提示开始下一轮；③常用操作 2×2 卡片（录入题目 / 管理题库 / 扫描回读 / 查看复习进度），图标 + 标题 + 一行说明；首页任意时刻只有一张引导卡使用主色容器：当前环节为录入时高亮「录入题目」、为扫描时高亮「扫描回读」，为生成/打印或本轮完成（主卡已聚焦下一周）时高亮计划主卡，其余卡片退回中性（2026-11-25 用户要求）；④学习概览四栏（题目 / 知识点 / 已逾期 / 7 天内，读取失败显示“—”，不阻塞入口）；⑤设置面板（每日复习时长、关于两行，右侧箭头，分割线区分）；顶部导航栏右侧保留「导入数据库」「导出数据库」（图标互换后的下载/上传图形）；首页允许滚动 | 无计划、计划已完成、待扫描、统计加载失败、`LATE_BATCH`、每日时长非法值 |
| Progress | 复习进度可视化（只读）：题目总数/已逾期/今天到期/7 天内统计（描边统计面板）、未来 14 天到期分布（圆角柱图，今天用主色）、逐题题号/来源页/上次复习/最早到期与每个知识点的档位、连续掌握度、下次到期日、冷启动或半衰期；空题库显示教学式空态 | 空题库、加载失败（可重试） |
| Entry | 题库页（2026-11-25 重排）：顶部搜索框带搜索图标与清除按钮（题号/页码/知识点，大小写不敏感），下方「共 N 道题 / 匹配 X / N」与「按最近修改排序」；题目卡片以大标题分两行展示来源与「第X页 第Y题」（无来源说明时第一行为「编号X」、第二行为「第Y题」），下方耗时标签与知识点标签并排，整卡点击进入编辑，右侧保留编辑/删除按钮（2026-11-25 用户要求）；列表按 `updatedAt` 倒序；空题库显示教学式空态与「录入第 1 道题」按钮（此时隐藏悬浮按钮），非空时右下角 Extended FAB「录入题目」；搜索无结果显示查询词与「清除搜索」 | 空题库、无匹配、加载失败 |
| QuestionEdit | 题号、编号（未选来源时标题「试卷编号」，选中来源后标题「页码编号」，2026-11-24 用户要求）与复习时长三栏并排、各占三分之一，题号示例放入占位符、时长单位「分钟」放在输入区后缀（2026-11-25 用户要求）；来源（单选标签，可空）、关联知识点（≤4）、笔记附件；「知识点标签」标题下为横向分类筛选选项卡（「全部」+ 各分类，2026-11-23 用户要求），筛选卡与下方标签条目之间以分割线分隔，标签卡只显示当前分类下的知识点；保存改为页面底部通栏主按钮（48dp），保存成功后先以带对勾 Toast 提示「保存成功」，再按入口分流：从 Home 的「录入题目」进入（`fromHome = true`）时以题库页替换当前页（返回键回 Home），从 Entry 进入时关闭当前页返回 Entry（2026-11-24 用户要求） | 关联超限、已打印旧稿提示、来源标签库为空、当前分类无知识点 |
| KnowledgePoints | 顶部为横向分类筛选选项卡（「全部」+ 各分类），再下方一行依次为「共 N 个标签」、管理分类、新增标签；编号/名称/所属分类列表（按用户修改时间倒序，新增/编辑后立即置顶，2026-11-24 用户要求），编辑（可改名称与分类）、删除；「管理分类」可新增/重命名/删除分类（有知识点的分类不可删）；新增/编辑标签必须选择分类；分类筛选与顶部导航栏间距收紧为 8dp（2026-11-23 用户要求）；2026-11-24 用户要求删去搜索框 | 被题目引用不可删、删除确认、分类有知识点不可删、分类名称重复、无分类时无法新增标签 |
| SourceTags | `AppRoute.SourceTags` 独立页面（顶部标题「来源标签」）：名称列表、新增、rename、删除确认（2026-11-24 用户要求删去搜索框）；2026-11-24 起首页不再提供「管理题目来源」入口，路由与页面保留（录入页内的来源标签窗口仍可增删改查） | 被题目引用不可删、名称重复、空库 |
| 来源标签（录入页内全屏窗口） | 导航栏与「知识点」页一致（左侧语义关闭图标 + 标题的 TopAppBar）；名称列表、新增、rename、删除确认（2026-11-24 用户要求删去搜索框）；关闭后录入页保留未保存内容 | 被题目引用不可删、名称重复、空库 |
| PlanPreview | 顶部计划摘要卡（计划号、页数/题数与通栏「打印计划」主按钮）；「每日安排」列表按页显示为「日期栏（日/月/星期）+ 题目标签」卡片；overflow 警告与“有 N 道题未排入本次计划”分别用错误/朱砂容器展示，统一用「来源 第X页 第Y题」标识题目（X 为页码编号、Y 为题号；没有来源说明时显示「编号X 第Y题」，无来源页码时只显示「第Y题」），推迟卡片逐条给出题目标识、中文原因与处理建议；按打开的窗口显示“重新生成本周计划/重新生成下周计划”的二次确认（C-098），入口降到页面底部 | 容量不足、`KNOWLEDGE_TEXT_OVERFLOW`、`ITEM_TOO_TALL`、`INVALID_TEMPLATE_GEOMETRY`、`PLAN_TOO_LONG`、被推迟题目（冷却期/复习时间不足/知识点缺失） |

> 警告代码（`PaginationWarningCode`）只给程序/日志使用；PlanPreview 必须把代码映射为中文标签，并展示给用户看的 `message`，不得直接显示枚举名（C-065 方案 B）。`KNOWLEDGE_TEXT_OVERFLOW` 由计划层产出并与分页警告合并（C-064 方案 A）。
>
> 题目标识统一为「来源 第X页 第Y题」（X 为页码编号、Y 为题号；没有来源说明时页码显示为「编号X」，无来源页码时只显示「第Y题」），同时用于计划页面卡片与“未排入计划”卡片，避免不同来源页上的相同题号无法区分；格式化函数 `formatQuestionLabel` 位于 `core:common`，计划预览与打印稿共用（2026-11-25 用户要求）。推迟卡片读 `PlanPreviewDeferred`：生成时随 `plan.deferred` 冻结 `questionId/题号/来源页码/原因`，重开计划从快照重建；旧快照缺来源页码时从当前题库兜底补齐展示（不改写快照）。
| Print | 由 PlanPreview 进入；计划摘要卡（计划号、页数/题数、每日安排）+ 通栏「打开系统打印」主按钮与「分享 PDF」次按钮；系统打印返回后把「纸张打印好了吗？」确认卡提升为主内容（已打印/未打印/重新打开打印）；「分享 PDF」把同一份 A4 布局写成缓存 PDF 并打开系统分享面板（微信、QQ 等），分享成功后与「已打印」一样写入 pageId/QR 并把 GENERATED 计划标记为 PRINTED（2026-11-24 用户要求） | 打印中、失败、等待「已打印/未打印」确认、PDF 生成中、分享失败；确认「已打印」或分享成功后只为当前计划写 pageId/QR，SUPERSEDED 计划不写（C-094 方案 B、C-097） |
| Scan | 顶部流程说明（拍摄 → 识别 → 确认）；相机/相册、当前 `ScanSession`、已扫页；识别成功后以主色结果卡突出「进入确认页」，权限/错误/结果提示使用语义色容器 | 一页一拍、`PAGE_*` 错误、重复页提示 |
| Confirm | plan 级多页确认、批次实际复习日期、逐行决策；顶部显示整批确认进度条、已标记/总行数与剩余行数；每行用语义色标签展示触发/关联、识别结果与最终决策，决策完成后收起操作按钮、只保留最终选择与对勾；底部提交面板显示剩余行数并承载唯一提交按钮 | `UNMARKED`、`LATE_BATCH`、未来日期行只能 NOT_REVIEWED |
| Settings | 每日分钟、计划周、扫描窗口、保留策略 | 见 `settings.md` |
| Backup | 由 Home 顶部导航栏右侧「导出数据库」进入（标签右侧图标与导入互换后显示上传图形，2026-11-22 用户要求）；页面顶部为导出说明与通栏「导出并分享」主按钮；导出完成后展示文件名、大小、备份格式版本与分享结果（不展示导出范围与各实体行数，2026-11-22 用户要求）；分享失败可只重试分享。导入入口为同一导航栏「导出数据库」左侧的「导入数据库」（图标互换后显示下载图形）：SAF 选文件或系统「用其他方式打开」json 文件，校验合法后弹「将覆盖当前数据，是否继续？」，确认后覆盖并以带对勾 Toast 提示「导入成功」，同时刷新首页/题库/知识点/来源/复习进度/计划预览的数据缓存 | 见 `backup.md` 的导出/导入状态机（`IDLE → EXPORTING → EXPORTED/ERROR`；`IDLE → READING → CONFIRMING → IMPORTING → SUCCESS/ERROR`） |
| About | 由 Home 底部信息图标「关于」进入（`AppRoute.About`，顶部标题「关于」）：展示应用图标、名称「拾知」、版本号、作者 `@ziyangdev910`、开源仓库 `https://github.com/ziyangdev910/recollect`（整行可点，右侧箭头，在系统浏览器打开）、开源许可证 `GPL-3.0 with Classpath Exception`（SPDX：`GPL-3.0-only WITH Classpath-exception-2.0`，整行可点进入 `AppRoute.License`）与简要功能说明；底部「致谢」卡片说明记忆调度算法在思想上受 Jingyong Su 等人提出的 SSP-MMC-Plus 工作启发，附论文完整引用（DOI 可点击打开网页），并声明记忆模型公式参考墨墨团队以 MIT 许可开源的 SSP-MMC 实现、排程与工程代码为独立编写且未复制或改编其实现代码；只读，不申请新权限（2026-11-23 用户要求；致谢与开源许可证栏目 2026-11-24 用户要求；2026-11-25 信息列表重排） | 版本号读取失败（显示 `unknown`） |
| License | 由 About 页「查看许可证全文」进入（`AppRoute.License`，顶部标题「开源许可」）：三个选项卡分别展示打包在 `app/src/main/assets/licenses/` 的 GPL-3.0 全文（`LICENSE.txt`）、Classpath 例外（`LICENSE-EXCEPTION.txt`）与 NOTICE（`NOTICE.txt`）；正文等宽小字号、整页可滚动；只读，不申请新权限（2026-11-24 用户要求） | 资源读取失败（显示错误说明，不崩溃） |

## 权限与文件

- `CAMERA`：仅扫描页使用；拒绝时允许相册选图。
- 文件导入/导出使用 SAF，不申请存储权限。「导出并分享」只写应用私有缓存并经 `FileProvider` 授予一次性读权限。
- 导入还支持系统 `ACTION_VIEW` + `application/json`（`android:label="导入到拾知数据库"`）：文件管理器以
  content URI 打开 json 时直接进入 `AppRoot` 的导入流程，跳过 SAF 选择；URI 由 `MainActivity` 处理后交给
  `ImportViewModel`，消费后立即清空以避免重复触发。该入口导入成功、Toast 显示后由 `MainActivity`
  调用 `finishAndRemoveTask()` 退出应用（2026-11-22 用户要求）；应用内 SAF 导入不退出。
- 笔记图片使用 Photo Picker 或 SAF，复制到 app 私有目录。
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
- Home → About：点击底部「关于」卡片进入关于页；返回由全局返回栈处理。首页「每日复习时长」卡片点击弹出悬浮窗，保存后写入 Room `app_settings` 并用于下一份计划。
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
