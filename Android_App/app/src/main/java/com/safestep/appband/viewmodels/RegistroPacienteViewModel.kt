package com.safestep.appband.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safestep.appband.repositories.StorageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para RegistroPacienteFragment.
 * Gestiona los 7 pasos del formulario de registro de paciente.
 */
class RegistroPacienteViewModel(application: Application) : AndroidViewModel(application) {

    private val storageRepo = StorageRepository(application)

    // Current step (1-7)
    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep

    // ══════════════════════════════════════════
    // STEP 1: Datos del Paciente
    // ══════════════════════════════════════════
    var nombreCompleto: String = ""
    var fechaNacimiento: String = ""
    var genero: String = ""
    var peso: String = ""
    var altura: String = ""
    var tipoSangre: String = ""
    var direccion: String = ""
    var telefono: String = ""
    var photoUri: String = ""

    // ══════════════════════════════════════════
    // STEP 2: Información Médica
    // ══════════════════════════════════════════
    var tieneMarcapasos: Boolean = false
    val enfermedadesSeleccionadas = mutableSetOf<String>()
    val medicamentosSeleccionados = mutableSetOf<String>()
    var frecuenciaReposo: Int = 70
    var tieneDesmayos: Boolean = false
    var tieneCaidas: Boolean = false
    var tieneDesfibrilador: Boolean = false

    // ══════════════════════════════════════════
    // STEP 3: Umbrales de Alerta
    // ══════════════════════════════════════════
    var pulsoMaximo: Int = 120
    var pulsoMinimo: Int = 50
    var sensibilidadCaidas: String = "Media" // Alta, Media, Baja

    // ══════════════════════════════════════════
    // STEP 4: Tutor / Familiar Responsable
    // ══════════════════════════════════════════
    var tutorNombre: String = ""
    var tutorParentesco: String = ""
    var tutorTelefono: String = ""
    var tutorTelefonoAlt: String = ""
    var tutorCorreo: String = ""
    var notifSMS: Boolean = true
    var notifCorreo: Boolean = true
    var tutorDireccion: String = ""

    // ══════════════════════════════════════════
    // STEP 5: Contactos de Emergencia
    // ══════════════════════════════════════════
    data class ContactoEmergencia(
        var nombre: String = "",
        var telefono: String = "",
        var parentesco: String = "",
        var llamarAuto: Boolean = false
    )
    val contactosEmergencia = mutableListOf(ContactoEmergencia())

    // ══════════════════════════════════════════
    // STEP 6: Configuración de Alertas
    // ══════════════════════════════════════════
    var modoAlerta: String = "Sonido y Vibración"
    var alertaPulso: Boolean = true
    var alertaCaida: Boolean = true
    var alertaInactividad: Boolean = false
    var enviarGPS: Boolean = true
    var intervaloMonitoreo: Int = 30

    // ══════════════════════════════════════════
    // STEP 7: Resumen
    // ══════════════════════════════════════════
    var aceptaTerminos: Boolean = false

    // ══════════════════════════════════════════
    // Save state
    // ══════════════════════════════════════════
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    sealed class SaveState {
        object Idle : SaveState()
        object Saving : SaveState()
        object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }

    fun nextStep() {
        if (_currentStep.value < 7) {
            _currentStep.value = _currentStep.value + 1
        }
    }

    fun previousStep() {
        if (_currentStep.value > 1) {
            _currentStep.value = _currentStep.value - 1
        }
    }

    fun goToStep(step: Int) {
        if (step in 1..7) {
            _currentStep.value = step
        }
    }

    fun addContact() {
        if (contactosEmergencia.size < 3) {
            contactosEmergencia.add(ContactoEmergencia())
        }
    }

    fun removeContact(index: Int) {
        if (contactosEmergencia.size > 1 && index in contactosEmergencia.indices) {
            contactosEmergencia.removeAt(index)
        }
    }

    fun validateCurrentStep(): Boolean {
        return when (_currentStep.value) {
            1 -> nombreCompleto.isNotBlank() && fechaNacimiento.isNotBlank() && genero.isNotBlank()
            2 -> true // Información médica es opcional en su mayoría
            3 -> pulsoMinimo < pulsoMaximo
            4 -> tutorNombre.isNotBlank() && tutorTelefono.isNotBlank()
            5 -> contactosEmergencia.any { it.nombre.isNotBlank() && it.telefono.isNotBlank() }
            6 -> true // Config alertas tiene defaults
            7 -> aceptaTerminos
            else -> false
        }
    }

    fun saveAllData() {
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            try {
                val data = mapOf(
                    // Step 1
                    "nombreCompleto" to nombreCompleto,
                    "fechaNacimiento" to fechaNacimiento,
                    "genero" to genero,
                    "peso" to peso,
                    "altura" to altura,
                    "tipoSangre" to tipoSangre,
                    "direccion" to direccion,
                    "telefono" to telefono,
                    "photoUri" to photoUri,
                    // Step 2
                    "tieneMarcapasos" to tieneMarcapasos,
                    "enfermedades" to enfermedadesSeleccionadas.toList(),
                    "medicamentos" to medicamentosSeleccionados.toList(),
                    "frecuenciaReposo" to frecuenciaReposo,
                    "tieneDesmayos" to tieneDesmayos,
                    "tieneCaidas" to tieneCaidas,
                    "tieneDesfibrilador" to tieneDesfibrilador,
                    // Step 3
                    "pulsoMaximo" to pulsoMaximo,
                    "pulsoMinimo" to pulsoMinimo,
                    "sensibilidadCaidas" to sensibilidadCaidas,
                    // Step 4
                    "tutorNombre" to tutorNombre,
                    "tutorParentesco" to tutorParentesco,
                    "tutorTelefono" to tutorTelefono,
                    "tutorTelefonoAlt" to tutorTelefonoAlt,
                    "tutorCorreo" to tutorCorreo,
                    "notifSMS" to notifSMS,
                    "notifCorreo" to notifCorreo,
                    "tutorDireccion" to tutorDireccion,
                    // Step 5
                    "contactosEmergencia" to contactosEmergencia.map {
                        mapOf(
                            "nombre" to it.nombre,
                            "telefono" to it.telefono,
                            "parentesco" to it.parentesco,
                            "llamarAuto" to it.llamarAuto
                        )
                    },
                    // Step 6
                    "modoAlerta" to modoAlerta,
                    "alertaPulso" to alertaPulso,
                    "alertaCaida" to alertaCaida,
                    "alertaInactividad" to alertaInactividad,
                    "enviarGPS" to enviarGPS,
                    "intervaloMonitoreo" to intervaloMonitoreo
                )
                storageRepo.saveLocal("paciente_registro", data.toString())
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    // Summary generators for Step 7
    fun getResumenDatos(): String {
        return buildString {
            appendLine("Nombre: $nombreCompleto")
            appendLine("Fecha de Nacimiento: $fechaNacimiento")
            appendLine("Género: $genero")
            appendLine("Peso: ${peso}kg  •  Altura: ${altura}cm")
            appendLine("Tipo de Sangre: $tipoSangre")
            appendLine("Dirección: $direccion")
            append("Teléfono: $telefono")
        }
    }

    fun getResumenMedica(): String {
        return buildString {
            appendLine("Marcapasos: ${if (tieneMarcapasos) "Sí" else "No"}")
            appendLine("Enfermedades: ${if (enfermedadesSeleccionadas.isEmpty()) "Ninguna" else enfermedadesSeleccionadas.joinToString(", ")}")
            appendLine("Medicamentos: ${if (medicamentosSeleccionados.isEmpty()) "Ninguno" else medicamentosSeleccionados.joinToString(", ")}")
            appendLine("FC en Reposo: ${frecuenciaReposo} BPM")
            appendLine("Desmayos: ${if (tieneDesmayos) "Sí" else "No"}")
            appendLine("Caídas previas: ${if (tieneCaidas) "Sí" else "No"}")
            append("Desfibrilador: ${if (tieneDesfibrilador) "Sí" else "No"}")
        }
    }

    fun getResumenUmbrales(): String {
        return buildString {
            appendLine("Pulso Máximo: $pulsoMaximo BPM")
            appendLine("Pulso Mínimo: $pulsoMinimo BPM")
            append("Sensibilidad Caídas: $sensibilidadCaidas")
        }
    }

    fun getResumenTutor(): String {
        return buildString {
            appendLine("Nombre: $tutorNombre")
            appendLine("Parentesco: $tutorParentesco")
            appendLine("Teléfono: $tutorTelefono")
            if (tutorTelefonoAlt.isNotBlank()) appendLine("Tel. Alternativo: $tutorTelefonoAlt")
            appendLine("Correo: $tutorCorreo")
            appendLine("Notificaciones SMS: ${if (notifSMS) "Sí" else "No"}")
            appendLine("Notificaciones Correo: ${if (notifCorreo) "Sí" else "No"}")
            append("Dirección: $tutorDireccion")
        }
    }

    fun getResumenContactos(): String {
        return buildString {
            contactosEmergencia.forEachIndexed { index, c ->
                appendLine("Contacto ${index + 1}: ${c.nombre}")
                appendLine("  Teléfono: ${c.telefono}")
                appendLine("  Parentesco: ${c.parentesco}")
                appendLine("  Llamada auto: ${if (c.llamarAuto) "Sí" else "No"}")
                if (index < contactosEmergencia.size - 1) appendLine()
            }
        }
    }

    fun getResumenConfig(): String {
        return buildString {
            appendLine("Modo de Alerta: $modoAlerta")
            appendLine("Alerta Pulso: ${if (alertaPulso) "Activa" else "Inactiva"}")
            appendLine("Alerta Caída: ${if (alertaCaida) "Activa" else "Inactiva"}")
            appendLine("Alerta Inactividad: ${if (alertaInactividad) "Activa" else "Inactiva"}")
            appendLine("Enviar GPS: ${if (enviarGPS) "Sí" else "No"}")
            append("Intervalo: ${intervaloMonitoreo} segundos")
        }
    }
}
