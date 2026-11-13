# 回读确认后的状态更新 UseCase 规格（data/domain）

关联决策：D-01、D-03、D-04、D-05、D-11、D-13
关联裁决：C-006、C-009、C-010、C-018、C-019、C-021、C-022、C-050

## 冻结决策（D-01、D-03、D-04、D-05）

- **D-01**：调度单元是知识点；触发知识点正常更新；非触发关联知识点按 0.3 权重更新连续掌握度增量，tier/nextDueDate 不变；非触发行独立填涂。
- **D-03**：`continuousScore` 仅同档位内使用；掌握 +0.3、较熟悉 −0.1、不会 −0.3，夹在 [0,1]。C-050：算术在 UseCase 内部用 `Double`，写入 Room / 构造 `KnowledgeState` 时转 `Float`；`MemoryStateInitializer`/`NextDueCalculator` 的入参仍为 `Float`，实现内部可转 `Double`；`effectiveFeedbackCount` 阈值保持精确 `>= 3.0f`，不做 epsilon。
- **D-04**：冷却期优先于到期触发；默认 7 天；一次性 `CoolingOverrideEvent` 覆盖。
- **D-05**：冷启动 1/3/7、FAMILIAR 0.5 次；第 3 条 triggered 反馈后初始化 DHP 状态；≥3.0 后下一次反馈起用墨墨 DHP（FAMILIAR 半强度、90% + 30 天）。

## 为什么在 UseCase（C-019 方案二）

`core/scheduler` 保持纯计划：输入知识状态与题目，输出计划。回读确认后的状态更新需要读写 Room、写 `ReviewFeedback`、写确认决定日志，并依赖时钟与事务；这些属于 data/domain 的 UseCase。落点：

- `ApplyFeedbackUseCase`：人工确认提交后，按行写反馈并更新知识点状态。
- `ApplyPlanDeferralsUseCase`：计划生成后，把 `DeferredItem.targetNextDueDates` 落到知识点。
- `core/scheduler` 不再声明 `applyFeedback`，也不接收待确认反馈。

### 三方案对比（C-019）

| 方案 | 做法 | 优点 | 代价 |
|---|---|---|---|
| 一 | 在 `Scheduler` 增加 `applyFeedback(...)` | 调度相关逻辑集中在一个模块；调用点少 | `core/scheduler` 被迫依赖反馈/Room 语义，破坏纯计划边界；S-03/S-11 与计划测试混在一起；后续接记忆模型时更难拆分 |
| 二（采用） | 状态更新移出 scheduler，另立 UseCase 规格 | `core/scheduler` 纯 Kotlin、可独立测试；R-04 的写入门槛有明确落点；便于事务、审计与备份 | 需要新增 UseCase 与仓储接口；调用链多一层 |
| 三 | `plan()` 接收待确认反馈并返回 delta | 一次调用完成计划与更新 | `plan()` 语义膨胀；计划输入混入未确认反馈，违反 R-04 的「先确认后写」；无法区分「计划」与「执行」 |

## 接口

```kotlin
class ApplyFeedbackUseCase(
    private val knowledgeRepository: KnowledgePointRepository,
    private val feedbackRepository: ReviewFeedbackRepository,
    private val decisionLogRepository: ConfirmationDecisionLogRepository,
    private val pendingFeedbackRepository: PendingFeedbackRepository,
    private val nextDueCalculator: NextDueCalculator,
    private val clock: Clock
) {
    suspend fun execute(
        sessionId: String,
        decisions: List<ConfirmedDecision>
    ): ApplyResult
}

data class ConfirmedDecision(
    val planId: String,
    val pageId: String,
    val plannedDate: LocalDate,
    val actualReviewDate: LocalDate?,    // 默认 = plannedDate；NOT_REVIEWED 可为 null（C-038/C-039）
    val questionId: String,
    val knowledgePointId: String,
    val triggered: Boolean,
    val decision: UserDecision,
    val detectedTier: Tier?,
    val selectedTier: Tier?,
    val detectedRawOcr: String?,
    val correctedText: String?,
    val isSupplemental: Boolean
)

data class ApplyResult(
    val reviewFeedbackIds: List<Long>,
    val updatedKnowledgeStates: List<KnowledgeState>
)

data class MemoryState(
    val difficulty: Int,
    val halfLifeDays: Float
)

data class NextDueResult(
    val nextDueDate: LocalDate,
    val memoryState: MemoryState?
)

data class TriggeredFeedback(
    val tier: Tier,
    val intervalDays: Int                 // 距上次复习/创建的天数；缺失时按冷启动 1/3/7
)

interface MemoryStateInitializer {
    fun initial(
        recentTriggeredFeedbacks: List<TriggeredFeedback>,
        continuousScoreAfterThird: Float
    ): MemoryState
}

interface NextDueCalculator {
    fun nextDueDate(
        memoryState: MemoryState?,
        tier: Tier,
        continuousScoreAfter: Float,
        feedbackEventCountAfter: Int,
        effectiveFeedbackCountBefore: Float,
        reviewedOn: LocalDate
    ): NextDueResult
}
```

> **C-018 已确认（2026-11-13）**：采用墨墨 DHP 简化版；FAMILIAR 用半强度回忆；90% 保持率 + 30 天上限；新增 `difficulty`/`halfLifeDays` 字段。冷启动阶段 `memoryState = null`；第 3 条 triggered 反馈后由 `MemoryStateInitializer` 初始化，之后 `NextDueCalculator` 返回更新后的状态；有效反馈达到 3.0 后从下一次反馈起用 DHP。完整 SSP-MMC 策略表仍不实现。

## 前置条件（任一不满足即抛 `InvalidConfirmationException`，事务内不写任何行）

- `decisions` 非空；每行 `decision != UserDecision.UNMARKED`（C-010）。
- 每行 `questionId + knowledgePointId` 属于该题关联知识点；`triggered` 与计划一致。
- `ACCEPT` 要求 `detectedTier != null || selectedTier != null`（C-022：OMR 为空时可用 `selectedTier` 补足）；`CORRECTED`/`SUPPLEMENTED` 要求 `selectedTier != null`。
- 归属校验已由 scan/confirm 完成（C-011）；本 UseCase 再校验一次 `PendingFeedback` 会话存在且行集合完整。
- 同一 `(questionId, knowledgePointId)` 在 `decisions` 中最多出现一次。
- 所有 `decisions` 属于同一 `sessionId` 的 `ScanSession`；可跨多页，`planId` 一致。
- `plannedDate` 与 `PendingFeedback.plannedDate` 一致。
- 非 `NOT_REVIEWED` 行：`actualReviewDate != null`、`>= 计划周起始日`、`<= 今天`；`NOT_REVIEWED` 行 `actualReviewDate` 可为 `null`，若提供也不能晚于今天。

## 处理顺序（单事务）

对每一行：

1. 写 `ConfirmationDecisionLog`：所有决策都写，包括 `NOT_REVIEWED`（C-010 的审计痕迹）；含 `planId/pageId/plannedDate`，`reviewedOn = if (decision == NOT_REVIEWED) null else actualReviewDate`，`decidedAt = clock.now()`。
2. 若 `decision == NOT_REVIEWED`：不写 `ReviewFeedback`，不改知识点状态，继续下一行。
3. 解析档位：
   - `ACCEPT`：`effectiveTier = detectedTier ?: selectedTier`（优先 OMR，OMR 为空时用用户选的档位）；
   - `CORRECTED` / `SUPPLEMENTED`：`effectiveTier = selectedTier`。
4. 写 `ReviewFeedback`：
   - `planId = planId`、`pageId = pageId`
   - `plannedDate = plannedDate`、`reviewedOn = actualReviewDate`
   - `tier = effectiveTier`
   - `triggered = triggered`（非触发行也写反馈，但 `triggered=false`，不推进冷启动）
   - `rawOcrText = detectedRawOcr`（只读，D-06）
   - `correctedText = correctedText`（可编辑）
   - `isSupplemental = isSupplemental`
   - `effectiveWeight = if (effectiveTier == FAMILIAR) 0.5f else 1.0f`（C-006）
   - `weightApplied = if (triggered) 1.0f else 0.3f`（审计字段，不反向驱动）
   - `timestamp = clock.now()`
5. 更新知识点状态：
   - **触发知识点**：
     - `currentTier = effectiveTier`
     - `continuousScore = clamp(continuousScore + delta(effectiveTier), 0.0, 1.0)`，
       `delta(MASTERED)=+0.3`、`delta(FAMILIAR)=-0.1`、`delta(UNKNOWN)=-0.3`（D-03）。
       算术用 `Double`，落库/构造 DTO 时 `toFloat()`（C-050）。
     - `effectiveFeedbackCount += effectiveWeight`（C-006）
     - `val due = nextDueCalculator.nextDueDate(... reviewedOn = actualReviewDate)`；`nextDueDate = due.nextDueDate`
     - `due.memoryState != null` 时写回 `difficulty = memoryState.difficulty`、`halfLifeDays = memoryState.halfLifeDays`；为 `null` 时保持原值
   - **非触发关联知识点**（C-009）：
     - `currentTier` 不变
     - `continuousScore = clamp(continuousScore + delta(effectiveTier) * 0.3, 0.0, 1.0)`
       —— 使用**该行自己填涂的档位**，不借用触发行档位；算术用 `Double`（C-050）
     - `nextDueDate` 不变
     - `effectiveFeedbackCount` 不变
   - **NOT_REVIEWED**：以上全部不变（C-010）。
   - **题目级**：任一非 `NOT_REVIEWED` 行成功后，`Question.lastReviewedDate = max(现有值, actualReviewDate)`；`NOT_REVIEWED` 不更新。
6. 提交成功后：删除该 `sessionId` 的 `PendingFeedback`；`ScanSession.status=SUBMITTED`；`ConfirmationDecisionLog` 与 `ReviewFeedback` 保留。

## 冷启动与切换边界（D-05、C-006、C-018）

- `feedbackEventCount` = 该知识点在本次提交前的 `ReviewFeedback` 中 `triggered = true` 的条数 + 1（`NOT_REVIEWED` 不写 `ReviewFeedback`）；**非触发行 `triggered = false`，不推进冷启动阶梯**。`ConfirmationDecisionLog` 仅作审计，不参与计数。
- `effectiveFeedbackCountBefore/After` 为 Float，值始终是 0.5 的倍数；比较阈值为 `>= 3.0f`，不做 epsilon 容差（0.5 在二进制浮点中可精确表示）。
- 冷启动间隔按**反馈事件序号**取阶梯：

  | 事件序号 | 间隔 |
  |---|---|
  | 1 | 1 天 |
  | 2 | 3 天 |
  | 3 | 7 天 |
  | >3 且 effectiveFeedbackCountBefore < 3.0 | 7 天（继续冷启动，直到有效计数达标） |
  | >3 且 effectiveFeedbackCountBefore >= 3.0 | 记忆模型（C-018） |

- 「累计 3 次后切换」的精确语义：**达到 3.0 的这次反馈仍按冷启动设置下一次到期日；从下一次反馈开始使用记忆模型**。这样第 3 次反馈后的 7 天间隔不会被跳过，也不会提前切换。
- 全 MASTERED：第 3 次后 `effectiveFeedbackCount = 3.0`；第 4 次反馈使用记忆模型。
- 全 FAMILIAR：每次 +0.5；第 3 次后为 1.5，继续按 7 天；第 6 次后达到 3.0，第 7 次反馈使用记忆模型。
- C-018 已确认（2026-11-13）：采用墨墨 DHP 简化版，FAMILIAR 用半强度回忆，90% 保持率 + 30 天上限，新增 `difficulty`/`halfLifeDays` 字段。
- **记忆状态初始化**：在第 3 条 triggered `ReviewFeedback` 写入后，由 `MemoryStateInitializer` 用前 3 条反馈的档位、实际间隔与第 3 次后的 `continuousScore` 计算 `d_init`/`h_init`（公式见 `docs/research/maimemo-model.md` 第 4 节），写入 `KnowledgePoint.difficulty/halfLifeDays`；初始化已包含第 3 条反馈。`NextDueCalculator` 在 `feedbackEventCountAfter == 3` 且状态刚初始化时只返回冷启动 `nextDueDate`，不重复更新状态；此后的 triggered 反馈继续用 DHP 更新。
- **切换边界**：达到 3.0 的这次反馈仍按冷启动设置 `nextDueDate`；从下一次反馈起，`NextDueCalculator` 用 DHP 计算 `interval = clamp(round(-halfLifeDays * log2(0.9)), 1, 30)`。在 3.0 之前，DHP 状态继续由 Calculator 更新，但 `nextDueDate` 仍用 1/3/7 阶梯。
- **DHP 更新**：MASTERED/FAMILIAR 用 recall 公式（FAMILIAR 取半强度），UNKNOWN 用 forget 公式且 `difficulty = min(difficulty + 2, 18)`；具体系数见研究稿第 2 节。
- 不实现完整 SSP-MMC 策略表；不训练参数；`NextDueCalculator` 只做公式计算，不读数据库。

### DHP 边界（C-037）

- **前三次间隔**：`TriggeredFeedback.intervalDays` 第 1 条 = `max(1, days(createdAt.toLocalDate(), firstReviewDate))`；第 2/3 条 = 相邻 triggered `ReviewFeedback.timestamp` 的天数差，至少 1；上限 365。`createdAt` 缺失或历史不足 3 条时，缺失间隔按 1/3/7 兜底并标记 `HISTORY_INCOMPLETE`。
- **延迟复习**：冷启动阶段 `nextDueDate` 仍按事件序号 1/3/7，不因晚复习改变阶梯；DHP 状态用**实际间隔** `t_i` 更新，晚复习不会被当成 1 天。达到 3.0 后 `nextDueDate = reviewedOn + clamp(round(-h*log2(0.9)), 1, 30)`，`reviewedOn` 是该次确认的实际本地日期。
- **d_init**：`clamp(roundHalfUp(1 + 9*(1 - avgTier) - 2*(continuousScoreAfterThird - 0.5)), 1, 10)`；`h0 = -1 / log2(max(0.925 - 0.05*d_init, 0.025))`。
- **h/d 更新**：MASTERED 用 recall；FAMILIAR 用 `h + 0.5*(recall_h - h)`；UNKNOWN 用 forget 且 `d = min(d+2, 18)`。`h` 夹在 `[0.5, 365]`，`d` 夹在 `[1, 18]`。
- **迁移兜底**：`difficulty/halfLifeDays` 为空且已有 ≥3 条 triggered 历史 → 下一次 triggered 反馈用最近 3 条初始化；历史不足 → 用当前 tier 重复 + 1/3/7 兜底并警告。
- **事务**：初始化/更新 `difficulty/halfLifeDays` 与 `ReviewFeedback`、`ConfirmationDecisionLog`、知识点状态、`PendingFeedback` 删除在同一事务；失败全部回滚。

## 冷却顺延应用（C-021）

`ApplyPlanDeferralsUseCase` 在 `plan()` 返回后执行：

- 对每个 `DeferredItem(reason = COOLING)`，按 `targetNextDueDates` 更新对应知识点的 `nextDueDate`。该值是调度器基于**当前** `nextDueDate + 1 天`计算的，不做二次推导。
- `TIME_OVERFLOW` 不修改知识状态，只影响本计划是否排入。
- 每份计划都应用一次；同一知识点在连续多份计划中处于冷却期时，每次 +1 天。
- 顺延标记「因冷却顺延」由 `DeferredItem(reason = COOLING)` 在计划的 deferred 区域展示；`PlannedQuestion` 不包含冷却顺延项，也不写 `ReviewFeedback`。

## 用例表

| ID | 场景 | 断言 |
|---|---|---|
| U-01 | 触发行 ACCEPT，detectedTier=MASTERED | ReviewFeedback.tier=MASTERED；effectiveWeight=1.0；continuousScore +0.3；count +1.0；nextDueDate 按事件序号推进 |
| U-02 | 触发行 CORRECTED，selectedTier=UNKNOWN | tier=UNKNOWN；continuousScore −0.3；rawOcrText 与 correctedText 都保存 |
| U-03 | 一题关联 A（触发）、B（非触发且已填涂） | 写两条 ReviewFeedback：A weightApplied=1.0，B weightApplied=0.3；B 的 continuousScore 增量 ×0.3，tier/nextDueDate/count 不变 |
| U-04 | 非触发行填 FAMILIAR | B 的 continuousScore 按 −0.1×0.3 更新；B 的 count 不变 |
| U-05 | 非触发行 NOT_REVIEWED | B 完全不变；不写 ReviewFeedback；写 ConfirmationDecisionLog |
| U-06 | 冷启动第 1/2/3 次反馈（MASTERED） | 间隔 1/3/7 天；第 3 次后 count=3.0 |
| U-07 | 达到 3.0 后的下一次反馈 | 使用 DHP 记忆模型；`nextDueDate = reviewedOn + clamp(round(-halfLifeDays * log2(0.9)), 1, 30)`；不回退固定间隔 |
| U-08 | 全 FAMILIAR 冷启动 | 第 1/2/3 次间隔 1/3/7；count 到第 6 次才 3.0；第 7 次切换 |
| U-09 | 存在 UNMARKED 行 | `execute` 抛异常，事务不写任何行 |
| U-10 | NOT_REVIEWED | 无 ReviewFeedback；有 ConfirmationDecisionLog；知识点状态不变 |
| U-11 | 非触发知识点更新（原 S-11） | tier 不变、nextDueDate 不变、count 不变、continuousScore 增量 ×0.3 |
| U-12 | 2.5 → 3.0 边界 | 2.5 时不切换；加 0.5 到 3.0 的这次仍按冷启动；下一次切换 |
| U-13 | 同一 session 多行 | 每行一条 ReviewFeedback（NOT_REVIEWED 除外）；提交后 PendingFeedback 清空 |
| U-14 | 冷却顺延 | 按 `targetNextDueDates` 更新；连续两份计划各 +1 天 |
| U-15 | ACCEPT 但 `detectedTier=null`、`selectedTier=FAMILIAR` | 写入非空 `ReviewFeedback.tier=FAMILIAR`；effectiveWeight=0.5；不得写空 tier |
| U-16 | 该知识点历史只有非触发行反馈 | 本次首次触发的间隔仍为 1 天；非触发 ReviewFeedback 条数不推进冷启动阶梯 |
| U-17 | 第 3 条 triggered 反馈后初始化记忆状态（全 MASTERED） | `difficulty/halfLifeDays` 非空；第 3 次仍按冷启动 7 天；第 4 次使用 DHP；FAMILIAR 半强度、UNKNOWN 难度 +2 |
| U-18 | 批量扫描：plannedDate=周一，actualReviewDate=周三 | `ReviewFeedback.reviewedOn=周三`；`lateDays=2`；`nextDueDate` 以周三为基准；`Question.lastReviewedDate=周三` |
| U-19 | 批量扫描：某题全部 `NOT_REVIEWED` | 不写 `ReviewFeedback`；`lastReviewedDate` 不变；下一份计划不因该题冷却而跳过 |

## 验收门禁

- `./gradlew :data:test`（UseCase 单元测试，使用内存 Room 或仓储 fake；不 mock 识别结果）
- 事务测试：任一前置条件失败时，`ReviewFeedback`、`ConfirmationDecisionLog`、知识点状态均无变化
- 状态更新断言全部可追溯到 D-01、D-03、D-05、D-13 与本文件用例表
- C-050：`continuousScore` 的内部期望值用 `Double` 同序计算；落库断言用 `Float` 精确转换结果；`effectiveFeedbackCount` 用精确 `>= 3.0f`，不引入 epsilon 容差
