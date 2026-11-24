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
    data class QuestionEdit(val questionId: String?) : AppRoute
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
| Home | 仪表盘式布局：顶部计划主卡默认本周为主区块（真实本周窗口、状态胶囊“未生成/待打印/已打印/已完成”，按状态显示“生成本周计划/预览并打印本周计划”），卡内下周为次要行（日期、compact 状态胶囊、30dp“生成/预览下周计划”入口）；本周计划为 `COMPLETED` 时整体交换主次——下周升为主区块（大标题、说明、通栏“生成/预览并打印下周计划”），本周降为次要行（“已完成”胶囊 + “预览本周计划”，仍可预览/重印），其余状态保持本周为主，只提供本周与下周两个窗口（C-098/C-099/C-100）；其下为题目/知识点/已逾期/7 天内概览（读取失败显示“—”，不阻塞入口）；再下为操作入口（不再显示「快捷操作」标签）：顶部一行两个并排小标签，左「录入题目」直达新建题目页、右「管理题库」进入题库列表页；下方 2×2 小标签左上「扫描回读」、右上「查看复习进度」、左下「管理知识点」、右下「管理题目来源」；六个入口均为图标 + 左对齐单行标题、统一 56dp 高；底部卡片栏左侧为时钟图标 + 左对齐「每日复习时长」（副标题显示当前分钟数，点击弹出悬浮窗修改，输入框下方附简要说明），右侧为信息图标「关于」（副标题显示当前版本号）进入关于页；新增底部卡片后首页允许滚动（2026-11-23 用户要求） | 无计划、计划已完成、待扫描、统计加载失败、`LATE_BATCH`、每日时长非法值 |
| Progress | 复习进度可视化（只读）：题目总数/已逾期/今天到期/7 天内统计、未来 14 天到期分布、逐题题号/来源页/上次复习/最早到期与每个知识点的档位、连续掌握度、下次到期日、冷启动或半衰期 | 空题库、加载失败（可重试） |
| Entry | 题目列表、搜索（题号/页码/知识点）、归档筛选、新建 | 空题库、无匹配、加载失败 |
| QuestionEdit | 题号与页码编号同行并排、各占一半；来源（单选标签，可空）、关联知识点（≤4）、预估分钟、笔记附件；「知识点标签」标题下为横向分类筛选选项卡（「全部」+ 各分类，2026-11-23 用户要求），筛选卡与下方标签条目之间以分割线分隔，标签卡只显示当前分类下的知识点 | 关联超限、已打印旧稿提示、来源标签库为空、当前分类无知识点 |
| KnowledgePoints | 搜索框下方为横向分类筛选选项卡（「全部」+ 各分类），再下方一行依次为「共 N 个标签」、管理分类、新增标签；编号/名称/所属分类列表，编辑（可改名称与分类）、删除；「管理分类」可新增/重命名/删除分类（有知识点的分类不可删）；新增/编辑标签必须选择分类；搜索框与顶部导航栏间距收紧为 8dp（2026-11-23 用户要求） | 被题目引用不可删、删除确认、分类有知识点不可删、分类名称重复、无分类时无法新增标签 |
| SourceTags | `AppRoute.SourceTags` 独立页面（顶部标题「来源标签」）：名称列表、搜索、新增、rename、删除确认；由首页快捷操作 2×2 右下方的「管理题目来源」卡片进入 | 被题目引用不可删、名称重复、空库 |
| 来源标签（录入页内全屏窗口） | 导航栏与「知识点」页一致（左侧语义关闭图标 + 标题的 TopAppBar）；名称列表、搜索、新增、rename、删除确认；关闭后录入页保留未保存内容 | 被题目引用不可删、名称重复、空库 |
| PlanPreview | 计划页、overflow 警告、每日容量、打印入口（打印页只从此进入）；页面卡片与“有 N 道题未排入本次计划”卡片统一用「页码-第X题」标识题目（无来源页码时只显示「第X题」），推迟卡片逐条给出题目标识、中文原因与处理建议；按打开的窗口显示“重新生成本周计划/重新生成下周计划”的二次确认（C-098） | 容量不足、`KNOWLEDGE_TEXT_OVERFLOW`、`ITEM_TOO_TALL`、`INVALID_TEMPLATE_GEOMETRY`、`PLAN_TOO_LONG`、被推迟题目（冷却期/复习时间不足/知识点缺失） |

> 警告代码（`PaginationWarningCode`）只给程序/日志使用；PlanPreview 必须把代码映射为中文标签，并展示给用户看的 `message`，不得直接显示枚举名（C-065 方案 B）。`KNOWLEDGE_TEXT_OVERFLOW` 由计划层产出并与分页警告合并（C-064 方案 A）。
>
> 题目标识统一为「页码-第X题」（X 为题号；无来源页码时只显示「第X题」），同时用于计划页面卡片与“未排入计划”卡片，避免不同来源页上的相同题号无法区分。推迟卡片读 `PlanPreviewDeferred`：生成时随 `plan.deferred` 冻结 `questionId/题号/来源页码/原因`，重开计划从快照重建；旧快照缺来源页码时从当前题库兜底补齐展示（不改写快照）。
| Print | 由 PlanPreview 进入；系统打印适配器、文档生成后的结果确认；「分享 PDF」把同一份 A4 布局写成缓存 PDF 并打开系统分享面板（微信、QQ 等），分享成功后与「已打印」一样写入 pageId/QR 并把 GENERATED 计划标记为 PRINTED（2026-11-24 用户要求） | 打印中、失败、等待「已打印/未打印」确认、PDF 生成中、分享失败；确认「已打印」或分享成功后只为当前计划写 pageId/QR，SUPERSEDED 计划不写（C-094 方案 B、C-097） |
| Scan | 相机/相册、当前 `ScanSession`、已扫页 | 一页一拍、`PAGE_*` 错误、重复页提示 |
| Confirm | plan 级多页确认、批次实际复习日期、逐行决策 | `UNMARKED`、`LATE_BATCH`、未来日期行只能 NOT_REVIEWED |
| Settings | 每日分钟、计划周、扫描窗口、保留策略 | 见 `settings.md` |
| Backup | 由 Home 顶部导航栏右侧「导出数据库」进入（标签右侧图标与导入互换后显示上传图形，2026-11-22 用户要求）；页面展示导出说明，主按钮「导出并分享」生成 JSON 后打开系统分享面板，导出完成后展示文件名、大小、备份格式版本与分享结果（不展示导出范围与各实体行数，2026-11-22 用户要求）；分享失败可只重试分享。导入入口为同一导航栏「导出数据库」左侧的「导入数据库」（图标互换后显示下载图形）：SAF 选文件或系统「用其他方式打开」json 文件，校验合法后弹「将覆盖当前数据，是否继续？」，确认后覆盖并以带对勾 Toast 提示「导入成功」，同时刷新首页/题库/知识点/来源/复习进度/计划预览的数据缓存 | 见 `backup.md` 的导出/导入状态机（`IDLE → EXPORTING → EXPORTED/ERROR`；`IDLE → READING → CONFIRMING → IMPORTING → SUCCESS/ERROR`） |
| About | 由 Home 底部信息图标「关于」进入（`AppRoute.About`，顶部标题「关于」）：展示应用图标、名称「拾知」、版本号、作者 `@ziyangdev910`、开源仓库 `https://github.com/ziyangdev910/recollect`（可点击在系统浏览器打开）与简要功能说明；底部「致谢」卡片说明记忆调度算法在思想上受 Jingyong Su 等人提出的 SSP-MMC-Plus 工作启发，附论文完整引用（J. Su, J. Ye, L. Nie, Y. Cao and Y. Chen, "Optimizing Spaced Repetition Schedule by Capturing the Dynamics of Memory," IEEE Transactions on Knowledge and Data Engineering, vol. 35, no. 10, pp. 10085-10097, 1 Oct. 2023, doi: 10.1109/TKDE.2023.3251721（DOI 可点击在系统浏览器打开）），并声明实现为基于论文描述的独立编写、未复制或改编原项目源代码；只读，不申请新权限（2026-11-23 用户要求；致谢栏目 2026-11-24 用户要求） | 版本号读取失败（显示 `unknown`） |

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
- Entry → QuestionEdit → 返回 Entry：复用 ViewModel 与 `LazyListState`，不重新加载、保留滚动位置；保存后静默刷新列表。
- QuestionEdit 内「管理来源」：来源标签库以全屏窗口（`Dialog`）打开，不进入返回栈；关闭后仅刷新来源候选，题号/页码/来源选择/知识点/预估分钟等未保存内容保持原样。
- QuestionEdit 内「管理知识点」：知识点标签库（含分类管理）以全屏窗口打开；关闭后刷新标签与分类候选，未保存内容与分类筛选保持原样（失效的分类筛选回到「全部」）。
- Home → About：点击底部「关于」卡片进入关于页；返回由全局返回栈处理。首页「每日复习时长」卡片点击弹出悬浮窗，保存后写入 Room `app_settings` 并用于下一份计划。
- Home → SourceTags：首页快捷操作 2×2 右下方的「管理题目来源」卡片进入独立的来源标签页（`AppRoute.SourceTags`，顶部标题「来源标签」），返回首页由全局返回栈处理；从该页返回后再进入 Entry/QuestionEdit 时会重新读取来源候选。
- Home → PlanPreview：本周或下周已有 GENERATED/PRINTED/COMPLETED 计划时进入预览复用同一计划号；不会再次点击首页按钮就生成新计划。只有预览页的“重新生成本周计划/重新生成下周计划”经确认后才生成新计划号并替代该窗口的旧计划（C-097/C-098）。
- 打印确认：App 内“已记录本次打印”只在计划仍为当前计划时出现；若计划已被重新生成替代，显示“本次计划已被新的计划替代，未更新打印快照。”
- 分享 PDF：系统分享面板打开即视为文档已导出，按与「已打印」相同的入口写入 pageId/QR 并标记 PRINTED；计划已被替代时仍生成文件，但提示“未更新打印快照”，首页状态不变（2026-11-24 用户要求）。
- PlanPreview → Print → 返回 PlanPreview 复用同一份计划，不重新生成、计划号不变；只有再次从 Home/Entry 点击某个窗口的生成入口才生成新计划。
- 不在 ViewModel 持有 `Context`；文件/相机/打印操作通过 UseCase/Repository 注入。

## 验收

- 冷启动 → Home 无计划空态；录入 1 题 1 知识点 → 生成计划 → 打印 → 扫描 → 确认 → Home 显示完成度。
- 旋转/进程重建后 Confirm 的批次日期与逐行决策不丢。
- 返回栈、权限拒绝、SAF 取消、打印失败均有明确恢复路径。
