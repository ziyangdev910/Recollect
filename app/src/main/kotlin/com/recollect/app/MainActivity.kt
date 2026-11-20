package com.recollect.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.recollect.app.navigation.AppNavigator
import com.recollect.app.navigation.AppRoute

/**
 * 应用入口（app.md「DI 与导航」）。
 *
 * 只创建 [AppContainer]、[AppNavigator] 和 [AppRoot]；不直接访问
 * Room/CameraX/PrintManager/ML Kit（R-03）。
 */
class MainActivity : ComponentActivity() {

    private lateinit var container: AppContainer
    private lateinit var navigator: AppNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        container = AppContainer(applicationContext)
        // 扫描页尚未落地前的会话恢复入口：带 sessionId 启动时直接进入确认页。
        val initialRoute = intent.getStringExtra(EXTRA_CONFIRM_SESSION_ID)
            ?.takeIf { it.isNotBlank() }
            ?.let { AppRoute.Confirm(it) }
            ?: AppRoute.Home
        navigator = AppNavigator(initialRoute)

        setContent {
            AppRoot(container = container, navigator = navigator)
        }
    }

    companion object {
        const val EXTRA_CONFIRM_SESSION_ID = "confirmSessionId"
    }
}
