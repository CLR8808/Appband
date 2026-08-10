package com.safestep.appband.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safestep.appband.repositories.BluetoothRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * ViewModel para PasswordFragment.
 * Envía las credenciales Wi-Fi (SSID y Password) a la SafeBand vía Bluetooth.
 */
class PasswordViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothRepo = BluetoothRepository.getInstance(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _connectSuccessEvent = MutableSharedFlow<Unit>()
    val connectSuccessEvent: SharedFlow<Unit> = _connectSuccessEvent.asSharedFlow()

    private val _errorMessageEvent = MutableSharedFlow<String>()
    val errorMessageEvent: SharedFlow<String> = _errorMessageEvent.asSharedFlow()

    fun conectarWifi(ssid: String, password: String) {
        val targetSsid = if (ssid.isNotBlank()) ssid else "Mega-2.4G-24B1"
        val targetPass = if (password.isNotBlank()) password else "QmteqN9Ejd"

        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Trama JSON transmitida por Bluetooth a la pulsera SafeBand: {"ssid":"...","password":"..."}
                val payload = JSONObject().apply {
                    put("ssid", targetSsid)
                    put("password", targetPass)
                }.toString() + "\n"

                // Transmitir trama a través de la conexión Bluetooth activa
                bluetoothRepo.enviarComando(payload)
                delay(1500)

                _isLoading.value = false
                _connectSuccessEvent.emit(Unit)
            } catch (e: Exception) {
                _isLoading.value = false
                _errorMessageEvent.emit("Error al enviar credenciales: ${e.message}")
            }
        }
    }
}
