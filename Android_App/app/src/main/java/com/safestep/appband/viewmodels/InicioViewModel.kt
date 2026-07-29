package com.safestep.appband.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safestep.appband.models.Dispositivo
import com.safestep.appband.repositories.StorageRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel para InicioFragment.
 * Migrado desde components/inicio/inicio.component.ts
 */
class InicioViewModel(application: Application) : AndroidViewModel(application) {

    private val storageRepository = StorageRepository(application)

    val dispositivos: StateFlow<List<Dispositivo>> = storageRepository.dispositivosFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
