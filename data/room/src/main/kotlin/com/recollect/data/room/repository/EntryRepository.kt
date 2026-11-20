package com.recollect.data.room.repository

/**
 * 录入端口：一个事务内新建「1 道题 + 1～4 个知识点标签 + 关联」。
 *
 * 知识点不再以自由文本录入，而是从标签库（[KnowledgePointLibraryRepository]）选择已有标签。
 * 知识点编号由数据层在事务内分配（D-12：录入层不得自行生成或修改 `number`）。
 */
interface EntryRepository {
    suspend fun create(draft: EntryDraft): EntrySummary

    /** 覆盖已有题目的题号、来源页码、预估分钟与知识点关联；校验规则与 create 一致。 */
    suspend fun update(questionId: String, draft: EntryDraft): EntrySummary

    /** 归档题目（D-11：题目只归档、不硬删除）；退出调度并保留历史反馈。 */
    suspend fun archive(questionId: String)

    /** 当前题库（未归档）投影；用于 Entry 的空态/列表。 */
    suspend fun summaries(): List<EntrySummary>

    /**
     * 录入卡片默认显示的来源页码编号：沿用已使用的最大值（无数据时为 1）。
     * 编号不会在保存后自增；用户通过 +/− 自行调整，同一页的多道题共用编号。
     */
    suspend fun defaultSourcePageCode(): Int
}

data class EntryDraft(
    val questionNumber: String,
    /** 来源页码编号：用户通过 +/− 指定，最小 1；同一页可复用同一编号。 */
    val sourcePageCode: Int,
    /**
     * 来源页码的说明文字（用户可编辑、可空），如「作业本」「同步练习」。
     * 不是复习打印稿的 pageId。
     */
    val sourcePageLabel: String = "",
    /** 从标签库选择的标签 id；1～[MAX_KNOWLEDGE_POINTS] 个（D-12）。 */
    val knowledgePointIds: List<String>,
    val estimatedReviewMinutes: Int = DEFAULT_REVIEW_MINUTES,
) {
    companion object {
        const val DEFAULT_REVIEW_MINUTES = 15

        /** 单题关联知识点上限（D-12）。 */
        const val MAX_KNOWLEDGE_POINTS = 4
    }
}

data class EntrySummary(
    val questionId: String,
    val questionNumber: String,
    /** 来源页码编号（用户可调，同页可复用）。 */
    val sourcePageCode: Int,
    /** 来源页码的说明文字，可为空。 */
    val sourcePageLabel: String,
    val knowledgeNames: List<String>,
    /** 关联标签 id，与 [knowledgeNames] 同序（按编号升序）；用于编辑回填。 */
    val knowledgePointIds: List<String> = emptyList(),
    val estimatedReviewMinutes: Int = EntryDraft.DEFAULT_REVIEW_MINUTES,
    /** 是否已打印过复习稿；编辑时提示旧稿失效。 */
    val printed: Boolean = false,
) {
    /** 展示用页码：`编号-说明`；说明为空时只显示编号。 */
    val sourcePage: String
        get() = formatSourcePage(sourcePageCode, sourcePageLabel)
}

/** 单题关联标签不合法（为空、超过 4 个或指向不存在/已作废的知识点）。 */
class InvalidKnowledgePointLinkException(message: String) : Exception(message)

/** 要编辑/归档的题目不存在。 */
class EntryQuestionNotFoundException(message: String) : Exception(message)
