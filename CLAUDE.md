# CLAUDE.md — 拾知 / Recollect

## 项目定位
面向初高中学生的 Android 原生纸数融合复习工具：手机录入题目与知识点 → 生成可打印 A4 周计划 → 纸质填涂执行 → 拍照回读经人工确认更新状态 → 再规划。调度单元是知识点，题目是复习载体。

- 开源许可证：`GPL-3.0-only WITH Classpath-exception-2.0`（`LICENSE` + `LICENSE-EXCEPTION` + `NOTICE`）；衍生分发必须同样开源，闭源上架应用商店违反许可证。三份文本同时打包在 `app/src/main/assets/licenses/` 供 App 内展示，修改根目录文件时必须同步该副本。

## 技术栈（不得擅自变更）
- Kotlin + Jetpack Compose，minSdk 26，targetSdk 以 libs.versions.toml 为准
- Room 持久化；JDK 17；Gradle Wrapper 命令行构建；release 开启 R8 代码压缩与资源裁剪，debug 不压缩；只打包 `arm64-v8a`/`armeabi-v7a`（去掉模拟器 x86/x86_64 原生库）；`debug_compile_and_install.bat` 构建 debug APK 并安装到实机；`release_compile_and_install.bat` 构建 minify 后的 release APK 并安装到实机（签名读取仓库外 `keystore.properties` 中的正式 keystore，缺失时回退 debug 签名）
- ML Kit 中文离线 OCR：text-recognition-chinese:16.0.0 + vision-common:17.3.0
- 扫描页相机：CameraX 1.7.0（camera-core/camera2/lifecycle/view；C-096 方案 B），仅扫描页使用 `CAMERA` 权限，拒绝时回退相册选图
- 回读引擎（C-092 方案 C，自研 `ScanEngine`）：ArUco 定位与透视矫正用 OpenCV（`com.quickbirdstudios:opencv-contrib:4.5.3.0`），逐题 QR 用 ML Kit `barcode-scanning:17.2.0`，OMR 三档识别按 `docs/specs/scan-pipeline.md` 的几何与阈值自行采样；打印层每题二维码用 ZXing core 3.5.3 生成（版本 5、纠错 M），四角 ArUco 图案以纯 Kotlin 常量绘制；`openscanvision:1.0.0` 已停用（坐标保留供 spike 复现）
- 全部推理端侧完成，不调用云端 AI/OCR，不使用 Gemini Nano
- 测试：纯 Kotlin 与 Android 单元测试用 JUnit 5；`androidTest` 用 JUnit 4（C-052 方案 C）。属性测试用 JUnit `@RepeatedTest` + `kotlin.random.Random` 手写（种子固定以保证可复现），不引入 jqwik/kotest 等属性测试库

## 模块结构与依赖方向
app → feature → data → core
现有功能模块：`:feature:entry`（Entry 空态与题目录入；知识点以标签库形式维护，1 题从标签库选择 1～4 个标签，不再输入自由文本；标签库支持新增/重命名/删除，删除前校验未被未归档题目引用并写编号墓碑；录入页内以全屏窗口管理知识点（左上角关闭叉，关闭后只刷新候选、保留未保存表单）；新建/编辑使用独立页面（`QuestionEditScreen`，由悬浮按钮或卡片铅笔进入），题目列表占满页面，支持按题号、来源页码或知识点关键词搜索（2026-11-22 用户要求；关键词大小写不敏感、空查询显示全部，搜索条件在进入编辑页再返回后保留）；保存成功后先以带对勾 Toast 提示「保存成功」，再按入口分流——从首页「录入题目」进入时替换当前页强制跳到题库页（返回键回到首页），从题库页进入时关闭当前页返回题库并保留列表滚动位置（2026-11-24 用户要求）；所有手动输入文本框在键盘可见期间隐藏 Compose 光标/选区手柄（手柄是独立窗口里的 Popup，不会随系统整窗平移或 `imePadding` 滚动重新锚定，2026-11-24 用户要求），键盘收起后恢复显示且位置正确；进入录入/编辑题目页时把窗口软输入模式设为 `adjustResize`（离开时恢复 `adjustPan`），避免系统整窗平移与表单 `imePadding` 叠加在输入框下方留下空白（2026-11-24 用户要求）；来源页码由用户可调的编号 `sourcePageCode`（选中来源时可数字键盘输入，无来源时只读并仅用 +/−；默认值按「有无来源」分别记忆，切换来源且未手改时套用对应默认值，同页可复用；字段标题随来源切换——未选来源时显示「试卷编号」，选中来源后显示「页码编号」，2026-11-24 用户要求）与可选的来源标签 `sourcePage` 组成（来源与知识点一样单选标签，标签库 `source_tag` 于 schema v9 落地，在录入页内以全屏窗口增删改查，重命名同步题目引用、被引用不可删），展示为 `编号-来源`（来源为空时只有编号），用于定位试卷/作业本的面，不是打印稿 pageId；题库卡片依次展示题号、页码、知识点与「预估复习时长：X分钟」；题库与知识点标签库均按用户修改时间倒序展示，新增或编辑后条目立即置顶（`Question.updatedAt` / `KnowledgePoint.updatedAt`，schema v14；2026-11-24 用户要求）；题目卡片右侧提供编辑（铅笔，进入编辑页面并覆盖保存）与删除（归档，D-11）图标）、`:feature:progress`（复习进度页：只读聚合未归档题目与关联知识点状态，展示题目总数/逾期/今天/7 天内统计、未来 14 天到期分布、逐题掌握度与到期日；聚合逻辑在纯 Kotlin `ReviewProgressAggregator`，首页以 2×2 快捷操作卡片提供入口（与录入题目、管理题库、扫描回读并列，2026-11-22 用户要求；2026-11-24 起首页不再提供「管理知识点」「管理题目来源」入口，2026-11-24 用户要求）、`:feature:planning`（计划生成与预览状态；`PlanSession` 本周计划单例：已有计划时加载同一 planId，只有显式“重新生成本周计划”才生成新计划并替代旧计划，C-097；首页提供本周与下周两个计划窗口，`currentWeekSettings` 取今天所在自然周、`nextWeekSettings` 紧随其后，只允许这两周，不允许生成更远的周，C-098；计划的所有页都确认提交后在同一事务内迁移为 COMPLETED（C-099），首页据此把下周计划升为卡片主区块、本周降为次要行（C-100，本周 COMPLETED 时切换）；计划预览页与“未排入计划”卡片统一用「页码-第X题」标识题目（未标注来源页码时只显示「第X题」，避免不同页面的相同题号无法区分），并显示被推迟题目的原因（容量不足/冷却/知识点缺失）；推迟快照随计划以 `plan.deferred` 冻结（schema v11，含题号与来源页码），重新打开计划与强停重开后仍可见，2026-11-23 用户要求）、`:feature:print`（A4 打印布局、`PrintDocumentAdapter`、`PrintedPdfDocument`、`KnowledgeTextMeasurer` 实现，以及打印计划页 `PrintPlanScreen`/`PrintPlanViewModel`、`AndroidPrintLauncher` 与 `PrintSnapshotWriter`；打印文档写出后由用户在 App 内确认「已打印」再回写 `Question.pageId/qrPayload`（C-094 方案 B）；2026-11-24 用户要求：打印页新增「分享 PDF」，经 `PlanPdfSharer` 端口（实现 `AndroidPlanPdfSharer` 在 app 层）把同一份 A4 布局写成缓存 PDF，经 `FileProvider` 以 `application/pdf` 打开系统分享面板，可发送到微信、QQ 等；分享成功后与「已打印」走同一 `PrintSnapshotWriter` 回写路径（写 `Question.pageId/qrPayload` 并把 GENERATED 计划标记为 PRINTED），首页状态标签随之更新，SUPERSEDED 计划不写；快照只为当前 GENERATED/PRINTED 计划写入，SUPERSEDED 计划不得覆盖最新快照（C-097）；页脚 QR 下方以 2.2mm 小字右对齐显示完整 `pageId`（与 QR 间距 2mm，即标签高度的一半），供扫码失败时手动回退（C-095 方案 B；2026-11-21 用户要求）；2026-11-24 用户要求删除题目右上第一行的「页码 XX」，纸面只保留题号与来源页（QR 载荷与页脚 pageId 不变）；C-091 方案 A：生产模板用 `TemplateGeometry.a4Print()`，为四角标记预留 14mm 角区，内容区从 26mm 开始；C-092 方案 C：四角画 `DICT_4X4_50` 真实图案，图案以纯 Kotlin 常量固化，不把 OpenCV 带进 release；2026-11-22 用户要求填涂格改为答题卡式 16:9 宽格 5.7×3.2mm，`TemplateGeometry` 统一序号 6mm / 知识点 34mm / 列间距 1.5mm / 框间距 3.1mm（左侧占比 0.42），打印与扫描共用；同日删除首页图例、把框上方 A/B/C 改为每页「掌握 / 较熟悉 / 不会」并删除「本日共需复习 X 题」）、`:feature:scan`（自研回读引擎：QR 载荷/CRC 校验、OMR 阈值判定、OpenCV ArUco 定位与 A4 透视、ML Kit 逐题 QR；扫描回读页 `ScanScreen`/`ScanViewModel`：CameraX 页内取景（A4 210:297 比例；`CAMERA` 权限，拒绝时相册选图；C-096 方案 B）或相册选图、QR 类失败开放手动输入 `pageId + 题号`（`ScanManualCodec`/`ScanManualEntryUseCase`）、`PAGE_*`/归属失败只提示重拍或重新打印、重复页/`LATE_BATCH`/未来日期提示、未提交会话恢复，成功后进入确认页；`ScanPageUseCase` 由 `pageId` 推导 planId）、`:feature:confirm`（确认页：纯 Kotlin `ConfirmStateMachine` 逐行标记接受/修改复习效果/本次未复习，不提供一键全部接受；未标记行禁用提交；提交交给 `ApplyFeedbackUseCase`，整题跳过清单与已提交会话拒绝由 UseCase/Repository 保证；2026-11-22 用户要求卡片直接展示「掌握/较熟悉/不会」识别结果，并只保留一个默认填入 OCR 原文的可编辑文本框；同日取消「修正/补填」区分，`isSupplemental` 从数据模型删除；单行完成决策后锁定校对文本框与该行按钮，并在按钮行右侧显示对勾；「本次未复习」后复习效果显示为「复习效果：本次未复习」）、`:feature:backup`（导出/导入数据库：Home 顶部导航栏右侧为「导入数据库」+「导出数据库」，两者图标已按 2026-11-22 用户要求互换——导入显示下载图形、导出显示上传图形。导出页主按钮「导出并分享」把白名单实体导出为 JSON、写入应用缓存并经 `FileProvider` 打开系统分享面板，完成后只展示文件名/大小/格式版本与分享结果；导入由导航栏「导入数据库」或系统「用其他方式打开」json 文件（`ACTION_VIEW` + `application/json`，标签「导入到拾知数据库」）触发，SAF/外部 URI 读取后校验：不合法弹「导入失败」，合法弹「将覆盖当前数据，是否继续？」，确认后单事务清空白名单表及 `PendingFeedback`/`ScanSession`/`CoolingOverrideEvent` 并写入，失败整体回滚，成功以带对勾 Toast「导入成功」并让首页统计、题库、知识点、来源标签、复习进度与计划预览的数据缓存全部失效重读（计划打印交接同时清空）；跨 `roomSchemaVersion` 按 backup.md 字段矩阵降级，`backupFormatVersion` 高于当前拒绝；导入上限 20MB，2026-11-22 用户要求）；2026-11-23 用户要求：首页去掉「快捷操作」标签，底部卡片栏左侧为时钟图标 + 左对齐「每日复习时长」（悬浮窗修改，输入框下附说明），右侧为信息图标「关于」进入关于页（应用图标/名称/版本/作者 `@ziyangdev910`/开源仓库/开源许可证（GPL-3.0 with Classpath Exception，可进入全文页）/功能说明/致谢：记忆调度算法受 SSP-MMC-Plus 论文启发并附引用（DOI 可点击打开网页；DHP 公式参考 MIT 许可的 SSP-MMC 实现），2026-11-24 用户要求）。「每日复习时长」持久化在 Room 单行表 `app_settings`（schema v12，默认 60 分钟，1～240），随备份导出/导入，并铺满下一份计划的每一天；2026-11-23 用户要求：知识点增加上层分类（schema v13 `knowledge_category`，默认 11 个：语文、数学、英语、物理、化学、生物、政治、历史、地理、信息技术、通用技术，可增删改），新增/编辑知识点必须指定分类，分类名唯一、有知识点的分类不可删；旧库与旧备份的知识点统一归入「语文」；「知识点」页与「录入题目」的知识点标签区共用横向分类筛选选项卡（首项「全部」），「知识点」页把筛选卡放在「共 N 个标签」与「新增标签」按钮之上并缩小其与顶部导航栏的间距（2026-11-24 用户要求删去该页与「来源标签」页的搜索框）；schema v14 给 `Question` 与 `KnowledgePoint` 增加 `updatedAt`（用户新建/编辑时间，迁移时旧数据回填为 `createdAt`），题库与知识点列表据此倒序展示，复习调度、打印快照、归档等系统写回不改变该值（2026-11-24 用户要求）；其余 feature 随对应阶段创建。
core/common、core/model、core:scheduler、core:pagination 为纯 Kotlin，禁止 import android.*/androidx.*，只允许 Kotlin 标准库与 java.*（含 java.time）。`core:model` 只放纯 DTO/枚举与 `KnowledgeRowsCodec`（含 `Tier`、`KnowledgeState`、`SchedulerQuestion`，C-053）；打印题目投影 `RenderableQuestion` 属于 `core:pagination`，不在 `core:model` 重复定义（C-073 方案 A）；Room Entity ↔ DTO 的映射放在 `data:room`（C-046 方案 A）；计划持久化草稿 `PlanDraft` 与 `PlanGenerationException` 放在 `data:room`，编排层只构造入参与展示（C-074 方案 A）。计划生成返回 `PlanGenerationOutcome`：成功含 `PrintJob` 与预览投影，空计划含空态与提示；分页失败仍抛 `PlanGenerationException` 且不写库（C-047、C-060、C-090 方案 A）。确认提交的状态在 `data:room` 的 `ApplyFeedbackUseCase` 内计算并转 `Float`，写入反馈/决定日志/删除待确认由 `saveDecisions` 单事务负责，`ApplyResult` 携带 `SaveDecisionsResult`（C-075 方案 A）。确认前校验（会话 READY/未提交、待确认行完整、计划日与触发标记一致、知识点归属、复习日上下界、档位可用）由 `ApplyFeedbackUseCase` 承担，统一抛 `InvalidConfirmationException`（C-079 方案 A）；整题跳过仍写确认日志，但不写反馈、状态与题目复习日（C-078 方案 A、C-077 方案 A）；已提交会话拒绝重复提交（C-080 方案 A）。冷却顺延由 `PlanRepository.savePlan` 在计划保存的同一事务内按调度器给出的目标日期写入，不另设独立顺延用例（C-076 方案 A）。

## 冻结的架构决策（不得"优化"或"简化"）
D-01 调度单元是知识点；同一题目被多知识点触发只打印一次，触发知识点正常更新；非触发关联知识点按 0.3 权重更新连续掌握度增量，档位与下次到期日不变。非触发知识点也各占一行并独立填涂；有填涂时按该行自己的档位计算 ×0.3，未填涂（本次未复习）则不更新。
D-02 档位差至少一档仅作为同档位组内的排序权重（只比较本期到期点与非触发的、仍会打印的关联点之间的档位差，C-054；最高档位分组优先于档位差，C-059），不淘汰候选；所有到期题目都进入候选池。
D-03 连续掌握度仅用于同档位内排序与时间装箱；更新规则：掌握 +0.3、较熟悉 −0.1、不会 −0.3，夹在 [0,1]；不得替代档位比较。C-050：算术在 UseCase 内部用 `Double`，对外/落库转 `Float`；`effectiveFeedbackCount` 阈值精确 `>= 3.0f`，不做 epsilon。
D-04 冷却期优先于到期触发；固定 7 天（设置页不可修改；0 仅内部/测试表示关闭，负数非法，C-057），期间知识点顺延并标记"因冷却顺延"；用户手动覆盖后才可突破。覆盖采用确认页创建的一次性 CoolingOverrideEvent，plan() 通过 coolingOverrides 接收。冷却顺延由计划保存的同一事务原样写入（C-076 方案 A）。
D-05 冷启动固定阶梯 1/3/7 天；三档任意填涂均计有效反馈，"较熟悉"按 0.5 次计入；累计有效反馈 ≥ 3.0 后从下一次反馈起切换到墨墨背单词 DHP 简化模型：FAMILIAR 半强度、90% 保持率 + 30 天上限、新增 difficulty/halfLifeDays 字段；D-01～D-04 的排序/权重/冷却仍生效。
D-06 手写笔记 OCR 只存储和展示，不参与调度更新；原始 OCR 只读、修正文本可编辑，导出两者都含。
D-07 打印模板：左侧填涂区约 40%（一行一个知识点组，掌握/较熟悉/不会三框水平），右上题目信息区约 60%（题号、来源页、知识点全称，不预留作答空白；2026-11-24 用户要求删除「页码 XX」），右下笔记区约 60%（书写区+可选附加摘要）。填涂框上方每页直接标注"掌握 较熟悉 不会"（分别与三框中心对齐，并贴近该页第一道题），不再绘制 A/B/C 图例。知识点名称限一行；8–10 字截断只作用于左侧填涂区，右上保留全称并以" / "拼接、允许换行。左上/右上均按知识点编号升序；右上名称与左侧同一 printable 集合，数量等于知识点数量（由计划/数据层保证，C-067）。
D-08 按日强制分页，条目不得跨页；条目整条移至下页，绝不拆分；不再绘制"续下页"标签，分页结果中的续下页元数据只在内部使用。已删除"本日共需复习 X 题"，只保留日期大标题；每页容量相同（C-063）；分页公式见 docs/specs/pagination.md。
D-09 回读顺序：ArUco 定位 → 透视矫正 → 每题 QR 解码 → 左侧填涂区 OMR 按行分组 → ML Kit 中文 OCR（仅笔记）→ 人工确认页。QR 含版本、planId、pageId、questionId、校验码，不含知识点 ID。QR 失败走"手动输入页面 ID + 题号"，确认页校验归属。页码即 pageId（格式 `{planId}-{pageNumber:02d}`，全局唯一）；planId 由计划层保证 6 位 Crockford Base32 且全局唯一，分页器只拼接、不校验（C-069）；校验码算法与解码/校验失败分支见 docs/specs/scan-pipeline.md。C-092 方案 C（取代 C-051 方案 A）：回读引擎自研——OpenCV 做 ArUco 定位与透视（输出 2480×3508）、ML Kit 做逐题 QR、OMR 按规格自采样，填涂列中心直接取 `TemplateGeometry`（与打印共用，2026-11-22）；行判定以唯一明确命中优先：恰好 1 个 >30% 命中即判该档，其余框的 10%–30% 模糊读数不推翻它（2026-11-22 修复低分辨率扫描误判 `UNREADABLE`）；`ScanEngine` 适配层隔离 OpenCV/ML Kit（R-03）；真实打印稿 R-01 仍是硬门禁，PDF 光栅位图不得代替。C-093 方案 A：回读所需的题块与笔记区几何随打印快照持久化（`plan_page.itemGeometry`，schema v3），扫描端只读快照、不自行推导；无几何的旧稿按不可回读处理。C-096 方案 B：扫描页用 CameraX 1.7.0 做页内 A4 取景与拍摄，`CAMERA` 权限仅扫描页使用，拒绝时回退相册选图（取代系统相机 Intent + `FileProvider`）。
D-10 端侧推理；依赖固定；不引入云端 SDK。
D-11 归档条目退出调度，历史反馈保留为知识点背景数据；活跃题目计数为 0 时知识点休眠；取消归档重新激活。
D-12 单题关联知识点 ≤ 4；知识点编号全局唯一不复用；知识点、题目与关联 id 的唯一性由 `data:room` 保证，调度器不做去重/合并（C-056）。
D-13 回读确认页必须逐项标记：接受 / 修改复习效果 / 本次未复习；"修改复习效果"计入有效反馈，"本次未复习"不计入。不得提供一键全部接受。2026-11-22 用户要求取消「修正」与「补填」的区分，`isSupplemental` 退出数据模型。

D-01～D-13 是本项目的冻结决策摘要；可执行定义分散在 `docs/specs/` 的对应规格（scheduler、feedback-usecase、data-model、print-template、pagination、scan-pipeline、confirm-flow、backup、entry、planning、repository、app、settings）。摘要与规格不一致时，以对应规格为准，并回写本摘要；修改决策需人类确认。独立约束 B-01 见 `docs/specs/backup.md`。

## 红线（每轮编码必须自查）
R-01 不得用 mock 或模拟数据充当识别验收证据；OMR/ML Kit 必须在真机或模拟器上跑通实际图像，证据存 evidence/。
R-02 分页决策只在 core/pagination；PrintDocumentAdapter 只消费映射，不在 onLayout 中猜测分页。
R-03 ViewModel 不得持有 Context 或直接调用 CameraX/PrintManager/ML Kit；这些封装在 Repository/UseCase。
R-04 回读结果先进 PendingFeedback，人工确认（含"本次未复习"）后才写 ReviewFeedback 与知识点状态。"本次未复习"只写确认决定日志，不写 ReviewFeedback、不改知识点状态；确认流程必须包含该选项。
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