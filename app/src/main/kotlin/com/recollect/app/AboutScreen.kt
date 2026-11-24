package com.recollect.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import org.ziyangdev910.recollect.R

/** 作者与开源仓库；「关于」页展示，仓库地址可点击在浏览器打开。 */
private const val AUTHOR = "@ziyangdev910"
private const val REPOSITORY_URL = "https://github.com/ziyangdev910/recollect"

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
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon()
        Text(
            text = appName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "版本 $versionName",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                InfoRow(label = "作者", value = AUTHOR)
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                InfoRow(label = "开源仓库", value = REPOSITORY_URL)
                // 用紧凑的可点击文本替代 TextButton：后者的 40dp 最小高度会把卡片撑高，
                // 导致关于页需要滚动；这里固定为约 28dp 的一行链接。
                Text(
                    text = "在浏览器中打开",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { uriHandler.openUri(REPOSITORY_URL) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                InfoRow(label = "开源许可证", value = LICENSE_NAME)
                Text(
                    text = "查看许可证全文",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenLicense)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "关于拾知",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "拾知是一款面向初高中学生的纸数融合复习工具。手机录入题目与知识点，" +
                        "按每周到期情况生成可打印的 A4 复习计划；在纸上完成填涂后拍照回读，" +
                        "经人工确认更新复习状态，再生成下一份计划。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = "计划按知识点调度，支持掌握度追踪、每日复习时长设置、" +
                        "知识点与题目来源标签管理，以及数据库的导入与导出。全程离线运行，" +
                        "识别与计划生成都在设备本地完成。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "致谢",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "本项目的记忆调度算法在思想上受到了 Jingyong Su 等人提出的 " +
                        "SSP-MMC-Plus 工作的启发。该算法的具体逻辑与评估方法，请参阅其论文：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
                // 引用中的 DOI 做成可点击链接：点按后用系统浏览器打开 doi.org 解析地址。
                val citation = buildAnnotatedString {
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
                                    color = colors.primary,
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
                Text(
                    text = citation,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    text = "本项目的记忆模型公式参考了墨墨团队以 MIT 许可开源的 SSP-MMC 实现；" +
                        "排程与工程代码为独立编写，未复制或改编其实现代码。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * 应用图标：用启动图标的背景色渐变 + 前景 vector 合成。
 *
 * 不直接加载 adaptive-icon 的 mipmap：Compose 的 `painterResource` 在部分版本
 * 无法解析 adaptive-icon XML，用前景 vector 叠在品牌渐变上更稳妥。
 */
@Composable
private fun AppIcon() {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF7C63B4), Color(0xFF4F378B)),
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

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}
