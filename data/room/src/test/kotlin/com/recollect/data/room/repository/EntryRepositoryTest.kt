package com.recollect.data.room.repository

import com.recollect.data.room.dao.QuestionKnowledgeCrossRefDao
import com.recollect.data.room.entity.KnowledgePoint
import com.recollect.data.room.entity.QuestionKnowledgeCrossRef
import com.recollect.model.Tier
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 录入事务：题目从标签库选择 1～4 个知识点（D-12；entry.md E-01/E-02/E-03）。
 *
 * Repository 是最后一道防线：UI 已拦截的情况下，直接调用仍要拒绝越界/悬空关联，且不留下半成品数据。
 */
class EntryRepositoryTest {

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

    private class Fixture {
        val stores = InMemoryStores()
        val knowledgePointDao = FakeKnowledgePointDao(stores)
        val questionDao = FakeQuestionDao(stores)
        val crossRefDao = FakeCrossRefDao(stores)

        fun repository(clock: Clock) = RoomEntryRepository(
            transactionRunner = RollbackTransactionRunner(stores),
            knowledgePointDao = knowledgePointDao,
            questionDao = questionDao,
            crossRefDao = crossRefDao,
            clock = clock,
        )

        suspend fun addKnowledgePoint(id: String, number: Int, name: String) {
            knowledgePointDao.insert(
                KnowledgePoint(
                    id = id,
                    number = number,
                    name = name,
                    currentTier = Tier.UNKNOWN,
                    continuousScore = 0.5f,
                    effectiveFeedbackCount = 0.0f,
                    nextDueDate = LocalDate.of(2026, 11, 21),
                    activeQuestionCount = 0,
                    dormant = true,
                    difficulty = null,
                    halfLifeDays = null,
                    createdAt = Instant.parse("2026-11-01T00:00:00Z"),
                ),
            )
        }
    }

    @Test
    fun `create links selected tags and recomputes active question count`() = runBlocking {
        val fixture = Fixture()
        fixture.addKnowledgePoint("kp-1", 1, "分数")
        fixture.addKnowledgePoint("kp-2", 2, "几何")
        fixture.addKnowledgePoint("kp-3", 3, "未选择")
        val repository = fixture.repository(clock)

        val summary = repository.create(
            EntryDraft(
                questionNumber = "T-1",
                sourcePageCode = 1,
                sourcePageLabel = "P12",
                knowledgePointIds = listOf("kp-2", "kp-1"),
            ),
        )

        assertEquals(listOf("分数", "几何"), summary.knowledgeNames)
        assertEquals(setOf("kp-1", "kp-2"), fixture.stores.questionLinks[summary.questionId])
        val first = fixture.knowledgePointDao.byId("kp-1")!!
        assertEquals(1, first.activeQuestionCount)
        assertFalse(first.dormant, "关联活跃题目后取消休眠（D-11）")
        assertEquals(0, fixture.knowledgePointDao.byId("kp-3")!!.activeQuestionCount)
        assertTrue(fixture.knowledgePointDao.byId("kp-3")!!.dormant)
    }

    @Test
    fun `more than four tags are rejected without writing a question`() = runBlocking {
        val fixture = Fixture()
        (1..5).forEach { fixture.addKnowledgePoint("kp-$it", it, "标签$it") }
        val repository = fixture.repository(clock)

        assertFailsWith<InvalidKnowledgePointLinkException> {
            repository.create(
                EntryDraft(
                    questionNumber = "T-1",
                sourcePageCode = 1,
                    sourcePageLabel = "P12",
                    knowledgePointIds = (1..5).map { "kp-$it" },
                ),
            )
        }

        assertTrue(fixture.questionDao.active().isEmpty(), "越界时不得写入题目")
        assertTrue(fixture.stores.questionLinks.isEmpty())
    }

    @Test
    fun `unknown tag id is rejected without writing a question`() = runBlocking {
        val fixture = Fixture()
        fixture.addKnowledgePoint("kp-1", 1, "分数")
        val repository = fixture.repository(clock)

        assertFailsWith<InvalidKnowledgePointLinkException> {
            repository.create(
                EntryDraft(
                    questionNumber = "T-1",
                sourcePageCode = 1,
                    sourcePageLabel = "P12",
                    knowledgePointIds = listOf("kp-1", "kp-missing"),
                ),
            )
        }

        assertTrue(fixture.questionDao.active().isEmpty())
        assertTrue(fixture.stores.questionLinks.isEmpty())
    }

    @Test
    fun `empty tag selection is rejected`() = runBlocking {
        val fixture = Fixture()
        val repository = fixture.repository(clock)

        assertFailsWith<InvalidKnowledgePointLinkException> {
            repository.create(
                EntryDraft(
                    questionNumber = "T-1",
                sourcePageCode = 1,
                    sourcePageLabel = "P12",
                    knowledgePointIds = emptyList(),
                ),
            )
        }

        assertTrue(fixture.questionDao.active().isEmpty())
    }

    @Test
    fun `summaries resolve tag names in number order`() = runBlocking {
        val fixture = Fixture()
        fixture.addKnowledgePoint("kp-1", 1, "分数")
        fixture.addKnowledgePoint("kp-2", 2, "几何")
        val repository = fixture.repository(clock)
        repository.create(
            EntryDraft(
                questionNumber = "T-1",
                sourcePageCode = 1,
                sourcePageLabel = "P12",
                knowledgePointIds = listOf("kp-2", "kp-1"),
            ),
        )

        val summary = repository.summaries().single()

        assertEquals("T-1", summary.questionNumber)
        assertEquals("1-P12", summary.sourcePage)
        assertEquals(listOf("分数", "几何"), summary.knowledgeNames)
    }

    @Test
    fun `page code is user controlled and can be reused on the same page`() = runBlocking {
        val fixture = Fixture()
        fixture.addKnowledgePoint("kp-1", 1, "分数")
        val repository = fixture.repository(clock)

        val first = repository.create(
            EntryDraft(
                questionNumber = "T-1",
                sourcePageCode = 3,
                sourcePageLabel = "",
                knowledgePointIds = listOf("kp-1"),
            ),
        )
        val second = repository.create(
            EntryDraft(
                questionNumber = "T-2",
                sourcePageCode = 3,
                sourcePageLabel = "作业本",
                knowledgePointIds = listOf("kp-1"),
            ),
        )
        val third = repository.create(
            EntryDraft(
                questionNumber = "T-3",
                sourcePageCode = 4,
                sourcePageLabel = "练习",
                knowledgePointIds = listOf("kp-1"),
            ),
        )

        assertEquals(3, first.sourcePageCode)
        assertEquals("3", first.sourcePage, "说明为空时只显示编号，不带「-」")
        assertEquals(3, second.sourcePageCode, "同一页可复用同一编号")
        assertEquals("3-作业本", second.sourcePage)
        assertEquals("4-练习", third.sourcePage)
        assertEquals(4, repository.defaultSourcePageCode(), "默认取最近一次编号，不自增")
        assertEquals(listOf("3", "3-作业本", "4-练习"), repository.summaries().map { it.sourcePage })
    }

    @Test
    fun `page code below one is rejected`() = runBlocking {
        val fixture = Fixture()
        fixture.addKnowledgePoint("kp-1", 1, "分数")
        val repository = fixture.repository(clock)

        assertFailsWith<IllegalArgumentException> {
            repository.create(
                EntryDraft(
                    questionNumber = "T-1",
                    sourcePageCode = 0,
                    sourcePageLabel = "",
                    knowledgePointIds = listOf("kp-1"),
                ),
            )
        }

        assertTrue(fixture.questionDao.active().isEmpty())
    }

    @Test
    fun `update overwrites fields and re-points links`() = runBlocking {
        val fixture = Fixture()
        fixture.addKnowledgePoint("kp-1", 1, "分数")
        fixture.addKnowledgePoint("kp-2", 2, "几何")
        val repository = fixture.repository(clock)
        val created = repository.create(
            EntryDraft(
                questionNumber = "T-1",
                sourcePageCode = 1,
                sourcePageLabel = "P12",
                knowledgePointIds = listOf("kp-1"),
            ),
        )

        val updated = repository.update(
            questionId = created.questionId,
            draft = EntryDraft(
                questionNumber = "T-1b",
                sourcePageCode = 4,
                sourcePageLabel = "练习",
                knowledgePointIds = listOf("kp-2"),
                estimatedReviewMinutes = 30,
            ),
        )

        assertEquals("T-1b", updated.questionNumber)
        assertEquals("4-练习", updated.sourcePage)
        assertEquals(listOf("几何"), updated.knowledgeNames)
        assertEquals(30, updated.estimatedReviewMinutes)
        assertEquals(setOf("kp-2"), fixture.stores.questionLinks[created.questionId])

        val summary = repository.summaries().single()
        assertEquals("T-1b", summary.questionNumber)
        assertEquals("4-练习", summary.sourcePage)
        assertEquals(listOf("kp-2"), summary.knowledgePointIds)

        val oldTag = fixture.knowledgePointDao.byId("kp-1")!!
        assertEquals(0, oldTag.activeQuestionCount)
        assertTrue(oldTag.dormant, "失去唯一引用后休眠")
        val newTag = fixture.knowledgePointDao.byId("kp-2")!!
        assertEquals(1, newTag.activeQuestionCount)
        assertFalse(newTag.dormant)
    }

    @Test
    fun `update of a missing question is rejected`() = runBlocking {
        val fixture = Fixture()
        fixture.addKnowledgePoint("kp-1", 1, "分数")
        val repository = fixture.repository(clock)

        assertFailsWith<EntryQuestionNotFoundException> {
            repository.update(
                questionId = "missing",
                draft = EntryDraft(
                    questionNumber = "T-1",
                    sourcePageCode = 1,
                    knowledgePointIds = listOf("kp-1"),
                ),
            )
        }
    }

    @Test
    fun `archive hides the question and releases tag activity`() = runBlocking {
        val fixture = Fixture()
        fixture.addKnowledgePoint("kp-1", 1, "分数")
        val repository = fixture.repository(clock)
        val created = repository.create(
            EntryDraft(
                questionNumber = "T-1",
                sourcePageCode = 1,
                knowledgePointIds = listOf("kp-1"),
            ),
        )

        repository.archive(created.questionId)

        assertTrue(repository.summaries().isEmpty(), "归档题目不再出现在题库")
        val tag = fixture.knowledgePointDao.byId("kp-1")!!
        assertEquals(0, tag.activeQuestionCount)
        assertTrue(tag.dormant)
        assertEquals(
            true,
            fixture.questionDao.byId(created.questionId)?.archived,
            "题目只归档、不删除（D-11）",
        )
    }
}
