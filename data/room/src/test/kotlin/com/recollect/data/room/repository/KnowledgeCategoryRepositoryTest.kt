package com.recollect.data.room.repository

import com.recollect.data.room.entity.KnowledgeCategory
import com.recollect.data.room.entity.KnowledgePoint
import com.recollect.model.Tier
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 知识点分类的增删改查（2026-11-23 用户要求）：名称唯一、有知识点不可删。
 */
class KnowledgeCategoryRepositoryTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-23T08:00:00Z"), ZoneOffset.UTC)

    private class Fixture {
        val stores = InMemoryStores()
        val categoryDao = FakeKnowledgeCategoryDao(stores)
        val knowledgePointDao = FakeKnowledgePointDao(stores)
    }

    private fun repository(fixture: Fixture) = RoomKnowledgeCategoryRepository(
        transactionRunner = RollbackTransactionRunner(fixture.stores),
        categoryDao = fixture.categoryDao,
        knowledgePointDao = fixture.knowledgePointDao,
        clock = clock,
    )

    private fun knowledgePoint(id: String, categoryId: String) = KnowledgePoint(
        id = id,
        number = 1,
        name = "分数",
        currentTier = Tier.UNKNOWN,
        continuousScore = 0.5f,
        effectiveFeedbackCount = 0f,
        nextDueDate = LocalDate.of(2026, 11, 24),
        activeQuestionCount = 0,
        dormant = false,
        difficulty = null,
        halfLifeDays = null,
        createdAt = Instant.parse("2026-11-20T00:00:00Z"),
        categoryId = categoryId,
    )

    @Test
    fun `create and rename a category`() = runBlocking {
        val fixture = Fixture()
        val repository = repository(fixture)

        val created = repository.create("数学")
        repository.rename(created.id, "高数")

        val categories = repository.categories()
        assertEquals(listOf("高数"), categories.map { it.name })
    }

    @Test
    fun `create keeps the selected cover`() = runBlocking {
        val repository = repository(Fixture())

        val created = repository.create(
            name = "数学",
            coverTone = 2,
            coverIcon = "calculator",
        )

        assertEquals(2, created.coverTone)
        assertEquals("calculator", created.coverIcon)
        val stored = repository.categories().single { it.id == created.id }
        assertEquals(2, stored.coverTone)
        assertEquals("calculator", stored.coverIcon)
    }

    @Test
    fun `reorder rewrites sort order and rejects incomplete sets`() = runBlocking {
        val repository = repository(Fixture())
        val first = repository.create("语文")
        val second = repository.create("数学")
        val third = repository.create("英语")

        repository.reorder(listOf(third.id, first.id, second.id))

        assertEquals(
            listOf("英语", "语文", "数学"),
            repository.categories().map { it.name },
        )
        assertFailsWith<IllegalArgumentException> {
            repository.reorder(listOf(first.id))
        }
    }

    @Test
    fun `update details changes name and cover`() = runBlocking {
        val repository = repository(Fixture())
        val created = repository.create("数学")

        repository.updateDetails(
            categoryId = created.id,
            name = "高等数学",
            coverTone = 3,
            coverIcon = "bolt",
        )

        val updated = repository.categories().single { it.id == created.id }
        assertEquals("高等数学", updated.name)
        assertEquals(3, updated.coverTone)
        assertEquals("bolt", updated.coverIcon)
    }

    @Test
    fun `duplicate category names are rejected`() = runBlocking {
        val fixture = Fixture()
        val repository = repository(fixture)
        repository.create("数学")

        assertFailsWith<KnowledgeCategoryNameConflictException> {
            repository.create(" 数学 ")
        }
    }

    @Test
    fun `a category with knowledge points cannot be deleted`() = runBlocking {
        val fixture = Fixture()
        fixture.stores.knowledgeCategories["cat-math"] = KnowledgeCategory(
            id = "cat-math",
            name = "数学",
            createdAt = clock.instant(),
        )
        fixture.stores.knowledgePoints["kp-1"] = knowledgePoint("kp-1", "cat-math")
        val repository = repository(fixture)

        assertEquals(1, repository.categories().single().linkedKnowledgePointCount)

        assertFailsWith<KnowledgeCategoryInUseException> {
            repository.delete("cat-math")
        }
        assertTrue(repository.categories().isNotEmpty(), "有知识点的分类不得删除")
    }

    @Test
    fun `an empty category can be deleted`() = runBlocking {
        val fixture = Fixture()
        val repository = repository(fixture)
        val created = repository.create("数学")

        repository.delete(created.id)

        assertTrue(repository.categories().isEmpty())
    }
}
