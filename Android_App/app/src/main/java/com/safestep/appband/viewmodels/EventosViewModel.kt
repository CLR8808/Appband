package com.safestep.appband.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safestep.appband.models.Evento
import com.safestep.appband.repositories.EventosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel para EventosFragment.
 * Filtra los incidentes por estado de revisión (Nuevos/Pendientes vs Revisados).
 */
class EventosViewModel(
    private val repository: EventosRepository = EventosRepository()
) : ViewModel() {

    private val _todosLosEventos = MutableStateFlow<List<Evento>>(emptyList())
    val todosLosEventos: StateFlow<List<Evento>> = _todosLosEventos.asStateFlow()

    private val _filtroMostrarRevisados = MutableStateFlow(false)
    val filtroMostrarRevisados: StateFlow<Boolean> = _filtroMostrarRevisados.asStateFlow()

    // Conteo de nuevos incidentes no revisados
    val conteoNuevos: StateFlow<Int> = combine(_todosLosEventos) { lista ->
        lista[0].count { !it.revisado }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // Lista filtrada según la pestaña activa (Nuevos vs Revisados)
    val eventosFiltrados: StateFlow<List<Evento>> = combine(_todosLosEventos, _filtroMostrarRevisados) { lista, mostrarRevisados ->
        if (mostrarRevisados) {
            lista.filter { it.revisado }
        } else {
            lista.filter { !it.revisado }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        cargarIncidentes()
    }

    fun cargarIncidentes() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.obtenerIncidentesFlow().collect { lista ->
                _todosLosEventos.value = lista
                _isLoading.value = false
            }
        }
    }

    fun setFiltroRevisados(mostrarRevisados: Boolean) {
        _filtroMostrarRevisados.value = mostrarRevisados
    }

    fun cargarIncidentesAnteriores() {
        cargarIncidentes()
    }
}
