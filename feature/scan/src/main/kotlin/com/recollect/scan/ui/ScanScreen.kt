package com.recollect.scan.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 扫描回读页（app.md「Scan」、scan-pipeline.md）。
 *
 * 页内相机（C-096 方案 B）拍摄或相册选图 → 识别 → 写入待确认记录 → 进入确认页。
 * 相机权限被拒绝时保留相册选图作为回退；识别失败不写库，只提供「重拍 / 手动输入 / 取消」
 * （C-011 方案二）。页面自身不访问 OpenCV/ML Kit：图像管线由 [ScanViewModel] 注入的端口完成（R-03）。
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

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) scope.launch { viewModel.onImageSelected(uri.toString()) }
    }

    val openGallery: () -> Unit = {
        pickLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    val state = viewModel.state

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {

        if (cameraGranted) {
            ScanCameraPane(
                busy = state.busy,
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
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("从相册选择")
            }
        }

        if (state.busy || state.loading) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator()
                Text(if (state.busy) "正在识别：定位 → 二维码 → 填涂 → 笔记…" else "正在恢复扫描会话…")
            }
        }

        state.session?.let { session ->
            SessionCard(
                session = session,
                onOpenConfirm = { onOpenConfirm(session.sessionId) },
            )
        }

        state.message?.let { message ->
            Text(
                text = message,
                color = if (state.messageIsError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }

        state.error?.let { error ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("无法回读本页", style = MaterialTheme.typography.titleSmall)
                    Text(error.message)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (error.manualAllowed) {
                            Button(onClick = { viewModel.startManualInput() }) {
                                Text("手动输入")
                            }
                        }
                        TextButton(onClick = { viewModel.dismissError() }) {
                            Text("取消")
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
                text = "还没有识别到打印页。请拍摄带四角标记与二维码的打印稿。",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun CameraPermissionCard(
    denied: Boolean,
    onRequestPermission: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("需要相机权限", style = MaterialTheme.typography.titleSmall)
            Text(
                text = if (denied) {
                    "相机权限已被拒绝。可再次授权，或直接使用下方的「从相册选择」。"
                } else {
                    "扫描打印稿需要授权使用相机权限。"
                },
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "计划 ${session.planId} · 已扫 ${session.pages.size} 页 / ${session.rowCount} 行",
                style = MaterialTheme.typography.titleSmall,
            )
            session.pages.forEach { page ->
                val future = if (page.pageIsFuture) " · 未来日期" else ""
                Text("${page.pageId} · ${page.plannedDate} · ${page.rowCount} 行$future")
            }
            Button(
                onClick = onOpenConfirm,
                modifier = Modifier.fillMaxWidth(),
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
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("手动输入", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "按页脚二维码旁的小字输入页面 ID，再填纸面题号。",
                style = MaterialTheme.typography.bodySmall,
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
                Text(error, color = MaterialTheme.colorScheme.error)
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
