package com.recollect.scan.ui

import android.net.Uri
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File

/**
 * 扫描页相机控制句柄（C-096 方案 B）。
 *
 * 相机预览铺满整屏，快门按钮由页面统一排布；`ScanViewModel` 只接收拍摄文件的 URI 字符串（R-03）。
 */
@Stable
class ScanCameraController {
    internal var bound by mutableStateOf(false)
    internal var captureAction: (() -> Unit)? = null

    val isReady: Boolean get() = bound

    fun takePhoto() {
        if (bound) captureAction?.invoke()
    }
}

@Composable
fun rememberScanCameraController(): ScanCameraController = remember { ScanCameraController() }

/**
 * 全屏相机预览。
 *
 * CameraX 只在这里使用；照片写入 app 私有缓存，识别后由 `AndroidScanImageLoader` 删除。
 */
@Composable
fun ScanCameraPane(
    controller: ScanCameraController,
    busy: Boolean,
    onCaptured: (String) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnCaptured by rememberUpdatedState(onCaptured)
    val currentOnError by rememberUpdatedState(onError)

    val previewView = remember(context) {
        PreviewView(context).apply {
            // 铺满取景区域，让整页打印稿更容易对齐引导线。
            scaleType = PreviewView.ScaleType.FILL_CENTER
            // Compose 滚动容器与悬浮窗场景下，TextureView 比 SurfaceView 更稳定。
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    DisposableEffect(lifecycleOwner, context) {
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
                    controller.bound = true
                    controller.captureAction = {
                        imageCapture.targetRotation =
                            previewView.display?.rotation ?: Surface.ROTATION_0
                        val file = captureFile(context)
                        val options = ImageCapture.OutputFileOptions.Builder(file).build()
                        imageCapture.takePicture(
                            options,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    currentOnCaptured(Uri.fromFile(file).toString())
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    currentOnError(
                                        "拍照失败：${exception.message ?: "未知错误"}，请重试。",
                                    )
                                }
                            },
                        )
                    }
                } catch (e: Exception) {
                    controller.bound = false
                    currentOnError(
                        "无法启动相机：${e.message ?: "未知错误"}；可以改用相册选图。",
                    )
                }
            },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            disposed = true
            controller.bound = false
            controller.captureAction = null
            runCatching { provider?.unbindAll() }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier,
    )
}

private fun captureFile(context: android.content.Context): File {
    val directory = File(context.cacheDir, CAPTURE_DIRECTORY).apply { mkdirs() }
    return File(directory, "scan-${System.currentTimeMillis()}.jpg")
}

internal const val CAPTURE_DIRECTORY = "scan-captures"
