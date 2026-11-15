# 冲突与裁决归档

> 用途：归档已裁决/已落地条目，供审计与追溯；**未裁决条目只保留在 `docs/conflicts.md`**。
> 建立：2026-11-13（按用户要求把 `conflicts.md` 中已裁决问题移入本文件）。

## 2026-11-13 已裁决并落地

### 实现层缺口 C-034～C-045

- C-034 编排层 → `planning.md`、`repository.md`
- C-035 打印 → `pagination.md`、`print-template.md`
- C-036 扫描/OMR → `scan-pipeline.md`
- C-037 DHP 边界 → `feedback-usecase.md`
- C-038～C-041 周计划批量回读 → `data-model.md`、`scheduler.md`、`confirm-flow.md`、`scan-pipeline.md`、`planning.md`、`backup.md`
- C-042 App/UI/导航/DI → `app.md`、`settings.md`
- C-043 备份 schema/迁移/SAF UI → `backup.md`
- C-044 计划完成度/MISSED → `repository.md`、`app.md`
- C-045 保留/清理 → `backup.md`、`settings.md`

### 架构与实现裁决 C-046～C-053

- C-046 方案 A → `repository.md`、`architecture/overview.md`、`CLAUDE.md`、`app.md`
- C-047 方案 A → `pagination.md`
- C-048 全部建议 → `pagination.md`
- C-049 全部建议 → `scheduler.md`
- C-050 建议 2、3 → `feedback-usecase.md`、`data-model.md`、`scheduler.md`、`CLAUDE.md`
- C-051 方案 A → `scan-pipeline.md`、`print-template.md`、`dependency-proposals.md`
- C-052 方案 C → `CLAUDE.md`、`app.md`、`dependency-proposals.md`、`gradle/libs.versions.toml`
- C-053 方案 A → `core:model`、`core:scheduler`、`docs/prompts.md`

#### C-053 scheduler DTO 的模块归属（已裁决：方案 A）

- 裁决：按 `repository.md`，`Tier`、`KnowledgeState`、`SchedulerQuestion` 统一放 `core:model`；`core:scheduler` 通过 `implementation(project(":core:model"))` 依赖它；测试 import 改为 `com.recollect.model.*`。
- 落地：`core/model/src/main/kotlin/com/recollect/model/SchedulerModels.kt` 新增上述三个类型；`core/scheduler` 的 `Scheduler.kt` 只保留 `Scheduler` 接口与 `PlanResult`/`PlannedQuestion`/`DeferredItem`/`DeferReason`（`repository.md` 未把这四个列入 `core:model` 清单）。
- 命名同步：`docs/prompts.md` 1.1 的旧称 `Question` 改为 `SchedulerQuestion`（`Question` 作为 Room 实体名的引用保持不变）。
- 约束：不得同时维护两份同名 DTO；`core:scheduler` 与 `core:model` 只允许单向依赖（scheduler → model）。

## 2026-11-15 已裁决并落地

### C-054 同题跨档到期题是否应优先（已裁决：方案 A）

- **关联决策/规格**：D-02；`docs/specs/scheduler.md` 的档位差排序键与用例 S-18。
- **现象**：一道题关联的多个知识点都在本期到期、且分属不同掌握档位时，规格文字会把它排到当天最前；但 S-18 要求“只关联较高档位、且该档位内掌握度更低的另一道题”排在它前面。两者不能同时按字面成立。
- **对用户的实际影响**：
  - 若“跨档即优先”：容量紧张时，学生先做横跨强弱档的题，纯高档位的巩固题更容易被推迟；弱档点被保住。
  - 若“只奖励额外关联本期未到期的不同档位”：学生先做最高档位组里最薄弱的题；横跨两档但都已到期的题按该档位内的掌握度排队，弱档点可能随题一起被推迟。
  - 两种选择只改变同一天内的题目顺序、以及时间不足时哪道题被推迟；不改变到期判定、题目内容、每题时长或冷却。
- **可选方案（用户语言）**：
  - A：只有这道题还关联了“本期未到期、且档位不同”的知识点时，才把它提前；同题里都在本期到期的不同档位不额外加分。
  - B：只要一道题覆盖多个档位就提前，即使这些知识点都在本期到期。
  - C：A 的明确写法，效果与 A 相同。
- **选型参考（以用户体验为依据）**：若希望“一题顺带复习本期未到期的不同档位知识点”才值得优先，并避免弱档点仅因同题到期就抬高高档位巩固题的优先级，选 A/C；若希望“覆盖多个档位的题总是先做”，选 B，但需同步调整 S-18 的期望。2026-11-15 用户选择方案 A。
- **落地**：`DefaultScheduler` 的档位差只比较“本期到期”与“本期未到期但仍会打印”的关联知识点；同题内都在本期到期的不同档位不计档位差。S-04、S-05、S-18 覆盖；无行为变更（上一轮实现即 A），2026-11-15 仅更新注释与文档。
- **规格/模板同步（2026-11-15）**：按人类授权回写 `docs/specs/scheduler.md` 的 `hasTierGap` 定义（边界输入段与算法分层第 1 键），并同步 `docs/prompts.md` 的冲突模板，加入“对用户的实际影响”与“以用户影响为依据的简短选型参考”。
- **裁决前禁止事项（已解除）**：不再适用；后续若改为“含其它 triggered”，需新开冲突条目。

### C-055 关联知识点失效时如何处理（已裁决：方案 C）

- **关联决策/规格**：D-11、D-12；`docs/specs/scheduler.md` 的边界输入与第 0 层到期判定；`docs/specs/data-model.md` 的知识点删除前置条件。
- **现象**：题目关联的知识点 id 不在本次知识点集合里时，旧实现静默丢弃；只剩这类关联的题整题不出现，还有其它到期点的题会排入但少一行。
- **对用户的实际影响**：学生可能整周看不到某道题，或打印稿少一个应复习的知识点行，且没有任何提示。
- **可选方案**：A. 静默忽略；B. 视为非法输入报错；C. 整题跳过并留下可见记录；D. 规格声明前置条件。
- **裁决**：2026-11-15 用户选择 **C**（整题跳过 + 可见记录）。
- **落地**：`DeferReason` 增加 `UNRESOLVED_KNOWLEDGE`；`DefaultScheduler` 在归档跳过与 >4 校验之后，若任一关联 id 解析不到，整题跳过并生成 `DeferredItem(knowledgePointIds = 未解析 id 字典序升序, targetNextDueDates = emptyMap())`；deferred 顺序为 `COOLING → TIME_OVERFLOW → UNRESOLVED_KNOWLEDGE`。
- **规格同步**：`scheduler.md` 的接口枚举、边界输入、deferred 顺序、用例表 S-20。
- **测试**：`S-20 unresolved knowledge id skips the whole question...` 覆盖“即使还有其它到期点也整题跳过”与 deferred 记录、reason 顺序。
- **裁决前禁止事项（已解除）**：不再适用；不得改回静默丢弃或补默认知识点。

### C-056 重复知识点/题目 id 的口径（已裁决：方案 A）

- **关联决策/规格**：D-01、D-12、C-049；`docs/specs/scheduler.md` 输出稳定性与“同一题至多一次”；`docs/specs/data-model.md` 主键只约束库表。
- **现象**：旧实现对重复知识点 id 后写覆盖先写、关联先按原始条目数校验 >4 再去重、题目列表完全不去重，三套规则均未写明。
- **对用户的实际影响**：同一题可能印两次、挤掉别的题；同一数据因加载顺序不同排到不同日期；重复关联把条目数推到 5 时整周失败。
- **可选方案**：A. 规格声明 id 唯一、由数据层保证；B. 调度器防御性归一化；C. 任何重复报错；D. 保留现状。
- **裁决**：2026-11-15 用户选择 **A**。
- **落地**：移除旧的 `.distinct()`，调度器直接使用 `linkedKnowledgeIds`，不再去重/合并；在 `scheduler.md` 声明 `knowledgeStates`/`questions`/`linkedKnowledgeIds` 的 id 必须唯一，由 `data:room` 构造 DTO 时保证；`repository.md` 同步该前置条件。重复输入的行为不在规格定义范围内。
- **测试**：属性测试与显式用例只使用唯一 id；不为未定义输入新增断言。
- **裁决前禁止事项（已解除）**：不再适用；若将来数据层无法保证唯一，需新开冲突条目。

### C-057 冷却天数为 0 或负数（已裁决：方案 D）

- **关联决策/规格**：D-04；`docs/specs/scheduler.md` 冷却判定；`planning.md` 的可传参数；`settings.md` 固定 7 天。
- **现象**：旧实现按字面做日期加法，0/负数会让冷却静默失效。
- **对用户的实际影响**：正常设置不触发；一旦误传，刚复习的题第二天又出现，冷却休息期被无声取消。
- **可选方案**：A. 按字面计算；B. 非正数报错；C. 夹到默认值；D. 规格明确 0 = 关闭、负数为非法。
- **裁决**：2026-11-15 用户选择 **D**。
- **落地**：`DefaultScheduler` 在入口 `require(coolingPeriodDays >= 0)`；`isInCoolingPeriod` 以 `coolingPeriodDays > 0` 短路，**0 无条件关闭冷却**（即使记录的实际复习日晚于目标日），负数抛 `IllegalArgumentException`。`scheduler.md`、`planning.md`、`settings.md` 同步语义：设置页固定 7 天，0 仅内部/测试使用。
- **测试**：`S-22 zero cooling period disables cooling and negative is illegal`。
- **裁决前禁止事项（已解除）**：不再适用；不得把冷却天数改成用户可配或修改 D-04 的默认值。

### C-058 “>4 必报错”与“空范围/归档题”的优先级（已裁决：方案 A）

- **关联决策/规格**：D-11、D-12；`scheduler.md` 边界输入与第 0 层。
- **现象**：旧实现空范围先短路、非空范围先校验 >4 再跳过归档题；规格未写优先级。
- **对用户的实际影响**：坏数据在空范围下被藏起来，正常范围突然整周失败；一条归档旧题可能让本周无法生成计划。
- **可选方案**：A. 先跳过不参与计划的对象（空范围、归档题）再校验；B. 先校验全部输入；C. 只校验会进入候选的题。
- **裁决**：2026-11-15 用户选择 **A**。
- **落地**：`DefaultScheduler` 先校验 `coolingPeriodDays` 非负，空范围随后短路；每道题先 `if (archived) continue`，再对未归档题做 >4 校验与关联完整性检查。
- **规格同步**：`scheduler.md` 边界输入的“校验优先级”、用例 S-21。
- **测试**：`S-21 archived question with too many links...` 与 `S-21 empty date range with too many links...`；S-10 继续锁定未归档 >4 抛异常。
- **裁决前禁止事项（已解除）**：不再适用；不得静默截断为前 4 个。

### C-059 “档位差优先”与 S-05 文字冲突（已裁决：方案 B）

- **关联决策/规格**：D-02、D-03、C-054；`scheduler.md` 排序层与 S-05。
- **现象**：旧排序把档位差放在最高档位之前，低档位跨档题会越过只触发高档位、组内更薄弱的题，与 S-05 文字相反。
- **对用户的实际影响**：容量只够一题时，学生可能先做低档位但顺带覆盖别的档位的题，最高档位里更薄弱的题被推迟。
- **可选方案**：A. 保持档位差优先并澄清 S-05；B. 最高档位优先于档位差；C. 只有跨到更低档位才加分。
- **裁决**：2026-11-15 用户选择 **B**。
- **落地**：候选排序键改为 `groupTier → hasTierGap → groupScore → targetDate → questionId`；高档位组不得被低档位题以任何理由越过，档位差只在同档位组内作第二键。
- **规格同步**：`scheduler.md` 算法分层第 2 层与 S-04 断言。
- **测试**：S-04 改为同档位组内“有档位差 vs 无档位差”的区分用例；新增 `C-059 higher group tier precedes a lower tier question...`。S-05/S-18 仍锁定不得跨档混分。
- **裁决前禁止事项（已解除）**：不再适用；不得把档位差改成淘汰条件。

## 已裁决但待验证（非新冲突）

- C-050 的 `Double` 内部算术与 `Float` 边界转换，需在 `ApplyFeedbackUseCase` 实现后用 U-01～U-19 验证。
- C-051 方案 A 的 R-01 spike 结果待验证：850×540 → 2480×3508 适配后的 OMR/QR 精度必须用真实打印稿判断；不达标时按 `scan-pipeline.md` 转方案 B（fork）。这不改变已裁决的 A 方案，只记录验证门禁。

## 历史引用

- C-001～C-005 的原始裁决记录不在仓库；ADR 已删除，历史裁决仅存于 `docs/agent-log/2026-11-11.md` 与 Blurt H-0001。
