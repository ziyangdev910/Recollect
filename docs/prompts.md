# TDD Prompts（按阶段、带固定循环）

> 用途：把 CLAUDE.md 的 Observe → Act → Verify → Record 工作循环落成可直接复制给 Claude Code 的提示词。
> 原则：**一次只做一个用例**；先红后绿；不跳过失败测试；不改 specs；不引入表外依赖。
> 每条 Prompt 都建议先粘贴「通用前缀」，再粘贴对应阶段的具体任务。
>
> 已裁决摘要（2026-11-13）：C-046 方案 A（core:model 纯 DTO/Codec，映射在 data/room）；C-047 方案 A（PaginationResult sealed）；C-048 全部建议；C-049 全部建议；C-050 建议 2/3（内部 Double、阈值精确）；C-051 方案 A（OpenScanVision 根坐标 + 适配层 spike）；C-052 方案 C（Android 单元测试 JUnit 5，androidTest JUnit 4）。

---

## 0. 通用前缀（每条 Prompt 前都加）

```text
你是本仓库（d:/Engineering/Android/Recollect）的 Android 工程师。
开始前先读：CLAUDE.md、docs/architecture/overview.md、docs/conflicts.md，以及本轮涉及的 docs/specs/ 文件。
必须遵守 D-01～D-13、R-01～R-06，以及 docs/dependency-proposals.md 的依赖约束。

固定循环（本轮只走一遍）：
1. Observe：用 3～5 行说明本轮涉及的模块现状、要改的文件、对应的用例编号与决策编号。
2. Red：只写一个失败测试；运行目标测试并把完整失败输出贴出来。
3. Green：写最小实现让该测试变绿；不顺手实现其它用例。
4. Refactor：只重构本轮代码，保持测试绿。
5. Verify：先跑目标测试，再跑本模块全部测试；最后跑 ./gradlew verifyCorePurity。
6. Record：更新 docs/changelog.md 与 docs/agent-log/YYYY-MM-DD.md，写清目标、改动文件、验证命令与结果、未决问题、下轮建议。

硬性禁止：
- 不改 docs/specs/、不修改 D-01～D-13；发现冲突只写 docs/conflicts.md。
- 不引入 libs.versions.toml 之外的新依赖；需要时先写 docs/dependency-proposals.md。
- 不用 mock/模拟数据充当识别验收证据。
- 不同时实现多个用例；不为了让测试变绿而放宽断言。
- core 模块不得出现 import android.* / androidx.*。
```

---

## 1. 阶段 0：Bootstrap 验证

> 已由 2026-11-13 的初始化完成；以后换机器或升级版本时重跑。

```text
先不要写业务代码。验证当前构建骨架：
1) 运行 ./gradlew --version，确认是 Gradle 8.13、Launcher JVM 17；
2) 运行 ./gradlew projects，确认 :core:common、:core:model、:core:scheduler、:core:pagination 都在；
3) 运行 ./gradlew :core:scheduler:compileKotlin，确认无源码也能成功；
4) 运行 ./gradlew verifyCorePurity；
5) 如果任一步失败，只修构建文件（settings.gradle.kts、build.gradle.kts、libs.versions.toml、模块 build.gradle.kts），不要写业务 Kotlin。
把每条命令的输出摘要贴出来，并写 docs/agent-log/YYYY-MM-DD.md。
```

---

## 2. 阶段 1：core:scheduler

> 规格：docs/specs/scheduler.md。每轮只做一个用例；先跑 `./gradlew :core:scheduler:test --tests "*SchedulerPlanTest"`。

### 1.1 第一个红测：S-01

```text
本轮只做 S-01（D-01）：
- 新建 core/scheduler/src/main/kotlin/.../Scheduler.kt，只放 scheduler.md 第 20–76 行的公开类型与接口（Tier、KnowledgeState、Question、PlanResult、PlannedQuestion、DeferredItem、DeferReason、Scheduler）。
- 新建 core/scheduler/src/test/kotlin/.../SchedulerPlanTest.kt，只写 S-01：
  k1(UNKNOWN, nextDueDate=2026-11-16, activeQuestionCount=1) + q1(linkedKnowledgeIds=[k1])，
  dateRange=2026-11-16..2026-11-22，dailyAvailableMinutes 每天 60。
  断言 dailyAssignments[2026-11-16] 只有 q1，triggeredKnowledgeIds=[k1]，estimatedMinutes=15，deferred 为空。
- 运行测试，确认红；再写 DefaultScheduler 的最小实现（只做候选过滤 + targetDate + 直接放入目标日，不排序、不装箱），让 S-01 绿。
- 不要实现 S-02、S-08、S-09 或排序；不要改 specs。
```

### 1.2 归档与休眠：S-08 / S-09

```text
本轮只做 S-08、S-09（D-11）：
- S-08：archived=true 的题目不出现在任何 dailyAssignments。
- S-09：dormant=true 或 activeQuestionCount=0 的知识点不调度，也不进入 nonTriggeredKnowledgeIds。
- 先各写一个失败测试，确认红；再在 DefaultScheduler 的候选过滤中补最小逻辑。
- 验证：./gradlew :core:scheduler:test --tests "*SchedulerPlanTest"。
```

### 1.3 冷却与一次性覆盖：S-02 / S-19

```text
本轮只做 S-02、S-19（D-04）：
- S-02：lastReviewedDate 使 lastReviewedDate + coolingPeriodDays > targetDate 时，题目不排入；生成 DeferredItem(COOLING)，knowledgePointIds 含全部触发知识点，targetNextDueDates 每个 +1 天。
- 边界：lastReviewedDate + 7 == targetDate 时仍正常排入（冷却判定是严格大于）。
- S-19：coolingOverrides 命中 questionId 时本次跳过冷却并正常排入；未命中/已消费的 id 不生效。
- 先红后绿；冷却判定只在 scheduler 内，不写数据库。
```

### 1.4 候选排序：S-04 / S-05 / S-18

```text
本轮只做候选排序层（D-02、D-03）：
- 实现优先级：hasTierGap → groupTier（MASTERED > FAMILIAR > UNKNOWN）→ 组内 min continuousScore → targetDate → questionId 字典序。
- 先写 S-04（有档位差优先，但不淘汰）、S-05/S-18（跨档位分数反例：Q1 同时触发 MASTERED 0.9 与 UNKNOWN 0.1，Q2 触发 MASTERED 0.5，Q2 必须在前）。
- 禁止跨 groupTier 比较 continuousScore。
- 时间容量给足，避免和装箱混在一起。
```

### 1.5 时间装箱：S-05b / S-15 / S-17

```text
本轮只做时间装箱层（C-017、C-020）：
- 按日期升序，每日只处理 targetDate == 当天的候选；容量缺失按 0。
- first-fit：剩余容量 >= estimatedReviewMinutes 则排入；否则 DeferredItem(TIME_OVERFLOW)，继续尝试后续候选，不停止整日。
- estimatedReviewMinutes 缺失或 <= 0 按 15。
- 先写 S-05b（20/20/10，容量 30，第 2 条溢出后第 3 条仍可入）、S-15（0/-5 按 15）、S-17（第 1 天溢出不去第 2 天，第 2 天候选不填第 1 天）。
- 不要在装箱阶段改变候选顺序。
```

### 1.6 日期范围与稳定输出：S-13 / S-14 / S-16

```text
本轮只做日期与顺序：
- S-13：逾期知识点落到 dateRange.start，只排一次，日期键升序。
- S-14：一题多个知识点在范围内到期，只排最早 targetDate 一次，全部计入 triggeredKnowledgeIds。
- S-16：输入 Map/List 顺序打乱，输出结果相同。
- 输出 dailyAssignments 用 LinkedHashMap 或按日期显式排序；没有条目的日期不出现。
```

### 1.7 校验与属性测试：S-10 + property

```text
本轮做最后两组：
1) S-10：linkedKnowledgeIds 数量 > 4 时抛 IllegalArgumentException；0 个关联跳过，不进入候选（C-049）。
2) 属性测试：JUnit @RepeatedTest(100) + kotlin.random.Random(20261113)，断言：
   - archived/dormant 不出现在任何 dailyAssignments；
   - 同一题目同一日不重复；
   - 同一题目在一份计划内至多出现一次；
   - dailyAssignments 的日期键严格升序。
3) 跑 ./gradlew :core:scheduler:test 与 ./gradlew verifyCorePurity，确认全绿、无 @Ignore。
```

### 1.8 模块收尾 + 规格审计

```text
本轮不写新功能，只做收尾：
1) 逐条核对 scheduler.md 的用例表 S-01～S-19，列出每个用例对应的测试方法；缺哪个补哪个（仍按先红后绿）。
2) 检查测试中没有 android.* / androidx.* import；运行 ./gradlew verifyCorePurity。
3) 用 3～5 行写 docs/agent-log/YYYY-MM-DD.md，并把本次改动记入 docs/changelog.md。
4) 另起一个只读审计任务：读 docs/specs/scheduler.md 与当前实现，列出规格与代码不一致的地方；只写 docs/conflicts.md，不改代码、不改 specs。
```

---

## 3. 阶段 2：core:pagination

> 规格：docs/specs/pagination.md。C-047（sealed `PaginationResult`）与 C-048（放置语义）已裁决，按规格实现；不要自行选择其它放置策略。

### 2.1 锁定模板几何：TemplateGeometryTest

```text
本轮只做 TemplateGeometry 的派生量测试，不实现 PagePlanner：
- 用默认参数断言：usableHeightMm=273.0、firstPageUsableHeightMm=265.0、contentHeightMm=266.0、firstPageContentHeightMm=258.0、maxAttachmentHeightMm=81.9、qrFooterHeightMm=21.0。
- 先写失败测试（可以先用 TODO/临时实现让编译通过），再补 TemplateGeometry。
- 注意：默认几何下 P-05/P-11/P-13/P-14/P-18 不可达；后续这些用例必须显式传自定义 template。
```

### 2.2 最小分页：P-01

```text
本轮只做 P-01：
- 一日一条、高度正常 → 一页一条；pageId="{planId}-01"；itemHeight 按 pagination.md 的公式计算。
- 先写失败测试，再写 PagePlanner 的最小实现：每日一页、按公式算高、顺序放置；不做移页、不做 overflow、不做附件上限。
- 不要碰 P-03/P-08/P-10/P-13。
```

### 2.3 移页与续下页：P-03（C-048）

```text
本轮只做 P-03：
- 四条默认高度 68.5mm 的条目；第 4 条放不下时整条移到下一页，第 1 页生成恰好一个 ContinuationMarker(order=0)，from/to pageId 正确。
- 按 C-048：遇到第一条放不下的条目就结束当前页，该条及后续条目按原顺序进入下一页；不得跳过该条去填更小的后续条目。
- 先红后绿；不要拆分条目、不要压缩行高。
```

### 2.4 跨日与首页差异：P-08 / P-10 / P-13

```text
本轮只做跨日与首页/后续页：
- P-08：每日从新页开始，pageNumber 全文档连续，同日多页 date 相同，日期升序。
- P-10：首页扣 legendHeightMm + headerRowHeightMm；后续页只扣 headerRowHeightMm。
- P-13：条目能进后续页但进不了首页时，首页仍存在、items 为空、有一个 marker；条目完整出现在下一页。
- 先红后绿；不要改分页公式。
```

### 2.5 溢出边界：P-14 / P-18

```text
本轮只做单题过高：
- P-14：itemHeight > contentHeightMm → OverflowItem(ITEM_TOO_TALL)，不进 pages，不拆分、不压缩。
- P-18：itemHeight 落在 (contentHeightMm, usableHeightMm] 也必须进入 ITEM_TOO_TALL，不得出现“既放不下又不在 overflow”的区间。
- 先构造自定义 template 让条件可达（默认几何不可达），再写红测、再实现。
```

### 2.6 附件上限：P-05 / P-11

```text
本轮只做附加笔记区：
- P-05：attachmentCount 1/2/3 时 rightBottom 递增；附件面积超过 usableHeightMm*0.30 时整条进入 ATTACHMENT_OVER_LIMIT。
- P-11：恰好 30% 通过；超过 1mm 进入 ATTACHMENT_OVER_LIMIT；不截断附件区。
- 必须用自定义 template（pageHeight/margins/attachmentHeightPerItemMm），默认几何不可达。
```

### 2.7 其余边界 + 属性测试

```text
本轮补齐 P-02/P-12/P-15/P-16/P-19 与 P-09 属性测试：
- P-02 恰好填满；P-12 输入 Map 乱序；P-15 hasNoteArea=false；P-16 knowledgeLineCount 1..4；P-19 QR 预留区。
- P-09：@RepeatedTest(100)+Random(20261113)，无条目跨页/重叠、marker 与被移条目一一对应、overflow 不在 pages、pageNumber 连续、pageId 唯一。
- 跑 ./gradlew :core:pagination:test 与 ./gradlew verifyCorePurity。
```

### 2.8 模块收尾 + 规格审计

```text
只做收尾：
1) 逐条核对 pagination.md 的 P-01～P-19，列出测试方法映射；
2) 确认 C-047（sealed Failure）与 C-048（放置语义）已反映在实现与测试里；不要假装通过；
3) 运行 ./gradlew :core:pagination:test 与 ./gradlew verifyCorePurity；
4) 写 changelog/agent-log；只读审计 spec 与实现，不一致写 docs/conflicts.md。
```

---

## 4. 阶段 3：core:model

> 规格：docs/specs/repository.md、docs/specs/data-model.md。C-046 方案 A：`core:model` 只做纯 DTO/枚举与 `KnowledgeRowsCodec`；Room ↔ DTO 映射在 `data:room`。

### 3.1 只做 Codec

```text
本轮只做 KnowledgeRowsCodec：
- 用 data-model.md 的 JSON 示例做 decode→encode 往返；
- 断言 rows 数量/顺序、triggeredKnowledgePointIds 原样保留；
- 断言 rows=[number5,number3] 解码后仍是 [5,3]，不按 number 重排（C-015 冻结）；
- 不写任何 Room 映射；不引入 androidx。
```

### 3.2 data/room 映射（C-046 方案 A）

```text
在 data:room 实现 Room Entity ↔ 纯 DTO 映射，不改 core:model 的纯度：
- KnowledgePoint.toKnowledgeState()；
- Question.toSchedulerQuestion(linkedKnowledgeIds)：linkedKnowledgeIds 从 QuestionKnowledgeCrossRef 读取，按 KnowledgePoint.number 升序构造；
- PlannedQuestion.toRenderableQuestion(questionNumber, knowledgeNames, knowledgeLineCount, attachmentCount)：questionNumber 从 Question 实体补齐；
- PlannedPage.toPlanPageRows()；
先写 ModelMappingTest（字段一一对应、difficulty/halfLifeDays=null、estimatedReviewMinutes<=0→15），再实现。
core:model 只允许纯 DTO/枚举与 KnowledgeRowsCodec；不得出现 android.* / androidx.*。
```

### 3.3 首个纵切：FirstVerticalSliceTest

```text
本轮只做一个跨模块纵切测试（不写 UI）：
- 2 个知识点、2 道题 → Scheduler.plan → RenderableQuestion → PagePlanner.plan；
- 断言 1 页 2 条、pageId 格式、D-01（同一题只出现一次）与 D-08（按日分页）；
- 先写失败测试；如果失败原因是规格没有覆盖，停下来写 docs/conflicts.md，不要绕过。
```

---

## 5. 阶段 4：data/room 与 UseCase

> 规格：docs/specs/repository.md、docs/specs/feedback-usecase.md。C-052 方案 C：Android 单元测试用 JUnit 5（`de.mannodermaus.android-junit5`），`androidTest` 用 JUnit 4。

### 4.1 Room schema 与 DAO

```text
本轮只做 data/room 的实体与 DAO：
- 按 data-model.md 建 13 个实体；先只做 KnowledgePoint、Question、QuestionKnowledgeCrossRef、ReviewFeedback、ConfirmationDecisionLog、PendingFeedback、ScanSession、CoolingOverrideEvent、PlanPage；
- 写 schema 测试：字段名/类型/主键/外键/索引与 data-model.md 一一对应；
- 不写业务 UseCase；不写 UI。
- 运行 ./gradlew :data:room:test。
```

### 4.2 Repository 事务

```text
本轮只做 Repository 事务：
- PlanRepository.savePlan：PlanPage + lastScheduledDate + 消费 CoolingOverrideEvent + 冷却顺延，同一事务；
- FeedbackRepository.saveDecisions：ReviewFeedback + ConfirmationDecisionLog + KnowledgePoint + NoteAttachmentItem + 删除 PendingFeedback，同一事务；
- 先写“任一步失败全部回滚”的测试，再实现；
- 不 mock 识别结果；用内存 Room 或 fake repository。
```

### 4.3 ApplyFeedbackUseCase 分批

```text
本轮只做 U-01～U-05：
- 触发行 ACCEPT/CORRECTED；非触发行 ×0.3；NOT_REVIEWED 不写 ReviewFeedback、只写 ConfirmationDecisionLog；
- 先红后绿；每完成一条用例单独跑测试；
- 不要一次实现 U-06～U-19。
```

```text
本轮只做 U-06～U-12：
- 冷启动 1/3/7、FAMILIAR 0.5、2.5→3.0 的切换边界、达到 3.0 的这次仍按冷启动；
- 先红后绿；`continuousScore` 算术用 Double、落库/断言转精确 Float；`effectiveFeedbackCount` 阈值保持精确 >=3.0f，不做 epsilon（C-050）。
```

```text
本轮只做 U-13～U-19：
- 同一 session 多行、冷却顺延、ACCEPT 但 detectedTier=null、历史只有非触发行、第 3 条后初始化记忆状态、批量扫描晚复习、全部 NOT_REVIEWED；
- 先红后绿；事务失败必须回滚。
```

### 4.4 UseCase 收尾

```text
收尾：
1) 逐条核对 feedback-usecase.md 的 U-01～U-19；
2) 跑 ./gradlew :data:test；
3) 写 changelog/agent-log；
4) 只读审计 spec 与实现，不一致写 conflicts。
```

---

## 6. 阶段 5：App 纵切

> 规格：docs/specs/app.md、entry.md、planning.md。实体机验证，不装模拟器。

### 5.1 AppContainer 与导航

```text
本轮只做 app 壳：
- 手写 AppContainer（Repository、UseCase、Scheduler、PagePlanner 单例），不引入 Hilt/Koin；
- 手写 AppNavigator（mutableStateOf + ArrayDeque + BackHandler），不引入 navigation-compose；
- MainActivity 只创建 AppContainer/AppNavigator/AppRoot，不直接访问 Room/CameraX/PrintManager/ML Kit；
- 先写导航返回栈的单测，再写 Compose 壳。
```

### 5.2 录入 → 计划

```text
本轮只做首个可跑纵切：
- Entry 空态 → 新建 1 题 1 知识点 → 生成计划 → PlanPreview 显示 1 页 1 条；
- ViewModel 不持有 Context；错误映射为 UiError(code, message, retryable)；
- 先写 UseCase/状态测试，再写 Compose；
- 在实体机上安装并截图到 evidence/screens/。
```

### 5.3 实体机 UI 验证

```text
用实体机做一次 observe-act-verify：
1) adb devices -l，确认设备序列号；
2) ./gradlew :app:assembleDebug；
3) adb install -r app/build/outputs/apk/debug/app-debug.apk；
4) adb shell am start -n <applicationId>/<activity>；
5) adb shell uiautomator dump /sdcard/window.xml 并 pull 到 evidence/ui/；
6) adb exec-out screencap -p > evidence/screens/step01.png；
7) 用 adb shell input tap/text 执行一步操作，再截图；
8) 断言用 UI 树文本或截图，失败就贴出证据，不要口头说“应该可以”。
```

---

## 7. 阶段 6：Print / Scan / Confirm（R-01 硬门禁）

> C-051 方案 A 已裁决；**先完成适配层 spike，再写正式管线**。没有真实打印稿回读证据，不得声称完成。

### 6.1 打印几何

```text
本轮只做打印布局，不做回读：
- 按 print-template.md 实现 A4 几何、左侧填涂区、右上信息、右下笔记、QR 预留、四角 ArUco；
- 用 PrintedPdfDocument 生成 PDF，保存到 evidence/print/；
- 断言几何参数与 TemplateGeometry 一致；不在 onLayout 重新分页（R-02）。
```

### 6.2 OpenScanVision 回读 spike

```text
本轮只做 C-051 方案 A 的适配层 spike（根坐标 `com.github.MatiwosKebede:openscanvision:1.0.0`）：
- 用 Android PdfRenderer 把打印 PDF 渲染成 2480×3508 Bitmap；
- 喂给 OpenScanVision 1.0.0，记录 ArUco 定位、透视矫正、QR 解码、OMR 三档识别的原始输出；
- 人工比对只作辅助记录；把 PDF、PNG、原始输出、测试报告存 evidence/print/；
- 如果 850×540 的 OMR 精度不达标，停下来在 docs/conflicts.md 新开一条 spike 结论，建议转方案 B；不要在未验证的情况下改 specs 或声称通过。
```

### 6.3 扫描管线

```text
本轮只做 ScanEngine 适配层：
- 按 scan-pipeline.md 的顺序：ArUco → 透视 → 每题 QR → OMR → OCR → PendingFeedback；
- QR 校验失败/归属失败不进入确认页；
- OCR 只写 detectedRawOcr，不写 ReviewFeedback；
- 先写 QR 载荷/CRC/归属校验的单测，再写图像管线。
```

### 6.4 确认页

```text
本轮只做 Confirm：
- 逐项标记 ACCEPT/CORRECTED/NOT_REVIEWED/SUPPLEMENTED，禁止一键全部接受；
- NOT_REVIEWED 只写 ConfirmationDecisionLog；
- 先写状态机测试，再写 Compose；实体机跑一遍并留截图。
```

### 6.5 R-01 收尾

```text
只做验收：
1) 5 张不同光照/角度的真实打印稿跑完整管线；
2) 证据存 evidence/scan/；
3) 断言 OMR 分组与人工核对一致（允许 <=1 组误判）；
4) 如果失败，先修几何/参数，不回退 D-07；仍失败就写 conflicts；
5) 没有真实图像证据，不得在 agent-log 写“通过”。
```

---

## 8. 辅助 Prompts

### A. 规格审计员（只读）

```text
只读审计，不要改代码、不要改 specs：
读 docs/specs/<本轮规格>.md 与对应实现/测试，逐条列出：
1) 规格要求但代码没有的；
2) 代码做了但规格没定义的；
3) 测试断言与规格文字不一致的；
4) 需要人类裁决的。
只把 2/3/4 写进 docs/conflicts.md 的待裁决区；不要替人类做决定。
```

### B. 代码审查

```text
只审查本轮 diff，不写新功能：
- 检查是否违反 R-01～R-06、D-01～D-13；
- 检查 core 模块是否出现 android.*/androidx.*；
- 检查测试是否只覆盖“ happy path”；
- 检查是否用 mock 冒充识别证据；
- 输出“必须修 / 建议修 / 可不改”三档，并给出 file:line。
```

### C. 冲突记录

```text
把本轮发现的规格冲突写成 docs/conflicts.md 条目：
- 关联决策/规格、现象、影响、可选方案、建议、裁决前禁止事项；
- 不修改 specs；不把任一方案写成既定事实；
- 编号顺延，保留已有条目。
```

### D. 实体机验证（无 android-emu-agent）

```text
用实体机 + adb 做 observe-act-verify：
- 先 adb devices -l；
- 安装/启动/截图/UI dump/点击；
- 证据写 evidence/ui/ 与 evidence/screens/；
- 不要使用 android-emu-agent daemon（原生 Windows 不可用）。
如果 adb 无设备，停下来报告“无设备”，不要伪造截图。
```

### E. 每轮收尾

```text
本轮收尾，不写新功能：
1) 跑 ./gradlew <目标模块>:test 与 ./gradlew verifyCorePurity；
2) 更新 docs/changelog.md（文件、意图、关联决策）；
3) 更新 docs/agent-log/YYYY-MM-DD.md（目标、改动、验证命令与结果、未决问题、下轮建议）；
4) 列出下一轮建议的单个用例编号；
5) 不要 git commit，除非我明确要求。
```

### F. 新增依赖

```text
先不要改 build 文件：
- 在 docs/dependency-proposals.md 追加依赖名称、版本、用途、替代方案、体积/ABI/16KB 风险；
- 说明为什么现有依赖不能满足；
- 等我确认后再改 libs.versions.toml 与模块 build.gradle.kts。
```

---

## 9. 固定循环速查

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

> 记住：**没有测试输出的“完成”不算完成；没有真实图像的“识别通过”不算通过。**
