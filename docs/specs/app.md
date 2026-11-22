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

## DI 与导航

- **不使用 Hilt/Koin**，避免引入设计决策外的新依赖（R-05）。`app` 内使用手写 `AppContainer` 持有 Repository、UseCase、Scheduler、PagePlanner、ScanEngine、TextMeasurer 等单例。
- **不引入 navigation-compose**。`AppNavigator` 用 `mutableStateOf<AppRoute>` + 简单返回栈实现；`BackHandler` 处理返回。
- `MainActivity` 只创建 `AppContainer`、`AppNavigator` 和 `AppRoot`；不直接访问 Room/CameraX/PrintManager/ML Kit（R-03）。

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
| Home | 仪表盘式布局：顶部本周计划主卡（周窗口、状态“未生成/待打印/已打印/已完成”，按状态显示“生成本周计划/查看并打印本周计划”）；其下为题目/知识点/已逾期/7 天内概览（读取失败显示“—”，不阻塞入口）；再下为「快捷操作」：顶部通栏「录入题目」，下方 2×2 卡片左上「扫描回读」、右上「查看复习进度」、左下「管理知识点」、右下「管理题目来源」，每张卡片含语义化图标与一句说明 | 无计划、计划已完成、待扫描、统计加载失败、`LATE_BATCH` |
| Progress | 复习进度可视化（只读）：题目总数/已逾期/今天到期/7 天内统计、未来 14 天到期分布、逐题题号/来源页/上次复习/最早到期与每个知识点的档位、连续掌握度、下次到期日、冷启动或半衰期 | 空题库、加载失败（可重试） |
| Entry | 题目列表、搜索（题号/页码/知识点）、归档筛选、新建 | 空题库、无匹配、加载失败 |
| QuestionEdit | 题号与页码编号同行并排、各占一半；来源（单选标签，可空）、关联知识点（≤4）、预估分钟、笔记附件 | 关联超限、已打印旧稿提示、来源标签库为空 |
| KnowledgePoints | 编号/名称列表、rename、删除 | 被题目引用不可删、删除确认 |
| SourceTags | `AppRoute.SourceTags` 独立页面（顶部标题「来源标签」）：名称列表、搜索、新增、rename、删除确认；由首页快捷操作 2×2 右下方的「管理题目来源」卡片进入 | 被题目引用不可删、名称重复、空库 |
| 来源标签（录入页内全屏窗口） | 导航栏与「知识点」页一致（左侧语义关闭图标 + 标题的 TopAppBar）；名称列表、搜索、新增、rename、删除确认；关闭后录入页保留未保存内容 | 被题目引用不可删、名称重复、空库 |
| PlanPreview | 计划页、overflow 警告、每日容量、打印入口（打印页只从此进入）；“重新生成本周计划”为二次确认的显式操作 | 容量不足、`KNOWLEDGE_TEXT_OVERFLOW`、`ITEM_TOO_TALL`、`INVALID_TEMPLATE_GEOMETRY`、`PLAN_TOO_LONG` |

> 警告代码（`PaginationWarningCode`）只给程序/日志使用；PlanPreview 必须把代码映射为中文标签，并展示给用户看的 `message`，不得直接显示枚举名（C-065 方案 B）。`KNOWLEDGE_TEXT_OVERFLOW` 由计划层产出并与分页警告合并（C-064 方案 A）。
| Print | 由 PlanPreview 进入；系统打印适配器、文档生成后的结果确认 | 打印中、失败、等待「已打印/未打印」确认；确认「已打印」后只为当前计划写 pageId/QR，SUPERSEDED 计划不写（C-094 方案 B、C-097） |
| Scan | 相机/相册、当前 `ScanSession`、已扫页 | 一页一拍、`PAGE_*` 错误、重复页提示 |
| Confirm | plan 级多页确认、批次实际复习日期、逐行决策 | `UNMARKED`、`LATE_BATCH`、未来日期行只能 NOT_REVIEWED |
| Settings | 每日分钟、计划周、扫描窗口、保留策略 | 见 `settings.md` |
| Backup | 导出/导入、预览、覆盖确认、结果 | 见 `backup.md` 的 SAF 状态机 |

## 权限与文件

- `CAMERA`：仅扫描页使用；拒绝时允许相册选图。
- 文件导入/导出使用 SAF，不申请存储权限。
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
- Home → SourceTags：首页快捷操作 2×2 右下方的「管理题目来源」卡片进入独立的来源标签页（`AppRoute.SourceTags`，顶部标题「来源标签」），返回首页由全局返回栈处理；从该页返回后再进入 Entry/QuestionEdit 时会重新读取来源候选。
- Home → PlanPreview：本周已有 GENERATED/PRINTED 计划时进入预览复用同一计划号；不会再次点击首页按钮就生成新计划。只有预览页的“重新生成本周计划”经确认后才生成新计划号并替代旧计划（C-097）。
- 打印确认：App 内“已记录本次打印”只在计划仍为当前计划时出现；若计划已被重新生成替代，显示“本次计划已被新的计划替代，未更新打印快照。”
- PlanPreview → Print → 返回 PlanPreview 复用同一份计划，不重新生成、计划号不变；只有再次从 Home/Entry 点击“生成本周计划”才生成新计划。
- 不在 ViewModel 持有 `Context`；文件/相机/打印操作通过 UseCase/Repository 注入。

## 验收

- 冷启动 → Home 无计划空态；录入 1 题 1 知识点 → 生成计划 → 打印 → 扫描 → 确认 → Home 显示完成度。
- 旋转/进程重建后 Confirm 的批次日期与逐行决策不丢。
- 返回栈、权限拒绝、SAF 取消、打印失败均有明确恢复路径。
