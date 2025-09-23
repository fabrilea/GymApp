package com.example.gymcheckin.data.entity

import androidx.room.Entity
import java.time.LocalDate

@Entity(tableName = "resumen")
data class Resumen(
    val dni: String,
    val nombre: String,
    val mes: String,
    val inicio: LocalDate,
    val fin: LocalDate,
    val monto: Double,
    val totalAsistencias: Int
)