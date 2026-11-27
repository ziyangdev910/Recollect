package com.recollect.app

import org.ziyangdev910.recollect.R

/**
 * 「添加分类」页的官方封面（2026-11-27 用户要求）。
 *
 * 每个封面由一组常用的纸张底色与提示图标组成；封面在创建分类时写入
 * `KnowledgeCategory.coverTone/coverIcon`，首页错题本卡片据此渲染。
 * 图标以稳定的字符串 key 持久化，避免资源 id 随构建变化。
 */
internal data class OfficialCover(
    val toneIndex: Int,
    val iconKey: String,
    val iconRes: Int,
)

/** 16 个常用封面：纸张底色循环搭配学科/通用提示图标。 */
internal val OFFICIAL_COVERS: List<OfficialCover> = listOf(
    OfficialCover(0, "book", R.drawable.ic_subject_chinese),
    OfficialCover(1, "calculator", R.drawable.ic_subject_math),
    OfficialCover(2, "translate", R.drawable.ic_subject_english),
    OfficialCover(3, "bolt", R.drawable.ic_subject_physics),
    OfficialCover(4, "flask", R.drawable.ic_subject_chemistry),
    OfficialCover(5, "leaf", R.drawable.ic_subject_biology),
    OfficialCover(1, "columns", R.drawable.ic_subject_politics),
    OfficialCover(2, "clock", R.drawable.ic_subject_history),
    OfficialCover(3, "globe", R.drawable.ic_subject_geography),
    OfficialCover(4, "computer", R.drawable.ic_subject_it),
    OfficialCover(5, "wrench", R.drawable.ic_subject_technology),
    OfficialCover(0, "library", R.drawable.ic_home_library),
    // 2026-11-27 用户要求：联想补充 4 个常用封面，跟在现有封面之后。
    OfficialCover(0, "music", R.drawable.ic_cover_music),
    OfficialCover(4, "art", R.drawable.ic_cover_art),
    OfficialCover(1, "sports", R.drawable.ic_cover_sports),
    OfficialCover(5, "writing", R.drawable.ic_edit_cover),
)

/** 封面图标 key → 资源 id；null/未知 key 返回 null，由调用方回退到按分类名派生的图标。 */
internal fun coverIconRes(iconKey: String?): Int? = when (iconKey) {
    "book" -> R.drawable.ic_subject_chinese
    "calculator" -> R.drawable.ic_subject_math
    "translate" -> R.drawable.ic_subject_english
    "bolt" -> R.drawable.ic_subject_physics
    "flask" -> R.drawable.ic_subject_chemistry
    "leaf" -> R.drawable.ic_subject_biology
    "columns" -> R.drawable.ic_subject_politics
    "clock" -> R.drawable.ic_subject_history
    "globe" -> R.drawable.ic_subject_geography
    "computer" -> R.drawable.ic_subject_it
    "wrench" -> R.drawable.ic_subject_technology
    "library" -> R.drawable.ic_home_library
    "music" -> R.drawable.ic_cover_music
    "art" -> R.drawable.ic_cover_art
    "sports" -> R.drawable.ic_cover_sports
    "writing" -> R.drawable.ic_edit_cover
    else -> null
}

/** 分类名 → 默认封面图标 key；与 `subjectIconRes` 的匹配规则保持一致。 */
internal fun subjectIconKey(name: String): String {
    val normalized = name.trim()
    return when {
        normalized.contains("音乐") -> "music"
        normalized.contains("美术") || normalized.contains("绘画") -> "art"
        normalized.contains("体育") -> "sports"
        normalized.contains("作文") || normalized.contains("写作") -> "writing"
        normalized.contains("语文") -> "book"
        normalized.contains("数学") || normalized.contains("代数") || normalized.contains("几何") ->
            "calculator"
        normalized.contains("英语") || normalized.contains("英文") -> "translate"
        normalized.contains("物理") -> "bolt"
        normalized.contains("化学") -> "flask"
        normalized.contains("生物") -> "leaf"
        normalized.contains("政治") || normalized.contains("道德") || normalized.contains("法治") ->
            "columns"
        normalized.contains("历史") -> "clock"
        normalized.contains("地理") -> "globe"
        normalized.contains("信息") || normalized.contains("计算机") || normalized.contains("编程") ->
            "computer"
        normalized.contains("通用技术") || normalized.contains("技术") -> "wrench"
        else -> "library"
    }
}

/**
 * 把分类当前的封面解析成官方封面选项：
 * 优先精确匹配已保存的配色/图标；旧分类没有封面时按名称派生，未命中再回退第一项。
 */
internal fun resolveOfficialCover(
    name: String,
    coverTone: Int?,
    coverIcon: String?,
    fallbackTone: Int,
): OfficialCover {
    if (coverTone != null && coverIcon != null) {
        OFFICIAL_COVERS.firstOrNull {
            it.toneIndex == coverTone && it.iconKey == coverIcon
        }?.let { return it }
        OFFICIAL_COVERS.firstOrNull { it.iconKey == coverIcon }?.let { return it }
    }
    val key = subjectIconKey(name)
    val candidates = OFFICIAL_COVERS.filter { it.iconKey == key }
    return candidates.firstOrNull { it.toneIndex == fallbackTone }
        ?: candidates.firstOrNull()
        ?: OFFICIAL_COVERS.first()
}

/**
 * 未保存封面的分类使用的稳定底色：
 * 优先取同图标官方封面的底色，保证卡片与「编辑封面」预填一致，并且拖拽排序后颜色不变。
 */
internal fun defaultToneFor(name: String): Int =
    OFFICIAL_COVERS.firstOrNull { it.iconKey == subjectIconKey(name) }?.toneIndex
        ?: name.hashCode().mod(NOTEBOOK_TONE_COUNT)
