package com.safestep.appband.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safestep.appband.models.DatosTelemetriaBle
import com.safestep.appband.models.Dispositivo
import com.safestep.appband.repositories.BluetoothRepository
import com.safestep.appband.repositories.SensorFirebaseRepository
import com.safestep.appband.repositories.StorageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel para DispositivoFragment.
 * Expone datos del sensor desde el último registro de Firestore en tiempo real,
 * el estado de conexión y acciones de desvinculación.
 */
class DispositivoViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothRepo = BluetoothRepository.getInstance(application)
    private val storageRepo = StorageRepository(application)
    private val sensorFirebaseRepo = SensorFirebaseRepository.getInstance()

    val telemetria: StateFlow<DatosTelemetriaBle> = bluetoothRepo.telemetria
    val estadoConexion: StateFlow<BluetoothRepository.EstadoConexion> = bluetoothRepo.estadoConexion
    val dispositivoActivo: StateFlow<Dispositivo?> = storageRepo.dispositivoActivoFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Datos del sensor desde el último registro en Firestore
    val datosSensor: StateFlow<SensorFirebaseRepository.DatosSensor> = sensorFirebaseRepo.datosSensor
    val conectadoFirebase: StateFlow<Boolean> = sensorFirebaseRepo.conectadoFirebase

    // Promedios diarios
    val promedioPulso: StateFlow<String> = kotlinx.coroutines.flow.MutableStateFlow("-- BPM")
    val promedioOxigenacion: StateFlow<String> = kotlinx.coroutines.flow.MutableStateFlow("-- %")

    init {
        iniciarEscucha()
    }

    fun iniciarEscucha() {
        sensorFirebaseRepo.iniciarEscucha()
    }

    fun recargarDatos() {
        sensorFirebaseRepo.iniciarEscucha()
    }

    fun desconectar() {
        bluetoothRepo.desconectar()
    }

    fun actualizarNombre(nuevoNombre: String) {
        viewModelScope.launch {
            storageRepo.actualizarNombreDispositivoActivo(nuevoNombre)
        }
    }

    fun desvincularPulsera(nombreDispositivo: String = "", onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            bluetoothRepo.desconectar()
            storageRepo.desvincularDispositivo(nombreDispositivo)
            onComplete()
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
