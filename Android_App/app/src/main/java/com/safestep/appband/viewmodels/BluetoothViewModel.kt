package com.safestep.appband.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.safestep.appband.models.DispositivoBluetooth
import com.safestep.appband.repositories.BluetoothRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel para la pantalla / fragment de conectividad Bluetooth.
 */
class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothRepo = BluetoothRepository(application)

    val dispositivosEncontrados: StateFlow<List<DispositivoBluetooth>> = bluetoothRepo.dispositivosEncontrados
    val estadoConexion: StateFlow<BluetoothRepository.EstadoConexionBle> = bluetoothRepo.estadoConexion
    val pulsoActual: StateFlow<Int?> = bluetoothRepo.pulsoActual

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

    fun enviarWifiConfig(ssid: String, pass: String): Boolean {
        return bluetoothRepo.enviarConfiguracionWifiOverBle(ssid, pass)
    }
}
