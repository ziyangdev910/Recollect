package com.recollect.data.room.entity

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
