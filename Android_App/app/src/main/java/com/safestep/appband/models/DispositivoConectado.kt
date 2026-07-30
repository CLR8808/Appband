package com.safestep.appband.models

/**
 * Modelo para dispositivo ESP32 / SafeBand conectado vía Wi-Fi HTTP.
 */
data class DispositivoConectado(
    val ip: String = "",
    val nombre: String = "",
    val fechaConexion: String = ""
)

/**
 * Modelo completo para lista de dispositivos guardados en Storage.
 */
data class Dispositivo(
    val nombre: String = "SafeBand",
    val macAddress: String = "",
    val ip: String = "",
    val tipoConexion: String = "BLE",
    val fechaAgregado: String = "",
    val estado: String = "Conectado"
)
