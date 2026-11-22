package com.recollect.feature.planning

import com.recollect.data.room.repository.PlanRepository

/**
 * 计划会话：区分“打开本周已有计划”与“显式重新生成”。
 *
 * - [open]：本周已有 GENERATED/PRINTED 计划时加载原计划，绝不生成新计划号；
 * - [regenerate]：用户确认后生成新计划，并在保存事务内把旧计划标记为 SUPERSEDED。
 */
interface PlanSession {
    suspend fun open(settings: PlanningSettings): PlanGenerationOutcome

    suspend fun regenerate(settings: PlanningSettings): PlanGenerationOutcome
}

class DefaultPlanSession(
    private val generator: PlanGenerator,
    private val planRepository: PlanRepository,
    private val loader: LoadPlanUseCase,
) : PlanSession {

    override suspend fun open(settings: PlanningSettings): PlanGenerationOutcome {
        val active = planRepository.activePlan(settings.planStartDate, settings.planEndDate)
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
