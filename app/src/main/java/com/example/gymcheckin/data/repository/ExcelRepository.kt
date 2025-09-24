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
    private val excelSync: ExcelSync
) : GymRepository {

    private fun safeUriOrNull(ctx: Context): Uri? = ExcelLinkStore.getUri(ctx)

    // ---------- HELPERS ----------
    private suspend fun withLocalFile(
        ctx: Context,
        block: suspend (localFile: java.io.File, driveUri: Uri) -> Unit
    ) {
        val uri = safeUriOrNull(ctx) ?: return
        val localFile = excelSync.copyExcelToLocal(ctx, uri)
        block(localFile, uri)
        excelSync.exportToDrive(ctx, localFile, uri) // siempre sincroniza cambios
    }

    private suspend fun <T> withLocalFileResult(
        ctx: Context,
        block: suspend (localFile: java.io.File, driveUri: Uri) -> T
    ): T? {
        val uri = safeUriOrNull(ctx) ?: return null
        val localFile = excelSync.copyExcelToLocal(ctx, uri)
        val result = block(localFile, uri)
        excelSync.exportToDrive(ctx, localFile, uri)
        return result
    }

    // ---------- COUNTS ----------
    override suspend fun getCounts(context: Context): Triple<Int, Int, Int> =
        withContext(Dispatchers.IO) {
            val uri = safeUriOrNull(context) ?: return@withContext Triple(0, 0, 0)
            val localFile = excelSync.copyExcelToLocal(context, uri)
            val clientes = excelSync.readClientes(localFile).size
            val pagos = excelSync.readPagos(localFile).size
            val asistencias = excelSync.readAsistencias(localFile).size
            Triple(clientes, pagos, asistencias)
        }

    // ---------- CLIENTES ----------
    override suspend fun getClientePorDni(context: Context, dni: String): ClienteEntity? =
        withLocalFileResult(context) { localFile, _ ->
            excelSync.readClientes(localFile).firstOrNull { it.dni == dni }?.toEntity()
        }

    override suspend fun getClientes(context: Context): List<ClienteEntity> =
        withContext(Dispatchers.IO) {
            val uri = safeUriOrNull(context) ?: return@withContext emptyList()
            val localFile = excelSync.copyExcelToLocal(context, uri)
            excelSync.readClientes(localFile).map { it.toEntity() }
        }

    override suspend fun upsertCliente(context: Context, cliente: ClienteEntity) =
        withLocalFile(context) { localFile, _ ->
            excelSync.upsertCliente(localFile, cliente.toExcelDto())
        }

    // ---------- PAGOS ----------
    override suspend fun upsertPago(context: Context, pago: PagoEntity) =
        withLocalFile(context) { localFile, _ ->
            excelSync.upsertPagoSeguro(
                localFile = localFile,
                dto = pago.toExcelDto(),
                nombre = pago.nombre,
                apellido = pago.apellido
            )
        }

    override suspend fun getPagos(context: Context): List<PagoEntity> =
        withContext(Dispatchers.IO) {
            val uri = safeUriOrNull(context) ?: return@withContext emptyList()
            val localFile = excelSync.copyExcelToLocal(context, uri)

            excelSync.resetPagosSiNuevoMes(localFile)

            val clientes = excelSync.readClientes(localFile)
            excelSync.readPagos(localFile).map { dto ->
                val cliente = clientes.firstOrNull { it.dni == dto.dni }
                dto.toEntity(cliente?.nombre ?: "Desconocido", cliente?.apellido ?: "")
            }
        }

    override suspend fun getUltimoPago(context: Context, dni: String): PagoEntity? =
        withContext(Dispatchers.IO) {
            val uri = safeUriOrNull(context) ?: return@withContext null
            val localFile = excelSync.copyExcelToLocal(context, uri)

            val clientes = excelSync.readClientes(localFile)
            val dto = excelSync.readPagos(localFile)
                .filter { it.dni == dni }
                .maxByOrNull { it.fechaPago } ?: return@withContext null

            val cliente = clientes.firstOrNull { it.dni == dni }
            dto.toEntity(cliente?.nombre ?: "Desconocido", cliente?.apellido ?: "")
        }

    // ---------- ASISTENCIAS ----------
    override suspend fun countAsistenciasSemana(
        context: Context,
        dni: String,
        start: Long,
        end: Long
    ): Int = withContext(Dispatchers.IO) {
        val uri = safeUriOrNull(context) ?: return@withContext 0
        val localFile = excelSync.copyExcelToLocal(context, uri)

        excelSync.resetAsistenciasSiNuevoMes(localFile)
        excelSync.readAsistencias(localFile)
            .map { it.toEntity() }
            .count {
                it.dniCliente == dni &&
                        !it.fecha.isBefore(java.time.Instant.ofEpochMilli(start)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()) &&
                        !it.fecha.isAfter(java.time.Instant.ofEpochMilli(end)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate())
            }
    }

    override suspend fun appendAsistenciaOk(
        context: Context,
        asistencia: AsistenciaEntity,
        limiteSemanal: Int?,
        countSemanaPrevio: Int
    ) = withLocalFile(context) { localFile, _ ->
        val cliente = getClientePorDni(context, asistencia.dniCliente)
        val estado =
            if (limiteSemanal == null || countSemanaPrevio + 1 <= limiteSemanal) "OK"
            else "EXCEDIDO"

        excelSync.appendAsistenciaExcel(
            localFile = localFile,
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
    ) = withLocalFile(context) { localFile, _ ->
        val cliente = getClientePorDni(context, asistencia.dniCliente)
        excelSync.appendAsistenciaExcel(
            localFile = localFile,
            dto = asistencia.toExcelDto(),
            estado = estado,
            nombre = cliente?.nombre ?: "",
            apellido = cliente?.apellido ?: ""
        )
    }

    override suspend fun resetPagosSiNuevoMes(context: Context, uri: Uri) =
        withContext(Dispatchers.IO) {
            val localFile = excelSync.copyExcelToLocal(context, uri)
            excelSync.resetPagosSiNuevoMes(localFile)
            excelSync.exportToDrive(context, localFile, uri)
        }.let { }

}
