package com.safestep.appband.models

/**
 * Modelo de datos para dispositivos Bluetooth BLE (SafeBand / ESP32).
 */
data class DispositivoBluetooth(
    val nombre: String,
    val macAddress: String,
    val rssi: Int = 0,
    val isSafeBand: Boolean = false,
    val conectado: Boolean = false
)
