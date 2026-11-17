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

### C-060 整份计划的题目全部放不进单页时是否保留空白首页（已裁决：方案 B）

- **关联决策/规格**：D-07（图例仅文档首页一次）、D-08（按日分页、条目不得拆分）；`docs/specs/pagination.md` 的「分页规则」「放置语义（C-048）」。
- **现象**：单条题目高于一页能放下的高度时，规格要求整条不打印、只作为提示返回；但规格没有写清：当某一天的题目全部过高（或整份计划的题目全部过高）时，打印稿是否还要输出一页只含图例的空白页。
- **对用户的实际影响**：两种做法只在「整份计划没有任何可打印题目」时才有可见差异。用户要么收到一张只有图例、没有题目的空纸（需自行判断当天/当周没有任务），要么打印不出纸张、只在应用里看到「题目过高，请减少附加笔记或拆分题目」的提示。若计划里仍有其它日期的正常题目，两种做法对用户完全一致，首页本来就存在。
- **可选方案**：A 保留一页仅含图例的空白首页；B 过高条目不使计划产生空白页，由计划层以空态或明确提示告知用户。
- **裁决**：2026-11-15 用户选择 **B**。
- **落地**：`DefaultPagePlanner` 移出全部过高条目后，若该日期已无可打印条目则不建页；不生成仅含图例的空白页，也不为过高条目生成占位条目或续下页标记。新增测试锁定：11-23 只有一条 105mm 过高条目、11-24 一条 60mm 正常条目时，`pages` 只含 11-24 一页且它是文档第 1 页（空白页不占页号，首页图例仍扣在第 1 页），11-23 只出现在 `overflowItems`。
- **规格同步**：2026-11-15 获人类授权后回写 `docs/specs/pagination.md`：头部关联裁决加入 C-060；「分页规则」新增“全部条目进入 overflow 的日期不建页、整份计划无可打印条目时返回空 `pages` 并由计划层提示”一条；「放置语义」把“有任意条目时…”改为“有可打印条目时…”，并补充全溢出日期不建页。
- **后续（计划层）**：「整份计划没有可打印题目」的空态或明确失败提示由 `feature/planning` 实现，不在分页器内。
- **测试**：`C-060 day whose items all overflow produces no blank page`。
- **裁决前禁止事项（已解除）**：不再适用；不得在分页器内生成空白页或占位条目，也不得静默丢弃 overflow 提示。

### C-061 附加笔记超限与整题过高同时成立时，提示归为哪一种（已裁决：方案 B）

- **关联决策/规格**：D-07、D-08；`docs/specs/pagination.md` 的「单题高度公式」段、用例 P-05/P-11/P-14/P-18；C-027。
- **现象**：一道题可能同时满足两个“不打印”的条件——附加笔记面积超过模板允许的 30%，且整题高度超过一页的可用内容高度。规格分别给出了两种情形与各自的用户提示（“减少附加笔记”或“拆分题目”），但没有规定两者同时成立时，打印计划页上显示哪一种原因。
- **对用户的实际影响**：两种情形下该题都不会出现在本周打印稿上，用户能看到的差异只有提示原因与建议：归为附加笔记超限时，提示会引导用户删减笔记；归为整题过高时，提示会引导用户拆分题目。两个建议都成立，但当题目同时过高且笔记过多时，只有一条提示会出现在计划页，用户可能先改错方向、再打印一次才发现仍放不下。
- **可选方案**：A 先看附加笔记是否超限，超限即提示“减少附加笔记”；B 先看整题高度，过高即提示“拆分题目”；C 两者同时成立时给出合并提示，需补规格文案。
- **裁决**：2026-11-15 用户选择 **B**。
- **落地**：`DefaultPagePlanner.overflowReasonOf` 改为先判整题高度（> `contentHeightMm` → `ITEM_TOO_TALL`），再判附加面积（> `usableHeightMm × 30%` → `ATTACHMENT_OVER_LIMIT`）；同时成立时归为 `ITEM_TOO_TALL`。两个条件各自独立成立时的行为不变（P-05、P-11、P-14、P-18 仍绿）。
- **文案更新（2026-11-17，C-066）**：原因归类仍按本条；提示文案不再建议“拆分题目”，改为减少关联知识点或缩短知识点描述。
- **测试**：新增 `C-061 item both too tall and over attachment limit is reported as ITEM_TOO_TALL`（自定义短页模板、每条附件 15mm：3 条附件 45mm 超限且整题 105mm 过高，断言归为 `ITEM_TOO_TALL`，`heightMm` 保持公式原值，该日不建页）。
- **规格同步**：2026-11-15 获人类授权后回写 `docs/specs/pagination.md`：头部关联裁决加入 C-061；「单题高度公式」段新增「两条同时成立时按 `ITEM_TOO_TALL` 处理（C-061 方案 B）：只输出一条 overflow 记录，附加上限只在整题高度未超时判定」。
- **裁决前禁止事项（已解除）**：不再适用；不得把任一独立判定改成淘汰以外的行为，也不得把两条规则都成立的条目同时输出两条 overflow 记录。

### C-062 不打印笔记书写区的题目，是否仍受附加笔记上限约束（已裁决：方案 B）

- **关联决策/规格**：D-07、D-08；`docs/specs/pagination.md` 的「单题高度公式」段、用例 P-11/P-15；C-027。
- **现象**：规格规定附加笔记面积超过模板允许的 30% 时整题不打印，但没有说明“题目不打印右下笔记书写区”时这条上限是否仍要判定。此时笔记区与附加笔记都不会出现在纸上，附加面积也确实不计入整题高度。
- **对用户的实际影响**：若仍判定，一道本来能正常放下、纸上也不会出现任何笔记内容的题，可能因为“附加笔记太多”被整题移出本周打印稿；极端情况下该天不再产生页面，用户少做几道题，而提示指向一个纸上根本不存在的区域。若不再判定，这类题照常打印（高度会矮很多），用户能看到题，但附加笔记内容同样不会出现。
- **可选方案**：A 始终判定；B 只有打印笔记区时才判定，无笔记区时忽略附加数量；C 把“无笔记区却带附件”视为非法输入并拒绝，需规格先定义校验位置。
- **裁决**：2026-11-15 用户选择 **B**。
- **落地**：`DefaultPagePlanner.overflowReasonOf` 的附加上限分支增加 `question.hasNoteArea` 前置；`hasNoteArea=false` 时只保留整题高度判定，附加数量既不参与高度（原公式已如此）也不参与上限判定，且不被静默清零。打印笔记区时的行为不变（P-05/P-11 仍绿）。
- **测试**：新增 `C-062 attachment limit applies only when the note area is printed`：同一模板（usable=100、上限 30、每条附件 15.5、2 条 = 31mm）下，打印笔记区的题目 81mm 因附件超限移出；不打印笔记区的题目 40mm 正常打印，该日建页且为文档第 1 页。
- **规格同步**：2026-11-15 获人类授权后回写 `docs/specs/pagination.md`：头部关联裁决加入 C-062；「单题高度公式」段把附加上限改为「仅在 `hasNoteArea=true` 时判定」，并注明 `hasNoteArea=false` 时附加数量不参与上限判定、也不得被静默清零。
- **裁决前禁止事项（已解除）**：不再适用；不得把附加数量静默清零，也不得改变 `hasNoteArea=false` 时 `rightBottom=0` 的公式。

### pagination 收尾审计裁决 C-063～C-069（2026-11-15 / 2026-11-17）

#### C-063 级联移页的“续下页”标记（已裁决：删去标签，改为每日总题数）

- **关联决策/规格**：D-08；`docs/specs/pagination.md`、`docs/specs/print-template.md`、`docs/specs/planning.md`。
- **现象**：一个日期跨多页时，同一道题会在多个来源页各出现一次“续下页”提示；规格对“逐页列出”与“每题一个”两种读法都成立。
- **对用户的实际影响**：旧做法会重复提示、占用留白；改成每日总题数后，学生每天只看一个数字，不再纠结某道题在第几页被提示。
- **裁决**：删去“续下页”标签；每个日期的第一页在日期大标题下方标注“本日共需复习 X 题”。X = 该日期实际打印的题目数（`PlannedPage.items` 总数），不含 overflow 题目（2026-11-17 人类确认）。
- **几何裁决（2026-11-17）**：每日总题数字体大小为日期标题字体的一半，高度也为一半。
- **落地**：`ContinuationMarker`/`PlannedPage.continuationMarkers` 保留为内部移页元数据，KDoc 注明不再渲染；打印规则写入 `print-template.md`；D-08、`overview.md`、`planning.md`、`prompts.md` 6.1 同步。`TemplateGeometry` 新增 `dateHeadingFontMm=5.0`、`dateHeadingHeightMm=7.0`，派生 `dailyTotalFontMm=2.5`、`dailyTotalHeightMm=3.5`、`dateHeadingBlockHeightMm=10.5`；两者绘制在默认 12mm 上边距内，不占用内容区高度。若打印层改到内容区，必须从该日期首页容量扣除该高度并重跑 P-01～P-20。
- **测试**：核心测试继续锁定 marker 元数据与被移条目一一对应（P-03/P-08/P-09/P-13/P-18）；`TemplateGeometryTest` 锁定 5/7mm、2.5/3.5mm、合计 10.5mm ≤ 12mm 上边距。渲染验收留待 6.1 的真实 PDF/回读。
- **规格同步**：已回写 `pagination.md`（marker 仅内部元数据、日期标题几何参数与派生量）、`print-template.md`（D-08、页面几何、移除“续下页”）、`planning.md`（marker 内部元数据）。
- **裁决前禁止事项（已解除）**：不得再在纸上绘制“续下页”；不得删除内部 marker 的一一对应断言；X 不得计入 overflow 题目。

#### C-064 `KNOWLEDGE_TEXT_OVERFLOW` 的产出与合并（已裁决：方案 A）

- **关联决策/规格**：D-07；`docs/specs/print-template.md`、`docs/specs/pagination.md`、`docs/specs/planning.md`、`docs/specs/app.md`。
- **现象**：打印规格要求名称超长时产生该警告，但分页器不接收文本测量结果，计划层也未定义如何合并。
- **对用户的实际影响**：长名称被降字号或省略时，学生收不到“名称过长”的解释，只看到截断结果。
- **裁决**：方案 A——计划层产出该警告，与分页警告合并；分页器不猜文本换行、不做字体测量。
- **落地**：`planning.md` 写明合并规则：按 `questionId + code` 去重，按 `questionId` 字典序、同题按 `code` 枚举序排序；`pagination.md` 注明该代码由计划层产出；`print-template.md`、`app.md` 同步。计划层代码尚未实现（`feature/planning` 无业务 Kotlin），落地待 5.2。
- **测试**：待计划层实现；本轮 pagination 测试不涉及。
- **规格同步**：已回写 `pagination.md`、`planning.md`、`print-template.md`、`app.md`。
- **裁决前禁止事项（已解除）**：不得在分页器内实现文本测量或猜换行；不得新增未定义的警告种类；不得删除打印规格的 4 行上限、降字号与省略号规则。

#### C-065 警告代码与文案的对外契约（已裁决：方案 B）

- **关联决策/规格**：`docs/specs/pagination.md`、`docs/specs/app.md`；C-047、C-064。
- **现象**：警告代码直接使用内部原因名，规格未定义代码集合、粒度与展示方式；文案未说明题号/日期。
- **对用户的实际影响**：界面若直接展示代码，学生看到英文内部标识；若多题溢出，提示相似且难以定位。
- **裁决**：方案 B——新增稳定代码枚举；保留每题一条（同一题同一代码至多一条；不同代码可并存，例如整题过高与文字溢出）；代码只给程序/日志，文案给用户看，界面映射中文标签。
- **落地**：`PaginationWarning.code` 改为 `PaginationWarningCode` 枚举（`ITEM_TOO_TALL`、`ATTACHMENT_OVER_LIMIT`，并为 C-064 预留 `KNOWLEDGE_TEXT_OVERFLOW`）；`DefaultPagePlanner` 只产出前两类；`app.md` 规定 PlanPreview 只展示中文标签与 `message`。
- **测试**：P-14/P-05/P-11/C-061 改为断言枚举；P-09 属性测试断言 `warning.code.name` 与 overflow 原因一致；`./gradlew :core:pagination:test` 127 次执行全绿。
- **规格同步**：已回写 `pagination.md`（类型与角色）、`app.md`（展示规则）。
- **裁决前禁止事项（已解除）**：界面不得直接展示枚举名；不得把代码改成中文字符串；不得增删警告种类而不补规格与测试。

#### C-066 整题过高的提示是否建议“拆分题目”（已裁决：不拆分，改为减少知识点/缩短描述）

- **关联决策/规格**：C-061、C-066；`docs/specs/pagination.md` 的单题高度公式与警告、`docs/specs/print-template.md`、`docs/prompts.md` 6.1。
- **现象**：旧提示先建议“减少附加笔记”，再建议“拆分题目”；C-061 的双条件情形也引导拆分。对于没有附加笔记的题，提示让人去减不存在的附加笔记；人类认为“拆分题目”这个设计本身不合理。
- **对用户的实际影响**：学生按错误方向改数据、重新打印，才发现题目仍然过高，浪费一次打印。
- **裁决（2026-11-17）**：不再建议拆分题目。整题过高提示“减少本题关联的知识点，或缩短知识点描述（减少右上名称行数）”；附加超限提示“减少附加笔记”。原因归类仍按 C-061（两条件同时成立归 `ITEM_TOO_TALL`）。
- **落地**：`DefaultPagePlanner.overflowMessage` 更新两段文案，删除“拆分题目”；`overflowReasonOf` 的判定顺序不变（C-061）。
- **测试**：P-14、C-061 断言提示包含“减少本题关联的知识点”与“缩短知识点描述”，且不含“拆分题目”；P-05 断言附加超限文案不含“拆分题目”且含“减少附加笔记”。用旧文案临时变异时 P-14/C-061/P-05 三条同时失败（3/3），随后逐字还原。
- **规格同步**：已回写 `pagination.md`（两处提示文案）、`print-template.md`（关联裁决）、`prompts.md` 6.1；`conflicts.md` 移除本条。
- **裁决前禁止事项（已解除）**：不得再出现“拆分题目”文案；不得改变 C-061 的原因归类；不得在分页器内自动拆分、压缩或删减附件。

#### C-067 右上名称与左侧可打印集合的一致性（已裁决：方案 B）

- **关联决策/规格**：D-07；`docs/specs/pagination.md`、`docs/specs/print-template.md`、`docs/specs/planning.md`、`docs/specs/repository.md`。
- **现象**：左侧高度按知识点数量、右上名称按名称列表、换行行数按第三个字段；分页器不校验三者一致，规格也未明说名称列表属于哪一集合。
- **对用户的实际影响**：上游数据错位时，纸上可能出现左侧填涂行与右上名称数量对不上、题块重叠或明显留白。
- **裁决**：方案 B——由计划/数据层保证名称列表与左侧同一 printable 集合、按 `number` 升序、数量等于 `knowledgeCount`；分页器只消费、不校验、不补齐/截断。
- **落地**：`repository.md` 的 `toRenderableQuestion` 映射要求、`planning.md` 的构建要求、`pagination.md` 的字段注释、`prompts.md` 3.2/5.2 同步；分页器代码不变。
- **测试**：待 `data:room` 映射测试（ModelMappingTest 增加 `knowledgeNames.size == knowledgeCount` 断言）。
- **规格同步**：已回写 `pagination.md`、`planning.md`、`repository.md`、`print-template.md`。
- **裁决前禁止事项（已解除）**：不得静默截断/补齐名称；不得在分页器内按名称重算知识点数量；不得改变单题高度公式。

#### C-068 模板/计数/空输入的报错顺序（已裁决：方案 A）

- **关联决策/规格**：`docs/specs/pagination.md`；C-047、P-20。
- **现象**：规格分别定义模板失败、计数异常与空输入返回，但没有写同时出现时的优先级。
- **对用户的实际影响**：低影响；主要是坏数据/开发期路径，用户可能看到“模板参数错误”而不是数据问题。
- **裁决**：方案 A——模板几何 → 输入计数 → 空态/分页；模板非法时即使计数越界或没有题目，也返回 `Failure(INVALID_TEMPLATE_GEOMETRY)`。
- **落地**：实现原本已是该顺序；在 `acceptance invalid template...` 增加“模板+计数同时非法”与“模板非法+空输入”两条断言。
- **测试**：`acceptance invalid template returns INVALID_TEMPLATE_GEOMETRY failure` 扩展后全绿。
- **规格同步**：已回写 `pagination.md` 的校验顺序。
- **裁决前禁止事项（已解除）**：不得用 Failure 与异常两种方式表达同一错误；不得静默忽略非法计数；模板非法时不得生成页面。

#### C-069 计划编号的前置条件（已裁决：方案 A）

- **关联决策/规格**：D-09；`docs/specs/pagination.md`、`docs/specs/planning.md`、`docs/specs/scan-pipeline.md`。
- **现象**：分页规格只写页码格式，未声明计划编号的格式/唯一性；分页器直接拼接。
- **对用户的实际影响**：正常路径无差异；坏计划编号可能导致页码/QR 无法唯一回读、确认页归属失败或反馈记到别的计划。
- **裁决**：方案 A——计划层保证 `planId` 为 6 位 Crockford Base32 且全局唯一；分页器只拼接、不校验、不改写。
- **落地**：`pagination.md` 补前置条件，`planning.md` 标注该前置条件，D-09 同步；代码不新增校验。
- **测试**：不新增（“不校验”即裁决行为；正常格式由 5.2 的计划层测试覆盖）。
- **规格同步**：已回写 `pagination.md`、`planning.md`、`CLAUDE.md` D-09。
- **裁决前禁止事项（已解除）**：不得截断/改写/重新生成计划编号；不得改变“计划编号-两位页号”格式；不得在分页器内生成随机替代编号。

### C-070 打印行快照携带题目标识（已裁决：方案 A，2026-11-17）

- **关联决策/规格**：C-046 方案 A、C-015；`docs/specs/repository.md`、`docs/specs/data-model.md`。
- **现象**：`data-model.md` 的 `knowledgeRows` 示例中，每条题目记录都带该题的标识；`repository.md` 的记录结构字段只列了行列表与触发知识点列表。按后者实现，解码后再编码会丢题目标识，回读无法把纸面行组对回具体题目。
- **对用户的实际影响**：拍照回读后，确认页可能无法把填涂行归属到正确题目，部分题目被标成「本次未复习」或无法逐项确认；打印稿本身不受影响。
- **裁决**：2026-11-17 用户选择 **A**——记录结构补上题目标识，按 `data-model.md` 的示例读写；并授权回写 `repository.md`。
- **落地**：`core:model` 的 `PlanPageRows` 增加 `questionId`，`KnowledgeRowsCodec` 对每条记录读写该字段；`rows` 与 `triggeredKnowledgePointIds` 顺序原样保留，不按 `number` 重排（C-015）。代码在裁决前已按 A 实现，本轮只同步规格与注解。
- **测试**：`KnowledgeRowsCodecTest` 用 `data-model.md` 示例做 decode→encode 往返，断言 rows 数量/顺序、`triggeredKnowledgePointIds` 原样、`[5,3]` 不重排；`./gradlew :core:model:test --tests "*KnowledgeRowsCodecTest"` 1/1 全绿。
- **规格同步**：已获授权回写 `repository.md` 的 `PlanPageRows` 字段与行快照说明；`data-model.md` 的示例无需修改（原本即含题目标识）。
- **裁决前禁止事项（已解除）**：不得丢弃题目标识；不得按知识点编号重排行快照；不得为消除差异改动 C-015。

## 已裁决但待验证（非新冲突）

- C-050 的 `Double` 内部算术与 `Float` 边界转换，需在 `ApplyFeedbackUseCase` 实现后用 U-01～U-19 验证。
- C-051 方案 A 的 R-01 spike 结果待验证：850×540 → 2480×3508 适配后的 OMR/QR 精度必须用真实打印稿判断；不达标时按 `scan-pipeline.md` 转方案 B（fork）。这不改变已裁决的 A 方案，只记录验证门禁。

## 历史引用

- C-001～C-005 的原始裁决记录不在仓库；ADR 已删除，历史裁决仅存于 `docs/agent-log/2026-11-11.md` 与 Blurt H-0001。
