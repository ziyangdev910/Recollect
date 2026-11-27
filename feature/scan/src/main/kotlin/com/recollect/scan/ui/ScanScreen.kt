package com.recollect.scan.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.recollect.feature.scan.R
import com.recollect.scan.io.GalleryMediaRefresher
import kotlinx.coroutines.launch

/**
 * 首次演示用的打印稿：一张已填涂的 A4 图片，以及识别引擎读取它的 URI。
 *
 * 图片只用于页面预览；识别仍走 `ScanViewModel.onImageSelected` 与真实 ScanEngine。
 */
data class DemoScanSheet(
    val image: ImageBitmap,
    val imageLocation: String,
)

private val ScanBackground = Color(0xFF0B0D0C)
private val ScanOverlay = Color(0xE6141815)
private val ScanText = Color(0xFFF4F1E6)
private val ScanMutedText = Color(0xFFB9BDAF)
private val ScanAccent = Color(0xFF7FD6A8)
private val OnScanAccent = Color(0xFF10352A)

/**
 * 扫描回读页（app.md「Scan」、scan-pipeline.md；2026-11-27 用户要求按 reference/CaptureWindow.png 重做）。
 *
 * 全屏深色相机取景，中央引导线对齐整页打印稿；底部始终为多页录入：
 * 相册 / 快门 / 完成。录入一页或多页后，缩略图与页数徽标提示已扫页数，
 * 点「完成」进入确认页。演示模式在取景框里展示预生成的打印稿，不调用相机（见「首次复习演示」）。
 */
@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onOpenConfirm: (sessionId: String) -> Unit,
    /** 关闭本页（全屏取景没有应用标题栏，左上角关闭按钮回到上一页）。 */
    onClose: () -> Unit = {},
    /**
     * 首次演示会直接切到本页讲解，而不打算真的拍照；
     * 传 false 可避免一进入就弹系统相机权限框打断黑底演示。
     */
    autoRequestCamera: Boolean = true,
    /** 首次演示：扫描框内直接展示已填涂的打印稿，不打开相机。 */
    demoSheet: DemoScanSheet? = null,
    /** 首次演示用：报告「识别这张打印稿」按钮的位置。 */
    demoScanButtonBounds: (Rect) -> Unit = {},
    /** 首次演示用：报告「完成」按钮的位置。 */
    confirmButtonBounds: (Rect) -> Unit = {},
    /** 首次演示用：识别结果写入会话后通知外层切换到「完成」聚焦。 */
    onSessionReady: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cameraGranted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var cameraDenied by remember { mutableStateOf(false) }
    var autoRequested by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) { viewModel.load() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraGranted = granted
        cameraDenied = !granted
        if (!granted) {
            viewModel.showMessage("相机权限被拒绝；仍可在本页从相册选择打印稿照片。")
        }
    }

    // 首次进入只自动请求一次；拒绝后由底部按钮再次请求。
    // 演示模式展示已生成的打印稿，不打开相机，也不请求权限。
    LaunchedEffect(autoRequestCamera, demoSheet) {
        if (autoRequestCamera && demoSheet == null && !cameraGranted && !autoRequested) {
            autoRequested = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val galleryRefresher = remember(context) {
        GalleryMediaRefresher(context.applicationContext)
    }
    var refreshingGallery by remember { mutableStateOf(false) }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) scope.launch { viewModel.onImageSelected(uri.toString()) }
    }

    // 系统照片选择器只读 MediaStore，打开前先补扫公共存储中的图片；
    // 权限被拒绝时只跳过刷新，仍照常打开选择器。
    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        scope.launch {
            if (granted) {
                runCatching { galleryRefresher.refresh() }
            } else {
                viewModel.showMessage(
                    "未授予相册读取权限，无法自动刷新相册；仍可选择已入库的图片。",
                    isError = false,
                )
            }
            refreshingGallery = false
            pickLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }

    val openGallery: () -> Unit = {
        if (!refreshingGallery && !viewModel.state.busy) {
            viewModel.clearMessage()
            refreshingGallery = true
            val permission = galleryReadPermission()
            if (context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                scope.launch {
                    runCatching { galleryRefresher.refresh() }
                    refreshingGallery = false
                    pickLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                }
            } else {
                galleryPermissionLauncher.launch(permission)
            }
        }
    }

    val state = viewModel.state
    val session = state.session
    val pageCount = session?.pages?.size ?: 0
    val controller = rememberScanCameraController()

    // 演示：识别结果写入会话后，把焦点从「识别」切到「完成」。
    LaunchedEffect(state.session?.sessionId) {
        if (state.session != null) onSessionReady()
    }

    Box(modifier = Modifier.fillMaxSize().background(ScanBackground)) {
        // 取景层：演示稿、相机或拒绝权限后的深色背景。
        if (demoSheet != null) {
            DemoSheetBackdrop(demoSheet)
        } else if (cameraGranted) {
            ScanCameraPane(
                controller = controller,
                busy = state.busy,
                onCaptured = { location -> scope.launch { viewModel.onImageSelected(location) } },
                onError = { message -> viewModel.showMessage(message) },
                modifier = Modifier.fillMaxSize(),
            )
            ScanGuideLines()
        }

        // 左上角关闭：全屏页不显示应用标题栏。
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 6.dp, top = 6.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = "关闭",
                tint = ScanText,
            )
        }

        // 中央引导文案。
        if (demoSheet == null && !cameraGranted) {
            Unit
        } else {
            Text(
                text = when {
                    demoSheet != null && session == null -> "演示打印稿\n点击下方按钮识别"
                    demoSheet != null -> "已识别 $pageCount 页\n点击「完成」继续"
                    session == null -> "平行纸面拍照\n题目对齐参考线"
                    else -> "请拍第 ${pageCount + 1} 页"
                },
                style = MaterialTheme.typography.titleLarge,
                color = ScanText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }

        // 底部操作区。
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            state.message?.let { message ->
                ScanMessageCard(message = message, isError = state.messageIsError)
            }
            state.error?.let { error ->
                ScanErrorCard(
                    error = error,
                    onManual = viewModel::startManualInput,
                    onCancel = viewModel::dismissError,
                )
            }
            state.manual?.let { manual ->
                ManualEntryCard(
                    manual = manual,
                    onPageIdChange = viewModel::updateManualPageId,
                    onQuestionNumberChange = viewModel::updateManualQuestionNumber,
                    onSubmit = { scope.launch { viewModel.submitManualInput() } },
                    onCancel = viewModel::cancelManualInput,
                )
            }

            if (state.busy || state.loading) {
                BusyPill(
                    text = if (state.busy) {
                        "正在识别：定位 → 二维码 → 填涂 → 笔记…"
                    } else {
                        "正在恢复扫描会话…"
                    },
                )
            }

            // 演示稿的识别入口：识别后收起，改由「完成」进入确认页。
            if (demoSheet != null && session == null) {
                Button(
                    onClick = {
                        scope.launch { viewModel.onImageSelected(demoSheet.imageLocation) }
                    },
                    enabled = !state.busy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .onGloballyPositioned { demoScanButtonBounds(it.boundsInRoot()) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ScanAccent,
                        contentColor = OnScanAccent,
                    ),
                ) {
                    Text(if (state.busy) "正在识别…" else "识别这张打印稿")
                }
            }

            if (!cameraGranted && demoSheet == null) {
                PermissionPanel(
                    denied = cameraDenied,
                    busy = state.busy || refreshingGallery,
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    onPickFromGallery = openGallery,
                )
            } else {
                SessionStrip(
                    session = session,
                    demoSheet = demoSheet,
                )
                if (demoSheet == null) {
                    CameraControls(
                        ready = controller.isReady,
                        busy = state.busy,
                        galleryBusy = refreshingGallery,
                        onCapture = { controller.takePhoto() },
                        onPickFromGallery = openGallery,
                        doneEnabled = session != null,
                        onDone = { session?.let { onOpenConfirm(it.sessionId) } },
                        onDoneBounds = confirmButtonBounds,
                    )
                } else if (session != null) {
                    Button(
                        onClick = { onOpenConfirm(session.sessionId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .onGloballyPositioned { confirmButtonBounds(it.boundsInRoot()) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ScanAccent,
                            contentColor = OnScanAccent,
                        ),
                    ) {
                        Text("完成，进入确认页")
                    }
                }
            }
        }
    }
}

/** 演示打印稿：整页 A4 居中展示在取景框内。 */
@Composable
private fun DemoSheetBackdrop(sheet: DemoScanSheet) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScanBackground),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .fillMaxHeight(0.62f),
            shape = RoundedCornerShape(6.dp),
            color = Color.White,
        ) {
            Image(
                bitmap = sheet.image,
                contentDescription = "已填涂的演示打印稿",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** 三分法参考线：帮助把整页打印稿平行对齐取景框。 */
@Composable
private fun ScanGuideLines() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val color = Color.White.copy(alpha = 0.22f)
        val stroke = 1.dp.toPx()
        listOf(1f / 3f, 2f / 3f).forEach { fraction ->
            drawLine(
                color = color,
                start = Offset(size.width * fraction, 0f),
                end = Offset(size.width * fraction, size.height),
                strokeWidth = stroke,
            )
            drawLine(
                color = color,
                start = Offset(0f, size.height * fraction),
                end = Offset(size.width, size.height * fraction),
                strokeWidth = stroke,
            )
        }
    }
}

/** 已扫页数与缩略图；右上角徽标显示页数。 */
@Composable
private fun SessionStrip(
    session: ScanSessionSummary?,
    demoSheet: DemoScanSheet?,
) {
    if (session == null && demoSheet == null) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (session == null) {
                "还没有识别到页面"
            } else {
                "已扫 ${session.pages.size} 页 · ${session.rowCount} 行"
            },
            style = MaterialTheme.typography.bodySmall,
            color = ScanMutedText,
            modifier = Modifier.weight(1f),
        )
        ScanThumbnail(
            image = demoSheet?.image,
            count = session?.pages?.size ?: 0,
        )
    }
}

@Composable
private fun ScanThumbnail(image: ImageBitmap?, count: Int) {
    Box {
        Surface(
            modifier = Modifier.size(54.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFEDE7D8),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        ) {
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                    val lineColor = Color(0xFF8C8B7E)
                    val heights = listOf(0.2f, 0.38f, 0.56f, 0.74f)
                    heights.forEach { y ->
                        drawLine(
                            color = lineColor.copy(alpha = 0.5f),
                            start = Offset(0f, size.height * y),
                            end = Offset(size.width, size.height * y),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                }
            }
        }
        if (count > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .size(20.dp)
                    .background(ScanAccent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = OnScanAccent,
                )
            }
        }
    }
}

/** 底部相机操作栏：相册 / 快门 / 完成。 */
@Composable
private fun CameraControls(
    ready: Boolean,
    busy: Boolean,
    galleryBusy: Boolean,
    onCapture: () -> Unit,
    onPickFromGallery: () -> Unit,
    doneEnabled: Boolean,
    onDone: () -> Unit,
    onDoneBounds: (Rect) -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth().height(84.dp)) {
        RoundIconButton(
            iconRes = R.drawable.ic_photo_library,
            contentDescription = "从相册选择",
            enabled = !busy && !galleryBusy,
            onClick = onPickFromGallery,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        // 快门：外圈白色描边，内圈主色；识别期间禁用。
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(72.dp)
                .clickable(enabled = ready && !busy, onClick = onCapture),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent, CircleShape)
                    .border(3.dp, if (ready && !busy) Color.White else Color.White.copy(alpha = 0.4f), CircleShape),
            )
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        if (ready && !busy) ScanAccent else ScanAccent.copy(alpha = 0.45f),
                        RoundedCornerShape(16.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_camera),
                    contentDescription = "拍照",
                    tint = OnScanAccent,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        if (doneEnabled) {
            Button(
                onClick = onDone,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .onGloballyPositioned { onDoneBounds(it.boundsInRoot()) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ScanAccent,
                    contentColor = OnScanAccent,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 18.dp,
                    vertical = 10.dp,
                ),
            ) {
                Text("完成 ›", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Spacer(modifier = Modifier.align(Alignment.CenterEnd).size(54.dp))
        }
    }
}

@Composable
private fun RoundIconButton(
    iconRes: Int,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(52.dp)
            .background(Color.White.copy(alpha = if (enabled) 0.14f else 0.06f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = ScanText.copy(alpha = if (enabled) 1f else 0.5f),
            modifier = Modifier.size(24.dp),
        )
    }
}

/** 相机权限被拒绝时的深色面板：仍保留相册回退。 */
@Composable
private fun PermissionPanel(
    denied: Boolean,
    busy: Boolean,
    onRequestPermission: () -> Unit,
    onPickFromGallery: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ScanOverlay,
        contentColor = ScanText,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "需要相机权限",
                style = MaterialTheme.typography.titleSmall,
                color = ScanText,
            )
            Text(
                text = if (denied) {
                    "相机权限已被拒绝。可再次授权，或直接从相册选择打印稿照片。"
                } else {
                    "扫描打印稿需要授权使用相机权限；也可以直接从相册选择。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = ScanMutedText,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ScanAccent,
                        contentColor = OnScanAccent,
                    ),
                ) {
                    Text(if (denied) "再次授权相机" else "授权相机")
                }
                OutlinedButton(
                    onClick = onPickFromGallery,
                    enabled = !busy,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ScanText),
                ) {
                    Text(if (busy) "正在刷新相册…" else "从相册选择")
                }
            }
        }
    }
}

@Composable
private fun BusyPill(text: String) {
    Surface(
        shape = CircleShape,
        color = ScanOverlay,
        contentColor = ScanText,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = ScanAccent,
            )
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = ScanText)
        }
    }
}

@Composable
private fun ScanMessageCard(message: String, isError: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isError) Color(0xFF5A1B14) else ScanOverlay,
        contentColor = ScanText,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = ScanText,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        )
    }
}

@Composable
private fun ScanErrorCard(
    error: ScanErrorView,
    onManual: () -> Unit,
    onCancel: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF4A1712),
        contentColor = ScanText,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "无法回读本页",
                style = MaterialTheme.typography.titleSmall,
                color = ScanText,
            )
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodySmall,
                color = ScanMutedText,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (error.manualAllowed) {
                    Button(
                        onClick = onManual,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ScanAccent,
                            contentColor = OnScanAccent,
                        ),
                    ) {
                        Text("手动输入")
                    }
                }
                TextButton(onClick = onCancel) {
                    Text("取消", color = ScanText)
                }
            }
        }
    }
}

@Composable
private fun ManualEntryCard(
    manual: ManualEntryState,
    onPageIdChange: (String) -> Unit,
    onQuestionNumberChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        contentColor = colors.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("手动输入", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "按页脚二维码旁的小字输入页面 ID，再填纸面题号。",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
            OutlinedTextField(
                value = manual.pageId,
                onValueChange = onPageIdInput(onPageIdChange),
                label = { Text("页面 ID") },
                placeholder = { Text("例如 K7M2Q9-01") },
                singleLine = true,
                enabled = !manual.busy,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = manual.questionNumber,
                onValueChange = onQuestionNumberChange,
                label = { Text("题号") },
                placeholder = { Text("例如 T-1") },
                singleLine = true,
                enabled = !manual.busy,
                modifier = Modifier.fillMaxWidth(),
            )
            manual.error?.let { error ->
                Text(error, color = colors.error)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onSubmit, enabled = !manual.busy) {
                    Text(if (manual.busy) "提交中…" else "提交")
                }
                OutlinedButton(onClick = onCancel, enabled = !manual.busy) {
                    Text("取消")
                }
            }
        }
    }
}

/** 页面 ID 只允许 ASCII 字母、数字与连字符；中文输入法的全角字符直接丢弃。 */
private fun onPageIdInput(change: (String) -> Unit): (String) -> Unit = { value ->
    change(value.filter { it == '-' || (it in '0'..'9') || (it in 'A'..'Z') || (it in 'a'..'z') })
}

/** 自动补扫相册所需的读取权限：Android 13 起改用按类型的媒体权限。 */
private fun galleryReadPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
