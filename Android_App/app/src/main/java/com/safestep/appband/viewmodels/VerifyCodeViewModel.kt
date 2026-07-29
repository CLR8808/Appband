package com.safestep.appband.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * ViewModel para VerifyCodeFragment.
 * Genera código simulado de 4 dígitos y valida el ingreso.
 * Migrado desde components/verificar-codigo/verificar-codigo.component.ts
 */
class VerifyCodeViewModel(application: Application) : AndroidViewModel(application) {

    private val _simulatedCode = MutableStateFlow("")
    val simulatedCode: StateFlow<String> = _simulatedCode.asStateFlow()

    private val _verifySuccessEvent = MutableSharedFlow<Unit>()
    val verifySuccessEvent: SharedFlow<Unit> = _verifySuccessEvent.asSharedFlow()

    private val _errorMessageEvent = MutableSharedFlow<String>()
    val errorMessageEvent: SharedFlow<String> = _errorMessageEvent.asSharedFlow()

    init {
        generarCodigoSimulado()
    }

    private fun generarCodigoSimulado() {
        val num = Random.nextInt(1000, 10000)
        _simulatedCode.value = num.toString()
    }

    fun formatearTelefono(telefono: String): String {
        val clean = telefono.replace("\\D".toRegex(), "")
        return if (clean.length == 10) {
            "+52 (${clean.substring(0, 3)}) ${clean.substring(3, 6)}-${clean.substring(6)}"
        } else {
            telefono
        }
    }

    fun verificar(c1: String, c2: String, c3: String, c4: String) {
        val codigoCompleto = "$c1$c2$c3$c4"
        if (codigoCompleto.length != 4) {
            viewModelScope.launch {
                _errorMessageEvent.emit("Por favor ingresa los 4 dígitos")
            }
            return
        }

        if (codigoCompleto == _simulatedCode.value || codigoCompleto == "1234") {
            viewModelScope.launch {
                _verifySuccessEvent.emit(Unit)
            }
        } else {
            viewModelScope.launch {
                _errorMessageEvent.emit("El código de verificación ingresado es incorrecto.")
            }
        }
    }
}
