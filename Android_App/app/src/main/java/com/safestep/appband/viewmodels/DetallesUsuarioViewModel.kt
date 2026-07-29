package com.safestep.appband.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
 * ViewModel para DetallesUsuarioFragment.
 * Migrado desde components/detalles-usuario/detalles-usuario.component.ts
 */
class DetallesUsuarioViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val usuarioRepository = UsuarioRepository(application)

    private val _perfil = MutableStateFlow<PerfilUsuario?>(null)
    val perfil: StateFlow<PerfilUsuario?> = _perfil.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _saveSuccessEvent = MutableSharedFlow<Unit>()
    val saveSuccessEvent: SharedFlow<Unit> = _saveSuccessEvent.asSharedFlow()

    private val _errorMessageEvent = MutableSharedFlow<String>()
    val errorMessageEvent: SharedFlow<String> = _errorMessageEvent.asSharedFlow()

    init {
        cargarPerfil()
    }

    private fun cargarPerfil() {
        val user = authRepository.usuarioActual ?: return
        viewModelScope.launch {
            usuarioRepository.obtenerUsuarioFlow(user.uid).collect { perfilObtenido ->
                _perfil.value = perfilObtenido ?: PerfilUsuario(
                    uid = user.uid,
                    nombre = user.displayName ?: "",
                    correo = user.email ?: ""
                )
            }
        }
    }

    fun guardarCambios(nombre: String, telefono: String, genero: String, fechaNacimiento: String) {
        val nameClean = nombre.trim()
        val phoneClean = telefono.replace("\\D".toRegex(), "")

        if (nameClean.isEmpty()) {
            viewModelScope.launch { _errorMessageEvent.emit("El nombre no puede estar vacío.") }
            return
        }
        if (phoneClean.isNotEmpty() && phoneClean.length != 10) {
            viewModelScope.launch { _errorMessageEvent.emit("El teléfono debe tener 10 dígitos.") }
            return
        }

        val uid = authRepository.usuarioActual?.uid ?: return
        _isLoading.value = true

        val updateMap = hashMapOf<String, Any>(
            "nombre" to nameClean,
            "telefono" to phoneClean,
            "genero" to genero,
            "fechaNacimiento" to fechaNacimiento
        )

        viewModelScope.launch {
            val result = usuarioRepository.actualizarUsuario(uid, updateMap)
            _isLoading.value = false

            result.fold(
                onSuccess = {
                    _saveSuccessEvent.emit(Unit)
                },
                onFailure = { err ->
                    _errorMessageEvent.emit(err.message ?: "No se pudieron guardar los cambios.")
                }
            )
        }
    }
}
