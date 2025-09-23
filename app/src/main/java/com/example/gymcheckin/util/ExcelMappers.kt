package com.example.gymcheckin.util

import com.example.gymcheckin.data.dto.AsistenciaExcelDto
import com.example.gymcheckin.data.dto.ClienteExcelDto
import com.example.gymcheckin.data.dto.PagoExcelDto
import com.example.gymcheckin.data.entity.AsistenciaEntity
import com.example.gymcheckin.data.entity.ClienteEntity
import com.example.gymcheckin.data.entity.PagoEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object ExcelMappers {

    // ---------------- CLIENTE ----------------
    fun ClienteExcelDto.toEntity(): ClienteEntity =
        ClienteEntity(
            dni = dni,
            nombre = nombre,
            apellido = apellido,   // 🔹 agregado
            fechaInicio = fechaInicio
        )

    fun ClienteEntity.toExcelDto(): ClienteExcelDto =
        ClienteExcelDto(
            dni = dni,
            nombre = nombre,
            apellido = apellido,   // 🔹 agregado
            fechaInicio = fechaInicio
        )

    // ---------------- PAGO ----------------
    fun PagoExcelDto.toEntity(nombre: String, apellido: String): PagoEntity {
        val inicio = fechaPago

        return PagoEntity(
            dni = dni,
            nombre = nombre,
            apellido = apellido,   // 🔹 agregado
            fechaPago = inicio,
            fin = inicio.plusDays(28),
            mes = inicio.month.name,
            monto = monto,
            diasPorSemana = diasPorSemana
        )
    }

    fun PagoEntity.toExcelDto(): PagoExcelDto =
        PagoExcelDto(
            dni = dni,
            fechaPago = fechaPago,
            monto = monto,
            diasPorSemana = diasPorSemana
        )

    // ---------------- ASISTENCIA ----------------
    fun AsistenciaExcelDto.toEntity(nombre: String = "", apellido: String = ""): AsistenciaEntity =
        AsistenciaEntity(
            dniCliente = dni,
            nombre = nombre,       // 🔹 agregado
            apellido = apellido,   // 🔹 agregado
            fecha = fecha
        )

    fun AsistenciaEntity.toExcelDto(): AsistenciaExcelDto =
        AsistenciaExcelDto(
            dni = dniCliente,
            fecha = fecha.atStartOfDay(ZoneId.systemDefault()).toLocalDate()
        )
}
