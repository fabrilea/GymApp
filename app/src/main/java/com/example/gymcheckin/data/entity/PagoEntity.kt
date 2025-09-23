package com.example.gymcheckin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "pagos")
data class PagoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dni: String,
    val nombre: String = "",
    val apellido: String = "",   // 🔹 agregado
    val fechaPago: LocalDate,
    val fin: LocalDate,
    val mes: String,
    val monto: Double,
    val diasPorSemana: Int
)
