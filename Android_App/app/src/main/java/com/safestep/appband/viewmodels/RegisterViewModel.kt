package com.safestep.appband.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthException
import com.safestep.appband.models.PerfilUsuario
import com.safestep.appband.repositories.AuthRepository
import com.safestep.appband.repositories.UsuarioRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para RegisterFragment.
 * Migrado desde components/crear-cuenta/crear-cuenta.component.ts
 */
class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val usuarioRepository = UsuarioRepository(application)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _registerSuccessEvent = MutableSharedFlow<String>() // Emite el teléfono registrado
    val registerSuccessEvent: SharedFlow<String> = _registerSuccessEvent.asSharedFlow()

    private val _errorMessageEvent = MutableSharedFlow<String>()
    val errorMessageEvent: SharedFlow<String> = _errorMessageEvent.asSharedFlow()

    fun validarYRegistrar(
        nombre: String,
        telefono: String,
        email: String,
        genero: String,
        fechaNacimiento: String,
        pass: String,
        confirmPass: String
    ) {
        val nameClean = nombre.trim()
        val phoneClean = telefono.replace("\\D".toRegex(), "")
        val emailClean = email.trim()

        if (nameClean.isEmpty()) {
            emitError("Ingresa el nombre.")
            return
        }
        if (phoneClean.length != 10) {
            emitError("Debe tener 10 dígitos.")
            return
        }
        if (!emailClean.contains("@") || !emailClean.lowercase().contains(".com")) {
            emitError("Ingresa un correo válido.")
            return
        }
        if (genero.isEmpty()) {
            emitError("Selecciona el género.")
            return
        }
        if (fechaNacimiento.isEmpty()) {
            emitError("Ingresa la fecha de nacimiento.")
            return
        }
        if (pass.isEmpty()) {
            emitError("Ingresa una contraseña.")
            return
        }
        if (pass != confirmPass) {
            emitError("Las contraseñas no coinciden.")
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            val authResult = authRepository.crearCuenta(emailClean, pass)
            authResult.fold(
                onSuccess = { user ->
                    val perfil = PerfilUsuario(
                        uid = user.uid,
                        nombre = nameClean,
                        correo = emailClean,
                        genero = genero,
                        telefono = phoneClean,
                        fechaNacimiento = fechaNacimiento
                    )

                    val saveResult = usuarioRepository.guardarUsuario(perfil)
                    _isLoading.value = false

                    saveResult.fold(
                        onSuccess = {
                            _registerSuccessEvent.emit(phoneClean)
                        },
                        onFailure = { err ->
                            if (err.message?.contains("PERMISSION_DENIED") == true || err.message?.contains("permission-denied") == true) {
                                emitError("La cuenta se creó. Tus datos se mostrarán en este dispositivo, pero Firestore aún no permite guardar el perfil.")
                                _registerSuccessEvent.emit(phoneClean)
                            } else {
                                emitError(obtenerMensajeError(err))
                            }
                        }
                    )
                },
                onFailure = { err ->
                    _isLoading.value = false
                    emitError(obtenerMensajeError(err))
                }
            )
        }
    }

    private fun emitError(msg: String) {
        viewModelScope.launch {
            _errorMessageEvent.emit(msg)
        }
    }

    private fun obtenerMensajeError(error: Throwable): String {
        return if (error is FirebaseAuthException) {
            when (error.errorCode) {
                "ERROR_EMAIL_ALREADY_IN_USE", "auth/email-already-in-use" -> "Ese correo ya tiene una cuenta."
                "ERROR_INVALID_EMAIL", "auth/invalid-email" -> "Ingresa un correo válido."
                "ERROR_WEAK_PASSWORD", "auth/weak-password" -> "La contraseña debe tener al menos 6 caracteres."
                "ERROR_OPERATION_NOT_ALLOWED" -> "Activa el registro con correo y contraseña en Firebase Authentication."
                else -> "No se pudo conectar con Firebase. Revisa tu conexión a internet."
            }
        } else {
            error.message ?: "No se pudo crear la cuenta. Intenta de nuevo."
        }
    }
}
