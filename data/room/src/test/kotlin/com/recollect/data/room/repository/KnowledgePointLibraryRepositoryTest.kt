package com.recollect.data.room.repository

import com.recollect.data.room.dao.KnowledgeNumberSequenceDao
import com.recollect.data.room.dao.KnowledgePointTombstoneDao
import com.recollect.data.room.dao.QuestionKnowledgeCrossRefDao
import com.recollect.data.room.entity.KnowledgeCategory
import com.recollect.data.room.entity.KnowledgeCategoryDefaults
import com.recollect.data.room.entity.KnowledgeNumberSequence
import com.recollect.data.room.entity.KnowledgePointTombstone
import com.recollect.data.room.entity.Question
import com.recollect.data.room.entity.QuestionKnowledgeCrossRef
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
 * 标签库增删改查与编号规则（D-12 / C-028；entry.md E-04/E-05/E-07/E-10）。
 */
class KnowledgePointLibraryRepositoryTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-20T08:00:00Z"), ZoneOffset.UTC)

    private class FakeCrossRefDao(private val stores: InMemoryStores) : QuestionKnowledgeCrossRefDao {
        override suspend fun insert(entity: QuestionKnowledgeCrossRef) {
            stores.questionLinks.getOrPut(entity.questionId) { mutableSetOf() } += entity.knowledgePointId
        }

        override suspend fun all(): List<QuestionKnowledgeCrossRef> =
            stores.questionLinks.flatMap { (questionId, ids) ->
                ids.map { QuestionKnowledgeCrossRef(questionId = questionId, knowledgePointId = it) }
            }

        override suspend fun forQuestion(questionId: String): List<QuestionKnowledgeCrossRef> =
            stores.questionLinks[questionId].orEmpty().map {
                QuestionKnowledgeCrossRef(questionId = questionId, knowledgePointId = it)
            }

        override suspend fun countForKnowledgePoint(knowledgePointId: String): Int =
            stores.questionLinks.values.count { knowledgePointId in it }

        override suspend fun deleteForQuestion(questionId: String) {
            stores.questionLinks.remove(questionId)
        }

        override suspend fun deleteForKnowledgePoint(knowledgePointId: String) {
            stores.questionLinks.values.forEach { it.remove(knowledgePointId) }
        }
    }

    private class FakeSequenceDao : KnowledgeNumberSequenceDao {
        var row: KnowledgeNumberSequence? = null

        override suspend fun get(): KnowledgeNumberSequence? = row

        override suspend fun upsert(entity: KnowledgeNumberSequence) {
            row = entity
        }

        override suspend fun updateLastIssued(lastIssued: Int) {
            row = KnowledgeNumberSequence(lastIssued = lastIssued)
        }
    }

    private class FakeTombstoneDao : KnowledgePointTombstoneDao {
        val rows = mutableListOf<KnowledgePointTombstone>()

        override suspend fun insert(entity: KnowledgePointTombstone) {
            rows += entity
        }

        override suspend fun exists(number: Int): Boolean = rows.any { it.number == number }

        override suspend fun maxNumber(): Int = rows.maxOfOrNull { it.number } ?: 0
    }

    private class Fixture {
        val stores = InMemoryStores()
        val knowledgePointDao = FakeKnowledgePointDao(stores)
        val categoryDao = FakeKnowledgeCategoryDao(stores)
        val questionDao = FakeQuestionDao(stores)
        val crossRefDao = FakeCrossRefDao(stores)
        val sequenceDao = FakeSequenceDao()
        val tombstoneDao = FakeTombstoneDao()

        init {
            stores.knowledgeCategories[CATEGORY_ID] = KnowledgeCategory(
                id = CATEGORY_ID,
                name = "语文",
                createdAt = Instant.parse("2026-11-01T00:00:00Z"),
            )
        }
    }

    private fun repository(fixture: Fixture, clock: Clock = this.clock) =
        RoomKnowledgePointLibraryRepository(
            transactionRunner = RollbackTransactionRunner(fixture.stores),
            knowledgePointDao = fixture.knowledgePointDao,
            categoryDao = fixture.categoryDao,
            crossRefDao = fixture.crossRefDao,
            questionDao = fixture.questionDao,
            sequenceDao = fixture.sequenceDao,
            tombstoneDao = fixture.tombstoneDao,
            clock = clock,
        )

    private fun question(id: String, archived: Boolean = false) = Question(
        id = id,
        pageId = "",
        questionNumber = id,
        qrPayload = "",
        archived = archived,
        lastScheduledDate = null,
        lastReviewedDate = null,
        estimatedReviewMinutes = 15,
        createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        archivedAt = null,
        sourcePage = "P1",
    )

    private fun knowledgePoint(id: String, number: Int, name: String) = KnowledgePoint(
        id = id,
        number = number,
        name = name,
        currentTier = Tier.UNKNOWN,
        continuousScore = 0.5f,
        effectiveFeedbackCount = 0.0f,
        nextDueDate = LocalDate.of(2026, 11, 21),
        activeQuestionCount = 0,
        dormant = false,
        difficulty = null,
        halfLifeDays = null,
        createdAt = Instant.parse("2026-11-01T00:00:00Z"),
    )

    companion object {
        private const val CATEGORY_ID = KnowledgeCategoryDefaults.DEFAULT_CATEGORY_ID
    }

    @Test
    fun `create allocates increasing numbers from an empty sequence`() = runBlocking {
        val fixture = Fixture()
        val repository = repository(fixture)

        val first = repository.create("分数", CATEGORY_ID)
        val second = repository.create("几何", CATEGORY_ID)

        assertEquals(1, first.number)
        assertEquals(2, second.number)
        assertEquals(listOf(1, 2), repository.tags().map { it.number })
        assertEquals(2, fixture.sequenceDao.row?.lastIssued)
    }

    @Test
    fun `empty sequence is seeded from existing numbers`() = runBlocking {
        val fixture = Fixture()
        val existing = knowledgePoint(id = "kp-old", number = 7, name = "旧标签")
        fixture.knowledgePointDao.insert(existing)

        val created = repository(fixture).create("新标签", CATEGORY_ID)

        assertEquals(8, created.number, "空序列表按现有最大编号种子（E-10）")
        assertEquals(8, fixture.sequenceDao.row?.lastIssued)
    }

    @Test
    fun `create skips tombstoned numbers`() = runBlocking {
        val fixture = Fixture()
        fixture.sequenceDao.upsert(KnowledgeNumberSequence(lastIssued = 1))
        fixture.tombstoneDao.insert(
            KnowledgePointTombstone(
                number = 2,
                knowledgePointId = "kp-deleted",
                voidedAt = clock.instant(),
                reason = "deleted",
            ),
        )

        val created = repository(fixture).create("分数", CATEGORY_ID)

        assertEquals(3, created.number, "新编号跳过墓碑编号（E-05）")
    }

    @Test
    fun `update keeps the number and can change the category`() = runBlocking {
        val fixture = Fixture()
        fixture.stores.knowledgeCategories["cat-math"] = KnowledgeCategory(
            id = "cat-math",
            name = "数学",
            createdAt = Instant.parse("2026-11-01T00:00:01Z"),
        )
        val repository = repository(fixture)
        val created = repository.create("分数", CATEGORY_ID)

        repository.update(created.id, "分式", "cat-math")

        val tag = repository.tags().single()
        assertEquals("分式", tag.name)
        assertEquals("cat-math", tag.categoryId, "编辑可修改分类（2026-11-23 用户要求）")
        assertEquals(created.number, tag.number, "重命名不改编号（E-07）")
    }

    @Test
    fun `create requires an existing category`() = runBlocking {
        val fixture = Fixture()
        val repository = repository(fixture)

        assertFailsWith<KnowledgeCategoryNotFoundException> {
            repository.create("分数", "cat-missing")
        }

        assertTrue(repository.tags().isEmpty(), "无效分类不得创建知识点")
    }

    @Test
    fun `delete is rejected while an unarchived question references the tag`() = runBlocking {
        val fixture = Fixture()
        val repository = repository(fixture)
        val created = repository.create("分数", CATEGORY_ID)
        fixture.questionDao.insert(question("q1"))
        fixture.crossRefDao.insert(
            QuestionKnowledgeCrossRef(questionId = "q1", knowledgePointId = created.id),
        )

        assertFailsWith<KnowledgePointInUseException> {
            repository.deleteWithTombstone(created.id)
        }

        assertEquals(1, repository.tags().size, "被引用时不得删除")
        assertTrue(fixture.tombstoneDao.rows.isEmpty(), "被引用时不得写墓碑")
    }

    @Test
    fun `delete writes a tombstone and the number is not reused`() = runBlocking {
        val fixture = Fixture()
        val repository = repository(fixture)
        val first = repository.create("分数", CATEGORY_ID)

        repository.deleteWithTombstone(first.id)
        val second = repository.create("几何", CATEGORY_ID)

        assertTrue(fixture.tombstoneDao.rows.single().number == first.number)
        assertEquals(first.number + 1, second.number, "删除后的编号不复用（E-05）")
    }

    @Test
    fun `archived references do not block deletion and are not counted`() = runBlocking {
        val fixture = Fixture()
        val repository = repository(fixture)
        val created = repository.create("分数", CATEGORY_ID)
        fixture.questionDao.insert(question("q-archived", archived = true))
        fixture.crossRefDao.insert(
            QuestionKnowledgeCrossRef(questionId = "q-archived", knowledgePointId = created.id),
        )

        assertEquals(0, repository.tags().single().linkedQuestionCount)

        repository.deleteWithTombstone(created.id)

        assertTrue(fixture.tombstoneDao.rows.isNotEmpty(), "归档题目不阻止删除")
    }

    @Test
    fun `tags orders by number and counts active references`() = runBlocking {
        val fixture = Fixture()
        val repository = repository(fixture)
        val first = repository.create("分数", CATEGORY_ID)
        val second = repository.create("几何", CATEGORY_ID)
        fixture.questionDao.insert(question("q1"))
        fixture.questionDao.insert(question("q2", archived = true))
        fixture.crossRefDao.insert(
            QuestionKnowledgeCrossRef(questionId = "q1", knowledgePointId = second.id),
        )
        fixture.crossRefDao.insert(
            QuestionKnowledgeCrossRef(questionId = "q2", knowledgePointId = second.id),
        )

        val tags = repository.tags()

        assertEquals(listOf(first.id, second.id), tags.map { it.id })
        assertEquals(0, tags[0].linkedQuestionCount)
        assertEquals(1, tags[1].linkedQuestionCount, "只统计未归档题目")
    }
}
