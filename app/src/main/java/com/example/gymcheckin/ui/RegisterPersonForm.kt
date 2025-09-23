// RegisterPersonForm.kt
package com.example.gymcheckin.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.gymcheckin.vm.MainViewModel
import com.example.gymcheckin.vm.Screen
import com.example.gymcheckin.vm.MessageKind

@Composable
fun RegisterPersonForm(
    onSave: (dni: String, nombre: String, apellido: String, monto: Double, dps: Int, fIni: Long) -> Unit,
    onCancel: () -> Unit
) {
    var dni by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var diasPorSemana by remember { mutableStateOf("3") }

    // Para feedback visual
    var status by remember { mutableStateOf<String?>(null) }
    var statusKind by remember { mutableStateOf(MessageKind.None) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = apellido,
            onValueChange = { apellido = it },
            label = { Text("Apellido") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = dni,
            onValueChange = { dni = it },
            label = { Text("DNI") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = monto,
            onValueChange = { monto = it },
            label = { Text("Monto") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = diasPorSemana,
            onValueChange = { diasPorSemana = it },
            label = { Text("Días por semana") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // Cartel grande
        StatusBanner(kind = statusKind, text = status)

        Row(horizontalArrangement = spacedBy(12.dp)) {
            Button(onClick = {
                val fIni = System.currentTimeMillis()
                if (dni.isBlank() || nombre.isBlank()) {
                    status = "Faltan datos obligatorios"
                    statusKind = MessageKind.Error
                } else {
                    onSave(
                        dni.trim(),
                        nombre.trim(),
                        apellido.trim(),
                        monto.toDoubleOrNull() ?: 0.0,
                        diasPorSemana.toIntOrNull() ?: 0,
                        fIni
                    )
                    status = "Cliente registrado correctamente ✅"
                    statusKind = MessageKind.Ok
                }
            }, modifier = Modifier.weight(1f)) {
                Text("Guardar")
            }
            OutlinedButton(onClick = {
                onCancel()
                status = "Registro cancelado"
                statusKind = MessageKind.Warning
            }, modifier = Modifier.weight(1f)) {
                Text("Cancelar")
            }
        }
    }
}
