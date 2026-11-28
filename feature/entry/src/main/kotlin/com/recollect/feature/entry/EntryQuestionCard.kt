package com.recollect.feature.entry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.recollect.common.formatQuestionLabelLines
import com.recollect.data.room.repository.EntrySummary

/**
 * 题目卡片：大标题分两行——来源（无来源时「编号X」）与「第X页 第Y题」；其下耗时与知识点标签并排，
 * 右下角可显示按当前列表顺序生成的临时编号「#N」。
 *
 * 错题本列表与「录入题目」底部抽屉的实时预览共用同一张卡片：
 * 预览不传 [onEdit]/[onDelete]/[displayNumber]，因此没有「更多」菜单、不可点击、不显示序号。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EntryQuestionCard(
    row: EntrySummary,
    modifier: Modifier = Modifier,
    displayNumber: Int? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    var menuExpanded by remember { mutableStateOf(false) }
    val hasMenu = onEdit != null || onDelete != null
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .then(if (onEdit != null) Modifier.clickable(onClick = onEdit) else Modifier),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLowest,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // 来源与「第X页 第Y题」分行显示；无来源时第一行为「编号X」。
                    val labelLines = formatQuestionLabelLines(row.sourcePage, row.questionNumber)
                    // 与「复习进度」页的题目卡片保持一致：16sp 的 titleMedium。
                    Text(
                        text = labelLines.first,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (labelLines.second.isNotBlank()) {
                        Text(
                            text = labelLines.second,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (hasMenu) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_more),
                                contentDescription = "题目操作",
                                tint = colors.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            if (onEdit != null) {
                                DropdownMenuItem(
                                    text = { Text("编辑") },
                                    onClick = {
                                        menuExpanded = false
                                        onEdit()
                                    },
                                )
                            }
                            if (onDelete != null) {
                                DropdownMenuItem(
                                    text = { Text("删除", color = colors.error) },
                                    onClick = {
                                        menuExpanded = false
                                        onDelete()
                                    },
                                )
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (row.estimatedReviewMinutes > 0) {
                        DurationChip(row.estimatedReviewMinutes)
                    }
                    row.knowledgeNames.forEach { name ->
                        KnowledgeChip(name)
                    }
                }
                if (displayNumber != null) {
                    // 临时序号：始终按当前列表从上到下的顺序生成，中性色，不参与题目数据。
                    Text(
                        text = "#$displayNumber",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp, bottom = 2.dp),
                    )
                }
            }
        }
    }
}

/** 耗时标签：中性底色，与赭石色知识点标签并排；时钟图标 + 分钟数。 */
@Composable
private fun DurationChip(minutes: Int) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.small,
        color = colors.surfaceContainerHighest,
        contentColor = colors.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_clock),
                contentDescription = null,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = "$minutes 分钟",
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
            )
        }
    }
}

/** 知识点标签：赭石色底，和主色的计划/按钮区分开。 */
@Composable
private fun KnowledgeChip(name: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.small,
        color = colors.secondaryContainer,
        contentColor = colors.onSecondaryContainer,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
