package com.safestep.appband.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.safestep.appband.helpers.HealthConfigHelper
import com.safestep.appband.models.RegistroPaciente
import com.safestep.appband.repositories.AuthRepository
import com.safestep.appband.repositories.PacienteRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para RegistroPacienteFragment.
 * Migrado desde components/registro-paciente/registro-paciente.component.ts
 */
class RegistroPacienteViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val pacienteRepository = PacienteRepository(application)

    private val _currentStep = MutableStateFlow(0)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _registroState = MutableStateFlow(RegistroPaciente())
    val registroState: StateFlow<RegistroPaciente> = _registroState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveSuccessEvent = MutableSharedFlow<Unit>()
    val saveSuccessEvent: SharedFlow<Unit> = _saveSuccessEvent.asSharedFlow()

    private val _errorMessageEvent = MutableSharedFlow<String>()
    val errorMessageEvent: SharedFlow<String> = _errorMessageEvent.asSharedFlow()

    init {
        cargarDatosExistentes()
    }

    private fun cargarDatosExistentes() {
        val user = authRepository.usuarioActual ?: return
        viewModelScope.launch {
            pacienteRepository.obtenerPacienteFlow(user.uid).collect { paciente ->
                if (paciente != null) {
                    _registroState.value = paciente
                }
            }
        }
    }

    fun setStep(step: Int) {
        if (step in 0..6) {
            _currentStep.value = step
        }
    }

    fun siguientePaso() {
        if (_currentStep.value < 6) {
            _currentStep.value++
        }
    }

    fun pasoAnterior() {
        if (_currentStep.value > 0) {
            _currentStep.value--
        }
    }

    fun guardarRegistro(datosCompletos: RegistroPaciente) {
        val user = authRepository.usuarioActual
        if (user == null) {
            viewModelScope.launch {
                _errorMessageEvent.emit("Debe iniciar sesión para registrar un paciente.")
            }
            return
        }

        _isLoading.value = true
        val pacienteFinal = datosCompletos.copy(uid = user.uid)

        viewModelScope.launch {
            val result = pacienteRepository.guardarPaciente(pacienteFinal)
            _isLoading.value = false

            result.fold(
                onSuccess = {
                    _saveSuccessEvent.emit(Unit)
                },
                onFailure = { err ->
                    _errorMessageEvent.emit(err.message ?: "Error al guardar paciente.")
                }
            )
        }
    }
}
