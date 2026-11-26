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
现有功能模块：`:feature:entry`（Entry 空态与题目录入；知识点以标签库形式维护，1 题从标签库选择 1～4 个标签，不再输入自由文本；标签库支持新增/重命名/删除，删除前校验未被未归档题目引用并写编号墓碑；录入页内以全屏窗口管理知识点（左上角关闭叉，关闭后只刷新候选、保留未保存表单）；新建/编辑使用独立页面（`QuestionEditScreen`，由悬浮按钮或卡片铅笔进入），题目列表占满页面，支持按题号、来源页码或知识点关键词搜索（2026-11-22 用户要求；关键词大小写不敏感、空查询显示全部，搜索条件在进入编辑页再返回后保留）；保存成功后先以带对勾 Toast 提示「保存成功」，再按入口分流——从首页「录入题目」进入时替换当前页强制跳到题库页（返回键回到首页），从题库页进入时关闭当前页返回题库并保留列表滚动位置（2026-11-24 用户要求）；题库页右下角纵向排列「录入题目」与「预览计划」两个按钮，已有题目时「录入题目」淡化、「预览计划」高亮（2026-11-26 用户要求）；所有手动输入文本框始终隐藏 Compose 光标/选区手柄（光标下方的小三角/水滴）：应用级把 `LocalTextSelectionColors.handleColor` 置为透明，覆盖表单、搜索框与全部弹窗输入框；手柄是独立窗口里的 Popup，不会随系统整窗平移或 `imePadding` 滚动重新锚定，2026-11-25 用户要求彻底移除（取代 2026-11-24 的「仅键盘可见期间隐藏」）；进入录入/编辑题目页时把窗口软输入模式设为 `adjustResize`（离开时恢复 `adjustPan`），避免系统整窗平移与表单 `imePadding` 叠加在输入框下方留下空白（2026-11-24 用户要求）；来源页码由用户可调的编号 `sourcePageCode`（选中来源时可数字键盘输入，无来源时只读并仅用 +/−；默认值按「有无来源」分别记忆，切换来源且未手改时套用对应默认值，同页可复用；字段标题随来源切换——未选来源时显示「试卷编号」，选中来源后显示「页码编号」，2026-11-24 用户要求）与可选的来源标签 `sourcePage` 组成（来源与知识点一样单选标签，标签库 `source_tag` 于 schema v9 落地，在录入页内以全屏窗口增删改查，重命名同步题目引用、被引用不可删），展示为 `编号-来源`（来源为空时只有编号），用于定位试卷/作业本的面，不是打印稿 pageId；题库卡片以两行大标题展示「来源」与「第X页 第Y题」（无来源说明时第一行为「编号X」），下方把「X 分钟」耗时标签与知识点标签并排（2026-11-25 用户要求）；题库与知识点标签库均按用户修改时间倒序展示，新增或编辑后条目立即置顶（`Question.updatedAt` / `KnowledgePoint.updatedAt`，schema v14；2026-11-24 用户要求）；题目卡片右侧提供编辑（铅笔，进入编辑页面并覆盖保存）与删除（归档，D-11）图标）、`:feature:progress`（复习进度页：只读聚合未归档题目与关联知识点状态，展示题目总数/逾期/今天/7 天内统计、未来 14 天到期分布、逐题掌握度与到期日；聚合逻辑在纯 Kotlin `ReviewProgressAggregator`，首页从「学习概览」标题右侧的透明文字按钮进入（2026-11-26 用户要求，取代 2026-11-22 的 2×2 快捷卡片入口；2026-11-24 起首页不再提供「管理知识点」「管理题目来源」入口，2026-11-24 用户要求）、`:feature:planning`（计划生成与预览状态；`PlanSession` 本周计划单例：已有计划时加载同一 planId，只有显式“重新生成本周计划”才生成新计划并替代旧计划，C-097；计划预览页顶部提供「本周计划 / 下周计划」两个选项，切换时加载该窗口已有计划、没有则生成；进入时本周计划 COMPLETED 则默认选中下周、否则本周；打开已有计划且题库在生成后新增/编辑、或计划内的题目被归档时，弹出「题库已更新，是否重新生成计划？」（`PlanSession.activePlan`、`StoredPlan.createdAt`、`EntrySummary.updatedAt`；不用全部题库与计划题目做集合比较，避免未到期题目误报）；`currentWeekSettings` 取今天所在自然周、`nextWeekSettings` 紧随其后，只允许这两周，不允许生成更远的周，C-098；计划的所有页都确认提交后在同一事务内迁移为 COMPLETED（C-099）；首页不再显示本周/下周计划卡（2026-11-26 用户要求）；计划预览页与“未排入计划”卡片统一用「来源 第X页 第Y题」标识题目（2026-11-25 用户要求：X 为页码编号、Y 为题号；没有来源说明时显示「编号X 第Y题」，缺少页码时只拼接已有部分），并显示被推迟题目的原因（容量不足/冷却/知识点缺失）；推迟快照随计划以 `plan.deferred` 冻结（schema v11，含题号与来源页码），重新打开计划与强停重开后仍可见，2026-11-23 用户要求）、`:feature:print`（A4 打印布局、`PrintDocumentAdapter`、`PrintedPdfDocument`、`KnowledgeTextMeasurer` 实现，以及打印计划页 `PrintPlanScreen`/`PrintPlanViewModel`、`AndroidPrintLauncher` 与 `PrintSnapshotWriter`；打印文档写出后由用户在 App 内确认「已打印」再回写 `Question.pageId/qrPayload`（C-094 方案 B）；2026-11-24 用户要求：打印页新增「分享 PDF」，经 `PlanPdfSharer` 端口（实现 `AndroidPlanPdfSharer` 在 app 层）把同一份 A4 布局写成缓存 PDF，经 `FileProvider` 以 `application/pdf` 打开系统分享面板，可发送到微信、QQ 等；分享成功后与「已打印」走同一 `PrintSnapshotWriter` 回写路径（写 `Question.pageId/qrPayload` 并把 GENERATED 计划标记为 PRINTED），首页状态标签随之更新，SUPERSEDED 计划不写；确认「已打印」写回成功后退回首页并提示「打印完成」，分享 PDF 在页面重新获得焦点且快照写入成功后退回首页并提示「打印完成」（2026-11-26 用户要求）；快照只为当前 GENERATED/PRINTED 计划写入，SUPERSEDED 计划不得覆盖最新快照（C-097）；2026-11-24 用户要求：每题排版改为左上方填涂区、右上方题号/来源/知识点，下方通栏先显示历史笔记、再是复习笔记书写区，QR 与 pageId 标签嵌在书写区右侧并排（书写区不占满宽度，题块不再有独立 QR 页脚带，空间利用率提高；无笔记区时 QR 仍落在题块底部预留带）；QR 下方以 2.2mm 小字右对齐显示完整 `pageId`（与 QR 间距 2mm，即标签高度的一半），供扫码失败时手动回退（C-095 方案 B；2026-11-21 用户要求）；2026-11-24 用户要求删除题目右上第一行的「页码 XX」；2026-11-25 用户要求把该行改为与计划预览一致的「来源 第X页 第Y题」（X 为页码编号、Y 为题号；没有来源说明时页码显示为「编号X」），QR 载荷与页脚 pageId 不变；C-091 方案 A：生产模板用 `TemplateGeometry.a4Print()`，为四角标记预留 14mm 角区，内容区从 26mm 开始；C-092 方案 C：四角画 `DICT_4X4_50` 真实图案，图案以纯 Kotlin 常量固化，不把 OpenCV 带进 release；2026-11-22 用户要求填涂格改为答题卡式 16:9 宽格 5.7×3.2mm，`TemplateGeometry` 统一序号 6mm / 知识点 34mm / 列间距 1.5mm / 框间距 3.1mm（左侧占比 0.42），打印与扫描共用；同日删除首页图例、把框上方 A/B/C 改为每页「掌握 / 较熟悉 / 不会」并删除「本日共需复习 X 题」）、`:feature:scan`（自研回读引擎：QR 载荷/CRC 校验、OMR 阈值判定、OpenCV ArUco 定位与 A4 透视、ML Kit 逐题 QR；扫描回读页 `ScanScreen`/`ScanViewModel`：CameraX 页内取景（A4 210:297 比例；`CAMERA` 权限，拒绝时相册选图；C-096 方案 B）或相册选图、QR 类失败开放手动输入 `pageId + 题号`（`ScanManualCodec`/`ScanManualEntryUseCase`）、`PAGE_*`/归属失败只提示重拍或重新打印、重复页/`LATE_BATCH`/未来日期提示、未提交会话恢复，成功后进入确认页；`ScanPageUseCase` 由 `pageId` 推导 planId）、`:feature:confirm`（确认页：纯 Kotlin `ConfirmStateMachine` 逐行标记接受/修改复习效果/本次未复习，不提供一键全部接受；未标记行禁用提交；提交交给 `ApplyFeedbackUseCase`，整题跳过清单与已提交会话拒绝由 UseCase/Repository 保证；2026-11-22 用户要求卡片直接展示「掌握/较熟悉/不会」识别结果，并只保留一个默认填入 OCR 原文的可编辑文本框；同日取消「修正/补填」区分，`isSupplemental` 从数据模型删除；单行完成决策后锁定校对文本框与该行按钮，并在按钮行右侧显示对勾；「本次未复习」后复习效果显示为「复习效果：本次未复习」；整批提交成功后退回首页并提示「确认完成」，2026-11-26 用户要求）、`:feature:backup`（导出/导入数据库：入口在 Home「设置」面板底部，与「关于」并排的「导入数据库」「导出数据库」两个等宽入口（各占面板宽度三分之一；2026-11-26 用户要求从顶部导航栏移入），两者图标已按 2026-11-22 用户要求互换——导入显示下载图形、导出显示上传图形。导出以设置风格悬浮窗呈现（2026-11-26 用户要求，Material 3 `AlertDialog` 悬浮在首页上方，底部「关闭」与系统返回键均可关闭，不显示返回图标，不进入路由返回栈，`AppRoute.Backup` 已删除），主按钮「导出并分享」把白名单实体导出为 JSON、写入应用缓存并经 `FileProvider` 打开系统分享面板，完成后只展示文件名/大小/格式版本与分享结果；导入由「导入数据库」入口或系统「用其他方式打开」json 文件（`ACTION_VIEW` + `application/json`，标签「导入到拾知数据库」）触发，SAF/外部 URI 读取后校验：不合法弹「导入失败」，合法弹「将覆盖当前数据，是否继续？」，确认后单事务清空白名单表及 `PendingFeedback`/`ScanSession`/`CoolingOverrideEvent` 并写入，失败整体回滚，成功以带对勾 Toast「导入成功」并让首页统计、题库、知识点、来源标签、复习进度与计划预览的数据缓存全部失效重读（计划打印交接同时清空）；跨 `roomSchemaVersion` 按 backup.md 字段矩阵降级，`backupFormatVersion` 高于当前拒绝；导入上限 20MB，2026-11-22 用户要求）；2026-11-23 用户要求：首页去掉「快捷操作」标签，底部卡片栏左侧为时钟图标 + 左对齐「每日复习时长」（悬浮窗修改，输入框下附说明），右侧为信息图标「关于」进入关于页（应用图标/名称/版本/作者 `@ziyangdev910`/开源仓库/开源许可证（GPL-3.0 with Classpath Exception，可进入全文页）/功能说明/致谢：记忆调度算法受 SSP-MMC-Plus 论文启发并附引用（DOI 可点击打开网页；DHP 公式参考 MIT 许可的 SSP-MMC 实现），2026-11-24 用户要求）。「每日复习时长」持久化在 Room 单行表 `app_settings`（schema v12，默认 60 分钟，1～240），随备份导出/导入，并铺满下一份计划的每一天；2026-11-23 用户要求：知识点增加上层分类（schema v13 `knowledge_category`，默认 11 个：语文、数学、英语、物理、化学、生物、政治、历史、地理、信息技术、通用技术，可增删改），新增/编辑知识点必须指定分类，分类名唯一、有知识点的分类不可删；旧库与旧备份的知识点统一归入「语文」；「知识点」页与「录入题目」的知识点标签区共用横向分类筛选选项卡（首项「全部」），「知识点」页把筛选卡放在「共 N 个标签」与「新增标签」按钮之上并缩小其与顶部导航栏的间距（2026-11-24 用户要求删去该页与「来源标签」页的搜索框）；schema v14 给 `Question` 与 `KnowledgePoint` 增加 `updatedAt`（用户新建/编辑时间，迁移时旧数据回填为 `createdAt`），题库与知识点列表据此倒序展示，复习调度、打印快照、归档等系统写回不改变该值（2026-11-24 用户要求）；其余 feature 随对应阶段创建。
2026-11-25 UI 设计系统与页面重排（用户要求，不改变任何功能、路由与数据语义）：`app` 新增 `com.recollect.app.ui.theme.RecollectTheme`——「纸张与墨色」Material 3 主题，浅/深色均完整定义 color roles（纸张底、松烟墨绿主色、赭石次色、朱砂警示），字号阶梯收敛，形状统一为 6/10/18/24dp，跟随系统深色模式；不再使用 Material 基线紫色。首页顺序固定为「计划主卡 → 复习流程条（录入/生成/打印/扫描/确认，按本周计划状态点亮当前环节）→ 常用操作 2×2（录入题目/管理题库/扫描回读/查看复习进度）→ 学习概览 → 设置面板（每日复习时长/关于）」；首页按当前环节动态高亮唯一一张引导卡（纯 Kotlin `currentReviewStage`/`homeHighlight`：录入、扫描对应 2×2 操作卡，生成、打印、本轮完成对应计划主卡，其余卡片退回中性），流程条右侧同步显示「下一步：录入题目/生成计划/打印计划/扫描回读」；题库页为带搜索/清除图标的搜索框 + 「题号 + 来源与时长 + 知识点标签」三段式题卡 + 教学式空态（空题库隐藏悬浮按钮、改为居中「录入第 1 道题」）；录入页题号、编号（试卷编号/页码编号）与复习时长三栏并排、保存为底部通栏主按钮；计划预览为计划摘要卡 + 「日期栏 + 题目标签」页卡；打印页在系统返回后把「已打印/未打印」确认提升为主内容；扫描页以结果卡突出「进入确认页」；确认页顶部显示整批确认进度、单行决策后收起操作按钮；复习进度、导出数据库与关于页同步重排。规格与设计说明见 `docs/specs/app.md`；本轮未引入新依赖（R-05）。
2026-11-26 首页以复习流程为主轴（用户要求，不改变调度与数据语义）：①「复习流程」条移到首页最上方，点亮当前环节并显示「第 N / 5 步」，流程图下方新增一句操作指引（`stageGuidance`）；②删除本周/下周计划主卡，首页不再区分两周，只保留「预览计划」入口；计划窗口改由计划预览页顶部的「本周计划 / 下周计划」选项栏选择；③删除原先「常用操作」右侧的「记录 → 打印 → 回读」标签，并删除「常用操作」区块标题，四张卡片直接排在流程条下方（2026-11-26 用户要求）；④四张快捷卡片（录入题目 / 管理题库 / 预览计划 / 扫描回读），按环节把对应入口前置并高亮：录入前置「录入题目」，生成/打印前置「预览计划」，扫描/确认前置「扫描回读」，高亮卡标注「下一步」，其余入口保持固定顺序（`orderedHomeActions`）；卡片不显示副标题，左侧图标、右侧纵向右对齐显示标题与「下一步」，高度随内容自适应（同一行用 `IntrinsicSize.Min` 对齐，无固定高度与底部留白）；⑤「管理题库」页右下角纵向排列「录入题目」与「预览计划」两个按钮，已有题目时前者淡化、后者高亮；⑥确认「已打印」写回成功后立即退回首页并提示「打印完成」，分享 PDF 在页面重新获得焦点且快照写入成功后同样退回首页并提示；⑦确认页整批提交成功后退回首页并提示「确认完成」；⑧首页每次回到该路由或应用恢复前台时重读计划状态与统计；⑨「学习概览」与「查看复习进度」放入页面最上方的独立卡片（样式与字号同复习流程卡标题行，位于流程卡之上），题目 / 知识点 / 已逾期 / 7 天内四项统计并入同一张卡片，不再有独立统计卡；卡内无分隔线，卡片顶部到标题、标题到统计的间距与复习流程卡一致（16dp / 14dp）；⑩计划预览进入时默认周由本周计划状态决定（COMPLETED → 下周，否则本周），打开已有计划且题库在生成后新增/编辑或有计划内题目被归档时，询问「题库已更新，是否重新生成计划？」（`PlanSession.activePlan`、`StoredPlan.createdAt`、`EntrySummary.updatedAt`；不用全部题库与计划题目做集合比较，避免未到期题目误报）。规格与设计说明见 `docs/specs/app.md`、`docs/specs/planning.md`。
2026-11-26 设置区收纳数据入口与版本号位置（用户要求，不改变导入/导出行为与数据语义）：①「导入数据库」「导出数据库」两个入口从首页顶部导航栏删除，移入首页「设置」面板——设置面板上方保留「每日复习时长」行，分割线下方并排「导入数据库」「导出数据库」「使用引导」「关于」四个等宽入口（图标在上、文字在下），各占原面板总宽度的四分之一；标题使用 labelSmall 且不设水平内边距，保证 1.3 倍字号下完整显示；点击「使用引导」重新播放首次复习演示，演示前快照当前数据库、结束后原样恢复（2026-11-26 用户要求）；图标仍按 2026-11-22 的互换规则（导入显示下载图形、导出显示上传图形）；「关于」行不再显示版本号副标题。②首页顶部标题「拾知」的右下角以 `labelSmall` 字号与 `onSurfaceVariant` 非强调色显示 `v{versionName}`（`AppContainer.appVersionName`），其他页面标题不带版本号。新增 `HomeSettingsAction` 组合函数承载等宽入口；`HomeScreen` 的 `versionName` 参数随之移除，改由 `AppRoot` 顶层导航栏直接读取。规格同步见 `docs/specs/app.md`、`docs/specs/backup.md`、`docs/specs/settings.md`。
2026-11-26 导出数据库改为悬浮弹窗（用户要求）：删除独立页面路由 `AppRoute.Backup` 与其标题映射；设置面板的「导出数据库」入口改为置位 `AppRoot` 内的 `showBackupDialog` 状态，由 `feature:backup` 新增的 `BackupDialog` 以 Material 3 `AlertDialog` 悬浮在首页上方呈现——样式与「每日复习时长」设置弹窗一致，底部提供「关闭」，与系统返回键一样只关闭弹窗、不改变返回栈，不显示返回图标；弹窗内容可滚动。`BackupViewModel` 只在弹窗显示期间创建，关闭后丢弃，重新打开回到空闲状态。原 `BackupScreen` 页面组合函数改名为 `BackupDialog`（正文抽为私有 `BackupContent`）；导出/导入数据行为与状态机不变。规格同步见 `docs/specs/app.md`、`docs/specs/backup.md`。
2026-11-26 首次打开时的复习流程演示（用户要求，不改变调度、打印、扫描与确认的数据语义）：首次启动或从设置「使用引导」手动重看时，由 `DemoDataStore` 把当前数据库导出为 JSON 快照，再只种入演示用的两个知识点（数学）与一个来源标签（`app/src/main/assets/demo/seed.json`，不预置题目/计划）；先显示居中的「欢迎使用拾知」欢迎卡（点「开始引导」进入第 1 步，点「跳过演示」直接还原；欢迎卡不计入 5 步）；从首页设置「使用引导」手动重看时，欢迎卡额外提示「您的数据不会丢失」，演示前仍快照当前数据库、结束后原样恢复；随后 `DemoTourOverlay` 以黑底蒙层挖出真实控件（镂空圆角按目标适配：文本框 6dp、整卡 14dp、来源/知识点分组 0dp（直角，与确认结果一致）、Extended FAB 18dp、普通按钮取高度一半、确认页整块内容 0dp；确认提交成功后整页压暗（无镂空），只保留说明条与「开始使用」），用户亲手完成「录入 → 生成 → 打印 → 扫描 → 确认」：①首页聚焦「录入题目」卡，进入录入页时表单已由 `QuestionEditPrefill` 填好题号 12、页码 42、来源与时长，说明条依次聚焦题号/页码/时长、来源、知识点与「保存」，用户点保存；②题库页聚焦「预览计划」，计划按保存的题目生成；③计划预览聚焦「打印计划」，打印页在演示模式提供「使用演示打印稿」（不调起系统打印），用同一份 `PrintPlanInput` 经 `PrintLayoutBuilder`/`PrintRenderer` 渲染 300 DPI A4 稿并在填涂框内涂黑，写入缓存经 `FileProvider` 交给扫描页；④扫描页在扫描框内展示该稿（不调用相机），点击「识别这张打印稿」走真实 QR/OMR 识别，成功后聚焦「进入确认页」；⑤确认页整块内容区保持可操作，用户逐行确认并提交，提交成功后说明条上的「开始使用」才可用；点击后恢复启动前快照并直接回到空首页。演示不显示独立的「演示结束」页，也不弹「示例数据已清除」Toast；中途进程被杀时下次启动先还原快照：首次引导会再重新演示，手动重看则直接回到用户数据。演示期间拦截系统返回；扫描页 `autoRequestCamera`/`demoSheet` 参数避免相机权限框与相机管线。`docs/specs/app.md` 已回写（2026-11-26 用户授权）。

core/common、core/model、core:scheduler、core:pagination 为纯 Kotlin，禁止 import android.*/androidx.*，只允许 Kotlin 标准库与 java.*（含 java.time）。`core:model` 只放纯 DTO/枚举与 `KnowledgeRowsCodec`（含 `Tier`、`KnowledgeState`、`SchedulerQuestion`，C-053）；打印题目投影 `RenderableQuestion` 属于 `core:pagination`，不在 `core:model` 重复定义（C-073 方案 A）；Room Entity ↔ DTO 的映射放在 `data:room`（C-046 方案 A）；计划持久化草稿 `PlanDraft` 与 `PlanGenerationException` 放在 `data:room`，编排层只构造入参与展示（C-074 方案 A）。计划生成返回 `PlanGenerationOutcome`：成功含 `PrintJob` 与预览投影，空计划含空态与提示；分页失败仍抛 `PlanGenerationException` 且不写库（C-047、C-060、C-090 方案 A）。确认提交的状态在 `data:room` 的 `ApplyFeedbackUseCase` 内计算并转 `Float`，写入反馈/决定日志/删除待确认由 `saveDecisions` 单事务负责，`ApplyResult` 携带 `SaveDecisionsResult`（C-075 方案 A）。确认前校验（会话 READY/未提交、待确认行完整、计划日与触发标记一致、知识点归属、复习日上下界、档位可用）由 `ApplyFeedbackUseCase` 承担，统一抛 `InvalidConfirmationException`（C-079 方案 A）；整题跳过仍写确认日志，但不写反馈、状态与题目复习日（C-078 方案 A、C-077 方案 A）；已提交会话拒绝重复提交（C-080 方案 A）。冷却顺延由 `PlanRepository.savePlan` 在计划保存的同一事务内按调度器给出的目标日期写入，不另设独立顺延用例（C-076 方案 A）。

## 冻结的架构决策（不得"优化"或"简化"）
D-01 调度单元是知识点；同一题目被多知识点触发只打印一次，触发知识点正常更新；非触发关联知识点按 0.3 权重更新连续掌握度增量，档位与下次到期日不变。非触发知识点也各占一行并独立填涂；有填涂时按该行自己的档位计算 ×0.3，未填涂（本次未复习）则不更新。
D-02 档位差至少一档仅作为同档位组内的排序权重（只比较本期到期点与非触发的、仍会打印的关联点之间的档位差，C-054；最高档位分组优先于档位差，C-059），不淘汰候选；所有到期题目都进入候选池。
D-03 连续掌握度仅用于同档位内排序与时间装箱；更新规则：掌握 +0.3、较熟悉 −0.1、不会 −0.3，夹在 [0,1]；不得替代档位比较。C-050：算术在 UseCase 内部用 `Double`，对外/落库转 `Float`；`effectiveFeedbackCount` 阈值精确 `>= 3.0f`，不做 epsilon。
D-04 冷却期优先于到期触发；固定 7 天（设置页不可修改；0 仅内部/测试表示关闭，负数非法，C-057），期间知识点顺延并标记"因冷却顺延"；用户手动覆盖后才可突破。覆盖采用确认页创建的一次性 CoolingOverrideEvent，plan() 通过 coolingOverrides 接收。冷却顺延由计划保存的同一事务原样写入（C-076 方案 A）。
D-05 冷启动固定阶梯 1/3/7 天；三档任意填涂均计有效反馈，"较熟悉"按 0.5 次计入；累计有效反馈 ≥ 3.0 后从下一次反馈起切换到墨墨背单词 DHP 简化模型：FAMILIAR 半强度、90% 保持率 + 30 天上限、新增 difficulty/halfLifeDays 字段；D-01～D-04 的排序/权重/冷却仍生效。
D-06 手写笔记 OCR 只存储和展示，不参与调度更新；原始 OCR 只读、修正文本可编辑，导出两者都含。
D-07 打印模板（2026-11-24 用户要求调整）：每题上方左侧为知识点填涂区（约 42%，一行一个知识点组，掌握/较熟悉/不会三框水平），右侧为题号/来源/知识点信息区（约 58%，全称不预留作答空白；2026-11-24 删除「页码 XX」）；两个模块下方改为通栏笔记区——上方是历史笔记显示区（可选附加摘要），下方是「复习笔记」书写区，QR 与 pageId 标签嵌在书写区右侧并排（书写区不占满整幅宽度），题块不再有独立 QR 页脚带。填涂框上方每页直接标注"掌握 较熟悉 不会"（分别与三框中心对齐，并贴近该页第一道题），不再绘制 A/B/C 图例。知识点名称限一行；8–10 字截断只作用于左侧填涂区，右上保留全称并以" / "拼接、允许换行。左上/右上均按知识点编号升序；右上名称与左侧同一 printable 集合，数量等于知识点数量（由计划/数据层保证，C-067）。
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
