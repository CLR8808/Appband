package com.safestep.appband.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.safestep.appband.models.DatosTelemetriaBle
import com.safestep.appband.repositories.BluetoothRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel para DispositivoFragment.
 * Expone la telemetría en tiempo real del sensor DHT11.
 */
class DispositivoViewModel(application: Application) : AndroidViewModel(application) {

    // Usamos la instancia única del repositorio
    private val bluetoothRepo = BluetoothRepository.getInstance(application)

    val telemetria: StateFlow<DatosTelemetriaBle> = bluetoothRepo.telemetria
    val estadoConexion: StateFlow<BluetoothRepository.EstadoConexion> = bluetoothRepo.estadoConexion

    fun desconectar() {
        bluetoothRepo.desconectar()
    }
}
