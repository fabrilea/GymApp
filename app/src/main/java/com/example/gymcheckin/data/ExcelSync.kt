package com.example.gymcheckin.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.gymcheckin.data.dto.ClienteExcelDto
import com.example.gymcheckin.data.dto.PagoExcelDto
import com.example.gymcheckin.data.dto.AsistenciaExcelDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ExcelSync {

    companion object {
        private const val HOJA_CLIENTES = "clientes"
        private const val HOJA_PAGOS = "pagos"
        private const val HOJA_ASISTENCIAS = "asistencias"
        private const val TAG = "ExcelSync"

        private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }

    private fun ensureAtLeastOneSheet(wb: Workbook) {
        if (wb.numberOfSheets == 0) {
            Log.w(TAG, "⚠️ Workbook vacío → creando hoja dummy Sheet1")
            wb.createSheet("Sheet1").apply {
                createRow(0).apply {
                    createCell(0).setCellValue("dummy")
                }
            }
        }
    }


    // ----------- Abrir o crear Excel -----------
// ----------- Abrir o crear Excel -----------
    private fun openWorkbook(ctx: Context, uri: Uri): Workbook {
        return try {
            val wb = requireNotNull(ctx.contentResolver.openInputStream(uri)) {
                "No se pudo abrir Excel (lectura)"
            }.use { WorkbookFactory.create(it) }

            ensureAtLeastOneSheet(wb)
            ensureAllSheets(wb)

            Log.d(TAG, "✅ Workbook abierto y validado correctamente")
            wb
        } catch (e: Exception) {
            val wb = XSSFWorkbook()
            ensureAtLeastOneSheet(wb)
            ensureAllSheets(wb)

            Log.w(TAG, "⚠️ Se creó un workbook nuevo porque no se pudo abrir: ${e.message}")
            saveWorkbook(ctx, uri, wb)
            wb
        }
    }



    private fun saveWorkbook(ctx: Context, uri: Uri, wb: Workbook) {
        try {
            ctx.contentResolver.openOutputStream(uri, "w").use { out ->
                requireNotNull(out) { "No se pudo abrir Excel (escritura)" }
                wb.write(out)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar Excel", e)
            throw IllegalStateException("No se pudo guardar el Excel. Usá archivo local editable.")
        } finally {
            wb.close()
        }
    }

    // ----------- Asegurar hojas con encabezados -----------
    private fun ensureAllSheets(wb: Workbook) {
        if (wb.getSheet(HOJA_CLIENTES) == null) {
            Log.w(TAG, "⚠️ Faltaba hoja clientes → creada con encabezados")
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
            Log.w(TAG, "⚠️ Faltaba hoja pagos → creada con encabezados")
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
            Log.w(TAG, "⚠️ Faltaba hoja asistencias → creada con encabezados")
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


    // -------------------- READERS --------------------
    suspend fun readClientes(ctx: Context, uri: Uri): List<ClienteExcelDto> =
        withContext(Dispatchers.IO) {
            val wb = openWorkbook(ctx, uri)
            val out = mutableListOf<ClienteExcelDto>()
            wb.getSheet(HOJA_CLIENTES)?.let { sheet ->
                val it = sheet.rowIterator()
                if (it.hasNext()) it.next() // encabezado
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

    suspend fun readPagos(ctx: Context, uri: Uri): List<PagoExcelDto> =
        withContext(Dispatchers.IO) {
            val wb = openWorkbook(ctx, uri)
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

    suspend fun readAsistencias(ctx: Context, uri: Uri): List<AsistenciaExcelDto> =
        withContext(Dispatchers.IO) {
            val wb = openWorkbook(ctx, uri)
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
    suspend fun upsertClienteExcel(ctx: Context, uri: Uri, dto: ClienteExcelDto) =
        withContext(Dispatchers.IO) {
            val wb = openWorkbook(ctx, uri)
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
                val row = sheet.createRow(sheet.lastRowNum + 1)
                row.createCell(0).setCellValue(dto.dni)
                row.createCell(1).setCellValue(dto.nombre)
                row.createCell(2).setCellValue(dto.apellido)
                row.createCell(3).setCellValue(dto.fechaInicio.toString())
            }
            saveWorkbook(ctx, uri, wb)
        }

    // -------------------- PAGOS --------------------
    fun upsertPagoConIntegracion(
        ctx: Context,
        uri: Uri,
        dto: PagoExcelDto,
        nombre: String = "",
        apellido: String = ""
    ) {
        val fechaInicio = dto.fechaPago
        val fechaFin = fechaInicio.plusDays(28)

        val wb = openWorkbook(ctx, uri)
        val pagosSheet = wb.getSheet(HOJA_PAGOS)

        // Insertar fila en pagos
        val rowPago = pagosSheet.createRow(pagosSheet.lastRowNum + 1)
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
            val rowCli = clientesSheet.createRow(clientesSheet.lastRowNum + 1)
            rowCli.createCell(0).setCellValue(dto.dni)
            rowCli.createCell(1).setCellValue(nombre)
            rowCli.createCell(2).setCellValue(apellido)
        }

        saveWorkbook(ctx, uri, wb)
    }


    // -------------------- ASISTENCIAS --------------------
    fun appendAsistenciaExcel(
        ctx: Context,
        uri: Uri,
        dto: AsistenciaExcelDto,
        estado: String = "OK",
        nombre: String = "",
        apellido: String = "",
    ) {
        val wb = openWorkbook(ctx, uri)
        val sheet = wb.getSheet(HOJA_ASISTENCIAS)

        val row = sheet.createRow(sheet.lastRowNum + 1)
        row.createCell(0).setCellValue(dto.dni)
        row.createCell(1).setCellValue(nombre)
        row.createCell(2).setCellValue(apellido)
        row.createCell(3).setCellValue(dto.fecha.toString())
        row.createCell(4).setCellValue(estado)

        if (estado == "SIN_PAGO") {
            val style = wb.createCellStyle().apply {
                fillPattern = FillPatternType.SOLID_FOREGROUND
                fillForegroundColor = IndexedColors.LIGHT_YELLOW.index
            }
            row.getCell(4).cellStyle = style
        }

        saveWorkbook(ctx, uri, wb)
    }

    // -------------------- RECONCILIAR ASISTENCIAS --------------------
    fun reconcileAsistenciasPorPago(
        ctx: Context,
        uri: Uri,
        dni: String,
        fechaInicio: LocalDate,
        diasPorSemana: Int
    ) {
        val wb = openWorkbook(ctx, uri)
        val asistenciasSheet = wb.getSheet(HOJA_ASISTENCIAS)
        val pagosSheet = wb.getSheet(HOJA_PAGOS)

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
            saveWorkbook(ctx, uri, wb)
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

        saveWorkbook(ctx, uri, wb)
    }

    fun resetAsistenciasSiNuevoMes(ctx: Context, uri: Uri) {
        val wb = openWorkbook(ctx, uri)

        val sheet = wb.getSheet(HOJA_ASISTENCIAS)
        if (sheet != null && sheet.lastRowNum > 0) {
            val primeraAsistencia = sheet.getRow(1)
            val fechaStr = primeraAsistencia?.getCell(3)?.toString()?.trim()
            val fecha = if (!fechaStr.isNullOrBlank()) {
                try { LocalDate.parse(fechaStr, DATE_FMT) } catch (_: Exception) { null }
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
                    createRow(0).apply {
                        createCell(0).setCellValue("dni")
                        createCell(1).setCellValue("nombre")
                        createCell(2).setCellValue("apellido")
                        createCell(3).setCellValue("fechaAsistencia")
                        createCell(4).setCellValue("estado")
                        createCell(5).setCellValue("fechaInicioPago")
                        createCell(6).setCellValue("fechaFinPago")
                    }
                }
            }
        }

        saveWorkbook(ctx, uri, wb)
    }

    fun resetPagosSiNuevoMes(ctx: Context, uri: Uri) {
        val wb = openWorkbook(ctx, uri)

        val sheet = wb.getSheet(HOJA_PAGOS)
        if (sheet != null && sheet.lastRowNum > 0) {
            val primerPago = sheet.getRow(1)
            val fechaStr = primerPago?.getCell(3)?.toString()?.trim()
            val fecha = if (!fechaStr.isNullOrBlank()) {
                try { LocalDate.parse(fechaStr, DATE_FMT) } catch (_: Exception) { null }
            } else null

            val mesGuardado = fecha?.monthValue
            val anioGuardado = fecha?.year
            val hoy = LocalDate.now()

            if (mesGuardado != hoy.monthValue || anioGuardado != hoy.year) {
                val idx = wb.getSheetIndex(HOJA_PAGOS)
                val backupName = "pagos_${anioGuardado ?: "NA"}_${mesGuardado?.toString()?.padStart(2, '0') ?: "NA"}"

                val existingIdx = wb.getSheetIndex(backupName)
                if (existingIdx != -1) {
                    wb.removeSheetAt(existingIdx)
                }

                wb.setSheetName(idx, backupName)

                // 👇 aseguramos que nunca quede vacío
                ensureAtLeastOneSheet(wb)

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
        }

        saveWorkbook(ctx, uri, wb)
    }

    fun upsertPagoSeguro(
        ctx: Context,
        uri: Uri,
        dto: PagoExcelDto,
        nombre: String,
        apellido: String
    ) {
        val wb = openWorkbook(ctx, uri)
        val pagosSheet = wb.getSheet(HOJA_PAGOS)

        // Buscar si ya tiene fila de pago en este mes
        var updated = false
        for (i in 1..pagosSheet.lastRowNum) {
            val r = pagosSheet.getRow(i) ?: continue
            val dniCell = r.getCell(0)?.toString()?.trim() ?: continue
            if (dniCell == dto.dni) {
                // ⚡ actualizar fila existente
                (r.getCell(1) ?: r.createCell(1)).setCellValue(nombre)
                (r.getCell(2) ?: r.createCell(2)).setCellValue(apellido)
                (r.getCell(3) ?: r.createCell(3)).setCellValue(dto.fechaPago.toString())
                (r.getCell(4) ?: r.createCell(4)).setCellValue(dto.monto)
                (r.getCell(5) ?: r.createCell(5)).setCellValue(dto.diasPorSemana.toDouble())
                (r.getCell(6) ?: r.createCell(6)).setCellValue(dto.fechaPago.toString())
                (r.getCell(7) ?: r.createCell(7)).setCellValue(dto.fechaPago.plusDays(28).toString())
                updated = true
                break
            }
        }

        // Si no existía → crear fila nueva
        if (!updated) {
            val row = pagosSheet.createRow(pagosSheet.lastRowNum + 1)
            row.createCell(0).setCellValue(dto.dni)
            row.createCell(1).setCellValue(nombre)
            row.createCell(2).setCellValue(apellido)
            row.createCell(3).setCellValue(dto.fechaPago.toString())
            row.createCell(4).setCellValue(dto.monto)
            row.createCell(5).setCellValue(dto.diasPorSemana.toDouble())
            row.createCell(6).setCellValue(dto.fechaPago.toString())
            row.createCell(7).setCellValue(dto.fechaPago.plusDays(28).toString())
        }

        // Actualizar estado en clientes
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
            val rowCli = clientesSheet.createRow(clientesSheet.lastRowNum + 1)
            rowCli.createCell(0).setCellValue(dto.dni)
            rowCli.createCell(1).setCellValue(nombre)
            rowCli.createCell(2).setCellValue(apellido)
            rowCli.createCell(3).setCellValue(dto.fechaPago.toString())
        }

        saveWorkbook(ctx, uri, wb)
    }
    suspend fun addPagoSolo(
        ctx: Context,
        uri: Uri,
        dni: String,
        monto: Double,
        fechaMs: Long,
        diasPorSemana: Int // 👈 usar este parámetro
    ) {
        val wb = WorkbookFactory.create(ctx.contentResolver.openInputStream(uri))
        val sheetClientes = wb.getSheet("clientes") ?: error("No existe la hoja 'clientes'")
        val sheetPagos = wb.getSheet("pagos") ?: wb.createSheet("pagos")

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

        val fecha = java.time.Instant.ofEpochMilli(fechaMs)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
        val fechaFin = fecha.plusDays(28)

        // 👇 Usar directamente el parámetro recibido
        val dps = diasPorSemana

        // Crear nueva fila en pagos
        val lastUsed = getLastUsedRowIndex(sheetPagos)
        val row = sheetPagos.createRow(lastUsed + 1)
        row.createCell(0).setCellValue(dni)
        row.createCell(1).setCellValue(nombre)
        row.createCell(2).setCellValue(apellido)
        row.createCell(3).setCellValue(fecha.toString())
        row.createCell(4).setCellValue(monto)
        row.createCell(5).setCellValue(dps.toDouble())   // 👈 guarda lo que vino del form
        row.createCell(6).setCellValue(fecha.toString())
        row.createCell(7).setCellValue(fechaFin.toString())

        // Guardar cliente
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
            val rowCli = sheetClientes.createRow(sheetClientes.lastRowNum + 1)
            rowCli.createCell(0).setCellValue(dni)
            rowCli.createCell(1).setCellValue(nombre)
            rowCli.createCell(2).setCellValue(apellido)
            rowCli.createCell(3).setCellValue(fecha.toString())
        }

        // Guardar cambios
        ctx.contentResolver.openOutputStream(uri, "w")?.use { out ->
            wb.write(out)
            out.flush()
        }
        wb.close()

        // 🔹 Reiniciar asistencias desde este pago
        reconcileAsistenciasPorPago(ctx, uri, dni, fecha, dps)
    }






    /**
     * Busca la última fila REAL con datos en la hoja de pagos.
     * Ignora filas sucias o creadas en blanco al final.
     */
    fun getLastUsedRowIndex(sheet: Sheet): Int {
        return (sheet.lastRowNum downTo 1).firstOrNull { i ->
            val r = sheet.getRow(i)
            val dni = r?.getCell(0)?.toString()?.trim()
            !dni.isNullOrBlank()
        } ?: 0
    }




}
