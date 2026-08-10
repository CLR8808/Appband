package com.safestep.appband.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthException
import com.safestep.appband.repositories.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para LoginFragment.
 * Migrado desde components/iniciar-sesion/iniciar-sesion.component.ts
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loginSuccessEvent = MutableSharedFlow<Unit>()
    val loginSuccessEvent: SharedFlow<Unit> = _loginSuccessEvent.asSharedFlow()

    private val _errorMessageEvent = MutableSharedFlow<String>()
    val errorMessageEvent: SharedFlow<String> = _errorMessageEvent.asSharedFlow()

    fun manejarInicioSesion(correo: String, contrasena: String) {
        val emailClean = correo.trim()
        val passClean = contrasena.trim()

        if (emailClean.isEmpty() || passClean.isEmpty()) {
            viewModelScope.launch {
                _errorMessageEvent.emit("Ingresa tu correo y contraseña")
            }
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            val result = authRepository.iniciarSesion(emailClean, passClean)
            _isLoading.value = false

            result.fold(
                onSuccess = {
                    _loginSuccessEvent.emit(Unit)
                },
                onFailure = { error ->
                    _errorMessageEvent.emit(obtenerMensajeError(error))
                }
            )
        }
    }

    private fun obtenerMensajeError(error: Throwable): String {
        android.util.Log.e("LoginVM", "Error login: ${error.javaClass.simpleName} - ${error.message}")
        return if (error is FirebaseAuthException) {
            android.util.Log.e("LoginVM", "Firebase error code: ${error.errorCode}")
            when (error.errorCode) {
                "ERROR_INVALID_EMAIL", "auth/invalid-email" -> "Ingresa un correo válido."
                "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND",
                "ERROR_INVALID_CREDENTIAL", "auth/invalid-credential" -> "Correo o contraseña incorrectos."
                "ERROR_USER_DISABLED" -> "Esta cuenta ha sido deshabilitada."
                "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos. Espera un momento."
                "ERROR_OPERATION_NOT_ALLOWED" -> "Activa el inicio de sesión con correo y contraseña en Firebase Authentication."
                "ERROR_NETWORK_REQUEST_FAILED" -> "Error de red. Revisa tu conexión a internet."
                else -> "Error de autenticación (${error.errorCode}). Intenta de nuevo."
            }
        } else {
            android.util.Log.e("LoginVM", "Non-Firebase error: ${error.javaClass.name}")
            error.message ?: "No se pudo iniciar sesión. Intenta de nuevo."
        }
    }
}
