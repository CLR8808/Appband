package com.safestep.appband.models

import java.io.Serializable

/**
 * Modelo de datos para un Incidente / Evento detectado por SafeBand.
 *
 * @param id                 Identificador único del evento en Firestore.
 * @param tipo               Tipo de severidad: "CRITICO" o "ADVERTENCIA".
 * @param titulo             Título breve del incidente (ej. "POSIBLE CAÍDA DETECTADA").
 * @param descripcion        Descripción extendida del incidente.
 * @param tiempoTexto        Texto legible del tiempo transcurrido (ej. "Hace 5 minutos").
 * @param timestamp          Timestamp Unix del evento (ms).
 * @param latitud            Latitud GPS para el mapa.
 * @param longitud           Longitud GPS para el mapa.
 * @param ubicacionTexto     Nombre legible de la ubicación (ej. "Mazatlán, Sinaloa").
 * @param frecuenciaCardiaca Frecuencia cardíaca (ej. "115 BPM").
 * @param estadoPulsera      Estado de la pulsera (ej. "Activa").
 * @param fechaTexto         Fecha legible (ej. "08/08/2026").
 * @param horaTexto          Hora legible (ej. "10:15 AM").
 * @param revisado           Si el incidente ha sido marcado como revisado.
 */
data class Evento(
    val id: String = "",
    val tipo: String = "ADVERTENCIA",
    val titulo: String = "",
    val descripcion: String = "",
    val tiempoTexto: String = "",
    val timestamp: Long = 0L,
    val latitud: Double = 23.264864,
    val longitud: Double = -106.430832,
    val ubicacionTexto: String = "Mazatlán, Sinaloa",
    val frecuenciaCardiaca: String = "85 BPM",
    val estadoPulsera: String = "Activa",
    val fechaTexto: String = "",
    val horaTexto: String = "",
    val revisado: Boolean = false
) : Serializable
