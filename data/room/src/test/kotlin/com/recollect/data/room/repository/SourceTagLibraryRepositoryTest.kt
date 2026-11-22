package com.recollect.data.room.repository

import com.recollect.data.room.entity.Question
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 来源标签库增删改查（2026-11-22 用户要求）。
 *
 * 题目只保存来源名称，因此重命名必须在同一事务内同步题目引用；
 * 删除前只统计未归档题目的引用。
 */
class SourceTagLibraryRepositoryTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-22T08:00:00Z"), ZoneOffset.UTC)

    private inner class Fixture {
        val stores = InMemoryStores()
        val sourceTagDao = FakeSourceTagDao(stores)
        val questionDao = FakeQuestionDao(stores)

        fun repository() = RoomSourceTagLibraryRepository(
            transactionRunner = RollbackTransactionRunner(stores),
            sourceTagDao = sourceTagDao,
            questionDao = questionDao,
            clock = clock,
        )

        suspend fun addQuestion(
            id: String,
            sourcePage: String,
            archived: Boolean = false,
            createdAt: Instant = Instant.parse("2026-11-22T07:00:00Z"),
        ) {
            questionDao.insert(
                Question(
                    id = id,
                    pageId = "",
                    questionNumber = "T-$id",
                    qrPayload = "",
                    archived = archived,
                    lastScheduledDate = null,
                    lastReviewedDate = null,
                    createdAt = createdAt,
                    archivedAt = if (archived) createdAt else null,
                    sourcePage = sourcePage,
                    sourcePageCode = 1,
                ),
            )
        }
    }

    @Test
    fun `create trims the name and lists tags alphabetically`() = runBlocking {
        val fixture = Fixture()
        val repository = fixture.repository()

        repository.create("  作业本 ")
        repository.create("同步练习")

        val tags = repository.tags()
        assertEquals(setOf("作业本", "同步练习"), tags.map { it.name }.toSet())
        assertTrue(tags.none { it.name != it.name.trim() }, "名称应去除首尾空格")
    }

    @Test
    fun `blank or duplicate names are rejected`() = runBlocking {
        val fixture = Fixture()
        val repository = fixture.repository()
        repository.create("作业本")

        assertFailsWith<InvalidSourceTagNameException> { repository.create("   ") }
        assertFailsWith<DuplicateSourceTagException> { repository.create("作业本") }
        assertFailsWith<DuplicateSourceTagException> { repository.create("作业本 ") }
        assertEquals(1, repository.tags().size)
    }

    @Test
    fun `rename updates question references in the same transaction`() = runBlocking {
        val fixture = Fixture()
        val repository = fixture.repository()
        val tag = repository.create("作业本")
        fixture.addQuestion("q1", sourcePage = "作业本")
        fixture.addQuestion("q2", sourcePage = "作业本", archived = true)

        repository.rename(tag.id, "练习册")

        assertEquals("练习册", repository.tags().single().name)
        assertEquals("练习册", fixture.questionDao.byId("q1")!!.sourcePage)
        assertEquals("练习册", fixture.questionDao.byId("q2")!!.sourcePage, "归档题目也同步，取消归档后保持一致")
    }

    @Test
    fun `rename to an existing name is rejected without touching questions`() = runBlocking {
        val fixture = Fixture()
        val repository = fixture.repository()
        val first = repository.create("作业本")
        repository.create("练习册")
        fixture.addQuestion("q1", sourcePage = "作业本")

        assertFailsWith<DuplicateSourceTagException> { repository.rename(first.id, "练习册") }
        assertEquals("作业本", fixture.questionDao.byId("q1")!!.sourcePage)
        assertEquals(setOf("作业本", "练习册"), repository.tags().map { it.name }.toSet())
    }

    @Test
    fun `delete is rejected while active questions reference the tag`() = runBlocking {
        val fixture = Fixture()
        val repository = fixture.repository()
        val tag = repository.create("作业本")
        fixture.addQuestion("q1", sourcePage = "作业本")
        fixture.addQuestion("q2", sourcePage = "作业本", archived = true)

        assertEquals(1, repository.tags().single().linkedQuestionCount, "只统计未归档题目")
        assertFailsWith<SourceTagInUseException> { repository.delete(tag.id) }
        assertEquals(1, repository.tags().size)
    }

    @Test
    fun `delete succeeds when only archived questions reference the tag`() = runBlocking {
        val fixture = Fixture()
        val repository = fixture.repository()
        val tag = repository.create("作业本")
        fixture.addQuestion("q1", sourcePage = "作业本", archived = true)

        repository.delete(tag.id)

        assertTrue(repository.tags().isEmpty())
        assertEquals("作业本", fixture.questionDao.byId("q1")!!.sourcePage, "历史题目保留来源文本")
    }

    @Test
    fun `deleting or renaming a missing tag reports not found`() = runBlocking {
        val repository = Fixture().repository()

        assertFailsWith<SourceTagNotFoundException> { repository.delete("missing") }
        assertFailsWith<SourceTagNotFoundException> { repository.rename("missing", "新名称") }
    }
}
