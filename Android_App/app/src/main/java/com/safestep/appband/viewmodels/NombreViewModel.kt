package com.safestep.appband.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safestep.appband.repositories.StorageRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para NombreFragment.
 * Migrado desde components/nombre/nombre.component.ts
 */
class NombreViewModel(application: Application) : AndroidViewModel(application) {

    private val storageRepository = StorageRepository(application)

    private val _saveSuccessEvent = MutableSharedFlow<Unit>()
    val saveSuccessEvent: SharedFlow<Unit> = _saveSuccessEvent.asSharedFlow()

    fun guardarDispositivo(nombre: String) {
        val nombreLimpio = if (nombre.trim().isEmpty()) "SafeBand_V1" else nombre.trim()
        viewModelScope.launch {
            storageRepository.agregarDispositivo(nombreLimpio)
            _saveSuccessEvent.emit(Unit)
        }
    }
}
