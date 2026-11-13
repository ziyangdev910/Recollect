# 调度器规格（core/scheduler）

关联决策：D-01、D-02、D-03、D-04、D-05、D-11、D-12
关联裁决：C-006、C-007、C-009、C-017、C-019、C-020、C-021、C-046、C-049、C-050

## 冻结决策（D-01～D-05）

- **D-01**：调度单元是知识点，题目是载体；同一题只打印一次；触发知识点正常更新；非触发关联知识点按 0.3 权重更新连续掌握度增量，tier/nextDueDate 不变；非触发行独立填涂。
- **D-02**：档位差至少一档只作排序权重，不淘汰候选；所有到期题目进入候选池。
- **D-03**：`continuousScore` 仅同档位内排序与时间装箱；掌握 +0.3、较熟悉 −0.1、不会 −0.3，夹在 [0,1]；不得替代档位比较。C-050：状态更新算术在 UseCase 内部用 `Double`，对外/落库为 `Float`。
- **D-04**：冷却期优先于到期触发；默认 7 天，期间知识点顺延并标记「因冷却顺延」；一次性 `CoolingOverrideEvent` 才可突破。
- **D-05**：冷启动 1/3/7 天；FAMILIAR 按 0.5 次；第 3 条 triggered 反馈后初始化 `difficulty/halfLifeDays`；有效反馈 ≥ 3.0 后从下一次反馈起用墨墨 DHP（FAMILIAR 半强度、90% + 30 天上限）；不实现完整 SSP-MMC 策略表。

## 职责边界

`core/scheduler` 只负责「给定知识状态与题目，生成某个日期范围内的计划」：纯 Kotlin，不读写数据库、不写反馈、不更新知识点状态。回读确认后的 `currentTier / continuousScore / nextDueDate / effectiveFeedbackCount` 更新属于 data/UseCase，见 `docs/specs/feedback-usecase.md`（C-019 方案二）。

## 接口

```kotlin
enum class Tier { UNKNOWN, FAMILIAR, MASTERED }   // 由弱到强；groupTier 取最高档位

interface Scheduler {
    fun plan(
        knowledgeStates: List<KnowledgeState>,
        questions: List<SchedulerQuestion>,
        dailyAvailableMinutes: Map<LocalDate, Int>,
        dateRange: ClosedRange<LocalDate>,
        coolingPeriodDays: Int = 7,
        coolingOverrides: Set<String> = emptySet()  // C-008 方案二：未消费的一次性覆盖事件 questionId
    ): PlanResult
}

data class KnowledgeState(
    val id: String,
    val number: Int,
    val currentTier: Tier,
    val continuousScore: Float,             // [0,1]；对外 Float，内部算术用 Double（C-050）
    val nextDueDate: LocalDate,
    val effectiveFeedbackCount: Float,      // 0.5 的倍数；见 C-006
    val dormant: Boolean,
    val activeQuestionCount: Int,
    val difficulty: Int?,                   // 墨墨 DHP；冷启动阶段 null（C-018）
    val halfLifeDays: Float?,               // 墨墨 DHP；冷启动阶段 null（C-018）
    val createdAt: Instant                  // DHP 第一段间隔起点（C-037）
)

data class SchedulerQuestion(
    val id: String,
    val linkedKnowledgeIds: List<String>,   // 1..4；由 data/room 从 cross-ref 映射（C-046）
    val archived: Boolean,
    val lastReviewedDate: LocalDate?,       // 实际复习日；冷却判定用（C-039）
    val estimatedReviewMinutes: Int = 15    // C-017；缺失或 <=0 按 15 分钟
)

data class PlanResult(
    val dailyAssignments: Map<LocalDate, List<PlannedQuestion>>,  // 按日期升序的 LinkedHashMap
    val deferred: List<DeferredItem>
)

data class PlannedQuestion(
    val questionId: String,
    val triggeredKnowledgeIds: List<String>,      // 本计划范围内到期
    val nonTriggeredKnowledgeIds: List<String>,   // 关联但未到期；不含休眠知识点
    val estimatedMinutes: Int
)

data class DeferredItem(
    val questionId: String,
    val reason: DeferReason,                      // COOLING / TIME_OVERFLOW
    val knowledgePointIds: List<String> = emptyList(),
    val targetNextDueDates: Map<String, LocalDate> = emptyMap()  // 仅 COOLING 非空
)

enum class DeferReason { COOLING, TIME_OVERFLOW }
```

- `SchedulerQuestion` 是 `core:model` 的纯 DTO（C-046 方案 A）；Room `@Entity Question` 不直接进入 scheduler，由 `data:room` 通过 cross-ref 映射得到 `linkedKnowledgeIds`。
- 实现类固定为 `class DefaultScheduler : Scheduler`，不依赖数据库、时钟或 Android。

## 确定性与边界输入（C-049）

- **id 列表顺序**：`triggeredKnowledgeIds`、`nonTriggeredKnowledgeIds`、`DeferredItem.knowledgePointIds` 一律按字符串字典序升序；`PlanResult.dailyAssignments` 的题目顺序按第 2 层排序键确定。
- **deferred 顺序**：先按 `reason`（`COOLING` 在 `TIME_OVERFLOW` 前），同 reason 内按 `questionId` 字典序升序。
- **TIME_OVERFLOW**：`knowledgePointIds = emptyList()`、`targetNextDueDates = emptyMap()`；冷却顺延只属于 `COOLING`。
- **关联数量**：`linkedKnowledgeIds` 为空时该题跳过，不进入候选；数量 > 4 时抛 `IllegalArgumentException`（D-12）。
- **hasTierGap**：比较 triggered 知识点与所有 printable 关联知识点（含其它 triggered；`!dormant && activeQuestionCount > 0`）；只要存在档位差 ≥ 1 即为 `true`；没有可比较对象时为 `false`。
- **日期范围**：`dateRange.start > dateRange.endInclusive` 时返回空 `PlanResult`（`dailyAssignments` 与 `deferred` 都为空），不抛异常。
- **容量与时长**：`dailyAvailableMinutes` 缺失按 0，负值也按 0；`estimatedReviewMinutes` 缺失或 <= 0 按 15。
- **输出稳定性**：相同输入（无论 List/Map 迭代顺序）必须产生相同输出；不得依赖输入顺序作为 tie-break。

## 算法分层（必须按此顺序实现）

### 0. 候选与目标日（C-020 方案一）

对每个未归档题目 `q`：

- `dueInRange(q)` = `q.linkedKnowledgeIds` 中满足以下全部条件的知识点：
  - 未休眠；
  - `activeQuestionCount > 0`；
  - `nextDueDate <= dateRange.endInclusive`。
- 若 `dueInRange(q)` 为空，`q` 不进入候选。
- `targetDate(q) = max(min(nextDueDate ∈ dueInRange(q)), dateRange.start)`。
  - 早于 `dateRange.start` 的逾期知识点统一落到计划首日，不得丢弃。
  - 晚于 `dateRange.endInclusive` 的知识点不作为 `triggered`，也不影响 `targetDate` 与冷却判定；若题目因其他知识点成为候选，它仍作为非触发行打印（C-009）。
- **同一题目在一份计划内只出现一次**。若其多个关联知识点在 `dateRange` 内到期，全部计入 `triggeredKnowledgeIds`，题目排入最早的 `targetDate`；不再因后续到期日重复排入（D-01「同一题目只打印一次」）。
- `nonTriggeredKnowledgeIds` = 其余关联知识点中未休眠、`activeQuestionCount > 0` 的集合；它们不参与到期判定，但会打印为非触发行（C-009）。

### 1. 到期/冷却判定层

- 到期对象是知识点，载体是题目；`dueInRange(q)` 非空才继续。
- 题目在冷却期内：`q.id !in coolingOverrides && lastReviewedDate != null && lastReviewedDate + coolingPeriodDays > targetDate(q)`。`lastScheduledDate` 只作审计/展示，不参与冷却（C-039）。
- `coolingOverrides` 是 C-008 方案二的一次性事件集合：命中的题目本次跳过冷却，但只在本次 `plan()` 生效；调用方（feature/planning）负责在题目实际排入计划后消费事件。
- 冷却期优先于到期触发（D-04）：题目不排入，生成
  `DeferredItem(q.id, COOLING, triggeredKnowledgeIds, targetNextDueDates)`。
  - `targetNextDueDates[k] = nextDueDate(k) + 1 day`，作用于全部触发知识点。
  - 每份计划各顺延 1 天；若下一份计划仍处于冷却期，再 +1 天。顺延的触发契机只有「冷却期」，不是通用惩罚。
- 事件缺失、已消费或题目不在冷却期时，`coolingOverrides` 不影响结果。

### 2. 候选排序层（C-007 方案一）

按以下键升序（逐级比较，全部为确定性排序）：

1. `hasTierGap(q)`：触发知识点与任一关联知识点存在档位差 ≥ 1 档时为 `true`，排在 `false` 之前（D-02：只作排序权重，不淘汰）。
2. `groupTier(q)`：触发知识点中的**最高档位**（`MASTERED > FAMILIAR > UNKNOWN`）在前；这样低档位高连续分不会排到高档位低连续分之前（S-05）。
3. `groupScore(q)`：仅在该 `groupTier(q)` 组内，取触发知识点 `continuousScore` 的最小值升序；不得把其它档位知识点的分数混入该组。
4. `targetDate(q)` 升序。
5. `questionId` 字典序升序（最终稳定 tie-break）。

**禁止**：跨 `groupTier` 组比较 `continuousScore`。连续掌握度只在同档位组内使用（D-03）。

### 3. 时间装箱层（C-017、C-020）

- 按 `dateRange` 中的日期升序逐日装箱。
- 当日候选集合 = `{ q | targetDate(q) == date }`；候选只在其唯一目标日尝试，**不得顺延到后续日期**（C-020）。
- 当日容量 `dailyAvailableMinutes[date]`；缺失视为 `0`。
- 当日候选按第 2 层排序依次尝试 first-fit：
  - 剩余容量 ≥ `estimatedReviewMinutes`：排入；
  - 否则：生成 `DeferredItem(q.id, TIME_OVERFLOW)`，继续尝试后续候选（不因一个大条目停止整日装箱）。
- `estimatedReviewMinutes` 从 `Question` 读取；缺失或 ≤ 0 时按 15 分钟处理。
- 同一题目、同一知识点在一份计划内不重复排入。
- 输出 `dailyAssignments` 按日期升序；没有条目的日期不出现在 Map 中。

## 状态更新规则（已迁移，C-019）

本模块不承载回读确认后的状态更新。以下内容见 `docs/specs/feedback-usecase.md`：

- 触发知识点的 `currentTier / continuousScore / nextDueDate / effectiveFeedbackCount` 更新；
- 非触发关联知识点的 ×0.3 连续掌握度增量；
- `ReviewFeedback.effectiveWeight / weightApplied`；
- 冷启动 1/3/7 天阶梯与切换到记忆模型的边界；
- 冷却顺延 `targetNextDueDates` 的落库。

调度器只返回计划与 `DeferredItem`；`feature/planning` 在计划生成后调用 `ApplyPlanDeferralsUseCase` 应用顺延。

## 用例表（测试必须逐条覆盖）

| ID | 场景 | 断言 |
|---|---|---|
| S-01 | 知识点到期，关联题目不在冷却期 | 排入 `targetDate`，`triggeredKnowledgeIds` 含该知识点 |
| S-02 | 知识点到期，关联题目在冷却期 | 不排入；`Deferred(COOLING)`；`knowledgePointIds` 含全部触发知识点；`targetNextDueDates` 每个 +1 天 |
| S-03 | 一题关联 A（到期）、B（未到期） | 只排入一次；`triggered=[A]`，`nonTriggered=[B]`；状态更新断言见 U-03 |
| S-04 | 两候选，一有档位差、一无档位差 | 有档位差在前；无档位差不淘汰 |
| S-05 | 每日时间超额；跨档位连续分反例 | 按第 2 层排序 first-fit；低档位高连续分不得排到高档位低连续分之前；未排入记 `TIME_OVERFLOW` |
| S-08 | 归档条目 | 不进入候选 |
| S-09 | 知识点休眠（activeQuestionCount=0） | 不调度；取消归档后恢复 |
| S-10 | 单题关联 > 4 知识点 | 录入层拦截；调度器收到时抛 `IllegalArgumentException` |
| S-11 | 非触发知识点 | `tier/nextDueDate/effectiveFeedbackCount` 不变；状态更新断言见 U-11 |
| S-12 | `effectiveFeedbackCount` 浮点边界 | 2.5 → 3.0 达到阈值；达到的这次仍按冷启动，下一次切换；3.0 与 3.0000001 不做容差；见 U-12 |
| S-13 | dateRange 跨多日，含逾期候选 | 逾期落到 `dateRange.start`；只排入一次；`dailyAssignments` 日期升序 |
| S-14 | 一题多个知识点在同一 dateRange 内到期 | 只排入最早 `targetDate` 一次；全部到期知识点均为 triggered |
| S-15 | `estimatedReviewMinutes` 缺失/自定义 | 缺失按 15 分钟；自定义值参与累加 |
| S-16 | 输入 Map 顺序打乱 | 输出与输入顺序无关，按日期升序 |
| S-17 | 第 1 天候选溢出、第 2 天仍有候选 | 第 1 天溢出的候选记 `TIME_OVERFLOW`，不占用第 2 天容量；第 2 天候选不填补第 1 天剩余容量 |
| S-18 | Q1 同时触发 MASTERED 0.9 与 UNKNOWN 0.1，Q2 触发 MASTERED 0.5 | Q1 的组内分数只能取 MASTERED 的 0.9；Q2（0.5）在前。不得用 UNKNOWN 的 0.1 把 Q1 排到 Q2 之前 |
| S-19 | 题目在冷却期，但 `coolingOverrides` 含其 id | 本次跳过冷却并正常排入；未命中/已消费的 id 不生效；下一份计划不再自动跳过 |

> `S-06`、`S-07` 的冷启动/切换断言已迁移至 `feedback-usecase.md` 的 `U-06`、`U-07`；本模块只测计划，不测状态更新（C-019）。

## 验收门禁

- `./gradlew :core:scheduler:test` 全绿，无 `@Ignore`
- 每条测试断言可追溯到本文件用例表或 D-01～D-05
- 属性测试：JUnit `@RepeatedTest(100)` + `kotlin.random.Random`（固定种子），断言：
  - 归档/休眠知识点不出现在任何 `dailyAssignments`
  - 同一题目同一日不重复出现
  - 同一题目在一份计划内至多出现一次
  - `dailyAssignments` 的日期键严格升序

## 明确不做的事

- 不实现记忆模型参数训练、半衰期拟合或 SSP-MMC 值迭代；C-018 已确认采用墨墨 DHP 的简化版，`nextDueDate` 的记忆模型计算放在纯 Kotlin 的 `NextDueCalculator`，由 UseCase 调用，不放进 `plan()`。
- 不实现完整 SSP-MMC 最优策略表；首版用 90% 保持率阈值公式（见 `docs/research/maimemo-model.md`）。
- 不引入难度自评、目标半衰期等用户参数（`estimatedReviewMinutes` 除外）。
- 不读写数据库；不写 `ReviewFeedback`；不更新知识点状态。
