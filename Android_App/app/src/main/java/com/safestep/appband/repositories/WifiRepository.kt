package com.safestep.appband.repositories

import android.content.Context
import android.net.wifi.WifiManager
import com.safestep.appband.models.DispositivoConectado
import com.safestep.appband.models.SignalLevel
import com.safestep.appband.models.WifiNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Repositorio para escaneo de redes Wi-Fi y conexión HTTP con ESP32.
 * Migrado desde services/wifi.ts y services/dispositivo-wifi.ts
 */
class WifiRepository(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val _networks = MutableStateFlow<List<WifiNetwork>>(emptyList())
    val networks: StateFlow<List<WifiNetwork>> = _networks.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _selectedNetwork = MutableStateFlow<WifiNetwork?>(null)
    val selectedNetwork: StateFlow<WifiNetwork?> = _selectedNetwork.asStateFlow()

    private val _connectedDevice = MutableStateFlow<DispositivoConectado?>(null)
    val connectedDevice: StateFlow<DispositivoConectado?> = _connectedDevice.asStateFlow()

    fun selectNetwork(network: WifiNetwork) {
        _selectedNetwork.value = network
    }

    suspend fun escaneoRedes(): List<WifiNetwork> = withContext(Dispatchers.IO) {
        _isScanning.value = true
        val lista = mutableListOf<WifiNetwork>()
        try {
            @Suppress("DEPRECATION")
            val scanResults = wifiManager.scanResults
            for (result in scanResults) {
                if (result.SSID.isNotBlank()) {
                    val level = calcularNivelSignal(result.level)
                    val freq = if (result.frequency >= 5000) "5 GHz" else "2.4 GHz"
                    val sec = normalizarSeguridad(result.capabilities)

                    lista.add(
                        WifiNetwork(
                            ssid = result.SSID,
                            rssi = result.level,
                            signalLevel = level,
                            security = sec,
                            frequency = freq
                        )
                    )
                }
            }
            lista.sortByDescending { it.rssi }
        } catch (e: Exception) {
            // Manejo de permisos o error
        } finally {
            _isScanning.value = false
        }
        _networks.value = lista
        lista
    }

    suspend fun conectarESP32(ip: String): Result<DispositivoConectado> = withContext(Dispatchers.IO) {
        val ipLimpia = ip.trim()
        if (ipLimpia.isEmpty()) return@withContext Result.failure(Exception("Dirección IP inválida"))

        try {
            val request = Request.Builder()
                .url("http://$ipLimpia/status")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val dispositivo = DispositivoConectado(
                        ip = ipLimpia,
                        nombre = "ESP32 ($ipLimpia)",
                        fechaConexion = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault()).format(java.util.Date())
                    )
                    _connectedDevice.value = dispositivo
                    Result.success(dispositivo)
                } else {
                    Result.failure(Exception("HTTP Error ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calcularNivelSignal(rssi: Int): SignalLevel {
        return when {
            rssi >= -52 -> SignalLevel.EXCELENTE
            rssi >= -67 -> SignalLevel.BUENA
            rssi >= -80 -> SignalLevel.REGULAR
            else -> SignalLevel.DEBIL
        }
    }

    private fun normalizarSeguridad(capabilities: String): String {
        val caps = capabilities.uppercase()
        return when {
            caps.contains("WPA3") -> "WPA3"
            caps.contains("WPA2") -> "WPA2"
            caps.contains("OPEN") -> "Abierta"
            else -> "Desconocida"
        }
    }
}
