package io.github.wtfjb.aximo.data.db

import androidx.room.TypeConverter
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

/** How Room stores types it doesn't know. Enums are stored by name automatically. */
class Converters {
    @TypeConverter
    fun instantToMillis(value: Instant?): Long? = value?.toEpochMilliseconds()

    @TypeConverter
    fun millisToInstant(value: Long?): Instant? = value?.let { Instant.fromEpochMilliseconds(it) }

    @TypeConverter
    fun localDateToEpochDay(value: LocalDate?): Long? = value?.toEpochDays()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let { LocalDate.fromEpochDays(it) }
}
