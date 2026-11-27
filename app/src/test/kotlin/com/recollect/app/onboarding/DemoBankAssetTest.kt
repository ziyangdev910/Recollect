package com.recollect.app.onboarding

import com.recollect.data.room.dao.BackupRestoreDao
import com.recollect.data.room.export.ImportValidation
import com.recollect.data.room.export.RoomDatabaseImporter
import com.recollect.data.room.repository.TransactionRunner
import java.io.File
import java.lang.reflect.Proxy
import java.time.Clock
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 首次演示用的最小种子必须始终能被现有备份导入器接受。
 *
 * 种子只提供两个知识点与一个来源标签，题目由用户在预填表单中保存；
 * 如果 schema 字段或编号规则变化后资产文件没跟上，用户首次打开就会直接跳过演示。
 */
class DemoBankAssetTest {

    private val json: String by lazy {
        sequenceOf(
            File("src/main/assets/demo/seed.json"),
            File("app/src/main/assets/demo/seed.json"),
        ).first { it.exists() }.readText(Charsets.UTF_8)
    }

    @Test
    fun `bundled demo seed passes backup validation`() = runBlocking {
        val importer = RoomDatabaseImporter(
            restoreDao = unusedRestoreDao(),
            transactionRunner = object : TransactionRunner {
                override suspend fun <T> inTransaction(block: suspend () -> T): T = block()
            },
            clock = Clock.systemUTC(),
        )

        val result = importer.validate(json)
        assertTrue(result is ImportValidation.Valid, "演示种子必须能通过导入校验：$result")
        val summary = (result as ImportValidation.Valid).summary

        assertFalse(summary.degraded, "演示种子应与当前 schema 一致，不应走降级导入")
        assertEquals(11, summary.entityCounts["knowledgeCategories"])
        assertEquals(2, summary.entityCounts["knowledgePoints"])
        assertEquals(0, summary.entityCounts["questions"], "题目必须由用户在演示中保存，不能预置")
        assertEquals(0, summary.entityCounts["questionKnowledgeCrossRefs"])
        assertEquals(1, summary.entityCounts["sourceTags"])
        assertEquals(1, summary.entityCounts["appSettings"])
        assertEquals(0, summary.entityCounts["plans"])
        assertTrue(summary.skippedRows.isEmpty(), "演示种子不应含悬空关联：${summary.skippedRows}")
    }

    @Test
    fun `demo stops follow the five review stages in order`() {
        val stops = generateSequence(DemoTourStop.WELCOME) { it.next() }.toList()
        assertEquals(
            listOf(
                DemoTourStop.WELCOME,
                DemoTourStop.HOME_RECORD,
                DemoTourStop.FORM_NUMBER,
                DemoTourStop.FORM_PAGE_CODE,
                DemoTourStop.FORM_MINUTES,
                DemoTourStop.FORM_SOURCE,
                DemoTourStop.FORM_KNOWLEDGE,
                DemoTourStop.FORM_SAVE,
                DemoTourStop.ENTRY_PLAN,
                DemoTourStop.PLAN_PRINT,
                DemoTourStop.SCAN_SHEET,
                DemoTourStop.SCAN_CONFIRM,
                DemoTourStop.CONFIRM,
            ),
            stops,
        )
        assertNull(DemoTourStop.CONFIRM.next())
        assertEquals(DemoTourStop.HOME_RECORD, DemoTourStop.WELCOME.next())
        assertEquals("欢迎使用拾知！", DemoTourStop.WELCOME.title)
        assertEquals(
            listOf(1, 1, 1, 1, 1, 1, 1, 1, 2, 3, 4, 4, 5),
            stops.map { it.stage.number },
        )
        assertEquals("第 1 / 5 步", DemoTourStop.FORM_NUMBER.stage.progressLabel)
        assertEquals("第 5 / 5 步", DemoTourStop.CONFIRM.stage.progressLabel)
        // 第五步只有提交确认后才显示「开始使用」。
        assertEquals(DemoStopMode.FINISH, DemoTourStop.CONFIRM.mode)
    }

    /** validate 只读、不写库；任何 DAO 调用都说明校验路径被意外改坏。 */
    private fun unusedRestoreDao(): BackupRestoreDao =
        Proxy.newProxyInstance(
            BackupRestoreDao::class.java.classLoader,
            arrayOf(BackupRestoreDao::class.java),
        ) { _, method, _ ->
            error("演示种子校验不应写库：${method.name}")
        } as BackupRestoreDao
}
