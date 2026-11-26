package com.recollect.scan.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.recollect.scan.io.GalleryMediaRefresher
import kotlinx.coroutines.launch

/**
 * 扫描回读页（app.md「Scan」、scan-pipeline.md）。
 *
 * 页内相机（C-096 方案 B）拍摄或相册选图 → 识别 → 写入待确认记录 → 进入确认页。
 * 相机权限被拒绝时保留相册选图作为回退；识别失败不写库，只提供「重拍 / 手动输入 / 取消」
 * （C-011 方案二）。页面自身不访问 OpenCV/ML Kit：图像管线由 [ScanViewModel] 注入的端口完成（R-03）。
 *
 * 2026-11-25 排版调整：顶部先说明「拍摄 → 识别 → 确认」的流程；识别成功后用一张结果卡
 * 把「进入确认页」变成唯一主操作，错误与权限说明各自使用语义色容器。
 */
@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onOpenConfirm: (sessionId: String) -> Unit,
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

    // 首次进入只自动请求一次；拒绝后由权限卡片上的按钮再次请求。
    LaunchedEffect(Unit) {
        if (!cameraGranted && !autoRequested) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "拍摄整页打印稿，系统会自动定位并识别填涂与笔记，最后由你逐题确认。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (cameraGranted) {
            ScanCameraPane(
                busy = state.busy,
                galleryRefreshing = refreshingGallery,
                onCaptured = { location -> scope.launch { viewModel.onImageSelected(location) } },
                onPickFromGallery = openGallery,
                onError = { message -> viewModel.showMessage(message) },
            )
        } else {
            CameraPermissionCard(
                denied = cameraDenied,
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
            )
            OutlinedButton(
                onClick = openGallery,
                enabled = !state.busy && !refreshingGallery,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(if (refreshingGallery) "正在刷新相册…" else "从相册选择")
            }
        }

        if (state.busy || state.loading) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = if (state.busy) {
                            "正在识别：定位 → 二维码 → 填涂 → 笔记…"
                        } else {
                            "正在恢复扫描会话…"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }

        state.session?.let { session ->
            SessionCard(
                session = session,
                onOpenConfirm = { onOpenConfirm(session.sessionId) },
            )
        }

        state.message?.let { message ->
            MessageCard(message = message, isError = state.messageIsError)
        }

        state.error?.let { error ->
            val colors = MaterialTheme.colorScheme
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.errorContainer),
                border = BorderStroke(1.dp, colors.error.copy(alpha = 0.2f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "无法回读本页",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onErrorContainer,
                    )
                    Text(
                        text = error.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onErrorContainer,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (error.manualAllowed) {
                            Button(onClick = { viewModel.startManualInput() }) {
                                Text("手动输入")
                            }
                        }
                        TextButton(onClick = { viewModel.dismissError() }) {
                            Text("取消", color = colors.onErrorContainer)
                        }
                    }
                }
            }
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

        if (state.session == null && !state.loading && !state.busy) {
            Text(
                text = "还没有识别到打印页。请拍摄带四角标记与二维码的打印稿；" +
                    "也可以从相册选择已有照片。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CameraPermissionCard(
    denied: Boolean,
    onRequestPermission: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.secondaryContainer,
        border = BorderStroke(1.dp, colors.secondary.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "需要相机权限",
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSecondaryContainer,
            )
            Text(
                text = if (denied) {
                    "相机权限已被拒绝。可再次授权，或直接使用下方的「从相册选择」。"
                } else {
                    "扫描打印稿需要授权使用相机权限。"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSecondaryContainer,
            )
            Button(onClick = onRequestPermission) {
                Text(if (denied) "再次授权相机" else "授权相机")
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: ScanSessionSummary,
    onOpenConfirm: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.primaryContainer),
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(
                    text = "已识别到打印页",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "计划 ${session.planId} · 已扫 ${session.pages.size} 页 / ${session.rowCount} 行",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
            session.pages.forEach { page ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${page.pageId} · ${page.plannedDate} · ${page.rowCount} 行",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    if (page.pageIsFuture) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = colors.tertiaryContainer,
                            contentColor = colors.onTertiaryContainer,
                        ) {
                            Text(
                                text = "未来日期",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
            Button(
                onClick = onOpenConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
            ) {
                Text("进入确认页")
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        border = BorderStroke(1.dp, colors.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
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

/** 操作结果：成功用主色容器，失败用错误容器。 */
@Composable
private fun MessageCard(message: String, isError: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isError) colors.errorContainer else colors.primaryContainer,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) colors.onErrorContainer else colors.onPrimaryContainer,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        )
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
