# 回读管线规格（feature/scan）

关联决策：D-06、D-09、D-10
关联裁决：C-010、C-011、C-012、C-015、C-021、C-030、C-051、C-091、C-092

## 冻结决策（D-06、D-09、D-10）

- **D-06**：手写笔记 OCR 只存储和展示，不参与调度；原始 OCR 只读、修正文本可编辑；导出两者都含。
- **D-09**：回读顺序 ArUco → 透视 → QR → OMR → OCR → 确认；QR 含版本/planId/pageId/questionId/校验码，不含知识点 ID；页码即 pageId；QR 失败走手动输入 pageId + 题号。
- **D-10**：端侧推理；依赖固定；不引入云端 SDK；不使用 Gemini Nano；ML Kit 固定 text-recognition-chinese:16.0.0 + vision-common:17.3.0。

## 顺序（不得调换）

1. CameraX 取帧（或相册选图）
2. 自研引擎（OpenCV）：ArUco 定位 → 透视矫正（C-092 方案 C）
3. 逐题在其页脚区域解码 QR（每题一个）；解码后立即做版本/校验码/归属校验（见下）；失败不进入确认页
4. 自研引擎 OMR：仅采样左侧填涂区，按行分组识别 A/B/C（几何与阈值见下）
5. 对右下笔记区**手写书写区**裁剪 → ML Kit 中文离线 OCR（text-recognition-chinese:16.0.0）；不读取印刷的附加摘要区
6. 产出 `PendingFeedback`（含 `triggered`、`detectedTier`、`detectionStatus`、`conflict`、`detectedRawOcr`），初始 `userDecision = UNMARKED`
7. 转交 feature/confirm；本模块不直接写 `ReviewFeedback`（R-04）

## ScanEngine 适配层

`feature/scan` 定义 `ScanEngine`，内部封装 OpenCV 与 ML Kit；ViewModel/UseCase 不直接调用库（R-03）：

```kotlin
interface ScanEngine {
    fun detectPage(bitmap: Bitmap): PageDetection
    fun decodeQrs(bitmap: Bitmap, page: PageDetection): List<QrResult>
    fun readOmr(
        bitmap: Bitmap,
        page: PageDetection,
        blocks: List<ItemBlockGeometry>
    ): List<OmrRow>
}
```

- `PageDetection` 含四角 ArUco、透视矩阵、归一化后的 2480×3508 图像。
- `QrResult` 含原始载荷、解析字段、页内中心坐标。
- `OmrRow` 含题块 questionId、行号、A/B/C 命中、`DetectionStatus`。

具体字段：

```kotlin
data class PageDetection(
    val corners: List<PointF>,          // 左上/右上/右下/左下
    val corrected: Bitmap,              // 2480×3508
    val rotation: Int                   // 0/90/180/270
)

data class QrResult(
    val raw: String,
    val planId: String?,
    val pageId: String?,
    val questionId: String?,
    val centerX: Float,
    val centerY: Float,
    val checksumOk: Boolean
)

data class ItemBlockGeometry(
    val questionId: String,
    val topMm: Double,
    val heightMm: Double,
    val rowCount: Int
)

data class OmrRow(
    val questionId: String,
    val rowIndex: Int,
    val status: DetectionStatus,
    val detectedTier: Tier?
)
```

## C-092 方案 C：自研回读引擎

- C-051 方案 A（OpenScanVision 根坐标、850×540 适配）经 6.2 spike 判定不可行：库的参考画面固定 850×540 横向，四角标记参考位置与唯一一个二维码区域写死，自定义模板在模块外不可达（见 `docs/conflicts-archive.md` C-092）。`openscanvision:1.0.0` 停用，坐标仅保留供 spike 复现。
- 现行实现：ArUco 定位与单应性透视用 `com.quickbirdstudios:opencv-contrib:4.5.3.0`（`DICT_4X4_50`），按标记中心与打印几何矫正，统一输出 2480×3508（300 DPI）；逐题 QR 用 ML Kit `barcode-scanning:17.2.0`；OMR 按本规格的几何与阈值自行采样。
- 打印层不引入 OpenCV：四角标记图案以常量固化（`ArucoPattern`），由 Canvas 绘制；扫描端按同一 `DICT_4X4_50` 检测。
- `ScanEngine` 适配层必须与 `feature:print` 共用同一 `TemplateGeometry.a4Print()`；采样窗口、阈值与二维码生成参数写入 `evidence/scan/omr-config.json`。
- 原生库（`libopencv_java4.so`、ML Kit `libbarhopper_v3.so`）当前为 4KB 对齐；Android 15+ 16KB 页设备需升级依赖或替换实现，解决前不得声称 16KB 设备可用。

## 页面检测、矫正与多页

- MVP 一次只接受**一页**；检测到多组 ArUco 返回 `MULTIPLE_PAGES`，提示重拍。
- 用 ArUco 顺序（左上→右上→右下→左下）判断旋转，支持 0/90/180/270；透视后统一输出 2480×3508（300 DPI）。
- 四角任一无检测 → `PAGE_NOT_FOUND`；透视后边长比例异常/清晰度低于引擎阈值 → `PAGE_UNREADABLE`。
- 不自动拼接多页；多页计划逐页拍摄。多页计划如何合并为一个确认会话见 `confirm-flow.md` 的“扫描会话”。

## 扫描会话与批量回读（C-038、C-040、C-041）

- 一次计划扫描使用一个 `ScanSession(sessionId, planId, startedAt, status)`；多页图片向同一 session 追加 `PendingFeedback` 行。
- `PendingFeedback` 写入 `planId`、`pageId`、`plannedDate`（来自 `PlanPage.date`）、`actualReviewDate=plannedDate`（确认页可覆盖）。
- 同一页重复扫描：按 `(planId,pageId,questionId,knowledgePointId)` 去重，保留最新识别结果并提示。
- 扫描日早于 `planEndDate`：`plannedDate > 今天` 的行只能标 `NOT_REVIEWED`，或提示“计划尚未结束”；不得给未来日期写非 `NOT_REVIEWED` 反馈。
- 扫描日 > `planEndDate + 7`：标记 `LATE_BATCH`，但不自动改每行 `reviewedOn`；`planEnd+1..+7` 为正常窗口。

## OMR 采样几何与阈值

- 左区 x 起点 = `TemplateGeometry.contentLeftMm`（`a4Print()` 下 26mm），宽 = `leftAreaWidthMm`（`a4Print()` 下 66.36mm）；列宽：序号 6mm、知识点 34mm、列间距 1.5mm；填涂格为答题卡式 16:9 宽格，`bubbleWidthMm × bubbleHeightMm` = 5.7 × 3.2mm，框间距 **3.1mm**（2026-11-22 用户要求：为「掌握 / 较熟悉 / 不会」留出文字间距）。不得再用旧的 12mm/74.4mm（C-091 方案 A），也不得再使用旧的 8/34/3.5/2mm 列宽。
- 框中心 x 直接取 `TemplateGeometry.bubbleCenterXMm(i)`（i=0/1/2 对应掌握/较熟悉/不会，`a4Print()` 下为 71.85/80.65/89.45mm）；框中心 y = 行 y 起点 + `rowHeightMm/2`。打印与扫描共用同一 `TemplateGeometry`，不得在任一模块另写常量；框间距变化时采样中心随公式自动平移，无需在扫描端另调参数。项目未发布，不保留旧模板兼容分支。
- 单个题块内第 j 行 y 起点 = `yOffset + j*rowHeightMm`；`yOffset` 已包含页首表头与上方题块（2026-11-22 删除首页图例后不再有图例偏移）。不要用跨题块的全局行号公式。
- 题块内定位优先用 `PlacedItem.yOffsetMm`：题块左侧行从 `yOffset` 开始（已包含页首与上方题块），按 `knowledgeRows` 快照行序绑定。
- 每个框采样中心 2.5mm×2.5mm（`bubbleSampleSizeMm`，必须小于框高 `bubbleHeightMm`）；灰度 < 128 记为暗像素。
- 单个框：暗像素比例 > 30% → 命中；< 10% → 未命中；10%–30% → 模糊。
- 行判定优先级（2026-11-22 修复）：>1 个明确命中 → `CONFLICT`；**恰好 1 个明确命中 → `OK`**，其余框落在 10%–30% 的模糊区间也不推翻唯一命中（低分辨率扫描时未填涂框边缘常落在此区间，旧逻辑会把整行误判为 `UNREADABLE`）；0 个命中且存在模糊框 → `UNREADABLE`；0 个命中且全部未命中 → `NO_MARK`。

## QR 与题块绑定

- 每个题块是 `PlacedItem.yOffsetMm` 到 `yOffset+heightMm` 的水平带；QR 位于题块底部 `qrFooterHeightMm` 区域。
- 扫描端按 QR 中心 y 坐标匹配最近的题块，容差 ±`qrFooterHeightMm/2`。
- 匹配后：QR 给出 `questionId`；题块内左侧行按 `PlanPage.knowledgeRows` 快照绑定；行数不一致 → `OWNERSHIP_MISMATCH`。
- 一页多题时，每题块各自带 QR；不要求整页只有一个 QR。

## QR 载荷与校验算法（C-030）

QR 载荷格式（UTF-8）：

```text
version|planId|pageId|questionId|checksum
```

- `version`：QR 载荷版本，当前固定 `1`。
- `planId`：6 位 Crockford Base32（去掉 I、L、O、U），由计划创建时生成，本地全局唯一。
- `pageId`：`{planId}-{pageNumber:02d}`，例如 `K7M2Q9-03`；全局唯一（C-014）。
- `questionId`：题目 ID，字符集 `[A-Za-z0-9_-]`。
- 字段分隔符为 `|`；参与拼接的字段不得包含 `|`，生成端发现即抛错，不做转义。
- `checksum`：对 `version|planId|pageId|questionId` 的 UTF-8 字节计算 CRC-32（IEEE 802.3，多项式 `0xEDB88320`），输出 8 位大写十六进制。
- 解码端：必须恰好 5 个字段；版本不支持 → `QR_INVALID`；CRC 不匹配 → `QR_INVALID`；字段字符集不符 → `QR_INVALID`。

## 归属校验（C-011 方案二：scan 先校验，失败不进入确认页）

QR 解码成功后、创建 `PendingFeedback` 前，按以下顺序校验：

1. `version == 1`（仅 QR；手动输入没有 QR 载荷，跳过）；
2. `PlanPage` 存在 `(planId, pageId)`；
3. `questionId` 存在，且在该 `PlanPage.questionIds` 中；手动输入时其 `questionNumber` 还必须与纸面题号一致；
4. 该题关联知识点与 OMR 行数一致（行数 = printable 知识点数量，`!dormant && activeQuestionCount > 0`）；
5. CRC-32 重算一致（仅 QR；手动输入跳过）。

任一项失败：**不创建 PendingFeedback、不进入确认页**，只显示错误与「重拍 / 手动输入 / 取消」。

### 手动回退

- QR 失败时提示「请重拍，或按页脚二维码旁的小字手动输入页面 ID + 题号」；打印稿页脚以 2.2mm 小字显示完整 `pageId`（C-095 方案 B）。
- 用户输入 `pageId + 题号`；`pageId` 全局唯一，直接命中 `PlanPage`，再按 `questionIds` 与 `questionNumber` 定位题目。
- 输入规范化：大小写不敏感；`I/L → 1`、`O → 0`；允许字符 `A-Z a-z 0-9 -`；长度必须为 9（6 位 planId + `-` + 2 位页码）；不匹配则提示格式错误。
- 手动输入成功后，执行上述 **2～4** 的归属校验（版本与 CRC 仅 QR 适用；`questionId` 由 `PlanPage.questionIds` + 题号在本地解析）。
- 手动输入失败：不创建 `PendingFeedback`，允许重试或取消。

## QR 错误路径（C-030）

| 错误码 | 场景 | 处理 |
|---|---|---|
| `PAGE_NOT_FOUND` | 四角 ArUco 未检测到 | 提示重拍 |
| `MULTIPLE_PAGES` | 图中检测到多组 ArUco | 提示重拍，一次只拍一页 |
| `PAGE_UNREADABLE` | 透视/清晰度低于阈值 | 提示重拍 |
| `QR_NOT_FOUND` | 图中未检测到 QR | 提示重拍或手动输入 |
| `QR_UNREADABLE` | 检测到 QR 但模糊/无法解码 | 提示重拍或手动输入 |
| `QR_MULTIPLE` | 同一题目的 QR 区域检测到多个 QR，或无法按页脚位置把 QR 与题目一一对应 | 提示重拍；不自动取第一个 |
| `QR_INVALID` | 字段数/版本/字符集/CRC 失败 | 提示重拍或手动输入；不得降级为「信任纸面」 |
| `OWNERSHIP_MISMATCH` | planId/pageId/questionId/知识点归属不一致（版本失败统一归 `QR_INVALID`） | 阻断；提示联系或重新打印；不写任何反馈 |

## OMR 行组重建（C-015 方案一）

- 行序的生成规则 = 该题关联知识点中**未休眠且 `activeQuestionCount > 0`** 的列表按 `KnowledgePoint.number` 升序；打印端在生成 `PlanPage.knowledgeRows` 时按此规则冻结行快照（C-015）。
- **回读端只读取 `PlanPage.knowledgeRows`** 的 `rows` 与 `triggeredKnowledgePointIds`，按快照重建行组并设置 `PendingFeedback.triggered`；`knowledgeRows` 为空或不可解析时视为无快照，不创建 `PendingFeedback`、不进入确认页，提示重新生成计划并按新稿打印，不得按当前关联知识点 + number 升序重算（C-071/C-072 方案 A）。下文的 `sortedKnowledgePoints` 即快照中的 rows。
- 若快照与当前 DB 的关联集合不一致（例如打印后用户改了关联），确认页按快照校验；用户必须重新打印才能使用新关联。
- 行组边界：透视矫正后，左侧填涂区顶部为 `leftAreaTop`；第 i 行（0-based）的 y 范围为
  `[leftAreaTop + headerRowHeightMm + i * rowHeightMm, leftAreaTop + headerRowHeightMm + (i + 1) * rowHeightMm)`。
- 每行三个框：掌握/较熟悉/不会在行内水平等距；框中心与边距来自 `TemplateGeometry.a4Print()` 的 `bubbleCenterXMm(i)`（序号 6mm、知识点 34mm、列间距 1.5mm、框 5.7×3.2mm、框间距 3.1mm）。采样窗口不跨行、不跨框。
- 第 i 行绑定到 `sortedKnowledgePoints[i].id`（来自 `PlanPage.knowledgeRows` 快照）；因此回读端不需要 QR 携带知识点 ID（D-09）。
- `KnowledgePoint.number` 创建后不可变、删除后由墓碑保留；若打印后用户修改了题目的关联知识点集合，旧打印稿仍按快照行映射，确认页会校验快照与当前关联的**成员与顺序**是否一致，不一致则阻断写入，要求重新打印。
- OMR 识别结果：
  - 恰好一组填涂 → `OK`，`detectedTier` = 该组；
  - 无填涂 → `NO_MARK`，`detectedTier = null`；
  - 多组填涂 → `CONFLICT`，`conflict = true`，`detectedTier = null`；
  - 图像不可判定 → `UNREADABLE`，`detectedTier = null`。
- 行分组与人工核对规则不变：允许 ≤ 1 组误判，误判由确认页修正。

## 引擎版本与配置

- `libs.versions.toml` 固定 `opencv-contrib:4.5.3.0` 与 `barcode-scanning:17.2.0`，不使用 `+`/`latest`。
- 打印验收与扫描共用同一 `OmrConfig`：ArUco 字典、二维码 ECC、采样窗口、阈值写入 `evidence/` JSON，保证可复现。
- 四角标记：`DICT_4X4_50`，ID 0/1/2/3 = 左上/右上/右下/左下，内码 10mm、白边 4mm、距页边 12mm；打印层按 `ArucoPattern` 的 6×6 模块绘制。
- 二维码：每题一个，载荷见「QR 载荷与校验算法」；生成参数（版本、纠错级别）与打印几何写入 `OmrConfig`。
- 不引入云端 OCR/AI；ML Kit 中文 OCR 仍固定 `text-recognition-chinese:16.0.0` + `vision-common:17.3.0`。
- 升级 OpenCV/ML Kit 或更换二维码生成方案时，必须重跑 `evidence/print/` 与 `evidence/scan/` 的 R-01 门禁，不能只跑单元测试。

## 约束

- 不调用云端 API；不引入 Gemini Nano（D-10）。
- 原始帧不落盘；仅保存渲染后的识别结果与关键截图到 `evidence/scan/`。
- OCR 结果只写入 `detectedRawOcr`，不触发调度更新（D-06）；`correctedText` 由确认页产生。
- `detectedRawOcr` 只读；任何编辑都写入 `correctedText`，两者分开保存（C-012）。
- 本模块不写 `ReviewFeedback`、不更新知识点状态（R-04）。

## 验收

- 5 张不同光照/角度打印稿跑通完整管线，结果存 `evidence/scan/`。
- OMR 分组与人工核对一致（允许 ≤ 1 组误判，误判时确认页可修正）。
- **OCR 原文只读**：`detectedRawOcr` 从识别到确认页不可编辑；确认页只允许编辑 `correctedText`。
- **修正文本可编辑**：修正后的 `correctedText` 与原始 `rawOcrText` 在数据库与导出中同时存在。
- 页面：`PAGE_NOT_FOUND`、`MULTIPLE_PAGES`、`PAGE_UNREADABLE` 各有用例；一次只接受一页。
- QR：正常解码、CRC 失败、版本不支持、字段缺失、多 QR 五种情况各有用例；解码失败与校验失败走独立错误路径。
- OMR：按 30%/10% 阈值与 2.5mm 采样窗口验证；`OK/NO_MARK/CONFLICT/UNREADABLE` 各有用例。
- QR 中心与题块匹配、`knowledgeRows` 行序绑定各有用例；不匹配走 `OWNERSHIP_MISMATCH`。
- 同一 `ScanSession` 多页追加、重复页去重、提前扫描/晚扫描标记各有用例。
- 手动输入 `pageId + 题号` 的降级路径可用；pageId 全局唯一命中；手动输入不要求 version/CRC（无 QR 载荷）。
- 行组重建：同一题 1/2/3/4 个知识点，行 i 与 `number` 升序一致；重启/重新规划后行序不变；只使用 `PlanPage.knowledgeRows` 快照，无快照的旧稿阻断并提示重印，不回退重算（C-072 方案 A）。
- 快照与当前关联的成员/顺序不一致、或快照中的知识点已删除时，归属校验阻断并提示重新打印。
- C-092 方案 C：四角定位、A4 透视（2480×3508）、逐题 QR 与 OMR 必须给出 `evidence/scan/` 的原始输出；5 张真实打印稿的照片回读仍是 R-01 硬门禁，不得用 PDF 光栅位图代替。
