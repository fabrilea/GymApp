package com.example.gymcheckin.data.dto

import java.time.LocalDate

data class ClienteExcelDto(
    val dni: String,
    val nombre: String,
    val apellido: String,
    val fechaInicio: LocalDate
)