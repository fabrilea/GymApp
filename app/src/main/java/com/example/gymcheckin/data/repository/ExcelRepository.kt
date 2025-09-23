package com.example.gymcheckin.data.repository

import android.content.Context
import android.net.Uri
import com.example.gymcheckin.data.ExcelLinkStore
import com.example.gymcheckin.data.ExcelSync
import com.example.gymcheckin.data.entity.ClienteEntity
import com.example.gymcheckin.data.entity.PagoEntity
import com.example.gymcheckin.data.entity.AsistenciaEntity
import com.example.gymcheckin.util.ExcelMappers.toEntity
import com.example.gymcheckin.util.ExcelMappers.toExcelDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class ExcelRepository(
    val excelSync: ExcelSync
) : GymRepository {

    private fun safeUriOrNull(ctx: Context): Uri? = ExcelLinkStore.getUri(ctx)

    override suspend fun getCounts(context: Context): Triple<Int, Int, Int> = withContext(Dispatchers.IO) {
        val uri = safeUriOrNull(context) ?: return@withContext Triple(0, 0, 0)
        val clientes = excelSync.readClientes(context, uri).size
        val pagos = excelSync.readPagos(context, uri).size
        val asistencias = excelSync.readAsistencias(context, uri).size
        Triple(clientes, pagos, asistencias)
    }

    // ---------------- CLIENTES ----------------
    override suspend fun getClientePorDni(context: Context, dni: String): ClienteEntity? =
        withContext(Dispatchers.IO) {
            val uri = safeUriOrNull(context) ?: return@withContext null
            excelSync.readClientes(context, uri)
                .firstOrNull { it.dni == dni }
                ?.toEntity()
        }

    override suspend fun getClientes(context: Context): List<ClienteEntity> =
        withContext(Dispatchers.IO) {
            val uri = safeUriOrNull(context) ?: return@withContext emptyList()
            excelSync.readClientes(context, uri).map { it.toEntity() }
        }

    override suspend fun upsertCliente(context: Context, cliente: ClienteEntity) =
        withContext(Dispatchers.IO) {
            val uri = safeUriOrNull(context) ?: return@withContext
            excelSync.upsertClienteExcel(context, uri, cliente.toExcelDto())
        }

    // ---------------- PAGOS ----------------
// ---------------- PAGOS ----------------
    override suspend fun upsertPago(context: Context, pago: PagoEntity) =
        withContext(Dispatchers.IO) {
            val uri = safeUriOrNull(context) ?: return@withContext
            excelSync.upsertPagoSeguro(
                ctx = context,
                uri = uri,
                dto = pago.toExcelDto(),
                nombre = pago.nombre,
                apellido = pago.apellido
            )
        }


    override suspend fun getPagos(context: Context): List<PagoEntity> =
        withContext(Dispatchers.IO) {
            val uri = safeUriOrNull(context) ?: return@withContext emptyList()
            excelSync.resetPagosSiNuevoMes(context, uri)
            val clientes = excelSync.readClientes(context, uri) // para nombre y apellido
            excelSync.readPagos(context, uri).map { dto ->
                val cliente = clientes.firstOrNull { it.dni == dto.dni }
                dto.toEntity(cliente?.nombre ?: "Desconocido", cliente?.apellido ?: "")
            }
        }

    override suspend fun getUltimoPago(context: Context, dni: String): PagoEntity? =
        withContext(Dispatchers.IO) {
            val uri = safeUriOrNull(context) ?: return@withContext null
            val clientes = excelSync.readClientes(context, uri)
            val dto = excelSync.readPagos(context, uri)
                .filter { it.dni == dni }
                .maxByOrNull { it.fechaPago } ?: return@withContext null
            val cliente = clientes.firstOrNull { it.dni == dni }
            dto.toEntity(cliente?.nombre ?: "Desconocido", cliente?.apellido ?: "")
        }

    // ---------------- ASISTENCIAS ----------------
    override suspend fun countAsistenciasSemana(context: Context, dni: String, start: Long, end: Long): Int =
        withContext(Dispatchers.IO) {
            val uri = safeUriOrNull(context) ?: return@withContext 0
            excelSync.resetAsistenciasSiNuevoMes(context, uri)
            excelSync.readAsistencias(context, uri)
                .map { it.toEntity() }
                .count { it.dniCliente == dni && !it.fecha.isBefore(java.time.Instant.ofEpochMilli(start).atZone(java.time.ZoneId.systemDefault()).toLocalDate()) && !it.fecha.isAfter(java.time.Instant.ofEpochMilli(end).atZone(java.time.ZoneId.systemDefault()).toLocalDate()) }
        }

    override suspend fun appendAsistenciaOk(
        context: Context,
        asistencia: AsistenciaEntity,
        limiteSemanal: Int?,
        countSemanaPrevio: Int
    ) = withContext(Dispatchers.IO) {
        val uri = safeUriOrNull(context) ?: return@withContext
        val cliente = getClientePorDni(context, asistencia.dniCliente)
        val estado = if (limiteSemanal == null || countSemanaPrevio + 1 <= limiteSemanal) "OK" else "EXCEDIDO"
        excelSync.appendAsistenciaExcel(
            ctx = context,
            uri = uri,
            dto = asistencia.toExcelDto(),
            estado = estado,
            nombre = cliente?.nombre ?: "",
            apellido = cliente?.apellido ?: ""
        )
    }

    override suspend fun appendAsistenciaEstadoFijo(
        context: Context,
        asistencia: AsistenciaEntity,
        estado: String
    ) = withContext(Dispatchers.IO) {
        val uri = safeUriOrNull(context) ?: return@withContext
        val cliente = getClientePorDni(context, asistencia.dniCliente)
        excelSync.appendAsistenciaExcel(
            ctx = context,
            uri = uri,
            dto = asistencia.toExcelDto(),
            estado = estado,
            nombre = cliente?.nombre ?: "",
            apellido = cliente?.apellido ?: ""
        )
    }

    override suspend fun resetPagosSiNuevoMes(context: Context, uri: Uri) =
        withContext(Dispatchers.IO) {
            excelSync.resetPagosSiNuevoMes(context, uri)
        }


}
