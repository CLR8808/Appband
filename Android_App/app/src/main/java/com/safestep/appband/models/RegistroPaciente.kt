package com.safestep.appband.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Sección 1 — Datos personales del paciente
 */
data class DatosPaciente(
    val nombreCompleto: String = "",
    val fechaNacimiento: String = "",
    val edad: Int = 0,
    val genero: String = "",
    val peso: Double = 0.0,
    val altura: Double = 0.0,
    val tipoSangre: String = "",
    val fotoPerfil: String? = null,
    val direccion: String = "",
    val telefono: String = ""
)

/**
 * Sub-estructura de Desmayos para InfoMedica
 */
data class DesmayosInfo(
    val tiene: Boolean = false,
    val frecuencia: String = ""
)

/**
 * Sub-estructura de Historial de Caídas para InfoMedica
 */
data class HistorialCaidasInfo(
    val tiene: Boolean = false,
    val cantidad: Int = 0
)

/**
 * Sección 2 — Información médica relevante al monitoreo de pulso
 */
data class InfoMedica(
    val tieneMarcapasos: Boolean = false,
    val enfermedadesCardiacas: List<String> = emptyList(),
    val otraEnfermedad: String = "",
    val medicamentos: List<String> = emptyList(),
    val otroMedicamento: String = "",
    val fcReposo: Int = 70,
    val desmayos: DesmayosInfo = DesmayosInfo(),
    val historialCaidas: HistorialCaidasInfo = HistorialCaidasInfo(),
    val tieneDesfibrilador: Boolean = false
)

/**
 * Sección 3 — Umbrales de alerta personalizados
 */
data class UmbralesAlerta(
    val pulsoMaximo: Int = 120,
    val pulsoMinimo: Int = 50,
    val sensibilidadCaidas: String = "media" // alta, media, baja
)

/**
 * Sección 4 — Datos del tutor/familiar responsable
 */
data class DatosTutor(
    val nombreCompleto: String = "",
    val parentesco: String = "",
    val telefonoPrincipal: String = "",
    val telefonoAlternativo: String = "",
    val correo: String = "",
    val notificacionesSMS: Boolean = true,
    val notificacionesCorreo: Boolean = true,
    val direccion: String = ""
)

/**
 * Sección 5 — Contacto de emergencia (máximo 3)
 */
data class ContactoEmergencia(
    val nombre: String = "",
    val telefono: String = "",
    val parentesco: String = "",
    val llamarAutomaticamente: Boolean = true
)

/**
 * Sección 6 — Configuración de alertas
 */
data class ConfiguracionAlertas(
    val deteccionCaidas: Boolean = true,
    val tiempoInactividad: Int = 5,
    val notificarTutorPulso: Boolean = true,
    val numeroEmergencias: String = "911"
)

/**
 * Documento raíz — Unifica todas las secciones del Registro de Paciente.
 * Migrado desde interfaces/paciente.ts
 */
data class RegistroPaciente(
    val uid: String = "",
    val datosPaciente: DatosPaciente = DatosPaciente(),
    val infoMedica: InfoMedica = InfoMedica(),
    val umbralesAlerta: UmbralesAlerta = UmbralesAlerta(),
    val datosTutor: DatosTutor = DatosTutor(),
    val contactosEmergencia: List<ContactoEmergencia> = emptyList(),
    val configuracionAlertas: ConfiguracionAlertas = ConfiguracionAlertas(),
    @ServerTimestamp
    val fechaRegistro: Date? = null,
    @ServerTimestamp
    val ultimaActualizacion: Date? = null
)
