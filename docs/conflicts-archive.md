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

### C-071 行快照映射的签名与数据来源不一致（已裁决：方案 A，2026-11-17）

- **关联决策/规格**：C-046 方案 A、C-015、C-070；`docs/specs/repository.md` 的 `core/model 与 data/room 映射` 段、`docs/specs/data-model.md` 的 `PlanPage.knowledgeRows`。
- **现象**：映射规格把行快照转换写成「分页阶段的打印页 → 单条行快照记录」；但分页阶段的页面对象只含题目、位置与高度，不含行快照，且一页通常有多道题。真正冻结行集合与行序的是打印页实体上持久化的那段快照 JSON。
- **对用户的实际影响**：若按规格字面从当前题目关联关系重新生成行，旧打印稿回读时纸面填涂行可能对不回它当初对应的知识点或题目，确认页会出现错行、漏行或被误标为「本次未复习」；若只返回单条记录，一页多题时无法逐题确认。打印稿本身不变。
- **可选方案**：A. 打印页实体上的冻结快照 JSON → 每题一条记录，只解码、不重排；B. 让分页阶段的页面对象携带行快照；C. 保持单条记录签名，由调用方按题拆分。
- **裁决**：2026-11-17 用户选择 **A**。
- **落地**：`data/room` 的 `ModelMapping.kt` 提供 `PlanPage.toPlanPageRows(): List<PlanPageRows>`，实现为 `KnowledgeRowsCodec.decode(knowledgeRows)`；只解码冻结快照，不从当前关联重新生成、不按 `number` 重排、不丢弃或合并一页内的多条题目记录。`ModelMappingTest` 锁定 `questionId` 与 rows 顺序保真（5/5 全绿）。
- **测试**：`./gradlew :data:room:test --tests "*ModelMappingTest" --rerun-tasks` → `tests=5 failures=0 errors=0 skipped=0`；`./gradlew :data:room:test` 与 `./gradlew verifyCorePurity` 均 BUILD SUCCESSFUL。
- **规格同步**：2026-11-17 人类授权后已回写。`docs/specs/repository.md` 的关联裁决、小节标题、映射签名、行快照说明、默认值处理与验收段：映射签名改为 `PlanPage.toPlanPageRows(): List<PlanPageRows>`，补「只解码冻结 JSON、不从当前关联重生成、不重排、不合并/丢弃」及对应验收；`docs/specs/data-model.md` 的关联裁决、`knowledgeRows` 字段注释、映射说明与快照说明段同步。`CLAUDE.md` 的 D-01～D-13/红线/模块约束无需改决策；`overview.md` 多形态表已按 C-071 同步。旧稿无快照的回退条款由 C-072 方案 A 裁决并同步。
- **裁决前禁止事项（已解除）**：不得从当前关联关系重新计算旧稿的行；不得按知识点编号重排快照；不得丢弃或合并一页内的多条题目记录；不得改动 C-015/C-070。

### C-072 旧打印稿没有行快照时是否回退重算（已裁决：方案 A，2026-11-17）

- **关联决策/规格**：C-071 方案 A、C-015、C-009；`docs/specs/data-model.md` 的迁移条目、`docs/specs/print-template.md` 的打印职责、`docs/specs/scan-pipeline.md` 的 OMR 行组重建与验收、`docs/specs/confirm-flow.md` 的归属校验。
- **现象**：C-071 方案 A 要求回读只认打印页实体上冻结的行快照；但迁移条目、打印职责与扫描行组重建三处仍写着「旧稿没有快照时，按当前题目关联 + 知识点编号升序重算并记录警告」。迁移会把旧打印页的行快照默认成空数组，因此升级前的旧稿正好落入这条回退路径。
- **对用户的实际影响**：
  - 保留回退：升级前的旧打印稿仍能拍照回读，但若打印后改过题目关联，纸面上的填涂行与按当前关联重算的行可能错位，确认页会出现错行、漏行或被误标为「本次未复习」，且用户可能未察觉就提交了错误反馈。
  - 禁止回退：旧稿无法回读，用户必须重新生成计划并重新打印，那一周的纸面反馈不能录入；对还没有真实旧稿的新安装用户没有影响。
- **可选方案**：A. 严格按 C-071 方案 A——无快照即视为不可回读，提示重新生成计划并按新稿打印；B. 只对升级前的旧稿开一次性回退，并在确认页要求用户显式确认「按当前关联重建行」；C. 保持现状——任何无快照旧稿都重算，仅写日志警告。
- **裁决**：2026-11-17 用户选择 **A**。
- **落地**：回读/确认路径只认 `PlanPage.knowledgeRows` 的冻结快照；快照为空或不可解析时视为无快照，阻断回读与提交，提示重新生成计划并按新稿打印，不创建 `PendingFeedback`、不按当前关联重算。规格已回写：`data-model.md` 迁移条目、`print-template.md` 打印职责、`scan-pipeline.md` 行组重建与验收、`confirm-flow.md` 归属校验、`repository.md` 的默认值处理。
- **测试**：扫描/确认实现尚未落地（阶段 4/6），本轮无对应自动化用例；已落地部分沿用 `./gradlew :data:room:test verifyCorePurity` 全绿。后续实现必须补「无快照旧稿阻断且不重算」的用例。
- **规格同步**：已获授权回写上述五处；`CLAUDE.md` 的 D-01～D-13/红线/模块约束无需修改；`overview.md` 生命周期表补充「无快照旧稿不可回读」注记。
- **裁决前禁止事项（已解除）**：不得在回读/确认路径实现按当前关联重算；不得把无快照旧稿当作有快照处理；不得改动 C-015/C-071。

### C-073 打印题目投影类型的模块归属（已裁决：方案 A，2026-11-17）

- **关联决策/规格**：C-046 方案 A、C-053 方案 A；`docs/specs/repository.md` 的「core/model 与 data/room 映射」段、`docs/specs/pagination.md` 的接口段、`docs/architecture/overview.md` 的多形态表。
- **现象**：同一份规格把“打印用的题目投影”（`RenderableQuestion`）同时写成“放在核心模型模块”和“由分页模块定义/使用”。当前实现把它放在分页模块，阶段 3.3 的纵切测试也只能从分页模块引用；若后续实现者按另一份文字去核心模型模块找它，会找不到，或在两处各建一份同名类型。
- **对用户的实际影响**：没有用户可见差异——打印出的题目、页码、每日行数与顺序都不变；影响只在内部模块边界和后续实现者的引用路径。只有在因此产生两份同名类型、且其中一份字段漂移时，才可能间接影响打印内容（例如题号或知识点名称数量不一致）。
- **可选方案**：
  - A：以分页规格与现有实现为准，该类型留在分页模块；只把映射规格里的归属文字校正为分页模块。
  - B：把该类型迁到核心模型模块，分页模块改为依赖核心模型模块；所有引用、测试与打印/回读路径同步。
- **选型参考（以用户影响为依据）**：两种方案对用户看到的计划、打印稿与回读没有任何差异；A 不需要重新验收打印/回读，也不会产生第二份同名类型，对现有用户流程的风险更小；若将来希望所有纯数据类型集中在同一模块，再评估 B。
- **裁决**：2026-11-17 用户选择 **A**。
- **落地**：`RenderableQuestion` 保持在 `core:pagination`；`core:model` 不新增同名 DTO；`data:room` 继续通过 `core:pagination` 依赖构造打印投影；阶段 3.3 的 `FirstVerticalSliceTest` 从 `com.recollect.pagination` 引用。无代码/行为变更。
- **测试**：`./gradlew :data:room:test --tests "*FirstVerticalSliceTest" --rerun-tasks` → `tests=1 failures=0 errors=0 skipped=0`；`./gradlew :core:model:test :core:scheduler:test :core:pagination:test :data:room:test --rerun-tasks` → 262 次执行、0 失败；`./gradlew verifyCorePurity` BUILD SUCCESSFUL。
- **规格同步**：人类选择方案 A 即授权校正归属文字，已回写 `docs/specs/repository.md` 的关联裁决、小节标题、映射段说明与代码块（`RenderableQuestion` 移出 `// core:model`，标注 `// core:pagination（C-073 方案 A）`）；`docs/specs/pagination.md` 无需修改（原本就在分页接口段定义该类型）。`CLAUDE.md` 模块约束与 `overview.md` 多形态表补注。
- **裁决前禁止事项（已解除）**：不得同时在两个模块定义同名类型；不得为此改动已通过的分页/调度行为或测试断言；不得擅自修改 `docs/specs/`（已获授权，范围仅限归属文字）。

### C-074 Repository 接口签名缺项（已裁决：方案 A，2026-11-17）

- **关联决策/规格**：D-01、D-13、R-04；C-047、C-055、C-071、C-072；`docs/specs/repository.md` 的 Repository 接口与事务规则、`docs/specs/planning.md` 的 `PlanDraft`/`PlanGenerationException`、`docs/specs/feedback-usecase.md` 的确认提交调用。
- **现象**：按规格签名实现数据层时，草稿/提交输入缺少完成事务所必需的信息：打印时冻结的每题行快照、更新后的知识点状态、题目实际复习日、用户显式保存的笔记附件；也没有返回「关联失效被整题跳过」的可见记录。计划草稿与计划失败异常在规格中归属编排层，而数据层不能反向依赖编排层。
- **对用户的实际影响**：不补齐时，旧打印稿的行组可能在重新生成计划后被改写（回读错行）、确认提交后复习日/冷却判断不更新、关联失效的题目无声消失；补齐后这些用户可见结果保持不变，只是由数据层接收已算好的输入并返回跳过清单。
- **可选方案**：A. 数据层的草稿/提交输入显式补齐上述字段，提交结果返回跳过清单，编排层与确认页只构造入参与展示；B. 维持原签名，由编排层另建「事务载荷」类型传入数据层；C. 数据层在写库时自行推导行快照与状态（与 C-071/C-072、R-04 冲突）。
- **选型参考（以用户影响为依据）**：A 与 B 对用户影响相同——旧稿行组稳定、复习日与冷却及时更新、被跳过的题目对用户可见；差别只在代码归属。C 会让旧稿回读错行或确认后状态不对，应排除。
- **裁决**：2026-11-17 用户选择 **A**。
- **落地**：
  - `data/room/.../repository/PlanDraft.kt`：计划草稿增加每题冻结行快照 `knowledgeRowsByQuestionId`；`PlanGenerationException` 与 `fromPagination` 放在 `data:room`，分页失败在进入事务前抛出（C-047）。
  - `data/room/.../repository/PlanRepository.kt`：`savePlan(draft)` 按草稿写 `PlanPage`，不从当前题目-知识点关联重算。
  - `data/room/.../repository/FeedbackRepository.kt`：`saveDecisions(sessionId, decisions, updatedKnowledgeStates, questionLastReviewedDates, noteAttachments): SaveDecisionsResult`；状态由 UseCase 计算并按 C-050 转 `Float`，Repository 只落库；返回 C-055 的整题跳过清单。
  - 编排层（`feature/planning`）与确认页后续只负责构造入参、展示跳过清单；不得在 Repository 内补推导逻辑。
- **测试**：`./gradlew :data:room:test` → debug/release 各 13 个测试、0 失败；`PlanRepositoryTransactionTest` 与 `FeedbackRepositoryTransactionTest` 覆盖成功落库、任一步失败整体回滚、C-047 事务前失败、C-055 整题跳过。`./gradlew verifyCorePurity` BUILD SUCCESSFUL。
- **规格同步**：2026-11-17 人类授权后已回写。`docs/specs/repository.md`：关联裁决补 C-074；`saveDecisions` 签名补 `updatedKnowledgeStates`/`questionLastReviewedDates`/`noteAttachments` 与返回值 `SaveDecisionsResult`；新增 `PlanDraft`（含 `knowledgeRowsByQuestionId`）、`PlanGenerationException`、`NoteAttachmentDraft`、`SaveDecisionsResult` 的定义与类型归属；事务规则补行快照只编码、状态只落库、跳过清单与待确认行保留；验收补 C-074。`docs/specs/planning.md`：关联裁决补 C-074；`PlanDraft` 增加 `knowledgeRowsByQuestionId` 并标注定义在 `data:room`；`PlanGenerationException` 标注归属；固定流程与验收同步。
- **裁决前禁止事项（已解除）**：不得按当前关联重算旧稿行快照；不得在关联失效时补默认知识点或静默丢弃；不得把知识点状态计算搬进 Repository。

### C-075 状态更新用例接口与已落地事务提交入口不一致（已裁决：方案 A，2026-11-17）

- **关联决策/规格**：D-13、R-04；C-074 方案 A；`docs/specs/feedback-usecase.md` 的「接口」与「处理顺序」、`docs/specs/repository.md` 的 `saveDecisions`。
- **现象**：反馈规格给状态更新用例单独注入反馈、决定日志、待确认三个仓储，并返回每条反馈的行号；4.2 已按 C-074 方案 A 落地统一的确认提交事务入口，该入口只返回写入条数与关联失效的跳过清单，不返回行号，提交时间也由数据层事务负责。照规格写会拆成多次写入，或要求数据层额外返回行号。
- **对用户的实际影响**：没有可见差异——确认页仍逐行确认，提交后状态、反馈与决定日志在同一事务落库；用户可见的是「写了多少条」「哪些题因关联失效被跳过」，而不是每条反馈的内部行号。
- **可选方案**：A. 用例结果携带数据层的提交结果（写入条数 + 跳过清单）与更新后的状态，仓储接口沿用 C-074；B. 让数据层提交额外返回每条反馈的行号，恢复规格的结果形状；C. 维持规格接口，用例层分别调用三个仓储并自行拼事务。
- **选型参考（以用户影响为依据）**：A 与 B 对用户影响相同；A 不扩大数据层契约、确认页可直接展示跳过清单，建议 A。C 会让一次提交跨多个事务，失败时可能留下部分写入，用户会看到「有的行记了、有的没记」，应排除。
- **裁决**：2026-11-17 用户选择 **A**。
- **落地**：
  - `data/room/.../usecase/ApplyFeedbackUseCase.kt`：构造函数注入 `KnowledgePointRepository`、`ReviewFeedbackHistoryRepository`、`FeedbackRepository`、`NextDueCalculator`；`ApplyResult` 携带 `SaveDecisionsResult` 与更新后的状态，不返回反馈行 id。
  - `data/room/.../repository/KnowledgePointRepository.kt`：只读端口 `states()` 与 `triggeredCount()`（非触发行不推进冷启动）。
  - 写入 `ReviewFeedback`/`ConfirmationDecisionLog`、更新知识点与题目复习日、删除 `PendingFeedback` 全部由 `saveDecisions` 在同一事务完成；`decidedAt` 由数据层时钟负责。UseCase 只做前置校验与状态计算，不拼事务（C-074 方案 A）。
- **测试**：`./gradlew :data:room:test` → debug/release 各 18 个测试、0 失败；`ApplyFeedbackUseCaseTest` 覆盖 U-01～U-05。`./gradlew verifyCorePurity` BUILD SUCCESSFUL。
- **规格同步**：2026-11-17 人类授权后已回写。`docs/specs/feedback-usecase.md`：关联裁决补 C-075；接口段改为读端口 + 单事务提交入口、`ApplyResult` 携带 `SaveDecisionsResult` 与状态；处理顺序说明步骤 1/4 与状态落库、删除待确认由数据层事务执行，UseCase 负责步骤 3/5 的状态计算，`ScanSession` 状态更新另属确认流程集成。
- **命令路径修正（2026-11-17 授权后完成）**：`feedback-usecase.md` 的「验收门禁」与 `entry.md` 的 `./gradlew :data:test` 均已改为 `./gradlew :data:room:test`（仓库只有 `:data:room` 项目）；`prompts.md` 的同类命令此前已同步修正。
- **裁决前禁止事项（已解除）**：不得为返回行号把同一提交拆成多个事务；不得在数据层补推导知识点状态或默认关联；不得改变 C-055 的整题跳过语义。

### C-076 冷却顺延由谁写入（已裁决：方案 A，2026-11-17）

- **关联决策/规格**：D-04、C-021、C-057；`docs/specs/feedback-usecase.md`「冷却顺延应用」与用例 U-14、`docs/specs/repository.md`「事务规则」、`docs/architecture/overview.md` 字段产权表。
- **现象**：反馈规格写计划生成后由一个独立的顺延用例把「因冷却顺延」的目标日期写回知识点；仓储规格与 4.2 已落地的事务入口则要求计划保存时在同一个事务里完成同样的写入。两份规格对「谁写、在哪个事务里写」说法不同；代码没有独立的顺延用例，U-14 的行为由计划保存事务承载。
- **对用户的实际影响**：最终落库的到期日相同，用户看到的顺延天数、下一份计划是否继续顺延、打印内容都不受影响。差异只在失败边界：同一事务写入时，计划保存失败不会出现「计划没生成、到期日却后移」的半成功状态；若拆成两个写入点，则需要额外的补偿或重试才能避免。
- **可选方案**：A. 维持现状，由计划保存的单事务入口写入顺延（与 `repository.md` 一致）；B. 恢复独立的顺延用例与单独写入入口，把计划保存改为两段提交；C. 不改写入边界，仅在规格中把独立用例描述为计划保存事务内的一步。
- **选型参考（以用户影响为依据）**：A 与 C 不会让用户看到「计划没生成、到期日却变了」的半成功状态，且无需额外补偿；B 需要额外机制才能避免该状态，建议 A 或 C。
- **裁决**：2026-11-17 用户选择 **A**。
- **落地**：`data/room/.../repository/PlanRepository.kt` 的 `savePlan` 已在同一事务内按 `DeferredItem(COOLING).targetNextDueDates` 原样更新知识点到期日；不新增独立顺延用例，不改写天数、不夹取（C-057）。
- **测试**：`PlanRepositoryTransactionTest` 的 U-14 用真实 `DefaultScheduler` 驱动连续两份计划各 +1 天，并覆盖 C-057 的 0=关闭冷却、负数非法；`./gradlew :data:room:test` → debug/release 各 33 个测试、0 失败；`./gradlew verifyCorePurity` BUILD SUCCESSFUL。
- **规格同步**：2026-11-17 人类授权后已回写。`docs/specs/feedback-usecase.md`：关联裁决补 C-076；「为什么在 UseCase」改为不另设顺延用例；「冷却顺延应用」改为 `savePlan` 单事务应用并补失败整体回滚；U-14 断言补事务口径。`docs/specs/scheduler.md`：关联裁决补 C-076，状态更新规则段改为规划层放入 `PlanDraft`、由 `savePlan` 事务应用顺延。`docs/specs/planning.md`：关联裁决补 C-076，职责与固定流程第 5 步标注同一事务。`docs/specs/repository.md`：关联裁决补 C-076，事务规则注明不另设独立 UseCase。`CLAUDE.md` 与 `docs/architecture/overview.md` 同步落点表述。
- **裁决前禁止事项（已解除）**：不得把冷却顺延拆成第二个写事务；不得让计划保存失败时保留顺延结果；不得恢复独立顺延用例并把计划保存改为两段提交。

### C-077 整题被跳过后状态仍落库（已裁决：方案 A，2026-11-17）

- **关联决策/规格**：D-01、D-13；C-055；`repository.md:123`、`feedback-usecase.md` 处理顺序第 5 步。
- **现象/用户影响**：关联损坏的题目被整题跳过时，它贡献的知识点状态仍可能落库，出现“没有复习记录、复习间隔却前进”；修复后重试可能二次计数。
- **裁决**：用户选择 **A**——状态更新用例不把会被整题跳过的题目所贡献的状态并入提交；数据层再按未跳过题目过滤一次。
- **落地**：`ApplyFeedbackUseCase` 先判定“题目不存在/关联知识点不存在”的整题跳过集合，跳过行不计算状态与复习日；`RoomFeedbackRepository.saveDecisions` 只落库仍被未跳过题目引用的知识点状态。
- **测试**：`c077_skipped_question_does_not_update_knowledge_state_and_still_logs`、`state contributed only by a skipped question is not written`；`./gradlew :data:room:test` 全绿。
- **规格同步**：2026-11-17 授权后已回写。`docs/specs/repository.md` 事务规则新增“整题跳过的审计与状态”；`docs/specs/feedback-usecase.md` 处理顺序第 5 步补“整题跳过不写反馈/状态/复习日”。
- **裁决前禁止事项（已解除）**：不得补默认反馈/默认知识点；不得改变 C-055 的整题跳过语义。

### C-078 关联失效的可见结果与跳过行日志（已裁决：方案 A，2026-11-17）

- **关联决策/规格**：D-13、R-04；C-055；`confirm-flow.md:46,51,53`、`repository.md:123`、`feedback-usecase.md:144`、`data-model.md:203`。
- **现象/用户影响**：规格一边要求阻断整页，一边要求整题跳过并给清单；跳过行是否写确认日志未定义。用户可能看到整批失败或部分跳过，且事后无法追溯跳过行当时的选择。
- **裁决**：用户选择 **A**——整题跳过、返回可见清单、跳过行也写确认日志。
- **落地**：`saveDecisions` 先为所有决策写 `ConfirmationDecisionLog`，再判定整题跳过；跳过题不写反馈/状态/复习日，待确认行保留；会话在有跳过时维持 READY 供修复后重试。
- **测试**：`c078_missing_knowledge_point_skips_whole_question_without_throwing`、`unresolved knowledge id skips the whole question...`（断言跳过行日志存在、写入条数为 2）。
- **规格同步**：2026-11-17 授权后已回写。`docs/specs/confirm-flow.md` 新增“数据层兜底/部分成功”；`docs/specs/repository.md` 事务规则与 `SaveDecisionsResult` 注明跳过行日志；`docs/specs/feedback-usecase.md` 处理顺序第 1/6 步；`docs/specs/data-model.md` 的 `ConfirmationDecisionLog` 与 `ScanSession` 同步。
- **裁决前禁止事项（已解除）**：不得一边声称跳过一边静默写入部分反馈；跳过清单必须可展示。

### C-079 提交前合法性校验的层级（已裁决：方案 A，2026-11-17）

- **关联决策/规格**：D-13、R-04；C-075；`feedback-usecase.md:127-136`、`confirm-flow.md:34,38,53,120`、`repository.md:121`。
- **现象/用户影响**：会话完整性、行归属、计划日、实际复习日上下界没有一层能完整校验；未来日期一旦落库，题目会长期处于冷却。错误类型也不统一。
- **裁决**：用户选择 **A**——UseCase 注入时钟、待确认读取、题目关联读取与计划周起始端口，完成全部前置校验并统一抛 `InvalidConfirmationException`；确认页保留按钮禁用的双保险。
- **落地**：`ApplyFeedbackUseCase` 新增 `QuestionLookupRepository`、`PlanWeekStartProvider`、`Clock` 依赖；校验非空/无未标记/无重复/同计划/会话 READY/未提交/行集合完整/计划日与触发标记一致/知识点属于题目/复习日上下界/档位可用。`RoomFeedbackRepository` 的防御校验也改为抛 `InvalidConfirmationException`。
- **测试**：`c079_*` 九个负例（空、重复、未来日期、缺复习日/缺档位、计划日/触发标记不一致、行集合不完整、知识点不属该题）。
- **规格同步**：2026-11-17 授权后已回写。`docs/specs/feedback-usecase.md` 前置条件写明 UseCase 承担校验、注入时钟/待确认/题目关联/周起始读取，C-075 注记修订时钟部分；`docs/specs/repository.md` 事务规则补前置校验归属。
- **裁决前禁止事项（已解除）**：不得硬编码今天/时区；不得在数据层静默修正非法日期。

### C-080 同一会话重复提交（已裁决：方案 A，2026-11-17）

- **关联决策/规格**：R-04；`feedback-usecase.md:177`、`confirm-flow.md:127`。
- **现象/用户影响**：重复提交会重复写反馈并再次推进计数/到期日，或从已删除的待确认行得到空列表报错；用户可能把一次复习算两遍。
- **裁决**：用户选择 **A**——提交入口幂等，会话已提交则拒绝并提示“已提交”，不写任何行。
- **落地**：`FeedbackRepository` 增加 `session(sessionId)` 只读投影；UseCase 在 `SUBMITTED` 时抛 `InvalidConfirmationException`，非 `READY` 也拒绝；`saveDecisions` 在整批无跳过时于同一事务调用 `ScanSessionDao.markSubmitted`。有跳过时保持 READY，保留待确认行供修复后重试。
- **测试**：`c080_repeated_submission_of_a_submitted_session_is_rejected_without_writes`。
- **规格同步**：2026-11-17 授权后已回写。`docs/specs/feedback-usecase.md` 前置条件与处理顺序第 6 步、`docs/specs/confirm-flow.md` 批量回读/禁止/验收、`docs/specs/data-model.md` 的 `ScanSession`、`docs/specs/repository.md` 事务规则均已同步重复提交与部分成功语义。
- **裁决前禁止事项（已解除）**：不得靠删除待确认行隐式实现幂等；不得静默追加反馈。

### C-081 计划周起始日的口径（已裁决：方案 A，2026-11-17）

- **关联决策/规格**：D-13；`feedback-usecase.md:136`、`settings.md:21,31,39`。
- **现象/用户影响**：周起始日可配置；硬编码周一会让改过设置的用户被错误拒绝或放行，进而改变下次复习日期与冷却判断。
- **裁决**：用户选择 **A**——按计划日所在周 + 用户设置的周起始日计算下界。
- **落地**：新增 `PlanWeekStartProvider`（默认周一），UseCase 用 `plannedDate` 与起始日计算周起点。
- **测试**：`c081_review_before_plan_week_start_is_rejected`、`c081_plan_week_start_follows_the_configured_day`。
- **规格同步**：2026-11-17 授权后已回写。`docs/specs/feedback-usecase.md` 前置条件写明按 `plannedDate` 所在周 + 用户设置的周起始日计算；设置存储接入前默认周一。
- **裁决前禁止事项（已解除）**：不得硬编码周一；不得忽略设置变更的生效时点。

### C-082 迁移窗口首条间隔的来源（已裁决：方案 A，2026-11-17）

- **关联决策/规格**：D-05、C-018、C-037；`feedback-usecase.md:204,208`。
- **现象/用户影响**：老数据迁移时窗口第一条一律从创建日算间隔，会让升级后首次半衰期失真，复习节奏突变。
- **裁决**：用户选择 **A**——迁移窗口多取一条历史，窗口第一条从前一条复习日算起。
- **落地**：`applyTriggered` 读取最近 4 条历史（含本批已处理行），窗口首条使用紧邻的前一条复习日作为锚点；正常第 3 条初始化仍从创建日起算。
- **测试**：`c082_migration_fallback_uses_previous_reviewed_on_as_interval_anchor`（独立硬编码半衰期，未修复时会被夹到 365 天）。
- **规格同步**：2026-11-17 授权后已回写。`docs/specs/feedback-usecase.md` 的 DHP 迁移兜底条目补“窗口首条从前一条复习日起算”。
- **裁决前禁止事项（已解除）**：不得静默用当前档位重复 3 次代替真实历史；不得改变正常路径语义。

### C-083 有效档位解析重复（已裁决：方案 C，2026-11-17）

- **裁决**：抽出共享纯函数，UseCase 与写入入口共用同一实现。
- **落地**：`ConfirmedDecision.effectiveTier()` 放在 `repository/ConfirmedDecision.kt`；UseCase 与 `RoomFeedbackRepository` 都调用它，删除重复实现。
- **测试**：U-02/U-15 继续锁定接受/修正/补填的档位解析。
- **规格同步**：2026-11-17 授权后已回写。`docs/specs/feedback-usecase.md` 接口注记明确 `effectiveTier()` 是唯一档位解析入口。

### C-084 实际间隔参数的默认值（已裁决：方案 A，2026-11-17）

- **裁决**：去掉 `actualIntervalDays` 的默认值，由编译器强制传参。
- **落地**：`NextDueCalculator` 接口与两个实现均改为必填参数；唯一调用方 `ApplyFeedbackUseCase` 显式传入。
- **测试**：编译期锁定；U-06～U-18 全绿。
- **规格同步**：无需改规格（规格本就是必填，代码已与规格一致）。

### C-085 无调用方的冷启动实现（已裁决：方案 B，2026-11-17）

- **裁决**：移到测试源集作为对照实现，不进入生产装配。
- **落地**：`ColdStartNextDueCalculator` 从 `src/main` 移到 `src/test/kotlin/.../usecase/`；生产只剩 `MaimemoNextDueCalculator`。
- **测试**：模块测试全绿；生产源码无该类型。
- **规格同步**：无需改规格（C-085 只涉及生产/测试源集的归属）。

### C-086 DHP 期望值反推与“不等于 7 天”断言（已裁决：方案 A，2026-11-17）

- **裁决**：用独立硬编码的期望半衰期/间隔计算到期日，去掉“不等于 7 天”的额外断言。
- **落地**：U-07/U-08/U-12/U-17 改为断言硬编码的半衰期与间隔天数（U-07 第 4 次 h=243.53787231445312、间隔 30；全 FAMILIAR 第 7 次 h=127.32052612304688、间隔 19；U-17 第 4 次 h=178.90626525878906、间隔 27）。
- **测试**：目标用例全绿。
- **规格同步**：无需改规格（C-086 只涉及测试断言）。

### C-087 第 3 条反馈后的初始化只有非空断言（已裁决：方案 A，2026-11-17）

- **裁决**：在 U-17 中按公式给出初始难度/半衰期的数值期望。
- **落地**：U-17 断言第 3 次后 `difficulty=1`、`halfLifeDays=114.27466583251953f`；第 5 次 UNKNOWN 后 `difficulty=3`、`halfLifeDays=11.457879066467285f`。`agent-log` 的过度声明已在本轮记录中更正。
- **测试**：U-17 全绿。
- **规格同步**：无需改规格（C-087 只涉及测试断言）。

### C-088 “补填”无测试（已裁决：方案 A，2026-11-17）

- **裁决**：补一个“补填 + 用户选择档位”的用例。
- **落地/测试**：`supplemented_row_uses_selected_tier_and_counts_effective_feedback` 锁定 `tier=FAMILIAR`、`isSupplemental=true`、`effectiveWeight=0.5f`、连续掌握度与有效反馈计数。
- **规格同步**：2026-11-17 授权后已回写。`docs/specs/confirm-flow.md` 验收补补填用例；`docs/specs/repository.md` 验收补 C-077～C-089 回归。

### C-089 计划号/页号未断言（已裁决：方案 A，2026-11-17）

- **裁决**：在提交事务测试中断言反馈/日志的计划号、页号与计划日。
- **落地/测试**：`FeedbackRepositoryTransactionTest` 的成功用例新增 `log.planId/pageId/plannedDate` 与 `feedback.planId/pageId/plannedDate` 断言。
- **规格同步**：2026-11-17 授权后已回写。`docs/specs/repository.md` 验收补计划号/页号/计划日断言。

## 已裁决但待验证（非新冲突）

- C-050 的 `Double` 内部算术与 `Float` 边界转换，需在 `ApplyFeedbackUseCase` 实现后用 U-01～U-19 验证。
- C-051 方案 A 的 R-01 spike 结果待验证：850×540 → 2480×3508 适配后的 OMR/QR 精度必须用真实打印稿判断；不达标时按 `scan-pipeline.md` 转方案 B（fork）。这不改变已裁决的 A 方案，只记录验证门禁。

## 历史引用

- C-001～C-005 的原始裁决记录不在仓库；ADR 已删除，历史裁决仅存于 `docs/agent-log/2026-11-11.md` 与 Blurt H-0001。
