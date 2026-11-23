package com.recollect.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.recollect.data.room.entity.AppSettings

/**
 * 应用设置单行表的读写入口（schema v12）。
 *
 * 读取在缺少行时返回 `null`，由仓库层回退到默认值；写入始终覆盖固定单行。
 */
@Dao
interface AppSettingsDao {

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun get(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: AppSettings)
}
