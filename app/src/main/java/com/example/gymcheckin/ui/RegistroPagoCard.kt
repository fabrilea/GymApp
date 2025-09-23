package com.example.gymcheckin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gymcheckin.vm.MainViewModel
import com.example.gymcheckin.vm.MessageKind

@Composable
fun RegisterPagoForm(
    vm: MainViewModel,
    onCancel: () -> Unit,
    onToast: (String) -> Unit
) {
    val ctx = LocalContext.current
    val ui by vm.ui.collectAsState()

    var dniPago by remember { mutableStateOf("") }
    var montoTxt by remember { mutableStateOf("") }
    var diasPorSemanaTxt by remember { mutableStateOf("") } // 🔹 campo de texto

    val montoD = montoTxt.toDoubleOrNull() ?: 0.0
    val diasPorSemana = diasPorSemanaTxt.toIntOrNull() ?: 0 // 🔹 conversión segura
    val formOk = dniPago.isNotBlank() && montoD > 0.0 && diasPorSemana > 0

    var status by remember { mutableStateOf<String?>(null) }
    var statusKind by remember { mutableStateOf(MessageKind.None) }

    LaunchedEffect(ui.message) {
        ui.message?.let { onToast(it) }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Registrar Pago", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = dniPago,
            onValueChange = { dniPago = it },
            label = { Text("DNI del cliente") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = montoTxt,
            onValueChange = { montoTxt = it },
            label = { Text("Monto") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // 🔹 Nuevo campo: Días por semana
        OutlinedTextField(
            value = diasPorSemanaTxt,
            onValueChange = { diasPorSemanaTxt = it },
            label = { Text("Días por semana") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        StatusBanner(kind = statusKind, text = status)

        Button(
            onClick = {
                if (formOk) {
                    vm.registrarPago(
                        ctx,
                        dni = dniPago.trim(),
                        monto = montoD,
                        diasPorSemana = diasPorSemana
                    )
                    status = "Pago registrado correctamente ✅"
                    statusKind = MessageKind.Ok

                    // limpiar inputs
                    dniPago = ""
                    montoTxt = ""
                    diasPorSemanaTxt = ""
                } else {
                    status = "Datos inválidos"
                    statusKind = MessageKind.Error
                    onToast("Datos inválidos")
                }
            },
            enabled = formOk,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar pago")
        }

        OutlinedButton(
            onClick = {
                onCancel()
                status = "Registro cancelado"
                statusKind = MessageKind.Warning
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cancelar")
        }
    }
}
