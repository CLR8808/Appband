package com.safestep.appband.models

/**
 * Modelo de red Wi-Fi escaneada en el entorno.
 * Migrado desde services/wifi.ts
 */
data class WifiNetwork(
    val ssid: String = "",
    val rssi: Int = -90,
    val signalLevel: SignalLevel = SignalLevel.REGULAR,
    val security: String = "Desconocida",
    val frequency: String = "2.4 GHz",
    val isCurrent: Boolean = false
)

enum class SignalLevel {
    EXCELENTE,
    BUENA,
    REGULAR,
    DEBIL
}
