package com.recollect.scan.ui

import android.net.Uri
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File

/**
 * 扫描页内相机预览与拍摄（C-096 方案 B）。
 *
 * CameraX 只在这里使用；`ScanViewModel` 只接收拍摄文件的 URI 字符串（R-03）。
 * 照片写入 app 私有缓存，识别后由 `AndroidScanImageLoader` 删除。
 */
@Composable
fun ScanCameraPane(
    busy: Boolean,
    galleryRefreshing: Boolean,
    onCaptured: (String) -> Unit,
    onPickFromGallery: () -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember(context) {
        PreviewView(context).apply {
            // A4 取景框内保留完整传感器画面，避免边缘的四角标记被裁掉。
            scaleType = PreviewView.ScaleType.FIT_CENTER
            // Compose 滚动容器与悬浮窗场景下，TextureView 比 SurfaceView 更稳定；
            // SurfaceView 在拍照/布局变化后曾出现预览黑屏。
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    var ready by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var disposed = false
        future.addListener(
            {
                if (disposed) return@addListener
                try {
                    val cameraProvider = future.get()
                    provider = cameraProvider
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                    )
                    ready = true
                } catch (e: Exception) {
                    ready = false
                    onError("无法启动相机：${e.message ?: "未知错误"}；可以改用相册选图。")
                }
            },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            disposed = true
            ready = false
            runCatching { provider?.unbindAll() }
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "注意：1. 一次只拍一页\n2. 请完整拍摄整页，包括四角标记",
        )
        // 取景画面按 A4 纸长宽比（210:297）显示，方便把整页打印稿对齐进框。
        Card(modifier = Modifier.fillMaxWidth()) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(A4_RATIO),
            )
        }
        // 拍摄与相册选择横排在同一行，节省纵向空间。
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    imageCapture.targetRotation = previewView.display?.rotation ?: Surface.ROTATION_0
                    val file = captureFile(context)
                    val options = ImageCapture.OutputFileOptions.Builder(file).build()
                    imageCapture.takePicture(
                        options,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                onCaptured(Uri.fromFile(file).toString())
                            }

                            override fun onError(exception: ImageCaptureException) {
                                onError("拍照失败：${exception.message ?: "未知错误"}，请重试。")
                            }
                        },
                    )
                },
                enabled = ready && !busy,
                modifier = Modifier.weight(1f),
            ) {
                Text("拍摄")
            }
            OutlinedButton(
                onClick = onPickFromGallery,
                enabled = !busy && !galleryRefreshing,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (galleryRefreshing) "刷新中…" else "从相册选择")
            }
        }
    }
}

private fun captureFile(context: android.content.Context): File {
    val directory = File(context.cacheDir, CAPTURE_DIRECTORY).apply { mkdirs() }
    return File(directory, "scan-${System.currentTimeMillis()}.jpg")
}

internal const val CAPTURE_DIRECTORY = "scan-captures"

/** A4 纸长宽比：210mm × 297mm。 */
private const val A4_RATIO = 210f / 297f
