# CLAUDE.md — 拾知 / Recollect

## 项目定位
面向初高中学生的 Android 原生纸数融合复习工具：手机录入题目与知识点 → 生成可打印 A4 周计划 → 纸质填涂执行 → 拍照回读经人工确认更新状态 → 再规划。调度单元是知识点，题目是复习载体。

## 技术栈（不得擅自变更）
- Kotlin + Jetpack Compose，minSdk 26，targetSdk 以 libs.versions.toml 为准
- Room 持久化；JDK 17；Gradle Wrapper 命令行构建
- ML Kit 中文离线 OCR：text-recognition-chinese:16.0.0 + vision-common:17.3.0
- OpenScanVision（JitPack 根坐标 `com.github.MatiwosKebede:openscanvision:1.0.0`；C-051 方案 A 适配层）负责 ArUco 定位、透视矫正、QR 解码、OMR 三档识别
- 全部推理端侧完成，不调用云端 AI/OCR，不使用 Gemini Nano
- 测试：纯 Kotlin 与 Android 单元测试用 JUnit 5；`androidTest` 用 JUnit 4（C-052 方案 C）。属性测试用 JUnit `@RepeatedTest` + `kotlin.random.Random` 手写（种子固定以保证可复现），不引入 jqwik/kotest 等属性测试库

## 模块结构与依赖方向
app → feature → data → core
core/common、core/model、core/scheduler、core/pagination 为纯 Kotlin，禁止 import android.*/androidx.*，只允许 Kotlin 标准库与 java.*（含 java.time）。`core:model` 只放纯 DTO/枚举与 `KnowledgeRowsCodec`；Room Entity ↔ DTO 的映射放在 `data:room`（C-046 方案 A）。

## 冻结的架构决策（不得"优化"或"简化"）
D-01 调度单元是知识点；同一题目被多知识点触发只打印一次，触发知识点正常更新；非触发关联知识点按 0.3 权重更新连续掌握度增量，档位与下次到期日不变。非触发知识点也各占一行并独立填涂；有填涂时按该行自己的档位计算 ×0.3，未填涂（本次未复习）则不更新。
D-02 档位差至少一档仅作为排序权重，不淘汰候选；所有到期题目都进入候选池。
D-03 连续掌握度仅用于同档位内排序与时间装箱；更新规则：掌握 +0.3、较熟悉 −0.1、不会 −0.3，夹在 [0,1]；不得替代档位比较。C-050：算术在 UseCase 内部用 `Double`，对外/落库转 `Float`；`effectiveFeedbackCount` 阈值精确 `>= 3.0f`，不做 epsilon。
D-04 冷却期优先于到期触发；默认 7 天，期间知识点顺延并标记"因冷却顺延"；用户手动覆盖后才可突破。覆盖采用确认页创建的一次性 CoolingOverrideEvent，plan() 通过 coolingOverrides 接收。
D-05 冷启动固定阶梯 1/3/7 天；三档任意填涂均计有效反馈，"较熟悉"按 0.5 次计入；累计有效反馈 ≥ 3.0 后从下一次反馈起切换到墨墨背单词 DHP 简化模型：FAMILIAR 半强度、90% 保持率 + 30 天上限、新增 difficulty/halfLifeDays 字段；D-01～D-04 的排序/权重/冷却仍生效。
D-06 手写笔记 OCR 只存储和展示，不参与调度更新；原始 OCR 只读、修正文本可编辑，导出两者都含。
D-07 打印模板：左侧填涂区约 40%（一行一个知识点组，A/B/C 三框水平），右上题目信息区约 60%（页码、题号、知识点全称，不预留作答空白），右下笔记区约 60%（书写区+可选附加摘要）。图例"A—掌握 B—较熟悉 C—不会"仅文档首页出现一次，后续页不重复。知识点名称限一行；8–10 字截断只作用于左侧填涂区，右上保留全称并以" / "拼接、允许换行。左上/右上均按知识点编号升序。
D-08 按日强制分页，条目不得跨页；条目整条移至下页，绝不拆分；每移一条在当页留白区底部加一个"续下页"标记，按题目顺序从上到下堆叠，不画填涂框；分页公式见 docs/specs/pagination.md。
D-09 回读顺序：ArUco 定位 → 透视矫正 → 每题 QR 解码 → 左侧填涂区 OMR 按行分组 → ML Kit 中文 OCR（仅笔记）→ 人工确认页。QR 含版本、planId、pageId、questionId、校验码，不含知识点 ID。QR 失败走"手动输入页面 ID + 题号"，确认页校验归属。页码即 pageId（格式 `{planId}-{pageNumber:02d}`，全局唯一）；校验码算法与解码/校验失败分支见 docs/specs/scan-pipeline.md。C-051 方案 A：OpenScanVision 根坐标 + `ScanEngine` 适配层；850×540 → 2480×3508 的精度以真实打印稿 R-01 为准，不达标转方案 B。
D-10 端侧推理；依赖固定；不引入云端 SDK。
D-11 归档条目退出调度，历史反馈保留为知识点背景数据；活跃题目计数为 0 时知识点休眠；取消归档重新激活。
D-12 单题关联知识点 ≤ 4；知识点编号全局唯一不复用。
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

## 文档索引
规格：docs/specs/scheduler.md、feedback-usecase.md、pagination.md、data-model.md、entry.md、planning.md、repository.md、app.md、settings.md、print-template.md、scan-pipeline.md、confirm-flow.md、backup.md
决策摘要：本文件 D-01～D-13；可执行定义见 docs/specs/ 各规格
架构总览：docs/architecture/overview.md（三张图 + 字段产权表；Agent 在跨模块改动前必读）
研究稿：docs/research/maimemo-model.md（C-018，已确认）
TDD Prompts：docs/prompts.md
依赖提案：docs/dependency-proposals.md
前置验证报告：docs/preflight-report.md