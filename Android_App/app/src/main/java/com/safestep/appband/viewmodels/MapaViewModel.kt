package com.safestep.appband.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safestep.appband.models.DeviceCoordinates
import com.safestep.appband.repositories.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para MapaFragment.
 * Migrado desde components/mapa/mapa.component.ts
 */
class MapaViewModel(application: Application) : AndroidViewModel(application) {

    private val locationRepository = LocationRepository(application)

    private val _currentLocation = MutableStateFlow<DeviceCoordinates?>(null)
    val currentLocation: StateFlow<DeviceCoordinates?> = _currentLocation.asStateFlow()

    init {
        iniciarSeguimientoGPS()
    }

    fun obtenerUbicacionActual() {
        viewModelScope.launch {
            val result = locationRepository.obtenerUbicacionActual()
            result.getOrNull()?.let { coords ->
                _currentLocation.value = coords
            }
        }
    }

    private fun iniciarSeguimientoGPS() {
        viewModelScope.launch {
            locationRepository.seguimientoUbicacionFlow().collect { coords ->
                _currentLocation.value = coords
            }
        }
    }
}
