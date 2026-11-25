package com.recollect.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 拾知的设计系统：纸张与墨色。
 *
 * 产品是「纸数融合」的复习工具，界面以未漂白的纸张为底、松烟墨绿为唯一强调色，
 * 朱砂只用于逾期/需要注意的状态。颜色全部走 Material 3 角色，浅色与深色各自完整定义，
 * 避免回落到 Material 基线紫色。
 */
private val PaperLight = lightColorScheme(
    primary = Color(0xFF2C5547),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD8E8DF),
    onPrimaryContainer = Color(0xFF13332A),
    inversePrimary = Color(0xFFA9D2C0),
    secondary = Color(0xFF7D5B2B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF1E2C6),
    onSecondaryContainer = Color(0xFF3C2A10),
    tertiary = Color(0xFF9A3D2D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF6DCD2),
    onTertiaryContainer = Color(0xFF45140B),
    background = Color(0xFFF6F1E6),
    onBackground = Color(0xFF20231D),
    surface = Color(0xFFFBF7EE),
    onSurface = Color(0xFF20231D),
    surfaceVariant = Color(0xFFE9E2D3),
    onSurfaceVariant = Color(0xFF55584C),
    surfaceTint = Color(0xFF2C5547),
    inverseSurface = Color(0xFF30342C),
    inverseOnSurface = Color(0xFFF2EFE6),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFF77796C),
    outlineVariant = Color(0xFFD7D1C2),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFCF8EF),
    surfaceDim = Color(0xFFDCD5C6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF5EB),
    surfaceContainer = Color(0xFFF3EDE1),
    surfaceContainerHigh = Color(0xFFEDE6D8),
    surfaceContainerHighest = Color(0xFFE7DFD0),
)

private val InkDark = darkColorScheme(
    primary = Color(0xFFA9D2C0),
    onPrimary = Color(0xFF10352A),
    primaryContainer = Color(0xFF2C4F42),
    onPrimaryContainer = Color(0xFFD6E9DF),
    inversePrimary = Color(0xFF2C5547),
    secondary = Color(0xFFDFC18C),
    onSecondary = Color(0xFF432F12),
    secondaryContainer = Color(0xFF4B3A1F),
    onSecondaryContainer = Color(0xFFF1E2C6),
    tertiary = Color(0xFFEFB6A4),
    onTertiary = Color(0xFF5A1F13),
    tertiaryContainer = Color(0xFF5A2519),
    onTertiaryContainer = Color(0xFFF6DCD2),
    background = Color(0xFF121510),
    onBackground = Color(0xFFE7E4D7),
    surface = Color(0xFF191C16),
    onSurface = Color(0xFFE7E4D7),
    surfaceVariant = Color(0xFF2A2E26),
    onSurfaceVariant = Color(0xFFC3C1B3),
    surfaceTint = Color(0xFFA9D2C0),
    inverseSurface = Color(0xFFE7E4D7),
    inverseOnSurface = Color(0xFF30342C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF8D9082),
    outlineVariant = Color(0xFF3A3F36),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF383C33),
    surfaceDim = Color(0xFF121510),
    surfaceContainerLowest = Color(0xFF0C0F0B),
    surfaceContainerLow = Color(0xFF1D211B),
    surfaceContainer = Color(0xFF21251F),
    surfaceContainerHigh = Color(0xFF2B3028),
    surfaceContainerHighest = Color(0xFF363A32),
)

/**
 * 产品界面使用同一套无衬线字体，字号阶梯比品牌页更收敛（1.125–1.2）。
 * 中文正文行高放宽到 1.55–1.6，保证长段说明在手机上易读。
 */
private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.3).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)

/** 圆角只保留三档：控件 10dp、卡片 16dp、面板 20dp。 */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** 全局主题入口；浅色与深色跟随系统，但都使用「纸张 / 墨色」一套品牌色。 */
@Composable
fun RecollectTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) InkDark else PaperLight,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
