# 人工确认页规格（feature/confirm）

关联决策：D-06、D-09、D-13
关联裁决：C-009、C-010、C-011、C-012、C-022、C-030、C-038、C-040、C-041、C-050、C-077、C-078、C-079、C-080、C-081

## 冻结决策（D-09、D-13）

- **D-09**：QR 失败走手动输入 pageId + 题号；确认页校验归属。
- **D-13**：逐项标记接受/修改复习效果/本次未复习；不得一键全部接受；`NOT_REVIEWED` 不写 `ReviewFeedback`，只写 `ConfirmationDecisionLog`，知识点状态不变。2026-11-22 用户要求取消「修正」与「补填」的区分，统一为「修改复习效果」，`isSupplemental` 退出数据模型。

## 页面结构

- 顶部：`pageId`（显示为页码）、题号范围、`planId`、归属校验结果
- 逐知识点行（左侧打印为一题多行，此处一一对应）：
  - 行类型标记：`触发` / `关联`（C-009）
  - 来源页码：`来源页 {sourcePage}`，为用户可调编号或 `编号-说明`（如 `2-同步练习`）；说明为空时只有编号（2026-11-20 用户要求）
  - 知识点编号 + 全称
  - 复习效果识别结果：直接显示「掌握 / 较熟悉 / 不会」；无填涂/冲突/无法识别时显示对应提示（2026-11-22 用户要求，不再显示内部术语“OMR”）；该行标记「本次未复习」后改为显示「复习效果：本次未复习」（2026-11-22 用户要求）
  - 用户操作："确认（ACCEPT）/ 修改复习效果（CORRECTED）/ 本次未复习（NOT_REVIEWED）"（D-13；2026-11-22 用户要求）
  - 修改复习效果时弹出档位单选
  - 单行完成任一操作后：在该行按钮同一行最右侧（与校对文本框右缘对齐、垂直居中）显示对勾图标，并禁用校对文本框与该行全部按钮，表示本题确认结束（2026-11-22 用户要求）
  - 只保留**一个可编辑文本框**，左上角提示「请核对笔记识别结果」；默认填入 ML Kit OCR 识别文本，原始 OCR 仍单独保存为 `detectedRawOcr`，编辑只改 `correctedText`（2026-11-22 用户要求）
  - 修正文本可一键「保存为笔记附件」（写 `NoteAttachmentItem(text=correctedText)`）
- 题目级操作："本次提前复习"（创建一次性 `CoolingOverrideEvent`，C-008 方案二）
- 底部："确认提交"；存在未标记行时禁用并提示「还有 N 行未标记」

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
UNMARKED ──接受──▶ ACCEPT
   │        ├─修改复习效果──▶ CORRECTED
   │        └─本次未复习──▶ NOT_REVIEWED
   └─（未操作，提交被阻止）
```

`PendingFeedback.userDecision` 初始为 `UNMARKED`（C-010）。状态转移规则：

- `ACCEPT`：优先使用 `detectedTier`；若 `detectedTier == null`（NO_MARK/CONFLICT/UNREADABLE），允许用户先在该行选择 `selectedTier` 再点接受，写入 `detectedTier ?: selectedTier`；两者都为空时不可接受。`selectedTier` 仅在 `detectedTier` 为空时参与 ACCEPT（C-022）。
- `CORRECTED`（修改复习效果）：必须选择 `selectedTier`；`rawOcrText` 与 `correctedText` 都保存。确认页加载时 `correctedText` 默认等于 OCR 原文，用户编辑后只覆盖 `correctedText`（2026-11-22 用户要求）。
- `NOT_REVIEWED`：**不写 `ReviewFeedback`**；写 `ConfirmationDecisionLog`；该知识点 `nextDueDate` 与所有状态不变（C-010）。
- 任何 `UNMARKED` 行存在时，"确认提交"禁用（D-13、R-04）。

### 识别结果与操作可用性（C-030；2026-11-22 起界面显示为复习效果识别结果）

| detectionStatus | 含义 | 可用操作 |
|---|---|---|
| `OK` | 恰好一组填涂 | 接受 / 修改复习效果 / 本次未复习 |
| `NO_MARK` | 该行无填涂 | 接受（需先选档位）/ 修改复习效果 / 本次未复习 |
| `CONFLICT` | 多组填涂冲突，`conflict=true` | 接受（需先选档位）/ 修改复习效果 / 本次未复习 |
| `UNREADABLE` | 图像无法判定 | 接受（需先选档位）/ 修改复习效果 / 本次未复习 |

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
- 不允许在存在 `UNMARKED` 行时提交。
- 不允许对已 `SUBMITTED` 的扫描会话再次提交。
- 不允许把 `detectedRawOcr` 本身绑定为可编辑控件（D-06）；可编辑控件绑定 `correctedText`，其初始值可复制自 OCR 原文，原始值保持不变。

## 验收

- UI 测试（android-emu-agent）：构造 3 知识点场景，分别标记接受/修正/未复习，断言数据库写入符合状态机。
- 「存在 UNMARKED 行时提交被阻止」的测试（按钮禁用 + UseCase 拒绝双保险）。
- 单行完成决策后显示对勾，并禁用校对文本框与该行全部按钮（2026-11-22 用户要求）。
- `NOT_REVIEWED` 只写 `ConfirmationDecisionLog`、不写 `ReviewFeedback` 的测试。
- 非触发行填不同档位 → 按该行档位 ×0.3；非触发行「本次未复习」→ 不更新的测试。
- 归属校验失败（版本/planId/pageId/questionId/校验码/知识点归属）逐项阻断测试。
- 整题跳过（题目或关联知识点不存在）时：不写反馈/状态/复习日，仍写决定日志，返回并展示跳过清单，待确认行保留（C-077/C-078 方案 A）。
- 已 `SUBMITTED` 会话重复提交被拒绝且不写任何行（C-080 方案 A）。
- 复习日早于“计划日所在周 + 用户设置的周起始日”时提交被拒绝（C-081 方案 A）。
- 修改复习效果（`CORRECTED`）按用户选择的档位计入有效反馈，与接受走同一套档位解析（C-083 方案 C）。
- 原始 OCR 与修正文本都被保存；原始 OCR 控件只读断言。
- 「本次提前复习」创建一条未消费 `CoolingOverrideEvent`；题目排入下一份计划后事件被消费；重复点击不重复插入。
- 「保存为笔记附件」写入 `NoteAttachmentItem(text=correctedText, order=当前最大+1)`；不改变原始 OCR。
- 多页同一 `ScanSession` 只提交一次；按页分组展示；重复扫描同页去重并提示。
- 批次实际复习日期默认 `plannedDate`；逐行覆盖后 `lateDays = actualReviewDate - plannedDate`；`NOT_REVIEWED` 不写 `lastReviewedDate`。
- 扫描日早于 `planEndDate` 时，未来日期行只能标 `NOT_REVIEWED`；否则提交被阻止。
