package com.example.gymcheckin.util

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object Converters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter
    @JvmStatic
    fun fromString(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it, formatter) }
    }

    @TypeConverter
    @JvmStatic
    fun toString(date: LocalDate?): String? {
        return date?.format(formatter)
    }
}
