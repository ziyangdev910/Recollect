package com.recollect.app.navigation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * app.md「DI 与导航」：`AppNavigator` 用 `mutableStateOf` + 简单返回栈，不引入 navigation-compose。
 */
class AppNavigatorTest {

    @Test
    fun `push and pop walk the back stack and stop at the root`() {
        val navigator = AppNavigator()

        assertFalse(navigator.canPop)

        navigator.push(AppRoute.Entry(categoryId = "cat-math", categoryName = "数学"))
        assertTrue(navigator.canPop)
        navigator.push(AppRoute.Settings)
        assertTrue(navigator.canPop)
        assertEquals(AppRoute.Settings, navigator.current)

        assertTrue(navigator.pop())
        assertEquals(AppRoute.Entry(categoryId = "cat-math", categoryName = "数学"), navigator.current)
        assertTrue(navigator.canPop)

        assertTrue(navigator.pop())
        assertEquals(AppRoute.Home, navigator.current)
        assertFalse(navigator.canPop)

        assertFalse(navigator.pop())
        assertEquals(AppRoute.Home, navigator.current)
    }

    @Test
    fun `replace swaps the current route without dropping history`() {
        val navigator = AppNavigator(AppRoute.Confirm("session-a"))
        assertFalse(navigator.canPop)

        navigator.push(AppRoute.Home)
        navigator.replace(AppRoute.Settings)

        assertEquals(AppRoute.Settings, navigator.current)
        assertTrue(navigator.canPop)
        assertTrue(navigator.pop())
        assertEquals(AppRoute.Confirm("session-a"), navigator.current)
        assertFalse(navigator.canPop)
    }
}
