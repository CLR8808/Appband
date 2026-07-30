package com.safestep.appband.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.safestep.appband.models.DatosTelemetriaBle
import com.safestep.appband.models.DispositivoBluetooth
import com.safestep.appband.repositories.BluetoothRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel nativo para conectividad Bluetooth BLE real y telemetría continua.
 */
class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothRepo = BluetoothRepository(application)

    val dispositivosEncontrados: StateFlow<List<DispositivoBluetooth>> = bluetoothRepo.dispositivosEncontrados
    val estadoConexion: StateFlow<BluetoothRepository.EstadoConexionBle> = bluetoothRepo.estadoConexion
    val pulsoActual: StateFlow<Int?> = bluetoothRepo.pulsoActual
    val telemetria: StateFlow<DatosTelemetriaBle> = bluetoothRepo.telemetria

    fun isBluetoothEnabled(): Boolean = bluetoothRepo.isBluetoothHabilitado()
    fun isUbicacionEnabled(): Boolean = bluetoothRepo.isUbicacionHabilitada()

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

    fun enviarComando(comando: String): Boolean {
        return bluetoothRepo.enviarComando(comando)
    }

    fun enviarWifiConfig(ssid: String, pass: String): Boolean {
        return bluetoothRepo.enviarConfiguracionWifiOverBle(ssid, pass)
    }
}
