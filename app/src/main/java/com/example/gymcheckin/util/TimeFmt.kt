// TimeFmt.kt
package com.example.gymcheckin.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

object TimeFmt {
    fun year(): Int = LocalDate.now().year
    fun month(): Int = LocalDate.now().monthValue

    fun today(): String = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
    fun now(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))

    data class WeekRange(val start: String, val end: String)

    fun currentWeekRange(): WeekRange {
        val now = LocalDate.now()
        val weekFields = WeekFields.ISO
        val first = now.with(weekFields.dayOfWeek(), 1)
        val last = now.with(weekFields.dayOfWeek(), 7)
        val fmt = DateTimeFormatter.ISO_DATE
        return WeekRange(first.format(fmt), last.format(fmt))
    }

    /** Devuelve clave tipo "2025-W36" para detectar cambios de semana */
    fun weekIsoKey(): String {
        val now = LocalDate.now()
        val weekFields = WeekFields.ISO
        val week = now.get(weekFields.weekOfWeekBasedYear())
        val year = now.get(weekFields.weekBasedYear())
        return "$year-W$week"
    }
}
