package com.safestep.appband.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safestep.appband.models.Evento
import com.safestep.appband.repositories.EventosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel para EventosFragment.
 * Carga en tiempo real los incidentes/eventos registrados en la base de datos Firestore.
 */
class EventosViewModel(
    private val repository: EventosRepository = EventosRepository()
) : ViewModel() {

    private val _eventos = MutableStateFlow<List<Evento>>(emptyList())
    val eventos: StateFlow<List<Evento>> = _eventos.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        cargarIncidentes()
    }

    fun cargarIncidentes() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.obtenerIncidentesFlow().collectLatest { lista ->
                _eventos.value = lista
                _isLoading.value = false
            }
        }
    }

    fun cargarIncidentesAnteriores() {
        cargarIncidentes()
    }
}
