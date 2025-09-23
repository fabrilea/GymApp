package com.example.gymcheckin.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "clientes")
data class ClienteEntity(
    @PrimaryKey val dni: String,
    val nombre: String = "",
    val apellido: String = "",   // 🔹 agregado
    val fechaInicio: LocalDate
)
