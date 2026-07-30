package com.safestep.appband.models

/**
 * Modelo de datos para la telemetría continua recibida en tiempo real desde la pulsera SafeBand / ESP32.
 */
data class DatosTelemetriaBle(
    val dispositivoNombre: String = "SafeBand",
    val macAddress: String = "",
    val conectado: Boolean = false,
    val pulsoBpm: Int? = null,
    val bateriaPorcentaje: Int? = null,
    val alertaActiva: String? = null,
    val ultimoMensaje: String = "Esperando telemetría de la pulsera...",
    val timestampMs: Long = System.currentTimeMillis(),
    val serviciosCount: Int = 0
)
