package com.recollect.data.room.repository

import com.recollect.data.room.dao.AppSettingsDao
import com.recollect.data.room.entity.AppSettings

/**
 * 应用设置端口（settings.md，schema v12）。
 *
 * 当前只有「每日复习时长」：持久化在 Room 单行表 `app_settings`，随备份导入/导出。
 * 取值做范围收敛，避免外部写入非法数据。
 */
interface SettingsRepository {
    /** 每日复习分钟数；无记录时回退默认 60。 */
    suspend fun dailyReviewMinutes(): Int

    /** 保存每日复习分钟数（自动收敛到 1～240）。 */
    suspend fun updateDailyReviewMinutes(minutes: Int)
}

class RoomSettingsRepository(
    private val dao: AppSettingsDao,
) : SettingsRepository {

    override suspend fun dailyReviewMinutes(): Int =
        dao.get()?.dailyReviewMinutes
            ?.coerceIn(
                AppSettings.MIN_DAILY_REVIEW_MINUTES,
                AppSettings.MAX_DAILY_REVIEW_MINUTES,
            )
            ?: AppSettings.DEFAULT_DAILY_REVIEW_MINUTES

    override suspend fun updateDailyReviewMinutes(minutes: Int) {
        dao.upsert(
            AppSettings(
                dailyReviewMinutes = minutes.coerceIn(
                    AppSettings.MIN_DAILY_REVIEW_MINUTES,
                    AppSettings.MAX_DAILY_REVIEW_MINUTES,
                ),
            ),
        )
    }
}
