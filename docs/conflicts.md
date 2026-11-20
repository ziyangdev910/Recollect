# 冲突与待裁决记录（仅保留未裁决）

- 建立日期：2026-11-11。
- 当前未决：**C-032（本地 Kotlin 实现缺失，部分落地）**。
- 已裁决/已落地条目（C-034～C-096）已归档至 `docs/conflicts-archive.md`（C-034～C-053 于 2026-11-13；C-054～C-074 于 2026-11-15、2026-11-17；C-075、C-076 于 2026-11-17；C-077～C-089 于 2026-11-17；C-090、C-091 于 2026-11-18；C-092～C-095 于 2026-11-18；C-096 于 2026-11-20），本文件不再保留其详情。
- 已裁决但待验证（非新冲突）：C-050、C-051；验证门禁见 `docs/conflicts-archive.md`。

## 既有未决

### C-032 本地 Kotlin 实现缺失，规格无法代码级核对

- **状态：部分落地**（2026-11-13 建立 Gradle 骨架；同日落地 `core:model` 的 `SchedulerModels` 与 `core:scheduler` 的 `Scheduler`/`DefaultScheduler`/`SchedulerPlanTest`；截至 2026-11-15 收尾，scheduler 已有 28 个 `@Test` 方法 + 1 个 `@RepeatedTest(100)` 属性测试（共 128 次执行），覆盖 S-01～S-22；S-12 按规格由 feedback-usecase 的 U-12 承载，scheduler 不读取该字段；S-06/S-07 已迁移 U-06/U-07。2026-11-15 起 `core:pagination` 落地 `TemplateGeometry`、`RenderableQuestion`、`PaginationResult` 系列类型、`PagePlanner`/`DefaultPagePlanner`、`TemplateGeometryTest`、26 个 `PagePlannerTest` 方法与 1 个 `@RepeatedTest(100)` 属性测试（P-09）；收尾后 P-01～P-20 均有测试映射，并补齐三条验收门禁：模板非法返回 `Failure(INVALID_TEMPLATE_GEOMETRY)`、页数超过 99 返回 `Failure(PLAN_TOO_LONG)`、P-20 三类非法计数抛 `IllegalArgumentException`；C-047、C-048、C-060、C-061、C-062、C-063、C-065、C-066、C-068 均有实现或测试锁定；C-064、C-067、C-069 的实现分别在计划/数据层，规格已回写。2026-11-17 起 `data:room` 落地四个实体与 Entity↔DTO 映射、`ModelMappingTest` 5 个用例，同日落 `FirstVerticalSliceTest`（scheduler → pagination 纵切，1 个用例）；尚未接入 Room 注解/schema/DAO）。2026-11-18 起 `:app` 落地阶段 5.1 壳：手写 `AppContainer`、`AppNavigator`/`BackHandler`、Compose 路由占位与 `MainActivity`，导航返回栈有 1 个 JUnit 5 用例。同日阶段 5.2 落地 `:feature:entry`/`:feature:planning` 与 `GeneratePlanUseCase`，串起 Entry → 计划 → PlanPreview 的首个纵切（C-047、C-060～C-067 有测试）；计划生成返回类型差异已按 C-090 方案 A 裁决并回写规格。剩余：设置/备份页面、`data:repository`、知识点编号序列表/墓碑与真实打印稿 R-01 验收仍未落地；打印计划页与确认页已于 2026-11-18 落地并通过实体机验收，扫描回读页已于 2026-11-20 落地并通过实体机 UI/权限/手动补录验证（真实打印稿拍照仍属 R-01 缺口）。
- 现象：2026-11-13 前仓库无 `.kt`/`.kts`/`.gradle`；当前已有 `settings.gradle.kts`、根 `build.gradle.kts`、`gradle/libs.versions.toml`、Gradle Wrapper、四个 `core` 模块的构建脚本与 scheduler/pagination 的业务代码。
- 影响：`docs/architecture/overview.md` 仍标注“规范口径，待代码复核”；`./gradlew test` 目前有 scheduler 的 28 个 `@Test` 方法 + 1 个属性测试，以及 pagination 的 `TemplateGeometryTest`、26 个 `PagePlannerTest` 方法与 1 个属性测试（P-09，100 次重复），其余模块为 `NO-SOURCE`。
- **对用户的实际影响**：用户现在可以录入、生成计划、进入打印页并调起系统打印，也可以从 Home 的「扫描回读」用页内相机或相册取图并进入确认页逐行提交（2026-11-20 落地扫描页并在实体机验证权限、A4 取景、拍摄、手动补录与提交）；调度器的候选、冷却与排序，以及分页器的页码、移页、溢出与提示逻辑已有测试覆盖。仍缺少设置/备份页与真实打印稿 R-01 验收，用户还不能确认“拍照回读 → 确认”在真实打印稿上稳定可用。
- **选型参考**：scheduler 与 pagination 均已收尾；下一步按 `docs/prompts.md` 阶段 3.1 落地 `core:model` 的 `KnowledgeRowsCodec`，再进入 `data:room` 映射与 App 纵切，是让用户拿到可打印周计划的下一段路径，暂无其它可选方案。
- 建议：每完成一个模块，按 specs 反向核对 `overview.md` 与字段产权表。

## pagination 收尾审计观察（无待裁决条目）

> 来源：只读规格审计（`docs/specs/pagination.md` 与当前实现/测试逐条比对）。C-063～C-069 已按 2026-11-15/2026-11-17 人类裁决落地并归档；本区仅保留 ③ 与不构成冲突的低影响观察。①（规格有、代码没有）见 `docs/agent-log/2026-11-15.md` 与 `docs/agent-log/2026-11-17.md`。

### ③ 测试断言与规格文字不一致

- 本轮未发现测试断言与规格文字相反的项；P-01～P-20 与验收门禁的断言逐条核对一致。P-09 的标记口径已由 C-063 裁决：不再渲染“续下页”，分页器保留内部 marker 元数据，测试继续按内部元数据断言；P-17 的名称顺序措辞已在测试中澄清为“分页器不得读取或重排名称”（名称按编号升序是上游契约）。

### 低影响观察（非冲突，随对应模块实现时处理）

- **同一计划内题目编号唯一**：C-056 已确立“id 唯一由 data/room 保证”，分页器不去重；建议在分页规格补一句前置条件。
- **溢出条目的高度口径**：当前取整题高度（含全额附加区），规格只写 `heightMm` 未定义口径；警告文案不使用该值，用户不可见。
- **模板校验范围**：只拒绝非正的可用高度与首页内容区；负边距、负图例、负行高等不会被拒绝，生产模板固定 A4，正常用户不可达。
- **警告排序对非 ASCII 题目编号**：使用 UTF-16 自然序，不是中文拼音序；当前题目编号为内部 ASCII，无实际影响。
- **计划过长失败时不返回溢出题目清单**：用户只看到“计划过长”，计划层可重算；低影响。

## 未决文档缺口（非冲突）

- `docs/preflight-report.md` 尚未创建；CLAUDE.md 的文档索引已标注该状态。
- `docs/dependency-proposals.md` 已于 2026-11-13 创建，记录 bootstrap 版本矩阵与 C-046～C-052 裁决；后续新增依赖仍需追加。
- Gradle Wrapper、`settings.gradle.kts`、`gradle/libs.versions.toml` 已于 2026-11-13 建立；C-032 的 `core:common` 与 `data`/`feature`/`app` 业务代码仍未完成。
