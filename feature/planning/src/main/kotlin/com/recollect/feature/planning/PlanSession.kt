package com.recollect.feature.planning

import com.recollect.data.room.repository.PlanRepository
import com.recollect.data.room.repository.StoredPlan

/**
 * 计划会话：区分“打开本周已有计划”与“显式重新生成”。
 *
 * - [activePlan]：读取该窗口当前的活动计划（不存在时为 null），供预览页选择默认周、
 *   判断计划生成后题库是否变化；
 * - [open]：本周已有 GENERATED/PRINTED 计划时加载原计划，绝不生成新计划号；
 * - [regenerate]：用户确认后生成新计划，并在保存事务内把旧计划标记为 SUPERSEDED。
 */
interface PlanSession {
    suspend fun activePlan(settings: PlanningSettings): StoredPlan?

    suspend fun open(settings: PlanningSettings): PlanGenerationOutcome

    suspend fun regenerate(settings: PlanningSettings): PlanGenerationOutcome
}

class DefaultPlanSession(
    private val generator: PlanGenerator,
    private val planRepository: PlanRepository,
    private val loader: LoadPlanUseCase,
) : PlanSession {

    override suspend fun activePlan(settings: PlanningSettings): StoredPlan? =
        planRepository.activePlan(settings.planStartDate, settings.planEndDate)

    override suspend fun open(settings: PlanningSettings): PlanGenerationOutcome {
        val active = activePlan(settings)
        return if (active != null) {
            loader.load(active.planId, settings.template)
        } else {
            generator.execute(settings)
        }
    }

    override suspend fun regenerate(settings: PlanningSettings): PlanGenerationOutcome {
        val active = planRepository.activePlan(settings.planStartDate, settings.planEndDate)
        return generator.regenerate(settings, active?.planId)
    }
}
