# 设置规格（feature/settings + data/repository）

关联决策：D-04、D-05、D-11
关联规格：`app.md`、`planning.md`、`backup.md`

## 设置数据

```kotlin
data class AppSettings(
    val dailyAvailableMinutes: Map<DayOfWeek, Int>, // 周一..周日，默认 60
    val planStartDay: DayOfWeek = DayOfWeek.MONDAY,
    val scanWindowStartOffsetDays: Int = 1,          // planEnd + 1
    val scanWindowEndOffsetDays: Int = 7,            // planEnd + 7
    val keepPlanPagesWeeks: Int = 26,                // 旧 PlanPage 保留 26 周
    val cleanupPlanPagesAfterExport: Boolean = false  // 仅清理旧 PlanPage；历史反馈不自动删除
)
```

- 存储：`SettingsRepository` 使用 SharedPreferences（不引入 DataStore/Hilt 等新依赖）；对外暴露 `Flow<AppSettings>`。
- 默认每日 60 分钟；用户可按星期调整，范围 0～240 分钟。

> 2026-11-23 落地说明：本轮先实现单值「每日复习时长」（1～240 分钟，默认 60），
> 持久化在 Room 单行表 `app_settings`（schema v12），随数据库备份导入/导出；
> 首页底部「每日复习时长」悬浮窗修改，铺满下一份计划的每一天。
> 按星期细分、计划起始日、扫描窗口等设置项仍未实现，待后续小阶段落地。
- `planStartDay` 默认周一；允许改为任意星期，计划周仍为连续 7 天。
- 扫描窗口默认 `planEnd+1..planEnd+7`；超出窗口标 `LATE_BATCH`。
- `keepPlanPagesWeeks` 默认 26 周；旧 `PlanPage` 可按此清理，`ReviewFeedback`/`ConfirmationDecisionLog` 不自动删除。
- `cleanupPlanPagesAfterExport` 默认 false；开启后仅在用户手动“导出后清理旧 PlanPage”时执行，不自动删除 `ReviewFeedback`/`ConfirmationDecisionLog`。

## 设置页

| 分组 | 项 | 交互 |
|---|---|---|
| 学习量 | 周一～周日每日可用分钟 | 数字输入/滑杆，0～240；非法值禁用保存 |
| 计划 | 计划周起始日 | 星期选择，默认周一 |
| 扫描 | 扫描窗口起止偏移 | 1～14 天；结束 ≥ 开始 |
| 数据 | 保留 PlanPage 周数 | 4～104 周，默认 26 |
| 数据 | 导出后清理旧 PlanPage | 开关，默认关；开启时二次确认；`ReviewFeedback`/`ConfirmationDecisionLog` 永不自动删除 |
| 关于 | 版本、备份格式版本 | 只读；首页不再在「关于」行显示版本号，版本号改为显示在首页标题「拾知」右下角（2026-11-26 用户要求） |
| 数据 | 导入数据库 / 导出数据库 / 使用引导 | 首页「设置」面板底部四个等宽入口（导入 / 导出 / 使用引导 / 关于），各占面板总宽度四分之一；「使用引导」重新播放首次复习演示并保留当前数据（2026-11-26 用户要求；原在顶部导航栏） |

## 变更生效

- 每日分钟/计划起始日：下一份计划生效，不回溯已生成计划。
- 扫描窗口：下一次扫描生效。
- 保留周数：下一次清理任务生效；清理前必须提示“旧打印稿将无法手动回退”。
- 不在本页修改 D-01～D-13 的冻结规则（冷却 7 天、DHP 90% 等）。
- 冷却天数固定 7 天；0 仅为内部/测试的“关闭冷却”语义，负数为非法，设置页不提供该选项（C-057 方案 D）。

## 验收

- 修改每日分钟 → 下一份计划的 `dailyAvailableMinutes` 使用新值；已生成计划不变。
- 修改扫描窗口 → 确认页的 `LATE_BATCH` 判定使用新值。
- 清理 PlanPage 不影响 `ReviewFeedback`/`ConfirmationDecisionLog` 查询。
