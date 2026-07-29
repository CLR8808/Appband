package com.safestep.appband.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safestep.appband.models.Dispositivo
import com.safestep.appband.repositories.StorageRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel para ConfiguracionFragment.
 * Migrado desde components/configuracion/configuracion.component.ts
 */
class ConfiguracionViewModel(application: Application) : AndroidViewModel(application) {

    private val storageRepository = StorageRepository(application)

    val dispositivos: StateFlow<List<Dispositivo>> = storageRepository.dispositivosFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _desvincularSuccessEvent = MutableSharedFlow<Unit>()
    val desvincularSuccessEvent: SharedFlow<Unit> = _desvincularSuccessEvent.asSharedFlow()

    fun desvincular() {
        viewModelScope.launch {
            storageRepository.eliminarDispositivos()
            _desvincularSuccessEvent.emit(Unit)
        }
    }
}
