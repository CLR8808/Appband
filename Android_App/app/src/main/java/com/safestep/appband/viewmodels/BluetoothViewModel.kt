package com.safestep.appband.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safestep.appband.models.DatosTelemetriaBle
import com.safestep.appband.models.Dispositivo
import com.safestep.appband.models.DispositivoBluetooth
import com.safestep.appband.repositories.BluetoothRepository
import com.safestep.appband.repositories.StorageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel para conectividad Bluetooth Clásico, telemetría continua y persistencia local del dispositivo.
 */
class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothRepo = BluetoothRepository.getInstance(application)
    private val storageRepo = StorageRepository(application)

    val dispositivosEncontrados: StateFlow<List<DispositivoBluetooth>> = bluetoothRepo.dispositivosEncontrados
    val estadoConexion: StateFlow<BluetoothRepository.EstadoConexion> = bluetoothRepo.estadoConexion
    val telemetria: StateFlow<DatosTelemetriaBle> = bluetoothRepo.telemetria

    val dispositivoGuardadoActivo: StateFlow<Dispositivo?> = storageRepo.dispositivoActivoFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun isBluetoothEnabled(): Boolean = bluetoothRepo.isBluetoothHabilitado()

    fun iniciarEscaneo() {
        bluetoothRepo.iniciarEscaneoBle()
    }

    fun detenerEscaneo() {
        bluetoothRepo.detenerEscaneoBle()
    }

    fun conectar(macAddress: String) {
        bluetoothRepo.conectarDispositivo(macAddress)
    }

    fun desconectar() {
        bluetoothRepo.desconectar()
    }

    fun guardarDispositivoConectado(nombre: String, macAddress: String, onGuardado: () -> Unit = {}) {
        viewModelScope.launch {
            storageRepo.agregarDispositivoCompleto(nombre, macAddress, "Classic")
            onGuardado()
        }
    }

    fun enviarComando(comando: String): Boolean {
        return bluetoothRepo.enviarComando(comando)
    }

    fun enviarWifiConfig(ssid: String, pass: String): Boolean {
        return bluetoothRepo.enviarComando("WIFI:$ssid:$pass\n")
    }
}
