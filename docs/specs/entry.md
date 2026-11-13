# 录入规格（feature/entry）

关联决策：D-06、D-12
关联裁决：C-026、C-028

## 冻结决策（D-06、D-12）

- **D-06**：手写笔记 OCR 只存储和展示，不参与调度；原始 OCR 只读、修正文本可编辑；导出两者都含。
- **D-12**：单题关联知识点 ≤ 4；知识点编号全局唯一不复用；超出 4 个时录入层拦截并提示拆分；录入层与 Repository 双重校验。

## 职责

- 题目录入、知识点录入/编辑、题目与知识点的关联选择。
- 题目录入包含 `estimatedReviewMinutes`（预估复习分钟数），默认 15，允许用户修改；调度时间装箱直接读取该字段（C-017）。
- 知识点编号由 Repository 统一分配，录入层不得自行生成或修改 `number`。
- 手写笔记/附件录入：原始 OCR 只读、修正文本可编辑（D-06）；附件文本写入 `NoteAttachmentItem.text` 时取 `correctedText` 快照。

## 单题关联知识点上限（D-12 / C-028）

- 上限 4 个；0 或负数不允许。
- **双重校验**：
  1. **录入层（UI/ViewModel）**：选择第 5 个时不允许加入，显示「单题最多关联 4 个知识点，请拆分为多道题目」；保存按钮禁用。
  2. **Repository（事务内）**：`QuestionRepository.create/update` 再次校验；超过 4 个或关联到不存在/已作废的知识点时抛 `InvalidKnowledgePointLinkException`，整个事务回滚。
- 调度器 `S-10` 的 `IllegalArgumentException` 是最后一道防线，不是用户可用的拦截。
- 不允许把「超过 4 个」静默截断为前 4 个。

## 知识点编号（D-12 / C-028）

- `KnowledgePoint.number` 全局唯一、创建后不可变、不复用。
- 分配流程（单事务）：
  1. 读取 `KnowledgeNumberSequence` 单行；若不存在（新库/旧库/导入异常），先种入 `(id = 1, lastIssued = max(现有 number, 墓碑 number, 0))`；
  2. `next = lastIssued + 1`；
  3. 若 `next` 命中 `KnowledgePointTombstone.number`，继续 +1（墓碑存在的意义就是阻止复用）；
  4. 写回 `lastIssued = next`；
  5. 插入 `KnowledgePoint(number = next)`。
- 唯一索引 `knowledge_point.number` 是最终约束；并发创建时由事务 + 唯一索引共同保证，不依赖内存计数。
- 删除知识点：先写 `KnowledgePointTombstone(number, knowledgePointId, voidedAt, reason)`，再删除实体；墓碑不随知识点删除而删除。
- 墓碑字段：编号、原知识点 ID、作废时间、原因（`deleted` / `import-conflict` / 其他）。墓碑不是「已删除知识点」的完整副本，只保留编号归属与作废审计。
- 导入时不得重置 `lastIssued` 到较小值；按 `backup.md` 规则校正为最大值。

## 题库/知识点编辑

- 编辑知识点名称不影响 `number`，也不影响已打印计划的行序（行序按 `number` 排序）。
- 修改题目关联知识点后，已打印的旧计划稿失效；录入层提示「该题已打印过旧计划，修改关联后请重新生成计划并打印」。
- 归档/取消归档属于 `Question.archived`；归档退出调度，历史反馈保留（D-11）。

## 用例表

| ID | 场景 | 断言 |
|---|---|---|
| E-01 | 关联 1～4 个知识点 | 保存成功，事务提交 |
| E-02 | 选择第 5 个知识点 | UI 拒绝加入并提示拆分；不产生保存调用 |
| E-03 | 绕过 UI 直接调用 Repository 传 5 个 | 抛 `InvalidKnowledgePointLinkException`，事务回滚 |
| E-04 | 新建知识点 | 编号由序列表分配，唯一索引通过 |
| E-05 | 删除知识点后新建 | 新编号跳过墓碑编号，不复用 |
| E-06 | 并发创建两个知识点 | 两个编号不同；无唯一索引冲突未处理异常 |
| E-07 | 修改知识点名称 | `number` 不变；打印行序不变 |
| E-08 | 修改已打印题目的关联知识点 | 提示旧稿失效；不静默沿用旧行序 |
| E-09 | 笔记附件写入 | `NoteAttachmentItem.text` 取 `correctedText`，不取 `rawOcrText` |
| E-10 | 空 `KnowledgeNumberSequence` 表（新库/旧库） | Repository 种入 `lastIssued = max(现有 number, 墓碑 number, 0)`；首个知识点成功编号，不抛空表异常 |

## 验收

- `./gradlew :feature:entry:test`（UI 校验）与 `:data:test`（Repository 事务）全绿。
- 「双重校验」两条路径各有测试：UI 层阻止 + Repository 拒绝。
- 编号不复用测试必须包含「删除 → 新建」与「导入 → 新建」两种路径。
