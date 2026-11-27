package com.recollect.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * 知识点分类（2026-11-23 用户要求）：知识点之上的第一层分类。
 *
 * 名称唯一；允许增删改，删除前由 Repository 校验该分类下没有知识点。
 * `createdAt` 兼作展示顺序：默认分类按用户给定顺序种入（语文 → 通用技术），
 * 之后新增的分类排在最末。
 */
@Entity(
    tableName = "knowledge_category",
    indices = [Index(value = ["name"], unique = true)],
)
data class KnowledgeCategory(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Instant,
    /**
     * 首页错题本卡片的封面配色下标（0..5）；null 表示按分类顺序/名称自动派生。
     * schema v15 新增（2026-11-27 用户要求）。
     */
    val coverTone: Int? = null,
    /**
     * 首页错题本卡片的封面图标 key（如 `book`、`calculator`）；null 表示按分类名称自动派生。
     * 存字符串而不是资源 id，保证跨构建与备份稳定。
     */
    val coverIcon: String? = null,
    /**
     * 首页错题本卡片的拖拽排序；越小越靠前（schema v16，2026-11-27 用户要求）。
     * 旧分类默认 0，排序回退到 `createdAt`，不改变原有顺序。
     */
    @ColumnInfo(defaultValue = "0")
    val sortOrder: Int = 0,
)

/**
 * 默认分类：id 固定，迁移、空库初始化与旧备份导入共用同一份定义。
 *
 * 顺序即用户给定顺序：语文，数学，英语，物理，化学，生物，政治，历史，地理，信息技术，通用技术。
 * [DEFAULT_CATEGORY_ID] 用于把旧数据（schema v12 及更早）统一回填到「语文」。
 */
object KnowledgeCategoryDefaults {

    /** 旧知识点默认归入的分类 id（语文）。 */
    const val DEFAULT_CATEGORY_ID = "cat-chinese"

    /** Room `@ColumnInfo(defaultValue = ...)` 需要的 SQL 字面量形式。 */
    const val DEFAULT_CATEGORY_ID_SQL = "'cat-chinese'"

    /** 默认分类（id → 名称），按展示顺序排列。 */
    val NAMES: List<Pair<String, String>> = listOf(
        "cat-chinese" to "语文",
        "cat-math" to "数学",
        "cat-english" to "英语",
        "cat-physics" to "物理",
        "cat-chemistry" to "化学",
        "cat-biology" to "生物",
        "cat-politics" to "政治",
        "cat-history" to "历史",
        "cat-geography" to "地理",
        "cat-information-technology" to "信息技术",
        "cat-general-technology" to "通用技术",
    )
}
