package com.recollect.feature.print

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 四角定位图案（print-template.md「ArUco 与 QR 几何」）：`DICT_4X4_50`、0～3 号、6×6 模块。
 */
class ArucoPatternTest {

    @Test
    fun `provides six by six markers for ids zero to three with a black border`() {
        for (id in 0..3) {
            val modules = requireNotNull(ArucoPattern.modulesFor(id)) { "缺少 id=$id" }
            assertEquals(6, modules.size)
            assertTrue(modules.all { it.size == 6 })
            // 每边 1 模块黑边，保证检测器能定位标记外框。
            for (i in 0 until 6) {
                assertTrue(modules[0][i], "id=$id 上边应为黑")
                assertTrue(modules[5][i], "id=$id 下边应为黑")
                assertTrue(modules[i][0], "id=$id 左边应为黑")
                assertTrue(modules[i][5], "id=$id 右边应为黑")
            }
        }
    }

    @Test
    fun `has no pattern outside the four corner ids`() {
        assertNull(ArucoPattern.modulesFor(4))
    }
}
