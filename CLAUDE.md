# CLAUDE.md — 拾知 / Recollect

## 项目定位
面向初高中学生的 Android 原生纸数融合复习工具：手机录入题目与知识点 → 生成可打印 A4 周计划 → 纸质填涂执行 → 拍照回读经人工确认更新状态 → 再规划。调度单元是知识点，题目是复习载体。

## 技术栈（不得擅自变更）
- Kotlin + Jetpack Compose，minSdk 26，targetSdk 以 libs.versions.toml 为准
- Room 持久化；JDK 17；Gradle Wrapper 命令行构建
- ML Kit 中文离线 OCR：text-recognition-chinese:16.0.0 + vision-common:17.3.0
- 扫描页相机：CameraX 1.7.0（camera-core/camera2/lifecycle/view；C-096 方案 B），仅扫描页使用 `CAMERA` 权限，拒绝时回退相册选图
- 回读引擎（C-092 方案 C，自研 `ScanEngine`）：ArUco 定位与透视矫正用 OpenCV（`com.quickbirdstudios:opencv-contrib:4.5.3.0`），逐题 QR 用 ML Kit `barcode-scanning:17.2.0`，OMR 三档识别按 `docs/specs/scan-pipeline.md` 的几何与阈值自行采样；打印层每题二维码用 ZXing core 3.5.3 生成（版本 5、纠错 M），四角 ArUco 图案以纯 Kotlin 常量绘制；`openscanvision:1.0.0` 已停用（坐标保留供 spike 复现）
- 全部推理端侧完成，不调用云端 AI/OCR，不使用 Gemini Nano
- 测试：纯 Kotlin 与 Android 单元测试用 JUnit 5；`androidTest` 用 JUnit 4（C-052 方案 C）。属性测试用 JUnit `@RepeatedTest` + `kotlin.random.Random` 手写（种子固定以保证可复现），不引入 jqwik/kotest 等属性测试库

## 模块结构与依赖方向
app → feature → data → core
现有功能模块：`:feature:entry`（Entry 空态与题目录入；知识点以标签库形式维护，1 题从标签库选择 1～4 个标签，不再输入自由文本；标签库支持新增/重命名/搜索/删除，删除前校验未被未归档题目引用并写编号墓碑；新建/编辑使用独立页面（`QuestionEditScreen`，由悬浮按钮或卡片铅笔进入），题目列表占满页面；来源页码由用户可调的编号 `sourcePageCode`（+/- 调整、默认沿用上次使用、同页可复用）与可选说明 `sourcePage` 组成，展示为 `编号-说明`（说明为空时只有编号），用于定位试卷/作业本的面，不是打印稿 pageId；题目卡片右侧提供编辑（铅笔，进入编辑页面并覆盖保存）与删除（归档，D-11）图标）、`:feature:planning`（计划生成与预览状态；`PlanSession` 本周计划单例：已有计划时加载同一 planId，只有显式“重新生成本周计划”才生成新计划并替代旧计划，C-097）、`:feature:print`（A4 打印布局、`PrintDocumentAdapter`、`PrintedPdfDocument`、`KnowledgeTextMeasurer` 实现，以及打印计划页 `PrintPlanScreen`/`PrintPlanViewModel`、`AndroidPrintLauncher` 与 `PrintSnapshotWriter`；打印文档写出后由用户在 App 内确认「已打印」再回写 `Question.pageId/qrPayload`（C-094 方案 B）；快照只为当前 GENERATED/PRINTED 计划写入，SUPERSEDED 计划不得覆盖最新快照（C-097）；页脚 QR 下方以 2.2mm 小字右对齐显示完整 `pageId`（与 QR 间距 2mm，即标签高度的一半），供扫码失败时手动回退（C-095 方案 B；2026-11-21 用户要求）；C-091 方案 A：生产模板用 `TemplateGeometry.a4Print()`，为四角标记预留 14mm 角区，内容区从 26mm 开始；C-092 方案 C：四角画 `DICT_4X4_50` 真实图案，图案以纯 Kotlin 常量固化，不把 OpenCV 带进 release）、`:feature:scan`（自研回读引擎：QR 载荷/CRC 校验、OMR 阈值判定、OpenCV ArUco 定位与 A4 透视、ML Kit 逐题 QR；扫描回读页 `ScanScreen`/`ScanViewModel`：CameraX 页内取景（A4 210:297 比例；`CAMERA` 权限，拒绝时相册选图；C-096 方案 B）或相册选图、QR 类失败开放手动输入 `pageId + 题号`（`ScanManualCodec`/`ScanManualEntryUseCase`）、`PAGE_*`/归属失败只提示重拍或重新打印、重复页/`LATE_BATCH`/未来日期提示、未提交会话恢复，成功后进入确认页；`ScanPageUseCase` 由 `pageId` 推导 planId）、`:feature:confirm`（确认页：纯 Kotlin `ConfirmStateMachine` 逐行标记接受/修正/本次未复习/补填，不提供一键全部接受；未标记行禁用提交；提交交给 `ApplyFeedbackUseCase`，整题跳过清单与已提交会话拒绝由 UseCase/Repository 保证）；其余 feature 随对应阶段创建。
core/common、core/model、core:scheduler、core:pagination 为纯 Kotlin，禁止 import android.*/androidx.*，只允许 Kotlin 标准库与 java.*（含 java.time）。`core:model` 只放纯 DTO/枚举与 `KnowledgeRowsCodec`（含 `Tier`、`KnowledgeState`、`SchedulerQuestion`，C-053）；打印题目投影 `RenderableQuestion` 属于 `core:pagination`，不在 `core:model` 重复定义（C-073 方案 A）；Room Entity ↔ DTO 的映射放在 `data:room`（C-046 方案 A）；计划持久化草稿 `PlanDraft` 与 `PlanGenerationException` 放在 `data:room`，编排层只构造入参与展示（C-074 方案 A）。计划生成返回 `PlanGenerationOutcome`：成功含 `PrintJob` 与预览投影，空计划含空态与提示；分页失败仍抛 `PlanGenerationException` 且不写库（C-047、C-060、C-090 方案 A）。确认提交的状态在 `data:room` 的 `ApplyFeedbackUseCase` 内计算并转 `Float`，写入反馈/决定日志/删除待确认由 `saveDecisions` 单事务负责，`ApplyResult` 携带 `SaveDecisionsResult`（C-075 方案 A）。确认前校验（会话 READY/未提交、待确认行完整、计划日与触发标记一致、知识点归属、复习日上下界、档位可用）由 `ApplyFeedbackUseCase` 承担，统一抛 `InvalidConfirmationException`（C-079 方案 A）；整题跳过仍写确认日志，但不写反馈、状态与题目复习日（C-078 方案 A、C-077 方案 A）；已提交会话拒绝重复提交（C-080 方案 A）。冷却顺延由 `PlanRepository.savePlan` 在计划保存的同一事务内按调度器给出的目标日期写入，不另设独立顺延用例（C-076 方案 A）。

## 冻结的架构决策（不得"优化"或"简化"）
D-01 调度单元是知识点；同一题目被多知识点触发只打印一次，触发知识点正常更新；非触发关联知识点按 0.3 权重更新连续掌握度增量，档位与下次到期日不变。非触发知识点也各占一行并独立填涂；有填涂时按该行自己的档位计算 ×0.3，未填涂（本次未复习）则不更新。
D-02 档位差至少一档仅作为同档位组内的排序权重（只比较本期到期点与非触发的、仍会打印的关联点之间的档位差，C-054；最高档位分组优先于档位差，C-059），不淘汰候选；所有到期题目都进入候选池。
D-03 连续掌握度仅用于同档位内排序与时间装箱；更新规则：掌握 +0.3、较熟悉 −0.1、不会 −0.3，夹在 [0,1]；不得替代档位比较。C-050：算术在 UseCase 内部用 `Double`，对外/落库转 `Float`；`effectiveFeedbackCount` 阈值精确 `>= 3.0f`，不做 epsilon。
D-04 冷却期优先于到期触发；固定 7 天（设置页不可修改；0 仅内部/测试表示关闭，负数非法，C-057），期间知识点顺延并标记"因冷却顺延"；用户手动覆盖后才可突破。覆盖采用确认页创建的一次性 CoolingOverrideEvent，plan() 通过 coolingOverrides 接收。冷却顺延由计划保存的同一事务原样写入（C-076 方案 A）。
D-05 冷启动固定阶梯 1/3/7 天；三档任意填涂均计有效反馈，"较熟悉"按 0.5 次计入；累计有效反馈 ≥ 3.0 后从下一次反馈起切换到墨墨背单词 DHP 简化模型：FAMILIAR 半强度、90% 保持率 + 30 天上限、新增 difficulty/halfLifeDays 字段；D-01～D-04 的排序/权重/冷却仍生效。
D-06 手写笔记 OCR 只存储和展示，不参与调度更新；原始 OCR 只读、修正文本可编辑，导出两者都含。
D-07 打印模板：左侧填涂区约 40%（一行一个知识点组，A/B/C 三框水平），右上题目信息区约 60%（页码、题号、知识点全称，不预留作答空白），右下笔记区约 60%（书写区+可选附加摘要）。图例"A—掌握 B—较熟悉 C—不会"仅文档首页出现一次，后续页不重复。知识点名称限一行；8–10 字截断只作用于左侧填涂区，右上保留全称并以" / "拼接、允许换行。左上/右上均按知识点编号升序；右上名称与左侧同一 printable 集合，数量等于知识点数量（由计划/数据层保证，C-067）。
D-08 按日强制分页，条目不得跨页；条目整条移至下页，绝不拆分；不再绘制"续下页"标签，分页结果中的续下页元数据只在内部使用。每个日期的第一页在日期大标题下方标注"本日共需复习 X 题"（X = 该日期实际打印的题目数，不含 overflow，C-063）；分页公式见 docs/specs/pagination.md。
D-09 回读顺序：ArUco 定位 → 透视矫正 → 每题 QR 解码 → 左侧填涂区 OMR 按行分组 → ML Kit 中文 OCR（仅笔记）→ 人工确认页。QR 含版本、planId、pageId、questionId、校验码，不含知识点 ID。QR 失败走"手动输入页面 ID + 题号"，确认页校验归属。页码即 pageId（格式 `{planId}-{pageNumber:02d}`，全局唯一）；planId 由计划层保证 6 位 Crockford Base32 且全局唯一，分页器只拼接、不校验（C-069）；校验码算法与解码/校验失败分支见 docs/specs/scan-pipeline.md。C-092 方案 C（取代 C-051 方案 A）：回读引擎自研——OpenCV 做 ArUco 定位与透视（输出 2480×3508）、ML Kit 做逐题 QR、OMR 按规格自采样；`ScanEngine` 适配层隔离 OpenCV/ML Kit（R-03）；真实打印稿 R-01 仍是硬门禁，PDF 光栅位图不得代替。C-093 方案 A：回读所需的题块与笔记区几何随打印快照持久化（`plan_page.itemGeometry`，schema v3），扫描端只读快照、不自行推导；无几何的旧稿按不可回读处理。C-096 方案 B：扫描页用 CameraX 1.7.0 做页内 A4 取景与拍摄，`CAMERA` 权限仅扫描页使用，拒绝时回退相册选图（取代系统相机 Intent + `FileProvider`）。
D-10 端侧推理；依赖固定；不引入云端 SDK。
D-11 归档条目退出调度，历史反馈保留为知识点背景数据；活跃题目计数为 0 时知识点休眠；取消归档重新激活。
D-12 单题关联知识点 ≤ 4；知识点编号全局唯一不复用；知识点、题目与关联 id 的唯一性由 `data:room` 保证，调度器不做去重/合并（C-056）。
D-13 回读确认页必须逐项标记：接受 / 修正 / 本次未复习 / 补填；"补填"计入有效反馈，"本次未复习"不计入。不得提供一键全部接受。

D-01～D-13 是本项目的冻结决策摘要；可执行定义分散在 `docs/specs/` 的对应规格（scheduler、feedback-usecase、data-model、print-template、pagination、scan-pipeline、confirm-flow、backup、entry、planning、repository、app、settings）。摘要与规格不一致时，以对应规格为准，并回写本摘要；修改决策需人类确认。独立约束 B-01 见 `docs/specs/backup.md`。

## 红线（每轮编码必须自查）
R-01 不得用 mock 或模拟数据充当识别验收证据；OMR/ML Kit 必须在真机或模拟器上跑通实际图像，证据存 evidence/。
R-02 分页决策只在 core/pagination；PrintDocumentAdapter 只消费映射，不在 onLayout 中猜测分页。
R-03 ViewModel 不得持有 Context 或直接调用 CameraX/PrintManager/ML Kit；这些封装在 Repository/UseCase。
R-04 回读结果先进 PendingFeedback，人工确认（含"本次未复习/补填"）后才写 ReviewFeedback 与知识点状态。"本次未复习"只写确认决定日志，不写 ReviewFeedback、不改知识点状态；确认流程必须包含该选项。
R-05 不引入设计决策外的新依赖；需要时写入 docs/dependency-proposals.md 等人类确认。
R-06 不得为适配实现而修改架构决策；冲突写入 docs/conflicts.md 等待裁决。

## Agent 工作循环（每轮必须遵循）
1. Observe：读取 CLAUDE.md 与本轮相关 docs/specs/ 文件；确认模块现状与未完成项。
2. Act：只改本轮目标模块；跨模块改动先报告并拆分；每文件改动记录到 docs/changelog.md（文件、意图、关联决策）。
3. Verify：纯 Kotlin 跑 JUnit；UI 通过实体机 + adb 执行 observe-act-verify，截图/UI 树存 evidence/；打印模块生成 PDF 后必须以 OpenScanVision 回读为硬门禁，人工比对几何仅作辅助记录，不能单独作为验收依据。（`android-emu-agent` 的 daemon 在原生 Windows 不可用，见 docs/agent-log/2026-11-13.md。）
4. Record：写入 docs/agent-log/YYYY-MM-DD.md（目标、改动、验证命令与结果、未决问题、下轮建议）。验证未通过不得声称完成。

## 验证命令速查
- 单元测试：./gradlew :core:common:test :core:model:test :core:scheduler:test :core:pagination:test
- 全量测试 + 纯度：./gradlew test verifyCorePurity
- 构建：./gradlew assembleDebug
- 设备验证（实体机）：adb devices -l；adb install -r app/build/outputs/apk/debug/app-debug.apk；adb shell uiautomator dump /sdcard/window.xml；adb exec-out screencap -p > evidence/screens/step.png
- Bash 超时：300000ms（已在 .claude/settings.json 配置）

## 冲突处理
发现规格与代码不一致时，不改规格、不自行"修正"设计；写入 docs/conflicts.md（哪条决策、现象、可选方案、建议），等待人类裁决。

冲突条目还必须包含"对用户的实际影响"：写清用户在复习顺序、当天能排入哪些题、哪些知识点被推迟、打印稿内容或顺序等方面会感知到什么差异。每个条目都要给出简短的选型参考，且理由必须建立在这些实际影响上，而不是架构偏好。描述尽量使用用户能感知的语言，少提内部变量名、类名和排序键；确需技术定位时，可在括号内附注。

## 决策落地后的文档同步（每完成一个决策必做）

每当人类完成一个决策（在 docs/conflicts.md 或对话中明确选定方案），当轮除实现与测试外，必须同步完成：

1) 归档与关闭：在 `docs/conflicts-archive.md` 记录裁决、落地位置、测试与规格同步状态；从 `docs/conflicts.md` 移除该未决条目。
2) `docs/prompts.md`：修订受影响小阶段的【本轮背景】与【本轮任务】，使后续轮次直接按新决策执行，不保留与裁决冲突的旧任务描述；同时为已完成的小节标记【done】。
3) 本文件（`CLAUDE.md`）与 `docs/architecture/overview.md`：修订 D-01～D-13 摘要、红线、模块约束、字段产权/多形态表或文档索引中与新决策相关的表述；摘要只保留可执行结论，细节仍指向 `docs/specs/`。
4) `docs/specs/`：仅在人类明确授权后回写；未获授权时在归档中注明“规格同步待授权”，不得自行修改规格。
5) `docs/changelog.md` 与 `docs/agent-log/YYYY-MM-DD.md`：记录本轮代码、`prompts.md`、`CLAUDE.md`、`overview.md` 与规格的改动。

不得只改代码或只改其中一份文档；代码、`prompts.md`、`CLAUDE.md` 与 `overview.md` 的表述必须与本轮裁决一致。

## 无需决策的一轮完成后的文档同步

每当一轮对话顺利完成而不需要人类进行决策，执行“决策落地后的文档同步”的第2至5条任务。

## 文档索引
规格：docs/specs/scheduler.md、feedback-usecase.md、pagination.md、data-model.md、entry.md、planning.md、repository.md、app.md、settings.md、print-template.md、scan-pipeline.md、confirm-flow.md、backup.md
决策摘要：本文件 D-01～D-13；可执行定义见 docs/specs/ 各规格
架构总览：docs/architecture/overview.md（三张图 + 字段产权表；Agent 在跨模块改动前必读）
研究稿：docs/research/maimemo-model.md（C-018，已确认）
TDD Prompts：docs/prompts.md
依赖提案：docs/dependency-proposals.md
前置验证报告：docs/preflight-report.md（尚未创建；见 docs/conflicts.md 的未决文档缺口）