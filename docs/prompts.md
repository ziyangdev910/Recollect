# TDD Prompts（自包含小阶段版）

> 用途：把 CLAUDE.md 的 Observe → Act → Verify → Record 工作循环落成给 Claude Code 的提示词。
> 用法：**新开对话时，只复制对应小阶段的一个代码块**；每个代码块已内置通用前缀、本大阶段的背景约束，以及该阶段需要的辅助 prompt（收尾、规格审计、代码审查、实体机验证等）。
> 原则：**一次只做一个用例**；先红后绿；不跳过失败测试；不改 specs；不引入表外依赖。
> 状态：已完成的小阶段在标题标注【done】。

---

## 阶段 0：Bootstrap 验证

> 已由 2026-11-13 的初始化完成；以后换机器或升级版本时重跑。

### 0.1 构建骨架验证【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】本阶段只验证构建骨架，不写业务代码，不走 Red/Green 循环，按 Observe → Verify → Record 执行。

【本轮任务】
1) 运行 ./gradlew --version，确认是 Gradle 8.13、Launcher JVM 17；
2) 运行 ./gradlew projects，确认 :core:common、:core:model、:core:scheduler、:core:pagination 都在；
3) 运行 ./gradlew :core:scheduler:compileKotlin，确认无源码也能成功；
4) 运行 ./gradlew verifyCorePurity；
5) 如果任一步失败，只修构建文件（settings.gradle.kts、build.gradle.kts、libs.versions.toml、模块 build.gradle.kts），不要写业务 Kotlin。

【硬性禁止】
- 不写任何业务 Kotlin 代码；不改 docs/specs/、不修改 D-01～D-13。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。

【记录】把每条命令的输出摘要贴出来，并写 docs/agent-log/YYYY-MM-DD.md（目标、命令与结果、未决问题、下轮建议）。不要 git commit，除非我明确要求。
```

---

## 阶段 1：core:scheduler

> 规格：docs/specs/scheduler.md。测试类：`SchedulerPlanTest`；每轮只做一个用例。

### 1.1 第一个红测：S-01【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/scheduler.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 1（core:scheduler），规格 docs/specs/scheduler.md；测试类 SchedulerPlanTest；本轮只做用例 S-01（D-01）。

【本轮任务】
- 新建 core/scheduler/src/main/kotlin/.../Scheduler.kt，只放 scheduler.md 第 20–76 行的公开类型与接口（Tier、KnowledgeState、SchedulerQuestion、PlanResult、PlannedQuestion、DeferredItem、DeferReason、Scheduler）。
- 新建 core/scheduler/src/test/kotlin/.../SchedulerPlanTest.kt，只写 S-01：
  k1(UNKNOWN, nextDueDate=2026-11-16, activeQuestionCount=1) + q1(linkedKnowledgeIds=[k1])，
  dateRange=2026-11-16..2026-11-22，dailyAvailableMinutes 每天 60。
  断言 dailyAssignments[2026-11-16] 只有 q1，triggeredKnowledgeIds=[k1]，estimatedMinutes=15，deferred 为空。
- 运行测试，确认红；再写 DefaultScheduler 的最小实现（只做候选过滤 + targetDate + 直接放入目标日，不排序、不装箱），让 S-01 绿。
- 不要实现 S-02、S-08、S-09 或排序；不要改 specs。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、对应的用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试 ./gradlew :core:scheduler:test --tests "*SchedulerPlanTest"，再跑本模块全部测试；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

### 1.2 归档与休眠：S-08 / S-09【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/scheduler.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 1（core:scheduler），规格 docs/specs/scheduler.md；测试类 SchedulerPlanTest；本轮只做 S-08、S-09（D-11）。

【本轮任务】
- S-08：archived=true 的题目不出现在任何 dailyAssignments。
- S-09：dormant=true 或 activeQuestionCount=0 的知识点不调度，也不进入 nonTriggeredKnowledgeIds。
- 先各写一个失败测试，确认红；再在 DefaultScheduler 的候选过滤中补最小逻辑。
- 验证：./gradlew :core:scheduler:test --tests "*SchedulerPlanTest"。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试，再跑本模块全部测试；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

### 1.3 冷却与一次性覆盖：S-02 / S-19【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/scheduler.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 1（core:scheduler），规格 docs/specs/scheduler.md；测试类 SchedulerPlanTest；本轮只做 S-02、S-19（D-04）。

【本轮任务】
- S-02：lastReviewedDate 使 lastReviewedDate + coolingPeriodDays > targetDate 时，题目不排入；生成 DeferredItem(COOLING)，knowledgePointIds 含全部触发知识点，targetNextDueDates 每个 +1 天。
- 边界：lastReviewedDate + 7 == targetDate 时仍正常排入（冷却判定是严格大于）。
- S-19：coolingOverrides 命中 questionId 时本次跳过冷却并正常排入；未命中/已消费的 id 不生效。
- 先红后绿；冷却判定只在 scheduler 内，不写数据库。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试 ./gradlew :core:scheduler:test --tests "*SchedulerPlanTest"，再跑本模块全部测试；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

### 1.4 候选排序：S-04 / S-05 / S-18【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/scheduler.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 1（core:scheduler），规格 docs/specs/scheduler.md；测试类 SchedulerPlanTest；本轮只做候选排序层（D-02、D-03），用例 S-04、S-05、S-18。

【本轮任务】
- 实现优先级：hasTierGap → groupTier（MASTERED > FAMILIAR > UNKNOWN）→ 组内 min continuousScore → targetDate → questionId 字典序。
- 先写 S-04（有档位差优先，但不淘汰）、S-05/S-18（跨档位分数反例：Q1 同时触发 MASTERED 0.9 与 UNKNOWN 0.1，Q2 触发 MASTERED 0.5，Q2 必须在前）。
- 禁止跨 groupTier 比较 continuousScore。
- 时间容量给足，避免和装箱混在一起。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试 ./gradlew :core:scheduler:test --tests "*SchedulerPlanTest"，再跑本模块全部测试；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

### 1.5 时间装箱：S-05b / S-15 / S-17【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/scheduler.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 1（core:scheduler），规格 docs/specs/scheduler.md；测试类 SchedulerPlanTest；本轮只做时间装箱层（C-017、C-020），用例 S-05b、S-15、S-17。

【本轮任务】
- 按日期升序，每日只处理 targetDate == 当天的候选；容量缺失按 0。
- first-fit：剩余容量 >= estimatedReviewMinutes 则排入；否则 DeferredItem(TIME_OVERFLOW)，继续尝试后续候选，不停止整日。
- estimatedReviewMinutes 缺失或 <= 0 按 15。
- 先写 S-05b（20/20/10，容量 30，第 2 条溢出后第 3 条仍可入）、S-15（0/-5 按 15）、S-17（第 1 天溢出不去第 2 天，第 2 天候选不填第 1 天）。
- 不要在装箱阶段改变候选顺序。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试 ./gradlew :core:scheduler:test --tests "*SchedulerPlanTest"，再跑本模块全部测试；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

### 1.6 日期范围与稳定输出：S-13 / S-14 / S-16【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/scheduler.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 1（core:scheduler），规格 docs/specs/scheduler.md；测试类 SchedulerPlanTest；本轮只做日期与顺序，用例 S-13、S-14、S-16。

【本轮任务】
- S-13：逾期知识点落到 dateRange.start，只排一次，日期键升序。
- S-14：一题多个知识点在范围内到期，只排最早 targetDate 一次，全部计入 triggeredKnowledgeIds。
- S-16：输入 Map/List 顺序打乱，输出结果相同。
- 输出 dailyAssignments 用 LinkedHashMap 或按日期显式排序；没有条目的日期不出现。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试 ./gradlew :core:scheduler:test --tests "*SchedulerPlanTest"，再跑本模块全部测试；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

### 1.7 校验与属性测试：S-10 + property【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/scheduler.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 1（core:scheduler），规格 docs/specs/scheduler.md；测试类 SchedulerPlanTest；本轮做最后两组：S-10 与属性测试。

【本轮任务】
1) S-10：linkedKnowledgeIds 数量 > 4 时抛 IllegalArgumentException；0 个关联跳过，不进入候选（C-049）。
2) 属性测试：JUnit @RepeatedTest(100) + kotlin.random.Random(20261113)，断言：
   - archived/dormant 不出现在任何 dailyAssignments；
   - 同一题目同一日不重复；
   - 同一题目在一份计划内至多出现一次；
   - dailyAssignments 的日期键严格升序。
3) 跑 ./gradlew :core:scheduler:test 与 ./gradlew verifyCorePurity，确认全绿、无 @Ignore。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试，再跑本模块全部测试；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

### 1.8 模块收尾 + 规格审计【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/scheduler.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 1（core:scheduler）收尾，不写新功能。

【本轮任务：收尾】
1) 逐条核对 scheduler.md 的用例表 S-01～S-19，列出每个用例对应的测试方法；缺哪个补哪个（仍按先红后绿）。
2) 检查测试中没有 android.* / androidx.* import；运行 ./gradlew :core:scheduler:test 与 ./gradlew verifyCorePurity，确认全绿、无 @Ignore。
3) 更新 docs/changelog.md（文件、意图、关联决策）与 docs/agent-log/YYYY-MM-DD.md（目标、改动、验证命令与结果、未决问题、下轮建议），并列出下一轮建议的单个用例编号。
4) 不要 git commit，除非我明确要求。

【本轮任务：规格审计（只读，另起任务）】
读 docs/specs/scheduler.md 与当前实现/测试，逐条列出：
① 规格要求但代码没有的；
② 代码做了但规格没定义的；
③ 测试断言与规格文字不一致的；
④ 需要人类裁决的。
不要改代码、不要改 specs；只把 ②③④ 写进 docs/conflicts.md 的待裁决区，条目须包含关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议与裁决前禁止事项；描述尽量少用内部变量名/类名，不替人类做决定。

【本轮任务：代码审查（只读，另起任务）】
只审查本轮 diff，不写新功能：
- 检查是否违反 R-01～R-06、D-01～D-13；
- 检查 core 模块是否出现 android.*/androidx.*；
- 检查测试是否只覆盖 happy path；
- 检查是否用 mock 冒充识别证据；
- 输出“必须修 / 建议修 / 可不改”三档，并给出 file:line。

【固定循环】本轮为收尾，固定循环退化为 Verify → Record；上面列出的核对与补测仍需先红后绿。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
```

---

## 阶段 2：core:pagination

> 规格：docs/specs/pagination.md。C-047（sealed `PaginationResult`）与 C-048（放置语义）已裁决，按规格实现；不要自行选择其它放置策略。

### 2.1 锁定模板几何：TemplateGeometryTest【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/pagination.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 2（core:pagination），规格 docs/specs/pagination.md；C-047（sealed PaginationResult）与 C-048（放置语义）已裁决，不要自行选择其它放置策略。本轮只做 TemplateGeometry 的派生量测试，不实现 PagePlanner。

【本轮任务】
- 用默认参数断言：usableHeightMm=273.0、firstPageUsableHeightMm=265.0、contentHeightMm=266.0、firstPageContentHeightMm=258.0、maxAttachmentHeightMm=81.9、qrFooterHeightMm=21.0。
- 先写失败测试（可以先用 TODO/临时实现让编译通过），再补 TemplateGeometry。
- 注意：默认几何下 P-05/P-11/P-13/P-14/P-18 不可达；后续这些用例必须显式传自定义 template。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试 ./gradlew :core:pagination:test --tests "*TemplateGeometryTest"，再跑本模块全部测试；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

### 2.2 最小分页：P-01【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/pagination.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 2（core:pagination），规格 docs/specs/pagination.md；C-047（sealed PaginationResult）与 C-048（放置语义）已裁决，不要自行选择其它放置策略。本轮只做 P-01。

【本轮任务】
- 一日一条、高度正常 → 一页一条；pageId="{planId}-01"；itemHeight 按 pagination.md 的公式计算。
- 先写失败测试，再写 PagePlanner 的最小实现：每日一页、按公式算高、顺序放置；不做移页、不做 overflow、不做附件上限。
- 不要碰 P-03/P-08/P-10/P-13。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试（本轮新建的测试方法），再跑 ./gradlew :core:pagination:test；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

### 2.3 移页（C-048；C-063 后不再渲染续下页）：P-03【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/pagination.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 2（core:pagination），规格 docs/specs/pagination.md；C-048（放置语义）已裁决，按规格实现，不要自行选择其它放置策略。本轮只做 P-03。

【本轮任务】
- 四条默认高度 68.5mm 的条目；第 4 条放不下时整条移到下一页，第 1 页生成恰好一个 ContinuationMarker(order=0)，from/to pageId 正确。
- 按 C-048：遇到第一条放不下的条目就结束当前页，该条及后续条目按原顺序进入下一页；不得跳过该条去填更小的后续条目。
- 先红后绿；不要拆分条目、不要压缩行高。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试（本轮新建的测试方法），再跑 ./gradlew :core:pagination:test；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

### 2.4 跨日与首页差异：P-08 / P-10 / P-13【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/pagination.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 2（core:pagination），规格 docs/specs/pagination.md；C-047、C-048 已裁决。本轮只做跨日与首页/后续页，用例 P-08、P-10、P-13。

【本轮任务】
- P-08：每日从新页开始，pageNumber 全文档连续，同日多页 date 相同，日期升序。
- P-10：首页扣 legendHeightMm + headerRowHeightMm；后续页只扣 headerRowHeightMm。
- P-13：条目能进后续页但进不了首页时，首页仍存在、items 为空、有一个 marker；条目完整出现在下一页。
- 先红后绿；不要改分页公式。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试（本轮新建的测试方法），再跑 ./gradlew :core:pagination:test；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

### 2.5 溢出边界：P-14 / P-18【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/pagination.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 2（core:pagination），规格 docs/specs/pagination.md；C-047、C-048 已裁决。本轮只做单题过高，用例 P-14、P-18。

【本轮任务】
- P-14：itemHeight > contentHeightMm → OverflowItem(ITEM_TOO_TALL)，不进 pages，不拆分、不压缩。
- P-18：itemHeight 落在 (contentHeightMm, usableHeightMm] 也必须进入 ITEM_TOO_TALL，不得出现“既放不下又不在 overflow”的区间。
- 先构造自定义 template 让条件可达（默认几何不可达），再写红测、再实现。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试（本轮新建的测试方法），再跑 ./gradlew :core:pagination:test；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

### 2.6 附件上限：P-05 / P-11【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/pagination.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 2（core:pagination），规格 docs/specs/pagination.md；C-047、C-048 已裁决。本轮只做附加笔记区，用例 P-05、P-11。

【本轮任务】
- P-05：attachmentCount 1/2/3 时 rightBottom 递增；附件面积超过 usableHeightMm*0.30 时整条进入 ATTACHMENT_OVER_LIMIT。
- P-11：恰好 30% 通过；超过 1mm 进入 ATTACHMENT_OVER_LIMIT；不截断附件区。
- 必须用自定义 template（pageHeight/margins/attachmentHeightPerItemMm），默认几何不可达。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试（本轮新建的测试方法），再跑 ./gradlew :core:pagination:test；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

### 2.7 其余边界 + 属性测试【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/pagination.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 2（core:pagination），规格 docs/specs/pagination.md；C-047、C-048、C-060、C-061、C-062 已裁决。本轮补齐 P-02/P-12/P-15/P-16/P-19 与 P-09 属性测试；模板校验、PLAN_TOO_LONG 与 P-20 非法计数留到 2.8 收尾。

【本轮任务】
- P-02：一日多条恰好填满内容区 → 无 overflow、无跨页、无续下页元数据（C-063 起标签不再渲染）。
- P-12：输入 Map 乱序 → 输出按日期升序，与输入迭代顺序无关；P-08 已覆盖大半，仍须单独命名并锁定。
- P-15：hasNoteArea=false → rightBottom=0，附加数量不计入高度；附加上限也不判定（C-062 方案 B），附加数量不得被静默清零。默认模板下同一题 68.5→38.5mm 已可区分，无需自定义 template；若要同时覆盖“无笔记区 + 附件超限不判”的边界，显式传自定义 template（参考 C-062 锁定测试）。
- P-16：knowledgeLineCount 1..4 → rightTop 按行数递增，不按 knowledgeCount 递增。
- P-19：itemHeight 包含 qrFooterHeightMm（= qrSizeMm + qrPaddingMm），QR 不挤占笔记书写区。
- P-09：@RepeatedTest(100) + Random(20261113)，断言无条目跨页/重叠、marker 与被移条目一一对应、overflow 不在 pages、pageNumber 连续、pageId 唯一；生成器同时覆盖 C-060（全溢出日期不建页）、C-061（两条件同时成立时只输出一条 ITEM_TOO_TALL）、C-062（无笔记区不判附加上限）。
- 跑 ./gradlew :core:pagination:test 与 ./gradlew verifyCorePurity。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试，再跑本模块全部测试；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

### 2.8 模块收尾 + 规格审计【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/pagination.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 2（core:pagination）收尾，不写新功能。C-047（sealed Failure：`INVALID_TEMPLATE_GEOMETRY`、`PLAN_TOO_LONG`）、C-048（放置语义）、C-060（全溢出日期不建页）、C-061（两条 overflow 同时成立归为 `ITEM_TOO_TALL`）、C-062（附加上限仅在 `hasNoteArea=true` 时判定）均已裁决。

【本轮任务：收尾】
1) 逐条核对 pagination.md 的 P-01～P-20，列出测试方法映射；缺哪个补哪个（先红后绿），不得用删除断言或 `@Ignore` 凑绿；
2) 确认 C-047、C-048、C-060、C-061、C-062 已反映在实现与测试里；不要假装通过；
3) 补齐验收门禁：模板非法返回 `Failure(INVALID_TEMPLATE_GEOMETRY)`；`pageNumber` 超过 99 返回 `Failure(PLAN_TOO_LONG)`；P-20 非法计数抛 `IllegalArgumentException`；P-09 属性测试 `@RepeatedTest(100)` + `Random(20261113)` 覆盖 C-060/C-061/C-062；
4) 运行 ./gradlew :core:pagination:test 与 ./gradlew verifyCorePurity；
5) 更新 docs/changelog.md（文件、意图、关联决策）与 docs/agent-log/YYYY-MM-DD.md（目标、改动、验证命令与结果、未决问题、下轮建议），列出下一轮建议的单个用例编号；
6) 不要 git commit，除非我明确要求。

【本轮任务：规格审计（只读，另起任务）】
读 docs/specs/pagination.md 与当前实现/测试，逐条列出：
① 规格要求但代码没有的；
② 代码做了但规格没定义的；
③ 测试断言与规格文字不一致的；
④ 需要人类裁决的。
不要改代码、不要改 specs；只把 ②③④ 写进 docs/conflicts.md 的待裁决区，条目须包含关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议与裁决前禁止事项；描述尽量少用内部变量名/类名，不替人类做决定。

【本轮任务：代码审查（只读，另起任务）】
只审查本轮 diff，不写新功能：
- 检查是否违反 R-01～R-06、D-01～D-13；
- 检查 core 模块是否出现 android.*/androidx.*；
- 检查测试是否只覆盖 happy path；
- 检查是否用 mock 冒充识别证据；
- 输出“必须修 / 建议修 / 可不改”三档，并给出 file:line。

【固定循环】本轮为收尾，固定循环退化为 Verify → Record。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
```

---

## 阶段 3：core:model

> 规格：docs/specs/repository.md、docs/specs/data-model.md。C-046 方案 A：`core:model` 只做纯 DTO/枚举与 `KnowledgeRowsCodec`；Room ↔ DTO 映射在 `data:room`。

### 3.1 只做 Codec【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/repository.md、docs/specs/data-model.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 3（core:model），规格 docs/specs/repository.md、docs/specs/data-model.md；C-046 方案 A：core:model 只做纯 DTO/枚举与 KnowledgeRowsCodec，Room ↔ DTO 映射在 data:room。C-053 方案 A：`Tier`、`KnowledgeState`、`SchedulerQuestion` 已放在 `core:model`，不得另建同名 DTO 或反向依赖。C-070 方案 A（2026-11-17 已裁决）：行快照每条记录携带题目标识，rows 顺序冻结、不按 number 重排（C-015）。本轮只做 KnowledgeRowsCodec。

【本轮任务】
- 用 data-model.md 的 JSON 示例做 decode→encode 往返；
- 断言 rows 数量/顺序、triggeredKnowledgePointIds 原样保留；
- 断言 rows=[number5,number3] 解码后仍是 [5,3]，不按 number 重排（C-015 冻结）；
- 行快照记录含题目标识（C-070）；不重新定义 C-053 已迁移到 core:model 的三个 DTO；不写任何 Room 映射；不引入 androidx。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试 ./gradlew :core:model:test --tests "*KnowledgeRowsCodecTest"，再跑本模块全部测试；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

### 3.2 data/room 映射（C-046 方案 A）【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/repository.md、docs/specs/data-model.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 3（core:model → data:room），规格 docs/specs/repository.md、docs/specs/data-model.md；C-046 方案 A：core:model 只做纯 DTO/枚举与 KnowledgeRowsCodec，Room ↔ DTO 映射在 data:room。C-050：`continuousScore` 内部 `Double`、落库/构造 DTO 转 `Float`。C-053：`Tier`/`KnowledgeState`/`SchedulerQuestion` 来自 `core:model`。C-056 方案 A：id 唯一性与关联完整性由 `data:room` 保证，调度器不做去重/合并。C-067 方案 B：`knowledgeNames` 与左侧同一 printable 集合、按 number 升序、数量 = `knowledgeCount`，由 data/room 映射保证，分页器不校验。C-070 方案 A：`PlanPageRows` 含 `questionId`，行快照顺序冻结（C-015）。C-071 方案 A（2026-11-17 已裁决）：行快照映射来源是 Room 实体 `PlanPage.knowledgeRows` 的冻结 JSON → `List<PlanPageRows>`，只解码，不从当前关联重新生成、不重排、不丢记录。C-072 方案 A（2026-11-17 已裁决）：无 `knowledgeRows` 快照的旧打印稿不可回读，提示重新生成计划并按新稿打印，不得按当前关联重算。本轮只做映射。

【本轮任务】
在 data:room 实现 Room Entity ↔ 纯 DTO 映射，不改 core:model 的纯度：
- KnowledgePoint.toKnowledgeState()：`continuousScore` 按 C-050 转 `Float`；
- Question.toSchedulerQuestion(linkedKnowledgeIds)：linkedKnowledgeIds 从 QuestionKnowledgeCrossRef 读取，按 KnowledgePoint.number 升序构造；构造结果须满足 id 唯一前置条件（C-056），不补不存在的知识点、不静默去重/合并（C-055）；
- PlannedQuestion.toRenderableQuestion(questionNumber, knowledgeNames, knowledgeLineCount, attachmentCount)：questionNumber 从 Question 实体补齐；knowledgeNames 与左侧同一 printable 集合、按 number 升序、数量 = knowledgeCount（C-067）；
- PlanPage.toPlanPageRows()：Room 实体 `PlanPage.knowledgeRows` 的冻结 JSON → `List<PlanPageRows>`（一页可有多条记录）；每题记录带 `questionId`（C-070）；rows 保持打印快照顺序，不按 number 重排（C-015）；不从当前题目关联重新生成行（C-071 方案 A）。
先写 ModelMappingTest（字段一一对应、difficulty/halfLifeDays=null、estimatedReviewMinutes<=0→15、continuousScore 为 Float、linkedKnowledgeIds 唯一且按 number 升序、knowledgeNames 数量 = knowledgeCount、questionId 与 rows 顺序保真、行快照只解码 `PlanPage.knowledgeRows` 且不从当前题目关联重新生成），再实现。
core:model 只允许纯 DTO/枚举与 KnowledgeRowsCodec；不得出现 android.* / androidx.*。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试 ./gradlew :data:room:test --tests "*ModelMappingTest"，再跑本模块全部测试；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

### 3.3 首个纵切：FirstVerticalSliceTest【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/repository.md、docs/specs/data-model.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 3（core:model），规格 docs/specs/repository.md、docs/specs/data-model.md；C-046 方案 A、C-053 方案 A（调度 DTO 在 core:model）。C-073 方案 A（2026-11-17 已裁决）：`RenderableQuestion` 属于 `core:pagination`，`core:model` 不重复定义同名类型。本轮只做一个跨模块纵切测试（不写 UI），测试放在 `data:room`（唯一同时依赖三个 core 模块的模块）。

【本轮任务】
- 2 个知识点、2 道题 → Scheduler.plan → RenderableQuestion（从 `core:pagination` 引用）→ PagePlanner.plan；
- 断言 1 页 2 条、pageId 格式、D-01（同一题只出现一次）与 D-08（按日分页）；
- 若 `PagePlanner` 返回 `PaginationResult.Failure`，按 C-047 记录 code/message 并停下，不得吞掉或改成 `Success`；
- 先写失败测试；如果失败原因是规格没有覆盖，停下来写 docs/conflicts.md，不要绕过。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试（./gradlew :data:room:test --tests "*FirstVerticalSliceTest"），再跑相关模块全部测试；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

---

## 阶段 4：data/room 与 UseCase

> 规格：docs/specs/repository.md、docs/specs/feedback-usecase.md。C-052 方案 C：Android 单元测试用 JUnit 5（`de.mannodermaus.android-junit5`），`androidTest` 用 JUnit 4。

### 4.1 Room schema 与 DAO【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/repository.md、docs/specs/data-model.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 4（data/room），规格 docs/specs/repository.md、docs/specs/data-model.md；C-052 方案 C：Android 单元测试用 JUnit 5（de.mannodermaus.android-junit5），androidTest 用 JUnit 4。C-050：`continuousScore`/`effectiveFeedbackCount` 落库为 `Float`。C-056 方案 A：知识点/题目/关联 id 唯一与关联完整性由 `data:room` 保证。本轮只做实体与 DAO。

【本轮任务】
- 按 data-model.md 建 13 个实体；先做 KnowledgePoint、Question、QuestionKnowledgeCrossRef、ReviewFeedback、ConfirmationDecisionLog、PendingFeedback、ScanSession、CoolingOverrideEvent、PlanPage，另 4 个（NoteAttachment、NoteAttachmentItem、KnowledgePointTombstone、KnowledgeNumberSequence）随对应 UseCase 补齐；
- 写 schema 测试：字段名/类型/主键/外键/索引与 data-model.md 一一对应，至少锁定 `knowledge_point.number` 与 `plan_page.pageId` 唯一索引、交叉表复合主键、`continuousScore`/`effectiveFeedbackCount` 为 Float 列（C-050、C-056）；
- 迁移脚本遇到重复 `number`/`pageId` 必须失败并提示，不得静默重编号；
- 不写业务 UseCase；不写 UI。
- 运行 ./gradlew :data:room:test。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试，再跑 ./gradlew :data:room:test；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

### 4.2 Repository 事务【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/repository.md、docs/specs/data-model.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 4（data/room），规格 docs/specs/repository.md；C-052 方案 C：Android 单元测试用 JUnit 5，androidTest 用 JUnit 4。C-047：`PaginationResult.Failure` 转 `PlanGenerationException`，不进入事务、不写库。C-055：关联 id 失效时整题跳过并留可见记录，不由 Repository 补默认知识点。C-056：id 唯一与关联完整是 `data:room` 的前置条件。C-057：冷却天数固定 7 天，0 仅内部/测试使用、负数非法。本轮只做 Repository 事务。

**2026-11-17 已落地（【done】）**：`RoomPlanRepository.savePlan` 与 `RoomFeedbackRepository.saveDecisions` 均为单事务；JVM 单元测试用 fake DAO + 可回滚事务执行器（不引入 Robolectric）。实际签名相对 `repository.md` 有扩展：计划草稿带每题冻结行快照；确认提交带 `updatedKnowledgeStates`/`questionLastReviewedDates`/显式 `noteAttachments`，并返回 `SaveDecisionsResult` 的跳过清单；`PlanDraft`/`PlanGenerationException` 目前放在 `data:room`。这些签名/归属已由 C-074 方案 A 裁决（见 `docs/conflicts-archive.md`），后续轮次不得改回按当前关联重算行快照、不得在关联失效时补默认知识点。

【本轮任务】
- PlanRepository.savePlan：PlanPage + lastScheduledDate + 消费 CoolingOverrideEvent + 冷却顺延，同一事务；只接收成功的分页结果，`Failure` 在进入事务前抛 `PlanGenerationException`（C-047）；
- 落库的冷却顺延结果须符合 C-057 口径（设置页固定 7 天、0 关闭冷却、负数非法），Repository 不得自行改写天数或静默夹取；
- FeedbackRepository.saveDecisions：ReviewFeedback + ConfirmationDecisionLog + KnowledgePoint + NoteAttachmentItem + 删除 PendingFeedback，同一事务；`continuousScore` 落库转 `Float`（C-050）；
- 构造 DTO/写库前保证 id 唯一与关联完整（C-056）；关联失效按 C-055 的整题跳过记录处理，不得补默认知识点；
- 先写“任一步失败全部回滚”的测试，再实现；
- 不 mock 识别结果；用内存 Room 或 fake repository。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试，再跑 ./gradlew :data:room:test；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

### 4.3a ApplyFeedbackUseCase：U-01～U-05【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/feedback-usecase.md、docs/specs/repository.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 4（UseCase），规格 docs/specs/feedback-usecase.md；C-052 方案 C：Android 单元测试用 JUnit 5，androidTest 用 JUnit 4。C-050：`continuousScore` 算术用 `Double`，落库/断言转 `Float`；`effectiveFeedbackCount` 精确 `>= 3.0f`，不做 epsilon。本轮只做 U-01～U-05。依赖 4.2 已落地的 `saveDecisions(sessionId, decisions, updatedKnowledgeStates, questionLastReviewedDates, noteAttachments): SaveDecisionsResult`：状态在 UseCase 内计算并转 `Float`，Repository 只落库；C-055 的整题跳过清单由返回结果驱动确认页提示（C-074 方案 A）。

**2026-11-17 已落地（【done】）**：`data:room` 的 `ApplyFeedbackUseCase` 覆盖 U-01～U-05；`ApplyResult` 携带 `SaveDecisionsResult` 与更新后的状态。读侧新增 `KnowledgePointRepository.states()` 与 `ReviewFeedbackHistoryRepository.triggeredCount()`；`ColdStartNextDueCalculator` 只实现冷启动 1/3/7 天，DHP 与 3.0 切换留给 4.3b。`feedback-usecase.md` 原「接口」段（三仓储注入、返回反馈行号、独立时钟）与 C-074 的单一事务入口不一致，已按 C-075 方案 A 裁决并回写：读端口 + 单事务提交入口，`ApplyResult` 携带 `SaveDecisionsResult`。

【本轮任务】
- 触发行 ACCEPT/CORRECTED；非触发行 ×0.3；NOT_REVIEWED 不写 ReviewFeedback、只写 ConfirmationDecisionLog；
- 先红后绿；每完成一条用例单独跑测试；写入路径按 C-050 在边界处转 `Float`；
- 不要一次实现 U-06～U-19。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试，再跑 ./gradlew :data:room:test；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

### 4.3b ApplyFeedbackUseCase：U-06～U-12【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/feedback-usecase.md、docs/specs/repository.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 4（UseCase），规格 docs/specs/feedback-usecase.md；C-052 方案 C：Android 单元测试用 JUnit 5，androidTest 用 JUnit 4。本轮只做 U-06～U-12。写入统一走 4.2 已落地的 `saveDecisions`：UseCase 负责计算状态，Repository 只落库（C-074 方案 A）。C-075 方案 A：`ApplyResult` 携带 `SaveDecisionsResult` 与更新后状态，不返回反馈行 id。

【本轮任务】
- 冷启动 1/3/7、FAMILIAR 0.5、2.5→3.0 的切换边界、达到 3.0 的这次仍按冷启动；
- 先红后绿；continuousScore 算术用 Double、落库/断言转精确 Float；effectiveFeedbackCount 阈值保持精确 >=3.0f，不做 epsilon（C-050）。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试，再跑 ./gradlew :data:room:test；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

### 4.3c ApplyFeedbackUseCase：U-13～U-19【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/feedback-usecase.md、docs/specs/repository.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 4（UseCase），规格 docs/specs/feedback-usecase.md；C-052 方案 C：Android 单元测试用 JUnit 5，androidTest 用 JUnit 4。C-050：内部 `Double`、落库/断言 `Float`，阈值精确 `>= 3.0f`。C-057：冷却天数固定 7 天，0 关闭、负数非法。本轮只做 U-13～U-19。写入统一走 4.2 已落地的 `saveDecisions`，事务回滚与 C-055 跳过清单语义沿用该实现（C-074 方案 A）。C-075 方案 A：`ApplyResult` 携带 `SaveDecisionsResult` 与更新后状态，不返回反馈行 id。

**2026-11-17 已落地（【done】）**：`ApplyFeedbackUseCase` 已覆盖 U-13～U-19，其中 U-13 修复了同一提交内同一知识点被多道题触发时事件序号不累计的问题（事件序号与 DHP 历史现在包含本批已处理的触发行）；U-14 按 `repository.md` 的事务规则由 `RoomPlanRepository.savePlan` 在计划保存事务内应用，不另设独立顺延用例；该落点已由 C-076 方案 A 裁决并回写规格；U-15～U-19 由既有实现自然覆盖，首跑即绿并用断言锁定。新增「多行提交中途写库失败整体回滚、待确认行保留」用例。

【本轮任务】
- 同一 session 多行、冷却顺延、ACCEPT 但 detectedTier=null、历史只有非触发行、第 3 条后初始化记忆状态、批量扫描晚复习、全部 NOT_REVIEWED；
- U-18/U-19 的 `nextDueDate`/`lastReviewedDate` 按实际复习日计算，冷却顺延遵守 C-057；
- 先红后绿；事务失败必须回滚。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试，再跑 ./gradlew :data:room:test；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
- 不要 git commit，除非我明确要求。
```

### 4.4 UseCase 收尾【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/feedback-usecase.md、docs/specs/repository.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 4（UseCase）收尾，不写新功能。C-052 方案 C：Android 单元测试用 JUnit 5，androidTest 用 JUnit 4。C-050 已裁决但待验证：`continuousScore` 内部 `Double`、落库/断言 `Float`，`effectiveFeedbackCount` 精确 `>= 3.0f`。

【本轮任务：收尾】
1) 逐条核对 feedback-usecase.md 的 U-01～U-19，缺哪个补哪个（先红后绿）；
2) 用 U-01～U-19 验证 C-050 的 Double/Float 边界与精确阈值，不得用 epsilon 容差迁就浮点误差；
3) 跑 ./gradlew :data:room:test；
4) 更新 docs/changelog.md（文件、意图、关联决策）与 docs/agent-log/YYYY-MM-DD.md（目标、改动、验证命令与结果、未决问题、下轮建议），列出下一轮建议的单个用例编号；
5) 不要 git commit，除非我明确要求。

【本轮任务：规格审计（只读，另起任务）】
读 docs/specs/feedback-usecase.md、docs/specs/repository.md 与当前实现/测试，逐条列出：
① 规格要求但代码没有的；
② 代码做了但规格没定义的；
③ 测试断言与规格文字不一致的；
④ 需要人类裁决的。
不要改代码、不要改 specs；只把 ②③④ 写进 docs/conflicts.md 的待裁决区，条目须包含关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议与裁决前禁止事项；描述尽量少用内部变量名/类名，不替人类做决定。

【本轮任务：代码审查（只读，另起任务）】
只审查本轮 diff，不写新功能：
- 检查是否违反 R-01～R-06、D-01～D-13；
- 检查测试是否只覆盖 happy path；
- 检查是否用 mock 冒充识别证据；
- 检查事务回滚断言是否真的验证了回滚；
- 输出“必须修 / 建议修 / 可不改”三档，并给出 file:line。

【固定循环】本轮为收尾，固定循环退化为 Verify → Record。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
```

> 落地说明（2026-11-17）：U-01～U-19 已逐条核对，均有对应测试（U-14 在 `PlanRepositoryTransactionTest`）；C-050 的 Double 同序计算/`Float` 精确断言与 `effectiveFeedbackCount >= 3.0f` 精确边界已由 U-01/U-02/U-03/U-12/U-08 等锁定，测试中无 epsilon 容差。U-05 原测试只覆盖“触发行 NOT_REVIEWED”，已改为用例表要求的“触发行 A 已填涂 + 非触发行 B 本次未复习”，临时变异取得红证据后还原。只读审计与代码审查结论见 `docs/agent-log/2026-11-17.md`。
>
> 裁决落地（2026-11-17）：C-077～C-089 已按人类选择落地，归档见 `docs/conflicts-archive.md`。后续确认流程直接按以下口径实现：
> - C-077 A / C-078 A：整题跳过的题目不写反馈、状态与题目复习日，但仍写确认日志并返回可见清单；待确认行保留，会话维持 READY 供修复后重试。
> - C-079 A：`ApplyFeedbackUseCase` 负责全部前置校验（会话 READY/未提交、行集合完整、计划日/触发标记一致、知识点归属、复习日上下界、档位可用），统一抛 `InvalidConfirmationException`；确认页保留按钮禁用的双保险。
> - C-080 A：会话整批成功后标记 SUBMITTED；重复提交直接拒绝，不写任何行。
> - C-081 A：实际复习日下界按“计划日所在周 + 用户设置的周起始日”计算（默认周一，设置未接入前用 `PlanWeekStartProvider`）。
> - C-082 A：迁移兜底窗口首条间隔取紧邻前一条复习日；C-083 C：档位解析共用 `ConfirmedDecision.effectiveTier()`；C-084 A：`actualIntervalDays` 为必填。
> - C-085 B：`ColdStartNextDueCalculator` 只在测试源集；C-086～C-089 A：DHP 期望值硬编码、初始化数值断言、补填用例、计划号/页号断言。

---

## 阶段 5：App 纵切

> 规格：docs/specs/app.md、entry.md、planning.md。实体机验证，不装模拟器，不使用 android-emu-agent daemon。
>
> 状态：**5.1～5.3 均已完成（2026-11-18）**。5.3 的实体机证据见 `evidence/screens/`（Home、Entry 空态、1 题 1 知识点、PlanPreview 1 页 1 条、C-060 空计划）与 `evidence/ui/`。

### 5.1 AppContainer 与导航【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/app.md、docs/specs/entry.md、docs/specs/planning.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 5（App 纵切），规格 docs/specs/app.md、entry.md、planning.md；实体机验证，不装模拟器。C-047：`PaginationResult.Failure` 由计划层转 `PlanGenerationException` 并映射为可重试 UI 错误。C-052 方案 C：Android 单元测试用 JUnit 5，androidTest 用 JUnit 4。C-053：调度 DTO 来自 `core:model`。本轮只做 app 壳。

【本轮任务】
- 手写 AppContainer（Repository、UseCase、Scheduler、PagePlanner 单例），只依赖 `core:model` 的 DTO（C-053），不引入 Hilt/Koin、不复制 DTO；
- 手写 AppNavigator（Compose 快照状态返回栈 + BackHandler），不引入 navigation-compose；
- MainActivity 只创建 AppContainer/AppNavigator/AppRoot，不直接访问 Room/CameraX/PrintManager/ML Kit；
- `PagePlanner` 的 `Failure` 不得被吞掉或改成空计划，按 C-047 抛 `PlanGenerationException`；
- 先写导航返回栈的单测（JUnit 5），再写 Compose 壳。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标单测 ./gradlew :app:testDebugUnitTest，再跑本模块全部测试；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- 不要 git commit，除非我明确要求。
```

> 落地说明（2026-11-18）：已创建 `:app`（Compose + android-junit5）。`AppNavigator` 用 `mutableStateListOf` 快照状态返回栈（栈顶即 `current`，`canPop` 随栈变化），返回栈单测 2 个 JUnit 5 用例；`BackHandler` 只在可返回时拦截，根路由交系统结束 Activity。2026-11-18 修复：初版 `current` 用 `mutableStateOf`、返回栈用普通 `ArrayDeque`，`BackHandler(enabled = canPop)` 只在首次组合读到 `false`，二级页面按返回会直接结束 Activity；改为整栈快照状态并在 `AppRoot` 顶层读取 `current`/`canPop` 后，已用实体机验证逐级返回（`evidence/screens/nav-back-*`、`evidence/ui/nav-back-*`）。`AppContainer` 装配现有 `data:room` Repository、`ApplyFeedbackUseCase`、`DefaultScheduler`、`DefaultPagePlanner`，不复制 `core:model` DTO；`GeneratePlanUseCase` 随 5.2 的 `feature/planning` 接入。容器不捕获 `PaginationResult.Failure`/`PlanGenerationException`（C-047 由 `data:room` 的 `PlanDraft.fromPagination` 抛出）。Compose 壳只提供路由占位与返回，不含业务页面。依赖接入见 `docs/dependency-proposals.md` 2026-11-18 条目；5.1 完成时无实体机，设备验证已由 5.3 补做（证据见 `evidence/`）。

### 5.2 录入 → 计划【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/app.md、docs/specs/entry.md、docs/specs/planning.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 5（App 纵切），规格 docs/specs/app.md、entry.md、planning.md；实体机验证，不装模拟器。C-047：分页失败转 `PlanGenerationException`，不写库。C-060：整份计划无可打印题目时由计划层给出空态或提示，分页器不生成空白页。C-061/C-062：overflow 提示必须原样呈现（含原因与题目），不得静默丢弃。本轮只做首个可跑纵切。

【本轮任务】
- Entry 空态 → 新建 1 题 1 知识点 → 生成计划 → PlanPreview 显示 1 页 1 条；
- ViewModel 不持有 Context；错误映射为 UiError(code, message, retryable)；`PaginationResult.Failure` 与 `PlanGenerationException` 走该通道（C-047）；
- 空计划（C-060）与 overflow warnings（C-061/C-062）分别映射为可读空态/提示，不得只保留 pages；提示文案按 C-066（减少关联知识点/缩短描述，不拆分题目）；
- 计划层把 `KNOWLEDGE_TEXT_OVERFLOW` 与分页警告合并（按 questionId + code 去重、按 questionId 排序，C-064 方案 A）；PlanPreview 只展示中文标签与 message，不展示内部代码（C-065 方案 B）；映射保证 knowledgeNames 数量 = 知识点数量（C-067）；
- 先写 UseCase/状态测试，再写 Compose；
- 在实体机上安装并截图到 evidence/screens/（无设备就停下来报告“无设备”，不要伪造截图）。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标单测 ./gradlew :app:testDebugUnitTest，再跑本模块全部测试；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- 不要 git commit，除非我明确要求。
```

> 落地说明（2026-11-18）：已新增 `:feature:entry`/`:feature:planning`，走通 Home → Entry（空态/新建 1 题 1 知识点）→ PlanPreview（1 页 1 条）。`core:common` 补 `UiState`/`UiError`/`PlanIdGenerator`/`KnowledgeTextMeasurer`；`data:room` 补录入事务、`schedulerQuestions()`、`pendingCoolingOverrides()`；`core:pagination` 补 `rightTextWidthMm` 等宽度派生量。C-047 由计划层在写库前抛 `PlanGenerationException`，预览状态映射为 `PLAN_*` 可重试错误；C-060 空计划与全 overflow 返回带提示的空态；C-064 合并去重排序、C-065 中文标签、C-066 文案、C-067 名称数量均有测试锁定。计划生成返回类型与 `planning.md` 的差异已按 C-090 方案 A 裁决并回写：`execute()` 返回 `PlanGenerationOutcome`，成功含 `PrintJob` + 预览投影，空计划含空态与提示。知识点编号序列表/墓碑、`planId` 冲突重试未落，见 agent-log 未决。5.2 完成时无设备；设备安装与截图已由 5.3 补做并通过：Entry 空态 → 1 题 1 知识点 → PlanPreview 1 页 1 条，另验证 C-060 空计划文案，证据见 `evidence/screens/step01_home.png`～`step08_plan_empty.png`。

### 5.3 实体机 UI 验证【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/app.md、docs/specs/entry.md、docs/specs/planning.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 5（App 纵切），规格 docs/specs/app.md、entry.md、planning.md；用实体机验证，不装模拟器。

【本轮任务：实体机 + adb 做 observe-act-verify】
1) adb devices -l，确认设备序列号；无设备就停下来报告“无设备”，不要伪造截图。
2) ./gradlew :app:assembleDebug；
3) adb install -r app/build/outputs/apk/debug/app-debug.apk；
4) adb shell am start -n <applicationId>/<activity>；
5) adb shell uiautomator dump /sdcard/window.xml 并 pull 到 evidence/ui/；
6) adb exec-out screencap -p > evidence/screens/step01.png；
7) 用 adb shell input tap/text 执行一步操作，再截图；
8) 断言用 UI 树文本或截图，失败就贴出证据，不要口头说“应该可以”。
不要使用 android-emu-agent daemon（原生 Windows 不可用）。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要验证的路径与用例编号。
2. 每步操作前说明预期，再执行 adb 命令。
3. Verify：截图/UI dump 与预期不一致时贴出证据并停下，不绕过。
4. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、操作步骤、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据；不伪造截图。
- 不为了让测试变绿而放宽断言。
- 不要 git commit，除非我明确要求。
```

> 落地说明（2026-11-18）：已在实体机 `AXRBUT6518000086`（VKJ-AN00，1264×2728）完成安装与纵切验收。`adb install -r` 成功，冷启动 `MainActivity` 成功；Home → Entry 空态 → 输入 `T－1` / `M` → 保存（`已有 1 道题`）→ 生成计划 → PlanPreview 显示 `计划号 HSBJ4T · 共 1 页`、`第 1 页 · 2026-11-23 · 1 条`、`题号 T－1`。截图在 `evidence/screens/step01_home.png`～`step08_plan_empty.png`，UI 树在 `evidence/ui/`。清空数据后从 Home 直接生成计划，已验证 C-060 空计划文案 `当前没有需要复习的题目；先录入题目和知识点，再来生成计划。`（`step08_plan_empty.png`）。中文 IME 把知识点名称截成 `M`、题号连字符为全角；不影响“1 题 1 知识点”验收。键盘遮挡按钮，用 ESC（keyevent 111）收起；中途一次返回键结束了 Activity，已重启并如实保留 `step03_accidental_exit.png`；一次截图为黑屏（设备休眠），唤醒后重跑。C-061/C-062 的全 overflow 界面路径未在设备上构造（由 JUnit 用例覆盖），打印/扫描仍未开始。2026-11-18 追加：录入表单新增来源页码 `sourcePage`（必填，定位题目在试卷/作业本的第几面，不是打印稿 pageId）；`Question`、`EntryDraft`、`EntrySummary`、状态层与 Repository 双重校验同步；schema v4（`ALTER TABLE question ADD COLUMN sourcePage TEXT NOT NULL DEFAULT ''`），旧题在列表显示「未填写」。同日打印稿右上第一行改为「页码 01　题号 T-1　来源页 P12」，页码只显示页序、不显示计划号；`RenderableQuestion` 增加 `sourcePage`，计划层从 `QuestionRepository.sourcePages()` 填充；确认页逐行显示「来源页」（空值「未填写」）。

### 5.4 知识点标签库与底部录入卡片【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/app.md、docs/specs/entry.md、docs/specs/data-model.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】知识点名称不应以纯文本输入。每个知识点是一个标签，题目从标签库选择标签；
标签库允许增删改查（app.md「KnowledgePoints」：编号/名称列表、rename、删除；被题目引用不可删、删除确认）。
D-12：单题关联 1～4 个知识点；编号全局唯一、不复用；删除写墓碑、编号序列只增不减。

【本轮任务】
- 标签库：新增/重命名/搜索/删除；删除前校验未被未归档题目引用，否则拒绝并提示；
- 编号：`KnowledgeNumberSequence` 事务内分配，跳过 `KnowledgePointTombstone`；空序列按现有最大编号种子（E-10）；
- 录入：Entry 表单从标签库选择 1～4 个标签，UI 与 Repository 双重校验；不再输入知识点名称；
- 布局：题目列表占满页面；新建/编辑使用独立页面（悬浮按钮或卡片铅笔进入），不再使用底部卡片；
- 页码：用户通过「页码编号」控件内的 +/− 调整编号（最小 1，默认沿用最近一次使用值，保存后不自增，同一页可复用）+ 用户可编辑说明（可留空）；说明为空时只显示编号、不输出「-」；输入框提示保持简短；
- 先写 Repository/ViewModel 测试，再写 Compose；Entity 变更同步 schema 导出、迁移与 `RoomSchemaTest`；
- 实体机上验证增删改查、编号不复用、录入关联与重启持久化，截图与 UI 树存 evidence/。

【固定循环（本轮只走一遍）】
1. Observe：说明涉及的模块、文件、用例编号与决策编号。
2. Red/Green/Refactor：按用例逐个推进，测试保持绿。
3. Verify：./gradlew test verifyCorePurity；:app:assembleDebug；实体机安装并逐项操作验证。
4. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md。
- 不引入 libs.versions.toml 之外的新依赖。
- 不用 mock/模拟数据充当识别验收证据；不伪造截图。
- 不为了让测试变绿而放宽断言。
- 不要 git commit，除非我明确要求。
```

> 落地说明（2026-11-20）：已落地标签库与标签式录入。`data:room` 新增 `KnowledgePointLibraryRepository`（`tags/create/rename/deleteWithTombstone`）、`KnowledgePointTombstone`、`KnowledgeNumberSequence`、对应 DAO 与 v4→v5 迁移；`RoomEntryRepository.create` 改为接收 1～4 个 `knowledgePointIds`，事务内校验存在性并重算 `activeQuestionCount/dormant`。`feature:entry` 新增标签库页面（搜索、新增、重命名、删除确认、引用计数与禁用删除）与 `KnowledgePointLibraryViewModel`；`EntryScreen` 改为列表占满页面 + 可拖动底部卡片（`ModalBottomSheet`）录入，悬浮按钮打开，FilterChip 选择标签。测试：`data:room` 补标签库与录入事务用例（编号分配/墓碑跳过/空序列种子/引用不可删/越界与悬空拒绝），`feature:entry` 补标签库与录入 ViewModel 用例。实机 `AXRBUT6518000086` 已跑通新增 #6→重命名→删除→新建编号跳到 #7（不复用）、录入 T-6 关联 A/Temp、重启后持久化；证据见 `evidence/screens/tag-*` 与 `evidence/ui/tag-*`。同日按用户要求把来源页码拆为编号 + 可选说明：`Question` 增加 `sourcePageCode`，`sourcePage` 只存说明；展示/打印用 `formatSourcePage` 拼为 `编号-说明`（空说明不带「-」）；schema v6 迁移按创建时间补 1..N 编号。用户随后要求编号不再每题自增，改为在「页码编号」文本框内自带上下堆叠的 +/− 调整（最小 1、默认沿用最近一次使用值、保存后不自增，同一页的多道题共用编号），schema v7 取消唯一索引。实机验证同页两道题均为 `9-Same`、重开卡片仍为 `9`，证据见 `evidence/screens/pageadj-*`。同日补齐题库增删改查：卡片右侧增加编辑（铅笔）与删除（垃圾桶）图标；铅笔打开同一录入卡片并回填题号/页码/标签/分钟，保存覆盖原题；删除为归档（D-11）并二次确认；`EntryRepository.update/archive` 在事务内替换关联、重算标签活跃计数；实机验证 T－1 → T－1E、页码 1→3、关联 A→A/D，删除 T－4 后列表 12→11，重启后保持；证据见 `evidence/screens/crud-*`。entry.md/data-model.md/print-template.md/confirm-flow.md 已按用户要求同步；无新增冲突。随后按用户要求把「录入/编辑题目」从底部卡片改为独立页面：新增 `QuestionEditViewModel`/`QuestionEditScreen`，复用 `AppRoute.QuestionEdit(questionId)`（null 为新建、非空为编辑），Entry 列表只保留列表与删除；实机验证新建页、编辑回填（T-7 → 8-P21）、保存返回与返回箭头不保存，证据见 `evidence/screens/page-*`。

---

## 阶段 6：Print / Scan / Confirm（R-01 硬门禁）

> 规格：docs/specs/print-template.md、scan-pipeline.md、confirm-flow.md。C-051 方案 A 已裁决；先完成适配层 spike，再写正式管线。没有真实打印稿回读证据，不得声称完成。

### 6.1 打印几何【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/print-template.md、docs/specs/pagination.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 6（Print），规格 docs/specs/print-template.md；R-02 要求不在 onLayout 重新分页。C-047：分页失败不进入打印；C-060：全溢出日期没有页面，打印层不得补空白页；C-061：两种 overflow 同时成立时归为 `ITEM_TOO_TALL`；C-066：整题过高提示减少关联知识点或缩短描述，不再建议拆分题目；C-062：附加上限仅在 `hasNoteArea=true` 时生效，不得截断附件区。C-063：不绘制“续下页”，每个日期第一页标注“本日共需复习 X 题”（X = 实际打印题目数），总题数字号与图例一致、高度为日期标题行高的一半。C-051 方案 A：打印验收以适配层回读为准。C-091 方案 A 已裁决：生产模板用 `TemplateGeometry.a4Print()`，内容区从 26mm 开始以避开四角标记（规格回写待授权）。本轮只做打印布局，不做回读。

【本轮任务】
- 按 print-template.md 实现 A4 几何、左侧填涂区、右上信息、右下笔记、QR 预留、四角 ArUco；
- 只消费 `PagePlanner` 的 `PlannedPage`/`PlacedItem` 映射，不在 `onLayout` 重算高度或重分页（R-02）；
- 用 PrintedPdfDocument 生成 PDF，保存到 evidence/print/；
- 断言几何参数与 TemplateGeometry 一致；`hasNoteArea=false` 的条目按 `rightBottom=0` 绘制，附件不截断、不补画（C-062）；
- overflow 条目只输出 warnings，不生成占位行或空白页（C-060/C-061）；
- 不绘制“续下页”；每个日期的第一页绘制日期大标题（默认 `dateHeadingFontMm=5mm` / `dateHeadingHeightMm=7mm`），并在其下标注“本日共需复习 X 题”（字号与图例一致：3.2mm；高度为日期标题行高的一半：3.5mm；top 不变，合计 10.5mm 绘制在默认 12mm 上边距内，不占用内容区），X = 该日期实际打印的题目数，不含 overflow（C-063）；不得在 onLayout 临时压缩行高；
- 警告只渲染中文文案/映射标签，不展示内部代码（C-065）；`KNOWLEDGE_TEXT_OVERFLOW` 由计划层产出并合并（C-064）；overflow 提示文案按 C-066：整题过高提示减少关联知识点/缩短描述，附加超限提示减少附加笔记，不得建议拆分题目。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标单测 ./gradlew :app:testDebugUnitTest，再跑本模块全部测试；生成 PDF 并确认文件非空；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- 不要 git commit，除非我明确要求。
```

**6.1 落地进度（2026-11-18）**

- 已新增 `:feature:print`：`PrintLayoutBuilder` 只消费 `PlannedPage`/`PlacedItem` 生成毫米布局；`PrintedPdfDocument` + `PrintDocumentAdapter` 已可输出 A4 PDF；`KnowledgeTextMeasurer` 实现迁入该模块。
- 已用设备 `AXRBUT6518000086` 生成 `evidence/print/recollect-print-K7M2Q9.pdf`（2 页、146878 字节）与 300 DPI PNG；`PrintLayoutTest`（JUnit 5）与设备 `PdfRenderer` 页数断言均通过。
- **C-091 已按方案 A 裁决并落地（2026-11-18）**：`TemplateGeometry` 新增 `cornerReserveMm` 与生产模板 `a4Print()`；内容区从 26mm 开始（12mm 页边距 + 14mm 预留），宽 158mm、可用高 245mm；`DefaultPagePlanner` 起点改用 `contentTopMm`，`PlanningSettings.template` 默认改用 `a4Print()`。渲染图已确认左上角不再覆盖图例/表头/首行。归档见 `docs/conflicts-archive.md` C-091。
- **规格已授权回写（2026-11-18）**：`docs/specs/print-template.md` 补四角预留/C-091、日期标题预留带与验收；`docs/specs/pagination.md` 增加 `cornerReserveMm`/`contentLeftMm`/`contentTopMm`/`a4Print()` 与 P-21；`docs/specs/scan-pipeline.md` 的 OMR 左区/列宽改按 `a4Print()`（26mm 起点、3.5mm 框）；`docs/specs/planning.md` 默认模板改为 `a4Print()`。
- 仍未完成：四角 ArUco 只是标记位占位，`DICT_4X4_50` 实际位图与 QR 载荷生成留到 6.2；本轮未做任何回读，不声称 R-01 通过。
- 6.2 必须用 `TemplateGeometry.a4Print()` 的几何；不得回退到 12mm 内容区，也不得把标记移到与扫描规格不一致的位置。

**2026-11-18 追加：打印计划页已接入（6.1 后续）**：`:feature:print` 新增 `PrintPlanScreen`/`PrintPlanViewModel` 与 `AndroidPrintLauncher`；PlanPreview 成功生成计划后保留同一份 `PrintJob`，Home 与 PlanPreview 均可进入打印页，点击“打开系统打印”调起系统打印对话框。打印文档写出后不再自动回写：App 内显示「已打印 / 未打印」确认，用户点「已打印」才通过 `PrintSnapshotWriter` 回写 `Question.pageId/qrPayload`（C-094 方案 B）；页脚 QR 左侧以 2.2mm 小字显示完整 `pageId`（C-095 方案 B）。实体机 `AXRBUT6518000086` 已完成 PlanPreview → 打印页 → 系统打印（A4 2 页）→ 保存为 PDF，证据见 `evidence/screens/print-*`、`evidence/print/recollect-XW21BB.pdf` 与 `print-confirm-recorded.xml`。未完成：`PrintPlanSession` 仅内存保存、进程重建后需重新生成计划。

### 6.2 OpenScanVision 回读 spike【done】

> 2026-11-18 完成：真机把 2480×3508 渲染页喂给库，原始输出为 `ARUCO_RAW=none`、`LowConfidence(confidence=0.0)`；库模板写死、只解一个 QR。结论与证据见 `evidence/print/osv-spike/`，已按 C-092 转方案 C（自研引擎）。

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/scan-pipeline.md、docs/specs/print-template.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 6（Scan），C-051 方案 A 已裁决；本轮只做适配层 spike，不写正式管线。没有真实打印稿回读证据，不得声称完成。

【本轮任务】
- 用 Android PdfRenderer 把打印 PDF 渲染成 2480×3508 Bitmap；
- 喂给 OpenScanVision 1.0.0（根坐标 com.github.MatiwosKebede:openscanvision:1.0.0），记录 ArUco 定位、透视矫正、QR 解码、OMR 三档识别的原始输出；
- 人工比对只作辅助记录；把 PDF、PNG、原始输出、测试报告存 evidence/print/；
- 如果 850×540 的 OMR 精度不达标，停下来在 docs/conflicts.md 新开一条 spike 结论，建议转方案 B；不要在未验证的情况下改 specs 或声称通过；
- 在 docs/dependency-proposals.md 记录本 spike 结论与 16KB 页面对齐风险（C-051）。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、spike 目标与决策编号。
2. 先写最小可运行的验证测试/脚本，再运行并把完整原始输出贴出来。
3. 失败就贴证据，不修改断言迁就实现。
4. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据；人工比对不能替代原始输出。
- 不为了让测试变绿而放宽断言。
- 不要 git commit，除非我明确要求。
```

### 6.3 扫描管线【done】

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/scan-pipeline.md、docs/specs/print-template.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 6（Scan），规格 docs/specs/scan-pipeline.md；**C-092 方案 C 已裁决**：自研 `ScanEngine`（OpenCV 做 ArUco/透视、ML Kit 做逐题 QR、OMR 按规格自采样），OpenScanVision 1.0.0 停用；**C-093 方案 A 已裁决**：回读几何随打印快照持久化（`plan_page.itemGeometry`，schema v3），扫描端不再由调用方传入几何。C-038～C-041：批次实际复习日期、`ScanSession` 多页追加/去重、提前/晚扫描标记。C-052 方案 C：单元测试 JUnit 5、androidTest JUnit 4。R-04：回读只写 `PendingFeedback`，人工确认后才写 `ReviewFeedback`。本轮只做 ScanEngine 适配层。

【本轮任务】
- 按 scan-pipeline.md 的顺序：ArUco → 透视 → 每题 QR → OMR → OCR → `PendingFeedback`；
- QR 校验失败/归属失败不进入确认页；多页扫描按 `ScanSession` 追加并去重，记录提前/晚扫描（C-038～C-041）；
- OCR 只写 detectedRawOcr，不写 ReviewFeedback；本阶段任何路径都不得直接改知识点状态（R-04）；
- 先写 QR 载荷/CRC/归属校验的单测（JUnit 5），再写图像管线；`androidTest` 保持 JUnit 4。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标单测 ./gradlew :app:testDebugUnitTest，再跑本模块全部测试；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- 不要 git commit，除非我明确要求。
```

### 6.4 确认页

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/confirm-flow.md、docs/specs/feedback-usecase.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 6（Confirm），规格 docs/specs/confirm-flow.md；实体机验证，不装模拟器。C-050：确认后的状态更新内部 `Double`、落库 `Float`，`effectiveFeedbackCount` 精确 `>= 3.0f`。C-052 方案 C：单元测试 JUnit 5，androidTest JUnit 4。R-04/D-13：逐项确认，NOT_REVIEWED 只写决定日志。C-077 A/C-078 A：整题跳过不写反馈/状态/复习日但仍写日志并展示清单；C-079 A：提交前置校验由 UseCase 承担，确认页保留按钮禁用双保险；C-080 A：已提交会话拒绝重复提交；C-081 A：复习日下界按用户设置的计划周起始日；C-088：补填必须可测且计入有效反馈。本轮只做 Confirm。

【本轮任务】
- 逐项标记 ACCEPT/CORRECTED/NOT_REVIEWED/SUPPLEMENTED，禁止一键全部接受；
- NOT_REVIEWED 只写 ConfirmationDecisionLog；写入知识点状态时按 C-050 在边界转 `Float`；
- 先写状态机测试（JUnit 5），再写 Compose；
- 实体机跑一遍并留截图到 evidence/screens/：adb devices -l → 安装 → 启动 → 操作 → screencap；无设备就停下来报告“无设备”，不要伪造截图。

【固定循环（本轮只走一遍）】
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、用例编号与决策编号。
2. Red：为当前用例只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标单测 ./gradlew :app:testDebugUnitTest，再跑本模块全部测试；实体机截图；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不引入 libs.versions.toml 之外的新依赖；需要时先在 docs/dependency-proposals.md 写名称、版本、用途、替代方案、体积/ABI/16KB 风险，等确认后再改构建文件。
- 不用 mock/模拟数据充当识别验收证据；不伪造截图。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- 不要 git commit，除非我明确要求。
```

**2026-11-18 已落地（【done】）**：新增 `:feature:confirm`（`ConfirmStateMachine` 纯 Kotlin 状态机 + JUnit 5 测试 + Compose 页面）。逐行提供接受/修正/本次未复习/补填，无批量接受；未标记行存在时按钮禁用，提交仍由 `ApplyFeedbackUseCase` 复核；NOT_REVIEWED 只写决定日志（`actualReviewDate=null`），C-050 的 `Float` 边界与 C-088 补填计数由既有 UseCase 承担。实体机 `AXRBUT6518000086` 完成四类决策提交：3 条反馈、4 条决定日志、未复习知识点完全不变；重复打开已提交会话被拒绝。证据见 `evidence/screens/confirm-*` 与 `evidence/screens/confirm-device-verify.txt`。未覆盖：逐行复习日覆盖/LATE_BATCH、冷却覆盖事件、保存为笔记附件、确认页渲染前/提交前完整归属复校验、整题跳过设备样例。扫描回读页已于 2026-11-20 落地，确认页现有普通用户路径：Home → 扫描回读 → 识别成功 → 进入确认页；`MainActivity --es confirmSessionId` 保留为调试入口。

**2026-11-20 追加：扫描回读页【done】**：`AppRoute.Scan` 不再是占位。`:feature:scan` 新增 `ScanViewModel`/`ScanScreen`、手动回退 `ScanManualCodec`/`ScanManualEntryUseCase` 与 `ScanImageLoader`；`ScanPageUseCase` 支持由 `pageId` 推导 planId，并在 `Stored` 返回 `replacedPage`；`data:room` 的 `ScanSessionRepository` 新增 `activeSession()` 以恢复最近一次未提交会话。**C-096 已裁决方案 B（2026-11-20）**：扫描页改用 CameraX 页内取景（`camera-core/camera2/lifecycle/view:1.7.0`），预览容器为 A4 长宽比 `210:297`；进入页面请求 `CAMERA` 权限，拒绝时保留相册选图。页面把 QR 类失败开放「手动输入 pageId + 题号」，`PAGE_*`/`OWNERSHIP_MISMATCH` 只提示重拍或重新打印；重复页、`LATE_BATCH`、未来日期均有提示。`./gradlew test verifyCorePurity` 与 `:app:assembleDebug` 通过（feature:scan 46 个用例）。实体机 `AXRBUT6518000086` 已验证：权限授予、A4 取景、CameraX 拍摄（无纸张时 `PAGE_NOT_FOUND`）、相册选图、仅保留四角标记的渲染页触发 `QR_NOT_FOUND`、手动输入 `ZQ5FN8-01 + T-2`、进入确认页并提交（0 条反馈、1 条决定日志）；证据见 `evidence/screens/scanA4-*`、`evidence/ui/scanA4-*`。**R-01 仍需真实打印稿的纸张照片**，渲染图/相册图不能代替。

### 6.5 R-01 收尾

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及 docs/specs/scan-pipeline.md、docs/specs/print-template.md。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

【本轮背景】阶段 6（R-01 硬门禁）验收，不写新功能。C-051 方案 A 已裁决；没有真实图像证据，不得声称通过。

【本轮任务：验收】
1) 5 张不同光照/角度的真实打印稿跑完整管线；
2) 证据存 evidence/scan/；
3) 断言 OMR 分组与人工核对一致（允许 <=1 组误判）；
4) 如果失败，先修几何/参数，不回退 D-07；仍失败就写 docs/conflicts.md；
5) 按 C-051 记录适配层精度结论；不达标时在冲突条目中建议转方案 B，等人类确认后才能换实现，不得自行 fork 或替换库；
6) 没有真实图像证据，不得在 agent-log 写“通过”。

【本轮任务：收尾】
- 更新 docs/changelog.md（文件、意图、关联决策）与 docs/agent-log/YYYY-MM-DD.md（目标、验证步骤与结果、未决问题、下轮建议）；
- 列出所有未通过项与证据路径；
- 不要 git commit，除非我明确要求。

【硬性禁止】
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md：按“关联决策/规格、现象、对用户的实际影响、可选方案、以用户影响为依据的简短选型参考、建议、裁决前禁止事项”追加条目（描述尽量少用内部变量名/类名），编号顺延，不替人类裁决。
- 不用 mock/模拟数据充当识别验收证据；人工核对不能替代真实图像证据。
- 不为了让测试变绿而放宽断言。
```

---

## 附录：固定循环速查

```text
Observe → Red → Green → Refactor → Verify → Record
```

| 步骤 | 产物 | 命令/位置 |
|---|---|---|
| Observe | 本轮用例编号、涉及文件 | 读 CLAUDE.md + 本轮 spec |
| Red | 一个失败测试 + 失败输出 | `./gradlew :core:<module>:test --tests "*XxxTest"` |
| Green | 最小实现 | 只改本轮模块 |
| Refactor | 不改变行为的整理 | 重跑目标测试 |
| Verify | 模块测试 + 纯度门禁 | `./gradlew :core:<module>:test verifyCorePurity` |
| Record | changelog + agent-log | `docs/changelog.md`、`docs/agent-log/YYYY-MM-DD.md` |

> 辅助 prompt 已内联：收尾/规格审计/代码审查 → 1.8、2.8、4.4；实体机验证 → 5.3、6.4、6.5；冲突记录/新增依赖规则 → 每个小阶段的「硬性禁止」。
> 记住：**没有测试输出的“完成”不算完成；没有真实图像的“识别通过”不算通过。**
