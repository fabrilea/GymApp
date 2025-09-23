package com.example.gymcheckin.ui

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.gymcheckin.data.ExcelLinkStore
import com.example.gymcheckin.data.ExcelSync
import com.example.gymcheckin.vm.MainViewModel
import com.example.gymcheckin.vm.Screen
import com.example.gymcheckin.vm.MessageKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AdminScreen(vm: MainViewModel, onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Panel de Admin", style = MaterialTheme.typography.headlineSmall)

                Button(onClick = { vm.goTo(Screen.RegisterPerson) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Registrar persona")
                }
                Button(onClick = { vm.goTo(Screen.ChangePin) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cambiar PIN")
                }
                Button(onClick = { vm.goTo(Screen.Excel) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Gestionar Excel")
                }
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Volver")
                }
            }
        }
    }
}

/** Pantalla de gestión de Excel — Excel-only (sin Room) */
@Composable
fun ExcelScreenAdmin(vm: MainViewModel, onBack: () -> Unit = { vm.goTo(Screen.Admin) }) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var statusKind by remember { mutableStateOf(MessageKind.None) }
    var counts by remember { mutableStateOf("—") }

    val sync = remember { ExcelSync() }
    var excelUri by remember { mutableStateOf(ExcelLinkStore.getUri(ctx)) }

    // Inputs del formulario de pago
// Inputs del formulario de pago
    var dniPago by remember { mutableStateOf("") }
    var montoPago by remember { mutableStateOf("") }
    var diasPorSemanaTxt by remember { mutableStateOf("") }

// Conversión a número
    val diasPorSemana: Int = diasPorSemanaTxt.toIntOrNull() ?: 0



    // 🔹 Estados para confirmaciones
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showReplaceDialog by remember { mutableStateOf(false) }
    var pendingUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var lastFileName by remember { mutableStateOf<String?>(null) }

    // Launcher para elegir Excel
    val openExcel = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            if (excelUri == null) {
                // Primera vez → guardar directo
                try {
                    ctx.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (_: SecurityException) { }
                ExcelLinkStore.setUri(ctx, uri)
                excelUri = uri
                lastFileName = uri.lastPathSegment
                showConfirmDialog = true
                refreshCountsExcelOnly(scope, sync, ctx) { counts = it }
            } else {
                // Ya existe → pedir confirmación antes de reemplazar
                pendingUri = uri
                showReplaceDialog = true
            }
        }
    }

    // Refrescar conteos
    fun refresh() {
        refreshCountsExcelOnly(scope, sync, ctx) { counts = it }
    }
    LaunchedEffect(Unit) { refresh() }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Gestión de Excel", style = MaterialTheme.typography.headlineSmall)
                Text(counts, style = MaterialTheme.typography.bodyMedium)

                // --- FORMULARIO DE PAGO ---
                OutlinedTextField(
                    value = dniPago,
                    onValueChange = { dniPago = it.trim() },
                    label = { Text("DNI") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = montoPago,
                    onValueChange = { montoPago = it.trim() },
                    label = { Text("Monto") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = diasPorSemanaTxt,
                    onValueChange = { diasPorSemanaTxt = it.trim() },
                    label = { Text("Días por semana") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )


                Button(
                    onClick = {
                        val uri = excelUri
                        if (uri == null) {
                            status = "Vinculá un Excel primero."
                            statusKind = MessageKind.Error
                        } else if (dniPago.isBlank() || montoPago.isBlank()) {
                            status = "Completá DNI y monto."
                            statusKind = MessageKind.Warning
                        } else {
                            scope.launch {
                                busy = true
                                status = "Agregando pago…"
                                statusKind = MessageKind.None
                                runCatching {
                                    withContext(Dispatchers.IO) {
                                        val monto = montoPago.toDoubleOrNull()
                                            ?: error("Monto inválido")
                                        sync.addPagoSolo(
                                            ctx,
                                            uri,
                                            dniPago,
                                            monto,
                                            System.currentTimeMillis(),
                                            diasPorSemana // 👈 agregar este parámetro
                                        )
                                    }
                                }
                                    .onSuccess {
                                        status = "Pago agregado para $dniPago ✅"
                                        statusKind = MessageKind.Ok
                                        dniPago = ""
                                        montoPago = ""
                                        refresh()
                                    }
                                    .onFailure { e ->
                                        status = "Error: ${e.message}"
                                        statusKind = MessageKind.Error
                                    }
                                busy = false
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Agregar Pago") }

                // Vincular / Cambiar Excel
                Button(
                    onClick = {
                        openExcel.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-excel"
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (excelUri == null) "Vincular Excel" else "Cambiar Excel") }

                // Estado
                StatusBanner(kind = statusKind, text = status)

                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("Volver")
                }
            }
        }
    }

    // 🔹 Dialogo de confirmación al guardar/reemplazar
    if (showConfirmDialog && lastFileName != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            confirmButton = {
                Button(onClick = { showConfirmDialog = false }) {
                    Text("OK")
                }
            },
            title = { Text("Excel vinculado") },
            text = { Text("Se vinculó correctamente el archivo: $lastFileName") }
        )
    }

    if (showReplaceDialog && pendingUri != null) {
        AlertDialog(
            onDismissRequest = { showReplaceDialog = false; pendingUri = null },
            confirmButton = {
                Button(onClick = {
                    val uri = pendingUri!!
                    try {
                        ctx.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    } catch (_: SecurityException) { }
                    ExcelLinkStore.setUri(ctx, uri)
                    excelUri = uri
                    lastFileName = uri.lastPathSegment
                    refreshCountsExcelOnly(scope, sync, ctx) { counts = it }
                    showReplaceDialog = false
                    pendingUri = null
                    showConfirmDialog = true
                }) {
                    Text("Reemplazar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showReplaceDialog = false
                    pendingUri = null
                }) { Text("Cancelar") }
            },
            title = { Text("Cambiar Excel") },
            text = { Text("Ya existe un archivo vinculado.\n¿Querés reemplazarlo por: ${pendingUri?.lastPathSegment}?") }
        )
    }
}

private fun refreshCountsExcelOnly(
    scope: CoroutineScope? = null,
    sync: ExcelSync,
    ctx: Context,
    onText: (String) -> Unit
) {
    if (scope == null) return
    scope.launch {
        val txt = withContext(Dispatchers.IO) {
            val uri = ExcelLinkStore.getUri(ctx)
            if (uri == null) {
                "Clientes: — • Pagos: — • Asistencias: —"
            } else {
                val c = sync.readClientes(ctx, uri).size
                val p = sync.readPagos(ctx, uri).size
                val a = sync.readAsistencias(ctx, uri).size
                "Clientes: $c • Pagos: $p • Asistencias: $a"
            }
        }
        onText(txt)
    }
}
