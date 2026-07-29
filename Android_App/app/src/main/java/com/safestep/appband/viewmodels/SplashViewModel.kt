package com.safestep.appband.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para SplashActivity.
 * Maneja el temporizador de 3 segundos antes de navegar al Login.
 * Migrado desde components/index/index.component.ts
 */
class SplashViewModel : ViewModel() {

    private val _navigateToLoginEvent = MutableStateFlow(false)
    val navigateToLoginEvent: StateFlow<Boolean> = _navigateToLoginEvent.asStateFlow()

    init {
        iniciarTemporizador()
    }

    private fun iniciarTemporizador() {
        viewModelScope.launch {
            delay(3000) // 3 segundos exactos como en Ionic index.component.ts
            _navigateToLoginEvent.value = true
        }
    }
}
