package com.recollect.feature.print

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 打印计划页（app.md「屏幕清单与状态」）。
 *
 * 只展示计划与调起系统打印；实际纸张、打印服务选择由系统打印对话框负责。
 * 页面不直接访问 Context/PrintManager（R-03），打印通过 [PrintPlanViewModel] 的端口完成。
 */
@Composable
fun PrintPlanScreen(viewModel: PrintPlanViewModel) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (val state = viewModel.state) {
            PrintPlanUiState.Empty -> {
                Text("还没有可打印的计划。", style = MaterialTheme.typography.titleMedium)
                Text("请先返回生成计划，再进入打印页。")
            }

            is PrintPlanUiState.Content -> {
                Text(
                    text = "计划 ${state.input.planId}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("共 ${state.pageCount} 页 / ${state.itemCount} 题")

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text("每日安排", style = MaterialTheme.typography.titleSmall)
                        state.days.forEach { day ->
                            Text("${day.date} · ${day.itemCount} 题")
                        }
                    }
                }

                Text(
                    text = "页面为纵向A4纸尺寸，请确认纸张与打印机。",
                    style = MaterialTheme.typography.bodySmall,
                )

                Button(
                    onClick = { viewModel.print() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("打开系统打印")
                }

                // 直接分享生成的 PDF（微信、QQ 等）；成功后与「已打印」一样记录打印快照。
                OutlinedButton(
                    onClick = { scope.launch { viewModel.sharePdf() } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.pdfSharing,
                ) {
                    Text(if (state.pdfSharing) "正在生成 PDF…" else "分享 PDF")
                }

                state.message?.let { message ->
                    Text(
                        text = message,
                        color = if (state.messageIsError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }

                // C-094 方案 B：文档生成不等于纸张打印完成，由用户确认后再写快照。
                if (state.awaitingPrintedConfirmation) {
                    // Text(
                    //     text = "只有确认纸张已经打印，才会记录每一题的最近打印页与二维码。",
                    //     style = MaterialTheme.typography.bodySmall,
                    // )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { scope.launch { viewModel.confirmPrinted() } },
                        ) {
                            Text("已打印")
                        }
                        OutlinedButton(onClick = { viewModel.declinePrinted() }) {
                            Text("未打印")
                        }
                    }
                }
            }
        }
    }
}
