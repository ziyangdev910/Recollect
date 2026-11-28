# 人工确认页规格（feature/confirm）

关联决策：D-06、D-09、D-13
关联裁决：C-009、C-010、C-011、C-012、C-022、C-030、C-038、C-040、C-041、C-050、C-077、C-078、C-079、C-080、C-081

## 冻结决策（D-09、D-13）

- **D-09**：QR 失败走手动输入 pageId + 题号；确认页校验归属。
- **D-13**：逐项标记接受/修改复习效果/本次未复习；不得一键全部接受；`NOT_REVIEWED` 不写 `ReviewFeedback`，只写 `ConfirmationDecisionLog`，知识点状态不变。2026-11-22 用户要求取消「修正」与「补填」的区分，统一为「修改复习效果」，`isSupplemental` 退出数据模型。

## 页面结构

- 顶部：`planId`、总页数/题数、整批确认进度（已标记行数 / 总行数）与进度条；按页分组，页头显示打印稿 `pageId` 与计划日。
- 校对卡片按题合并（2026-11-28 用户要求）：同一题的多个知识点行共用一张卡片，标题统一为 `来源 第X页 第Y题`（`formatQuestionLabel`；无来源说明时按试卷份数显示）；不再显示「触发 / 关联」、题号、来源页等分散标签。
- 卡片内逐知识点行（左侧打印为一题多行，此处一一对应）：
  - 知识点编号 + 全称。
  - 复习效果胶囊：显示当前生效的效果——已落定看决策，未落定看弹窗暂存或识别预览；「本次未复习」显示「复习效果：未复习」，无填涂/冲突/无法识别显示对应提示（2026-11-22 用户要求，不再显示内部术语“OMR”）。
  - 胶囊右侧铅笔图标：打开紧凑弹窗「修改【知识点】掌握程度」，四个档位（掌握 / 较熟悉 / 不会 / 未复习）排成一行可选标签，默认勾选当前状态；未填涂、识别失败或计划日晚于今天的行默认「未复习」，未来日期的行只能选「未复习」（2026-11-28 用户要求）。
  - 弹窗只暂存选择，关闭后该行仍为 `UNMARKED`；必须点击卡片右下角的「确认」才把该行落定为 `ACCEPT` / `CORRECTED` / `NOT_REVIEWED`（2026-11-28 用户要求，禁止修改即自动确认）。
  - 已确认的行锁定复习效果：铅笔禁用，再次修改不生效（2026-11-28 用户要求）。
  - 删除卡片底部的「修改复习效果」「本次未复习」按钮与「未标记」标签；卡片底部只保留右对齐的「确认」按钮（2026-11-28 用户要求）。
- 笔记识别结果（整题共用，写入该题每个知识点行）：紧凑文本框；无识别文字时占位「笔记识别结果为空」；默认填入 ML Kit OCR 识别文本，原始 OCR 仍单独保存为 `detectedRawOcr`，编辑只改 `correctedText`（2026-11-22 / 2026-11-28 用户要求）。卡片全部知识点确认后隐藏文本框，改为 `复习笔记：…`：单行接在冒号后，多行换行开始，无笔记显示「复习笔记：无」。
- 修正文本可一键「保存为笔记附件」（写 `NoteAttachmentItem(text=correctedText)`）。
- 题目级操作："本次提前复习"（创建一次性 `CoolingOverrideEvent`，C-008 方案二）。
- 底部：右下角胶囊「完成」（前置对勾图标，正常主色背景，全部行标记后才可用）；不显示「还有 N 行未标记」提示与提交结果卡片（2026-11-28 用户要求）。

## 批量回读与扫描会话（C-038、C-040、C-041）

- 一次 `ScanSession` 对应一个 `planId`；多页扫描在同一会话中追加行，确认页按页分组展示，底部一次提交。
- 顶部显示 `planId`、当前页 `pageId`、`plannedDate`、批次实际复习日期、`LATE_BATCH` 提示（如有）。
- **实际复习日期**：
  - 批次默认 = 各行的 `plannedDate`；用户可逐行覆盖，写回 `PendingFeedback.actualReviewDate`（进程重建后仍保留）。
  - `actualReviewDate > plannedDate` → 该行标 `LATE`，显示 `lateDays`；`actualReviewDate <= plannedDate` → `ON_TIME`。
  - 扫描日 > `planEndDate + 7` → 显示 `LATE_BATCH`，但不自动改每行日期；窗口内（planEnd+1..+7）为正常批量。
  - 扫描日 < `planEndDate` 时，`plannedDate > 今天` 的行必须标 `NOT_REVIEWED`，否则禁止提交。
- 提交前：所有行 `UNMARKED` 必须清零；`actualReviewDate` 不能晚于今天、不能早于“计划日所在周 + 用户设置的计划周起始日”；未来日期的行不能写非 `NOT_REVIEWED` 决策。
- 同一 `ScanSession` 只提交一次：会话已 `SUBMITTED` 时再次提交必须被拒绝并提示“已提交”，不写任何行（C-080 方案 A）；确认页的按钮禁用与 UseCase 拒绝构成双保险。

## 归属校验（C-011 方案一 + 方案二）

scan 端在进入本页前完成校验；本页渲染前与提交前各再校验一次。校验对象来自 QR 或手动输入：

| 检查 | 适用 | 规则 | 失败处理 |
|---|---|---|---|
| 版本 | 仅 QR | `version == 1`（当前 QR 载荷版本） | 阻断，不进确认页 |
| planId | QR / 手动输入 | QR 的 `planId` 与 `PlanPage.planId` 一致；手动输入由 pageId 内嵌的 planId 解析 | 阻断 |
| pageId | QR / 手动输入 | `PlanPage` 存在且 `pageId` 全局唯一命中 | 阻断 |
| 题目归属 | QR / 手动输入 | `questionId` 存在，且在该页 `PlanPage.questionIds` 中；手动输入时 `questionNumber` 与纸面题号一致 | 阻断 |
| 知识点归属 | QR / 手动输入 | 每行 `knowledgePointId` 属于该题 `QuestionKnowledgeCrossRef`，与 `PlanPage.knowledgeRows` 快照的成员/顺序一致，且该知识点仍存在于 DB（未删除） | 阻断该行，页面不可提交；已删除则提示重新打印 |
| 校验码 | 仅 QR | CRC-32 与 `version|planId|pageId|questionId` 重算一致 | 阻断 |
| 手动输入 | 手动输入 | `pageId + 题号` 能解析出唯一题目；pageId 命中 `PlanPage` 后取 `questionIds` 按题号匹配 | 阻断，提示「页面或题号不存在」 |

- 手动输入的 `pageId` 可输入字符集：`A-Z a-z 0-9 -`；大小写不敏感；规范化映射 `I/L → 1`、`O → 0`，其余非法字符拒绝。
- 归属校验失败时**不得写入** `PendingFeedback`、`ReviewFeedback` 或知识点状态；只显示错误与「重拍 / 重新输入 / 取消」。这是渲染前/提交前的界面层校验。
- `PlanPage.knowledgeRows` 为空或不可解析时视为无快照：阻断本页、不进入提交，提示重新生成计划并按新稿打印；不得按当前关联重算行（C-072 方案 A）。
- 提交时若校验结果与渲染时不一致（例如计划被删除），同样阻断（TOCTOU 防护）。
- **数据层兜底（C-078 方案 A）**：若提交进入数据层后才发现某题的题目记录或关联知识点已不存在，该题整题跳过：不写反馈、不写知识点状态、不写题目复习日，但仍写该题所有行的 `ConfirmationDecisionLog`，并把题目加入 `SaveDecisionsResult.skippedQuestionIds` 返回。确认页必须展示跳过清单；被跳过题目的待确认行保留，会话保持 `READY`，数据修复后可重新提交该题。
- **部分成功**：有整题跳过时，其余题目正常落库；确认页同时展示“已生效题目”和“被跳过题目”，不把整批结果伪装成全部成功或全部失败。

## 冷却覆盖（C-008 方案二）

- 页面对每道题提供题目级操作「本次提前复习」；它不改变逐知识点行的决策，只创建一条 `CoolingOverrideEvent(sessionId, questionId, createdAt, consumedAt = null)`。
- 同一题至多保留一个未消费事件；重复点击复用已有事件，不重复插入。
- 事件只影响**下一份计划**：`feature/planning` 把未消费事件的 questionId 传给 `plan(coolingOverrides = ...)`；题目实际排入计划后把事件标记为已消费。
- 若题目因时间溢出未排入，事件保持未消费，可继续用于下一份计划；若题目不在冷却期，事件不产生额外效果。
- 该事件不写入 `ReviewFeedback`，不改变知识点状态；备份不导出（一次性操作态）。

## 状态机

```text
UNMARKED ──卡片确认（接受识别结果）──▶ ACCEPT
   │        ├─弹窗暂存档位 + 卡片确认──▶ CORRECTED
   │        └─弹窗暂存未复习 + 卡片确认──▶ NOT_REVIEWED
   └─（未确认，提交被阻止）
```

`PendingFeedback.userDecision` 初始为 `UNMARKED`（C-010）。状态转移规则：

- `ACCEPT`：优先使用 `detectedTier`；若 `detectedTier == null`（NO_MARK/CONFLICT/UNREADABLE），允许用户先在该行选择 `selectedTier` 再点接受，写入 `detectedTier ?: selectedTier`；两者都为空时不可接受。`selectedTier` 仅在 `detectedTier` 为空时参与 ACCEPT（C-022）。
- `CORRECTED`（修改复习效果）：必须选择 `selectedTier`；`rawOcrText` 与 `correctedText` 都保存。确认页加载时 `correctedText` 默认等于 OCR 原文，用户编辑后只覆盖 `correctedText`（2026-11-22 用户要求）。
- `NOT_REVIEWED`：**不写 `ReviewFeedback`**；写 `ConfirmationDecisionLog`；该知识点 `nextDueDate` 与所有状态不变（C-010）。
- 任何 `UNMARKED` 行存在时，底部「完成」胶囊禁用（D-13、R-04）。

### 弹窗暂存与卡片确认（2026-11-28 用户要求）

- 修改掌握程度只在界面状态中暂存 `EffectChoice.Rated(tier)` 或 `EffectChoice.NotReviewed`，**不**改变 `userDecision`；暂存行仍计入 `UNMARKED`，提交继续被阻止。
- 点击卡片「确认」时逐行落定：有暂存档位时，与 `detectedTier` 相同按 `ACCEPT`、不同按 `CORRECTED`，暂存「未复习」按 `NOT_REVIEWED`；没有暂存时接受识别档位；识别档位为空时返回「需要选择」，由弹窗补选；计划日晚于今天的行确认即按 `NOT_REVIEWED` 落定。
- 已确认的行进入锁定状态：界面禁用铅笔图标，状态机忽略后续暂存修改；不得通过再次修改效果把已确认行退回未标记。

### 识别结果与操作可用性（C-030；2026-11-22 起界面显示为复习效果识别结果）

下表操作均在弹窗/卡片中完成，最终必须手动点卡片「确认」落定（2026-11-28 用户要求）。

| detectionStatus | 含义 | 可用操作 |
|---|---|---|
| `OK` | 恰好一组填涂 | 卡片确认（接受）/ 修改复习效果 / 本次未复习 |
| `NO_MARK` | 该行无填涂 | 卡片确认（需先在弹窗选档位）/ 修改复习效果 / 本次未复习 |
| `CONFLICT` | 多组填涂冲突，`conflict=true` | 卡片确认（需先在弹窗选档位）/ 修改复习效果 / 本次未复习 |
| `UNREADABLE` | 图像无法判定 | 卡片确认（需先在弹窗选档位）/ 修改复习效果 / 本次未复习 |

## 有效反馈计数（D-05、C-006）

- `effectiveFeedbackCount` 是 `Float`，值为 0.5 的倍数。
- 仅**触发行**计入 `effectiveFeedbackCount`：`MASTERED` / `UNKNOWN` 计 1.0，`FAMILIAR` 计 0.5，`NOT_REVIEWED` 计 0。
- 非触发行即使选择 ACCEPT/CORRECTED，也**不计入** `effectiveFeedbackCount`；它只更新 `continuousScore ×0.3`（C-009）。
- 切换阈值为 `>= 3.0f`，不做 epsilon 容差（0.5 在二进制浮点中可精确表示）。
- `continuousScore` 的加减与夹取在 `ApplyFeedbackUseCase` 内部用 `Double`，写入 Room 时转 `Float`（C-050）。
- 计数由 `ApplyFeedbackUseCase` 在提交事务内更新；本页只收集决策。

## 触发行与非触发行（C-009）

- 打印模板为每道题的每个 printable 知识点（`!dormant && activeQuestionCount > 0`）各留一行；本页按 `PlanPage.knowledgeRows` 快照逐行展示，`PendingFeedback.triggered` 标记行类型。
- **触发知识点**：按该行档位全量更新 `tier / continuousScore / nextDueDate / effectiveFeedbackCount`。
- **非触发关联知识点**：该行可以填 A/B/C，也可以标记「本次未复习」：
  - 填 A/B/C：仅按**该行档位**的 D-03 增量 ×0.3 更新 `continuousScore`；`tier / nextDueDate / effectiveFeedbackCount` 不变；
  - 本次未复习：全部不变；
  - 不得借用触发行的档位。
- 因此「掌握来自哪一次填涂」的答案是该知识点自己那一行的填涂；C-009 的原始疑问由逐行模型消解，不另设全局档位。

## 禁止

- 不提供"一键全部接受"绕过逐项核对（R-04、D-13）。
- 不在本页直接更新知识点状态；提交后由 `ApplyFeedbackUseCase` 统一写入。
- 不允许未经用户确认的批量写入。
- 不允许在存在 `UNMARKED` 行时提交；弹窗暂存选择不算完成标记（2026-11-28 用户要求）。
- 不允许在卡片「确认」落定后再次修改该知识点的复习效果（2026-11-28 用户要求）。
- 不允许对已 `SUBMITTED` 的扫描会话再次提交。
- 不允许把 `detectedRawOcr` 本身绑定为可编辑控件（D-06）；可编辑控件绑定 `correctedText`，其初始值可复制自 OCR 原文，原始值保持不变。

## 验收

- UI 测试（android-emu-agent）：构造 3 知识点场景，分别标记接受/修正/未复习，断言数据库写入符合状态机。
- 「存在 UNMARKED 行时提交被阻止」的测试（按钮禁用 + UseCase 拒绝双保险）。
- 弹窗选择只暂存：关闭弹窗后该行仍为 `UNMARKED`、提交被阻止；手动点卡片「确认」后才显示对勾（2026-11-28 用户要求）。
- 卡片确认后：同一题的知识点锁定复习效果（铅笔禁用、再次修改不生效），文本框折叠为「复习笔记」文本；同一题的多个知识点共用一张卡片与一个笔记框（2026-11-28 用户要求）。
- `NOT_REVIEWED` 只写 `ConfirmationDecisionLog`、不写 `ReviewFeedback` 的测试。
- 非触发行填不同档位 → 按该行档位 ×0.3；非触发行「本次未复习」→ 不更新的测试。
- 归属校验失败（版本/planId/pageId/questionId/校验码/知识点归属）逐项阻断测试。
- 整题跳过（题目或关联知识点不存在）时：不写反馈/状态/复习日，仍写决定日志，返回跳过清单并以错误提示告知（不显示提交结果卡片），待确认行保留（C-077/C-078 方案 A；2026-11-28 用户要求删去提交结果卡片）。
- 已 `SUBMITTED` 会话重复提交被拒绝且不写任何行（C-080 方案 A）。
- 复习日早于“计划日所在周 + 用户设置的周起始日”时提交被拒绝（C-081 方案 A）。
- 修改复习效果（`CORRECTED`）按用户选择的档位计入有效反馈，与接受走同一套档位解析（C-083 方案 C）。
- 原始 OCR 与修正文本都被保存；原始 OCR 控件只读断言。
- 「本次提前复习」创建一条未消费 `CoolingOverrideEvent`；题目排入下一份计划后事件被消费；重复点击不重复插入。
- 「保存为笔记附件」写入 `NoteAttachmentItem(text=correctedText, order=当前最大+1)`；不改变原始 OCR。
- 多页同一 `ScanSession` 只提交一次；按页分组展示；重复扫描同页去重并提示。
- 批次实际复习日期默认 `plannedDate`；逐行覆盖后 `lateDays = actualReviewDate - plannedDate`；`NOT_REVIEWED` 不写 `lastReviewedDate`。
- 扫描日早于 `planEndDate` 时，未来日期行只能标 `NOT_REVIEWED`；否则提交被阻止。
