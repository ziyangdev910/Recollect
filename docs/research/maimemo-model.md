# 墨墨背单词记忆模型研究与「前三次反馈 → 初始量」方案

- 关联：C-018（D-05 的切换目标）
- 状态：**已确认（2026-11-13）**：FAMILIAR 半强度回忆；接受 90% 保持率 + 30 天上限；新增 `difficulty`/`halfLifeDays` 字段。D-05 与 specs 已同步。
- 日期：2026-11-11（2026-11-13 确认）
- 来源：
  - Ye, Su, Cao. *A Stochastic Shortest Path Algorithm for Optimizing Spaced Repetition Scheduling*. KDD 2022, 4381–4390. <https://www.maimemo.com/paper/>
  - 官方开源实现：<https://github.com/maimemo/SSP-MMC>（`model/utils.py`、`simulator.py`、`cal_model_param.py`；MIT License）
  - 论文配套开源实现（SSP-MMC-Plus）：<https://github.com/maimemo/SSP-MMC-Plus>（MIT License）
  - 墨墨官方文档：<https://memodocs.maimemo.com/docs/algorithm-intro>、<https://memodocs.maimemo.com/docs/PulqwOB1xi5jV0kb8Y1cPE7ZnKc>

## 1. 墨墨模型是什么

墨墨的排程由两层组成：

1. **记忆模型（DHP）**：每个学习项维护难度 `d`、半衰期 `h`、当前回忆概率 `p`。遗忘曲线：
   `p(t) = 2^(-t/h)`。
2. **调度/建议规划**：根据 `d/h/p` 与目标记忆保持率，选择下次复习间隔；「每日学习量 = 当日复习量 + 当日新学量」，复习优先，新学量由剩余时间/精力决定，不固定新学数量。

本项目的「建议规划模型」落在第 1 层 + 第 2 层的间隔选择；每日学习量仍由 `estimatedReviewMinutes` 与用户每日可用分钟数约束（C-017/C-020），不照搬「新学量」概念。

## 2. 官方公式（逐字对应开源代码）

```text
start_h(d) = -1 / log2(max(0.925 - 0.05*d, 0.025))          # model/utils.py

p = 2^(-delta_t / h)

# 回忆成功（认识）
h' = h * (1 + exp(3.81) * d^(-0.534) * h^(-0.127) * (1-p)^0.97)

# 回忆失败（忘记）
h' = exp(-0.041) * d^(-0.041) * h^0.377 * (1-p)^(-0.227)
```

- `d` 在墨墨中主要是词条难度（1..18）；开源模拟器里初始 `d` 随机 1..10，失败后 `d = min(d + 2, 18)`。
- `start_h(d)` 的取值范围约 0.8～5.2 天：`d=1` 约 5.19 天，`d=10` 约 0.81 天。
- 开源模拟器的 `THRESHOLD` 基线用 `interval = -h * log2(0.9)`（目标保持率 90%），可作为 MVP 的间隔规则；完整 SSP-MMC 用离线值迭代得到的最优策略表，工程量大，暂不作为首版。

## 3. 「初始量」的两种可能含义

用户原话中的「初始量」有两种合理解释，本方案建议以第一种为主：

| 解释 | 定义 | 建议 |
|---|---|---|
| A. 初始记忆状态（推荐） | 前三次反馈后，进入墨墨模型时的 `d_init` 与 `h_init`（半衰期，天） | 作为 `NextDueCalculator` 的初始状态；后续每次反馈按 DHP 更新 |
| B. 初始每日学习量 | 切换后第一天建议复习多少分钟/多少题 | 由 `h_init` 推出的到期集合 + `estimatedReviewMinutes` 求和得到，不是独立学习参数；用户仍可改每日可用分钟 |

如果用户指的是 B，则需要额外确认「每日学习量」是否允许用户直接设定，还是只由模型建议。

## 4. 由前三次反馈推导 `d_init` / `h_init`

### 4.1 难度 `d_init`

输入：三次反馈的档位 `tier_i` 与第三次后的 `continuousScore`。档位分数：

```text
tierScore(MASTERED) = 1.0
tierScore(FAMILIAR) = 0.5
tierScore(UNKNOWN)  = 0.0

avgTier = (tierScore_1 + tierScore_2 + tierScore_3) / 3
d_init  = clamp(round(1 + 9*(1 - avgTier) - 2*(continuousScore - 0.5)), 1, 10)
```

- `continuousScore` 初始 0.5；`d_init` 落在 1..10，数值越大越难。
- 全 MASTERED 且连续分 1.0 → `d=1`；全 UNKNOWN 且连续分 0.0 → `d=10`；混合 → 中间值。
- 这是把墨墨的「词条难度」替换为「本项目前三次表现估计的难度」，属于必要适配，不是原论文公式。

### 4.2 半衰期 `h_init`

从 `h_0 = start_h(d_init)` 开始，按三次反馈的实际间隔 `t_i`（天，最小 1）依次更新：

```text
p_i = 2^(-t_i / h_{i-1})

MASTERED:  h_i = recall_h(d_i, h_{i-1}, p_i)
FAMILIAR:  h_i = h_{i-1} + 0.5 * (recall_h(d_i, h_{i-1}, p_i) - h_{i-1})   # 半强度回忆，适配「较熟悉」
UNKNOWN:   h_i = forget_h(d_i, h_{i-1}, p_i);  d_{i+1} = min(d_i + 2, 18)
```

- `t_i` 使用冷启动实际间隔（理想为 1/3/7 天；若用户延迟，按实际天数）。
- FAMILIAR 的半强度更新是本项目的适配；原论文只有二分类「认识/忘记」。若用户希望严格二分类，可把 FAMILIAR 归入「认识」，但会高估记忆强度。
- 三次后得到 `h_init = h_3`。

### 4.3 切换后的下次到期日

MVP 采用 90% 保持率。全 1.0 计数（MASTERED/UNKNOWN）时，切换发生在**第 4 次反馈**：第 3 次反馈后仍按冷启动 7 天；第 4 次反馈到来时，用该次实际间隔 `t_4` 与档位把 `h_3` 更新为 `h_4`，再计算。若 FAMILIAR 半次计数使有效反馈在更晚才达到 3.0，则切换发生在达到 3.0 后的下一次反馈。

```text
interval = clamp(round(-h_4 * log2(0.9)), 1, 30)
nextDueDate = 第 4 次反馈日期 + interval
```

- **不要**用第 3 次反馈日期做锚点：第 4 次反馈尚未发生，可能算出早于当前日期的 `nextDueDate`；尤其全 UNKNOWN 的 `h_3 ≈ 0.81`、间隔只有 1 天，而第 4 次反馈实际在第 3 次后 7 天。
- 第 4 次反馈的档位按第 4.2 节同一规则更新 `h`；`UNKNOWN` 仍使 `d += 2`。
- 上限 30 天：本项目是周计划工具，超过 30 天的间隔对初高中复习节奏意义有限；用户可在设置中调整上限（后续）。
- 完整 SSP-MMC 策略表可后续接入：按 `d` 与 `log(h)` 查表得到 interval；首版不引入值迭代与策略表，避免把纯 Kotlin 调度器做成训练管线。

### 4.4 计算示例（理想 1/3/7 天间隔）

| 三次反馈 | `d_init` | `h_0` | `h_3` | 第 4 次反馈后的 `h_4` | 90% 间隔 |
|---|---|---|---|---|---|
| 全 MASTERED | 1 | 5.19 | 225.4 | 347.7 | 30（上限） |
| 全 FAMILIAR | 6 | 1.47 | 45.5 | 71.8 | 11 天 |
| 全 UNKNOWN | 10 | 0.81 | 0.81 | 0.79 | 1 天 |
| M, M, U | 3 | 2.71 | 7.16 | 2.22（第 4 次 U） | 1 天 |
| M, F, F | 4 | 2.16 | 63.1 | 95.3（第 4 次 F） | 14 天 |

（计算使用第 2 节公式、FAMILIAR 半强度、UNKNOWN 后 `d += 2`；第 4 次假设间隔 7 天，档位与第 3 次相同。）

## 5. 与现有 D-05 的冲突与建议

- D-05 已按本方案修订为「累计有效反馈 ≥ 3.0 后从下一次反馈起切换到墨墨 DHP 简化模型」；D-01～D-04 的排序/权重/冷却仍生效。
- 建议修订 D-05 的措辞为：
  > 冷启动固定阶梯 1/3/7 天；三档任意填涂均计有效反馈，「较熟悉」按 0.5 次计入；累计有效反馈 ≥ 3.0 后，从下一次反馈起切换到墨墨 DHP 建议规划模型（`docs/research/maimemo-model.md`），冷启动阶梯不再使用。
- 不改 D-01～D-04 的排序/权重/冷却规则；它们仍负责候选排序、连续掌握度与冷却，DHP 只替换 `nextDueDate` 的推进函数。
- 切换时**不重置** `currentTier`、`continuousScore`、`effectiveFeedbackCount`；`d_init`/`h_init` 保存到 `KnowledgePoint.difficulty/halfLifeDays`，`nextDueDate` 由 `halfLifeDays` 计算，避免每次从历史重算漂移。

## 6. 已确认的决策（2026-11-13）

1. 「初始量」按 **A（初始半衰期/难度）** 处理；每日学习量由到期集合与 `estimatedReviewMinutes` 派生，不单独学习。
2. FAMILIAR 采用**半强度回忆**。
3. 接受 **90% 保持率 + 30 天上限** 作为首版；完整 SSP-MMC 策略表暂不实现。
4. **新增** `KnowledgePoint.difficulty: Int?` 与 `KnowledgePoint.halfLifeDays: Float?`（仅调度用，不展示）；`KnowledgeState` 同步。
5. 全 1.0 计数时切换发生在第 4 次反馈；FAMILIAR 半次计数导致 3.0 更晚时，在达到 3.0 后的下一次反馈切换。第 3 次仍按冷启动 7 天。

## 7. 实现落点（已确认）

- `core/scheduler` 不引入训练代码；新增纯 Kotlin `MaimemoNextDueCalculator`，只包含上述公式与常量。`NextDueCalculator` 契约使用 `MemoryState?`（difficulty / halfLifeDays）与 `NextDueResult(nextDueDate, memoryState)`；`KnowledgePoint`/`KnowledgeState` 增加可空 `difficulty`/`halfLifeDays` 字段与 Room 迁移。
- `ApplyFeedbackUseCase` 在第 3 条 triggered 反馈后初始化 `MemoryState`；达到 3.0 后从下一次反馈起用 DHP 计算 `nextDueDate`；冷启动分支仍按 1/3/7。
- 属性测试：随机 100 组 `d/tier/t`，断言 `h > 0`、`nextDueDate >= reviewedOn + 1`、间隔 ≤ 30、`UNKNOWN` 不增大 `h`、`MASTERED` 不减小 `h`。
- 版本迁移：新增 `difficulty/halfLifeDays` 可空列，旧数据默认 `null`；第 3 条 triggered 反馈后按第 4 节公式初始化，达到 3.0 后从下一次反馈起用 DHP。
