package com.recollect.feature.print

/**
 * `DICT_4X4_50` 的 0～3 号标记（docs/specs/print-template.md「ArUco 与 QR 几何」）。
 *
 * 6×6 模块：每边 1 模块黑边 + 4×4 数据位。图案由 OpenCV `Aruco.drawMarker` 在设备上导出，
 * 一次性固化为常量，打印层因此不需要把 OpenCV 带进 release 产物；
 * 原始导出见 `evidence/print/osv-spike/aruco-dict-4x4-50.txt`。
 * `1` = 黑，`0` = 白。
 */
object ArucoPattern {

    /** 模块数：4×4 数据位 + 每边 1 模块黑边。 */
    const val MODULES = 6

    private val PATTERNS: Map<Int, Array<String>> = mapOf(
        0 to arrayOf(
            "111111",
            "101001",
            "110101",
            "111001",
            "111011",
            "111111",
        ),
        1 to arrayOf(
            "111111",
            "111111",
            "100001",
            "101101",
            "101011",
            "111111",
        ),
        2 to arrayOf(
            "111111",
            "111001",
            "111001",
            "111011",
            "100101",
            "111111",
        ),
        3 to arrayOf(
            "111111",
            "101101",
            "101101",
            "110111",
            "110011",
            "111111",
        ),
    )

    /** 返回该 id 的 6×6 模块图案；`row` 自上而下、`col` 自左而右，`true` 为黑。 */
    fun modulesFor(id: Int): Array<BooleanArray>? {
        val rows = PATTERNS[id] ?: return null
        return Array(MODULES) { row ->
            BooleanArray(MODULES) { col -> rows[row][col] == '1' }
        }
    }
}
