package com.recollect.data.room.repository

import com.recollect.model.PlanPageRows
import com.recollect.pagination.PaginationFailureCode
import com.recollect.pagination.PaginationResult
import com.recollect.pagination.PlannedPage
import com.recollect.pagination.ScanItemGeometry
import com.recollect.scheduler.DeferredItem
import java.time.LocalDate

/**
 * 计划持久化草稿：`PlanRepository.savePlan` 的唯一输入（planning.md）。
 *
 * [knowledgeRowsByQuestionId] 是打印时冻结的每题行快照（C-071/C-072）：Repository 只编码，
 * 不从当前题目-知识点关联重算。`planning.md` 的 `PlanDraft` 未列出该字段，差异见 docs/conflicts.md C-074。
 */
data class PlanDraft(
    val planId: String,
    val pages: List<PlannedPage>,
    val knowledgeRowsByQuestionId: Map<String, PlanPageRows>,
    val scheduledDates: Map<String, LocalDate>, // questionId -> targetDate
    val coolingEventIdsToConsume: List<Long>,
    val deferred: List<DeferredItem>,
    // 回读几何快照（C-093 方案 A）：pageId → 该页每个题块的几何；旧草稿可为空。
    val geometryByPage: Map<String, List<ScanItemGeometry>> = emptyMap(),
    /** 计划头所属周；从分页日期推导，生产路径显式传入设置里的计划窗口。 */
    val weekStartDate: LocalDate = pages.minOfOrNull { it.date } ?: LocalDate.MIN,
    val weekEndDate: LocalDate = pages.maxOfOrNull { it.date } ?: LocalDate.MIN,
    /** 生成时冻结的打印元数据；进程重建后据此按原布局重印同一计划。 */
    val renderMetaByQuestionId: Map<String, PlanRenderMeta> = emptyMap(),
    /** 显式重新生成时被替代的旧计划；保存事务内标记为 SUPERSEDED。 */
    val supersedesPlanId: String? = null,
) {
    companion object {
        /**
         * C-047：分页失败在进入事务前转 [PlanGenerationException]，不产生任何草稿、不写库。
         */
        fun fromPagination(
            planId: String,
            result: PaginationResult,
            knowledgeRowsByQuestionId: Map<String, PlanPageRows>,
            scheduledDates: Map<String, LocalDate>,
            coolingEventIdsToConsume: List<Long>,
            deferred: List<DeferredItem>,
            geometryByPage: Map<String, List<ScanItemGeometry>> = emptyMap(),
            weekStartDate: LocalDate? = null,
            weekEndDate: LocalDate? = null,
            renderMetaByQuestionId: Map<String, PlanRenderMeta> = emptyMap(),
            supersedesPlanId: String? = null,
        ): PlanDraft = when (result) {
            is PaginationResult.Failure -> throw PlanGenerationException(result.code, result.message)
            is PaginationResult.Success -> PlanDraft(
                planId = planId,
                pages = result.pages,
                knowledgeRowsByQuestionId = knowledgeRowsByQuestionId,
                scheduledDates = scheduledDates,
                coolingEventIdsToConsume = coolingEventIdsToConsume,
                deferred = deferred,
                geometryByPage = geometryByPage,
                weekStartDate = weekStartDate
                    ?: result.pages.minOfOrNull { it.date }
                    ?: LocalDate.MIN,
                weekEndDate = weekEndDate
                    ?: result.pages.maxOfOrNull { it.date }
                    ?: LocalDate.MIN,
                renderMetaByQuestionId = renderMetaByQuestionId,
                supersedesPlanId = supersedesPlanId,
            )
        }
    }
}

/**
 * 计划生成失败（C-047）：`PaginationResult.Failure` 的唯一出口，调用方映射为可重试 UI 错误。
 *
 * planning.md 把该类型放在 feature/planning；data:room 无法反向依赖 feature，放置差异见
 * docs/conflicts.md C-074。
 */
class PlanGenerationException(
    val code: PaginationFailureCode,
    message: String,
) : Exception(message)
