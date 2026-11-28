package com.recollect.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.recollect.data.room.repository.KnowledgeCategoryRepository
import com.recollect.feature.entry.notebookTone
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.ziyangdev910.recollect.R

/** 编辑已有分类时预填的数据（入口：首页长按卡片 →「编辑封面」）。 */
internal data class CategoryEditTarget(
    val categoryId: String,
    val name: String,
    val coverTone: Int,
    val coverIcon: String,
)

/**
 * 「添加分类 / 编辑封面」页的状态（2026-11-27 用户要求）。
 *
 * 只负责名称、封面选择与保存调用；不持有 Context（R-03）。
 * 传入 [editTarget] 时按钮为「保存」并更新已有分类，否则为「创建」。
 */
internal class CategoryCreateViewModel(
    private val repository: KnowledgeCategoryRepository,
    private val editTarget: CategoryEditTarget? = null,
) {
    val editing: Boolean = editTarget != null

    var name by mutableStateOf(editTarget?.name.orEmpty())
        private set

    var selectedCoverIndex by mutableStateOf(
        editTarget?.let { target ->
            OFFICIAL_COVERS.indexOfFirst {
                it.toneIndex == target.coverTone && it.iconKey == target.coverIcon
            }
        }?.takeIf { it >= 0 } ?: 0,
    )
        private set

    var saving by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    val canCreate: Boolean
        get() = name.isNotBlank() && !saving

    val actionLabel: String
        get() = if (editing) "保存" else "创建"

    val savingLabel: String
        get() = if (editing) "保存中…" else "创建中…"

    fun updateName(value: String) {
        name = value
        error = null
    }

    fun selectCover(index: Int) {
        if (index in OFFICIAL_COVERS.indices) selectedCoverIndex = index
    }

    /** 创建/保存成功后返回 true；重名/空名等失败写入 [error]。 */
    suspend fun save(): Boolean {
        if (!canCreate) {
            if (name.isBlank()) error = "请输入学习本名称。"
            return false
        }
        saving = true
        error = null
        return try {
            val cover = OFFICIAL_COVERS[selectedCoverIndex]
            if (editTarget != null) {
                repository.updateDetails(
                    categoryId = editTarget.categoryId,
                    name = name,
                    coverTone = cover.toneIndex,
                    coverIcon = cover.iconKey,
                )
            } else {
                repository.create(
                    name = name,
                    coverTone = cover.toneIndex,
                    coverIcon = cover.iconKey,
                )
            }
            saving = false
            true
        } catch (e: CancellationException) {
            saving = false
            throw e
        } catch (e: Exception) {
            saving = false
            error = e.message ?: if (editing) "保存分类失败，请重试。" else "创建分类失败，请重试。"
            false
        }
    }
}

/**
 * 添加分类页（2026-11-27 用户要求，参照 reference/CardEditWindow.png）。
 *
 * 顶部为关闭与「创建」；中部是所选封面的实时预览；下方填写学习本名称，并从
 * 「选择封面」中选择由常用底色与提示图标组成的封面。不提供「我的封面」与「上传封面」。
 */
@Composable
internal fun CategoryCreateScreen(
    viewModel: CategoryCreateViewModel,
    onClose: () -> Unit,
    onCreated: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme
    val selectedCover = OFFICIAL_COVERS[viewModel.selectedCoverIndex]
    val tone = notebookTone(selectedCover.toneIndex, isSystemInDarkTheme())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = "关闭",
                    tint = colors.onSurface,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            val enabled = viewModel.canCreate
            Surface(
                shape = CircleShape,
                color = if (enabled) colors.primaryContainer else colors.surfaceVariant,
                contentColor = if (enabled) colors.primary else colors.onSurfaceVariant,
            ) {
                Text(
                    text = if (viewModel.saving) viewModel.savingLabel else viewModel.actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(enabled = enabled) {
                            scope.launch {
                                if (viewModel.save()) onCreated()
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 封面实时预览：与首页错题本卡片同一套底色、图标与圆角。
        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.44f)
                    .aspectRatio(0.8f),
                shape = RoundedCornerShape(24.dp),
                color = tone.container,
                contentColor = tone.content,
                border = BorderStroke(1.dp, tone.content.copy(alpha = 0.08f)),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = viewModel.name.ifBlank { "自定义" },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "0 道题",
                            style = MaterialTheme.typography.labelSmall,
                            color = tone.content.copy(alpha = 0.65f),
                        )
                    }
                    Icon(
                        painter = painterResource(selectedCover.iconRes),
                        contentDescription = null,
                        tint = tone.content.copy(alpha = 0.20f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .size(56.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "学习本名称",
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = viewModel.name,
            onValueChange = viewModel::updateName,
            placeholder = { Text("自定义") },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            isError = viewModel.error != null,
            modifier = Modifier.fillMaxWidth(),
        )
        viewModel.error?.let { message ->
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "选择封面",
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        CoverGrid(
            selectedIndex = viewModel.selectedCoverIndex,
            onSelect = viewModel::selectCover,
        )
    }
}

/** 选择封面网格：4 列，每格是一种底色 + 提示图标的常用封面。 */
@Composable
private fun CoverGrid(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OFFICIAL_COVERS.chunked(4).forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                row.forEachIndexed { columnIndex, cover ->
                    val index = rowIndex * 4 + columnIndex
                    CoverTile(
                        cover = cover,
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(4 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CoverTile(
    cover: OfficialCover,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val tone = notebookTone(cover.toneIndex, isSystemInDarkTheme())
    Surface(
        modifier = modifier
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = tone.container,
        contentColor = tone.content,
        border = if (selected) {
            BorderStroke(2.dp, colors.primary)
        } else {
            BorderStroke(1.dp, tone.content.copy(alpha = 0.08f))
        },
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                painter = painterResource(cover.iconRes),
                contentDescription = null,
                tint = tone.content.copy(alpha = 0.30f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(26.dp),
            )
        }
    }
}
