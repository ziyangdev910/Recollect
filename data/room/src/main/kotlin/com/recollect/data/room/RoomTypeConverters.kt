package com.recollect.data.room

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/**
 * `java.time` 类型的 Room 转换器。
 *
 * Room 2.7.2 未提供内置的 `java.time` 转换器；这里固定落库形态，schema 测试据此锁定：
 * - `LocalDate` ↔ ISO-8601 文本（TEXT）
 * - `Instant` ↔ epoch 毫秒（INTEGER）
 *
 * 转换只发生在 data:room，不改变任何字段语义。
 */
class RoomTypeConverters {

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}
