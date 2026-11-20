package com.recollect.app

import android.test.AndroidTestCase
import com.recollect.data.room.RecollectDatabase
import com.recollect.data.room.entity.DetectionStatus
import com.recollect.data.room.entity.PendingFeedback
import com.recollect.data.room.entity.ScanSession
import com.recollect.data.room.entity.ScanSessionStatus
import com.recollect.data.room.entity.UserDecision
import com.recollect.model.KnowledgeRowsCodec
import com.recollect.model.Tier
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import java.io.File
import java.time.Instant
import java.time.LocalDate

/**
 * 确认页设备证据准备（不是识别验收）。
 *
 * 从真实计划快照（`plan_page.questionIds` / `knowledgeRows`）生成一组 `PendingFeedback`，
 * 让确认页可以在实体机上逐行操作。识别档位是人工指定的，仅用于驱动确认页状态机；
 * 不用于 R-01 识别精度验收。
 *
 * 运行：
 * `adb shell am instrument -w -e class com.recollect.app.ConfirmSeedTest \
 *   com.recollect.test/android.test.InstrumentationTestRunner`
 *
 * 随后：
 * `adb shell am start -n com.recollect/.app.MainActivity --es confirmSessionId CONFIRM01`
 */
class ConfirmSeedTest : AndroidTestCase() {

    fun testSeedConfirmSession() = runBlocking {
        val sessionId = "CONFIRM01"
        val planId = "EH0MKS"
        restoreDatabaseFromExternalFiles()
        val database = RecollectDatabase.build(context)

        val pages = database.planPageDao().forPlan(planId)
        check(pages.isNotEmpty()) { "计划不存在：$planId（先通过 App 生成计划）" }

        database.openHelper.writableDatabase.execSQL(
            "DELETE FROM pending_feedback WHERE sessionId = ?",
            arrayOf(sessionId),
        )
        database.openHelper.writableDatabase.execSQL(
            "DELETE FROM scan_session WHERE sessionId = ?",
            arrayOf(sessionId),
        )
        database.scanSessionDao().insert(
            ScanSession(
                sessionId = sessionId,
                planId = planId,
                startedAt = Instant.now(),
                status = ScanSessionStatus.READY,
                submittedAt = null,
            ),
        )

        // 计划日固定为本周一，避免「未来日期的行只能本次未复习」影响四种决策的演示。
        val plannedDate = LocalDate.of(2026, 11, 16)
        val rows = mutableListOf<PendingFeedback>()
        pages.forEach { page ->
            val questionIds = JSONArray(page.questionIds).let { array ->
                (0 until array.length()).map(array::getString)
            }
            KnowledgeRowsCodec.decode(page.knowledgeRows).forEach { pageRows ->
                check(pageRows.questionId in questionIds) {
                    "题号不在打印快照内：${pageRows.questionId}"
                }
                pageRows.rows.forEach { knowledge ->
                    val triggered = knowledge.knowledgePointId in pageRows.triggeredKnowledgePointIds
                    // 与 UI 操作计划对应：3→接受(A)，4→修正(A)，2→本次未复习，1→补填(C)。
                    val tier = when (knowledge.number) {
                        1 -> Tier.FAMILIAR
                        2 -> null
                        3 -> Tier.MASTERED
                        4 -> Tier.UNKNOWN
                        else -> null
                    }
                    rows += PendingFeedback(
                        sessionId = sessionId,
                        planId = planId,
                        pageId = page.pageId,
                        plannedDate = plannedDate,
                        actualReviewDate = plannedDate,
                        questionId = pageRows.questionId,
                        knowledgePointId = knowledge.knowledgePointId,
                        triggered = triggered,
                        detectedTier = tier,
                        detectionStatus = if (tier == null) {
                            DetectionStatus.NO_MARK
                        } else {
                            DetectionStatus.OK
                        },
                        conflict = false,
                        detectedRawOcr = if (knowledge.number == 4) "手写：x+1=2" else null,
                        userDecision = UserDecision.UNMARKED,
                        selectedTier = null,
                        correctedText = null,
                        isSupplemental = false,
                    )
                }
            }
        }

        database.pendingFeedbackDao().insertAll(rows)
        val stored = database.pendingFeedbackDao().forSession(sessionId)
        check(stored.size == 4) { "待确认行数量异常：${stored.size}" }
        println("CONFIRM_SEED_OK sessionId=$sessionId rows=${stored.size}")
        database.close()
        File(context.getExternalFilesDir(null), DATABASE_BACKUP).delete()
    }

    /**
     * 设备上 `run-as` 无法写应用数据库（SELinux），因此把已生成的数据库放到应用外部目录，
     * 由运行在目标 app 进程内的本用例复制回 `databases/`。只是测试准备，不改生产代码。
     */
    private fun restoreDatabaseFromExternalFiles() {
        val source = File(checkNotNull(context.getExternalFilesDir(null)), DATABASE_BACKUP)
        check(source.exists()) { "缺少数据库备份：${source.absolutePath}" }
        val target = context.getDatabasePath(RecollectDatabase.DATABASE_NAME)
        target.parentFile?.mkdirs()
        target.delete()
        File(target.path + "-wal").delete()
        File(target.path + "-shm").delete()
        source.inputStream().use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
    }

    private companion object {
        const val DATABASE_BACKUP = "recollect.db"
    }
}
