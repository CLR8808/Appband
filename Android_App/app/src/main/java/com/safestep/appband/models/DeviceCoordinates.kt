package com.safestep.appband.models

/**
 * Modelo de Coordenadas GPS del dispositivo / mapa.
 * Migrado desde services/location.ts
 */
data class DeviceCoordinates(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val accuracy: Float? = null,
    val timestamp: Long? = null
)
