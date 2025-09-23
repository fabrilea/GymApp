package com.example.gymcheckin.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

object PagoUtils {

    /** Para Entities (LocalDate) */
    fun semanasRestantes(inicio: LocalDate): Int {
        val hoy = LocalDate.now()
        val fin = inicio.plusDays(28) // asumimos pago válido por 4 semanas
        val dias = ChronoUnit.DAYS.between(hoy, fin)
        return (dias / 7).toInt().coerceAtLeast(0)
    }

    /** Para Excel DTOs (Long fechaPago en millis) */
    fun semanasRestantes(fechaPagoMillis: Long): Int {
        val inicio = Instant.ofEpochMilli(fechaPagoMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return semanasRestantes(inicio)
    }
}
