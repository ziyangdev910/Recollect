package com.recollect.feature.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.recollect.data.room.repository.KnowledgeCategoryItem

/**
 * 知识点分类筛选选项卡（2026-11-23 用户要求）。
 *
 * 「知识点」页与「录入题目」的知识点标签区共用：横向卡片，首项「全部」，
 * 选中某分类后只显示该分类下的知识点。[selectedCategoryId] 为 null 表示「全部」。
 */
@Composable
fun KnowledgeCategoryFilterRow(
    categories: List<KnowledgeCategoryItem>,
    selectedCategoryId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (categories.isEmpty()) return
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "all") {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onSelect(null) },
                label = { Text("全部") },
            )
        }
        items(categories, key = { it.id }) { category ->
            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = { onSelect(category.id) },
                label = { Text(category.name) },
            )
        }
    }
}
