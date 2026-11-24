package com.recollect.app

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 打包在 `assets/licenses/` 下的三份许可证文本。
 *
 * 根目录的 `LICENSE`、`LICENSE-EXCEPTION`、`NOTICE` 是源文件；修改它们时必须同步
 * `app/src/main/assets/licenses/` 下的副本，否则 App 内展示的全文会与仓库不一致。
 */
data class LicenseTexts(
    val license: String,
    val exception: String,
    val notice: String,
)

/** 从 assets 读取许可证全文；读取失败返回可展示的提示，避免页面崩溃。 */
fun readLicenseTexts(context: Context): LicenseTexts = LicenseTexts(
    license = readLicenseAsset(context, "licenses/LICENSE.txt"),
    exception = readLicenseAsset(context, "licenses/LICENSE-EXCEPTION.txt"),
    notice = readLicenseAsset(context, "licenses/NOTICE.txt"),
)

private fun readLicenseAsset(context: Context, path: String): String =
    runCatching {
        context.assets.open(path).bufferedReader().use { it.readText() }
    }.getOrElse { error ->
        "无法读取 $path：${error.message ?: "未知错误"}"
    }

private enum class LicenseDocument(val title: String) {
    License("GPL-3.0"),
    Exception("例外条款"),
    Notice("NOTICE"),
}

/**
 * 开源许可证全文页：三个选项卡分别展示 GPL-3.0 全文、Classpath 例外与 NOTICE。
 *
 * 正文使用等宽小字号，整页可滚动；顶部返回由 [AppRoot] 的全局导航栏提供。
 */
@Composable
fun LicenseScreen(texts: LicenseTexts) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val documents = LicenseDocument.entries
    val body = when (documents[selectedIndex]) {
        LicenseDocument.License -> texts.license
        LicenseDocument.Exception -> texts.exception
        LicenseDocument.Notice -> texts.notice
    }

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedIndex) {
            documents.forEachIndexed { index, document ->
                Tab(
                    selected = selectedIndex == index,
                    onClick = { selectedIndex = index },
                    text = { Text(document.title) },
                )
            }
        }
        Text(
            text = body,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
