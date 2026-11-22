package com.recollect.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.recollect.app.navigation.AppNavigator
import com.recollect.app.navigation.AppRoute

/**
 * 应用入口（app.md「DI 与导航」）。
 *
 * 只创建 [AppContainer]、[AppNavigator] 和 [AppRoot]；不直接访问
 * Room/CameraX/PrintManager/ML Kit（R-03）。
 *
 * 除启动参数进入确认页外，还接收系统「用其他方式打开」json 文件的
 * `ACTION_VIEW`。该 URI 交给 `AppRoot` 的导入流程，跳过文件选择步骤。
 */
class MainActivity : ComponentActivity() {

    private lateinit var container: AppContainer
    private lateinit var navigator: AppNavigator

    /** 待处理的导入文件 URI；由 AppRoot 消费后置空，避免重组时重复导入。 */
    private val pendingImportUri = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        container = AppContainer(applicationContext)
        // 扫描页尚未落地前的会话恢复入口：带 sessionId 启动时直接进入确认页。
        val initialRoute = intent.getStringExtra(EXTRA_CONFIRM_SESSION_ID)
            ?.takeIf { it.isNotBlank() }
            ?.let { AppRoute.Confirm(it) }
            ?: AppRoute.Home
        navigator = AppNavigator(initialRoute)
        pendingImportUri.value = importUriFrom(intent)

        setContent {
            AppRoot(
                container = container,
                navigator = navigator,
                importUri = pendingImportUri.value,
                onImportUriConsumed = { pendingImportUri.value = null },
                onExternalImportFinished = {
                    // 从外部「用其他方式打开」完成导入后退出应用，回到打开前的界面。
                    finishAndRemoveTask()
                },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        importUriFrom(intent)?.let { pendingImportUri.value = it }
    }

    private fun importUriFrom(intent: Intent?): String? =
        if (intent?.action == Intent.ACTION_VIEW) intent.dataString else null

    companion object {
        const val EXTRA_CONFIRM_SESSION_ID = "confirmSessionId"
    }
}
