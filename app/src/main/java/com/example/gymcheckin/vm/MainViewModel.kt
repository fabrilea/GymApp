package com.example.gymcheckin.vm

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymcheckin.data.PinStore
import com.example.gymcheckin.data.entity.AsistenciaEntity
import com.example.gymcheckin.data.entity.PagoEntity
import com.example.gymcheckin.data.entity.Resumen
import com.example.gymcheckin.data.repository.GymRepository
import com.example.gymcheckin.util.PagoUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class Screen { CheckIn, Admin, RegisterPerson, ChangePin, Excel }
enum class MessageKind { None, Ok, Warning, Error }

data class UiState(
    val screen: Screen = Screen.CheckIn,
    val input: String = "",
    val message: String? = null,
    val messageKind: MessageKind = MessageKind.None,
    val ultimoPago: PagoEntity? = null
)

class MainViewModel(
    private val repo: GymRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    // ---------------- Navegación ----------------
    fun goTo(screen: Screen) {
        _ui.value = _ui.value.copy(screen = screen, message = null, messageKind = MessageKind.None)
    }

    // ---------------- Entrada teclado ----------------
    fun onDigit(c: Char) {
        _ui.value = _ui.value.copy(input = _ui.value.input + c)
    }

    fun onBackspace() {
        _ui.value = _ui.value.copy(input = _ui.value.input.dropLast(1))
    }

    // ---------------- OK (check-in o admin) ----------------
    fun onOk(ctx: Context) {
        viewModelScope.launch {
            val dni = _ui.value.input.trim()
            if (dni.isBlank()) return@launch

            // 1) Código admin
// 1) Código admin
            val pinActual = PinStore.getPin(ctx)
            if (dni == pinActual) {
                _ui.value = _ui.value.copy(
                    screen = Screen.Admin,
                    input = "",
                    message = "Modo administrador",
                    messageKind = MessageKind.Ok
                )
                return@launch
            }

            // 2) Buscar cliente + pago
            val cliente = repo.getClientePorDni(ctx, dni)
            val pago = repo.getUltimoPago(ctx, dni)

            if (cliente == null) {
                _ui.value = _ui.value.copy(
                    input = "",
                    message = "El cliente no existe",
                    messageKind = MessageKind.Error,
                    ultimoPago = pago
                )
            }

            // Fecha de asistencia actual
            val fechaHoy = java.time.LocalDate.now()

                val restan = pago?.let { PagoUtils.semanasRestantes(it.fechaPago) }
            if (restan != null) {
                if (restan > 0) {
                    // Calcular rango de la semana ISO actual
                    val hoy = fechaHoy
                    val inicioSemana = hoy.with(java.time.DayOfWeek.MONDAY)
                    val finSemana = hoy.with(java.time.DayOfWeek.SUNDAY)

                    val countSemana = repo.countAsistenciasSemana(
                        ctx,
                        dni,
                        inicioSemana.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        finSemana.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    )

                    if (countSemana + 1 > pago.diasPorSemana) {
                        // ❌ Ya superó su límite semanal
                        repo.appendAsistenciaEstadoFijo(
                            ctx,
                            asistencia = com.example.gymcheckin.data.entity.AsistenciaEntity(
                                dniCliente = dni,
                                nombre = cliente?.nombre ?: "",
                                apellido = cliente?.apellido ?: "",
                                fecha = fechaHoy
                            ),
                            estado = "EXCEDIDO"
                        )

                        _ui.value = _ui.value.copy(
                            input = "",
                            message = "Ya asistió $countSemana veces esta semana. Límite: ${pago.diasPorSemana}. ❌ No puede ingresar.",
                            messageKind = MessageKind.Error,
                            ultimoPago = pago
                        )
                    } else {
                        // ✅ Dentro del límite → registrar asistencia OK
                        repo.appendAsistenciaOk(
                            ctx,
                            asistencia = com.example.gymcheckin.data.entity.AsistenciaEntity(
                                dniCliente = dni,
                                nombre = cliente?.nombre ?: "",
                                apellido = cliente?.apellido ?: "",
                                fecha = fechaHoy
                            ),
                            limiteSemanal = pago.diasPorSemana,
                            countSemanaPrevio = countSemana
                        )

                        if (pago != null) {
                            if (pago.monto > 0) {
                                _ui.value = _ui.value.copy(
                                    input = "",
                                    message = "Bienvenido ${cliente?.nombre ?: "Cliente"}! Asistencia ${countSemana + 1}/${pago.diasPorSemana} esta semana. Restan $restan semanas de vigencia.",
                                    messageKind = MessageKind.Ok,
                                    ultimoPago = pago
                                )
                            } else {
                                _ui.value = _ui.value.copy(
                                    input = "",
                                    message = "Bienvenido ${cliente?.nombre ?: "Cliente"}! Asistencia ${countSemana + 1}/${pago.diasPorSemana} esta semana. USTED DEBE PAGAR EL MES",
                                    messageKind = MessageKind.Ok,
                                    ultimoPago = pago
                                )
                            }
                        }
                    } }else {
                    // Pago vencido
                    repo.appendAsistenciaEstadoFijo(
                        ctx,
                        asistencia = com.example.gymcheckin.data.entity.AsistenciaEntity(
                            dniCliente = dni,
                            nombre = cliente?.nombre ?: "",
                            apellido = cliente?.apellido ?: "",
                            fecha = fechaHoy
                        ),
                        estado = "SIN_PAGO"
                    )

                    _ui.value = _ui.value.copy(
                        input = "",
                        message = "Pago vencido — debe renovar",
                        messageKind = MessageKind.Warning,
                        ultimoPago = pago
                    )
                }
            }
            }
        }


    // ---------------- Registrar cliente y pago ----------------
    fun registerClienteYpago(
        context: Context,
        dni: String,
        nombre: String,
        apellido: String,
        monto: Double,
        diasPorSemana: Int
    ) {
        viewModelScope.launch {
            // ⚡️ usar la fecha actual como fechaPago
            val fechaPago = java.time.LocalDate.now()

            // validar monto


            val pago = PagoEntity(
                dni = dni,
                nombre = nombre,
                apellido = apellido,
                fechaPago = fechaPago,      // ahora sí fechaPago
                fin = fechaPago.plusDays(28),
                mes = fechaPago.month.name,
                monto = monto,
                diasPorSemana = diasPorSemana
            )

            repo.upsertPago(context, pago)
            if (monto <= 0) {
                _ui.value = _ui.value.copy(
                    message = "Usted debe pagar el mes luego",
                    messageKind = MessageKind.Ok,
                    ultimoPago = pago
                )
            } else {
                _ui.value = _ui.value.copy(
                    message = "Cliente y pago registrados el $fechaPago",
                    messageKind = MessageKind.Ok,
                    ultimoPago = pago
                )
            }
        }
    }

    // ---------------- Registrar solo pago ----------------
    fun registrarPago(
        context: Context,
        dni: String,
        monto: Double,
        diasPorSemana: Int = 3
    ) {
        viewModelScope.launch {
            try {
                // ⚡️ usar la fecha actual como fechaPago
                val fechaPago = java.time.LocalDate.now()

                if (monto <= 0) {
                    _ui.value = _ui.value.copy(
                        message = "El monto debe ser mayor a 0",
                        messageKind = MessageKind.Error
                    )
                    return@launch
                }

                val cliente = repo.getClientePorDni(context, dni)
                val nombre = cliente?.nombre ?: ""
                val apellido = cliente?.apellido ?: ""

                val pago = PagoEntity(
                    dni = dni,
                    nombre = nombre,
                    apellido = apellido,
                    fechaPago = fechaPago,               // ahora es fechaPago
                    fin = fechaPago.plusDays(28),
                    mes = fechaPago.month.name,
                    monto = monto,
                    diasPorSemana = diasPorSemana
                )

                repo.upsertPago(context, pago)

                val restan = com.example.gymcheckin.util.PagoUtils.semanasRestantes(pago.fechaPago)
                _ui.value = _ui.value.copy(
                    message = "Pago registrado el $fechaPago. Vigencia: $restan semanas restantes",
                    messageKind = MessageKind.Ok,
                    ultimoPago = pago
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    message = "Error al registrar pago: ${e.message}",
                    messageKind = MessageKind.Error
                )
            }
        }
    }





    // ---------------- Resumen ----------------
    private fun recalcularResumen(pagos: List<PagoEntity>, asistencias: List<AsistenciaEntity>): List<Resumen> {
        return pagos.map { pago ->
            val total = asistencias.count {
                it.dniCliente == pago.dni &&
                        !it.fecha.isBefore(pago.fechaPago) &&
                        !it.fecha.isAfter(pago.fin)
            }
            Resumen(
                dni = pago.dni,
                nombre = pago.nombre,
                mes = pago.mes,
                inicio = pago.fechaPago,
                fin = pago.fin,
                monto = pago.monto,
                totalAsistencias = total
            )
        }
    }
}
