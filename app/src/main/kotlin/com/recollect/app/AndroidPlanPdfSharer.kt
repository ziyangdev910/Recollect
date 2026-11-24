package com.recollect.app

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.recollect.feature.print.PdfShareOutcome
import com.recollect.feature.print.PlanPdfSharer
import com.recollect.feature.print.PrintLayoutBuilder
import com.recollect.feature.print.PrintPdfWriter
import com.recollect.feature.print.PrintPlanInput
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 「分享 PDF」端口实现（app 层）：生成 PDF 到缓存 → `FileProvider` → 系统分享面板。
 *
 * 与 [AndroidBackupSharer] 同一模式：只有 app 层持有 `Context`、`Intent` 与 `FileProvider`（R-03）；
 * 文件写入应用私有缓存目录，无需存储权限；分享时授予目标应用一次性只读权限，
 * 微信、QQ 等接收方通过 `content://` 读取文件。
 */
class AndroidPlanPdfSharer(context: Context) : PlanPdfSharer {

    private val appContext: Context = context.applicationContext

    override suspend fun share(input: PrintPlanInput): PdfShareOutcome = withContext(Dispatchers.IO) {
        try {
            val layout = PrintLayoutBuilder.build(
                planId = input.planId,
                pages = input.pages,
                questions = input.questions,
                noteTexts = input.noteTexts,
                template = input.template,
            )
            if (layout.pages.isEmpty()) {
                return@withContext PdfShareOutcome.Failed("没有可分享的页面；请先生成可打印的计划。")
            }

            val directory = File(appContext.cacheDir, SHARE_DIRECTORY).apply { mkdirs() }
            pruneOldExports(directory, keep = MAX_KEEP_FILES)
            val file = File(directory, "拾知周计划-${input.planId}.pdf")
            FileOutputStream(file).use { output ->
                PrintPdfWriter.write(
                    appContext,
                    layout,
                    PrintPdfWriter.a4PrintAttributes(),
                    output,
                )
            }

            val uri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                file,
            )
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "拾知周计划 ${input.planId}")
                // 部分分享目标（如微信旧版本）只认 ClipData 中的授权，两个都带上更稳妥。
                clipData = ClipData.newRawUri("拾知周计划", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            withContext(Dispatchers.Main) {
                appContext.startActivity(
                    Intent.createChooser(send, "分享周计划 PDF")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            PdfShareOutcome.Shared(fileName = file.name)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PdfShareOutcome.Failed(e.message ?: "无法打开系统分享面板。")
        }
    }

    /** 缓存目录只保留最近几次分享，避免重复分享长期占用空间。 */
    private fun pruneOldExports(directory: File, keep: Int) {
        val files = directory.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() }
            ?: return
        files.drop(keep).forEach { it.delete() }
    }

    private companion object {
        const val SHARE_DIRECTORY = "plan-pdf-exports"
        const val MAX_KEEP_FILES = 5
    }
}
