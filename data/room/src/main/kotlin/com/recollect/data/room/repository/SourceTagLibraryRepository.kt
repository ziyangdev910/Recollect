package com.recollect.data.room.repository

import com.recollect.data.room.dao.QuestionDao
import com.recollect.data.room.dao.SourceTagDao
import com.recollect.data.room.entity.SourceTag
import java.time.Clock
import java.util.UUID

/**
 * 来源标签库的读写端口（2026-11-22 用户要求）。
 *
 * 录入题目时来源以单选标签填写；题目只保存标签名称，因此重命名需要在同一事务内
 * 同步更新题目的 `sourcePage`，删除前校验未被未归档题目引用。
 */
interface SourceTagLibraryRepository {

    /** 来源标签列表（按名称升序），附带未归档题目的引用数。 */
    suspend fun tags(): List<SourceTagItem>

    /** 新建来源标签；名称为空白或与已有标签重复时拒绝。 */
    suspend fun create(name: String): SourceTagItem

    /** 重命名来源标签，并同步更新引用该名称的题目。 */
    suspend fun rename(sourceTagId: String, name: String)

    /** 删除来源标签；仍被未归档题目引用时拒绝。 */
    suspend fun delete(sourceTagId: String)
}

/** 来源标签投影；[linkedQuestionCount] 只统计未归档题目。 */
data class SourceTagItem(
    val id: String,
    val name: String,
    val linkedQuestionCount: Int,
)

/** 来源标签名称为空白。 */
class InvalidSourceTagNameException(message: String) : Exception(message)

/** 来源标签名称与已有标签重复。 */
class DuplicateSourceTagException(message: String) : Exception(message)

/** 来源标签仍被未归档题目引用，禁止删除。 */
class SourceTagInUseException(message: String) : Exception(message)

/** 来源标签不存在（可能已被删除）。 */
class SourceTagNotFoundException(message: String) : Exception(message)

class RoomSourceTagLibraryRepository(
    private val transactionRunner: TransactionRunner,
    private val sourceTagDao: SourceTagDao,
    private val questionDao: QuestionDao,
    private val clock: Clock,
) : SourceTagLibraryRepository {

    override suspend fun tags(): List<SourceTagItem> {
        val counts = questionDao.active()
            .groupingBy { it.sourcePage.trim() }
            .eachCount()
        return sourceTagDao.all().map {
            SourceTagItem(
                id = it.id,
                name = it.name,
                linkedQuestionCount = counts[it.name] ?: 0,
            )
        }
    }

    override suspend fun create(name: String): SourceTagItem {
        val trimmed = requireName(name)

        return transactionRunner.inTransaction {
            if (sourceTagDao.all().any { it.name.equals(trimmed, ignoreCase = true) }) {
                throw DuplicateSourceTagException("来源标签「$trimmed」已存在。")
            }
            val entity = SourceTag(
                id = UUID.randomUUID().toString(),
                name = trimmed,
                createdAt = clock.instant(),
            )
            sourceTagDao.insert(entity)
            SourceTagItem(id = entity.id, name = entity.name, linkedQuestionCount = 0)
        }
    }

    override suspend fun rename(sourceTagId: String, name: String) {
        val trimmed = requireName(name)

        transactionRunner.inTransaction {
            val existing = sourceTagDao.byId(sourceTagId)
                ?: throw SourceTagNotFoundException("来源标签不存在或已被删除。")
            if (sourceTagDao.all().any {
                    it.id != sourceTagId && it.name.equals(trimmed, ignoreCase = true)
                }
            ) {
                throw DuplicateSourceTagException("来源标签「$trimmed」已存在。")
            }
            if (existing.name == trimmed) return@inTransaction
            sourceTagDao.update(existing.copy(name = trimmed))
            // 题目只保存名称：同一事务内把旧名称换成新名称，避免引用悬空。
            questionDao.renameSourcePageReferences(existing.name, trimmed)
        }
    }

    override suspend fun delete(sourceTagId: String) {
        transactionRunner.inTransaction {
            val existing = sourceTagDao.byId(sourceTagId)
                ?: throw SourceTagNotFoundException("来源标签不存在或已被删除。")
            if (questionDao.activeSourcePageCount(existing.name) > 0) {
                throw SourceTagInUseException(
                    "该来源仍被题目引用，不能删除。请先解除题目的来源。",
                )
            }
            sourceTagDao.delete(existing.id)
        }
    }

    private fun requireName(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) throw InvalidSourceTagNameException("来源标签名称不能为空。")
        return trimmed
    }
}
