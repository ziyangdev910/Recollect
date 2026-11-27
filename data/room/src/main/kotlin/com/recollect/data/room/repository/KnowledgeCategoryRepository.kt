package com.recollect.data.room.repository

import com.recollect.data.room.dao.KnowledgeCategoryDao
import com.recollect.data.room.dao.KnowledgePointDao
import com.recollect.data.room.entity.KnowledgeCategory
import java.time.Clock
import java.util.UUID

/**
 * 知识点分类的读写端口（2026-11-23 用户要求）。
 *
 * 分类是知识点之上的一层分类，可按用户需要增删改；名称唯一。
 * 删除分类前校验其下没有知识点（含休眠知识点），避免产生无分类的知识点。
 */
interface KnowledgeCategoryRepository {

    /** 分类列表（按创建顺序），附带知识点数量与可选封面。 */
    suspend fun categories(): List<KnowledgeCategoryItem>

    /**
     * 新建分类。封面为可选：新页面创建的错题本会传入选中的配色与图标；
     * 知识点页的快捷新增不传，首页按分类名称自动派生封面。
     */
    suspend fun create(
        name: String,
        coverTone: Int? = null,
        coverIcon: String? = null,
    ): KnowledgeCategoryItem

    suspend fun rename(categoryId: String, name: String)

    /** 编辑分类的名称与封面（「编辑封面」页保存时使用）。 */
    suspend fun updateDetails(
        categoryId: String,
        name: String,
        coverTone: Int?,
        coverIcon: String?,
    )

    /** 按给定 id 顺序重排首页错题本卡片；ids 必须恰好覆盖现有全部分类。 */
    suspend fun reorder(categoryIds: List<String>)

    /** 删除分类；仍有知识点归属该分类时拒绝。 */
    suspend fun delete(categoryId: String)
}

/** 分类投影；[linkedKnowledgePointCount] 含休眠知识点，用于「有知识点不可删」提示。 */
data class KnowledgeCategoryItem(
    val id: String,
    val name: String,
    val linkedKnowledgePointCount: Int,
    val coverTone: Int? = null,
    val coverIcon: String? = null,
    val sortOrder: Int = 0,
)

/** 分类名称不合法（空白）。 */
class InvalidKnowledgeCategoryNameException(message: String) : Exception(message)

/** 已有同名分类。 */
class KnowledgeCategoryNameConflictException(message: String) : Exception(message)

/** 分类仍被知识点引用，禁止删除。 */
class KnowledgeCategoryInUseException(message: String) : Exception(message)

/** 分类不存在（可能已被删除）。 */
class KnowledgeCategoryNotFoundException(message: String) : Exception(message)

class RoomKnowledgeCategoryRepository(
    private val transactionRunner: TransactionRunner,
    private val categoryDao: KnowledgeCategoryDao,
    private val knowledgePointDao: KnowledgePointDao,
    private val clock: Clock,
) : KnowledgeCategoryRepository {

    override suspend fun categories(): List<KnowledgeCategoryItem> {
        val counts = knowledgePointDao.allOrderedByNumber()
            .groupingBy { it.categoryId }
            .eachCount()
        return categoryDao.all().map {
            KnowledgeCategoryItem(
                id = it.id,
                name = it.name,
                linkedKnowledgePointCount = counts[it.id] ?: 0,
                coverTone = it.coverTone,
                coverIcon = it.coverIcon,
                sortOrder = it.sortOrder,
            )
        }
    }

    override suspend fun create(
        name: String,
        coverTone: Int?,
        coverIcon: String?,
    ): KnowledgeCategoryItem {
        val trimmed = validateName(name)
        return transactionRunner.inTransaction {
            categoryDao.byName(trimmed)?.let {
                throw KnowledgeCategoryNameConflictException("已存在同名分类「$trimmed」。")
            }
            val nextOrder = (categoryDao.all().maxOfOrNull { it.sortOrder } ?: -1) + 1
            val entity = KnowledgeCategory(
                id = UUID.randomUUID().toString(),
                name = trimmed,
                createdAt = clock.instant(),
                coverTone = coverTone,
                coverIcon = coverIcon,
                sortOrder = nextOrder,
            )
            categoryDao.insert(entity)
            KnowledgeCategoryItem(
                id = entity.id,
                name = entity.name,
                linkedKnowledgePointCount = 0,
                coverTone = entity.coverTone,
                coverIcon = entity.coverIcon,
                sortOrder = entity.sortOrder,
            )
        }
    }

    override suspend fun rename(categoryId: String, name: String) {
        val trimmed = validateName(name)
        transactionRunner.inTransaction {
            val existing = categoryDao.byId(categoryId)
                ?: throw KnowledgeCategoryNotFoundException("分类不存在或已被删除。")
            categoryDao.byName(trimmed)?.let {
                if (it.id != existing.id) {
                    throw KnowledgeCategoryNameConflictException("已存在同名分类「$trimmed」。")
                }
            }
            categoryDao.update(existing.copy(name = trimmed))
        }
    }

    override suspend fun updateDetails(
        categoryId: String,
        name: String,
        coverTone: Int?,
        coverIcon: String?,
    ) {
        val trimmed = validateName(name)
        transactionRunner.inTransaction {
            val existing = categoryDao.byId(categoryId)
                ?: throw KnowledgeCategoryNotFoundException("分类不存在或已被删除。")
            categoryDao.byName(trimmed)?.let {
                if (it.id != existing.id) {
                    throw KnowledgeCategoryNameConflictException("已存在同名分类「$trimmed」。")
                }
            }
            categoryDao.update(
                existing.copy(
                    name = trimmed,
                    coverTone = coverTone,
                    coverIcon = coverIcon,
                ),
            )
        }
    }

    override suspend fun reorder(categoryIds: List<String>) {
        transactionRunner.inTransaction {
            val existing = categoryDao.all()
            if (categoryIds.size != existing.size || categoryIds.toSet() != existing.map { it.id }.toSet()) {
                throw IllegalArgumentException("分类顺序不完整，请刷新后重试。")
            }
            val byId = existing.associateBy { it.id }
            categoryIds.forEachIndexed { index, id ->
                categoryDao.update(byId.getValue(id).copy(sortOrder = index))
            }
        }
    }

    override suspend fun delete(categoryId: String) {
        transactionRunner.inTransaction {
            categoryDao.byId(categoryId)
                ?: throw KnowledgeCategoryNotFoundException("分类不存在或已被删除。")
            if (categoryDao.knowledgePointCount(categoryId) > 0) {
                throw KnowledgeCategoryInUseException(
                    "该分类下仍有知识点，不能删除。请先把这些知识点移到其他分类。",
                )
            }
            categoryDao.delete(categoryId)
        }
    }

    private fun validateName(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) throw InvalidKnowledgeCategoryNameException("分类名称不能为空。")
        return trimmed
    }
}
