# 备份导出规格（data/backup）

关联决策：D-11；独立约束 B-01
关联裁决：C-023、C-024、C-026、C-031

## 冻结决策（D-11、B-01）

- **D-11**：归档条目退出调度，历史反馈保留为知识点背景数据；活跃题目计数为 0 时知识点休眠；取消归档重新激活，`continuousScore` 保留。
- **B-01**：本约束独立于 D-01～D-13，仅约束导入行为。只有 `backupFormatVersion` 与 `roomSchemaVersion` 都相同的导出文件才全量恢复；`backupFormatVersion` 相同但 `roomSchemaVersion` 不同、或 `backupFormatVersion` 跨版本时，按本文件逐字段矩阵降级：恢复题目、知识点、关联、笔记内容、编号墓碑与编号序列，不恢复调度状态、历史反馈与 `PlanPage`；跨版本/跨 schema 导入后旧打印稿失效。B-01 的最终裁决权归本文件。

## 版本字段（C-023）

导出 JSON 顶层：

```json
{
  "backupFormatVersion": 1,
  "roomSchemaVersion": 1,
  "appVersionName": "0.1.0",
  "exportedAt": "2026-11-11T14:00:00Z",
  "data": { }
}
```

- `backupFormatVersion: Int`：备份文件结构版本，当前为 `1`；改变字段名/层级/语义时递增。
- `roomSchemaVersion: Int`：导出时的 Room schema 版本；用于同格式、不同 schema 的字段级降级。
- `appVersionName: String`：仅诊断用，不参与兼容判断。
- 导入顺序：先校验 `backupFormatVersion`：
  - 低于当前 → 顺序迁移到当前 `backupFormatVersion`；
  - 高于当前 → 拒绝导入；
  - 相同且 `roomSchemaVersion` 相同 → 全量恢复；
  - 相同但 `roomSchemaVersion` 不同 → 按逐字段矩阵的「跨版本/跨 schema」列降级恢复，并提示哪些字段被重置。

## JSON Schema（C-043）

```json
{
  "backupFormatVersion": 1,
  "roomSchemaVersion": 1,
  "appVersionName": "0.1.0",
  "exportedAt": "2026-11-13T10:00:00Z",
  "data": {
    "knowledgePoints": [],
    "questions": [],
    "questionKnowledgeCrossRefs": [],
    "reviewFeedbacks": [],
    "confirmationDecisionLogs": [],
    "planPages": [],
    "noteAttachments": [],
    "noteAttachmentItems": [],
    "knowledgePointTombstones": [],
    "knowledgeNumberSequences": []
  }
}
```

- 数组键名固定为小驼峰复数；每个元素字段名与 `data-model.md` 的 Kotlin 属性名一致。
- `LocalDate` 用 `"YYYY-MM-DD"`；`Instant` 用 ISO-8601 UTC 字符串 `"YYYY-MM-DDTHH:mm:ssZ"`。
- `PendingFeedback`、`ScanSession`、`CoolingOverrideEvent` 不出现。
- 笔记图片放在 zip 的 `attachments/<questionId>/<file>`；JSON 的 `imageUri` 存相对路径。

```kotlin
data class BackupEnvelope(
    val backupFormatVersion: Int,
    val roomSchemaVersion: Int,
    val appVersionName: String,
    val exportedAt: Instant,
    val data: Map<String, List<Map<String, Any?>>>
)

data class ImportReport(
    val restored: Map<String, Int>,     // 实体名 -> 数量
    val skippedRows: List<String>,      // 悬空/冲突行
    val warnings: List<String>
)
```

### 版本迁移

- 每个 `backupFormatVersion` 提供纯函数：
  ```kotlin
  fun migrateV1ToV2(envelope: JsonObject): JsonObject
  ```
- 导入时从文件版本逐级迁移到当前版本；迁移失败 → 拒绝导入，不触碰本地数据。
- 当前只有 v1；`migrate` 表为空，但接口必须存在，后续版本按顺序追加。
- `roomSchemaVersion` 不参与迁移函数选择，只参与字段级矩阵。

## 导出/导入 UI（C-043）

- 导出：`IDLE → PICK_TARGET → EXPORTING → DONE/ERROR`；SAF 选择目标文件；失败保留本地数据。
- 导入：`IDLE → PICK_FILE → PREVIEW → CONFIRM_REPLACE → IMPORTING → DONE/ERROR`。
  - `PREVIEW` 显示版本、导出时间、实体数量、预计覆盖范围、旧打印稿失效提示。
  - `CONFIRM_REPLACE` 必须二次确认；用户取消则回到 `IDLE`。
  - `IMPORTING` 单事务；失败回滚并显示错误。
- 备份大小超过 20MB 时提示“文件较大，建议先导出/清理历史”；不自动分片。

## 导出

- 格式：JSON（UTF-8）
- 白名单（唯一导出范围，不再表述为「全部实体」）：
  - `KnowledgePoint`
  - `Question`
  - `QuestionKnowledgeCrossRef`
  - `ReviewFeedback`
  - `ConfirmationDecisionLog`
  - `PlanPage`
  - `NoteAttachment`
  - `NoteAttachmentItem`
  - `KnowledgePointTombstone`
  - `KnowledgeNumberSequence`
- **不导出** `PendingFeedback` 与 `CoolingOverrideEvent`：前者是人工确认前的临时状态，未确认决策不得通过备份绕过 R-04（C-031 明确排除）；后者是一次性冷却覆盖操作，导入后重新触发即可（C-008 方案二）。
- 含归档与休眠状态。
- 位置：通过 SAF 让用户选择；默认建议 `Downloads/Recollect/recollect-YYYYMMDD-HHmm.json`
- 不加密；文档中声明文件含个人信息，提示用户妥善保管。

## 导入

### 替换式导入

- 导入前提示「将覆盖本机全部现有数据」；用户确认后，在单事务内清空本地白名单表、`PendingFeedback`、`ScanSession` 与 `CoolingOverrideEvent`（临时状态）再恢复。避免导入后残留旧会话的未确认行、旧扫描会话或旧冷却覆盖事件。
- 不做自动合并；如果用户需要合并，后续另行设计（本版不做）。
- 恢复 `KnowledgePoint.number` 前先建墓碑/序列表；若备份内编号重复或与墓碑冲突，导入失败并列出冲突编号，不得静默重编号。
- 恢复后校正 `KnowledgeNumberSequence.lastIssued = max(lastIssued, max(number), max(tombstone.number))`。

### 逐字段恢复矩阵（C-023）

| 实体 | 字段 | 同格式同 schema（backupFormatVersion 与 roomSchemaVersion 都相同） | 跨版本/跨 schema（降级） |
|---|---|---|---|
| KnowledgePoint | `id` / `number` / `name` / `createdAt` | 恢复 | 恢复 |
| KnowledgePoint | `currentTier` | 恢复 | 重置为 `UNKNOWN` |
| KnowledgePoint | `continuousScore` | 恢复 | 重置为 `0.0` |
| KnowledgePoint | `effectiveFeedbackCount` | 恢复 | 重置为 `0.0` |
| KnowledgePoint | `difficulty` / `halfLifeDays` | 恢复 | 重置为 `null`（下次第 3 条 triggered 反馈后重新初始化） |
| KnowledgePoint | `nextDueDate` | 恢复 | 重置为导入日 + 1 天 |
| KnowledgePoint | `activeQuestionCount` | 恢复 | 按恢复后的未归档题目重算 |
| KnowledgePoint | `dormant` | 恢复 | 按 `activeQuestionCount == 0` 重算 |
| Question | `id` / `questionNumber` / `estimatedReviewMinutes` / `createdAt` | 恢复 | 恢复；缺失时 `estimatedReviewMinutes = 15` |
| Question | `archived` / `archivedAt` | 恢复 | 恢复 |
| Question | `pageId` / `qrPayload` | 恢复 | 清空为 `""`；旧打印稿失效 |
| Question | `lastScheduledDate` / `lastReviewedDate` | 恢复 | 重置为 `null` |
| QuestionKnowledgeCrossRef | 全部 | 恢复 | 恢复；任一端缺失则跳过该行并警告 |
| ReviewFeedback | 全部 | 恢复 | **不恢复**（历史反馈丢弃，提示用户） |
| ConfirmationDecisionLog | 全部 | 恢复 | **不恢复** |
| PlanPage | 全部 | 恢复 | **不恢复**；pageId 由新计划重新生成 |
| NoteAttachment | `attachToPrint` | 恢复 | 恢复 |
| NoteAttachment | `attachmentCount` | 恢复 | 按恢复后的 `NoteAttachmentItem` 条数重算为 `min(3, 条数)` |
| NoteAttachmentItem | `text` / `order` / `createdAt` / `imageUri` | 恢复 | 恢复（笔记内容保留）；`imageUri` 若指向本地文件，需随备份打包或置空并警告 |
| NoteAttachmentItem | `sourceFeedbackId` | 恢复 | 置 `null`（跨版本不恢复 ReviewFeedback，避免悬空引用） |
| KnowledgePointTombstone | 全部 | 恢复 | 恢复 |
| KnowledgeNumberSequence | `lastIssued` | 恢复 | 恢复后按上一条规则校正；若备份缺少该行，初始化为 `max(恢复后的 KnowledgePoint.number, 墓碑 number, 0)` |
| PendingFeedback | — | 不导出 | 不导出 |
| ScanSession | — | 不导出 | 不导出（计划扫描临时态） |
| CoolingOverrideEvent | — | 不导出 | 不导出（一次性操作态） |

### 悬空引用与旧打印稿

- 跨版本不恢复 `PlanPage`，且清空 `Question.pageId/qrPayload`；导入完成后提示「旧打印稿已失效，请重新生成计划并打印」。
- 同格式同 schema 恢复保留 `PlanPage` 与 `Question.pageId/qrPayload`，旧打印稿在该版本内继续可用于手动回退；同格式跨 schema 也走降级，旧稿失效。
- 任何恢复失败的关联行进入 `ImportReport.skippedRows`，不得产生半恢复的悬空引用。
- 归档反馈按 D-11 保留为知识点背景数据；跨版本导入不恢复 `ReviewFeedback` 是 B-01 的降级例外，需在导入确认页明确告知「历史反馈不会恢复」。D-11 的「历史反馈保留」适用于同版本/正常使用，不豁免跨版本降级。

## 保留与清理（C-045）

- 默认**不自动删除** `ReviewFeedback`、`ConfirmationDecisionLog`；它们保留为知识点背景数据（D-11）。
- `PlanPage` 默认保留最近 26 周（`AppSettings.keepPlanPagesWeeks`）；更早的 `PlanPage` 可在用户确认后清理，清理前提示“旧打印稿将无法手动回退”。
- `cleanupPlanPagesAfterExport=false` 时，导出后不删除任何历史；开启后也只执行用户手动确认的“清理旧 PlanPage”，不自动删除 `ReviewFeedback`/`ConfirmationDecisionLog`。
- 备份前估算大小；超过 20MB 提示导出/清理建议。

## 验收

- 导出 → 清空 DB → 导入 → 断言：
  - 各实体数量；
  - `KnowledgePoint.number` 唯一且未被墓碑占用；
  - `QuestionKnowledgeCrossRef` 关联完整，题目 `linkedKnowledgeIds` 可重建；
  - 归档、休眠、`continuousScore`、`effectiveFeedbackCount` 一致；
  - `PlanPage.pageId` 全局唯一，`Question.pageId` 与之对应；
  - `NoteAttachmentItem` 条数与摘要文本一致；
  - `ReviewFeedback` 的 `rawOcrText`（只读原文）与 `correctedText`（可编辑修正）都出现在导出与恢复结果中；
  - 跨版本恢复的 `NoteAttachmentItem.sourceFeedbackId` 全为 `null`，不存在指向缺失 `ReviewFeedback` 的悬空引用；
  - `PendingFeedback`、`ScanSession`、`CoolingOverrideEvent` 未出现在导出文件中；
  - JSON 键名/日期格式符合 `BackupEnvelope`，`LocalDate`/`Instant` 可无损恢复。
- 跨版本用旧 schema sample JSON 验证：
  - 调度状态重置；
  - 题目、知识点、关联、笔记内容保留；
  - `PlanPage` 不恢复，`pageId/qrPayload` 清空，旧打印稿失效提示出现；
  - 历史反馈不恢复的提示出现。
- 版本号高于当前 → 拒绝导入；`backupFormatVersion` 相同但 `roomSchemaVersion` 不同 → 走逐字段矩阵。
- 导入失败（编号冲突/事务异常）→ 本地数据保持导入前状态。
- SAF 状态机：取消选择、预览取消、覆盖确认取消都不改本地数据；导入失败回滚。
- 迁移函数 v1 → 当前版本为恒等；新增版本必须补迁移与 sample JSON 测试。
- 清理：PlanPage 超过 `keepPlanPagesWeeks` 后可清理；`ReviewFeedback`/`ConfirmationDecisionLog` 不自动删除。
