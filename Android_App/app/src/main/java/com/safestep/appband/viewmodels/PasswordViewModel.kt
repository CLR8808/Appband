package com.safestep.appband.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para PasswordFragment.
 * Migrado desde components/password/password.component.ts
 */
class PasswordViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _connectSuccessEvent = MutableSharedFlow<Unit>()
    val connectSuccessEvent: SharedFlow<Unit> = _connectSuccessEvent.asSharedFlow()

    private val _errorMessageEvent = MutableSharedFlow<String>()
    val errorMessageEvent: SharedFlow<String> = _errorMessageEvent.asSharedFlow()

    fun conectarWifi(password: String) {
        if (password == "123456") {
            _isLoading.value = true
            viewModelScope.launch {
                delay(2000) // Simular 2s como en Ionic
                _isLoading.value = false
                _connectSuccessEvent.emit(Unit)
            }
        } else {
            viewModelScope.launch {
                _errorMessageEvent.emit("Contraseña incorrecta")
            }
        }
    }
}
