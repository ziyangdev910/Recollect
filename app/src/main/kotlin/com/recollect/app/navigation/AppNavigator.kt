package com.recollect.app.navigation

import androidx.compose.runtime.mutableStateListOf

/**
 * 应用路由（app.md「DI 与导航」）。
 *
 * 不引入 navigation-compose；页面切换由 [AppNavigator] 的简单返回栈驱动。
 */
sealed interface AppRoute {
    data object Home : AppRoute
    /**
     * 错题本页面。[categoryId] 为 null 表示「全部错题」；否则只显示该知识点分类下的题目。
     * [categoryName] 用作导航栏标题。
     */
    data class Entry(
        val categoryId: String? = null,
        val categoryName: String? = null,
    ) : AppRoute
    data object KnowledgePoints : AppRoute
    data object SourceTags : AppRoute
    data object Progress : AppRoute
    /**
     * 添加分类 / 编辑封面。[categoryId] 为 null 时为新建；否则预填该分类并保存。
     */
    data class CategoryCreate(
        val categoryId: String? = null,
        val name: String? = null,
        val coverTone: Int? = null,
        val coverIcon: String? = null,
    ) : AppRoute
    data object PlanPreview : AppRoute
    data object Scan : AppRoute
    data class Confirm(val sessionId: String) : AppRoute
    data object Settings : AppRoute
    data object About : AppRoute
    data object License : AppRoute
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

    /**
     * 清空返回栈并落到目标页。
     *
     * 打印/确认完成后退回首页使用：这两条流程可能从计划预览或多层扫描页进入，
     * 只 pop 一层会回到中间页；重置后返回键直接交给系统。
     */
    fun resetTo(route: AppRoute) {
        backStack.clear()
        backStack.add(route)
    }
}
