package com.recollect.data.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 应用设置（单行表，schema v12）。
 *
 * 目前只持久化「每日复习时长」一个字段：它随备份导出/导入，保证换机后仍保留用户设置。
 * 单行表固定 [SINGLE_ROW_ID]，写入使用 REPLACE 覆盖。
 *
 * 该字段用于生成计划时每天可用的复习分钟数；修改只对“下一份生成的计划”生效，
 * 不回溯已经生成或打印的计划。
 */
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val dailyReviewMinutes: Int = DEFAULT_DAILY_REVIEW_MINUTES,
) {
    companion object {
        const val SINGLE_ROW_ID = 1

        /** 默认每日复习 60 分钟，与 planning.md 的默认值一致。 */
        const val DEFAULT_DAILY_REVIEW_MINUTES = 60

        /** 允许范围 1～240 分钟；0 会让当天排不进任何题目，界面层不接受。 */
        const val MIN_DAILY_REVIEW_MINUTES = 1
        const val MAX_DAILY_REVIEW_MINUTES = 240
    }
}
