package com.recollect.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import org.ziyangdev910.recollect.R

/** 作者与开源仓库；「关于」页展示，仓库地址可点击在浏览器打开。 */
private const val AUTHOR = "GitHub @ziyangdev910"
private const val REPOSITORY_URL = "https://github.com/ziyangdev910/Recollect"

/** 本项目采用的开源许可证；「关于」页展示，正文见仓库 LICENSE 与 LICENSE-EXCEPTION。 */
private const val LICENSE_NAME = "GPL-3.0 with Classpath Exception"

/** 致谢中论文引用的 DOI 解析地址；DOI 文本可点击在系统浏览器打开。 */
private const val DOI_URL = "https://doi.org/10.1109/TKDE.2023.3251721"
private const val DOI_LABEL = "doi: 10.1109/TKDE.2023.3251721"

/**
 * 「关于」页（首页底部信息图标入口）。
 *
 * 展示应用图标、名称、版本号、作者、开源仓库、开源许可证（可进入全文页）、简要功能说明
 * 与致谢（记忆调度算法的学术来源）。
 * 不申请任何新权限，仓库链接通过系统浏览器打开。
 *
 * 2026-11-25 排版调整：信息项改为整行可点的设置式列表（右侧箭头提示可进入），
 * 说明与致谢分区展示，减少卡片层层嵌套。
 */
@Composable
fun AboutScreen(
    appName: String,
    versionName: String,
    onOpenLicense: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppIcon()
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = appName,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "v$versionName",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = colors.surfaceContainerLow,
            border = BorderStroke(1.dp, colors.outlineVariant),
        ) {
            Column {
                AboutInfoRow(label = "作者", value = AUTHOR)
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    color = colors.outlineVariant,
                )
                AboutInfoRow(
                    label = "开源仓库",
                    value = REPOSITORY_URL,
                    onClick = { uriHandler.openUri(REPOSITORY_URL) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 16.dp),
                    color = colors.outlineVariant,
                )
                AboutInfoRow(
                    label = "开源许可证",
                    value = LICENSE_NAME,
                    onClick = onOpenLicense,
                )
            }
        }

        AboutTextSection(
            title = "关于拾知",
            paragraphs = listOf(
                "拾知是一款面向初高中学生的纸数融合复习工具。手机录入题目与知识点，" +
                    "按每周到期情况生成可打印的 A4 复习计划；在纸上完成填涂后拍照回读，" +
                    "经人工确认更新复习状态，再生成下一份计划。",
                "计划按知识点调度，支持掌握度追踪、每日复习时长设置、" +
                    "知识点与题目来源标签管理，以及数据库的导入与导出。全程离线运行，" +
                    "识别与计划生成都在设备本地完成。",
            ),
        )

        AboutTextSection(
            title = "致谢",
            paragraphs = listOf(
                "本项目的记忆调度算法在思想上受到了 Jingyong Su 等人提出的 " +
                    "SSP-MMC-Plus 工作的启发。该算法的具体逻辑与评估方法，请参阅其论文：",
            ),
            citation = buildCitation(linkColor = colors.primary, uriHandler = uriHandler),
            footnote = "本项目的记忆模型公式参考了墨墨团队以 MIT 许可开源的 SSP-MMC 实现；" +
                "排程与工程代码为独立编写，未复制或改编其实现代码。",
        )
    }
}

/**
 * 信息行：左侧标签、右侧值；可点击的行在右端显示箭头，并使用整行 48dp 以上的点击区。
 */
@Composable
private fun AboutInfoRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val rowModifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }
    Row(
        modifier = rowModifier.padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (onClick != null) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AboutTextSection(
    title: String,
    paragraphs: List<String>,
    citation: androidx.compose.ui.text.AnnotatedString? = null,
    footnote: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = colors.surfaceContainerLow,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            paragraphs.forEach { paragraph ->
                Text(
                    text = paragraph,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
            if (citation != null) {
                Text(
                    text = citation,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            if (footnote != null) {
                Text(
                    text = footnote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun buildCitation(
    linkColor: Color,
    uriHandler: androidx.compose.ui.platform.UriHandler,
) = buildAnnotatedString {
    append(
        "J. Su, J. Ye, L. Nie, Y. Cao and Y. Chen, \"Optimizing Spaced Repetition " +
            "Schedule by Capturing the Dynamics of Memory,\" in IEEE Transactions on " +
            "Knowledge and Data Engineering, vol. 35, no. 10, pp. 10085-10097, " +
            "1 Oct. 2023, ",
    )
    withLink(
        LinkAnnotation.Url(
            url = DOI_URL,
            styles = TextLinkStyles(
                style = SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                ),
            ),
            linkInteractionListener = { uriHandler.openUri(DOI_URL) },
        ),
    ) {
        append(DOI_LABEL)
    }
    append(".")
}

/**
 * 应用图标：用启动图标的墨绿渐变背景 + 前景 vector 合成。
 *
 * 不直接加载 adaptive-icon 的 mipmap：Compose 的 `painterResource` 在部分版本
 * 无法解析 adaptive-icon XML，用前景 vector 叠在品牌渐变上更稳妥。
 */
@Composable
private fun AppIcon() {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF3E6B5A), Color(0xFF1F4034)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "应用图标",
            modifier = Modifier.fillMaxSize(),
        )
    }
}
