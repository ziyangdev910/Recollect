# 冲突与待裁决记录（仅保留未裁决）

- 建立日期：2026-11-11。
- 当前未决：**C-032（本地 Kotlin 实现缺失，部分落地）**。
- 已裁决/已落地条目（C-034～C-059）已归档至 `docs/conflicts-archive.md`（C-034～C-053 于 2026-11-13；C-054～C-059 于 2026-11-15），本文件不再保留其详情。
- 已裁决但待验证（非新冲突）：C-050、C-051；验证门禁见 `docs/conflicts-archive.md`。

## 既有未决

### C-032 本地 Kotlin 实现缺失，规格无法代码级核对

- **状态：部分落地**（2026-11-13 建立 Gradle 骨架；同日落地 `core:model` 的 `SchedulerModels` 与 `core:scheduler` 的 `Scheduler`/`DefaultScheduler`/`SchedulerPlanTest`；截至 2026-11-15 收尾，scheduler 已有 28 个 `@Test` 方法 + 1 个 `@RepeatedTest(100)` 属性测试（共 128 次执行），覆盖 S-01～S-22；S-12 按规格由 feedback-usecase 的 U-12 承载，scheduler 不读取该字段；S-06/S-07 已迁移 U-06/U-07。2026-11-15 起 `core:pagination` 落地 `TemplateGeometry`、`RenderableQuestion`、`PaginationResult` 系列类型、`PagePlanner`/`DefaultPagePlanner` 与 3 个测试（P-01：一日一条一页；P-03：放不下的条目整条移页并生成续下页标记），overflow/附件上限/日期排序/首页单独保留尚未实现；`core:common`、`data`/`feature`/`app` 仍无业务 Kotlin）。
- 现象：2026-11-13 前仓库无 `.kt`/`.kts`/`.gradle`；当前已有 `settings.gradle.kts`、根 `build.gradle.kts`、`gradle/libs.versions.toml`、Gradle Wrapper、四个 `core` 模块的构建脚本与 scheduler 的业务代码。
- 影响：`docs/architecture/overview.md` 仍标注“规范口径，待代码复核”；`./gradlew test` 目前有 scheduler 的 28 个 `@Test` 方法 + 1 个属性测试，以及 pagination 的 `TemplateGeometryTest` 与 `PagePlannerTest`（P-01、P-03），其余模块为 `NO-SOURCE`。
- **对用户的实际影响**：用户目前还不能走完任何真实流程（录入、生成并打印周计划、拍照回读与确认）；调度器的候选、冷却与排序逻辑虽已在测试中验证，但尚未产生可打印、可复习的计划。
- **选型参考**：scheduler 已收尾；继续按 `docs/specs/pagination.md` 的用例表与 `docs/prompts.md` 的固定循环落地分页/打印，是让用户拿到可打印周计划的下一段路径，暂无其它可选方案。
- 建议：每完成一个模块，按 specs 反向核对 `overview.md` 与字段产权表。

## 未决文档缺口（非冲突）

- `docs/preflight-report.md` 被 CLAUDE.md 索引引用但不存在。
- `docs/dependency-proposals.md` 已于 2026-11-13 创建，记录 bootstrap 版本矩阵与 C-046～C-052 裁决；后续新增依赖仍需追加。
- Gradle Wrapper、`settings.gradle.kts`、`gradle/libs.versions.toml` 已于 2026-11-13 建立；C-032 的业务代码部分仍未完成。
