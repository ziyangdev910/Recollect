package com.recollect.data.room.repository

import com.recollect.data.room.dao.KnowledgeNumberSequenceDao
import com.recollect.data.room.dao.KnowledgePointDao
import com.recollect.data.room.dao.KnowledgePointTombstoneDao
import com.recollect.data.room.dao.QuestionDao
import com.recollect.data.room.dao.QuestionKnowledgeCrossRefDao
import com.recollect.data.room.entity.KnowledgeNumberSequence
import com.recollect.data.room.entity.KnowledgePoint
import com.recollect.data.room.entity.KnowledgePointTombstone
import com.recollect.model.Tier
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

/**
 * 知识点标签库的读写端口：标签库增删改查（D-12、entry.md、app.md「KnowledgePoints」）。
 *
 * 每个知识点就是一个标签；录入题目时从标签库选择标签，不再输入自由文本。
 * 编号分配、墓碑写入与实体删除都在一个事务内完成，编号全局唯一且不复用。
 */
interface KnowledgePointLibraryRepository {

    /** 标签库列表（按编号升序），附带未归档题目的引用数。 */
    suspend fun tags(): List<KnowledgePointTag>

    /** 新建标签；编号由 Repository 在事务内分配（D-12 / C-028）。 */
    suspend fun create(name: String): KnowledgePointTag

    /** 重命名标签；`number` 不变（E-07）。 */
    suspend fun rename(knowledgePointId: String, name: String)

    /** 删除标签；仍被未归档题目引用时拒绝，否则写墓碑后删除（D-12）。 */
    suspend fun deleteWithTombstone(knowledgePointId: String, reason: String = "deleted")
}

/** 标签库投影；[linkedQuestionCount] 只统计未归档题目，用于「被题目引用不可删」提示。 */
data class KnowledgePointTag(
    val id: String,
    val number: Int,
    val name: String,
    val linkedQuestionCount: Int,
)

/** 标签名称不合法（空白）。 */
class InvalidKnowledgePointNameException(message: String) : Exception(message)

/** 标签仍被未归档题目引用，禁止删除。 */
class KnowledgePointInUseException(message: String) : Exception(message)

/** 标签不存在（可能已被删除）。 */
class KnowledgePointNotFoundException(message: String) : Exception(message)

class RoomKnowledgePointLibraryRepository(
    private val transactionRunner: TransactionRunner,
    private val knowledgePointDao: KnowledgePointDao,
    private val crossRefDao: QuestionKnowledgeCrossRefDao,
    private val questionDao: QuestionDao,
    private val sequenceDao: KnowledgeNumberSequenceDao,
    private val tombstoneDao: KnowledgePointTombstoneDao,
    private val clock: Clock,
) : KnowledgePointLibraryRepository {

    override suspend fun tags(): List<KnowledgePointTag> {
        val activeQuestionIds = questionDao.active().mapTo(mutableSetOf()) { it.id }
        val countsByKnowledgePoint = crossRefDao.all()
            .filter { it.questionId in activeQuestionIds }
            .groupingBy { it.knowledgePointId }
            .eachCount()
        return knowledgePointDao.allOrderedByNumber().map {
            KnowledgePointTag(
                id = it.id,
                number = it.number,
                name = it.name,
                linkedQuestionCount = countsByKnowledgePoint[it.id] ?: 0,
            )
        }
    }

    override suspend fun create(name: String): KnowledgePointTag {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) throw InvalidKnowledgePointNameException("标签名称不能为空。")

        return transactionRunner.inTransaction {
            val number = allocateNumber()
            val entity = KnowledgePoint(
                id = UUID.randomUUID().toString(),
                number = number,
                name = trimmed,
                currentTier = Tier.UNKNOWN,
                continuousScore = 0.5f,
                effectiveFeedbackCount = 0.0f,
                nextDueDate = LocalDate.now(clock).plusDays(1),
                activeQuestionCount = 0,
                dormant = false,
                difficulty = null,
                halfLifeDays = null,
                createdAt = clock.instant(),
            )
            knowledgePointDao.insert(entity)
            KnowledgePointTag(
                id = entity.id,
                number = entity.number,
                name = entity.name,
                linkedQuestionCount = 0,
            )
        }
    }

    override suspend fun rename(knowledgePointId: String, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) throw InvalidKnowledgePointNameException("标签名称不能为空。")

        transactionRunner.inTransaction {
            val existing = knowledgePointDao.byId(knowledgePointId)
                ?: throw KnowledgePointNotFoundException("知识点不存在或已被删除。")
            knowledgePointDao.update(existing.copy(name = trimmed))
        }
    }

    override suspend fun deleteWithTombstone(knowledgePointId: String, reason: String) {
        transactionRunner.inTransaction {
            val existing = knowledgePointDao.byId(knowledgePointId)
                ?: throw KnowledgePointNotFoundException("知识点不存在或已被删除。")
            if (activeReferenceCount(knowledgePointId) > 0) {
                throw KnowledgePointInUseException(
                    "该知识点仍被题目引用，不能删除。请先解除题目的关联。",
                )
            }
            tombstoneDao.insert(
                KnowledgePointTombstone(
                    number = existing.number,
                    knowledgePointId = existing.id,
                    voidedAt = clock.instant(),
                    reason = reason,
                ),
            )
            knowledgePointDao.delete(existing.id)
        }
    }

    /**
     * 事务内分配下一个编号：读取序列（缺失则按现有最大编号/墓碑重建）→ +1 → 跳过墓碑 → 写回。
     *
     * 编号只增不减；删除标签不会让编号回到池子里（D-12 / E-05 / E-10）。
     */
    private suspend fun allocateNumber(): Int {
        val lastIssued = sequenceDao.get()?.lastIssued
            ?: maxOf(knowledgePointDao.maxNumber(), tombstoneDao.maxNumber(), 0).also {
                sequenceDao.upsert(KnowledgeNumberSequence(lastIssued = it))
            }
        var next = lastIssued + 1
        while (tombstoneDao.exists(next)) next++
        sequenceDao.updateLastIssued(next)
        return next
    }

    private suspend fun activeReferenceCount(knowledgePointId: String): Int {
        val activeQuestionIds = questionDao.active().mapTo(mutableSetOf()) { it.id }
        return crossRefDao.all().count {
            it.knowledgePointId == knowledgePointId && it.questionId in activeQuestionIds
        }
    }
}
