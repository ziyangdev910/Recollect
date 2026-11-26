package com.recollect.app.onboarding

import android.content.Context
import com.recollect.data.room.export.DatabaseExporter
import com.recollect.data.room.export.DatabaseImporter
import com.recollect.data.room.export.ImportResult
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 演示数据准备 / 还原的结果。 */
internal sealed interface DemoDataResult {
    data object Success : DemoDataResult

    data class Failure(val message: String) : DemoDataResult
}

/**
 * 首次复习演示的数据管理（2026-11-26 用户要求）。
 *
 * 演示不改动数据库文件本身，而是复用已有的备份导出/覆盖导入：
 * 1. 启动演示前，把当前数据库导出成 JSON 快照写到应用私有目录；
 * 2. 用打包在 assets 的初中示例题库覆盖当前数据；
 * 3. 演示结束（或用户跳过）时，再把快照覆盖导入，恢复演示前的空数据库。
 *
 * 快照在演示期间一直保留：进程被杀后下次启动先还原，再重新开始演示，
 * 避免示例数据残留成用户的真实数据库。
 */
internal class DemoDataStore(
    context: Context,
    private val exporter: DatabaseExporter,
    private val importer: DatabaseImporter,
) {

    private val assets = context.assets
    private val snapshotFile = File(
        File(context.filesDir, SNAPSHOT_DIR),
        SNAPSHOT_NAME,
    )
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** 用户是否已经看过（或选择跳过）首次演示。 */
    fun isCompleted(): Boolean = preferences.getBoolean(KEY_COMPLETED, false)

    /**
     * 标记演示已完成；只在数据库成功还原后调用。
     *
     * 用 `commit` 同步落盘：完成标记决定下次启动是否还会再次演示，
     * 这里宁可多等一次小文件的写入，也不要让刚看完演示的用户重启后又看一遍。
     */
    fun markCompleted() {
        preferences.edit().putBoolean(KEY_COMPLETED, true).commit()
    }

    /** 是否存在一份尚未还原的演示前快照（进程在演示中被杀）。 */
    fun hasPendingSnapshot(): Boolean = snapshotFile.exists()

    /**
     * 导出当前数据库并覆盖导入示例题库。
     *
     * 失败时不留下半份示例数据：导入本身是单事务，失败整体回滚；
     * 已在导入前写下的快照会被删除，避免下次启动误还原。
     */
    suspend fun prepare(): DemoDataResult = withContext(Dispatchers.IO) {
        if (snapshotFile.exists()) {
            return@withContext DemoDataResult.Failure(
                "检测到上次未完成的演示，请先恢复原数据库。",
            )
        }
        try {
            val snapshot = exporter.export().json
            writeSnapshot(snapshot)
            val demoJson = readAsset(DEMO_ASSET)
            when (val result = importer.apply(demoJson)) {
                is ImportResult.Success -> DemoDataResult.Success
                is ImportResult.Failure -> {
                    deleteSnapshot()
                    DemoDataResult.Failure(result.message)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // 导入失败时 deleteSnapshot 已在分支内处理；这里主要覆盖导出/读资产/写快照失败。
            runCatching { deleteSnapshot() }
            DemoDataResult.Failure(e.message ?: "示例数据准备失败。")
        }
    }

    /**
     * 用启动前的快照覆盖当前数据库。
     *
     * 没有快照时视为成功（例如演示准备阶段就失败、数据库从未被改写）。
     * 还原失败时保留快照，让下次启动继续尝试。
     */
    suspend fun restore(): DemoDataResult = withContext(Dispatchers.IO) {
        if (!snapshotFile.exists()) return@withContext DemoDataResult.Success
        try {
            val snapshot = snapshotFile.readText(Charsets.UTF_8)
            when (val result = importer.apply(snapshot)) {
                is ImportResult.Success -> {
                    deleteSnapshot()
                    DemoDataResult.Success
                }
                is ImportResult.Failure -> DemoDataResult.Failure(result.message)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DemoDataResult.Failure(e.message ?: "恢复数据库失败。")
        }
    }

    private fun readAsset(path: String): String =
        assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun writeSnapshot(json: String) {
        val parent = snapshotFile.parentFile ?: error("无法创建演示快照目录")
        if (!parent.exists() && !parent.mkdirs()) {
            error("无法创建演示快照目录")
        }
        // 先写临时文件再改名，避免进程在写入中途被杀后留下截断的 JSON。
        val temporary = File(parent, "$SNAPSHOT_NAME.tmp")
        temporary.writeText(json, Charsets.UTF_8)
        if (!temporary.renameTo(snapshotFile)) {
            temporary.copyTo(snapshotFile, overwrite = true)
            temporary.delete()
        }
    }

    private fun deleteSnapshot() {
        // 删除失败不影响本次还原结果；下次启动会再次覆盖导入同一份快照。
        snapshotFile.delete()
    }

    companion object {
        private const val PREFERENCES_NAME = "first_run_onboarding"
        private const val KEY_COMPLETED = "review_flow_demo_completed"
        private const val SNAPSHOT_DIR = "first-run-demo"
        private const val SNAPSHOT_NAME = "restore.json"
        private const val DEMO_ASSET = "demo/seed.json"
    }
}
