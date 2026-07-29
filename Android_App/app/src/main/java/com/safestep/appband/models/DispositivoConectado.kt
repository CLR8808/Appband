package com.safestep.appband.models

/**
 * Modelo para dispositivo ESP32 / SafeBand conectado vía Wi-Fi HTTP.
 * Migrado desde services/dispositivo-wifi.ts
 */
data class DispositivoConectado(
    val ip: String = "",
    val nombre: String = "",
    val fechaConexion: String = ""
)

/**
 * Modelo para lista de dispositivos guardados en Storage.
 * Migrado desde services/storage.ts
 */
data class Dispositivo(
    val nombre: String = ""
)
