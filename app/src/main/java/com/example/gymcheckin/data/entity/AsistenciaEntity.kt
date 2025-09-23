package com.example.gymcheckin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "asistencias")
data class AsistenciaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dniCliente: String,
    val nombre: String = "",     // 🔹 agregado
    val apellido: String = "",   // 🔹 agregado
    val fecha: LocalDate
)
