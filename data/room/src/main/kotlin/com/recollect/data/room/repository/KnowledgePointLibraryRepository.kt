package com.recollect.data.room.repository

import com.recollect.data.room.dao.KnowledgeCategoryDao
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

    /**
     * 新建标签并指定分类；编号由 Repository 在事务内分配（D-12 / C-028）。
     * 分类必须存在（2026-11-23 用户要求：添加知识点必须指定分类）。
     */
    suspend fun create(name: String, categoryId: String): KnowledgePointTag

    /** 编辑标签名称与分类；`number` 不变（E-07）。 */
    suspend fun update(knowledgePointId: String, name: String, categoryId: String)

    /** 删除标签；仍被未归档题目引用时拒绝，否则写墓碑后删除（D-12）。 */
    suspend fun deleteWithTombstone(knowledgePointId: String, reason: String = "deleted")
}

/** 标签库投影；[linkedQuestionCount] 只统计未归档题目，用于「被题目引用不可删」提示。 */
data class KnowledgePointTag(
    val id: String,
    val number: Int,
    val name: String,
    val categoryId: String,
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
    private val categoryDao: KnowledgeCategoryDao,
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
                categoryId = it.categoryId,
                linkedQuestionCount = countsByKnowledgePoint[it.id] ?: 0,
            )
        }
    }

    override suspend fun create(name: String, categoryId: String): KnowledgePointTag {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) throw InvalidKnowledgePointNameException("标签名称不能为空。")

        return transactionRunner.inTransaction {
            requireCategory(categoryId)
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
                categoryId = categoryId,
            )
            knowledgePointDao.insert(entity)
            KnowledgePointTag(
                id = entity.id,
                number = entity.number,
                name = entity.name,
                categoryId = entity.categoryId,
                linkedQuestionCount = 0,
            )
        }
    }

    override suspend fun update(knowledgePointId: String, name: String, categoryId: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) throw InvalidKnowledgePointNameException("标签名称不能为空。")

        transactionRunner.inTransaction {
            val existing = knowledgePointDao.byId(knowledgePointId)
                ?: throw KnowledgePointNotFoundException("知识点不存在或已被删除。")
            requireCategory(categoryId)
            knowledgePointDao.update(existing.copy(name = trimmed, categoryId = categoryId))
        }
    }

    /** 分类必须已存在：添加/编辑知识点时强制指定有效分类（2026-11-23 用户要求）。 */
    private suspend fun requireCategory(categoryId: String) {
        categoryDao.byId(categoryId)
            ?: throw KnowledgeCategoryNotFoundException("分类不存在或已被删除，请重新选择分类。")
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
