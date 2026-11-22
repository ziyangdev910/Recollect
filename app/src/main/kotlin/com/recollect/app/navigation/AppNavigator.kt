package com.recollect.app.navigation

import androidx.compose.runtime.mutableStateListOf

/**
 * 应用路由（app.md「DI 与导航」）。
 *
 * 不引入 navigation-compose；页面切换由 [AppNavigator] 的简单返回栈驱动。
 */
sealed interface AppRoute {
    data object Home : AppRoute
    data object Entry : AppRoute
    data class QuestionEdit(val questionId: String?) : AppRoute
    data object KnowledgePoints : AppRoute
    data object SourceTags : AppRoute
    data object Progress : AppRoute
    data object PlanPreview : AppRoute
    data object Print : AppRoute
    data object Scan : AppRoute
    data class Confirm(val sessionId: String) : AppRoute
    data object Settings : AppRoute
    data object Backup : AppRoute
}

/**
 * 手写导航器：返回栈本身是 Compose 快照状态，栈顶即 [current]。
 *
 * 栈内始终保留根路由，因此 [canPop] 只在存在上一页时为 `true`。
 *
 * 整个返回栈必须是快照状态，而不能只有 [current]：`AppRoot` 顶层的
 * `BackHandler(enabled = canPop)` 依赖它来启用/停用返回拦截。若 [canPop]
 * 读取普通 `ArrayDeque`，该状态只在首次组合时求值一次（根路由时为 `false`），
 * 入栈后不会重新启用，系统返回键会直接结束 Activity 退回桌面。
 *
 * [pop] 在根路由返回 `false`，由调用方交给系统默认返回（例如结束 Activity）。
 */
class AppNavigator(initial: AppRoute = AppRoute.Home) {
    private val backStack = mutableStateListOf(initial)

    val current: AppRoute
        get() = backStack.last()

    val canPop: Boolean
        get() = backStack.size > 1

    fun push(route: AppRoute) {
        backStack.add(route)
    }

    fun pop(): Boolean {
        if (!canPop) return false
        backStack.removeAt(backStack.lastIndex)
        return true
    }

    fun replace(route: AppRoute) {
        backStack[backStack.lastIndex] = route
    }
}
