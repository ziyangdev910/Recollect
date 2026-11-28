package com.recollect.feature.entry

import androidx.compose.ui.graphics.Color

/**
 * 错题本卡片配色（2026-11-28 用户要求）。
 *
 * 原先只服务首页卡片；「录入题目」底部抽屉的「科目」需要与主页卡片背景色一致，
 * 因此把同一套纸张底色移到 feature:entry，由首页与抽屉共用，避免两处各写一份。
 */
data class NotebookTone(val container: Color, val content: Color)

private val NOTEBOOK_TONES_LIGHT = listOf(
    Color(0xFFDCEBE1) to Color(0xFF1D3A2E),
    Color(0xFFDDE7F5) to Color(0xFF1E3049),
    Color(0xFFE8E0F3) to Color(0xFF34244A),
    Color(0xFFF6E3D5) to Color(0xFF4A2A18),
    Color(0xFFF3DFE3) to Color(0xFF4A1F2B),
    Color(0xFFEFE8D7) to Color(0xFF3E3522),
)

private val NOTEBOOK_TONES_DARK = listOf(
    Color(0xFF24342B) to Color(0xFFD6E9DF),
    Color(0xFF23303F) to Color(0xFFD7E3F5),
    Color(0xFF2E2740) to Color(0xFFE4DBF3),
    Color(0xFF3B2A20) to Color(0xFFF3DDCE),
    Color(0xFF3A2429) to Color(0xFFF1DCE1),
    Color(0xFF332F24) to Color(0xFFEDE6D3),
)

val NOTEBOOK_TONE_COUNT: Int = NOTEBOOK_TONES_LIGHT.size

fun notebookTone(index: Int, dark: Boolean): NotebookTone {
    val palette = if (dark) NOTEBOOK_TONES_DARK else NOTEBOOK_TONES_LIGHT
    val (container, content) = palette[index.mod(palette.size)]
    return NotebookTone(container, content)
}
