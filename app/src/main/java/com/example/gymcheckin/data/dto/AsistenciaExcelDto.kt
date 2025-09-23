package com.example.gymcheckin.data.dto

import java.time.LocalDate

data class AsistenciaExcelDto(
    val dni: String,
    val fecha: LocalDate // en millis
)