package com.example.gymcheckin.data

import android.content.Context
import android.net.Uri
import com.example.gymcheckin.data.dto.ClienteExcelDto
import com.example.gymcheckin.data.dto.PagoExcelDto
import com.example.gymcheckin.data.dto.AsistenciaExcelDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ExcelSync {

    companion object {
        private const val HOJA_CLIENTES = "clientes"
        private const val HOJA_PAGOS = "pagos"
        private const val HOJA_ASISTENCIAS = "asistencias"

        private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }

    // ----------- Helpers de import/export con Drive -----------

    suspend fun exportToDrive(ctx: Context, localFile: File, driveUri: Uri) =
        withContext(Dispatchers.IO) {
            ctx.contentResolver.openOutputStream(driveUri, "rwt")?.use { out ->
                FileInputStream(localFile).use { input -> input.copyTo(out) }
            }
        }

    // ----------- Apertura y guardado -----------

    private fun openWorkbook(localFile: File): Workbook =
        FileInputStream(localFile).use { fis -> WorkbookFactory.create(fis) }

    private fun saveWorkbook(localFile: File, wb: Workbook) {
        FileOutputStream(localFile).use { fos -> wb.write(fos) }
        wb.close()
    }

    // ----------- Asegurar hojas -----------

    private fun ensureAllSheets(wb: Workbook) {
        if (wb.getSheet(HOJA_CLIENTES) == null) {
            wb.createSheet(HOJA_CLIENTES).apply {
                createRow(0).apply {
                    createCell(0).setCellValue("dni")
                    createCell(1).setCellValue("nombre")
                    createCell(2).setCellValue("apellido")
                    createCell(3).setCellValue("fechaInicio")
                }
            }
        }
        if (wb.getSheet(HOJA_PAGOS) == null) {
            wb.createSheet(HOJA_PAGOS).apply {
                createRow(0).apply {
                    createCell(0).setCellValue("dni")
                    createCell(1).setCellValue("nombre")
                    createCell(2).setCellValue("apellido")
                    createCell(3).setCellValue("fechaPago")
                    createCell(4).setCellValue("monto")
                    createCell(5).setCellValue("diasPorSemana")
                    createCell(6).setCellValue("fechaInicio")
                    createCell(7).setCellValue("fechaFin")
                }
            }
        }
        if (wb.getSheet(HOJA_ASISTENCIAS) == null) {
            wb.createSheet(HOJA_ASISTENCIAS).apply {
                createRow(0).apply {
                    createCell(0).setCellValue("dni")
                    createCell(1).setCellValue("nombre")
                    createCell(2).setCellValue("apellido")
                    createCell(3).setCellValue("fechaAsistencia")
                    createCell(4).setCellValue("estado")
                }
            }
        }
    }

    // ----------- Lectores -----------

    suspend fun readClientes(localFile: File): List<ClienteExcelDto> =
        withContext(Dispatchers.IO) {
            val wb = openWorkbook(localFile)
            val out = mutableListOf<ClienteExcelDto>()
            wb.getSheet(HOJA_CLIENTES)?.let { sheet ->
                val it = sheet.rowIterator()
                if (it.hasNext()) it.next()
                while (it.hasNext()) {
                    val r = it.next()
                    val dni = r.getCell(0)?.toString()?.trim().orEmpty()
                    if (dni.isBlank()) continue
                    val nombre = r.getCell(1)?.toString()?.trim().orEmpty()
                    val apellido = r.getCell(2)?.toString()?.trim().orEmpty()
                    val fechaStr = r.getCell(3)?.toString()?.trim()
                    val fechaInicio = if (!fechaStr.isNullOrBlank())
                        LocalDate.parse(fechaStr, DATE_FMT)
                    else LocalDate.now()
                    out.add(ClienteExcelDto(dni, nombre, apellido, fechaInicio))
                }
            }
            wb.close()
            out
        }

    suspend fun readPagos(localFile: File): List<PagoExcelDto> =
        withContext(Dispatchers.IO) {
            val wb = openWorkbook(localFile)
            val out = mutableListOf<PagoExcelDto>()
            wb.getSheet(HOJA_PAGOS)?.let { sheet ->
                val it = sheet.rowIterator()
                if (it.hasNext()) it.next()
                while (it.hasNext()) {
                    val r = it.next()
                    val dni = r.getCell(0)?.toString()?.trim().orEmpty()
                    if (dni.isBlank()) continue
                    val fechaPagoStr = r.getCell(3)?.toString()?.trim()
                    val fechaPago = if (!fechaPagoStr.isNullOrBlank())
                        LocalDate.parse(fechaPagoStr, DATE_FMT)
                    else LocalDate.now()
                    val monto = r.getCell(4)?.numericCellValue ?: 0.0
                    val dps = r.getCell(5)?.numericCellValue?.toInt() ?: 0
                    out.add(PagoExcelDto(dni, fechaPago, monto, dps))
                }
            }
            wb.close()
            out
        }

    suspend fun readAsistencias(localFile: File): List<AsistenciaExcelDto> =
        withContext(Dispatchers.IO) {
            val wb = openWorkbook(localFile)
            val out = mutableListOf<AsistenciaExcelDto>()
            wb.getSheet(HOJA_ASISTENCIAS)?.let { sheet ->
                val it = sheet.rowIterator()
                if (it.hasNext()) it.next()
                while (it.hasNext()) {
                    val r = it.next()
                    val dni = r.getCell(0)?.toString()?.trim().orEmpty()
                    if (dni.isBlank()) continue
                    val fechaStr = r.getCell(3)?.toString()?.trim()
                    val fecha = if (!fechaStr.isNullOrBlank())
                        LocalDate.parse(fechaStr, DATE_FMT)
                    else LocalDate.now()
                    out.add(AsistenciaExcelDto(dni, fecha))
                }
            }
            wb.close()
            out
        }

    // -------------------- CLIENTES --------------------

    fun upsertCliente(localFile: File, dto: ClienteExcelDto) {
        val wb = openWorkbook(localFile)
        ensureAllSheets(wb)
        val sheet = wb.getSheet(HOJA_CLIENTES)
        var found = false
        val it = sheet.rowIterator()
        if (it.hasNext()) it.next()
        while (it.hasNext()) {
            val r = it.next()
            if (r.getCell(0)?.toString()?.trim().orEmpty() == dto.dni) {
                (r.getCell(1) ?: r.createCell(1)).setCellValue(dto.nombre)
                (r.getCell(2) ?: r.createCell(2)).setCellValue(dto.apellido)
                (r.getCell(3) ?: r.createCell(3)).setCellValue(dto.fechaInicio.toString())
                found = true
                break
            }
        }
        if (!found) {
            val lastUsed = getLastUsedRowIndex(sheet)
            val row = sheet.createRow(lastUsed + 1)
            row.createCell(0).setCellValue(dto.dni)
            row.createCell(1).setCellValue(dto.nombre)
            row.createCell(2).setCellValue(dto.apellido)
            row.createCell(3).setCellValue(dto.fechaInicio.toString())
        }
        saveWorkbook(localFile, wb)
    }

    // -------------------- PAGOS --------------------

    fun upsertPagoConIntegracion(
        localFile: File,
        dto: PagoExcelDto,
        nombre: String,
        apellido: String
    ) {
        val fechaInicio = dto.fechaPago
        val fechaFin = fechaInicio.plusDays(28)
        val wb = openWorkbook(localFile)
        val pagosSheet = wb.getSheet(HOJA_PAGOS)

        // Insertar fila en pagos
        val lastUsedPago = getLastUsedRowIndex(pagosSheet)
        val rowPago = pagosSheet.createRow(lastUsedPago + 1)
        rowPago.createCell(0).setCellValue(dto.dni)
        rowPago.createCell(1).setCellValue(nombre)
        rowPago.createCell(2).setCellValue(apellido)
        rowPago.createCell(3).setCellValue(dto.fechaPago.toString())
        rowPago.createCell(4).setCellValue(dto.monto)
        rowPago.createCell(5).setCellValue(dto.diasPorSemana.toDouble())
        rowPago.createCell(6).setCellValue(fechaInicio.toString())
        rowPago.createCell(7).setCellValue(fechaFin.toString())

        // Actualizar cliente
        val clientesSheet = wb.getSheet(HOJA_CLIENTES)
        var clienteActualizado = false
        for (i in 1..clientesSheet.lastRowNum) {
            val r = clientesSheet.getRow(i) ?: continue
            if (r.getCell(0)?.toString()?.trim() == dto.dni) {
                (r.getCell(1) ?: r.createCell(1)).setCellValue(nombre)
                (r.getCell(2) ?: r.createCell(2)).setCellValue(apellido)
                clienteActualizado = true
                break
            }
        }
        if (!clienteActualizado) {
            val lastUsedCli = getLastUsedRowIndex(clientesSheet)
            val rowCli = clientesSheet.createRow(lastUsedCli + 1)
            rowCli.createCell(0).setCellValue(dto.dni)
            rowCli.createCell(1).setCellValue(nombre)
            rowCli.createCell(2).setCellValue(apellido)
        }

        saveWorkbook(localFile, wb)
    }

    fun upsertPagoSeguro(localFile: File, dto: PagoExcelDto, nombre: String, apellido: String) {
        val wb = openWorkbook(localFile)
        val pagosSheet = wb.getSheet(HOJA_PAGOS)
        var updated = false
        for (i in 1..pagosSheet.lastRowNum) {
            val r = pagosSheet.getRow(i) ?: continue
            val dniCell = r.getCell(0)?.toString()?.trim() ?: continue
            if (dniCell == dto.dni) {
                (r.getCell(1) ?: r.createCell(1)).setCellValue(nombre)
                (r.getCell(2) ?: r.createCell(2)).setCellValue(apellido)
                (r.getCell(3) ?: r.createCell(3)).setCellValue(dto.fechaPago.toString())
                (r.getCell(4) ?: r.createCell(4)).setCellValue(dto.monto)
                (r.getCell(5) ?: r.createCell(5)).setCellValue(dto.diasPorSemana.toDouble())
                (r.getCell(6) ?: r.createCell(6)).setCellValue(dto.fechaPago.toString())
                (r.getCell(7) ?: r.createCell(7)).setCellValue(
                    dto.fechaPago.plusDays(28).toString()
                )
                updated = true
                break
            }
        }
        if (!updated) {
            val lastUsedPago = getLastUsedRowIndex(pagosSheet)
            val row = pagosSheet.createRow(lastUsedPago + 1)
            row.createCell(0).setCellValue(dto.dni)
            row.createCell(1).setCellValue(nombre)
            row.createCell(2).setCellValue(apellido)
            row.createCell(3).setCellValue(dto.fechaPago.toString())
            row.createCell(4).setCellValue(dto.monto)
            row.createCell(5).setCellValue(dto.diasPorSemana.toDouble())
            row.createCell(6).setCellValue(dto.fechaPago.toString())
            row.createCell(7).setCellValue(dto.fechaPago.plusDays(28).toString())
        }

        // actualizar cliente
        val clientesSheet = wb.getSheet(HOJA_CLIENTES)
        var foundCli = false
        for (i in 1..clientesSheet.lastRowNum) {
            val r = clientesSheet.getRow(i) ?: continue
            if (r.getCell(0)?.toString()?.trim() == dto.dni) {
                (r.getCell(1) ?: r.createCell(1)).setCellValue(nombre)
                (r.getCell(2) ?: r.createCell(2)).setCellValue(apellido)
                (r.getCell(3) ?: r.createCell(3)).setCellValue(dto.fechaPago.toString())
                foundCli = true
                break
            }
        }
        if (!foundCli) {
            val lastUsedCli = getLastUsedRowIndex(clientesSheet)
            val rowCli = clientesSheet.createRow(lastUsedCli + 1)
            rowCli.createCell(0).setCellValue(dto.dni)
            rowCli.createCell(1).setCellValue(nombre)
            rowCli.createCell(2).setCellValue(apellido)
            rowCli.createCell(3).setCellValue(dto.fechaPago.toString())
        }

        saveWorkbook(localFile, wb)
    }

    // -------------------- ASISTENCIAS --------------------

    fun appendAsistenciaExcel(
        localFile: File,
        dto: AsistenciaExcelDto,
        estado: String,
        nombre: String,
        apellido: String
    ) {
        val wb = openWorkbook(localFile)
        val sheet = wb.getSheet(HOJA_ASISTENCIAS)
        val lastUsed = getLastUsedRowIndex(sheet)
        val row = sheet.createRow(lastUsed + 1)
        row.createCell(0).setCellValue(dto.dni)
        row.createCell(1).setCellValue(nombre)
        row.createCell(2).setCellValue(apellido)
        row.createCell(3).setCellValue(dto.fecha.toString())
        row.createCell(4).setCellValue(estado)
        saveWorkbook(localFile, wb)
    }

    // -------------------- Helpers --------------------

    fun getLastUsedRowIndex(sheet: Sheet): Int {
        return (sheet.lastRowNum downTo 1).firstOrNull { i ->
            val r = sheet.getRow(i)
            val dni = r?.getCell(0)?.toString()?.trim()
            !dni.isNullOrBlank()
        } ?: 0
    }

    suspend fun copyExcelToLocal(ctx: Context, uri: Uri): File =
        withContext(Dispatchers.IO) {
            val localFile = File(ctx.filesDir, "gym-resumen.xlsx")
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(localFile).use { output ->
                    input.copyTo(output)
                }
            }
            localFile
        }

    fun resetAsistenciasSiNuevoMes(localFile: File) {
        val wb = openWorkbook(localFile)

        val sheet = wb.getSheet(HOJA_ASISTENCIAS)
        if (sheet != null && sheet.lastRowNum > 0) {
            val primeraAsistencia = sheet.getRow(1)
            val fechaStr = primeraAsistencia?.getCell(3)?.toString()?.trim()
            val fecha = if (!fechaStr.isNullOrBlank()) {
                try {
                    LocalDate.parse(fechaStr, DATE_FMT)
                } catch (_: Exception) {
                    null
                }
            } else null

            val mesGuardado = fecha?.monthValue
            val anioGuardado = fecha?.year
            val hoy = LocalDate.now()

            if (mesGuardado != hoy.monthValue || anioGuardado != hoy.year) {
                val idx = wb.getSheetIndex(HOJA_ASISTENCIAS)
                wb.removeSheetAt(idx)

                // 👇 aseguramos que nunca quede vacío
                ensureAtLeastOneSheet(wb)

                wb.createSheet(HOJA_ASISTENCIAS).apply {
                    val headerRow = createRow(getLastUsedRowIndex(this) + 1)
                    headerRow.createCell(0).setCellValue("dni")
                    headerRow.createCell(1).setCellValue("nombre")
                    headerRow.createCell(2).setCellValue("apellido")
                    headerRow.createCell(3).setCellValue("fechaAsistencia")
                    headerRow.createCell(4).setCellValue("estado")
                    headerRow.createCell(5).setCellValue("fechaInicioPago")
                    headerRow.createCell(6).setCellValue("fechaFinPago")
                }
            }
        }

        saveWorkbook(localFile, wb)
    }


    fun resetPagosSiNuevoMes(localFile: File) {
        val wb = openWorkbook(localFile)

        val sheet = wb.getSheet(HOJA_PAGOS)
        if (sheet != null && sheet.lastRowNum > 0) {
            val primerPago = sheet.getRow(1)
            val fechaStr = primerPago?.getCell(3)?.toString()?.trim()
            val fecha = if (!fechaStr.isNullOrBlank()) {
                try {
                    LocalDate.parse(fechaStr, DATE_FMT)
                } catch (_: Exception) {
                    null
                }
            } else null

            val mesGuardado = fecha?.monthValue
            val anioGuardado = fecha?.year
            val hoy = LocalDate.now()

            if (mesGuardado != hoy.monthValue || anioGuardado != hoy.year) {
                val idx = wb.getSheetIndex(HOJA_PAGOS)
                val backupName =
                    "pagos_${anioGuardado ?: "NA"}_${
                        mesGuardado?.toString()?.padStart(2, '0') ?: "NA"
                    }"

                val existingIdx = wb.getSheetIndex(backupName)
                if (existingIdx != -1) {
                    wb.removeSheetAt(existingIdx)
                }

                wb.setSheetName(idx, backupName)

                // 👇 aseguramos que nunca quede vacío
                ensureAtLeastOneSheet(wb)

                wb.createSheet(HOJA_PAGOS).apply {
                    val headerRow = createRow(getLastUsedRowIndex(this) + 1)
                    headerRow.createCell(0).setCellValue("dni")
                    headerRow.createCell(1).setCellValue("nombre")
                    headerRow.createCell(2).setCellValue("apellido")
                    headerRow.createCell(3).setCellValue("fechaPago")
                    headerRow.createCell(4).setCellValue("monto")
                    headerRow.createCell(5).setCellValue("diasPorSemana")
                    headerRow.createCell(6).setCellValue("fechaInicio")
                    headerRow.createCell(7).setCellValue("fechaFin")
                }
            }
        }

        saveWorkbook(localFile, wb)
    }

    private fun ensureAtLeastOneSheet(wb: Workbook) {
        if (wb.numberOfSheets == 0) {
            wb.createSheet("Sheet1").apply {
                createRow(0).apply {
                    createCell(0).setCellValue("dummy")
                }
            }
        }
    }
    suspend fun addPagoSolo(
        localFile: File,
        dni: String,
        monto: Double,
        fechaMs: Long,
        diasPorSemana: Int
    ) {
        // Abrir workbook desde el archivo local
        val wb = WorkbookFactory.create(FileInputStream(localFile))
        val sheetClientes = wb.getSheet("clientes") ?: error("No existe la hoja 'clientes'")
        val sheetPagos = wb.getSheet("pagos") ?: wb.createSheet("pagos")

        // Buscar cliente
        val cliente = (1..sheetClientes.lastRowNum).mapNotNull { i ->
            val r = sheetClientes.getRow(i) ?: return@mapNotNull null
            if (r.getCell(0)?.toString()?.trim() == dni) {
                Pair(r.getCell(1)?.toString().orEmpty(), r.getCell(2)?.toString().orEmpty())
            } else null
        }.firstOrNull() ?: run {
            wb.close()
            error("El DNI $dni no existe en la tabla de clientes")
        }

        val (nombre, apellido) = cliente

        // Fechas
        val fecha = java.time.Instant.ofEpochMilli(fechaMs)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        val fechaFin = fecha.plusDays(28)

        // Crear nueva fila en pagos
        val lastUsedPagos = getLastUsedRowIndex(sheetPagos)
        val row = sheetPagos.createRow(lastUsedPagos + 1)
        row.createCell(0).setCellValue(dni)
        row.createCell(1).setCellValue(nombre)
        row.createCell(2).setCellValue(apellido)
        row.createCell(3).setCellValue(fecha.toString())
        row.createCell(4).setCellValue(monto)
        row.createCell(5).setCellValue(diasPorSemana.toDouble())
        row.createCell(6).setCellValue(fecha.toString())
        row.createCell(7).setCellValue(fechaFin.toString())

        // Actualizar o crear cliente
        var clienteActualizado = false
        for (i in 1..sheetClientes.lastRowNum) {
            val r = sheetClientes.getRow(i) ?: continue
            if (r.getCell(0)?.toString()?.trim() == dni) {
                (r.getCell(1) ?: r.createCell(1)).setCellValue(nombre)
                (r.getCell(2) ?: r.createCell(2)).setCellValue(apellido)
                (r.getCell(3) ?: r.createCell(3)).setCellValue(fecha.toString())
                clienteActualizado = true
                break
            }
        }
        if (!clienteActualizado) {
            val lastUsedCli = getLastUsedRowIndex(sheetClientes)
            val rowCli = sheetClientes.createRow(lastUsedCli + 1)
            rowCli.createCell(0).setCellValue(dni)
            rowCli.createCell(1).setCellValue(nombre)
            rowCli.createCell(2).setCellValue(apellido)
            rowCli.createCell(3).setCellValue(fecha.toString())
        }

        // Guardar cambios en el archivo local
        FileOutputStream(localFile).use { fos ->
            wb.write(fos)
        }
        wb.close()

        // 🔹 Reiniciar asistencias desde este pago
        reconcileAsistenciasPorPago(localFile, dni, fecha, diasPorSemana)
    }
    fun reconcileAsistenciasPorPago(
        localFile: File,
        dni: String,
        fechaInicio: LocalDate,
        diasPorSemana: Int
    ) {
        val wb = WorkbookFactory.create(FileInputStream(localFile))
        val asistenciasSheet = wb.getSheet("asistencias") ?: return
        val pagosSheet = wb.getSheet("pagos") ?: return

        // buscar el último pago de ese cliente
        var ultimoPago: Pair<LocalDate, LocalDate>? = null
        for (i in 1..pagosSheet.lastRowNum) {
            val r = pagosSheet.getRow(i) ?: continue
            val dniPago = r.getCell(0)?.toString()?.trim().orEmpty()
            if (dniPago == dni) {
                val fInicio = r.getCell(6)?.toString()?.trim()
                val fFin = r.getCell(7)?.toString()?.trim()
                if (!fInicio.isNullOrBlank() && !fFin.isNullOrBlank()) {
                    try {
                        val inicio = LocalDate.parse(fInicio, DATE_FMT)
                        val fin = LocalDate.parse(fFin, DATE_FMT)
                        ultimoPago = inicio to fin
                    } catch (_: Exception) { }
                }
            }
        }

        if (ultimoPago == null) {
            wb.close()
            return
        }

        val (inicioPago, finPago) = ultimoPago

        // recorrer asistencias y marcar estado según rango y límite
        val semanaField = java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()
        var semanaActual = -1
        var semanaCounter = 0

        for (i in 1..asistenciasSheet.lastRowNum) {
            val r = asistenciasSheet.getRow(i) ?: continue
            val dniAsis = r.getCell(0)?.toString()?.trim().orEmpty()
            if (dniAsis != dni) continue

            val fechaStr = r.getCell(3)?.toString()?.trim()
            if (fechaStr.isNullOrBlank()) continue
            val fecha = LocalDate.parse(fechaStr, DATE_FMT)

            val dentroDePago = !fecha.isBefore(inicioPago) && !fecha.isAfter(finPago)

            if (!dentroDePago) {
                (r.getCell(4) ?: r.createCell(4)).setCellValue("FUERA_VIGENCIA")
            } else {
                val nroSemana = fecha.get(semanaField)
                if (nroSemana != semanaActual) {
                    semanaActual = nroSemana
                    semanaCounter = 0
                }
                semanaCounter++

                val estado = if (semanaCounter <= diasPorSemana) "OK" else "EXCEDIDO"
                (r.getCell(4) ?: r.createCell(4)).setCellValue(estado)
                (r.getCell(5) ?: r.createCell(5)).setCellValue(inicioPago.toString())
                (r.getCell(6) ?: r.createCell(6)).setCellValue(finPago.toString())
            }
        }

        // Guardar cambios
        FileOutputStream(localFile).use { fos -> wb.write(fos) }
        wb.close()
    }

}
