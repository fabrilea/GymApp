package com.example.gymcheckin.data.repository

import android.content.Context
import android.net.Uri
import com.example.gymcheckin.data.entity.AsistenciaEntity
import com.example.gymcheckin.data.entity.ClienteEntity
import com.example.gymcheckin.data.entity.PagoEntity

interface GymRepository {

    suspend fun getCounts(context: Context): Triple<Int, Int, Int>

    // CLIENTES
    suspend fun getClientePorDni(context: Context, dni: String): ClienteEntity?
    suspend fun getClientes(context: Context): List<ClienteEntity>
    suspend fun upsertCliente(context: Context, cliente: ClienteEntity)

    // PAGOS
    suspend fun upsertPago(context: Context, pago: PagoEntity)
    suspend fun getPagos(context: Context): List<PagoEntity>
    suspend fun getUltimoPago(context: Context, dni: String): PagoEntity?   // ✅ agregado

    // ASISTENCIAS
    suspend fun countAsistenciasSemana(context: Context, dni: String, start: Long, end: Long): Int
    suspend fun appendAsistenciaOk(context: Context, asistencia: AsistenciaEntity, limiteSemanal: Int?, countSemanaPrevio: Int)
    suspend fun appendAsistenciaEstadoFijo(context: Context, asistencia: AsistenciaEntity, estado: String)
}
