package com.recollect.common

/**
 * 屏幕状态（app.md「DI 与导航」）。
 *
 * 每个屏幕使用 `Loading` / `Content` / `Empty` / `Error`；错误必须可重试由调用方按 [Error.retryable] 决定。
 * 本类型保持纯 Kotlin，供 `feature` 模块的状态持有者与 Compose 页面共用。
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data class Error(val code: String, val message: String, val retryable: Boolean) : UiState<Nothing>
}

/**
 * 面向界面的错误（app.md「全局错误与恢复」）。
 *
 * [code] 只给程序/日志使用，界面展示 [message]；[retryable] 决定是否显示“重试”。
 */
data class UiError(
    val code: String,
    val message: String,
    val retryable: Boolean,
) {
    fun toUiState(): UiState.Error = UiState.Error(code = code, message = message, retryable = retryable)
}
