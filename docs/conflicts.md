# 冲突与待裁决记录（仅保留未裁决）

- 建立日期：2026-11-11；2026-11-13 已落地 C-034～C-045，并裁决 C-046～C-052。
- 当前未决：**C-032（本地 Kotlin 实现缺失，部分落地）**。
- 已落地：
  - C-034 编排层 → `planning.md`、`repository.md`
  - C-035 打印 → `pagination.md`、`print-template.md`
  - C-036 扫描/OMR → `scan-pipeline.md`
  - C-037 DHP 边界 → `feedback-usecase.md`
  - C-038～C-041 周计划批量回读 → `data-model.md`、`scheduler.md`、`confirm-flow.md`、`scan-pipeline.md`、`planning.md`、`backup.md`
  - C-042 App/UI/导航/DI → `app.md`、`settings.md`
  - C-043 备份 schema/迁移/SAF UI → `backup.md`
  - C-044 计划完成度/MISSED → `repository.md`、`app.md`
  - C-045 保留/清理 → `backup.md`、`settings.md`
- 2026-11-13 已裁决并落地：
  - C-046 方案 A → `repository.md`、`architecture/overview.md`、`CLAUDE.md`、`app.md`
  - C-047 方案 A → `pagination.md`
  - C-048 全部建议 → `pagination.md`
  - C-049 全部建议 → `scheduler.md`
  - C-050 建议 2、3 → `feedback-usecase.md`、`data-model.md`、`scheduler.md`、`CLAUDE.md`
  - C-051 方案 A → `scan-pipeline.md`、`print-template.md`、`dependency-proposals.md`
  - C-052 方案 C → `CLAUDE.md`、`app.md`、`dependency-proposals.md`、`gradle/libs.versions.toml`

## 既有未决

### C-032 本地 Kotlin 实现缺失，规格无法代码级核对

- **状态：部分落地**（2026-11-13 已建立 Gradle 骨架；业务 Kotlin 代码仍未开始）。
- 现象：2026-11-13 前仓库无 `.kt`/`.kts`/`.gradle`；当前已有 `settings.gradle.kts`、根 `build.gradle.kts`、`gradle/libs.versions.toml`、Gradle Wrapper 与四个 `core` 模块的空构建脚本，但没有任何业务 `.kt`。
- 影响：`docs/architecture/overview.md` 仍标注“规范口径，待代码复核”；`./gradlew test` 现在可以运行，但没有测试用例。
- 建议：从 `docs/specs/scheduler.md` 的 S-01 开始，按 `docs/prompts.md` 的固定循环逐条落地；每完成一个模块，按 specs 反向核对 `overview.md` 与字段产权表。

## 已裁决但待验证（非新冲突）

- C-051 方案 A 的 R-01 spike 结果待验证：850×540 → 2480×3508 适配后的 OMR/QR 精度必须用真实打印稿判断；不达标时按 `scan-pipeline.md` 转方案 B（fork）。这不改变已裁决的 A 方案，只记录验证门禁。
- C-050 的 `Double` 内部算术与 `Float` 边界转换，需在 `ApplyFeedbackUseCase` 实现后用 U-01～U-19 验证；当前只有规格，无业务代码。

## 未决文档缺口（非冲突）

- `docs/preflight-report.md` 被 CLAUDE.md 索引引用但不存在。
- `docs/dependency-proposals.md` 已于 2026-11-13 创建，记录 bootstrap 版本矩阵与 C-046～C-052 裁决；后续新增依赖仍需追加。
- Gradle Wrapper、`settings.gradle.kts`、`gradle/libs.versions.toml` 已于 2026-11-13 建立；C-032 的业务代码部分仍未完成。
- C-001～C-005 的原始裁决记录不在仓库；ADR 已删除，历史裁决仅存于 `docs/agent-log/2026-11-11.md` 与 Blurt H-0001。
