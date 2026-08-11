package com.safestep.appband.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.safestep.appband.activities.AuthActivity
import com.safestep.appband.activities.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para SplashActivity.
 * Maneja la animación de 2.5 segundos y verifica si existe una sesión activa de Firebase.
 * Si el usuario está autenticado -> navega directamente a MainActivity.
 * Si el usuario no está autenticado -> navega a AuthActivity (Login).
 */
class SplashViewModel : ViewModel() {

    private val _navigationTarget = MutableStateFlow<Class<*>?>(null)
    val navigationTarget: StateFlow<Class<*>?> = _navigationTarget.asStateFlow()

    init {
        verificarSesionYNavegar()
    }

    private fun verificarSesionYNavegar() {
        viewModelScope.launch {
            delay(2500) // 2.5 segundos de pantalla de Splash

            val usuarioActual = FirebaseAuth.getInstance().currentUser
            if (usuarioActual != null) {
                // Usuario autenticado previamente -> ir directamente a la pantalla principal
                _navigationTarget.value = MainActivity::class.java
            } else {
                // Sin sesión activa -> ir al Login
                _navigationTarget.value = AuthActivity::class.java
            }
        }
    }
}
