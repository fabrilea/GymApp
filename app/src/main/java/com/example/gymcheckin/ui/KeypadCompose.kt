// KeypadCompose.kt
package com.example.gymcheckin.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.gymcheckin.vm.MainViewModel
import com.example.gymcheckin.vm.MessageKind
import com.example.gymcheckin.vm.Screen

@Composable
fun GymApp(vm: MainViewModel) {
    val ui = vm.ui.collectAsState().value
    val ctx = LocalContext.current

    Surface(color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (ui.screen) {
                Screen.CheckIn -> CheckInView(vm = vm, onOk = { vm.onOk(ctx) })
                Screen.Admin -> AdminScreen(vm = vm, onBack = { vm.goTo(Screen.CheckIn) })
                Screen.RegisterPerson -> RegisterPersonView(vm = vm)
                Screen.ChangePin -> ChangePinView(vm = vm, onOk = { vm.onOk(ctx) })
                Screen.Excel -> ExcelScreenAdmin(vm = vm)
            }
        }
    }
}

/** ---------- CHECK-IN ---------- **/
@Composable
private fun CheckInView(vm: MainViewModel, onOk: () -> Unit) {
    val ui = vm.ui.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 🔹 Bajamos el cartel desde arriba
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "🏋️‍♂️ Bienvenido al Gym",
            style = MaterialTheme.typography.displayMedium, // cartel bien grande
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        // 🔹 Subimos un poco el keypad (queda más centrado visualmente)
        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center // 👈 lo deja un poco más arriba
        ) {
            NumericKeypad(
                onDigit = { vm.onDigit(it) },
                onBackspace = { vm.onBackspace() },
                onOk = onOk
            )
        }
    }

    // 🔹 Popup cartel de estado
    if (!ui.message.isNullOrBlank() && ui.messageKind != MessageKind.None) {
        AlertDialog(
            onDismissRequest = { vm.goTo(Screen.CheckIn) },
            modifier = Modifier.fillMaxWidth(0.9f),
            title = {
                Text(
                    when (ui.messageKind) {
                        MessageKind.Ok -> "✔️ ACCESO PERMITIDO"
                        MessageKind.Error, MessageKind.Warning -> "⛔ ACCESO DENEGADO"
                        else -> "Mensaje"
                    },
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    ui.message ?: "",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { vm.goTo(Screen.CheckIn) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                ) {
                    Text(
                        "CERRAR",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        )
    }
}



/** ---------- REGISTER PERSON ---------- **/
@Composable
private fun RegisterPersonView(vm: MainViewModel) {
    val ctx = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Registrar Persona", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        RegisterPersonForm(
            onSave = { dni, nombre, apellido, monto, dps, fIni ->
                vm.registerClienteYpago(
                    context = ctx,
                    dni = dni,
                    nombre = nombre,
                    apellido = apellido,
                    monto = monto,
                    diasPorSemana = dps
                )
            },
            onCancel = { vm.goTo(Screen.Admin) }
        )
    }
}

/** ---------- CHANGE PIN ---------- **/
@Composable
private fun ChangePinView(vm: MainViewModel, onOk: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Cambiar PIN", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        NumericKeypad(
            onDigit = { vm.onDigit(it) },
            onBackspace = { vm.onBackspace() },
            onOk = onOk
        )

        // 👇 Botón de volver
        OutlinedButton(
            onClick = { vm.goTo(Screen.Admin) }, // vuelve al panel de admin
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver")
        }
    }
}


/** ---------- REUSABLES ---------- **/
@Composable
fun NumericKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onOk: () -> Unit
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('B', '0', 'K') // B = borrar, K = ok
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { d ->
                    when (d) {
                        'B' -> KeyButton(
                            label = "Borrar",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.error,
                            textColor = MaterialTheme.colorScheme.onError,
                            onClick = onBackspace
                        )
                        'K' -> KeyButton(
                            label = "OK",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.primary,
                            textColor = MaterialTheme.colorScheme.onPrimary,
                            onClick = onOk
                        )
                        else -> KeyButton(
                            label = d.toString(),
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            textColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = { onDigit(d) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    label: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp > 600

    Box(
        modifier
            .height(if (isTablet) 120.dp else 90.dp)
            .background(color, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .then(modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = if (isTablet) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}
@Composable
fun StatusBanner(kind: MessageKind, text: String?) {
    if (!text.isNullOrBlank()) {
        val (bg, fg) = when (kind) {
            MessageKind.Ok -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
            MessageKind.Error, MessageKind.Warning ->
                MaterialTheme.colorScheme.error to MaterialTheme.colorScheme.onError
            else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(bg, RoundedCornerShape(12.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text,
                color = fg,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
