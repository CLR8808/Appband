package com.safestep.appband.models

/**
 * Modelo de datos para la telemetría continua recibida en tiempo real desde la pulsera SafeBand / ESP32.
 * Actualizado para recibir datos de sensor DHT11 (Temperatura y Humedad).
 */
data class DatosTelemetriaBle(
    val dispositivoNombre: String = "SafeBand",
    val macAddress: String = "",
    val conectado: Boolean = false,
    val temperatura: Float? = null,
    val humedad: Float? = null,
    val bateriaPorcentaje: Int? = null,
    val alertaActiva: String? = null,
    val ultimoMensaje: String = "Esperando telemetría de la pulsera...",
    val timestampMs: Long = System.currentTimeMillis(),
    val serviciosCount: Int = 0
)
