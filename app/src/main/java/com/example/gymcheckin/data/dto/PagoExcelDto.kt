package com.example.gymcheckin.data.dto

import java.time.LocalDate

data class PagoExcelDto(
    val dni: String,
    val fechaPago: LocalDate,
    val monto: Double,
    val diasPorSemana: Int
)