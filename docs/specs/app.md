# App 壳、UI 与导航规格（app）

关联决策：D-10、D-11、D-13
关联裁决：C-046、C-052
关联规格：`settings.md`、`planning.md`、`repository.md`、`backup.md`、`scan-pipeline.md`、`confirm-flow.md`

## 模块图

```text
:app
 ├─ :feature:entry
 ├─ :feature:planning
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
| Home | 当前计划卡片、完成度、打印/扫描入口、题目/知识点数量 | 无计划、计划已完成、待扫描、`LATE_BATCH` |
| Entry | 题目列表、搜索、归档筛选、新建 | 空题库、加载失败 |
| QuestionEdit | 题号、关联知识点（≤4）、预估分钟、笔记附件 | 关联超限、已打印旧稿提示 |
| KnowledgePoints | 编号/名称列表、rename、删除 | 被题目引用不可删、删除确认 |
| PlanPreview | 计划页、overflow 警告、每日容量、生成/打印 | 容量不足、`KNOWLEDGE_TEXT_OVERFLOW`、`ITEM_TOO_TALL`、`INVALID_TEMPLATE_GEOMETRY`、`PLAN_TOO_LONG` |
| Print | 系统打印适配器、打印结果 | 打印中、失败、成功写 pageId/QR |
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
- 不在 ViewModel 持有 `Context`；文件/相机/打印操作通过 UseCase/Repository 注入。

## 验收

- 冷启动 → Home 无计划空态；录入 1 题 1 知识点 → 生成计划 → 打印 → 扫描 → 确认 → Home 显示完成度。
- 旋转/进程重建后 Confirm 的批次日期与逐行决策不丢。
- 返回栈、权限拒绝、SAF 取消、打印失败均有明确恢复路径。
