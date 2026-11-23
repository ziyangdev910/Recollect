package com.recollect.data.room.repository

import com.recollect.data.room.dao.AppSettingsDao
import com.recollect.data.room.entity.AppSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

/**
 * 每日复习时长设置（schema v12）的读写口径：
 * 无记录回退默认 60，写入做 1～240 收敛，读取也收敛外部脏数据。
 */
class SettingsRepositoryTest {

    @Test
    fun `无记录时回退默认 60 分钟`() = runBlocking {
        val repository = RoomSettingsRepository(FakeSettingsDao())
        assertEquals(60, repository.dailyReviewMinutes())
    }

    @Test
    fun `保存后读取到新值`() = runBlocking {
        val dao = FakeSettingsDao()
        val repository = RoomSettingsRepository(dao)

        repository.updateDailyReviewMinutes(90)

        assertEquals(90, repository.dailyReviewMinutes())
        assertEquals(AppSettings.SINGLE_ROW_ID, dao.row?.id)
    }

    @Test
    fun `写入值收敛到 1 到 240`() = runBlocking {
        val dao = FakeSettingsDao()
        val repository = RoomSettingsRepository(dao)

        repository.updateDailyReviewMinutes(0)
        assertEquals(1, repository.dailyReviewMinutes())

        repository.updateDailyReviewMinutes(999)
        assertEquals(240, repository.dailyReviewMinutes())
    }

    @Test
    fun `读取时收敛外部写入的非法值`() = runBlocking {
        val dao = FakeSettingsDao(row = AppSettings(dailyReviewMinutes = 0))
        assertEquals(1, RoomSettingsRepository(dao).dailyReviewMinutes())
    }
}

private class FakeSettingsDao(var row: AppSettings? = null) : AppSettingsDao {
    override suspend fun get(): AppSettings? = row

    override suspend fun upsert(row: AppSettings) {
        this.row = row
    }
}
