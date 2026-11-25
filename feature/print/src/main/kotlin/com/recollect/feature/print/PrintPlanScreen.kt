package com.recollect.feature.print

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.launch

/** 打印页日期与计划预览保持一致：`11月25日 · 周二`。 */
private fun formatPrintDay(date: LocalDate): String {
    val weekday = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }
    return "${date.monthValue}月${date.dayOfMonth}日 · $weekday"
}

/**
 * 打印计划页（app.md「屏幕清单与状态」）。
 *
 * 只展示计划与调起系统打印；实际纸张、打印服务选择由系统打印对话框负责。
 * 页面不直接访问 Context/PrintManager（R-03），打印通过 [PrintPlanViewModel] 的端口完成。
 *
 * 2026-11-25 排版调整：先给计划摘要，再把「打开系统打印」作为唯一主按钮；
 * 系统返回后，把「纸面是否已打印」的确认提升为主内容，避免用户误以为流程已经结束。
 */
@Composable
fun PrintPlanScreen(viewModel: PrintPlanViewModel) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when (val state = viewModel.state) {
            PrintPlanUiState.Empty -> Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "还没有可打印的计划",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "请先返回「计划预览」生成计划，再从预览页进入打印。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            is PrintPlanUiState.Content -> {
                PlanPrintSummaryCard(state)

                if (state.awaitingPrintedConfirmation) {
                    PrintedConfirmationCard(
                        onConfirm = { scope.launch { viewModel.confirmPrinted() } },
                        onDecline = { viewModel.declinePrinted() },
                        onReprint = { viewModel.print() },
                    )
                } else {
                    Button(
                        onClick = { viewModel.print() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_print),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("打开系统打印")
                    }

                    // 直接分享生成的 PDF（微信、QQ 等）；成功后与「已打印」一样记录打印快照。
                    OutlinedButton(
                        onClick = { scope.launch { viewModel.sharePdf() } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !state.pdfSharing,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_share),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (state.pdfSharing) "正在生成 PDF…" else "分享 PDF")
                    }

                    // Text(
                    //     text = "系统打印页为纵向 A4。请确认纸张与打印机后再点打印。",
                    //     style = MaterialTheme.typography.bodySmall,
                    //     color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // )
                }

                // 待确认期间，确认卡本身已经说明「请确认是否已完成打印」，
                // 不再重复显示同一条提示；真正的错误（如快照写入失败）仍然显示。
                if (!state.awaitingPrintedConfirmation || state.messageIsError) {
                    state.message?.let { message ->
                        MessageCard(message = message, isError = state.messageIsError)
                    }
                }
            }
        }
    }
}

/** 计划摘要：计划号、页数/题数与每日安排。 */
@Composable
private fun PlanPrintSummaryCard(state: PrintPlanUiState.Content) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
        border = BorderStroke(1.dp, colors.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(
                    text = "计划 ${state.input.planId}",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "共 ${state.pageCount} 页 / ${state.itemCount} 题",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
            HorizontalDivider(color = colors.outlineVariant)
            Text(
                text = "每日安排",
                style = MaterialTheme.typography.titleSmall,
            )
            state.days.forEach { day ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatPrintDay(day.date),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${day.itemCount} 题",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * 系统打印返回后的确认：文档生成不等于纸张已经打印（C-094 方案 B）。
 * 只有确认后，才会把每道题的最近打印页与二维码写回。
 */
@Composable
private fun PrintedConfirmationCard(
    onConfirm: () -> Unit,
    onDecline: () -> Unit,
    onReprint: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.large,
        color = colors.primaryContainer,
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.18f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "纸张打印好了吗？",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onPrimaryContainer,
            )
            Text(
                text = "确认「已打印」后，会记录每道题的打印页与二维码，供拍照回读使用。",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
            ) {
                Text("已打印")
            }
            OutlinedButton(
                onClick = onDecline,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("未打印")
            }
            TextButton(
                onClick = onReprint,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "重新打开系统打印",
                    color = colors.onPrimaryContainer,
                )
            }
        }
    }
}

/** 打印/分享结果：成功用主色，失败用错误容器，避免把两种结果混成一行灰字。 */
@Composable
private fun MessageCard(message: String, isError: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isError) colors.errorContainer else colors.secondaryContainer,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) colors.onErrorContainer else colors.onSecondaryContainer,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        )
    }
}
